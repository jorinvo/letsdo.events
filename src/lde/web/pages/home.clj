(ns lde.web.pages.home
  (:require
    [lde.web :refer [render]]
    [lde.core.topic :as topic]))

(defn handler [{{user-id :id} :session ctx :ctx}]
  (render {:title "Let's do events!"
           :description "Hi"}
          [:div
           [:h1 "Let's do events!"]
           (if user-id
             [:div
              [:a {:href "/new"} "New Topic"]
              " "
              [:a {:href "/logout"} "Logout"]
              [:div
               [:h3 "My Topics"]
               [:ul
                (->> (topic/list-by-user user-id ctx)
                     (map (fn [{:keys [:topic/name :topic/slug]}]
                        [:li [:a {:href (str "/for/" slug)} name]])))]]]
             [:div
              [:a {:href "/login"} "Login/Signup"]])]))
