(ns folders-naming.core
  (:use [midje.sweet])
  (:require [fs.core :as fs]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn list-files "List the files of a folder 'dir' with the absolute path"
  [dir]
  (let [fdir (io/file dir)]
    (map (partial fs/file fdir) (fs/list-dir dir))))

;; does not work as this use the global *cwd* binding and not the one provided (bug?!)
#_(defn list-f "Should list the files of a folder 'dir' with the absolute path."
  [dir]
  (fs/with-cwd dir
    (map fs/absolute-path (fs/list-dir dir))))

(def naming-conv (comp str/join
                       (partial interpose \-)
                       (partial map (comp str/lower-case
                                          #(str/replace % #"_" "")
                                          str/trim))
                       #(str/split % #"_|\s")
                       str/trim))

(defn rename-file! "Rename a file according to the naming convention"
  [file]
  (let [path (. file getAbsolutePath)
        name (fs/base-name path)
        nname (naming-conv name);; we can abstract the naming-conv function here!
        ppath (fs/parent path)
        npath (str ppath "/" nname)]
    (fs/rename path npath)))

;; a multi method to deal with the renaming of all the files in the folder root
(defmulti rename! fs/file?)

;; rename the file
(defmethod rename! true
  [file]
  (rename-file! file))

;; rename the contents of the directory and the directory itself
(defmethod rename! false
  [file]
  (do
    (let [all-sub-files (list-files (. file getAbsolutePath))]
      (doall (map rename! all-sub-files)))
    (rename-file! file)))

;; inspired from https://github.com/Raynes/fs/blob/master/test/fs/core_test.clj
(defn test-create-walk-dir "Create a temporary folder to test the life, the universe and everything!"
  []
  (let [root (fs/temp-dir "fs-")
        dir-with-underscore (fs/file root "dir_with_underscores and blank")]
    (fs/mkdir dir-with-underscore)
    (fs/create (fs/file root "toto with blank space_and_underscore.txt"))
    (let [subroot (fs/file (str (. dir-with-underscore getAbsolutePath) "/another_subdirectory"))]
      (fs/mkdir subroot)
      (fs/create (fs/file subroot "This is a subfile.another_extension"))
      root)))

;; manual test
#_(rename! (test-create-walk-dir))
