#!/bin/bash

# 最新tag，例如：v1.0.0
latest_tag=$(git describe --tags `git rev-list --tags --max-count=1`)
echo ">>>latest tag: $latest_tag"

# 最新tag，例如：1.0.0
vtag=${latest_tag#*v}
echo ">>>latest tag(no v): $vtag"

# snapshot版本自增（最新tag+1）
vtaglist=(${vtag//./ })
firsttag=${vtaglist[0]}
secondtag=${vtaglist[1]}
thirdtag=${vtaglist[2]}
thirdtag=`expr $thirdtag + 1`

new_version=$firsttag.$secondtag.$thirdtag

echo ">>>new version: $new_version"

sed -i 's#def version.*#def version = \"'$new_version'-SNAPSHOT\"#g' config.gradle

echo ">>>over!!!"



