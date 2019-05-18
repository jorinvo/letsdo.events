(ns lde.web.css
  (:require [garden.core :refer [css]]))

(defn style [{:keys [primary-color
                     background-color
                     text-color
                     title-font
                     base-font]}]
  (css
    [[:body
      {:font-family base-font
       :margin 0
       :font-weight 300}]
     [:h1
      {:font-family title-font
       :font-weight 500
       :font-size "3rem"
       :color text-color}]
     [:h2
      {:font-size "1.5rem"
       :font-weight 300}]
     [:h3
      {:font-family title-font
       :font-size "2rem"
       :font-weight 300
       :color text-color}]
     [:a
      {:text-decoration :none}]
     [:.btn
      {:border (str "0.05rem solid " primary-color)
       :color primary-color
       :background :none
       :margin "1rem 0"
       :padding "0.3rem 0.5rem"}
      [:&:hover
       {:cursor "pointer"
        :background primary-color
        :color background-color}]]
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
   :body (style (-> req :ctx :config :style))})
