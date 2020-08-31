(ns akvo-devops-stats.handler.commits-test
  (:require [clojure.test :refer :all]
            [akvo-devops-stats.commits :as commits]))

(deftest calculate-lead-time
  (let [release-date #inst"2020-08-05"]
    (is (= [{:repository "akvo-flow",
             :authored-date #inst"2020-08-06"
             :release-date release-date}]
          (commits/deploy-time
            {:repository "akvo-flow",
             :sha "77cb97915574009db2c9be27a708e321f443ca67"
             :finish-date release-date}
            [{:repository "akvo-flow"
              :sha "77cb97915574009db2c9be27a708e321f443ca67"
              :authored-date #inst"2020-08-06"}]))))

  (testing "Merge Squash"
    (testing "Commit gets date of first commit in PR."
      (let [release-date #inst"2020-08-05"
            pr-date #inst"2020-08-11"]
        (is (= [{:repository "akvo-flow",
                 :authored-date pr-date
                 :release-date release-date}]
              (commits/deploy-time
                {:repository "akvo-flow",
                 :sha "77cb97915574009db2c9be27a708e321f443ca67",
                 :finish-date release-date}
                [{:repository "akvo-flow",
                  :sha "77cb97915574009db2c9be27a708e321f443ca67",
                  :authored-date #inst"2020-08-06"
                  :first-pr-sha "c0e90d34e1fc218cda7a320bf411f4a833f5cd95",
                  :first-pr-authored-date pr-date
                  :index 300}])))))
    (testing "All commits in PR get condensed in one"
      (let [release-date #inst"2020-08-05"
            pr-with-one-commits #inst"2020-08-01"
            pr-with-TWO-commits #inst"2020-08-11"
            pr-sha "77cb97915574009db2c9be27a708e321f443ca67"]
        (is (= [{:authored-date pr-with-TWO-commits
                 :release-date release-date}
                {:authored-date pr-with-one-commits
                 :release-date release-date}]
              (commits/deploy-time
                {:sha pr-sha
                 :finish-date release-date}
                [{:sha "a"
                  :authored-date #inst"2020-08-03"
                  :first-pr-sha pr-sha
                  :first-pr-authored-date pr-with-TWO-commits
                  :index 302}
                 {:sha "b"
                  :authored-date #inst"2020-08-02"
                  :first-pr-sha "other sha"
                  :first-pr-authored-date pr-with-one-commits
                  :index 301}
                 {:sha "c"
                  :authored-date #inst"2020-08-01"
                  :first-pr-sha pr-sha
                  :first-pr-authored-date pr-with-TWO-commits
                  :index 300}])))))))


(let [pr-with-one-commits #inst"2020-08-01"
      pr-with-TWO-commits #inst"2020-08-11"]
  (- (.getTime pr-with-one-commits)
    (.getTime pr-with-TWO-commits)))
