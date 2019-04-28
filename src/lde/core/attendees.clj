(ns lde.core.attendees
  (:require
    [lde.db :as db]))

(defn add [ctx event-id user-id]
  (let [attendee {:attendee/event event-id
                  :attendee/user user-id}]
    (when-not (db/get-by-attributes ctx attendee)
      (db/save ctx attendee))))

(defn count-by-event-id [ctx event-id]
  (db/count-by-attribute ctx :attendee/event event-id))
