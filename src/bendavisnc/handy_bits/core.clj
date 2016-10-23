(ns bendavisnc.handy-bits.core
  (:require 
   [clojure.java.io :as io]
   [clojure.pprint]
   )
    )

(def in?
	"Tells us if an x is in some list c."
  (fn [x c]
    (some #(= x %) c)))

(def spit-proper
	"Spits out what where and in a way that aims to be the most readable and complete."
  (fn [where, what]
    (spit 
      where
      (clojure.pprint/write
        what
        :stream nil
        :length nil
        ))))

;; 
;; https://github.com/clojure/clojure-contrib/blob/62078f31f425a0bf4be6508a51ac1fa8ce09ea51/src/main/clojure/clojure/contrib/strint.clj#L49

(defn- silent-read
  "Attempts to clojure.core/read a single form from the provided String, returning
   a vector containing the read form and a String containing the unread remainder
   of the provided String.  Returns nil if no valid form can be read from the
   head of the String."
  [s]
  (try
    (let [r (-> s java.io.StringReader. java.io.PushbackReader.)]
      [(read r) (slurp r)])
    (catch Exception e))) ; this indicates an invalid form -- the head of s is just string data

(defn- interpolate
  "Yields a seq of Strings and read forms."
  ([s atom?]
    (lazy-seq
      (if-let [[form rest] (silent-read (subs s (if atom? 2 1)))]
        (cons form (interpolate (if atom? (subs rest 1) rest)))
        (cons (subs s 0 2) (interpolate (subs s 2))))))
  ([^String s]
    (if-let [start (->> ["~{" "~("]
                     (map #(.indexOf s %))
                     (remove #(== -1 %))
                     sort
                     first)]
      (lazy-seq (cons
                  (subs s 0 start)
                  (interpolate (subs s start) (= \{ (.charAt s (inc start))))))
      [s])))

(defmacro <<
  "Takes a single string argument and emits a str invocation that concatenates
   the string data and evaluated expressions contained within that argument.
   Evaluation is controlled using ~{} and ~() forms.  The former is used for
   simple value replacement using clojure.core/str; the latter can be used to
   embed the results of arbitrary function invocation into the produced string.
   Examples:
   user=> (def v 30.5)
   #'user/v
   user=> (<< \"This trial required ~{v}ml of solution.\")
   \"This trial required 30.5ml of solution.\"
   user=> (<< \"There are ~(int v) days in November.\")
   \"There are 30 days in November.\"
   user=> (def m {:a [1 2 3]})
   #'user/m
   user=> (<< \"The total for your order is $~(->> m :a (apply +)).\")
   \"The total for your order is $6.\"
   Note that quotes surrounding string literals within ~() forms must be
   escaped."
  [string]
  `(str ~@(interpolate string)))

;; 
;; 
