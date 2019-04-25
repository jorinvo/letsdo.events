(ns lde.core.topic
  (:require
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(def types (array-map
             :activities
             {:singular "Activity"
              :plural "Activities"}
             :talks
             {:singular "Talk"
              :plural "Talks"}
             :meetups
             {:singular "Meetup"
              :plural "Meetups"}
             :events
             {:singular "Event"
              :plural "Events"}))

(defn singular [topic]
  (-> topic :topic/type types :singular))

(defn create [topic ctx]
  (-> topic
      (assoc :topic/slug (cuerdas/slug (:topic/name topic)))
      (db/save ctx)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :topic/slug slug))

(defn list-by-user [user-id ctx]
  (db/list-by-attribute ctx :topic/creator user-id))
