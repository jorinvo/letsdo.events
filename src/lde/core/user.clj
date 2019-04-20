(ns lde.core.user
  (:require [lde.auth :as auth]
            [lde.db :as db]))

(defn create [data ctx]
  (let [enc-pw (auth/encrypt (get data "password"))
        user (-> data
                 (assoc "password" enc-pw)
                 (db/save-user ctx))
        token (auth/gen-token (:uuid user))]
    {:token token}))

(comment

  (let [ctx (db/init "./db")]
    (create {"password" "hi"} ctx))

  (= (capture ctx [save-user (fn [x] {:uuid "uuid"})
                   gen-token (fn [x] {:uuid "uuid"})]
              (= {:token "123"} (create {"password" "hi"} ctx)))
     {:save-user [{"password" "hi"}]
      :gen-token ["uuid"]})

  (with-redefs [db/create-user (fn [x] {:uuid "123"})]
    (= {:token "123"} (create {"password" "hi"})))

  )
