(ns lde.web.router
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.test.check.generators :as gen]
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
            [lde.web :as web]
            [lde.web.css :as css]
            [lde.web.js :as js]
            [lde.web.error :as error]
            [lde.web.pages.topic :as topic-page]
            [lde.web.pages.event :as event-page]
            [lde.web.pages.home :as home]
            [lde.web.pages.login :as login])
  (:import [java.util.regex Pattern]))

(defn authorize-user [handler]
  (fn [{:as req {user-id :id} :session}]
    (if user-id
      (handler req)
      (response/redirect (str "/login?goto=" (:uri req))))))

; TODO For now all topics are still public
(defn authorize-topic-read [handler]
  (fn [{:as req :keys [topic ctx] {user-id :id} :session}]
    (if (or user-id
            (= :public (:topic/visibility topic)))
      (handler req)
      (error/render {:status 404
                     :title "Not found"}
                    ctx))))

(defn authorize-topic-edit [handler]
  (fn [{:as req :keys [topic ctx] {user-id :id} :session}]
    (if (topic/admin? ctx (:id topic) user-id)
      (handler req)
      (error/render {:status 403
                     :title "Not allowed"}
                    ctx))))

(defn authorize-event-edit [handler]
  (fn [{:as req :keys [event ctx] {user-id :id} :session}]
    (if (or (event/organizer? ctx (:id event) user-id)
            (= user-id (:event/creator event)))
      (handler req)
      (error/render {:status 403
                     :title "Not allowed"}
                    ctx))))

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
               :multipart-img (fn [{t :content-type}] (contains? web/image-mime-types t)))))

