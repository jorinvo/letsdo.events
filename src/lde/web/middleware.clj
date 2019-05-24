(ns lde.web.middleware
  (:require
    [clojure.string :as str]
    [ring.util.response :as response]
    [lde.web.error :as error]
    [lde.core.topic :as topic]
    [lde.core.event :as event]))

(defn authorize-user [handler]
  (fn [{:as req {user-id :id} :session}]
    (if user-id
      (handler req)
      (response/redirect (str "/login?goto=" (:uri req))))))

; TODO For now all topics are still public
(defn authorize-topic-read [handler]
  (fn [{:as req :keys [topic ctx] {user-id :id} :session}]
    (if (or user-id
            (= :public (:topic/visibility topic)))
      (handler req)
      (error/render {:status 404
                     :title "Not found"}
                    ctx))))

(defn authorize-topic-edit [handler]
  (fn [{:as req :keys [topic ctx] {user-id :id} :session}]
    (if (topic/admin? ctx (:id topic) user-id)
      (handler req)
      (error/render {:status 403
                     :title "Not allowed"}
                    ctx))))

(defn authorize-event-edit [handler]
  (fn [{:as req :keys [event ctx] {user-id :id} :session}]
    (if (or (event/organizer? ctx (:id event) user-id)
            (= user-id (:event/creator event)))
      (handler req)
      (error/render {:status 403
                     :title "Not allowed"}
                    ctx))))

(defn load-topic [handler]
  (fn [{:as req :keys [path-params ctx]}]
    (if-let [topic (topic/get-by-slug (:topic path-params) ctx)]
      (handler (assoc req :topic topic))
      (error/render {:status 404
                     :title "Topic not found"}
                    ctx))))

(defn load-event [handler]
  (fn [{:as req :keys [path-params topic ctx]}]
    (if-let [event (event/get-by-topic-and-slug ctx (:id topic) (:event path-params))]
      (handler (assoc req :event event))
      (error/render {:status 404
                     :title (str (topic/singular topic) " not found")
                     :link (str "/for/" (:topic/slug topic))
                     :link-text (str "Go back to " (str/lower-case (topic/singular topic)) " overview")}
                    ctx))))

