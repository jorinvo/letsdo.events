(ns lde.core.settings
  (:require [buddy.core.codecs :refer [bytes->str str->bytes]]
            [buddy.core.codecs.base64 :as base64]
            [lde.db :as db]
            [lde.auth :as auth]))

(defn get-cookie-secret [ctx]
  (if-let [secret (db/get-setting ctx :cookie-secret)]
    (base64/decode (str->bytes secret))
    (let [new-secret (auth/gen-cookie-secret)]
      (db/set-setting ctx :cookie-secret (bytes->str (base64/encode new-secret)))
      new-secret)))
