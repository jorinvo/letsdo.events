(ns events.letsdo.server
  (:require [aleph.http :as http]))

(defn handler [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "hello!"})

(defn make-server [{:keys [port]}]
  (http/start-server handler {:port port}))

