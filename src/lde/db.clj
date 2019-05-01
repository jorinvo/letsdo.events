(ns lde.db
  (:require [clojure.set :refer [rename-keys]]
            [crux.api :as crux])
  (:import [java.util UUID]
           [crux.api ICruxAPI]))

(defn init
  ([path]
   (init {} path))
  ([ctx path]
   (assoc ctx ::crux (crux/start-standalone-system
                     {:kv-backend "crux.kv.rocksdb.RocksKv"
                      :db-dir path}))))

(defn close [{:keys [::crux]}]
  (.close crux))

(defn save [data {:keys [::crux]}]
  (let [id (UUID/randomUUID)
        op [:crux.tx/put id (assoc data :crux.db/id id)]]
    (crux/submit-tx crux [op])
    (assoc data :id id)))

(defn list-by-attributes [{:keys [::crux]} attrs]
  (let [db (crux/db crux)]
    (->> (crux/q db {:find '[id]
                     :where (mapv (fn [[attr value]]
                                    ['id attr value]) attrs)})
         (map first)
         (map #(crux/entity db %))
         (map #(rename-keys % {:crux.db/id :id})))))

(defn list-by-attribute [ctx attr value]
  (list-by-attributes ctx {attr value}))

(defn get-by-attributes [ctx attrs]
  (first (list-by-attributes ctx attrs)))

(defn get-by-attribute [ctx attr value]
  (first (list-by-attribute ctx attr value)))

(defn count-by-attributes [{:keys [::crux]} attrs]
  (->> (crux/q (crux/db crux) {:find '[id]
                               :where (mapv (fn [[attr value]]
                                              ['id attr value]) attrs)})
       count))

(defn count-by-attribute [ctx attr value]
  (count-by-attributes ctx {attr value}))

(defn get-by-id [{:keys [::crux]} id]
  (-> (crux/entity (crux/db crux) id)
      (rename-keys {:crux.db/id :id})))

(defn set-key [{:keys [::crux]} k v]
  (crux/submit-tx crux [[:crux.tx/put k {:crux.db/id k :value v}]]))

(defn get-key [{:keys [::crux]} k]
  (let [db (crux/db crux)
        q (crux/q db {:find '[value]
                      :where '[[k :value value]]
                      :args [{:id k}]})]
    (first (first q))))

(comment

  (.close (::crux ctx))

  (def ctx (init "db"))

  (set-setting ctx :cookie-secret "aaa")
  (get-setting ctx :cookie-secret)

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
