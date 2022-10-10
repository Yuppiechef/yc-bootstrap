## Websockets

Ok, that was a bit of a distraction while we get things ready to continue building actual _functionality_. But now that we have the correct abstractions in place, lets get back to some ClojureScript and get websockets up and running - this will give us a nice full-duplex comms framework to work with.

### Dependencies

We're going to use Sente [https://github.com/ptaoussanis/sente] for this - so we add
`com.taoensso/sente {:mvn/version "1.17.0"}` to our `deps.edn` file and `[com.taoensso/sente "1.17.0"]` to `shadow-cljs.edn`.

We will also need the anti-forgery ring middleware so that Sente can safely connect: [https://github.com/ring-clojure/ring-anti-forgery] - add `ring/ring-anti-forgery {:mvn/version "1.3.0"}` to `deps.edn` as well.

### Get going

Get the project started up by running the following commands in their own terminals:

```bash
clj -X start/-main
```

```bash
shadow-cljs watch app
```

And open up the [http://localhost/8080] page.

I'm getting a weird `main.js:1425 ReferenceError: components is not defined` error, so checking my `app.cljs` file, I see that I need to change `[web.components]` to `[web.components :as components]` - with that fixed and reload, my clojurescript appears to work once more (`inc` button does what it's supposed to).

We'll likely need to repeat the above steps while implementing websockets.. We can go to town to not have to restart ever, but for now that's a bit out of scope here.

### Initial stab at websocket code

I'm going to go through the README on [https://github.com/ptaoussanis/sente](Sente's) github page to implement this. Where we'll fundamentally differ is to minimize global hardcoded state, which lets us manage state properly. 

Ideally, we'd respond with the state as a result of the `start` or `restart` function and put the `websocket` state in the context of a request where needed. For now, we'll work with a simple `server` atom, similar to the `http` approach and refactor down the line.

Create a `services.websocket` namespace and we'll give it a similar set of `start/stop/restart` set of functions as the `services.http` :

```clojure
(ns services.websocket
  (:require
   [taoensso.sente :as sente]
   
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
   [orchestration.servicedef :as s]))

(defonce server (atom nil))

(defn stop []
  ((:stop-fn @server))
  (reset! server nil))

(defn start [service-rules]
  (reset! server
    (sente/make-channel-socket! (get-sch-adapter) {})))

(defn restart [service-rules]
  (stop)
  (start service-rules))
```

(Most of the work here happens in the `(sente/make-channel-socket! (get-sch-adapter) {})` line)

Let's add the needed middleware to `services.http` namespace and alter the `app` function to include it:

```clojure
(defonce memory-store (memory/memory-store))


(defn wrap-middleware [app]
  (-> app
    wrap-result
    ring.middleware.anti-forgery/wrap-anti-forgery
    ring.middleware.keyword-params/wrap-keyword-params
    ring.middleware.params/wrap-params
    (ring.middleware.session/wrap-session {:store memory-store})
    (res/wrap-resource "public")))

(defn app [service-list-atom req]
  (let [handler
        (->
          (partial #'routing @service-list-atom)
          (wrap-middleware))]
    (handler req)))
```

your `services.http` namespace should look like:

```clojure
(ns services.http
  (:require
   [org.httpkit.server :as http-kit]
   [ring.middleware.anti-forgery]
   [ring.middleware.keyword-params]
   [ring.middleware.cookies]
   [ring.middleware.params]
   [ring.middleware.session.memory :as memory]
   [ring.middleware.resource :as res]
   [ring.middleware.session]
   [web.components]))
```

Restart to check that the above is all in order.

### Websocket endpoints

In prep for next step, add a `no-op` function to `orchestration.servicedef`:

```clojure
(defn no-op [req] {:success true})
```

We'll need some service rules for the websocket http endpoints:


```clojure
(defn ring-ajax-post [{{:keys [websocket]} :ctx :as req}]
  ((:ajax-post-fn @server) req))

(defn ring-ajax-get-or-ws-handshake [{{:keys [websocket]} :ctx :as req}]
  ((:ajax-get-or-ws-handshake-fn @server) req))

(defn service-rules []
  [(s/service #'s/no-op #{:websocket}
     (s/web :post "/api/socket" #'ring-ajax-post))

   (s/service #'s/no-op #{:websocket}
     (s/web :get "/api/socket" #'ring-ajax-get-or-ws-handshake))])
```