; email stuff adopted from: https://github.com/SparkFund/useful-specs/blob/master/src/specs/internet.clj
(s/def ::hostpart
  (letfn [(pred [s]
            (re-matches #"\A(?:\p{Alnum}|\p{Alnum}(?:\p{Alnum}|-)*\p{Alnum})\z" s))
          (gen []
            (let [middle-char (gen/fmap char
                                        (gen/one-of [(gen/choose 48 57)
                                                     (gen/choose 65 90)
                                                     (gen/choose 97 122)
                                                     (gen/return 45)]))]
              (gen/let [length (gen/choose 1 64)]
                (let [chars-gen (if (= 1 length)
                                  (gen/vector gen/char-alphanumeric 1)
                                  (gen/let [first-char gen/char-alphanumeric
                                            last-char gen/char-alphanumeric
                                            middle-chars (gen/vector middle-char
                                                                     (- length 2))]
                                    (gen/return (-> [first-char]
                                                    (into middle-chars)
                                                    (conj last-char)))))]
                  (gen/fmap str/join chars-gen)))))]
    (s/spec pred :gen gen)))

(s/def ::hostname
  (let [hostpart-spec (s/get-spec ::hostpart)]
    (letfn [(pred [s]
              (and (>= 253 (count s))
                   (let [parts (str/split s #"\.")]
                     (and (not (str/starts-with? s "."))
                          (not (str/ends-with? s "."))
                          (every? (partial s/valid? hostpart-spec) parts)))))
            (gen []
              (let [min-needed 2
                    max-needed 4]
                (let [parts-gen (gen/vector (s/gen hostpart-spec)
                                            min-needed max-needed)]
                  (gen/fmap (partial str/join ".") parts-gen))))]
      (s/spec pred :gen gen))))

(s/def ::local-email-part
  (let [chars (set (str "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        "abcdefghijklmnopqrstuvwxyz"
                        "0123456789"
                        "!#$%&'*+-/=?^_`{|}~"))
        dot-chars (conj chars \.)]
    (letfn [(pred [s]
              (and (>= 64 (count s) 1)
                   (not (str/starts-with? s "."))
                   (not (str/ends-with? s "."))
                   (every? dot-chars s)
                   (not (re-find #"\.\." s))))
            (gen []
              (gen/fmap str/join (gen/vector (gen/elements chars) 1 64)))]
      (s/spec pred :gen gen))))

(s/def ::email
  (s/spec (fn [s]
              (let [parts (str/split s #"@")]
                (and (= 2 (count parts))
                     (s/valid? ::local-email-part (first parts))
                     (s/valid? ::hostname (second parts)))))
        :gen (fn []
              (gen/let [local-part (s/gen ::local-email-part)
                        hostname-part (s/gen ::hostname)]
                (gen/return (str local-part "@" hostname-part))))))

(s/def ::name string?)
(s/def ::link string?)
(s/def ::password
  (s/and string? (s/or :empty empty?
                       :strong-enough #(<= 8 (count %)))))

(s/def ::login-form
  (s/keys :req-un [::email ::password]))

(s/def ::signup-form
  (s/keys :req-un [::name ::email ::password ::link]))


(s/def ::goto string?)
(s/def ::goto-query
  (s/keys :opt-un [::goto]))

(s/def ::token string?)
(s/def ::mail-login-query
  (s/keys :req-un [::token]
          :opt-un [::goto]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/js/script.js" {:get js/handler}]
   ["/" {:get home/handler}]
   ["/login"
    ["" {:get {:handler login/handler
               :parameters {:query ::goto-query}}
         :post {:handler login/post-login
                :parameters {:query ::goto-query
                             :form ::login-form}}}]
    ["/mail" {:get {:handler login/mail
                    :parameters {:query ::mail-login-query}}}]
    ["/mail-confirm" {:get login/mail-confirm}]]
   ["/signup" {:get {:handler login/handler
                     :parameters {:query ::goto-query}}
               :post {:handler login/post-signup
                      :parameters {:query ::goto-query
                                   :form ::signup-form}}}]
   ["/logout" {:get {:handler login/logout
                      :parameters {:query ::goto-query}}}]
   ["/new" {:middleware [authorize-user]
            :get topic-page/new
            :post {:handler topic-page/post
                   :parameters {:multipart {:name req-str
                                            :description string?
                                            :type #(contains? topic/types (keyword %))
                                            :visibility #(contains? topic/visibilities (keyword %))
                                            :image ::image}}}}]
   ["/for"
    ["" {:get (constantly (response/redirect "/" :permanent-redirect))}]
    ["/:topic" {:middleware [topic-page/load-middleware
                             authorize-topic-read]}
     ["" {:get topic-page/overview}]
     ["/edit" {:middleware [authorize-topic-edit]
               :get topic-page/edit
               :post {:handler topic-page/post-edit
                      :parameters {:multipart {:name req-str
                                               :description string?
                                               :type #(contains? topic/types (keyword %))
                                               :visibility #(contains? topic/visibilities (keyword %))
                                               :image ::image
                                               :delete-image string?}}}}]
     ["/delete" {:middleware [authorize-topic-edit]
                 :post topic-page/delete}]
     ["/new" {:middleware [authorize-user]
              :get event-page/new
              :post {:handler event-page/post
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
      ["/:event" {:middleware [event-page/load-middleware]}
       ["" {:get event-page/get}]
       ["/edit" {:middleware [authorize-event-edit]
                 :get event-page/edit
                 :post {:handler event-page/post-edit
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
       ["/organize" {:middleware [authorize-user]
                     :get event-page/organize
                     :post event-page/organize}]
       ["/join" {:middleware [authorize-user]
                 :get event-page/join
                 :post event-page/join}]
       ["/leave" {:middleware [authorize-user]
                  :get event-page/leave
                  :post event-page/leave}]
       ["/delete" {:middleware [authorize-event-edit]
                   :get event-page/delete
                   :post event-page/delete}]]]]]])

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
    {:middleware [parameters-middleware
                  wrap-keyword-params
                  (make-session-middleware ctx)
                  (make-context-middleware ctx)]}))
