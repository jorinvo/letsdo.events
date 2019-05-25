(ns dev.reload
  (:require
    [clojure.string :as str]
    [aleph.http :as http]
    [manifold.stream :as stream]))

(defonce websockets (atom []))

(def websocket-script
  "<script>
    devWs = new WebSocket(`ws://${location.host}/dev-websocket`)
    devWs.onmessage = event => {
      console.log(event.data)
      window.location.reload()
    }
  </script>
  </body>")

(defn websocket-handler [req]
  (let [s @(http/websocket-connection req)]
    (swap! websockets #(conj (remove stream/closed? %) s))))

(defn inject-websocket-script [req]
  (if-let [body (:body req)]
    (assoc req :body
           (str/replace-first body
                              "</body>"
                              websocket-script))
    req))

(defn reload-browser []
  (let [c (->> @websockets
               (remove stream/closed?)
               (map #(stream/put! % "reload"))
               count)]
    (if (< 0 c)
      (str c " browser" (when (< 1 c) "s") " reloaded")
      "no browser connected")))

(defn middleware [handler]
  (fn [req]
    (-> req
       ((if (and (= "/dev-websocket" (:uri req))
                 (= :get (:request-method req)))
          websocket-handler
          handler))
       inject-websocket-script)))


