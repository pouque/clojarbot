(ns clojarbot.core
  (require [clojure.core.async :as async :refer [thread]]
           [clojure.data.json :as json]
           [clojure.core.match :refer [match]])
  (use [clojure.string :as strings :only [includes?
                                          lower-case
                                          index-of
                                          split
                                          triml
                                          starts-with?
                                          replace]]))

(import java.net.Socket)
(import java.io.BufferedReader)
(import java.io.BufferedWriter)
(import java.io.InputStreamReader)
(import java.io.OutputStreamWriter)

(def address "irc.freenode.net")
(def port 6667)
(def nick "clojarbot")
(def user (format "%s 0 * :little-lisp bot" nick))
(def channel "#lor")

(defn connect [address port]
  (println (format "Connecting to: %s:%d" address port))
  (let [socket (Socket. address port)]
      (if (.isConnected socket) socket nil)))

(defn write [writer command data]
  (do
    (.write writer (format "%s %s\r\n" command data))
    (println (format "> %s %s\n" command data))))

(def socket (connect address port))

(def reader
  (BufferedReader.
    (InputStreamReader.
      (.getInputStream socket))))

(def writer
  (BufferedWriter.
    (OutputStreamWriter.
      (.getOutputStream socket))))

(defn clean-line [line]
  (apply str (drop 1 (drop-while #(not= \: %) (drop 1 line)))))

(defn read-one-line [socket]
  (.readLine socket))

(defn connected? [reader]
  (let [line (read-one-line reader)]
    (cond
      (nil? line)
      (do (println "Something went wrong")
          false)

      (includes? line (apply str [nick " MODE"]))
      (do (println "Connected")
          true)

      (includes? line "433")
      (do (println "Nickname is in use")
          false)

      :else
      (do (println line)
          (recur reader)))))

(defn login [reader writer]
  (write writer "NICK" nick)
  (write writer "USER" user)
  (.flush writer)
  (connected? reader))

(defn join-channel [writer]
  (write writer "JOIN" channel)
  (.flush writer))
  
(def pong-answer '("pong" "понг" "PONG!" "пунг"))
(defn get-random-pong []
  (rand-nth pong-answer))

(defn if-includes? [line words]
  (some true? (map #(includes? (lower-case line) %) words)))

(defn extract-nick [line]
  (let [start 1 stop (index-of line "!")]
    (if (not (nil? stop))
      (subs line start (index-of line "!"))
      channel)))

(defn answer-server-ping [writer]
  (write writer "PONG" nick)
  (.flush writer))

(defn privmsg! [writer messages]
  (doseq [message messages]
    (write writer "PRIVMSG" message))
  (.flush writer))

(defn answer-to-a-curse [writer]
  (privmsg! writer [(format "%s :%s" channel "Разврат и содомия!")]))

(def greeters '("hi" "hello" "hola" "nazdar" "aloha" "ahoj" "sieg heil"
                "привет" "хай" "дратути" "прив" "sholom" "шолом" "шалом"))
(defn get-random-greeter []
  (rand-nth greeters))

(def giphy-random-url
  "https://api.giphy.com/v1/gifs/random?api_key=dc6zaTOxFJmzC&tag=")
(defn get-random-gif [topic]
  (let [response (json/read-str (slurp (apply str [giphy-random-url topic])))]
    (let [url (get (get response "data") "image_url")]
      (match [url]
             [nil] "giphy does not have anything for you, bro ;("
             [_] url))))

(defn write-gif-url [writer topic]
  (privmsg! writer [(format "%s :%s" channel (get-random-gif topic))]))

(defn get-topic [line]
  (strings/replace (apply str (drop 5 line)) #"\s" "%20"))

(def gay-spam (atom {}))
(defn update-gays [m line]
  (when (if-includes? line ["gay" "гей" "пидор" "геи" "gays"])
    (swap! m update-in [(extract-nick line)]
           (fn [old] (inc (or old 0))))))

(defn get-most-gay [m]
  (apply max-key val @m))

(defn get-gay-string [m line]
  (let [clear-line (clean-line line)]
    (if (= (apply str (drop 5 clear-line)) "")
      (format "%s is a natural born gay" (key (get-most-gay m)))
      (let [username (apply str (drop 5 clear-line))]
        (let [gay-score (get @m username)]
          (if (nil? gay-score)
            (format "%s is not gay" username)
            (let [most-gay-val (val (get-most-gay m))]
              (let [gay-percentage (* 100 (/ gay-score most-gay-val))]
                (format "%s is %s percent gay" username gay-percentage)))))))))

(defn write-gay [writer line]
    (privmsg! writer [(format "%s :%s" channel (get-gay-string gay-spam line))]))

(defn read-channel [reader writer]
  (let [raw-line (read-one-line reader)]
    (if (not (nil? raw-line))
      (let [line (clean-line raw-line)]
        (println raw-line)
        (async/go
          (update-gays gay-spam raw-line))

        (cond
          (starts-with? raw-line "PING :")
          (async/thread (answer-server-ping writer))

          (starts-with? line "!gay")
          (async/thread
            (write-gay writer raw-line))

          (starts-with? line "!gif")
          (async/thread
            (write-gif-url writer (get-topic line)))

          (if-includes? line '("sex" "разврат" "содомия"
                               "панин" "секс" "ебля"
                               "трах" "ебать"))
          (async/thread (answer-to-a-curse writer))

          (if-includes? line '("русня" "русак" "рузьге" "русский"))
          (async/thread
            (privmsg! writer [(format "%s :русофобию в чяте ощущаю я" channel)]))
          
          (includes? line nick)
          (cond
            (if-includes? line '("ping" "пинг"))
            (async/thread
              (privmsg! writer [(format "%s :%s: %s" channel (extract-nick raw-line) (get-random-pong))]))
              
            (if-includes? line '("хуй" "dick" "гей" "gay"))
            (async/thread
              (privmsg! writer [(format "%s :%s: сам три дня не умывался!" channel (extract-nick raw-line))]))
                       
            (if-includes? line greeters)
            (async/thread
              (privmsg! writer [(format "%s :%s: %s" channel (extract-nick raw-line) (get-random-greeter))])))))

      (System/exit 1)))
  (recur reader writer))

(defn -main []
  (cond (not (nil? socket))
        (cond (login reader writer)
              (do
                (join-channel writer)
                (read-channel reader writer)))))
 
