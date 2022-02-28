## Hot reloading

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step5-hotreloading

Right, so having to actually push changes via a repl is uber painful. Let's fix that right now with https://github.com/wkf/hawk

Hawk listens to inotify messages from the OS (or polling, as fallback) and invokes a function you give it, every time there's a file change.

Again, for 'real world' use, you usually want some kind of debounce/collecting mechanism in here so that you get all changes reloaded only a bit after it's likely finished writing.

As always, add the library dependency to `deps.edn` - this may be a useful place to use an alias so you don't need the dep unless you specify dev mode, but I'm not worrying about that for now.

We'll also need a simple way to parse a namespace name so we can trigger the reload correctly, hence the `tools.namespace` dep.

```clojure
hawk/hawk {:mvn/version "0.2.11"}
org.clojure/tools.namespace {:mvn/version "1.0.0"}
```

Now, there's a fair bit of code involved in this one, so create a new `src/services/reload.clj` file.

Edit the namespace declaration to have the `require` and `import` bits we'll need:

```clojure
(ns services.reload
  (:require
   [hawk.core :as hawk]
   [clojure.java.io :as io]
   [clojure.tools.namespace.parse :as parse])
  (:import
   [java.io PushbackReader]))
```

We're going to need a place to put the watcher state, so that we can stop it later, so:

```clojure
(defonce watcher-atom (atom nil))
```

And then we add a `start` function that initializes hawk to do its thing.

```clojure
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
```

It'll complain about `reload-file` being missing, so lets put it above the `start` function - This is a bit convoluted, mostly so that errors don't break stuff and so you have decent info to work with when it doesn't work:

```clojure
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
``` 
For the `stop` function, we'll just hit `hawk/stop!` and clear the atom:

```clojure
(defn stop []
  (when-let [watcher @watcher-atom]
    (hawk/stop! watcher)
    (reset! watcher nil)))
```

Last thing to do is to pop that into the start namespace and `-main` function:

```clojure
:require [services.reload :as reload]

...

;; In main function, efore the blocking read.
   (reload/start)

```

et voila. Go edit the services.http file, change the body message, save and reload the browser - it should just update.