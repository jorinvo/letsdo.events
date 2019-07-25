(ns lde.core.db
  (:require [clojure.set :refer [rename-keys]]
            [clojure.core.async :as async]
            [crux.api :as crux]
            [crux.decorators.aggregation.alpha :as aggr])
  (:import [java.util UUID]
           [crux.api ICruxAPI]))

(defn id [] (UUID/randomUUID))

(defn init
  "Initializes the DB.
  Adds DB state to given ctx."
  [{:as ctx {:keys [db-dir event-log-dir]} :config}]
  (let [aquire (async/chan)
        release (async/chan)]
    (async/pipe release aquire)
    (async/>!! release :ok)
    (-> ctx
        (assoc ::crux (crux/start-standalone-node
                        {:kv-backend "crux.kv.rocksdb.RocksKv"
                         :db-dir db-dir
                         :event-log-dir event-log-dir})
               ::aquire aquire
               ::release release
               ::transaction (atom nil)))))

(defn q [{:keys [::crux]} query]
  (crux/q (crux/db crux) query))

(defn close [{:keys [::crux ::release ::aquire]}]
  (.close crux)
  (async/close! release)
  (async/<!! aquire))

(defn- crux->id [x] (rename-keys x {:crux.db/id :id}))
(defn- id->crux [x] (rename-keys x {:id :crux.db/id}))

(defmacro tx
  "Execute body as DB transaction.
  All DB write need to be wrappen with tx.
  Only one tx can run at a time.
  Having a single transactor for DB writes allows you to specify
  all kinds of constraints of your DB writes as simple queries within a transaction.

  Example:

  ```
  (let [ctx (init {:config config})]
    (tx ctx (if (exists-by-attribute :user/email mail)
              :email-taken
              (create! {:user/email mail} ctx)))
    (close ctx))
  ```
  "
  [ctx & body]
  (let [result (gensym 'result)
        txs (gensym 'txs)
        chan-response (gensym 'chan-response)]
    `(let [~chan-response (async/<!! (::aquire ~ctx))]
       (when (not= :ok ~chan-response)
         (throw (Exception. "DB closed. Transaction aborded.")))
       (reset! (::transaction ~ctx) [])
       (try
         (let [~result ~@body
               ~txs (->> @(::transaction ~ctx)
                         (filterv some?))]
           (when-not (empty? ~txs)
             (crux/submit-tx (::crux ~ctx) ~txs))
           (reset! (::transaction ~ctx) nil)
           ~result)
         (finally (async/>!! (::release ~ctx) :ok))))))

(defn submit!
  "Submit a transaction to the DB.
  Ignores nil items."
  [ctx transaction]
  (swap! (::transaction ctx)
         #(if (nil? %)
            (throw (Exception. "DB writes must be wrapped transaction"))
            (concat % transaction))))

(defn save-multi!
  "Creates a transaction for a list of entities.
  Each must have an :id.
  Ignores nil items."
  [ctx entitiy-list]
  (->> entitiy-list
       (filterv some?)
       (mapv #(vector :crux.tx/put (id->crux %)))
       (submit! ctx))
  entitiy-list)

(defn save! [entity ctx]
  (first (save-multi! ctx [entity])))

(defn create! [entity ctx]
  (first (save-multi! ctx [(assoc entity :id (id))])))

(defn update! [new-entity previous-entity ctx]
  (->> [[:crux.tx/cas
         (id->crux previous-entity)
         (id->crux new-entity)]]
       (submit! ctx))
  new-entity)

(defn set-key! [ctx k v]
  (submit! ctx [[:crux.tx/put {:crux.db/id k :value v}]]))

(defn delete-by-ids! [ctx ids]
  (->> ids
       (mapv #(vector :crux.tx/delete %))
       (submit! ctx)))

(defn delete-by-id! [id ctx]
  (delete-by-ids! ctx [id]))

(defn delete-by-attributes! [ctx attrs]
  (->> (crux/q (crux/db (::crux ctx))
               {:find '[id]
                :where (mapv (fn [[attr value]]
                               ['id attr value]) attrs)})
       (map first)
       (mapv #(vector :crux.tx/delete %))
       (submit! ctx)))

(defn list-ids-by-attributes [{:keys [::crux]} attrs]
  (let [db (crux/db crux)]
    (->> (crux/q db {:find '[id]
                     :where (mapv (fn [[attr value]]
                                    ['id attr value]) attrs)})
         (map first))))

(defn list-ids-by-attribute [ctx attr value]
  (list-ids-by-attributes ctx {attr value}))

(defn get-id-by-attributes [ctx attrs]
  (first (list-ids-by-attributes ctx attrs)))

(defn get-id-by-attribute [ctx attr value]
  (first (list-ids-by-attribute ctx attr value)))

(defn list-by-attributes [{:keys [::crux]} attrs]
  (let [db (crux/db crux)]
    (->> (crux/q db {:find '[id]
                     :where (mapv (fn [[attr value]]
                                    ['id attr value]) attrs)})
         (map first)
         (map #(crux/entity db %))
         (map crux->id))))

(defn list-by-attribute [ctx attr value]
  (list-by-attributes ctx {attr value}))

(defn get-by-attributes [ctx attrs]
  (first (list-by-attributes ctx attrs)))

(defn get-by-attribute [ctx attr value]
  (first (list-by-attribute ctx attr value)))

(defn count-by-attributes [{:keys [::crux]} attrs]
  (let [valid-attrs (->> attrs
                         (remove #(nil? (second %)))
                         (mapv (fn [[attr value]]
                                 ['?id attr value])))]
    (if (empty? valid-attrs)
      0
      (-> (crux/db crux)
          (aggr/q {:aggr '{:partition-by []
                           :select {?count [0 (inc acc) ?id]}}
                   :where valid-attrs})
          first
          (get :count 0)))))

(defn count-by-attribute [ctx attr value]
  (count-by-attributes ctx {attr value}))

(defn exists-by-id? [{:keys [::crux]} id]
  (if id
    (-> (crux/db crux)
        (crux/q {:find '[?id]
                 :where [['?id :crux.db/id id]]})
        first
        nil?
        not)
    false))

(defn exists-by-attributes [ctx attrs]
  (if (some nil? (vals attrs))
    false
    (< 0 (count-by-attributes ctx attrs))))

(defn exists-by-attribute [ctx attr value]
  (exists-by-attributes ctx {attr value}))

(defn list-by-ids [{:keys [::crux]} ids]
  (let [db (crux/db crux)]
    (->> ids
         (map #(crux->id (crux/entity db %))))))

(defn get-by-id [ctx id]
  (first (list-by-ids ctx [id])))

(defn list-by-ids-with-timestamps [{:keys [::crux]} ids]
  (let [db (crux/db crux)]
    (->> ids
         (map #(let [h (crux/history crux %)]
                 (assoc (crux->id (crux/entity db %))
                       :created (-> h last :crux.db/valid-time)
                       :updated (-> h first :crux.db/valid-time)))))))

(defn get-by-id-with-timestamps [ctx id]
  (first (list-by-ids-with-timestamps ctx [id])))

(defn get-key [{:keys [::crux]} k]
  (let [db (crux/db crux)
        q (crux/q db {:find '[value]
                      :where '[[id :value value]]
                      :args [{'id k}]})]
    (ffirst q)))
