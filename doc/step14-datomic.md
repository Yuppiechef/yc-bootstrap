## Persistence: Datomic

For getting this started, we're going to use the datomic cloud dev-local version. There's a few features that Datomic cloud does not have (eg. tx log listening and data excision, as of time of writing) but it will be enough to illustrate how we can get going.

### Installation and getting things going.

I first tried using docker + datomic-free, but for whatever reason, I couldn't get that to work properly, so this is the next best thing - follow the steps here 

https://docs.datomic.com/cloud/dev-local.html

You'll need to follow the instruction to setup dev-tools: https://cognitect.com/dev-tools - including some maven repo setup stuff that will be outlined in an email you receive.

Roughly, you will:

 - Agree to the license, using your email address.
 - Download and extract the zip file
 - Receive email, click on link
 - Write stuff into your `~/.m2/settings.xml` file (remove `...`'s)
 - Write stuff into `~/.clojure/deps.edn`
 - Configure data folder at `~/.datomic/dev-local.edn` - I'm pointing mine to `/home/cmdrdats/.datomic/data` for simplicity.
 - Add dep to `deps.edn` - `com.datomic/dev-local {:mvn/version "1.0.243"}`

Now we can restart our server and repl in, then poke about a bit to see that it all works as expected (run these forms one by one in the repl):

```clojure
(require '[datomic.client.api :as d])

(def client (d/client {:server-type :dev-local :system "dev"}))

(d/create-database client {:db-name "dev"})

(d/list-databases client {})

(def conn (d/connect client {:db-name "dev"}))

(d/q '[:find ?e ?v :where [?e :db/ident ?v]]
  (d/db conn))
```

This should give us a list of all attribute entities currently in the database.

Let's build a small 'guestbook' thingy, so we'll need a name and message:

```clojure
(def schema
  [{:db/ident :guestbook-entry/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A person's name"}
    
   {:db/ident :guestbook-entry/message
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Guestbook entry message"}])
```

and then commit that as a transaction and try it out:

```clojure
(d/transact conn {:tx-data schema})

(d/transact conn 
  {:tx-data 
    [{:guestbook-entry/name "Deon" 
      :guestbook-entry/message "Fun times!"}]})

(d/q '[:find (pull ?e [:guestbook-entry/name :guestbook-entry/message])
       :where [?e :guestbook-entry/name]]
  (d/db conn))
```
et viola!

### Pushing this into the code

Of course, we can keep poking at this from the repl, but we want to create an interface and hook it all together, so let's do this now.. Create a `services.datomic` namespace and shift the `name-schema` into there - We'll also want a similar vibe to `services.http` so we can manage the state and start/stop/restart it as a proper service.

```clojure
(ns services.datomic)

(def name-schema
  [{:db/ident :guestbook-entry/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "A person's name"}
   {:db/ident :guestbook-entry/message
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Guestbook entry message"}])

(defn start [])

(defn stop [])

(defn restart []
  (stop)
  (start))
```

We'll setup the state so that it gets going on startup:

```clojure
(defonce server (atom nil))

(defn start []
  (let [client (d/client {:server-type :dev-local :system "dev"})
        conn (d/connect client {:db-name "dev"})]

    ;; Just create the db/schema in case it isn't already done.
    (d/create-database client {:db-name "dev"})
    (d/transact conn {:tx-data schema})

    (reset! server
      {:client client
       :conn conn})))
```

Ideally we find a different way of sending in the config so that we can replace stuff here for production use... but for now, this'll do.

Let's throw together some guestbook views and the api to make it work:

In a new namespace, `web.guestbook`, we slap together some pretty horrible looking html. Make it pretty with some CSS if you prefer :)

```clojure
(ns web.guestbook
  (:require
   [rum.core :as rum]
   [util.comms :as comms]
   [util.flow :as flow]))

(rum/defc guestbook-entry [entry]
  [:div
   [:b (:guestbook-entry/name entry)]
   [:div
    (:guestbook-entry/message entry)]
   [:hr]])

(rum/defc guestbook-form < rum/reactive [app-atom]
  (let [form-data-atom (rum/cursor-in app-atom [:guestbook :form])
        form-data (rum/react form-data-atom)]
    [:form
     {:on-submit
      (fn [e]
        (.preventDefault e)
        (comms/send-msg :guestbook/create @form-data-atom))}
     [:label "Name:"]
     [:br]
     [:input
      {:on-change #(swap! form-data-atom assoc :guestbook-entry/name (.. % -target -value))
       :type "text"
       :value (or (:guestbook-entry/name form-data) "")}]
     [:br]

     [:label "Message"]
     [:br]
     [:textarea
      {:rows 10
       :on-change #(swap! form-data-atom assoc :guestbook-entry/message (.. % -target -value))
       :value (or (:guestbook-entry/message form-data) "")}]
     [:br]
     [:input
      {:type "submit"
       :value "Submit"}]]))

(rum/defc guestbook < rum/reactive [app-atom]
  (let [entries (rum/cursor-in app-atom [:guestbook :entries])]
    (into
      [:div
       [:h1 "Guestbook entries!"]
       (guestbook-form app-atom)]
      (map guestbook-entry (rum/react entries)))))

(defmethod flow/render-screen :guestbook [app-atom _]
  (guestbook app-atom))
```

