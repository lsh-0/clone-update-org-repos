#!/bin/bash
set -e
clear
rg "git://" -g '!elifesciences--repos--page0*' -g '!*.sch' -g '!*.xml' -g '!*.xsl' -g '!gp.sh'
