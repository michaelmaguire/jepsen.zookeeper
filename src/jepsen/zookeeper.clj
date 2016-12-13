(ns jepsen.zookeeper
  (:gen-class) 
  (:require [jepsen [cli :as cli] 
             [db :as db]
             [control :as c]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [clojure.tools.logging :refer :all]
            )
  )

(defn db "A zookeeper DB at the given version"
  [version]
  (reify db/DB
    (setup! [_ test node]
      (info "Setting up ZK on " node))
    (teardown! [_ test node]
      (info node "tearing down ZK"))))


(defn zk-test "Given an options map from the command-line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map." 
  [opts] 
  (merge tests/noop-test 
         {
          :nodes (:nodes opts) 
          :ssh (:ssh opts)
          }))

(defn -main
  "Handle command line arguments"
  [& args]
  (cli/run! (cli/single-test-cmd {:test-fn zk-test}) args))



