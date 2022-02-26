(ns hello
  (:require [hello-time :as ht])
  (:gen-class))

(defn -main [& args]
  (println "Hello world, the time is" (ht/time-str (ht/now))))