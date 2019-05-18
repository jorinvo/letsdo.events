(ns lde.db
  (:require [clojure.set :refer [rename-keys]]
            [clojure.core.async :as async]
            [crux.api :as crux]
            [crux.decorators.aggregation.alpha :as aggr])
  (:import [java.util UUID]
           [crux.api ICruxAPI]))

(defn id [] (UUID/randomUUID))

(defn init [ctx]
  (let [aquire (async/chan)
        release (async/chan)]
    (async/pipe release aquire)
    (async/>!! release :ok)
    (-> ctx
        (assoc ::crux (crux/start-standalone-system
                        {:kv-backend "crux.kv.rocksdb.RocksKv"
                         :db-dir (-> ctx :config :db-dir)})
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

(comment
  (def ctx (init "dbdb"))
  (close ctx)
  (tx ctx (str "a" "b"))
)

(defmacro tx [ctx & body]
  (let [result (gensym 'result)
        chan-response (gensym 'chan-response)]
    `(let [~chan-response (async/<!! (::aquire ~ctx))]
       (when (not= :ok ~chan-response)
         (throw (Exception. "DB closed. Transaction aborded.")))
       (reset! (::transaction ~ctx) [])
       (try
         (let [~result ~@body]
           (->> @(::transaction ~ctx)
                (filterv some?)
                (crux/submit-tx (::crux ~ctx)))
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
       (mapv #(vector :crux.tx/put (:id %) (id->crux %)))
       (submit! ctx))
  entitiy-list)

(defn save! [entity ctx]
  (first (save-multi! ctx [entity])))

(defn create! [entity ctx]
  (first (save-multi! ctx [(assoc entity :id (id))])))

(defn update! [new-entity previous-entity ctx]
  (->> [[:crux.tx/cas
         (:id new-entity)
         (id->crux previous-entity)
         (id->crux new-entity)]]
       (submit! ctx))
  new-entity)

(defn set-key! [ctx k v]
  (submit! ctx [[:crux.tx/put k {:crux.db/id k :value v}]]))

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
  (-> (crux/db crux)
      (aggr/q {:aggr '{:partition-by []
                        :select {?count [0 (inc acc) ?id]}}
                :where (mapv (fn [[attr value]]
                               ['?id attr value]) attrs)})
       first
       (get :count 0)))

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
  (< 0 (count-by-attributes ctx attrs)))

(defn exists-by-attribute [ctx attr value]
  (exists-by-attributes ctx {attr value}))

(defn get-by-id [{:keys [::crux]} id]
  (-> (crux/db crux)
      (crux/entity id)
      crux->id))

(defn get-key [{:keys [::crux]} k]
  (let [db (crux/db crux)
        q (crux/q db {:find '[value]
                      :where '[[id :value value]]
                      :args [{'id k}]})]
    (first (first q))))

(comment

  (.close (::crux ctx))

  (def ctx (init "db"))

  (save ctx {:email "abCd@de.com"})
  (< 0 (count (find-by-email ctx "hi@jorin.me")))

  (let [db (crux/db (::crux ctx))]
    (map #(crux/entity db (first %))
         (crux/q db '{:find [e]
                      :where [[e :email "abCd@de.com"]]})))

  (save (init "./db") {"email" "abCd@de.com"})
  (save (init "./db") {:password nil})
  (->> (jdbc/query db "select * from users") (map :email))
  (jdbc/query db "show tables")
  (jdbc/query db "show columns from users")
  (jdbc/execute! db table-users)
  (jdbc/execute! db "drop table users")

)
