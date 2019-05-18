(ns lde.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def default-config
  {:port 3000
   :db-dir "./db"})

(defn get-config [path]
  (if path
    (-> path
       io/reader
       java.io.PushbackReader.
       edn/read)
    default-config))
