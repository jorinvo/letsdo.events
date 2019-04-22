(ns lde.web.pages.home
  (:require
    [lde.web :refer [render]]))

(defn handler [{{user-id :id} :session}]
  (render {:title "Let's do events!"
           :description "Hi"}
          [:div
           [:h1 "Let's do events!"]
           (if user-id
             [:div
              [:a {:href "/new"} "New Topic"]
              " "
              [:a {:href "/logout"} "Logout"]]
             [:div
              [:a {:href "/login"} "Login/Signup"]])]))
