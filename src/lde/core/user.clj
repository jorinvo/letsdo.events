(ns lde.core.user
  (:require [clojure.set :refer [rename-keys]]
            [clojure.java.browse :refer [browse-url]]
            [buddy.sign.jwt :as jwt]
            [lde.auth :as auth]
            [lde.core.settings :as settings]
            [lde.db :as db]))

(def user-key-map {:email :user/email
                   :name :user/name
                   :link :user/link
                   :password :user/password})

(defn create [data ctx]
  (if (db/get-by-attribute ctx :user/email (:email data))
    :duplicate-email
    (-> data
        (select-keys (keys user-key-map))
        (rename-keys user-key-map)
        (update :user/password #(when-not (empty? %) (auth/hash %)))
        (db/save ctx))))

(defn get-by-id [ctx id]
  (db/get-by-id ctx id))

(defn login [ctx email password]
  (let [user (db/get-by-attribute ctx :user/email email)]
    (when (auth/validate password (:user/password user))
      user)))

(defn login-with-token [ctx token]
  (when-let [{id :user-id} (try (jwt/unsign token (settings/get-jwt-secret ctx))
                                (catch Exception e nil))]
    (db/get-by-id ctx id)))

(defn get-token-for-mail [ctx email]
  (when-let [user (db/get-by-attribute ctx :user/email email)]
    (jwt/sign {:user-id (:id user)} (settings/get-jwt-secret ctx))))

(defn send-login-mail [ctx email]
  (browse-url (str "http://localhost:3000/login/mail?token="
                   (get-token-for-mail ctx email))))
