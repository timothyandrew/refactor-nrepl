(ns com.deps.clojurec-file)

(do
  #?(:clj
     (defn clj-fn-cljc-file [])
     :cljs
     (defn cljs-fn-cljc-file [])))

(do
  #?(:clj
     (defmacro clj-macro-cljc-file [])))
