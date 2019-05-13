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

(defn save-multi [{:keys [::crux]} data-list]
  (->> data-list
       (mapv #(vector :crux.tx/put (:id %) (id->crux %)))
       (crux/submit-tx crux))
  data-list)

(defn save [data ctx]
  (let [with-id (assoc data :id (id))]
    (first (save-multi ctx [with-id]))))

(defn update [new-data previous-data {:keys [::crux]}]
  (->> [[:crux.tx/cas
         (:id new-data)
         (id->crux previous-data)
         (id->crux new-data)]]
       (crux/submit-tx crux))
  new-data)

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

(defn exists-by-attributes [ctx attrs]
  (< 0 (count-by-attributes ctx attrs)))

(defn exists-by-attribute [ctx attr value]
  (exists-by-attributes ctx {attr value}))

(defn get-by-id [{:keys [::crux]} id]
  (rename-keys (crux/entity (crux/db crux) id)
               {:crux.db/id :id}))

(defn set-key [{:keys [::crux]} k v]
  (crux/submit-tx crux [[:crux.tx/put k {:crux.db/id k :value v}]]))

(defn get-key [{:keys [::crux]} k]
  (let [db (crux/db crux)
        q (crux/q db {:find '[value]
                      :where '[[id :value value]]
                      :args [{'id k}]})]
    (first (first q))))

(defn delete-by-attributes [{:keys [::crux]} attrs]
  (->> (crux/q (crux/db crux)
               {:find '[id]
                :where (mapv (fn [[attr value]]
                               ['id attr value]) attrs)})
       (map first)
       (mapv #(vector :crux.tx/delete %))
       (crux/submit-tx crux)))

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