Then make it so we can _get_ there - in `web.components/main-page`, add:

```clojure
[:button
  (urlstate/href-props app-atom "guestbook")
  "Open Guestbook"]
```


By this point..... you may have realized that your browser code doesn't automatically reload when you make changes. This is unacceptable, I can't believe you dealt with this for so long! We fix in `app.cljs` - (need the `^:dev/after-load refresh` function):

```clojure
(defn ^:dev/after-load refresh []
  (rum/mount
    (flow/render-state app-atom)
    (.getElementById js/document "reactMount")))
```

Alright, now we can continue. Refresh the `guestbook.htm` page to make sure SSR is still keeping up ok. Looks fine, so let's continue.

For the server side part of the guest book, let's add some service rules and hook em in:

```clojure
(defn guestbook-load [req]
  #?(:clj
     {:entries
      (->>
        (d/q '[:find (pull ?e [:guestbook-entry/name :guestbook-entry/message])
               :where [?e :guestbook-entry/name]]
          (datomic/db))
        (map first))}))

(defn guestbook-create [{:keys [params] :as req}]
  #?(:clj
     (let [entry
           {:guestbook-entry/name (:guestbook-entry/name params)
            :guestbook-entry/message (:guestbook-entry/message params)}]
       (d/transact (datomic/conn) {:tx-data [entry]})
       {:entry entry})))

(defn service-rules []
  [(s/service #'guestbook-load #{}
     (s/api "guestbook.load"))

   (s/service #'guestbook-create #{}
     (s/api "guestbook.create"))])
```

You'll need to add the following requires using the special reader tag for specifying language, since these are not available on cljs:

```clojure
  #?@(:clj
      [[datomic.client.api :as d]
        [services.datomic :as datomic]])
```

Notice that I'm _not_ just transacting `params` directly. This is important, you don't just arbitrarily commit what a client sends you in its raw shape. The API has a specific contract and you should be as explicit and precise as possible here.

Hook the service rules in at the `start.clj` :

```clojure
(s/defservices service-rules
  #'services.websocket/service-rules
  #'web.guestbook/service-rules
  #'web.index/service-rules)
```

And we should be ready to roll!

Hit the 'Submit' button while keeping an eye on the message you get back - this should ping the entry back at you. Let's write this back into the state so we actually _show_ the new entry:

```clojure
(defmethod comms/receive :guestbook/create [app-atom e]
  (swap! app-atom update-in [:guestbook :entries] conj (:entry e)))
```

Simple enough, receive the message and smash it into the app-atom in the same place as we're rending the entries. That should work now.

Next up, we really should be loading the list of entries when the page loads. Kinda important. This is a quick hack way of doing it (don't do this in prod because it can fire arbitrarily, all the time), but we can throw a `comms/send` call into the screen:

```clojure
(defmethod flow/render-screen :guestbook [app-atom _]
  (comms/send-msg :guestbook/load {})
  (guestbook app-atom))
```

The correct way of handling this would be to create a lifecycle method like `transition-open`, which gets called once before it switches to the page - but in interest of time, I'm not going to implement this at the moment.

To make the browser handle the `:guestbook/load` response:

```clojure
(defmethod comms/receive :guestbook/load [app-atom e]
  (swap! app-atom assoc-in [:guestbook :entries] (:entries e)))
```

et viola.

if you switch pages, using the buttons, it should work fine.

You'll notice a lack of consistent ordering, so let's fix that and bring in the transacted dates:

```clojure
(rum/defc guestbook-entry [entry]
  [:div
   [:i (str (:db/txInstant entry))]
   [:br]
   [:b (:guestbook-entry/name entry)]
   [:div
    (:guestbook-entry/message entry)]
   [:hr]])
```

and

```clojure   
(defn guestbook-load [req]
  #?(:clj
     {:entries
      (->>
        (d/q '[:find
               (pull ?e [:guestbook-entry/name :guestbook-entry/message])
               (pull ?t [:db/txInstant])
               :where [?e :guestbook-entry/name _ ?t]]
          (datomic/db))
        (map
          (fn [[e t]]
            (merge e t)))
        (sort-by :db/txInstant)
        (reverse))}))
```