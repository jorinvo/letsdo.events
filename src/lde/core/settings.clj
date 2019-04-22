(ns lde.core.settings
  (:require [buddy.core.codecs :refer [bytes->str str->bytes]]
            [buddy.core.codecs.base64 :as base64]
            [buddy.core.nonce :as nonce]
            [lde.db :as db]))

(defn get-cookie-secret [ctx]
  (if-let [secret (db/get-setting ctx :cookie-secret)]
    (base64/decode (str->bytes secret))
    (let [new-secret (nonce/random-bytes 16)]
      (db/set-setting ctx :cookie-secret (bytes->str (base64/encode new-secret)))
      new-secret)))
