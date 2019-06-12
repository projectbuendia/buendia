#!/bin/bash
# Copyright 2019 The Project Buendia Authors
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.  You may obtain a copy
# of the License at: http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distrib-
# uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
# OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
# specific language governing permissions and limitations under the License.

# get_package_version.sh tries to come up with a sensible package version
# scheme for the current build situation. The logic we're following is that
# only release packages built in CI get a formal version number; all other
# builds should be marked as such.
#
# Also, we should treat CI builds as more recent than release builds for the
# same version tag; however, we should NOT treat manual builds as more recent
# than either. The logic there is that we do want to allow deployment of
# replicable pre-release builds, but we don't want accidental deployment of 
# developer builds. The most recent version is retained for the sanity of the
# developer.

# If we're building in CircleCI and this is a tagged release, strip the leading
# 'v' and use that as a version number.
if echo "$CIRCLE_TAG" | grep -Eq '^v([0-9]+\.?)+$'; then
    echo "${CIRCLE_TAG#v}"
    exit
fi

# Find the most recent release tag on master.
TAG=$(git describe --match='v*' --abbrev=0 origin/master)

# If we're building in CircleCI, the version is "x.y.z+b<n>" where <n> is the
# latest build number. 
if [ -n "$CIRCLE_BUILD_NUM" ]; then
    echo "${TAG#v}+b${CIRCLE_BUILD_NUM}"
    exit
fi

# Otherwise, we're building locally, so the version should be "x.y.z~g<commit>"
# where <commit> is the latest unambiguous commit identifier. This version is
# actually treated by Debian as _less_ recent than "x.y.z".
COMMIT=$(git describe --abbrev --dirty=+dirty)
echo "${TAG#v}~${COMMIT#*g}"
