(ns lde.db
  (:require [crux.api :as crux])
  (:import [java.util UUID]
           [crux.api ICruxAPI]))

(defn init
  ([path]
   (init {} path))
  ([ctx path]
   (assoc ctx ::crux (crux/start-standalone-system
                     {:kv-backend "crux.kv.rocksdb.RocksKv"
                      :db-dir path}))))

(def table-users
  "create table if not exists users (
    id                identity,
    uuid              uuid default uuid(),
    name              varchar(255),
    email             varchar_ignorecase(255) unique,
    link              varchar(1023),
    created           timestamp not null default current_timestamp,
    password          varchar(1023),
    password_updated  timestamp not null default current_timestamp,
  )")

(defn save-user [{:keys [::crux]} data]
  (let [id (UUID/randomUUID)
        op [:crux.tx/put id (assoc data :crux.db/id id)]]
    (crux/submit-tx crux [op])
    (assoc data :id id)))

(defn find-by-email [{:keys [::crux]} email]
  (let [db (crux/db crux)
        q (crux/q db {:find '[id email]
                      :where '[[id :email email]]
                      :args [{:email email}]})]
    q))

(comment

  (.close (::crux ctx))

  (def ctx (init "db"))

  (save-user ctx {:email "abCd@de.com"})
  (find-by-email ctx "abCd@de.com")

  (let [db (crux/db (::crux ctx))]
    (map #(crux/entity db (first %))
         (crux/q db '{:find [e]
                      :where [[e :email "abCd@de.com"]]})))

  (save-user (init "./db") {"email" "abCd@de.com"})
  (save-user (init "./db") {:password nil})
  (->> (jdbc/query db "select * from users") (map :email))
  (jdbc/query db "show tables")
  (jdbc/query db "show columns from users")
  (jdbc/execute! db table-users)
  (jdbc/execute! db "drop table users")
)
