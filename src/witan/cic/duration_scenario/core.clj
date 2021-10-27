(ns witan.cic.duration-scenario.core
  (:require [witan.cic.duration-scenario.io.read :as read]
            [witan.cic.duration-scenario.io.write :as write]
            [witan.cic.duration-scenario.time :as time]
            [witan.cic.duration-scenario.random :as r]))

(defn add-duration-to-episodes-reducer
  "The duration of each episode is implied by the gap between consecutive offsets.
  This is inconvenient for our purposes, so this function adds a duration key
  by calculating the days between consecutive episodes, or between
  the offset of the final episode and the end of the period."
  [period-duration [acc episodes] [{:keys [placement] :as episode} {:keys [offset]}]]
  (let [duration (if offset
                   (- offset acc)
                   (- period-duration (:offset episode)))]
    [(+ acc duration) (conj episodes (assoc episode :duration duration))]))

(defn apply-duration-cap-reducer
  "Apply the duration cap whilst reducing over episodes. If the duration cap is triggered, return a `reduced` to short-circuit completion."
  [{:keys [snapshot-date provenance beginning]} target-placement duration-cap-days [total-duration episodes] {:keys [placement duration] :as episode}]
  (if (and (= placement target-placement)
           (> duration duration-cap-days)
           (or (= provenance "S")
               (time/<= snapshot-date (time/days-after beginning (+ total-duration duration-cap-days)))))
    (reduced [(+ total-duration duration-cap-days) (conj episodes (dissoc episode :duration))])
    [(+ total-duration duration) (conj episodes (dissoc episode :duration))]))

(defn assoc-some
  [coll k v]
  (cond-> coll
    (not (contains? coll k))
    (assoc k v)))

(defn apply-stochastic-rule-to-period
  [{:keys [episodes beginning end duration snapshot-date provenance] :as period} target-placement duration-cap-days probability-cap-applies random-seed]
  (let [cap-triggered? (<= (r/rand-long random-seed) probability-cap-applies)
        episodes (-> (reduce (partial add-duration-to-episodes-reducer duration) [0 []] (partition-all 2 1 episodes))
                     (last))
        [total-duration episodes] (reduce (partial apply-duration-cap-reducer period target-placement duration-cap-days)
                                          [0 []]
                                          episodes)]
    (if (not= total-duration duration)
      (if cap-triggered?
        (vector (assoc period :marked :remove)
                (-> period
                    (assoc :marked :applied)
                    (assoc :episodes episodes)
                    (assoc :duration total-duration)
                    (assoc :end (time/days-after beginning total-duration))))
        (vector (assoc-some period :marked :not-applied)))
      (vector (assoc-some period :marked :not-applicable)))))

(defn apply-stochastic-rule-to-simulation
  [periods placement duration-cap-days probability-cap-applies random-seed]
  (into []
        (comp (map (fn [[period random-seed]]
                     (apply-stochastic-rule-to-period period placement duration-cap-days probability-cap-applies random-seed)))
              cat)
        (map vector periods (r/split-n random-seed (count periods)))))

(defn apply-duration-scenario-rule
  [{:keys [simulations random-seed] :as state} {:keys [placement duration-cap-days probability-cap-applies]}]
  (-> state
      (update :simulations (partial into [] (map #(apply-stochastic-rule-to-simulation % placement duration-cap-days probability-cap-applies random-seed))))
      (update :random-seed r/next-seed)))

(defn duration-scenario!
  [{:keys [input-periods output-periods output-periods-csv scenario-parameters project-to random-seed] :as config}]
  (let [period-simulations (read/periods input-periods)
        parameters (read/scenario-parameters scenario-parameters)
        scenario-periods (-> (reduce apply-duration-scenario-rule {:simulations period-simulations
                                                                   :random-seed (r/seed random-seed)}
                                     parameters)
                             :simulations)]
    (write/write-nippy! output-periods scenario-periods)
    (->> scenario-periods
         (into [] (map (partial into [] (remove (comp #{:remove} :marked)))))
         (write/episodes-table project-to)
         (write/write-csv! output-periods-csv))))
