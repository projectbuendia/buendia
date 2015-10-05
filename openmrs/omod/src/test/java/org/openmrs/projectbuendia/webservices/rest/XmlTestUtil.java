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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static org.junit.Assert.assertEquals;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.toElementIterable;
import static org.openmrs.projectbuendia.webservices.rest.XmlUtil.toIterable;

public class XmlTestUtil {
    static String readResourceAsString(Class<?> clazz, String file) throws IOException {
        return IOUtils.toString(clazz.getResourceAsStream(file), "utf-8");
    }

    /**
     * Normalizes XML by parsing and reformatting, then asserts that the two documents
     * are equal.
     */
    static void assertXmlEqual(String expected, String actual) throws TransformerException,
        SAXException, IOException, ParserConfigurationException {
        Document expectedDoc = XmlUtil.parse(expected);
        Document actualDoc = XmlUtil.parse(actual);
        expected = toIndentedString(expectedDoc);
        actual = toIndentedString(actualDoc);

        assertEquals(expected, actual);
    }

    /**
     * Converts an XML document into a string, applying indentation. First all elements have
     * their text
     * content trimmed, just for simplicity.
     */
    static String toIndentedString(Document doc) throws TransformerException {
        for (Element element : toElementIterable(doc.getElementsByTagName("*"))) {
            for (Node node : toIterable(element.getChildNodes())) {
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
