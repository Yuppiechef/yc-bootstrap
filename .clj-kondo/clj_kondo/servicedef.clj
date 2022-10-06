(ns clj-kondo.servicedef
  (:require [clj-kondo.hooks-api :as api]))

#_(s/defservices service-rules
    #'orchestration.servicedef/service-rules
    #'api.activity.assignment/service-rules
    #'foo)

;; to

#_(def service-rules
    (do
      (require 'orchestration.servicedef)
      (require 'api.activity.assignment))
    [#'orchestration.servicedef/service-rules
     #'api.activity.assignment/service-rules
     #'foo])

(defn rewrite [node]
  (let [[nm & rules] (rest (:children node))
        result
        (with-meta
          (api/list-node
            (list*
              (api/token-node 'def)
              [nm
               (api/list-node
                 (list*
                   (api/token-node 'do)
                   (concat
                     (->>
                       rules
                       (filter #(namespace (:value (first (:children %)))))
                       (map
                         (fn [r]
                           (with-meta
                             (api/list-node
                               (list*
                                 (api/token-node 'require)
                                 [(api/list-node
                                    (list*
                                      (api/token-node 'quote)
                                      [(api/token-node
                                         (symbol (namespace (:value (first (:children r))))))]))]))
                             (meta r)))))
                     [(api/vector-node
                        (vec rules))])))]))
          (meta node))]
    result))

(defn defservices [{:keys [node]}]
  #_(require 'clj-kondo.servicedef :reload-all)
  (let [new-node (rewrite node)]
    {:node new-node}))
