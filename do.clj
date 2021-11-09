(comment
  "clones and updates all public+private repositories that are not archived/disabled/forks of other repositories.")

(ns foo.fetch
  (:require
   [joker
    [base64 :as base64]
    [http :as http]
    [os :as os]
    [json :as json]
    [string :as string]]))

(def github-user (-> (os/env) (get "GITHUB_USER")))
(def github-token (-> (os/env) (get "GITHUB_TOKEN")))
(def github-org (-> (os/env) (get "GITHUB_ORG")))

(assert github-user "the envvar 'GITHUB_USER' is not set")
(assert github-token "the envvar 'GITHUB_TOKEN' is not set")
(assert github-org "the envvar 'GITHUB_ORG' is not set")

(def auth-string (base64/encode-string (str github-user ":" github-token)))

(defn cached-fetch
  "makes an authorised request to `api.github.com` and returns the deserialised JSON results.
  results are cached to `cache-path` and used the next time the request is made.
  returns `nil` if no results returned."
  [url cache-path]
  (if (os/exists? cache-path)
    (let [file-body (do (println (str "slurping: " cache-path))
                        (json/read-string (slurp cache-path)))]
      (when-not (empty? file-body)
        file-body))

    (let [url (str "https://api.github.com" url)
          opts {:url url
                :headers {"Authorization" (str "Basic " auth-string)}}
          resp (do (println "requesting: " url)
                   (http/send opts))]
      (if (not (= (:status resp) 200))
        (do (print "error: non-200 response")
            (pprint resp))
        (let [json-body (:body resp)]
          (spit cache-path json-body)
          (if (= json-body "[]")
            ;; reached end of results. a 404 would have been nicer :(
            nil
            (json/read-string json-body)))))))

(defn repo-list
  "returns a list of repositories for the given `org`.
  pages through the results until no results left.
  uses cached results if found."
  ([org]
   (repo-list org 1))
  ([org start-page]
   (loop [page start-page
          result-list []]
     (println (count result-list))
     (let [url (format "/orgs/%s/repos?page=%d&per_page=100&type=all" org page)
           cache-path (format "%s--repos--page%02d.json" github-org page)
           resp (cached-fetch url cache-path)]
       
       (if (nil? resp)
         result-list
         (recur (inc page)
                (into result-list resp)))))))

(defn print-result
  [result fail-msg]
  (if (:success result)
    result
    
    (do (println (str fail-msg ": " (:err-msg result)))
        (println "stdout:")
        (println (:stdout result))
        (println "stderr:")
        (println (:stderr result)))))

(defn clone+update-repo
  "clones the given `repo` if it doesn't exist or performs a `git pull` if it does."
  [repo]
  (let [full-name (get repo "full_name")
        output-path (get repo "name")
        clone-args {:args ["clone" (str "ssh://git@github.com/" full-name)]}
        pull-args {:args ["-C" output-path "pull"]}
        ]
    (if (os/exists? (str output-path))
      (do (println "updating" full-name)
          (print-result (os/exec "git" pull-args)
                        (format "failed to pull '%s'" full-name)))

      (do (println "cloning" full-name)
          (print-result (os/exec "git" clone-args)
                        (format "failed to clone '%s'" full-name))))))

(defn clone+update-all-repos
  [repo-list]
  (mapv clone+update-repo repo-list))

;; ----

(defn org-repo-list
  []
  (let [results (repo-list github-org)
        print-repo (fn [repo]
                     (println (get repo "full_name")))

        archived-disabled-fn (fn [repo]
                               (or (get repo "archived")
                                   (get repo "disabled")))

        archived-disabled (filter archived-disabled-fn results)

        private (filter (fn [repo]
                          (get repo "private")) results)

        forked (filter (fn [repo]
                         (get repo "fork")) results)

        viable-fn (fn [repo]
                    (or (get repo "archived")
                        (get repo "disabled")
                        (get repo "fork")))

        viable (remove viable-fn results)
        
        total (count results)]
    (mapv print-repo results)
    (println (format "%d repos" total))
    (println)
  
    (mapv print-repo archived-disabled)
    (println (format "%d/%d archived or disabled" (count archived-disabled) total))
    (println)

    (mapv print-repo private)
    (println (format "%d/%d private" (count private) total))
    (println)

    (mapv print-repo forked)
    (println (format "%d/%d forks" (count forked) total))
    (println)

    (println (format "%d viable repositories" (count viable)))
    (println)

    viable))

;; --

(defn -main
  [& args]
  (clone+update-all-repos (org-repo-list)))

(when (= *main-file* *file*)
  (-main))
