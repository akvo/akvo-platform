(ns akvo-devops-stats.util.db
  (:require [integrant.core :as ig]
            ragtime.jdbc
            [hugsql-adapter-case.adapters :as adapter-case]
            [hugsql.core :as hugsql]))

(hugsql/set-adapter! (adapter-case/kebab-adapter))

(defmethod ig/init-key ::migration [_ config]
  (ragtime.jdbc/load-resources "migrations"))
