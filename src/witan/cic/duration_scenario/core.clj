(ns witan.cic.duration-scenario.core
  (:require [witan.cic.duration-scenario.io.read :as read]))

(defn duration-scenario
  [{:keys [input-periods scenario-parameters] :as config}]
  (let [periods (read/periods input-periods)]
    (println (first periods))))
