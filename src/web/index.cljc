(ns web.index
  (:require
   [rum.core :as rum]
   [web.components :as components]
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
        app-atom (atom {:name "Clojure" :count c})]
    {:session (assoc (:session req) :count (inc c))
     :status 200
     :headers
     {"Content-Type" "text/html"}
     :body
     (rum/render-static-markup
       (components/index
         (components/main-page app-atom)
         app-atom))}))


(defn service-rules []
  [(s/service #'test-page #{}
     (s/web :get "/test.htm" #'test-page-render)
     (s/web :get "/foo.htm" #'test-page-render))

   (s/service #'index #{}
     (s/web-fallback #'index-render))])
