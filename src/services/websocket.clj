(ns services.websocket
  (:require
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
   [orchestration.servicedef :as s]))

(defonce server (atom nil))

(defn stop []
  ((:stop-fn @server))
  (reset! server nil))

(defn start [service-rules]
  (reset! server
    (sente/make-channel-socket! (get-sch-adapter) {})))

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