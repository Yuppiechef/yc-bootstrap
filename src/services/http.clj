(ns services.http
  (:require
   [org.httpkit.server :as http-kit]
   [ring.middleware.anti-forgery]
   [ring.middleware.keyword-params]
   [ring.middleware.cookies]
   [ring.middleware.params]
   [ring.middleware.session.memory :as memory]
   [ring.middleware.resource :as res]
   [ring.middleware.session]
   [web.components]))

(defn maybe-status [result status]
  (if (:status result)
    result
    (assoc result :status status)))

(defn maybe-content-type [result content-type]
  (if (get-in result [:headers "Content-Type"])
    result
    (assoc-in result [:headers "Content-Type"] content-type)))

(defn wrap-result [handler]
  (fn [req]
    (let [result (handler req)]
      (cond
        (map? result)
        (->
          result
          (maybe-status 200)
          (maybe-content-type "text/html"))

        :else
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body result}))))

(defn handle-route [service setup req]
  (let [result ((:handler service) req)]
    ;; Call the render function with the result params merged in.
    ((:render-fn setup)
     (update req :params merge result))))

(defn routing [service-list req]
  ;; We need to step through the service list AND each of their setup rules.
  ;; The cleaner, faster way would be to have a separate function that transforms our service-list
  ;; into a flat and indexed map to find the matching endpoints quicker.
  (loop [[s & ss :as slist] service-list
         [setup & setuplist] (:setup s)
         fallback nil]
    (cond
      ;; Can't find a suitable match, index page
      (nil? s)
      (if fallback
        (handle-route (first fallback) (second fallback) req)
        "404 route not found and no fallback specified")

      ;; Done with setup for this service endpoint, try next.
      (nil? setup)
      (recur ss (:setup (first ss)) fallback)

      ;; Match...
      (and
        (= (:type setup) :http)
        (= (:uri req) (:path setup))
        (contains? #{:any (:request-method req)} (:method setup)))
      (handle-route s setup req)

      ;; Else if this is fallback, pick it up it
      (= (:type setup) :http-fallback)
      (recur slist setuplist [s setup])

      ;; No match, continue.
      :else
      (recur slist setuplist fallback))))

(defonce memory-store (memory/memory-store))

(defn wrap-middleware [app]
  (-> app
    wrap-result
    ring.middleware.anti-forgery/wrap-anti-forgery
    ring.middleware.keyword-params/wrap-keyword-params
    ring.middleware.params/wrap-params
    (ring.middleware.session/wrap-session {:store memory-store})
    (res/wrap-resource "public")))

(defn app [service-list-atom req]
  (let [handler
        (->
          (partial #'routing @service-list-atom)
          (wrap-middleware))]
    (handler req)))

(defonce server (atom nil))

(defn stop []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start [service-rules]
  (let [service-list-atom (atom (service-rules))]
    (add-watch service-rules ::update
      (fn [k r o n]
        (reset! service-list-atom (n))))

    (reset!
      server
      (http-kit/run-server
        (partial #'app service-list-atom) {:port 8080}))))

(defn restart [service-rules]
  (stop)
  (start service-rules))