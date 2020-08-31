(ns akvo-devops-stats.projects
  (:require
    [akvo-devops-stats.promotions :as promotions]
    [akvo-devops-stats.flips :as flips]))

(def projects
  [
   {:repository "akvo-rsr"
    :tag-prefix "promote"
    :semaphoreci-project-id "2eb9e64a-5f1a-4e4e-a0a8-3307e1e69565"
    :initial-promotion-exclusive "promote-20200518-124657"
    :initial-commit-exclusive "32bb28f6bacf2954ecba236732bfb12a498c1423"
    :trunk-branch "master"
    :team "RSR"
    :release-fn #'promotions/get-promotions}

   {:repository "akvo-flow-services"
    :tag-prefix "promote"
    :initial-promotion-exclusive "promote-20200501-161837"
    :initial-commit-exclusive "ed896e336331089b7311e1a550e31a832ad59b1e"
    :trunk-branch "develop"
    :team "Flumen"
    :release-fn #'promotions/get-promotions}

   {:repository "akvo-flow-api"
    :tag-prefix "promote"
    :initial-promotion-exclusive "promote-20200320-113228"
    :initial-commit-exclusive "3ff0425a8444522e6188c7dd27791ec16989a033"
    :trunk-branch "develop"
    :team "Flumen"
    :release-fn #'promotions/get-promotions}

   {:repository "akvo-lumen"
    :tag-prefix "promote"
    :semaphoreci-project-id "0f114080-6c03-4963-bb26-c43769f8580a"
    :initial-promotion-exclusive "promote-20200507-132322"
    :initial-commit-exclusive "086e95048b1042646fd1a2240cfa481733db8d1a"
    :trunk-branch "master"
    :team "Flumen"
    :release-fn #'flips/get-releases}

   {:repository "akvo-flow"
    :tag-prefix "promote"
    :semaphoreci-project-id "9285d79e-251f-4aa7-ba38-cc31655cb401"
    :semaphore-yaml-file "deploy.yml"
    :initial-promotion-exclusive "promote-20200528-131124"
    :initial-commit-exclusive "6f6c3c0c756702ccf5dd23c66854830b944658a9"
    :initial-flip-exclusive "flip-20200602-170358"
    :trunk-branch "master"
    :team "Flumen"
    :release-fn #'flips/get-releases}

   ;{:repo-name "akvo-unified-log" :tag-prefix "promote" :semaphoreci-project-id "0f114080-6c03-4963-bb26-c43769f8580a"}
   ;{:repo-name "akvo-authorization" :tag-prefix "promote" :semaphoreci-project-id "0f114080-6c03-4963-bb26-c43769f8580a"}
   ])

(def project->team (into {} (map (juxt :repository :team) projects)))
