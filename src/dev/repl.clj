(ns dev.repl
  (:require
    [clojure.string :as str]
    [clojure.repl]
    [crux.api :as crux]
    [crux.decorators.aggregation.alpha :as aggr]
    [cider-nrepl.main :as cider]
    [aleph.http :as http]
    [byte-streams :as bs]
    [manifold.stream :as stream]
    [reitit.ring :as ring]
    [lde.web.router :as router]
    [lde.db :as db]
    [lde.core.settings :as settings]
    [lde.core.event :as event]
    [lde.core.user :as user]
    [lde.core.topic :as topic]
    [lde.config :refer [get-config]]))

(def config (get-config))

(defn make-context []
  (db/init (:db-dir config)))

(defonce ctx (atom (make-context)))

(defonce dev-websockets (atom []))

(def dev-websocket-script
  "<script>
    devWs = new WebSocket(`ws://${location.host}/dev-websocket`)
    devWs.onmessage = event => {
      console.log(event.data)
      window.location.reload()
    }
  </script>
  </body>")

(defn dev-websocket-handler [req]
  (let [s @(http/websocket-connection req)]
    (swap! dev-websockets #(conj (remove stream/closed? %) s))))

(defn inject-dev-websocket-script [req]
  (if-let [body (:body req)]
    (assoc req :body
           (str/replace-first body
                              "</body>"
                              dev-websocket-script))
    req))

(defn reload-browser []
  (let [c (->> @dev-websockets
               (remove stream/closed?)
               (map #(stream/put! % "reload"))
               count)]
    (if (< 0 c)
      (str c " browser" (when (< 1 c) "s") " reloaded")
      "no browser connected")))

(defn dev-handler [req]
  (-> req
      ((if (and (= "/dev-websocket" (:uri req))
                (= :get (:request-method req)))
         dev-websocket-handler
         (router/init @ctx)))
      inject-dev-websocket-script))

(defn make-dev-server []
  (http/start-server dev-handler config))

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

  (do (in-ns 'dev.repl) (reload-browser))

  (db/get-by-email @ctx "hi@jorin.me")

  (user/login {:email "hi@jorin.me" :password "123"} @ctx)

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

(crux/q (crux/db (:lde.db/crux @ctx))
        {:find '[n un]
         :where '[[e :event/topic t]
                  [e :event/name n]
                  [o :organizer/event e]
                  [o :organizer/user u]
                  [u :user/name un]]
         :args [{'t #uuid "a249f31c-b644-43c1-8fa9-15d9fddb8935"}]})

(aggr/q (crux/db (:lde.db/crux @ctx))
        {:find [?id]
         :where '[[topic-id :topic/slug topic-slug]
                  [?id :event/topic topic-id]
                  [?id :event/slug event-slug]]
         :args [{'event-slug "hi"
                 'topic-slug "ho-2"}]})

(db/count-by-attribute @ctx :attendee/event #uuid "e1d83332-d2f2-41db-9783-34c0c311d53")

(aggr/q (crux/db (:lde.db/crux @ctx))
        {:aggr '{:partition-by []
                 :select {?count [0 (inc acc) ?attendee]}}
         :where [['?attendee :attendee/event #uuid "e1d83332-d2f2-41db-9783-34c0c311d53b"]]})

(aggr/q (crux/db (:lde.db/crux @ctx))
        {:aggr '{:partition-by [?id]
                :select {?attendee-count [0 (inc acc) ?attendee]}}
         :where '[[?id :event/slug ?slug]
                 [?attendee :attendee/event ?id]]
         :args [{'?slug "hi"}]})

(aggr/q (crux/db (:lde.db/crux @ctx))
        {:aggr '{:partition-by [?id ?t]
                :select {?attendee-count [0 (inc acc) ?attendee]}}
         :where '[[?id :event/slug ?slug]
                 [?attendee :attendee/event ?id]
                 [?id :event/topic ?x]
                 [?x :topic/name ?t]]
         :args [{'?slug "hi"}]})

(aggr/q (crux/db (:lde.db/crux @ctx))
        '{:aggr {:partition-by [?tn]
                :select {?ec [0 (inc acc) ?e]}}
         :where [[?t :topic/name ?tn]
                  [?e :event/topic ?t]]})

(crux/q (crux/db (:lde.db/crux @ctx))
        {:find ['?id]
            :where '[[topic-id :topic/slug topic-slug]
                     [?id :event/topic topic-id]
                     [?id :event/slug event-slug]]
            :args {'event-slug "a-is-fun-2"
                   'topic-slug "ho-2"}})

(user/create {:email "ho@jorin.me"} @ctx)

(user/login-with-token @ctx
                       (user/get-token-for-mail @ctx "hi@jorin.me"))

(settings/get-jwt-secret @ctx)


(event/get-organizer-names-by-event-id
  @ctx
  (event/get-id-from-slugs @ctx {:topic "heart-of-clojure-2019"
                                 :event "testing-jam"}))



(event/list-attached-ids-by-topic @ctx (:id (topic/get-by-slug "heart-of-clojure-2019" @ctx)))

(db/exists-by-id? @ctx :settings/cookie-secret)

)
