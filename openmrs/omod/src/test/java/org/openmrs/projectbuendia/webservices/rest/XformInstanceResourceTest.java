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
package org.openmrs.projectbuendia.webservices.rest;

import org.junit.Test;
import org.openmrs.projectbuendia.Utils;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.assertXmlEqual;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.getXmlResource;

public class XformInstanceResourceTest {
    @Test public void addForm() throws Exception {
        Document expected = getXmlResource(getClass(), "expected-instance-add.xml");
        Document doc = getXmlResource(getClass(), "original-instance-add.xml");
        XformInstanceResource.adjustXformDocument(doc, 8, 1, Utils.parse8601("2014-11-15T12:34:56.789Z"));
        assertXmlEqual(expected, doc);
    }

    @Test public void editForm() throws Exception {
        Document expected = getXmlResource(getClass(), "expected-instance-edit.xml");
        Document doc = getXmlResource(getClass(), "original-instance-edit.xml");
        XformInstanceResource.adjustXformDocument(doc, 9, 2, Utils.parse8601("2014-11-15T12:34:56.789Z"));
        assertXmlEqual(expected, doc);
    }

    @Test public void moveGroupsIntoObs() throws Exception {
        Document expected = getXmlResource(getClass(), "expected-grouped.xml");
        Document doc = getXmlResource(getClass(), "original-grouped.xml");
        XformInstanceResource.adjustXformDocument(doc, 10, 3, Utils.parse8601("2014-11-15T12:34:56.789Z"));
        assertXmlEqual(expected, doc);
    }

    @Test public void parseNonstandardTimestamp() {
        String input = "20141120T092547.373Z";
        String expected = "2014-11-20T09:25:47.373Z";
        String actual = Utils.formatUtc8601(XformInstanceResource.parseTimestamp(input));
        assertEquals(expected, actual);
    }

    @Test public void parseStandardTimestamp() {
        String input = "2014-11-20T09:25:47.373Z";
        String expected = "2014-11-20T09:25:47.373Z";
        String actual = Utils.formatUtc8601(XformInstanceResource.parseTimestamp(input));
        assertEquals(expected, actual);
    }
}
