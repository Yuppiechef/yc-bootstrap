(ns util.flow
  (:require
   [rum.core :as rum]))

(defmulti render-screen (fn [app-atom screen] screen))
(defmethod render-screen :default [app-atom screen]
  [:div "Default screen: " (pr-str screen)])

(rum/defc render-state < rum/reactive
  [app-atom]
  (let [screen-atom (rum/cursor-in app-atom [:page :screen])
        screen (rum/react screen-atom)]
    (println "SCREEN: " screen)
    (render-screen app-atom screen)))