(ns light-alarm.handler
  (:use [compojure.core]
        [ring.util.response :only [redirect]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json])
  (:import [com.pi4j.io.gpio GpioController GpioFactory GpioPinDigitalOutput
            PinState RaspiPin])
  (:gen-class))

(def alarm-time (ref {:hr 7 :min 30 :meridian "am"}))
(def pwm-status (ref 0))
(def pwm-pin (ref nil))
(defroutes app-routes
  (GET "/" [] (redirect "/index.html"))
  (route/resources "/")
  (route/not-found "Not Found"))

(defroutes api-routes
  (GET "/status" [] {:body  {:pwm_status (deref pwm-status)}})
  (POST "/status" [pwm_status]
        (dosync (ref-set pwm-status pwm_status))
        {:body  {:pwm_status (deref pwm-status)}})
  (GET "/alarm" [] {:body (deref alarm-time)})
  (POST "/alarm" [& p]
        (dosync (ref-set alarm-time (conj (deref alarm-time) p)))
        {:body   (deref alarm-time)}))

(def app
  (handler/site (routes (->  api-routes json/wrap-json-params
                             json/wrap-json-response)
                        app-routes)))
(defn init-pwm
  "Provsions pin1 (PWM pin) on raspberry pi and sets output to pwm-init. Returns
   instance of GpioPinPwmOutput"
  [pwm-init]
  (let [gpio (GpioFactory/getInstance)]
    (dosync (ref-set pwm-pin (. gpio provisionPwmOutputPin  RaspiPin/GPIO_01  "MyLED" pwm-init)))))

(defn -main  [& [port]]
      (let [port (Integer. (or port (System/getenv "PORT") 5000))]
        (jetty/run-jetty #'app {:port port :join? false})))
