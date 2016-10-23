(ns reddit-background.core
  (:require 
    [reddit-background.config :refer [config]]
    [bendavisnc.handy-bits.core :refer [<<, in?]]
    [clojure.java.shell :refer [sh]]
    [reddit-background.helpers :refer [get-today, get-extension, get-filename, delete-old-background-images]])
  (:import 
    [org.jsoup Jsoup]
    [java.io PrintStream, PrintWriter, FileWriter]
    [java.net SocketTimeoutException]
    [java.io IOException]
    [org.apache.commons.io FileUtils])
  (:gen-class))

(def img-filename 
  "The name of the file that we're saving for our desktop background." 
  "reddit_background")

(def archive-loc
  "The dir we keep old images (just for kicks)"
  "archive")

(def log-filename
  "log.txt")

(def with-logging
  (fn [f]
    (cond
      (config :logging)
        (with-open [fw (FileWriter. (clojure.java.io/file (str "./" log-filename)), true)]
          (binding [*out* fw]
            (f)))
      :else
        (f))))

(def get-subreddit-url
  "Return a subreddit link based on the config and today."
  (fn []
    ((config :days) (get-today))))

(def get-subreddit-page
  "Simply return a jsoup Document from the given subreddit url."
  (memoize
    (fn [which-url]
      (loop [timeouts '(30, 60, 90, 120)]
        (let [
            success
              (try
                (do
                  (println (<< "Trying to fetch ~{which-url}."))
                  (->
                    (Jsoup/connect which-url)
                    (.header "User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36")
                    (.get)))
                (catch IOException e 
                  (println "Connection timed out.")))
          ]
          (or
            success
            (cond
              (empty? timeouts)
                (do
                  (println (<< "Failed to obtain ~{which-url}."))
                  (println "Maybe check internet connection?")
                  (throw (Error. "Unable to retrieve content.")))
              :else
                (do
                  (println (<< "Trying again in ~{(first timeouts)} seconds."))
                  (Thread/sleep (* (first timeouts) 1000))
                  (recur (rest timeouts))))))))))


(def get-image-links-from-page ; TODO - make more sophisticated (parse thumb sources for example)
  (fn [page-doc]
    (let [
        good-extensions #{"jpg", "jpeg", "png"}
        ; good-extensions #{"png"}
        outer-content
          (->
            page-doc
            (.select "div.entry.unvoted"))
        inner-links
          (map
            (fn [oc]
              (->
                oc
                (.select "* a.title.may-blank")
                (.first)
                (.attr "href")))
            outer-content)
      ]
      (filter
        #(contains? good-extensions (get-extension %))
        inner-links))))


(def save-random-image-from-links!
  "Saves an image from one of the links given - selected randomly."
  (fn [links]
    (let [
        random-link-chosen (rand-nth links)
        orig-filename (get-filename random-link-chosen)
        write-file (clojure.java.io/file 
          (str img-filename "." (get-extension orig-filename)))
      ]
      (do
        (println (<< "~{(count links)} links to choose from."))
        (assert (delete-old-background-images img-filename) "Couldn't delete old background file.")
        (FileUtils/writeByteArrayToFile
          write-file
          (->
            (Jsoup/connect random-link-chosen)
            (.header "User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36")
            (.ignoreContentType true)
            (.maxBodySize 0)
            (.execute)
            (.bodyAsBytes)))
        (println (<< "Saved background image file (from ~{random-link-chosen})."))
        (assert 
          (> (.length write-file) 100000)
          "Saved file suspiciously small.")
        (when (config :keep-files)
          (FileUtils/copyFile
            write-file
            (clojure.java.io/file (str "./" archive-loc "/" orig-filename))))
        (.getCanonicalPath write-file)))))

(def set-background-wallpaper!
  (fn [filename]
    (boolean
      (sh "bash" "-c"
        (format (config :background-set-command) filename)))))

(def college-try
  (comp 
    set-background-wallpaper!
    save-random-image-from-links!
    get-image-links-from-page
    get-subreddit-page
    get-subreddit-url))

(def go
  (fn []
    (time
      (do
        (println "----------------------------------------")
        (println (str (java.util.Date.)))
        (loop [attempts-left 10]
          (let [
              success
                (try
                  (college-try)
                  (catch Exception e
                    (println "Problem in trying to set background."))
                  (catch AssertionError e
                    (println "Problem in trying to set background.")))
            ]
            (or
              success
              (cond
                (not (> attempts-left 0))
                  (throw (Error. "Unsuccessful in trying to set reddit wallpaper."))
                :else
                  (do
                    (println (<< "Going to try ~{attempts-left} more times."))
                    (recur (dec attempts-left)))))))))))

            


(defn -main
  [& args]
  (with-logging go))
  ; (go))
