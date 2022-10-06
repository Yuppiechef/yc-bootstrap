(ns start
  (:require
   [nrepl.server :as nrepl]
   [services.http :as http]
   [services.reload :as reload]
   [orchestration.servicedef :as s])
  (:gen-class))

(s/defservices service-rules
  #'web.index/service-rules)


(defn -main [& args]
  (println "Starting nrepl server: port 7888")
  (nrepl/start-server :port 7888)
  (println "Starting HTTP server: http://localhost:8080")
  (http/start #'service-rules)
  (reload/start)
  (.read (System/in))
  (println "The end."))