(ns reddit-background.config
  (:require [clojure.data.json :as json])
  )

(def get-config
  (memoize
    (fn []
      (with-open [file-access (clojure.java.io/reader "./config.json")]
        (do
          ; (println "@ config/get-config")
          (json/read file-access :key-fn keyword))))))

(def config (get-config))




