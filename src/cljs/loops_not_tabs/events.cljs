(ns loops-not-tabs.events
  (:require
   [re-frame.core :as rf]
   [loops-not-tabs.db :as db]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 :keydown
 (fn [_ [_ e]]
   (case (.-code e)
     "Space" {:dispatch [:toggle-player]}
     "ArrowLeft" {:dispatch [:backward]}
     "ArrowRight" {:dispatch [:forward]}
     "KeyR" {:dispatch [:toggle-loop-rec]}
     (println (.-code e)))))

(rf/reg-event-db
 :toggle-player
 (fn [db _]
   (when-let [player (:player db)]
     (if (:playing? db)
       (do
         (.pause player)
         (assoc db :playing? false))
       (do
         (.play player)
         (assoc db :playing? true))))))

(rf/reg-event-fx
 :toggle-loop-rec
 [(rf/inject-cofx :video-id)]
 (fn [{:keys [db video-id]} _]
   (let [curr-time (.getCurrentTime (:player db))]
     (println "Toggled Loop Rec: CurrTime:" curr-time)
     (if (:recording? db)
       (let [new-loops (conj (:loops db) {:begin (:begin (:rec-loop db)) :end curr-time})]
         {:db (-> db
                  (assoc :recording? false)
                  (assoc :rec-loop nil)
                  (assoc :loops new-loops))
          :persist [(str "loops:" video-id) new-loops]})
       {:db (-> db
                (assoc :recording? true)
                (assoc :rec-loop {:begin curr-time}))}))))


(rf/reg-event-db
 :play-loop
 (fn [db [_ loop]]
   (when (:looping? db) (js/clearInterval (:looping? db)))
   (.seek (:player db) (:begin loop))
   (-> db
       (assoc :looping? (js/setInterval (fn [] 
                                          (let [player (:player db)
                                                diff (- (.getCurrentTime player) (:end loop))]
                                            (when (pos? diff) (.seek player (:begin loop)))))
                                        450))
       (assoc :active-loop loop))))

(rf/reg-event-db
 :stop-loop
 (fn [db [_]]
   (js/clearInterval (:looping? db))
   (-> db 
       (assoc :looping? nil)
       (assoc :active-loop nil))))

(rf/reg-event-db
 :restart
 (fn [db _]
   (.seek (:player db) (or (:begin (:active-loop db)) 0))
   db))

(rf/reg-event-db
 :playback-change
 (fn [db [_ rate]]
   (println "RATE CHANGED " (:playback-rate db) " -> " rate)
   (.setPlaybackRate (:player db) rate)
   (assoc db :playback-rate rate)))

(rf/reg-event-fx
 :player-ready
 (fn [{:keys [db]} [_ player]]
   (println "Player is ready")
   (.on player "paused" #(rf/dispatch [:paused]))
   (.on player "playing" #(rf/dispatch [:playing]))
   (.on player "playbackRateChange" #(rf/dispatch [:playback-change %]))
   {:db (assoc db :player player)
    :load-video player}))

(rf/reg-event-db 
 :playing
 (fn [db _]
   (println  "STARTED PLAYING")
   (assoc db :playing? true)))

(rf/reg-event-db
 :paused
 (fn [db _]
   (assoc db :playing? false)))


(rf/reg-fx
 :load-video
 (fn [player]
   (println "LOADING VID")
   (.load player "GKSRyLdjsPA")))

(rf/reg-fx
 :persist
 (fn [[key val]]
   (println "Saving: " key " -> " val )
   (.setItem (.-localStorage js/window) key val)))

(rf/reg-cofx
 :video-id
 (fn [cofx _]
   (assoc cofx :video-id (.-videoId (-> cofx :db :player)))))
