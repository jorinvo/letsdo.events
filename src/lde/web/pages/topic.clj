(ns lde.web.pages.topic
  (:require
    [clojure.set :refer [rename-keys]]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [hiccup.core :refer [h]]
    [lde.web :refer [render escape-with-br]]
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

(defn- event-item [event topic ctx]
  (let [title (:event/name event)
        join-url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event) "/join")
        attendees 0]
    [:li
     "< image goes here >"
     [:h3 (h title)]
     (if-let [{organizer :user/name} (user/get-by-id ctx (:event/organizer event))]
       (str " by " (if (empty? organizer) "Anonymous" organizer))
       "there is no organizer yet! can you take over? < take over >")
     [:div
      "starting " (:event/start-date event) " at " (:event/start-time event)
      ", until " (:event/start-date event) " at " (:event/start-time event)]
     (when-let [l (:event/location event)]
       [:p (h l)])
     [:p (escape-with-br (:event/description event))]
     [:form {:action join-url :method "post"}
      [:button {:type "submit"} "Join " (topic/singular topic)]]
     attendees (when-let [m (:event/max-attendees event)] (str "/" m)) " attendees"]))

(defn overview [{:keys [path-params ctx]}]
  (let [topic (topic/get-by-slug (:topic path-params) ctx)
        events (event/list-by-topic (:id topic) ctx)]
    (render {:title (:topic/name topic)
             :description "Hi"}
            [:div
             [:h1 (:topic/name topic)]
             [:h2 (:topic/description topic)]
             [:div
              [:a {:href (str "/for/" (:topic/slug topic) "/new")}
               "New " (topic/singular topic)]]
             [:ul (map #(event-item % topic ctx) events)]])))
