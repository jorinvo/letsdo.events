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
       :color text-color
       :background background-color
       :margin 0
       :font-weight 300}]
     [:h1
      {:font-family title-font
       :font-weight 500
       :font-size "3rem"
       :color text-color
       :margin "0.5rem 0"}]
     [:h2
      {:font-size "1.5rem"
       :font-weight 300
       :margin-top "0.5rem"
       :margin-bottom "2.0rem"}]
     [:h3
      {:font-family title-font
       :font-size "2rem"
       :font-weight 300
       :color text-color
       :margin "0.5rem 0"}]
     [:p
      {:margin "0.5rem 0"}]
     [:a
      {:text-decoration :none
       :color text-color}
      [:&:hover
       {:text-decoration :underline}]]
     [:nav
      {:margin-bottom "2rem"}
      [:.nav-item
       {:padding-right "0.8rem"}]]
     [:.btn
      {:border (str "0.1rem solid " primary-color)
       :font-size "1rem"
       :color primary-color
       :background :none
       :margin "1rem 0"
       :padding "0.4rem 0.6rem"}
      [:&:hover :&:focus
       {:cursor "pointer"
        :background primary-color
        :outline :none
        :color background-color}]]
     [:.hide {:display :none}]
     [:.container
      {:margin-left :auto
       :margin-right :auto
       :padding-left "1rem"
       :padding-right "1rem"
       :max-width "36rem"}]
     [:ul {:list-style :none
            :padding-left 0}
      [:li {:margin 0
            :padding "0.5rem 0"}]]
     [:ul.overview-list
      {}]
     [:input
      {:font-size "1rem"
       :margin "0.2rem 0"
       :padding "0.4rem 0.6rem"
       :border (str "0.1rem solid " text-color)
       :max-width "100%"
       :width "15rem"}
      [:&:required {:box-shadow :none}]]
     [:.form-field
      {:padding "0.7rem 0"}]
     [:.login-container
      [:a:hover {:text-decoration :none
                 :opacity 0.8}]
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
