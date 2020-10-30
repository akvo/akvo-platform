(ns akvo-devops-stats.promotions
  (:require
    [clj-http.client :as http]
    [venia.core :as v]
    [cheshire.core :as json]
    clojure.set
    [clojure.string :as str]
    [akvo-devops-stats.util.semaphoreci :as semaphoreci]
    [akvo-devops-stats.util.travis :as travis]
    [hugsql.core :as hugsql]))

(hugsql/def-db-fns "sql/promotions.sql")

(defn query-github [venia-query]
  (:body (http/post "https://api.github.com/graphql"
           {:headers {"Authorization" (str "token " (System/getenv "GITHUB_TOKEN"))}
            :body (json/generate-string {:query (str "query " (v/graphql-query venia-query))})
            :as :json})))

(defn fetch-promotions [{:keys [repository tag-prefix cursor]}]
  (->
    (query-github
      {:venia/queries [[:repository {:owner "akvo" :name repository}
                        [[:refs {:refPrefix "refs/tags/" :query tag-prefix :first 4 :orderBy {:field :TAG_COMMIT_DATE :direction :DESC} :after cursor}
                          [[:edges
                            [:cursor
                             [:node
                              [:name
                               [:target :Commit]
                               [:target :Tag]]]]]]]]]]
       :venia/fragments [{:fragment/name "Tag"
                          :fragment/type :Tag
                          :fragment/fields [:message
                                            [:target :Commit]]}
                         {:fragment/name "Commit"
                          :fragment/type :Commit
                          :fragment/fields [:oid
                                            :messageHeadline
                                            :authoredDate]}]})
    :data :repository :refs :edges))

(defn fetch-all-promotions [project]
  (let [promotions (fetch-promotions project)
        cursor (->> promotions
                 (map :cursor)
                 last)]
    (concat promotions (when cursor (lazy-seq (fetch-all-promotions (assoc project :cursor cursor)))))))

(defn parse-github [repository response]
  (->>
    response
    (map :node)
    (map (fn [{:keys [name target]}] (assoc target :name name)))
    (map (fn [{:keys [name message target oid]}]
           {:name name
            :reason message
            :repository repository
            :sha (or oid (:oid target))}))
    (remove (fn [{:keys [name sha]}]
              (or
                (nil? name)
                (nil? sha))))))

(defn fetch-all-promotions-up-to [project promotion-name]
  (->>
    (fetch-all-promotions project)
    (parse-github (:repository project))
    (take-while (fn [{:keys [name]}] (not= promotion-name name)))))

(defn save-promotions [db promotions]
  (doseq [promotion promotions]
    (insert-promotion! db promotion)))

(defn update-promotions-with-build-status [db promotions]
  (doseq [promotion promotions]
    (update-promotion-with-build-status! db promotion)))

(defn get-promotions [db project]
  (->>
    (get-promotions-for-repository db project)
    (map (fn [promotion]
           (update promotion :reason (fn [reason] (and reason (str/trim reason))))))))

(defn collect-all-new-promotions [db projects]
  (doseq [project projects]
    (let [initial-promotion-to-fetch (or
                                       (:name (get-last-promotion-for-repository db project))
                                       (:initial-promotion-exclusive project))]
      (save-promotions db (doall (fetch-all-promotions-up-to project initial-promotion-to-fetch))))
    (let [initial-promotion-to-fetch (or
                                       (:name (get-last-promotion-with-build-status-for-repository db project))
                                       (:initial-promotion-exclusive project))]
      (let [build-system (if (:semaphoreci-project-id project) semaphoreci/fetch-up-to travis/fetch-up-to)]
        (update-promotions-with-build-status db (doall (build-system project initial-promotion-to-fetch)))))))