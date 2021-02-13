(ns attr-query.core
  (:require
   [clojure.set]
   [reagent.core :as r]
   [roam.datascript :as d]
   [roam.datascript.reactive :as dr]))

(def db-name "LuisThiamNye")

(defn conjv [coll x] (if (nil? coll) [x] (conj coll x)))
(defn conjset [coll x] (if (nil? coll) #{x} (conj coll x)))

(defn link-attr-block-uid [link]
  (-> link second :value peek))

(defn link-value [link]
  (-> link peek :value))

(defn link-value-block-uid [link]
  (let [v (link-value link)]
    (when (vector? v)
      (peek v))))

(defn intersect-pred [q var-id pred]
  (update-in q [:specs var-id] conjv pred))

;; Filter the v
(defn ea-constraint [q s all-es]
  (when-some [[_ e-id a-name var-id]
              (re-find #"^\s*\[\[(.*?)\]\]\s+\[\[(.*?)\]\]\s+(.*?)\s*$" s)]
    (let [target-e (some (fn [e]
                           (when (identical? e-id (or (:node/title e) (:block/uid e)))
                             e))
                         all-es)
          allowed-vs (group-by string? (into []
                                             (comp
                                              (filter #(-> % link-attr-block-uid (identical? a-name)))
                                              (map (comp #(if (string? %)
                                                            %
                                                            (d/pull '[:block/uid] %))
                                                         link-value)))
                                             (:entity/attrs target-e)))
          allowed? (fn [e]
                     (if (string? e)
                       (some #{e} (get allowed-vs true))
                       (= (:db/id e) (:db/id (get allowed-vs false)))))]
      (intersect-pred q var-id allowed?))))

(defn uid-from-title [title]
  (d/q '[:find ?uid .
         :in $ ?title
         :where
         [?e :node/title ?title]
         [?e :block/uid ?uid]]
       title))

;; Filter the e
(defn av-constraint [q s all-es]
  (when-some [[_ e-id a-name v-str]
              (re-find #"^\s*(\S+)\s+\[\[(.+?)\]\]\s+(.+?)\s*$" s)]
    (let [v-page-names (some->> (re-seq #"\[\[(.*?)\]\]" v-str)
                               (map second))
          v-uids (into #{}
                       (map (fn [name]
                              (uid-from-title name)))
                       v-page-names)
          allowed-v? (if (nil? v-page-names)
                       #(identical? v-str (link-value %))
                       (comp v-uids link-value-block-uid))
          a-uid (uid-from-title a-name)
          allowed? (fn [e]
                     (some (fn [link]
                             (and (identical? a-uid (link-attr-block-uid link))
                                  (allowed-v? link)))
                           (:entity/attrs e)))]
      (intersect-pred q e-id allowed?))))

(defn a-constraint [q s]
  (when-some [[_ e-id a-name spec-str]
              (re-find #"^\s*(\S+)\s+\[\[(.+?)\]\]\s+(.+?)\s*$" s)]))

(defn eval-var [preds all-blocks]
  (into []
        (comp (filter (fn [block]
                        (every? #(% block) preds)))
              (take 10))
        all-blocks))

(defn parse-clause-fn [all-es]
  (fn [q b]
  (let [s (:block/string b)]
    (js/console.log "parsing" (prn-str b))
    (or
     (ea-constraint q s all-es)
     (av-constraint q s all-es)
     q))))

(defn parse-query [q-blocks e-blocks]
  (let [return-id (-> q-blocks first :block/string)
        query (reduce (parse-clause-fn (into []
                                             (filter #(contains? % :entity/attrs))
                                             e-blocks))
                      {:specs {}}
                      (rest q-blocks))]
    (-> query :specs (get return-id) (eval-var e-blocks))))

(defn render-eid [b]
  (d/q '[:find ?e .
           :in $ ?uid
           :where
           [?e :block/uid ?uid]]
         (:block-uid b)))

(defn component [b q-block]
  (let [q-blocks (:block/children (d/pull '[:block/string :block/refs {:block/children ...}] (render-eid b)))
        entities (into []
                       (map
                        (comp #(select-keys % [:entity/attrs :block/uid :db/id]) d/entity :e))
                       (d/datoms :aevt :block/uid))]
    [:div "Query results:"
     [:ul
      (map
       (fn [d]
         (let [e (select-keys (d/entity (:db/id d))
                              [:block/string :node/title])
               nature (if (contains? e :block/string) "Block" "Page")
               content (or (:node/title e) (:block/string e))]
           [:li
            nature ": "
            [:a {:href (str "#/app/" db-name "/page/" (:block/uid d))}
             content]]))
       (parse-query q-blocks entities))]]))
