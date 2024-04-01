(ns rest-api.tracing-demo
  (:require
    [clojure.core.async :refer [go <!]]
    [rest-api.macros :refer-macros [go! safe-go!]]
    [rest-api.tracing :as t-setup]
    [rest-api.tracing-utils :as t-utils]))

(defn do-more-work [span]
  (go!
    (println ">>> 3. inner")
    (t-utils/set-span-ok! span)
    (t-utils/end-span! span)))

(defn do-db-work [span]
  (go!
    (println ">>> 2. middle")
    (t-utils/start-active-span "inner" do-more-work)
    (t-utils/set-span-ok! span)
    (t-utils/end-span! span)))

(defn do-resolver-work [span]
  (go!
    (println ">>> 1. outer")
    (t-utils/start-active-span "middle" do-db-work)
    (t-utils/set-span-ok! span)
    (t-utils/end-span! span)))

(defn test-nested-in-separate-function []
  (go!
    (println ">>> test-nested-in-separate-function")
    (<! (t-utils/start-active-span "outer" do-resolver-work))))

(defn test-nested-simple []
  (println ">>> test-nested-simple")
  (t-utils/start-active-span
    "outer"
    (fn [span]
      (println ">>> 1. outer")
      (t-utils/start-active-span
        "middle"
        (fn [span]
          (println ">>> 2. middle")
          (t-utils/start-active-span
            "inner"
            (fn [span]
              (println ">>> 3. inner")
              (t-utils/end-span! span)))
          (t-utils/end-span! span)))
      (t-utils/end-span! span))))

(defn test-nested-go-block []
  (println ">>> test-nested-go-block")
  (go!
    (t-utils/start-active-span
     "outer"
     (fn [span]
       (go!
         (println ">>> outer")
         (t-utils/start-active-span
           "middle"
           (fn [span]
             (go!
               (println ">>> middle")
               (t-utils/start-active-span
                 "inner"
                 (fn [span]
                   (println ">>> inner")
                   (t-utils/end-span! span)))
               (t-utils/end-span! span)))))
       (t-utils/end-span! span)))))


(defn test-context-with
  "Example that doesn't require modifying the callback (nested span's
  implementation) argument list to change. Because the span in the context
(parent) gets set manually with `set-span-context!`"
  []
  (println ">>> test-context-with")
  (go!
    (let [span (t-utils/start-span "outer")]
      (println ">>> outer")
      (t-utils/with-span-context span
        (fn []
          (go!
            (let [span (t-utils/start-span "middle")]
              (println ">>> middle")
              (t-utils/with-span-context span
                (fn []
                  (go!
                    (let [span (t-utils/start-span "inner")]
                      (println ">>> inner")
                      (t-utils/end-span! span)))))
              (t-utils/end-span! span)))))
      (t-utils/end-span! span))))

(defn inner [{:keys [error?]}]
  (go!
    (let [span (t-utils/start-span "inner")]
      (println ">>> inner failing?" error?)
      (t-utils/set-span-ok! span)
      (try
        (when error? (throw (js/Error "I was told to throw") ))
      (catch js/Error e
        (t-utils/set-span-error! span e "Interesting situation"))
      (finally
        (t-utils/set-span-attributes! span {"survival_probability" (rand-int 100)})))
      (t-utils/end-span! span))))

(defn error-in-go []
  (safe-go!
    (println ">>> error-in-go")
    (throw (js/Error "This was from error-in-go"))))

(defn middle []
  (go!
    (t-utils/start-active-span
      "middle"
      (fn [span]
        (println ">>> middle")
        (inner {:error? true})
        (inner {:error? false})
        (t-utils/start-active-span "error-in-go" (fn [span]
                                                   (error-in-go)
                                                   (t-utils/end-span! span)))
        (t-utils/end-span! span)))))

(defn outer []
  (go!
    (let [active-span (t-utils/get-active-span)]
      (println ">>> outer active-span:" active-span)
      (middle)
      (middle)
      (t-utils/end-span! active-span))))

(defn test-instrumentation []
  (t-utils/start-active-span "outer" (fn [span] (outer))))
