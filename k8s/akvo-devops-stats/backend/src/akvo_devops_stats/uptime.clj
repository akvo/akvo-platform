(ns akvo-devops-stats.uptime
  (:require [clojure.java.io :as io])
  (:import (com.google.cloud.bigquery BigQueryOptions QueryJobConfiguration JobId JobInfo BigQuery BigQuery$JobOption Job BigQuery$QueryResultsOption TableResult)
           (java.util UUID)
           (com.google.cloud RetryOption)))

(defn opts [type]
  (into-array type []))

(comment
  (def ^BigQuery s (.getService (BigQueryOptions/getDefaultInstance)))

  (def query-config (-> (QueryJobConfiguration/newBuilder (slurp (io/resource "bigquery/but-flow.sql")))
                      (.setUseLegacySql false)
                      .build))

  (def job-id (JobId/of (str (UUID/randomUUID))))

  (def ^Job job-query (.create s
                        (->
                          (JobInfo/newBuilder query-config)
                          (.setJobId job-id)
                          .build)
                        (opts BigQuery$JobOption)))

  (.waitFor job-query (opts RetryOption))

  (def ^TableResult result (.getQueryResults job-query (opts BigQuery$QueryResultsOption)))

  (for [x (.iterateAll result)]
    [(.getStringValue (.get x "service"))
     (.getStringValue (.get x "day"))
     (.getLongValue (.get x "status"))
     (.getLongValue (.get x "count"))
     ]))