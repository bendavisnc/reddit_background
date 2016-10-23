(ns reddit-background.helpers
  (:require 
    [bendavisnc.handy-bits.core :refer [<<]])
  (:import 
    [java.util Calendar]
    [org.jsoup Jsoup]
    [java.net SocketTimeoutException]
    [java.io IOException]
    [org.apache.commons.io.filefilter PrefixFileFilter]
    [org.apache.commons.io FileUtils])
  )


(def get-today
  "Returns a key representing the day of the week"
  (fn []
    (nth
    [:sunday, :monday, :tuesday, :wednesday, :thursday, :friday, :saturday]
    (->
      (Calendar/getInstance)
      (.get Calendar/DAY_OF_WEEK)
      dec))))

(def get-extension
  (fn [path]
    (if-let [last-index (clojure.string/last-index-of path ".")]
      (subs
        path
        (inc last-index)))))

(def get-filename
  (fn [url]
    (if-let [last-index (clojure.string/last-index-of url "/")]
      (subs
        url
        (inc last-index)))))

(def delete-old-background-images
  "Deletes any previously set background images. Returns true if successful."
  (fn [filename]
    (apply = true 
      (map
        (fn [f]
          (do
            ; (println (<< "Deleted ~{f}."))
            (.delete f)))
        (filter
          (fn [f]
            (not (= (get-extension (.getName f)) "jar")))
          (iterator-seq
            (FileUtils/iterateFiles
              (clojure.java.io/file ".")
              (PrefixFileFilter. filename)
              nil)))))))


      




