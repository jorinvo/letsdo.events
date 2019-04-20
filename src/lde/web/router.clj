(ns lde.web.router
  (:require [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as coercion]
            [lde.web.css :as css]
            [lde.web.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler
               :post {:parameters {:form {:email string?
                                          :name string?}}
                      :handler login/post-signup}}]])

(defn make-context-middleware [ctx]
  (fn [handler]
    (fn [req]
      (handler (assoc req :ctx ctx)))))

(defn init [ctx]
  (ring/ring-handler
   (ring/router (routes)
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})
   (ring/routes
     (ring/redirect-trailing-slash-handler)
     (ring/create-default-handler))
   {:middleware [parameters-middleware
                 (make-context-middleware ctx)]}))

