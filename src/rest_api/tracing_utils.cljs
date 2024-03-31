(ns rest-api.tracing-utils
  (:require
    ["@opentelemetry/api" :refer [trace context SpanStatusCode]]
    [rest-api.tracing :as tracing]))

(def SPAN_STATUS_OK (.-OK SpanStatusCode))
(def SPAN_STATUS_ERROR (.-ERROR SpanStatusCode))

(defn set-span-context! [span]
  (.setSpan trace (.active context) span))

(defn active-context []
  (.active context))

(defn set-span-attributes!
  [span attributes]
  (doseq [[k v] attributes] (.setAttribute span k v))
  span)

(defonce tracer (.getTracer trace "ethl-dev" "0.0.1"))

(defn start-span [span-name & [attributes]]
  (let [; tracer (.getTracer trace "ethl-dev" "0.0.1")
        span (.startSpan tracer span-name)]
    (doto span
      (set-span-attributes! ,,, attributes))))

(defn start-active-span [span-name callback]
  (.startActiveSpan tracer span-name callback))

(defn get-active-span
  ([] (get-active-span (active-context)))
  ([ctx]
   (.getSpan trace ctx)))

(defn end-span! [span]
  (.end span))

(defn set-span-error!
  [span error & [message]]
  (doto span
    (.setStatus ,,, (clj->js {"code" SPAN_STATUS_ERROR "message" message}))
    (.recordException ,,, error)))

(defn set-span-ok!
  [span & [message]]
  (.setStatus span (clj->js {"code" SPAN_STATUS_OK "message" message})))

(defn with-context
  [provided-context fn-to-call]
  (.with context provided-context fn-to-call js/undefined))

(defn with-span-context [span fn-to-call]
  (with-context (set-span-context! span) fn-to-call))
