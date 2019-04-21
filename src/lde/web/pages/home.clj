(ns lde.web.pages.home
  (:require
    [lde.web :refer [render]]))

(defn handler [{s :session}]
  (prn s)
  (render {:title "Let's do events!"
           :description "Hi"}
          [:h1 "Let's do events!"]))
