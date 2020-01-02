(ns loops-not-tabs.views
  (:require
   [re-frame.core :as rf]
   
   [loops-not-tabs.subs :as subs]
   [reagent.core :as r]
   ["yt-player" :as YTPlayer]))


(defn player []
  (r/create-class
   {:display-name "Player"
    :component-did-mount (fn [_] 
                           (rf/dispatch [:player-ready (new YTPlayer "#player")])
                           (.addEventListener js/document "keydown" #(rf/dispatch [:keydown %])))
    :reagent-render (fn []
                      [:div.player-container
                       [:div#player]])}))

(defn main-panel []
  (let [loops @(rf/subscribe [:loops])]
    [:div
     [player]
     [:h1 {:on-click #(rf/dispatch [:stop-loop])} "LOOPS" ]
     [:ul
      (doall (map-indexed (fn [i loop] 
                            ^{:key i}
                            [:li {:on-click (fn []
                                              (println "Trying play loop")
                                              (rf/dispatch [:play-loop loop]))} [:h4 (str loop)]]) 
                          loops))]]))
