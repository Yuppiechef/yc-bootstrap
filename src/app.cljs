(ns app
  (:require
   [cljs.reader :as edn]
   [goog.crypt :as gcrypt]
   [goog.crypt.base64 :as base64]
   [rum.core :as rum]
   [util.comms :as comms]
   [util.flow :as flow]
   [util.urlstate :as urlstate]
   [web.components]
   [web.index]))

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

(defn ^:dev/after-load refresh []
  (rum/mount
    (flow/render-state app-atom)
    (.getElementById js/document "reactMount")))

(urlstate/setup-history app-atom)
(rum/hydrate
  (flow/render-state app-atom)
  (.getElementById js/document "reactMount"))

(comms/init app-atom)

