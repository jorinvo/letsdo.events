(ns lde.web.forms.topic
  (:require
    [ring.util.response :as response]
    [lde.web.util :refer [multipart-image-to-data-uri]]
    [lde.web.error :as error]
    [lde.core.topic :as topic]
    [lde.core.invite :as invite]))

(defn post [{:keys [ctx session parameters]}]
  (let [topic (-> (:multipart parameters)
                  (assoc :creator (:id session))
                  (update :image multipart-image-to-data-uri)
                  (topic/create ctx))]
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn post-edit [{:keys [ctx topic parameters]}]
  (let [new-topic (-> (:multipart parameters)
                      (update :image multipart-image-to-data-uri)
                      (update :delete-image #(= "true" %))
                      (topic/update (:id topic) ctx))]
    (response/redirect (str "/for/" (:topic/slug new-topic)) :see-other)))

(defn delete [{:keys [ctx topic]}]
  (topic/delete ctx (:id topic))
  (response/redirect "/" :see-other))

(defn post-invite [{:keys [topic ctx session parameters]}]
  (let [invite (-> (:form parameters)
                   (assoc :inviter (:id session))
                   (invite/create topic ctx))
        link (str "/for/" (:topic/slug topic) "/invites")]
    (if (= :already-member invite)
      (error/render {:status 409
                     :title "User already joined"
                     :link link
                     :link-text "Go back to invites"}
                    ctx)
     (response/redirect link :see-other))))

(defn post-delete-invite [{:keys [topic ctx]
                           {invite-id :invite} :path-params}]
  (invite/delete invite-id ctx)
  (response/redirect (str "/for/" (:topic/slug topic) "/invites") :see-other))

(defn accept-invite [{:keys [topic session ctx]}]
  (invite/accept {:user-id (:id session)
                  :topic-id (:id topic)}
                 ctx)
  (response/redirect (str "/for/" (:topic/slug topic)) :see-other))
