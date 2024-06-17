#!/bin/bash
git pull origin --tags > /dev/null 2>&1
git log --pretty=format:"%h %s" --no-merges $(git describe --tags --abbrev=0)..HEAD
