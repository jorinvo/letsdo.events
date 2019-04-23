(ns lde.core.user
  (:require [lde.auth :as auth]
            [lde.db :as db]))

(defn create [user ctx]
  (if (db/get-by-attribute ctx :user/email (:user/email user))
    :duplicate-email
    (-> user
        (update :user/password auth/hash)
        (db/save ctx))))

(defn login [{:keys [email password]} ctx]
  (let [user (db/get-by-attribute ctx :user/email email)]
    (when (auth/validate password (:user/password user))
      user)))

(comment

  (let [ctx (db/init "./db")]
    (create {:password "hi"} ctx))

  )
