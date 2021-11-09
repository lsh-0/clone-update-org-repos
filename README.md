# clone+update org repo list

Clones/updates an entire organisation's set of Github repositories, excluding archived/disabled/forked repositories.

Just a quick script to investigate the Joker implementation of the Clojure language while searching for any signs of

    "git://github.com"

## requisites

* [Joker](https://joker-lang.org)
* a Github Personal Access Token

optional: 

* ripgrep (`rg`)

## usage

    GITHUB_USER=<username> GITHUB_TOKEN=<personal-access-token> GITHUB_ORG=some-org joker do.clj

and optionally:

    ./gp.sh

to find any `git://` protocol usages.
