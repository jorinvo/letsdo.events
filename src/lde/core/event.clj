(ns lde.core.event
  (:refer-clojure :exclude [update])
  (:require
    [clojure.set :refer [rename-keys]]
    [clojure.spec.alpha :as s]
    [clojure.test.check.generators :as gen]
    [java-time :as time]
    [cuerdas.core :as cuerdas]
    [lde.core.image :as image]
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
                 :creator :event/creator
                 :topic :event/topic})

(def updatable-event-keys
  (select-keys event-keys [:name
                           :description
                           :location
                           :max-attendees
                           :start-date
                           :start-time
                           :end-date
                           :end-time]))

(s/def ::maybe-local-date-str
  (s/spec #(or (nil? %)
               (try (time/local-date %) true
                    (catch Exception e false)))
          :gen
          (fn [] (gen/one-of
                   [(s/gen (s/spec nil?))
                    (gen/fmap #(-> % time/instant str (subs 0 10))
                              (s/gen (s/spec inst?)))]))))

(s/def ::maybe-local-time-str
  (s/spec #(or (nil? %)
               (try (time/local-time  %) true
                    (catch Exception e false)))
          :gen
          (fn [] (gen/one-of
                   [(s/gen (s/spec nil?))
                    (gen/fmap #(-> % time/instant str (subs 11 16))
                                 (s/gen (s/spec inst?)))]))))

(s/def :event/start-date ::maybe-local-date-str)
(s/def :event/start-time ::maybe-local-time-str)
(s/def :event/end-date ::maybe-local-date-str)
(s/def :event/end-time ::maybe-local-time-str)
(s/def ::event (s/keys :opt [:event/start-date
                             :event/start-time
                             :event/end-date
                             :event/end-time]))

(defn- unique-slug [event-name ctx]
  (let [base (cuerdas/slug event-name)]
    (if (db/exists-by-attribute ctx :event/slug base)
      (loop [n 2]
        (let [slug (str base "-" n)]
          (if (db/exists-by-attribute ctx :event/slug slug)
            (recur (inc n))
            slug)))
      base)))

(defn- empty-vals-to-nil [o]
  (->> o
       (remove (fn [[k v]] (or (= "" v) (nil? v))))
       (into {})))

(defn create [data ctx]
  (db/tx ctx
         (let [image (image/new-entity-from-data (:image data) ctx)
               event (-> data
                         (select-keys (keys event-keys))
                         (rename-keys event-keys)
                         (assoc :id (db/id)
                                :event/slug (unique-slug (:name data) ctx)
                                :event/image (:id image))
                         empty-vals-to-nil)
               {intention :intention} data]
           (->> [event
                 image
                 {:id (db/id)
                  (keyword intention "event") (:id event)
                  (keyword intention "user") (:event/creator event)}]
                (db/save-multi! ctx)
                first))))

(defn update [data event-id ctx]
  (db/tx ctx
         (when-let [existing-event (db/get-by-id ctx event-id)]
           (let [image (image/new-entity-from-data (:image data) ctx)
           delete-image (:delete-image data)
           previous-image-id (:event/image existing-event)
           new-event (-> existing-event
                         (merge (-> data
                                    (select-keys (keys updatable-event-keys))
                                    (rename-keys updatable-event-keys)))
                         (clojure.core/update :event/image #(cond delete-image nil
                                                                  image (:id image)
                                                                  :else %))
                         empty-vals-to-nil)]
             (db/save! new-event ctx)
             (when-not (or delete-image (image/exists-by-hash? (:id image) ctx))
               (db/save! image ctx))
             new-event))))

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
      ffirst))

(defn get-by-id [ctx id]
  (-> (db/get-by-id ctx id)
      (assoc-attendee-count ctx)))

(defn list-by-topic [topic-id ctx]
  (->> (db/list-by-attribute ctx :event/topic topic-id)
       (map #(assoc-attendee-count % ctx))))

(defn joined? [ctx event-id user-id]
  (db/exists-by-attributes ctx {:attendee/event event-id
                                :attendee/user user-id}))
(defn organizer? [ctx event-id user-id]
  (db/exists-by-attributes ctx {:organizer/event event-id
                                :organizer/user user-id}))

(defn organize [ctx event-id user-id]
  (db/tx ctx
         (cond
           (organizer? ctx event-id user-id)
           :already-organizer

           :else
           (-> {:organizer/event event-id
                :organizer/user user-id}
               (db/create! ctx)))))

(defn join [ctx event-id user-id]
  (db/tx ctx
         (let [{:keys [:event/max-attendees :event/attendee-count]} (get-by-id ctx event-id)]
           (cond
             (joined? ctx event-id user-id)
             :already-joined

             (and max-attendees (>= attendee-count max-attendees))
             :full

             :else
             (do (-> {:attendee/event event-id
                      :attendee/user user-id}
                     (db/create! ctx))
                 :ok)))))

(defn leave [ctx event-id user-id]
  (db/tx ctx
         (let [attendee {:attendee/event event-id
                         :attendee/user user-id}]
           (db/delete-by-attributes! ctx attendee))))

(defn list-attached-ids [ctx event-id]
  (->> (db/q ctx {:find ['?id]
                   :where '[(or [?id :attendee/event e]
                                [?id :interest/event e]
                                [?id :organizer/event e])]
                   :args [{'e event-id}]})
       (map first)))

(defn delete [ctx event-id]
  (db/tx ctx (db/delete-by-ids! ctx (conj (list-attached-ids ctx event-id) event-id))))

(defn list-attached-ids-by-topic [ctx topic-id]
  (->> (db/q ctx {:find ['?id]
                   :where '[[e :event/topic t]
                            (or [?id :attendee/event e]
                                [?id :interest/event e]
                                [?id :organizer/event e])]
                   :args [{'t topic-id}]})
       (map first)))

(defn get-organizer-names-by-event-id [ctx event-id]
  (let [names (->> (db/q ctx {:find ['?name]
                        :where '[[o :organizer/event event-id]
                                 [o :organizer/user u]
                                 [u :user/name ?name]]
                        :args [{'event-id event-id}]})
             (map first))]
    (when-not (empty? names)
      names)))
