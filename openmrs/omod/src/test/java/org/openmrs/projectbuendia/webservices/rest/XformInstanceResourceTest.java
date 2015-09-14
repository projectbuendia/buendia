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
import org.openmrs.module.webservices.rest.SimpleObject;

import static org.junit.Assert.assertEquals;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.assertXmlEqual;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.readResourceAsString;

public class XformInstanceResourceTest {
    @Test
    public void addForm() throws Exception {
        String input = readResourceAsString(getClass(), "original-instance-add.xml");
        String expected = readResourceAsString(getClass(), "expected-instance-add.xml");
        SimpleObject post = new SimpleObject();
        post.add("date_entered", "2014-11-15");
        post.add("enterer_id", 1);
        post.add("xml", input);
        String actual = XformInstanceResource.completeXform(post);
        assertXmlEqual(expected, actual);
    }

    @Test
    public void editForm() throws Exception {
        String input = readResourceAsString(getClass(), "original-instance-edit.xml");
        String expected = readResourceAsString(getClass(), "expected-instance-edit.xml");
        SimpleObject post = new SimpleObject();
        post.add("date_entered", "2014-11-15");
        post.add("enterer_id", 1);
        post.add("patient_id", 10);
        post.add("xml", input);
        String actual = XformInstanceResource.completeXform(post);
        assertXmlEqual(expected, actual);
    }

    @Test
    public void moveGroupsIntoObs() throws Exception {
        String input = readResourceAsString(getClass(), "original-grouped.xml");
        String expected = readResourceAsString(getClass(), "expected-grouped.xml");
        SimpleObject post = new SimpleObject();
        post.add("date_entered", "2014-11-15");
        post.add("enterer_id", 1);
        post.add("xml", input);
        String actual = XformInstanceResource.completeXform(post);
        assertXmlEqual(expected, actual);
    }

    @Test
    public void workAroundClientIssue_beforeFix() {
        String input = "20141120T092547.373Z";
        String expected = "2014-11-20T09:25:47.373Z";
        String actual = XformInstanceResource.workAroundClientIssue(input);
        assertEquals(expected, actual);
    }

    @Test
    public void workAroundClientIssue_afterFix() {
        // Nothing to fix
        String input = "2014-11-20T09:25:47.373Z";
        String expected = "2014-11-20T09:25:47.373Z";
        String actual = XformInstanceResource.workAroundClientIssue(input);
        assertEquals(expected, actual);
    }
}
