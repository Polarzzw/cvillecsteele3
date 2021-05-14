(ns lein-git-version.plugin
  (:use
   clojure.pprint)
  (:require
   [clojure.string :as str]
   [leiningen.git-version :refer :all])
  (:import
   (java.io File)
   (java.util.regex Pattern)))

(defn middleware
  [project]
  (let [fs       File/separator
        fsp      (Pattern/quote fs)
        ns (str "(ns "
                (cond
                  (-> project :git-version :root-ns) (-> project :git-version :root-ns)
                  (-> project :git-version :path)
                  (let [path (-> project :git-version :path)]
                    (-> path
                        (str/replace (:root project) "")
                        (str/replace (re-pattern (str "^" fsp "src" fsp)) "")
                        (str/replace fs ".")))
                  :else (:name project))
                ".version)\n")
        code (str
              ";; Do not edit.  Generated by lein-git-version plugin.\n"
              ns
              "(def timestamp " (get-git-ts project) ")\n"
              "(def version \"" (get-git-version project) "\")\n"
              "(def gitref \"" (get-git-ref project) "\")\n"
              "(def gitmsg \"" (get-git-last-message project) "\")\n")
        proj-dir (.toLowerCase (.replace (:name project) \- \_))
        filename (if (:git-version-path project)
                   (str (:git-version-path project) "/version.clj")
                   (str (or (first (:source-paths project)) "src") "/"
                        proj-dir "/version.clj"))]
    (cond-> project
      (:git-version project)
      (-> project
          (update-in [:injections] concat `[(spit ~filename ~code)])
          (assoc :version (get-git-version project))
          (assoc :gitref (get-git-ref project))))))
