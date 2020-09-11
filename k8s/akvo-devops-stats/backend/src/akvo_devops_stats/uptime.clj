(ns akvo-devops-stats.uptime
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [taoensso.timbre :as log]
            [clj-time.coerce :as tc]
            [hugsql.core :as hugsql]
            [akvo-devops-stats.util.bigquery :as bigquery])
  (:import (java.text SimpleDateFormat)))

(hugsql/def-db-fns "sql/http-codes.sql")

(def first-day (t/local-date 2020 6 1))

(defn k8s-query [from-date to-date]
  (->
    (slurp (io/resource "bigquery/but-flow.sql"))
    (str/replace "$from-date$" from-date)
    (str/replace "$to-date$" to-date)))

(defn flow-query [date]
  (->
    (slurp (io/resource "bigquery/flow.sql"))
    (str/replace "$date$" date)))

(defn parse-row [row]
  (let [day (.getStringValue (.get row "day"))
        day-date (.parse (SimpleDateFormat. "yyyy-MM-dd") day)]
    {:day day
     :year-month (.format (SimpleDateFormat. "yyyy-MM") day-date)
     :year-week (.format (SimpleDateFormat. "yyyy-ww") day-date)
     :http-code (.getLongValue (.get row "status"))
     :times (.getLongValue (.get row "count"))}))

(defn get-k8s-services-stats [from-date to-date]
  (let [query (k8s-query from-date to-date)]
    (log/info "Getting K8S services stats for " from-date "to" to-date)
    (bigquery/query query
      (fn [row]
        (-> (parse-row row)
          (assoc
            :service (.getStringValue (.get row "service"))
            :tenant nil))))))

(defn get-flow-services-stats-for-one-day [date]
  (let [date-str (tf/unparse-local-date (tf/formatter "yyyyMMdd") date)
        query (flow-query date-str)]
    (log/info "Getting Flow stats for " date)
    (bigquery/query query
      (fn [row]
        (-> (parse-row row)
          (assoc
            :service "flow"
            :tenant (.getStringValue (.get row "tenant"))))))))

(defn days-between [first-included last-exclusive]
  (when (t/before? first-included last-exclusive)
    (cons first-included
      (lazy-seq (days-between (t/plus first-included (t/days 1)) last-exclusive)))))

(defn all-flow-stats [start-date end-date]
  (doall (mapcat get-flow-services-stats-for-one-day (days-between start-date end-date))))

(defn collect-all-uptime-stats [db]
  (let [first-day (or
                    (some->
                      (last-day-with-stats db)
                      :day
                      (tc/to-local-date)
                      (t/plus (t/days 1)))
                    first-day)
        k8s-stats (get-k8s-services-stats (str first-day) (str (t/today)))
        flow-stats (all-flow-stats first-day (t/today))]
    (doseq [stat (concat flow-stats k8s-stats)]
      (insert-http-code! db stat))))

(defn daily-stats-last-X-days [db days]
  (let [start-date (->
                     (t/today)
                     (t/minus (t/days days)))]
    (stats-per-day-since db {:day (str start-date)})))

(comment
  (collect-all-uptime-stats (dev/local-db)))