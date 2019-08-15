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

import org.openmrs.module.webservices.rest.web.response.IllegalPropertyException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/** XML manipulation functions. */
public class XmlUtils {
    /** Converts a NodeList to an Iterable of Elements. */
    public static Iterable<Element> elementsIn(NodeList nodeList) {
        List<Element> elements = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }
        return elements;
    }

    /** Removes a node from its tree. */
    public static void removeNode(Node node) {
        node.getParentNode().removeChild(node);
    }

    /**
     * Given an element, returns all its direct child elements that have the
     * specified namespace and name.  Use null to indicate the empty namespace.
     */
    public static List<Element> getChildren(
        Element element, String namespaceURI, String localName) {
        List<Element> elements = new ArrayList<>();
        for (Element candidate : getChildren(element)) {
            if (namespaceURI.equals(candidate.getNamespaceURI())
                && localName.equals(candidate.getLocalName())) {
                elements.add(candidate);
            }
        }
        return elements;
    }

    /** Returns all the direct child elements of the given element. */
    public static List<Element> getChildren(Element element) {
        List<Element> elements = new ArrayList<>();
        for (Node node : getChildNodes(element)) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) node);
            }
        }
        return elements;
    }

    /** Gets an Iterable over the child nodes of a given node. */
    public static Iterable<Node> getChildNodes(Node node) {
        NodeList list = node.getChildNodes();
        List<Node> nodes = new ArrayList<>(list.getLength());
        for (int i = 0; i < list.getLength(); i++) {
            nodes.add(list.item(i));
        }
        return nodes;
    }

    /** Requires the given element to have the given tag name. */
    public static Element requireElementTagName(Element element, String name) {
        if (!element.getLocalName().equals(name)) {
            throw new IllegalPropertyException(String.format(
                "Expected <%s> element but found <%s>", name, element.getLocalName()));
        }
        return element;
    }

    /** Gets a child element by name, requiring exactly one such descendant. */
    public static Element requireDescendant(Element element, String name) {
        return requireDescendant(element, null, name);
    }

    /** Gets a sequence of descending children by name, requiring each to exist. */
    public static Element requirePath(Element element, String... names) {
        for (String name : names) {
            element = requireDescendant(element, name);
        }
        return element;
    }

    /** Gets a child by namespace and name, requiring exactly one such child. */
    public static Element requireDescendant(
        Element element, String ns, String localName) {
        NodeList elements = element.getElementsByTagNameNS(ns, localName);
        int count = elements.getLength();
        if (count != 1) {
            throw new IllegalPropertyException(String.format(
                "<%s> element should contain one <%s> element, but there are %s",
                element.getTagName(), localName, count == 0 ? "none" : "" + count));
        }
        return (Element) elements.item(0);
    }

    /** Appends a new empty child element with the given namespace and name. */
    public static Element appendChild(Element parent, String ns, String localName) {
        Element ret = parent.getOwnerDocument().createElementNS(ns, localName);
        parent.appendChild(ret);
        return ret;
    }

    /** Constructs a new namespace-aware DocumentBuilder. */
    public static DocumentBuilder createDocumentBuilder() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Parses the given XML string to produce a Document. */
    public static Document parse(String xml) throws SAXException, IOException {
        // DocumentBuilder is not thread-safe; parsing multiple documents concurrently
        // will cause the error "FWK005 parse may not be called while parsing".  So,
        // we have to construct a new DocumentBuilder every time we parse something.
        return createDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    /** Gets a sequence of descending children by name, creating each if not present. */
    public static Element getOrCreatePath(Document doc, Element element, String... names) {
        for (String name : names) {
            element = getOrCreateChild(doc, element, name);
        }
        return element;
    }

    /* Gets a child of the given element by name, creating it if not present. */
    public static Element getOrCreateChild(Document doc, Element element, String name) {
        for (Element child : getChildren(element)) {
            if (child.getLocalName().equals(name)) {
                return child;
            }
        }
        Element child = doc.createElement(name);
        element.appendChild(child);
        return child;
    }
}
