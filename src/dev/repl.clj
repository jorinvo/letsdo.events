(ns dev.repl
  (:require
    [clojure.repl]
    [cider-nrepl.main :as cider]
    [aleph.http :as http]
    [byte-streams :as bs]
    [manifold.stream :as stream]
    [reitit.ring :as ring]
    [lde.web.router :as router]
    [lde.db :as db]
    [lde.core.settings :as settings]
    [lde.config :refer [get-config]]))

(def config (get-config "config.dev.edn"))

(defn make-context []
  (db/init "./db"))

(defonce ctx (atom (make-context)))

(defonce dev-websocket (atom nil))

(defn dev-websocket-handler [req]
  (let [s @(http/websocket-connection req)]
    (reset! dev-websocket s)))

(defn reload-browser []
  (when-let [s @dev-websocket]
    (when-not (stream/closed? s)
      (do (stream/put! s "reload")
          :browser-reloaded))))

(defn dev-handler [req]
  ((ring/ring-handler
     (ring/router
       [["/dev-websocket" {:get dev-websocket-handler}]])
     (router/init @ctx))
   req))

(defn make-dev-server []
  (http/start-server dev-handler config))

(defonce server (atom (make-dev-server)))

(defn reset []
  (db/close @ctx)
  (reset! ctx (make-context))
  (.close @server)
  (reset! server (make-dev-server)))

(defn -main []
  (cider/init))

(comment

  (reset)

  (reload-browser)

  (settings/get-cookie-secret @ctx)

  (-> @(http/get (str "http://localhost:" (:port config) "/login"))
      :body
      bs/to-string)

  (-> @(http/post (str "http://localhost:" (:port config) "/signup")
                  {:form-params {:email "hi" :password "name"}})
      :body
      bs/to-string)

)
