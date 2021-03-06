(ns molsketch-cljs.util)

(declare distance distance-squared matrix-transform)

(defn len-squared [[x y]]
  (+ (* x x)
     (* y y)))

(defn len [[x y]]
  (Math/sqrt (len-squared [x y]))) 

(defn degree-to-radian [deg]
  (* Math/PI (/ deg 180)))

(defn radian-to-degree [rad]
  (* 180 (/ rad Math/PI)))

(defn scale [[x y] s]
  [(* s x) (* s y)])

(defn normalize [[x y] l]
  (let [L (len [x y])]
    (scale [x y] (/ l L))))

(defn inner-product [[x1 y1] [x2 y2]]
  (+  (* x1 x2)
      (* y1 y2)))

(defn projection [[x1 y1] [x2 y2]]
  (let [[x2 y2] (normalize [x2 y2] 1.0)]
    (scale [x2 y2] (inner-product [x1 y1] [x2 y2]))))

;; Clip line section by radius clip1 on one side
;; and clip2 on the other.
(defn clip-line [[x1 y1] [x2 y2] clip1 clip2]
  (let [l (distance [x1 y1] [x2 y2])
        dx1 (/ (* clip1 (- x2 x1)) l)
        dx2 (/ (* clip2 (- x1 x2)) l)
        dy1 (/ (* clip1 (- y2 y1)) l)
        dy2 (/ (* clip2 (- y1 y2)) l)]
      [[(+ x1 dx1) (+ y1 dy1)] [(+ x2 dx2) (+ y2 dy2)]]))

(defn displacement [[x1 y1] [x2 y2]]
  [(- x2 x1) (- y2 y1)])

(defn distance-squared [p1 p2]
  (len-squared (displacement p1 p2)))

(defn distance [p1 p2]
  (Math/sqrt (distance-squared p1 p2)))

(defn invert [v]
  (mapv - v))

(defn distance-node [node point]
  (distance (:pos node) point))

(defn distance-line-section [[x1 y1] [x2 y2] point]
  (let [l (displacement [x1 y1] [x2 y2])
        l1 (displacement [x1 y1] point)
        l2 (displacement point [x2 y2])
        p1 (inner-product l l1)
        p2 (inner-product l l2)]
    (cond (and (>= p1 0) (>= p2 0)) (len (map - l1 (projection l1 l)))
          (and (>= p1 0) (< p2 0)) (distance [x2 y2] point)
          (and (< p1 0) (>= p2 0)) (distance [x1 y1] point))))

(defn distance-bond [state bond-id point]
  (let [node-ids (get-in state [:bonds bond-id :nodes])
        [p1 p2] (map #(get-in state [:nodes % :pos]) node-ids)]
    (distance-line-section p1 p2 point)))

(defn angle [[x y]]
  (let [y (- y)
        a (Math/atan (/ y x))
        a (if (pos? x) a (+ a (.-PI js/Math)))]
    (/ (* a 180) (.-PI js/Math))))

; Performs point transformation xform with the origin
; set to [ox oy]
(defn transform-with-origin [xform [x y] [ox oy]]
  (let [[x y] (mapv - [x y] [ox oy])
        [x y] (xform [x y])]
    (mapv + [x y] [ox oy])))

(defn translate [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn translator-from-to [[x1 y1] [x2 y2]]
  "Returns a function xform [x y] -> [X Y] that moves the point [x1 y1] to
  [x2 y2]."
  (let [[vx vy] [(- x2 x1) (- y2 y1)]]
    (fn [[x y]] [(+ x vx) (+ y vy)]))) 

(defn rotate-degrees
  [[x y] degrees] ; Note: y axis points down, so rotations are count-clockwise.
  (let [radians (degree-to-radian degrees)
        a (Math/cos radians)
        b (Math/sin radians)]
    (matrix-transform [x y] a b)))

(defn matrix-transform
  "Matrix multiply with      a             b
                            -b             a
  representing a general rotation and scaling operation."
  [[x y] a b]
  [(+ (* x a) (* y b))
   (- (* y a) (* x b))])

(defn xform-from-to [[x1 y1] [x2 y2]]
  "Returns a unitary transformation that takes [x1 y1] to [x2 y2] by rotation
  and scaling. "
  (let [factor (+ (* x1 x1) (* y1 y1))
        a (/ (+ (* x1 x2) (* y1 y2)) factor)
        b (/ (- (* x2 y1) (* x1 y2)) factor)]
    (fn [[x y]] (matrix-transform [x y] a b))))

(defn orient-along [[x1 y1] [x2 y2]]
  "Returns a function that performs on a given point [x y] the rotation
  necessary to orient [x1 y1] along [x2 y2] by rotation."
  (let [[x2 y2] (normalize [x2 y2] (len [x1 y2]))] ; rescale second vector
    (xform-from-to [x1 y1] [x2 y2])))

;; (defn add-class [node class]
;;   (update node :class str " " class))
