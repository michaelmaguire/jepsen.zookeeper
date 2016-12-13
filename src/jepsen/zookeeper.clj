(ns jepsen.zookeeper
  (:gen-class) 
  (:require [jepsen [cli :as cli] 
             [db :as db]
             [control :as c]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [clojure.tools.logging :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            )
  )



(defn zk-node-ids "Returns a map of node names to node ids." [test] (->> test :nodes (map-indexed (fn [i node] [node i])) (into {})))

(defn zk-node-id "Given a test and a node name from that test, returns the ID for that node." [test node] ((zk-node-ids test) node))


(defn zoo-cfg-servers
  "Constructs a zoo.cfg fragment for servers."
  [test]
  (->> (zk-node-ids test)
       (map (fn[[node id]]
              (str "server." id "=" (name node) ":2888:3888")))
       (str/join "\n")))


(defn db "A zookeeper DB at the given version"
  [version]
  (reify db/DB
    (setup! [_ test node]
      (info "Setting up ZK on " node)
      (c/su
        (info node "Installing ZK" version)
        (debian/install {:zookeeper version
                         :zookeeper-bin version
                         :zookeeperd version})
        (info node "id is" (zk-node-id test node))))
    (teardown! [_ test node]
      (info node "tearing down ZK"))))


(defn zk-test "Given an options map from the command-line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map." 
  [opts] 
  (merge tests/noop-test 
         {
          :nodes (:nodes opts) 
          :ssh (:ssh opts)
          :name "zookeeper"
          :os debian/os
          :db (db "3.4.5+dfsg-2")
          }))

(defn -main
  "Handle command line arguments"
  [& args]
  (cli/run! (cli/single-test-cmd {:test-fn zk-test}) args))



