package ke.skyworld.mbanking.agencyapi.api.customer.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.mbanking.agencyapi.AgencyAPIConstants;
import ke.skyworld.mbanking.agencyapi.api.agent.authentication.LoginEP;
import ke.skyworld.mbanking.agencyapi.models.AgencyAPIResponse;
import ke.skyworld.mbanking.agencyapi.utils.PrettyPrint;
import ke.skyworld.mbanking.agencyapi.utils.XMLUtils;
import ke.skyworld.mbanking.nav.cbs.CBSAgencyAPI;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

public class CustomerSearchOptionsEP {
    public static AgencyAPIResponse fnGetCustomerSearchOptions(MAPPRequest theMAPPRequest ) {
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/
            String strUserName =theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);


            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT){
                System.out.println("**************************************************");
                System.out.println("PARAMETERS - SEARCH OPTIONS REQUEST TO CBS");
                System.out.println("--------------------------------------------------");
                System.out.println("Username: "+strUserName);
                System.out.println("Password: ***********");
                System.out.println("**************************************************\n\n");
            }


            /*Hardcoded Response - From CBS*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            String strResponse = CBSAgencyAPI.getCustomerSearchOptions("GET_CUSTOMER_SEARCH_OPTIONS", false, strUserName, strPassword);
            JsonObject joResponseRoot = new JsonParser().parse(strResponse).getAsJsonObject();
            String strJsonResponse = PrettyPrint.fnPrettifyJson(joResponseRoot);
            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("JSON SEARCH OPTIONS RESPONSE FROM CBS");
                System.out.println("--------------------------------------------------");
                System.out.println(strJsonResponse);
                System.out.println("**************************************************\n\n");
            }

            /*Declare Strings from JsonPath */
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            Object obJsonResponse = Configuration.defaultConfiguration().jsonProvider().parse(strJsonResponse);

            String strResponseStatus = JsonPath.parse(obJsonResponse).read("$.response.status");
            List<Object> lsoOptions = JsonPath.parse(obJsonResponse).read("$.response.payload.data.options");

            /*XML Response - From CBS JSON*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            DocumentBuilderFactory dfDocFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dblDocBuilder = dfDocFactory.newDocumentBuilder();
            Document dcDocument = dblDocBuilder.newDocument();

            Element elData = dcDocument.createElement("DATA");
            dcDocument.appendChild(elData);

            String strTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.title");
            Element elTitle = dcDocument.createElement("TITLE");
            elTitle.setTextContent(strTitle);
            elData.appendChild(elTitle);

            String strMessage = JsonPath.parse(obJsonResponse).read("$.response.payload.message");
            Element elMessage = dcDocument.createElement("MESSAGE");
            elMessage.setTextContent(strMessage);
            elData.appendChild(elMessage);

            Element elOptions = dcDocument.createElement("OPTIONS");
            elData.appendChild(elOptions);

            for(int count = 0; count < lsoOptions.size(); count++) {
                Element elOption = dcDocument.createElement("OPTION");
                elOptions.appendChild(elOption);

                String strId = JsonPath.parse(obJsonResponse).read("$.response.payload.data.options["+count+"].id");
                Element elId = dcDocument.createElement("ID");
                elId.setTextContent(strId);
                elOption.appendChild(elId);

                String strCaption = JsonPath.parse(obJsonResponse).read("$.response.payload.data.options["+count+"].caption");
                Element elCaption = dcDocument.createElement("CAPTION");
                elCaption.setTextContent(strCaption);
                elOption.appendChild(elCaption);
            }

            String strResponseXML = XMLUtils.fnTransformXMLDocument(dcDocument);
            String strPrettyResponseXML = PrettyPrint.fnPrettifyXML(strResponseXML, 4);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("XML RESPONSE GENERATED FROM CBS JSON");
                System.out.println("--------------------------------------------------");
                System.out.println(strPrettyResponseXML);
                System.out.println("**************************************************");
            }

            arRVal.setResponseAction(MAPPConstants.ResponseAction.CON);
            arRVal.setResponseStatus(MAPPConstants.ResponseStatus.ERROR);
            arRVal.setTitle(strTitle);
            arRVal.setResponseText(strMessage);
            arRVal.setResponseXML(strResponseXML);
            if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.SUCCESS.getValue())){
                arRVal.setCharge("YES");
                arRVal.setResponseAction(MAPPConstants.ResponseAction.CON);
                arRVal.setResponseStatus(MAPPConstants.ResponseStatus.SUCCESS);
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.ERROR.getValue())){
                arRVal.setCharge("NO");
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.FAILED.getValue())){
                arRVal.setCharge("NO");
                arRVal.setResponseStatus(MAPPConstants.ResponseStatus.FAILED);
            } else {
                arRVal.setCharge("NO");
            }
        } catch (Exception e){
            System.err.println(LoginEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }


}
