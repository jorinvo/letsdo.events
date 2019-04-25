(ns lde.core.event
  (:require
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(defn create [event ctx]
  (-> event
      (assoc :event/slug (cuerdas/slug (:event/name event)))
      (db/save ctx)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :event/slug slug))

