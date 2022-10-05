(ns orchestration.servicedef)


;; Service registry

;; 

(defmacro defservices [nm & service-vars]
  `(do
     ;; Infer requiring namespaces so that we don't have to double this up.
     (require
       ;; Just throw in something for it to do in case the below list is blank.
       '[util.core :as u]
       ~@(->>
           service-vars
           (remove #(str/blank? (namespace (second %))))
           (map #(list (symbol "quote") (vector (symbol (namespace (second %))))))))

     ;; Create the defservice function that will yield the final service endpoint list
     (defn ~nm []
       (mapcat
         (fn [s#]
           (if (var? s#)
             (s#)
             [s#]))
         [~@service-vars]))

     ;; Trigger a watcher change when any of the underlying vars change (to support dynamic reloading)
     (doseq [s# [~@service-vars]]
       (add-watch s# ::service-watch
         (fn [k# r# o# n#]
           (try
             (alter-var-root (var ~nm) identity)
             (catch Exception e#
               (.printStackTrace e#))))))))


(defn foo [])

(defservices blah
  #'services.http/service-rules
  #'services.reload/service-rules
  #'foo)




