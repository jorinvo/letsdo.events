(ns lde.web.pages.event
  (:refer-clojure :exclude [new get])
  (:require
    [clojure.string :as str]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [hiccup.core :refer [h]]
    [lde.web :refer [render escape-with-br multipart-image-to-data-uri image-mime-types]]
    [lde.core.event :as event]
    [lde.core.topic :as topic]
    [lde.core.user :as user]))

(defn event-item [event topic user ctx]
  (let [title (:event/name event)
        event-url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))
        attendees (:event/attendee-count event)
        max-attendees (:event/max-attendees event)
        user-joined (event/joined? ctx (:id event) (:id user))]
    [:div
     (when-let [image (:event/image event)]
               [:img {:src image
                      :alt "event image"}])
     [:a {:href event-url}
      [:h3 (h title)]]
     (if-let [{organizer :user/name} (user/get-by-id ctx (:event/organizer event))]
       (str " by " (if (empty? organizer) "Anonymous" organizer))
       "there is no organizer yet! can you take over? < take over >")
     [:div
      "starting " (:event/start-date event) " at " (:event/start-time event)
      ", until " (:event/end-date event) " at " (:event/end-time event)]
     (when-let [l (:event/location event)]
       [:p (h l)])
     [:p (escape-with-br (:event/description event))]
     attendees
     (if max-attendees
       (str "/" max-attendees " " (if (= max-attendees 1) "attendee" "attendees"))
       (if (= attendees 1) " attendee" " attendees"))
     (cond
       user-joined
       [:div
        [:span " - including you!"]
        [:form {:action (str event-url "/leave") :method "post"}
         [:button {:type "submit"} "Leave " (topic/singular topic)]]]

       (and max-attendees (>= attendees max-attendees))
       [:span " No spot left!"]

       :else
       [:form {:action (str event-url "/join") :method "post"}
        [:button {:type "submit"} "Join " (topic/singular topic)]])]))

(defn get [{:keys [ctx path-params session]}]
  (let [topic-slug (:topic path-params)
        topic (topic/get-by-slug topic-slug ctx)
        event (event/get-by-topic-and-slug ctx
                                           (:id topic)
                                           (:event path-params))
        title (:event/name event)
        topic-url (str "/for/" topic-slug)
        user (user/get-by-id ctx (:id session))]
    (render
      {:title title
       :description "hi"}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       [:h2 (:topic/description topic)]
       (event-item event topic user ctx)])))

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
       [:form {:action path
               :method "post"
               :enctype "multipart/form-data"}
        [:label (topic/singular topic) " title: "
         [:input {:type "text"
                  :name "name"
                  :required true
                  :placeholder (str (topic/singular topic) " name")}]]
        [:br]
        (->> event/intentions
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
        [:label "optional: Select an image"
         [:input {:type "file"
                  :name "image"
                  :accept (str/join ", " image-mime-types)}]]
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

(defn post [{:keys [ctx parameters path-params session]}]
  (let [topic-slug (:topic path-params)
        topic (topic/get-by-slug topic-slug ctx)
        user-id (:id session)
        multipart (:multipart parameters)
        event (-> multipart
                  (assoc :creator user-id
                         :topic (:id topic))
                  (update :max-attendees #(if (empty? %)
                                                  nil
                                                  (Integer/parseInt %)))
                  (update :image multipart-image-to-data-uri)
                  (event/create ctx))
        url (str "/for/" topic-slug "/about/" (:event/slug event))]
    (response/redirect url :see-other)))

(defn join [{:keys [ctx path-params session]}]
  (let [event-id (event/get-id-from-slugs ctx path-params)
        user-id (:id session)
        url (str "/for/" (:topic path-params) "/about/" (:event path-params))]
    (case (event/join ctx event-id user-id)
      :full (response/bad-request "event full")
      (response/redirect url :see-other))))

(defn leave [{:keys [ctx path-params session]}]
  (let [event-id (event/get-id-from-slugs ctx path-params)
        url (str "/for/" (:topic path-params))]
    (event/leave ctx event-id (:id session))
    (response/redirect url :see-other)))
