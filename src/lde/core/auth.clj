(ns lde.core.auth
  (:refer-clojure :exclude [hash])
  (:require [buddy.hashers :as hashers]))

(def password-algorithm :bcrypt+blake2b-512)

(defn hash [s]
  (hashers/derive s {:alg password-algorithm}))

(defn validate [password hash]
  (hashers/check password hash))

(comment
  (validate "hi" (hash "hi"))
)
