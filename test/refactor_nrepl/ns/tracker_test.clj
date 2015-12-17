(ns refactor-nrepl.ns.tracker-test
  (:require [clojure.test :as t]
            [clojure.tools.namespace.dependency :as dep]
            [refactor-nrepl.ns.tracker :as sut]))

(t/deftest dependency-graph-finds-all-dependencies
  (let [immediate-deps (dep/immediate-dependencies (build-dependency-graph))]))
