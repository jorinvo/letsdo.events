(ns events.letsdo.dev
  (:require [aleph.http :as http]
            [byte-streams :as bs]
            [events.letsdo.server :refer [make-server]]
            [events.letsdo.config :refer [get-config]]))

(comment

(def config (get-config "config.dev.edn"))

(def server (make-server config))

(.close server)

(-> @(http/get (str "http://localhost:" (:port config) "/login"))
  :body
  bs/to-string)

  )
