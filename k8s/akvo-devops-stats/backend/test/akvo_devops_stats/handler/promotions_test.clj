(ns akvo-devops-stats.handler.promotions-test
  (:require [clojure.test :refer :all]
            [akvo-devops-stats.promotions :as promotions]))


(deftest parse-github

  (let [example [{:node {:name "Has a tag associated"
                         :target {:message "REGULAR_RELEASE"
                                  :target {:oid "first"}}}}
                 {:node {:name "No tag",
                         :target {:oid "second"}}}]]
    (is (= [{:name "Has a tag associated"
             :reason "REGULAR_RELEASE"
             :repository "any-repo"
             :sha "first"}
            {:name "No tag"
             :reason nil
             :repository "any-repo",
             :sha "second"}]
          (promotions/parse-github "any-repo" example)))))