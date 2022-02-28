(ns components
  (:require
   [rum.core :as rum])
  #?(:clj
     (:import
      (org.apache.commons.codec.binary Base64))))

(rum/defc main-page < rum/reactive [app-atom]
  (let [state (rum/react app-atom)]
    [:div
     [:h1 "Hello " (:name state) " World"]
     [:p "We've got " (:count state)]
     [:button
      {:on-click #(swap! app-atom update :count (fnil inc 0))}
      "inc"]]))

(rum/defc index [body app-atom]
  [:html
   [:head
    [:title "ClojureScript"]]
   [:body
    [:div#reactMount
     {:data-state
      #?(:clj (Base64/encodeBase64String (.getBytes (pr-str @app-atom)))
         :cljs nil)
      :dangerouslySetInnerHTML
      {:__html
       (rum/render-html body)}}]
    [:script {:src "/js/main.js" :language "javascript"}]]])