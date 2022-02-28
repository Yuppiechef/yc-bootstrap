# Breakdown Notes:

This is not a production ready process, but is for showcasing the rough architecture and structure that goes into building a project like this from scratch.

Note, there are many libraries out there that accomplish you could flip out instead of the ones used below. I am using the ones we use at Yuppiechef, but you may decide for yourself what is appropriate for your usecase. A starting point for the menu of options you have available is over here: https://www.clojure-toolbox.com/

Currently tagged step: 6

## Step 1 - Initial Basic setup

 - Calva getting started
 - Clojure getting started: https://clojure.org/guides/getting_started
    - Get 'clojure' CLI installed
 - Create `yc-bootstrap` folder
 - Open in vscode, create deps.edn, follow https://clojure.org/guides/deps_and_cli

You should now have a very basic project going - good time to get some git integration going -- Finish up with a setup where you have only a single project folder and single deps.edn, else things will get weird fast.

Enter the following in your .gitignore:

```gitignore
.clj-kondo/.cache
.cpcache
.lsp
.calva/output-window
.nrepl-port
```

Also, create a cljfmt.edn file with the contents:
```clojure
{:indents ^:replace
 {#".*" [[:inner 0]]}
 :remove-multiple-non-indenting-spaces? true
 :insert-missing-whitespace? true}
 ```

Then, go to VSCode settings and filter for `cljfmt` - set the `Calva > Fmt: Config Path` to `cljfmt.edn`. This will get the code formatted in a consistent way. I also found better mileage unchecking the `New Indent Engine` option.

## Step 2 - Setup uberjar and run process from jar.

https://github.com/tonsky/uberdeps

Do the Project Setup - Quick and dirty (uberjar inside current deps.edn) 

 - run `clj -M:uberdeps` to create the jar and check it builds properly
   - You should now have a target/yc-bootstrap.jar file
   - `java -jar yc-bootstrap.jar` should give you an 'no main manifest' error.

Fix the manifest by going through the next section: Creating an executable jar

 - add (:gen-class) to hello namespace form
 - change `run` to (defn -main [& args] ...)
 - create `classes` folder
 - add 'classes' to deps.edn sources folder 
      `:paths ["src" "classes"]`
 - add clojure dependency
   - use `clj -X:deps find-versions :lib org.clojure/clojure` to find latest version
 - compile the thing: `clj -M -e "(compile 'hello)"`
 - uberjar + manifest `clj -M:uberdeps --main-class hello`
 - test by:

```bash
cd target
java -jar yc-bootstrap.jar
```

you should see a successful run happening:
```
Hello world, the time is 07:24 PM
```

Throw this into a new `uberjar.sh` file so that you can run it quickly:
```bash
#!/bin/bash

clj -M -e "(compile 'hello)"
clj -M:uberdeps --main-class hello
```

then chmod to make it executable.
```bash
chmod +x uberjar.sh
```

Congratulations, you now have an artifact you can deploy on a live environment.

To run it directly without creating a java archive, you can still:
`clj -X hello/-main`

Some bookeepping before committing and tagging:

 - Add `target` and `classes` to `.gitignore` file

There are things we can do to speed up the AOT process, like lazily require the namespaces from the main class - but that's out of scope for here, as that is a production level optimization.

## Some clearing up in prep

Rename the main file to `start.clj`, remove anything else except for a very simple 'hello world' `-main` function in the file.

Remove any other clj source files.

Update the `uberjar.sh` file to use `start` instead of `hello` and test.

## Embedded nrepl support

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

## HTTP server

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

## Hot reloading

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

## HTTP Routing

