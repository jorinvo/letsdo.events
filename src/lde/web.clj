(ns lde.web
  (:require
    [clojure.spec.alpha :as s]
    [clojure.java.io :as io]
    [reitit.ring :as ring]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.coercion :as ring-coericion]
    [reitit.ring.middleware.parameters :refer [parameters-middleware]]
    [reitit.ring.middleware.multipart :as multipart]
    [ring.middleware.multipart-params.byte-array :refer [byte-array-store]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.session :refer [wrap-session]]
    [ring.middleware.session.cookie :refer [cookie-store]]
    [ring.util.response :as response]
    [lde.core.settings :as settings]
    [lde.core.topic :as topic]
    [lde.core.event :as event]
    [lde.web.util :refer [image-mime-types]]
    [lde.web.middleware :as middleware]
    [lde.web.css :as css]
    [lde.web.error :as error]
    [lde.web.pages.topic :as topic-page]
    [lde.web.pages.event :as event-page]
    [lde.web.pages.home :as home]
    [lde.web.pages.login :as login]
    [lde.web.forms.topic :as topic-form]
    [lde.web.forms.event :as event-form]
    [clj-honeycomb.middleware.ring :refer [with-honeycomb-event]])
  (:import [java.util.regex Pattern]))

(def cookie-expiration-in-seconds (* 30 24 60 60))

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

