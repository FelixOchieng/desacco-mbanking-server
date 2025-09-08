package ke.skyworld.mbanking.mbankingapi;

import ke.skyworld.lib.mbanking.mapp.MAPPLocalParameters;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.msg.MSGLocalParameters;
import ke.skyworld.lib.mbanking.msg.MSGProcessor;
import ke.skyworld.mbanking.nav.Navision;
import ke.skyworld.mbanking.nav.NavisionAgency;
import ke.skyworld.mbanking.nav.NavisionUtils;
import ke.skyworld.mbanking.nav.services.cbs.*;
import ke.skyworld.mbanking.nav.services.mbanking.DeleteOTPData;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.mbanking.pesaapi.APIConstants;
import ke.skyworld.lib.mbanking.ussd.USSDLocalParameters;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.UUID;

public class MBankingAPI {
    public void processOnStartup() {
        try {
            /*String integritySecret = PESALocalParameters.getIntegritySecret();
            updateHashes(integritySecret);*/
            //System.exit(0);

            //Initialize Required Configs
            Navision.params = NavisionUtils.getNavisionLocalParameters("navision_conf.xml");
            NavisionAgency.params = NavisionUtils.getNavisionLocalParameters("agency_banking_navision_conf.xml");

            //CBS SERVICES
            /**
             * 1 - MBANKING TRANSACTIONS
             * 2 - LOANS
             * 3 - SEND MEMBER EMAIL
             * 4 - ATM TRANSACTIONS
             * 5 - OTHER
             */
            //MBankingTransactionPoster.start(1, 5);
            //ATMTransactionPoster.start(5, 10);
            //AgencyTransactionPoster.start(10, 30);
            //LoansProcessor.start(30, 60);
            //MemberEmailsProcessor.start(120, 300);
            //OtherProcessor.start(300, 300);

            //HashMBankingPINsProcessor.start(60, 120);
            //HashAgencyPINsProcessor.start(70, 150);

            //MBANKING SERVICES
            DeleteOTPData.start(10, 120);

        } catch (Exception e) {
            System.err.println("MBankingAPI.processOnStartup Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void processOnDBReconnect() {
        try {

        } catch (Exception e) {
            System.err.println("MBankingAPI.processOnDBReconnect Error: " + e.getMessage());
        }
    }

    public static String getValueFromLocalParams(APIConstants.APPLICATION_TYPE theApplicationType, String thePath) {

        String rVal = "";
        try {
            String strConfigXML = "";
            if (theApplicationType == APIConstants.APPLICATION_TYPE.PESA) {
                strConfigXML = PESALocalParameters.getClientXMLParameters();
            } else if (theApplicationType == APIConstants.APPLICATION_TYPE.MSG) {
                strConfigXML = MSGLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == APIConstants.APPLICATION_TYPE.MAPP) {
                strConfigXML = MAPPLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == APIConstants.APPLICATION_TYPE.USSD) {
                strConfigXML = USSDLocalParameters.getClientXMLParameters();
            }

            InputSource source = new InputSource(new StringReader(strConfigXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            rVal = configXPath.evaluate(thePath, xmlDocument, XPathConstants.STRING).toString();
        } catch (Exception e) {
            System.err.println("MBankingAPI.getValueFromLocalParams() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static NodeList getNodeListFromLocalParams(APIConstants.APPLICATION_TYPE theApplicationType, String thePath) {
        NodeList rVal = null;
        try {
            String strConfigXML = "";
            if (theApplicationType == APIConstants.APPLICATION_TYPE.PESA) {
                strConfigXML = PESALocalParameters.getClientXMLParameters();
            } else if (theApplicationType == APIConstants.APPLICATION_TYPE.MSG) {
                strConfigXML = MSGLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == APIConstants.APPLICATION_TYPE.MAPP) {
                strConfigXML = MAPPLocalParameters.getClientXMLParameters();
            } else if (theApplicationType == APIConstants.APPLICATION_TYPE.USSD) {
                strConfigXML = USSDLocalParameters.getClientXMLParameters();
            }

            InputSource source = new InputSource(new StringReader(strConfigXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            rVal = ((NodeList) configXPath.evaluate(thePath, xmlDocument, XPathConstants.NODESET));
        } catch (Exception e) {
            System.err.println("PESADB.getValueFromLocalParams() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static void processSendMSG(String theReceiverType, String theReceiver, String theMSG, String theCategory) {
        try {
            try {
                String strProductID = MBankingAPI.getValueFromLocalParams(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/PRODUCT_ID");
                String strSender = MBankingAPI.getValueFromLocalParams(APIConstants.APPLICATION_TYPE.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/SENDER");
                long lnMSGProductId = Long.parseLong(strProductID);
                String strOriginatorID = UUID.randomUUID().toString();

                int status = MSGProcessor.sendMSG(strOriginatorID, lnMSGProductId, "SENDER_ID", strSender,
                        theReceiverType, theReceiver, "TEXT", theMSG, "BulkSMS", MSGConstants.Sensitivity.NORMAL,
                        theCategory, 210, "YES", MSGConstants.MSGMode.SAF, "USSD", "", "MBANKING_SERVER", "");

                if (status <= 0) {
                    System.err.println("ERROR Sending " + theCategory + " to " + theReceiver + "\n");
                }

            } catch (Exception e) {
                System.out.println("MBankingAPI.processSendMSG() Error message: " + e.getMessage());
            }

        } finally {
        }
    }

    public static void processSendEmail(String theIdentifier, String theMSGSubject, String theMSG, String theCategory) {
        try {
            try {

            } catch (Exception e) {
                System.out.println("MBankingAPI.processSendEmail() Error message: " + e.getMessage());
            }

        } finally {
        }
    }
}
