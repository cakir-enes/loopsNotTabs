(ns loops-not-tabs.views
  (:require
   [re-frame.core :as re-frame]
   [loops-not-tabs.subs :as subs]
   ))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     ]))
