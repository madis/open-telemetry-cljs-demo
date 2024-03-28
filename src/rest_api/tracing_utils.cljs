(ns rest-api.tracing-utils
  (:require
    ["@opentelemetry/api" :refer [trace context SpanStatusCode]]
    [rest-api.tracing :as tracing]))

(def span-status-ok (.-OK SpanStatusCode))
(def span-status-error (.-ERROR SpanStatusCode))

(defn set-span-context! [span]
  (.setSpan trace (.active context) span))

(defn active-context []
  (.active context))

(defn set-span-attributes!
  [span attributes]
  (doseq [[k v] attributes] (.setAttribute span k v))
  span)

(defn start-span [span-name & [attributes]]
  (let [tracer (.getTracer trace "ethl-dev" "0.0.1")
        span (.startSpan tracer span-name)]
    (doto span
      (set-span-attributes! ,,, attributes))))

(defn start-active-span [span-name callback]
  (.startActiveSpan (.getTracer trace "ethl-dev" "0.0.1") span-name callback))

(defn end-span! [span]
  (.end span))

(defn set-span-error!
  [span error & [message]]
  (doto span
    (.setStatus ,,, (clj->js {:code span-status-error :message message}))
    (.recordException ,,, error)))

(defn set-span-ok!
  [span]
  (.setStatus span (clj->js {:code span-status-ok})))

(defn with-context
  [provided-context fn-to-call]
  (println ">>> with-context" context)
  (.with context provided-context fn-to-call js/undefined))
