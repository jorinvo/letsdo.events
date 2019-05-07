(ns lde.core.event
  (:require
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(def intentions (array-map
                  :organizer
                  {:text "I will be the organizer"}
                  :interest
                  {:text "This is only somthing I'm interested in"}))

(defn unique-slug [event ctx]
  (let [base (cuerdas/slug (:event/name event))]
    (if (db/exists-by-attribute ctx :event/slug base)
      (loop [n 2]
        (let [slug (str base "-" n)]
          (if (db/exists-by-attribute ctx :event/slug slug)
            (recur (inc n))
            slug)))
      base)))

(defn create [event ctx]
  (-> event
      (assoc :event/slug (unique-slug event ctx))
      (db/save ctx)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :event/slug slug))

(defn list-by-topic [topic-id ctx]
  (db/list-by-attribute ctx :event/topic topic-id))