(s/def ::max-attendees
  (s/and string?
         (s/or :empty empty?
               :number #(re-matches #"^[1-9][0-9]*$" %))))

(defn file-max-size [size]
  (fn [{b :bytes}]
    (> size (count b))))

(s/def ::image
  (s/and multipart/bytes-part
         (file-max-size (* 5 1024 1024))
         (s/or :empty-multipart-file (fn [{b :bytes}] (empty? b))
               :multipart-img (fn [{t :content-type}] (contains? image-mime-types t)))))

(s/def ::name string?)
(s/def ::link string?)
(s/def ::password
  (s/and string? (s/or :empty empty?
                       :strong-enough #(<= 8 (count %)))))

(s/def ::login-form
  (s/keys :req-un [:lde.config/email ::password]))

(s/def ::signup-form
  (s/keys :req-un [::name :lde.config/email ::password ::link]))


(s/def ::goto string?)
(s/def ::goto-query
  (s/keys :opt-un [::goto]))

(s/def ::token string?)
(s/def ::email-login-query
  (s/keys :req-un [::token]
          :opt-un [::goto]))

(s/def ::whats #{"upcoming" "new" "mine"})
(s/def ::overview-query (s/keys :opt-un [::whats]))

(s/def ::invite-form
  (s/keys :req-un [:lde.config/email]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/js/script.js" {:get (fn [_] (-> (response/resource-response "public/js/script.js")
                                        (update :body slurp)))}]
   ["/" {:get home/handler}]
   ["/login"
    ["" {:get {:handler login/handler
               :parameters {:query ::goto-query}}
         :post {:handler login/post-login
                :parameters {:query ::goto-query
                             :form ::login-form}}}]
    ["/email" {:get {:handler login/email
                    :parameters {:query ::email-login-query}}}]
    ["/email-confirm" {:get login/email-confirm}]]
   ["/signup" {:get {:handler login/handler
                     :parameters {:query ::goto-query}}
               :post {:handler login/post-signup
                      :parameters {:query ::goto-query
                                   :form ::signup-form}}}]
   ["/logout" {:get {:handler login/logout
                      :parameters {:query ::goto-query}}}]
   ["/new" {:middleware [middleware/authorize-user]
            :get topic-page/new
            :post {:handler topic-form/post
                   :parameters {:multipart {:name req-str
                                            :description string?
                                            :type #(contains? topic/types (keyword %))
                                            :visibility #(contains? topic/visibilities (keyword %))
                                            :image ::image}}}}]
   ["/accept"
    ["" {:get (constantly (response/redirect "/" :permanent-redirect))}]
    ["/:topic" {:middleware [middleware/load-topic]
                :get topic-form/accept-invite}]]
   ["/for"
    ["" {:get (constantly (response/redirect "/" :permanent-redirect))}]
    ["/:topic" {:middleware [middleware/load-topic
                             middleware/authorize-topic-read]}
     ["" {:get {:handler topic-page/overview
                :parameters {:query ::overview-query}}}]
     ["/edit" {:middleware [middleware/authorize-topic-edit]
               :get topic-page/edit
               :post {:handler topic-form/post-edit
                      :parameters {:multipart {:name req-str
                                               :description string?
                                               :type #(contains? topic/types (keyword %))
                                               :visibility #(contains? topic/visibilities (keyword %))
                                               :image ::image
                                               :delete-image string?}}}}]
     ["/delete" {:middleware [middleware/authorize-topic-edit]
                 :post topic-form/delete}]
     ["/join" {:get topic-form/accept-invite}]
     ["/invites"
      ["" {:middleware [middleware/authorize-topic-edit]
           :get topic-page/list-invites
           :post {:handler topic-form/post-invite
                  :parameters {:form ::invite-form}}}]
      ["/:invite/delete" {:post topic-form/post-delete-invite}]]
     ["/new" {:middleware [middleware/authorize-user]
              :get event-page/new
              :post {:handler event-form/post
                     :parameters {:multipart {:name req-str
                                              :description req-str
                                              :intention #(contains? event/intentions (keyword %))
                                              :start-date opt-date
                                              :start-time opt-time
                                              :end-date opt-date
                                              :end-time opt-time
                                              :max-attendees ::max-attendees
                                              :location string?
                                              :image ::image}}}}]
     ["/about"
      ["" {:get (fn [{{t :topic} :path-params}]
                  (response/redirect (str "/for/" t) :permanent-redirect))}]
      ["/:event" {:middleware [middleware/load-event]}
       ["" {:get event-page/get}]
       ["/edit" {:middleware [middleware/authorize-event-edit]
                 :get event-page/edit
                 :post {:handler event-form/post-edit
                        :parameters {:multipart {:name req-str
                                                 :description req-str
                                                 :start-date opt-date
                                                 :start-time opt-time
                                                 :end-date opt-date
                                                 :end-time opt-time
                                                 :max-attendees ::max-attendees
                                                 :location string?
                                                 :image ::image
                                                 :delete-image string?}}}}]
       ["/organize" {:middleware [middleware/authorize-user]
                     :get event-form/organize
                     :post event-form/organize}]
       ["/join" {:middleware [middleware/authorize-user]
                 :get event-form/join
                 :post event-form/join}]
       ["/leave" {:middleware [middleware/authorize-user]
                  :get event-form/leave
                  :post event-form/leave}]
       ["/delete" {:middleware [middleware/authorize-event-edit]
                   :get event-form/delete
                   :post event-form/delete}]]]]]])

(defn make-context-middleware [ctx]
  (fn [handler]
    (fn [req]
      (handler (assoc req :ctx ctx)))))

(defn make-session-middleware [ctx]
  (let [store (cookie-store {:key (settings/get-cookie-secret ctx)})]
    (fn [handler]
      (wrap-session handler {:store store
                             :cookie-name "letsdoevents-session"
                             :cookie-attr {:domain (-> ctx :config :public-base-url io/as-url .getHost)
                                           :secure true
                                           :max-age cookie-expiration-in-seconds
                                           :same-site :strict}}))))

(defn init [ctx]
  (ring/ring-handler
    (ring/router
      (routes)
      {:data {:coercion spec-coercion/coercion
              :middleware [ring-coericion/coerce-exceptions-middleware
                           ring-coericion/coerce-request-middleware
                           ring-coericion/coerce-response-middleware
                           (multipart/create-multipart-middleware {:store (byte-array-store)})]}})
    (ring/routes
      (ring/redirect-trailing-slash-handler)
      (ring/create-default-handler {:not-found (constantly (error/render {:status 404
                                                                          :title "404 - Not found"}
                                                                         ctx))
                                    :method-not-allowed (constantly (error/render {:status 403
                                                                                   :title "403 - Method not allowed"}
                                                                                  ctx))}))
    {:middleware [(when (-> ctx :config :honeycomb) with-honeycomb-event)
                  parameters-middleware
                  wrap-keyword-params
                  (make-session-middleware ctx)
                  (make-context-middleware ctx)]}))
