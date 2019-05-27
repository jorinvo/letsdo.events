(ns lde.web.pages.home
  (:require
    [hiccup.core :refer [h]]
    [lde.web :refer [render]]
    [lde.core.topic :as topic]))

(defn handler [{{user-id :id} :session
                {:as ctx
                 {{title :system-title} :content} :config} :ctx}]
  (render
    ctx
    {:title title}
    [:div
     [:h1 (h title)]
     [:h2 "All Topics"]
     (if user-id
       [:nav
        [:a.nav-item {:href "/new"} "New Topic"]
        [:a.nav-item {:href "/logout"} "Logout"]]
       [:nav
        [:a.nav-item {:href "/login"} "Login"]
        [:a.nav-item {:href "/signup"} "Signup"]])
     [:ul
      (for [{:keys [:topic/name
                    :topic/slug
                    :topic/description]} (topic/list-all-public ctx)]
        [:li [:a
              {:href (str "/for/" (h slug))}
              [:h3 (h name)]]
         [:p (h description)]])]]))
