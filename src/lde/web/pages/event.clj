(ns lde.web.pages.event
  (:refer-clojure :exclude [new get])
  (:require
    [clojure.string :as str]
    [java-time :as time]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [hiccup.core :refer [h]]
    [lde.web :refer [render escape-with-br multipart-image-to-data-uri image-mime-types]]
    [lde.core.event :as event]
    [lde.core.image :as image]
    [lde.core.topic :as topic]
    [lde.core.user :as user]))

(defn format-date [d]
  (let [ld (time/local-date d)]
    (time/format
      (str "E, d MMM"
           (when (not= (time/year ld) (time/year))
             " yyyy"))
      ld)))

(defn format-time [t]
  (time/format "kk:mm" (time/local-time t)))

(defn date-and-time [{:keys [:event/start-date
                             :event/start-time
                             :event/end-date
                             :event/end-time]}]
  (let [start-end-same-day (= start-date end-date)]
    (when (or start-date start-time end-date end-time)
      [:div
       [:small
        (when start-date
          (format-date start-date))
        (when start-time
          (str (when start-date
                 ", ")
               (format-time start-time)))
        (when (or (and end-date (not start-end-same-day))
                  end-time)
          " until ")
        (when (and end-date (not start-end-same-day))
          (str (format-date end-date)
               (when end-date
                 ", ")))
        (when end-time
          (format-time end-time))]])))

(comment

(require
  '[clojure.spec.alpha :as s]
  '[clojure.test.check.generators :as gen]
  '[clojure.pprint :refer [pprint]]
  '[hiccup.core :refer [html]])

(->> (gen/sample (s/gen (s/get-spec :lde.core.event/event)) 20)
     (map #(vector % (html (date-and-time %))))
     pprint)

)

(defn item [event topic user ctx]
  (let [title (:event/name event)
        event-url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))
        attendees (:event/attendee-count event)
        max-attendees (:event/max-attendees event)
        user-joined (event/joined? ctx (:id event) (:id user))
        user-is-organizer (event/organizer? ctx (:id event) (:id user))
        {:keys [:event/location]} event]
    [:div
     [:a {:href event-url}
      [:h3 (h title)]]
     (when-let [image (image/get-by-hash (:event/image event) ctx)]
       [:img.logo {:src image
                   :alt "event image"}])
     (date-and-time event)
     (when location
       [:small (h location)])
     [:p (escape-with-br (:event/description event))]
     (if-let [organizer-names (event/get-organizer-names-by-event-id ctx (:id event))]
       [:div
        [:small (str " by " (str/join ", " (map #(if (empty? %) "Anonymous" %) organizer-names)))]
        [:div
         [:small
          attendees
          (if max-attendees
            (str "/" max-attendees " " (if (= max-attendees 1) "attendee" "attendees"))
            (if (= attendees 1) " attendee" " attendees"))]
         (cond
           user-joined
           [:small
            " - including you!"
            [:form.inline {:action (str event-url "/leave") :method "post"}
             [:button.btn.btn-small {:type "submit"} "Leave " (topic/singular topic)]]]

           (and max-attendees (>= attendees max-attendees))
           [:small " No spot left!"]

           (or user-is-organizer
               (= (:id user) (:event/creator event)))
           [:a.btn.btn-small.inline {:href (str event-url "/edit")} "Edit"]

           (not user-is-organizer)
           [:form.inline {:action (str event-url "/join") :method "post"}
            [:button.btn.btn-small {:type "submit"} "Join " (topic/singular topic)]])]]
       [:small "There is no organizer yet! can you take over?"
        [:form.inline {:action (str event-url "/organize") :method "post"}
        [:button.btn.btn-small {:type "submit"} "Organize " (topic/singular topic)]]])]))

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
      ctx
      {:title title
       :description "hi"}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       [:h2 (:topic/description topic)]
       (item event topic user ctx)])))

