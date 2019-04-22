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

(defn get-by-attribute [{:keys [::crux]} attr value]
  (let [db (crux/db crux)
        q (crux/q db {:find '[id]
                      :where [['id attr value]]})
        entity (crux/entity db (first (first q)))]
    (rename-keys entity {:crux.db/id :id})))

(defn set-setting [{:keys [::crux]} k v]
  (let [id (keyword "settings" (name k))]
    (crux/submit-tx crux [[:crux.tx/put id {:crux.db/id id
                                            :settings/value v}]])))

(defn get-setting [{:keys [::crux]} k]
  (let [db (crux/db crux)
        id (keyword "settings" (name k))
        q (crux/q db {:find '[value]
                      :where '[[id :settings/value value]]
                      :args [{:id id}]})]
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
