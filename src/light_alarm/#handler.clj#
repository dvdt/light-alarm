(ns light-alarm.handler
  (:use [compojure.core]
        [ring.util.response :only [redirect]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json])
  (:gen-class))

(def alarm-time (ref 0))

(def pwm-status (ref 0))

(defroutes app-routes
  (GET "/" [] (redirect "/index.html"))
  (route/resources "/")
  (route/not-found "Not Found"))

(defroutes api-routes
  (GET "/status" [] {:body  {:pwm_status (deref pwm-status)}})
  (POST "/status" [pwm_status]
        (dosync (ref-set pwm-status pwm_status))
        {:body  {:pwm_status (deref pwm-status)}}))

(def app
  (handler/site (routes (->  api-routes json/wrap-json-params
                             json/wrap-json-response)
                        app-routes)))

(defn -main  [& [port]]
      (let [port (Integer. (or port (System/getenv "PORT") 5000))]
        (jetty/run-jetty #'app {:port port :join? false})))
