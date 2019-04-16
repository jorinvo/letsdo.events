(ns lde.web
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css]]))

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
                 (include-css
                                    "https://unpkg.com/tachyons@4.9.0/css/tachyons.min.css")
                 (include-css
                   "/css/main.css")]
                [:body {} content]])})
