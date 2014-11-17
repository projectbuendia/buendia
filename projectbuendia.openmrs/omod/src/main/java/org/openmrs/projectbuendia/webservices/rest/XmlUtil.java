package org.openmrs.projectbuendia.webservices.rest;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** XML utility methods. */
public class XmlUtil {
    
    private static final DocumentBuilder documentBuilder;

    static {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Iterable<Node> toIterable(NodeList nodeList) {
        // TODO(jonskeet): Lazy implementation.
        List<Node> nodes = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            nodes.add(nodeList.item(i));
        }
        return nodes;
    }

    public static Iterable<Element> toElementIterable(NodeList nodeList) {
        // TODO(jonskeet): Lazy implementation.
        List<Element> elements = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }
        return elements;
    }
    
    public static void removeNode(Node node) {
        node.getParentNode().removeChild(node);
    }
    
    /** Finds exactly one element with the given name within the specified element. */
    public static Element getElementOrThrow(Element element, String name) {
        NodeList elements = element.getElementsByTagName(name);
        if (elements.getLength() != 1) {
            throw new IllegalPropertyException("Element " + element.getNodeName()
                    + " must have exactly one " + name + " element");
        }
        return (Element) elements.item(0);
    }
    

    public static Element getElementOrThrowNS(Element element, String namespaceURI, String localName) {
        NodeList elements = element.getElementsByTagNameNS(namespaceURI, localName);
        if (elements.getLength() != 1) {
            throw new IllegalPropertyException("Element "
                    + element.getNodeName() + " must have exactly one " + localName
                    + " element");
        }
        return (Element) elements.item(0);
    }
    
    /** Returns a namespace-aware DocumentBuilder. */
    public static DocumentBuilder getDocumentBuilder() {
        return documentBuilder;
    }
    
    public static Document parse(String xml) throws SAXException, IOException {
        return documentBuilder.parse(new InputSource(new StringReader(xml)));
    }
}
