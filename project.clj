(defproject jepsen.zookeeper "0.1.0-SNAPSHOT"
  :description "Michael's test for Jepsen"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [jepsen "0.1.4"]
                 [avout "0.5.4" :exclusions [org.slf4j/slf4j-log4j12]]]
  :main jepsen.zookeeper)
