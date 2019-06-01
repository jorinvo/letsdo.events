(ns lde.web.error
  (:require
    [hiccup.core :refer [h]]
    [lde.web.util :as util]))

(defn render [{:keys [status title link link-text]
               :or {link "/"
                    link-text "Go to homepage"}}
              ctx]
  (util/render
    ctx
    {:status status
     :title title}
    [:div
     [:h1 "Let's do events!"]
     [:h2 (h title)]
     [:p
      [:a.btn {:href link} (h link-text)]]]))
