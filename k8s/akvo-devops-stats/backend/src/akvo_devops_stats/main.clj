(ns akvo-devops-stats.main
  (:gen-class)
  (:require [duct.core :as duct]
            [integrant.core :as ig]))

(duct/load-hierarchy)

(defonce system-atom (atom nil))

(defn -main [& args]
  (let [keys (or (duct/parse-keys args) [:duct/daemon])
        profiles [:duct.profile/prod]
        system (->
                (duct/resource "akvo_devops_stats/config.edn")
                (duct/read-config)
                (duct/prep-config profiles)
                (ig/init keys))]
    (reset! system-atom system)
    (duct/await-daemons system)))
