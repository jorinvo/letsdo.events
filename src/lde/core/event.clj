(ns lde.core.event
  (:refer-clojure :exclude [update])
  (:require
    [clojure.set :refer [rename-keys]]
    [clojure.spec.alpha :as s]
    [clojure.test.check.generators :as gen]
    [java-time :as time]
    [lde.core.util :as util]
    [lde.core.image :as image]
    [lde.core.db :as db]))

(def intentions (array-map
                  :organizer
                  {:text "I will be the organizer"}
                  :interest
                  {:text "This is only something I'm interested in"}))

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

(comment
  (s/exercise ::event))

(defn- unique-slug [event-name ctx]
  (let [base (util/slug event-name)]
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
         (let [image (image/new-entity-from-data (:image data))
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
           (let [image (image/new-entity-from-data (:image data))
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
  (when event
    (assoc event :event/attendee-count
           (db/count-by-attribute ctx :attendee/event (:id event)))))

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

(defn assoc-ref-date [event]
  (if-let [ref-date (if-let [ed (:event/end-date event)]
                      (str ed "T" (or (:event/end-time event) "23:59"))
                      (when-let [sd (:event/start-date event)]
                        (str sd "T" (or (:event/start-time event) "00:00"))))]
    (assoc event :ref-date (time/local-date-time ref-date))
    event))

(defn map-mark-as-past [events]
  (let [rel (time/minus (time/local-date-time)
                        (time/days 2))]
    (map #(if-let [r (:ref-date %)]
            (assoc %
                   :past-related-to-what-date rel
                   :mark-as-past (time/before? r rel))
            %)
         events)))

(defn sort-by-upcoming
  "sort order: end date, start date, no date sorted by created, past"
  [events]
  (->> events
       (map assoc-ref-date)
       map-mark-as-past
       (map #(vector % (if-let [r (:ref-date %)]
                         (if (:mark-as-past %)
                           (str "c-" (time/duration r (:past-related-to-what-date %)))
                           (str "a-" r))
                         (str "b-" (time/instant (:created %))))))
       (sort-by second)
       (map first)))

(defn upcoming-by-topic [topic-id ctx]
  (->> (db/list-ids-by-attribute ctx :event/topic topic-id)
       (db/list-by-ids-with-timestamps ctx)
       sort-by-upcoming
       (map #(assoc-attendee-count % ctx))))

(defn mine-by-topic [topic-id user-id ctx]
  (->> (db/q ctx {:find ['?id]
                   :where '[[?id :event/topic t]
                            (or [x :attendee/event ?id]
                                [x :organizer/event ?id])
                            (or [x :attendee/user u]
                                [x :organizer/user u])]
                   :args [{'t topic-id
                           'u user-id}]})
       (map first)
       (db/list-by-ids-with-timestamps ctx)
       sort-by-upcoming
       (map #(assoc-attendee-count % ctx))))

(defn latest-by-topic [topic-id ctx]
  (->> (db/list-ids-by-attribute ctx :event/topic topic-id)
       (db/list-by-ids-with-timestamps ctx)
       (map assoc-ref-date)
       map-mark-as-past
       (sort-by :created #(compare %2 %1))
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
  (->> (db/q ctx {:find ['?name]
                  :where '[[o :organizer/event e]
                           [o :organizer/user u]
                           [u :user/name ?name]]
                  :args [{'e event-id}]})
       (map first)
       not-empty))
