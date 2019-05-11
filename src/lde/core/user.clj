(ns lde.core.user
  (:require [clojure.set :refer [rename-keys]]
            [lde.auth :as auth]
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
        (update :user/password auth/hash)
        (db/save ctx))))

(defn get-by-id [ctx id]
  (db/get-by-id ctx id))

(defn login [ctx email password]
  (let [user (db/get-by-attribute ctx :user/email email)]
    (when (auth/validate password (:user/password user))
      user)))

(comment

  (let [ctx (db/init "./db")]
    (create {:password "hi"} ctx))

  )