Then hook this all up in the `start` namespace:


```clojure
(s/defservices service-rules
  #'services.websocket/service-rules
  #'web.index/service-rules)


(defn -main [& args]
  (println "Starting nrepl server: port 7888")
  (nrepl/start-server :port 7888)

  (println "Starting websocket service.")
  (websocket/start #'service-rules)

  (println "Starting HTTP server: http://localhost:8080")
  (http/start #'service-rules)
  (reload/start)
  (.read (System/in))
  (println "The end."))
```

You can then restart and check that going to [http://localhost:8080/api/socket] gives you some blurb about client-id and middleware wrappers. That's ok, since we're not supposed to be calling this directly.

### CSRF token

Sente needs to be able to find the server side CSRF token in the original emitted html in order to connect from the client side, so we can inject that at the `web.components/index` component:

```clojure
(defn csrf-div []
  #?(:clj
     (let [csrf-token (force ring.middleware.anti-forgery/*anti-forgery-token*)]
       [:div#sente-csrf-token {:data-csrf-token csrf-token}])))
```

You'll need to add `[ring.middleware.anti-forgery]` to the `:require` in the ns form - but because it's a cljc file, this won't work on cljs side.. so you'll need a reader macro:

```clojure
(ns web.components
  (:require
   [rum.core :as rum]
   #?@(:clj
       [[ring.middleware.anti-forgery]]))
  #?(:clj
     (:import
      (org.apache.commons.codec.binary Base64))))
```

Then inject the csrf-div into the index function:

```clojure
(rum/defc index [body app-atom]
  [:html
   [:head
    [:title "ClojureScript"]]
   [:body
    (csrf-div)
    [:div#reactMount
     {:data-state
      #?(:clj (Base64/encodeBase64String (.getBytes (pr-str @app-atom)))
         :cljs nil)
      :dangerouslySetInnerHTML
      {:__html
       (rum/render-html body)}}]
    [:script {:src "/js/main.js" :language "javascript"}]]])
```

If you refresh the page and check the page source, you should be able to see the csrf div with the token there.

Now to actually _connect_!

Create a `util/comms.cljc` file and throw this in there:

```clojure
(ns util.comms
  (:require
   [taoensso.sente :as sente]))

(def comms-atom (atom nil))

(defn csrf-token []
  #?(:cljs
     (when-let [el (.getElementById js/document "sente-csrf-token")]
       (.getAttribute el "data-csrf-token"))))

(defn init []
  (let [csrf (csrf-token)

        {:keys [chsk ch-recv send-fn state] :as comms}
        (sente/make-channel-socket-client!
          "/api/socket" csrf {:type :auto})]
    (reset! comms-atom
      (assoc comms :csrf csrf))))
```

and at the bottom of `apps.cljs` add a call to `(comms/init)`

### Troubleshooting

The CSRF bit is quite fiddly and no end in pain - it should work with the above, but if it doesn't, you'll want to dial back and update your `web.index/index-render` and check that sessions are working as expected:

```clojure
(defn index-render [req]
  (let [c (or (get-in req [:session :count]) 10)
        app-atom (atom {:name "Clojure" :count c})]
    (println (pr-str (:session req)))
    {:session (assoc (:session req) :count (inc c))
     :status 200
     :headers
     {"Content-Type" "text/html"}
     :body
     (rum/render-static-markup
       (components/index
         (components/main-page app-atom)
         app-atom))}))
```

When you refresh the page, the counter should increase. If it doesn't, then you'll need to poke about until you can get that to work - comment out all the js loading in the `components/index` so that you're purely working with html at this point and keep an eye on the request/response headers in your browsers network tab in dev tools. The `ring-session` cookie should remain stable.


### Actually making this useful.

If you check the dev console on your index page now, you shouldn't see any errors - you can also check the `socket?client-id=...` page in your network tab, under 'messages' you should see `+[[:chsk/handshake [:taoensso.sente/nil-uid nil]]]` happening. 

We need to implement a way to send message to the server and have it send a response back. 

So we'll break it into 3 parts - Send from client, Receive and reply from server, receive on client.

#### Send from client

In `util.comms` add a `send-msg` function under the `comms-atom` somewhere:

```clojure
(defn send-msg [event data]
  (let [send-fn (:send-fn @comms-atom)]
    (send-fn [event data])))
```

Then let's make something fire that - in `web.components/main-page`, let's add a button to fire off a message:

```clojure
[:button
 {:on-click #(comms/send-msg :components/commtest {:msg "Hello World!"})}
  "Send Message"]
```

and with that, when you click on the button, you should see the mesage getting sent over the wire on the socket as `+[[:components/commtest {:msg "Hello World!"}]]`!

#### Receive on server and respond.

This has a few parts, but let's do a basic dumb response regardless of message to begin with, just to have information go over the wire, and then come back to making it actually useful.

In the `services.websocket` namespace, replace the start function with:

```clojure
(defn user-uuid [u]
  (:client-id u))

(defn event-msg-handler
  [service-rules {:as ev-msg :keys [client-id event id ?data ring-req ?reply-fn send-fn]}]
  (send-fn client-id [:foo/ping {:msg "Hello world"}]))

(defn start [service-rules]
  (let [state
        (reset! server
          (sente/make-channel-socket! (get-sch-adapter)
            {:user-id-fn #'user-uuid}))]
    (sente/start-chsk-router!
      (:ch-recv state) (partial #'event-msg-handler service-rules))

    state))
```

Save and hit the 'Send message' button on the page while having the websocket messages open, and you should see the `commtest` echoing back! That was easy.

Before we implement dispatching logic, let's make the echo do something when it's received on client side.

#### Receive response on client side

Back in our `util.comms` namespace, let's handle the receiving on the client side:

```clojure
(defn event-msg-handler
  [{:as ev-msg :keys [client-id event id ?data ring-req ?reply-fn send-fn]}]

  ;; Sente has this weird tendency to double wrap events?? I don't get it.
  (let [[id ?data] (if (= id :chsk/recv) ?data [id ?data])]
    (println "Received: " (pr-str [id ?data]))
    nil))
```

And add the following in the `init` function (before the `reset!` call, but inside the let binding)

```clojure
    (sente/start-chsk-router!
      (:ch-recv comms) #'event-msg-handler)
```

This will send all comms from server to the `event-msg-handler` function, where we'll dispatch accordingly.

For now, if we check the dev console on the browser side and click the 'Send Message' button we should get a `[:foo/ping {:msg "Hello world"}]` response printed out.

#### Setup API dispatching on server side

OK, just having the `event-msg-handler` ping back some hardcoded thing is largely useless. We need to lookup relevant service endpoint, invoke the handler and if there's a response, respond to the client automatically.

We'll bring the same optimization as we have in the `services.http` namespace across here (I'm not DRY'ing it at this point - exercise for the reader.)

Alter the `websocket/start` function:

```clojure
(defn start [service-rules]
  (let [service-list-atom (atom (service-rules))
        state
        (reset! server
          (sente/make-channel-socket! (get-sch-adapter)
            {:user-id-fn #'user-uuid}))]

    (add-watch service-rules ::update
      (fn [k r o n]
        (reset! service-list-atom (n))))

    (sente/start-chsk-router!
      (:ch-recv state) (partial #'event-msg-handler service-list-atom))

    state))
```

Then we implement a similar version of the `routing` function as the `services.http` - though here it's a slightly different version in that it's only looking up the service to call, not actually doing the call:

```clojure
(defn ->api-name [event-id]
  (str (namespace event-id) "." (name event-id)))

(defn routing [service-list ev-msg]
  (let [api-name (->api-name (:id ev-msg))]
  ;; We need to step through the service list AND each of their setup rules.
  ;; The cleaner, faster way would be to have a separate function that transforms our service-list
  ;; into a flat and indexed map to find the matching endpoints quicker.
    (loop [[s & ss :as slist] service-list
           [setup & setuplist] (:setup s)]
      (cond
      ;; Can't find a suitable match, index page
        (nil? s)
        {:result
         {:ws/action :api/error
          :success false
          :msg (str "Service route, '" api-name "' not found")}}

      ;; Done with setup for this service endpoint, try next.
        (nil? setup)
        (recur ss (:setup (first ss)))

      ;; Match...
        (and
          (= (:type setup) :api)
          (= (:path setup) api-name))
        {:service s
         :setup setup}

      ;; No match, continue.
        :else
        (recur slist setuplist)))))
```

Alter the `event-msg-handler` to use it:

```clojure
(defn handle-route [{:keys [service setup]} ev-msg]
  ((:handler service) {:params (:?data ev-msg)}))

(defn event-msg-handler
  [service-list-atom {:as ev-msg :keys [client-id event id ?data ring-req ?reply-fn send-fn]}]
  (let [route (routing @service-list-atom ev-msg)
        result (or (:result route) (handle-route route ev-msg))]
    (when (and send-fn result)
      (send-fn client-id
        [(or (:ws/action result) id) (dissoc result :ws/action)]))))
```

Hitting 'Send Message' in the browser should now give us:

```
Received:  [:api/error {:success false, :msg "Service route not found"}]
```

Since we have no API's built - let's build a simple one to make sure this all ties together. In `ochestration.servicedef`, add an api:

```clojure
(defn api [path]
  {:type :api
   :path path})
```

and then a quick stub to check that this works (in the `web.index` namespace):

```clojure
(defn test-api [req]
  {:success true
   :msg (str "ECHO:" (pr-str (:params req)))})
```

and add

```clojure
(s/service #'test-api #{}
  (s/api "components.commtest"))
```

into the `service-rules` function somewhere. Hit the comms button and you should see the following printed on the console:

```
Received:  [:components/commtest {:success true, :msg "ECHO:{:msg \"Hello World!\"}"}]
```

Perfect, now we've got things hooked up and dispatching to the right places on the server side, lets hook up the client side as well.

#### Setup Client Side dispatching

A small aside here: Ideally we really should havea similar method of using `service-rules` on the frontend, the same as the backend so that all screen routing and comms are all explicilty connected. Unfortunately, this hasn't been implemented in our actual frontend code, so I'm choosing to implement it the same as our current frontend code handles it.

First thing we need is the `receive` in the `util.comms` namespace:

```clojure
(defmulti receive
  (fn [app-atom data]
    (:type (meta data))))

(defmethod receive :default [app-atom data]
  (println "No comms handler for " (:type (meta data))))
```

Now we need to update `event-msg-handler` to call it:

```clojure
(defn event-msg-handler
  [app-atom {:as ev-msg :keys [client-id event id ?data ring-req ?reply-fn send-fn]}]

  ;; Sente has this weird tendency to double wrap events?? I don't get it.
  (let [[id ?data] (if (= id :chsk/recv) ?data [id ?data])]
    (when id
      (receive app-atom (with-meta ?data {:type id})))
    nil))
```

You'll notice that we now also need to pass the app state into it, else we're going to have a hard time making changes to the app state! so also update `init`:

```clojure
(defn init [app-atom]
  (let [csrf (csrf-token)

        {:keys [chsk ch-recv send-fn state] :as comms}
        (sente/make-channel-socket-client!
          "/api/socket" csrf {:type :auto})]

    (sente/start-chsk-router!
      (:ch-recv comms) (partial #'event-msg-handler app-atom))

    (reset! comms-atom
      (assoc comms :csrf csrf))))
```

And finally, in the `app.cljs` add `app-atom` to the `comms/init` callsite.

You should now get a bunch of console errors in the browser for 'No comms handler' events. You'll want to implement the `:chsk` namespaced ones, mostly just to shut it up (though this lets you track connectivity status, so keep that in mind). You'll also want to implement something for `:api/error` so that you can generically toast or something if there's a server side error - for now we'll just pop it into the console:

```clojure
(defmethod receive :chsk/state [_ _])
(defmethod receive :chsk/handshake [_ _])
(defmethod receive :chsk/uiport-open [_ _])
(defmethod receive :chsk/ws-ping [_ _])
(defmethod receive :api/error [_ e]
  (println "Server Error: " (:msg e)))
```

Then let's connect our `:components/commtest` back up in `web.components` namespace (under `main-page` is preferable, but it's not really relevant):

```clojure
(defmethod comms/receive :components/commtest [app-atom e]
  (println "Receiving comms test")
  (swap! app-atom assoc :count 0))
```

Now if you click, you'll see the receive on the console, and it should update the state with zero count! Hoorah. Fully dispatched. Now you can go to town.