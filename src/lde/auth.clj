(ns lde.auth
  (:refer-clojure :exclude [hash])
  (:require [buddy.hashers :as hashers]
            [buddy.core.nonce :as nonce]))

(def password-algorithm :bcrypt+blake2b-512)

(defn hash [s]
  (hashers/derive s {:alg password-algorithm}))

(defn validate [password hash]
  (hashers/check password hash))

(defn gen-cookie-secret []
  (nonce/random-bytes 16))

(comment
  (validate "hi" (hash "hi"))
)
