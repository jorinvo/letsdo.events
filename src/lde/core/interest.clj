(ns lde.core.interest
  (:require
    [lde.db :as db]))

(defn add [ctx event-id user-id]
  (-> {:interest/event event-id
       :interest/user user-id}
      (db/save ctx)))

