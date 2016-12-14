(ns jepsen.zookeeper
  (:gen-class) 
  (:require [jepsen [cli :as cli] 
             [db :as db]
             [control :as c]
             [tests :as tests]
             [cli :as cli]
             [generator :as gen]
             [util :as util :refer [timeout]]
             [client :as client]
             [checker :as checker]
             [nemesis :as nemesis]]
            [jepsen.os.debian :as debian]
            [clojure.tools.logging :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [avout.core :as avout]
            [knossos.model :as model]
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
        (info node "id is" (zk-node-id test node))
        (c/exec :echo (zk-node-id test node) :> "/etc/zookeeper/conf/myid")
        (c/exec :echo (str (slurp (io/resource "zoo.cfg"))
                           "\n"
                           (zoo-cfg-servers test)
                           :> "/etc/zookeeper/conf/zoo.cfg"))
        (info node "ZK restarting")
        (c/exec :service :zookeeper :restart)
        (Thread/sleep 5000)
        (info node "ZK ready")))
    (teardown! [_ test node]
      (info node "tearing down ZK")
      (c/su
        (c/exec :service :zookeeper :stop)
        (c/exec :rm :-rf
                (c/lit "/var/lib/zookeeper/version-*")
                (c/lit "/var/log/zookeeper/*"))))

    db/LogFiles
      (log-files [_ test node]
               ["/var/log/zookeeper/zookeeper.log"])))

(defn r  [_ _] {:type :invoke, :f :read, :value nil})
(defn w  [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defn client
  "A client for single compare-and-set register"
  [conn a]
  (reify client/Client
    (setup! [_ test node]
      (let [conn (avout/connect (name node))
            a (avout/zk-atom conn "/jepsen" 0)]
      (client conn a)))
    (invoke! [this test op]
      (timeout 5000 (assoc op :type :info, :error :timeout)
               (case (:f op)
                 :read (do (info "log from test" @a) (assoc op :type :ok, :value @a))
                 :write (do (avout/reset!! a (:value  op))
                            (assoc op :type :ok))
                 :cas (let [[value value'] (:value op)
                            type  (atom :fail)]
                        (avout/swap!! a (fn [current]
                                          (if (= current value)
                                            (do (reset! type :ok)
                                                value')
                                            (do (reset! type :fail)
                                                current))))
                        (assoc op :type @type)))))
    (teardown! [_ test]
      (.close conn))))




(defn zk-test "Given an options map from the command-line runner (e.g. :nodes, :ssh, :concurrency, ...), constructs a test map." 
  [opts] 
  (merge tests/noop-test 
         {
          :nodes (:nodes opts) 
          :ssh (:ssh opts)
          :name "zookeeper"
          :os debian/os
          :db (db "3.4.5+dfsg-2")
          :client (client nil nil)
          :nemesis (nemesis/partition-random-halves)
          :generator(->> (gen/mix [r w cas])
                         (gen/stagger 1)
                         (gen/nemesis
                           (gen/seq (cycle [(gen/sleep 5)
                                            {:type :info, :f :start}
                                            (gen/sleep 5)
                                            {:type :info, :f :stop}])))
                         (gen/time-limit 15))
          :model (model/cas-register 0)
          :checker (checker/compose {:linear checker/linearizable :perf (checker/perf)})}))

(defn -main
  "Handle command line arguments"
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn zk-test})
                   (cli/serve-cmd)) args))



