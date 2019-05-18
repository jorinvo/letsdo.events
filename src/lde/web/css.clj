(ns lde.web.css
  (:require [garden.core :refer [css]]))

(def black "#000000")
(def white "#ffffff")

(def style
  (css [[:body
         {:font-family "sans-serif"
          :margin 0
          :font-weight 300}]
        [:h1
         {:font-family "serif"
          :font-weight 500
          :font-size "3rem"
          :color black}]
        [:h2
         {:font-size "1.5rem"
          :font-weight 300}]
        [:h3
         {:font-family "serif"
          :font-size "2rem"
          :font-weight 300
          :color black}]
        [:a
         {:text-decoration :none}]
        [:.btn
         {:border "0.05rem solid black"
          :background :none
          :margin "1rem 0"
          :padding "0.3rem 0.5rem"}
         [:&:hover
          {:cursor "pointer"
           :background black
           :color white}]]
        [:.hide {:display :none}]
        [:.container
         {:margin-left :auto
          :margin-right :auto
          :padding-left "1rem"
          :padding-right "1rem"
          :max-width "36rem"}]
        [:ul.overview-list
         {:list-style :none
          :padding-left 0}
         :&.li {}]
        [:.form-field
         {:padding "0.5rem 0"}]
        [:.login-container
         [:&.login
          [:.login-heading {:text-decoration :underline}]
          [:.signup-button {:display :none}]
          [:.name-field {:display :none}]
          [:.link-field {:display :none}]]
         [:&.signup
          [:.signup-heading {:text-decoration :underline}]
          [:.login-button {:display :none}]]]
        [:#image-upload-preview {:max-width "100%" :max-height "500px"}]]))

(defn handler [req]
  {:status 200
   :headers {"content-type" "text/css"}
   :body style})
