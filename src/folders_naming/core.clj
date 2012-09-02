(ns folders-naming.core
  (:require [fs.core :as fs]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]))

(defn list-files "List the files of a folder 'dir' with the absolute path"
  [dir]
  (let [fdir (io/file dir)]
    (map (partial fs/file fdir) (fs/list-dir dir))))

;; does not work as this use the global *cwd* binding and not the one provided (bug?!)
#_(defn list-f "Should list the files of a folder 'dir' with the absolute path."
  [dir]
  (fs/with-cwd dir
    (map fs/absolute-path (fs/list-dir dir))))

(def ^{:doc "the function to rename the folder or file accordingly" }
  naming-conv (comp str/join
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

(defn -main [& args]
  (let [[options args banner :as opts]
        (cli/cli args
             ["-h" "--help"         "Show help" :default false :flag true]
             ["-f" "--files"        "List of comma separated files (or folders) to rename (no existing checks)."])]

    (when (options :help)
      (println banner)
      (System/exit 0))

    (when (options :files)
      (let [files (str/split (options :files) #",")
            rfiles (map (fn [b] (if b "Success" "Failure")) (map (comp rename! io/file) files))]
        (clojure.pprint/pprint (split-at 2 (interleave files rfiles)))))))
