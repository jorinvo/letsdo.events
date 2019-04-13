(ns events.letsdo.dev
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [events.letsdo.server :refer [make-server]]
            [events.letsdo.config :refer [get-config]]))

(def config (get-config "config.edn"))
(def server (make-server config))


(-> @(http/get (str "http://localhost:" (:port config)))
  :body
  bs/to-string
  prn)
