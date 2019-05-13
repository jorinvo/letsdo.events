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
            [lde.core.settings :as settings]
            [lde.core.topic :as topic]
            [lde.core.event :as event]
            [lde.web :as web]
            [lde.web.css :as css]
            [lde.web.pages.topic :as topic-page]
            [lde.web.pages.event :as event-page]
            [lde.web.pages.home :as home]
            [lde.web.pages.login :as login])
  (:import [java.util.regex Pattern]))

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

(s/def ::token string?)

(s/def ::login-query
  (s/keys :req-un [::token]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/" {:get home/handler}]
   ["/login"
    ["" {:get login/handler
              :post {:handler login/post-login
                     :parameters {:form ::login-form}}}]
    ["/mail" {:get {:handler login/mail
               :parameters {:query ::login-query}}}]
    ["/mail-confirm" {:get login/mail-confirm}]]
   ["/signup" {:get login/handler
               :post {:handler login/post-signup
                      :parameters {:form ::signup-form}}}]
   ["/logout" {:get login/logout}]
   ["/new" {:middleware [authorize]
            :get topic-page/new
            :post {:handler topic-page/post
                   :parameters {:multipart {:name req-str
                                            :description string?
                                            :type #(contains? topic/types (keyword %))
                                            :visibility #(contains? topic/visibilities (keyword %))
                                            :image ::image}}}}]
   ["/for/:topic" {:middleware [authorize]}
    ["" {:get topic-page/overview}]
    ["/new" {:get event-page/new
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
    ["/about/:event"
     ["" {:get event-page/get}]
     ["/edit" {:post {:handler event-page/edit
                      :parameters {:multipart {:name req-str
                                               :description req-str
                                               :start-date opt-date
                                               :start-time opt-time
                                               :end-date opt-date
                                               :end-time opt-time
                                               :max-attendees ::max-attendees
                                               :location string?
                                               :image ::image}}}}]
     ["/join" {:post event-page/join}]
     ["/leave" {:post event-page/leave}]]]])

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
      (ring/create-default-handler))
    {:middleware [parameters-middleware
               wrap-keyword-params
               (make-session-middleware ctx)
               (make-context-middleware ctx)]}))
