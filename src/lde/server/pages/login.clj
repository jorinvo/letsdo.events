(ns lde.server.pages.login
  (:require [lde.server.web :refer [render]]))

(def login-click
  "var cl = document.getElementById('login-container').classList;
  cl.add('login');
  cl.remove('signup');")

(def signup-click
  "var cl = document.getElementById('login-container').classList;
  cl.add('signup');
  cl.remove('login');")

(defn handler [req]
  (render
    {:title "Login"
     :description "Hi"}
    [:div#login-container.login
     [:h1.f1
      [:span.login-heading
       {:onClick login-click}
       "Login"]
      " | "
      [:span.signup-heading
       {:onClick signup-click}
       "Signup"]]
     [:form
      [:label.name-field "Name: "
       [:input {:type "text"
                :placeholder "Name"}]]
      [:br]
      [:label "Email: "
       [:input {:type "email"
                :placeholder "Email"}]]
      [:br]
      [:label [:i "Optionally"] " password: "
       [:input {:type "password"
                :placeholder "Password"}]]
      [:br]
      [:small "No need to set a password, we will send you a mail"]
      [:br]
      [:label.link-field [:i "Optionally"] " link to your website/social media/...: "
       [:input {:type "text"
                :placeholder "Link"}]]
      [:br]
      [:button.login-button {:action "POST"} "Login"]
      [:button.signup-button {:action "POST"} "Signup"]]]))

