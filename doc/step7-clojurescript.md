## ClojureScript, shadow-cljs initial bits.

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step7-clojurescript 

Ok, now that we have an http server going with a way to handle static files (which is important for .js files!), let's setup ClojureScript using shadow-cljs as our 'library' of choice.

We could handle this with other ways and painstakingly build up some kind of reload flow, but that feels like a lot of work for minimal gain (possibly following something like https://clojurescript.org/guides/quick-start, but naah), soooo...

https://shadow-cljs.github.io/docs/UsersGuide.html

Let's follow the installation process described here: https://shadow-cljs.github.io/docs/UsersGuide.html#_standalone_via_code_npm_code

Importantly, you'll need npm installed on your system to continue. Maybe google it? this _might_ help: https://docs.npmjs.com/downloading-and-installing-node-js-and-npm ymmv.

```bash
npm init -y
npm install --save-dev shadow-cljs
sudo npm install -g shadow-cljs
shadow-cljs init
```

You'll want to add `node_modules` to your `.gitignore` file.

At the end you'll have a brand spanking new `shadow-cljs.edn` file waiting for you, like so much chocolate ice cream on a hot summers day. mmmm.

Replace the `:source-paths` with just

```clojure
:source-paths
 ["src"]
```

then setup the builds:

```clojure
:builds
 {:app 
  {:target :browser
   :output-dir "resources/public/js"
   :asset-path "/js"
   :modules 
   {:main {:entries [app]}}}}
```

Here we're targeting the browser, putting the compiled js files in `resources/public/js`, with a local asset path to `/js` so that the js loading can figure out how to get to each other from the browser side.

We're also setting up a single module - we can use this to code split into multiple modules where we leverage clojurescripts ability to figure out what should go where. Maybe we'll cover that at another time.

Now create a `src/app.cljs` (notice the `cljs` extension. muchos importante) with:

```clojure
(ns app)

(js/alert "Hello.")
```

Let's fire it up to check if it compiles properly - in your shell:

```bash
shadow-cljs compile app
```

Possibly grab a coffee at this point while your internet downloads itself. But when it's done, let's checkout our new main.js! http://localhost:8080/js/main.js 

1.7mb of beautiful javascript! That's impressive. Let's trim that down just a tad with:

```bash
shadow-cljs release app
```

307b. slightly more palatable. Importantly, you'll be using the `release` command for creating the Google Closure minified code. Also, remember to _test_ your code in this way often as you're building. This code gets _heavily_ minified and if you have any weird outside deps, you'll need to configure externs (more on that later) to tell Google Closure to not touch your toys.

For dev, you'll want to:

```bash
shadow-cljs watch app
```

which will fire up a little server that can handle live code reloading all batteries included already!

Ok, let's edit/create our little `resources/public/index.htm` and have it load up our clojurescript file:

```html
<html>
<head>
    <title>ClojureScript</title>
</head>
<body>
    <script src="/js/main.js" langauge="javascript"></script>
</body>
</html>
```

Well, that wasn't so hard. Load up http://localhost:8080/index.htm to test. (yes, we haven't made the root handle that yet)

Close the annoying alert and change the message - it should automatically update and annoy you with another alert. Change the alert to:

```clojure
(.log js/console "Hello")
```

This is less annoying.

You'll notice that we've done a bit of javascript interop here. `.log` part calls the `log` function on the `js/console` object with `"Hello"` as an argument. Exactly as if we wrote:

```Javascript
console.log("Hello")
```

But the ClojureScript makes more sense. Note that you have direct access to other things like `js/window` and `js/document` - you can go nuts:

```clojure
(.log js/console (.-title js/document))
```

the `.-` is property accessor for javascript interop. Else it'll be weird and try run the property as a function. Good luck with that.

Oh, add the following to `.gitignore`, you _reeaaally don't want to commit these:

```
resources/public/js
.shadow-cljs
```