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
    
    /**
     * Returns all the direct child elements of the given element.
     */
    public static List<Element> getElements(Element element) {
        List<Element> elements = new ArrayList<>();
        for (Node node : toIterable(element.getChildNodes())) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    /**
     * Returns all the direct child elements of the given element with the specified namespace/local name.
     * Use null to indicate the empty namespace.
     */
    public static List<Element> getElementsNS(Element element, String namespaceURI, String localName) {
        List<Element> elements = new ArrayList<>();
        for (Element candidate : getElements(element)) {
            if (namespaceURI.equals(candidate.getNamespaceURI())
                    && localName.equals(candidate.getLocalName())) {
                elements.add(candidate);
            }
        }
        return elements;
    }

    /** Finds exactly one direct child element with the given name in the empty namespace URI. */
    public static Element getElementOrThrow(Element element, String name) {
        return getElementOrThrowNS(element, null, name);
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
    
    public static Element appendElementNS(Element parent, String namespaceURI, String localName) {
        Element ret = parent.getOwnerDocument().createElementNS(namespaceURI, localName);
        parent.appendChild(ret);
        return ret;
    }

    
    /** Returns a namespace-aware DocumentBuilder. */
    public static DocumentBuilder getDocumentBuilder() {
        return documentBuilder;
    }
    
    public static Document parse(String xml) throws SAXException, IOException {
        return documentBuilder.parse(new InputSource(new StringReader(xml)));
    }
}
