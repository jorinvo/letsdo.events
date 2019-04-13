(ns events.letsdo.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn get-config [path]
  (-> path
      io/reader
      java.io.PushbackReader.
      edn/read))
