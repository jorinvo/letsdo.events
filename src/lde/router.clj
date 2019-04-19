(ns lde.router
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [lde.css :as css]
            [lde.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler
               :post login/post-signup}]])

(defn make-handler []
  (ring/ring-handler
   (ring/router (routes))
   (ring/routes
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler))
   {:middleware [parameters-middleware]}))
