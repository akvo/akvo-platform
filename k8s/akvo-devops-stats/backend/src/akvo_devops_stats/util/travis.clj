(ns akvo-devops-stats.util.travis
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [clj-http.client :as http])
  (:import (java.sql Timestamp)
           (java.nio.charset StandardCharsets)
           (java.net URLEncoder)
           (java.time ZonedDateTime)))

(defn parse-date [date-str]
  (when date-str
    (Timestamp/from (.toInstant (ZonedDateTime/parse date-str)))))

(defn initial-url [repo-name]
  (let [slug (URLEncoder/encode (str "akvo/" repo-name) StandardCharsets/UTF_8)]
    (str "/repo/github/" slug "/builds")))

(defn query [travis-partial-url]
  (->
    (http/get
      (str "https://api.travis-ci.org" travis-partial-url)
      {:headers {"Authorization" (str "token " (System/getenv "TRAVIS_CI_TOKEN"))
                 "Travis-API-Version" "3"}})
    (update :body json/parse-string true)))

(defn fetch-all [project]
  (let [pipelines (query (or
                           (:cursor project)
                           (initial-url (:repository project))))
        cursor (get-in pipelines [:body (keyword "@pagination") :next (keyword "@href")])]
    (concat
      (-> pipelines :body :builds)
      (when cursor (lazy-seq
                     (fetch-all
                       (assoc project :cursor cursor)))))))

(defn parse [{:keys [repository tag-prefix]} pipelines]
  (->>
    pipelines
    (filter (fn [pipeline] (str/starts-with? (-> pipeline :branch :name) tag-prefix)))
    (map (fn [pipeline]
           (-> pipeline
             (select-keys [:state :finished_at :commit :branch])
             (update :branch :name)
             (update :commit :sha)
             (assoc :repository repository)
             (clojure.set/rename-keys {:state :build-status
                                       :commit :sha
                                       :finished_at :finish-date
                                       :branch :name})
             (update :finish-date parse-date))))))

(defn fetch-up-to [project promotion-name]
  (->>
    (fetch-all project)
    (parse project)
    (take-while (fn [{:keys [name]}] (not= promotion-name name)))))