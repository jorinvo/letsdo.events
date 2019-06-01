(ns lde.web.forms.event
  (:require
    [ring.util.response :as response]
    [lde.web.util :refer [multipart-image-to-data-uri]]
    [lde.core.event :as event]))

(defn post [{:keys [ctx parameters topic session]}]
  (let [user-id (:id session)
        multipart (:multipart parameters)
        event (-> multipart
                  (assoc :creator user-id
                         :topic (:id topic))
                  (update :max-attendees #(if (empty? %)
                                                  nil
                                                  (Integer/parseInt %)))
                  (update :image multipart-image-to-data-uri)
                  (event/create ctx))
        url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))]
    (response/redirect url :see-other)))

(defn post-edit [{:keys [ctx parameters event topic session]}]
  (let [multipart (:multipart parameters)
        new-event (-> multipart
                      (update :max-attendees #(if (empty? %)
                                                nil
                                                (Integer/parseInt %)))
                      (update :image multipart-image-to-data-uri)
                      (update :delete-image #(= "true" %))
                      (event/update (:id event) ctx))
        url (str "/for/" (:topic/slug topic) "/about/" (:event/slug new-event))]
    (response/redirect url :see-other)))

(defn organize [{:keys [ctx event topic session]}]
  (let [user-id (:id session)
        url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))]
    (case (event/organize ctx (:id event) user-id)
      :already-organizer (response/bad-request "you are already organizing this event!")
      (response/redirect url :see-other))))

(defn join [{:keys [ctx event topic session]}]
  (let [user-id (:id session)
        url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))]
    (case (event/join ctx (:id event) user-id)
      :full (response/bad-request "event full")
      (response/redirect url :see-other))))

(defn leave [{:keys [ctx event topic session]}]
  (let [url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))]
    (event/leave ctx (:id event) (:id session))
    (response/redirect url :see-other)))

(defn delete [{:keys [ctx event topic]}]
  (let [url (str "/for/" (:topic/slug topic))]
    (event/delete ctx (:id event))
    (response/redirect url :see-other)))
