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