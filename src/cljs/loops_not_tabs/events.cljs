(ns loops-not-tabs.events
  (:require
   [re-frame.core :as re-frame]
   [loops-not-tabs.db :as db]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))
