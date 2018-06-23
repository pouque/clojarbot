(defproject clojarbot "0.1.0-SNAPSHOT"
  :description "Some stupid bot to make you laugh"
  :url "https://github.com/puque/clojarbot"
  :license {:name "BSD-3-Clause"
            :url "https://opensource.org/licenses/BSD-3-Clause"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.3"]
                 [org.postgresql/postgresql "42.1.4"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/core.match "0.3.0-alpha5"]]
  :main clojarbot.core)
