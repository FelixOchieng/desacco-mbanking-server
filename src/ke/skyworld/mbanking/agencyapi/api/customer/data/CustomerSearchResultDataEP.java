package ke.skyworld.mbanking.agencyapi.api.customer.data;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

public class CustomerSearchResultDataEP {
    public static AgencyAPIResponse fnGetCustomerData(MAPPRequest theMAPPRequest){
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/
            String strUserName=theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            //strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);
            String strSearchCriteria= AgencyAPIUtils.getStringFromNode("SEARCH_OPTION", theMAPPRequest.getMSG());
            String strSearchData=AgencyAPIUtils.getStringFromNode("DATA", theMAPPRequest.getMSG());
            String strAccountType=AgencyAPIUtils.getStringFromNode("SERVICE", theMAPPRequest.getMSG());

            if(strSearchCriteria.equals("MOBILE_NUMBER")){
                strSearchData = APIUtils.sanitizePhoneNumber(strSearchData);
            }

            if (AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("PARAMETERS REQUEST TO CBS");
                System.out.println("--------------------------------------------------");
                System.out.println("UserName: " + strUserName);
                System.out.println("Password: ***********");
                System.out.println("SearchCriteria: "  +strSearchCriteria);
                System.out.println("SearchData: " + strSearchData);
                System.out.println("AccountType: " + strAccountType);
                System.out.println("**************************************************\n\n");
            }

            /*Hardcoded Response - From CBS*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            String strResponse = CBSAgencyAPI.getCustomerSearchResult("GET_CUSTOMER_SEARCH_RESULT",false, strUserName, strPassword, strSearchCriteria, strSearchData, strAccountType);
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

            if(strResponseStatus.equals("SUCCESS")){
                List<Object> lsoAccounts = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts");
                List<Object> lsoLoans = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans");
                List<Object> lsoDisplay = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.display");

                Element elCustomer = dcDocument.createElement("CUSTOMER");
                elData.appendChild(elCustomer);

                String strPersonName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.name");
                Element elPersonName = dcDocument.createElement("NAME");
                elPersonName.setTextContent(strPersonName);
                elCustomer.appendChild(elPersonName);

                String strMobileNumber = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.mobile_number");
                Element elMobileNumber = dcDocument.createElement("MOBILE_NUMBER");
                elMobileNumber.setTextContent(strMobileNumber);
                elCustomer.appendChild(elMobileNumber);

                String strID = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.mobile_number");
                Element elID = dcDocument.createElement("ID_NUMBER");
                elID.setTextContent(strID);
                elCustomer.appendChild(elID);

                String strMemberNumber = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.member_number");
                Element elMemberNumber = dcDocument.createElement("MEMBER_NUMBER");
                elMemberNumber.setTextContent(strMemberNumber);
                elCustomer.appendChild(elMemberNumber);

                Element elCustomerDisplay = dcDocument.createElement("DISPLAY");
                elCustomer.appendChild(elCustomerDisplay);

                for(int count = 0; count < lsoDisplay.size(); count++){
                    Element elDatum = dcDocument.createElement("DATUM");
                    elCustomerDisplay.appendChild(elDatum);

                    String strDatumTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.display["+count+"].title");
                    Element elDatumTitle = dcDocument.createElement("TITLE");
                    elDatumTitle.setTextContent(strDatumTitle);
                    elDatum.appendChild(elDatumTitle);

                    String strDatumValue = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.display["+count+"].value");
                    Element elDatumValue = dcDocument.createElement("VALUE");
                    elDatumValue.setTextContent(strDatumValue);
                    elDatum.appendChild(elDatumValue);

                    String strDatumMask = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.display["+count+"].mask").toString().toUpperCase();
                    Element elDatumMask = dcDocument.createElement("MASK");
                    elDatumMask.setTextContent(strDatumMask);
                    elDatum.appendChild(elDatumMask);

                    String strDatumMaskRegex = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.display["+count+"].mask_regex");
                    Element elDatumMaskRegex = dcDocument.createElement("MASK_REGEX");
                    elDatumMaskRegex.setTextContent(strDatumMaskRegex);
                    elDatum.appendChild(elDatumMaskRegex);
                }

                Element elAccounts = dcDocument.createElement("ACCOUNTS");
                elCustomer.appendChild(elAccounts);

                for(int count = 0; count < lsoAccounts.size(); count++){
                    Element elAccount = dcDocument.createElement("ACCOUNT");
                    elAccounts.appendChild(elAccount);

                    String strAccountName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts["+count+"].name");
                    Element elAccountName = dcDocument.createElement("NAME");
                    elAccountName.setTextContent(strAccountName);
                    elAccount.appendChild(elAccountName);

                    String strAccountNumber = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts["+count+"].number");
                    Element elAccountNumber = dcDocument.createElement("NUMBER");
                    elAccountNumber.setTextContent(strAccountNumber);
                    elAccount.appendChild(elAccountNumber);

                    Element elAccountBalances = dcDocument.createElement("BALANCE");
                    elAccount.appendChild(elAccountBalances);

                    String strAccountBookBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts["+count+"].balances.book_balance").toString();
                    Element elAccountBookBalance = dcDocument.createElement("BOOK_BALANCE");
                    elAccountBookBalance.setTextContent(strAccountBookBalance);
                    elAccountBalances.appendChild(elAccountBookBalance);

                    String strAccountAvailableBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts["+count+"].balances.available_balance").toString();
                    Element elAccountAvailableBalance = dcDocument.createElement("AVAILABLE_BALANCE");
                    elAccountAvailableBalance.setTextContent(strAccountAvailableBalance);
                    elAccountBalances.appendChild(elAccountAvailableBalance);

                    Element elAccountCurrency = dcDocument.createElement("CURRENCY");
                    elAccount.appendChild(elAccountCurrency);

                    String strAccountCurrencyCode = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts["+count+"].currency.code");
                    Element elAccountCurrencyCode = dcDocument.createElement("CODE");
                    elAccountCurrencyCode.setTextContent(strAccountCurrencyCode);
                    elAccountCurrency.appendChild(elAccountCurrencyCode);

                    String strAccountCurrencySymbol = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.accounts["+count+"].currency.symbol");
                    Element elAccountCurrencySymbol = dcDocument.createElement("SYMBOL");
                    elAccountCurrencySymbol.setTextContent(strAccountCurrencySymbol);
                    elAccountCurrency.appendChild(elAccountCurrencySymbol);
                }

                Element elLoans = dcDocument.createElement("LOANS");
                elCustomer.appendChild(elLoans);

                for(int count = 0; count < lsoLoans.size(); count++){
                    Element elLoan = dcDocument.createElement("LOAN");
                    elLoans.appendChild(elLoan);

                    String strLoanName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].name");
                    Element elLoanName = dcDocument.createElement("NAME");
                    elLoanName.setTextContent(strLoanName);
                    elLoan.appendChild(elLoanName);

                    String strLoanNumber = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].number");
                    Element elLoanNumber = dcDocument.createElement("NUMBER");
                    elLoanNumber.setTextContent(strLoanNumber);
                    elLoan.appendChild(elLoanNumber);

                    Element elLoanBalances = dcDocument.createElement("BALANCE");
                    elLoan.appendChild(elLoanBalances);

                    String strLoanBookBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].balances.outstanding_balance").toString();
                    Element elLoanBookBalance = dcDocument.createElement("OUTSTANDING_BALANCE");
                    elLoanBookBalance.setTextContent(strLoanBookBalance);
                    elLoanBalances.appendChild(elLoanBookBalance);

                    String strLoanAvailableBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].balances.outstanding_interest").toString();
                    Element elLoanAvailableBalance = dcDocument.createElement("OUTSTANDING_INTEREST");
                    elLoanAvailableBalance.setTextContent(strLoanAvailableBalance);
                    elLoanBalances.appendChild(elLoanAvailableBalance);

                    String strLoanArrears = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].balances.arrears").toString();
                    Element elLoanArrears = dcDocument.createElement("ARREARS");
                    elLoanArrears.setTextContent(strLoanArrears);
                    elLoanBalances.appendChild(elLoanArrears);

                    Element elLoanCurrency = dcDocument.createElement("CURRENCY");
                    elLoan.appendChild(elLoanCurrency);

                    String strLoanCurrencyCode = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].currency.code");
                    Element elLoanCurrencyCode = dcDocument.createElement("CODE");
                    elLoanCurrencyCode.setTextContent(strLoanCurrencyCode);
                    elLoanCurrency.appendChild(elLoanCurrencyCode);

                    String strLoanCurrencySymbol = JsonPath.parse(obJsonResponse).read("$.response.payload.data.customer.loans["+count+"].currency.symbol");
                    Element elLoanCurrencySymbol = dcDocument.createElement("SYMBOL");
                    elLoanCurrencySymbol.setTextContent(strLoanCurrencySymbol);
                    elLoanCurrency.appendChild(elLoanCurrencySymbol);
                }
            }

            String strResponseXML = XMLUtils.fnTransformXMLDocument(dcDocument);
            strResponseXML = PrettyPrint.fnPrettifyXML(strResponseXML, 4);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("XML RESPONSE GENERATED FROM CBS JSON");
                System.out.println("--------------------------------------------------");
                System.out.println(strResponseXML);
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
            System.err.println(CustomerSearchResultDataEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }

    public static AgencyAPIResponse fnGetEmployeeData(MAPPRequest theMAPPRequest){
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/
            String strUserName=theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            //strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);
            String strSearchCriteria= AgencyAPIUtils.getStringFromNode("SEARCH_OPTION", theMAPPRequest.getMSG());
            String strSearchData=AgencyAPIUtils.getStringFromNode("DATA", theMAPPRequest.getMSG());

            if(strSearchCriteria.equals("MOBILE_NUMBER")){
                strSearchData = APIUtils.sanitizePhoneNumber(strSearchData);
            }

            if (AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("PARAMETERS REQUEST TO CBS");
                System.out.println("--------------------------------------------------");
                System.out.println("UserName: " + strUserName);
                System.out.println("Password: ***********");
                System.out.println("SearchCriteria: "  +strSearchCriteria);
                System.out.println("SearchData: " + strSearchData);
                System.out.println("**************************************************\n\n");
            }

            /*Hardcoded Response - From CBS*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            String strResponse = CBSAgencyAPI.getNewCusotomerDetails("GET_EMPLOYEE_SEARCH_RESULT",false, strUserName, strPassword, strSearchCriteria, strSearchData);
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

            if(strResponseStatus.equals("SUCCESS")){
                List<Object> lsoDisplay = JsonPath.parse(obJsonResponse).read("$.response.payload.data.display");

                Element elEmployee = dcDocument.createElement("EMPLOYEE");
                elData.appendChild(elEmployee);

                String strPersonName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.employee.name");
                Element elPersonName = dcDocument.createElement("NAME");
                elPersonName.setTextContent(strPersonName);
                elEmployee.appendChild(elPersonName);

                String strID = JsonPath.parse(obJsonResponse).read("$.response.payload.data.employee.id_no");
                Element elID = dcDocument.createElement("ID_NUMBER");
                elID.setTextContent(strID);
                elEmployee.appendChild(elID);

                String strServiceID = JsonPath.parse(obJsonResponse).read("$.response.payload.data.employee.service_no");
                Element elServiceID = dcDocument.createElement("SERVICE_NUMBER");
                elServiceID.setTextContent(strServiceID);
                elEmployee.appendChild(elServiceID);

                Element elEmployeeDisplay = dcDocument.createElement("DISPLAY");
                elEmployee.appendChild(elEmployeeDisplay);

                for(int count = 0; count < lsoDisplay.size(); count++){
                    Element elDatum = dcDocument.createElement("DATUM");
                    elEmployeeDisplay.appendChild(elDatum);

                    String strDatumTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.data.display["+count+"].title");
                    Element elDatumTitle = dcDocument.createElement("TITLE");
                    elDatumTitle.setTextContent(strDatumTitle);
                    elDatum.appendChild(elDatumTitle);

                    String strDatumValue = JsonPath.parse(obJsonResponse).read("$.response.payload.data.display["+count+"].value");
                    Element elDatumValue = dcDocument.createElement("VALUE");
                    elDatumValue.setTextContent(strDatumValue);
                    elDatum.appendChild(elDatumValue);

                    String strDatumMask = JsonPath.parse(obJsonResponse).read("$.response.payload.data.display["+count+"].mask").toString().toUpperCase();
                    Element elDatumMask = dcDocument.createElement("MASK");
                    elDatumMask.setTextContent(strDatumMask);
                    elDatum.appendChild(elDatumMask);

                    String strDatumMaskRegex = JsonPath.parse(obJsonResponse).read("$.response.payload.data.display["+count+"].mask_regex");
                    Element elDatumMaskRegex = dcDocument.createElement("MASK_REGEX");
                    elDatumMaskRegex.setTextContent(strDatumMaskRegex);
                    elDatum.appendChild(elDatumMaskRegex);
                }
            }

            Element elAccountTypes = dcDocument.createElement("ACC_TYPES");
            elData.appendChild(elAccountTypes);

            for (int i = 0; i < 2; i++) {
                String strAccountID;
                String strAccountName;
                if(i == 0) {
                    strAccountID = "0";
                    strAccountName = "Normal Account";
                } else {
                    strAccountID = "1";
                    strAccountName = "Sharia Account";
                }
                Element elAccountType = dcDocument.createElement("ACCOUNT");
                elAccountType.setAttribute("ID", strAccountID);
                elAccountType.setAttribute("NAME", strAccountName);
                elAccountTypes.appendChild(elAccountType);
            }

            String strResponseXML = XMLUtils.fnTransformXMLDocument(dcDocument);
            strResponseXML = PrettyPrint.fnPrettifyXML(strResponseXML, 4);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("XML RESPONSE GENERATED FROM CBS JSON");
                System.out.println("--------------------------------------------------");
                System.out.println(strResponseXML);
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
            System.err.println(CustomerSearchResultDataEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }
}
