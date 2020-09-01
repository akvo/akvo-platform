(ns akvo-devops-stats.handler.handler
  (:require [compojure.core :refer :all]
            [integrant.core :as ig]
            [clojure.string :as str]
            [akvo-devops-stats.projects :as projects]
            [akvo-devops-stats.commits :as commits])
  (:import (java.text SimpleDateFormat)))

(defn prepare-commits [db]
  (->>
    projects/projects
    (mapcat (fn [{:keys [release-fn] :as project}]
              (let [commits (commits/get-commits-for-repo db project)
                    releases (release-fn db project)]
                (->>
                  (commits/find-deploy-times releases commits)
                  (map (fn [{:keys [release-date authored-date] :as commit}]
                         (-> commit
                           (select-keys [:repository :release-date :authored-date])
                           (assoc :team (get projects/project->team (:repository commit)))
                           (assoc :lead-time-minutes (int (/ (- (.getTime release-date) (.getTime authored-date)) 1000 60)))
                           (assoc :year-week (.format (SimpleDateFormat. "yyyy-ww") release-date))
                           (assoc :year-month (.format (SimpleDateFormat. "yyyy-MM") release-date)))))))))))

(defn prepare-releases [db]
  (->>
    projects/projects
    (mapcat (fn [{:keys [release-fn] :as project}]
              (->>
                (sort-by :name (release-fn db project))
                (map (fn [x]
                       (-> x
                         (select-keys [:repository :name :reason :sha :finish-date])
                         (assoc :team (get projects/project->team (:repository x)))
                         (update :reason (fn [reason] (let [reason (and reason (str/trim reason))]
                                                        (get #{"FIX_RELEASE" "REGULAR_RELEASE"} reason "UNKNOWN"))))
                         (assoc :year-week (.format (SimpleDateFormat. "yyyy-ww") (:finish-date x)))
                         (assoc :year-month (.format (SimpleDateFormat. "yyyy-MM") (:finish-date x)))))))))))

(defmethod ig/init-key :akvo-devops-stats.handler/handler [_ {:keys [db]}]
  (context "/devopsstats" []
    (GET "/commits" []
      {:body (let [db (:spec db)
                   all (prepare-commits db)]
               (->>
                 all
                 (map (fn [x] (str/join "," (vals x))))
                 (cons (str/join "," (map name (keys (first all)))))
                 (str/join "\n")))})
    (GET "/releases" []
      {:body (let [db (:spec db)
                   all (prepare-releases db)]
               (->>
                 all
                 (map (fn [x] (str/join "," (vals x))))
                 (cons (str/join "," (map name (keys (first all)))))
                 (str/join "\n")))})))

(comment
  (clj-http.client/get "http://localhost:3000/devopsstats/releases")
  (clj-http.client/get "http://localhost:3000/devopsstats/commits")
  )