(ns loops-not-tabs.events
  (:require
   [re-frame.core :as rf]
   [loops-not-tabs.db :as db]
   ))

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

(rf/reg-event-db
 :toggle-loop-rec
 (fn [db _]
   (let [curr-time (.getCurrentTime (:player db))]
     (println "Toggled Loop Rec: CurrTime:" curr-time)
     (if (:recording? db)
       (-> db
           (assoc :recording? false)
           (assoc :rec-loop nil)
           (update :loops #(conj % {:begin (:begin (:rec-loop db)) :end curr-time})))
       (-> db 
           (assoc :recording? true)
           (assoc :rec-loop {:begin curr-time}))))))


(rf/reg-event-db
 :play-loop
 (fn [db [_ loop]]
   (when (:looping? db) (js/clearInterval (:looping? db)))
   (.seek (:player db) (:begin loop))
   (assoc db :looping? (js/setInterval (fn [] 
                                         (let [player (:player db)
                                               diff (- (.getCurrentTime player) (:end loop))]
                                           (when (pos? diff) (.seek player (:begin loop)))))
                                       450))))

(rf/reg-event-db
 :stop-loop
 (fn [db [_]]
   (println "STOPPING LOOP")
   (js/clearInterval (:looping? db))
   (assoc db :looping? nil)))

(rf/reg-event-fx
 :player-ready
 (fn [{:keys [db]} [_ player]]
   (println "Player is ready")
   (.on player "paused" (fn []  (assoc db :playing? false)))
   (.on player "playing" (fn []  (assoc db :playing? true)))
   {:db (assoc db :player player)
    :load-video player}))

(rf/reg-fx
 :load-video
 (fn [player]
   (println "LOADING VID")
   (.load player "GKSRyLdjsPA")))
