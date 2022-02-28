(ns services.http
  (:require
   [org.httpkit.server :as http-kit]
   [ring.middleware.resource :as res]))

(defn test [req]
  "Test Page")

(defn index [req]
  {:body (pr-str req)})

(defn routing [req]
  (cond
    (and
      (= (:request-method req) :get)
      (= (:uri req) "/test.htm"))
    (test req)

    :else
    (index req)))

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

(defn app [req]
  (let [handler
        (->
          routing
          (wrap-result)
          (res/wrap-resource "public"))]
    (handler req)))

(defonce server (atom nil))

(defn stop []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn start []
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and https://http-kit.github.io/migration.html#reload
  (reset! server (http-kit/run-server #'app {:port 8080})))

(defn restart []
  (stop)
  (start))