We could go off and use compojure (https://github.com/weavejester/compojure) directly, but in interest of showing the plumbing that makes this work, I think it worthwhile to roll our own lightweight mechanism instead. It won't have all the bells and whistles, but it'll do for our purposes.

In our `start.http` namespace, let's have a look at the request format - update the `app` function to look like this:

```clojure
(defn app [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (pr-str req)})
```

Refresh the page and you'll see a very raw http request with headers and some additional info around. Try out some url's like http://localhost:8080/test.htm and notice the `:uri "/test.htm"` and the `:request-method :get` bits. We'll build our routing off that.

Let's do a manual cond to route `test.htm` to its own 'page', as it were.

```clojure
(defn test [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Test Page!"})
```

And just to make it simpler, we also move the initial page to an `index` function:

```clojure
(defn index [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (pr-str req)})
```

Now then, let's 'route' to the two different pages!

```clojure
(defn app [req]
  (cond
    (and
      (= (:request-method req) :get)
      (= (:uri req) "/test.htm"))
    (test req)

    :else
    (index req)))
```

Done! This is effectively all that's behind every single routing library out there - there's cleverer syntax, there's optimisations for branching efficiently and whatnot, but there's no magic.

Repeating the whole status 200 thing is a bit tiresome, so how about we assume that if we don't respond with a map, that it's wrapped in a 200 response? Also, if it is a map, and there's no status or `"Content-Type"` header, we add it in?

First, pull the routing bit out to make this more sensible:

```clojure
(defn routing [req]
  (cond
    (and
      (= (:request-method req) :get)
      (= (:uri req) "/test.htm"))
    (test req)

    :else
    (index req)))
``` 

Then a function that would add status if none exists on the result map:

```clojure
(defn maybe-status [result status]
  (if (:status result)
    result
    (assoc result :status status)))
```

And a similar one for the Content-type header:

```clojure
(defn maybe-content-type [result content-type]
  (if (get-in result [:headers "Content-Type"])
    result
    (assoc-in result [:headers "Content-Type"] content-type)))
```

We rework the `app` function by calling the routing function and checking the result:

```clojure
(defn app [req]
  (let [result (routing req)]
    (cond
      (map? result)
      (->
        result
        (maybe-status 200)
        (maybe-content-type "text/html"))

      :else
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body result})))
```

This should work, but let's simplify our `test` and `index` pages slightly:

```clojure
(defn test [req]
  "Test Page")

(defn index [req]
  {:body (pr-str req)})
```

Done. In time, we'll pull this out a bit further into actually doing some cleverer things so you can have dynamic routing, but for now, this will suffice.

## Serving up static files (Side Quest: Middleware).

Before we head into ClojureScript land, we'll need a way to serve up static files of some kind. It's idiomatic to use the `resources/public` folder for that, so let's create that in the terminal within your `yc-bootstrap` folder with:

```bash
mkdir -p resources/public
```

Let's create a favicon.ico - but first, we need one, so let's head over to https://favicon.io/emoji-favicons/ and download one. https://favicon.io/emoji-favicons/cat-with-wry-smile is the one I'm going to go for, because why not?

Unzip the file and copy the `favicon.ico` over to `resources/public` folder.

Now, let's serve it up! We could just add cases to the `routing` function, but that would get super painful as we add a bajillion more files, so let's create a middleware wrapper to handle it.

Middleware wrappers simply intercept the `app` function (called a 'handler') and decide to add something to the `req` map and call the handler - then after the call, it has a chance to inspect the result and do something else.

Let's rename our `app` function and call it `base-app` then create a brand spanking new `app` function that we'll use as a wrapping point.

Sidenote: Traditionally, you would wrap the `#'app` directly where the `http-kit/run-server` is called - this is more performant, since it won't need to setup the middleware function chain every time, but I don't want to restart the http server every time I make a change here.

A identity middleware function (a middleware function that does nothing) simply takes a `handler` function, and returns a function that accepts a `request` argument, and calls the handler function with the request argument:

```clojure
(defn identity-wrapper [handler]
  (fn [req]
    (handler req)))
```

This is important, since we can now create an `app` function that looks like:

```clojure
(defn app [req]
  (let [handler (identity-wrapper base-app)]
    (handler req)))
```

and we have the recipe we need to chain a bunch of middleware together, since every middleware function simply responds with another, spiced up, handler function.

In fact, our handling of the 200 result and content-type is a bit weird in the `base-app`, so let's promote that to a middleware function:

```clojure
(defn wrap-result [handler]
  (fn [req]
    (let [result (handler req)]
      (cond
        (map? result)
        (->
          result
          (maybe-status 200)
          (maybe-content-type "text/html"))

        :else
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body result}))))
```

That looks awfully like the `base-app`, doesn't it? So let's remove that functionality from `base-app`, which leaves it looking like:

```clojure
(defn base-app [req]
  (routing req))
```

huh. ok, let's just remove `base-app` and just use `routing` directly. Reworking `app` into:

```clojure
(defn app [req]
  (let [handler 
        (wrap-identity 
          (wrap-result routing))]
    (handler req)))
```

This is a bit hard to read, so let's thread it:

```clojure
(defn app [req]
  (let [handler 
        (->
          routing
          (wrap-result)
          (wrap-identity))]
    (handler req)))
```

This means the middleware functions will be called _bottom up_ on the way in and _top down_ on the way out from serving a request. Note that _each_ middleware has the ability to decide to simply sidestep the original handler and do something entirely differently - like serving a file instead.

Add the following to your namespace `:require` form:

```clojure
[clojure.java.io :as io]
```

Then we write a bit that can take a url and turn it into a valid HTTP response map (http-kit knows how to handle InputStream `:body` objects):

```clojure
(defn url-response
  "Return a response for the supplied URL."
  {:added "1.2"}
  [^URL url]
  (let [conn (.openConnection url)
        last-mod (.getLastModified conn)]
    {:body (.getInputStream conn)
     :status 200
     :headers
     {"Content-Length" (.getContentLength conn)
      "Last-Modified"
      (when-not (zero? last-mod)
        (Date. last-mod))}}))
```

We create a the `wrap-resource` function:

```clojure
(defn wrap-resource [handler foldername]
  (fn [req]
    (let [f (io/resource (str foldername "/" (subs (:uri req) 1)))]
      (cond
        f
        (url-response f)

        :else
        (handler req)))))
```

And finally add it to the middleware of `app`, right at the end of the chain (so it happens first) - might as well also remove the identity wrapper at this point:

```clojure
(defn app [req]
  (let [handler
        (->
          routing
          (wrap-result)
          (wrap-resource "public"))]
    (handler req)))
```

Here you see the way you can pass arguments to middleware functions.

If you reload, you should see your chosen favicon - hurrah! You can check that this by throwing together a `.htm` page into the `public` folder and check that it loads ok.

Now if you poke around, you might notice an a _folder_ serves up a file list - it also probably allows weird things like `../../etc/passwd`. oops. Don't use this in production. But let's also make `/` not try `wrap-resource` - fix this in the `wrap-resource`:

```clojure
    (let [f 
          (when-not (= (:uri req) "/")
            (io/resource (str "public/" (subs (:uri req) 1))))]
```



The more complete way of implementing this (which handles a bunch of corner cases) is to include the Ring core middleware from https://github.com/ring-clojure/ring - Add the following dep to the `deps.edn` :

```clojure
ring/ring-core {:mvn/version "1.9.5"}
```

Now `ctrl-c` on your main process and fire it up again - it should download a few dependencies and we're off again!

Add the resource namespace to your `:require`

```clojure
[ring.middleware.resource :as res]
```

if you replace our `wrap-resource` in `app` with `res/wrap-resource`, it should work exactly the same. Remove the `wrap-resource` and `url-response`.If you poke around the code here https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/util/response.clj - you may be able to tease out the exact same foundation code (that's where I got it from, after all), but this takes care of a bit more pieces.

An aside - the reason why we want to use `io/resource` instead of `io/file` is that the files may be packaged _inside_ the jar archive. This lets us serve it directly from there too.

In production, you'll want to pull the `resources/public` folder out and serve it with something that can handle straight files a whole lot more efficiently, like nginx.

## Clojurescript, shadow-cljs initial bits.

## Rum & Server side rendering support.

## Websockets

## Persistence: Datomic

## Persistence: MySQL

## Queuing: Kafka

## Unified endpoints

## Configuration

## Profiles

# VSCode vs intellij Calva Notes


# Better in VSCode
Quicker startup
Lightweight memory footprint

# Better in Intellij
Java integration (autocomplete & bytecode decompilation)
Namespace Require auto-management
