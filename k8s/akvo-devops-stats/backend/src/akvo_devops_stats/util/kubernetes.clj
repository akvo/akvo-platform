(ns akvo-devops-stats.util.kubernetes
  (:require [clojure.string :as str])
  (:import (io.kubernetes.client.util KubeConfig ClientBuilder)
           (com.google.auth.oauth2 GoogleCredentials)
           (io.kubernetes.client.openapi.apis CoreV1Api)
           (java.text SimpleDateFormat)
           (java.sql Timestamp)))

;; need to have a fresh access token.
;; workaround: run "kubectl get pods"
;; see https://github.com/kubernetes-client/java/issues/290
;(GoogleCredentials/getApplicationDefault)
;; when running externally make sure that kubectx is production
(defn get-lumen-flips []
  (let [client (.build (ClientBuilder/kubeconfig
                         (KubeConfig/loadKubeConfig (clojure.java.io/reader "/root/.kube/config"))))
        api (CoreV1Api. client)
        config-maps (.listNamespacedConfigMap api "default" "false" false nil "" "" (int 10) nil (int 100) false)]
    (->>
      (.getItems config-maps)
      (filter
        (fn [configmap]
          (str/starts-with? (-> configmap .getMetadata .getName) "akvo-lumen-flip")))
      (map (fn [x]
             (let [data (.getData x)
                   finish-date (get data "when")]
               {:sha (get data "new-live-version")
                :name finish-date
                :repository "akvo-lumen"
                :finish-date2 finish-date
                :finish-date (Timestamp. (.getTime (.parse (SimpleDateFormat. "yyyyMMdd-HHmmss") finish-date)))}))))))
