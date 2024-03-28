(ns rest-api.tracing
  (:require
    ["@opentelemetry/sdk-node" :refer [NodeSDK]]
    ["@opentelemetry/sdk-trace-node" :refer [ConsoleSpanExporter]]
    ["@opentelemetry/sdk-metrics" :refer [PeriodicExportingMetricReader
                                          ConsoleMetricExporter]]
    ["@opentelemetry/resources" :refer [Resource]]
    ["@opentelemetry/semantic-conventions" :refer [SEMRESATTRS_SERVICE_NAME
                                                   SEMRESATTRS_SERVICE_VERSION]]
    ["@opentelemetry/api" :refer [trace]]))


(defn init-sdk [service-name service-version]
  (let [resource (new Resource (clj->js {SEMRESATTRS_SERVICE_NAME service-name
                                         SEMRESATTRS_SERVICE_VERSION service-version}))
        span-exporter (new ConsoleSpanExporter)
        metric-exporter (new ConsoleMetricExporter)
        params {:resource resource
                :traceExporter span-exporter
                :metricReader (new PeriodicExportingMetricReader (clj->js {:exporter metric-exporter}))}]
    (new NodeSDK (clj->js params))))

(defonce sdk (doto (init-sdk "ethlance-server" "0.0.1")
               (.start ,,,)))


;; It’s generally recommended to call getTracer in your app when you need it
;; rather than exporting the tracer instance to the rest of your app.
;; This helps avoid trickier application load issues when other required dependencies are involved.
(defn get-tracer [scope-name scope-version]
  (.getTracer trace scope-name scope-version))

(defonce tracer (get-tracer "syncer" "0.0.1"))

