(ns lde.server.router
  (:require [lde.server.css :as css]
            [lde.server.pages.login :as login]))

(defn routes []
  [["/css/main.css" {:get css/handler}]
   ["/login" {:get login/handler}]
   ["/signup" {:get login/handler}]])


