(ns lde.web.pages.event
  (:refer-clojure :exclude [new get])
  (:require
    [clojure.string :as str]
    [java-time :as time]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [hiccup.core :refer [h]]
    [lde.web :refer [render escape-with-br image-mime-types goto-url]]
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
      (not-empty
        (str
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
            (format-time end-time)))
        ))))

(comment
  (require
    '[clojure.spec.alpha :as s]
    '[clojure.test.check.generators :as gen]
    '[clojure.pprint :refer [pprint]]
    '[hiccup.core :refer [html]])
  (->> (gen/sample (s/gen (s/get-spec :lde.core.event/event)) 20)
       (map #(vector % (html (date-and-time %))))
       pprint))

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
     [:div [:small (date-and-time event)]]
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
             [:button.btn.btn-small
              {:type "submit"
               :data-confirm (str "Are you sure you want to leave the " (str/lower-case (topic/singular topic)) "?")}
              "Leave " (topic/singular topic)]]]

           (and max-attendees (>= attendees max-attendees))
           [:small " No spot left!"]

           (or user-is-organizer
               (= (:id user) (:event/creator event)))
           [:a.btn.btn-small.inline {:href (str event-url "/edit")} "Edit"]

           (not user-is-organizer)
           [:form.inline {:action (str event-url "/join") :method "post"}
            [:button.btn.btn-small {:type "submit"} "Join " (topic/singular topic)]])]]
       [:small "There is no organizer yet! Can you take over?"
        [:form.inline {:action (str event-url "/organize") :method "post"}
        [:button.btn.btn-small {:type "submit"} "Organize " (topic/singular topic)]]])]))

(defn get [{:keys [ctx topic event session]}]
  (let [title (str (:event/name event) " - " (:topic/name topic))
        topic-url (str "/for/" (:topic/slug topic))
        event-url (str topic-url "/about/" (:event/slug event))
        user (user/get-by-id ctx (:id session))]
    (render
      ctx
      {:title title
       :description (str title ": "
                         (when-let [d (date-and-time event)]
                           (str "Date and Time: " d " - "))
                         (when-let [l (:event/location event)]
                           (str "Location: " l " - "))
                         (:event/description event))}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       [:h2 (:topic/description topic)]
       [:nav
        [:a.nav-item {:href (str "/for/" (:topic/slug topic))}
         (topic/singular topic) " overview"]
        (when user
          [:a.nav-item {:href (goto-url "/logout" event-url)} "Logout"])
        (when-not user
          [:a.nav-item {:href (goto-url "/login" event-url)} "Login"])
        (when-not user
          [:a.nav-item {:href (goto-url "/signup" event-url)} "Signup"])]
       (item event topic user ctx)])))

(defn edit [{:keys [ctx topic event session]}]
  (let [title (str "Edit " (topic/singular topic) ": " (:event/name event))
        user (user/get-by-id ctx (:id session))]
    (render
      ctx
      {:title title}
      [:div
       [:h1 "Edit " (topic/singular topic)]
       (if (or (event/organizer? ctx (:id event) (:id user))
               (= (:id user) (:event/creator event)))
         (let [url (str "/for/" (:topic/slug topic) "/about/" (:event/slug event))
               attendees (:event/attendee-count event)]
           [:div
            [:p
             [:i "You "
              (if (event/organizer? ctx (:id event) (:id user))
                "are organizing"
                "created")
              " this " (str/lower-case (topic/singular topic)) "!"] ]
            [:form {:action (str url "/edit")
                    :method "post"
                    :enctype "multipart/form-data"}
             [:div.form-field
              [:label [:div (topic/singular topic) " title" [:sup " *"]]
               [:input.input-field
                {:type "text"
                 :name "name"
                 :value (:event/name event)
                 :required true}]]]
             [:div.form-field
              [:label [:div "Description" [:sup " *"]]
               [:textarea.input-field
                {:type "text"
                 :name "description"
                 :required true
                 :rows 10
                 :cols 50}
                (:event/description event)]]]
             (let [image (image/get-by-hash (:event/image event) ctx)]
               [:div.form-field.image-upload
                [:label "Select an image"
                 [:div [:img#image-upload-preview {:src image
                                                   :alt "logo"
                                                   :class (when-not image "hide")}]
                  [:span#image-upload-message.btn {:class (when image "hide")}
                   "click to select image"]]
                 [:input#image-upload-input {:type "file"
                                             :name "image"
                                             :accept (str/join ", " image-mime-types)
                                             :class "hide"}]]
                [:input#delete-image-input {:type "hidden"
                                            :name "delete-image"}]
                [:span#image-upload-clear.btn {:class (when-not image "hide")} "remove image"]])
             [:div.form-field
              [:label [:div "Starting"]]
              [:input.input-field.input-date
               {:type "date"
                :value (:event/start-date event)
                :name "start-date"}]
              [:input.input-field.input-time
               {:type "time"
                :value (:event/start-time event)
                :name "start-time"}]]
             [:div.form-field
              [:label [:div "Until"]]
              [:input.input-field.input-date
               {:type "date"
                :value (:event/end-date event)
                :name "end-date"}]
              [:input.input-field.input-time
               {:type "time"
                :value (:event/end-time event)
                :name "end-time"}]]
             [:div.form-field
              [:label [:div "Where"]
               [:input.input-field
                {:type "text"
                 :value (:event/location event)
                 :name "location"}]]]
             [:div.form-field
              [:label [:div "Max. number of attendees"]
               [:input.input-field.input-small
                {:type "number"
                 :name "max-attendees"
                 :value (:event/max-attendees event)
                 :min 1
                 :step 1}]]]
             [:p [:small "currently " attendees (if (= attendees 1) " attendee" " attendees")]]
             [:button.btn {:type "submit"} "Update " (topic/singular topic)]
             [:a.cancel {:href (str "/for/" (:topic/slug topic))} "Cancel"]]
            [:form {:action (str url "/delete") :method "post"}
             [:button.btn.btn-small
              {:type "submit"
               :data-confirm (str "Are you sure you want to delete the " (str/lower-case (topic/singular topic)) "?")}
              "Delete " (topic/singular topic)]]]))])))

(comment time/local-date-time (str (:start-date params) "T" (:start-time params)))

(defn new [{:as req :keys [topic ctx]}]
  (let [path (-> req get-match match->path)
        topic-url (str "/for/" (:topic/slug topic))
        sub-title (str "Plan a New " (topic/singular topic))
        title (str sub-title " For " (:topic/name topic))]
    (render
      ctx
      {:title title}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       [:h2 sub-title]
       [:form {:action path
               :method "post"
               :enctype "multipart/form-data"}
        [:div.form-field
         [:label [:div (topic/singular topic) " Title" [:sup " *"]]
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
          [:textarea.input-field
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
