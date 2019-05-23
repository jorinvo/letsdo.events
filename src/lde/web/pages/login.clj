(ns lde.web.pages.login
  (:require
    [reitit.core :refer [match->path]]
    [reitit.ring :refer [get-match]]
    [ring.util.response :as response]
    [lde.web :refer [render goto-url]]
    [lde.core.user :as user]))

(def login-click
  "var cl = document.getElementById('login-container').classList;
  cl.add('login');
  cl.remove('signup');
  document.querySelector('title').innerHTML = 'Login';
  history.replaceState({}, 'Login', '/login');")

(def signup-click
  "var cl = document.getElementById('login-container').classList;
  cl.add('signup');
  cl.remove('login');
  document.querySelector('title').innerHTML = 'Signup';
  history.replaceState({}, 'Signup', '/signup');")

(defn handler [{:as req :keys [ctx]
                {user-id :id} :session
                {{:keys [goto]} :query} :parameters}]
  (let [path (-> req get-match match->path)]
    (if user-id
      (response/redirect (if goto goto "/"))
      (render
        ctx
        {:title "Login"
         :description "Hi"}
        [:div.login-container {:class (subs path 1)}
         [:h1.f1
          [:a.login-heading
           {:href (goto-url "/login" goto)
            :onClick login-click}
           "Login"]
          " | "
          [:a.signup-heading
           {:href (goto-url "/signup" goto)
            :onClick signup-click}
           "Signup"]]
         [:form {:action (goto-url path goto) :method "post"}
          [:div.form-field.name-field
           [:label [:div "Name"]
            [:input.input-field {:type "text"
                                 :name "name"}]]]
          [:div.form-field
           [:label
            [:div "Email" [:sup " *"]]
            [:input.input-field {:type "email"
                                 :name "email"
                                 :required true}]]]
          [:div.form-field
           [:label
            [:div "Password"]
            [:div
             [:input.input-field {:type "password"
                                  :name "password"}]]
            [:small "No need for a password, you get a mail instead"]]]
          [:div.form-field.link-field
           [:label
            [:div "Link to your website / social media / ..."]
            [:input.input-field {:type "text"
                                 :name "link"}]]]
          [:div.form-field
           [:button.btn.login-button {:type "submit"} "Login"]
           [:button.btn.signup-button {:type "submit"} "Signup"]]]]))))

(defn post-login [{:keys [ctx]
                   {{:keys [email password]} :form
                    {:keys [goto]} :query} :parameters}]
  (if (empty? password)
    (do (user/send-login-mail ctx email goto)
        (response/redirect "/login/mail-confirm" :see-other))
    (if-let [user (user/login ctx email password)]
      (-> (response/redirect (if goto goto "/") :see-other)
          (assoc :session (select-keys user [:id])))
      (response/bad-request "Invalid login"))))

(defn post-signup [{:keys [ctx]
                    {form-params :form
                     {:keys [goto]} :query} :parameters}]
  (let [user (user/create form-params ctx)]
    (condp = user
      :duplicate-email (response/bad-request "Email already taken")
      (-> (response/redirect (if goto goto "/") :see-other)
          (assoc :session (select-keys user [:id]))))))

(defn logout [{{{:keys [goto]} :query} :parameters}]
  (-> (response/redirect (if goto goto "/"))
      (assoc :session nil)))

(defn mail [{:keys [ctx]
             {{:keys [token goto]} :query} :parameters}]
  (if token
    (if-let [user (user/login-with-token ctx token)]
      (-> (response/redirect (if goto goto "/") :see-other)
          (assoc :session (select-keys user [:id])))
      (response/bad-request "Invalid token"))
    (response/bad-request "No token")))

(defn mail-confirm [{:keys [ctx]}]
  (render
    ctx
    {:title "Login link sent"
     :description "Hi"}
    [:div
     [:h1 "Let's do events!"]
     [:h2 "We sent you a login link"]
     [:div
      [:p "Please check your mails."]]]))
