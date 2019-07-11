(ns lde.core.user
  (:require [clojure.set :refer [rename-keys]]
            [buddy.sign.jwt :as jwt]
            [postal.core :as postal]
            [java-time :as time]
            [lde.auth :as auth]
            [lde.core.settings :as settings]
            [lde.db :as db]
            [lde.email :as email]))

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

(defn get-by-email [email ctx]
  (db/get-by-attribute ctx :user/email email))

(defn get-token-for-email [user-id ctx]
  (jwt/sign {:user-id user-id
             :exp (time/plus (time/instant)
                             (time/days 7))}
            (settings/get-jwt-secret ctx)))

(defn get-email-login-link [base-url token goto]
  (str base-url
       "/login/email?token="
       token
       (when goto
         (str "&goto=" goto))))

(defn render-email [login-link user-name system-title]
  (str "Hi " (or user-name "there") "!\n\n"
       "Please click the link below to login to "
       system-title
       ":\n\n"
       login-link))

(defn send-email [{:keys [to subject body]}
                 {{{email-config :default} :smtp} :config}]
  (if email-config
    (postal/send-message email-config
                         {:from (:from email-config)
                          :to to
                          :subject subject
                          :body body})
    (println "WARNING: SMTP is not configured. Trying to send email:\n\n"
             body)))

(defn send-login-email [ctx email goto]
  (when-let [user (get-by-email email ctx)]
    (let [token (get-token-for-email (:id user) ctx)
         login-link (get-email-login-link (-> ctx :config :public-base-url) token goto)
         system-title (-> ctx :config :content :system-title)
         body (render-email login-link
                           (:user/name user)
                           system-title)]
      (email/send {:to email
                   :subject (str system-title " Login Link")
                   :body body}
                  ctx))))

