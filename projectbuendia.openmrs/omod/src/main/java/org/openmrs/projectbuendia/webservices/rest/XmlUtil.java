package org.openmrs.projectbuendia.webservices.rest;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** XML utility methods. */
public class XmlUtil {

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
}
