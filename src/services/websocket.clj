(ns services.websocket
  (:require
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
   [orchestration.servicedef :as s]))

(defonce server (atom nil))

(defn stop []
  ((:stop-fn @server))
  (reset! server nil))

(defn user-uuid [u]
  (:client-id u))

(defn ->api-name [event-id]
  (str (namespace event-id) "." (name event-id)))

(defn routing [service-list ev-msg]
  (let [api-name (->api-name (:id ev-msg))]
    (println api-name)
  ;; We need to step through the service list AND each of their setup rules.
  ;; The cleaner, faster way would be to have a separate function that transforms our service-list
  ;; into a flat and indexed map to find the matching endpoints quicker.
    (loop [[s & ss :as slist] service-list
           [setup & setuplist] (:setup s)]
      (println (:type setup) " - " (:path setup))
      (cond
      ;; Can't find a suitable match, index page
        (nil? s)
        {:result
         {:ws/action :api/error
          :success false
          :msg (str "Service route, '" api-name "' not found")}}

      ;; Done with setup for this service endpoint, try next.
        (nil? setup)
        (recur ss (:setup (first ss)))

      ;; Match...
        (and
          (= (:type setup) :api)
          (= (:path setup) api-name))
        {:service s
         :setup setup}

      ;; No match, continue.
        :else
        (recur slist setuplist)))))

(defn handle-route [{:keys [service setup]} ev-msg]
  ((:handler service) {:params (:?data ev-msg)}))

(defn event-msg-handler
  [service-list-atom {:as ev-msg :keys [client-id event id ?data ring-req ?reply-fn send-fn]}]
  (let [route (routing @service-list-atom ev-msg)
        result (or (:result route) (handle-route route ev-msg))]
    (when (and send-fn result)
      (send-fn client-id
        [(or (:ws/action result) id) (dissoc result :ws/action)]))))

(defn start [service-rules]
  (let [service-list-atom (atom (service-rules))
        state
        (reset! server
          (sente/make-channel-socket! (get-sch-adapter)
            {:user-id-fn #'user-uuid}))]

    (add-watch service-rules ::update
      (fn [k r o n]
        (reset! service-list-atom (n))))

    (sente/start-chsk-router!
      (:ch-recv state) (partial #'event-msg-handler service-list-atom))

    state))

(defn restart [service-rules]
  (stop)
  (start service-rules))


(defn ring-ajax-post [{{:keys [websocket]} :ctx :as req}]
  ((:ajax-post-fn @server) req))

(defn ring-ajax-get-or-ws-handshake [{{:keys [websocket]} :ctx :as req}]
  ((:ajax-get-or-ws-handshake-fn @server) req))

(defn service-rules []
  [(s/service #'s/no-op #{:websocket}
     (s/web :post "/api/socket" #'ring-ajax-post))

   (s/service #'s/no-op #{:websocket}
     (s/web :get "/api/socket" #'ring-ajax-get-or-ws-handshake))])