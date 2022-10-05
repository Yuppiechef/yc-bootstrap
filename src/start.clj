(ns start
  (:require
   [nrepl.server :as nrepl]
   [web.index :as index]
   [services.http :as http]
   [services.reload :as reload])
  (:gen-class))


(defn -main [& args]
  (println "Starting nrepl server")
  (nrepl/start-server :port 7888)
  (println "Starting HTTP server")
  (http/start index/routing)
  (reload/start)
  (.read (System/in))
  (println "The end."))