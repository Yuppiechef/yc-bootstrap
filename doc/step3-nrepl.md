## Embedded nrepl support

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step3-cleanup

This is mostly a convenience measure for your first app so that everything runs together in the same way as your 'production' app. You will want to check whether you want to actually have this exposed in production.

Add nrepl into the `:deps` map at `deps.edn`:

```clojure
nrepl/nrepl "0.9.0
```

In the start namespace, add:

```clojure
(:require
  [nrepl.server :as nrepl])
```

and change the `-main` function to:

```clojure
(defn -main [& args]
  (println "Starting nrepl server")
  (nrepl/start-server :port 7888)
  (.read (System/in))
  (println "The end."))
```

The `.read` is just to make the thread block until there's some kind of input - this will go away when we have an HTTP server running.

In VSCode, you can now click on the REPL at the bottom left and 'Connect to running REPL' at the address `localhost:7888` - you should be able to `alt + enter` on your code in the start namespace and see results, as well as use the REPL.
