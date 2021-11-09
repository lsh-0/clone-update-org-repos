#!/bin/bash
# searches the cloned repos for mentions of given search term
set -ex
val="$1"
rg "$val" \
    --no-ignore --no-hidden --no-binary \
    --glob-case-insensitive \
    --max-filesize 5M \
    -g '!*.ipynb' \
    -g '!*.xml' \
    -g '!*.header' \
    -g '!*.checkin' \
    -g '!*.new' \
    -g '!*.generated*'
