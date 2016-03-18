#!/bin/bash

# NOTE: Because this script uses git to create the build server, there's a non-zero chance that
# we'll get merge conflicts and the script will fail because Travis CI builds can run in parallel.
# Let's see how that goes for the time being, and if it doesn't work we'll sort it out later.

# Die if any command fails. Better that we know about it through a build failure.
set -e

# Setup: Sanity checks!

# Check that this actually is the correct repo, and not a fork.
if [ "$TRAVIS_REPO_SLUG" != "projectbuendia/buendia" ]; then
  echo "This repo is a fork, not pushing the build."
  exit 0
fi

# Check that GITHUB_API_TOKEN is set. We do this before other checks because if this isn't set, we
# want early notification - that is, we want the CI to fail on all builds, not just releases.
if [ -z "$GITHUB_API_TOKEN" ]; then
  echo "No GITHUB_API_TOKEN environment variable set, we require one to push builds."
  exit 1
fi

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "This is a pull request, not deploying to build server."
  # Shouldn't count as failure
  exit 0
fi

case "$TRAVIS_BRANCH" in
    dev|master) ;;
    *)
      echo "This branch isn't \`dev\` or \`master\`, not deploying to build server."
      # Not a failure
      exit 0
      ;;
esac

echo "Deploying to build server."

# Configure git so that commits look sensible.
git config --global user.email "noreply+travis-ci@projectbuendia.org"
git config --global user.name "Travis CI"

# Make a working directory to create the file structure in.
dir=`mktemp -d`
# Clone the build server repo.
# Use --quiet and /dev/null because we don't want to leak the API token.
git clone --quiet https://${GITHUB_API_TOKEN}@github.com/projectbuendia/builds.git --depth=1 --single-branch --branch=gh-pages "$dir" > /dev/null

# The output debian packages from the build process are dropped in their individual source
# directories (TODO: fix this) so we consolidate them all to the temporary directory.

cp `find packages -name *.deb` dir
# Actually run the index step.
packages/buendia-pkgserver/data/usr/bin/buendia-pkgserver-index-debs "$dir"
cd "$dir"
git add .
git commit -m "Autoupdate package server from Travis CI build $TRAVIS_BUILD_NUMBER."
# The API token is encoded in the remote, so the `git push` will use the same credentials as the
# clone.
git push --quiet >/dev/null
rm "$dir"

echo "Successfully deployed."
