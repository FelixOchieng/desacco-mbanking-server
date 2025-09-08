package ke.skyworld.mbanking.agencyapi;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPLocalParameters;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.mapp.MAPPResponse;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.agencyapi.api.agent.authentication.ChangePasswordEP;
import ke.skyworld.mbanking.agencyapi.api.agent.authentication.LoginEP;
import ke.skyworld.mbanking.agencyapi.api.agent.data.AgentAccountsEP;
import ke.skyworld.mbanking.agencyapi.api.agent.data.AgentFloatDepositEP;
import ke.skyworld.mbanking.agencyapi.api.agent.data.HomeRefreshEP;
import ke.skyworld.mbanking.agencyapi.api.customer.data.CustomerSearchOptionsEP;
import ke.skyworld.mbanking.agencyapi.api.customer.data.CustomerSearchResultDataEP;
import ke.skyworld.mbanking.agencyapi.api.customer.services.TransactEP;
import ke.skyworld.mbanking.agencyapi.models.AgencyAPIResponse;
import ke.skyworld.mbanking.mappapi.APIConstants;
import ke.skyworld.mbanking.mappapi.MAPPAPIDB;
import ke.skyworld.mbanking.nav.cbs.CBSAgencyAPI;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseAction.CON;
import static ke.skyworld.mbanking.ussdapi.APIUtils.*;

public class AgencyAPI {
    private static MAPPResponse fnSetMAPPResponse(Node theRepsonseMSG, MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = new MAPPResponse();

        try {
            String strDateTime = MBankingDB.getDBDateTime();
            theMAPPResponse.setMessagesVersion("1.01");
            theMAPPResponse.setMessagesDateTime(strDateTime);
            theMAPPResponse.setSessionID(theMAPPRequest.getSessionID());
            theMAPPResponse.setMAPPType(theMAPPRequest.getMAPPType());

            theMAPPResponse.setMSG(theRepsonseMSG);
            theMAPPResponse.setDateCreated(strDateTime);
            theMAPPResponse.setIntegrityHash("");
        } catch (Exception e) {
            System.err.println(AgencyAPI.class.getSimpleName() + ".fnSetMAPPResponse() ERROR : " + e.getMessage());
        }


        return theMAPPResponse;
    }

    private static void fnGenerateResponseMSGNode(Document doc, Element theElementData, MAPPRequest theMAPPRequest, MAPPConstants.ResponseAction theAction, MAPPConstants.ResponseStatus theStatus, String theCharge, String theTitle, MAPPConstants.ResponsesDataType theDataType) {
        MAPPResponse theMAPPResponse = new MAPPResponse();

        try {
            Element elMSG = doc.createElement("MSG");
            doc.appendChild(elMSG);

            // set attribute SESSION_ID to MSG element
            Attr attrSessionID = doc.createAttribute("SESSION_ID");
            attrSessionID.setValue(Long.toString(theMAPPRequest.getSessionID()));
            elMSG.setAttributeNode(attrSessionID);

            // set attribute TYPE to MSG element
            Attr attrType = doc.createAttribute("TYPE");
            attrType.setValue(theMAPPRequest.getMAPPType().getValue());
            elMSG.setAttributeNode(attrType);

            // set attribute ACTION to MSG element
            Attr attrAction = doc.createAttribute("ACTION");
            attrAction.setValue(theAction.getValue());
            elMSG.setAttributeNode(attrAction);

            // set attribute STATUS to MSG element
            Attr attrStatus = doc.createAttribute("STATUS");
            attrStatus.setValue(theStatus.getValue());
            elMSG.setAttributeNode(attrStatus);

            // set attribute CHARGE to MSG element
            Attr attrCharge = doc.createAttribute("CHARGE");
            attrCharge.setValue(theCharge);
            elMSG.setAttributeNode(attrCharge);

            // set Element TITLE to MSG element
            Element elTitle = doc.createElement("TITLE");
            elTitle.setTextContent(theTitle);
            elMSG.appendChild(elTitle);

            // set Element TYPE to MSG element
            elMSG.appendChild(theElementData);

            // set attribute CHARGE to MSG element
            Attr attrDataType = doc.createAttribute("TYPE");
            attrDataType.setValue(theDataType.getValue());
            theElementData.setAttributeNode(attrDataType);

        } catch (Exception e) {
            System.err.println(AgencyAPI.class.getSimpleName() + ".fnGenerateResponseMSGNode() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public MAPPResponse fnMethodNotFound(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            String strTitle = "Method Not Found";
            String strResponseText = "The submitted method could not be found. Please contact us for support.";
            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.OBJECT;

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            Element elData = doc.createElement("DATA");

            Element elDisplay = doc.createElement("DISPLAY");
            elDisplay.setTextContent(strResponseText);

            elData.appendChild(elDisplay);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, "NO", strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnLogin(MAPPRequest theMAPPRequest, APIConstants.OTP_TYPE theOTPType, boolean blFirstLogin) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strOTPRecipientPhoneNumber = strUsername;
            String strPassword = theMAPPRequest.getPassword();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            if (theOTPType == APIConstants.OTP_TYPE.TRANSACTIONAL || theOTPType == APIConstants.OTP_TYPE.TRANSACTIONAL_WITH_CUSTOMER_OTP) {
                strPassword = configXPath.evaluate("PASSWORD", ndRequestMSG).trim();
            }

            if (theOTPType == APIConstants.OTP_TYPE.TRANSACTIONAL_WITH_CUSTOMER_OTP) {
                strOTPRecipientPhoneNumber = configXPath.evaluate("CUSTOMER_PHONE_NUMBER", ndRequestMSG).trim();
            }

            boolean blOTPVerificationRequired = fnCheckOTPRequirement(theMAPPRequest, APIConstants.OTP_CHECK_STAGE.GENERATION).isEnabled();

            strPassword = APIUtils.hashAgentPIN(strPassword, strUsername);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            AgencyAPIResponse arAPIResponse = LoginEP.fnAgentLogin(theMAPPRequest, theOTPType, blFirstLogin);

            Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAPIResponse.getResponseXML())));

