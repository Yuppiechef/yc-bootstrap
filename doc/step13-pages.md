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
