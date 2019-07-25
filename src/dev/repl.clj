(ns dev.repl
  (:require
    [clojure.string :as str]
    [clojure.repl]
    [clojure.java.io :as io]
    [crux.api :as crux]
    [crux.decorators.aggregation.alpha :as aggr]
    [cider-nrepl.main :as cider]
    [aleph.http :as http]
    [byte-streams :as bs]
    [manifold.stream :as stream]
    [reitit.ring :as ring]
    [java-time :as time]
    [dev.reload :as reload]
    [lde.web :as web]
    [lde.core.db :as db]
    [lde.core.settings :as settings]
    [lde.core.event :as event]
    [lde.core.user :as user]
    [lde.core.topic :as topic]
    [lde.config :refer [get-config]]))

(defn make-context []
  (-> {:config (get-config)}
      (db/init)))

(defonce ctx (atom (make-context)))

(defn make-dev-server []
  (http/start-server (reload/middleware (fn [req]
                                          ((web/init @ctx) req)))
                     (:config @ctx)))

(defonce server (atom (make-dev-server)))

(defn stop []
  (db/close @ctx)
  (.close @server))

(defn start []
  (reset! ctx (make-context))
  (reset! server (make-dev-server)))

(defn reset []
  (stop)
  (start))

(defn -main []
  (cider/init ['cider.nrepl/cider-middleware]))

(comment

  (start)
  (stop)
  (reset)

  (clojure.repl/dir ring.util.response)

  (do (in-ns 'dev.reload) (reload-browser))

  (settings/get-cookie-secret @ctx)

  (-> @(http/get (str "http://localhost:" (:port config) "/login"))
      :body
      bs/to-string)

  (-> @(http/post (str "http://localhost:" (:port config) "/signup")
                  {:form-params {:email "hi" :password "name"}})
      :body
      bs/to-string)

  (-> @(http/post (str "http://localhost:" (:port config) "/new")
                  {:form-params {:name "hi"
                                 :visibility "invite"
                                 :type "event"}})
      :body
      bs/to-string)

  (db/get-by-attribute @ctx :topic/slug "hi")

  (crux/q (crux/db (:lde.core.db/crux @ctx))
          {:find '[n un]
           :where '[[e :event/topic t]
                    [e :event/name n]
                    [o :organizer/event e]
                    [o :organizer/user u]
                    [u :user/name un]]
           :args [{'t #uuid "a249f31c-b644-43c1-8fa9-15d9fddb8935"}]})

  (aggr/q (crux/db (:lde.core.db/crux @ctx))
          {:find [?id]
           :where '[[topic-id :topic/slug topic-slug]
                    [?id :event/topic topic-id]
                    [?id :event/slug event-slug]]
           :args [{'event-slug "hi"
                   'topic-slug "ho-2"}]})

  (db/count-by-attribute @ctx :attendee/event #uuid "e1d83332-d2f2-41db-9783-34c0c311d53")

  (aggr/q (crux/db (:lde.core.db/crux @ctx))
          {:aggr '{:partition-by []
                   :select {?count [0 (inc acc) ?attendee]}}
           :where [['?attendee :attendee/event #uuid "e1d83332-d2f2-41db-9783-34c0c311d53b"]]})

  (aggr/q (crux/db (:lde.core.db/crux @ctx))
          {:aggr '{:partition-by [?id]
                   :select {?attendee-count [0 (inc acc) ?attendee]}}
           :where '[[?id :event/slug ?slug]
                    [?attendee :attendee/event ?id]]
           :args [{'?slug "hi"}]})

  (aggr/q (crux/db (:lde.core.db/crux @ctx))
          {:aggr '{:partition-by [?id ?t]
                   :select {?attendee-count [0 (inc acc) ?attendee]}}
           :where '[[?id :event/slug ?slug]
                    [?attendee :attendee/event ?id]
                    [?id :event/topic ?x]
                    [?x :topic/name ?t]]
           :args [{'?slug "hi"}]})

  (aggr/q (crux/db (:lde.core.db/crux @ctx))
          '{:aggr {:partition-by [?tn]
                   :select {?ec [0 (inc acc) ?e]}}
            :where [[?t :topic/name ?tn]
                    [?e :event/topic ?t]]})

  (settings/get-jwt-secret @ctx)


(db/exists-by-id? @ctx :settings/cookie-secret)

)
