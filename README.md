# Breakdown Notes:

This is not a production ready process, but is for showcasing the rough architecture and structure that goes into building a project like this from scratch.

Currently tagged step: 2

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

## HTTP server & routing

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
