(ns folders-naming.core
  (:use [midje.sweet])
  (:require [fs.core :as fs]
            [clojure.java.io :as io]))

(defn list-files "List the files of a folder 'dir' with the absolute path"
  [dir]
  (map #(str dir "/" (. (io/file %) getPath)) (fs/list-dir dir)))

(defn list-f "Should list the files of a folder 'dir' with the absolute path."
  [dir]
  (fs/with-cwd dir
    (map fs/absolute-path (fs/list-dir dir))))

;; first list the folder of the tmp, the result are folder with no paths
(def test-tmp-files
  (list-files "/tmp"))

;; now reconstruct the absolute paths to those files
(def test-tmp-files-absolute
  (map fs/absolute-path test-tmp-files))

;; to retrieve an extension from a file
(fact "Play with extension"
  (fs/extension "/tmp/test.txt") => ".txt")

(fact "play with predicate file? and directory?"
  (fs/file? "/tmp/file-does-not-exist") => falsey
  (fs/file? "/etc/lsb-release") => truthy
  (fs/directory? "/home/tony") => truthy)

(fact "Play with home"
  (fs/absolute-path (fs/home "try")) => "/home/try")

;; taken from https://github.com/Raynes/fs/blob/master/test/fs/core_test.clj
(defn create-walk-dir []
  (let [root (fs/temp-dir "fs-")]
    (fs/mkdir (fs/file root "a"))
    (fs/mkdir (fs/file root "b"))
    (spit (fs/file root "1") "1")
    (spit (fs/file root "a" "2") "1")
    (spit (fs/file root "b" "3") "1")
    root))

;; create a temporary directory to walk on!
(def tmp-test-dir (create-walk-dir))

(fs/walk vector tmp-test-dir)
;; ([#<File /tmp/fs-1346434699865-1888304452> #{"a" "b"} #{"1"}] [#<File /tmp/fs-1346434699865-1888304452/b> #{} #{"3"}]
;; [#<File /tmp/fs-1346434699865-1888304452/a> #{} #{"2"}])
