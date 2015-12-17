(ns refactor-nrepl.util
  (:import java.util.regex.Pattern))

(defn normalize-to-unix-path
  "Replace use / as separator and lower-case."
  [path]
  (if (.contains (System/getProperty "os.name") "Windows")
    (.replaceAll path (Pattern/quote "\\") "/")
    path))

(defn filter-map
  "Return a new map where (pred [k v]) is true for every key-value pair."
  [pred m]
  (into {} (filter pred m)))

(defn dissoc-when
  "Remove the enumerated keys from m on which pred is truthy."
  [m pred & ks]
  (if (seq ks)
    (reduce (fn [m k] (if (pred (get m k)) (dissoc m k) m)) m ks)
    m))

(defn ex-info-assoc
  "Assoc kvs onto e's data map."
  [^clojure.lang.ExceptionInfo e & kvs]
  (ex-info (.getMessage e) (apply assoc (ex-data e) kvs) (.getCause e)))

(defmacro with-additional-ex-data
  "Execute body and if an ex-info is thrown, assoc kvs onto the data
  map and rethrow."
  [kvs & body]
  `(try
     ~@body
     (catch clojure.lang.ExceptionInfo e#
       (throw (apply ex-info-assoc e# ~kvs)))))

(defn conj-some
  "Like conj but nil values are discared from xs."
  [coll & xs]
  (let [xs (remove nil? xs)]
    (if (seq xs)
      (apply conj coll xs)
      coll)))

(defn aggregate [x y]
  "If x and y are collections then return (concat x y).

If x or y is a collection and the other is an atom then conj the value
onto the collection.

If both x and y are values then create a seq of x and y."
  (cond
    (and (coll? x) (coll? y)) (concat x y)
    (coll? x) (conj x y)
    (coll? y) (conj y x)
    :else (list x y)))

(defn- ensure-sequential [x]
  (if (sequential? x) x [x]))

(defn mapvals [f m]
  "Apply f to each val in m"
  (into (empty m)
        (for [[k v] m]
          [k (f v)])))

(defn index-by [k maps]
  "Create an index for the maps on the common key key."
  (mapvals ensure-sequential
           (reduce (fn [acc m]
                     (merge-with aggregate acc
                                 (when-let [v (get m k)]
                                   {v m}))) {}
                   maps)))

(defn keep-keys
  "Return a map containing only keys ks in m."
  [m & ks]
  (into (empty m)
        (for [k ks
              :when (not= ::not-found (get m k ::not-found))]
          [k (get m k)])))
