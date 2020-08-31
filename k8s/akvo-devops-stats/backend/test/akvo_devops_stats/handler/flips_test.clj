(ns akvo-devops-stats.handler.flips-test
  (:require [clojure.test :refer :all]
            [akvo-devops-stats.flips :as flip]))

(def last-flip-sha "last-flip")
(def previous-flip-sha "previous-flip")

(deftest flip-reason
  (testing "one to one mapping"
    (let [flips (shuffle [{:sha last-flip-sha
                           :name "flip-202007"}
                          {:sha previous-flip-sha
                           :name "flip-202004"}])
          promotions (shuffle [{:sha last-flip-sha
                                :name "promote-2020.02"
                                :reason "OK"}
                               {:sha previous-flip-sha
                                :name "promote-2020.01"
                                :reason "OK also"}])]
      (is (= [{:sha last-flip-sha
               :name "flip-202007"
               :reason "OK"}
              {:sha previous-flip-sha
               :name "flip-202004"
               :reason "OK also"}]
            (flip/find-reasons flips promotions)))))

  (testing "One flip to many promotions. One FIX_RELEASE promotion marks the whole flip as a FIX_RELEASE"
    (let [flips (shuffle [{:sha last-flip-sha
                           :name "flip-202007"}
                          {:sha previous-flip-sha
                           :name "flip-202004"}])
          promotions (shuffle [{:sha last-flip-sha
                                :name "promote-2020.05"
                                :reason "OK"}
                               {:sha (rand)
                                :name "promote-2020.04"
                                :reason "FIX_RELEASE"}
                               {:sha (rand)
                                :name "promote-2020.03"
                                :reason "OK"}
                               {:sha (rand)
                                :name "promote-2020.02"
                                :reason "OK"}
                               {:sha previous-flip-sha
                                :name "promote-2020.01"
                                :reason "OK also"}])]
      (is (= [{:sha last-flip-sha
               :name "flip-202007"
               :reason "FIX_RELEASE"}
              {:sha previous-flip-sha
               :name "flip-202004"
               :reason "OK also"}]
            (flip/find-reasons flips promotions)))))

  (testing "Promotion not flipped yet"
    (let [flips (shuffle [{:sha last-flip-sha
                           :name "flip-202007"}])
          promotions (shuffle [{:sha (rand)
                                :name "promote-2020.07"
                                :reason "OK"}
                               {:sha (rand)
                                :name "promote-2020.05"
                                :reason "OK"}
                               {:sha last-flip-sha
                                :name "promote-2020.04"
                                :reason "this one"}
                               {:sha (rand)
                                :name "promote-2020.03"
                                :reason "any"}])]
      (is (= [{:sha last-flip-sha
               :name "flip-202007"
               :reason "this one"}]
            (flip/find-reasons flips promotions)))))

  (testing "Flip without promotions"
    (let [flips (shuffle [{:sha last-flip-sha
                           :name "flip-202007"}
                          {:sha "no promotions"
                           :name "flip-202006"}])
          promotions (shuffle [{:sha last-flip-sha
                                :name "promote-2020.04"
                                :reason "this one"}
                               {:sha (rand)
                                :name "promote-2020.03"
                                :reason "any"}])]
      (is (= [{:sha last-flip-sha
               :name "flip-202007"
               :reason "this one"}
              {:sha "no promotions"
               :reason nil
               :name "flip-202006"}]
            (flip/find-reasons flips promotions))))))