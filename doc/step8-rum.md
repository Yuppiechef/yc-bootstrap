## Rum & Server side rendering support.

If you haven't already, be sure to fire up shadow-cljs with:

```bash
shadow-clj watch pos
```

Maybe you're a little underwhelmed right now. That's ok. Have a lie down, have a coffee and press on. It might get better.

Let's get some React goodness going - update your `shadow-cljs.edn` `:dependencies` like so:

```clojure
 :dependencies
 [[rum "0.12.8"]]
```

Now, let's create a `components.cljc` because, we want to make some components. We'll probably want this somewhere else at some point, but for now, this is fine :coffee:.

Change the namespace to look like:

```clojure
(ns components
  (:require
   [rum.core :as rum]))

(rum/defc main-page []
  [:h1 "Hello World"])
```

so we can have access to `rum/defc`, mostly.

If you save this (and still have shadow-cljs running), you'll now get shadow-cljs complaining about React being AFK. So let's kill that process and fix that.

Add the following block to `package.json` - remember to add a comma in the preceding line, json is arbitrary like that.

```json
  "dependencies": {
    "react": "^16.12.0",
    "react-dom": "^16.12.0"
  }
```
In your shell:

```bash
npm install
shadow-cljs watch app
```

And that should work a bit better.

We need a spot for React to mount in our little index file, so let's create a div for that in the `resources/public/index.htm` (before the script, unless you want to have some fun debugging):

```html
<div id="reactMount">Loading...</div>
```

and actually use that mount in the `app.cljs` file instead of writing to console (or afterwards, if you've become particularly partial to it)

```clojure
(rum/mount
  (components/main-page)
  (.getElementById js/document "reactMount"))
```

Good practice would mean we should create an `app-atom` for state before mounting and pass that to the component.

```clojure
(defonce app-atom (atom nil))

(rum/mount
  (components/main-page app-atom)
  (.getElementById js/document "reactMount"))
```

Alter the `components/main-page` to reflect - this will make more sense once we start having state around.

```clojure
(rum/defc main-page [app-atom]
  [:h1 "Hello World"])
```

Ok, so now we have a basic react component rendering, let's throw in some light SSR. First we're going to need to get dynamic control of the index.htm, so let's create an `index` component in the `components` namespace that's basically a rum version of the same thing:

```clojure
(rum/defc index []
  [:html
   [:head
    [:title "ClojureScript"]]
   [:body
    [:div#reactMount "Loading..."]
    [:script {:src "/js/main.js" :language "javascript"}]]])
```

Very nice. Now let's use our nice `index` function in the `services.http` namespace (though we'll have to refactor this out later) - First we need to add the `components` and `rum.core` namespaces to `:require` like:

```clojure
[components]
[rum.core :as rum]
```

and then update the `index` function:

```clojure
(defn index [req]
  {:body (pr-str req)})
```
You will need to restart your http server:

```bash
clj -X start/-main
```

Oh, whoops.

```
Execution error (FileNotFoundException) at components/eval1927$loading (components.cljc:1).
Could not locate rum/core__init.class, rum/core.clj or rum/core.cljc on classpath.

Full report at:
/tmp/clojure-7055105614205127020.edn
```

I guess we forgot to add rum to the `deps.edn`:

```clojure
rum/rum {:mvn/version "0.12.8"}
```

and try again. Should be better - if not, better start googling, yo. Also check out the `/tmp/...edn` file, that's usually helpful, if a little weird to read.

hit http://localhost:8080 and check the console, you should see the page and clojurescript doing its thing.

Now, that "Loading..." is rather annoying - wouldn't it be better if it could just be the current page? Why yes, but we will need an app state of some kind first.

Update the `components/index` function - add a `body` argument and have it render the html:

```clojure
(rum/defc index [body]
  [:html
   [:head
    [:title "ClojureScript"]]
   [:body
    [:div#reactMount
     {:dangerouslySetInnerHTML
      {:__html
       (rum/render-html body)}}]
    [:script {:src "/js/main.js" :language "javascript"}]]])
```

