(ns web.index
  (:require
   [rum.core :as rum]
   [web.components :as components]
   [util.flow :as flow]
   [orchestration.servicedef :as s]))

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
