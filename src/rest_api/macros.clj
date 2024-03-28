(ns rest-api.macros
  (:require [cljs.core.async.impl.ioc-macros :as ioc]))

(defmacro go!
  "just like go, just executes immediately until the first put/take"
  [& body]
  `(let [c# (cljs.core.async/chan 1)
         f# ~(ioc/state-machine body 1 &env ioc/async-custom-terminators)
         state# (-> (f#)
                    (ioc/aset-all! cljs.core.async.impl.ioc-helpers/USER-START-IDX c#))]
     (cljs.core.async.impl.ioc-helpers/run-state-machine state#)
     c#))
