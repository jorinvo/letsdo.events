(ns lde.web.router
  (:require [spec-tools.data-spec :as ds]
            [reitit.ring :as ring]
            [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.middleware.parameters :refer [parameters-middleware]]
            [reitit.ring.middleware.multipart :as multipart]
            [ring.middleware.multipart-params.byte-array :refer [byte-array-store]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [lde.core.settings :as settings]
            [lde.core.topic :as topic]
            [lde.core.event :as event]
            [lde.web.css :as css]
            [lde.web.pages.topic :as topic-page]
            [lde.web.pages.event :as event-page]
            [lde.web.pages.home :as home]
            [lde.web.pages.login :as login]))

(defn authorize [handler]
  (fn [req]
    (if (get-in req [:session :id])
      (handler req)
      {:status 403
       :body "unauthorized"})))

(defn req-str [s]
  (and (string? s)
       (< 0 (count s))))

(defn opt-date [s]
  (and (string? s)
       (or (empty? s)
           (re-matches #"^\d{4}-\d{2}-\d{2}$" s))))

(defn opt-time [s]
  (and (string? s)
       (or (empty? s)
           (re-matches #"^\d{2}:\d{2}$" s))))

(defn opt-str-pos-int [s]
  (and (string? s)
       (or (empty? s)
           (re-matches #"^[1-9][0-9]*$" s))))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/" {:get home/handler}]
   ["/login" {:get login/handler
              :post login/post-login}]
   ["/signup" {:get login/handler
               :post login/post-signup}]
   ["/logout" {:get login/logout}]
   ["/new" {:middleware [authorize]
            :get topic-page/new
            :post {:handler topic-page/post
                   :parameters {:multipart {:name req-str
                                            :description string?
                                            :type #(contains? topic/types (keyword %))
                                            :visibility #(contains? topic/visibilities (keyword %))
                                            :image multipart/bytes-part}}}}]
   ["/for/:topic" {:middleware [authorize]}
    ["" {:get topic-page/overview}]
    ["/new" {:get event-page/new
             :post {:handler event-page/post
                    :parameters {:multipart {:name req-str
                                             :description req-str
                                             :intention #(contains? event/intentions (keyword %))
                                             :start-date opt-date
                                             :end-date opt-date
                                             :start-time opt-time
                                             :end-time opt-time
                                             :max-attendees opt-str-pos-int
                                             :location string?
                                             :image multipart/bytes-part}}}}]
    ["/about/:event"
     ["/" {:get event-page/get}]
     ["/join" {:post event-page/join}]]]])

(defn make-context-middleware [ctx]
  (fn [handler]
    (fn [req]
      (handler (assoc req :ctx ctx)))))

(defn make-session-middleware [ctx]
  (let [store (cookie-store {:key (settings/get-cookie-secret ctx)})]
    (fn [handler]
      (wrap-session handler {:store store
                             :cookie-name "letsdoevents-session"}))))

(defn init [ctx]
  (ring/ring-handler
    (ring/router
      (routes)
      {:data {:coercion spec-coercion/coercion
              :middleware [(multipart/create-multipart-middleware {:store (byte-array-store)})]}})
    (ring/routes
      (ring/redirect-trailing-slash-handler)
      (ring/create-default-handler))
    {:middleware [parameters-middleware
               wrap-keyword-params
               (make-session-middleware ctx)
               (make-context-middleware ctx)]}))
