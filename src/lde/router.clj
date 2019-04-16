(ns lde.router
  (:require [reitit.ring :as ring]
            [lde.css :as css]
            [lde.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler}]])

(defn make-handler []
  (ring/ring-handler
   (ring/router (routes))
   (ring/routes
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler))))
