(ns lde.core.util
  (:require
    [clojure.string :as str]))

; thanks to https://github.com/funcool/cuerdas/blob/a4ed2a0404874ad2b46a18bf019af3b0fbd98422/src/cuerdas/core.cljc#L611-L621
(def ^:private +slug-tr-map+
  (zipmap "ąàáäâãåæăćčĉęèéëêĝĥìíïîĵłľńňòóöőôõðøśșšŝťțŭùúüűûñÿýçżźž"
          "aaaaaaaaaccceeeeeghiiiijllnnoooooooossssttuuuuuunyyczzz"))

(defn slug [s]
  (some-> s
      (.toLowerCase)
      (str/escape +slug-tr-map+)
      (replace #"[^\w\s]+" "")
      (replace #"\s+" "-")))

