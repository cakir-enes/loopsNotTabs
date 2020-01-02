(ns loops-not-tabs.db)

(def default-db
  {:name "re-frame"
   :library []
   :player nil
   :loops []
   :active-loop {}
   :rec-loop {}
   :title ""
   :recording? false
   :playing? false
   :url ""})
