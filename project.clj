(defproject light-alarm "0.1.0-SNAPSHOT"
  :description "Web server for RPi LED Alarm Clock"
  :url ""
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.6"]
                 [ring/ring-jetty-adapter "1.1.6"]
                 [ring/ring-core "1.2.2"]
                 [ring/ring-json "0.3.0"]
                 [hiccup "1.0.5"]
                 [com.pi4j/pi4j-core "0.0.5"]
                 [clj-time "0.6.0"]
                 [org.clojure/tools.logging "0.2.6"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler light-alarm.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}
   :main light-alarm.handler
   :aot :all
   :repositories [["sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"}]])
