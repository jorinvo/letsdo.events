(ns lde.core.topic
  (:require
    [clojure.set :refer [rename-keys]]
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(def visibilities (array-map
                  :public
                  {:label "Anyone can see and participate in this topic"}
                  :invite
                  {:label "You need to be invited to topic"}
                  :request
                  {:label "You can request to join this topic"}))

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

(defn unique-slug [topic-name ctx]
  (let [base (cuerdas/slug topic-name)]
    (if (db/exists-by-attribute ctx :topic/slug base)
      (loop [n 2]
        (let [slug (str base "-" n)]
          (if (db/exists-by-attribute ctx :topic/slug slug)
            (recur (inc n))
            slug)))
      base)))

(def topic-keys {:name :topic/name
                 :creator :topic/creator
                 :type :topic/type
                 :visibility :topic/visibility
                 :description :topic/description
                 :image :topic/image})

(defn create [data ctx]
  (let [topic (-> data
            (select-keys (keys topic-keys))
            (rename-keys topic-keys)
            (update :topic/visibility keyword)
            (update :topic/type keyword)
            (assoc :id (db/id)
                   :topic/slug (unique-slug (:name data) ctx)))]
    (->> [topic
        {:id (db/id)
         :admin/topic (:id topic)
         :admin/user (:creator data)}]
       (db/save-multi ctx)
       first)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :topic/slug slug))

(defn list-by-user [user-id ctx]
  (db/list-by-attribute ctx :topic/creator user-id))

(defn admin? [ctx topic-id user-id]
  (db/exists-by-attributes ctx {:admin/topic topic-id
                                :admin/user user-id}))
