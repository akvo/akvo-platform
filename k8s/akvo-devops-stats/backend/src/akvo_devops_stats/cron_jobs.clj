(ns akvo-devops-stats.cron-jobs
  (:require
    [akvo-devops-stats.promotions :as promotions]
    [akvo-devops-stats.projects :as projects]
    [akvo-devops-stats.flips :as flips]
    [akvo-devops-stats.commits :as commits]
    [clojure.java.jdbc :as jdbc]
    [clojure.string :as str]
    [integrant.core :as ig]
    [jsonista.core :as json]
    [clojure.spec.alpha :as s]
    [com.climate.claypoole :as cp]
    [hugsql.core :as hugsql]
    [taoensso.timbre :as timbre]
    [iapetos.core :as prometheus])
  (:import (java.util.concurrent Executors TimeUnit)))

(defmacro log-and-ignore-error [metrics-collector & body]
  `(try
     (let [metrics-labels# {}]
       (prometheus/with-duration (~metrics-collector :job/duration metrics-labels#)
         (prometheus/with-timestamps {:last-run (~metrics-collector :job/last-run metrics-labels#)
                                      :last-success (~metrics-collector :job/last-success metrics-labels#)
                                      :last-failure (~metrics-collector :job/last-failure metrics-labels#)}
           (prometheus/set ~metrics-collector :job/last-start metrics-labels# (System/currentTimeMillis))
           ~@body)))
     (catch Throwable t#
       (timbre/error t#))))

(defn collect-data [{:keys [db]}]
  (let [db (:spec db)]
    (promotions/collect-all-new-promotions db projects/projects)
    (commits/collect-all-new-commits db projects/projects)
    (flips/collect-all-new-flips db projects/projects)))

(defmethod ig/init-key ::start-cron [_ {:keys [db metrics-collector] :as config}]
  (assert db)
  (assert metrics-collector)
  (timbre/info "Starting cron job to collect stats")
  #_(let [cron-thread (Executors/newScheduledThreadPool 1)
        cron-task (.scheduleWithFixedDelay cron-thread
                    (fn []
                      (log-and-ignore-error metrics-collector
                        (collect-data config))) 0 23 TimeUnit/HOURS)]
    (assoc
      config
      :cron-task cron-task
      :cron-thread cron-thread)))


(defmethod ig/halt-key! ::start-cron [_ {:keys [cron-thread cron-task]}]
  (when cron-task
    (.cancel cron-task true))
  (when cron-thread
    (.shutdownNow cron-thread)))