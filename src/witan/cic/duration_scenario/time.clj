(ns witan.cic.duration-scenario.time
  (:refer-clojure :exclude [< <= = > >=])
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def date-string (f/formatter "yyyy-MM-dd"))

(def < t/before?)

(def > t/after?)

(def = t/equal?)

(def earliest t/earliest)

(def latest t/latest)

(defn <=
  [a b]
  (or (< a b)
      (= a b)))

(defn >=
  [a b]
  (or (> a b)
      (= a b)))

(defn between?
  [x a b]
  (and (>= x a)
       (< x b)))

(defn date-as-string
  [date]
  (f/unparse date-string date))

(defn string-as-date
  [s]
  (f/parse date-string s))

(defn days-after
  [date days]
  (t/plus date (t/days days)))

(defn day-interval
  [from to]
  (t/in-days (t/interval from to)))

