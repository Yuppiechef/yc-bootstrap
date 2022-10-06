(ns web.index
  (:require
   [rum.core :as rum]
   [web.components :as components]
   [orchestration.servicedef :as s]))

(defn test-page [req]
  (println "TEST PAGE")
  {:success true :name (:uri req)})

(defn test-page-render [req]
  (println "TEST PAGE RENDER: " (pr-str (:params req)))
  (str
    "Test " (get-in req [:params :name])))

;; No-op
(defn index [req]
  {:success true})

(defn index-render [req]
  (let [app-atom (atom {:name "Clojure" :count 10})]
    (rum/render-static-markup
      (components/index
        (components/main-page app-atom)
        app-atom))))


(defn service-rules []
  (println "RELOADING SERVICE RULES")
  [(s/service #'test-page #{}
     (s/web :get "/test.htm" #'test-page-render)
     (s/web :get "/foo.htm" #'test-page-render))

   (s/service #'index #{}
     (s/web-fallback #'index-render))])
