(ns lde.core.user
  (:require [lde.auth :as auth]
            [lde.db :as db]))

(defn create [param ctx]
  (let [email (:email param)]
    (if (db/get-by-email ctx email)
      :duplicate-email
      (if-let [pw (:password param)]
        (let [enc-pw (auth/hash pw)
              data {:user/email (:email param)
                    :user/name (:name param)
                    :user/link (:link param)
                    :user/password enc-pw}
              user (db/save-user data ctx)]
          user)
        :no-password))))

(defn login [{:keys [email password]} ctx]
  (let [user (db/get-by-email ctx email)]
    (when (auth/validate password (:user/password user))
      user)))

(comment

  (let [ctx (db/init "./db")]
    (create {:password "hi"} ctx))

  )
