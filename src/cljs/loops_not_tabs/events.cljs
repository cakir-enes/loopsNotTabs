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
 :player-ready
 (fn [{:keys [db]} [_ player]]
   {:db (assoc db :player player)
    :load-video player}))


(rf/reg-fx
 :load-video
 (fn [player]
   (.load player "GKSRyLdjsPA")))