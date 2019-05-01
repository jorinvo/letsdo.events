(ns lde.web.pages.topic
  (:require
    [clojure.set :refer [rename-keys]]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [lde.web :refer [render escape-with-br]]
    [lde.web.pages.event :refer [event-item]]
    [lde.core.topic :as topic]
    [lde.core.user :as user]
    [lde.core.event :as event]))

(def topic-visibility [{:value :public
                        :label "Anyone can see and participate in this topic"}
                       {:value :invite
                        :label "You need to be invited to topic"}
                       {:value :request
                        :label "You can request to join this topic"}])

(defn handler [req]
  (let [path (-> req get-match match->path)]
    (render
      {:title "Setup New Topic"
       :description "Hi"}
      [:div
       [:h1.f1
        "Setup new topic"]
       [:form {:action path :method "post"}
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
        "optional: < Image goes here >"
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

(def topic-keys {:name :topic/name
                 :type :topic/type
                 :visibility :topic/visibility
                 :description :topic/description})

(defn post-topic [{:keys [ctx params session]}]
  (let [topic (-> params
                  (rename-keys topic-keys)
                  (assoc :topic/creator (:id session))
                  (update :topic/visibility keyword)
                  (update :topic/type keyword)
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
              [:h1 (:topic/name topic)]]
             [:h2 (:topic/description topic)]
             [:div
              [:a {:href (str "/for/" (:topic/slug topic) "/new")}
               "New " (topic/singular topic)]]
             [:ul (map #(vector :li (event-item % topic user ctx)) events)]])))
