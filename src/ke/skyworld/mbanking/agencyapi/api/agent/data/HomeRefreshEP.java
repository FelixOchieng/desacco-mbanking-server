package ke.skyworld.mbanking.agencyapi.api.agent.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.mbanking.agencyapi.AgencyAPIConstants;
import ke.skyworld.mbanking.agencyapi.AgencyAPIDB;
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

public class HomeRefreshEP {

    public static AgencyAPIResponse fnRefresh(MAPPRequest theMAPPRequest) {
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/
            String strUserName =theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);


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
            String strResponse = CBSAgencyAPI.getAgentData("GET_AGENT_DATA",false,strUserName,strPassword);
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
            List<Object> lsoAccounts = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts");
            List<Object> lsoStatistics = JsonPath.parse(obJsonResponse).read("$.response.payload.data.statistics");

            /*XML Response - From CBS JSON*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            DocumentBuilderFactory dfDocFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dblDocBuilder = dfDocFactory.newDocumentBuilder();
            Document dcDocument = dblDocBuilder.newDocument();

            Element elAllData = dcDocument.createElement("DATA");
            dcDocument.appendChild(elAllData);

            String strTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.title");
            Element elTitle = dcDocument.createElement("TITLE");
            elTitle.setTextContent(strTitle);
            elAllData.appendChild(elTitle);

            String strMessage = JsonPath.parse(obJsonResponse).read("$.response.payload.message");
            Element elMessage = dcDocument.createElement("MESSAGE");
            elMessage.setTextContent(strMessage);
            elAllData.appendChild(elMessage);
            
            
            Element elAgency = dcDocument.createElement("AGENCY");
            elAllData.appendChild(elAgency);

            String strAgencyName  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.agency.name");
            Element elAgencyName = dcDocument.createElement("NAME");
            elAgencyName.setTextContent(strAgencyName);
            elAgency.appendChild(elAgencyName);

            String strAgencyLocation  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.agency.location");
            Element elAgencyLocation = dcDocument.createElement("LOCATION");
            elAgencyLocation.setTextContent(strAgencyLocation);
            elAgency.appendChild(elAgencyLocation);
            
            String strAgencyID  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.agency.id");
            Element elAgencyID = dcDocument.createElement("ID");
            elAgencyID.setTextContent(strAgencyID);
            elAgency.appendChild(elAgencyID);
            
            
            Element elUser = dcDocument.createElement("USER");
            elAllData.appendChild(elUser);
            
            String strUserFullName  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.user.name");
            Element elUserFullName = dcDocument.createElement("FULL_NAME");
            elUserFullName.setTextContent(strUserFullName);
            elUser.appendChild(elUserFullName);

            String strUserUsername  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.user.username");
            Element elUserUsername = dcDocument.createElement("USERNAME");
            elUserUsername.setTextContent(strUserUsername);
            elUser.appendChild(elUserUsername);

            String strUserRole  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.user.role");
            Element elUserRole = dcDocument.createElement("ROLE");
            elUserRole.setTextContent(strUserRole);
            elUser.appendChild(elUserRole);

            boolean blAgentDataExists = AgencyAPIDB.fnCheckIfAgentExists(strUserName);

            if(!blAgentDataExists){
                AgencyAPIDB.fnInsertAgentData(strUserName, "");
            }

            String strUserFingerprint  = AgencyAPIDB.fnSelectAgentData(strUserName, "fingerprint_data");

            String strUserBiometricStatus  =strUserFingerprint.equals("") ? "UNSET" : "SET";
            Element elUserBiometricStatus = dcDocument.createElement("BIOMETRIC_STATUS");
            elUserBiometricStatus.setTextContent(strUserBiometricStatus);
            elUser.appendChild(elUserBiometricStatus);


            Element elUserFingerprint = dcDocument.createElement("FINGERPRINT_DATA");
            elUserFingerprint.setTextContent(strUserFingerprint);
            elUser.appendChild(elUserFingerprint);

            String strUserMobileNumber  = JsonPath.parse(obJsonResponse).read("$.response.payload.data.user.mobile_number");
            Element elUserMobileNumber = dcDocument.createElement("MOBILE_NUMBER");
            elUserMobileNumber.setTextContent(strUserMobileNumber);
            elUser.appendChild(elUserMobileNumber);
            
            Element elAccounts = dcDocument.createElement("ACCOUNTS");
            elAllData.appendChild(elAccounts);

            for(int count = 0; count < lsoAccounts.size(); count++){
                Element elAccount = dcDocument.createElement("ACCOUNT");
                elAccounts.appendChild(elAccount);

                String strAccountName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts["+count+"].name");
                Element elAccountName = dcDocument.createElement("NAME");
                elAccountName.setTextContent(strAccountName);
                elAccount.appendChild(elAccountName);

                String strAccountNumber = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts["+count+"].number");
                Element elAccountNumber = dcDocument.createElement("NUMBER");
                elAccountNumber.setTextContent(strAccountNumber);
                elAccount.appendChild(elAccountNumber);

                Element elAccountBalances = dcDocument.createElement("BALANCE");
                elAccount.appendChild(elAccountBalances);

                String strAccountBookBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts["+count+"].balances.book_balance");
                Element elAccountBookBalance = dcDocument.createElement("BOOK_BALANCE");
                elAccountBookBalance.setTextContent(strAccountBookBalance);
                elAccountBalances.appendChild(elAccountBookBalance);

                String strAccountAvailableBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts["+count+"].balances.available_balance");
                Element elAccountAvailableBalance = dcDocument.createElement("AVAILABLE_BALANCE");
                elAccountAvailableBalance.setTextContent(strAccountAvailableBalance);
                elAccountBalances.appendChild(elAccountAvailableBalance);

                Element elAccountCurrency = dcDocument.createElement("CURRENCY");
                elAccount.appendChild(elAccountCurrency);

                String strAccountCurrencyCode = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts["+count+"].currency.code");
                Element elAccountCurrencyCode = dcDocument.createElement("CODE");
                elAccountCurrencyCode.setTextContent(strAccountCurrencyCode);
                elAccountCurrency.appendChild(elAccountCurrencyCode);

                String strAccountCurrencySymbol = JsonPath.parse(obJsonResponse).read("$.response.payload.data.accounts["+count+"].currency.symbol");
                Element elAccountCurrencySymbol = dcDocument.createElement("SYMBOL");
                elAccountCurrencySymbol.setTextContent(strAccountCurrencySymbol);
                elAccountCurrency.appendChild(elAccountCurrencySymbol);
            }

            Element elStatistics = dcDocument.createElement("DASHBOARD");
            elAllData.appendChild(elStatistics);

            for(int count = 0; count < lsoStatistics.size(); count++){
                Element elStatistic = dcDocument.createElement("STATISTIC");
                elStatistics.appendChild(elStatistic);

                String strStatisticsTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.data.statistics["+count+"].title");
                Element elStatisticsTitle = dcDocument.createElement("TITLE");
                elStatisticsTitle.setTextContent(strStatisticsTitle);
                elStatistic.appendChild(elStatisticsTitle);

                String strStatisticsValue = JsonPath.parse(obJsonResponse).read("$.response.payload.data.statistics["+count+"].value");
                Element elStatisticsValue = dcDocument.createElement("VALUE");
                elStatisticsValue.setTextContent(strStatisticsValue);
                elStatistic.appendChild(elStatisticsValue);

                String strStatisticsType = JsonPath.parse(obJsonResponse).read("$.response.payload.data.statistics["+count+"].type");
                Element elStatisticsType = dcDocument.createElement("TYPE");
                elStatisticsType.setTextContent(strStatisticsType);
                elStatistic.appendChild(elStatisticsType);
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
            arRVal.setMobileNumber(strUserMobileNumber);

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
            System.err.println(HomeRefreshEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }


}
