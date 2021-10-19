(ns witan.cic.duration-scenario.core
  (:require [witan.cic.duration-scenario.io.read :as read]
            [witan.cic.duration-scenario.io.write :as write]
            [witan.cic.duration-scenario.time :as time]))

(defn apply-cap
  [{:keys [episodes beginning end duration snapshot-date provenance] :as period} target-placement duration-cap-days]
  (let [episodes (-> (reduce (fn [[acc episodes] [{:keys [placement] :as episode} {:keys [offset]}]]
                               (let [duration (if offset
                                                (- offset acc)
                                                (- duration (:offset episode)))]
                                 [(+ acc duration) (conj episodes (assoc episode :duration duration))]))
                             [0 []]
                             (partition-all 2 1 episodes))
                     (last))
        [total-duration episodes] (reduce (fn [[total-duration episodes] {:keys [placement duration] :as episode}]
                                            (if (and (= placement target-placement)
                                                     (> duration duration-cap-days)
                                                     (or (= provenance "S")
                                                         (time/<= snapshot-date (time/days-after beginning (+ total-duration duration-cap-days)))))
                                              (reduced [(+ total-duration duration-cap-days) (conj episodes (dissoc episode :duration))])
                                              [(+ total-duration duration) (conj episodes (dissoc episode :duration))]))
                                          [0 []]
                                          episodes)]
    (if (not= total-duration duration)
      (-> period
          (assoc :episodes episodes)
          (assoc :duration total-duration)
          (assoc :end (time/days-after beginning total-duration)))
      period)))

(defn apply-stochastic-rule-to-period
  [period placement duration-cap-days probability-cap-applies]
  (let [cap-triggered? (<= (rand) probability-cap-applies)]
    (if cap-triggered? 
      (apply-cap period placement duration-cap-days)
      period)))

(defn apply-stochastic-rule-to-simulation
  [periods placement duration-cap-days probability-cap-applies]
  (into []
        (map #(apply-stochastic-rule-to-period % placement duration-cap-days probability-cap-applies))
        periods))

(defn apply-duration-scenario-rule
  [simulations {:keys [placement duration-cap-days probability-cap-applies]}]
  (into []
        (map #(apply-stochastic-rule-to-simulation % placement duration-cap-days probability-cap-applies))
        simulations))

(defn duration-scenario!
  [{:keys [input-periods output-periods output-periods-csv scenario-parameters project-to] :as config}]
  (let [period-simulations (read/periods input-periods)
        parameters (read/scenario-parameters scenario-parameters)
        scenario-periods (reduce apply-duration-scenario-rule period-simulations parameters)]
    (write/write-edn! output-periods scenario-periods)
    (->> (write/episodes-table project-to scenario-periods)
         (write/write-csv! output-periods-csv))))
