(ns loops-not-tabs.views
  (:require
   [re-frame.core :as rf]
  
   [loops-not-tabs.subs :as subs]
   [reagent.core :as r]
   ["yt-player" :as YTPlayer]))


(defn player []
  (let [listener #(rf/dispatch [:keydown %])]
   (r/create-class
    {:display-name "Player"
     :component-did-mount (fn [_] 
                            (rf/dispatch [:player-ready (new YTPlayer "#player")])
                            (.removeEventListener js/document "keydown" listener) ; prevent adding shit tons of listener
                            (.addEventListener js/document "keydown" listener))
     :component-will-unmount (fn []
                               (.removeEventListener js/document "keydown" listener))
     :reagent-render (fn []
                       [:div.player-container
                        [:div#player]])})))

(defn- format-time [sec]
  (-> (new js/Date (* 1000 sec))
      (.toISOString)
      (.substr 11 8)
      (.substr (if (> sec 3600) 0 3))))

(defn- format-loop [l]
  (str (format-time (:begin l)) "-" (format-time (:end l))))

(defn loop-list []
  (let [loops @(rf/subscribe [:loops])
        active-loop @(rf/subscribe [:active-loop])
        play-loop #(rf/dispatch [:play-loop %])]
    [:ul.loops
     (doall (map-indexed
             (fn [i loop]
               (let [l (assoc loop :index i)]
                 ^{:key l}
                 [:li.loop {:class (when (=  (:index active-loop) i) "active")}
                  [:h4.main  {:on-click #(play-loop l)} (str "Loop " i)]
                  [:h4.remove {:on-click #(rf/dispatch [:remove-loop l])} "X"]
                  [:h4.interval {:on-click #(play-loop l)} (format-loop l)]
                  [:h4.label {:on-click #(play-loop l)} (format-time (:practise l))]]))
             loops))]))

(defn navbar []
  [:h1.navbar {:on-click #(rf/dispatch [:stop-loop])} "Loops"])

(defn player-controls []
  (let [rec? @(rf/subscribe [:recording?])
        playing? @(rf/subscribe [:playing?])
        playback @(rf/subscribe [:playback-rate])
        rate (fn [rate text] [:h4 {:on-click #(rf/dispatch [:playback-change rate])  :class (when (= playback rate) "active")} text])]
    [:div.player-controls
     [:input {:type "image" :on-click #(rf/dispatch [:restart]) :src "icons/restart.svg"}]
     [:input {:type "image" :on-click #(rf/dispatch [:backward]) :src "icons/forward.svg" :style {:transform "rotate(180deg)"}}]
     [:input {:type "image" :on-click #(rf/dispatch [:toggle-player]) :src (if playing? "icons/pause.png" "icons/play.svg") }]
     [:input {:type "image" :on-click #(rf/dispatch [:forward]) :src "icons/forward.svg"}]
     [:input {:type "image" :on-click #(rf/dispatch [:toggle-loop-rec]) :src (if rec? "icons/loop-rec.png" "icons/loop.png")}]
     [:div.playback  [rate 0.25 ".25"] [rate 0.5 ".50"] [ rate 1 " 1 "]]]))

(defn content []
  [:div.content [loop-list]])

(defn main-panel []
  [:div.container
   [player]
   [player-controls]
   [navbar]
   [content]])
