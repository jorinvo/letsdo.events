(ns dev.repl
  (:require
    [clojure.string :as str]
    [clojure.repl]
    [cider-nrepl.main :as cider]
    [aleph.http :as http]
    [byte-streams :as bs]
    [manifold.stream :as stream]
    [reitit.ring :as ring]
    [lde.web.router :as router]
    [lde.db :as db]
    [lde.core.settings :as settings]
    [lde.core.user :as user]
    [lde.config :refer [get-config]]))

(def config (get-config "config.dev.edn"))

(defn make-context []
  (db/init "./db"))

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

  (reload-browser)

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

)
