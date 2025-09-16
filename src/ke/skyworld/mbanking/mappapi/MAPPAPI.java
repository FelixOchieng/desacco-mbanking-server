package ke.skyworld.mbanking.mappapi;

import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPLocalParameters;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.mapp.MAPPResponse;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.pesa.PESA;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.pesa.PESAProcessor;
import ke.skyworld.lib.mbanking.register.MemberRegisterResponse;
import ke.skyworld.lib.mbanking.register.RegisterConstants;
import ke.skyworld.lib.mbanking.register.RegisterProcessor;
import ke.skyworld.lib.mbanking.utils.Crypto;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.pesaapi.APIConstants;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseAction.CON;
import static ke.skyworld.mbanking.ussdapi.APIUtils.*;

public class MAPPAPI {

    boolean blGroupBankingEnabled = false;

    public MAPPConstants.ResponseStatus deactivateMobileApp(long lnMobileNo) {
        MAPPConstants.ResponseStatus rval = MAPPConstants.ResponseStatus.ERROR;
        try {
            String strMobileNumber = String.valueOf(lnMobileNo);

            String strDeactivationStatus = CBSAPI.deactivateMobileApp(strMobileNumber);

            switch (strDeactivationStatus) {
                case "SUCCESS": {
                    rval = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "NOT_FOUND": {
                    rval = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".deactivateMobileApp() ERROR : " + e.getMessage());
        }

        return rval;
    }

    private MAPPResponse setMAPPResponse(Node theRepsonseMSG, MAPPRequest theMAPPRequest) {
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
            System.err.println(this.getClass().getSimpleName() + ".setMAPPResponse() ERROR : " + e.getMessage());
        }


        return theMAPPResponse;
    }

    private void generateResponseMSGNode(Document doc, Element theElementData, MAPPRequest theMAPPRequest, MAPPConstants.ResponseAction theAction, MAPPConstants.ResponseStatus theStatus, String theCharge, String theTitle, MAPPConstants.ResponsesDataType theDataType) {
        MAPPResponse theMAPPResponse = new MAPPResponse();

        try {
            /*
            <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='END' STATUS='FAILED' CHARGE='NO'>
                <TITLE>Login Failed</TITLE>
                <DATA TYPE='TEXT'>INVALID_MOBILE_NUMBER or PIN</DATA>
            </MSG>
             */
            //TEST

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
            System.err.println(this.getClass().getSimpleName() + ".generateResponseMSGNode() ERROR : " + e.getMessage());
        }
    }

    static String splitCamelCase(String s) {
        return s.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        );
    }

