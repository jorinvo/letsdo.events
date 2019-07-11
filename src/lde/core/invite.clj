(ns lde.core.invite
  (:refer-clojure :exclude [send])
  (:require
  [clojure.set :refer [rename-keys]]
    [lde.db :as db]
    [lde.email :as email]
    [lde.core.user :as user]
    [lde.core.topic :as topic]))

(def invite-keys {:email :invite/email
                  :topic :invite/topic
                  :inviter :invite/inviter})

(defn save [invite ctx]
  (db/tx ctx
         (-> invite
             (select-keys (keys invite-keys))
             (rename-keys invite-keys)
             (db/create! ctx))))

(defn render-invite [{:keys [base-url
                             topic-name
                             topic-slug
                             user-name]}]
  (let [link (str base-url
                  (if user-name "/login" "/signup")
                  "?goto=/for/" topic-slug "/join")]
    (str "Hi "
        (or user-name "there")
        "!\n\n"
        "Please click the link below to accept the invite to "
        topic-name
        ":\n\n"
        link)))

(defn send [{:keys [email topic-name topic-slug user-name]} ctx]
  (let [system-title (-> ctx :config :content :system-title)]
    (email/send {:to email
                 :subject (str system-title " Invite To Join " topic-name)
                 :body (render-invite {:base-url (-> ctx :config :public-base-url)
                                       :topic-name topic-name
                                       :topic-slug topic-slug
                                       :user-name user-name})}
                ctx)))

(defn create [data topic ctx]
  (let [email (:email data)
        user (user/get-by-email email ctx)]
    (if (topic/is-member? {:user-id (:id user)
                           :topic-id (:id topic)}
                          ctx)
      :already-member
      (let [invite (-> data
                       (assoc :topic (:id topic))
                       (save ctx))]
        (send {:email email
               :topic-name (:topic/name topic)
               :topic-slug (:topic/slug topic)
               :user-name (:user/name user)} ctx)
        invite))))

(defn list-by-topic [topic-id ctx]
  (db/list-by-attribute ctx :invite/topic topic-id))

(defn delete [invite-id ctx]
  (db/tx ctx (db/delete-by-id! invite-id ctx)))

(defn accept [{:keys [topic-id user-id]} ctx]
  (db/tx ctx (if-let [invite-id (ffirst (db/q ctx {:find '[?i]
                                                :where '[[?i :invite/topic t]
                                                         [?i :invite/email email]
                                                         [u :user/email email]]
                                                :args [{'t topic-id
                                                        'u user-id}]}))]
               (do (db/delete-by-id! invite-id ctx)
                   (-> {:member/topic topic-id
                        :member/user user-id}
                       (db/create! ctx)))
               :invite-not-found)))
