(ns lde.main
  (:require
    [clojure.pprint :refer [pprint]]
    [aleph.http :as http]
    [signal.handler :as signal]
    [lde.web :as web]
    [lde.db :as db]
    [lde.config :refer [get-config]]
    [clj-honeycomb.core :as honeycomb]))

(defn -main [& args]
  (when (> (count args) 1)
    (println "start with path to config file as single argument")
    (System/exit 1))
  (let [config (get-config (first args))]
    (when-not config
      (println "invalid config")
      (System/exit 1))
    (println "starting with config:")
    (pprint config)
    (let [ctx (db/init {:config config})
          server (http/start-server (web/init ctx) config)
          stop (fn []
                 (println "\nshutting down")
                 (.close server)
                 (db/close ctx)
                 (println "bye")
                 (System/exit 0))]
      (println "init honeycomb")
      (honeycomb/init (:honeycomb config))
      (signal/with-handler :term (stop))
      (signal/with-handler :int (stop))
      (println "ready"))))
