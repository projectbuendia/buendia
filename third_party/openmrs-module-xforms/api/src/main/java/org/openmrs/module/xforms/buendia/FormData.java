/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.xforms.buendia;

/** Simple bean for information about an XForms form after it has been built. */
public class FormData {
    /** The Xform XML representation of the form. */
    public final String xml;

    /** Whether the XML includes data about currently available providers. */
    public final boolean includesProviders;

    /** Whether the XML includes data about currently available locations. */
    public final boolean includesLocations;

    public FormData(String xml, boolean includesProviders, boolean includesLocations) {
        this.xml = xml;
        this.includesProviders = includesProviders;
        this.includesLocations = includesLocations;
    }
}
