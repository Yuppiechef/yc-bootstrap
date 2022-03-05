# Breakdown Notes:

This is not a production ready process, but is for showcasing the rough architecture and structure that goes into building a project like this from scratch.

Note, there are many libraries out there that accomplish you could flip out instead of the ones used below. I am using the ones we use at Yuppiechef, but you may decide for yourself what is appropriate for your usecase. A starting point for the menu of options you have available is over here: https://www.clojure-toolbox.com/

Currently tagged step: 6

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

## Websockets

## A bit of housekeeping

Refactor stuff out of `services.http` namespace
Move rum components to `web`

## Sessions

## Pages (screens), History & Actions

## Modals


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
Totally Free!

# Better in Intellij
Java integration (autocomplete & bytecode decompilation)
Namespace Require auto-management
Copy/Paste Hiccup conversion!
