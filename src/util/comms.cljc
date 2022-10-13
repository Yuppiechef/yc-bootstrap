(ns util.comms
  (:require
   [taoensso.sente :as sente]))

(def comms-atom (atom nil))

(defn send-msg [event data]
  (let [send-fn (:send-fn @comms-atom)]
    (when send-fn
      (send-fn [event data]))))

(defn csrf-token []
  #?(:cljs
     (when-let [el (.getElementById js/document "sente-csrf-token")]
       (.getAttribute el "data-csrf-token"))))

(defmulti receive
  (fn [app-atom data]
    (:type (meta data))))

(defmethod receive :default [app-atom data]
  (println "No comms handler for " (:type (meta data))))

(defmethod receive :chsk/state [_ _])
(defmethod receive :chsk/handshake [_ _])
(defmethod receive :chsk/uiport-open [_ _])
(defmethod receive :chsk/ws-ping [_ _])
(defmethod receive :api/error [_ e]
  (println "Server Error: " (:msg e)))

(defn event-msg-handler
  [app-atom {:as ev-msg :keys [client-id event id ?data ring-req ?reply-fn send-fn]}]

  ;; Sente has this weird tendency to double wrap events?? I don't get it.
  (let [[id ?data] (if (= id :chsk/recv) ?data [id ?data])]
    (when id
      (receive app-atom (with-meta ?data {:type id})))
    nil))

(defn init [app-atom]
  (let [csrf (csrf-token)

        {:keys [chsk ch-recv send-fn state] :as comms}
        (sente/make-channel-socket-client!
          "/api/socket" csrf {:type :auto})]

    (sente/start-chsk-router!
      (:ch-recv comms) (partial #'event-msg-handler app-atom))

    (reset! comms-atom
      (assoc comms :csrf csrf))))