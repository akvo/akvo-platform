(ns akvo-devops-stats.uptime
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [hugsql.core :as hugsql]
            [akvo-devops-stats.util.bigquery :as bigquery])
  (:import (java.text SimpleDateFormat)))

(hugsql/def-db-fns "sql/http-codes.sql")

(def first-day "2020-06-01")

(defn k8s-query [from-date to-date]
  (->
    (slurp (io/resource "bigquery/but-flow.sql"))
    (str/replace "$from-date$" from-date)
    (str/replace "$to-date$" to-date)))

(defn get-k8s-services-stats [from-date to-date]
  (let [query (k8s-query from-date to-date)]
    (bigquery/query query
      (fn [row]
        (let [day (.getStringValue (.get row "day"))
              day-date (.parse (SimpleDateFormat. "yyyy-MM-dd") day)]
          {:service (.getStringValue (.get row "service"))
           :day day
           :tenant nil
           :year-month (.format (SimpleDateFormat. "yyyy-MM") day-date)
           :year-week (.format (SimpleDateFormat. "yyyy-ww") day-date)
           :http-code (.getLongValue (.get row "status"))
           :times (.getLongValue (.get row "count"))})))))

(defn collect-all-uptime-stats [db]
  (let [first-day (or
                    (some->
                      (last-day-with-stats db)
                      :day
                      (tc/to-local-date)
                      (t/plus (t/days 1)))
                    first-day)]
    (doseq [stat (get-k8s-services-stats (str first-day) (str (t/today)))]
      (insert-http-code! db stat))))

(comment
  (collect-all-uptime-stats (dev/local-db)))