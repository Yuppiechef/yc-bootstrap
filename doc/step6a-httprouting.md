## HTTP Routing

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step6-static-files

We could go off and use compojure (https://github.com/weavejester/compojure) directly, but in interest of showing the plumbing that makes this work, I think it worthwhile to roll our own lightweight mechanism instead. It won't have all the bells and whistles, but it'll do for our purposes.

In our `services.http` namespace, let's have a look at the request format - update the `app` function to look like this:

```clojure
(defn app [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (pr-str req)})
```

Refresh the page and you'll see a very raw http request with headers and some additional info around. Try out some url's like http://localhost:8080/test.htm and notice the `:uri "/test.htm"` and the `:request-method :get` bits. We'll build our routing off that.

Let's do a manual cond to route `test.htm` to its own 'page', as it were.

```clojure
(defn test [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Test Page!"})
```

And just to make it simpler, we also move the initial page to an `index` function:

```clojure
(defn index [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (pr-str req)})
```

Now then, let's 'route' to the two different pages!

```clojure
(defn app [req]
  (cond
    (and
      (= (:request-method req) :get)
      (= (:uri req) "/test.htm"))
    (test req)

    :else
    (index req)))
```

Done! This is effectively all that's behind every single routing library out there - there's cleverer syntax, there's optimisations for branching efficiently and whatnot, but there's no magic.

Repeating the whole status 200 thing is a bit tiresome, so how about we assume that if we don't respond with a map, that it's wrapped in a 200 response? Also, if it is a map, and there's no status or `"Content-Type"` header, we add it in?

First, pull the routing bit out to make this more sensible:

```clojure
(defn routing [req]
  (cond
    (and
      (= (:request-method req) :get)
      (= (:uri req) "/test.htm"))
    (test req)

    :else
    (index req)))
``` 

Then a function that would add status if none exists on the result map:

```clojure
(defn maybe-status [result status]
  (if (:status result)
    result
    (assoc result :status status)))
```

And a similar one for the Content-type header:

```clojure
(defn maybe-content-type [result content-type]
  (if (get-in result [:headers "Content-Type"])
    result
    (assoc-in result [:headers "Content-Type"] content-type)))
```

We rework the `app` function by calling the routing function and checking the result:

```clojure
(defn app [req]
  (let [result (routing req)]
    (cond
      (map? result)
      (->
        result
        (maybe-status 200)
        (maybe-content-type "text/html"))

      :else
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body result})))
```

This should work, but let's simplify our `test` and `index` pages slightly:

```clojure
(defn test [req]
  "Test Page")

(defn index [req]
  {:body (pr-str req)})
```

Done. In time, we'll pull this out a bit further into actually doing some cleverer things so you can have dynamic routing, but for now, this will suffice.