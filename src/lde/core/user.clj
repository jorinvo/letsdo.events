(ns lde.core.user
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [buddy.sign.jwt :as jwt]
            [lde.auth :as auth]
            [lde.core.settings :as settings]
            [lde.db :as db]))

(def user-key-map {:email :user/email
                   :name :user/name
                   :link :user/link
                   :password :user/password})

(defn create [data ctx]
  (db/tx ctx
         (if (db/get-by-attribute ctx :user/email (:email data))
           :duplicate-email
           (-> data
               (select-keys (keys user-key-map))
               (rename-keys user-key-map)
               (update :user/password #(when-not (empty? %) (auth/hash %)))
               (db/create! ctx)))))

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

(defn get-by-mail [email ctx]
  (db/get-by-attribute ctx :user/email email))

(defn get-token-for-mail [user-id ctx]
  (jwt/sign {:user-id user-id} (settings/get-jwt-secret ctx)))

(defn get-mail-login-link [base-url token goto]
  (str base-url
       "/login/mail?token="
       token
       (when goto
         (str "&goto=" goto))))

(defn render-mail [login-link user-name]
  (str "Hi " (or user-name "there") "!\n\n"
       "Please click the link below to login:\n\n"
       login-link))

(defn send-mail [mail-content email {{{mail-config :default} :smtp} :config}]
  (if mail-config
    (prn mail-content)
    (println "WARNING: SMTP is not configured. Trying to send mail:\n\n"
             mail-content)))

(defn send-login-mail [ctx email goto]
  (when-let [user (get-by-mail email ctx)]
    (let [token (get-token-for-mail (:id user) ctx)
         login-link (get-mail-login-link (-> ctx :config :public-base-url) token goto)
         mail-content (render-mail login-link (:user/name user))]
      (send-mail mail-content email ctx))))

