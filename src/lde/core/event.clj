(ns lde.core.event
  (:require
    [clojure.set :refer [rename-keys]]
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(def intentions (array-map
                  :organizer
                  {:text "I will be the organizer"}
                  :interest
                  {:text "This is only somthing I'm interested in"}))

(def event-keys {:name :event/name
                 :description :event/description
                 :location :event/location
                 :max-attendees :event/max-attendees
                 :start-date :event/start-date
                 :start-time :event/start-time
                 :end-date :event/end-date
                 :end-time :event/end-time
                 :image :event/image
                 :creator :event/creator
                 :topic :event/topic})

(defn- unique-slug [event-name ctx]
  (let [base (cuerdas/slug event-name)]
    (if (db/exists-by-attribute ctx :event/slug base)
      (loop [n 2]
        (let [slug (str base "-" n)]
          (if (db/exists-by-attribute ctx :event/slug slug)
            (recur (inc n))
            slug)))
      base)))

(defn create [data ctx]
  (let [event (-> data
                  (select-keys (keys event-keys))
                  (rename-keys event-keys)
                  (assoc :id (db/id)
                         :event/slug (unique-slug (:name data) ctx)))
        {intention :intention} data]
  (->> [event
        {:id (db/id)
         (keyword intention "event") (:id event)
         (keyword intention "user") (:event/creator event)}]
       (db/save-multi ctx)
       first)))

(defn assoc-attendee-count [event ctx]
  (assoc event :event/attendee-count
         (db/count-by-attribute ctx :attendee/event (:id event))))

(defn get-by-topic-and-slug [ctx topic-id slug]
  (-> (db/get-by-attributes ctx {:event/topic topic-id
                                 :event/slug slug})
      (assoc-attendee-count ctx)))

(def slug-map {:topic :topic/slug
               :event :event/slug})

(defn get-id-from-slugs [ctx {:keys [event topic]}]
  (-> (db/q ctx {:find ['?id]
                 :where '[[topic-id :topic/slug topic-slug]
                          [?id :event/topic topic-id]
                          [?id :event/slug event-slug]]
                 :args [{'event-slug event
                         'topic-slug topic}]})
      first
      first))

(defn get-by-id [ctx id]
  (-> (db/get-by-id ctx id)
      (assoc-attendee-count ctx)))

(defn list-by-topic [topic-id ctx]
  (->> (db/list-by-attribute ctx :event/topic topic-id)
       (map #(assoc-attendee-count % ctx))))

(defn joined? [ctx event-id user-id]
  (db/exists-by-attributes ctx {:attendee/event event-id
                                :attendee/user user-id}))

(defn join [ctx event-id user-id]
  (let [{:keys [:event/max-attendees :event/attendee-count]} (get-by-id ctx event-id)]
    (cond
      (joined? ctx event-id user-id)
      :already-joined

      (and max-attendees (>= attendee-count max-attendees))
      :full

      :else
      (do (-> {:attendee/event event-id
            :attendee/user user-id}
           (db/save ctx))
          :ok))))

(defn leave [ctx event-id user-id]
  (let [attendee {:attendee/event event-id
                  :attendee/user user-id}]
    (db/delete-by-attributes ctx attendee)))
