(ns lde.web
  (:require [clojure.string :as str]
            [hiccup.core :refer [html h]]
            [hiccup.page :refer [include-css]]
            [buddy.core.codecs.base64 :as base64]
            [buddy.core.codecs :refer [bytes->str str->bytes]]))

(def image-mime-types #{"image/png" "image/jpeg"})

(defn render [options content]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (html {:mode :html}
               [:html
                [:head
                 [:title (:title options)]
                 [:meta {:charset "utf-8"}]
                 [:meta {:content (:description options) :name "description"}]
                 [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                 (comment include-css
                                    "https://unpkg.com/tachyons@4.9.0/css/tachyons.min.css")
                 (include-css
                   "/css/main.css")]
                [:body {}
                 content]])})

(defn escape-with-br [s]
  (str/replace (h s) #"(\r\n|\r|\n)" "<br>"))

(defn multipart-image-to-data-uri [img]
  (let [{b :bytes} img]
    (when-not (empty? b)
      (str "data:"
           (:content-type img)
           ";base64,"
           (bytes->str (base64/encode b))))))
