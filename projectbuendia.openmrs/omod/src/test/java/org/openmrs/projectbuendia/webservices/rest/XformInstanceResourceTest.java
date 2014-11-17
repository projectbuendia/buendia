package org.openmrs.projectbuendia.webservices.rest;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.assertXmlEqual;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.readResourceAsString;

import org.junit.Test;
import org.openmrs.module.webservices.rest.SimpleObject;

public class XformInstanceResourceTest {

    @Test
    public void addForm() throws Exception {
        String input = readResourceAsString(getClass(), "original-instance-add.xml");
        String expected = readResourceAsString(getClass(), "expected-instance-add.xml");
        SimpleObject post = new SimpleObject();
        post.add(XformInstanceResource.DATE_ENTERED_PROPERTY, "2014-11-15");
        post.add(XformInstanceResource.ENTERER_ID_PROPERTY, 1);
        post.add(XformInstanceResource.XML_PROPERTY, input);
        String actual = XformInstanceResource.completeXform(post);
        assertXmlEqual(expected, actual);
    }

    @Test
    public void editForm() throws Exception {
        String input = readResourceAsString(getClass(), "original-instance-edit.xml");
        String expected = readResourceAsString(getClass(), "expected-instance-edit.xml");
        SimpleObject post = new SimpleObject();
        post.add(XformInstanceResource.DATE_ENTERED_PROPERTY, "2014-11-15");
        post.add(XformInstanceResource.ENTERER_ID_PROPERTY, 1);
        post.add(XformInstanceResource.PATIENT_ID_PROPERTY, 10);
        post.add(XformInstanceResource.XML_PROPERTY, input);
        String actual = XformInstanceResource.completeXform(post);
        assertXmlEqual(expected, actual);
    }
}
