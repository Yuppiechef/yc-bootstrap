## Pages (screens), History & Actions

An important part of any webapp is to be able to.. browse it xD You want to be able to leverage the browser URL management directly where it makes sense, so let's set that up.

First - Let's fix `web.components/index` - the `app-atom` should come first, so flip the argument order on both it and the callsite.

Then let's make the rendered state based on a 'page' element in the state by creating a `util.flow` namespace and adding this:

```clojure
(ns util.flow 
  (:require
   [rum.core :as rum]))

(defmulti render-screen (fn [app-atom screen] screen))
(defmethod render-screen :default [app-atom _]
  [:div "Default screen!"])

(rum/defc render-state < rum/reactive
  [app-atom]
  (let [screen-atom (rum/cursor-in app-atom [:page :screen])
        screen (rum/react screen-atom)]
    (render-screen app-atom screen)))
```

We need to hydrate to there instead, so in `app.cljs` require the `util.flow`, `web.index` and `web.components` namespaces - this is necessary so that the defmethods that define the screens get loaded (a curse of multimethods).

```clojure
   [util.comms :as comms]
   [util.flow :as flow]
   [web.index]
   [web.components]
```
and update the `rum/hydrate` call further down:

```clojure
(rum/hydrate
  (flow/render-state app-atom)
  (.getElementById js/document "reactMount"))
```

If you look at the app in this state, you should see errors about `orchestration.servicedef` isn't found - this is because `servicedef` is a `clj` namespace instead of cljc - there's no reason for it not to be, so rename the file to `servicedef.cljc` - that way, we'll have access to the service endpoint definitions for the client side if we ever need them.

It should work now, but have 'default' screen. Add a `flow/render-screen` definition for `:index` in the `web.components` (you'll need to add `[util.flow :as flow]` to the require's):

```clojure
(defmethod flow/render-screen :index
  [app-atom _]
  (main-page app-atom))
```

We should be back up and running and ready with the baseline page switching.

### Add another page

Add a button on the `main-page` somewhere:

```clojure
[:button
  {:on-click #(swap! app-atom assoc-in [:page :screen] :second)}
  "To second page"]
```

then implement that `:second` screen:

```clojure
(rum/defc second-page < rum/reactive [app-atom]
  [:div
   [:h1 "This is another page"]
   [:button
    {:on-click #(swap! app-atom assoc-in [:page :screen] :index)}
    "Back to index"]])

(defmethod flow/render-screen :second [app-atom _]
  (second-page app-atom))
```

Tada. That should be baseline _functional_. It would be pretty untenable if we had to `swap!` on the `app-atom` every time we want to switch state though - we also need to inform the browser of URL changes so that we can leverage the browser history correctly.

Let's shove all the complexity behind managing that into a `util.urlstate` namespace (remember .cljc file).

First thing we want to do is do the song and dance to setup an Html5History object and store in local state (we don't want it in `app-atom` since it's not serializable as edn):

```clojure
(defonce ^:dynamic history-atom
  (atom
    {:history nil}))

(defn update-screen [app-atom]
  ;; To be implemented.
  )

(defn- transformer-create-url
  [token path-prefix location]
  (str path-prefix token))

(defn- transformer-retrieve-token
  [path-prefix location]
  (str (.-pathname location) (.-search location) (.-hash location)))


(defn setup-history [app-atom]
  #?(:cljs
     (let [transformer (Html5History.TokenTransformer.)
           _
           (set! (.. transformer -retrieveToken) transformer-retrieve-token)
           _
           (set! (.. transformer -createUrl) transformer-create-url)

           history
           (doto
             (Html5History. js/window transformer)
             (.setPathPrefix
               (str js/window.location.protocol
                 "//"
                 js/window.location.host))
             (.setUseFragment false)
             (goog.events/listen EventType.NAVIGATE
               ;; wrap in a fn to allow live reloading
               #(update-screen app-atom))

             (.setEnabled true))]
       (swap! history-atom assoc :history history)
       (update-screen app-atom))))
```
We'll need to call this from our app root in `app.cljs`, before we hydrate:

```clojure
(urlstate/setup-history app-atom)
```

Next thing we want to define is that a path like '/demo.htm?action=foo' becomes the screen `:demo/foo`. This hides the fact that we're using Clojure - security through obscurity - so devious!

```clojure
;; Thanks cemerick! https://github.com/cemerick/url/blob/master/src/cemerick/url.cljx
(defn url-encode
  [string]
  #?(:clj
     (some-> string str (URLEncoder/encode "UTF-8") (.replace "+" "%20"))

     :cljs
     (some-> string str (js/encodeURIComponent) (.replace "+" "%20"))))

(defn url [link & [getparams]]
  (str link
    (when getparams
      (->>
        (remove (comp nil? val) getparams)
        (map (fn [[k v]] (str (url-encode (name k)) "=" (url-encode v))))
        (remove str/blank?)
        (str/join "&")
        (str "?")))))

(defn href-link [root & [action params]]
  (url
    (str "/" root ".htm")
    (u/maybe assoc params
      :action (u/presence action))))
```

You'll need the `maybe` and `presence` functions in `util.core` for this (you did make it a `cljc` file, right?)

