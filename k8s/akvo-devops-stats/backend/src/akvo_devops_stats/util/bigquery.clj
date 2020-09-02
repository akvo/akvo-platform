(ns akvo-devops-stats.util.bigquery
  (:import (com.google.cloud.bigquery BigQueryOptions QueryJobConfiguration JobId JobInfo BigQuery BigQuery$JobOption Job BigQuery$QueryResultsOption TableResult)
           (java.util UUID)
           (com.google.cloud RetryOption)))

(defn opts [type]
  (into-array type []))

(defn query [query f]
  (let [^BigQuery s (.getService (BigQueryOptions/getDefaultInstance))

        query-config (->
                       query
                       (QueryJobConfiguration/newBuilder)
                       (.setUseLegacySql false)
                       .build)

        job-id (JobId/of (str (UUID/randomUUID)))

        ^Job job-query (.create s
                         (->
                           (JobInfo/newBuilder query-config)
                           (.setJobId job-id)
                           .build)
                         (opts BigQuery$JobOption))

        job-query (.waitFor job-query (opts RetryOption))]

    (if-let [error (or
                     (nil? job-query)
                     (-> job-query .getStatus .getError))]
      (throw (ex-info "Job execution failed" {:error error}))
      (let [result (.getQueryResults job-query (opts BigQuery$QueryResultsOption))]
        (mapv f (.iterateAll result))))))
