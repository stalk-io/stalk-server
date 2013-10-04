package io.stalk.common.oauth.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.InputStream;

/**
 * This is a utility class, which is used to read the xml files.
 *
 * @author Tarun Nagpal
 */
public class XMLParseUtil {

    /**
     * Static helper function to get the element data of the specified node.
     *
     * @param node the node where the text data resides; may be <code>null</code>
     *             in which case this funtion will return ""
     * @return the complete text of the specified node, or an empty string if
     *         the node has no text or is <code>null</code>
     */
    public static String getElementData(final Node node) {
        StringBuffer ret = new StringBuffer();

        if (node != null) {
            Node text;
            for (text = node.getFirstChild(); text != null; text = text
                    .getNextSibling()) {
                /**
                 * the item's value is in one or more text nodes which are its
                 * immediate children
                 */
                if (text.getNodeType() == Node.TEXT_NODE
                        || text.getNodeType() == Node.CDATA_SECTION_NODE) {
                    ret.append(text.getNodeValue());
                } else {
                    if (text.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                        ret.append(getElementData(text));
                    }
                }
            }
        }
        return ret.toString();
    }

    /**
     * Gets the text value of the specified element.
     *
     * @param root        the root of the element whose text is to be retrieved; assumed
     *                    not to be <code>null</code>.
     * @param elementName the name of the element whose text is to be retrieved.
     * @return the retrieved text value of the specified element or null if root
     *         has no given element.
     */
    public static String getElementData(final Element root,
                                        final String elementName) {
        NodeList nodes = root.getElementsByTagName(elementName);
        if (nodes.getLength() < 1) {
            return null;
        }

        return getElementData(nodes.item(0));
    }

    /**
     * Loads the xml file into an xml document and returns the root element.
     *
     * @param fileName the fully qualified name of the XML file to load; assumed not
     *                 to be <code>null</code>.
     * @return root element of the xml document, never <code>null</code>.
     * @throws Exception on any error
     */
    public static Element loadXmlResource(final String fileName)
            throws Exception {
        File file = new File(fileName);
        DocumentBuilder db = getDocumentBuilder();
        Document doc = db.parse(file);
        return doc.getDocumentElement();
    }

    /**
     * Loads the imput stream and returns the root element.
     *
     * @param in Input Stream.
     * @return root element of the xml document, never <code>null</code>.
     * @throws Exception on any error
     */
    public static Element loadXmlResource(final InputStream in)
            throws Exception {
        DocumentBuilder db = getDocumentBuilder();
        Document doc = db.parse(in);
        return doc.getDocumentElement();
    }

    /**
     * Returns a <code>DocumentBuilder</code>, which is used for parsing XML
     * documents.
     *
     * @return a <code>DocumentBuilder</code> which is used for parsing XML
     *         documents. Never <code>null</code>.
     */
    public static DocumentBuilder getDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);

            return dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}
