(ns lde.web.pages.topic
  (:refer-clojure :exclude [new])
  (:require
    [clojure.string :as str]
    [hiccup.core :refer [h]]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [lde.web :refer [render escape-with-br multipart-image-to-data-uri image-mime-types]]
    [lde.web.pages.event :as event-page]
    [lde.core.topic :as topic]
    [lde.core.image :as image]
    [lde.core.user :as user]
    [lde.core.event :as event]))

(defn new [req]
  (let [path (-> req get-match match->path)]
    (render
      (:ctx req)
      {:title "Setup New Topic"
       :description "Hi"}
      [:div
       [:h1
        "Setup new topic"]
       [:form {:action path
               :method "post"
               :enctype "multipart/form-data"}
        [:div.form-field
         [:label [:div "Topic name" [:sup " *"]]
          [:input.input-field {:type "text"
                               :name "name"
                               :required true}]]]
        [:div.form-field
         [:label [:div "Description"]
          [:input.input-field {:type "text"
                   :name "description"}]]]
        [:div.form-field.image-upload
         [:label [:div "Select a logo"]
          [:div [:img#image-upload-preview { :alt "logo"
                                            :class "hide"}]
           [:span#image-upload-message.btn "click to select image"]]
          [:input#image-upload-input {:type "file"
                                      :name "image"
                                      :accept (str/join ", " image-mime-types)
                                      :class "hide"}]]
         [:span#image-upload-clear.btn {:class "hide"} "remove image"]]
        [:input {:type "hidden"
                 :name "visibility"
                 :required true
                 :value "public"}]
        (comment ->> topic/visibilities
             (map (fn [[value {:keys [label]}]]
                    [:label
                     [:input {:type "radio"
                              :name "visibility"
                              :required true
                              :value value}]
                     label
                     [:br]])))
        [:div.form-field
         [:div "This topic is about" [:sup " *"]]
         (for [[value {label :plural}] topic/types]
           [:label.radio
            [:input {:type "radio"
                     :name "type"
                     :required true
                     :value value}]
            label])]
        [:button.btn {:type "submit"} "Create Topic"]
        [:a.cancel {:href "/"} "Cancel"]]])))

(defn edit [{:keys [path-params ctx]}]
  (let [topic-slug (:topic path-params)
        topic (topic/get-by-slug topic-slug ctx)
        url (str "/for/" topic-slug)]
    (render
      ctx
      {:title "Edit Topic"
       :description "Hi"}
      [:div
       [:h1
        "Edit topic"]
       [:form {:action (str url "/edit")
               :method "post"
               :enctype "multipart/form-data"}
        [:div.form-field
         [:label [:div "Topic name" [:sup " *"]]
          [:input.input-field {:type "text"
                               :name "name"
                               :value (:topic/name topic)
                               :required true}]]]
        [:div.form-field
         [:label [:div "Description"]
          [:input.input-field {:type "text"
                               :name "description"
                               :value (:topic/description topic)}]]]
        (let [image (image/get-by-hash (:topic/image topic) ctx)]
          [:div.form-field.image-upload
           [:label [:div "Select a logo"]
            [:div [:img#image-upload-preview
                   {:src image
                    :alt "logo"
                    :class (when-not image "hide")}]
             [:span#image-upload-message.btn
              {:class (when image "hide")}
              "click to select image"]]
            [:input#image-upload-input {:type "file"
                                        :name "image"
                                        :accept (str/join ", " image-mime-types)
                                        :class "hide"}]]
           [:input#delete-image-input {:type "hidden"
                                       :name "delete-image"}]
           [:span#image-upload-clear.btn {:class (when-not image "hide")} "remove image"]])
        [:br]
        [:input {:type "hidden"
                 :name "visibility"
                 :required true
                 :value "public"}]
        (comment ->> topic/visibilities
             (map (fn [[value {:keys [label]}]]
                    [:label
                     [:input {:type "radio"
                              :name "visibility"
                              :required true
                              :checked (= value (:topic/visibility topic))
                              :value value}]
                     label
                     [:br]])))
        [:span {} "This topic is about: "]
        (for [[value {label :plural}] topic/types]
          [:label.radio
           [:input {:type "radio"
                    :name "type"
                    :required true
                    :checked (= value (:topic/type topic))
                    :value value}]
                     label])
        [:br]
        [:button.btn {:type "submit"} "Update Topic"]
        " "
        [:a.cancel {:href (str "/for/" topic-slug)} "Cancel"]]
       [:form {:action (str url "/delete") :method "post"}
        [:button.btn.btn-small {:type "submit"} "Delete Topic"]]])))

(defn post [{:keys [ctx session parameters]}]
  (let [topic (-> (:multipart parameters)
                  (assoc :creator (:id session))
                  (update :image multipart-image-to-data-uri)
                  (topic/create ctx))]
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn post-edit [{:keys [ctx session path-params parameters]}]
  (let [topic-id (:id (topic/get-by-slug (:topic path-params) ctx))
        topic (-> (:multipart parameters)
                  (update :image multipart-image-to-data-uri)
                  (update :delete-image #(= "true" %))
                  (topic/update topic-id ctx))]
    (prn parameters)
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn- event-item [event topic user ctx]
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
     (event-page/date-and-time event)
     (when location
       [:p "Where? " (h location)])
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
            [:form.inline-form {:action (str event-url "/leave") :method "post"}
             [:button.btn.btn-small {:type "submit"} "Leave " (topic/singular topic)]]]

           (and max-attendees (>= attendees max-attendees))
           [:small " No spot left!"]

           (not user-is-organizer)
           [:form.inline-form {:action (str event-url "/join") :method "post"}
            [:button.btn.btn-small {:type "submit"} "Join " (topic/singular topic)]])]]
       [:small "There is no organizer yet! can you take over?"
        [:form.inline-form {:action (str event-url "/organize") :method "post"}
        [:button.btn.btn-small {:type "submit"} "Organize " (topic/singular topic)]]])
     ]))

(defn overview [{:keys [path-params ctx session]}]
  (let [topic (topic/get-by-slug (:topic path-params) ctx)
        topic-url (str "/for/" (:topic path-params))
        events (event/list-by-topic (:id topic) ctx)
        user (user/get-by-id ctx (:id session))]
    (render
      ctx
      {:title (:topic/name topic)
       :description "Hi"}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       (when-let [image (image/get-by-hash (:topic/image topic) ctx)]
         [:img.logo {:src image
                     :alt "logo"}])
       [:h2 (:topic/description topic)]
       [:nav
        [:a.nav-item {:href (str "/for/" (:topic/slug topic) "/new")}
         "New " (topic/singular topic)]
        (when (topic/admin? ctx (:id topic) (:id user))
          [:a.nav-item {:href (str "/for/" (:topic/slug topic) "/edit")}
           "Edit Topic Meta"])
        [:a.nav-item {:href "/logout"} "Logout"]]
       [:ul.overview-list (map #(vector :li (event-item % topic user ctx)) events)]])))

(defn delete [{:keys [ctx path-params]}]
  (let [topic-id (:id (topic/get-by-slug (:topic path-params) ctx))]
    (topic/delete ctx topic-id)
    (response/redirect "/" :see-other)))
