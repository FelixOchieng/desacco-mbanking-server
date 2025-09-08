package ke.skyworld.mbanking.agencyapi.api.agent.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.mbanking.agencyapi.models.AgencyAPIResponse;
import ke.skyworld.mbanking.agencyapi.utils.PrettyPrint;
import ke.skyworld.mbanking.agencyapi.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

public class GetEmployersEP {
    public static AgencyAPIResponse fnGetEmployersEP(MAPPRequest theMAPPRequest) {
      AgencyAPIResponse arRval= new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/

          String strUserName = theMAPPRequest.getUsername();
          String strPassWord = theMAPPRequest.getPassword();

            System.out.println("**************************************************");
            System.out.println("JSON REQUEST TO CBS");
            System.out.println("--------------------------------------------------");
            System.out.println("Username: "+strUserName);
            System.out.println("Password: ***********");
            System.out.println("**************************************************\n\n");

            //TODO: Add get Employers Data function from NAV.


            /*Hardcoded Response - From CBS*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            String strResponse = "";//Navision.getAgencyPort().getEmployers
            JsonObject joResponseRoot = new JsonParser().parse(strResponse).getAsJsonObject();
            String strJsonResponse = PrettyPrint.fnPrettifyJson(joResponseRoot);
            System.out.println("**************************************************");
            System.out.println("JSON RESPONSE FROM CBS");
            System.out.println("--------------------------------------------------");
            System.out.println(strJsonResponse);
            System.out.println("**************************************************\n\n");

            /*Declare Strings from JsonPath */
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            Object obJsonResponse = Configuration.defaultConfiguration().jsonProvider().parse(strJsonResponse);

            String strResponseStatus = JsonPath.parse(obJsonResponse).read("$.response.status");
            List<Object> lsEmployerDetails = JsonPath.parse(obJsonResponse).read("$.response.payload.data.employers");

            /*XML Response - From CBS JSON*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            DocumentBuilderFactory dfDocFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dblDocBuilder = dfDocFactory.newDocumentBuilder();
            Document dcDocument = dblDocBuilder.newDocument();

            Element elEmployers = dcDocument.createElement("EMPLOYERS");
            dcDocument.appendChild(elEmployers);

            for (int count = 0; count < lsEmployerDetails.size(); count++) {
                Element elEmployer = dcDocument.createElement("EMPLOYER");
                elEmployers.appendChild(elEmployer);

                String strEmployerName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.employers[" + count + "].id");
                Element elEmployerName = dcDocument.createElement("NAME");
                elEmployerName.setTextContent(strEmployerName);
                elEmployer.appendChild(elEmployerName);

                String strEmployerLocation = JsonPath.parse(obJsonResponse).read("$.response.payload.data.employers[" + count + "].name");
                Element elEmployerLocation = dcDocument.createElement("LOCATION");
                elEmployerLocation.setTextContent(strEmployerLocation);
                elEmployer.appendChild(elEmployerLocation);

            }
            String strResponseXML = XMLUtils.fnTransformXMLDocument(dcDocument);
            strResponseXML = PrettyPrint.fnPrettifyXML(strResponseXML, 4);

            System.out.println("**************************************************");
            System.out.println("XML RESPONSE GENERATED FROM CBS JSON");
            System.out.println("---------------------------------------------------");
            System.out.println(strResponseXML);
            System.out.println("**************************************************");
        } catch (Exception e) {
            System.err.println(AgentAccountsEP.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRval;
    }
}