(ns qndspg.core
  (:gen-class)
  (:require [commonmark-hiccup.core :refer [markdown->html]]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]))

(defn normalize-path [path]
  (let [path-str (str path)]
    (if (.endsWith path-str "/")
      path-str
      (str path-str "/"))))

(defn generate-output-content
  "Pure function that takes in HMTL and MD content and outputes HTML with the MD content inserted into the provided HTML"
  [base-html md]
  (let [md-seq (string/split md #"\n|\r\n|\r")
        tokens (reduce (fn [{:keys [content vars :are-vars-read?] :as acc}  ele]
                         (if are-vars-read?
                           (assoc acc :content (str content (markdown->html ele)))
                           (if (= ele "---")
                             (assoc acc :are-vars-read? true)
                             (let [[key val] (string/split ele #":" 2)]
                               (assoc acc :vars (conj vars [(string/trim key) (string/trim val)]))))))
                       {:vars []
                        :are-vars-read? false
                        :content ""} (rest md-seq))
        base-with-vars (reduce (fn [acc [key val]]
                                 (string/replace acc (re-pattern (str "\\{\\{" key "\\}\\}")) val)) base-html (:vars tokens))
        base-with-vars-and-content (string/replace base-with-vars (re-pattern (str "\\{\\{content\\}\\}"))  (:content tokens))]
    base-with-vars-and-content))

(defn spit-creating-dirs
  "Like spit, but creates directories if they don't exist."
  [f content & options]
  (let [file (io/file f)]
    ;; Create parent directories if they don't exist
    (when-let [parent (.getParentFile file)]
      (.mkdirs parent))
    ;; Now that parent dirs exist, spit to the file
    (apply spit f content options)))

;; Check if file exists
(defn file-exists? [path]
  (if path
    (.exists (io/file path))
    false))

;; Check if path exists and is a directory
(defn directory? [path]
  (if path
    (let [file (io/file path)]
      (and (.exists file) (.isDirectory file)))
    false))

(defn generate-output! [arg-source arg-dest arg-base]
  (doseq [source  (->>  (io/file (normalize-path arg-source))
                        (file-seq)
                        (filter #(.isFile %))
                        (filter #(.endsWith (.getName %) ".md")))]
    (let [base-file  (normalize-path arg-base)
          source-path (.getAbsolutePath source)
          source-filename (.getName source)
          destination  (normalize-path arg-dest)
          destination-file (string/replace (str destination source-filename) #".md" ".html")]
      (println "writing" destination-file " using " source-path)
      (spit-creating-dirs destination-file (generate-output-content (slurp base-file) (slurp source-path)))
      (println "... done"))))

(defn validate-and-run [validataton-functions run-function]
  (let [validations (for [[f error] (partition 2 validataton-functions)]
                      (if-not (f)
                        error))]

    (if (every? nil? validations) ;; no errors -> run the given function
      (run-function)
      validations ;; validation functions failed, returned the messages back
      )))

(defn -main
  [& args]
  (let [{:keys [options]} (parse-opts args [["-s" "--source DIR" "Source Directory"]
                                            ["-d" "--dest DIR" "Destination Directory"]
                                            ["-b" "--base HTML" "Base HTML file"]])

        {:keys [source dest base]} options

        output (validate-and-run [#(directory? source) "source path not found!"
                                  #(file-exists? base) "base HTML  path not found!"]
                                 #(generate-output! source dest base))
        output-without-nil (remove nil? output)]
    (if (coll? output-without-nil)
      (doseq [err output-without-nil]
        (println "Error: " err "\n")))))

(comment

  (conj [2 3]  {:a ")e"})
  (-main "-s"  "/Users/ketansrivastav/code/content/" "-d" "/Users/ketansrivastav/code/public/"  "--base" "/Users/ketansrivastav/code/template/base.html")
  (markdown->html "Hello there *what?*")
  nil)
