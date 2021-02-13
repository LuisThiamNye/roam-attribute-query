(ns attr-query.scratchpad
  (:require
   [clojure.string]
   [clojure.set]
   [clojure.zip :as z]))

(defn probe [x] (js/console.log x) (prn x) x)

(defn block-string [zipper]
  (-> zipper  z/node  :string ))

(defn block-parent [zipper]
  (or (z/up zipper)
      "PAGE"))

(defn zip-seq [child]
  (when (some? child)
    (lazy-seq (cons child (zip-seq (z/right child))))))

(defn block-children [zipper]
  (when (z/branch? zipper)
    (zip-seq
     (z/down zipper))))

(defn page? [node]
  (string? node))

(defn page-name [node] node)
(defn page-refs [string]
  (into [] (map second) (re-seq #"\[\[(.*?)\]\]" string)))

(defn add-link [links node]

  (let  [[_ attr value-str] (re-find #"^(.+?)::(.*)" (block-string node))]
    (if (clojure.string/blank? attr)
      links
      (let [parent (block-parent node)
            parent-pages (if (page? parent)
                           [(page-name parent)]
                           (page-refs (block-string parent)))
            children-pages (if (clojure.string/blank? value-str)
                             (into []
                                   (mapcat (comp page-refs block-string))
                                   (block-children node))
                             (page-refs value-str))]
        (if (every? pos? [(count children-pages) (count parent-pages)])
          (assoc links attr (conj (or (get links attr) [])
                                  [parent-pages children-pages]))
          links)))))

(comment
  (do (def db {:string "xx[[n]]"
               :c      [{:string "yy::"
                         :c      [{:string "zz[[b]]"}]}
                        {:string "ol"
                         :c      []}]})

      (def zi (z/zipper (fn [node]
                          (vector? (:c node)))
                        (comp seq :c)
                        (fn [node children]
                          (update node :c conj children))
                        db)))
  (z/children zi)
  (-> zi z/down z/down z/down z/children)

  (type (z/node zi))

  (-> zi z/children)

  (-> zi z/down z/branch?)
  (-> zi z/branch?)

  (loop [zipper    zi
         links     {}
         forwards? true]
    (let [counter (atom 0)]
      (swap! counter inc)
      (when (< @counter 100)
        (let [new-links           (if forwards?
                                    (add-link links zipper)
                                    links)]
          (if-some [[zn next-forwards?] (or (when-some [zn (or (when forwards? (z/down zipper))
                                                               (z/right zipper))]
                                              [zn  true])
                                            (when-some [zn (z/up zipper)]
                                              [zn  false]))]
            (recur zn new-links next-forwards?)
            new-links)))))

  (into []
        (mapcat (comp page-refs block-string #(doto % prn)))
        (block-children (-> zi z/down)))
  (zip-seq (-> zi z/down z/down))
  (lazy-seq (z/node zi) nil)

  (into [] (map second) (re-seq #"\[\[(.*?)\]\]" "nn[[x]]"))

  (defn parse-clause-str [s]
    (re-find #"(\S)\[\[(.*?)\]\]" ""))
  (parse-clause-str "?e [[has]] ?x")
  (parse-clause-str "")

  (def results [{:a []}])

  @(dr/q '[:find ?e
           :in $ ?e-str ?a-name
           :where
           (and (or [?e :node/title ?e-str]
                    [?e :block/uid ?e-str])
                [?a :node/title ?a-name]
                [?e :entity/attrs [_ _ _]])]
         e-str a-name)

  (clojure.set/intersection #{{:a 44}} #{{:a 44}})

  (defn next-one [x]
    (lazy-seq (cons x (next-one (inc x)))))
  (def x (next-one 0))
  (time (second x))
  (time (-> x next next second))

  (def y x)
  (time (-> y next seconkd))

  (def x #{3 4})

  (x 4)

  (defn q [_]
    (let [x (r/atom [])]
      (fn [_]
        [:div
         [:button
          {:on-click (fn [_]
                       (reset!
                        x
                        (-> (d/q '[:find ?e :where
                                   [?e :node/title "sauce"]])
                            (subvec 0 20))))}
          "Query"]
         [:ul
          (map (fn [line]
                 [:li (prn-str line)])
               @x)]])))


  ;;;;;;;;
  )

(comment
  (def preds [even?])
  (every? #(% 2) preds)


  ;;;;;;;
  )