```clojure
(ns util.core
  (:require
   [clojure.string :as str]))

(defn maybe
  "Will run f over m for every k v pair where v is not nil
   example: (maybe assoc {} :comment comment)"
  [f m & kvs]
  (->>
    (partition 2 kvs)
    (remove (comp nil? second))
    (reduce (partial apply f) m)))

(defn presence
  "Return trimmed string, or nil if it is blank."
  [x]
  (cond
    (string? x)
    (let [s (str/trim x)]
      (when-not (str/blank? s) s))
    x x))
```

With this in place, we want to create a helper function so that we don't manually specify an `:on-click` on buttons or `:a` tags, back in `urlstate` namespace:

```clojure
(defn href-props [app-atom root & [action params]]
  (let [link (href-link root action params)
        s (if action
            (keyword (str root) action)
            (keyword (str root)))]

    (u/maybe assoc
      {:href link}
      :on-click
      (when (contains? (methods flow/render-screen) s)
        (fn [e]
          (when-not (= (.getAttribute (.-currentTarget e) "target") "_blank")
            (navigate app-atom e link)))))))
```

This pretends we've already built a `navigate` function that does the right thing - bear with me. In particular, this will check if the `flow/render-screen` has the screen we're interested in defined, else it'll just be a normal `:href` and won't do anything special.

Let's make `navigate` do something:

```clojure
(defn navigate [app-atom e href]
  (let [{:keys [history]} @history-atom]
    #?(:cljs
       (do
         (when e (.preventDefault e))
         (.setToken ^Html5History history href))
       :clj
       ;; This won't throw, this is intentional.
       (println "Navigate not implemented yet - needs to initiate redirect somehow?"))

    (update-screen app-atom)))
```

This updates the Html5History token in our `history-atom` to keep track of the url and updates the `app-atom` state with our not-yet-implemented `update-screen`.

First part of updating the screen is to be able to get the current path:

```clojure
(defn get-token [history]
  #?(:cljs
     (str/replace
       (str js/window.location.pathname js/window.location.search)
       #"(^[/])" "")
     :clj
     (str/replace
       (or (:token history) "/")
       #"(^[/])" "")))
```

Given that path, let's build a helper that splits it and does a bit of keywordizing and defaulting magick:

```clojure
(defn current-values [token]
  (let [[root vs] (str/split token #"[?]" 2)

        {:strs [action] :as qvals}
        (->>
          (str/split (or vs "") #"[&]")
          (map (fn [s] (str/split s #"[=]" 2)))
          (filter (comp u/presence first))
          (map (fn [[n v]] [n (or v "1")]))
          (into {}))

        base
        (-> root
          (str/replace #"[.]htm$" "")
          (str/lower-case))

        simple-root
        (if action
          (keyword base action)
          (keyword base))

        root
        (if (u/presence base)
          simple-root
          :index)]
    {:root root
     :base base
     :vals qvals}))
```

From here we can implement `update-screen` quite easily:

```clojure
(defn update-screen [app-atom]
  (let [{:keys [history]} @history-atom
        current-token (get-token history)
        {:keys [root base] :as values} (current-values current-token)]
    (swap! app-atom update-in [:page] assoc
      :screen root
      :params (:vals values)
      :base base)))
```

While we're at it, throw in the ability to `redirect` from any piece of code, or call browser `back` functionality:

```clojure
(defn redirect! [loc]
  #?(:cljs
     (let [protocol (.-protocol (.-location js/window))
           hostname (.-hostname (.-location js/window))]
       (set! (.-location js/window) (str protocol "//" hostname loc)))))

(defn redirect [app-atom root & [action params]]
  (let [link (href-link root action params)
        s (if action
            (keyword (str root) action)
            (keyword (str root)))]
    (swap! app-atom assoc-in [:redirect :uri] link)
    (if (contains? (methods flow/render-screen) s)
      (navigate app-atom nil link)
      (redirect! link))))

(defn back [& _]
  #?(:clj nil
     :cljs (.back js/history)))
```

Now that we have that all implemented, let's update the buttons so they point to each other using `href-props` - in `web.components/main-page` component:

```clojure
[:button
  (urlstate/href-props app-atom "second")
  "To second page"]
```

And pointing back to the index page, in `second-page`: 

```clojure
[:button
  (urlstate/href-props app-atom "index")
  "Back to index"]
```

If you test that out, it _should_ work, the url should be changing correctly as you flip through the pages. One thing you'll find that happens though, if you refresh on the second screen, the server side rendering _doesn't_ load the correct page.

To fix this, we'll need to write the current path into the history atom when the page loads - in `urlstate` namespace, add:

```clojure
(defn set-uri! [app-atom req]
  #?(:clj
     (do
       (swap! history-atom assoc-in [:history :token]
         (str (:uri req) "?" (:query-string req)))
       (update-screen app-atom))))
```

then in `web.index/index-render`, inside the let binding (before the response map), insert:

```clojure
(urlstate/set-uri! app-atom req)
```
And boom. That should work perfectly. If you're paying particular attention, you'll notice that there's no service rule for the `second.htm` - so everything like that is directly using the web fallback. Eventually you'd likely only ever use that single endpoint for rendering and use most comms over websockets (`s/api`).

Another problem you may face is that when you refresh on `index.htm`, the websocket won't connect and your JS console will complain about the hmtl not matching what is expected. This is because we have an old hardcoded `index.htm` page in the `resources/public` folder xD - delete it and it should all be good.