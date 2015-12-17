(ns com.deps.clojurescript-dependent
  (:require [com.deps.clojurec-file :refer [cljs-fn-cljc-file]]
            [com.deps.clojurescript-file :as cljs-file :include-macros true])
  (:require-macros [com.deps.clojure-file :refer [clojure-file-macro]]))

(cljs-file/clojurescript-fn)
(cljs-file/clj-macro-file-macro)
(clojure-file-macro)
(cljs-fn-cljc-file)
