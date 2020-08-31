(ns akvo-devops-stats.commits
  (:require
    [clj-http.client :as http]
    [venia.core :as v]
    [cheshire.core :as json]
    clojure.set
    [clojure.string :as str]
    [hugsql.core :as hugsql])
  (:import (java.time ZonedDateTime)
           (java.sql Timestamp)
           (java.text SimpleDateFormat)))

(hugsql/def-db-fns "sql/commits.sql")

(defn query [venia-query]
  (:body (http/post "https://api.github.com/graphql"
           {:headers {"Authorization" (str "token " (System/getenv "GITHUB_TOKEN"))}
            :body (json/generate-string {:query (str "query " (v/graphql-query venia-query))})
            :as :json})))

(defn fetch-commits [{:keys [repository trunk-branch cursor]}]
  (->
    (query
      {:venia/queries [[:repository {:owner "akvo" :name repository}
                        [[:ref {:qualifiedName trunk-branch}
                          [[:target :Commit]]]]]]
       :venia/fragments [{:fragment/name "Commit"
                          :fragment/type :Commit
                          :fragment/fields [[:history {:first 30 :after cursor}
                                             [[:edges
                                               [:cursor
                                                [:node
                                                 [:oid
                                                  :messageHeadline
                                                  :authoredDate
                                                  [:associatedPullRequests {:first 1}
                                                   [[:nodes
                                                     [[:commits {:first 1}
                                                       [[:nodes
                                                         [[:commit [:oid :authoredDate]]]]]]]]]]]]]]]]]}]})
    :data :repository :ref :target :history :edges))

(defn fetch-all-commits [project]
  (let [commits (fetch-commits project)
        cursor (->> commits
                 (map :cursor)
                 last)]
    (concat commits (when cursor (lazy-seq (fetch-all-commits (assoc project :cursor cursor)))))))

(defn parse-date [date-str]
  (when date-str
    (Timestamp/from (.toInstant (ZonedDateTime/parse date-str)))))

(defn parse-github [repository response]
  (->>
    response
    (map :node)
    (map (fn [{:keys [associatedPullRequests] :as commit}]
           (let [first-pr (-> associatedPullRequests :nodes first :commits :nodes first :commit)]
             (-> commit
               (assoc :repository repository)
               (dissoc :associatedPullRequests)
               (clojure.set/rename-keys {:oid :sha
                                         :messageHeadline :message-headline
                                         :authoredDate :authored-date})
               (assoc :first-pr-sha (:oid first-pr))
               (assoc :first-pr-authored-date (:authoredDate first-pr))
               (update :authored-date parse-date)
               (update :first-pr-authored-date parse-date)))))))

(defn fetch-all-commits-up-to [project commit-sha]
  (->>
    (fetch-all-commits project)
    (parse-github (:repository project))
    (take-while (fn [{:keys [sha]}] (not= commit-sha sha)))))

;; Assuming commits are in order, with the newest first.
(defn save-commits [db commits]
  (doseq [commit (reverse commits)]
    (insert-commit! db commit)))

(defn deploy-time [flip commits]
  (->>
    commits
    (map (fn [commit] (assoc commit :release-date (:finish-date flip))))
    (map (fn [{:keys [authored-date first-pr-authored-date] :as commit}]
           (assoc commit :authored-date (or first-pr-authored-date authored-date))))
    (map (fn [commit] (select-keys commit [:release-date :authored-date :repository])))
    distinct))

(defn split-at-flip [flip commits]
  (split-with (fn [commits] (not= (:sha commits) (:sha flip))) commits))

(defn find-deploy-times* [[flip previous-flip & flips] commits]
  (when flip
    (let [[commits-for-current-flip other-commits] (split-at-flip previous-flip commits)]
      (concat
        (deploy-time flip commits-for-current-flip)
        (find-deploy-times* (cons previous-flip flips) other-commits)))))

(defn find-deploy-times [flips commits]
  (let [flips flips
        promotions commits]
    (find-deploy-times* flips (second (split-at-flip (first flips) promotions)))))

(defn collect-all-new-commits [db projects]
  (doseq [project projects]
    (let [commits (fetch-all-commits-up-to project
                    (or
                      (:sha (last-commit-for-repo db project))
                      (:initial-commit-exclusive project)))]
      (save-commits db commits))))

(comment

  (require '[clojure.contrib.humanize])

  (let [all (->>
              akvo-devops-stats.flips/projects
              (mapcat (fn [{:keys [release-fn] :as project}]
                        (let [commits (get-commits-for-repo (dev/local-db) project)
                              releases (release-fn (dev/local-db) project)]
                          (->>
                            (find-deploy-times releases commits)
                            (map (fn [{:keys [release-date authored-date] :as commit}]
                                   (-> commit
                                     ;(select-keys [:repository :release-date :authored-date])
                                     (assoc :team (get akvo-devops-stats.flips/project->team (:repository commit)))
                                     (assoc :lead-time-minutes (int (/ (- (.getTime release-date) (.getTime authored-date)) 1000 60)))
                                     (assoc :year-week (.format (SimpleDateFormat. "yyyy-ww") release-date))
                                     (assoc :year-month (.format (SimpleDateFormat. "yyyy-MM") release-date))))))))))]
    (->>
      all
      ;(map (fn [x] (assoc x :lt (clojure.contrib.humanize/duration (:lead-time x)))))
      ;(filter (fn [x] (> (:lead-time x) (* 1000 60 60 24 30))))
      ;(clojure.pprint/print-table)
      (map (fn [x]
             (assert (pos? (:lead-time-minutes x)))
             (when (> (:lead-time-minutes x) (* 60 24 30))
               (println (clojure.contrib.humanize/duration (* (:lead-time-minutes x) 60 1000)))
               (println x))
             (str/join "," (vals x))))
      (cons (str/join "," (map name (keys (first all)))))
      (str/join "\n")
      (spit "commits.csv")
      ))

  )