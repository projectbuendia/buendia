package org.openmrs.projectbuendia.webservices.rest;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XformResourceTest {

    @Test
    public void convertToOdkCollect() throws Exception {
        String input = readResourceAsString("sample-original-form1.xml");
        String expected = readResourceAsString("expected-result-form1.xml");
        String actual = XformResource.convertToOdkCollect(input, "Form title");
        assertXmlEqual(expected, actual);
    }

    // At some stage, everything under here should probably go into common helper classes...
    
    private static String readResourceAsString(String file) throws IOException {
        return IOUtils.toString(XformResourceTest.class.getResourceAsStream(file), "utf-8");
    }
    
    /**
     * Normalizes XML by parsing and reformatting, then asserts that the two documents
     * are equal.
     */
    private static void assertXmlEqual(String expected, String actual) throws TransformerException, SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();

        Document expectedDoc = documentBuilder.parse(new InputSource(new StringReader(expected)));
        Document actualDoc = documentBuilder.parse(new InputSource(new StringReader(actual)));
        
        expected = toIndentedString(expectedDoc);
        actual = toIndentedString(actualDoc);
        
        assertEquals(expected, actual);
    }
    
    /**
     * Converts an XML document into a string, applying indentation.
     * Note that this isn't quite as robust as I'd like it to be - the first child element
     * appears not to get indented fully automatically, so the tests are still sensitive
     * to that bit of indentation... but only that bit. Odd.
     */
    private static String toIndentedString(Document doc) throws TransformerException {
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
