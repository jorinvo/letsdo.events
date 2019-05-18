(ns lde.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def default-config
  {:port 3000
   :db-dir "./db"
   :style {:background-color  "#ffffff"
           :primary-color  "rgb(226, 95, 125)"
           :text-color  "#000000"
           :title-font  "'Halant', serif"
           :base-font  "'Nunito', sans-serif"
           :additional-stylesheets ["https://fonts.googleapis.com/css?family=Halant|Nunito&display=swap"]}
   })

(defn get-config
  ([]
   (get-config nil))
  ([path]
   (if path
     (-> path
         io/reader
         java.io.PushbackReader.
         edn/read)
     default-config)))
