(defproject reddit-background "0.1.0-SNAPSHOT"
  :description "A clojure app that downloads and sets wallpapers from subreddit sources."
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"], [org.clojure/data.json "0.2.6"], [org.jsoup/jsoup "1.8.3"], [commons-io/commons-io "2.5"]]
  :main ^:skip-aot reddit-background.core
  :target-path "target/%s"
  :uberjar-name "reddit_background.jar"
  :profiles {:uberjar {:aot :all}})
