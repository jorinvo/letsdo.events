(ns events.letsdo.router
  (:require [events.letsdo.css :as css]
            [events.letsdo.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]])


