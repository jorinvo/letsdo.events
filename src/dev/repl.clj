(ns dev.repl
  (:require
    [cider-nrepl.main :as cider]
    [aleph.http :as http]
    [byte-streams :as bs]
    [lde.server :refer [make-server]]
    [lde.config :refer [get-config]]))

(def config (get-config "config.dev.edn"))

(defonce server (atom (make-server config)))

(defn -main []
  (cider/init))

(comment

  (do
    (.close @server)
    (reset! server (make-server config)))

  (-> @(http/get (str "http://localhost:" (:port config) "/login"))
      :body
      bs/to-string)

)
