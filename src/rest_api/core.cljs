(ns rest-api.core
  (:require
    ["http" :as http]
    [rest-api.tracing :as tracing]
    [rest-api.tracing-demo :as tracing-demo]))

(defonce server-instance (atom nil))

(defn start []
  (println "rest-api.core/start Starting HTTP server")
  (let [host "0.0.0.0"
        port 3003]
    (reset! server-instance
            (.createServer
              http
              (fn [req res]
                (println "Handling request")
                (doto res
                  (.writeHead ,,, 200 (clj->js {"Content-Type" "text/html"}))
                  (.write ,,, "Hello from server"))
                (.end res))))
    (.listen @server-instance port)))

(defn stop [done]
  (println "Shutting down HTTP server instance" server-instance)
  (.close @server-instance (fn []
                             (println "Server successfully shut down")
                             (done)))
  (println "Server was shut down"))

(defn main [& args]
  (start))
