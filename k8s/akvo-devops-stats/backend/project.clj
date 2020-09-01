(defproject akvo-devops-stats "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [duct/core "0.7.0"]
                 [duct/module.logging "0.4.0"]
                 [duct/module.sql "0.5.0"]
                 [duct/module.web "0.7.0"]
                 [reifyhealth/specmonstah "2.0.0-alpha-1"]
                 [org.clojure/test.check "0.10.0-alpha3"]
                 [org.clojure/spec.alpha "0.2.176"]
                 [org.postgresql/postgresql "42.2.6"]
                 [com.google.cloud.sql/postgres-socket-factory "1.0.16"]
                 [hugsql-adapter-case "0.1.0"]
                 [com.taoensso/nippy "2.14.0"]
                 [com.layerware/hugsql "0.4.9"]
                 [com.climate/claypoole "1.1.4"]
                 [vincit/venia "0.2.5"]
                 [nrepl "0.6.0"]
                 [iapetos "0.1.8"]
                 [io.prometheus/simpleclient_hotspot "0.6.0"]
                 [io.prometheus/simpleclient_jetty_jdk8 "0.6.0"]
                 [clj-http "3.10.0"]
                 [cheshire "5.8.1"]
                 [com.zaxxer/HikariCP "3.3.1"]
                 [raven-clj "1.5.2"]
                 [io.kubernetes/client-java "9.0.0"]
                 [clojure-humanize "0.2.2"]
                 [com.google.cloud/google-cloud-bigquery "1.117.1"]]
  :plugins [[duct/lein-duct "0.12.1"]
            [lein-eftest "0.5.8"]]
  :main ^:skip-aot akvo-devops-stats.main
  :resource-paths ["resources" "target/resources"]
  :prep-tasks     ["javac" "compile" ["run" ":duct/compiler"]]
  :uberjar-name "akvo-devops-stats.jar"
  :middleware     [lein-duct.plugin/middleware]
  :profiles
  {:dev  [:project/dev :profiles/dev]
   :repl {:prep-tasks   ^:replace ["javac" "compile"]
          :repl-options {:init-ns dev
                         :init (do
                                 (println "Starting BackEnd ...")
                                 (go))
                         :host "0.0.0.0"
                         :port 47480}}
   :uberjar {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.1"]
                                   [eftest "0.5.7"]
                                   [metosin/testit "0.4.0"]
                                   [kerodon "0.9.0"]]}})
