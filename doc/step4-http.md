## HTTP server

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step4-http

We'll be using http-kit https://github.com/http-kit/http-kit as the server for HTTP calls.

We *won't* cover setting up https, since you want a different termination point for that in production (nginx, most likely).

Add the following to your `deps.edn`, `:deps` map:

```clojure
http-kit/http-kit {:mvn/version "2.5.3"}
```

First thing is to create a namespace that we can bundle all the http specific stuff in, and since it's a backend service, create a `src/services` folder.

Create a new file `http.clj` in there and open it up.

Add the `:require` to the namespace form:

```clojure
(:require
   [org.httpkit.server :as http-kit])
```

Then, to start with, we'll just lift the code here: https://http-kit.github.io/server.html#stop-server 

Fix the call to `run-server` with `http-kit/run-server` and change `defn -main [& args]` to a more suitable `defn start []`

A restart function might be a good idea, but I'll leave that as an exercise for the reader.

In the `start` namespace, add the following to the `:require` namespace form:

```clojure
[services.http :as http]
```

And insert an `(http/start)` in the `-main` function.

Kill any existing process and start it again with 

```bash
clj -X start/-main
```

If that starts properly, you should be able to browse to http://localhost:8080 and you'll see `hello HTTP!`

You'll notice that I prefer using this kind of process instead of doing a local-process IDE level jack-in. This is simply because there's less dependency on tooling, so less surprises when it comes to production. You also have better flexibility, so you can run the process wherever you want.

However, nothing stops you from using jack-in and manually firing up the system.

You should be able to connect to the nrepl now, make a change to the `:body` of the `app`, `alt+enter` to push the form to the repl and refresh the browser.