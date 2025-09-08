package ke.skyworld.mbanking.agencyapi.api.agent.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.mbanking.agencyapi.AgencyAPIConstants;
import ke.skyworld.mbanking.agencyapi.AgencyAPIUtils;
import ke.skyworld.mbanking.agencyapi.models.AgencyAPIResponse;
import ke.skyworld.mbanking.agencyapi.utils.PrettyPrint;
import ke.skyworld.mbanking.agencyapi.utils.XMLUtils;
import ke.skyworld.mbanking.nav.cbs.CBSAgencyAPI;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.math.BigDecimal;

public class AgentFloatDepositEP {
    public static AgencyAPIResponse fnFloatDeposit(MAPPRequest theMAPPRequest) {
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            Node ndMSG = theMAPPRequest.getMSG();

            /*Dynamic Request - Generated Using Passed Parameters*/
            String strUserName =theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);

            String strAgentUsername = theMAPPRequest.getUsername();
            String strAgentPassword = theMAPPRequest.getPassword();
            String strAgentAccountNumber = AgencyAPIUtils.getStringFromNode("AGENT/ACCOUNT_NUMBER", ndMSG);
            String strAgentAccountFrom = AgencyAPIUtils.getStringFromNode("AGENT/ACCOUNT_FROM", ndMSG);
            String strAgentAccountTo = AgencyAPIUtils.getStringFromNode("AGENT/ACCOUNT_TO", ndMSG);

            String strCustomerAccountNumber = AgencyAPIUtils.getStringFromNode("CUSTOMER/ACCOUNT_NUMBER", ndMSG);
            String strCustomerAccountName = AgencyAPIUtils.getStringFromNode("CUSTOMER/ACCOUNT_NAME", ndMSG);
            String strCustomerLoanNumber = AgencyAPIUtils.getStringFromNode("CUSTOMER/LOAN_NUMBER", ndMSG);
            String strCustomerLoanName = AgencyAPIUtils.getStringFromNode("CUSTOMER/LOAN_NAME", ndMSG);
            String strCustomerName = AgencyAPIUtils.getStringFromNode("CUSTOMER/FULL_NAME", ndMSG);
            String strCustomerMemberNumber = AgencyAPIUtils.getStringFromNode("CUSTOMER/MEMBER_NUMBER", ndMSG);
            String strCustomerNationalIDNumber = AgencyAPIUtils.getStringFromNode("CUSTOMER/ID_NUMBER", ndMSG);
            String strCustomerMobileNumber = AgencyAPIUtils.getStringFromNode("CUSTOMER/MOBILE_NUMBER", ndMSG);

            String strTransactionType = AgencyAPIUtils.getStringFromNode("TRANSACTION/TYPE", ndMSG);
            String strTransactionName = AgencyAPIUtils.getStringFromNode("TRANSACTION/NAME", ndMSG);
            String strTransactionAmount = AgencyAPIUtils.getStringFromNode("TRANSACTION/AMOUNT", ndMSG);
            String strTransactionAmountStylized = AgencyAPIUtils.getStringFromNode("TRANSACTION/AMOUNT_STYLIZED", ndMSG);
            String strTransactionStatementCount = AgencyAPIUtils.getStringFromNode("TRANSACTION/STATEMENT_COUNT", ndMSG);
            String strTransactionDescription = AgencyAPIUtils.getStringFromNode("TRANSACTION/DESCRIPTION", ndMSG);
            String strTransactionDate = AgencyAPIUtils.getStringFromNode("TRANSACTION/DATE", ndMSG);
            String strTransactionPrintReceipt = AgencyAPIUtils.getStringFromNode("TRANSACTION/PRINT_RECEIPT", ndMSG);
            String strTransactionSessionID = APIUtils.fnModifyAGNTSessionID(theMAPPRequest);

            String strDeviceProcessorID = AgencyAPIUtils.getStringFromNode("DEVICE/PROCESSOR_ID", ndMSG);
            String strDeviceImei = AgencyAPIUtils.getStringFromNode("DEVICE/IMEI", ndMSG);
            String strDeviceSerialNumber = AgencyAPIUtils.getStringFromNode("DEVICE/SERIAL_NUMBER", ndMSG);

            if(strTransactionStatementCount.equals("")){
                strTransactionStatementCount = "0";
            }

            strTransactionAmount = strTransactionAmount.replaceAll(",", "");

            BigDecimal bdTransactionAmount = BigDecimal.valueOf(Long.parseLong(strTransactionAmount));
            boolean blTransactionPrintReceipt = strTransactionPrintReceipt.equals("TRUE");
            int intTransactionStatementCount = Integer.parseInt(strTransactionStatementCount);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("PARAMETERS REQUEST TO CBS");
                System.out.println("--------------------------------------------------");
                System.out.println("UserName: " + strUserName);
                System.out.println("Password: ***********");
                System.out.println("**************************************************\n\n");
            }

            /*Hardcoded Response - From CBS*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            String strResponse = CBSAgencyAPI.performAgentTransaction(
                    "PERFORM_AGENT_TRANSACTION",
                    false,
                    strAgentUsername,
                    strAgentPassword,
                    strAgentAccountFrom,
                    "WITHDRAWABLE",
                    strAgentAccountTo,
                    strTransactionSessionID,
                    bdTransactionAmount,
                    strTransactionAmountStylized,
                    intTransactionStatementCount,
                    strTransactionDescription,
                    strTransactionDate,
                    blTransactionPrintReceipt,
                    strDeviceImei,
                    strDeviceSerialNumber
                    );
            JsonObject joResponseRoot = new JsonParser().parse(strResponse).getAsJsonObject();
            String strJsonResponse = PrettyPrint.fnPrettifyJson(joResponseRoot);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("JSON RESPONSE FROM CBS");
                System.out.println("--------------------------------------------------");
                System.out.println(strJsonResponse);
                System.out.println("**************************************************\n\n");
            }
            /*Declare Strings from JsonPath */
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            Object obJsonResponse = Configuration.defaultConfiguration().jsonProvider().parse(strJsonResponse);

            String strResponseStatus = JsonPath.parse(obJsonResponse).read("$.response.status");

            /*XML Response - From CBS JSON*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            DocumentBuilderFactory dfDocFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dblDocBuilder = dfDocFactory.newDocumentBuilder();
            Document dcDocument = dblDocBuilder.newDocument();

            Element elResponse = dcDocument.createElement("RESPONSE");
            dcDocument.appendChild(elResponse);

            String strTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.title");
            Element elTitle = dcDocument.createElement("TITLE");
            elTitle.setTextContent(strTitle);
            elResponse.appendChild(elTitle);

            String strMessage = JsonPath.parse(obJsonResponse).read("$.response.payload.message");
            Element elMessage = dcDocument.createElement("MESSAGE");
            elMessage.setTextContent(strMessage);
            elResponse.appendChild(elMessage);


            Element elDisplay = dcDocument.createElement("DISPLAY");
            elDisplay.setTextContent(strMessage);
            elResponse.appendChild(elDisplay);

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
                CBSAgencyAPI.postMpesaTransaction(strTransactionSessionID);
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.ERROR.getValue())){
                arRVal.setCharge("NO");
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.FAILED.getValue())){
                arRVal.setCharge("NO");
                arRVal.setResponseStatus(MAPPConstants.ResponseStatus.FAILED);
            } else {
                arRVal.setCharge("NO");
            }
        } catch (Exception e){
            System.err.println(HomeRefreshEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }
}