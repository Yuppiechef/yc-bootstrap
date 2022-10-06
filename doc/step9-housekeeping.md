## A bit of housekeeping

Refactor stuff out of `services.http` namespace, the routes and index pages doesn't belong in the actual http service component at all. Create a `web.index` namespace.

Move the `test`, `index` and `routing` functions out to `web.index`, then refactor `start` and `restart` and add `routing` as an argument there:

```clojure
(defn start [routing]
  (reset! server
    (http-kit/run-server
      (partial #'app routing) {:port 8080})))
```

Note the `(partial #'app routing)` form. It will now pass the routing function to the `app` on request handling. We'll need to update the `app` function definition to add the `routing` arg.

```clojure
(defn app [routing req]
```

Now the http 'service' is standalone and doesn't link to our actual underlying logic - we'll bring some more plumbing in later, but for now, this will be fine.

Last thing we'll need to do to make it actualy compile is go to the `start` namespace and pass it into the `http/start` call, add `[web.index :as index]` to the require, and change `http/start` to `(http/start index/routing)` 

Move components.cljc to `web` folder, update the namespace to web.components (use rename on vscode so that it fixes all the require's automatically)

Check the require's in your `web.index` file.

Fire the server up to check it's all good:

`clj -X start/-main`