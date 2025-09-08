package ke.skyworld.mbanking.nav.utils;

import ke.skyworld.mbanking.nav.Navision;
import ke.skyworld.mbanking.nav.NavisionAgency;
import ke.skyworld.mbanking.nav.utils.memory.JvmManager;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * sls-api (ke.co.scedar.utils.xml)
 * Created by: elon
 * On: 14 Sep, 2018 9/14/18 6:31 PM
 **/
public class XmlObject {

    private boolean isValid;

    private boolean isNamespaceAware;
    private String xmlString;
    private String oldXmlString;
    private String xsdString;
    private String oldXsdString;
    private String xmlValidationError = "";
    private NamespaceContext nsContext = null;
    private Document xmlDoc;
    private Document oldXmlDoc;

    public XmlObject() {
        this.isNamespaceAware = false;
    }

    public XmlObject(String xmlString) {
        this.isValid = false;
        this.xmlString = xmlString;
        this.oldXmlString = xmlString;
        this.isNamespaceAware = false;
        parseXml();
        this.oldXmlDoc = xmlDoc;
    }

    public XmlObject(String xmlString, boolean isNamespaceAware, NamespaceContext nsContext) {
        this.isValid = false;
        this.xmlString = xmlString;
        this.oldXmlString = xmlString;
        this.isNamespaceAware = isNamespaceAware;
        this.nsContext = nsContext;
        parseXml();
        this.oldXmlDoc = xmlDoc;
    }

    public XmlObject(String xmlString, String xsdString) {
        this.isValid = false;
        this.xmlString = xmlString;
        this.oldXmlString = xmlString;
        this.xsdString = xsdString;
        this.oldXsdString = xsdString;
        this.isNamespaceAware = false;

        this.isValid = validate();
        if (this.isValid) {
            parseXml();
            this.oldXmlDoc = xmlDoc;
        }
    }

    public XmlObject(String xmlString, String xsdString, boolean isNamespaceAware) {
        this.isValid = false;
        this.xmlString = xmlString;
        this.oldXmlString = xmlString;
        this.xsdString = xsdString;
        this.oldXsdString = xsdString;
        this.isNamespaceAware = isNamespaceAware;

        this.isValid = validate();
        if (this.isValid) {
            parseXml();
            this.oldXmlDoc = xmlDoc;
        }
    }

    public void reconstruct(String xmlString) {
        this.isValid = true;
        this.oldXmlString = this.xmlString;
        this.xmlString = xmlString;
        oldXmlDoc = xmlDoc;
        parseXml();
    }

    public void reconstruct(String xmlString, String xsdString) {
        this.isValid = false;
        this.oldXmlString = this.xmlString;
        this.oldXsdString = this.xsdString;
        this.xmlString = xmlString;
        this.xsdString = xsdString;
        oldXmlDoc = xmlDoc;

        this.isValid = validate();

        if (this.isValid) {
            parseXml();
        }
    }

    public void reCreateXmlDoc() {
        this.xmlDoc = null;
        parseXml();
    }

    public boolean isValid() {
        return isValid;
    }

    public String getXmlString() {
        return xmlString;
    }

    public String getOldXmlString() {
        return oldXmlString;
    }

    public String getXsdString() {
        return xsdString;
    }

    public String getOldXsdString() {
        return oldXsdString;
    }

    public String getXmlValidationError() {
        return xmlValidationError;
    }

    public Document getXmlDoc() {
        return xmlDoc;
    }

    public Document getOldXmlDoc() {
        return oldXmlDoc;
    }

    public boolean validate() {
        DocumentBuilderFactory documentBuilderFactory = null;
        DocumentBuilder parser = null;
        InputSource xmlSource = null;
        Document xmlDocument = null;
        Source schemaSource = null;
        Schema schema = null;
        Validator validator = null;
        boolean validXml = false;

        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);

            parser = documentBuilderFactory.newDocumentBuilder();
            parser.setEntityResolver((publicId, systemId) -> new InputSource(xsdString));

            xmlSource = new InputSource(new StringReader(xmlString));
            xmlDocument = parser.parse(xmlSource);

            SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            schemaSource = new StreamSource(new StringReader(xsdString));
            schema = schemaFactory.newSchema(schemaSource);

