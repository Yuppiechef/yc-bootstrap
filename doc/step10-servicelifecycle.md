## Service endpoint lifecycle

Glossary:
 - API Host: Hosts endpoints and makes them available on their platform
 - Service Endpoint: An endpoint definition that will get exposed by an API Host.
 - Service Backend: Dependencies required in context of an endpoint for fulfilling its function.


First, implement the notion of a 'service backend'. These are services that an endpoint depend on in order to complete its job.

For example, an endpoint that updates an order on a database and sends a message on a queue needs a connection to the database (along with a transaction for the duration of the endpoint handling) and a connection to the queueing system. It does not need to know how they work, just that they need access to the specific services in order to work.

We want to be able to define an endpoint handler function, specify a set of service backends it will need, permission requirements and API Host endpoint exposure configuration (ie. is this a generic api, does it consume from specific topics, perhaps run as a cron?)

For system startup, we want to:

1. Get a service registry populated with the above service endpoint configuration.
2. Collect and understand which service backends the endpoints need.
3. Start up the service backends, enabling the endpoints as their dependencies are satisfied.
4. Report on global final state.

We can generalize this a little by assuming that the service registry is allowed to change during runtime, so that the same code is used in startup as changes:

1. Service registry changes are pushed (pushed by `defservices`)
2. Trigger a diff between subsection of registry:
  a. Changed endpoints (generically supports addin or removing full endpoints):
    - Shutdown any removed API Host config.
      o remove & wait for existing consumers to finish)
    - Startup any new API Host config.
    - Reconfigure any _changed_ API Host config.

## Implement service list

For the service endpoint list, we want to create a function that we can call that will simply return the list of endpoints:

In the `web.index` namespace, let's refactor the `routing` function to create a simple version of this:

```clojure
(defn service-rules []
  [{:handler #'test-page
    :backends #{}
    :setup
    [{:type :http
      :method :get
      :path "/test.htm"}]}])
```

This just lists a single service endpoint with a `#'test-page` as the handler, no declared backend dependencies and a single exposed web api endpoint at `/test.htm` - we can make this a little cleaner by introducing two helper functions in `orchestration.servicedef` namespace , since we'll be writing a lot of these:

```clojure
(defn web [method path render-fn]
  {:type :http
   :method method
   :path path
   :render-fn render-fn})

(defn service [handler backends & setup]
  {:handler handler
   :backends backends
   :setup setup})
```

Notice the `render-fn` - we'll want to make the handler function separate from the html rendering logic. Usually do all our database interactions and then pass data on that the render function will use to transform to html. This lets us use the same endpoint for json, edn, websockets etc.

Let's split those up quick:

```clojure
(defn test-page [req]
  {:success true :name "Page"})

(defn test-page-render [req]
  (str
    "Test " (get-in req [:params :name])))
```

Now we can rework `service-rules` (add `[orchestration.servicedef :as s]` to your `:require` form in the namespace):

```clojure
(defn service-rules []
  [(s/service #'test-page #{}
     (s/web :get "/test.htm" #'test-page-render))])
```

Note: the `#'` is a way to have the handle on the Var that references the function value instead of the raw function value at compile time. This way, if `test-page` function gets updated (specifically, a new function value is placed in the Var), this will start loading the new function instead of the old one. Without this, it's impossible to update the function value in the stack without restarting.

Ok, now we incorporate this into the `routing` function, have it take a service-list as the first argument and the request as second (update the callsite in `start` namespace to `(partial index/routing index/service-rules)`) - this will become easier to shift the `routing` out to `services.http` down the line.
 
First, given a service, its matching setup and a request - render the page:

```clojure
(defn handle-route [service setup req]
  (let [result ((:handler service) req)]
    ;; Call the render function with the result params merged in.
    ((:render-fn setup)
     (update req :params merge result))))
```

Then the new routing function:

```clojure
(defn routing [service-list req]
  ;; We need to step through the service list AND each of their setup rules.
  ;; The cleaner, faster way would be to have a separate function that transforms our service-list
  ;; into a flat and indexed map to find the matching endpoints quicker.
  (loop [[s & ss :as slist] service-list
         [setup & setuplist] (:setup s)]
    (cond
      ;; Can't find a suitable match, index page
      (nil? s) (index req)

      ;; Done with setup for this service endpoint, try next.
      (nil? setup)
      (recur ss (:setup (first ss)))

      ;; Match...
      (and
        (= (:type setup) :http)
        (= (:uri req) (:path setup))
        (contains? #{:any (:request-method req)} (:method setup)))
      (handle-route s setup req)

      ;; No match, continue.
      :else
      (recur slist setuplist))))
```

So now if we run `clj -X start/-main`, and browse to [http://localhost:7888/test.htm], we should see our test page.

Before we move on, let's shift this complexity into the `services.http` namespace where it belongs. cut the `routing` and `handle-route` out and shift it over to `services.http`.

You'll notice that it complains about `index` being undefined, oh dear. We can't get at the default index page, so let's add something for us to be able to define a fallback page.

In servicedef, add a `web-fallback` function:

```clojure
(defn web-fallback [render-fn]
  {:type :http-fallback
   :render-fn render-fn})
```

And add a fallback into the `web.index/service-rules`:

```clojure
(s/service #'index #{}
 (s/web-fallback #'index-render))
```

You'll need to rename the current `index` to `index-render` and let's just add `index` for completeness:

```clojure
(defn index [req] 
  {:success true})
```

Now we tweak the `services.http/routing` loop to catch this fallback and use it if it was found:

```clojure
(defn routing [service-list req]
  ;; We need to step through the service list AND each of their setup rules.
  ;; The cleaner, faster way would be to have a separate function that transforms our service-list
  ;; into a flat and indexed map to find the matching endpoints quicker.
  (loop [[s & ss :as slist] service-list
         [setup & setuplist] (:setup s)
         fallback nil]
    (cond
      ;; Can't find a suitable match, index page
      (nil? s)
      (if fallback
        (handle-route (first fallback) (second fallback) req)
        "404 route not found and no fallback specified")

      ;; Done with setup for this service endpoint, try next.
      (nil? setup)
      (recur ss (:setup (first ss)) fallback)

      ;; Match...
      (and
        (= (:type setup) :http)
        (= (:uri req) (:path setup))
        (contains? #{:any (:request-method req)} (:method setup)))
      (handle-route s setup req)

      ;; Else if this is fallback, pick it up it
      (= (:type setup) :http-fallback)
      (recur slist setuplist [s setup])

      ;; No match, continue.
      :else
      (recur slist setuplist fallback))))
```

Last bits here, we should be able to tidy up the external setup from `start` to only pass through the `service-rules`, not `routing` function - everything should be complaining about `routing` being shadowed now anyhow... Rename `routing` in the `start`, `restart` and `app` functions to `service-list`, then update `app` accordingly:

```clojure
(defn app [service-list req]
  (let [handler
        (->  
          (partial #'routing service-list)
          (wrap-result)
          (res/wrap-resource "public"))]
    (handler req)))
```

And finally, in the `start` namespace, fix up the callsite of `http/start`:

```clojure
(http/start (index/service-rules))
```