(ns web.guestbook
  (:require
   #?@(:clj
       [[datomic.client.api :as d]
        [services.datomic :as datomic]])
   [orchestration.servicedef :as s]
   [rum.core :as rum]

   [util.comms :as comms]
   [util.flow :as flow]))

(rum/defc guestbook-entry [entry]
  [:div
   [:i (str (:db/txInstant entry))]
   [:br]
   [:b (:guestbook-entry/name entry)]
   [:div
    (:guestbook-entry/message entry)]
   [:hr]])

(rum/defc guestbook-form < rum/reactive [app-atom]
  (let [form-data-atom (rum/cursor-in app-atom [:guestbook :form])
        form-data (rum/react form-data-atom)]
    [:form
     {:on-submit
      (fn [e]
        (.preventDefault e)
        (comms/send-msg :guestbook/create @form-data-atom))}
     [:label "Name:"]
     [:br]
     [:input
      {:on-change #(swap! form-data-atom assoc :guestbook-entry/name (.. % -target -value))
       :type "text"
       :value (or (:guestbook-entry/name form-data) "")}]
     [:br]

     [:label "Message"]
     [:br]
     [:textarea
      {:rows 10
       :on-change #(swap! form-data-atom assoc :guestbook-entry/message (.. % -target -value))
       :value (or (:guestbook-entry/message form-data) "")}]
     [:br]
     [:input
      {:type "submit"
       :value "Submit"}]]))

(rum/defc guestbook < rum/reactive [app-atom]
  (let [entries (rum/cursor-in app-atom [:guestbook :entries])]
    (into
      [:div
       [:h1 "Guestbook entries!"]
       (guestbook-form app-atom)]
      (map guestbook-entry (rum/react entries)))))

(defmethod comms/receive :guestbook/load [app-atom e]
  (swap! app-atom assoc-in [:guestbook :entries] (:entries e)))

(defmethod flow/render-screen :guestbook [app-atom _]
  (comms/send-msg :guestbook/load {})
  (guestbook app-atom))

(defn guestbook-load [req]
  #?(:clj
     {:entries
      (->>
        (d/q '[:find
               (pull ?e [:guestbook-entry/name :guestbook-entry/message])
               (pull ?t [:db/txInstant])
               :where [?e :guestbook-entry/name _ ?t]]
          (datomic/db))
        (map
          (fn [[e t]]
            (merge e t)))
        (sort-by :db/txInstant)
        (reverse))}))

(defn guestbook-create [{:keys [params] :as req}]
  #?(:clj
     (let [entry
           {:guestbook-entry/name (:guestbook-entry/name params)
            :guestbook-entry/message (:guestbook-entry/message params)}]
       (d/transact (datomic/conn) {:tx-data [entry]})
       {:entry entry})))

(defmethod comms/receive :guestbook/create [app-atom e]
  (swap! app-atom update-in [:guestbook :entries] conj (:entry e)))

(defn service-rules []
  [(s/service #'guestbook-load #{}
     (s/api "guestbook.load"))

   (s/service #'guestbook-create #{}
     (s/api "guestbook.create"))])
