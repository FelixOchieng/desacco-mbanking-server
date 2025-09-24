package ke.skyworld.mbanking.nav.cbs;

import ke.skyworld.mbanking.nav.NavisionAgency;
import ke.skyworld.mbanking.nav.utils.HttpSOAP;
import ke.skyworld.mbanking.nav.utils.LoggingLevel;
import ke.skyworld.mbanking.nav.utils.XmlObject;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import static ke.skyworld.mbanking.nav.utils.XmlObject.getNamespaceContext;

/**
 * mbanking-server-harambee-USSD-v2 (ke.skyworld.mbanking.nav.cbs)
 * Created by: dmutende
 * On: 08 Feb, 2024 20:05
 **/
public class CBSAgencyAPI {

    public static final String CBS_ERROR = "CAGEx0E1";

    public static String agentLogin(String action, boolean indent, String agentUsername, String agentPassword, String deviceIMEI,
                                    String deviceSerialNumber, String deviceMake, String deviceModel, String deviceProcessorID,
                                    String softwareID, String appVersionCode, String appEnvironment) {
        String response = CBS_ERROR;

        String SOAPFunction = "AgentLogin";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AgentLogin>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:deviceIMEI></sky:deviceIMEI>
                            <sky:deviceSerialNumber></sky:deviceSerialNumber>
                            <sky:deviceMake></sky:deviceMake>
                            <sky:deviceModel></sky:deviceModel>
                            <sky:deviceProcessorID></sky:deviceProcessorID>
                            <sky:softwareID></sky:softwareID>
                            <sky:appVersionCode></sky:appVersionCode>
                            <sky:appEnvironment></sky:appEnvironment>
                        </sky:AgentLogin>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceIMEI", deviceIMEI);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceSerialNumber", deviceSerialNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceMake", deviceMake);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceModel", deviceModel);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceProcessorID", deviceProcessorID);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:softwareID", softwareID);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:appVersionCode", appVersionCode);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:appEnvironment", appEnvironment);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String updateAuthAttempts(String action, boolean indent, String username, String type, int count,
                                            String tag, String authAction, XMLGregorianCalendar validity, boolean clearValidity) {
        String response = CBS_ERROR;

        String SOAPFunction = "UpdateAuthAttempts";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:UpdateAuthAttempts>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:username></sky:username>
                            <sky:type></sky:type>
                            <sky:count></sky:count>
                            <sky:tag></sky:tag>
                            <sky:auth_Action></sky:auth_Action>
                            <sky:validity></sky:validity>
                            <sky:clearValidity></sky:clearValidity>
                        </sky:UpdateAuthAttempts>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:username", username);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:type", type);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:count", String.valueOf(count));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:tag", tag);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:auth_Action", authAction);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:validity", validity.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:clearValidity", String.valueOf(clearValidity));

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getUserLoginAttemptAction(String action, boolean indent, String username, String type) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetUserLoginAttemptAction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUserLoginAttemptAction>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:username></sky:username>
                            <sky:type></sky:type>
                        </sky:GetUserLoginAttemptAction>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:username", username);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:type", type);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static XMLGregorianCalendar getUserLoginAttemptExpiry(String action, boolean indent, String username, String type) throws Exception {
        String response = CBS_ERROR;

        String SOAPFunction = "GetUserLoginAttemptAction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUserLoginAttemptAction>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:username></sky:username>
                            <sky:type></sky:type>
                        </sky:GetUserLoginAttemptAction>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:username", username);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:type", type);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date date = dateFormat.parse(response);

            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(date);

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar());
    }

    public static int getUserLoginAttemptCount(String action, boolean indent, String username, String type) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetUserLoginAttemptCount";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUserLoginAttemptCount>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:username></sky:username>
                            <sky:type></sky:type>
                        </sky:GetUserLoginAttemptCount>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:username", username);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:type", type);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Integer.parseInt(response);

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return 0;
    }

    public static String agentMinistatement(String entryCode, String transactionID, XMLGregorianCalendar startDate, XMLGregorianCalendar endDate,
                                         String statementAccount, String pin, String agentUsername) {
        String response = CBS_ERROR;

        String SOAPFunction = "AgentMinistatement";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AgentMinistatement>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:startDate></sky:startDate>
                            <sky:endDate></sky:endDate>
                            <sky:statementAccount></sky:statementAccount>
                            <sky:pin></sky:pin>
                            <sky:agentUsername></sky:agentUsername>
                        </sky:AgentMinistatement>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:startDate", startDate.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:endDate", endDate.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:statementAccount", statementAccount);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:pin", pin);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getAgentAccounts(String action, boolean indent, String agentUsername, String agentPassword, String accountType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAgentAccounts";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAgentAccounts>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:accountType></sky:accountType>
                        </sky:GetAgentAccounts>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:accountType", accountType);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getAgentReport(String action, boolean indent, String agentUsername, String agentPassword,
                                        XMLGregorianCalendar fromDate, XMLGregorianCalendar toDate) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAgentReport";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAgentReport>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:fromDate></sky:fromDate>
                            <sky:toDate></sky:toDate>
                        </sky:GetAgentReport>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:fromDate", fromDate.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:toDate", toDate.toXMLFormat());

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String changeAgentPassword(String action, boolean indent, String agentUsername, String agentOldPassword,
                                             String agentNewPassword) {
        String response = CBS_ERROR;

        String SOAPFunction = "ChangeAgentPassword";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ChangeAgentPassword>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentOldPassword></sky:agentOldPassword>
                            <sky:agentNewPassword></sky:agentNewPassword>
                        </sky:ChangeAgentPassword>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentOldPassword", agentOldPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentNewPassword", agentNewPassword);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getTransactionLimits(String action, String transactionType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetTransactionLimits";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetTransactionLimits>
                            <sky:action></sky:action>
                            <sky:transactionType></sky:transactionType>
                        </sky:GetTransactionLimits>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionType", transactionType);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String performAgentTransaction(String action, boolean indent, String agentUsername, String agentPassword,
                                                 String agentDebitNumber, String debitAccountType, String agentCreditNumber,
                                                 String transactionSessionID, BigDecimal transactionAmount, String transactionAmountStylized,
                                                 int transactionStatementCount, String transactionNarration, String transactionDate,
                                                 boolean transactionPrintReceipt, String deviceIMEI, String deviceSerialNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "PerformAgentTransaction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:PerformAgentTransaction>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:agentDebitNumber></sky:agentDebitNumber>
                            <sky:debitAccountType></sky:debitAccountType>
                            <sky:agentCreditNumber></sky:agentCreditNumber>
                            <sky:transactionSessionID></sky:transactionSessionID>
                            <sky:transactionAmount></sky:transactionAmount>
                            <sky:transactionAmountStylized></sky:transactionAmountStylized>
                            <sky:transactionStatementCount></sky:transactionStatementCount>
                            <sky:transactionNarration></sky:transactionNarration>
                            <sky:transactionDate></sky:transactionDate>
                            <sky:transactionPrintReceipt></sky:transactionPrintReceipt>
                            <sky:deviceIMEI></sky:deviceIMEI>
                            <sky:deviceSerialNumber></sky:deviceSerialNumber>
                        </sky:PerformAgentTransaction>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentDebitNumber", agentDebitNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:debitAccountType", debitAccountType);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentCreditNumber", agentCreditNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionSessionID", transactionSessionID);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionAmount", transactionAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionAmountStylized", transactionAmountStylized);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionStatementCount", String.valueOf(transactionStatementCount));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionNarration", transactionNarration);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionDate", transactionDate);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionPrintReceipt", String.valueOf(transactionPrintReceipt));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceIMEI", deviceIMEI);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceSerialNumber", deviceSerialNumber);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static boolean postMpesaTransaction(String sessionID) {
        String response = CBS_ERROR;

        String SOAPFunction = "PostMpesaTransaction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:PostMpesaTransaction>
                            <sky:sessionID></sky:sessionID>
                        </sky:PostMpesaTransaction>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:sessionID", sessionID);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return false;
    }

    public static String getAgentData(String action, boolean indent, String agentUsername, String agentPassword) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAgentData";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAgentData>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                        </sky:GetAgentData>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getUnhashedPINs() {
        String response = CBS_ERROR;

        String SOAPFunction = "GetUnhashedPINs";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUnhashedPINs></sky:GetUnhashedPINs>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String setHashedPIN(String username, String agentCode, String password) {
        String response = CBS_ERROR;

        String SOAPFunction = "SetHashedPIN";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:SetHashedPIN>
                            <sky:username></sky:username>
                            <sky:agentCode></sky:agentCode>
                            <sky:password></sky:password>
                        </sky:SetHashedPIN>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:username", username);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentCode", agentCode);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:password", password);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getCustomerSearchOptions(String action, boolean indent, String agentUsername, String agentPassword) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetCustomerSearchOptions";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetCustomerSearchOptions>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                        </sky:GetCustomerSearchOptions>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getCustomerSearchResult(String action, boolean indent, String agentUsername, String agentPassword,
                                                 String customerSearchOption, String customerSearchData, String accounttype) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetCustomerSearchResult";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetCustomerSearchResult>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:customerSearchOption></sky:customerSearchOption>
                            <sky:customerSearchData></sky:customerSearchData>
                            <sky:accounttype></sky:accounttype>
                        </sky:GetCustomerSearchResult>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerSearchOption", customerSearchOption);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerSearchData", customerSearchData);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:accounttype", accounttype);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }
    
    public static String performTransaction(String action, boolean indent, String agentUsername, String agentPassword,
                                            String agentAccountNumber, String customerAccountNumber, String customerAccountName,
                                            String customerLoanNumber, String customerLoanName, String customerName,
                                            String customerMemberNumber, String customerNationalIDNumber, String customerMobileNumber,
                                            String transactionType, String transactionName, String transactionSessionID,
                                            BigDecimal transactionAmount, String transactionAmountStylized, int transactionStatementCount,
                                            String transactionNarration, String transactionDate, boolean transactionPrintReceipt,
                                            String deviceIMEI, String deviceSerialNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "PerformTransaction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:PerformTransaction>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:agentAccountNumber></sky:agentAccountNumber>
                            <sky:customerAccountNumber></sky:customerAccountNumber>
                            <sky:customerAccountName></sky:customerAccountName>
                            <sky:customerLoanNumber></sky:customerLoanNumber>
                            <sky:customerLoanName></sky:customerLoanName>
                            <sky:customerName></sky:customerName>
                            <sky:customerMemberNumber></sky:customerMemberNumber>
                            <sky:customerNationalIDNumber></sky:customerNationalIDNumber>
                            <sky:customerMobileNumber></sky:customerMobileNumber>
                            <sky:transactionType></sky:transactionType>
                            <sky:transactionName></sky:transactionName>
                            <sky:transactionSessionID></sky:transactionSessionID>
                            <sky:transactionAmount></sky:transactionAmount>
                            <sky:transactionAmountStylized></sky:transactionAmountStylized>
                            <sky:transactionStatementCount></sky:transactionStatementCount>
                            <sky:transactionNarration></sky:transactionNarration>
                            <sky:transactionDate></sky:transactionDate>
                            <sky:transactionPrintReceipt></sky:transactionPrintReceipt>
                            <sky:deviceIMEI></sky:deviceIMEI>
                            <sky:deviceSerialNumber></sky:deviceSerialNumber>
                        </sky:PerformTransaction>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentAccountNumber", agentAccountNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerAccountNumber", customerAccountNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerAccountName", customerAccountName);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerLoanNumber", customerLoanNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerLoanName", customerLoanName);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerName", customerName);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerMemberNumber", customerMemberNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerNationalIDNumber", customerNationalIDNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerMobileNumber", customerMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionType", transactionType);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionName", transactionName);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionSessionID", transactionSessionID);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionAmount", transactionAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionAmountStylized", transactionAmountStylized);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionStatementCount", String.valueOf(transactionStatementCount));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionNarration", transactionNarration);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionDate", transactionDate);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:transactionPrintReceipt", String.valueOf(transactionPrintReceipt));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceIMEI", deviceIMEI);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:deviceSerialNumber", deviceSerialNumber);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }


    public static String registerVirtualMember(String action, boolean indent, String service_Number, String mobile_Number, String entry_Number,
                                               String agentCode, String postall_Address, BigDecimal monthlyContribution, String email,
                                               int member_Type_Sharia) {
        String response = CBS_ERROR;

        String SOAPFunction = "RegisterVirtualMember";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:RegisterVirtualMember>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:service_Number></sky:service_Number>
                            <sky:mobile_Number></sky:mobile_Number>
                            <sky:entry_Number></sky:entry_Number>
                            <sky:agentCode></sky:agentCode>
                            <sky:postall_Address></sky:postall_Address>
                            <sky:monthlyContribution></sky:monthlyContribution>
                            <sky:email></sky:email>
                            <sky:member_Type_Sharia></sky:member_Type_Sharia>
                        </sky:RegisterVirtualMember>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:service_Number", service_Number);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:mobile_Number", mobile_Number);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:entry_Number", entry_Number);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentCode", agentCode);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:postall_Address", postall_Address);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:monthlyContribution", monthlyContribution.toString());
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:email", email);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:member_Type_Sharia", String.valueOf(member_Type_Sharia));

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getVirtualMemberRegistrationImagesPath() {
        String response = CBS_ERROR;

        String SOAPFunction = "GetVirtualMemberRegistrationImagesPath";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetVirtualMemberRegistrationImagesPath></sky:GetVirtualMemberRegistrationImagesPath>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static boolean fromBase64(String location, String fileName, String base64) {
        String response = CBS_ERROR;

        String SOAPFunction = "FromBase64";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:FromBase64>
                            <sky:location></sky:location>
                            <sky:fileName></sky:fileName>
                            <sky:base64></sky:base64>
                        </sky:FromBase64>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:location", location);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:fileName", fileName);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:base64", base64);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return false;
    }

    public static String updateVirtualMemberRegistration(String action, String image_Entry_Number, String image_Name, String registration_Entry_Number,
                                               String image_Type, boolean indent) {
        String response = CBS_ERROR;

        String SOAPFunction = "UpdateVirtualMemberRegistration";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:UpdateVirtualMemberRegistration>
                            <sky:action></sky:action>
                            <sky:image_Entry_Number></sky:image_Entry_Number>
                            <sky:image_Name></sky:image_Name>
                            <sky:registration_Entry_Number></sky:registration_Entry_Number>
                            <sky:image_Type></sky:image_Type>
                            <sky:indent></sky:indent>
                        </sky:UpdateVirtualMemberRegistration>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:image_Entry_Number", image_Entry_Number);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:image_Name", image_Name);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:registration_Entry_Number", registration_Entry_Number);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:image_Type", image_Type);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    public static String getNewCusotomerDetails(String action, boolean indent, String agentUsername, String agentPassword, String customerSearchOption,
                                                String customerSearchData) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetNewCusotomerDetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetNewCusotomerDetails>
                            <sky:action></sky:action>
                            <sky:indent></sky:indent>
                            <sky:agentUsername></sky:agentUsername>
                            <sky:agentPassword></sky:agentPassword>
                            <sky:customerSearchOption></sky:customerSearchOption>
                            <sky:customerSearchData></sky:customerSearchData>
                        </sky:GetNewCusotomerDetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(NavisionAgency.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.AGENCY_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:indent", String.valueOf(indent));
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentUsername", agentUsername);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:agentPassword", agentPassword);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerSearchOption", customerSearchOption);
            requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:customerSearchData", customerSearchData);

            String strResponseXml = HttpSOAP.sendAgencyRequest(SOAPFunction, requestXml.format(true));
            if(strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/"+SOAPFunction+"_Result/return_value");

            response = replaceChars(response);

            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e){
            System.err.println("CBSAgencyAPI."+SOAPFunction+"() Error making CBS API Request: "+e.getMessage());
        }

        return response;
    }

    private static String replaceChars(String xml) {
        if(xml != null && !xml.isEmpty()){
            xml = xml.replaceAll("&lt;", "<");
            xml = xml.replaceAll("&gt;", ">");
        }
        return xml;
    }

}
