(ns services.reload
  (:require
   [hawk.core :as hawk]
   [clojure.java.io :as io]
   [clojure.tools.namespace.parse :as parse])
  (:import
   [java.io PushbackReader]))

(defonce watcher-atom (atom nil))

(defn reload-file [_ {:keys [file]}]
  (try
    (when file
      (let [n (second (parse/read-ns-decl (PushbackReader. (io/reader file))))]
        (println "Reloading:" n)

        (require n :reload)))
    (catch Exception e
      (println "Exception loading:" (.getMessage e))
      (loop [ex (.getCause e)]
        (cond
          (nil? ex) nil

          :else
          (do
            (println " - " (.getMessage ex))
            (recur (.getCause ex))))))))


(defn start []
  (reset! watcher-atom
    (hawk/watch!
      [{:paths ["src"]
        :filter
        (fn [_ {:keys [file kind]}]
          (and
            (.isFile file)
            (contains? #{:modify :create} kind)
            (or
              (.endsWith (.getName file) ".cljc")
              (.endsWith (.getName file) ".clj"))))
        :handler #'reload-file}])))

(defn stop []
  (when-let [watcher @watcher-atom]
    (hawk/stop! watcher)
    (reset! watcher nil)))

(defn service-rules [] [])