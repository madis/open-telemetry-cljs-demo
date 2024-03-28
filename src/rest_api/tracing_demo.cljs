(ns rest-api.tracing-demo
  (:require
    [clojure.core.async :refer [go <!]]
    [rest-api.macros :refer-macros [go!]]
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
