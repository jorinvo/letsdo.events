(ns lde.core.user
  (:require [lde.auth :as auth]
            [lde.db :as db]))

(defn create [param ctx]
  (if-let [pw (:password param)]
    (let [enc-pw (auth/encrypt pw)
          data {:user/email (:email param)
                :user/name (:name param)
                :user/link (:link param)
                :user/password enc-pw}
          user (db/save-user data ctx)]
      {:token (auth/gen-token (:id user))})
    :no-password))

(comment

  (let [ctx (db/init "./db")]
    (create {:password "hi"} ctx))

  (= (capture ctx [save-user (fn [x] {:uuid "uuid"})
                   gen-token (fn [x] {:uuid "uuid"})]
              (= {:token "123"} (create {"password" "hi"} ctx)))
     {:save-user [{"password" "hi"}]
      :gen-token ["uuid"]})

  (with-redefs [db/create-user (fn [x] {:uuid "123"})]
    (= {:token "123"} (create {"password" "hi"})))

  )
