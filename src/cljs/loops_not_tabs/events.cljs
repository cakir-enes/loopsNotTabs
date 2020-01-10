(ns loops-not-tabs.events
  (:require
   [re-frame.core :as rf]
   [loops-not-tabs.db :as db]
   [cljs.reader :as reader]))

(defn- get-item [key]
  (reader/read-string (.getItem (.-localStorage js/window) key)))

(rf/reg-event-fx
 ::initialize-db
 (fn [{:keys [db]} _]
   {:db (->
         db/default-db
         (assoc :library (get-item "library")))}))

(rf/reg-event-fx
 :keydown
 (fn [_ [_ e]]
   (case (.-code e)
     "Space" {:dispatch [:toggle-player]}
     "ArrowLeft" {:dispatch [:backward]}
     "ArrowRight" {:dispatch [:forward]}
     "KeyR" {:dispatch [:toggle-loop-rec]}
     "KeyX" {:dispatch [:stop-loop]}
     "KeyQ" {:dispatch [:restart]}
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
     (prn "Toggled Loop Rec: CurrTime:" curr-time)
     (if (:recording? db)
       (let [new-loops (conj (:loops db) {:begin (:begin (:rec-loop db)) :end curr-time})
             first-loop? (empty? (:loops db))
             fxs {:db (-> db
                          (assoc :recording? false)
                          (assoc :rec-loop nil)
                          (assoc :loops new-loops))
                  :persist [(str "loops:" video-id) new-loops]}]
         (if first-loop? (assoc fxs :dispatch [:new-video]) fxs))
       {:db (-> db
                (assoc :recording? true)
                (assoc :rec-loop {:begin curr-time}))}))))


(rf/reg-event-db
 :play-loop
 (fn [db [_ loop]]
   (println "ACTIVE_LOOP: " (:index (:active-loop db)))
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


(defn- vec-remove
  "remove elem in coll"
  [pos coll]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn drop-nth [n coll]
  (keep-indexed #(when (not= %1 n) %2) coll))

(rf/reg-event-fx
 :remove-loop
 (fn [{:keys [db]} [_ rem-loop]]
   (let [remove-fn (fn [loops]
                     (sort-by :begin (drop-nth (:index rem-loop) loops)))]
     (if (= (-> db :active-loop :index) (:index rem-loop))
       {:db (update db :loops remove-fn)
        :dispatch [:stop-loop]}
       {:db (update db :loops remove-fn)}))))

(rf/reg-event-fx
 :remove-video
 (fn [{:keys [db]} [_ id]]
   (let [new-lib (dissoc (:library db) id)]
     {:db (assoc db :library new-lib)
      :remove (str "loops:" id)
      :persist ["library" new-lib]})))

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
 :load-video
 (fn [{:keys [db]} [_ opts]]
   (prn "Loading vid " opts)
   (let [retry? (map? opts)
         id (if retry? (:id opts) opts)]
     (if retry?
       {:load-video [(:player db) id]}
       {:load-video [(:player db) id]
        :db (-> db
                (assoc  :loops (get-item (str "loops:" id)))
                (assoc :page "Loops"))
        :dispatch [:stop-loop]}))))

(rf/reg-event-fx
 :player-ready
 (fn [{:keys [db]} [_ player]]
   (println "Player is ready")
   (.on player "paused" #(rf/dispatch [:paused]))
   (.on player "playing" #(rf/dispatch [:playing]))
   (.on player "playbackRateChange" #(rf/dispatch [:playback-change %]))
   {:db (assoc db :player player)
    :dispatch [:load-video (or (get-item "last-video") "GaPEquMwBQQ")]}))

(rf/reg-event-db 
 :playing
 (fn [db _]
   (println  "STARTED PLAYING")
   (assoc db :playing? true)))

(rf/reg-event-db
 :paused
 (fn [db _]
   (assoc db :playing? false)))

(rf/reg-event-fx
 :new-video
 [(rf/inject-cofx :video-id)]
 (fn [{:keys [db video-id]} _]
   (prn "SAVING NEW VIDEO")
   (let [new-lib (assoc (:library db) video-id nil)]
     {:db (assoc db :library new-lib)
      :persist ["library" new-lib]})))

(rf/reg-event-db
 :forward
 (fn [db _]
   (.seek (:player db) (+ 3 (.getCurrentTime (:player db))))
   db))

(rf/reg-event-db
 :backward
 (fn [db _]
   (.seek (:player db) (- (.getCurrentTime (:player db)) 3))
   db))

(rf/reg-fx
 :load-video
 (fn [[player id]]
   (println "LOADING VID")
   (if player (.load player id) (js/setTimeout #(rf/dispatch [:load-video nil]))) 10))



(rf/reg-event-db
 :change-page
 (fn [db [_ page]]
   (assoc db :page page)))

(rf/reg-fx
 :persist
 (fn [[key val]]
   (println "Saving: " key " -> " val )
   (.setItem (.-localStorage js/window) key val)))

(rf/reg-fx
 :remove
 (fn [key]
   (println "Removing " key)
   (.removeItem (.-localStorage js/window) key)))

(rf/reg-cofx
 :video-id
 (fn [cofx _]
   (assoc cofx :video-id (.-videoId (-> cofx :db :player)))))
