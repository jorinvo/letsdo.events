(ns lde.core.user
  (:require [lde.auth :as auth]
            [lde.db :as db]))

(defn create [data]
  (-> data
      (assoc "password" (auth/encrypt (get data "password")))
      db/create-user))

(comment
  (create {"password" "hi"}))
