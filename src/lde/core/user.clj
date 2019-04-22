(ns lde.core.user
  (:require [lde.auth :as auth]
            [lde.db :as db]))

(defn create [param ctx]
  (if-let [pw (:password param)]
    (let [enc-pw (auth/hash pw)
          data {:user/email (:email param)
                :user/name (:name param)
                :user/link (:link param)
                :user/password enc-pw}
          user (db/save-user data ctx)]
      user)
    :no-password))

(comment

  (let [ctx (db/init "./db")]
    (create {:password "hi"} ctx))

  )
