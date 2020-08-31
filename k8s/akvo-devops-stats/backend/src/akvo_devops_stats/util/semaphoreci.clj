(ns akvo-devops-stats.util.semaphoreci
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as http])
  (:import (java.sql Timestamp)))


(defn initial-url [semaphoreci-project-id]
  (str "https://akvo.semaphoreci.com/api/v1alpha/pipelines?project_id=" semaphoreci-project-id))

(defn query [semaphore-url]
  (->
    (http/get
      semaphore-url
      {:headers {"Authorization" (str "Token " (System/getenv "SEMAPHORE_CI_TOKEN"))}})
    (update :body json/parse-string true)))

(defn fetch-all [project]
  (let [pipelines (query (or
                           (:cursor project)
                           (initial-url (:semaphoreci-project-id project))))
        cursor (->> pipelines
                 :links
                 :next
                 :href)]
    (concat
      (:body pipelines)
      (when cursor (lazy-seq
                     (fetch-all
                       (assoc project :cursor cursor)))))))

(defn parse [{:keys [repository tag-prefix]} pipelines]
  (->>
    pipelines
    (filter (fn [pipeline] (str/starts-with? (:branch_name pipeline) (str "refs/tags/" tag-prefix))))
    (map (fn [pipeline]
           (-> pipeline
             (select-keys [:result :done_at :commit_sha :branch_name])
             (assoc :repository repository)
             (clojure.set/rename-keys {:result :build-status
                                       :commit_sha :sha
                                       :done_at :finish-date
                                       :branch_name :name})
             (update :name (fn [full-git-ref] (.substring full-git-ref (.length "refs/tags/"))))
             (update :finish-date (fn [{:keys [seconds]}] (Timestamp. (* seconds 1000)))))))))

(defn fetch-up-to [project promotion-name]
  (->>
    (fetch-all project)
    (parse project)
    (take-while (fn [{:keys [name]}] (not= promotion-name name)))))