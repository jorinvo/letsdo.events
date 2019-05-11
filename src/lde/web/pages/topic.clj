(ns lde.web.pages.topic
  (:refer-clojure :exclude [new])
  (:require
    [clojure.string :as str]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [lde.web :refer [render escape-with-br multipart-image-to-data-uri image-mime-types]]
    [lde.web.pages.event :refer [event-item]]
    [lde.core.topic :as topic]
    [lde.core.user :as user]
    [lde.core.event :as event]))

(defn new [req]
  (let [path (-> req get-match match->path)]
    (render
      {:title "Setup New Topic"
       :description "Hi"}
      [:div
       [:h1.f1
        "Setup new topic"]
       [:form {:action path
               :method "post"
               :enctype "multipart/form-data"}
        [:label "Topic name: "
         [:input {:type "text"
                  :name "name"
                  :required true
                  :placeholder "Topic name"}]]
        [:br]
        [:label "Description: "
         [:input {:type "text"
                  :name "description"
                  :placeholder "Description"}]]
        [:br]
        [:label "optional: Select an image"
         [:input {:type "file"
                  :name "image"
                  :accept (str/join ", " image-mime-types)}]]
        [:br]
        (->> topic/visibilities
             (map (fn [[value {:keys [label]}]]
                    [:label
                     [:input {:type "radio"
                              :name "visibility"
                              :required true
                              :value value}]
                     label
                     [:br]])))
        [:span {} "This topic is about: "]
        (->> topic/types
             (map (fn [[value {label :plural}]]
                    [:label
                     [:input {:type "radio"
                              :name "type"
                              :required true
                              :value value}]
                     " " label " "])))
        [:br]
        [:button {:type "submit"} "Create Topic"]
        " "
        [:a {:href "/"} "Cancel"]]])))

(defn post [{:keys [ctx session parameters]}]
  (let [topic (-> (:multipart parameters)
                  (assoc :creator (:id session))
                  (update :image multipart-image-to-data-uri)
                  (topic/create ctx))]
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn overview [{:keys [path-params ctx session]}]
  (let [topic (topic/get-by-slug (:topic path-params) ctx)
        topic-url (str "/for/" (:topic path-params))
        events (event/list-by-topic (:id topic) ctx)
        user (user/get-by-id ctx (:id session))]
    (render {:title (:topic/name topic)
             :description "Hi"}
            [:div
             [:a {:href topic-url}
              (when-let [image (:topic/image topic)]
               [:img {:src image
                      :alt "logo"}])
              [:h1 (:topic/name topic)]]
             [:h2 (:topic/description topic)]
             [:div
              [:a {:href (str "/for/" (:topic/slug topic) "/new")}
               "New " (topic/singular topic)]]
             [:ul (map #(vector :li (event-item % topic user ctx)) events)]])))
