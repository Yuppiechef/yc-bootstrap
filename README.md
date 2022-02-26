# Breakdown Notes:

This is not a production ready process, but is for showcasing the rough architecture and structure that goes into building a project like this from scratch.

Currently tagged step: 3

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

## HTTP server & routing

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

## Unified endpoints

## Clojurescript, shadow-cljs initial

## Rum & Server side rendering support.

## Websockets

## Persistence: Datomic

## Persistence: MySQL

## Queuing: Kafka

## Configuration

## Profiles

# VSCode vs intellij Calva Notes


# Better in VSCode
Quicker startup
Lightweight memory footprint

# Better in Intellij
Java integration (autocomplete & bytecode decompilation)
Namespace Require auto-management
