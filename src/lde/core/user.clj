(ns lde.core.user
  (:require [lde.auth :as auth]))

(defn create [data {:keys [save-user gen-token]}]
  (let [enc-pw (auth/encrypt (get data "password"))
        user (-> data
                 (assoc "password" enc-pw)
                 save-user)
        token (gen-token (:uuid user))]
    {:token token}))

(comment

  (let [ctx {:save-user db/save-user
             :gen-token auth/gen-token}]
    (create {"password" "hi"} ctx))

  (= (capture ctx [save-user (fn [x] {:uuid "uuid"})
                   gen-token (fn [x] {:uuid "uuid"})]
              (= {:token "123"} (create {"password" "hi"} ctx)))
     {:save-user [{"password" "hi"}]
      :gen-token ["uuid"]})

  (with-redefs [db/create-user (fn [x] {:uuid "123"})]
    (= {:token "123"} (create {"password" "hi"})))

  )
