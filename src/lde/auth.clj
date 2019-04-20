(ns lde.auth
  (:require [buddy.hashers :as hashers]
            [buddy.sign.jwt :as jwt]
            [buddy.core.nonce :as nonce]
            [java-time :as time]))

(def password-algorithm :bcrypt+blake2b-512)
(def jwt-algorithm :a256kw)
(def jwt-encoding :a128gcm)
(def jwt-token-duration (time/seconds 3600))
(def jwt-secret (nonce/random-bytes 32))

(defn encrypt [s]
  (hashers/derive s {:alg password-algorithm}))

(defn validate [password hash]
  (hashers/check password hash))

(defn gen-token [user-id]
  (let [claims {:user user-id
                :exp (time/plus (time/instant) jwt-token-duration)}
        options {:alg jwt-algorithm
                 :enc jwt-encoding}]
    (jwt/encrypt claims jwt-secret options)))

(comment
  (validate "hi" (encrypt "hi"))
  (gen-token "user")
)
