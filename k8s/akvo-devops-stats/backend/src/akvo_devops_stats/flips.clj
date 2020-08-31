(ns akvo-devops-stats.flips
  (:require
    clojure.set
    [clojure.string :as str]
    [akvo-devops-stats.util.semaphoreci :as semaphoreci]
    [akvo-devops-stats.util.kubernetes :as k8s]
    [hugsql.core :as hugsql]
    [akvo-devops-stats.promotions :as promotions])
  (:import (java.text SimpleDateFormat)))

(hugsql/def-db-fns "sql/flips.sql")

(defn upsert-flips [db flips]
  (doseq [flip flips]
    (upsert-flip! db flip)))

(defn split-promotions-at [flip promotions]
  (split-with (fn [promotion] (not= (:sha promotion) (:sha flip))) promotions))

(defn find-reasons* [[flip previous-flip & flips] promotions]
  (when flip
    (let [[promotions-for-current-flip other-promotions]
          (split-promotions-at previous-flip promotions)
          reason (if (some #{"FIX_RELEASE"} (map :reason promotions-for-current-flip))
                   "FIX_RELEASE"
                   (:reason (first promotions-for-current-flip)))]
      (cons
        (assoc flip :reason reason)
        (find-reasons* (cons previous-flip flips) other-promotions)))))

(defn find-reasons [flips promotions]
  (let [flips (reverse (sort-by :name flips))
        promotions (reverse (sort-by :name promotions))]
    (find-reasons* flips (second (split-promotions-at (first flips) promotions)))))

(defn get-releases [db project]
  (find-reasons
    (get-flips-for-repo db project)
    (promotions/get-promotions db project)))

(defn collect-Lumen-flips [db]
  (upsert-flips db (k8s/get-lumen-flips)))

(defn collect-Flow-flips [db projects]
  (when-let [flow (first (->> projects (filter (comp (partial = "akvo-flow") :repository))))]
    (let [initial-flip (or
                         (:name (get-last-flip-for-repo db flow))
                         (:initial-flip-exclusive flow))]
      (upsert-flips db
        (semaphoreci/fetch-up-to (assoc flow :tag-prefix "flip") initial-flip)))))

(defn collect-all-new-flips [db projects]
  ;; flow flips
  (collect-Flow-flips db projects)
  (collect-Lumen-flips db))

(comment

  ;; generate all releases
  (let [all (->>
              projects
              (mapcat (fn [{:keys [release-fn] :as project}]
                        (->>
                          (sort-by :name (release-fn (dev/local-db) project))
                          (map (fn [x]
                                 (-> x
                                   (select-keys [:repository :name :reason :sha :finish-date])
                                   (assoc :team (get project->team (:repository x)))
                                   (update :reason (fn [reason] (let [reason (and reason (str/trim reason))]
                                                                  (get #{"FIX_RELEASE" "REGULAR_RELEASE"} reason "UNKNOWN"))))
                                   (assoc :year-week (.format (SimpleDateFormat. "yyyy-ww") (:finish-date x)))
                                   (assoc :year-month (.format (SimpleDateFormat. "yyyy-MM") (:finish-date x))))))))))]
    (->>
      all
      (map (fn [x] (str/join "," (vals x))))
      (cons (str/join "," (map name (keys (first all)))))
      (str/join "\n")
      (spit "rsr-releases.csv")
      ))
  )