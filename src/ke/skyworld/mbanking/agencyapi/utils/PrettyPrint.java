package ke.skyworld.mbanking.agencyapi.utils;

import com.google.gson.*;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class PrettyPrint {
    public static String fnPrettifyJson(JsonObject joToParse){
        String strRVal = "";
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        JsonParser parser = new JsonParser();
        JsonElement jsonElement;
        try {
            jsonElement = parser.parse(joToParse.toString());
            strRVal = gson.toJson(joToParse);
        } catch (Exception e){
            System.err.println(PrettyPrint.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } finally {
            gson = null;
            parser = null;
            jsonElement = null;
        }
        return strRVal;
    }

    public static String fnPrettifyXML(String input, int indent) {
        StringWriter stringWriter;
        StreamResult xmlOutput;
        TransformerFactory transformerFactory;
        Transformer transformer;
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            stringWriter = new StringWriter();
            xmlOutput = new StreamResult(stringWriter);
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            System.err.println(PrettyPrint.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } finally {
            stringWriter = null;
            xmlOutput = null;
            transformerFactory = null;
            transformer = null;
        }
        return "";
    }
}
