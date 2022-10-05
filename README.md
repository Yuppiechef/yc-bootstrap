# Breakdown Notes:

This is not a production ready process, but is for showcasing the rough architecture and structure that goes into building a project like this from scratch.

Note, there are many libraries out there that accomplish you could flip out instead of the ones used below. I am using the ones we use at Yuppiechef, but you may decide for yourself what is appropriate for your usecase. A starting point for the menu of options you have available is over here: https://www.clojure-toolbox.com/


 - [Step 0 - Clojure + Reference](doc/step0-reference.md)
 - [Step 1 - Initial Basic setup](doc/step1-initial.md)
 - [Step 2 - Setup uberjar and run process from jar](doc/step2-uberjar.md)
 - [Step 3 - Embedded nrepl support](doc/step3-nrepl.md)
 - [Step 4 - HTTP server](doc/step4-http.md)
 - [Step 5 - Hot reloading](doc/step5-hotreload.md)
 - [Step 6a - HTTP Routing](doc/step6a-httprouting.md)
 - [Step 6b - Serving up static files](doc/step6b-static-files.md)
 - [Step 7 - ClojureScript](doc/step7-clojurescript.md)
 - [Step 8 - Rum & SSR](doc/step8-rum.md)


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

## Service endpoint lifecycle

Glossary:
 - API Host: Hosts endpoints and makes them available on their platform
 - Service Endpoint: An endpoint definition that will get exposed by an API Host.
 - Service Backend: Dependencies required in context of an endpoint for fulfilling its function.


First, implement the notion of a 'service backend'. These are services that an endpoint depend on in order to complete its job.

For example, an endpoint that updates an order on a database and sends a message on a queue needs a connection to the database (along with a transaction for the duration of the endpoint handling) and a connection to the queueing system. It does not need to know how they work, just that they need access to the specific services in order to work.

We want to be able to define an endpoint handler function, specify a set of service backends it will need, permission requirements and API Host endpoint exposure configuration (ie. is this a generic api, does it consume from specific topics, perhaps run as a cron?)

For system startup, we want to:

1. Get a service registry populated with the above service endpoint configuration.
2. Collect and understand which service backends the endpoints need.
3. Start up the service backends, enabling the endpoints as their dependencies are satisfied.
4. Report on global final state.

We can generalize this a little by assuming that the service registry is allowed to change during runtime, so that the same code is used in startup as changes:

1. Service registry changes are pushed (pushed by `defservices`)
2. Trigger a diff between subsection of registry:
  a. Changed endpoints (generically supports addin or removing full endpoints):
    - Shutdown any removed API Host config.
      o remove & wait for existing consumers to finish)
    - Startup any new API Host config.
    - Reconfigure any _changed_ API Host config.

   

## Websockets

## Sessions

## Pages (screens), History & Actions

## Modals


## Persistence: Datomic

## Persistence: MySQL

## Queuing: Kafka

## Unified endpoints

## Configuration

## Profiles

## Donut.system?

# VSCode vs intellij Calva Notes


# Better in VSCode
Quicker startup
Lightweight memory footprint
Totally Free!

# Better in Intellij
Java integration (autocomplete & bytecode decompilation)
Namespace Require auto-management
Copy/Paste Hiccup conversion!
