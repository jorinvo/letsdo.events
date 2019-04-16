(ns lde.server.server
  (:require [aleph.http :as http]
            [reitit.ring :as ring]
            [lde.server.router :refer [routes]]))

; TODO don't rebuild router on every request in production, only in dev
(defn handler [req]
  ((ring/ring-handler
     (ring/router (routes))
     (ring/routes
       (ring/create-resource-handler {:path "/"})
       (ring/redirect-trailing-slash-handler)
       (ring/create-default-handler))) req))

(defn make-server [{:keys [port]}]
  (http/start-server handler {:port port}))

