(ns lde.router
  (:require [lde.css :as css]
            [lde.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler}]])


