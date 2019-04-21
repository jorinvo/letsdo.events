(ns lde.web.router
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [lde.web.css :as css]
            [lde.web.pages.home :as home]
            [lde.web.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/" {:get home/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler
               :post login/post-signup}]])

(defn make-context-middleware [ctx]
  (fn [handler]
    (fn [req]
      (handler (assoc req :ctx ctx)))))

(defn init [ctx]
  (ring/ring-handler
   (ring/router (routes))
   (ring/routes
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler))
   {:middleware [parameters-middleware
                 wrap-keyword-params
                 wrap-cookies
                 (make-context-middleware ctx)]}))

