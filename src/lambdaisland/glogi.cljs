(ns lambdaisland.glogi
  (:require [goog.log :as glog]
            [goog.debug.Logger :as Logger]
            [goog.debug.Logger.Level :as Level]
            [goog.debug.Console :as Console]
            [goog.array :as array]
            [clojure.string :as str]
            [goog.object :as gobj])
  (:import [goog.debug Logger Console FancyWindow DivConsole LogRecord]
           [goog.debug.Logger Level])
  (:require-macros [lambdaisland.glogi]))

(defn name-str [x]
  (cond
    (= :glogi/root x)
    ""

    (string? x)
    x

    (simple-ident? x)
    (name x)

    (qualified-ident? x)
    (str (namespace x) "/" (name x))

    :else
    (str x)))

(defn logger
  "Get a logger by name, and optionally set its level. Name can be a string,
  keyword, or symbol. The special keyword :glogi/root returns the root logger."
  (^Logger [n]
   (glog/getLogger (name-str n)))
  (^Logger [n level]
   (glog/getLogger (name-str name) level)))

(def ^Logger root-logger (logger ""))

(def levels
  {:off     Level/OFF
   :shout   Level/SHOUT
   :severe  Level/SEVERE
   :warning Level/WARNING
   :info    Level/INFO
   :config  Level/CONFIG
   :fine    Level/FINE
   :finer   Level/FINER
   :finest  Level/FINEST
   :all     Level/ALL

   ;; pedestal style
   :trace Level/FINE
   :debug Level/CONFIG
   :warn  Level/WARNING
   :error Level/SEVERE})

(defn level ^Level [lvl]
  (get levels lvl))

(defn level-value
  "Get the numeric value of a log level (keyword)."
  [lvl]
  (.-value (level lvl)))

(defn make-log-record ^LogRecord [level message name exception]
  (let [record (LogRecord. level message name)]
    (when exception
      (.setException record exception))
    record))

(defn log
  "Output a log message to the given logger, optionally with an exception to be
  logged."
  ([name lvl message]
   (log name lvl message nil))
  ([name lvl message exception]
   (.logRecord (logger name)
               (make-log-record (level lvl) message name exception))))

(defn set-level
  "Set the level (a keyword) of the given logger, identified by name."
  [name lvl]
  (assert (contains? levels lvl))
  (.setLevel (logger name) (level lvl)))

(defn set-levels
  "Convenience function for setting several levels at one. Takes a map of logger name => level keyword."
  [lvls]
  (doseq [[logger level] lvls]
    (set-level logger level)))

(defn enable-console-logging!
  "Log to the browser console. This uses goog.debug.Console directly,
  use [lambdaisland.glogi.console/install!] for a version that plays nicely with
  cljs-devtools."
  []
  (when-let [instance Console/instance]
    (.setCapturing instance true)
    (let [instance (Console.)]
      (set! Console/instance instance)
      (.setCapturing instance)))
  nil)

(defn console-autoinstall!
  "Log to the browser console if the browser location contains Debug=true."
  []
  (Console/autoInstall)
  nil)

(defn popup-logger-window!
  "Pop up a browser window which will display log messages. Returns the FancyWindow instance."
  []
  (doto (FancyWindow.)
    (.setEnabled true)))

(defn log-to-div!
  "Log messages to an element on the page. Returns the DivConsole instance."
  [element]
  (doto (DivConsole. element)
    (.setCapturing  true)))

(defn add-handler
  "Add a log handler to the given logger, or to the root logger if no logger is
  specified. The handler is a function which receives a map as its argument."
  ([handler-fn]
   (add-handler "" handler-fn))
  ([name handler-fn]
   (.addHandler (logger name)
                (doto
                    (fn [^LogRecord record]
                      (handler-fn {:sequenceNumber (.-sequenceNumber_ record)
                                   :time           (.-time_ record)
                                   :level          (keyword (str/lower-case (.-name (.-level_ record))))
                                   :message        (.-msg_ record)
                                   :logger-name    (.-loggerName_ record)
                                   :exception      (.-exception_ record)}))
                  (gobj/set "handler-fn" handler-fn)))))

(defn remove-handler
  ([handler-fn]
   (remove-handler "" handler-fn))
  ([name handler-fn]
   (.removeHandler (logger name) handler-fn)))

(defn add-handler-once
  ([handler-fn]
   (add-handler-once "" handler-fn))
  ([name handler-fn]
   (when-not (some (comp #{handler-fn} #(gobj/get % "handler-fn"))
                   (.-handlers_ (logger name)))
     (add-handler name handler-fn))))
