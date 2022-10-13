(ns web.components
  (:require
   #?@(:clj
       [[ring.middleware.anti-forgery]])
   [rum.core :as rum]
   [util.comms :as comms]
   [util.flow :as flow]
   [util.urlstate :as urlstate]
   [web.guestbook])
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
      "inc"]

     [:button
      {:on-click #(comms/send-msg :components/commtest {:msg "Hello World!"})}
      "Send Message"]

     [:button
      (urlstate/href-props app-atom "guestbook")
      "Open Guestbook"]

     [:button
      (urlstate/href-props app-atom "second")
      "To second page"]]))

(defmethod flow/render-screen :index
  [app-atom _]
  (main-page app-atom))

(defmethod comms/receive :components/commtest [app-atom e]
  (println "Receiving comms test")
  (swap! app-atom assoc :count 0))

(defn csrf-div []
  #?(:clj
     (let [csrf-token (force ring.middleware.anti-forgery/*anti-forgery-token*)]
       [:div#sente-csrf-token {:data-csrf-token csrf-token}])))

(rum/defc second-page < rum/reactive [app-atom]
  [:div
   [:h1 "This is another page"]
   [:button
    (urlstate/href-props app-atom "index")
    "Back to index"]])

(defmethod flow/render-screen :second [app-atom _]
  (second-page app-atom))

(rum/defc index [app-atom body]
  [:html
   [:head
    [:title "ClojureScript"]]
   [:body
    (csrf-div)
    [:div#reactMount
     {:data-state
      #?(:clj (Base64/encodeBase64String (.getBytes (pr-str @app-atom)))
         :cljs nil)
      :dangerouslySetInnerHTML
      {:__html
       (rum/render-html body)}}]
    [:script {:src "/js/main.js" :language "javascript"}]]])