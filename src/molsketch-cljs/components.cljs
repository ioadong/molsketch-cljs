(ns molsketch-cljs.components
  (:require [molsketch-cljs.util :refer [clip-line]]
            [molsketch-cljs.constants :refer [node-radius margin
                                              hover-marker-radius
                                              selection-marker-radius]]))

(declare node bond hover-marker selection-marker)

;; (defn molecule [state {nodes :nodes bonds :bonds}]
;;   (concat
;;           (for [{id :id :as n} (map (:nodes state) nodes)]
;;             ^{:key (str "n" id)}[node state n])
;;           (for [{id :id :as b} (map (:bonds state) bonds)]
;;             ^{:key (str "b" id)}[bond state b])))

(defn molecule [state mol]
  [:g {}
   (for [[id n] (:nodes state)]
     ^{:key (str "n" id)}[node state n])
   (for [[id b] (:bonds state)]
     ^{:key (str "b" id)}[bond state b])
   [hover-marker state]
   [selection-marker state]])

(defn hover-marker [state]
  (when-let [hovered (get-in state [:status :hovered])]
    (let [[x y] (get-in state (conj hovered :pos))]
      [:circle {:cx x :cy y :r hover-marker-radius :class "hover-marker"}])))

(defn selection-marker [state]
  (when-let [selected (get-in state [:status :selected])]
    (let [[x y] (get-in state (conj selected :pos))]
      [:circle {:cx x :cy y :r selection-marker-radius :class "selection-marker"}])))

(defn node [state n]
  (let [{[x y] :pos elem :elem class :class} n]
    (if-not elem [:circle {:cx x :cy y :r node-radius :class class}]
           [:text {:x x :y y :class (str "label " class)}
            (name elem)])))

(defn bond [state b]
  (let [{n :nodes class :class} b
        [{p1 :pos} {p2 :pos} :as nodes] (map (:nodes state) n)
        [clip1 clip2] (map margin nodes)
        [[x1 y1] [x2 y2]] (clip-line p1 p2 clip1 clip2)]
    [:line {:x1 x1 :y1 y1
            :x2 x2 :y2 y2
            :class (str "bond " class)}]))
