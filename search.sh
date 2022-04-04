#!/bin/bash
# searches the cloned repos for mentions of given search term
set -ex
val="$1"
shift
clear
rg "$val" \
    --no-ignore --no-hidden --no-binary \
    --glob-case-insensitive \
    --max-filesize 5M \
    -g '!elifesciences--repos--page*.json' \
    -g '!*.ipynb' \
    -g '!*.xml' \
    -g '!*.svg' \
    -g '!*.header' \
    -g '!*.checkin' \
    -g '!*.new' \
    -g '!*.generated*' \
    "$@"
