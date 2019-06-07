(ns lde.web.pages.topic
  (:refer-clojure :exclude [new])
  (:require
    [clojure.string :as str]
    [hiccup.core :refer [h]]
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [lde.web.components :as components]
    [lde.web.util :refer [render escape-with-br multipart-image-to-data-uri goto-url]]
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
        (components/image-upload)
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
  (let [url (h (str "/for/" (:topic/slug topic)))]
    (render
      ctx
      {:title (str "Edit Topic: " (h (:topic/name topic)))}
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
                               :value (h (:topic/name topic))
                               :required true}]]]
        [:div.form-field
         [:label [:div "Description"]
          [:input.input-field {:type "text"
                               :name "description"
                               :value (h (:topic/description topic))}]]]
        (let [image (image/get-by-hash (:topic/image topic) ctx)]
          (components/image-upload image))
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

(defn overview [{:keys [topic ctx session]
                 {{:keys [whats]
                   :or {whats "upcoming"}} :query} :parameters}]
  (let [title (:topic/name topic)
        topic-url (h (str "/for/" (:topic/slug topic)))
        topic-id (:id topic)
        user-id (:id session)
        events (case whats
                 "upcoming" (event/upcoming-by-topic topic-id ctx)
                 "new" (event/latest-by-topic topic-id ctx)
                 "mine" (event/mine-by-topic topic-id user-id ctx))
        user (user/get-by-id ctx user-id)]
    (render
      ctx
      {:title title
       :description (str title " - " (:topic/description topic))}
      [:div
       [:a {:href topic-url}
        [:h1 (h (:topic/name topic))]]
       (when-let [image (image/get-by-hash (:topic/image topic) ctx)]
         [:img.logo {:src (h image)
                     :alt "logo"}])
       [:h2 (h (:topic/description topic))]
       (if user
         [:nav
          [:a.nav-item {:href (h (str "/for/" (:topic/slug topic) "/new"))}
           "New " (topic/singular topic)]
          (when (topic/admin? ctx (:id topic) (:id user))
            [:a.nav-item {:href (h (str "/for/" (:topic/slug topic) "/edit"))}
             "Edit Topic"])
          [:a.nav-item {:href (goto-url "/logout" topic-url)} "Logout"]]
         [:nav
          [:a.nav-item {:href (goto-url "/login" topic-url)} "Login"]
          [:a.nav-item {:href (goto-url "/signup" topic-url)} "Signup"]])
       [:nav
        [:a.nav-item.select-item
         {:href topic-url
          :class (when (= whats "upcoming")
                   "active")}
         (str "Upcoming " (topic/plural topic))]
        [:a.nav-item.select-item
         {:href (str topic-url "?whats=new")
          :class (when (= whats "new")
                   "active")}
         (str "New " (topic/plural topic))]
        (when user
          [:a.nav-item.select-item
           {:href (str topic-url "?whats=mine")
            :class (when (= whats "mine")
                     "active")}
           (str "My " (topic/plural topic))])]
       [:ul.overview-list (map #(vector :li (event-page/item % topic user ctx))
                               events)]])))
