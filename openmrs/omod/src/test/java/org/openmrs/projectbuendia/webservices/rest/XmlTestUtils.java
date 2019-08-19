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

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.junit.Assert.assertEquals;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.elementsIn;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtils.getChildNodes;

public class XmlTestUtils {
    static String getStringResource(Class<?> cls, String path) throws IOException {
        return IOUtils.toString(cls.getResourceAsStream(path), "utf-8");
    }

    static Document getXmlResource(Class<?> cls, String path) throws IOException, SAXException {
        return XmlUtils.parse(getStringResource(cls, path));
    }

    static void assertXmlEqual(Document expectedDoc, Document actualDoc) throws TransformerException {
        String expectedXml = toIndentedString(expectedDoc);
        String actualXml = toIndentedString(actualDoc);
        assertEquals(expectedXml, actualXml);
    }

    /** Checks that two documents are equal after normalization. */
    static void assertXmlEqual(String expectedXml, String actualXml)
        throws TransformerException, SAXException, IOException {
        assertXmlEqual(XmlUtils.parse(expectedXml), XmlUtils.parse(actualXml));
    }

    /**  Formats an XML document by indenting and trimming whitespace from elements. */
    static String toIndentedString(Document doc) throws TransformerException {
        for (Element element : elementsIn(doc.getElementsByTagName("*"))) {
            for (Node node : getChildNodes(element)) {
                if (node.getNodeType() == Node.TEXT_NODE) {
                    node.setNodeValue(node.getNodeValue().trim());
                }
            }
        }
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter outStream = new StringWriter();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(outStream);
        transformer.transform(source, result);
        return outStream.toString();
    }
}
