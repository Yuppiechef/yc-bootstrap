(ns web.index
  (:require
   [rum.core :as rum]
   [web.components :as components]))

(defn test [req]
  "Test Page")

(defn index [req]
  (let [app-atom (atom {:name "Clojure" :count 10})]
    (rum/render-static-markup
      (components/index
        (components/main-page app-atom)
        app-atom))))

(defn routing [req]
  (cond
    (and
      (= (:request-method req) :get)
      (= (:uri req) "/test.htm"))
    (test req)

    :else
    (index req)))

