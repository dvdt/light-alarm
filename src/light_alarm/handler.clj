(ns light-alarm.handler
  (:use [compojure.core]
        [ring.util.response :only [redirect]]
        [clojure.tools.logging :only (info error)])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json]
            [clj-time.core :as t])
  (:import [com.pi4j.io.gpio GpioController GpioFactory GpioPinDigitalOutput
            PinState RaspiPin])
  (:gen-class))

;; Application state
(def alarm-time (ref {:hr "7" :min "30" :meridian "am" :offset "300"}))
(def pwm-status (ref nil))
(def pwm-pin (ref nil))

(defn set-pwm!
  "Set the brightness level of the LED strip. 0 < pwm < 1024"
  [pwm]
  (dosync (ref-set pwm-status pwm)
          (if (nil? (deref pwm-pin)) '()
              (.setPwm (deref pwm-pin) (Integer. pwm)))))

(defn alarm-datetime
    "Calculates the next datetime that the alarm will go off"
    [{hr :hr min :min meridian :meridian utc-offset :offset} now]
    (let [year (t/year now)
        month (t/month now)
        day (t/day now)
        hr (Integer. hr)
        hr (if (= hr 12) 0 hr)
        min (Integer. min)
        utc-offset (Integer. utc-offset)
        alarm (if (= meridian "am") (t/date-time year month day (+ hr (/ utc-offset 60)) min)
                  (t/date-time year month day (mod (+ hr (/ utc-offset 60) 12) 24) min))
        alarm (if (t/after? now alarm)
                (t/plus alarm (t/days 1))
                alarm)]
    alarm))

(defn init-pwm
  "Provsions pin1 (PWM pin) on raspberry pi and sets output to pwm-init. Returns
   instance of GpioPinPwmOutput"
  [pwm-init]
  (let [gpio (GpioFactory/getInstance)]
    (dosync (ref-set pwm-pin (. gpio provisionPwmOutputPin  RaspiPin/GPIO_01  "MyLED" pwm-init))
            (ref-set pwm-status pwm-init))))

(defn alarm-loop
  "Infinite loop that compares current time to alarm setting. Turns on lights if current time matches alarm time"
  []
  (Thread/sleep 30000)
  (let [now (t/now)
        alarm (alarm-datetime (deref alarm-time) now)
        alarm-interval (t/interval (t/minus alarm (t/secs 60)) alarm)]
    (if (t/within? alarm-interval now)
      (do  (set-pwm! 1023)
           (info "Turning LEDs on!"))))
  (recur))

;; API
(defroutes app-routes
  (GET "/lights/" [] (redirect "/lights/index.html"))
  (route/resources "/lights")
  (route/not-found "Not Found"))

(defroutes api-routes
  (GET "/lights/status" [] {:body  {:pwm_status (deref pwm-status)}})
  (POST "/lights/status" [pwm_status]
        (set-pwm! pwm_status)
        {:body  {:pwm_status (deref pwm-status)}})
  (GET "/lights/alarm" [] {:body (deref alarm-time)})
  (POST "/lights/alarm" [& p]
        (dosync (ref-set alarm-time (conj (deref alarm-time) p)))
        {:body   (deref alarm-time)}))

(def app
  (handler/site (routes (->  api-routes json/wrap-json-params
                             json/wrap-json-response)
                        app-routes)))
      
(defn -main  [& [port]]
  (init-pwm 0)
  (-> alarm-loop Thread. .start)
  (let [port (Integer. (or port (System/getenv "PORT") 5000))]
    (jetty/run-jetty #'app {:port port :join? false})
))
