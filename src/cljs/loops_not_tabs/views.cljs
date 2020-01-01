(ns loops-not-tabs.views
  (:require
   [re-frame.core :as rf]
   [loops-not-tabs.subs :as subs]
   [reagent.core :as r]
   ["yt-player" :as YTPlayer]))


(defn player []
  (r/create-class
   {:display-name "Player"
    :component-did-mount #(rf/dispatch [:player-ready (new YTPlayer "#player")])
    :reagent-render (fn []
                      [:div.player-container
                       [:div#player]])}))

(defn main-panel []
  (let [name (rf/subscribe [::subs/name])]
    [:div
     [player]
     [:h1 "Hello from " @name]]))
