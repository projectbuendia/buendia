package org.openmrs.projectbuendia.webservices.rest;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.assertXmlEqual;
import static org.openmrs.projectbuendia.webservices.rest.XmlTestUtil.readResourceAsString;

import org.junit.Test;

public class XformResourceTest {

    @Test
    public void convertToOdkCollect() throws Exception {
        String input = readResourceAsString(getClass(), "sample-original-form1.xml");
        String expected = readResourceAsString(getClass(), "expected-result-form1.xml");
        String actual = XformResource.convertToOdkCollect(input, "Form title");
        assertXmlEqual(expected, actual);
    }

    @Test
    public void removeRelationshipNodes() throws Exception {
        String input = readResourceAsString(getClass(), "relationships-original-form1.xml");
        String expected = readResourceAsString(getClass(), "relationships-result-form1.xml");
        String actual = XformResource.removeRelationshipNodes(input);
        assertXmlEqual(expected, actual);
    }
}
