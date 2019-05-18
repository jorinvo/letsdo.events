(ns lde.web.js
  (:require
    [clojure.java.io :as io]
    [ring.util.response :as response]))

(defn handler [req]
  (-> (response/resource-response "public/js/script.js")
      (update :body slurp)))