            validator = schema.newValidator();
            validator.validate(new DOMSource(xmlDocument));
            validXml = true;

            JvmManager.gc(documentBuilderFactory, parser, xmlSource, xmlDocument, schemaSource, schema, validator);

        } catch (Exception e) {
            xmlValidationError = "XML not valid. Error: " + e.getMessage();
            System.err.println("XmlObject.validate(): "+xmlValidationError);
        } finally {
            JvmManager.gc(documentBuilderFactory, parser, xmlSource, xmlDocument, schemaSource, schema, validator);
        }

        return validXml;
    }

    public boolean exists(String path) {
        boolean exists = false;
        if (xmlDoc == null) reCreateXmlDoc();

        XPathExpression xp;
        try {
            xp = XPathFactory.newInstance().newXPath().compile(path);
            NodeList nodes = (NodeList) xp.evaluate(xmlDoc, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                exists = true;
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return exists;
    }

    public static boolean exists(Document xmlDoc, String path) {
        boolean exists = false;
        if (xmlDoc == null) return false;

        XPathExpression xp;
        try {
            xp = XPathFactory.newInstance().newXPath().compile(path);
            NodeList nodes = (NodeList) xp.evaluate(xmlDoc, XPathConstants.NODESET);
            if (nodes != null && nodes.getLength() > 0) {
                exists = true;
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return exists;
    }

    public void update(String path, String value) {
        editXmlTagValue(path, value);
    }

    public void update(String path, String value, Integer... index) {
        path = String.format(path, (Object[]) index);
        editXmlTagValue(path, value);
    }

    public void editXmlTagValue(String tagPath, String value) {

        if (xmlDoc == null) reCreateXmlDoc();

        XPath xpath;

        try {
            xpath = XPathFactory.newInstance().newXPath();
            if(isNamespaceAware) xpath.setNamespaceContext(nsContext);
            NodeList nodeList = (NodeList) xpath.compile(tagPath).evaluate(xmlDoc, XPathConstants.NODESET);

            nodeList.item(0).setTextContent(value);

            updateXmlString();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("XmlObject.editXmlTagValue(): Error. Failed to editing XML object - " + e.getMessage());
        }
    }

    public String read(String path) {
        return getTagValue(path).trim();
    }

    public String getTagValue(String tagPath) {

        if (xmlDoc == null) reCreateXmlDoc();

        XPath xpath;
        XPathExpression xp;
        try {
            xpath = XPathFactory.newInstance().newXPath();
            if(isNamespaceAware) xpath.setNamespaceContext(nsContext);
            xp = xpath.compile(tagPath);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return null;
        }

        try {
            return Objects.requireNonNull(xp).evaluate(xmlDoc);
        } catch (XPathExpressionException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getTagValue(String tagPath, Integer... index) {
        tagPath = String.format(tagPath, (Object[]) index);
        return getTagValue(tagPath);
    }

    public String read(String tagPath, Integer... index) {
        tagPath = String.format(tagPath, (Object[]) index);
        return getTagValue(tagPath);
    }

    public String getXmlContentAsString(String tagPath) {
        return getXmlContentAsString(tagPath, false);
    }

    public String getXmlContentAsString(String tagPath, boolean stripXmlHeader) {
        if (xmlDoc == null) parseXml();
        NodeList nodeList = null;

        try {
            nodeList = (NodeList) XPathFactory.newInstance().newXPath().compile(tagPath)
                    .evaluate(xmlDoc, XPathConstants.NODESET);

        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }

        return format(new DOMSource(Objects.requireNonNull(nodeList).item(0)), stripXmlHeader);
    }

    public String getInnerXmlContentAsString(String tagPath, String parentTag, boolean stripXmlHeader) {
        String xmlContentAsString = getXmlContentAsString(tagPath, stripXmlHeader);
        xmlContentAsString = xmlContentAsString.replaceAll("<" + parentTag + ">", "");
        return xmlContentAsString.replaceAll("</" + parentTag + ">", "").trim();
    }

    public int countTags(String tagPath) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            XPathExpression expr = xpath.compile("count(/" + tagPath + ")");
            Number result = (Number) expr.evaluate(xmlDoc, XPathConstants.NUMBER);

            return result.intValue();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    public int countTags(String tagPath, Integer... index) throws Exception {
        tagPath = String.format(tagPath, (Object[]) index);
        return countTags(tagPath);
    }

    @Deprecated
    public int countInnerTags(String tagPath) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            XPathExpression expr = xpath.compile("count(/" + tagPath + ")");
            Number result = (Number) expr.evaluate(xmlDoc, XPathConstants.NUMBER);

            return result.intValue();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    public void create(String rootElement) throws Exception {
        create(rootElement, null);
    }

    public void create(String rootElement, HashMap<String, String> attributes) throws Exception {

        DocumentBuilderFactory dbFactory = null;
        DocumentBuilder dBuilder = null;

        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            xmlDoc = dBuilder.newDocument();

            Element ROOT = xmlDoc.createElement(rootElement);
            xmlDoc.appendChild(ROOT);

            if (attributes != null && !attributes.isEmpty()) {
                for (String key : attributes.keySet()) {
                    Attr nameAttr = xmlDoc.createAttribute(key);
                    nameAttr.setValue(attributes.get(key));
                    ROOT.setAttributeNode(nameAttr);
                }
            }

            updateXmlString();

            JvmManager.gc(dbFactory, dBuilder);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("XmlObject.create(): ERROR Creating XML Document: " + e.getMessage());
            throw new Exception(e);
        } finally {
            JvmManager.gc(dbFactory, dBuilder);
        }
    }

    public String addElement(String parentElement, String elementName, String value) throws Exception {
        return addElement(parentElement, elementName, value, null);
    }

    public String addElement(String parentElement, String elementName,
                             String value, HashMap<String, String> attributes) throws Exception {
        try {
            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, parentElement);
            if (nodeList.getLength() < 1) {
                throw new Exception("Parent element '" + parentElement + "' not found.");
            }

            Element ELEMENT = xmlDoc.createElement(elementName);
            ELEMENT.setTextContent(value);

            if (attributes != null && !attributes.isEmpty()) {
                for (String key : attributes.keySet()) {
                    Attr nameAttr = xmlDoc.createAttribute(key);
                    nameAttr.setValue(attributes.get(key));
                    ELEMENT.setAttributeNode(nameAttr);
                }
            }

            Element PARENT = (Element) nodeList.item(0);
            PARENT.appendChild(ELEMENT);

            updateXmlString();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Element to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
        return getXmlContentAsString("/", true);
    }

    public static Document addElement(Document xmlDoc, String parentElement, String elementName,
                             String value, HashMap<String, String> attributes) throws Exception {
        try {
            if (xmlDoc == null) return null;

            NodeList nodeList = getElementByName(xmlDoc, parentElement);
            if (nodeList.getLength() < 1) {
                throw new Exception("Parent element '" + parentElement + "' not found.");
            }

            Element ELEMENT = xmlDoc.createElement(elementName);
            ELEMENT.setTextContent(value);

            if (attributes != null && !attributes.isEmpty()) {
                for (String key : attributes.keySet()) {
                    Attr nameAttr = xmlDoc.createAttribute(key);
                    nameAttr.setValue(attributes.get(key));
                    ELEMENT.setAttributeNode(nameAttr);
                }
            }

            Element PARENT = (Element) nodeList.item(0);
            PARENT.appendChild(ELEMENT);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Element to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
        return xmlDoc;
    }

    public String addNodeElement(String parentElement, String elementName, String value) throws Exception {
        return addNodeElement(parentElement, elementName, value, null);
    }

    public String addNodeElement(String parentElement, String elementName,
                                 String value, HashMap<String, String> attributes) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, parentElement);
            if (nodeList.getLength() < 1) {
                throw new Exception("Parent element '" + parentElement + "' not found.");
            }

            Element ELEMENT = xmlDoc.createElement(elementName);

            Document document = ELEMENT.getOwnerDocument();
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Node fragmentNode = documentBuilder.parse(new InputSource(new StringReader(value))).getDocumentElement();
            fragmentNode = document.importNode(fragmentNode, true);
            ELEMENT.appendChild(fragmentNode);

            if (attributes != null && !attributes.isEmpty()) {
                for (String key : attributes.keySet()) {
                    Attr nameAttr = xmlDoc.createAttribute(key);
                    nameAttr.setValue(attributes.get(key));
                    ELEMENT.setAttributeNode(nameAttr);
                }
            }

            Element PARENT = (Element) nodeList.item(0);
            PARENT.appendChild(ELEMENT);
            //xmlDoc.appendChild(ELEMENT);

            updateXmlString();
            JvmManager.gc(document, documentBuilder, fragmentNode);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Element to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
        return getXmlContentAsString("/", true);
    }

    public String addNodeAttributes(String elementName, HashMap<String, String> attributes) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, elementName);
            if (nodeList.getLength() < 1) {
                throw new Exception("Element '" + elementName + "' not found.");
            }

            Element ELEMENT = (Element) nodeList.item(0);
            if (attributes != null && !attributes.isEmpty()) {
                for (String key : attributes.keySet()) {
                    Attr nameAttr = xmlDoc.createAttribute(key);
                    nameAttr.setValue(attributes.get(key));
                    ELEMENT.setAttributeNode(nameAttr);
                }
            }
            //xmlDoc.appendChild(ELEMENT);

            updateXmlString();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Attribute to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
        return getXmlContentAsString("/", true);
    }

    public void addNodeAttributesLite(String elementName, HashMap<String, String> attributes) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, elementName);
            if (nodeList.getLength() < 1) {
                throw new Exception("Element '" + elementName + "' not found.");
            }

            Element ELEMENT = (Element) nodeList.item(0);
            if (attributes != null && !attributes.isEmpty()) {
                for (String key : attributes.keySet()) {
                    Attr nameAttr = xmlDoc.createAttribute(key);
                    nameAttr.setValue(attributes.get(key));
                    ELEMENT.setAttributeNode(nameAttr);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Attribute to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
    }

    public void removeNodeElement(String elementName) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, elementName);
            if (nodeList.getLength() < 1) {
                //throw new Exception("Element '" + elementName + "' not found.");
                return;
            }

            Node nodeToRemove = nodeList.item(0);
            Node parentNode = nodeToRemove.getParentNode();
            if(parentNode != null) parentNode.removeChild(nodeToRemove);

            updateXmlString();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Element to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
    }

    public void removeNodeElementLite(String elementName) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, elementName);
            if (nodeList.getLength() < 1) {
                //throw new Exception("Element '" + elementName + "' not found.");
                return;
            }

            for (int i = 0; i < nodeList.getLength(); i++) {
                if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Node nodeToRemove = nodeList.item(i);
                    Node parentNode = nodeToRemove.getParentNode();
                    if(parentNode != null) parentNode.removeChild(nodeToRemove);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Element to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
    }

    public void removeNodeAttributeLite(String elementName, String attributeName) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, elementName);
            if (nodeList.getLength() < 1) {
                throw new Exception("Element '" + elementName + "' not found.");
                //return;
            }

            for (int i = 0; i < nodeList.getLength(); i++) {
                if(nodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Node nodeWithAttribute = nodeList.item(i);
                    Node parentNode = nodeWithAttribute.getParentNode();
                    if(parentNode != null) {
                        ((Element) nodeWithAttribute).removeAttribute(attributeName);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Removing Attribute from XML Document: " + e.getMessage());
            throw new Exception(e);
        }
    }

    /*public void removeNodeElement(Node node) throws Exception {
        try {

            if (xmlDoc == null) reCreateXmlDoc();

            NodeList nodeList = getElementByName(xmlDoc, elementName);
            if (nodeList.getLength() < 1) {
                //throw new Exception("Element '" + elementName + "' not found.");
                return;
            }

            Node nodeToRemove = nodeList.item(0);
            Node parentNode = nodeToRemove.getParentNode();
            if(parentNode != null) parentNode.removeChild(nodeToRemove);

            updateXmlString();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Adding Element to XML Document: " + e.getMessage());
            throw new Exception(e);
        }
    }*/

    public static NodeList getElementByName(Document xmlDoc, String elementName) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList;

        try {
            nodeList = (NodeList) xPath.compile(elementName).evaluate(xmlDoc, XPathConstants.NODESET);

            JvmManager.gc(xPath);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Getting element from XML Document: " + e.getMessage());
            throw new Exception(e);
        }

        return nodeList;
    }

    public NodeList getSelfElementByName(String elementName) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList;

        try {
            nodeList = (NodeList) xPath.compile(elementName).evaluate(xmlDoc, XPathConstants.NODESET);

            JvmManager.gc(xPath);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR Getting element from XML Document: " + e.getMessage());
            throw new Exception(e);
        }

        return nodeList;
    }

    public void updateXmlString() {
        TransformerFactory transformerFactory = null;
        Transformer transformer = null;
        DOMSource source = null;
        Writer out = null;
        StreamResult streamResult = null;

        try {
            transformerFactory = TransformerFactory.newInstance();
            /*transformer = transformerFactory.newTransformer(
                    new StreamSource(XmlObject.class.getClassLoader().getResourceAsStream(Constants.REMOVE_BLANK_LINES_XSLT)));*/
            InputStream stream = new ByteArrayInputStream(XSLT.getBytes(StandardCharsets.UTF_8));
            transformer = transformerFactory.newTransformer(new StreamSource(stream));
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            source = new DOMSource(xmlDoc);
            out = new StringWriter();
            streamResult = new StreamResult(out);
            transformer.transform(source, streamResult);
            this.xmlString = out.toString();
            JvmManager.gc(transformerFactory, transformer, source, streamResult, out);
        } catch (TransformerException e) {
            System.err.println("XmlObject.updateXmlString(): Error. Failed to format XML - " + e.getMessageAndLocation());
        } finally {
            JvmManager.gc(transformerFactory, transformer, source, streamResult, out);
        }
    }

    public String format(boolean omitXmlDeclaration) {
        return format(new DOMSource(this.xmlDoc), omitXmlDeclaration);
    }

    public String format(DOMSource domSource, boolean omitXmlDeclaration) {
        TransformerFactory transformerFactory = null;
        Transformer transformer = null;
        //DOMSource source = null;
        Writer out = null;
        StreamResult streamResult = null;

        String formattedXml;
        try {
            transformerFactory = TransformerFactory.newInstance();
            /*transformer = transformerFactory.newTransformer(
                    new StreamSource(XmlObject.class.getClassLoader().getResourceAsStream(Constants.REMOVE_BLANK_LINES_XSLT)));*/
            InputStream stream = new ByteArrayInputStream(XSLT.getBytes(StandardCharsets.UTF_8));
            transformer = transformerFactory.newTransformer(new StreamSource(stream));
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (omitXmlDeclaration) ? "yes" : "no");
            //source = new DOMSource(xmlDoc);
            out = new StringWriter();
            streamResult = new StreamResult(out);
            transformer.transform(domSource, streamResult);
            formattedXml = out.toString();
            JvmManager.gc(transformerFactory, transformer, streamResult, out);
            return formattedXml;
        } catch (TransformerException e) {
            System.err.println("XmlObject.format(): Error. Failed to format XML - " + e.getMessageAndLocation());
        } finally {
            JvmManager.gc(transformerFactory, transformer, streamResult, out);
        }
        return "";
    }

    public void save(String filePath, boolean omitXmlDeclaration) throws Exception {
        TransformerFactory transformerFactory = null;
        Transformer transformer = null;
        DOMSource source = null;
        StreamResult streamResult = null;
        try {
            transformerFactory = TransformerFactory.newInstance();
            /*transformer = transformerFactory.newTransformer(
                    new StreamSource(XmlObject.class.getClassLoader().getResourceAsStream(Constants.REMOVE_BLANK_LINES_XSLT)));*/
            InputStream stream = new ByteArrayInputStream(XSLT.getBytes(StandardCharsets.UTF_8));
            transformer = transformerFactory.newTransformer(new StreamSource(stream));
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (omitXmlDeclaration) ? "yes" : "no");
            source = new DOMSource(xmlDoc);
            streamResult = new StreamResult(new File(filePath));
            transformer.transform(source, streamResult);
            JvmManager.gc(transformerFactory, transformer, source, streamResult);
        } catch (TransformerException e) {
            System.err.println("XmlObject.save(): Error. Failed to save XML file (" + filePath + ") - " + e.getMessageAndLocation());
            throw new Exception(e);
        } finally {
            JvmManager.gc(transformerFactory, transformer, source, streamResult);
        }
    }

    private void parseXml() {
        Document doc;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            if(isNamespaceAware) dbFactory.setNamespaceAware(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new InputSource(new StringReader(xmlString)));
            doc.getDocumentElement().normalize();
            xmlDoc = doc;
            isValid = true;
        } catch (Exception e) {
            //e.printStackTrace();
            System.err.println("XmlObject.parseXml(): Error. Failed to parse XML file - " + e.getMessage());
            xmlValidationError = e.getMessage();
        }
    }

    public String toString() {
        return getXmlString();
    }

    public String serialize(boolean omitXmlDeclaration) {
        TransformerFactory transformerFactory = null;
        Transformer transformer = null;
        DOMSource source = null;
        Writer out = null;
        StreamResult streamResult = null;

        String formattedXml;
        try {
            transformerFactory = TransformerFactory.newInstance();
            /*transformer = transformerFactory.newTransformer(
                    new StreamSource(XmlObject.class.getClassLoader().getResourceAsStream(Constants.REMOVE_BLANK_LINES_XSLT)));*/
            InputStream stream = new ByteArrayInputStream(XSLT.getBytes(StandardCharsets.UTF_8));
            transformer = transformerFactory.newTransformer(new StreamSource(stream));
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (omitXmlDeclaration) ? "yes" : "no");
            source = new DOMSource(xmlDoc);
            out = new StringWriter();
            streamResult = new StreamResult(out);
            transformer.transform(source, streamResult);
            formattedXml = out.toString();
            JvmManager.gc(transformerFactory, transformer, source, streamResult, out);
            return formattedXml;
        } catch (TransformerException e) {
            System.err.println("XmlObject.serialize(): Error. Failed to format XML - " + e.getMessageAndLocation());
        } finally {
            JvmManager.gc(transformerFactory, transformer, source, streamResult, out);
        }
        return "";
    }

    //TODO: Parameterize Namespace prefixes and values
    public static NamespaceContext getNamespaceContext(String service){

        NamespaceContext namespaceContext =  new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                if ("x".equals(prefix)) {
                    return "http://schemas.xmlsoap.org/soap/envelope/";
                }
                if ("sky".equals(prefix)) {
                    return Navision.params.getCoreBankingSKYPrefix();
                }
                return null;
            }

            @Override
            public String getPrefix(String namespaceURI) {
                if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceURI)) {
                    return "x";
                }
                if (Navision.params.getCoreBankingSKYPrefix().equals(namespaceURI)) {
                    return "sky";
                }
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                List<String> prefixes = new ArrayList<>();
                if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceURI)) {
                    prefixes.add("x");
                }
                if (Navision.params.getCoreBankingSKYPrefix().equals(namespaceURI)) {
                    prefixes.add("sky");
                }
                return prefixes.iterator();
            }
        };


        switch (service) {

            case AGENCY_SERVICE -> {
                return new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String prefix) {
                        if ("x".equals(prefix)) {
                            return "http://schemas.xmlsoap.org/soap/envelope/";
                        }
                        if ("sky".equals(prefix)) {
                            return NavisionAgency.params.getCoreBankingSKYPrefix();
                        }
                        return null;
                    }

                    @Override
                    public String getPrefix(String namespaceURI) {
                        if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceURI)) {
                            return "x";
                        }
                        if (NavisionAgency.params.getCoreBankingSKYPrefix().equals(namespaceURI)) {
                            return "sky";
                        }
                        return null;
                    }

                    @Override
                    public Iterator<String> getPrefixes(String namespaceURI) {
                        List<String> prefixes = new ArrayList<>();
                        if ("http://schemas.xmlsoap.org/soap/envelope/".equals(namespaceURI)) {
                            prefixes.add("x");
                        }
                        if (NavisionAgency.params.getCoreBankingSKYPrefix().equals(namespaceURI)) {
                            prefixes.add("sky");
                        }
                        return prefixes.iterator();
                    }
                };
            }

            default -> {
                return namespaceContext;
            }
        }
    }


    public static final String MOBILE_SERVICE = "MOBILE";
    public static final String AGENCY_SERVICE = "AGENCY";
    private static final String XSLT = """
            <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
              <xsl:output indent="yes"/>
              <xsl:strip-space elements="*"/>
                        
              <xsl:template match="@*|node()">
                <xsl:copy>
                  <xsl:apply-templates select="@*|node()"/>
                </xsl:copy>
              </xsl:template>
                        
            </xsl:stylesheet>
            """;

}
