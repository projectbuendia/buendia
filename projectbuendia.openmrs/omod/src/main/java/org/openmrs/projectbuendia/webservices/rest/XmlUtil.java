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

/** XML manipulation functions. */
public class XmlUtil {
    private static final DocumentBuilder documentBuilder;

    static {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setIgnoringComments(true);
            documentBuilder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /** Converts a NodeList to an Iterable of Nodes. */
    public static Iterable<Node> toIterable(NodeList nodeList) {
        List<Node> nodes = new ArrayList<>(nodeList.getLength());
        for (int i = 0; i < nodeList.getLength(); i++) {
            nodes.add(nodeList.item(i));
        }
        return nodes;
    }

    /** Converts a NodeList to an Iterable of Elements. */
    public static Iterable<Element> toElementIterable(NodeList nodeList) {
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

    /** Returns all the direct child elements of the given element. */
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
     * Given an element, returns all its direct child elements that have the
     * specified namespace and name.  Use null to indicate the empty namespace.
     */
    public static List<Element> getElementsNS(
            Element element, String namespaceURI, String localName) {
        List<Element> elements = new ArrayList<>();
        for (Element candidate : getElements(element)) {
            if (namespaceURI.equals(candidate.getNamespaceURI())
                    && localName.equals(candidate.getLocalName())) {
                elements.add(candidate);
            }
        }
        return elements;
    }

    /**
     * Given an element, returns its direct child with the empty namespace
     * and the given name, failing unless there is one and only one match.
     */
    public static Element getElementOrThrow(Element element, String name) {
        return getElementOrThrowNS(element, null, name);
    }


    /**
     * Given an element, returns its direct child with the given namespace
     * and name, failing unless there is one and only one match.
     * Use null to indicate the empty namespace.
     */
    public static Element getElementOrThrowNS(
            Element element, String namespaceURI, String localName) {
        NodeList elements = element.getElementsByTagNameNS(namespaceURI, localName);
        if (elements.getLength() != 1) {
            throw new IllegalPropertyException("Element "
                    + element.getNodeName() + " must have exactly one " + localName
                    + " element");
        }
        return (Element) elements.item(0);
    }

    /** Appends a new empty child element with the given namespace and name. */
    public static Element appendElementNS(Element parent, String namespaceURI, String localName) {
        Element ret = parent.getOwnerDocument().createElementNS(namespaceURI, localName);
        parent.appendChild(ret);
        return ret;
    }

    /** Returns a namespace-aware DocumentBuilder. */
    public static DocumentBuilder getDocumentBuilder() {
        return documentBuilder;
    }

    /** Parses the given XML string to produce a Document. */
    public static Document parse(String xml) throws SAXException, IOException {
        return documentBuilder.parse(new InputSource(new StringReader(xml)));
    }
}
