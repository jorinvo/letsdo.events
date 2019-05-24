(ns lde.web.pages.topic
  (:refer-clojure :exclude [new])
  (:require
    [clojure.string :as str]
    [hiccup.core :refer [h]]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [lde.web :refer [render escape-with-br multipart-image-to-data-uri image-mime-types goto-url]]
    [lde.web.pages.event :as event-page]
    [lde.core.topic :as topic]
    [lde.core.image :as image]
    [lde.core.user :as user]
    [lde.core.event :as event]))

(defn new [req]
  (let [path (-> req get-match match->path)]
    (render
      (:ctx req)
      {:title "Setup New Topic"}
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

(defn edit [{:keys [topic ctx]}]
  (let [url (str "/for/" (:topic/slug topic))]
    (render
      ctx
      {:title (str "Edit Topic: " (:topic/name topic))}
      [:div
       [:h1
        "Edit Topic"]
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
        [:a.cancel {:href url} "Cancel"]]
       [:form {:action (str url "/delete") :method "post"}
        [:button.btn.btn-small
         {:type "submit"
          :data-confirm "Are you sure you want to delete the topic?"}
         "Delete Topic"]]])))

(defn post [{:keys [ctx session parameters]}]
  (let [topic (-> (:multipart parameters)
                  (assoc :creator (:id session))
                  (update :image multipart-image-to-data-uri)
                  (topic/create ctx))]
    (response/redirect (str "/for/" (:topic/slug topic)) :see-other)))

(defn post-edit [{:keys [ctx session topic parameters]}]
  (let [new-topic (-> (:multipart parameters)
                      (update :image multipart-image-to-data-uri)
                      (update :delete-image #(= "true" %))
                      (topic/update (:id topic) ctx))]
    (response/redirect (str "/for/" (:topic/slug new-topic)) :see-other)))

(defn overview [{:keys [topic ctx session]}]
  (let [title (:topic/name topic)
        topic-url (str "/for/" (:topic/slug topic))
        events (event/list-by-topic (:id topic) ctx)
        user (user/get-by-id ctx (:id session))]
    (render
      ctx
      {:title title
       :description (str title " - " (:topic/description topic))}
      [:div
       [:a {:href topic-url}
        [:h1 (:topic/name topic)]]
       (when-let [image (image/get-by-hash (:topic/image topic) ctx)]
         [:img.logo {:src image
                     :alt "logo"}])
       [:h2 (:topic/description topic)]
       (if user
         [:nav
          [:a.nav-item {:href (str "/for/" (:topic/slug topic) "/new")}
           "New " (topic/singular topic)]
          (when (topic/admin? ctx (:id topic) (:id user))
            [:a.nav-item {:href (str "/for/" (:topic/slug topic) "/edit")}
             "Edit Topic Meta"])
          [:a.nav-item {:href (goto-url "/logout" topic-url)} "Logout"]]
         [:nav
          [:a.nav-item {:href (goto-url "/login" topic-url)} "Login"]
          [:a.nav-item {:href (goto-url "/signup" topic-url)} "Signup"]])
       [:ul.overview-list (map #(vector :li (event-page/item % topic user ctx)) events)]])))

(defn delete [{:keys [ctx topic]}]
  (topic/delete ctx (:id topic))
  (response/redirect "/" :see-other))
