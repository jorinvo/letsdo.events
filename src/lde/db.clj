(ns lde.db
  (:refer-clojure :exclude [update])
  (:require [clojure.set :refer [rename-keys]]
            [crux.api :as crux]
            [crux.decorators.aggregation.alpha :as aggr])
  (:import [java.util UUID]
           [crux.api ICruxAPI]))

(defn id [] (UUID/randomUUID))

(defn init
  ([path]
   (init {} path))
  ([ctx path]
   (assoc ctx ::crux (crux/start-standalone-system
                     {:kv-backend "crux.kv.rocksdb.RocksKv"
                      :db-dir path}))))

(defn q [{:keys [::crux]} query]
  (crux/q (crux/db crux) query))

(defn close [{:keys [::crux]}]
  (.close crux))

(defn- crux->id [x] (rename-keys x {:crux.db/id :id}))
(defn- id->crux [x] (rename-keys x {:id :crux.db/id}))

(defn tx!
  "Submit a transaction to the DB.
  Ignores nil items."
  [{:keys [::crux]} transaction-list]
  (->> transaction-list
       (filterv some?)
       (crux/submit-tx crux)))

(defn save-multi
  "Creates a transaction-list for a list of entities.
  Each must have an :id.
  Ignores nil items."
  [entitiy-list]
  (->> entitiy-list
       (filterv some?)
       (mapv #(vector :crux.tx/put (:id %) (id->crux %)))))

(defn save-multi! [ctx entitiy-list]
  (->> (save-multi entitiy-list)
       (tx! ctx))
  entitiy-list)

(defn save [entity]
  (first (save-multi [entity])))

(defn save! [entity]
  (first (save-multi! [entity])))

(defn create [entity]
  (first (save-multi [(assoc entity :id (id))])))

(defn create! [entity ctx]
  (first (save-multi! ctx [(assoc entity :id (id))])))

(defn update [new-entity previous-entity]
  [:crux.tx/cas
   (:id new-entity)
   (id->crux previous-entity)
   (id->crux new-entity)])

(defn update! [new-entity previous-entity ctx]
  (->> [(update new-entity previous-entity)]
       (tx! ctx))
  new-entity)

(defn set-key [k v]
  [:crux.tx/put k {:crux.db/id k :value v}])

(defn set-key! [ctx k v]
  (tx! ctx [(set-key k v)]))

(defn delete-by-ids [ids]
  (->> ids
       (mapv #(vector :crux.tx/delete %))))

(defn delete-by-ids! [ctx ids]
  (->> (delete-by-ids ids)
       (tx! ctx)))

(defn delete-by-id [id]
  (delete-by-ids [id]))

(defn delete-by-id! [id ctx]
  (delete-by-ids! ctx [id]))

(defn delete-by-attributes! [ctx attrs]
  (->> (crux/q (crux/db (::crux ctx))
               {:find '[id]
                :where (mapv (fn [[attr value]]
                               ['id attr value]) attrs)})
       (map first)
       (mapv #(vector :crux.tx/delete %))
       (tx! ctx)))

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
  (-> (crux/db crux)
      (crux/q {:find '[?id]
               :where [['?id :crux.db/id id]]})
       first
       nil?
       not))

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
