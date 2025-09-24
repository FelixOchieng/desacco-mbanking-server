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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.List;

public class AgentAccountsEP {
    public static AgencyAPIResponse fnGetAgentAccounts(MAPPRequest theMAPPRequest) {
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/
            String strUserName =theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);
            String strAccountType= AgencyAPIUtils.getStringFromNode("ACCOUNT_TYPE",theMAPPRequest.getMSG());
            String strChargeType= AgencyAPIUtils.getStringFromNode("CHARGE_TYPE",theMAPPRequest.getMSG());

            if(strChargeType == null){
                strChargeType = "WITHDRAWAL";
            }
            if(strChargeType.equals("")){
                strChargeType = "WITHDRAWAL";
            }

            if(strAccountType.equals("") && strChargeType.equals("DEPOSIT")){
                strAccountType = "DEPOSIT";
            }

            if(strAccountType.equals("") && strChargeType.equals("LN_REPAYMENT")){
                strChargeType = "LOAN_PAYMENT";
                strAccountType = strChargeType;
            }

            switch (strAccountType) {
                case "WITHDRAWAL": {
                    strAccountType = "CASH_WITHDRAWAL";
                    break;
                }
                case "DEPOSIT": {
                    strAccountType = "CASH_DEPOSIT";
                    break;
                }
                case "FLOAT_DEPOSIT_ACCOUNTS": {
                    strAccountType = "FLOAT_DEP_ACCOUNTS";
                    break;
                }
            }

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("PARAMETERS REQUEST TO CBS");
                System.out.println("--------------------------------------------------");
                System.out.println("UserName: " + strUserName);
                System.out.println("Password: ***********");
                System.out.println("Charge Type: " + strChargeType);
                System.out.println("Account Type: " + strAccountType);
                System.out.println("**************************************************\n\n");
            }

            /*Hardcoded Response - From CBS*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            String strResponse = CBSAgencyAPI.getAgentAccounts("GET_AGENT_ACCOUNTS",false,strUserName,strPassword,strAccountType);
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
            List<Object> lsoTransfers = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer");

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

            Element elTransferAccounts = dcDocument.createElement("TRANSFERS");
            elAllData.appendChild(elTransferAccounts);

            for(int count = 0; count < lsoTransfers.size(); count++){
                Element elAccount = dcDocument.createElement("TRANSFER");
                elTransferAccounts.appendChild(elAccount);

                String strAccountName = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].name");
                Element elAccountName = dcDocument.createElement("NAME");
                elAccountName.setTextContent(strAccountName);
                elAccount.appendChild(elAccountName);

                String strAccountNumber = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].number");
                Element elAccountNumber = dcDocument.createElement("NUMBER");
                elAccountNumber.setTextContent(strAccountNumber);
                elAccount.appendChild(elAccountNumber);

                String strAccountAllowedTransactions = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].allowed_transactions");
                Element elAccountAllowedTransactions = dcDocument.createElement("ALLOWED_TRANSACTIONS");
                elAccountAllowedTransactions.setTextContent(strAccountAllowedTransactions);
                elAccount.appendChild(elAccountAllowedTransactions);

                Element elAccountBalances = dcDocument.createElement("BALANCE");
                elAccount.appendChild(elAccountBalances);

                String strAccountBookBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].balances.book_balance");
                Element elAccountBookBalance = dcDocument.createElement("BOOK_BALANCE");
                elAccountBookBalance.setTextContent(strAccountBookBalance);
                elAccountBalances.appendChild(elAccountBookBalance);

                String strAccountAvailableBalance = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].balances.available_balance");
                Element elAccountAvailableBalance = dcDocument.createElement("AVAILABLE_BALANCE");
                elAccountAvailableBalance.setTextContent(strAccountAvailableBalance);
                elAccountBalances.appendChild(elAccountAvailableBalance);

                Element elAccountCurrency = dcDocument.createElement("CURRENCY");
                elAccount.appendChild(elAccountCurrency);

                String strAccountCurrencyCode = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].currency.code");
                Element elAccountCurrencyCode = dcDocument.createElement("CODE");
                elAccountCurrencyCode.setTextContent(strAccountCurrencyCode);
                elAccountCurrency.appendChild(elAccountCurrencyCode);

                String strAccountCurrencySymbol = JsonPath.parse(obJsonResponse).read("$.response.payload.data.transfer["+count+"].currency.symbol");
                Element elAccountCurrencySymbol = dcDocument.createElement("SYMBOL");
                elAccountCurrencySymbol.setTextContent(strAccountCurrencySymbol);
                elAccountCurrency.appendChild(elAccountCurrencySymbol);
            }



            arRVal.setResponseAction(MAPPConstants.ResponseAction.CON);
            arRVal.setResponseStatus(MAPPConstants.ResponseStatus.ERROR);
            arRVal.setTitle(strTitle);
            arRVal.setResponseText(strMessage);
            if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.SUCCESS.getValue())){
                arRVal.setCharge("YES");
                arRVal.setResponseAction(MAPPConstants.ResponseAction.CON);
                arRVal.setResponseStatus(MAPPConstants.ResponseStatus.SUCCESS);

                String strResponseForCharges = CBSAgencyAPI.getTransactionLimits("GET_TRANSACTION_LIMITS", strAccountType);
                System.out.println("Response For Charges: "+strResponseForCharges);
                JsonObject joResponseRootForCharges = new JsonParser().parse(strResponseForCharges).getAsJsonObject();
                String strJsonResponseForCharges = PrettyPrint.fnPrettifyJson(joResponseRootForCharges);


                if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                    System.out.println("**************************************************");
                    System.out.println("JSON RESPONSE FROM CBS FOR CHARGES");
                    System.out.println("--------------------------------------------------");
                    System.out.println(strJsonResponseForCharges);
                    System.out.println("**************************************************\n\n");

                    Object obJsonResponseForCharge = Configuration.defaultConfiguration().jsonProvider().parse(strJsonResponseForCharges);
                    String strMinimumAmount = "0";
                    String strMaximumAmount = "0";

                    try {
                        strMinimumAmount = JsonPath.parse(obJsonResponseForCharge).read("$.response.payload.data.transaction_limits[0].minimum_amount");
                        strMaximumAmount = JsonPath.parse(obJsonResponseForCharge).read("$.response.payload.data.transaction_limits[0].maximum_amount");
                    } catch (Exception e){
                        e.printStackTrace();
                        strMinimumAmount = "0";
                        strMaximumAmount = "0";
                    }

                    double dblMinimumAmount = Double.parseDouble(strMinimumAmount.replaceAll(",", ""));
                    double dblMaximumAmount = Double.parseDouble(strMaximumAmount.replaceAll(",", ""));

                    Element elTransactionLimits = dcDocument.createElement("TRANSACTION_LIMITS");
                    elAllData.appendChild(elTransactionLimits);

                    Element elMinimum = dcDocument.createElement("MINIMUM_AMOUNT");
                    elMinimum.setTextContent(String.valueOf(dblMinimumAmount));
                    elTransactionLimits.appendChild(elMinimum);

                    Element elMaximum = dcDocument.createElement("MAXIMUM_AMOUNT");
                    elMaximum.setTextContent(String.valueOf(dblMaximumAmount));
                    elTransactionLimits.appendChild(elMaximum);
                }
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.ERROR.getValue())){
                arRVal.setCharge("NO");
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.FAILED.getValue())){
                arRVal.setCharge("NO");
                arRVal.setResponseStatus(MAPPConstants.ResponseStatus.FAILED);
            } else {
                arRVal.setCharge("NO");
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

            arRVal.setResponseXML(strResponseXML);
        } catch (Exception e){
            System.err.println(HomeRefreshEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }
}