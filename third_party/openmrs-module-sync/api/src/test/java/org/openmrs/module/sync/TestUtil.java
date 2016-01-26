/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.sync;

import org.junit.Ignore;
import org.openmrs.module.ModuleUtil;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.OpenmrsUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

@Ignore
public class TestUtil {


    public static final String TEST_DATASETS_PROPERTIES_FILE = "test-datasets.properties";

    /**
     * Determines the name of the proper test dataset based on what version of OpenMRS we are testing against
     */
    public String getTestDatasetFilename(String testDatasetName) throws Exception {

        InputStream propertiesFileStream = null;

        // try to load the file if its a straight up path to the file or
        // if its a classpath path to the file
        if (new File(TEST_DATASETS_PROPERTIES_FILE).exists()) {
            propertiesFileStream = new FileInputStream(TEST_DATASETS_PROPERTIES_FILE);
        } else {
            propertiesFileStream = getClass().getClassLoader().getResourceAsStream(TEST_DATASETS_PROPERTIES_FILE);
            if (propertiesFileStream == null)
                throw new FileNotFoundException("Unable to find '" + TEST_DATASETS_PROPERTIES_FILE + "' in the classpath");
        }

        Properties props = new Properties();

        OpenmrsUtil.loadProperties(props, propertiesFileStream);

        if (props.getProperty(testDatasetName) == null) {
            throw new Exception ("Test dataset named " + testDatasetName + " not found in properties file");
        }

        return props.getProperty(testDatasetName);
    }

	/**
	 * @return true if the current openmrs version (stripped of any snapshot or build information)
	 * is at least as high as the passed minimumVersion or if the passed minimumVersion is null
	 */
	public static boolean isOpenmrsVersionAtLeast(String minimumVersion) {
		if (minimumVersion == null) {
			return true;
		}
		String currentVersion = OpenmrsConstants.OPENMRS_VERSION;
		String versionWithoutSnapshot = currentVersion.split("[\\s\\-]")[0];

		boolean result =  (ModuleUtil.compareVersion(versionWithoutSnapshot, minimumVersion) >= 0);
		return result;
	}
}
