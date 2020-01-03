(ns loops-not-tabs.db)

(def default-db
  {:name "re-frame"
   :library []
   :player nil
   :loops []
   :active-loop {}
   :rec-loop {}
   :playback-rate 1
   :title ""
   :recording? false
   :playing? false
   :url ""})
