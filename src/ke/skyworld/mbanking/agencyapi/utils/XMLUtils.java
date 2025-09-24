package ke.skyworld.mbanking.agencyapi.utils;

import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class XMLUtils {
    public static String fnTransformXMLDocument(Document xmlDocument) {
        TransformerFactory tfTransformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tfTransformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            tfTransformerFactory = null;
            transformer = null;
        }
    }
}
