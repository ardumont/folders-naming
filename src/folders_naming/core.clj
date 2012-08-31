(ns folders-naming.core
  (:use [midje.sweet])
  (:require [fs.core :as fs]))

;; first list the folder of the tmp, the result are folder with no paths
(def test-tmp-files (fs/list-dir "/tmp"))

;; now reconstruct the absolute paths to those files
(def test-tmp-files-absolute (map fs/absolute-path test-tmp-files))

;; to retrieve an extension from a file
(fs/extension "/tmp/test.txt")


