(ns akvo-devops-stats.util.monitoring
  (:require [iapetos.core :as prometheus]
            [iapetos.collector.jvm :as jvm]
            [iapetos.collector.ring :as ring]
            [integrant.core :as ig]
            [iapetos.collector.exceptions :as ex]
            [iapetos.registry :as registry]
            [hugsql-adapter-case.adapters :as adapter-case]
            akvo-devops-stats.util.db
            [hugsql.core :as hugsql]
            [hugsql.adapter :as adapter]
            [iapetos.core :as prometheus]
            [iapetos.collector.exceptions :as ex]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.sentry :as sentry])
  (:import (com.zaxxer.hikari.metrics.prometheus PrometheusMetricsTrackerFactory)))

(defn wrap-health-check
  [handler]
  (fn [{:keys [request-method uri] :as request}]
    (if (and (= uri "/healthz") (= request-method :get))
      {:status 200}
      (handler request))))

(defmethod ig/init-key ::collector [_ config]
  (->
    (prometheus/collector-registry)
    (jvm/initialize)
    (prometheus/register
      (prometheus/gauge :job/last-run {:description "Timestamp of last job run"
                                         :labels [:db-name]})
      (prometheus/gauge :job/last-success {:description "Timestamp of last job  run without exceptions"
                                             :labels [:db-name]})
      (prometheus/gauge :job/last-failure {:description "Timestamp of last job run and fail"
                                             :labels [:db-name]})
      (prometheus/gauge :job/last-start {:description "Timestamp of last job run start time"
                                           :labels [:db-name]})
      (prometheus/histogram
        :job/duration
        {:description "Time taken to run a job"
         :labels [:db-name]
         :buckets [1, 3, 5, 10, 60, 300, (* 30 60), (* 60 60)]})
      (prometheus/histogram
        :sql/run-duration
        {:description "SQL query duration"
         :labels [:query]})
      (prometheus/counter
        :sql/run-total
        {:description "the total number of finished runs of the observed sql query."
         :labels [:query :result]})
      (ex/exception-counter
        :sql/exceptions-total
        {:description "the total number and type of exceptions for the observed sql query."
         :labels [:query]}))
    (ring/initialize)))

(defmethod ig/init-key ::middleware [_ {:keys [collector]}]
  #(-> %
     wrap-health-check
     (ring/wrap-metrics collector)))

(defmacro metrics
  [metrics-collector options & body]
  `(if ~metrics-collector
     (let [labels# {:query (:fn-name ~options), :result "success"}
           failure-labels# {:query (:fn-name ~options), :result "failure"}]
       (prometheus/with-success-counter (~metrics-collector :sql/run-total labels#)
         (prometheus/with-failure-counter (~metrics-collector :sql/run-total failure-labels#)
           (ex/with-exceptions (~metrics-collector :sql/exceptions-total labels#)
             (prometheus/with-duration (~metrics-collector :sql/run-duration labels#)
               ~@body)))))
     (do ~@body)))

(deftype MetricsAdapter [metrics-collector jdbc-adapter]

  adapter/HugsqlAdapter
  (execute [_ db sqlvec options]
    (metrics metrics-collector options
      (adapter/execute jdbc-adapter db sqlvec options)))

  (query [_ db sqlvec options]
    (metrics metrics-collector options
      (adapter/query jdbc-adapter db sqlvec options)))

  (result-one [_ result options]
    (adapter/result-one jdbc-adapter result options))

  (result-many [_ result options]
    (adapter/result-many jdbc-adapter result options))

  (result-affected [_ result options]
    (adapter/result-affected jdbc-adapter result options))

  (result-raw [_ result options]
    (adapter/result-raw jdbc-adapter result options))

  (on-exception [_ exception]
    (adapter/on-exception jdbc-adapter exception)))

(defmethod ig/init-key ::hikaricp
  [_ {:keys [hikari-cp metrics-collector skip-metrics-registration] :as options}]
  ;; Workaround HikariCP bug https://github.com/brettwooldridge/HikariCP/pull/1331
  ;; This just happens when having multiple HikariCP pools in the same JVM, which right now
  ;; just happens when running it in dev mode.
  ;; Bug should be fixed in the next HikariCP release (3.3.2 or higher) and this workaround can be
  ;; removed.
  (when-not skip-metrics-registration
    (-> hikari-cp
      :spec
      :datasource
      (.setMetricsTrackerFactory
        (PrometheusMetricsTrackerFactory. (registry/raw metrics-collector)))))

  (hugsql/set-adapter!
    (MetricsAdapter.
      metrics-collector
      (adapter-case/kebab-adapter)))

  hikari-cp)


(defmethod ig/init-key ::sentry-logger [_ {:keys [dsn version host env]}]
  (assert dsn)
  (timbre/handle-uncaught-jvm-exceptions!)
  (-> (sentry/sentry-appender dsn
        {:environment env
         :release version
         :event-fn (fn [event]
                     (assoc event :server_name host))})
    (assoc :min-level :error)))

(comment
  (println
    (slurp "http://authz:3000/metrics")))