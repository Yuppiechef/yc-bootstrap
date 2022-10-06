## Dynamically adding new services

If you were to experiment now, you'll find that if you want to create new page routes, you'll need to restart the entire app on the command line. This is super painful, so lets fix that.

The simplest first step is to change the `start.http` from taking a `service-list`, to taking a `service-rules` Var instead, evaluating it whenever a new request comes in - rename usages of `service-list` to `service-rules` in `start.http`, then update `app` to:

```clojure
(defn app [service-rules req]
  (let [handler
        (->
          (partial #'routing (service-rules))
          (wrap-result)
          (res/wrap-resource "public"))]
    (handler req)))
```

Notice that `service-rules` now gets called as a function.

And change the callsite of `http/start` in the `start` namespace (if you don't do this, you get a strange `clojure.lang.ArityException: Wrong number of args (0) passed to: clojure.lang.PersistentVector` exception, because you're trying to call the vector of service endpoints as a function - vectors can be called as functions to lookup by index, so this is a little confusing):

```clojure
(http/start #'index/service-rules)
```

(don't forget the #' else you'll be back at square one.)

Restart and test - I tweaked my `test-page` to include the uri to check that it's coming through as expected:

```clojure
(defn test-page [req]
  (println "TEST PAGE")
  {:success true :name (:uri req)})
```

Then add a `/foo.htm` route and browse to it to make sure it works.

```clojure
(defn service-rules []
  [(s/service #'test-page #{}
     (s/web :get "/test.htm" #'test-page-render))

   (s/service #'test-page #{}
     (s/web :get "/foo.htm" #'test-page-render))

   (s/service #'index #{}
     (s/web-fallback #'index-render))])
```

Notice that collapsing this achieves the same result:

```clojure
(defn service-rules []
  [(s/service #'test-page #{}
     (s/web :get "/test.htm" #'test-page-render)
     (s/web :get "/foo.htm" #'test-page-render))

   (s/service #'index #{}
     (s/web-fallback #'index-render))])
```

Which is quite handy if you have the same functionality on different routes! This is also fundamentally important later when you want to expose the same functionality to other API hosts, like Kafka or RabbitMQ.

## Optimization (not optional, for reasons given underneath)

If you play this ahead in your mind to a much larger system, you'll realize that _compiling_ every single service-rule on every request is not going to scale particularly well, so at the very least you'll want to only do this work if there's actual reloading happening - to be fair, in production we can actually rip out _all_ the dynamic reloading bits, since everything adds a bit of overhead here.

For this purpose, I've constructed a `defservices` macro which brings in the relevant code (so you don't have to duplicate in the ns form), creates a `service-rules` function and adds a watch on every underlying var so that if anything is reloaded, it will reload the parent `defservices` (which means they can also compose with each other in a tree form)

It's a good exercise to pull this macro apart and understand exactly how all of it works, but describing it fully here is beyond the scope at this point. You'll have to simply take my word for it xD

In `orchestration.servicedef` add:

```clojure
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
```

Make sure you've also got the `.clj-kondo/clj_kondo/servicedef.clj` and `.clj-kondo/config.edn` files in your project, else VSCode will not understand what it's looking at. That's also some convoluted code that's worth understanding at some point, but for now, let's move on.

In the `start` namespace, let's now create a `service-rules`:

```clojure
(s/defservices service-rules
  #'web.index/service-rules)
```

Nice thing is that we can now just add more namespaces arbitrarily in here.

Update the `http/start` accordingly, once more:

```clojure
(http/start #'service-rules)
```

Restart and things _should_ still work properly.

If you're particularly sharp, you'll have noticed that this hasn't optimized _anything_ xD `start.http/app` still calls `(service-rules)` on every request! Well observed.

We now have the tools to fix it though, but first refactor `app` to actually take a `service-list-atom` not the rules var:

```clojure
(defn app [service-list-atom req]
  (let [handler
        (->
          (partial #'routing @service-list-atom)
          (wrap-result)
          (res/wrap-resource "public"))]
    (handler req)))
```

If we simply use `(partial #'app (atom (service-rules)))` in `start`, that will now make everything work, but zero reloading.. so to make this work correctly, we need to pull that atom out and update it when `service-rules` changes:

```clojure
(defn start [service-rules]
  (let [service-list-atom (atom (service-rules))]
    (add-watch service-rules ::update
      (fn [k r o n]
        (println "Updating service rules")
        (reset! service-list-atom (n)))

    (reset!
      server
      (http-kit/run-server
        (partial #'app service-list-atom) {:port 8080}))))
```

And that connects everything together - you can add a println to `web.index/service-rules` and confirm that it only loads on when there are changes made and not on every request.

This whole optimization plays another important role when we start implementing consumers on Kafka, where we have to be proactively _pulling_ from topics and rebalancing consumers is not free, so we want to be very specific about what changes to make and when, else it becomes cumbersome to run any significantly large system.
