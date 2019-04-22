(ns lde.web.pages.topic
  (:require [reitit.core :refer [match->path]]
            [reitit.ring :refer [get-match]]
            [ring.util.response :as response]
            [lde.web :refer [render]]
            [lde.core.topic :as topic]))

(def topic-visibility [{:value "public"
                        :label "Anyone can see and participate in this topic"}
                       {:value "invite"
                        :label "You need to be invited to topic"}
                       {:value "request"
                        :label "You can request to join this topic"}])

(def topic-types [{:label "Activities" :value "activities"}
                  {:label "Talks" :value "talks"}
                  {:label "Meetups" :value "meetups"}
                  {:label "Events" :value "events"}])

(defn handler [req]
  (let [path (-> req get-match match->path)]
    (render
      {:title "Setup New Topic"
       :description "Hi"}
      [:div
       [:h1.f1
        "Setup new topic"]
       [:form {:action path :method "post"}
        [:label.name-field "Topic name: "
         [:input {:type "text"
                  :name "name"
                  :required true
                  :placeholder "Topic name"}]]
        [:br]
        (->> topic-visibility
             (map (fn [{:keys [value label]}]
                    [:label
                     [:input {:type "radio"
                              :name "visibility"
                              :required true
                              :value value}]
                     label
                     [:br]])))
        [:span {} "This topic is about: "]
        (->> topic-types
          (map (fn [{:keys [value label]}]
                 [:label
                  [:input {:type "radio"
                           :name "type"
                           :required true
                           :value value}]
                  " " label " "])))
        [:br]
        [:button {:type "submit"} "Create Topic"]]])))

(defn post-topic [{:keys [ctx params]}]
  (let [topic (topic/create params ctx)]
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn overview [{:keys [path-params ctx]}]
  (let [{title :topic/name} (topic/get-by-slug (:topic path-params) ctx)]
    (render {:title title
             :description "Hi"}
            [:span title])))
