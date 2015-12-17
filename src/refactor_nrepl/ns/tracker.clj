(ns refactor-nrepl.ns.tracker
  (:require [clojure.tools.namespace
             [dependency :as dep]
             [file :as file]
             [track :as tracker]]
            [refactor-nrepl.core :as core]
            [refactor-nrepl.ns.ns-parser :as ns-parser]
            [refactor-nrepl.util :as util]))

(defn build-tracker
  "Build a tracker for the project.

  If file-predicate is provided, use that instead of `core/source-file?`"
  ([]
   (build-tracker core/source-file?))
  ([file-predicate]
   (file/add-files (tracker/tracker) (core/find-in-project file-predicate))))

(defn get-dependents
  "Get the dependent files for ns from tracker."
  [tracker my-ns]
  (let [deps (dep/immediate-dependents (:clojure.tools.namespace.track/deps tracker)
                                       (symbol my-ns))]
    (for [[file ns] (:clojure.tools.namespace.file/filemap tracker)
          :when ((set deps) ns)]
      file)))

(defn project-files-in-topo-order []
  (let [tracker (build-tracker core/clj-file?)
        nses (dep/topo-sort (:clojure.tools.namespace.track/deps tracker))
        filemap (:clojure.tools.namespace.file/filemap tracker)
        ns2file (zipmap (vals filemap) (keys filemap))]
    (->> (map ns2file nses)
         (remove nil?))))

(defn- ns-dialect
  "Return the dialect of the ns referenced in libspec.

  Most of the time this will be either :clj or :cljs but #{:clj :cljs} is
  possible when a cljs file is requiring another cljs file and using
  macros from a clj file."
  [libspec source-dialect]
  (cond
    (:require-macros libspec) :clj
    (or (= source-dialect :cljc) (:refer-macros libspec) (:include-macros libspec))
    #{:cljs :clj}

    :else source-dialect))

(defn- libspec-to-files [libspec source-dialect parsed-files-by-ns]
  (not-empty (when-let [parsed-files (get parsed-files-by-ns (:ns libspec))]
               (condp = (ns-dialect libspec source-dialect)
                 :clj (filter (comp (some-fn core/clj-file? core/cljc-file?)
                                    :file)
                              parsed-files)

                 :cljs (filter (comp (some-fn core/cljs-file? core/cljc-file?)
                                     :file)
                               parsed-files)
                 parsed-files))))

(defn- add-dependencies
  [parsed-files dependency-graph {:keys [ns file source-dialect libspecs]}]
  (let [parsed-files-by-ns (util/index-by :ns parsed-files)]
    (reduce (fn [graph libspec]
              (if-let [dependencies (libspec-to-files libspec source-dialect
                                                      parsed-files-by-ns)]
                (reduce (fn [graph dep]
                          (dep/depend dependency-graph
                                      {:ns ns :file file}
                                      (util/keep-keys dep :ns :file)))
                        graph
                        dependencies)
                graph))
            dependency-graph libspecs)))

(defn build-dependency-graph
  "Return a dependency graph for all the source files in the project."
  []
  (let [project-files (core/find-in-dir core/source-file? "C:\\git\\refactor-nrepl\\test\\resources\\testproject\\src\\com\\deps")
        parsed-files (map (fn [f]
                            (let [{:keys [ns source-dialect]} (ns-parser/parse-ns f)
                                  libspecs (ns-parser/get-libspecs-from-file f)]
                              {:ns ns
                               :source-dialect source-dialect
                               :file f
                               :libspecs libspecs}))
                          project-files)]
    (reduce (partial add-dependencies parsed-files) (dep/graph) parsed-files)))

(defn get-immediate-dependents [graph the-ns]
  (let [node (some #(and (= (:ns %) the-ns) %) (dep/nodes graph))]
    (dep/immediate-dependents graph node)))
