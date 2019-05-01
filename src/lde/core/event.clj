(ns lde.core.event
  (:require
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(def intentions (array-map
                  :organizer
                  {:text "I will be the organizer"}
                  :interest
                  {:text "This is only somthing I'm interested in"}))

(defn create [event ctx]
  (-> event
      (assoc :event/slug (cuerdas/slug (:event/name event)))
      (db/save ctx)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :event/slug slug))

(defn list-by-topic [topic-id ctx]
  (db/list-by-attribute ctx :event/topic topic-id))


