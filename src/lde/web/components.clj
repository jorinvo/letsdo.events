(ns lde.web.components
  (:require
    [clojure.string :as str]
    [hiccup.core :refer [h]]
    [lde.web.util :refer [image-mime-types]]))

(defn image-upload
  ([]
   (image-upload nil))
  ([image]
   [:div.form-field.image-upload
    [:label "Select an image"
     [:div [:img#image-upload-preview {:src (h image)
                                       :alt "logo"
                                       :class (when-not image "hide")}]
      [:span#image-upload-message.btn {:class (when image "hide")}
       "click to select image"]]
     [:input#image-upload-input {:type "file"
                                 :name "image"
                                 :accept (str/join ", " image-mime-types)
                                 :class "hide"}]]
    [:input#delete-image-input {:type "hidden"
                                :name "delete-image"}]
    [:span#image-upload-clear.btn {:class (when-not image "hide")} "remove image"]]))
