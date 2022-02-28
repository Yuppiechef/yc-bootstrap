(ns app
  (:require
   [components]
   [rum.core :as rum]
   [cljs.reader :as edn]
   [goog.crypt :as gcrypt]
   [goog.crypt.base64 :as base64]))

(defn read-state [default]
  (let [node (.getElementById js/document "reactMount")
        state (.getAttribute node "data-state")]
    (cond
      state
      (->
        state
        (base64/decodeStringToByteArray true)
        gcrypt/utf8ByteArrayToString
        (edn/read-string))

      :else
      default)))

(defonce app-atom
  (atom
    (read-state
      {:name "ClojureScript" :count 1})))


(rum/hydrate
  (components/main-page app-atom)
  (.getElementById js/document "reactMount"))