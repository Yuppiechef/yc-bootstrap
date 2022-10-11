# Breakdown Notes:

This is not a production ready process, but is for showcasing the rough architecture and structure that goes into building a project like this from scratch. *NOT DIRECTLY FOR PRODUCTION USE* - there's a _ton_ of optimizations and hardening things you'd still need to do on top of this.

Also important to realize that we're not trying to build a product of any kind - purely focussing on the plumbing that a project would need to function. You're welcome to expand and morph it into whatever suits your fancy.

Note, there are many libraries out there that accomplish you could flip out instead of the ones used below. I am using the ones we use at Yuppiechef, but you may decide for yourself what is appropriate for your usecase. A starting point for the menu of options you have available is over here: https://www.clojure-toolbox.com/

Quickstart to see it in action:

 - Open in VSCode (`Dev Containers: Open Folder in Container...`)
 - In Terminal view (inside VSCode), run `clj -X start/-main`
 - Split the tab (or create a new terminal), run `shadow-cljs watch app`
 - Browse to `http://localhost:8080`

Developing this from scratch.

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
 - [Step 9 - Housekeeping](doc/step9-housekeeping.md)
 - [Step 10 - Service Lifecycle](doc/step10-servicelifecycle.md)
 - [Step 11 - Dynamic Services](doc/step11-dynamicservices.md)
 - [Step 12 - Websockets](doc/step12-websockets.md)
 - [Step 13 - Pages (screens)](doc/step13-pages.md)



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
 - Quicker startup
 - Lightweight memory footprint
 - Totally Free!

# Better in Intellij
 - Better Java integration (autocomplete)
 - Copy/Paste Hiccup conversion!