    public static String getUserDetails(String strUserPhoneNumber) {
        String strAccountName = "";
        try {
            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(strUserPhoneNumber, "Mobile");

            /*
            <Account>
                <AccountNo>5000000800000</AccountNo>
                <AccountName>ISAAC KIPTOO MULWA</AccountName>
                <Name>ISAAC KIPTOO MULWA</Name>
                <MemberNo>0000800</MemberNo>
                <PhoneNo>+254706405989</PhoneNo>
            </Account>*/

            String strTitle = "Account Details";

            String strCharge = "NO";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                configXPath = XPathFactory.newInstance().newXPath();

                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";
            }
        } catch (Exception e) {
            System.out.println(MAPPAPI.class.getSimpleName() + ".getUserDetails() ERROR: ");
        }
        return strAccountName;
    }

    public MAPPResponse userLogin(MAPPRequest theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE theOTPType) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strVersion = theMAPPRequest.getVersion();
            String strMessagesVersion = theMAPPRequest.getMessagesVersion();
            String strAppID = theMAPPRequest.getAppID();

            //System.out.println(strAppID);

            String strSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strNotificationID = configXPath.evaluate("NOTIFICATION_ID", ndRequestMSG).trim();
            if (theOTPType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL) {
                strPassword = configXPath.evaluate("PASSWORD", ndRequestMSG).trim();
            }

            boolean blOTPVerificationRequired = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.GENERATION).isEnabled();

            strPassword = APIUtils.hashPIN(strPassword, strUsername);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Mobile Banking";
            String strDescription = "Welcome to Mobile Banking. Please visit your nearest branch to activate your account for mobile banking.";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            boolean isUSSD = false;
            boolean blLoginSuccessful = false;
            String strCharge = "NO";
            Element elData = doc.createElement("DATA");

            String strCheckStatus = CBSAPI.userCheck(strUsername, strAppID, false, strSessionID);

            if ((!strCheckStatus.equals("BLOCKED")) && (!strCheckStatus.equals("ERROR"))) {
                String strNavResponse = CBSAPI.ussdLogin(strUsername, strPassword, strAppID, isUSSD, strSessionID);

                System.out.println("MOBILE NUMBER: " + strUsername);
                System.out.println("NAV LOGIN STATUS: " + strNavResponse);
                String strLoginStatus = strNavResponse.split(":::")[0];

                switch (strLoginStatus) {
                    case "SUCCESS": {
                        strTitle = "Login Successful";
                        strDescription = "The login was successful";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        blLoginSuccessful = true;

                        if (blOTPVerificationRequired) {
//                            String strOTPMode = strNavResponse.split(":::")[1];
//
//                            if(strOTPMode.equals("EMAIL")) {
//                                generateOTP(theMAPPRequest);
//                                if(strNavResponse.split(":::").length > 2){
//                                    String strOTPEMailAddress = strNavResponse.split(":::")[2];
//                                    if(!Objects.equals(strOTPEMailAddress, "")){
//                                        generateEmailOTP(strOTPEMailAddress, strUsername, strSessionID);
//                                    }
//                                }
//                            } else {
//                                generateOTP(theMAPPRequest);
//                            }

                            generateOTP(theMAPPRequest);
                        }

                        break;
                    }
                    case "INVALID_IMEI":
                    case "INVALID_IMEI_WITH_KYC":
                    case "MOBILEAPP_INACTIVE_WITH_KYC":
                    case "MOBILEAPP_INACTIVE": {
                        strTitle = "Mobile App is Not Activated";
                        String strActivationInstructions = "" +
                                "To retrieve your mobile app activation code:" +
                                "<br/>1. Dial <b>*515#</b>" +
                                "<br/>2. Enter your Mobile Banking PIN" +
                                "<br/>3. Select <b>'My Account'</b>" +
                                "<br/>4. Select <b>'Mobile App'</b>" +
                                "<br/>5. Select <b>'ACTIVATE Mobile App'</b>" +
                                "<br/>6. Select <b>'Yes'</b>" +
                                "<br/>7. Wait for an SMS with the mobile app activation code" +
                                "<br/>8. Enter the activation code below then press <b>'Activate'</b>";

                        strDescription = "Your Mobile App is not activated. Tap 'ACTIVATE' below to activate the Mobile App.";

                        Element elActivationInstructions = doc.createElement("ACTIVATION_INSTRUCTIONS");
                        elActivationInstructions.setTextContent(strActivationInstructions);
                        elData.appendChild(elActivationInstructions);

                        enResponseAction = MAPPConstants.ResponseAction.CHALLENGE_LOGIN;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        blLoginSuccessful = true;

                        break;
                    }
                    case "INCORRECT_PIN": {
                        strTitle = "Login Failed";
                        strDescription = "You have entered an incorrect username or password, please try again";
                        if (theOTPType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL) {
                            strTitle = "Incorrect Password";
                            strDescription = "You have entered an incorrect password, please try again";
                        }
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        if (theOTPType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL) {
                            enResponseAction = CON;
                        }
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

                        int intUserLoginAttemptsCount = Integer.parseInt(strNavResponse.split(":::")[1]);
                        String strName = strNavResponse.split(":::")[2];

                        String strResponseMessage = suspendUserAccess(strUsername, intUserLoginAttemptsCount, "LOGIN", strName, theOTPType).get("MESSAGE");
                        if (!strResponseMessage.equals("")) {
                            strDescription = strResponseMessage;
                        }

                        break;
                    }
                    case "SUSPENDED": {
                        strTitle = "Account Access Suspended";


                        String strAuthType = "LOGIN";
                        String strLoginAttemptAction = CBSAPI.getUserLoginAttemptAction(strUsername, strAuthType);
                        if (!strLoginAttemptAction.equals("SUSPENDED")) {
                            strAuthType = "OTP";
                            strLoginAttemptAction = CBSAPI.getUserLoginAttemptAction(strUsername, strAuthType);
                        }

                        XMLGregorianCalendar gcExpiryDate = CBSAPI.getUserLoginAttemptExpiry(strUsername, strAuthType);
                        Date dtExpiryDate = gcExpiryDate.toGregorianCalendar().getTime();

                        Date dtNow = new Date();

                        long dblDuration = dtExpiryDate.getTime() - dtNow.getTime();

                        String strTryAgainIn = "Please try again in " + APIUtils.getPrettyDateTimeDifferenceRoundedUp(dtNow, dtExpiryDate);

                        strDescription = "Sorry, your account is suspended from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn;

                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                    case "BLOCKED": {
                        strTitle = "Account Blocked";
                        strDescription = "Your account is blocked, please visit your nearest SACCO branch for assistance.";
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }

                    case "UNDER_MAINTENANCE": {
                        strTitle = "System Currently Under Maintainace";
                        strDescription = "Please be patient, We are currently working to restore back this services.";
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                    case "NOT_FOUND": {
                        strTitle = "Login Failed";

                        strDescription = "You have entered an incorrect username or password, please try again";

                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                    case "ERROR": {
                        strTitle = "Login Failed";
                        strDescription = "An Error occurred, please try again";
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                    default: {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    }
                }
            } else {
                strTitle = "Login Failed";
                strDescription = "Your login request failed with status " + strCheckStatus + ", please contact us for more information";
                enResponseAction = MAPPConstants.ResponseAction.END;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
            }

            Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
            elDescription.setTextContent(strDescription);
            elData.appendChild(elDescription);


            if (blLoginSuccessful) {
                String strMemberFullName = getUserDetails(strUsername).replaceAll(" {2}", " ").trim();

                Element elMemberData = doc.createElement("MEMBER_DATA");
                elMemberData.setAttribute("NAME", strMemberFullName.split(" ")[0]);
                elMemberData.setAttribute("FULL_NAME", strMemberFullName);
                elMemberData.setAttribute("GENDER", "MALE");
                elData.appendChild(elMemberData);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public LinkedHashMap<String, String> suspendUserAccess(String theUsername, int theLoginAttemptsCount, String theLoginType, String theName, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE theLoginInstanceType) {

        LinkedHashMap<String, String> hmRval = new LinkedHashMap<String, String>();
        hmRval.put("MESSAGE", "");
        hmRval.put("ACTION", "");

        try {
            LinkedHashMap<String, String> hmMSGPlaceholders = new LinkedHashMap<>();

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
                                hmRval.put("MESSAGE", "Sorry, the " + (theLoginInstanceType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL ? "" : ("username and/or ")) + "password provided is NOT correct.\nYou have " + attemptsRemainingToFutureLoginAction + " more attempt" + (attemptsRemainingToFutureLoginAction.equals("1") ? "" : "s") + " before your mobile banking account is suspended for " + friendlyFutureActionDuration);
                            } else if (futureLoginAction.equals("LOCK") && !currentLoginAction.equals("LOCK")) {
                                hmRval.put("MESSAGE", "Sorry, the " + (theLoginInstanceType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL ? "" : ("username and/or ")) + "password provided is NOT correct.\nYou have " + attemptsRemainingToFutureLoginAction + " more attempt" + (attemptsRemainingToFutureLoginAction.equals("1") ? "" : "s") + " before your mobile banking account is locked.");
                            }
                        }
                    }

                }

                gregorianCalendar.add(intUnit, intDuration);
                gcValidity = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);

                CBSAPI.updateAuthAttempts(theUsername, theLoginType, theLoginAttemptsCount, strTagDescription, strAction, gcValidity, false);
            } else {
                CBSAPI.updateAuthAttempts(theUsername, theLoginType, theLoginAttemptsCount, "WARNING", "WARN", gcValidity, false);
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return hmRval;
    }

    public APIUtils.OTP checkOTPRequirement(MAPPRequest theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE theOtpCheckStage) {
        boolean blRval = false;
        APIUtils.OTP otp = new APIUtils.OTP(0, 0, "", "", false);
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

            if (theOtpCheckStage == ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.GENERATION) {
                if (ndOTP != null && intOTPTTL != 0 && intOTPLength != 0) {
                    otp.setEnabled(true);
                }
            } else if (theOtpCheckStage == ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION) {
                if (ndOTP != null) {
                    otp.setEnabled(true);
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } finally {
            ndRequestMSG = null;
            configXPath = null;
            ndOTP = null;
        }
        return otp;
    }

    public MAPPResponse validateOTP(MAPPRequest theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE theOTPType) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            boolean blAddDataAction = false;

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strAppID = theMAPPRequest.getAppID();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strActivationCode = configXPath.evaluate("OTP", ndRequestMSG).trim();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "";
            String strDescription = "";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            boolean blStartKeyMatches = false;
            blStartKeyMatches = MAPPAPIDB.fnSelectOTPData(strUsername, strActivationCode);

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            String strUserLoginAttemptAction = CBSAPI.getUserLoginAttemptAction(strUsername, "OTP");

            XMLGregorianCalendar gcExpiryDate = CBSAPI.getUserLoginAttemptExpiry(strUsername, "OTP");
            Date dtExpiryDate = gcExpiryDate.toGregorianCalendar().getTime();

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
                if (blStartKeyMatches) {
                    String strUserAccountStatus = CBSAPI.mappSetIMEI(strUsername, strAppID);

                    switch (strUserAccountStatus) {
                        case "SUCCESS": {
                            strTitle = "Activation Successful";
                            strDescription = "Mobile app account activation was successful";
                            if (theOTPType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL) {
                                strTitle = "OTP Validation Successful";
                                strDescription = "Your OTP validation was successful";
                            }
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                            CBSAPI.updateAuthAttempts(strUsername, "OTP", 0, "", "NONE", gcExpiryDate, true);
                            MAPPAPIDB.fnDeleteOTPData(strUsername);
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
                            if (theOTPType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL) {
                                strTitle = "OTP Validation Failed";
                            }
                            strDescription = "An error occurred. Please try again after a few minutes.";
                            break;
                        }
                    }
                } else {
                    strTitle = "Incorrect Activation Code";
                    strDescription = "The activation code you entered is either incorrect or has expired. Please confirm the activation code and try again.";

                    if (theOTPType == ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL) {
                        strTitle = "Incorrect One Time Password";
                        strDescription = "You entered an incorrect/expired One Time Password";
                    }

                    int intUserLoginAttemptsCount = CBSAPI.getUserLoginAttemptCount(strUsername, "OTP");
                    intUserLoginAttemptsCount = intUserLoginAttemptsCount + 1;
                    String strName = getUserDetails(strUsername).replaceAll(" {2}", " ").split(" ")[0];

                    LinkedHashMap<String, String> hmSuspendUserAccess = suspendUserAccess(strUsername, intUserLoginAttemptsCount, "OTP", strName, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.ACTIVATION);
                    String strResponseMessage = hmSuspendUserAccess.get("MESSAGE");
                    String strResponseAction = hmSuspendUserAccess.get("ACTION");

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

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public void generateEmailOTP(String strDestinationAddress, String strUsername, String strSessionID) {
        try {
            String strServerName = "mail.unison.com";
            String strPortNo = "587";
            String strSecureConnection = "ssl";
            String strUserName = "no-reply@defencesacco.com";
            String strPassword = "DecT2300";
            String strSubject = "Defence Sacco Login OTP";

            String strOneTImePIN = String.valueOf(InMemoryCache.retrieve(strUsername + ":" + strSessionID));

            if (strOneTImePIN != null) {
                if (!strOneTImePIN.equals("")) {
                    SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
                    Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
                    Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (300 * 1000));

                    String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
                    String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);


                    String strMSG = "Dear Member,\n" + strOneTImePIN + " is your One Time Password(OTP) generated at " + strTimeGenerated + ". This OTP is valid up to " + strExpiryDate + ".\n";

//                    SendEmailsUsingSMTP.send(strServerName, strPortNo, strSecureConnection, strUserName, strPassword, strDestinationAddress, strSubject, strMSG);
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public MAPPResponse generateOTP(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strSessionID = String.valueOf(lnSessionID);
            String strTraceID = theMAPPRequest.getTraceID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.GENERATION);

            int intOTPTTL = 0;
            int intOTPLength = 0;
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

            InMemoryCache.store(strUsername + ":" + strSessionID, strOneTImePIN, intOTPTTL * 1000L);

            MAPPAPIDB.fnDeleteOTPData(strUsername);
            MAPPAPIDB.fnInsertOTPData(strUsername, strOneTImePIN, intOTPTTL);

            SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
            Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (intOTPTTL * 1000));

            String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
            String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);


            String strMSG = "Dear Member,\n" + strOneTImePIN + " is your One Time Password(OTP) generated at " + strTimeGenerated + ". This OTP is valid up to " + strExpiryDate + ".\n" + strOTPID + (!strAppSignature.equals("") ? ("\n" + strAppSignature) : "");

            String strCharge = "YES";

            int intMSGSent = fnSendSMS(strUsername, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "ONE_TIME_PASSWORD", "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);


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

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse activateMobileAppWithKYC(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + ":" + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strActivationCode = configXPath.evaluate("ACTIVATION_CODE", ndRequestMSG).trim();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "";
            String strDescription = "";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            if (CBSAPI.checkKYCByNationalIDNo(strUsername, strActivationCode, strPassword)) {
                String strUserAccountStatus = CBSAPI.mappSetIMEI(strUsername, strAppID);

                switch (strUserAccountStatus) {
                    case "SUCCESS": {
                        strTitle = "Activation Successful";
                        strDescription = "Mobile app account activation was successful";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        break;
                    }
                    case "ERROR": {
                        strTitle = "Account Blocked";
                        strDescription = "Your account is blocked, please visit you nearest SACCO branch for assistance.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                    case "NOT_FOUND": {
                        strTitle = "Account Not Found";
                        strDescription = "An error occurred. Please try again after a few minutes.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                    default: {
                        strTitle = "Activation Failed";
                        strDescription = "An error occurred. Please try again after a few minutes.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        break;
                    }
                }
            } else {
                strTitle = "Incorrect ID Number";
                strDescription = "The ID Number you entered is incorrect or has expired. Please confirm the activation code and try again.";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strDescription);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse registerMember(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("registerMember");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strName = configXPath.evaluate("NAME", ndRequestMSG).trim();
            String strPhoneNumber = configXPath.evaluate("PHONE_NUMBER", ndRequestMSG).trim();
            String strNationalIDNumber = configXPath.evaluate("NATIONAL_ID_NUMBER", ndRequestMSG).trim();
            String strDateOfBirth = configXPath.evaluate("DATE_OF_BIRTH", ndRequestMSG).trim();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

            Date dtMemberDateOfBirth = format.parse(strDateOfBirth);

            GregorianCalendar calMemberDateOfBirth = new GregorianCalendar();
            calMemberDateOfBirth.setTime(dtMemberDateOfBirth);
            XMLGregorianCalendar xmlGregCalMemberDateOfBirth = DatatypeFactory.newInstance().newXMLGregorianCalendar(calMemberDateOfBirth);

            String strEntryNumber = fnModifyMAPPSessionID(theMAPPRequest);

            String strNewMemberRegistrationStatus = "";//CBSAPI.registerVirtualMember(strName, strNationalIDNumber, strPhoneNumber, xmlGregCalMemberDateOfBirth, strUsername, strEntryNumber);

            switch (strNewMemberRegistrationStatus) {
                case "SUCCESS": {
                    NodeList nlMemberImages = ((NodeList) configXPath.evaluate("PASSPORT_SIZE_IMAGES/IMAGE", ndRequestMSG, XPathConstants.NODESET));
                    NodeList nlNationalIDImages = ((NodeList) configXPath.evaluate("NATIONAL_ID_IMAGES/IMAGE", ndRequestMSG, XPathConstants.NODESET));

                    String strImagesPath = "";//CBSAPI.getVirtualMemberRegistrationImagesPath();

                    for (int i = 0; i < nlMemberImages.getLength(); i++) {
                        String strImageName = configXPath.evaluate("@NAME", nlMemberImages.item(i)).trim();
                        String strImageType = configXPath.evaluate("@TYPE", nlMemberImages.item(i)).trim();
                        String strImageData = configXPath.evaluate("@DATA", nlMemberImages.item(i)).trim();

                        String strImagesPathForPhotographs = strImagesPath + "\\photographs\\" + strImageName + "." + strImageType;
                        APIUtils.fnCreateFileFromBase64(strImageData, strImagesPathForPhotographs);
                        //CBSAPI.updateVirtualMemberRegistration(strImageName, strImagesPathForPhotographs.replace("\\\\", "\\"), strEntryNumber, "Member Photographs");
                    }

                    for (int i = 0; i < nlNationalIDImages.getLength(); i++) {
                        String strImageName = configXPath.evaluate("@NAME", nlNationalIDImages.item(i)).trim();
                        String strImageType = configXPath.evaluate("@TYPE", nlNationalIDImages.item(i)).trim();
                        String strImageData = configXPath.evaluate("@DATA", nlNationalIDImages.item(i)).trim();

                        String strImagesPathForIDs = strImagesPath + "\\ids\\" + strImageName + "." + strImageType;
                        APIUtils.fnCreateFileFromBase64(strImageData, strImagesPathForIDs);
                        //CBSAPI.updateVirtualMemberRegistration(strImageName, strImagesPathForIDs.replace("\\\\", "\\"), strEntryNumber, "National ID");
                    }

                    strTitle = "Request Received Successfully";
                    strResponseText = "Your member registration was received successfully.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "ERROR": {
                    strTitle = "ERROR: Register New Member";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    strTitle = "ERROR: Register New Member";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                }
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getHomePageAddons(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("getHomePageAddons");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            String strTitle;
            String strResponseText;

            String strCharge = "NO";

            String strNewMemberRegistrationStatus = "SUCCESS";
            Element elData = doc.createElement("DATA");

            switch (strNewMemberRegistrationStatus) {
                case "SUCCESS": {
                    strTitle = "Request Received Successfully";

                    elData.setAttribute("TYPE", "ELEMENT");


                    Element elAddons = doc.createElement("ADD_ONS");
                    elData.appendChild(elAddons);

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "HOME");
                        elAddon.setAttribute("TAB", "HOME");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        Element elCard = createCardElement(doc, "AGM Announcement", "We will be having an AGM on 4th January 2021. Kindly plan to attend.", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.TEXT, 16);
                        elCards.appendChild(elCard);
                        Element elButtons = doc.createElement("BUTTONS");
                        elCard.appendChild(elButtons);
                        Element elButton = doc.createElement("BUTTON");
                        elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.CONTACT_US.getValue());
                        elButtons.appendChild(elButton);
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "TRANSACT");
                        elAddon.setAttribute("TAB", "TRANSACT");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        Element elCard = createCardElement(doc, "Launch of B2B Services", "We have launched Bank to Bank transfer services and you can now send money from SACCO to Bank.", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.TEXT, 16);
                        elCards.appendChild(elCard);
                        Element elButtons = doc.createElement("BUTTONS");
                        elCard.appendChild(elButtons);
                        Element elButton = doc.createElement("BUTTON");
                        elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.BANK_TRANSFER.getValue());
                        elButtons.appendChild(elButton);
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "ACCOUNTS");
                        elAddon.setAttribute("TAB", "MY_ACCOUNT");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        {
                            Element elCard = createCardElement(doc, "Total FOSA Accounts", "12345", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        {
                            Element elCard = createCardElement(doc, "Total BOSA Accounts", "5000", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        Element elList = doc.createElement("LIST");
                        {
                            elList.setAttribute("TYPE", "ACCOUNTS");
                            elAddon.appendChild(elList);
                            Element elCategories = doc.createElement("CATEGORIES");
                            elList.appendChild(elCategories);
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "All Accounts");
                                elCategory.setAttribute("NAME", "ALL");
                            }
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "BOSA");
                                elCategory.setAttribute("NAME", "BOSA");
                            }
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "FOSA");
                                elCategory.setAttribute("NAME", "FOSA");
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Savings Accounts");
                            elItems.setAttribute("CATEGORIES", "ALL,FOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "23893", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Deposit Contribution");
                            elItems.setAttribute("CATEGORIES", "ALL,BOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "456655", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Shares");
                            elItems.setAttribute("CATEGORIES", "ALL,BOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "563456", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "LOANS");
                        elAddon.setAttribute("TAB", "LOANS");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        {
                            Element elCard = createCardElement(doc, "Total Outstanding Loans", "12345", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        {
                            Element elCard = createCardElement(doc, "Total Guaranteed Loans", "5000", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        Element elList = doc.createElement("LIST");
                        {
                            elList.setAttribute("TYPE", "LOAND");
                            elAddon.appendChild(elList);
                            Element elCategories = doc.createElement("CATEGORIES");
                            elList.appendChild(elCategories);
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "My Loans");
                                elCategory.setAttribute("NAME", "MY_LOANS");
                            }
                            {
                                Element elCategory = doc.createElement("CATEGORY");
                                elCategories.appendChild(elCategory);
                                elCategory.setAttribute("LABEL", "Guaranteed Loans");
                                elCategory.setAttribute("NAME", "GUARANTEED_LOANS");
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Normal Loan");
                            elItems.setAttribute("CATEGORIES", "MY_LOANS");
                            {
                                Element elItem = createItemElement(doc, "Loan Type", "Normal Loan", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Loan Number", "LN893892", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Balance", "30000", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Installments", "3000", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            {
                                Element elButton = doc.createElement("BUTTON");
                                elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.PAY_LOAN.getValue());
                                elButtons.appendChild(elButton);
                            }
                            {
                                Element elButton = doc.createElement("BUTTON");
                                elButton.setAttribute("SERVICE", ke.skyworld.mbanking.mappapi.APIConstants.MAPPService.LOAN_STATEMENT.getValue());
                                elButtons.appendChild(elButton);
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Normal Loan");
                            elItems.setAttribute("CATEGORIES", "GUARANTEED_LOANS");
                            {
                                Element elItem = createItemElement(doc, "Loan Type", "Normal Loan", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Loan Number", "LN893892", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Balance", "30000", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Installments", "3000", ke.skyworld.mbanking.mappapi.APIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                        }
                    }


                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "ERROR": {
                    strTitle = "ERROR";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                    elData.setTextContent(strResponseText);
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    strTitle = "ERROR";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                    elData.setTextContent(strResponseText);
                }
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    Element createCardElement(Document theDocument, String theLabel, String theValue, ke.skyworld.mbanking.mappapi.APIConstants.CardValueType theType, float theFontSize) {
        Element rVal = theDocument.createElement("CARD");
        try {
            rVal.setAttribute("LABEL", theLabel);
            rVal.setAttribute("VALUE", theValue);
            rVal.setAttribute("TYPE", theType.getValue());
            rVal.setAttribute("FONT_SIZE", String.valueOf(theFontSize));
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    Element createItemElement(Document theDocument, String theLabel, String theValue, ke.skyworld.mbanking.mappapi.APIConstants.CardValueType theType) {
        Element rVal = theDocument.createElement("ITEM");
        try {
            rVal.setAttribute("LABEL", theLabel);
            rVal.setAttribute("VALUE", theValue);
            rVal.setAttribute("TYPE", theType.getValue());
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return rVal;
    }


    public MAPPResponse getBankAccounts(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType, boolean theForWithdrawal, String theAction) {

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

            String strAccountCategory = "ALL_ACCOUNTS";
            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                strAccountCategory = "FOSA_ACCOUNTS";
                bFOSA = true;
            }

            String strAccountsXML = CBSAPI.getSavingsAccountList(strUsername, bFOSA, strAccountCategory);

             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */

            InputSource source = new InputSource(new StringReader(strAccountsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts</TITLE>
                    <DATA TYPE='LIST'>
                        <ACCOUNTS>
                            <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                            <ACCOUNT NO='123457'>Moses Shares Acct</ACCOUNT>
                        </ACCOUNTS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            /*TODO START: GROUP BANKING TEST ITEMS - REMOVE WHEN DONE:-------------------------------------------------------------------------------------------------------*/

            Element elAccountTypes = doc.createElement("ACCOUNT_TYPES");
            elData.appendChild(elAccountTypes);

            {
                Element elAccountType1 = doc.createElement("ACCOUNT_TYPE");
                elAccountType1.setTextContent("Personal Account");
                elAccountTypes.appendChild(elAccountType1);

                // set attribute NO to ACCOUNT element
                Attr attrNO1 = doc.createAttribute("TYPE");
                attrNO1.setValue("PERSONAL_ACCOUNT");
                elAccountType1.setAttributeNode(attrNO1);

                if (blGroupBankingEnabled) {
                    Element elAccountType2 = doc.createElement("ACCOUNT_TYPE");
                    elAccountType2.setTextContent("Group Account");
                    elAccountTypes.appendChild(elAccountType2);

                    // set attribute NO to ACCOUNT element
                    Attr attrNO2 = doc.createAttribute("TYPE");
                    attrNO2.setValue("GROUP_ACCOUNT");
                    elAccountType2.setAttributeNode(attrNO2);
                }
            }

            Element elAccounts = doc.createElement("ACCOUNTS");
            elData.appendChild(elAccounts);

            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(strAccountNo);
                elAccount.setAttributeNode(attrNO);
            }

            {
                Element elAccount1 = doc.createElement("GROUP");
                elAccount1.setTextContent("Bidii Youth Group");
                elAccounts.appendChild(elAccount1);

                // set attribute NO to ACCOUNT element
                Attr attrNO1 = doc.createAttribute("NO");
                attrNO1.setValue("G0001");
                elAccount1.setAttributeNode(attrNO1);

                Element elAccount2 = doc.createElement("GROUP");
                elAccount2.setTextContent("Umoja Youth Group");
                elAccounts.appendChild(elAccount2);

                // set attribute NO to ACCOUNT element
                Attr attrNO2 = doc.createAttribute("NO");
                attrNO2.setValue("G0002");
                elAccount2.setAttributeNode(attrNO2);
            }

            {
                for (int i = 0; i < nlAccounts.getLength(); i++) {
                    String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                    String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                    Element elAccount = doc.createElement("GROUP_ACCOUNT");
                    elAccount.setTextContent("Bidii " + strAccountName);
                    elAccounts.appendChild(elAccount);

                    Attr attrNO = doc.createAttribute("NO");
                    attrNO.setValue(strAccountNo);
                    elAccount.setAttributeNode(attrNO);

                    Attr attrGroup = doc.createAttribute("GROUP_ID");
                    attrGroup.setValue("G0001");
                    elAccount.setAttributeNode(attrGroup);
                }

                for (int i = 0; i < nlAccounts.getLength(); i++) {
                    String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                    String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                    Element elAccount = doc.createElement("GROUP_ACCOUNT");
                    elAccount.setTextContent("Umoja " + strAccountName);
                    elAccounts.appendChild(elAccount);

                    Attr attrNO = doc.createAttribute("NO");
                    attrNO.setValue(strAccountNo);
                    elAccount.setAttributeNode(attrNO);

                    Attr attrGroup = doc.createAttribute("GROUP_ID");
                    attrGroup.setValue("G0002");
                    elAccount.setAttributeNode(attrGroup);
                }
            }

            /*TODO END: GROUP BANKING TEST ITEMS - REMOVE WHEN DONE:-------------------------------------------------------------------------------------------------------*/

            if (theAction.equalsIgnoreCase("GET_TRANSACTION_ACCOUNTS_AND_DEPOSIT_SERVICES")) {
                Element elServices = doc.createElement("SERVICES");
                elData.appendChild(elServices);

                //create element SERVICE and append to element SERVICES
                Element elServiceMpesa = doc.createElement("SERVICE");
                elServiceMpesa.setAttribute("ID", "MPESA");
                elServiceMpesa.setTextContent("Safaricom M-PESA");
                elServices.appendChild(elServiceMpesa);

                String strMin = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMinimum();
                String strMax = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMaximum();

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(strMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(strMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);
            }
            /*Start of Account Statement Duration Changes*/
            else {
                /*Prerequisites*/
                /*Add the following block of xml code to mapp client parameters XML under */
                /*OTHER_DETAILS / CUSTOM_PARAMETERS / SERVICE_CONFIGS*/

                /*<CONFIGURATION>
                    <ACCOUNT_STATEMENT>
                        <STATEMENT_PERIODS>
                            <PERIOD NAME="CUSTOM" LABEL="Custom Period" STATUS="ACTIVE" START_DATE="MONTH_START" END_DATE="MONTH_END" MAXIMUM_TRANSACTIONS="100"/>
                            <PERIOD NAME="1WEEK" LABEL="Past 1 Week" STATUS="ACTIVE" START_DATE="TODAY-7D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="50"/>
                            <PERIOD NAME="2WEEKS" LABEL="Past 2 Weeks" STATUS="ACTIVE" START_DATE="TODAY-14D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="75"/>
                            <PERIOD NAME="1MONTHS" LABEL="Past 1 Month" STATUS="ACTIVE" START_DATE="TODAY-30D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="100"/>
                            <PERIOD NAME="3MONTHS" LABEL="Past 3 Months" STATUS="ACTIVE" START_DATE="TODAY-90D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="250"/>
                            <PERIOD NAME="6MONTHS" LABEL="Past 6 Months" STATUS="ACTIVE" START_DATE="TODAY-183D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="500"/>
                            <PERIOD NAME="YTD" LABEL="This Year To Date" STATUS="ACTIVE" START_DATE="TODAY-YTD" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="750"/>
                            <PERIOD NAME="1YEAR" LABEL="Past 1 Year" STATUS="ACTIVE" START_DATE="TODAY-365D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="1000"/>
                        </STATEMENT_PERIODS>
                    </ACCOUNT_STATEMENT>
                </CONFIGURATION>*/
                Element elStatementConfiguration = doc.createElement("STATEMENT_CONFIGURATION");
                elStatementConfiguration.setAttribute("DEFAULT", "CUSTOM");

                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());

                /*Added the function below under APIUtils*/
                LinkedList<LinkedHashMap<String, String>> llHmStatementPeriods = getStatementPeriods(APIConstants.APPLICATION_TYPE.MAPP);

                llHmStatementPeriods.forEach(hmStatementPeriods -> {
                    String strName = hmStatementPeriods.get("NAME");
                    String strLabel = hmStatementPeriods.get("LABEL");
                    String strStartDate = hmStatementPeriods.get("START_DATE");
                    String strEndDate = hmStatementPeriods.get("END_DATE");
                    String strMaximumTransactions = hmStatementPeriods.get("MAXIMUM_TRANSACTIONS");

                    long lnEndDate = System.currentTimeMillis();
                    long lnStartDate = lnEndDate;
                    long lnMillisecondsInDay = 86400000;

                    if (strStartDate.matches("(TODAY-)+(\\d{1,})+(D)") && strEndDate.matches("^TODAY$")) {
                        String strDays = "";
                        ;

                        Pattern ptPattern = Pattern.compile("(?!TODAY)(-)\\d{1,}(?=D)");
                        Matcher mtMatcher = ptPattern.matcher(strStartDate);
                        if (mtMatcher.find()) {
                            strDays = mtMatcher.group();
                            long lnDays = Long.parseLong(strDays);
                            lnStartDate = lnEndDate + (lnDays * lnMillisecondsInDay);
                        }
                    }

                    if (strStartDate.matches("^MONTH_START$") && strEndDate.matches("^MONTH_END$")) {
                        LocalDate ldToday = LocalDate.now();
                        lnStartDate = ldToday.withDayOfMonth(1).toEpochDay() * lnMillisecondsInDay;
                        lnEndDate = ldToday.withDayOfMonth(ldToday.lengthOfMonth()).toEpochDay() * lnMillisecondsInDay;
                    }

                    if (strStartDate.matches("^TODAY-YTD$") && strEndDate.matches("^TODAY$")) {
                        LocalDate ldToday = LocalDate.now();
                        lnStartDate = ldToday.withDayOfYear(1).toEpochDay() * lnMillisecondsInDay;
                        lnEndDate = ldToday.toEpochDay() * lnMillisecondsInDay;
                    }

                    strStartDate = String.valueOf(lnStartDate);
                    strEndDate = String.valueOf(lnEndDate);

                    Element elStatementPeriod = doc.createElement("PERIOD");
                    elStatementPeriod.setAttribute("LABEL", strLabel);
                    elStatementPeriod.setAttribute("NAME", strName);
                    elStatementPeriod.setAttribute("START_DATE", strStartDate);
                    elStatementPeriod.setAttribute("END_DATE", strEndDate);
                    elStatementPeriod.setAttribute("MAXIMUM_TRANSACTIONS", strMaximumTransactions);
                    elStatementConfiguration.appendChild(elStatementPeriod);
                });

                elData.appendChild(elStatementConfiguration);
            }
            /*Start of Account Statement Duration Changes*/

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getATMCards(MAPPRequest theMAPPRequest) {

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

            boolean bFOSA = false;

            String strCardsXML = "" +
                    "<ATM_CARDS>" +
                    "<CARD><ID>01</ID><NAME>9235808234587239</NAME></CARD>" +
                    "<CARD><ID>02</ID><NAME>3249058234598079</NAME></CARD>" +
                    "</ATM_CARDS>";

             /*
             //Response from CBS is:
                <ATM_CARDS>
                    <CARD><ID>01</ID><NAME>9235808234587239</NAME></CARD>
                    <CARD><ID>02</ID><NAME>3249058234598079</NAME></CARD>
                </ATM_CARDS>
             */

            InputSource source = new InputSource(new StringReader(strCardsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/ATM_CARDS", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts</TITLE>
                    <DATA TYPE='LIST'>
                        <CARDS>
                            <CARD ID='123456' NAME='123456' />
                            <CARD ID='123457' NAME='123457' />
                        </CARDS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            Element elAccounts = doc.createElement("CARDS");
            elData.appendChild(elAccounts);

            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("ID", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("NAME", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("CARD");
                elAccounts.appendChild(elAccount);

                elAccount.setAttribute("ID", strAccountNo);
                elAccount.setAttribute("NAME", strAccountName);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccounts(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();

            String strAccountsXML = CBSAPI.getSavingsAccountList(strUsername, true, "WITHDRAWABLE_ACCOUNTS");

             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */

            InputSource source = new InputSource(new StringReader(strAccountsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts</TITLE>
                    <DATA TYPE='LIST'>
                        <ACCOUNTS>
                            <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                            <ACCOUNT NO='123457'>Moses Shares Acct</ACCOUNT>
                        </ACCOUNTS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elAccounts = doc.createElement("ACCOUNTS");
            elData.appendChild(elAccounts);

            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(strAccountNo);
                elAccount.setAttributeNode(attrNO);
            }


            double dblUtilityETopUplMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMinimum());
            double dblUtilityETopUplMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMaximum());

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(dblUtilityETopUplMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(dblUtilityETopUplMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndMobileMoneyServices(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

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

            String strAccountCategory = "ALL_ACCOUNTS";
            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                strAccountCategory = "FOSA_ACCOUNTS";
                bFOSA = true;
            }

            String strAccountsXML = CBSAPI.getSavingsAccountList(strUsername, bFOSA, strAccountCategory);

             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */

            InputSource source = new InputSource(new StringReader(strAccountsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts and Services</TITLE>
                    <DATA TYPE='LIST'>
                        <ACCOUNTS_AND_SERVICES>
                            <ACCOUNTS>
                                <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                                <ACCOUNT NO='123457'>Moses Shares Acct</ACCOUNT>
                            </ACCOUNTS>
                            <SERVICES>
                                <SERVICE ID='1112'>Safaricom M-PESA</SERVICE>
                                <SERVICE ID='1113'>Airtel Money</SERVICE>
                                <SERVICE ID='1114'>Equitel Money</SERVICE>
                            </SERVICES>
                        </ACCOUNTS_AND_SERVICES>
                        <WITHDRAWAL_LIMITS MIN='10' MAX='70000'/>
                    </DATA>
                </MSG>
            </MESSAGES>
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            //ceate element ACCOUNTS_AND_SERVICES and append to DATA
            Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_SERVICES");
            elData.appendChild(elAccountsAndServices);

            //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
            Element elAccounts = doc.createElement("ACCOUNTS");
            elAccountsAndServices.appendChild(elAccounts);

            //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
            Element elServices = doc.createElement("SERVICES");
            elAccountsAndServices.appendChild(elServices);


            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", strAccountNo);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }

            /*Prerequisites*/
            /*Add the following block of xml code to mapp client parameters XML under */
            /*OTHER_DETAILS / CUSTOM_PARAMETERS / SERVICE_CONFIGS*/
            /*for further, check mapp_client_parameters_xml.xml under the /notes/client_param_xmls folder of this project*/

            /*<CONFIGURATION>
                <CASH_WITHDRAWAL>
					<CHANNELS>
						<CHANNEL NAME="MPESA" LABEL="Safaricom M-PESA" STATUS="ACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
						<CHANNEL NAME="ATM" LABEL="Withdraw Via ATM" STATUS="ACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
						<CHANNEL NAME="AGENT" LABEL="Withdraw Via AGENT" STATUS="ACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
					</CHANNELS>
				</CASH_WITHDRAWAL>
            </CONFIGURATION>*/

            /*Modified parameters for this function as well*/
            LinkedList<APIUtils.WithdrawalChannel> lsWithdrawalChannels = APIUtils.getActiveWithdrawalChannels(APIConstants.APPLICATION_TYPE.MAPP);
            lsWithdrawalChannels.forEach(lsWithdrawalChannel -> {
                String strName = lsWithdrawalChannel.getName();
                String strLabel = lsWithdrawalChannel.getLabel();
                String strStatus = lsWithdrawalChannel.getStatus();

                if (strStatus.equals("ACTIVE")) {
                    Element elService = doc.createElement("SERVICE");
                    elService.setAttribute("ID", strName);
                    elService.setTextContent(strLabel);
                    elServices.appendChild(elService);
                }
            });

            double dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
            double dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(dblWithdrawalMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(dblWithdrawalMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndBanks(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

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

            String strAccountCategory = "ALL_ACCOUNTS";
            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                strAccountCategory = "FOSA_ACCOUNTS";
                bFOSA = true;
            }

            String strAccountsXML = CBSAPI.getSavingsAccountList(strUsername, bFOSA, strAccountCategory);

             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */

            InputSource source = new InputSource(new StringReader(strAccountsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts and Services</TITLE>
                    <DATA TYPE='LIST'>
                        <ACCOUNTS_AND_SERVICES>
                            <ACCOUNTS>
                                <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                                <ACCOUNT NO='123457'>Moses Shares Acct</ACCOUNT>
                            </ACCOUNTS>
                            <BANKS>
                                <BANK PAYBILL_NO='123456'>KCB</SERVICE>
                                <BANK PAYBILL_NO='123456'>Equity Bank</SERVICE>
                                <BANK PAYBILL_NO='123456'>Coop Bank</SERVICE>
                            </SERVICES>
                        </ACCOUNTS_AND_SERVICES>
                        <WITHDRAWAL_LIMITS MIN='10' MAX='70000'/>
                    </DATA>
                </MSG>
            </MESSAGES>
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            //ceate element ACCOUNTS_AND_SERVICES and append to DATA
            Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_BANKS");
            elData.appendChild(elAccountsAndServices);

            //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
            Element elAccounts = doc.createElement("ACCOUNTS");
            elAccountsAndServices.appendChild(elAccounts);

            //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
            Element elBanks = doc.createElement("BANKS");
            elAccountsAndServices.appendChild(elBanks);


            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", strAccountNo);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("BANK_SHORT_CODE");
            for (APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts) {
                Element elBank2 = doc.createElement("BANK");
                elBank2.setAttribute("PAYBILL_NO", serviceProviderAccount.getProviderAccountIdentifier());
                elBank2.setTextContent(serviceProviderAccount.getProviderAccountLongTag());
                elBanks.appendChild(elBank2);
            }

            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            SPManager spManager = new SPManager(strIntegritySecret);
            String strAccounts = spManager.getAllUserAccountsByProviders(SPManagerConstants.ProviderAccountType.UTILITY_CODE, SPManagerConstants.UserIdentifierType.MSISDN, strUsername);
            strAccounts = strAccounts.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
            strAccounts = trimXML(strAccounts);

            if (!strAccounts.equals("<ACCOUNTS/>")) {
                InputSource sourceForPaybillAccounts = new InputSource(new StringReader(strAccounts));
                DocumentBuilderFactory builderFactoryForPaybillAccounts = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForPaybillAccounts = builderFactoryForPaybillAccounts.newDocumentBuilder();
                Document xmlDocumentForPaybillAccounts = builderForPaybillAccounts.parse(sourceForPaybillAccounts);
                XPath configXPathForPaybillAccounts = XPathFactory.newInstance().newXPath();

                NodeList nlPayBillAccounts = ((NodeList) configXPathForPaybillAccounts.evaluate("/ACCOUNTS/ACCOUNT", xmlDocumentForPaybillAccounts, XPathConstants.NODESET));

                Element elAccountsForPaybill = doc.createElement("ACCOUNTS_FOR_PAYBILL");
                for (int i = 0; i < nlPayBillAccounts.getLength(); i++) {
                    Element elSingleAccountsForPaybill = doc.createElement("PAYBILL_ACCOUNT");
                    elSingleAccountsForPaybill.setAttribute("NAME", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NAME").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("NUMBER", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NUMBER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("TYPE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_IDENTIFIER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("PROVIDER_ACCOUNT_CODE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_CODE").getTextContent());
                    elAccountsForPaybill.appendChild(elSingleAccountsForPaybill);
                }
                elAccountsAndServices.appendChild(elAccountsForPaybill);
            }

            String strMin = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum();
            String strMax = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndPaybillServices(MAPPRequest theMAPPRequest, MAPPConstants.AccountType theAccountType) {

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

            String strAccountCategory = "ALL_ACCOUNTS";
            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                strAccountCategory = "FOSA_ACCOUNTS";
                bFOSA = true;
            }

            String strAccountsXML = CBSAPI.getSavingsAccountList(strUsername, bFOSA, strAccountCategory);

             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */

            InputSource source = new InputSource(new StringReader(strAccountsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts and Services</TITLE>
                    <DATA TYPE='LIST'>
                        <ACCOUNTS_AND_SERVICES>
                            <ACCOUNTS>
                                <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                                <ACCOUNT NO='123457'>Moses Shares Acct</ACCOUNT>
                            </ACCOUNTS>
                            <BANKS>
                                <BANK PAYBILL_NO='123456'>KCB</SERVICE>
                                <BANK PAYBILL_NO='123456'>Equity Bank</SERVICE>
                                <BANK PAYBILL_NO='123456'>Coop Bank</SERVICE>
                            </SERVICES>
                        </ACCOUNTS_AND_SERVICES>
                        <WITHDRAWAL_LIMITS MIN='10' MAX='70000'/>
                    </DATA>
                </MSG>
            </MESSAGES>
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            //ceate element ACCOUNTS_AND_SERVICES and append to DATA
            Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_PAYBILL_SERVICES");
            elData.appendChild(elAccountsAndServices);

            //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
            Element elAccounts = doc.createElement("ACCOUNTS");
            elAccountsAndServices.appendChild(elAccounts);

            //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
            Element elServices = doc.createElement("PAYBILL_SERVICES");
            elAccountsAndServices.appendChild(elServices);


            for (int i = 0; i < nlAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setAttribute("NO", strAccountNo);
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);
            }

            //create element SERVICE and append to element SERVICES
            //KPLC Prepaid (Tokens)
            Element elService1 = doc.createElement("SERVICE");
            elService1.setAttribute("PAYBILL_NO", "888880");
            elService1.setAttribute("REF_NAME", "Meter Number");
            elService1.setTextContent("KPLC Tokens");
            elServices.appendChild(elService1);

            //KPLC Postpaid
            Element elService2 = doc.createElement("SERVICE");
            elService2.setAttribute("PAYBILL_NO", "888888");
            elService2.setAttribute("REF_NAME", "Account Number");
            elService2.setTextContent("KPLC Post-Paid");
            elServices.appendChild(elService2);

            //DStv
            Element elService3 = doc.createElement("SERVICE");
            elService3.setAttribute("PAYBILL_NO", "444900");
            elService3.setAttribute("REF_NAME", "Smart Card Number");
            elService3.setTextContent("DStv");
            elServices.appendChild(elService3);

            //Gotv
            Element elService4 = doc.createElement("SERVICE");
            elService4.setAttribute("PAYBILL_NO", "423655");
            elService4.setAttribute("REF_NAME", "Account Number");
            elService4.setTextContent("GOtv");
            elServices.appendChild(elService4);

            //ZUKU
            Element elService5 = doc.createElement("SERVICE");
            elService5.setAttribute("PAYBILL_NO", "320320");
            elService5.setAttribute("REF_NAME", "Account Number");
            elService5.setTextContent("ZUKU");
            elServices.appendChild(elService5);

            //StarTimes
            Element elService6 = doc.createElement("SERVICE");
            elService6.setAttribute("PAYBILL_NO", "585858");
            elService6.setAttribute("REF_NAME", "Account Number");
            elService6.setTextContent("StarTimes");
            elServices.appendChild(elService6);

            //Nairobi Water
            Element elService7 = doc.createElement("SERVICE");
            elService7.setAttribute("PAYBILL_NO", "444400");
            elService7.setAttribute("REF_NAME", "Account Number");
            elService7.setTextContent("Nairobi Water");
            elServices.appendChild(elService7);

            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            SPManager spManager = new SPManager(strIntegritySecret);
            String strAccounts = spManager.getAllUserAccountsByProviders(SPManagerConstants.ProviderAccountType.UTILITY_CODE, SPManagerConstants.UserIdentifierType.MSISDN, strUsername);
            strAccounts = strAccounts.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
            strAccounts = trimXML(strAccounts);

            if (!strAccounts.equals("<ACCOUNTS/>")) {
                InputSource sourceForPaybillAccounts = new InputSource(new StringReader(strAccounts));
                DocumentBuilderFactory builderFactoryForPaybillAccounts = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForPaybillAccounts = builderFactoryForPaybillAccounts.newDocumentBuilder();
                Document xmlDocumentForPaybillAccounts = builderForPaybillAccounts.parse(sourceForPaybillAccounts);
                XPath configXPathForPaybillAccounts = XPathFactory.newInstance().newXPath();

                NodeList nlPayBillAccounts = ((NodeList) configXPathForPaybillAccounts.evaluate("/ACCOUNTS/ACCOUNT", xmlDocumentForPaybillAccounts, XPathConstants.NODESET));

                Element elAccountsForPaybill = doc.createElement("ACCOUNTS_FOR_PAYBILL");
                for (int i = 0; i < nlPayBillAccounts.getLength(); i++) {
                    Element elSingleAccountsForPaybill = doc.createElement("PAYBILL_ACCOUNT");
                    elSingleAccountsForPaybill.setAttribute("NAME", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NAME").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("NUMBER", nlPayBillAccounts.item(i).getAttributes().getNamedItem("NUMBER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("TYPE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_IDENTIFIER").getTextContent());
                    elSingleAccountsForPaybill.setAttribute("PROVIDER_ACCOUNT_CODE", nlPayBillAccounts.item(i).getAttributes().getNamedItem("PROVIDER_ACCOUNT_CODE").getTextContent());
                    elAccountsForPaybill.appendChild(elSingleAccountsForPaybill);
                }
                elAccountsAndServices.appendChild(elAccountsForPaybill);
            }


            String strMin = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMinimum();
            String strMax = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public static String trimXML(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ((line = reader.readLine()) != null)
                result.append(line.trim());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MAPPResponse getTransferAccounts(MAPPRequest theMAPPRequest) {

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

            String strAccountCategory = "ALL_ACCOUNTS";

            String strAccountsXMLToAccounts = CBSAPI.getSavingsAccountList(strUsername, false, "ALL_ACCOUNTS");
            String strAccountsXMLFromAccounts = CBSAPI.getSavingsAccountList(strUsername, true, "FOSA_ACCOUNTS");

             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */

            InputSource sourceFromAccounts = new InputSource(new StringReader(strAccountsXMLFromAccounts));
            DocumentBuilderFactory builderFactoryFromAccounts = DocumentBuilderFactory.newInstance();
            DocumentBuilder builderFromAccounts = builderFactoryFromAccounts.newDocumentBuilder();
            Document xmlDocumentFromAccounts = builderFromAccounts.parse(sourceFromAccounts);
            NodeList nlFromAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocumentFromAccounts, XPathConstants.NODESET)).item(0).getChildNodes();

            InputSource sourceToAccounts = new InputSource(new StringReader(strAccountsXMLToAccounts));
            DocumentBuilderFactory builderFactoryToAccounts = DocumentBuilderFactory.newInstance();
            DocumentBuilder builderToAccounts = builderFactoryToAccounts.newDocumentBuilder();
            Document xmlDocumentToAccounts = builderToAccounts.parse(sourceToAccounts);
            NodeList nlToAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocumentToAccounts, XPathConstants.NODESET)).item(0).getChildNodes();

            
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Accounts</TITLE>
                    <DATA TYPE='LIST'>
                        <FROM_ACCOUNTS>
                            <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                        </FROM_ACCOUNTS>
                        <TO_ACCOUNTS OTHER_ACCOUNT_ENABLED='TRUE'>
                            <ACCOUNT NO='123456'>Moses Savings Acct</ACCOUNT>
                            <ACCOUNT NO='123457'>Moses Shares Acct</ACCOUNT>
                        </FROM_ACCOUNTS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Transfer Accounts";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elFromAccounts = doc.createElement("FROM_ACCOUNTS");
            elData.appendChild(elFromAccounts);

            Element elToAccountTypes = doc.createElement("TO_ACCOUNT_TYPES");
            Element elAccountTypeMy = doc.createElement("ACCOUNT_TYPE");
            elAccountTypeMy.setTextContent("MY Account");
            elAccountTypeMy.setAttribute("TYPE_ID", "MY_ACCOUNT");
            elToAccountTypes.appendChild(elAccountTypeMy);

            if (CBSAPI.checkService("Transfer to Other Account")) {
                Element elAccountTypeOther = doc.createElement("ACCOUNT_TYPE");
                elAccountTypeOther.setTextContent("OTHER Account");
                elAccountTypeOther.setAttribute("TYPE_ID", "OTHER_ACCOUNT");
                elToAccountTypes.appendChild(elAccountTypeOther);
            }

            elData.appendChild(elToAccountTypes);

            Element elToAccounts = doc.createElement("TO_ACCOUNTS");
            elData.appendChild(elToAccounts);

            for (int i = 0; i < nlFromAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlFromAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlFromAccounts.item(i)).trim();

                Element elAccount = doc.createElement("FROM_ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elFromAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(strAccountNo);
                elAccount.setAttributeNode(attrNO);
            }

            for (int i = 0; i < nlToAccounts.getLength(); i++) {
                String strAccountNo = configXPath.evaluate("AccNo", nlToAccounts.item(i)).trim();
                String strAccountName = configXPath.evaluate("AccName", nlToAccounts.item(i)).trim();

                Element elAccount = doc.createElement("TO_ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elToAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(strAccountNo);
                elAccount.setAttributeNode(attrNO);
            }

            //Option for Transfer to Other Account
            /*Element elOtherAccount = doc.createElement("TO_ACCOUNT");
            elOtherAccount.setTextContent("OTHER Account");
            elOtherAccount.setAttribute("NO", "OTHER");
            elToAccounts.appendChild(elOtherAccount);*/

            //Option for Transfer to M-PESA
            /*Element elMpesaAccount = doc.createElement("TO_ACCOUNT");
            elMpesaAccount.setTextContent("Withdraw to M-Pesa");
            elMpesaAccount.setAttribute("NO", "MPESA");
            elToAccounts.appendChild(elMpesaAccount);*/

            String strMin = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMinimum();
            String strMax = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberLoans(MAPPRequest theMAPPRequest) {

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


            String strLoansXML = CBSAPI.getMemberLoanListMobileApp(strUsername);

             /*
             //Response from NAV is:
            <Loans>
                <Product>
                    <LoanNo>BLN-55740</LoanNo>
                    <Type>School Fees Loan</Type>
                    <LoanBalance>17,163.07</LoanBalance>
                </Product>
                <Product>
                    <LoanNo>BLN-63695</LoanNo>
                    <Type>BELA Loan</Type>
                    <LoanBalance>19,969.31</LoanBalance>
                </Product>
            </Loans>
             */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (!strLoansXML.equals("")) {
                enDataType = MAPPConstants.ResponsesDataType.LIST;

                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlLoans = ((NodeList) configXPath.evaluate("Loans/Product", xmlDocument, XPathConstants.NODESET));

                /*TODO START: GROUP BANKING TEST ITEMS - REMOVE WHEN DONE:-------------------------------------------------------------------------------------------------------*/

                Element elLoanTypes = doc.createElement("LOAN_TYPES");
                elData.appendChild(elLoanTypes);

                {
                    Element elLoanType1 = doc.createElement("LOAN_TYPE");
                    elLoanType1.setTextContent("Personal Loan");
                    elLoanTypes.appendChild(elLoanType1);

                    // set attribute NO to LOAN element
                    Attr attrNO1 = doc.createAttribute("TYPE");
                    attrNO1.setValue("PERSONAL_LOAN");
                    elLoanType1.setAttributeNode(attrNO1);

                    if (blGroupBankingEnabled) {
                        Element elLoanType2 = doc.createElement("LOAN_TYPE");
                        elLoanType2.setTextContent("Group Loan");
                        elLoanTypes.appendChild(elLoanType2);

                        // set attribute NO to LOAN element
                        Attr attrNO2 = doc.createAttribute("TYPE");
                        attrNO2.setValue("GROUP_LOAN");
                        elLoanType2.setAttributeNode(attrNO2);
                    }
                }


                Element elLoans = doc.createElement("LOANS");
                elData.appendChild(elLoans);


                for (int i = 0; i < nlLoans.getLength(); i++) {
                    String strLoanNo = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                    String strLoanName = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                    String strLoanBalance = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                    Element elLoan = doc.createElement("LOAN");
                    elLoan.setTextContent(strLoanName);
                    elLoan.setAttribute("SERIAL_NO", strLoanNo);
                    elLoan.setAttribute("AMOUNT", strLoanBalance);
                    elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                    elLoan.setAttribute("BALANCE", strLoanBalance);
                    elLoans.appendChild(elLoan);
                }


                if (blGroupBankingEnabled) {
                    Element elLoan1 = doc.createElement("GROUP");
                    elLoan1.setTextContent("Bidii Youth Group");
                    elLoanTypes.appendChild(elLoan1);

                    // set attribute NO to LOAN element
                    Attr attrNO1 = doc.createAttribute("NO");
                    attrNO1.setValue("G0001");
                    elLoan1.setAttributeNode(attrNO1);

                    Element elLoan2 = doc.createElement("GROUP");
                    elLoan2.setTextContent("Umoja Youth Group");
                    elLoanTypes.appendChild(elLoan2);

                    // set attribute NO to LOAN element
                    Attr attrNO2 = doc.createAttribute("NO");
                    attrNO2.setValue("G0002");
                    elLoan2.setAttributeNode(attrNO2);
                }

                {
                    for (int i = 0; i < nlLoans.getLength(); i++) {
                        String strLoanNo = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                        String strLoanName = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                        String strLoanBalance = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                        Element elLoan = doc.createElement("GROUP_LOAN");
                        elLoan.setTextContent("Bidii " + strLoanName);
                        elLoan.setAttribute("SERIAL_NO", strLoanNo);
                        elLoan.setAttribute("AMOUNT", strLoanBalance);
                        elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                        elLoan.setAttribute("BALANCE", strLoanBalance);
                        elLoan.setAttribute("GROUP_ID", "G0001");
                        elLoans.appendChild(elLoan);
                    }

                    for (int i = 0; i < nlLoans.getLength(); i++) {
                        String strLoanNo = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                        String strLoanName = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                        String strLoanBalance = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                        Element elLoan = doc.createElement("GROUP_LOAN");
                        elLoan.setTextContent("Umoja " + strLoanName);
                        elLoan.setAttribute("SERIAL_NO", strLoanNo);
                        elLoan.setAttribute("AMOUNT", strLoanBalance);
                        elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                        elLoan.setAttribute("BALANCE", strLoanBalance);
                        elLoan.setAttribute("GROUP_ID", "G0002");
                        elLoans.appendChild(elLoan);
                    }
                }

                /*TODO END: GROUP BANKING TEST ITEMS - REMOVE WHEN DONE:-------------------------------------------------------------------------------------------------------*/
            } else {
                elData.setTextContent("No Loans Found");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse addLoanGuarantors(MAPPRequest theMAPPRequest) {

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

            Node ndRequestMSG = theMAPPRequest.getMSG();

            NodeList nlGuarantors = ((NodeList) configXPath.evaluate("LOAN_AND_GUARANTORS/GUARANTORS/GUARANTOR", ndRequestMSG, XPathConstants.NODESET));
            String strLoanEntryNumber = configXPath.evaluate("LOAN_AND_GUARANTORS/LOAN_ENTRY_NO", ndRequestMSG).trim();

            boolean blErrorOccured = false;

            for (int i = 0; i < nlGuarantors.getLength(); i++) {
                String strPhoneNumber = nlGuarantors.item(i).getTextContent().trim();
                String strAdGuarantorResponse = CBSAPI.addRemoveMobileLoanGuarantor(Integer.parseInt(strLoanEntryNumber), strPhoneNumber, "ADD");
                if (!strAdGuarantorResponse.equals("SUCCESS")) {
                    blErrorOccured = true;
                }
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";
            String strResponseText = "An error occurred. Please try again after a few minutes.";

            if (!blErrorOccured) {
                strTitle = "Guarantors Added Successfully";
                strResponseText = "You loan guarantors have been added successfully. Please contact the guarantors so that they can approve guarantorship.";
                strCharge = "YES";
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
            } else {
                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                strTitle = "ERROR: Add Loan Guarantors";
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberLoansWithPendingGuarantors(MAPPRequest theMAPPRequest) {

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


            String strLoansXML = CBSAPI.getLoanPendingGuarantor(strUsername);

             /*
             //Response from NAV is:
            <Loans>
                <Product>
                    <LoanNo>BLN-55740</LoanNo>
                    <Type>School Fees Loan</Type>
                    <LoanBalance>17,163.07</LoanBalance>
                </Product>
                <Product>
                    <LoanNo>BLN-63695</LoanNo>
                    <Type>BELA Loan</Type>
                    <LoanBalance>19,969.31</LoanBalance>
                </Product>
            </Loans>
             */


            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Loans</TITLE>
                    <DATA TYPE='LIST'>
                        <LOANS>
                            <LOAN NO='123456'>Moses Savings Acct</LOAN>
                            <LOAN NO='123457'>Moses Shares Acct</LOAN>
                        </LOANS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (!strLoansXML.equals("NULL")) {
                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlLoans = ((NodeList) configXPath.evaluate("Loan", xmlDocument, XPathConstants.NODESET));

                Element elLoans = doc.createElement("LOANS");
                elData.appendChild(elLoans);


                for (int i = 0; i < nlLoans.getLength(); i++) {
                    String strLoanNo = configXPath.evaluate("EntryNo", nlLoans.item(i)).trim();
                    String strLoanName = configXPath.evaluate("ProductName", nlLoans.item(i)).trim();
                    String strRequestedAmount = configXPath.evaluate("RequestedAmount", nlLoans.item(i)).trim();
                    String strLoanStatus = configXPath.evaluate("LoanStatus", nlLoans.item(i)).trim();

                    Element elLoan = doc.createElement("LOAN");
                    elLoan.setAttribute("ENTRY_NO", strLoanNo);
                    elLoan.setAttribute("PRODUCT_NAME", strLoanName);
                    elLoan.setAttribute("REQUESTED_AMOUNT", strRequestedAmount);
                    elLoan.setAttribute("STATUS", strLoanStatus);
                    elLoans.appendChild(elLoan);

                    NodeList nlGuarantors = ((NodeList) configXPath.evaluate("Loan/Guarantors/GuarantorDetail", xmlDocument, XPathConstants.NODESET));
                    Element elGuarantors = doc.createElement("GUARANTORS");

                    for (int j = 0; j < nlGuarantors.getLength(); j++) {
                        String strGuarantorName = APIUtils.titleCase(configXPath.evaluate("GuarantorName", nlGuarantors.item(j)).trim());
                        String strPhoneNo = configXPath.evaluate("PhoneNo", nlGuarantors.item(j)).trim();
                        String strMemberNo = configXPath.evaluate("MemberNo", nlGuarantors.item(j)).trim();
                        String strLoanGuarantorStatus = configXPath.evaluate("LoanStatus", nlGuarantors.item(j)).trim();

                        Element elGuarantor = doc.createElement("GUARANTOR");
                        elGuarantor.setAttribute("NAME", strGuarantorName);
                        elGuarantor.setAttribute("MEMBER_NO", strMemberNo);
                        elGuarantor.setAttribute("PHONE_NO", strPhoneNo);
                        elGuarantor.setAttribute("APPROVAL_STATUS", strLoanGuarantorStatus);
                        elGuarantors.appendChild(elGuarantor);
                    }
                    elLoan.appendChild(elGuarantors);
                }
            } else {
                elData.setTextContent("No Loans Found");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberLoansWithPaymentDetails(MAPPRequest theMAPPRequest) {

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


            String strLoansXML = CBSAPI.getMemberLoanListMobileApp(strUsername);

             /*
             //Response from NAV is:
            <Loans>
                <Product>
                    <LoanNo>BLN-55740</LoanNo>
                    <Type>School Fees Loan</Type>
                    <LoanBalance>17,163.07</LoanBalance>
                </Product>
                <Product>
                    <LoanNo>BLN-63695</LoanNo>
                    <Type>BELA Loan</Type>
                    <LoanBalance>19,969.31</LoanBalance>
                </Product>
            </Loans>
             */



            /*
            Response to Gateway
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='NO'>
                    <TITLE>Withdrawal Loans</TITLE>
                    <DATA TYPE='LIST'>
                        <LOANS>
                            <LOAN SERIAL_NO="123456" AMOUNT="1000" CHANGE_AMOUNT="YES/NO" BALANCE="1000">Wikendi Njema Loan 0</LOAN>
                            <LOAN SERIAL_NO="123456" AMOUNT="1000" CHANGE_AMOUNT="YES/NO" BALANCE="1000">Wikendi Njema Loan 1</LOAN>
                            <LOAN SERIAL_NO="123456" AMOUNT="1000" CHANGE_AMOUNT="YES/NO" BALANCE="1000">Wikendi Njema Loan 2</LOAN>
                        </LOANS>
                        <REPAYMENT_OPTIONS ENABLED="TRUE">
                            <OPTION VALUE="">Savings Account</OPTION>
                            <OPTION VALUE="">Safaricom M-Pesa</OPTION>
                        </REPAYMENT_OPTIONS>
                    </DATA>
                </MSG>
            </MESSAGES
            */

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (!strLoansXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlLoans = ((NodeList) configXPath.evaluate("Loans/Product", xmlDocument, XPathConstants.NODESET));

                /*TODO START: GROUP BANKING TEST ITEMS - REMOVE WHEN DONE:-------------------------------------------------------------------------------------------------------*/

                Element elLoanTypes = doc.createElement("LOAN_TYPES");
                elData.appendChild(elLoanTypes);

                {
                    Element elLoanType1 = doc.createElement("LOAN_TYPE");
                    elLoanType1.setTextContent("Personal Loan");
                    elLoanTypes.appendChild(elLoanType1);

                    // set attribute NO to LOAN element
                    Attr attrNO1 = doc.createAttribute("TYPE");
                    attrNO1.setValue("PERSONAL_LOAN");
                    elLoanType1.setAttributeNode(attrNO1);

                    if (blGroupBankingEnabled) {
                        Element elLoanType2 = doc.createElement("LOAN_TYPE");
                        elLoanType2.setTextContent("Group Loan");
                        elLoanTypes.appendChild(elLoanType2);

                        // set attribute NO to LOAN element
                        Attr attrNO2 = doc.createAttribute("TYPE");
                        attrNO2.setValue("GROUP_LOAN");
                        elLoanType2.setAttributeNode(attrNO2);
                    }
                }


                Element elLoans = doc.createElement("LOANS");
                elData.appendChild(elLoans);


                for (int i = 0; i < nlLoans.getLength(); i++) {
                    String strLoanNo = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                    String strLoanName = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                    String strLoanBalance = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                    Element elLoan = doc.createElement("LOAN");
                    elLoan.setTextContent(strLoanName);
                    elLoan.setAttribute("SERIAL_NO", strLoanNo);
                    elLoan.setAttribute("AMOUNT", strLoanBalance);
                    elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                    elLoan.setAttribute("BALANCE", strLoanBalance);
                    elLoans.appendChild(elLoan);
                }


                if (blGroupBankingEnabled) {
                    Element elLoan1 = doc.createElement("GROUP");
                    elLoan1.setTextContent("Bidii Youth Group");
                    elLoanTypes.appendChild(elLoan1);

                    // set attribute NO to LOAN element
                    Attr attrNO1 = doc.createAttribute("NO");
                    attrNO1.setValue("G0001");
                    elLoan1.setAttributeNode(attrNO1);

                    Element elLoan2 = doc.createElement("GROUP");
                    elLoan2.setTextContent("Umoja Youth Group");
                    elLoanTypes.appendChild(elLoan2);

                    // set attribute NO to LOAN element
                    Attr attrNO2 = doc.createAttribute("NO");
                    attrNO2.setValue("G0002");
                    elLoan2.setAttributeNode(attrNO2);
                }

                if (blGroupBankingEnabled) {
                    for (int i = 0; i < nlLoans.getLength(); i++) {
                        String strLoanNo = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                        String strLoanName = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                        String strLoanBalance = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                        Element elLoan = doc.createElement("GROUP_LOAN");
                        elLoan.setTextContent("Bidii " + strLoanName);
                        elLoan.setAttribute("SERIAL_NO", strLoanNo);
                        elLoan.setAttribute("AMOUNT", strLoanBalance);
                        elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                        elLoan.setAttribute("BALANCE", strLoanBalance);
                        elLoan.setAttribute("GROUP_ID", "G0001");
                        elLoans.appendChild(elLoan);
                    }

                    for (int i = 0; i < nlLoans.getLength(); i++) {
                        String strLoanNo = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                        String strLoanName = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                        String strLoanBalance = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                        Element elLoan = doc.createElement("GROUP_LOAN");
                        elLoan.setTextContent("Umoja " + strLoanName);
                        elLoan.setAttribute("SERIAL_NO", strLoanNo);
                        elLoan.setAttribute("AMOUNT", strLoanBalance);
                        elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                        elLoan.setAttribute("BALANCE", strLoanBalance);
                        elLoan.setAttribute("GROUP_ID", "G0002");
                        elLoans.appendChild(elLoan);
                    }
                }

                /*TODO END: GROUP BANKING TEST ITEMS - REMOVE WHEN DONE:-------------------------------------------------------------------------------------------------------*/

                Element elRepaymentOptions = doc.createElement("REPAYMENT_OPTIONS");
                //if it is not enabled then the default repayment option is savings account
                elRepaymentOptions.setAttribute("ENABLED", "TRUE");

                Element elRepaymentOption1 = doc.createElement("OPTION");
                elRepaymentOption1.setAttribute("VALUE", "SAVINGS_ACCOUNT");
                elRepaymentOption1.setAttribute("TYPE", "ACCOUNT");
                elRepaymentOption1.setTextContent("Savings Account");
                elRepaymentOptions.appendChild(elRepaymentOption1);

                Element elRepaymentOption2 = doc.createElement("OPTION");
                elRepaymentOption2.setAttribute("VALUE", "MPESA");
                elRepaymentOption2.setAttribute("TYPE", "MPESA");
                elRepaymentOption2.setTextContent("Safaricom M-Pesa");
                elRepaymentOptions.appendChild(elRepaymentOption2);

                elData.appendChild(elRepaymentOptions);

                String strAccountsXML = CBSAPI.getSavingsAccountList(strUsername, true, "WITHDRAWABLE_ACCOUNTS");

                InputSource sourceForAccounts = new InputSource(new StringReader(strAccountsXML));
                DocumentBuilderFactory builderFactoryForAccounts = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForAccounts = builderFactoryForAccounts.newDocumentBuilder();
                Document xmlDocumentForAccounts = builderForAccounts.parse(sourceForAccounts);

                NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocumentForAccounts, XPathConstants.NODESET)).item(0).getChildNodes();


                Element elAccounts = doc.createElement("ACCOUNTS");
                elData.appendChild(elAccounts);

                for (int i = 0; i < nlAccounts.getLength(); i++) {
                    String strAccountNo = configXPath.evaluate("AccNo", nlAccounts.item(i)).trim();
                    String strAccountName = configXPath.evaluate("AccName", nlAccounts.item(i)).trim();

                    Element elAccount = doc.createElement("ACCOUNT");
                    elAccount.setTextContent(strAccountName);
                    elAccounts.appendChild(elAccount);

                    // set attribute NO to ACCOUNT element
                    Attr attrNO = doc.createAttribute("NO");
                    attrNO.setValue(strAccountNo);
                    elAccount.setAttributeNode(attrNO);
                }

                String strMin = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMinimum();
                String strMax = getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMaximum();

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(strMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(strMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);
            } else {
                elData.setTextContent("No Loans Found");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getLoanTypes(MAPPRequest theMAPPRequest) {

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

            Node ndRequestMSG = theMAPPRequest.getMSG();
            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            String strLoanSourcesXML = "<Sources><Source><Code>FOSA</Code><Name>FOSA Loans</Name></Source><Source><Code>FOSA</Code><Name>FOSA Loans</Name></Source></Sources>";

            //todo:add accounts parameter below
            String strLoansXML = CBSAPI.getMobileLoanList(strUsername, "ALL", strAccountNo);

            System.out.println("strLoansXML: " + strLoansXML);

            String strLoanBranchesXML = CBSAPI.getBranches();

            String strLoanPurposesXML = CBSAPI.getLoanPurpose();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (!strLoanSourcesXML.equals("")) {
                Element elLoanSources = docBuilder.parse(new ByteArrayInputStream(strLoanSourcesXML.getBytes())).getDocumentElement();
                Node ndLoanSources = doc.importNode(elLoanSources, true);
                //elData.appendChild(ndLoanSources);
            }

            if (!Objects.equals(strLoansXML, "")) {
                Element elLoans = docBuilder.parse(new ByteArrayInputStream(strLoansXML.getBytes())).getDocumentElement();
                Node ndLoans = doc.importNode(elLoans, true);
                elData.appendChild(ndLoans);
            }

            if (!Objects.equals(strLoanPurposesXML, "")) {
                Element elLoanPurposes = docBuilder.parse(new ByteArrayInputStream(strLoanPurposesXML.getBytes())).getDocumentElement();
                Node ndLoanPurposes = doc.importNode(elLoanPurposes, true);
                elData.appendChild(ndLoanPurposes);
            }

            if (!Objects.equals(strLoanBranchesXML, "")) {
                Element elLoanBranches = docBuilder.parse(new ByteArrayInputStream(strLoanBranchesXML.getBytes())).getDocumentElement();
                Node ndLoanBranches = doc.importNode(elLoanBranches, true);
                elData.appendChild(ndLoanBranches);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse accountBalanceEnquiry(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='ACCOUNT_BALANCE' VERSION='1.01'>
                      <ACCOUNT_NO>123456</ACCOUNT_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;

            String strEntryCode = fnModifyMAPPSessionID(theMAPPRequest);

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            String strAccountBalanceEnquiryXML = CBSAPI.accountBalanceEnquiryMobileApp(strEntryCode, strMAPPSessionID, strUsername, strPassword, strAccountNo);

            /*
            <Balances>
                <Account>
                    <Product>FOSA Savings</Product>
                    <Date>02/09/18</Date>
                    <BookBalance>17,786.05</BookBalance>
                    <AvailableBalance>13,748.05</AvailableBalance>
                </Account>
            </Balances>
             */

            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            InputSource source = new InputSource(new StringReader(strAccountBalanceEnquiryXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            String strProduct = String.valueOf(configXPath.evaluate("Balances/Account/Product", xmlDocument, XPathConstants.STRING));
            String strDate = String.valueOf(configXPath.evaluate("Balances/Account/Date", xmlDocument, XPathConstants.STRING));
            String strBookBalance = String.valueOf(configXPath.evaluate("Balances/Account/BookBalance", xmlDocument, XPathConstants.STRING));
            String strAvailableBalance = String.valueOf(configXPath.evaluate("Balances/Account/AvailableBalance", xmlDocument, XPathConstants.STRING));

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            if (!strProduct.equals("")) {
                strTitle = strProduct;
                strResponseText = "Your account balance is: <b>KES " + strBookBalance + "</b>" + "<br/>Available balance: <b>KES " + strAvailableBalance + "</b>";
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                strCharge = "YES";
            } else {
                strTitle = "ERROR: Account Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";
            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Account Balance</TITLE>
                    <DATA TYPE='TEXT'>Your account balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse checkMyBeneficiaries(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='ACCOUNT_BALANCE' VERSION='1.01'>
                      <ACCOUNT_NO>123456</ACCOUNT_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;

            String strEntryCode = fnModifyMAPPSessionID(theMAPPRequest);

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            String strAccountBalanceEnquiryXML = CBSAPI.accountBalanceEnquiryMobileApp(strEntryCode, strMAPPSessionID, strUsername, strPassword, strAccountNo);

            String strCharge = "";
            String strTitle = "";
            String strResponseText = "";

            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            String strBeneficiariesXML = "<TABLE><TR><TH>No</TH><TH>Name</TH><TH></TH></TR><TR><TD>1</TD><TD>John Juma</TD><TD></TD></TR><TR><TD>2</TD><TD>Jane Juma</TD><TD></TD></TR><TR><TD>3</TD><TD>James Juma</TD><TD></TD></TR></TABLE>";

            Element elBeneficiaries = docBuilder.parse(new ByteArrayInputStream(strBeneficiariesXML.getBytes())).getDocumentElement();
            Node ndBeneficiaries = doc.importNode(elBeneficiaries, true);

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            if (!strBeneficiariesXML.equals("")) {
                elData.appendChild(ndBeneficiaries);
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
            } else {
                strTitle = "ERROR: Account Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanBalanceEnquiry(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='LOAN_BALANCE' VERSION='1.01'>
                      <LOAN_NO>123456</LOAN_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();

            String strLoansXML = CBSAPI.getMemberLoanListMobileApp(strUsername);

             /*
             //Response from NAV is:
            <Loans>
                <Product>
                    <LoanNo>BLN-55740</LoanNo>
                    <Type>School Fees Loan</Type>
                    <LoanBalance>17,163.07</LoanBalance>
                </Product>
                <Product>
                    <LoanNo>BLN-63695</LoanNo>
                    <Type>BELA Loan</Type>
                    <LoanBalance>19,969.31</LoanBalance>
                </Product>
            </Loans>
             */

            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlLoans = ((NodeList) configXPath.evaluate("Loans/Product", xmlDocument, XPathConstants.NODESET));

            String strCharge = "NO";
            String strLoanBalance = "";
            String strLoanName = "";

            for (int i = 0; i < nlLoans.getLength(); i++) {
                String strLoanId = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                String strLoanNameForLoan = configXPath.evaluate("Type", nlLoans.item(i)).trim();
                String strLoanBalanceForLoan = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                if (strLoanId.equals(strLoanNo)) {
                    strLoanName = strLoanNameForLoan;
                    strLoanBalance = strLoanBalanceForLoan;
                    break;
                }
            }


            String strTitle = "";
            String strResponseText = "";

            if (!strLoanBalance.equals("")) {
                strTitle = strLoanName;
                strResponseText = "Your loan balance is: <b>KES " + strLoanBalance + "</b>";
                strCharge = "YES";
            } else {
                strTitle = "ERROR: Loan Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                enResponseAction = MAPPConstants.ResponseAction.END;

            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Loan Balance</TITLE>
                    <DATA TYPE='TEXT'>Your loan balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse disableATMCard(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='LOAN_BALANCE' VERSION='1.01'>
                      <LOAN_NO>123456</LOAN_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strATMCardID = configXPath.evaluate("ATM_CARD_ID", ndRequestMSG).trim();
            String strAction = configXPath.evaluate("ACTION", ndRequestMSG).trim();

            String strResponse = "SUCCESS";

            String strTitle = "";
            String strResponseText = "";

            if (strResponse.equals("SUCCESS")) {
                strTitle = "ATM Card Disabled";
                strResponseText = "Your request to disable ATM card " + strATMCardID + " was received successfully. You will receive an SMS confirmation shortly";
            } else {
                strTitle = "ERROR: Disable ATM Card";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                enResponseAction = MAPPConstants.ResponseAction.CON;

            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Loan Balance</TITLE>
                    <DATA TYPE='TEXT'>Your loan balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, "YES", strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse checkLoanLimit(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                 <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='LOAN_BALANCE' VERSION='1.01'>
                      <LOAN_NO>123456</LOAN_NO>
                </MSG>
            </MESSAGES>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            String strLoanLimit = CBSAPI.getLoanLimitMobileApp(strUsername, strLoanNo, strAccountNo);

             /*
             //Response from NAV is:
            <Loans>
                <Product>
                    <LoanNo>BLN-55740</LoanNo>
                    <Type>School Fees Loan</Type>
                    <LoanBalance>17,163.07</LoanBalance>
                </Product>
                <Product>
                    <LoanNo>BLN-63695</LoanNo>
                    <Type>BELA Loan</Type>
                    <LoanBalance>19,969.31</LoanBalance>
                </Product>
            </Loans>
             */

            String strCharge = "NO";
            String strTitle = "";
            String strResponseText = "";

            if (!strLoanLimit.equals("")) {
                strTitle = "Request Received Successfully";
                strResponseText = strLoanLimit;
                strCharge = "YES";
            } else {
                strTitle = "ERROR: Check Loan Limit";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                enResponseAction = MAPPConstants.ResponseAction.END;
            }

             /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <MSG SESSION_ID='123121' TYPE='MOBILE_BANKING' ACTION='CON' STATUS='SUCCESS' CHARGE='YES'>
                    <TITLE>Loan Balance</TITLE>
                    <DATA TYPE='TEXT'>Your loan balance is KES 5,100.00</DATA>
                </MSG>
            </MESSAGES>
             */

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }


    public MAPPResponse mobileMoneyWithdrawal(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {

                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);

                long lnSessionID = theMAPPRequest.getSessionID();

                String strTraceID = theMAPPRequest.getTraceID();

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strRecipientMobileNumber = configXPath.evaluate("MOBILE_NO", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                String strEnteredMobileNumber = strRecipientMobileNumber;
                strRecipientMobileNumber = APIUtils.sanitizePhoneNumber(strRecipientMobileNumber);

                /*if (strRecipientMobileNumber.equalsIgnoreCase("INVALID_MOBILE_NUMBER")) {
                    strTitle = "ERROR: Withdrawal Failed";
                    strResponseText = "The format of the mobile number you entered is invalid (" + strEnteredMobileNumber + ")</br>Please use the format 07XX XXX XXX";
                    enResponseAction = MAPPConstants.ResponseAction.CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {

                }*/

                MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strAccountNo, RegisterConstants.MemberRegisterType.BLACKLIST);

                double dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());


                if (!strRecipientMobileNumber.equals(strUsername)) {
                    dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL_TO_OTHER).getMinimum());
                    dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL_TO_OTHER).getMaximum());
                }

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) > dblWithdrawalMax) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (registerResponse.getResponseType().equals(ke.skyworld.mbanking.ussdapi.APIConstants.RegisterViewResponse.VALID.getValue())) {
                    strTitle = "ERROR: Cash Withdrawal";
                    strResponseText = "Dear member, your account is not allowed to perform this transaction.\nPlease contact us for more information.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    String strTransaction = "Withdrawal Request";
                    String strTransactionDescription = "Cash Withdrawal by " + strUsername + " to " + strRecipientMobileNumber;
                    strTransactionDescription = PESAAPI.shortenName(strTransactionDescription);
                    XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, ke.skyworld.mbanking.pesaapi.PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strMAPPSessionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2C);
                    pesa.setCommand("BusinessPayment");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
                    //pesa.setChargeProposed(null);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    //pesa.setInitiatorName(""); - Set after getting name from CBS
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strAccountNo);
                    pesa.setSourceAccount(strAccountNo);
                    //pesa.setSourceName(""); - Set after getting name from CBS
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("MSISDN");
                    pesa.setReceiverIdentifier(strRecipientMobileNumber);
                    pesa.setReceiverAccount(strRecipientMobileNumber);
                    //pesa.setReceiverName(""); - Set after getting name from CBS
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strRecipientMobileNumber);
                    pesa.setBeneficiaryAccount(strRecipientMobileNumber);
                    //pesa.setBeneficiaryName(""); - Set after getting name from CBS
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setCategory("CASH_WITHDRAWAL");

                    pesa.setPriority(200);
                    pesa.setSendCount(0);

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setPESAXMLData("<DATA/>");

                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);
                    boolean isOtherNumber= false;

                    if(strUsername.equals(strRecipientMobileNumber)){
                        isOtherNumber = true;
                    }

                    String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strMAPPSessionID, strMAPPSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountNo, bdAmount, strUsername, strPassword, "MAPP", strMAPPSessionID, "MBANKING", strRecipientMobileNumber, strRecipientMobileNumber, "M-Pesa",isOtherNumber,strRecipientMobileNumber);

                    String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

                    System.out.println("Withdrawal Request Result:" + strWithdrawalStatus);

                    switch (arrWithdrawalStatus[0]) {
                        case "SUCCESS": {
                            String strMemberName = arrWithdrawalStatus[1].trim();
                            pesa.setSourceName(strMemberName);

                            if (strRecipientMobileNumber.equalsIgnoreCase(strUsername)) {
                                pesa.setReceiverName(strMemberName);
                                pesa.setBeneficiaryName(strMemberName);
                            } else {
                                pesa.setReceiverName(strRecipientMobileNumber);
                                pesa.setBeneficiaryName(strRecipientMobileNumber);
                            }
                            pesa.setInitiatorName(strMemberName);

                            if (PESAProcessor.sendPESA(pesa) > 0) {
                                strAmount = Utils.formatAmount(strAmount);
                                strCharge = "YES";
                                strTitle = "Request for Withdrawal";
                                strResponseText = "Your request to withdraw <b>KES " + strAmount + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;

                                CBSAPI.reverseWithdrawalRequest(strMAPPSessionID);
                            }
                            break;
                        }
                        case "INCORRECT_PIN": {
                            strTitle = "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT": {
                            strTitle = "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle = "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE": {
                            strTitle = "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS": {
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED": {
                            strTitle = "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default: {
                            System.err.println("DEFAULT ON SWITCH -> " + this.getClass().getSimpleName() + "." + new Object() {
                            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + strWithdrawalStatus);
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse buyAirtime(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);

                String strTraceID = theMAPPRequest.getTraceID();

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strRecipientMobileNumber = configXPath.evaluate("MOBILE_NO", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                strRecipientMobileNumber = APIUtils.sanitizePhoneNumber(strRecipientMobileNumber);

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strAccountNo, RegisterConstants.MemberRegisterType.BLACKLIST);

                double dblUtilityETopUpMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMinimum());
                double dblUtilityETopUpMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "Please enter a valid amount for airtime purchase";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblUtilityETopUpMin) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblUtilityETopUpMin), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) > dblUtilityETopUpMax) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblUtilityETopUpMax), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (registerResponse.getResponseType().equals(ke.skyworld.mbanking.ussdapi.APIConstants.RegisterViewResponse.VALID.getValue())) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "Dear member, your account is not allowed to perform this transaction.\nPlease contact us for more information.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (!strRecipientMobileNumber.equals(strUsername)) {
                    strTitle = "ERROR: Buy Airtime";
                    strResponseText = "Airtime purchase for other number is not enabled";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();

                    String strTransaction = "Airtime Request";
                    String strTransactionDescription = "Airtime Purchase by " + strRecipientMobileNumber;

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.AIRTIME);

                    long getProductID = Long.parseLong(pesaParam.getProductId());

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strMAPPSessionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2C);
                    pesa.setCommand("E-TOPUP");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
                    //pesa.setChargeProposed(null);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    //pesa.setInitiatorName(""); - Set after getting name from CBS
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strAccountNo);
                    pesa.setSourceAccount(strAccountNo);
                    //pesa.setSourceName(""); - Set after getting name from CBS
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("MSISDN");
                    pesa.setReceiverIdentifier(strRecipientMobileNumber);
                    pesa.setReceiverAccount(strRecipientMobileNumber);
                    //pesa.setReceiverName(""); - Set after getting name from CBS
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strRecipientMobileNumber);
                    pesa.setBeneficiaryAccount(strRecipientMobileNumber);
                    //pesa.setBeneficiaryName(""); - Set after getting name from CBS
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setCategory("AIRTIME_PURCHASE");

                    pesa.setPriority(200);
                    pesa.setSendCount(0);

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setPESAXMLData("<DATA/>");

                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();
                    boolean isOtherNumber= false;

                    String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strMAPPSessionID, strMAPPSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountNo, bdAmount, strUsername, strPassword, "MAPP", strMAPPSessionID, "MBANKING", strRecipientMobileNumber, strRecipientMobileNumber, "Safaricom Airtime",isOtherNumber,"");
                    String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

                    System.out.println("Buy Airtime Request Result:" + strWithdrawalStatus);

                    switch (arrWithdrawalStatus[0]) {
                        case "SUCCESS": {
                            String strMemberName = arrWithdrawalStatus[1].trim();
                            pesa.setSourceName(strMemberName);
                            pesa.setInitiatorName(strMemberName);

                            if (strRecipientMobileNumber.equalsIgnoreCase(strUsername)) {
                                pesa.setReceiverName(strMemberName);
                                pesa.setBeneficiaryName(strMemberName);
                            } else {
                                pesa.setReceiverName(strRecipientMobileNumber);
                                pesa.setBeneficiaryName(strRecipientMobileNumber);
                            }

                            if (PESAProcessor.sendPESA(pesa) > 0) {
                                strAmount = Utils.formatAmount(strAmount);
                                strCharge = "YES";
                                strTitle = "Request for Airtime Top-up";
                                strResponseText = "Your request to top up airtime of <b>KES " + strAmount + "</b><br/>For :<b>+" + strUsername + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;

                                CBSAPI.reverseWithdrawalRequest(strMAPPSessionID);
                            }
                            break;
                        }
                        case "INCORRECT_PIN": {
                            strTitle = "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT": {
                            strTitle = "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle = "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE": {
                            strTitle = "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS": {
                            strTitle = "ERROR: Airtime Purchase Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED": {
                            strTitle = "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default: {
                            System.err.println("DEFAULT ON SWITCH: " + this.getClass().getSimpleName() + "." + new Object() {
                            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + strWithdrawalStatus);
                            strTitle = "ERROR: Airtime Purchase Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }



    public MAPPResponse bankTransferViaB2B(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);

                String strTraceID = theMAPPRequest.getTraceID();

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strFromAccountNo = configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();
                String strBank = configXPath.evaluate("BANK", ndRequestMSG).trim();
                String strBankName = configXPath.evaluate("BANK_NAME", ndRequestMSG).trim();
                String strReceiverBankAccountNumber = configXPath.evaluate("BANK_ACCOUNT_NO", ndRequestMSG).trim();
                String strReceiverBankAccountName = configXPath.evaluate("BANK_ACCOUNT_NAME", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();


                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strFromAccountNo, RegisterConstants.MemberRegisterType.BLACKLIST);

                double dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) > dblWithdrawalMax) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (registerResponse.getResponseType().equals(ke.skyworld.mbanking.ussdapi.APIConstants.RegisterViewResponse.VALID.getValue())) {
                    strTitle = "ERROR: Bank Transfer";
                    strResponseText = "Dear member, your account is not allowed to perform this transaction.\nPlease contact us for more information.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();

                    String strTransaction = "Bank Transfer Request";
                    String strTransactionDescription = "B2B Bank Transfer to " + strReceiverBankAccountNumber;

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

                    long getProductID = Long.parseLong(pesaParam.getProductId());

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strMAPPSessionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2B);
                    pesa.setCommand("BusinessPayBill");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
                    //pesa.setChargeProposed(null);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    //pesa.setInitiatorName(""); - Set after getting name from CBS
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strFromAccountNo);
                    pesa.setSourceAccount(strFromAccountNo);
                    //pesa.setSourceName(""); - Set after getting name from CBS
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("SHORT_CODE");
                    pesa.setReceiverIdentifier(strBank);
                    pesa.setReceiverAccount(strReceiverBankAccountNumber);
                    pesa.setReceiverName(strBankName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strUsername);
                    pesa.setBeneficiaryAccount(strUsername);
                    pesa.setBeneficiaryName(strReceiverBankAccountName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setCategory("EXTERNAL_BANK_TRANSFER");

                    pesa.setPriority(200);
                    pesa.setSendCount(0);

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setPESAXMLData("<DATA/>");

                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();
                    boolean isOtherNumber= false;


                    String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strMAPPSessionID, strMAPPSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strFromAccountNo, bdAmount, strUsername, strPassword, "MAPP", strMAPPSessionID, "MBANKING", strReceiverBankAccountNumber, strReceiverBankAccountNumber, strBankName,isOtherNumber,"");

                    String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

                    System.out.println("NAV Request Result:" + strWithdrawalStatus);

                    switch (arrWithdrawalStatus[0]) {
                        case "SUCCESS": {
                            String strMemberName = arrWithdrawalStatus[1].trim();
                            pesa.setSourceName(strMemberName);
                            pesa.setInitiatorName(strMemberName);

                            if (PESAProcessor.sendPESA(pesa) > 0) {
                                strAmount = Utils.formatAmount(strAmount);
                                strCharge = "YES";
                                strTitle = "Bank Transfer";
                                strResponseText = "Your request to transfer <b>KES " + strAmount + "</b> to has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;

                                CBSAPI.reverseWithdrawalRequest(strMAPPSessionID);
                            }
                            break;
                        }
                        case "INCORRECT_PIN": {
                            strTitle = "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT": {
                            strTitle = "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle = "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE": {
                            strTitle = "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS": {
                            strTitle = "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED": {
                            strTitle = "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default: {
                            System.err.println("DEFAULT ON SWITCH -> " + this.getClass().getSimpleName() + "." + new Object() {
                            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + strWithdrawalStatus);
                            strTitle = "ERROR: Bank Transfer Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }


    public MAPPResponse depositMoney(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);
            String strSender = pesaParam.getSenderIdentifier();
            /*
            <MSG SESSION_ID='12234' ORG_ID='12' TYPE='MOBILE_BANKING' ACTION='DEPOSIT_MONEY' VERSION='1.01'>"+
                <AMOUNT ACCOUNT_NO='1234567890'>1000</AMOUNT>
            </MSG>
            */
            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strAccountNo = configXPath.evaluate("AMOUNT/@ACCOUNT_NO", ndRequestMSG).trim();
            String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
            BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

            String strReceiver = strUsername;
            String strReceiverDetails = strReceiver;
            double lnAmount = Utils.stringToDouble(strAmount);
            String strReference = strUsername;
            String strTraceID = theMAPPRequest.getTraceID();

            boolean blPesaStkPushStatus = false;

            PESAAPI thePESAAPI = new PESAAPI();
            blPesaStkPushStatus = thePESAAPI.pesa_C2B_Request(
                    strMAPPSessionID,
                    strReceiver,
                    strReceiverDetails,
                    strAccountNo,
                    "KES",
                    lnAmount,
                    "DEPOSIT",
                    strReference,
                    "MAPP",
                    "MBANKING",
                    strTraceID,
                    strSessionID
            );

            String strResponseText = "";
            String strTitle = "";
            String strCharge = "NO";

            if (blPesaStkPushStatus) {
                strTitle = "Deposit Request";
                strResponseText = "You will be prompted by M-PESA for payment<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strAccountNo + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";
            } else {
                strTitle = "ERROR: Deposit Request";
                strResponseText = "Use the details below to pay via M-PESA<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strAccountNo + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";

                enResponseAction = CON;
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            //End USSD.

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanRepayment(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("loanRepayment");
            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);
            String strSender = pesaParam.getSenderIdentifier();
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='PAY_LOAN' VERSION='1.01'>
                    <AMOUNT LOAN_SERIAL_NO='12345'>123456</AMOUNT>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanId = configXPath.evaluate("AMOUNT/@LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
            String strRepaymentOption = configXPath.evaluate("REPAYMENT_OPTION", ndRequestMSG).trim();
            String strAccount = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            System.out.println("Payment option: " + strRepaymentOption);

            switch (strRepaymentOption) {
                case "MPESA": {
                    String strReceiver = strUsername;
                    String strReceiverDetails = strReceiver;
                    double lnAmount = Utils.stringToDouble(strAmount);
                    String strReference = strUsername;

                    boolean blPesaStkPushStatus = false;

                    String strTraceID = theMAPPRequest.getTraceID();

                    PESAAPI thePESAAPI = new PESAAPI();
                    blPesaStkPushStatus = thePESAAPI.pesa_C2B_Request(
                            strMAPPSessionID,
                            strReceiver,
                            strReceiverDetails,
                            strLoanId,
                            "KES",
                            lnAmount,
                            "LOAN_REPAYMENT",
                            strReference,
                            "MAPP",
                            "MBANKING",
                            strTraceID,
                            strSessionID
                    );

                    String strResponseText = "";
                    String strTitle = "";
                    String strCharge = "NO";

                    if (blPesaStkPushStatus) {
                        strTitle = "Deposit Request";
                        strResponseText = "You will be prompted by M-PESA for payment<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strLoanId + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";
                    } else {
                        strTitle = "ERROR: Deposit Request";
                        strResponseText = "Use the details below to pay via M-PESA<br/>Paybill no: <b>" + strSender + "</b><br/>" + "A/C: <b>" + strLoanId + "</b><br/>" + "Amount: <b>KES " + strAmount + "</b>";

                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    }

                    //End USSD.

                    Element elData = doc.createElement("DATA");
                    elData.setTextContent(strResponseText);

                    generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                    //Response
                    Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                    theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
                    break;
                }
                //case "Savings Account": {
                default: {
                    BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                    boolean blPayLoan = true;

                    String strDestination = "";

                    String strFundsTransferStatus = CBSAPI.accountTransfer_SOURCEACCOUNT(strMAPPSessionID, strMAPPSessionID, strUsername, strLoanId, strDestination, bdAmount, strPassword, blPayLoan, false, strAccount);

                    String strTitle = "";
                    String strResponseText = "";

                    String strCharge = "NO";

                    switch (strFundsTransferStatus) {
                        case "SUCCESS": {
                            strTitle = "Transaction Accepted";
                            strResponseText = "Your loan repayment request has been accepted successfully. Kindly wait as it is being processed";
                            strCharge = "YES";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                            break;
                        }
                        case "ERROR": {
                            strTitle = "Transaction Error";
                            strResponseText = "An error occurred while making your request for funds transfer. Please try again.";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            break;
                        }
                        case "INSUFFICIENT_BAL": {
                            strTitle = "Insufficient Balance";
                            strResponseText = "Error, you do not have sufficient balance in your account to complete this request";
                            enResponseAction = CON;
                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            break;
                        }
                        case "ACC_NOT_FOUND": {
                            strTitle = "Account Not Found";
                            strResponseText = "Error, your account could not be found, please try again";
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            break;
                        }
                        default: {
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                            strTitle = "ERROR: Loan Repayment";
                            strResponseText = "An error occurred. Please try again after a few minutes.";
                        }
                    }

                    Element elData = doc.createElement("DATA");
                    elData.setTextContent(strResponseText);

                    generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                    //Response
                    Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                    theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
        }

        return theMAPPResponse;
    }

    public MAPPResponse fundsTransfer(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("fundsTransfer");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
//Request
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);
                String strAppID = theMAPPRequest.getAppID();

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                // Root element - MSG
                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;
                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

                String strFromAccountNo = configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();
                String strToAccountNo = configXPath.evaluate("TO_ACCOUNT_NO", ndRequestMSG).trim();
                String strToOption = configXPath.evaluate("TRANSFER_OPTION", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

                boolean blPayLoan = false;

                String strDestination = "ACCOUNT";

                if (strToOption.equals("ID Number")) {
                    strDestination = "ID";
                } else if (strToOption.equals("Mobile Number")) {
                    strDestination = "Mobile";
                }

                String strFundsTransferStatus = CBSAPI.accountTransfer_SOURCEACCOUNT(fnModifyMAPPSessionID(theMAPPRequest) + "T", strMAPPSessionID + "T", strUsername, strToAccountNo, strDestination, bdAmount, strPassword, blPayLoan, false, strFromAccountNo);

                String strTitle = "";
                String strResponseText = "";

                String strCharge = "NO";

                switch (strFundsTransferStatus) {
                    case "SUCCESS": {
                        strTitle = "Transaction Accepted";
                        strResponseText = "Your funds transfer request has been accepted successfully. Kindly wait as it is being processed";
                        strCharge = "YES";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                        break;
                    }
                    case "ERROR": {
                        strTitle = "Transaction Error";
                        strResponseText = "An error occurred while making your request for funds transfer. Please try again.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "INSUFFICIENT_BAL": {
                        strTitle = "Insufficient Balance";
                        strResponseText = "Error, you do not have sufficient balance in your account to complete this request";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "ACC_NOT_FOUND": {
                        strTitle = "Account Not Found";
                        strResponseText = "Error, your account could not be found, please try again";
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    default: {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        strTitle = "ERROR: Funds Transfer";
                        strResponseText = "An error occurred. Please try again after a few minutes.";
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse accountStatement(MAPPRequest theMAPPRequest) {

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

            int intStartDateDay = Integer.parseInt(strStartDate.split("-")[2]);
            int intStartDateMonth = Integer.parseInt(strStartDate.split("-")[1]);
            int intStartDateYear = Integer.parseInt(strStartDate.split("-")[0]);

            int intEndDateDay = Integer.parseInt(strEndDate.split("-")[2]);
            int intEndDateMonth = Integer.parseInt(strEndDate.split("-")[1]);
            int intEndDateYear = Integer.parseInt(strEndDate.split("-")[0]);

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strAccountsXML = CBSAPI.accountMiniStatementMobileApp(fnModifyMAPPSessionID(theMAPPRequest), strMAPPSessionID, intMaximumTransactionCount, intStartDateDay, intStartDateMonth, intStartDateYear, intEndDateDay, intEndDateMonth, intEndDateYear, strAccountNo, strUsername, strPassword);

            strAccountsXML = strAccountsXML.replaceAll(" & ", " and ");
            strAccountsXML = strAccountsXML.replaceAll("&", "and");

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

                String strAccountBalanceEnquiryXML = CBSAPI.accountBalanceEnquiryMobileApp(fnModifyMAPPSessionID(theMAPPRequest) + "B", strMAPPSessionID + "B", strUsername, strPassword, strAccountNo);
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
             /*
             //Response from NAV is:
            <Accounts>
                <Account>
                    <AccNo>5000000127000</AccNo>
                    <AccName>FOSA Savings Accounts 00</AccName>
                </Account>
                <Account>
                    <AccNo>5000000127001</AccNo>
                    <AccName>FOSA Savings Accounts 01</AccName>
                </Account>
            </Accounts>
             */


            // Root element - MSG


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse accountStatementBase64(MAPPRequest theMAPPRequest) {

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

            int intStartDateDay = Integer.parseInt(strStartDate.split("-")[2]);
            int intStartDateMonth = Integer.parseInt(strStartDate.split("-")[1]);
            int intStartDateYear = Integer.parseInt(strStartDate.split("-")[0]);

            int intEndDateDay = Integer.parseInt(strEndDate.split("-")[2]);
            int intEndDateMonth = Integer.parseInt(strEndDate.split("-")[1]);
            int intEndDateYear = Integer.parseInt(strEndDate.split("-")[0]);

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strStatementBase64 = CBSAPI.accountMiniStatementMobileAppB64(intStartDateDay, intStartDateMonth, intStartDateYear, intEndDateDay, intEndDateMonth, intEndDateYear, strAccountNo);

            String strTitle = "Account Statement";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            Element elData = doc.createElement("DATA");
            String strCharge = "NO";

            if (!strStatementBase64.equals("")) {
                elData.setTextContent(strStatementBase64);
            } else {
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                strCharge = "NO";
                strTitle = "Error: No Statements Found";
                elData.setTextContent("You do not have any statements within this time period");
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse changePassword(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strNewPassword = configXPath.evaluate("NEW_PASSWORD", ndRequestMSG).trim();
            strNewPassword = APIUtils.hashPIN(strNewPassword, strUsername);

            String strFundsTransferStatus = CBSAPI.setNewPin(strUsername, strPassword, strNewPassword);

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            switch (strFundsTransferStatus) {
                case "SUCCESS": {
                    strTitle = "Password Changed Successfully";
                    strResponseText = "Your password has been changed successfully. You will be redirected to the login page.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "INVALID_NEW_PIN": {
                    strTitle = "Invalid New Password";
                    strResponseText = "Please ensure new PIN and its confirmation match and try again";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                case "INCORRECT_PIN": {
                    strTitle = "Incorrect PIN";
                    strResponseText = "Error, the PIN you have entered as current PIN is incorrect, please try again";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    strTitle = "Account Not Found";
                    strResponseText = "Error, your account could not be found, please try again";
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                    break;
                }
                default: {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    strTitle = "ERROR: Change Password";
                    strResponseText = "An error occurred. Please try again after a few minutes.";
                }
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse encryptText(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strClearText = configXPath.evaluate("CLEARTEXT", ndRequestMSG).trim();
            String strTimestamp = configXPath.evaluate("TIMESTAMP", ndRequestMSG).trim();

            String strEncryptedText = strClearText;
            Crypto crypto = new Crypto();
            strEncryptedText = crypto.encrypt(APIUtils.ENCRYPTION_KEY + strTimestamp, strClearText);

            String strTitle = strTitle = "Text Encrypted Successfully";
            String strResponseText = strResponseText = "Text was encrypted successfully.";

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            Element elEncrypted = doc.createElement("ENCRYPTED");
            elEncrypted.setTextContent(strEncryptedText);
            elData.appendChild(elEncrypted);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse decryptText(MAPPRequest theMAPPRequest) {
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

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strEncrypted = configXPath.evaluate("ENCRYPTED", ndRequestMSG).trim();
            String strTimestamp = configXPath.evaluate("TIMESTAMP", ndRequestMSG).trim();

            String strDecryptedText = strEncrypted;
            Crypto crypto = new Crypto();
            strDecryptedText = crypto.decrypt(APIUtils.ENCRYPTION_KEY + strTimestamp, strEncrypted);

            String strTitle = strTitle = "Text Encrypted Successfully";
            String strResponseText = strResponseText = "Text was encrypted successfully.";

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            String[] arStrDecryptedText = strDecryptedText.split("\\|");
            //FUNDS_TRANSFER|254722554433|JOHN DOE|100.00|1593884595611
            //QR_CODE_TYPE|PHONE_NO|FULL_NAME|AMOUNT|ACCOUNT
            //Amount should not have commas

            String strName = arStrDecryptedText[2];
            String strAccountNumber = "";
            String strAccountName = " ";
            String strPhoneNumber = arStrDecryptedText[1];
            String strAmount = arStrDecryptedText[3];
            Element elAccountDetails = null;

            String strType = arStrDecryptedText[0];
            switch (strType) {
                case "CASH_WITHDRAWAL":
                case "BUY_AIRTIME": {
                    elAccountDetails = getAccountElement(strPhoneNumber, "Mobile", doc, "ENCRYPTION");
                    break;
                }
                case "DEPOSIT_MONEY":
                case "FUNDS_TRANSFER": {
                    strAccountNumber = arStrDecryptedText[4];
                    elAccountDetails = getAccountElement(strAccountNumber, "ACCOUNT", doc, "ENCRYPTION");
                    break;
                }
                default: {
                    elAccountDetails = getAccountElement(strPhoneNumber, "Mobile", doc, "ENCRYPTION");
                    break;
                }
            }

            if (elAccountDetails != null) {
                strName = elAccountDetails.getAttribute("NAME");
                strAccountNumber = elAccountDetails.getAttribute("ACCOUNT_NO");
                strAccountName = elAccountDetails.getAttribute("ACCOUNT_NAME");
                strPhoneNumber = elAccountDetails.getAttribute("PHONE_NO");
            }

            strDecryptedText = strType + "|" + strPhoneNumber + "|" + strName + "|" + strAmount + "|" + strAccountNumber + "|" + strAccountName;

            Element elEncrypted = doc.createElement("DECRYPTED");
            elEncrypted.setTextContent(strDecryptedText);
            elData.appendChild(elEncrypted);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".decryptText() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse updateEmailAddress(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;
        System.out.println(this.getClass().getSimpleName() + "." + new Object() {
        }.getClass().getEnclosingMethod().getName());

        try {
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='UPDATE_EMAIL' VERSION='1.01'>
                    <EMAIL_ADDRESS>user@example.com</EMAIL_ADDRESS>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strNewEmailAddress = configXPath.evaluate("EMAIL_ADDRESS", ndRequestMSG).trim();

            boolean blEmailUpdated = CBSAPI.changeEmailAddress(strUsername, strNewEmailAddress, strPassword).equals("SUCCESS");

            String strTitle = "Email Updated Successfully";
            String strResponseText = "Your email address was updated to <b>" + strNewEmailAddress + "</b> successfully";

            if (!blEmailUpdated) {
                strTitle = "Update Failed";
                strResponseText = "An error occurred while trying to update your email address. Please try again later.";

                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse addOrDeleteUtilityAndPaybillAccount(MAPPRequest theMAPPRequest, String theAction) {
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

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strProviderAccountCode = configXPath.evaluate("PROVIDER_ACCOUNT_CODE", ndRequestMSG).trim();
            String strName = configXPath.evaluate("ACCOUNT_NAME", ndRequestMSG).trim();
            String strNumber = configXPath.evaluate("ACCOUNT_NUMBER", ndRequestMSG).trim();

            String strServiceProviderID = null;
            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            LinkedList<LinkedHashMap<String, String>> linkedHashMapLinkedList = new SPManager(strIntegritySecret).getSPAccounts(SPManagerConstants.Condition.YES, SPManagerConstants.Condition.YES, SPManagerConstants.Condition.YES, SPManagerConstants.Condition.YES);

            for (LinkedHashMap<String, String> stringStringLinkedHashMap : linkedHashMapLinkedList) {
                if (stringStringLinkedHashMap.get("provider_account_identifier").equalsIgnoreCase(strProviderAccountCode)) {
                    strServiceProviderID = stringStringLinkedHashMap.get("provider_account_code");
                    break;
                }
            }

            boolean blFundsTransferStatus;

            if (theAction.equalsIgnoreCase("ADD")) {
                long lnFundsTransferStatus = new SPManager(strIntegritySecret).createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN, strUsername, strServiceProviderID, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO, strNumber, strName);
                blFundsTransferStatus = lnFundsTransferStatus > 0;
            } else {
                blFundsTransferStatus = new SPManager(strIntegritySecret).removeUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN, strUsername, strServiceProviderID, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO, strNumber);
            }

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            if (blFundsTransferStatus) {
                strTitle = "Success";
                strResponseText = "Success.";
                strCharge = "YES";
            } else {
                strTitle = "Error";
                strResponseText = "Error";
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberName(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("getMemberName");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.OBJECT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strOption = configXPath.evaluate("OPTION", ndRequestMSG).trim();
            String strAccount = configXPath.evaluate("ACCOUNT", ndRequestMSG).trim();

            String strAccountNumberXML = "";
            String strSource = "Mobile";

            if (strOption.equals("ID Number")) {
                strSource = "ID";
            } else if (strOption.equals("Account Number")) {
                strSource = "ACCOUNT";
            } else if (strOption.equals("Member Number")) {
                strSource = "MEMBER_NO";
            } else {
                strAccount = APIUtils.sanitizePhoneNumber(strAccount);
            }

            String strTitle = "Account Details";

            String strCharge = "NO";
            Element elAccountDetails = getAccountElement(strAccount, strSource, doc, "GET_MEMBER_NAME");

            Element elData = doc.createElement("DATA");

            elData.appendChild(elAccountDetails);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberEmailAddress(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName());

            XPath configXPath = XPathFactory.newInstance().newXPath();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.OBJECT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
            String strUsername = theMAPPRequest.getUsername();

            String strSource = "Mobile";

            String strTitle = "Account Details";

            String strCharge = "NO";
            Element elAccountDetails = getAccountElement(strUsername, strSource, doc, "GET_MEMBER_NAME");

            Element elData = doc.createElement("DATA");

            elData.appendChild(elAccountDetails);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public Element getAccountElement(String theAccount, String theSource, Document doc, String theCategory) {
        try {
            String strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(theAccount, theSource);
            Element elAccountDetails = null;

            if (theSource.equals("Mobile")) {
                theAccount = APIUtils.sanitizePhoneNumber(theAccount);
            }

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strEmailAddress = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strEmailAddress = configXPath.evaluate("Account/Email", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";
            }


            if (theCategory != null) {
                if (theCategory.equals("VALIDATE_PESA_IN")) {
                    if (!strAccountNumberXML.equals("")) {
                        String strBeneficiaryType = "";
                        if (theSource.equals("Mobile")) {
                            strBeneficiaryType = "MSISDN";
                        } else if (theSource.equals("ID")) {
                            strBeneficiaryType = "NATIONAL_ID";
                        }

                        elAccountDetails = doc.createElement("PESA_OTHER_DETAILS");

                        Element elValidationDetails = doc.createElement("VALIDATION_DETAILS");
                        elAccountDetails.appendChild(elValidationDetails);

                        Element elBeneficiary = doc.createElement("BENEFICIARY");
                        elBeneficiary.setAttribute("TYPE", strBeneficiaryType);
                        elValidationDetails.appendChild(elBeneficiary);

                        Element elIdentifier = doc.createElement("IDENTIFIER");
                        elIdentifier.setTextContent(theAccount);
                        elBeneficiary.appendChild(elIdentifier);

                        Element elAccount = doc.createElement("ACCOUNT");
                        elAccount.setTextContent(strAccountNo);
                        elBeneficiary.appendChild(elAccount);

                        Element elName = doc.createElement("NAME");
                        elName.setTextContent(strAccountName);
                        elBeneficiary.appendChild(elName);
                    }
                } else {
                    elAccountDetails = doc.createElement("ACCOUNT");
                    elAccountDetails.setAttribute("STATUS", strAccountStatus);
                    elAccountDetails.setAttribute("ACCOUNT_NO", strAccountNo);
                    elAccountDetails.setAttribute("ACCOUNT_NAME", strAccountType);
                    elAccountDetails.setAttribute("NAME", strAccountName);
                    elAccountDetails.setAttribute("MEMBER_NO", strAccountMemberNo);
                    elAccountDetails.setAttribute("PHONE_NO", strPhoneNo);
                    elAccountDetails.setAttribute("EMAIL_ADDRESS", strEmailAddress);
                }
            }

            return elAccountDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MAPPResponse applyLoan(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("applyLoan");
            /*
            <MESSAGES DATETIME='2014-08-25 22:19:53.0' VERSION='1.01'>
                <LOGIN USERNAME='254721913958' PASSWORD=' 246c15fe971deb81c499281dbe86c1846bb2f336500efb88a8d4f99b66f52b39' IMEI='123456789012345'/>
                <MSG SESSION_ID='123121' ORG_ID='123' TYPE='MOBILE_BANKING' ACTION='INTER_ACCOUNT_TRANSFER' VERSION='1.01'>
                    <FROM_ACCOUNT_NO>123456</FROM_ACCOUNT_NO>
                    <TO_ACCOUNT_NO>654321</TO_ACCOUNT_NO>
                    <TRANSFER_OPTION>ID Number</TRANSFER_OPTION>
                    <AMOUNT>2000</AMOUNT>
                </MSG>
            </MESSAGES>
            */

            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                //Request
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);
                String strAppID = theMAPPRequest.getAppID();
                long lnSessionID = theMAPPRequest.getSessionID();

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                // Root element - MSG
                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;
                MAPPConstants.ResponseAction enResponseAction = CON;
                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

                String strLoanType = configXPath.evaluate("LOAN/TYPE", ndRequestMSG).trim();
                String strLoanDuration = configXPath.evaluate("LOAN/DURATION", ndRequestMSG).trim();
                String strLoanPurpose = configXPath.evaluate("LOAN/PURPOSE", ndRequestMSG).trim();
                String strLoanBranch = configXPath.evaluate("LOAN/BRANCH", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("LOAN/AMOUNT", ndRequestMSG).trim();
                String strPayslipPIN = configXPath.evaluate("LOAN/PAYSLIP_PIN", ndRequestMSG).trim();
                String strAccountNumber = configXPath.evaluate("LOAN/ACCOUNT_NO", ndRequestMSG).trim();

                NodeList nlGuarantors = ((NodeList) configXPath.evaluate("GUARANTORS/GUARANTOR", ndRequestMSG, XPathConstants.NODESET));

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                String strTitle = "";
                String strResponseText = "";

                String strCharge = "NO";

                int intLoanDuration = 0;
                if (strLoanDuration != "") {
                    intLoanDuration = Integer.parseInt(strLoanDuration);
                }

                String strLoanApplicationResponse = CBSAPI.applyLoan_BRANCH(fnModifyMAPPSessionID(theMAPPRequest), strMAPPSessionID, strUsername, strLoanType, bdAmount, strPassword, intLoanDuration, strLoanPurpose, strPayslipPIN, strLoanBranch, strAccountNumber);
                String strLoanApplicationStatus = strLoanApplicationResponse;
                if (strLoanApplicationResponse.contains(":::")) {
                    strLoanApplicationStatus = strLoanApplicationResponse.split(":::")[0];
                }
                switch (strLoanApplicationStatus) {
                    case "SUCCESS": {
                        strTitle = "Request Received Successfully";
                        strResponseText = "Your loan application request was received successfully. You will receive an SMS once the loan has been approved.";
                        strCharge = "YES";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

                        String strLoanEntryNumber = strLoanApplicationResponse.split(":::")[1];

                        if (nlGuarantors != null) {
                            for (int i = 0; i < nlGuarantors.getLength(); i++) {
                                String strName = configXPath.evaluate("@NAME", nlGuarantors.item(i)).trim();
                                String strMobileNumber = configXPath.evaluate("@MOBILE_NUMBER", nlGuarantors.item(i)).trim();
                                CBSAPI.addRemoveMobileLoanGuarantor(Integer.parseInt(strLoanEntryNumber), strMobileNumber, "ADD");
                            }
                        }

                        break;
                    }
                    case "INCORRECT_PIN": {
                        strTitle = "Incorrect PIN";
                        strResponseText = "Error, the PIN you have entered as current PIN is incorrect, please try again";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    case "LOAN_APPLICATION_EXISTS": {
                        strTitle = "Loan Already Exists";
                        strResponseText = "The loan you applied for already exists, please repay the current loan to apply for another one.";
                        enResponseAction = CON;
                        enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                        break;
                    }
                    default: {
                        enResponseAction = MAPPConstants.ResponseAction.END;
                        enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                        strTitle = "ERROR: Apply Loan";
                        strResponseText = "An error occurred. Please try again after a few minutes.";
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
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

    public MAPPResponse loanStatement(MAPPRequest theMAPPRequest) {

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

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();
            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim();
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim();

            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            int intStartDateDay = Integer.parseInt(strStartDate.split("-")[2]);
            int intStartDateMonth = Integer.parseInt(strStartDate.split("-")[1]);
            int intStartDateYear = Integer.parseInt(strStartDate.split("-")[0]);

            int intEndDateDay = Integer.parseInt(strEndDate.split("-")[2]);
            int intEndDateMonth = Integer.parseInt(strEndDate.split("-")[1]);
            int intEndDateYear = Integer.parseInt(strEndDate.split("-")[0]);

            String strLoansXML = CBSAPI.loanMiniStatementMobileApp(
                    fnModifyMAPPSessionID(theMAPPRequest),
                    strMAPPSessionID,
                    10,
                    intStartDateDay,
                    intStartDateMonth,
                    intStartDateYear,
                    intEndDateDay,
                    intEndDateMonth,
                    intEndDateYear,
                    strLoanNo,
                    strUsername,
                    strPassword
            );

            System.out.println("NAV Returned: " + strLoansXML);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Statement";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (!strLoansXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlTransactions = ((NodeList) configXPath.evaluate("Response/Transaction", xmlDocument, XPathConstants.NODESET));

                String strLoanBalanceXML = CBSAPI.getMemberLoanListMobileApp(strUsername);
                InputSource sourceForBalance = new InputSource(new StringReader(strLoanBalanceXML));
                DocumentBuilderFactory builderFactoryForBalance = DocumentBuilderFactory.newInstance();
                DocumentBuilder builderForBalance = builderFactoryForBalance.newDocumentBuilder();
                Document xmlDocumentForBalance = builderForBalance.parse(sourceForBalance);

                NodeList nlLoans = ((NodeList) configXPath.evaluate("Loans/Product", xmlDocumentForBalance, XPathConstants.NODESET));

                String strLoanBalance = "";

                for (int i = 0; i < nlLoans.getLength(); i++) {
                    String strLoanId = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                    String strLoanBalanceForLoan = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                    if (strLoanId.equals(strLoanNo)) {
                        strLoanBalance = strLoanBalanceForLoan;
                        break;
                    }
                }

                Element elBalance = doc.createElement("BALANCE");
                elBalance.setTextContent(strLoanBalance);
                elData.appendChild(elBalance);

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
                elThHeading4.setTextContent("Ref");
                elTrHeading.appendChild(elThHeading4);

                Element elThHeading5 = doc.createElement("TH");
                elThHeading5.setTextContent("Balance");
                elTrHeading.appendChild(elThHeading5);

                for (int i = 0; i < nlTransactions.getLength(); i++) {
                    String strDate = configXPath.evaluate("Date", nlTransactions.item(i)).trim();
                    String strDesc = configXPath.evaluate("Desc", nlTransactions.item(i)).trim();
                    String strAmount = configXPath.evaluate("Amount", nlTransactions.item(i)).trim();
                    String strReference = configXPath.evaluate("Reference", nlTransactions.item(i)).trim();
                    String strBalance = configXPath.evaluate("Balance", nlTransactions.item(i)).trim();

                    Element elTrBody = doc.createElement("TR");
                    elTable.appendChild(elTrBody);

                    Element elTDBody1 = doc.createElement("TD");
                    elTDBody1.setTextContent(strDesc);
                    elTrBody.appendChild(elTDBody1);

                    Element elTDBody2 = doc.createElement("TD");
                    elTDBody2.setTextContent("KES " + strAmount);
                    elTrBody.appendChild(elTDBody2);

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
                elData.setTextContent("No loan transactions found for the specified loan");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanGuarantors(MAPPRequest theMAPPRequest) {

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

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();


            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            String strLoansXML = CBSAPI.getLoanGuarantors(strMAPPSessionID, strLoanNo);

            System.out.println("NAV Returned: " + strLoansXML);

            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);

            NodeList nlTransactions = ((NodeList) configXPath.evaluate("Loan/Security", xmlDocument, XPathConstants.NODESET));

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Guarantors";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            String strLoanBalanceXML = CBSAPI.getMemberLoanListMobileApp(strUsername);
            InputSource sourceForBalance = new InputSource(new StringReader(strLoanBalanceXML));
            DocumentBuilderFactory builderFactoryForBalance = DocumentBuilderFactory.newInstance();
            DocumentBuilder builderForBalance = builderFactoryForBalance.newDocumentBuilder();
            Document xmlDocumentForBalance = builderForBalance.parse(sourceForBalance);

            NodeList nlLoans = ((NodeList) configXPath.evaluate("Loans/Product", xmlDocumentForBalance, XPathConstants.NODESET));

            String strLoanBalance = "";

            for (int i = 0; i < nlLoans.getLength(); i++) {
                String strLoanId = configXPath.evaluate("LoanNo", nlLoans.item(i)).trim();
                String strLoanBalanceForLoan = configXPath.evaluate("LoanBalance", nlLoans.item(i)).trim();

                if (strLoanId.equals(strLoanNo)) {
                    strLoanBalance = strLoanBalanceForLoan;
                    break;
                }
            }

            Element elBalance = doc.createElement("BALANCE");
            elBalance.setTextContent(strLoanBalance);
            elData.appendChild(elBalance);

            Element elTable = doc.createElement("TABLE");
            elData.appendChild(elTable);


            Element elTrHeading = doc.createElement("TR");
            elTable.appendChild(elTrHeading);

            Element elThHeading1 = doc.createElement("TH");
            elThHeading1.setTextContent("Name");
            elTrHeading.appendChild(elThHeading1);

            Element elThHeading2 = doc.createElement("TH");
            elThHeading2.setTextContent("Amount Guaranteed");
            elTrHeading.appendChild(elThHeading2);

            Element elThHeading4 = doc.createElement("TH");
            elThHeading4.setTextContent("Mobile No");
            elTrHeading.appendChild(elThHeading4);

            Element elThHeading3 = doc.createElement("TH");
            elThHeading3.setTextContent("Loan No");
            elTrHeading.appendChild(elThHeading3);

            Element elThHeading5 = doc.createElement("TH");
            elThHeading5.setTextContent("Current Commitment");
            elTrHeading.appendChild(elThHeading5);

            Element elThHeading6 = doc.createElement("TH");
            elThHeading6.setTextContent("Type");
            elTrHeading.appendChild(elThHeading6);

            for (int i = 0; i < nlTransactions.getLength(); i++) {
                String strName = configXPath.evaluate("Name", nlTransactions.item(i)).trim();
                String strAmountGuaranteed = configXPath.evaluate("AmountGuaranteed", nlTransactions.item(i)).trim();
                String strLoanNumber = configXPath.evaluate("LoanNo", nlTransactions.item(i)).trim();
                String strMobileNo = configXPath.evaluate("MobileNo", nlTransactions.item(i)).trim();
                String strCurrentCommitment = configXPath.evaluate("CurrentCommitment", nlTransactions.item(i)).trim();
                String strType = configXPath.evaluate("Type", nlTransactions.item(i)).trim();


                Element elTrBody = doc.createElement("TR");
                elTable.appendChild(elTrBody);

                Element elTDBody1 = doc.createElement("TD");
                elTDBody1.setTextContent(strName);
                elTrBody.appendChild(elTDBody1);

                Element elTDBody2 = doc.createElement("TD");
                elTDBody2.setTextContent("KES " + strAmountGuaranteed);
                elTrBody.appendChild(elTDBody2);

                Element elTDBody4 = doc.createElement("TD");
                elTDBody4.setTextContent(strMobileNo);
                elTrBody.appendChild(elTDBody4);

                Element elTDBody3 = doc.createElement("TD");
                elTDBody3.setTextContent(strLoanNumber);
                elTrBody.appendChild(elTDBody3);

                Element elTDBody5 = doc.createElement("TD");
                elTDBody5.setTextContent("KES " + strCurrentCommitment);
                elTrBody.appendChild(elTDBody5);

                Element elTDBody6 = doc.createElement("TD");
                elTDBody6.setTextContent(strType);
                elTrBody.appendChild(elTDBody6);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse loansGuaranteed(MAPPRequest theMAPPRequest) {

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

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

            String strLoansXML = CBSAPI.getLoansGuaranteed(strMAPPSessionID, strUsername);
            if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                strLoansXML = CBSAPI.getLoanToConfirmGuarantoship(strUsername);
            }

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Guarantors";

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            System.out.println("NAV Returned: " + strLoansXML);

            if (strLoansXML.equalsIgnoreCase("") || strLoansXML.equalsIgnoreCase("NULL")) {
                if (theMAPPRequest.getAction().equalsIgnoreCase("LOANS_GUARANTEED")) {
                    elData.setTextContent("There were no loan found");
                } else if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                    elData.setTextContent("There were no loan guarantorship requests found");
                }
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            } else {
                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);

                NodeList nlTransactions = ((NodeList) configXPath.evaluate("/", xmlDocument, XPathConstants.NODESET));
                NodeList nlTransaction = ((NodeList) configXPath.evaluate("/", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                if (theMAPPRequest.getAction().equalsIgnoreCase("LOANS_GUARANTEED")) {
                    nlTransactions = ((NodeList) configXPath.evaluate("Security/Loan", xmlDocument, XPathConstants.NODESET));
                    nlTransaction = ((NodeList) configXPath.evaluate("Security/Loan", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                } else if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                    nlTransactions = ((NodeList) configXPath.evaluate("Loans/Loan", xmlDocument, XPathConstants.NODESET));
                    nlTransaction = ((NodeList) configXPath.evaluate("Loans/Loan", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                }
            /*<Security>
                <Loan>
                    <LoanNo>BLN-50367</LoanNo>
                    <Loanee>Abdalla Said Aden</Loanee>
                    <MobileNo>+254725683351</MobileNo>
                    <LoanType>Development Loan</LoanType>
                    <GuarantorType>Guarantor</GuarantorType>
                    <IssuedDate>02/24/16</IssuedDate>
                    <EndDate>04/24/20</EndDate>
                    <Status>Performing</Status>
                    <LoanAmount>300,000</LoanAmount>
                    <Installments>48</Installments>
                    <LoanBalance>148,770</LoanBalance>
                    <DefaultedAmount>0</DefaultedAmount>
                    <AmountGuaranteed>0</AmountGuaranteed>
                    <CurrentCommitment>0</CurrentCommitment>
                </Loan>
            </Security>*/

                Element elTable = doc.createElement("TABLE");
                elData.appendChild(elTable);


                Element elTrHeading = doc.createElement("TR");
                elTable.appendChild(elTrHeading);

                for (int k = 0; k < nlTransaction.getLength(); k++) {
                    String strHeadingName = nlTransactions.item(0).getChildNodes().item(k).getNodeName();
                    strHeadingName = splitCamelCase(strHeadingName);
                    Element elThHeading1 = doc.createElement("TH");
                    elThHeading1.setTextContent(strHeadingName);
                    elTrHeading.appendChild(elThHeading1);
                }

                for (int i = 0; i < nlTransactions.getLength(); i++) {
                    Element elTrBody = doc.createElement("TR");
                    elTable.appendChild(elTrBody);

                    for (int j = 0; j < nlTransactions.item(i).getChildNodes().getLength(); j++) {
                        String strBodyValue = "";

                        if (theMAPPRequest.getAction().equalsIgnoreCase("LOANS_GUARANTEED")) {
                            strBodyValue = ((NodeList) configXPath.evaluate("Security/Loan", xmlDocument, XPathConstants.NODESET)).item(i).getChildNodes().item(j).getTextContent();//.item(j).getNodeValue();
                        } else if (theMAPPRequest.getAction().equalsIgnoreCase("LOAN_GUARANTORSHIP_REQUESTS")) {
                            strBodyValue = ((NodeList) configXPath.evaluate("Loans/Loan", xmlDocument, XPathConstants.NODESET)).item(i).getChildNodes().item(j).getTextContent();//.item(j).getNodeValue();
                        }

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strBodyValue);
                        elTrBody.appendChild(elTDBody1);
                    }
                }
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public MAPPResponse updateLoanGuarantorStatus(MAPPRequest theMAPPRequest) {

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

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();
            String strStatus = configXPath.evaluate("STATUS", ndRequestMSG).trim();

            boolean blApproved = strStatus.equalsIgnoreCase("APPROVED");

            String strNavResponse = CBSAPI.actionLoanGuarantorship(strUsername, Integer.parseInt(strLoanNo), strPassword, strStatus);

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            if (strNavResponse.equals("SUCCESS")) {
                strTitle = strStatus.equals("APPROVED") ? "Guarantorship Approved" : "Guarantorship Rejected";
                strResponseText = strStatus.equals("APPROVED") ? "Your request to <b>approve</b> loan guarantorship was received successfully" : "Your request to <b>reject</b> loan guarantorship was received successfully";
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                strCharge = "YES";
            } else {
                strTitle = "ERROR";
                strResponseText = "An error occurred. Please try again after a few minutes.";
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public static String getResponseStatus(String strXML) {
        String strStatus = "";
        try {
            if (!strXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList nlResponse = ((NodeList) configXPath.evaluate("/Response", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                strStatus = nlResponse.item(0).getTextContent();
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.getResponseStatus() ERROR : " + e.getMessage());
        }
        return strStatus;
    }

    public String getValueFromXMLUsingPath(String thePath, String theXML) {
        String rVal = "";
        try {
            if (theXML.equals("")) {
                theXML = MAPPLocalParameters.getClientXMLParameters();
            }

            InputSource source = new InputSource(new StringReader(theXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            rVal = configXPath.evaluate(thePath, xmlDocument, XPathConstants.STRING).toString();
        } catch (Exception e) {
            System.err.println("USSDAPI.getValueFromXMLUsingPath() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public MAPPResponse payBill(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == ke.skyworld.mbanking.mappapi.APIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);

                String strTraceID = theMAPPRequest.getTraceID();

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strMAPPSessionID = fnModifyMAPPSessionID(theMAPPRequest);

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;

                String strFromAccountNo =  configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();
                String strPaybillNo =  configXPath.evaluate("PAYBILL_NO", ndRequestMSG).trim();
                String strPaybillName =  configXPath.evaluate("PAYBILL_NAME", ndRequestMSG).trim();
                String strBillAccountNumber = configXPath.evaluate("BILL_ACCOUNT_NO", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strFromAccountNo, RegisterConstants.MemberRegisterType.BLACKLIST);

                double dblWithdrawalMin = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMaximum());

                if (!strAmount.matches("^[1-9][0-9]*$")) {
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "Please enter a valid amount for withdrawal";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if (Double.parseDouble(strAmount) < dblWithdrawalMin) {
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "MINIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMin), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if(Double.parseDouble(strAmount) > dblWithdrawalMax ){
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "MAXIMUM amount allowed is KES " + Utils.formatDouble(String.valueOf(dblWithdrawalMax), "#,###.##");
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else if(registerResponse.getResponseType().equals(ke.skyworld.mbanking.ussdapi.APIConstants.RegisterViewResponse.VALID.getValue())){
                    strTitle = "ERROR: Pay Bill";
                    strResponseText = "Dear member, your account is not allowed to perform this transaction.\nPlease contact us for more information.";
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                } else {
                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();

                    String strTransaction = "Utility Request";
                    String strTransactionDescription = "B2B Bill Payment to "+strPaybillName;

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);
                    long getProductID = Long.parseLong(pesaParam.getProductId());

                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    pesa.setOriginatorID(strMAPPSessionID);
                    pesa.setProductID(getProductID);
                    pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
                    pesa.setPESAAction(PESAConstants.PESAAction.B2B);
                    pesa.setCommand("BusinessPayBill");
                    pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
                    //pesa.setChargeProposed(null);

                    pesa.setInitiatorType("MSISDN");
                    pesa.setInitiatorIdentifier(strUsername);
                    pesa.setInitiatorAccount(strUsername);
                    //pesa.setInitiatorName(""); - Set after getting name from CBS
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strFromAccountNo);
                    pesa.setSourceAccount(strFromAccountNo);
                    //pesa.setSourceName(""); - Set after getting name from CBS
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("CBS");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("SHORT_CODE");
                    pesa.setReceiverIdentifier(strPaybillNo);
                    pesa.setReceiverAccount(strBillAccountNumber);
                    pesa.setReceiverName(strPaybillName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strUsername);
                    pesa.setBeneficiaryAccount(strUsername);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setCategory("UTILITY_BILL_PAYMENT");

                    pesa.setPriority(200);
                    pesa.setSendCount(0);

                    pesa.setSchedulePesa(PESAConstants.Condition.NO);
                    pesa.setPesaDateScheduled(strDate);
                    pesa.setPesaDateCreated(strDate);
                    pesa.setPESAXMLData("<DATA/>");

                    pesa.setPESAStatusCode(10);
                    pesa.setPESAStatusName("QUEUED");
                    pesa.setPESAStatusDescription("New PESA");
                    pesa.setPESAStatusDate(strDate);

                    XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();
                    boolean isOtherNumber= false;


                    String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strMAPPSessionID, strMAPPSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strFromAccountNo, bdAmount, strUsername, strPassword, "MAPP", strMAPPSessionID, "MBANKING", strBillAccountNumber, strBillAccountNumber,strPaybillName, isOtherNumber, "");

                    String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

                    System.out.println("NAV Request Result:"+strWithdrawalStatus);

                    switch (arrWithdrawalStatus[0]){
                        case "SUCCESS":{
                            String strMemberName = arrWithdrawalStatus[1].trim();
                            pesa.setSourceName(strMemberName);
                            pesa.setBeneficiaryName(strMemberName);
                            pesa.setInitiatorName(strMemberName);

                            if(PESAProcessor.sendPESA(pesa) > 0){
                                strAmount = Utils.formatAmount(strAmount);
                                strCharge = "YES";
                                strTitle= "Pay Bill Payment";
                                strResponseText = "Your payment of <b>KES "+strAmount+"</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                                enResponseAction = CON;
                            } else {
                                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                                enResponseAction = CON;

                                CBSAPI.reverseWithdrawalRequest(strMAPPSessionID);
                            }
                            break;
                        }
                        case "INCORRECT_PIN":{
                            strTitle= "ERROR: Incorrect PIN";
                            strResponseText = "You have entered an incorrect user PIN, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INVALID_ACCOUNT":{
                            strTitle= "ERROR: Invalid Account";
                            strResponseText = "You have selected an invalid account number, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "INSUFFICIENT_BAL":{
                            strTitle= "ERROR: Insufficient Balance";
                            strResponseText = "You have insufficient balance to complete this request, please try again";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = CON;
                            break;
                        }
                        case "ACCOUNT_NOT_ACTIVE":{
                            strTitle= "ERROR: Account Not Active";
                            strResponseText = "Your account is inactive at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "TRANSACTION_EXISTS":{
                            strTitle= "ERROR: Withdrawal Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        case "BLOCKED":{
                            strTitle= "ERROR: Account Blocked";
                            strResponseText = "Your account is blocked at the moment, please contact us or visit your nearest branch to get assistance";

                            enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
                            enResponseAction = MAPPConstants.ResponseAction.END;
                            break;
                        }
                        default:{
                            System.err.println("DEFAULT ON SWITCH -> "+this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + strWithdrawalStatus);
                            strTitle= "ERROR: Pay Bill Failed";
                            strResponseText = "An error occurred processing your request. Please try again after a few minutes.";
                        }
                    }
                }

                Element elData = doc.createElement("DATA");
                elData.setTextContent(strResponseText);

                generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

                //Response
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

                theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
            } else {
                theMAPPResponse = mrOTPVerificationMappResponse;
            }
        } catch (Exception e){
            System.err.println(this.getClass().getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }


    public MAPPAmountLimitParam getParam(ke.skyworld.mbanking.mappapi.APIConstants.MAPP_PARAM_TYPE theMAPPParamType) {
        MAPPAmountLimitParam rVal = new MAPPAmountLimitParam();
        try {
            String strMAPPParamType = "OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/AMOUNT_LIMITS";

            switch (theMAPPParamType) {
                case CASH_WITHDRAWAL: {
                    strMAPPParamType += "/CASH_WITHDRAWAL";
                    break;
                }
                case CASH_WITHDRAWAL_TO_OTHER: {
                    strMAPPParamType += "/CASH_WITHDRAWAL_TO_OTHER";
                    break;
                }
                case AIRTIME_PURCHASE: {
                    strMAPPParamType += "/AIRTIME_PURCHASE";
                    break;
                }
                case PAY_BILL: {
                    strMAPPParamType += "/PAY_BILL";
                    break;
                }
                case EXTERNAL_FUNDS_TRANSFER: {
                    strMAPPParamType += "/EXTERNAL_FUNDS_TRANSFER";
                    break;
                }
                case INTERNAL_FUNDS_TRANSFER: {
                    strMAPPParamType += "/INTERNAL_FUNDS_TRANSFER";
                    break;
                }
                case DEPOSIT: {
                    strMAPPParamType += "/DEPOSIT";
                    break;
                }
                case APPLY_LOAN: {
                    strMAPPParamType += "/APPLY_LOAN";
                    break;
                }
                case PAY_LOAN: {
                    strMAPPParamType += "/PAY_LOAN";
                    break;
                }
            }

            String strMinimum = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MAPP, strMAPPParamType + "/MIN_AMOUNT");
            String strMaximum = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.MAPP, strMAPPParamType + "/MAX_AMOUNT");

            rVal.setMinimum(strMinimum);
            rVal.setMaximum(strMaximum);
        } catch (Exception e) {
            System.err.println("MAPPAPI.getParam() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public MAPPResponse getDividendPayslipMapp(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = MAPPConstants.ResponsesDataType.TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;

            String strEntryCode = UUID.randomUUID().toString().toUpperCase();

            String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
            String strMAPPSessionId = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());


            String strDateNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            String strMemberName = "";

            String strEmailAddress = configXPath.evaluate("EMAIL_ADDRESS", ndRequestMSG).trim();
            String strYear = configXPath.evaluate("YEAR", ndRequestMSG).trim();

            if (strYear == "") {

                int previousYear = LocalDate.now().getYear() - 1;
                System.out.println("Previous Year: " + previousYear);
                strYear = String.valueOf(previousYear);

            }
            String strfinalYear = strYear;


            String strTraceId = theMAPPRequest.getTraceID();

            String strSessionId = String.valueOf(theMAPPRequest.getSessionID());

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy hh:mm:ss a");
            LocalDateTime now = LocalDateTime.now();
            String strDate = dtf.format(now);

            String strPhoneNumber = theMAPPRequest.getUsername();

            MAPPConstants.ResponseStatus enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

            String strTitle = "Dividend Payslip";
            String strResponseText = "";

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");


            String strDividendsPayslipBase64 = CBSAPI.getDividendPayslipCurrent(theMAPPRequest.getUsername(), strfinalYear);

            if (strDividendsPayslipBase64.isBlank()) {

                strResponseText = "Dear Member,\nNo dividends found for the selected period";
//                strResponseText = "Dear Member,\n";
                strCharge = "NO";

                elData.setTextContent(strResponseText);

            } else {

                strResponseText = strDividendsPayslipBase64;
                enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                strCharge = "YES";

                elData.setTextContent(strResponseText);
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);


            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return theMAPPResponse;
    }

    public static void processSendEmail(String theReceiver, String theMSGSubject, String theMSG, String theFilePath) {

        String strUserName = "admin@njiwa.co.ke";

        String strFrom = "Njiwa SACCO Society LTD<" + strUserName + ">";

        String strPassword = "Letmein@ICT";

        String strHostName = "mail.njiwa.co.ke";
        int intHostPort = 587;

        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", strHostName);
            properties.put("mail.smtp.port", intHostPort);
            properties.put("mail.smtp.auth", true);
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.ssl.checkserveridentity", "true");

            Session session = Session.getInstance(properties, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(strUserName, strPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(strFrom));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(theReceiver));
            message.setSubject(theMSGSubject);

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(theMSG);

            MimeBodyPart attachmentPart = new MimeBodyPart();

            if(theFilePath != null && !theFilePath.equals("")) {
                attachmentPart.attachFile(new File(theFilePath));
            }

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            if(theFilePath != null && !theFilePath.equals("")) {
                multipart.addBodyPart(attachmentPart);
            }

            message.setContent(multipart);
            Transport.send(message);

            System.out.println("Sent message successfully");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

}
