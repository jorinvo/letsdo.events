(ns lde.web.pages.home
  (:require
    [ring.util.response :as response]
    [lde.web :refer [render]]
    [lde.core.topic :as topic]))

(defn handler [{{user-id :id} :session ctx :ctx}]
  (if user-id
    (render
      ctx
      {:title "Let's do events!"
       :description "Hi"}
      [:div
       [:h1 "Let's do events!"]
       [:h2 "All Topics"]
       [:nav
        [:a.nav-item {:href "/new"} "New Topic"]
        [:a.nav-item {:href "/logout"} "Logout"]]
       [:ul
        (for [{:keys [:topic/name
                      :topic/slug
                      :topic/description]} (topic/list-all-public ctx)]
          [:li [:a
                {:href (str "/for/" slug)}
                [:h3 name]]
           [:p description]])]])
    (response/redirect "/login" :see-other)))
