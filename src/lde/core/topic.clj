(ns lde.core.topic
  (:require
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(defn create [topic ctx]
  (-> topic
      (assoc :topic/slug (cuerdas/slug (:topic/name topic)))
      (db/save ctx)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :topic/slug slug))

(defn get-by-user [user-id ctx]
  (db/get-by-attribute ctx :topic/creator user-id))
