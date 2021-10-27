(ns witan.cic.duration-scenario.io.write
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as data-csv]
            [taoensso.nippy :as nippy]
            [witan.cic.duration-scenario.time :as time]))

(defn period->episodes
  [{:keys [period-id simulation-id beginning dob birthday admission-age episodes end provenance
           match-offset matched-id matched-offset] :as period}]
  (let [placement-sequence (transduce (comp (map (comp name :placement))
                                            (interpose "-"))
                                      str
                                      episodes)
        placement-pathway (transduce (comp (map (comp name :placement))
                                           (dedupe)
                                           (interpose "-"))
                                     str
                                     episodes)
        period-duration (time/day-interval beginning end)]
    (into []
          (comp
           (map-indexed (fn [idx [{:keys [placement offset]} to]]
                          (hash-map :period-id period-id
                                    :simulation-id simulation-id
                                    :episode-number (inc idx)
                                    :dob dob
                                    :admission-age admission-age
                                    :birthday birthday
                                    :start (time/days-after beginning offset)
                                    :end (or (some->> to :offset dec (time/days-after beginning)) end)
                                    :placement placement
                                    :offset offset
                                    :provenance provenance
                                    :placement-sequence placement-sequence
                                    :placement-pathway placement-pathway
                                    :period-start beginning
                                    :period-duration period-duration
                                    :period-end end
                                    :period-offset 0
                                    :match-offset match-offset
                                    :matched-id matched-id
                                    :matched-offset matched-offset))))
          (partition-all 2 1 episodes))))

(defn episodes->table-rows-xf
  [project-to]
  (map (fn [{:keys [period-id simulation-id dob birthday admission-age
                    episode-number start end placement offset
                    provenance placement-sequence placement-pathway
                    period-start period-duration period-end period-offset
                    match-offset matched-id matched-offset] :as episode}]
         (vector simulation-id period-id
                 episode-number dob admission-age
                 (time/date-as-string birthday)
                 (time/date-as-string start)
                 (when end (time/date-as-string end)) ;; TODO - why would a period have no end date?
                 (name placement)
                 offset
                 provenance
                 placement-sequence
                 placement-pathway
                 (time/date-as-string period-start)
                 period-duration
                 (time/date-as-string period-end)
                 period-offset
                 match-offset
                 matched-id
                 matched-offset))))

(defn episodes-table
  [project-to projections]
  (let [headers ["Simulation" "ID" "Episode" "Birth Year" "Admission Age" "Birthday" "Start" "End" "Placement" "Offset" "Provenance"
                 "Placement Sequence" "Placement Pathway" "Period Start" "Period Duration" "Period End" "Period Offset"
                 "Match Offset" "Matched ID" "Matched Offset"]]
    (into [headers]
          (comp cat
                (mapcat period->episodes)
                (episodes->table-rows-xf project-to))
          projections)))

(defn write-csv!
  [out-file tablular-data]
  (with-open [writer (io/writer out-file)]
    (data-csv/write-csv writer tablular-data)))

(defn write-edn!
  [out-file data]
  (with-open [writer (io/writer out-file)]
    (binding [*out* writer]
      (pr data))))

(defn write-nippy!
  [out-file data]
  (with-open [writer (io/output-stream out-file)]
    (.write writer (nippy/freeze data))))
