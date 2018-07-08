(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/data.json "0.2.6"]])

(task-options!
 pom {:project 'clojarbot
      :version "0.1.0"}
 jar {:main 'clojarbot.core
      :manifest {"description" "Some stupid bot to make you laugh"
                 "url" "https://github.com/pouque/clojarbot"
                 "license" "BSD-3-Clause"}}
 aot {:all true
      :namespace '#{clojarbot.core}})

(deftask build
  "Build the bot!"
  []
  (comp (aot) (pom) (uber) (jar) (target)))
