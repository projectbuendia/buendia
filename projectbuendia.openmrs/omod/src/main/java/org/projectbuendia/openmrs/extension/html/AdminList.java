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

package org.projectbuendia.openmrs.extension.html;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.module.Extension;
import org.openmrs.module.web.extension.AdministrationSectionExt;

/**
 * This class defines the links that will appear on the administration page
 * under the "projectbuendia.openmrs.title" heading.
 */
public class AdminList extends AdministrationSectionExt {

    /** @see AdministrationSectionExt#getMediaType() */
    public Extension.MEDIA_TYPE getMediaType() {
        return Extension.MEDIA_TYPE.html;
    }

    /** @see AdministrationSectionExt#getTitle() */
    public String getTitle() {
        return "projectbuendia.openmrs.title";
    }

    /** @see AdministrationSectionExt#getLinks() */
    public Map<String, String> getLinks() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        map.put("/module/projectbuendia/openmrs/manage.form",
                "projectbuendia.openmrs.manage");
        return map;
    }
}
