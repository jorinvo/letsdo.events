(ns lde.config
  (:require
    [clojure.string :as str]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s]
    [clojure.test.check.generators :as gen]))

(s/def ::port (s/int-in 1 65536))
(s/def ::enable-password-authentication boolean?)
(s/def ::public-base-url (s/and string?
                                #(try (io/as-url (if (= "/" (last %))
                                                   (subs % 0 (dec (count %)))
                                                   %))
                                      (catch Exception e nil))))
(s/def ::db-dir #(and (string? %)
                      (when (.exists (.getParentFile (io/file %)))
                        %)))
(s/def ::event-log-dir #(and (string? %)
                             (when (.exists (.getParentFile (io/file %)))
                               %)))

(s/def ::system-title string?)
(s/def ::content (s/keys :req-un [::system-title]))

(s/def ::background-color string?)
(s/def ::primary-color string?)
(s/def ::text-color string?)
(s/def ::title-font string?)
(s/def ::base-font string?)
(s/def ::additional-stylesheets (s/every string?))
(s/def ::style
  (s/keys :opt-un [::background-color
                   ::primary-color
                   ::text-color
                   ::title-font
                   ::base-font
                   ::additional-stylesheets]))

; email stuff adopted from: https://github.com/SparkFund/useful-specs/blob/master/src/specs/internet.clj
(s/def ::hostpart
  (letfn [(pred [s]
            (re-matches #"\A(?:\p{Alnum}|\p{Alnum}(?:\p{Alnum}|-)*\p{Alnum})\z" s))
          (gen []
            (let [middle-char (gen/fmap char
                                        (gen/one-of [(gen/choose 48 57)
                                                     (gen/choose 65 90)
                                                     (gen/choose 97 122)
                                                     (gen/return 45)]))]
              (gen/let [length (gen/choose 1 64)]
                (let [chars-gen (if (= 1 length)
                                  (gen/vector gen/char-alphanumeric 1)
                                  (gen/let [first-char gen/char-alphanumeric
                                            last-char gen/char-alphanumeric
                                            middle-chars (gen/vector middle-char
                                                                     (- length 2))]
                                    (gen/return (-> [first-char]
                                                    (into middle-chars)
                                                    (conj last-char)))))]
                  (gen/fmap str/join chars-gen)))))]
    (s/spec pred :gen gen)))

(s/def ::host
  (let [hostpart-spec (s/get-spec ::hostpart)]
    (letfn [(pred [s]
              (and (>= 253 (count s))
                   (let [parts (str/split s #"\.")]
                     (and (not (str/starts-with? s "."))
                          (not (str/ends-with? s "."))
                          (every? (partial s/valid? hostpart-spec) parts)))))
            (gen []
              (let [min-needed 2
                    max-needed 4
                    parts-gen (gen/vector (s/gen hostpart-spec)
                                          min-needed max-needed)]
                (gen/fmap (partial str/join ".") parts-gen)))]
      (s/spec pred :gen gen))))

(s/def ::local-email-part
  (let [chars (set (str "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                        "abcdefghijklmnopqrstuvwxyz"
                        "0123456789"
                        "!#$%&'*+-/=?^_`{|}~"))
        dot-chars (conj chars \.)]
    (letfn [(pred [s]
              (and (>= 64 (count s) 1)
                   (not (str/starts-with? s "."))
                   (not (str/ends-with? s "."))
                   (every? dot-chars s)
                   (not (re-find #"\.\." s))))
            (gen []
              (gen/fmap str/join (gen/vector (gen/elements chars) 1 64)))]
      (s/spec pred :gen gen))))

(s/def ::email
  (s/spec (fn [s]
              (let [parts (str/split s #"@")]
                (and (= 2 (count parts))
                     (s/valid? ::local-email-part (first parts))
                     (s/valid? ::host (second parts)))))
        :gen (fn []
              (gen/let [local-part (s/gen ::local-email-part)
                        hostname-part (s/gen ::host)]
                (gen/return (str local-part "@" hostname-part))))))

(s/def :smtp/from ::email)
(s/def :smtp/user string?)
(s/def :smtp/pass string?)
(s/def :smtp/ssl boolean?)
(s/def :smtp/default
  (s/keys :req-un [:smtp/from]
          :opt-un [:smtp/host
                   :smtp/user
                   :smtp/pass
                   ::port
                   :smtp/ssl]))
(s/def ::smtp
  (s/keys :opt-un [:smtp/default]))

(s/def :honeycomb/data-set string?)
(s/def :honeycomb/write-key string?)
(s/def ::honeycomb
  (s/keys :req-un [:honeycomb/data-set
                   :honeycomb/write-key]))

(s/def ::config
  (s/keys :req-un [::port
                   ::public-base-url
                   ::db-dir
                   ::event-log-dir]
          :opt-un [::enable-password-authentication
                   ::content
                   ::style
                   ::smtp
                   ::honeycomb]))

(def default-config
  {:port 3000
   :enable-password-authentication true
   :public-base-url "http://localhost:3000"
   :db-dir "./data/db"
   :event-log-dir "./data/event-log"
   :content {:system-title "Let's do events!"}
   :style {:background-color "#ffffff"
           :primary-color "#ff009a"
           :text-color "#252525"
           :title-font "serif"
           :base-font "sans-serif"}})

(defn get-config
  ([]
   (get-config nil))
  ([path]
   (if path
     (let [f (-> path
                 io/reader
                 java.io.PushbackReader.
                 edn/read)]
       (if (s/valid? ::config f)
         (merge default-config
                (s/conform ::config f))
         (do (s/explain ::config f)
             nil)))
     default-config)))
