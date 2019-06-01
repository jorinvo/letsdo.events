(ns lde.web.forms.topic
  (:require
    [ring.util.response :as response]
    [lde.web.util :refer [multipart-image-to-data-uri]]
    [lde.core.topic :as topic]))

(defn post [{:keys [ctx session parameters]}]
  (let [topic (-> (:multipart parameters)
                  (assoc :creator (:id session))
                  (update :image multipart-image-to-data-uri)
                  (topic/create ctx))]
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn post-edit [{:keys [ctx session topic parameters]}]
  (let [new-topic (-> (:multipart parameters)
                      (update :image multipart-image-to-data-uri)
                      (update :delete-image #(= "true" %))
                      (topic/update (:id topic) ctx))]
    (response/redirect (str "/for/" (:topic/slug new-topic)) :see-other)))

(defn delete [{:keys [ctx topic]}]
  (topic/delete ctx (:id topic))
  (response/redirect "/" :see-other))

