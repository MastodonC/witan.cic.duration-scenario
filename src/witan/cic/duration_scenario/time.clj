(ns witan.cic.duration-scenario.time
  (:require [clj-time.format :as f]))

(def date-string (f/formatter "yyyy-MM-dd"))

(defn date-as-string
  [date]
  (f/unparse date-string date))

(defn string-as-date
  [s]
  (f/parse date-string s))

