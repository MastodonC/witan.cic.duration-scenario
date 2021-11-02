(ns witan.cic.duration-scenario-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [witan.cic.duration-scenario.core :as scenario]
            [witan.cic.duration-scenario.random :as rand]))

(deftest duration-cap-shortens-matching-period
  (let [single-episode-period {:beginning (t/date-time 2020 1 1)
                               :end (t/date-time 2022 1 1)
                               :duration (* 365 2)
                               :snapshot-date (t/date-time 2020 1 5)
                               :episodes [{:offset 0 :placement :Q1}]}]
    (is (= [(assoc single-episode-period :marked :remove)
            {:beginning (t/date-time 2020 1 1)
             :end (t/date-time 2020 1 21)
             :duration 20
             :snapshot-date (t/date-time 2020 1 5)
             :episodes [{:offset 0, :placement :Q1}]
             :marked :applied}]
           (scenario/apply-stochastic-rule-to-period single-episode-period :Q1 20 1.0 (rand/seed 1))))))

(deftest duration-cap-truncates-episode-sequence
  (let [single-episode-period {:beginning (t/date-time 2020 1 1)
                               :end (t/date-time 2022 1 1)
                               :duration (* 365 2)
                               :snapshot-date (t/date-time 2020 1 5)
                               :episodes [{:offset 0 :placement :X1}
                                          {:offset 1 :placement :Q1}
                                          {:offset 500 :placement :X1}]}]
    (is (= [(assoc single-episode-period :marked :remove)
            {:beginning (t/date-time 2020 1 1)
             :end (t/date-time 2020 1 22)
             :duration 21
             :snapshot-date (t/date-time 2020 1 5)
             :episodes [{:offset 0 :placement :X1}
                        {:offset 1, :placement :Q1}]
             :marked :applied}]
           (scenario/apply-stochastic-rule-to-period single-episode-period :Q1 20 1.0 (rand/seed 1))))))

(deftest duration-cap-ignores-periods-where-cap-is-prior-to-snapshot-date
  (let [single-episode-period {:beginning (t/date-time 2020 1 1)
                               :end (t/date-time 2022 1 1)
                               :duration (* 365 2)
                               :snapshot-date (t/date-time 2020 1 31)
                               :episodes [{:offset 0 :placement :Q1}]}]
    (is (= [{:beginning (t/date-time 2020 1 1)
             :end (t/date-time 2022 1 1)
             :duration (* 365 2)
             :snapshot-date (t/date-time 2020 1 31)
             :episodes [{:offset 0, :placement :Q1}]
             :marked :not-applicable}]
           (scenario/apply-stochastic-rule-to-period  single-episode-period :Q1 20 1.0 (rand/seed 1))))))
