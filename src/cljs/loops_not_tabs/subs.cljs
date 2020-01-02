(ns loops-not-tabs.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :loops
 (fn [db]
   (:loops db)))

(rf/reg-sub
 :active-loop
 (fn [db]
   (:active-loop db)))
