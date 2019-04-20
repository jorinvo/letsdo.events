(ns lde.web.router
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [lde.web.css :as css]
            [lde.web.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler
               :post login/post-signup}]])

(defn init [ctx]
  (ring/ring-handler
   (ring/router (routes))
   (ring/routes
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler))
   {:middleware [parameters-middleware
                 (fn [handler]
                   (fn [req]
                     (handler (assoc req :ctx ctx))))]}))
