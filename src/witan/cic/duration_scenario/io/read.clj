(ns witan.cic.duration-scenario.io.read
  (:require [camel-snake-kebab.core :as csk]
            [clojure.edn :as edn]
            [clojure.data.csv :as data-csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.coerce :as c]
            [clj-time.core])
  (:import [java.io PushbackReader]))

(defn blank-row? [row]
  (every? str/blank? row))

(defn parse-long
  [s]
  (Long/parseLong s))

(defn parse-double
  [s]
  (Double/parseDouble s))

(defn load-csv
  "Loads csv file with each row as a vector.
   Stored in map separating column-names from data"
  [filename]
  (with-open [in-file (io/reader filename)]
    (let [parsed-csv (->> in-file
                          data-csv/read-csv
                          (remove blank-row?))
          parsed-data (rest parsed-csv)
          headers (map csk/->kebab-case-keyword (first parsed-csv))]
      (into [] (map (partial zipmap headers)) parsed-data))))

(defn episodes
  [episodes-file]
  (load-csv episodes-file))

(defn periods
  [periods-file]
  (with-open [in-file (io/reader periods-file)]
    (edn/read {:readers c/data-readers}
              (PushbackReader. in-file))))

(defn scenario-parameters
  [parameters-file]
  (->> (load-csv parameters-file)
       (map (fn [row]
              (-> row
                  (update :placement symbol)
                  (update :duration-cap-days parse-long)
                  (update :probability-cap-applies parse-double))))))

