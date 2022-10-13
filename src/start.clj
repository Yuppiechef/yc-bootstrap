(ns start
  (:require
   [nrepl.server :as nrepl]
   [services.http :as http]
   [services.reload :as reload]
   [services.websocket :as websocket]
   [services.datomic :as datomic]
   [orchestration.servicedef :as s])
  (:gen-class))

(s/defservices service-rules
  #'services.websocket/service-rules
  #'web.guestbook/service-rules
  #'web.index/service-rules)


(defn -main [& args]
  (println "Starting nrepl server: port 7888")
  (nrepl/start-server :port 7888)

  (println "Starting Datomic client.")
  (datomic/start)

  (println "Starting websocket service.")
  (websocket/start #'service-rules)

  (println "Starting HTTP server: http://localhost:8080")
  (http/start #'service-rules)
  (reload/start)
  (.read (System/in))
  (println "The end."))