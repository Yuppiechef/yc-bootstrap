(ns hello-time
  (:require [java-time :as t]))

(defn now
  "Returns the current datetime"
  []
  (t/instant))

(defn time-str
  "Returns a string representation of a datetime in the local time zone."
  [instant]
  (t/format
   (t/with-zone (t/formatter "hh:mm a") (t/zone-id))
   instant))