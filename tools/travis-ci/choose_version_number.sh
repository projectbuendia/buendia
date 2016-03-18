#!/bin/bash
# Match 'v1000' or 'v1000.3000' or 'v1000.30.12'
if [[ "$TRAVIS_TAG" =~ ^v([0-9]+(\.[0-9]+){0,2})$ ]]; then
  echo "${BASH_REMATCH[1]}"
else
  echo "0.0.$TRAVIS_BUILD_NUMBER"
fi
