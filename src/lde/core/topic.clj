(ns lde.core.topic
  (:require
    [cuerdas.core :as cuerdas]
    [lde.db :as db]))

(defn create [params ctx]
  (let [data {:topic/name (:name params)
              :topic/slug (cuerdas/slug (:name params))
              :topic/type (:type params)
              :topic/visibility (:visibility params)}]
    (db/save data ctx)))

(defn get-by-slug [slug ctx]
  (db/get-by-attribute ctx :topic/slug slug))
