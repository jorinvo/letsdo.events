(ns lde.main
  (:require
    [clojure.pprint :refer [pprint]]
    [aleph.http :as http]
    [signal.handler :as signal]
    [lde.web.router :as router]
    [lde.db :as db]
    [lde.config :refer [get-config]]))

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
         server (http/start-server (router/init ctx) config)
         stop (fn []
                (println "\nshutting down")
                (.close server)
                (db/close ctx)
                (println "bye")
                (System/exit 0))]
     (signal/with-handler :term (stop))
     (signal/with-handler :int (stop)))))
