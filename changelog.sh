#!/bin/bash
git pull origin --tags
git log --pretty=format:"[%h](https://github.com/SkinsRestorer/SkinsRestorerX/commit/%H) %s" --no-merges $(git describe --tags --abbrev=0)..HEAD
