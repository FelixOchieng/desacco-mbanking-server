package ke.skyworld.mbanking.agencyapi;

import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class AgencyAPIUtils {


    public static String getStringFromNode(String thePath, Node theNode) {
        String strNodeData = "";
        XPath configXPath;
        try {
            configXPath = XPathFactory.newInstance().newXPath();
            strNodeData = configXPath.evaluate(thePath, theNode);
        } catch (Exception e) {
            System.err.println(AgencyAPIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            configXPath = null;
        }
        return strNodeData;
    }



}
