(ns lde.web.css
  (:require [garden.core :refer [css]]))

(def style
  (css [:#login-container
        [:&.login
         [:.login-heading {:text-decoration :underline}]
         [:.signup-button {:display :none}]
         [:.name-field {:display :none}]
         [:.link-field {:display :none}]]
        [:&.signup
         [:.signup-heading {:text-decoration :underline}]
         [:.login-button {:display :none}]]]))

(defn handler [req]
  {:status 200
   :headers {"content-type" "text/css"}
   :body style})
