(ns util.comms
  (:require
   [taoensso.sente :as sente]))

(def comms-atom (atom nil))

(defn send-msg [event data]
  (let [send-fn (:send-fn @comms-atom)]
    (send-fn [event data])))

(defn csrf-token []
  #?(:cljs
     (when-let [el (.getElementById js/document "sente-csrf-token")]
       (.getAttribute el "data-csrf-token"))))

(defn init []
  (let [csrf (csrf-token)

        {:keys [chsk ch-recv send-fn state] :as comms}
        (sente/make-channel-socket-client!
          "/api/socket" csrf {:type :auto})]
    (reset! comms-atom
      (assoc comms :csrf csrf))))