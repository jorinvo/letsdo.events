(ns dev.repl
  (:require
    [clojure.repl]
    [cider-nrepl.main :as cider]
    [aleph.http :as http]
    [byte-streams :as bs]
    [manifold.stream :as stream]
    [reitit.ring :as ring]
    [lde.router :refer [make-handler]]
    [lde.config :refer [get-config]]))

(def config (get-config "config.dev.edn"))

(defonce dev-websocket (atom nil))

(defn dev-websocket-handler [req]
  (let [s @(http/websocket-connection req)]
    (reset! dev-websocket s)))

(defn reload-browser []
  (when-let [s @dev-websocket]
    (when-not (stream/closed? s)
      (do (stream/put! s "reload")
          :ok))))

(defn dev-handler [req]
  ((ring/ring-handler
     (ring/router
       [["/dev-websocket" {:get dev-websocket-handler}]])
     (make-handler))
   req))

(defn make-dev-server []
  (http/start-server dev-handler config))

(defonce server (atom (make-dev-server)))

(defn -main []
  (cider/init))

(comment

  (do
    (.close @server)
    (reset! server (make-dev-server)))

  (reload-browser)

  (-> @(http/get (str "http://localhost:" (:port config) "/login"))
      :body
      bs/to-string)

)
