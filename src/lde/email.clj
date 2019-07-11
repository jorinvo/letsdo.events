(ns lde.email
(:refer-clojure :exclude [send])
  (:require
    [postal.core :as postal]))

(defn send [{:keys [to subject body]}
            {{{email-config :default} :smtp} :config}]
  (if email-config
    (postal/send-message email-config
                         {:from (:from email-config)
                          :to to
                          :subject subject
                          :body body})
    (println "WARNING: SMTP is not configured. Tried sending email:\n\n"
             body)))
