## Serving up static files (Side Quest: Middleware).

Tag: https://github.com/yuppiechef/yc-bootstrap/tree/step6-static-files

Before we head into ClojureScript land, we'll need a way to serve up static files of some kind. It's idiomatic to use the `resources/public` folder for that, so let's create that in the terminal within your `yc-bootstrap` folder with:

```bash
mkdir -p resources/public
```

Let's create a favicon.ico - but first, we need one, so let's head over to https://favicon.io/emoji-favicons/ and download one. https://favicon.io/emoji-favicons/cat-with-wry-smile is the one I'm going to go for, because why not?

Unzip the file and copy the `favicon.ico` over to `resources/public` folder.

Now, let's serve it up! We could just add cases to the `routing` function, but that would get super painful as we add a bajillion more files, so let's create a middleware wrapper to handle it.

Middleware wrappers simply intercept the `app` function (called a 'handler') and decide to add something to the `req` map and call the handler - then after the call, it has a chance to inspect the result and do something else.

Let's rename our `app` function and call it `base-app` then create a brand spanking new `app` function that we'll use as a wrapping point.

Sidenote: Traditionally, you would wrap the `#'app` directly where the `http-kit/run-server` is called - this is more performant, since it won't need to setup the middleware function chain every time, but I don't want to restart the http server every time I make a change here.

A identity middleware function (a middleware function that does nothing) simply takes a `handler` function, and returns a function that accepts a `request` argument, and calls the handler function with the request argument:

```clojure
(defn identity-wrapper [handler]
  (fn [req]
    (handler req)))
```

This is important, since we can now create an `app` function that looks like:

```clojure
(defn app [req]
  (let [handler (identity-wrapper base-app)]
    (handler req)))
```

and we have the recipe we need to chain a bunch of middleware together, since every middleware function simply responds with another, spiced up, handler function.

In fact, our handling of the 200 result and content-type is a bit weird in the `base-app`, so let's promote that to a middleware function:

```clojure
(defn wrap-result [handler]
  (fn [req]
    (let [result (handler req)]
      (cond
        (map? result)
        (->
          result
          (maybe-status 200)
          (maybe-content-type "text/html"))

        :else
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body result}))))
```

That looks awfully like the `base-app`, doesn't it? So let's remove that functionality from `base-app`, which leaves it looking like:

```clojure
(defn base-app [req]
  (routing req))
```

huh. ok, let's just remove `base-app` and just use `routing` directly. Reworking `app` into:

```clojure
(defn app [req]
  (let [handler 
        (wrap-identity 
          (wrap-result routing))]
    (handler req)))
```

This is a bit hard to read, so let's thread it:

```clojure
(defn app [req]
  (let [handler 
        (->
          routing
          (wrap-result)
          (wrap-identity))]
    (handler req)))
```

This means the middleware functions will be called _bottom up_ on the way in and _top down_ on the way out from serving a request. Note that _each_ middleware has the ability to decide to simply sidestep the original handler and do something entirely differently - like serving a file instead.

Add the following to your namespace `:require` form:

```clojure
[clojure.java.io :as io]
```

Then we write a bit that can take a url and turn it into a valid HTTP response map (http-kit knows how to handle InputStream `:body` objects):

```clojure
(defn url-response
  "Return a response for the supplied URL."
  {:added "1.2"}
  [^URL url]
  (let [conn (.openConnection url)
        last-mod (.getLastModified conn)]
    {:body (.getInputStream conn)
     :status 200
     :headers
     {"Content-Length" (.getContentLength conn)
      "Last-Modified"
      (when-not (zero? last-mod)
        (Date. last-mod))}}))
```

We create a the `wrap-resource` function:

```clojure
(defn wrap-resource [handler foldername]
  (fn [req]
    (let [f (io/resource (str foldername "/" (subs (:uri req) 1)))]
      (cond
        f
        (url-response f)

        :else
        (handler req)))))
```

And finally add it to the middleware of `app`, right at the end of the chain (so it happens first) - might as well also remove the identity wrapper at this point:

```clojure
(defn app [req]
  (let [handler
        (->
          routing
          (wrap-result)
          (wrap-resource "public"))]
    (handler req)))
```

Here you see the way you can pass arguments to middleware functions.

If you reload, you should see your chosen favicon - hurrah! You can check that this by throwing together a `.htm` page into the `public` folder and check that it loads ok.

Now if you poke around, you might notice an a _folder_ serves up a file list - it also probably allows weird things like `../../etc/passwd`. oops. Don't use this in production. But let's also make `/` not try `wrap-resource` - fix this in the `wrap-resource`:

```clojure
    (let [f 
          (when-not (= (:uri req) "/")
            (io/resource (str "public/" (subs (:uri req) 1))))]
```



The more complete way of implementing this (which handles a bunch of corner cases) is to include the Ring core middleware from https://github.com/ring-clojure/ring - Add the following dep to the `deps.edn` :

```clojure
ring/ring-core {:mvn/version "1.9.5"}
```

Now `ctrl-c` on your main process and fire it up again - it should download a few dependencies and we're off again!

Add the resource namespace to your `:require`

```clojure
[ring.middleware.resource :as res]
```

if you replace our `wrap-resource` in `app` with `res/wrap-resource`, it should work exactly the same. Remove the `wrap-resource` and `url-response`.If you poke around the code here https://github.com/ring-clojure/ring/blob/master/ring-core/src/ring/util/response.clj - you may be able to tease out the exact same foundation code (that's where I got it from, after all), but this takes care of a bit more pieces.

An aside - the reason why we want to use `io/resource` instead of `io/file` is that the files may be packaged _inside_ the jar archive. This lets us serve it directly from there too.

In production, you'll want to pull the `resources/public` folder out and serve it with something that can handle straight files a whole lot more efficiently, like nginx.