            Element elData = (Element) doc.importNode(innerDoc.getDocumentElement(), true);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, arAPIResponse.getResponseAction(), arAPIResponse.getResponseStatus(), arAPIResponse.getCharge(), arAPIResponse.getTitle(), enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnChangePassword(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            AgencyAPIResponse arAPIResponse = ChangePasswordEP.fnChangePassword(theMAPPRequest);

            Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAPIResponse.getResponseXML())));

            Element elData = (Element) doc.importNode(innerDoc.getDocumentElement(), true);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, arAPIResponse.getResponseAction(), arAPIResponse.getResponseStatus(), arAPIResponse.getCharge(), arAPIResponse.getTitle(), enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public static HashMap<String, String> fnSuspendUserAccess(String theUsername, int theLoginAttemptsCount, String theLoginType, String theName, APIConstants.OTP_TYPE theLoginInstanceType) {

        HashMap<String, String> hmRval = new HashMap<String, String>();
        hmRval.put("MESSAGE", "");
        hmRval.put("ACTION", "");

        try {
            HashMap<String, String> hmMSGPlaceholders = new HashMap<>();

            hmMSGPlaceholders.put("[MOBILE_NUMBER]", theUsername);
            hmMSGPlaceholders.put("[LOGIN_ATTEMPTS]", String.valueOf(theLoginAttemptsCount));
            hmMSGPlaceholders.put("[FIRST_NAME]", theName);

            MBankingConstants.AuthType authType = MBankingConstants.AuthType.PASSWORD;
            if (theLoginType.equalsIgnoreCase("OTP")) {
                authType = MBankingConstants.AuthType.OTP;
            }

            String strAuthenticationParametersXML = MAPPLocalParameters.getClientXMLParameters();

            HashMap<String, HashMap<String, String>> hmMBankingResponse = MBankingXMLFactory.getAuthenticationAttemptsAction(theLoginAttemptsCount, hmMSGPlaceholders, strAuthenticationParametersXML, authType);

            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            XMLGregorianCalendar gcValidity = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
            if (!hmMBankingResponse.isEmpty()) {
                HashMap<String, String> hmCurrentAttempt = hmMBankingResponse.get("CURRENT_ATTEMPT");
                HashMap<String, String> hmNextAttempt = hmMBankingResponse.get("NEXT_ATTEMPT");

                String strUnit = hmCurrentAttempt.get("UNIT") != null ? hmCurrentAttempt.get("UNIT") : "";
                String strAction = hmCurrentAttempt.get("ACTION") != null ? hmCurrentAttempt.get("ACTION") : "WARN";
                String strDuration = hmCurrentAttempt.get("DURATION") != null ? hmCurrentAttempt.get("DURATION") : "";
                String strTagDescription = hmCurrentAttempt.get("NAME") != null ? hmCurrentAttempt.get("NAME") : "";

                int intUnit = Calendar.DAY_OF_MONTH;
                int intDuration = 0;
                if (strDuration != null) {
                    if (!strDuration.equals("")) {
                        intDuration = Integer.parseInt(strDuration);
                    }
                }

                if (strAction != null) {
                    if (strAction.equalsIgnoreCase("SUSPEND")) {
                        if (strUnit.equalsIgnoreCase("SECOND")) {
                            intUnit = Calendar.SECOND;
                        } else if (strUnit.equalsIgnoreCase("MINUTE")) {
                            intUnit = Calendar.MINUTE;
                        } else if (strUnit.equalsIgnoreCase("HOUR")) {
                            intUnit = Calendar.HOUR;
                        } else if (strUnit.equalsIgnoreCase("DAY")) {
                            intUnit = Calendar.DAY_OF_YEAR;
                        } else if (strUnit.equalsIgnoreCase("MONTH")) {
                            intUnit = Calendar.MONTH;
                        } else if (strUnit.equalsIgnoreCase("YEAR")) {
                            intUnit = Calendar.YEAR;
                        }

                        if (strDuration == null) {
                            strDuration = "";
                        }

                        String strTryAgainIn = "Please try again in " + strDuration + " " + strUnit.toLowerCase() + (strDuration.equals("1") ? "" : "s");

                        hmRval.put("MESSAGE", "Sorry, your account has been suspended from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn);
                        hmRval.put("ACTION", "END");
                        //rVal = "Sorry, your account has been suspended from using "+AppConstants.strSACCOName+" mobile banking services.";
                    } else {
                        if (!hmNextAttempt.isEmpty()) {
                            String futureLoginAction = hmNextAttempt.get("ACTION");
                            String futureLoginActionDurationUnit = hmNextAttempt.get("UNIT");
                            String friendlyFutureActionDuration = hmNextAttempt.get("DURATION");
                            if (futureLoginActionDurationUnit != null && friendlyFutureActionDuration != null) {
                                friendlyFutureActionDuration = friendlyFutureActionDuration + " " + (futureLoginActionDurationUnit.toLowerCase() + (friendlyFutureActionDuration.equals("1") ? "" : "s"));
                            }
                            String attemptsRemainingToFutureLoginAction = hmNextAttempt.get("ATTEMPTS_REMAINING");

                            String currentLoginAction = hmCurrentAttempt.get("ACTION");
                            if (currentLoginAction == null) currentLoginAction = "NONE";

                            if (futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")) {
                                hmRval.put("MESSAGE", "Sorry, the " + (theLoginInstanceType == APIConstants.OTP_TYPE.TRANSACTIONAL ? "" : ("username and/or ")) + "password provided is NOT correct.\nYou have " + attemptsRemainingToFutureLoginAction + " more attempt" + (attemptsRemainingToFutureLoginAction.equals("1") ? "" : "s") + " before your mobile banking account is suspended for " + friendlyFutureActionDuration);
                            } else if (futureLoginAction.equals("LOCK") && !currentLoginAction.equals("LOCK")) {
                                hmRval.put("MESSAGE", "Sorry, the " + (theLoginInstanceType == APIConstants.OTP_TYPE.TRANSACTIONAL ? "" : ("username and/or ")) + "password provided is NOT correct.\nYou have " + attemptsRemainingToFutureLoginAction + " more attempt" + (attemptsRemainingToFutureLoginAction.equals("1") ? "" : "s") + " before your mobile banking account is locked.");
                            }
                        }
                    }

                }

                gregorianCalendar.add(intUnit, intDuration);
                gcValidity = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);

                CBSAgencyAPI.updateAuthAttempts("UPDATE_AUTH_ATTEMPTS", false, theUsername, theLoginType, theLoginAttemptsCount, strTagDescription, strAction, gcValidity, false);
            } else {
                CBSAgencyAPI.updateAuthAttempts("UPDATE_AUTH_ATTEMPTS", false, theUsername, theLoginType, theLoginAttemptsCount, "WARNING", "WARN", gcValidity, false);
            }
        } catch (Exception e) {
            System.err.println(AgencyAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return hmRval;
    }

    private static String convertNodeToString(Node node) {
        try {
            StringWriter writer = new StringWriter();

            Transformer trans = TransformerFactory.newInstance().newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.transform(new DOMSource(node), new StreamResult(writer));

            return writer.toString();
        } catch (Exception te) {
            te.printStackTrace();
        }

        return "";
    }


    public static OTP fnCheckOTPRequirement(MAPPRequest theMAPPRequest, APIConstants.OTP_CHECK_STAGE theOtpCheckStage) {
        boolean blRval = false;
        OTP otp = new OTP(0, 0, "", "", false);
        otp.setEnabled(false);

        Node ndRequestMSG;
        XPath configXPath;
        Node ndOTP;
        try {
            ndRequestMSG = theMAPPRequest.getMSG();
            configXPath = XPathFactory.newInstance().newXPath();

            String strOTPID = "";
            int intOTPTTL = 0;
            String strOTPTTL = "";
            int intOTPLength = 0;
            String strOTPLength = "";

            ndOTP = (Node) configXPath.evaluate("OTP", ndRequestMSG, XPathConstants.NODE);
            if (ndOTP != null) {
                strOTPID = configXPath.evaluate("@ID", ndOTP).trim();
                otp.setId(strOTPID);
                strOTPTTL = configXPath.evaluate("@TTL", ndOTP).trim();
                if (strOTPTTL != null && !strOTPTTL.equals("")) {
                    intOTPTTL = Integer.parseInt(strOTPTTL);
                    otp.setTtl(intOTPTTL);
                }
                strOTPLength = configXPath.evaluate("@LENGTH", ndOTP).trim();
                if (strOTPLength != null && !strOTPLength.equals("")) {
                    intOTPLength = Integer.parseInt(strOTPLength);
                    otp.setLength(intOTPLength);
                }
            }

            if (theOtpCheckStage == APIConstants.OTP_CHECK_STAGE.GENERATION) {
                if (ndOTP != null && intOTPTTL != 0 && intOTPLength != 0) {
                    otp.setEnabled(true);
                }
            } else if (theOtpCheckStage == APIConstants.OTP_CHECK_STAGE.VERIFICATION) {
                if (ndOTP != null) {
                    otp.setEnabled(true);
                }
            }
        } catch (Exception e) {
            System.err.println(AgencyAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } finally {
            ndRequestMSG = null;
            configXPath = null;
            ndOTP = null;
        }
        return otp;
    }

    public MAPPResponse fnValidateOTP(MAPPRequest theMAPPRequest, APIConstants.OTP_TYPE theOTPType) {

        MAPPResponse theMAPPResponse = null;

        System.out.println("Validate OTP: "+theOTPType.toString());

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            boolean blAddDataAction = false;

            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strAGNTSessionID = fnModifyAGNTSessionID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();
            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strAppID = theMAPPRequest.getAppID();

            String strPhoneNumber = "";

            try {
                strPhoneNumber = configXPath.evaluate("CUSTOMER/MOBILE_NUMBER", ndRequestMSG).trim();
            } catch (Exception e) {
                try {
                    strPhoneNumber = configXPath.evaluate("PHONE_NUMBER", ndRequestMSG).trim();
                } catch (Exception ignored){
                    strPhoneNumber = "";
                }
            }

            if (strPhoneNumber.equals("")) {
                strPhoneNumber = strUsername;
            }

            strPhoneNumber = APIUtils.sanitizePhoneNumber(strPhoneNumber);

            if(strPhoneNumber.equals("INVALID_MOBILE_NUMBER")){
                AgencyAPIResponse arHomeRefresh = HomeRefreshEP.fnRefresh(theMAPPRequest);

                String strMobileNUmber = MBankingXMLFactory.getXPathValueFromXMLString("/DATA/USER/MOBILE_NUMBER", arHomeRefresh.getResponseXML());

                if(arHomeRefresh.getResponseStatus() == MAPPConstants.ResponseStatus.SUCCESS) {
                    strPhoneNumber = APIUtils.sanitizePhoneNumber(strMobileNUmber);
                }
            }

            String strActivationCode = configXPath.evaluate("OTP", ndRequestMSG).trim();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "";
            String strDescription = "";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            boolean blStartKey = false;
            blStartKey = MAPPAPIDB.fnSelectOTPData(strPhoneNumber, strActivationCode);

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            String strUserLoginAttemptAction = CBSAgencyAPI.getUserLoginAttemptAction("GET_USER_LOGIN_ATTEMPT_EXPIRY", false, strPhoneNumber, "OTP");

            //XMLGregorianCalendar gcExpiryDate = CBSAgencyAPI.getUserLoginAttemptExpiry("GET_USER_LOGIN_ATTEMPT_EXPIRY", false, strPhoneNumber, "OTP");
            //Date dtExpiryDate = gcExpiryDate.toGregorianCalendar().getTime();

            //XMLGregorianCalendar gcExpiryDate = new XMLGregorianCalendar();
            Date dtExpiryDate = new Date();

            boolean blIncorrectOTP = false;

            if (strUserLoginAttemptAction.equalsIgnoreCase("SUSPENDED")) {
                strTitle = "OTP Validation Suspended";

                Date dtNow = new Date();

                long dblDuration = dtExpiryDate.getTime() - (dtNow.getTime() + 60);

                /*String strTryAgainIn = "Please try again in " + APIUtils.millisToLongDHMS(dblDuration);

                strDescription = "Sorry, your account is suspended from validating one time password. " + strTryAgainIn;*/
                strDescription = "Sorry, your account has been suspended from validating one time password.";
                enResponseAction = MAPPConstants.ResponseAction.END;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
            } else {
                if (blStartKey) {
                    String strUserAccountStatus = "SUCCESS";//CBSAgencyAPI.mappSetIMEI(strUsername, strAppID);

                    switch (strUserAccountStatus) {
                        case "SUCCESS": {
                            strTitle = "Activation Successful";
                            strDescription = "Mobile app account activation was successful";
                            if (theOTPType == APIConstants.OTP_TYPE.TRANSACTIONAL) {
                                strTitle = "OTP Validation Successful";
                                strDescription = "Your OTP validation was successful";
                            }
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

                            //CBSAgencyAPI.updateAuthAttempts("UPDATE_AUTH_ATTEMPTS", false, strUsername, "OTP", 0, "", "NONE", gcExpiryDate, true);
                            MAPPAPIDB.fnDeleteOTPData(strPhoneNumber);
                            break;
                        }
                        case "ERROR": {
                            strTitle = "Account Blocked";
                            strDescription = "Your account is blocked, please visit you nearest SACCO branch for assistance.";
                            break;
                        }
                        case "NOT_FOUND": {
                            strTitle = "Account Not Found";
                            strDescription = "An error occurred. Please try again after a few minutes.";
                            break;
                        }
                        default: {
                            strTitle = "Activation Failed";
                            if (theOTPType == APIConstants.OTP_TYPE.TRANSACTIONAL) {
                                strTitle = "OTP Validation Failed";
                            }
                            strDescription = "An error occurred. Please try again after a few minutes.";
                            break;
                        }
                    }
                } else {
                    strTitle = "Incorrect Activation Code";
                    strDescription = "The activation code you entered is either incorrect or has expired. Please confirm the activation code and try again.";

                    if (theOTPType == APIConstants.OTP_TYPE.TRANSACTIONAL) {
                        strTitle = "Incorrect One Time Password";
                        strDescription = "You entered an incorrect/expired One Time Password";
                        //todo: remove this
                        //blAddDataAction = true;
                    }

                    int intUserLoginAttemptsCount = CBSAgencyAPI.getUserLoginAttemptCount("GET_USER_LOGIN_ATTEMPT_COUNT", false, strUsername, "OTP");
                    intUserLoginAttemptsCount = intUserLoginAttemptsCount + 1;
                    String strName = theMAPPRequest.getUsername();

                    HashMap<String, String> hmfnSuspendUserAccess = fnSuspendUserAccess(strUsername, intUserLoginAttemptsCount, "OTP", strName, APIConstants.OTP_TYPE.ACTIVATION);
                    String strResponseMessage = hmfnSuspendUserAccess.get("MESSAGE");
                    String strResponseAction = hmfnSuspendUserAccess.get("ACTION");

                    if (!strResponseMessage.equals("")) {
                        strDescription = strResponseMessage;
                    }

                    if (strResponseAction.equals("END")) {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                    }
                }
            }

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strDescription);

            if (blAddDataAction) {
                elData.setAttribute("ACTION", "REQUEST_OTP");
            }

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public static MAPPResponse fnGenerateOTP(MAPPRequest theMAPPRequest, String strMobileNumber) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(AgencyAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            long lnSessionID = theMAPPRequest.getSessionID();

            String strSessionID = String.valueOf(lnSessionID);
            String strTraceID = theMAPPRequest.getTraceID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            String strPhoneNumber = "";

            try {
                strPhoneNumber = configXPath.evaluate("PHONE_NUMBER", ndRequestMSG).trim();
            } catch (Exception e) {
                strPhoneNumber = "";
            }

            if (strPhoneNumber.equals("")) {
                strPhoneNumber = strMobileNumber;
            }

            System.out.println("Mobile Number Receiving OTP: "+strPhoneNumber);

            strPhoneNumber = APIUtils.sanitizePhoneNumber(strPhoneNumber);

            if (strPhoneNumber.equals("INVALID_MOBILE_NUMBER")) {
                strPhoneNumber = strMobileNumber;
            }

            System.out.println("Mobile Number Receiving OTP: "+strPhoneNumber);

            strPhoneNumber = APIUtils.sanitizePhoneNumber(strPhoneNumber);

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            OTP otp = fnCheckOTPRequirement(theMAPPRequest, APIConstants.OTP_CHECK_STAGE.GENERATION);

            int intOTPTTL = 300;
            int intOTPLength = 6;
            String strOTPID = "";

            if (otp.isEnabled()) {
                intOTPTTL = otp.getTtl();
                intOTPLength = otp.getLength();
                strOTPID = otp.getId();
            }

            String strAppSignature = configXPath.evaluate("APP_SIGNATURE", ndRequestMSG).trim();
            if (strAppSignature == null) {
                strAppSignature = "";
            }

            String strOneTImePIN = Utils.generateRandomString(intOTPLength);

            MAPPAPIDB.fnDeleteOTPData(strPhoneNumber);
            MAPPAPIDB.fnInsertOTPData(strPhoneNumber, strOneTImePIN, intOTPTTL);


            SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
            Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (intOTPTTL * 1000));

            String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
            String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);


            String strMSG = "Dear Member,\n" + strOneTImePIN + " is your One Time Password(OTP) generated at " + strTimeGenerated + ". This OTP is valid up to " + strExpiryDate;

            System.out.println("\n\n\n\n\n\n"+strMSG+"\n\n\n\n\n\n");

            String strCharge = "YES";

            int intMSGSent = fnSendSMS(strPhoneNumber, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "ONE_TIME_PASSWORD", "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);

            String strTitle = "OTP Generated and Sent Successfully";
            String strResponseText = "Your One Time Password was generated and sent successfully.";

            if (intMSGSent <= 0) {
                strTitle = "OTP Generation Failed";
                strResponseText = "There was an error sending your One Time Password. Please try again";
                strCharge = "NO";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            AgencyAPI.fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = AgencyAPI.fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(AgencyAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnGetAgentAccounts(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            Element elData = doc.createElement("DATA");
            String strTitle;
            String strResponseText;
            String strCharge = "YES";
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            boolean blAmountLimits = configXPath.evaluate("AMOUNT_LIMITS", ndRequestMSG).trim().equals("ACTIVE");
            boolean blCustomerSearchOptions = configXPath.evaluate("CUSTOMER_SEARCH_OPTIONS", ndRequestMSG).trim().equals("ACTIVE");

            AgencyAPIResponse arAgentsAccounts = AgentAccountsEP.fnGetAgentAccounts(theMAPPRequest);


            if (arAgentsAccounts.getResponseStatus() == MAPPConstants.ResponseStatus.SUCCESS) {
                strTitle = "Request Successful";

                Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAgentsAccounts.getResponseXML())));

                Node elAgentAccounts = doc.importNode(innerDoc.getDocumentElement(), true);
                elData.appendChild(elAgentAccounts);

                if (blAmountLimits) {
                    Element elWithdrawalLimits = fnGetAmountLimits(APIConstants.AGNT_PARAM_TYPE.CASH_WITHDRAWAL, doc);
                    elData.appendChild(elWithdrawalLimits);
                }

                if (blCustomerSearchOptions) {
                    AgencyAPIResponse arSearchOptions = CustomerSearchOptionsEP.fnGetCustomerSearchOptions(theMAPPRequest);

                    Document searchOptionsDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arSearchOptions.getResponseXML())));

                    Node elSearchOptions = doc.importNode(searchOptionsDoc.getDocumentElement(), true);
                    elData.appendChild(elSearchOptions);
                }
            } else {
                strTitle = "Request Failed";
                strResponseText = "Request Failed. Please try again later";
                strCharge = "NO";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                elData.setTextContent(strResponseText);
            }

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnRefreshHomePage(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            Element elData = doc.createElement("DATA");
            String strTitle;
            String strResponseText;
            String strCharge = "YES";
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            AgencyAPIResponse arAgentsAccounts = HomeRefreshEP.fnRefresh(theMAPPRequest);


            if (arAgentsAccounts.getResponseStatus() == MAPPConstants.ResponseStatus.SUCCESS) {
                strTitle = "Request Successful";

                Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAgentsAccounts.getResponseXML())));

                Node elAgentAccounts = doc.importNode(innerDoc.getDocumentElement(), true);
                elData.appendChild(elAgentAccounts);
            } else {
                strTitle = "Request Failed";
                strResponseText = "Request Failed. Please try again later";
                strCharge = "NO";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                elData.setTextContent(strResponseText);
            }

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnGetCustomerSearchResult(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            Element elData = doc.createElement("DATA");
            String strCharge = "YES";

            boolean blGenerateOTP = configXPath.evaluate("GENERATE_OTP", ndRequestMSG).trim().equals("ACTIVE");

            AgencyAPIResponse arAgentsAccounts = CustomerSearchResultDataEP.fnGetCustomerData(theMAPPRequest);

            Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAgentsAccounts.getResponseXML())));

            Node elCustomerData = doc.importNode(innerDoc.getDocumentElement(), true);
            elData.appendChild(elCustomerData);

            if (arAgentsAccounts.getResponseStatus() == MAPPConstants.ResponseStatus.SUCCESS) {
                if (blGenerateOTP) {
                    String strPhoneNumber = fnGetDataAsStringFromXML(arAgentsAccounts.getResponseXML(), "/MEMBER/PHONE_NUMBER");
                }
            }

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, arAgentsAccounts.getResponseAction(), arAgentsAccounts.getResponseStatus(), strCharge, arAgentsAccounts.getTitle(), enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnGetEmployeeData(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            Element elData = doc.createElement("DATA");
            String strCharge = "YES";

            AgencyAPIResponse arAgentsAccounts = CustomerSearchResultDataEP.fnGetEmployeeData(theMAPPRequest);

            Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAgentsAccounts.getResponseXML())));

            Node elCustomerData = doc.importNode(innerDoc.getDocumentElement(), true);
            elData.appendChild(elCustomerData);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, arAgentsAccounts.getResponseAction(), arAgentsAccounts.getResponseStatus(), strCharge, arAgentsAccounts.getTitle(), enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnGetEmployers(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            Element elData = doc.createElement("DATA");
            String strTitle;
            String strResponseText;
            String strCharge = "YES";
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
            
                String strCBSResponse = "<RESPONSE><DATA><EMPLOYERS><EMPLOYER CODE=\"Chai SACCO\" NAME=\"Chai SACCO\" /><EMPLOYER CODE=\"Ministry of Agriculture\" NAME=\"Ministry of Agriculture\" /><EMPLOYER CODE=\"KFA\" NAME=\"KFA\" /><EMPLOYER CODE=\"Other\" NAME=\"Other\" /></EMPLOYERS><REGIONS><REGION CODE=\"47\" NAME=\"Nairobi City\" /><REGION CODE=\"1\" NAME=\"Mombasa\" /><REGION CODE=\"2\" NAME=\"Kwale\" /><REGION CODE=\"3\" NAME=\"Kilifi\" /><REGION CODE=\"4\" NAME=\"Tana River\" /><REGION CODE=\"5\" NAME=\"Lamu\" /><REGION CODE=\"6\" NAME=\"Taita/Taveta\" /><REGION CODE=\"7\" NAME=\"Garissa\" /><REGION CODE=\"8\" NAME=\"Wajir\" /><REGION CODE=\"9\" NAME=\"Mandera\" /><REGION CODE=\"10\" NAME=\"Marsabit\" /><REGION CODE=\"11\" NAME=\"Isiolo\" /><REGION CODE=\"12\" NAME=\"Meru\" /><REGION CODE=\"13\" NAME=\"Tharaka-Nithi\" /><REGION CODE=\"14\" NAME=\"Embu\" /><REGION CODE=\"15\" NAME=\"Kitui\" /><REGION CODE=\"16\" NAME=\"Machakos\" /><REGION CODE=\"17\" NAME=\"Makueni\" /><REGION CODE=\"18\" NAME=\"Nyandarua\" /><REGION CODE=\"19\" NAME=\"Nyeri\" /><REGION CODE=\"20\" NAME=\"Kirinyaga\" /><REGION CODE=\"21\" NAME=\"Murang'a\" /><REGION CODE=\"22\" NAME=\"Kiambu\" /><REGION CODE=\"23\" NAME=\"Turkana\" /><REGION CODE=\"24\" NAME=\"West Pokot\" /><REGION CODE=\"25\" NAME=\"Samburu\" /><REGION CODE=\"26\" NAME=\"Trans Nzoia\" /><REGION CODE=\"27\" NAME=\"Uasin Gishu\" /><REGION CODE=\"28\" NAME=\"Elgeyo/Marakwet\" /><REGION CODE=\"29\" NAME=\"Nandi\" /><REGION CODE=\"30\" NAME=\"Baringo\" /><REGION CODE=\"31\" NAME=\"Laikipia\" /><REGION CODE=\"32\" NAME=\"Nakuru\" /><REGION CODE=\"33\" NAME=\"Narok\" /><REGION CODE=\"34\" NAME=\"Kajiado\" /><REGION CODE=\"35\" NAME=\"Kericho\" /><REGION CODE=\"36\" NAME=\"Bomet\" /><REGION CODE=\"37\" NAME=\"Kakamega\" /><REGION CODE=\"38\" NAME=\"Vihiga\" /><REGION CODE=\"39\" NAME=\"Bungoma\" /><REGION CODE=\"40\" NAME=\"Busia\" /><REGION CODE=\"41\" NAME=\"Siaya\" /><REGION CODE=\"42\" NAME=\"Kisumu\" /><REGION CODE=\"43\" NAME=\"Homa Bay\" /><REGION CODE=\"44\" NAME=\"Migori\" /><REGION CODE=\"45\" NAME=\"Kisii\" /><REGION CODE=\"46\" NAME=\"Nyamira\" /></REGIONS></DATA></RESPONSE>";//CBSAgencyAPI.getEmployersAndRegions();

            if (!strCBSResponse.equals("ERROR")) {
                strTitle = "Request Successful";

                Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(strCBSResponse)));

                Node elAgentAccounts = doc.importNode(innerDoc.getDocumentElement(), true);
                elData.appendChild(elAgentAccounts);
            } else {
                strTitle = "Request Failed";
                strResponseText = "Request Failed. Please try again later";
                strCharge = "NO";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                elData.setTextContent(strResponseText);
            }

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnFloatDeposit(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element elData = doc.createElement("DATA");
            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            OTP otp = fnCheckOTPRequirement(theMAPPRequest, APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = fnValidateOTP(theMAPPRequest, APIConstants.OTP_TYPE.TRANSACTIONAL_WITH_AGENT_OTP);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                AgencyAPIResponse arAgentsAccounts = AgentFloatDepositEP.fnFloatDeposit(theMAPPRequest);
                Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAgentsAccounts.getResponseXML())));

                Node elCustomerData = doc.importNode(innerDoc.getDocumentElement(), true);
                elData.appendChild(elCustomerData);

                fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, arAgentsAccounts.getResponseAction(), arAgentsAccounts.getResponseStatus(), arAgentsAccounts.getCharge(), arAgentsAccounts.getTitle(), enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
            }
            else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnPerformTransaction(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element elData = doc.createElement("DATA");
            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;


            MAPPResponse mrOTPVerificationMappResponse = null;
            APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            OTP otp = fnCheckOTPRequirement(theMAPPRequest, APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = fnValidateOTP(theMAPPRequest, APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                AgencyAPIResponse arAgentsAccounts = TransactEP.fnPerformTransaction(theMAPPRequest);
                Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(arAgentsAccounts.getResponseXML())));

                Node elCustomerData = doc.importNode(innerDoc.getDocumentElement(), true);
                elData.appendChild(elCustomerData);

                fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, arAgentsAccounts.getResponseAction(), arAgentsAccounts.getResponseStatus(), arAgentsAccounts.getCharge(), arAgentsAccounts.getTitle(), enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnRegisterMember(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashAgentPIN(strPassword, strUsername);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strPhoneNumber = configXPath.evaluate("PHONE_NUMBER", ndRequestMSG).trim();
            String strServiceNumber = configXPath.evaluate("SERVICE_NUMBER", ndRequestMSG).trim();
            String strPostalAddress = configXPath.evaluate("POSTAL_ADDRESS", ndRequestMSG).trim();
            String strEmailAddress = configXPath.evaluate("EMAIL_ADDRESS", ndRequestMSG).trim();
            String strMonthlyContribution = configXPath.evaluate("MONTHLY_CONTRIBUTION", ndRequestMSG).trim();
            String strAccountType = configXPath.evaluate("ACCOUNT_TYPE", ndRequestMSG).trim();

            System.out.println("strPhoneNumber: "+strPhoneNumber);
            System.out.println("strServiceNumber: "+strServiceNumber);
            System.out.println("strPostalAddress: "+strPostalAddress);

            String strSessionID = fnModifyAGNTSessionID(theMAPPRequest);
            String strResponseText = "";

            String strCharge = "NO";

            String strCBSResponse = CBSAgencyAPI.registerVirtualMember(
                    "REGISTER_VIRTUAL_MEMBER",
                    false,
                    strServiceNumber,
                    strPhoneNumber,
                    strSessionID,
                    strUsername,
                    strPostalAddress,
                    new BigDecimal(strMonthlyContribution.replaceAll(",", "")),
                    strEmailAddress,
                    Integer.parseInt(strAccountType)
            );

            System.out.println("\n\n\n\n\n\n\nNAV RESPONSE:\n"+strCBSResponse+"\n\n\n\n\n\n\n");

            /*Declare Strings from JsonPath */
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            Object obJsonResponse = Configuration.defaultConfiguration().jsonProvider().parse(strCBSResponse);
            String strResponseStatus = JsonPath.parse(obJsonResponse).read("$.response.status");

            /*XML Response - From CBS JSON*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            DocumentBuilderFactory dfDocFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dblDocBuilder = dfDocFactory.newDocumentBuilder();
            Document dcDocument = dblDocBuilder.newDocument();

            String strTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.title");
            String strMessage = JsonPath.parse(obJsonResponse).read("$.response.payload.message");

            Element elData = dcDocument.createElement("DATA");

            String strPrint = JsonPath.parse(obJsonResponse).read("$.response.payload.data.print");
            Element elPrint = dcDocument.createElement("PRINT");
            elPrint.setTextContent(strPrint);
            elData.appendChild(elPrint);

            Element elDisplay = dcDocument.createElement("DISPLAY");
            elDisplay.setTextContent(strPrint);
            elData.appendChild(elDisplay);

            Element elEntryNumber = dcDocument.createElement("ENTRY_NUMBER");
            elEntryNumber.setTextContent(strSessionID);
            elData.appendChild(elEntryNumber);

            Element elSms = dcDocument.createElement("SMS");
            elSms.setTextContent("Member Registration");
            elData.appendChild(elSms);

            NodeList nlImages = ((NodeList) configXPath.evaluate("IMAGES/IMAGE", ndRequestMSG, XPathConstants.NODESET));


            for (int i = 0; i < nlImages.getLength(); i++) {
                String strImageName = configXPath.evaluate("NAME", nlImages.item(i)).trim();
                String strImageType = configXPath.evaluate("TYPE", nlImages.item(i)).trim();
                String strCategory = configXPath.evaluate("CATEGORY", nlImages.item(i)).trim();
                String strImageData = configXPath.evaluate("DATA", nlImages.item(i)).trim();

                System.out.println("strImageName: "+strImageName);
                System.out.println("strImageType: "+strImageType);
                System.out.println("strCategory: "+strCategory);
                //System.out.println("strImageData: "+strImageData);

                String strPath = CBSAgencyAPI.getVirtualMemberRegistrationImagesPath();

                switch (strCategory) {
                    case "PHOTOGRAPH": {
                        strPath = strPath+"\\PassportSize";
                        break;
                    }
                    case "MILITARY_ID_CARD": {
                        strPath = strPath+"\\ServiceCard";
                        break;
                    }
                    case "SIGNATURE": {
                        strPath = strPath+"\\Signature";
                        break;
                    }
                }

                System.out.println("strPath: "+strPath);

                boolean blCreateFile = CBSAgencyAPI.fromBase64(strPath, strImageName+"."+strImageType, strImageData);
                System.out.println("Created File "+(blCreateFile ? "SUCCESSFULLY" : "FAILED"));
                if(blCreateFile) {
                    CBSAgencyAPI.updateVirtualMemberRegistration(
                            "UPDATE",
                            strImageName,
                            strImageName+"."+strImageType,
                            strSessionID,
                            strCategory,
                            true
                    );

                    System.out.println("Updated Registration Entry SUCCESSFULLY");
                }
            }

            fnGenerateResponseMSGNode(dcDocument, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = dcDocument.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnUploadImages(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            Element elData = doc.createElement("DATA");

            String strImageName = configXPath.evaluate("IMAGE/NAME", ndRequestMSG).trim();
            String strImageType = configXPath.evaluate("IMAGE/TYPE", ndRequestMSG).trim();
            String strCategory = configXPath.evaluate("IMAGE/CATEGORY", ndRequestMSG).trim();
            String strReferenceId = configXPath.evaluate("IMAGE/REFERENCE_ID", ndRequestMSG).trim();
            String strImageData = configXPath.evaluate("IMAGE/BASE64", ndRequestMSG).trim();

            System.out.println("strImageName: "+strImageName);
            System.out.println("strImageType: "+strImageType);
            System.out.println("strCategory: "+strCategory);
            System.out.println("strReferenceId: "+strReferenceId);
            //System.out.println("strImageData: "+strImageData);


            String strPath = CBSAgencyAPI.getVirtualMemberRegistrationImagesPath();

            System.out.println("strPath: "+strPath);

            if(strPath != "") {
                boolean blCreateFile = CBSAgencyAPI.fromBase64(strPath, strImageName+"."+strImageType, strImageData);
                System.out.println("blCreateFile: "+blCreateFile);
                if(blCreateFile) {
                    CBSAgencyAPI.updateVirtualMemberRegistration(
                        "UPDATE",
                        strImageName,
                        strImageName+"."+strImageType,
                        strReferenceId,
                        strCategory,
                        true
                    );
                }
            }

            String strTitle = "Upload Successful";
            String strResponseText = "Image Upload Successful";
            elData.setTextContent(strResponseText);

            String strCharge = "YES";

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, CON, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnSendCustomerSMS(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            String strTitle = "Method Not Found";
            String strResponseText = "The submitted method could not be found. Please contact us for support.";
            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.OBJECT;

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strSessionID = fnModifyAGNTSessionID(theMAPPRequest);
            String strTraceID = theMAPPRequest.getTraceID();

            String strReceiver = configXPath.evaluate("RECEIVER", theMAPPRequest.getMSG()).trim();
            String strContent = configXPath.evaluate("CONTENT", theMAPPRequest.getMSG()).trim();

            fnSendSMS(strContent, strReceiver, "YES", MSGConstants.MSGMode.EXPRESS, 200, "CUSTOMER_SMS", "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            Element elData = doc.createElement("DATA");

            Element elDisplay = doc.createElement("DISPLAY");
            elDisplay.setTextContent(strResponseText);

            elData.appendChild(elDisplay);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, "NO", strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    Element fnGetAmountLimits(APIConstants.AGNT_PARAM_TYPE theMAPPParamType, Document doc) {
        try {
            String strMin = "100";//getParam(theMAPPParamType).getMinimum();
            String strMax = "100000";//getParam(theMAPPParamType).getMaximum();

            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            return elWithdrawalLimits;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return null;
    }

    public MAPPResponse fnGetAccountStatement(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", theMAPPRequest.getMSG()).trim();
            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim();
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim();
            String strMobileNumber = configXPath.evaluate("MOBILE_NUMBER", theMAPPRequest.getMSG()).trim();

            /*Start of Duration Change*/
            int intMaximumTransactionCount = 100;
            String strMaximumTransactionCount = "";

            try {
                strMaximumTransactionCount = configXPath.evaluate("MAXIMUM_TRANSACTION_COUNT", theMAPPRequest.getMSG()).trim();
            } catch (Exception ignored) {
            }

            if (!strMaximumTransactionCount.equals("")) {
                intMaximumTransactionCount = Integer.parseInt(strMaximumTransactionCount);
            }
            /*End of Duration Change*/

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

            Date dtStartDate = format.parse(strStartDate);
            Date dtEndDate = format.parse(strEndDate);

            GregorianCalendar calStartDate = new GregorianCalendar();
            GregorianCalendar calEndDate = new GregorianCalendar();
            calStartDate.setTime(dtStartDate);
            calEndDate.setTime(dtEndDate);

            XMLGregorianCalendar xmlGregCalStartDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(calStartDate);
            XMLGregorianCalendar xmlGregCalEndDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(calEndDate);

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strAGNTSessionID = fnModifyAGNTSessionID(theMAPPRequest);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strAccountsXML = CBSAgencyAPI.agentMinistatement(fnModifyAGNTSessionID(theMAPPRequest), strAGNTSessionID, xmlGregCalStartDate, xmlGregCalEndDate, strAccountNo, strPassword, strUsername);

            strAccountsXML = strAccountsXML.replaceAll(" & ", " and ");
            strAccountsXML = strAccountsXML.replaceAll("&", "and");

            System.out.println("NAV Returned: " + strAccountsXML);

            String strTitle = "Account Statement";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            Element elData = doc.createElement("DATA");
            String strCharge = "NO";

            if (!strAccountsXML.equals("")) {
                strCharge = "YES";
                InputSource source = new InputSource(new StringReader(strAccountsXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlTransactions = ((NodeList) configXPath.evaluate("Response/Transaction", xmlDocument, XPathConstants.NODESET));

                String strAccountBalanceEnquiryXML = CBSAgencyAPI.getAgentAccounts(
                        "GET_AGENT_ACCOUNTS",
                        false,
                        strUsername,
                        strPassword,
                        "ALL"
                );

                InputSource sourceForBalance = new InputSource(new StringReader(strAccountBalanceEnquiryXML));
                DocumentBuilderFactory builderFactoryForBalance = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForBalance = builderFactoryForBalance.newDocumentBuilder();
                Document xmlDocumentForBalance = builderForBalance.parse(sourceForBalance);

                String strBookBalance = String.valueOf(configXPath.evaluate("Balances/Account/BookBalance", xmlDocumentForBalance, XPathConstants.STRING));
                String strAvailableBalance = String.valueOf(configXPath.evaluate("Balances/Account/AvailableBalance", xmlDocumentForBalance, XPathConstants.STRING));

                Element elBalance = doc.createElement("BALANCE");
                elBalance.setTextContent(strAvailableBalance);
                elData.appendChild(elBalance);

                Element elAccountNo = doc.createElement("ACCOUNTNO");
                elAccountNo.setTextContent("345J987KJ");
                elData.appendChild(elAccountNo);

                Element elAccountName = doc.createElement("NAME");
                elAccountName.setTextContent("Sample Account Name");
                elData.appendChild(elAccountName);

                Element elTable = doc.createElement("TABLE");
                elData.appendChild(elTable);

                Element elTrHeading = doc.createElement("TR");
                elTable.appendChild(elTrHeading);

                Element elThHeading1 = doc.createElement("TH");
                elThHeading1.setTextContent("Description");
                elTrHeading.appendChild(elThHeading1);

                Element elThHeading2 = doc.createElement("TH");
                elThHeading2.setTextContent("Amount");
                elTrHeading.appendChild(elThHeading2);

                Element elThHeading3 = doc.createElement("TH");
                elThHeading3.setTextContent("Date");
                elTrHeading.appendChild(elThHeading3);

                Element elThHeading4 = doc.createElement("TH");
                elThHeading4.setTextContent("Reference");
                elTrHeading.appendChild(elThHeading4);

                Element elThHeading5 = doc.createElement("TH");
                elThHeading5.setTextContent("Running Bal");
                elTrHeading.appendChild(elThHeading5);

                for (int i = nlTransactions.getLength() - 1; i >= 0; i--) {
                    String strDate = configXPath.evaluate("Date", nlTransactions.item(i)).trim();
                    String strDesc = configXPath.evaluate("Desc", nlTransactions.item(i)).trim();
                    String strAmount = configXPath.evaluate("Amount", nlTransactions.item(i)).trim();
                    String strReference = configXPath.evaluate("Reference", nlTransactions.item(i)).trim();
                    String strBalance = configXPath.evaluate("RunningBalance", nlTransactions.item(i)).trim();

                    Element elTrBody = doc.createElement("TR");
                    elTable.appendChild(elTrBody);

                    Element elTDBody1 = doc.createElement("TD");
                    elTDBody1.setTextContent(strDesc);
                    elTrBody.appendChild(elTDBody1);

                    Element elTDBody2 = doc.createElement("TD");
                    elTDBody2.setTextContent("KES " + strAmount);
                    elTrBody.appendChild(elTDBody2);

                    // *** note that it's "yyyy-MM-dd hh:mm:ss" not "yyyy-mm-dd hh:mm:ss"
                    SimpleDateFormat dt = new SimpleDateFormat("MM/dd/yy");
                    Date date = dt.parse(strDate);

                    // *** same for the format String below
                    SimpleDateFormat dt1 = new SimpleDateFormat("EEE, dd MMM yyyy");
                    strDate = dt1.format(date);


                    Element elTDBody3 = doc.createElement("TD");
                    elTDBody3.setTextContent(strDate);
                    elTrBody.appendChild(elTDBody3);

                    Element elTDBody4 = doc.createElement("TD");
                    elTDBody4.setTextContent(strReference);
                    elTrBody.appendChild(elTDBody4);

                    Element elTDBody5 = doc.createElement("TD");
                    elTDBody5.setTextContent("KES " + strBalance);
                    elTrBody.appendChild(elTDBody5);
                }
            } else {
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                strCharge = "NO";
                strTitle = "Error: No Statements Found";
                elData.setTextContent("You do not have any statements within this time period");
            }

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnAgentReports(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashAgentPIN(strPassword, strUsername);

            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim().replaceAll("Z", "");
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim().replaceAll("Z", "");


            System.out.println("strStartDate: "+strStartDate);
            System.out.println("strEndDate: "+strEndDate);

            int intStartYear = Integer.parseInt(strStartDate.split("-")[0]);
            int intEndYear = Integer.parseInt(strEndDate.split("-")[0]);

            int intStartMonth = Integer.parseInt(strStartDate.split("-")[1]);
            int intEndMonth = Integer.parseInt(strEndDate.split("-")[1]);

            int intStartDate = Integer.parseInt(strStartDate.split("-")[2]);
            int intEndDate = Integer.parseInt(strEndDate.split("-")[2]);

            XMLGregorianCalendar xmlGregCalStartDate = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(intStartYear, intStartMonth, intStartDate, +3);
            XMLGregorianCalendar xmlGregCalEndDate = DatatypeFactory.newInstance().newXMLGregorianCalendarDate(intEndYear, intEndMonth, intEndDate, +3);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            //todo:revert this
            String strAgentReportJson = CBSAgencyAPI.getAgentReport("GET_AGENT_REPORT", false, strUsername, strPassword, xmlGregCalStartDate, xmlGregCalEndDate);

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strAgentReportJson);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, CON, MAPPConstants.ResponseStatus.SUCCESS, "YES", "Agent Report", MAPPConstants.ResponsesDataType.TEXT);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse fnAgentReportPrintouts(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashAgentPIN(strPassword, strUsername);

            String strSessionIds = configXPath.evaluate("TRANSACTION_SESSION_IDS", theMAPPRequest.getMSG()).trim();

            LinkedList<String> llsSessionIds = new LinkedList<>(Arrays.asList(strSessionIds.split(",")));

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strAgentReportJson = AgencyAPIDB.fnSelectReceipts(theMAPPRequest.getUsername(), llsSessionIds);

            if(strAgentReportJson != null){
                if(strAgentReportJson.equals("")){
                    strAgentReportJson = "NO_PRINTS";
                }
            } else {
                strAgentReportJson = "NO_PRINTS";
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strAgentReportJson);

            fnGenerateResponseMSGNode(doc, elData, theMAPPRequest, CON, MAPPConstants.ResponseStatus.SUCCESS, "YES", "Agent Report Printout", MAPPConstants.ResponsesDataType.TEXT);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = fnSetMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    Element fnGetDataAsElementFromXML(String theXML, String thePath) {
        try {
            InputSource source = new InputSource(new StringReader(theXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node elData = (Node) configXPath.evaluate(thePath, xmlDocument, XPathConstants.NODE);

            StringWriter buf = new StringWriter();
            Transformer xform = TransformerFactory.newInstance().newTransformer();
            xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); // optional
            xform.setOutputProperty(OutputKeys.INDENT, "yes"); // optional
            xform.transform(new DOMSource(elData), new StreamResult(buf));
            String strData = buf.toString();

            Document innerDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(strData)));
            return innerDoc.getDocumentElement();
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return null;
    }

    String fnGetDataAsStringFromXML(String theXML, String thePath) {
        try {
            InputSource source = new InputSource(new StringReader(theXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            return configXPath.evaluate(thePath, xmlDocument, XPathConstants.STRING).toString();
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return null;
    }

    XMLGregorianCalendar fnGetGregorianDate(String theDate, String theFormat) {
        try {
            DateFormat format = new SimpleDateFormat(theFormat);

            Date dtStartDate = format.parse(theDate);

            GregorianCalendar calStartDate = new GregorianCalendar();
            calStartDate.setTime(dtStartDate);

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calStartDate);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return null;
    }

    XMLGregorianCalendar fnGetCurrentGregorianDate() {
        try {
            Date dtStartDate = new Date();

            GregorianCalendar calStartDate = new GregorianCalendar();
            calStartDate.setTime(dtStartDate);

            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calStartDate);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return null;
    }

    String fnFormatStringToCurrency(String theString) {
        try {
            double dblAmount = Double.parseDouble(theString);
            DecimalFormat dfDateFormatter = new DecimalFormat("#,###.00");

            return dfDateFormatter.format(dblAmount);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return null;
    }

    void fnCreateReceiptOnDatabase(String theAgentID, String theReceiptTransactionType, float theReceiptAmount, String theReceiptPrintout, String theCustomerName, String theCustomerPhoneNumber, String theDateCreated) {
        try {

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
    }
}
