(ns witan.cic.duration-scenario.main
  (:gen-class)
  (:require [aero.core :as aero]
            [clojure.tools.cli :as cli]
            [witan.cic.duration-scenario.core :as core]
            [witan.cic.duration-scenario.time :as time]))

(defn read-config
  [config-file]
  (binding [*data-readers* {'date time/string-as-date}]
    (aero/read-config config-file)))

(def cli-options
  [
   ["-c" "--config FILE" "Config file"
    :default "data/demo/config.edn"
    :id :config-file]])

(defn -main
  [& args]
  (let [{:keys [options arguments]} (cli/parse-opts args cli-options)
        {:keys [config-file]} options
        [task] arguments]
    (case task
      "duration-scenario" (core/duration-scenario! (read-config config-file))
      :else nil)))

