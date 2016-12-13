(ns jepsen.zookeeper
  (:gen-class) (:require [jepsen.cli :as cli] [jepsen.tests :as tests])
  ) 

(defn zk-test "Given an options map from the command-line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map." [opts] (merge tests/noop-test {:nodes (:nodes opts) :ssh (:ssh opts)}))

(defn -main
  "Handle command line arguments"
  [& args]
  (cli/run! (cli/single-test-cmd {:test-fn zk-test}) args))



