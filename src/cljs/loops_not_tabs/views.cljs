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

(defn- format-time [sec]
  (-> (new js/Date (* 1000 sec))
      (.toISOString)
      (.substr 11 8)
      (.substr (if (> sec 3600) 0 3))))

(defn- format-loop [l]
  (str (format-time (:begin l)) "-" (format-time (:end l))))

(defn loop-list []
  (let [loops @(rf/subscribe [:loops])
        active-loop @(rf/subscribe [:active-loop])]
    [:div.loops 
     [:div.header [:h4 "NAME"] [:h4 "PRACTISE"] [:h4 "INTERVAL"]]
     [:ul
      
      (doall (map-indexed
              (fn [i loop]
                ^{:key i}

                [:li.loop {:on-click (fn []
                                       (println "Trying play loop")
                                       (rf/dispatch [:play-loop (assoc loop :idx i)]))
                           :class (when (= (:idx active-loop) i) "active")}
                 [:h4.main (str "LOOP " i)]
                 [:h4.label (format-time (:practise loop))]
                 [:h4.time (format-loop loop)]])
              loops))]]))

(defn main-panel []
  [:div
   [player]
   [:h1 {:on-click #(rf/dispatch [:stop-loop])} "LOOPS" ]
   [loop-list]])
