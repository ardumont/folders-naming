(defproject folders-naming/folders-naming "0.1.0-SNAPSHOT"
  :license {:name "Eclipse Public License",
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [fs "1.3.2"]
                 [org.clojure/tools.cli "0.2.2"]]
  :profiles {:dev
             {:dependencies
              [[com.intelie/lazytest
                "1.0.0-SNAPSHOT"
                :exclusions
                [swank-clojure]]
               [midje "1.4.0"]]}}
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :description "A project to massively rename my folder according to my naming convention."
  :main folders-naming.core)
