(ns lde.web.util
  (:require [clojure.string :as str]
            [hiccup.core :refer [html h]]
            [hiccup.page :refer [html5 include-css include-js]]
            [buddy.core.codecs.base64 :as base64]
            [buddy.core.codecs :refer [bytes->str str->bytes]]))

(def image-mime-types #{"image/png" "image/jpeg"})

(defn render [ctx {:keys [title description status] :or {status 200}} content]
  {:status status
   :headers {"content-type" "text/html"}
   :body (html {:mode :html}
               (html5
                 {:lang "en"}
                 [:head
                  [:title (h title)]
                  [:meta {:charset "utf-8"}]
                  (when description
                    [:meta {:content (h description) :name "description"}])
                  [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
                  (include-css "/css/main.css")
                  ; cannot use include-css since google fonts use | char which is not valid in a Java URI object
                  (for [style (-> ctx :config :style :additional-stylesheets)]
                    [:link {:type "text/css", :href (h style), :rel "stylesheet"}])]
                 [:body {}
                  [:div.container content]
                  (include-js "/js/script.js")]))})

(defn goto-url [base goto]
  (str base (when goto
              (str "?goto=" goto))))

(defn escape-with-br [s]
  (str/replace (h s) #"(\r\n|\r|\n)" "<br>"))

(defn multipart-image-to-data-uri [img]
  (let [{b :bytes} img]
    (when-not (empty? b)
      (str "data:"
           (:content-type img)
           ";base64,"
           (bytes->str (base64/encode b))))))
