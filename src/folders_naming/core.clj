(ns folders-naming.core
  (:use [midje.sweet])
  (:require [fs.core :as fs]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn list-files "List the files of a folder 'dir' with the absolute path"
  [dir]
  (map #(str dir "/" (. (io/file %) getPath)) (fs/list-dir dir)))

;; does not work as this use the global *cwd* binding and not the one provided (bug?!)
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

;; ############################ Serious stuff

(def naming-conv (comp str/join
                       (partial interpose \-)
                       (partial map (comp str/lower-case
                                          #(str/replace % #"_" "")
                                          str/trim))
                       #(str/split % #"_|\s")
                       str/trim))

(fact
  (naming-conv " no-space ")                        => "no-space"
  (naming-conv "I-DONOTWANTTOSEEUpperCase")         => "i-donotwanttoseeuppercase"
  (naming-conv "I-DoNotWantTo See Blank Space Too") => "i-donotwantto-see-blank-space-too"
  (naming-conv "I-DoNotWantTo_see_underscore_too")  => "i-donotwantto-see-underscore-too"
  (naming-conv "file_To-RENAME_correctly.ext")      => "file-to-rename-correctly.ext")

(comment "Test the renaming facility!"
  (fs/mkdir  "/tmp/test")                              ;;true
  (fs/touch "/tmp/test/0001.xt")                       ;;#<core$file fs.core$file@111ebb3>
  (fs/rename "/tmp/test/0001.xt" "/tmp/test/0001.txt") ;;true
  (fs/list-dir "/tmp/test")                            ;;("0001.txt")
  (fs/rename "/tmp/test" "/tmp/test2")                 ;;true
  (fs/list-dir "/tmp/test")                            ;;nil
  (fs/list-dir "/tmp/test2")                           ;;("0001.txt")
  (fs/mkdir  "/tmp/test")                              ;;true
  (fs/touch "/tmp/test/0001.xt")                       ;;#<core$file fs.core$file@111ebb3>
  (fs/rename "/tmp/test/0001.xt" "/tmp/test/0001.txt") ;;true
  (fs/list-dir "/tmp/test")                            ;;("0001.txt")
  (fs/rename "/tmp/test" "/tmp/test2")                 ;;true
  (fs/list-dir "/tmp/test")                            ;;nil
  (fs/list-dir "/tmp/test2")                           ;;("0001.txt")
)

(defn rename-file! "Rename a file according to the naming convention"
  [file]
  (let [path (. file getAbsolutePath)
        name (fs/base-name path)
        nname (naming-conv name);; we can abstract the naming-conv function here!
        ppath (fs/parent path)
        npath (str ppath "/" nname)]
    (fs/rename path npath)))

(fact "rename-file!"
  (let [temp-dir (fs/temp-dir (str "fs-"))
        temp-dir-path (. temp-dir getAbsolutePath)
        file-for-test (io/file (str temp-dir-path "/file_To-RENAME_correctly.ext"))
        file-created? (fs/create file-for-test)]
    (rename-file! file-for-test) => true
    (fs/list-dir temp-dir-path) => (contains "file-to-rename-correctly.ext")))

(defn do-rename-file!
  "A function to use with the fs/walk function.
  This will walk amongst the tree arborescence and rename files."
  [current-file dirs files]
  (if (fs/directory? current-file)
    (let [current-path (. current-file getAbsolutePath)
          ren-file! (comp rename-file!
                          io/file
                          (partial str current-path "/"))]
      (doall
       (map ren-file! (concat dirs files))))))

(defn list-files!
  [root dirs files]
  (list-files root))

(defn rename-files-and-dirs "Main function to rename the files and directory"
  [file-root-path]
  (let [root-path (. file-root-path getPath)
        res (fs/walk do-rename! root-path)]
    res))

;; inspired from https://github.com/Raynes/fs/blob/master/test/fs/core_test.clj
(defn test-create-walk-dir "Create a temporary folder to test the life, the universe and everything!"
  []
  (let [root (fs/temp-dir "fs-")
        dir-with-underscore (fs/file root "dir_with_underscores")]
    (fs/mkdir (fs/file root "This is a dir with_blank space"))
    (fs/mkdir dir-with-underscore)
    (fs/create (fs/file root "toto with blank space.txt"))
    (fs/create (fs/file root "ThisIsAFileAndBlank-space.mp4"))
    (fs/create (fs/file root "This_is_AFileInAnd_underscore and spaces.mp4"))
    (let [subroot (fs/file (str (. dir-with-underscore getAbsolutePath) "/another_subdirectory"))]
      (fs/mkdir subroot)
      (fs/create (fs/file subroot "This is a subfile.another_extension"))
      root)))

;; manual test
(let [root (test-create-walk-dir)
      res (rename-files-and-dirs root)]
  (clojure.pprint/pprint (fs/iterate-dir root));; see the new arbo
  res)