(defn edit [{:keys [ctx path-params session]}]
  (let [topic-slug (:topic path-params)
        topic (topic/get-by-slug topic-slug ctx)
        event (event/get-by-topic-and-slug ctx
                                           (:id topic)
                                           (:event path-params))
        title (:event/name event)
        topic-url (str "/for/" topic-slug)
        user (user/get-by-id ctx (:id session))]
    (render
      ctx
      {:title title
       :description "hi"}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       [:h2 (:topic/description topic)]
       (if (or (event/organizer? ctx (:id event) (:id user))
               (= (:id user) (:event/creator event)))
         (edit-event event topic user ctx)
         )])))

(comment time/local-date-time (str (:start-date params) "T" (:start-time params)))

(defn new [{:as req :keys [path-params ctx]}]
  (let [path (-> req get-match match->path)
        topic (topic/get-by-slug (:topic path-params) ctx)
        title (str "Plan a new " (str/lower-case (topic/singular topic)))]
    (render
      ctx
      {:title title
       :description "Hi"}
      [:div
       [:h1.f1 title]
       [:form {:action path
               :method "post"
               :enctype "multipart/form-data"}
        [:div.form-field
         [:label [:div (topic/singular topic) " title" [:sup " *"]]
          [:input.input-field
           {:type "text"
            :name "name"
            :required true}]]]
        [:div.form-field
         (for [[value {label :text}] event/intentions]
           [:label.radio
            [:input {:type "radio"
                     :name "intention"
                     :required true
                     :value value}]
            label])]
        [:div.form-field
         [:label [:div "Description" [:sup " *"]]
          [:textarea.input-field.input-wide
           {:type "text"
            :name "description"
            :required true
            :rows 10
            :cols 50}]]]
        [:div.form-field.image-upload
         [:label "Select an image"
          [:div [:img#image-upload-preview {:alt "logo"
                                            :class "hide"}]
           [:span#image-upload-message.btn "click to select image"]]
          [:input#image-upload-input {:type "file"
                                      :name "image"
                                      :accept (str/join ", " image-mime-types)
                                      :class "hide"}]]
         [:span#image-upload-clear.btn {:class "hide"} "remove image"]]
        [:div.form-field
         [:label [:div "Starting"]]
         [:input.input-field.input-date
          {:type "date"
           :name "start-date"}]
         [:input.input-field.input-time
          {:type "time"
           :name "start-time"}]]
        [:div.form-field
         [:label [:div "Until"]]
         [:input.input-field.input-date
          {:type "date"
           :name "end-date"}]
         [:input.input-field.input-time
          {:type "time"
           :name "end-time"}]]
        [:div.form-field
         [:label [:div "Where"]
          [:input.input-field
           {:type "text"
            :name "location"}]]]
        [:div.form-field
         [:label [:div "Max. number of attendees"]
          [:input.input-field.input-small
           {:type "number"
            :name "max-attendees"
            :min 1
            :step 1}]]]
        [:button.btn {:type "submit"} "Create " (topic/singular topic)]
        [:a.cancel {:href (str "/for/" (:topic/slug topic))} "Cancel"]]])))

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

(defn post-edit [{:keys [ctx parameters path-params session]}]
  (let [event-id (event/get-id-from-slugs ctx path-params)
        topic-slug (:topic path-params)
        multipart (:multipart parameters)
        event (-> multipart
                  (update :max-attendees #(if (empty? %)
                                                  nil
                                                  (Integer/parseInt %)))
                  (update :image multipart-image-to-data-uri)
                  (update :delete-image #(= "true" %))
                  (event/update event-id ctx))
        url (str "/for/" topic-slug "/about/" (:event/slug event))]
    (response/redirect url :see-other)))

(defn organize [{:keys [ctx path-params session]}]
  (let [event-id (event/get-id-from-slugs ctx path-params)
        user-id (:id session)
        url (str "/for/" (:topic path-params) "/about/" (:event path-params))]
    (case (event/organize ctx event-id user-id)
      :already-organizer (response/bad-request "you are already organizing this event!")
      (response/redirect url :see-other))))

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

(defn delete [{:keys [ctx path-params]}]
  (let [event-id (event/get-id-from-slugs ctx path-params)
        url (str "/for/" (:topic path-params))]
    (event/delete ctx event-id)
    (response/redirect url :see-other)))
