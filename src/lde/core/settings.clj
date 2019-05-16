(ns lde.core.settings
  (:require [buddy.core.codecs :refer [bytes->str bytes->hex str->bytes]]
            [buddy.core.codecs.base64 :as base64]
            [buddy.core.nonce :as nonce]
            [lde.db :as db]))

(defn get-cookie-secret [ctx]
  (if-let [secret (db/get-key ctx :settings/cookie-secret)]
    (base64/decode (str->bytes secret))
    (let [new-secret (nonce/random-bytes 16)]
      (db/set-key! ctx :settings/cookie-secret (bytes->str (base64/encode new-secret)))
      new-secret)))

(defn get-jwt-secret [ctx]
  (if-let [secret (db/get-key ctx :settings/jwt-secret)]
    secret
    (let [new-secret (bytes->hex (nonce/random-bytes 32))]
      (db/set-key! ctx :settings/jwt-secret new-secret)
      new-secret)))
