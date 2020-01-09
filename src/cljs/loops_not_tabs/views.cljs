(ns loops-not-tabs.views
  (:require
   [re-frame.core :as rf]
   [loops-not-tabs.subs :as subs]
   [reagent.core :as r]
   ["yt-player" :as YTPlayer]))

(defonce height (r/atom "100%"))

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



(defn player-controls []
  (let [rec? @(rf/subscribe [::subs/recording?])
        playing? @(rf/subscribe [:playing?])
        playback @(rf/subscribe [:playback-rate])
        looping? @(rf/subscribe [:active-loop])
        rate (fn [rate text] [:h4 {:on-click #(rf/dispatch [:playback-change rate])  :class (when (= playback rate) "active")} text])]
    [:div.player-controls
     [:input {:type "image" :on-click #(rf/dispatch [:stop-loop]) :src "icons/cross.png" :class (when-not looping? "disabled")}]
     [:input {:type "image" :on-click #(rf/dispatch [:restart]) :src "icons/restart.svg"}]
     [:input {:type "image" :on-click #(rf/dispatch [:backward]) :src "icons/forward.svg" :style {:transform "rotate(180deg)"}}]
     [:input {:type "image" :on-click #(rf/dispatch [:toggle-player]) :src (if playing? "icons/pause.png" "icons/play.svg") }]
     [:input {:type "image" :on-click #(rf/dispatch [:forward]) :src "icons/forward.svg"}]
     [:input {:type "image" :on-click #(rf/dispatch [:toggle-loop-rec]) :src (if rec? "icons/loop-rec.png" "icons/loop.png")}]
     [:div.playback  [rate 0.25 ".25"] [rate 0.5 ".50"] [ rate 1 " 1 "]]]))

(defn load []
  (let [inp (r/atom "")
        ; https://www.youtube.com/watch?v=EashgVqboWo
        ;hacky
        on-paste (fn [url] (js/setTimeout #(reset! inp (second 
                                                        (first (re-seq #"^.*(?:youtu.be/|v/|e/|u/\w+/|embed/|v=)([^#&?]*).*"
                                                                       url)))) 5))
        on-change #(reset! inp %)]
    (fn []
      [:form.load
       [:input {:type "text" :placeholder "Paste/Type YouTube url/id..."
                :value @inp
                :on-change #(on-change (-> % .-target .-value))
                :on-paste (fn [e] (on-paste (.getData (.-clipboardData e) "text")))}]
       [:input  {:type "submit" :value "LOAD" :on-click #(rf/dispatch [:load-video @inp])}]])))

(defn navbar []
  (let [page @(rf/subscribe [:page])]
    [:a.navbar {:on-click #(rf/dispatch [:change-page "Home"]) :href "#"} page]))

(defn nav []
  (let [goto #(rf/dispatch [:change-page %])
        page (fn [p] [:a {:on-click #(goto p) :href "#"} p])]
    [:nav.nav
     (page "Library")
     (page "Loops")
     (page "Load")]))

(defn video-card [[id meta]]
  [:div.video-card 
   [:h4 {:on-click #(rf/dispatch [:load-video id])} id]
   [:h4.remove {:on-click #(rf/dispatch [:remove-video id])} "X "]
   [:img {:on-click #(rf/dispatch [:load-video id]) :src (str "//img.youtube.com/vi/" id "/mqdefault.jpg")}]])

(defn library []
  (let [vids @(rf/subscribe [:library])]
    (println "LIB: " vids)
    [:ul.library 
     (for [vid vids]
       ^{:key (first vid)}
       [video-card vid])]))

(defn content []
  (let [resizer #(reset! height
                        (str (- (.-innerHeight js/window)
                                60
                                (.-top (.getBoundingClientRect (.getElementById js/document "content"))))
                             "px"))]
    (r/create-class
      {:component-did-mount (fn [] 
                              (.addEventListener js/window "resize" resizer)
                              (resizer))
       :component-will-unmount #(.removeEventListener js/window "resize" resizer)
       :reagent-render (fn []
                         (let [page @(rf/subscribe [:page])]
                           [:div#content.content {:style {:max-height @height}}
                            (case page
                              "Loops" [loop-list]
                              "Load" [load]
                              "Library" [library]
                              [nav])]))})))

; (defn content []
;   [:div])
  

(defn main-panel []
  [:div.container
   [player]
   [player-controls]
   [navbar]
   [content]])
