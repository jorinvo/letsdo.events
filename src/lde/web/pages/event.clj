(ns lde.web.pages.event
  (:refer-clojure :exclude [new get])
  (:require
    [clojure.set :refer [rename-keys]]
    [clojure.string :as str]
    [java-time :as time]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [hiccup.core :refer [h]]
    [lde.web :refer [render escape-with-br]]
    [lde.core.event :as event]
    [lde.core.interest :as interest]
    [lde.core.topic :as topic]
    [lde.core.user :as user]))

(def intentions (array-map
                  :organizer
                  {:text "I will be the organizer"}
                  :interest
                  {:text "This is only somthing I'm interested in"}))

(defn get [{:keys [ctx params path-params session]}]
  (let [e (event/get-by-slug (:event path-params) ctx)
        title (:event/name e)
        topic-slug (:topic path-params)
        topic (topic/get-by-slug topic-slug ctx)
        join-url (str "/for/" topic-slug "/about/" (:event/slug e) "/join")
        attendees 0]
    (render
      {:title title
       :description "hi"}
      [:div
       "< image goes here >"
       [:h1 (h title)]
       (if-let [{organizer :user/name} (user/get-by-id ctx (:event/organizer e))]
         (str " by " (if (empty? organizer) "Anonymous" organizer))
         "there is no organizer yet! can you take over? < take over >")
       [:div
        "starting " (:event/start-date e) " at " (:event/start-time e)
        ", until " (:event/start-date e) " at " (:event/start-time e)]
       (when-let [l (:event/location e)]
         [:p (h l)])
       [:p (escape-with-br (:event/description e))]
       [:form {:action join-url :method "post"}
        [:button {:type "submit"} "Join " (topic/singular topic)]]
       attendees (when-let [m (:event/max-attendees e)] (str "/" m)) " attendees"])))

(comment time/local-date-time (str (:start-date params) "T" (:start-time params)))

(defn new [{:as req :keys [path-params ctx]}]
  (let [path (-> req get-match match->path)
        topic (topic/get-by-slug (:topic path-params) ctx)
        title (str "Plan a new " (str/lower-case (topic/singular topic)))]
    (render
      {:title title
       :description "Hi"}
      [:div
       [:h1.f1 title]
       [:form {:action path :method "post"}
        [:label (topic/singular topic) " title: "
         [:input {:type "text"
                  :name "name"
                  :required true
                  :placeholder (str (topic/singular topic) " name")}]]
        [:br]
        (->> intentions
             (map (fn [[value {label :text}]]
                    [:label
                     [:input {:type "radio"
                              :name "intention"
                              :required true
                              :value value}]
                     label
                     [:br]])))
        [:textarea {:type "text"
                    :name "description"
                    :required true
                    :placeholder "Write a description"
                    :rows 10
                    :cols 50
                    }]
        [:br]
        "optional: < Image goes here >"
        [:br]
        "When "
        [:br]
        "Starting: "
        [:input {:type "date"
                 :name "start-date"
                 :placeholder "Date"}]
        [:input {:type "time"
                 :name "start-time"
                 :placeholder "Time"}]
        [:br]
        "Until: "
        [:input {:type "date"
                 :name "end-date"
                 :placeholder "Date"}]
        [:input {:type "time"
                 :name "end-time"
                 :placeholder "Time"}]
        [:br]
        [:label "Where "
         [:input {:type "text"
                  :name "location"
                  :placeholder "Location"}]]
        [:br]
        [:label "Max. number of attendees "
         [:input {:type "number"
                  :name "max-attendees"
                  :placeholder "Number"
                  :min 1
                  :step 1}]]
        [:br]
        [:button {:type "submit"} "Create " (topic/singular topic)]
        " "
        [:a {:href (str "/for/" (:topic/slug topic))} "Cancel"]]])))

(def event-keys {:name :event/name
                 :description :event/description
                 :location :event/location
                 :max-attendees :event/max-attendees
                 :start-date :event/start-date
                 :start-time :event/start-time
                 :end-date :event/end-date
                 :end-time :event/end-time})

(defn post [{:keys [ctx params path-params session]}]
  (let [topic-slug (:topic path-params)
        topic (topic/get-by-slug topic-slug ctx)
        user-id (:id session)
        organizer (when (= "organizer" (:intention params))
                    user-id)
        event (-> params
                  (rename-keys event-keys)
                  (assoc :event/creator user-id
                         :event/organizer organizer
                         :event/topic (:id topic))
                  (event/create ctx))
        url (str "/for/" topic-slug "/about/" (:event/slug event))]
    (when (= "interested" (:intention params))
      (interest/add ctx (:id event) user-id))
    (response/redirect url :see-other)))



