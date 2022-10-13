(ns util.urlstate
  (:require
   [clojure.string :as str]
   [util.core :as u]
   [util.flow :as flow]
   #?@(:cljs
       [[goog.events]]))
  #?(:cljs
     (:import
      [goog.history EventType Html5History]))
  #?(:clj
     (:import (java.net URLEncoder))))

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


(defonce ^:dynamic history-atom
  (atom
    {:history nil}))



(defn get-token [history]
  #?(:cljs
     (str/replace
       (str js/window.location.pathname js/window.location.search)
       #"(^[/])" "")
     :clj
     (str/replace
       (or (:token history) "/")
       #"(^[/])" "")))

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


(defn update-screen [app-atom]
  (let [{:keys [history]} @history-atom
        current-token (get-token history)
        {:keys [root base] :as values} (current-values current-token)]
    (swap! app-atom update-in [:page] assoc
      :screen root
      :params (:vals values)
      :base base)))

(defn set-uri! [app-atom req]
  #?(:clj
     (do
       (swap! history-atom assoc-in [:history :token]
         (str (:uri req) "?" (:query-string req)))
       (update-screen app-atom))))

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



(defn href-link [root & [action params]]
  (url
    (str "/" root ".htm")
    (u/maybe assoc params
      :action (u/presence action))))

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

(defn transformer-create-url
  [token path-prefix location]
  (str path-prefix token))

(defn transformer-retrieve-token
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