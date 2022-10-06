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
