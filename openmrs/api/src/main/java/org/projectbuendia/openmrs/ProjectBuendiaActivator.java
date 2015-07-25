// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.openmrs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.ModuleActivator;

/** Logic that is run every time this module is either started or stopped. */
public class ProjectBuendiaActivator implements ModuleActivator {

    protected Log log = LogFactory.getLog(getClass());

    public void willRefreshContext() {
        log.info("Refreshing Project Buendia module");
    }

    public void contextRefreshed() {
        log.info("Project Buendia module refreshed");
    }

    public void willStart() {
        log.info("Starting Project Buendia module");
    }

    public void started() {
        log.info("Project Buendia module started");
    }

    public void willStop() {
        log.info("Stopping Project Buendia module");
    }

    public void stopped() {
        log.info("Project Buendia module stopped");
    }
}
