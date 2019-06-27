# buendia

**Build status:** [![CircleCI](https://circleci.com/gh/projectbuendia/buendia/tree/dev.svg?style=svg)](https://circleci.com/gh/projectbuendia/buendia/tree/dev)

The latest stable release is **v0.11.1**.

You can install Buendia on a stock Debian stretch instance as follows:

```
sudo apt-get install apt-transport-https
echo "deb [trusted=yes] https://projectbuendia.github.io/builds/packages unstable main java" >/etc/apt/sources.list.d/buendia.list
sudo apt-get update
sudo apt-get install -y buendia-server buendia-site-test
```

The main repository for Project Buendia. See the
[Buendia wiki](https://github.com/projectbuendia/buendia/wiki) for details.

 - **client**: Android client app (from the projectbuendia/client repo).
 - **devices**: Scripts for setting up server devices.
 - **openmrs**: OpenMRS module that provides the server API.
 - **packages**: Subdirectories for each .deb package built from this repo.
 - **tools**: Data management and server administration utilities.

To set up an Edison as a new demo server, run `./setup-demo`.

#### Copyright notice

    Copyright 2015 The Project Buendia Authors

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
