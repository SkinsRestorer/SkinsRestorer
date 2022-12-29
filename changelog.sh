#!/bin/bash
git pull origin --tags
git log --pretty=format:"%h %s" --no-merges $(git describe --tags --abbrev=0)..HEAD
