(ns witan.cic.duration-scenario.main
  (:gen-class)
  (:require [aero.core :as aero]
            [clojure.tools.cli :as cli]
            [witan.cic.duration-scenario.core :as core]
            [witan.cic.duration-scenario.time :as time]
            [witan.cic.duration-scenario.io.read :as read]
            [witan.cic.duration-scenario.io.write :as write]))

(defn read-config
  [config-file]
  (binding [*data-readers* {'date time/string-as-date}]
    (aero/read-config config-file)))

(def cli-options
  [
   ["-c" "--config FILE" "Config file"
    :default "data/demo/config.edn"
    :id :config-file]])

(defn run-duration-scenario!
  [{:keys [input-periods output-periods output-periods-csv input-parameters project-to random-seed] :as config}]
  (let [period-simulations (read/periods input-periods)
        parameters (read/scenario-parameters input-parameters)
        scenario-periods (core/apply-duration-scenario-rules period-simulations parameters random-seed)]
    (write/write-nippy! output-periods scenario-periods)
    (->> scenario-periods
         (into [] (map (partial into [] (remove (comp #{:remove} :marked)))))
         (write/episodes-table project-to)
         (write/write-csv! output-periods-csv))))

(defn -main
  [& args]
  (let [{:keys [options arguments]} (cli/parse-opts args cli-options)
        {:keys [config-file]} options
        [task] arguments]
    (case task
      "duration-scenario" (run-duration-scenario! (read-config config-file))
      :else nil)))

