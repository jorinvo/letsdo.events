(ns lde.core.image
  (:require
    [buddy.core.hash :as hash]
    [buddy.core.codecs :refer [bytes->hex]]
    [lde.db :as db]))

(defn get-by-hash [hash-id ctx]
  (:image/data (db/get-by-id ctx hash-id)))

(defn exists-by-hash? [hash-id ctx]
  (db/exists-by-id? ctx hash-id))

(defn new-entity-from-data
  "Generates a new entity from a base64 data string.
  Entity gets hash as id so it can be reused."
  [data]
  (when-not (nil? data)
    (let [hash-id (-> data hash/sha256 bytes->hex keyword)]
      {:id hash-id
       :image/data data})))