then make the `services.http/index` provide the component with a `nil` state atom (this will make sense later, promise.)

```clojure
(defn index [req]
  (let [app-atom (atom nil)]
    (rum/render-static-markup
      (components/index
        (components/main-page app-atom)))))
```

Now comment out the `rum/mount` by prepending the form with `#_` in the `app.cljs`:

```clojure
(rum/mount
    (components/main-page app-atom)
    (.getElementById js/document "reactMount"))
```

And refresh the page, this is now fully server side rendered - uncomment and refresh, and you now have a semi-working server-side render + rum hydrate scenario.

The last thing we need is a way to handle state transfer, to illustrate, let's make our `main-page` component react to state in the `app-atom`

Update the `app-atom` as such (on the ClojureScript side)

```clojure
(defonce app-atom (atom {:name "ClojureScript" :count 1}))
```

This gives it a nice default value we can work with and do some reacting on.

We can now mark our `main-page` in `components` as `rum/reactive`, this tells Rum that to setup the plumbing, because some `rum/react` calls will need handling.

```clojure
(rum/defc main-page < rum/reactive [app-atom]
  (let [state (rum/react app-atom)]
    [:div
     [:h1 "Hello " (:name state) " World"]
     [:p "We've got " (:count state)]
     [:button
      {:on-click #(swap! app-atom update :count (fnil inc 0))}
      "inc"]]))
```

I've thrown in a count and a little button to count up the state, for fun.

Now if you refresh the page, we have obnoxious jumping happening as the mounting + new state loads.

Of course, we can fix this by putting the `{:name "ClojureScript" :count 1}` in the server side atom at `services.http` - but let's make it `{:name "Clojure" :count 10}` for now, then encode and hydrate the state.

A good place for this state is on the `reactMount` div itself, so let's jump into the components namespace, change the `ns` to:

```clojure
(ns components
  (:require
   [rum.core :as rum])
  #?(:clj
     (:import
      (org.apache.commons.codec.binary Base64))))
```

The `#?(:clj ...)` reader macro tells clojurescript to ignore that.

Change the `index` function to (adding the `app-atom` arg and printing the b64 string into the data-state html attribute, we'll read that, hydrate that into the app-atom:

```clojure
(rum/defc index [body app-atom]
  [:html
   [:head
    [:title "ClojureScript"]]
   [:body
    [:div#reactMount
     {:data-state
      #?(:clj (Base64/encodeBase64String (.getBytes (pr-str @app-atom)))
         :cljs nil)
      :dangerouslySetInnerHTML
      {:__html
       (rum/render-html body)}}]
    [:script {:src "/js/main.js" :language "javascript"}]]])
```
You can refresh the page, inspect and see the `data-state` in the div.

Next, in `app.cljs`, add the needed `:require`s and add a `read-state` function (a lot of this stuff was a result of lots of trial and error and loads of googling, so don't worry if your reaction is like 'how am I supposed to do this?!' - you can, with enough time and google.'):

```clojure
(ns app
  (:require
   [components]
   [rum.core :as rum]
   [cljs.reader :as edn]
   [goog.crypt :as gcrypt]
   [goog.crypt.base64 :as base64]))

(defn read-state [default]
  (let [node (.getElementById js/document "reactMount")
        state (.getAttribute node "data-state")]
    (cond
      state
      (->
        state
        (base64/decodeStringToByteArray true)
        gcrypt/utf8ByteArrayToString
        (edn/read-string))

      :else
      default)))

(defonce app-atom
  (atom
    (read-state
      {:name "ClojureScript" :count 1})))

(rum/hydrate
  (components/main-page app-atom)
  (.getElementById js/document "reactMount"))
```

Now refresh, and you should see the state as `Clojure` and `count 10` with no bouncyness, and the button should Just Work. Also, notice the change to `rum/hydrate` instead of `rum/mount`.

And that's it. Now as long as you can build up state on the server side and translate it to the client side consistently, you'll be Just Fine :tm:.