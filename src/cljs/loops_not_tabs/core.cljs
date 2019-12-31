(ns loops-not-tabs.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [loops-not-tabs.events :as events]
   [loops-not-tabs.views :as views]
   [loops-not-tabs.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
