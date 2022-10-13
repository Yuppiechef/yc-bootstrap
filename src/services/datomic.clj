(ns services.datomic
  (:require
   [datomic.client.api :as d]))

(def schema
  [{:db/ident :guestbook-entry/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A person's name"}

   {:db/ident :guestbook-entry/message
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Guestbook entry message"}])

(defonce server (atom nil))

(defn client []
  (:client @server))

(defn conn []
  (:conn @server))

(defn db []
  (d/db (:conn @server)))


(defn start []
  (let [datomic-client (d/client {:server-type :dev-local :system "dev"})
        connection (d/connect client {:db-name "dev"})]

    ;; Just create the db/schema in case it isn't already done.
    (d/create-database client {:db-name "dev"})
    (d/transact conn {:tx-data schema})

    (reset! server
      {:client datomic-client
       :conn connection})))

(defn stop []
  (reset! server nil))

(defn restart []
  (stop)
  (start))