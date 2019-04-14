(ns events.letsdo.server
  (:require [aleph.http :as http]
            [reitit.ring :as ring]
            [events.letsdo.router :refer [routes]]))

; TODO don't rebuild router on every request in production, only in dev
(defn handler [req]
  ((ring/ring-handler
     (ring/router (routes))
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler)) req))

(handler {:request-method :get :uri "/login"})

(defn make-server [{:keys [port]}]
  (http/start-server handler {:port port}))

