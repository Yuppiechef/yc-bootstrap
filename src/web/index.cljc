(ns web.index
  (:require
   [orchestration.servicedef :as s]
   [rum.core :as rum]
   [util.flow :as flow]
   [util.urlstate :as urlstate]
   [web.components :as components]))

(defn test-page [req]
  {:success true :name (:uri req)})

(defn test-page-render [req]
  (str
    "Test " (get-in req [:params :name])))


;; No-op
(defn index [req]
  {:success true})

(defn index-render [req]
  (let [c (or (get-in req [:session :count]) 10)
        app-atom (atom {:name "Clojure" :count c :page {:screen :index}})]

    (urlstate/set-uri! app-atom req)
    {:session (assoc (:session req) :count (inc c))
     :status 200
     :headers
     {"Content-Type" "text/html"}
     :body
     (rum/render-static-markup
       (components/index app-atom
         (flow/render-state app-atom)))}))

(defn test-api [req]
  {:success true
   :msg (str "ECHO:" (pr-str (:params req)))})


(defn service-rules []
  [(s/service #'test-page #{}
     (s/web :get "/test.htm" #'test-page-render)
     (s/web :get "/foo.htm" #'test-page-render))

   (s/service #'test-api #{}
     (s/api "components.commtest"))

   (s/service #'index #{}
     (s/web-fallback #'index-render))])
