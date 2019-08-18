# This Makefile automates the creation and updating of the `initial` snapshot
# for the `testing` VM.
#
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

all: update

initial: clean
	@echo Initializing testing VM...
	vagrant up testing
	@echo Snapshotting initial state of testing VM...
	vagrant snapshot save testing initial

update:
	@echo Checking to see if testing VM exists...
	@vagrant snapshot list testing | grep -q initial || make initial
	@echo Restoring and updating initial snapshot of testing VM...
	vagrant snapshot restore --provision testing initial
	@echo Saving updates to initial snapshot of testing VM...
	vagrant snapshot save --force testing initial

clean:
	vagrant destroy testing
