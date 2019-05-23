(ns lde.config
  (:require
    [clojure.string :as str]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s]
    [clojure.test.check.generators :as gen]))

(s/def ::port (s/int-in 1 65536))
(s/def ::db-dir (s/and string?
                       #(when (.exists (io/file %))
                          %)))
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
                    max-needed 4]
                (let [parts-gen (gen/vector (s/gen hostpart-spec)
                                            min-needed max-needed)]
                  (gen/fmap (partial str/join ".") parts-gen))))]
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

(s/def ::user string?)
(s/def ::password string?)
(s/def ::from ::email)
(s/def ::default
  (s/keys :req-un [::host ::from]
          :opt-un [::user ::password ::port]))
(s/def ::smtp
  (s/keys :opt-un [::default]))

(s/def ::config
  (s/keys :req-un [::port ::db-dir]
          :opt-un [::style ::smtp]))

(def default-config
  {:port 3000
   :db-dir "./db"
   :style {:background-color  "#ffffff"
           :primary-color  "rgb(226, 95, 125)"
           :text-color  "#252525"
           :title-font  "'Halant', serif"
           :base-font  "'Nunito', sans-serif"}})

(defn get-config
  ([]
   (get-config nil))
  ([path]
   (merge
     default-config
     (when path
      (s/conform ::config
                 (-> path
                     io/reader
                     java.io.PushbackReader.
                     edn/read))))))
