package ke.skyworld.mbanking.mappapi;

import com.jayway.jsonpath.JsonPath;
import ke.co.skyworld.smp.authentication_manager.MobileBankingCryptography;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_manager.util.SystemParameters;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.Misc;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
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
import ke.skyworld.mbanking.channelutils.EmailMessaging;
import ke.skyworld.mbanking.channelutils.Messaging;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.nav.cbs.ChannelService;
import ke.skyworld.mbanking.pesaapi.APIConstants;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import ke.skyworld.mbanking.ussdapplication.AppUtils;
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

import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES;
import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_MOBILE_BANKING_REGISTER;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseAction.*;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseAction.ACCEPT_TERMS_AND_CONDITIONS;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseStatus.*;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseStatus.FAILED;
import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponsesDataType.*;
import static ke.skyworld.mbanking.mappapi.MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL_TO_OTHER;
import static ke.skyworld.mbanking.ussdapi.APIUtils.*;
import static ke.skyworld.mbanking.ussdapi.USSDAPIConstants.StandardReturnVal.INVALID_APP_ID;

public class MAPPAPI {

    boolean blGroupBankingEnabled = false;

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

    String getUserFullName(MAPPRequest theMAPPRequest, String strUserPhoneNumber) {
        String strAccountName = "";

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                TBL_CUSTOMER_REGISTER_SIGNATORIES, "full_name",
                new FilterPredicate("primary_mobile_number = :primary_mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":primary_mobile_number", strUserPhoneNumber));

        if (signatoryDetailsWrapper.hasErrors()) {
            return "";
        }

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        if (signatoryDetailsMap != null && !signatoryDetailsMap.isEmpty()) {
            strAccountName = signatoryDetailsMap.getStringValue("full_name");
            return Utils.toTitleCase(strAccountName);
        }

        return strAccountName;
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

    public HashMap<String, String> getUserDetails(MAPPRequest theMAPPRequest, String identifierType, String identifier) {
        HashMap<Object, Object> hmRVal = null;
        try {
            String strMobileNumber = String.valueOf(theMAPPRequest.getUsername());
            String strAppID = String.valueOf(theMAPPRequest.getAppID());
            String strPassword = theMAPPRequest.getPassword();

            if (identifierType.equalsIgnoreCase("Mobile No") || identifierType.equals("MSISDN")) {
                identifierType = "MSISDN";
            } else if (identifierType.equalsIgnoreCase("ID Number") || identifierType.equals("ID") || identifierType.equals("NATIONAL_ID")) {
                identifierType = "NATIONAL_ID";
            } else if (identifierType.equalsIgnoreCase("Member Number") || identifierType.equals("MEMBER_NUMBER")) {
                identifierType = "MEMBER_NUMBER";
            } else if (identifierType.equalsIgnoreCase("Account Number") || identifierType.equals("Account") || identifierType.equals("ACCOUNT") || identifierType.equals("ACCOUNT_NUMBER")) {
                identifierType = "ACCOUNT_NUMBER";
            } else {
                identifierType = "MSISDN";
            }

            /*TransactionWrapper<FlexicoreHashMap> getUserDetailsWrapper = CBSAPI.validateAccountNumber(theMAPPRequest.getUsername(), getTraceID(theMAPPRequest), theMAPPRequest.getUsername(), identifier);
            if (!getUserDetailsWrapper.hasErrors()) {
                FlexicoreHashMap userDetailsMap = getUserDetailsWrapper.getSingleRecord();

                HashMap<String, String> userDetailsHashMap = new HashMap<>();

                userDetailsHashMap.put("number", userDetailsMap.getStringValue("acc_no"));
                userDetailsHashMap.put("type_name", userDetailsMap.getStringValue("ac_label"));
                userDetailsHashMap.put("member_number", userDetailsMap.getStringValue("cust_id"));
                userDetailsHashMap.put("full_name", userDetailsMap.getStringValue("cust_name"));
                userDetailsHashMap.put("identifier", userDetailsMap.getStringValue("pri_mobile_no"));
                userDetailsHashMap.put("identity", userDetailsMap.getStringValue("cust_id_no"));

                return userDetailsHashMap;
            }*/


            TransactionWrapper<FlexicoreHashMap> getUserDetailsWrapper = CBSAPI.validateAccountNumber(theMAPPRequest.getUsername(), UUID.randomUUID().toString(), theMAPPRequest.getUsername(), identifier);
            if (!getUserDetailsWrapper.hasErrors()) {
                FlexicoreHashMap userDetailsMap = getUserDetailsWrapper.getSingleRecord();

                HashMap<String, String> userDetailsHashMap = new HashMap<>();

                userDetailsHashMap.put("number", userDetailsMap.getStringValue("account_number"));
                userDetailsHashMap.put("type_name", userDetailsMap.getStringValue("account_label"));
                userDetailsHashMap.put("member_number", userDetailsMap.getStringValue("cust_id"));

                FlexicoreHashMap memberDetailsMap = userDetailsMap.getFlexicoreHashMap("member_details");
                String fullName = memberDetailsMap.getStringValueOrIfNull("full_name", "");
                String[] fullNameArr = fullName.split(" ");
                fullName = fullNameArr[0];

                String strMobileNumberCustomer = memberDetailsMap.getStringValueOrIfNull("mobile_number", "");
                strMobileNumberCustomer = AppUtils.maskPhoneNumber(strMobileNumberCustomer);

                userDetailsHashMap.put("full_name", fullName + " - " + strMobileNumberCustomer + "");
                userDetailsHashMap.put("identifier", strMobileNumberCustomer);
                userDetailsHashMap.put("identity", memberDetailsMap.getStringValueOrIfNull("id_number", ""));

                return userDetailsHashMap;


               /* String requestStatus = userDetailsMap.getStringValue("request_status");
                FlexicoreHashMap accountDetailsResponseMap = userDetailsMap.getFlexicoreHashMap("response_payload");

                if (requestStatus.equalsIgnoreCase("SUCCESS")) {

                    HashMap<String, String> userDetailsHashMap = new HashMap<>();

                    userDetailsHashMap.put("number", accountDetailsResponseMap.getStringValue("account_number"));
                    userDetailsHashMap.put("type_name", userDetailsMap.getStringValue("account_label"));
                    userDetailsHashMap.put("member_number", userDetailsMap.getStringValue("cust_id"));

                    FlexicoreHashMap memberDetailsMap = accountDetailsResponseMap.getFlexicoreHashMap("member_details");
                    String fullName = memberDetailsMap.getStringValueOrIfNull("full_name", "");
                    String[] fullNameArr = fullName.split(" ");
                    fullName = fullNameArr[0];

                    String strMobileNumberCustomer = memberDetailsMap.getStringValueOrIfNull("mobile_number", "");
                    strMobileNumberCustomer = AppUtils.maskPhoneNumber(strMobileNumberCustomer);

                    userDetailsHashMap.put("full_name", fullName + " - " + strMobileNumberCustomer + "");
                    userDetailsHashMap.put("identifier", strMobileNumberCustomer);
                    userDetailsHashMap.put("identity", memberDetailsMap.getStringValueOrIfNull("id_number", ""));

                    return userDetailsHashMap;
                }*/

            }


        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public MAPPResponse userLogin(MAPPRequest theMAPPRequest, MAPPAPIConstants.OTP_TYPE theOTPType) {

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
            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strNotificationID = configXPath.evaluate("NOTIFICATION_ID", ndRequestMSG).trim();
            if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                strPassword = configXPath.evaluate("PASSWORD", ndRequestMSG).trim();
            }

//            System.out.println(XmlUtils.convertNodeToStr(ndRequestMSG));

            String strMAPPVersionFromUser = configXPath.evaluate("BUILD_NUMBER", ndRequestMSG).trim();

            boolean blOTPVerificationRequired = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.GENERATION).isEnabled();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Mobile Banking";
            String strDescription = "Welcome to Mobile Banking. Please visit your nearest branch to activate your account for mobile banking.";

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            String strLoginStatus = "ERROR";
            String strLoginAttemptMessage = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";

            Element elData = doc.createElement("DATA");

            String strMemberFullName = "";
            String strMemberGender = "MALE";

            String strSettingsXML = SystemParameters.getParameter("MBANKING_SERVICES_MANAGEMENT");
            Document docSettingsXML = XmlUtils.parseXml(strSettingsXML);


            String strOrganizationMbankingSettings = SystemParameters.getParameter("ORGANIZATION_MBANKING_SETTINGS");
            Document docOrganizationSettingsXML = XmlUtils.parseXml(strOrganizationMbankingSettings);


            String strMobileAppServiceStatus = XmlUtils.getTagValue(docSettingsXML, "/MBANKING_SERVICES/MAPP/@STATUS");
            String strMobileAppDisplayMessage = XmlUtils.getTagValue(docSettingsXML, "/MBANKING_SERVICES/MAPP/@MESSAGE");

            String strCharge = "NO";

            /*if (!CBSAPI.isNumberWhitelisted(strUsername)) {
                strTitle = "Service Under Maintenance";
                strDescription = strMobileAppDisplayMessage;

                Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
                elDescription.setTextContent("Sorry, this service is currently under maintenance");
                elData.appendChild(elDescription);
                generateResponseMSGNode(doc, elData, theMAPPRequest, CON, ERROR, strCharge, strTitle, enDataType);
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
                return setMAPPResponse(ndResponseMSG, theMAPPRequest);
            }*/


            //1. CHECK IF MOBILE BANKING IS ENABLED
            if (!strMobileAppServiceStatus.equalsIgnoreCase("ACTIVE")) {
                strTitle = "Service Under Maintenance";
                strDescription = strMobileAppDisplayMessage;

                Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
                elDescription.setTextContent(strDescription);
                elData.appendChild(elDescription);
                generateResponseMSGNode(doc, elData, theMAPPRequest, CON, ERROR, strCharge, strTitle, enDataType);
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
                return setMAPPResponse(ndResponseMSG, theMAPPRequest);
            }

            //2. CHECK IF MOBILE BANKING APP IS LATEST VERSION

            String strCorrectMAPPVersion = XmlUtils.getTagValue(docSettingsXML, "/MBANKING_SERVICES/@MAPP_VERSION");

            if (!strMAPPVersionFromUser.matches("\\d+")) {
                strMAPPVersionFromUser = "0";
            }

            int intMAPPVersionFromUser;

            try {
                intMAPPVersionFromUser = Integer.parseInt(strMAPPVersionFromUser);
            } catch (Exception e) {
                intMAPPVersionFromUser = 0;
            }

            if (intMAPPVersionFromUser < Integer.parseInt(strCorrectMAPPVersion)) {

                strTitle = AppConstants.strSACCOProductName + " Update";
                strDescription = "A new version of " + AppConstants.strSACCOProductName + " has been released. Please update on Play Store or App Store before proceeding or dial *633# to access mobile banking services.";

                Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
                elDescription.setTextContent(strDescription);
                elData.appendChild(elDescription);
                generateResponseMSGNode(doc, elData, theMAPPRequest, UPGRADE, ERROR, strCharge, strTitle, enDataType);
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
                return setMAPPResponse(ndResponseMSG, theMAPPRequest);
            }

            //3. PROCEED TO LOG IN

            TransactionWrapper<FlexicoreHashMap> userLoginWrapper = CBSAPI.userLogin(getTraceID(theMAPPRequest), "MSISDN", strUsername, strPassword, "APP_ID", strAppID,
                    USSDAPIConstants.MobileChannel.MOBILE_APP);

            FlexicoreHashMap userLoginMap = userLoginWrapper.getSingleRecord();

            if (userLoginWrapper.hasErrors()) {

                USSDAPIConstants.StandardReturnVal theReturnVal = userLoginMap.getValue("cbs_api_return_val");
                strTitle = userLoginMap.getValue("title");
                strDescription = userLoginMap.getValue("display_message");

                if (theReturnVal != INVALID_APP_ID) {
                    Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
                    elDescription.setTextContent(strDescription);
                    elData.appendChild(elDescription);
                    generateResponseMSGNode(doc, elData, theMAPPRequest, CON, ERROR, strCharge, strTitle, enDataType);
                    Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
                    return setMAPPResponse(ndResponseMSG, theMAPPRequest);
                } else {

                    FlexicoreHashMap mobileBankingDetailsMap = userLoginMap.getFlexicoreHashMap("mobile_register_details");
                    FlexicoreHashMap signatoryDetailsMap = userLoginMap.getFlexicoreHashMap("signatory_details");

                    strMemberFullName = signatoryDetailsMap.getStringValueOrIfNull("full_name", "");
                    strMemberGender = signatoryDetailsMap.getStringValueOrIfNull("gender", "").equalsIgnoreCase("F") ? "FEMALE" : "MALE";

                    strTitle = "Mobile App is Not Activated";
                    strDescription = "Your Mobile App is not activated. Tap 'ACTIVATE' below to activate the Mobile App.";

                    Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
                    elDescription.setTextContent(strDescription);
                    elData.appendChild(elDescription);

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

                    Element elActivationInstructions = doc.createElement("ACTIVATION_INSTRUCTIONS");
                    elActivationInstructions.setTextContent(strActivationInstructions);
                    elData.appendChild(elActivationInstructions);

                    String strMemberName = strMemberFullName.split(" ")[0];

                    Element elMemberData = doc.createElement("MEMBER_DATA");
                    elMemberData.setAttribute("NAME", strMemberName);
                    elMemberData.setAttribute("FULL_NAME", strMemberFullName);
                    elMemberData.setAttribute("GENDER", strMemberGender);
                    elData.appendChild(elMemberData);

                    String strPrivacyStatementLink = XmlUtils.getTagValue(docOrganizationSettingsXML, "/MBANKING_SETTINGS/PRIVACY_STATEMENT");
                    Element elPrivacyStatement = doc.createElement("PRIVACY_STATEMENT");
                    elPrivacyStatement.setTextContent(strPrivacyStatementLink);
                    elData.appendChild(elPrivacyStatement);

                    generateResponseMSGNode(doc, elData, theMAPPRequest, CHALLENGE_LOGIN, SUCCESS, strCharge, strTitle, enDataType);
                    Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
                    return setMAPPResponse(ndResponseMSG, theMAPPRequest);
                }
            }


            FlexicoreHashMap mobileBankingDetailsMap = userLoginMap.getFlexicoreHashMap("mobile_register_details");
            FlexicoreHashMap signatoryDetailsMap = userLoginMap.getFlexicoreHashMap("signatory_details");

            //Check if force change PIN

            /*String strPINStatus = mobileBankingDetailsMap.getStringValue("pin_status");
            if (strPINStatus.equalsIgnoreCase("RESET")) {
                enDataType = TEXT;
                elData.setTextContent("Change Password");
                generateResponseMSGNode(doc, elData, theMAPPRequest, FORCE_PASSWORD_CHANGE, SUCCESS, strCharge, strTitle, enDataType);
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
                return setMAPPResponse(ndResponseMSG, theMAPPRequest);
            }*/

            strMemberFullName = signatoryDetailsMap.getStringValueOrIfNull("full_name", "");
            strMemberGender = signatoryDetailsMap.getStringValueOrIfNull("gender", "").equalsIgnoreCase("F") ? "FEMALE" : "MALE";

            String strMemberName = strMemberFullName.split(" ")[1];

            Element elMemberData = doc.createElement("MEMBER_DATA");
            elMemberData.setAttribute("NAME", strMemberName);
            elMemberData.setAttribute("FULL_NAME", strMemberFullName);
            elMemberData.setAttribute("GENDER", strMemberGender);
            elData.appendChild(elMemberData);

            String strAcceptedTermsAndConditions = mobileBankingDetailsMap.getStringValue("accepted_terms_and_conditions");
            if (strAcceptedTermsAndConditions.equalsIgnoreCase("NO")) {

                strTitle = "Privacy Statement";
                strDescription = XmlUtils.getTagValue(docOrganizationSettingsXML, "/MBANKING_SETTINGS/PRIVACY_STATEMENT");

//                System.out.println(XmlUtils.convertNodeToStr(docOrganizationSettingsXML));

                Element elDescription = doc.createElement("PRIVACY_STATEMENT");
                elDescription.setTextContent(strDescription);
                elData.appendChild(elDescription);
                generateResponseMSGNode(doc, elData, theMAPPRequest, ACCEPT_TERMS_AND_CONDITIONS, SUCCESS, strCharge, strTitle, enDataType);
                Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

//                System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));

                return setMAPPResponse(ndResponseMSG, theMAPPRequest);
            }

            strTitle = "Login Successful";
            strDescription = "The login was successful";

            String strMobileNumber = signatoryDetailsMap.getStringValueOrIfNull("primary_mobile_number", "");

            FlexicoreHashMap mobileAppVersionMap = Repository.selectWhere(StringRefs.SENTINEL, "tmp.tmp_mobile_app_versions",
                    new FilterPredicate("mobile_number = :mobile_number"),
                    new FlexicoreHashMap().addQueryArgument(":mobile_number", strUsername)).getSingleRecord();

            if (mobileAppVersionMap != null && !mobileAppVersionMap.isEmpty()) {
                String strMAPPVersionFromUser2 = mobileAppVersionMap.getStringValue("mobile_app_version");

                int intMAPPVersionFromUser2;

                try {
                    intMAPPVersionFromUser2 = Integer.parseInt(strMAPPVersionFromUser2);
                } catch (Exception e) {
                    intMAPPVersionFromUser2 = 0;
                }

                if (intMAPPVersionFromUser > intMAPPVersionFromUser2) {
                    CBSAPI.addOrUpdateMobileAppVersion(strMobileNumber, "" + intMAPPVersionFromUser);
                }

            } else {
                CBSAPI.addOrUpdateMobileAppVersion(strMobileNumber, "" + intMAPPVersionFromUser);
            }

            if (blOTPVerificationRequired) {
                generateOTP(theMAPPRequest);
            }

            Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
            elDescription.setTextContent(strDescription);
            elData.appendChild(elDescription);

            generateResponseMSGNode(doc, elData, theMAPPRequest, CON, SUCCESS, strCharge, strTitle, enDataType);
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            return setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            e.printStackTrace();

            Document doc = XmlUtils.createNewDocument();
            Element elDescription = doc.createElement("LOGIN_RESPONSE_DESCRIPTION");
            Element elData = doc.createElement("DATA");

            elDescription.setTextContent("Error occurred while processing your request. If the problem persists please contact your organization for further assistance");
            elData.appendChild(elDescription);
            generateResponseMSGNode(doc, elData, theMAPPRequest, CON, ERROR, "NO", "ERROR", TEXT);
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);
            return setMAPPResponse(ndResponseMSG, theMAPPRequest);
        }
    }

    public APIUtils.OTP checkOTPRequirement(MAPPRequest theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE theOtpCheckStage) {
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

            if (theOtpCheckStage == MAPPAPIConstants.OTP_CHECK_STAGE.GENERATION) {
                if (ndOTP != null && intOTPTTL != 0 && intOTPLength != 0) {
                    otp.setEnabled(true);
                }
            } else if (theOtpCheckStage == MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION) {
                if (ndOTP != null) {
                    otp.setEnabled(true);
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            e.printStackTrace();
        } finally {
            ndRequestMSG = null;
            configXPath = null;
            ndOTP = null;
        }
        return otp;
    }

    public MAPPResponse validateOTP(MAPPRequest theMAPPRequest, MAPPAPIConstants.OTP_TYPE theOTPType) {

        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            boolean blAddDataAction = false;

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            String strActivationCode = configXPath.evaluate("OTP", ndRequestMSG).trim();


            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Error";
            String strDescription = "An error occurred. Please try again after a few minutes.";

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            String strStartKey = "";
            strStartKey = (String) InMemoryCache.retrieve(strUsername + strActivationCode);

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = ERROR;

            Element elData = doc.createElement("DATA");


            TransactionWrapper<FlexicoreHashMap> otpValidationWrapper = CBSAPI.validateOTP(getTraceID(theMAPPRequest), "MSISDN",
                    strUsername, "APP_ID", strAppID, theOTPType, strActivationCode);

            FlexicoreHashMap otpValidationMap = otpValidationWrapper.getSingleRecord();

            if (otpValidationWrapper.hasErrors()) {
                strTitle = otpValidationMap.getStringValue("title");
                strDescription = otpValidationMap.getStringValue("display_message");
                USSDAPIConstants.Condition endSession = otpValidationMap.getValue("end_session");

                if (endSession == USSDAPIConstants.Condition.YES) {
                    enResponseAction = END;
                } else {
                    enResponseAction = CON;
                }

                enResponseStatus = ERROR;

            } else {

                FlexicoreHashMap mobileBankingMap = otpValidationMap.getFlexicoreHashMap("mobile_register_details");

                String strUserAccountStatus;
                if (theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) {

                    TransactionWrapper<FlexicoreHashMap> activateMobileAppWrapper = CBSAPI.activateMobileApp(getTraceID(theMAPPRequest), "MSISDN", strUsername,
                            "APP_ID", strAppID, mobileBankingMap);

                    if (activateMobileAppWrapper.hasErrors()) {
                        strTitle = "Activation Failed";
                        strDescription = "An error occurred. Please try again after a few minutes.";

                        enResponseAction = END;
                        enResponseStatus = ERROR;
                    } else {
                        strTitle = "Activation Successful";
                        strDescription = "Mobile app account activation was successful";

                        enResponseAction = END;
                        enResponseStatus = SUCCESS;
                    }

                } else {
                    strTitle = "OTP Validation Successful";
                    strDescription = "Your OTP validation was successful";

                    enResponseAction = CON;
                    enResponseStatus = SUCCESS;

                }
            }

            String strCharge = "NO";
            elData.setTextContent(strDescription);

            if (blAddDataAction) {
                elData.setAttribute("ACTION", "REQUEST_OTP");
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            e.printStackTrace();
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

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.GENERATION);

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

            String strOneTimePIN = Utils.generateRandomString(intOTPLength);

            if (strUsername.equalsIgnoreCase("254790491947") || strUsername.equalsIgnoreCase("0790491947")) {
                strOneTimePIN = "123456";
            }

            TransactionWrapper<FlexicoreHashMap> mobileMappingDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                    TBL_MOBILE_BANKING_REGISTER,
                    new FilterPredicate("mobile_number = :mobile_number"),
                    new FlexicoreHashMap().addQueryArgument(":mobile_number", strUsername));

            FlexicoreHashMap mobileBankingDetailsMap = mobileMappingDetailsWrapper.getSingleRecord();


            FlexicoreHashMap signatoryDetailsMap = Repository.selectWhere(StringRefs.SENTINEL,
                    TBL_CUSTOMER_REGISTER_SIGNATORIES,
                    new FilterPredicate("signatory_id = :signatory_id"),
                    new FlexicoreHashMap().addQueryArgument(":signatory_id", mobileBankingDetailsMap.getStringValue("signatory_id"))).getSingleRecord();

            String strFullName = signatoryDetailsMap.getStringValue("full_name");

            String memberName = strFullName.split(" ")[0];
            if (memberName.trim().isEmpty()) {
                memberName = "member";
            }
            memberName = memberName.trim();
            memberName = Misc.convertToTitleCase(memberName);

            //MAPPAPIDB.fnDeleteOTPData(strUsername);
            MAPPAPIDB.fnInsertOTPData(mobileBankingDetailsMap, strUsername, strOneTimePIN, intOTPTTL);

            SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
            Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (intOTPTTL * 1000));

            String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
            String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);

            String strMSG = "Dear Member,\n" + strOneTimePIN + " is your One Time Password(OTP) generated at " + strTimeGenerated + ". This OTP is valid up to " + strExpiryDate + ".\n" + strOTPID + (!strAppSignature.equals("") ? ("\n" + strAppSignature) : "");

            String strEmailOTPMessage = Messaging.getMessagingTemplate("EMAIL", "MOBILE_BANKING_OTP_MESSAGE");
            strEmailOTPMessage = strEmailOTPMessage.replace("[MEMBER_NAME]", memberName);
            strEmailOTPMessage = strEmailOTPMessage.replace("[OTP]", strOneTimePIN);
            strEmailOTPMessage = strEmailOTPMessage.replace("[GENERATED_AT]", DateTime.getCurrentDateTime("dd-MM-yyyy HH:mm:ss"));
            strEmailOTPMessage = strEmailOTPMessage.replace("[VALID_TILL]", strExpiryDate);

            sendOTP(theMAPPRequest, mobileBankingDetailsMap, signatoryDetailsMap, strMSG, strEmailOTPMessage);

            String strCharge = "YES";
            int intMSGSent = 1;
         /*   if (!strUsername.equalsIgnoreCase("254706405989")) {
                intMSGSent = fnSendSMS(strUsername, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "ONE_TIME_PASSWORD", "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);
            }*/

            String strTitle = "OTP Generated and Sent Successfully";
            String strResponseText = "Your One Time Password was generated and sent successfully.";

            if (intMSGSent <= 0) {
                strTitle = "OTP Generation Failed";
                strResponseText = "There was an error sending your One Time Password. Please try again";
                strCharge = "NO";
                enResponseAction = CON;
                enResponseStatus = ERROR;
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

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
                        Element elCard = createCardElement(doc, "AGM Announcement", "We will be having an AGM on 4th January 2021. Kindly plan to attend.", MAPPAPIConstants.CardValueType.TEXT, 16);
                        elCards.appendChild(elCard);
                        Element elButtons = doc.createElement("BUTTONS");
                        elCard.appendChild(elButtons);
                        Element elButton = doc.createElement("BUTTON");
                        elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.CONTACT_US.getValue());
                        elButtons.appendChild(elButton);
                    }

                    {
                        Element elAddon = doc.createElement("ADD_ON");
                        elAddon.setAttribute("NAME", "TRANSACT");
                        elAddon.setAttribute("TAB", "TRANSACT");
                        elAddons.appendChild(elAddon);
                        Element elCards = doc.createElement("CARDS");
                        elAddon.appendChild(elCards);
                        Element elCard = createCardElement(doc, "Launch of B2B Services", "We have launched Bank to Bank transfer services and you can now send money from SACCO to Bank.", MAPPAPIConstants.CardValueType.TEXT, 16);
                        elCards.appendChild(elCard);
                        Element elButtons = doc.createElement("BUTTONS");
                        elCard.appendChild(elButtons);
                        Element elButton = doc.createElement("BUTTON");
                        elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.BANK_TRANSFER.getValue());
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
                            Element elCard = createCardElement(doc, "Total FOSA Accounts", "12345", MAPPAPIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        {
                            Element elCard = createCardElement(doc, "Total BOSA Accounts", "5000", MAPPAPIConstants.CardValueType.CURRENCY, 20);
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
                            Element elItem = createItemElement(doc, "6100487005678", "23893", MAPPAPIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Deposit Contribution");
                            elItems.setAttribute("CATEGORIES", "ALL,BOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "456655", MAPPAPIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
                            elButtons.appendChild(elButton);
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Shares");
                            elItems.setAttribute("CATEGORIES", "ALL,BOSA");
                            Element elItem = createItemElement(doc, "6100487005678", "563456", MAPPAPIConstants.CardValueType.CURRENCY);
                            elItems.appendChild(elItem);
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            Element elButton = doc.createElement("BUTTON");
                            elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.ACCOUNT_STATEMENT.getValue());
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
                            Element elCard = createCardElement(doc, "Total Outstanding Loans", "12345", MAPPAPIConstants.CardValueType.CURRENCY, 20);
                            elCards.appendChild(elCard);
                        }
                        {
                            Element elCard = createCardElement(doc, "Total Guaranteed Loans", "5000", MAPPAPIConstants.CardValueType.CURRENCY, 20);
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
                                Element elItem = createItemElement(doc, "Loan Type", "Normal Loan", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Loan Number", "LN893892", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Balance", "30000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Installments", "3000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            Element elButtons = doc.createElement("BUTTONS");
                            elItems.appendChild(elButtons);
                            {
                                Element elButton = doc.createElement("BUTTON");
                                elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.PAY_LOAN.getValue());
                                elButtons.appendChild(elButton);
                            }
                            {
                                Element elButton = doc.createElement("BUTTON");
                                elButton.setAttribute("SERVICE", MAPPAPIConstants.MAPPService.LOAN_STATEMENT.getValue());
                                elButtons.appendChild(elButton);
                            }
                        }
                        {
                            Element elItems = doc.createElement("ITEMS");
                            elList.appendChild(elItems);
                            elItems.setAttribute("LABEL", "Normal Loan");
                            elItems.setAttribute("CATEGORIES", "GUARANTEED_LOANS");
                            {
                                Element elItem = createItemElement(doc, "Loan Type", "Normal Loan", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Loan Number", "LN893892", MAPPAPIConstants.CardValueType.TEXT);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Balance", "30000", MAPPAPIConstants.CardValueType.CURRENCY);
                                elItems.appendChild(elItem);
                            }
                            {
                                Element elItem = createItemElement(doc, "Installments", "3000", MAPPAPIConstants.CardValueType.CURRENCY);
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

    Element createCardElement(Document theDocument, String theLabel, String theValue, MAPPAPIConstants.CardValueType theType, float theFontSize) {
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

    Element createItemElement(Document theDocument, String theLabel, String theValue, MAPPAPIConstants.CardValueType theType) {
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

    public MAPPResponse getBankAccounts(MAPPRequest theMAPPRequest, MAPPAPIConstants.AccountType theAccountType, String theAction) {

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

            boolean bFOSA = false;

         /*   if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }
*/
            //Accounts HashMap
            /*{5-04-00010-02=Salary Acc (5-04-00010-02), 4-61-90010-01=Micro-cred (4-61-90010-01)}*/
            LinkedHashMap<String, String> accounts = getMemberAccountsList(theMAPPRequest, theAccountType);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Member Accounts";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elAccounts = doc.createElement("ACCOUNTS");
            elData.appendChild(elAccounts);


            for (String accountNumber : accounts.keySet()) {
                String strAccountName = accounts.get(accountNumber);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
                elAccount.setAttributeNode(attrNO);
            }

            if (theAction.equalsIgnoreCase("GET_TRANSACTION_ACCOUNTS_AND_DEPOSIT_SERVICES")) {
                Element elServices = doc.createElement("SERVICES");
                elData.appendChild(elServices);

                //create element SERVICE and append to element SERVICES
                Element elServiceMpesa = doc.createElement("SERVICE");
                elServiceMpesa.setAttribute("ID", "MPESA");
                elServiceMpesa.setTextContent("Safaricom M-PESA");
                elServices.appendChild(elServiceMpesa);

                String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMinimum();
                String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.DEPOSIT).getMaximum();

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(strMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(strMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);

                Element elToAccountTypes = doc.createElement("TO_ACCOUNT_TYPES");

                Element elAccountTypeMy = doc.createElement("ACCOUNT_TYPE");
                elAccountTypeMy.setTextContent("MY Account");
                elAccountTypeMy.setAttribute("TYPE_ID", "MY_ACCOUNT");
                elToAccountTypes.appendChild(elAccountTypeMy);

                Element elAccountTypeOther = doc.createElement("ACCOUNT_TYPE");
                elAccountTypeOther.setTextContent("OTHER Account");
                elAccountTypeOther.setAttribute("TYPE_ID", "OTHER_ACCOUNT");
                elToAccountTypes.appendChild(elAccountTypeOther);

                elData.appendChild(elToAccountTypes);

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
                LinkedList<HashMap<String, String>> llHmStatementPeriods = APIUtils.getStatementPeriods(MBankingConstants.ApplicationType.MAPP);

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

            System.out.println("RESPONSE\n***************************************\n");
            System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));

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

    public MAPPResponse getWithdrawalAccounts(MAPPRequest theMAPPRequest, MAPPAPIConstants.AccountType theAccountType) {

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

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> accounts = getMemberAccountsList(theMAPPRequest, theAccountType);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (accounts != null && !accounts.isEmpty()) {

                Element elAccounts = doc.createElement("ACCOUNTS");
                elData.appendChild(elAccounts);

                for (String accountNumber : accounts.keySet()) {
                    String strAccountName = accounts.get(accountNumber);

                    Element elAccount = doc.createElement("ACCOUNT");
                    elAccount.setTextContent(strAccountName);
                    elAccounts.appendChild(elAccount);

                    // set attribute NO to ACCOUNT element
                    Attr attrNO = doc.createAttribute("NO");
                    attrNO.setValue(accountNumber);
                    elAccount.setAttributeNode(attrNO);
                }


                double dblUtilityETopUplMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                double dblUtilityETopUplMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(dblUtilityETopUplMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(dblUtilityETopUplMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);

            } else {

                enResponseStatus = ERROR;
                enDataType = TEXT;
                String strDescription = "Sorry, you don't have any ACTIVE withdrawable accounts to perform the request.";
                elData.setTextContent(strDescription);
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

    public MAPPResponse getWithdrawalAccountsAndMobileMoneyServices(MAPPRequest theMAPPRequest, MAPPAPIConstants.AccountType theAccountType) {

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

            boolean bFOSA = false;


            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> accounts = getMemberAccountsList(theMAPPRequest, theAccountType);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            if (accounts != null && !accounts.isEmpty()) {

                //ceate element ACCOUNTS_AND_SERVICES and append to DATA
                Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_SERVICES");
                elData.appendChild(elAccountsAndServices);

                //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
                Element elAccounts = doc.createElement("ACCOUNTS");
                elAccountsAndServices.appendChild(elAccounts);

                //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
                Element elServices = doc.createElement("SERVICES");
                elAccountsAndServices.appendChild(elServices);

                for (String accountNumber : accounts.keySet()) {
                    String strAccountName = accounts.get(accountNumber);

                    Element elAccount = doc.createElement("ACCOUNT");
                    elAccount.setAttribute("NO", accountNumber);
                    elAccount.setTextContent(strAccountName);
                    elAccounts.appendChild(elAccount);
                }

                //create element SERVICE and append to element SERVICES
                Element elServiceMpesa = doc.createElement("SERVICE");
                elServiceMpesa.setAttribute("ID", "MPESA");
                elServiceMpesa.setTextContent("Safaricom M-PESA");
                elServices.appendChild(elServiceMpesa);

                //Airtel Money
                //create element SERVICE and append to element SERVICES
                //Element elServiceAirtelMoney = doc.createElement("SERVICE");
                //elServiceAirtelMoney.setAttribute("ID", "AIRTEL");
                //elServiceAirtelMoney.setTextContent("Airtel Money");
                //elServices.appendChild(elServiceAirtelMoney);

                //Equitel Money
                //create element SERVICE and append to element SERVICES
                //Element elServiceEquitelMoney = doc.createElement("SERVICE");
                //elServiceEquitelMoney.setAttribute("ID", "EQUITEL");
                //elServiceEquitelMoney.setTextContent("Equitel Money");
                //elServices.appendChild(elServiceEquitelMoney);

                //ATM Withdrawal
                //create element SERVICE and append to element SERVICES
                //Element elServiceATM = doc.createElement("SERVICE");
                //elServiceATM.setAttribute("ID", "ATM");
                //elServiceATM.setTextContent("Withdraw Via ATM");
                //elServices.appendChild(elServiceATM);

                //Agent Withdrawal
                //create element SERVICE and append to element SERVICES
                //Element elServiceAgent = doc.createElement("SERVICE");
                //elServiceAgent.setAttribute("ID", "AGENT");
                //elServiceAgent.setTextContent("Withdraw Via Agent");
                //elServices.appendChild(elServiceAgent);


                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

                //create element AMOUNT_LIMITS and append to element DATA
                Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
                Element elMinAmount = doc.createElement("MIN_AMOUNT");
                elMinAmount.setTextContent(String.valueOf(dblWithdrawalMin));
                Element elMaxAmount = doc.createElement("MAX_AMOUNT");
                elMaxAmount.setTextContent(String.valueOf(dblWithdrawalMax));
                elWithdrawalLimits.appendChild(elMinAmount);
                elWithdrawalLimits.appendChild(elMaxAmount);
                elData.appendChild(elWithdrawalLimits);


                Element elOtherNumberSetup = doc.createElement("OTHER_NO_SETUP");
                elOtherNumberSetup.setTextContent("ACTIVE");
                elData.appendChild(elOtherNumberSetup);


            } else {

                enResponseStatus = ERROR;
                enDataType = TEXT;
                String strDescription = "Sorry, you don't have any ACTIVE withdrawable accounts to perform the request.";
                elData.setTextContent(strDescription);

            }





            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);


            /*System.out.println("\n\nGET WITHDRAWABLE ACCOUNTS: \n\n");
            System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));

            System.out.println("\n");*/


            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getWithdrawalAccountsAndBanks(MAPPRequest theMAPPRequest, MAPPAPIConstants.AccountType theAccountType) {

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

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{5-04-00010-02=Salary Acc (5-04-00010-02), 4-61-90010-01=Micro-cred (4-61-90010-01)}*/
            LinkedHashMap<String, String> accounts = getMemberAccountsList(theMAPPRequest, theAccountType);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");


            if (accounts != null && !accounts.isEmpty()) {


                //ceate element ACCOUNTS_AND_SERVICES and append to DATA
                Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_BANKS");
                elData.appendChild(elAccountsAndServices);

                //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
                Element elAccounts = doc.createElement("ACCOUNTS");
                elAccountsAndServices.appendChild(elAccounts);

                //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
                Element elBanks = doc.createElement("BANKS");
                elAccountsAndServices.appendChild(elBanks);


                for (String accountNumber : accounts.keySet()) {
                    String strAccountName = accounts.get(accountNumber);

                    Element elAccount = doc.createElement("ACCOUNT");
                    elAccount.setAttribute("NO", accountNumber);
                    elAccount.setTextContent(strAccountName);
                    elAccounts.appendChild(elAccount);
                }

                LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.BANK_SHORT_CODE);
                for (APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts) {
                    Element elBank2 = doc.createElement("BANK");
                    elBank2.setAttribute("PAYBILL_NO", serviceProviderAccount.getProviderAccountIdentifier());
                    elBank2.setTextContent(serviceProviderAccount.getProviderAccountLongTag());
                    elBanks.appendChild(elBank2);
                }

                String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                SPManager spManager = new SPManager(strIntegritySecret);
                String strAccounts = spManager.getAllUserAccountsByProviders(SPManagerConstants.ProviderAccountType.BANK_SHORT_CODE, SPManagerConstants.UserIdentifierType.MSISDN, strUsername);
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

                String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum();
                String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum();

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

                enResponseStatus = ERROR;
                enDataType = TEXT;
                String strDescription = "Sorry, you don't have any ACTIVE withdrawable accounts to perform the request.";
                elData.setTextContent(strDescription);

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

    public MAPPResponse getWithdrawalAccountsAndPaybillServices(MAPPRequest theMAPPRequest, MAPPAPIConstants.AccountType theAccountType) {

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

            boolean bFOSA = false;

            if (theAccountType.getValue().equals("FOSA")) {
                bFOSA = true;
            }

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> accounts = getMemberAccountsList(theMAPPRequest, theAccountType);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Withdrawal Accounts";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            //create ELEMENT DATA
            Element elData = doc.createElement("DATA");

            if (accounts != null && !accounts.isEmpty()) {

                //ceate element ACCOUNTS_AND_SERVICES and append to DATA
                Element elAccountsAndServices = doc.createElement("ACCOUNTS_AND_PAYBILL_SERVICES");
                elData.appendChild(elAccountsAndServices);

                //create element ACCOUNTS and append to element ACCOUNTS_AND_SERVICES
                Element elAccounts = doc.createElement("ACCOUNTS");
                elAccountsAndServices.appendChild(elAccounts);

                //create element SERVICES and append to element ACCOUNTS_AND_SERVICES
                Element elServices = doc.createElement("PAYBILL_SERVICES");
                elAccountsAndServices.appendChild(elServices);

                for (String accountNumber : accounts.keySet()) {
                    String strAccountName = accounts.get(accountNumber);

                    Element elAccount = doc.createElement("ACCOUNT");
                    elAccount.setAttribute("NO", accountNumber);
                    elAccount.setTextContent(strAccountName);
                    elAccounts.appendChild(elAccount);
                }

                //create element SERVICE and append to element SERVICES
                LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts(SPManagerConstants.ProviderAccountType.UTILITY_CODE);
                Element elService;
                for (APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts) {
                    elService = doc.createElement("SERVICE");
                    elService.setAttribute("PAYBILL_NO", serviceProviderAccount.getProviderAccountIdentifier());
                    elService.setAttribute("REF_NAME", serviceProviderAccount.getProviderAccountTypeTag());
                    elService.setTextContent(serviceProviderAccount.getProviderAccountName());
                    elServices.appendChild(elService);
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


                String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMinimum();
                String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMaximum();

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

                enResponseStatus = ERROR;
                enDataType = TEXT;
                String strDescription = "Sorry, you don't have any ACTIVE withdrawable accounts to perform the request.";
                elData.setTextContent(strDescription);

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
            String strAppID = theMAPPRequest.getAppID();

            long lnSessionID = theMAPPRequest.getSessionID();

            boolean bFOSA = false;

            //Accounts HashMap
            /*{Salary Acc (5-04-00010-02)=5-04-00010-02, Micro-cred (4-61-90010-01)=4-61-90010-01}*/
            LinkedHashMap<String, String> fromAccounts = getMemberAccountsList(theMAPPRequest, MAPPAPIConstants.AccountType.WITHDRAWABLE_IFT);
            LinkedHashMap<String, String> toAccounts = getMemberAccountsList(theMAPPRequest, MAPPAPIConstants.AccountType.DEPOSIT_IFT);

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Transfer Accounts";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");
            Element elFromAccounts = doc.createElement("FROM_ACCOUNTS");
            elData.appendChild(elFromAccounts);

            Element elToAccountTypes = doc.createElement("TO_ACCOUNT_TYPES");
            Element elAccountTypeMy = doc.createElement("ACCOUNT_TYPE");
            elAccountTypeMy.setTextContent("MY Account");
            elAccountTypeMy.setAttribute("TYPE_ID", "MY_ACCOUNT");
            elToAccountTypes.appendChild(elAccountTypeMy);

            Element elAccountTypeOther = doc.createElement("ACCOUNT_TYPE");
            elAccountTypeOther.setTextContent("OTHER Account");
            elAccountTypeOther.setAttribute("TYPE_ID", "OTHER_ACCOUNT");
            elToAccountTypes.appendChild(elAccountTypeOther);

            elData.appendChild(elToAccountTypes);

            Element elToAccounts = doc.createElement("TO_ACCOUNTS");
            elData.appendChild(elToAccounts);

            for (String accountNumber : fromAccounts.keySet()) {

                String strAccountName = fromAccounts.get(accountNumber);

                Element elAccount = doc.createElement("FROM_ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elFromAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
                elAccount.setAttributeNode(attrNO);
            }

            for (String accountNumber : toAccounts.keySet()) {
                String strAccountName = toAccounts.get(accountNumber);

                Element elAccount = doc.createElement("TO_ACCOUNT");
                elAccount.setTextContent(strAccountName);
                elToAccounts.appendChild(elAccount);

                // set attribute NO to ACCOUNT element
                Attr attrNO = doc.createAttribute("NO");
                attrNO.setValue(accountNumber);
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

            String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMinimum();
            String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMaximum();

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

            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getMemberLoans(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        String strUsername = theMAPPRequest.getUsername();
        String strPassword = theMAPPRequest.getPassword();
        // strPassword = APIUtils.hashPIN(strPassword, strUsername);
        String strAppID = theMAPPRequest.getAppID();
        long lnSessionID = theMAPPRequest.getSessionID();

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            TransactionWrapper<FlexicoreHashMap> customerLoanAccountsWrapper = CBSAPI.getCustomerLoanAccounts(strUsername, "MSISDN", strUsername);

            FlexicoreHashMap customerLoanAccountsMap = customerLoanAccountsWrapper.getSingleRecord();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (customerLoanAccountsWrapper.hasErrors()) {
                USSDAPIConstants.Condition endSession = customerLoanAccountsMap.getValue("end_session");
                String strResponse = customerLoanAccountsMap.getStringValue("display_message");

                elData.setTextContent(strResponse);
                enResponseStatus = FAILED;

            } else {

                FlexicoreArrayList accountsList = customerLoanAccountsMap.getFlexicoreArrayList("payload");

                if (accountsList != null && !accountsList.isEmpty()) {

                    enDataType = LIST;

                    Element elLoans = doc.createElement("LOANS");
                    elData.appendChild(elLoans);

                    for (FlexicoreHashMap accountMap : accountsList) {

                        String strAccountName = accountMap.getStringValue("loan_type_name").trim();
                        String strAccountNumber = accountMap.getStringValue("loan_serial_number").trim();
                        String strAccountBalance = accountMap.getStringValue("loan_balance").trim();
                        String strInterestBalance = accountMap.getStringValue("interest_amount").trim();

                        double dblAccountBalance = Double.parseDouble(strAccountBalance);
                        double dblInterestBalance = Double.parseDouble(strInterestBalance);

                        dblAccountBalance = APIUtils.roundUp(dblAccountBalance);
                        dblInterestBalance = APIUtils.roundUp(dblInterestBalance);

                        if (dblAccountBalance + dblInterestBalance <= 0.00) {
                            continue;
                        }

                        //String strAccountLabel = accountMap.getStringValue("account_name").trim();

                        //strAccountBalance = strAccountBalance.replaceFirst("-", "");

                      /*  String strLoanNo = loansInService.get(loanTypeCode).get("id");
                        String strLoanName = loansInService.get(loanTypeCode).get("type");
                        String strLoanBalance = loansInService.get(loanTypeCode).get("balance");
*/
                        Element elLoan = doc.createElement("LOAN");
                        elLoan.setTextContent(strAccountName);
                        elLoan.setAttribute("SERIAL_NO", strAccountNumber);
                        elLoan.setAttribute("NAME", strAccountName);
                        elLoan.setAttribute("AMOUNT", Utils.formatDouble(dblAccountBalance, "##0.00"));
                        elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                        elLoan.setAttribute("BALANCE", Utils.formatDouble(dblAccountBalance, "##0.00"));
                        elLoan.setAttribute("INTEREST", Utils.formatDouble(dblInterestBalance, "##0.00"));
                        //elLoan.setAttribute("ACCOUNT_CD", strAccountCd);

                        elLoans.appendChild(elLoan);

                    }
                } else {
                    elData.setTextContent("No Loans Found");
                    enResponseStatus = FAILED;
                }
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            System.out.println("\n\nTHE LOAN ACCOUNTS RESPONSE\n\n");
            System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return theMAPPResponse;
    }
    
    public MAPPResponse getMemberLoans_PREV(MAPPRequest theMAPPRequest) {

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

    public MAPPResponse getMemberLoansWithPaymentDetails_PREV(MAPPRequest theMAPPRequest) {

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

                String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMinimum();
                String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMaximum();

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

    public MAPPResponse getMemberLoansWithPaymentDetails(MAPPRequest theMAPPRequest) {

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

            //todo: Add sample HashMap as documentation
            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            TransactionWrapper<FlexicoreHashMap> customerLoanAccountsWrapper = CBSAPI.getCustomerLoanAccounts(strUsername, "MSISDN", strUsername);


            FlexicoreHashMap customerLoanAccountsMap = customerLoanAccountsWrapper.getSingleRecord();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = LIST;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");


            if (customerLoanAccountsWrapper.hasErrors()) {
                USSDAPIConstants.Condition endSession = customerLoanAccountsMap.getValue("end_session");
                String strResponse = customerLoanAccountsMap.getStringValue("display_message");

                elData.setTextContent("Sorry, an error occurred while processing your request");
                enResponseStatus = FAILED;

            } else {

                FlexicoreArrayList accountsList = customerLoanAccountsMap.getFlexicoreArrayList("payload");

                if (accountsList != null && !accountsList.isEmpty()) {

                    enDataType = LIST;

                    Element elLoans = doc.createElement("LOANS");
                    elData.appendChild(elLoans);

                    for (FlexicoreHashMap accountMap : accountsList) {

                        String strAccountName = accountMap.getStringValue("loan_type_name").trim();
                        String strAccountNumber = accountMap.getStringValue("loan_serial_number").trim();
                        String strAccountBalance = accountMap.getStringValue("loan_balance").trim();

                        String strInterestBalance = accountMap.getStringValue("interest_amount").trim();

                        double dblAccountBalance = Double.parseDouble(strAccountBalance);
                        double dblInterestBalance = Double.parseDouble(strInterestBalance);

                        dblAccountBalance = APIUtils.roundUp(dblAccountBalance);
                        dblInterestBalance = APIUtils.roundUp(dblInterestBalance);

                        if (dblAccountBalance + dblInterestBalance <= 0.00) {
                            continue;
                        }

                      /*  String strLoanNo = loansInService.get(loanTypeCode).get("id");
                        String strLoanName = loansInService.get(loanTypeCode).get("type");
                        String strLoanBalance = loansInService.get(loanTypeCode).get("balance");
*/
                        Element elLoan = doc.createElement("LOAN");
                        elLoan.setTextContent(strAccountName);
                        elLoan.setAttribute("SERIAL_NO", strAccountNumber);
                        elLoan.setAttribute("NAME", strAccountName);
                        elLoan.setAttribute("AMOUNT", Utils.formatDouble(dblAccountBalance, "##0.00"));
                        elLoan.setAttribute("CHANGE_AMOUNT", "YES");
                        elLoan.setAttribute("BALANCE", Utils.formatDouble(dblAccountBalance, "##0.00"));
                        elLoan.setAttribute("INTEREST", Utils.formatDouble(dblInterestBalance, "##0.00"));

                        elLoans.appendChild(elLoan);
                    }

                    Element elRepaymentOptions = doc.createElement("REPAYMENT_OPTIONS");
                    //if it is not enabled then the default repayment option is savings account
                    elRepaymentOptions.setAttribute("ENABLED", "TRUE");

                    Element elRepaymentOption2 = doc.createElement("OPTION");
                    elRepaymentOption2.setAttribute("VALUE", "MPESA");
                    elRepaymentOption2.setAttribute("TYPE", "MPESA");
                    elRepaymentOption2.setTextContent("Safaricom M-Pesa");
                    elRepaymentOptions.appendChild(elRepaymentOption2);

                    LinkedHashMap<String, String> FOSAAccounts = getMemberAccountsList(theMAPPRequest, MAPPAPIConstants.AccountType.WITHDRAWABLE);

                    for (String account : FOSAAccounts.keySet()) {
                        Element elRepaymentOption1 = doc.createElement("OPTION");
                        elRepaymentOption1.setAttribute("VALUE", account);
                        elRepaymentOption1.setAttribute("TYPE", "ACCOUNT");
                        elRepaymentOption1.setTextContent(FOSAAccounts.get(account));
                        elRepaymentOptions.appendChild(elRepaymentOption1);
                    }

                /*Element elRepaymentOption1 = doc.createElement("OPTION");
                elRepaymentOption1.setAttribute("VALUE", "SAVINGS_ACCOUNT");
                elRepaymentOption1.setAttribute("TYPE", "ACCOUNT");
                elRepaymentOption1.setTextContent("Savings Account");
                elRepaymentOptions.appendChild(elRepaymentOption1);*/

                    elData.appendChild(elRepaymentOptions);

                    String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMinimum();
                    String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_LOAN).getMaximum();

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
                    enResponseStatus = FAILED;
                }
            }

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            System.out.println("\n\nTHE LOAN ACCOUNTS RESPONSE\n\n");
            System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            e.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse getLoanTypes_PREV(MAPPRequest theMAPPRequest) {

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

    public MAPPResponse getLoanTypes(MAPPRequest theMAPPRequest) {

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

            TransactionWrapper<FlexicoreHashMap> getLoanTypesWrapper = CBSAPI.getLoanTypes(strUsername, "MSISDN", strUsername);
            FlexicoreHashMap getLoanTypesMap = getLoanTypesWrapper.getSingleRecord();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strTitle = "Loans";

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (getLoanTypesWrapper.hasErrors()) {
                USSDAPIConstants.Condition endSession = getLoanTypesMap.getValue("end_session");
                String strResponse = getLoanTypesMap.getStringValue("display_message");

                elData.setTextContent(strResponse);
                enResponseStatus = FAILED;
            } else {

                FlexicoreArrayList mobileEnabledLoansList = getLoanTypesMap.getFlexicoreArrayList("payload");

                if (mobileEnabledLoansList != null && !mobileEnabledLoansList.isEmpty()) {

                    enDataType = LIST;


                   /* Element elLoans = doc.createElement("LOANS");
                    //elData.appendChild(elLoans);



                    for (FlexicoreHashMap flexicoreHashMap : mobileEnabledLoansList) {
                        String strLoanTypeName = flexicoreHashMap.getStringValue("loan_type_name");
                        String strLoanTypeID = flexicoreHashMap.getStringValue("loan_type_id");
                        String strLoanTypeLabel = flexicoreHashMap.getStringValue("loan_type_name");
                        String strLoanTypeMin = flexicoreHashMap.getStringValue("loan_type_min_amount");
                        String strLoanTypeMax = flexicoreHashMap.getStringValue("loan_type_max_amount");
                        String strLoanTypeMaxInstallments = flexicoreHashMap.getStringValue("loan_type_max_installments");

                        Element elLoan = doc.createElement("LOAN_TYPE");

                        elLoan.setTextContent(strLoanTypeName);
                        elLoan.setAttribute("ID", strLoanTypeID);
                        elLoan.setAttribute("MIN_AMOUNT", strLoanTypeMin);
                        elLoan.setAttribute("MAX_AMOUNT", strLoanTypeMax);
                        elLoan.setAttribute("MAX_INSTALLMENTS", strLoanTypeMaxInstallments);
                        elLoan.setAttribute("MIN_INSTALLMENTS", "1");
                        //elLoan.setAttribute("REQUIRES_INSTALLMENTS", strLoanRequiresInstallments.toUpperCase());

                        elLoans.appendChild(elLoan);
                    }*/


                    String strLoansXML = "<Loans>";
                    for (FlexicoreHashMap flexicoreHashMap : mobileEnabledLoansList) {
                        String strLoanTypeName = flexicoreHashMap.getStringValue("loan_type_name");
                        String strLoanTypeID = flexicoreHashMap.getStringValue("loan_type_id");
                        String strLoanTypeLabel = flexicoreHashMap.getStringValue("loan_type_name");
                        String strLoanTypeMin = flexicoreHashMap.getStringValue("loan_type_min_amount");
                        String strLoanTypeMax = flexicoreHashMap.getStringValue("loan_type_max_amount");
                        String strLoanTypeMaxInstallments = flexicoreHashMap.getStringValue("loan_type_max_installments");

                         strLoansXML +=
                                 "<Product>\n" +
                                 "          <Code>"+strLoanTypeID+"</Code>\n" +
                                 "          <Type>"+strLoanTypeLabel+"</Type>\n" +
                                 "          <UserCanApply>TRUE</UserCanApply>\n" +
                                 "          <Message>Success</Message>\n" +
                                 "          <Source>FOSA</Source>\n" +
                                 "          <RequiresGuarantors>FALSE</RequiresGuarantors>\n" +
                                 "          <RequiresPurpose>TRUE</RequiresPurpose>\n" +
                                 "          <RequiresBranch>FALSE</RequiresBranch>\n" +
                                 "          <InstallmentsType>NONE</InstallmentsType>\n" +
                                 "          <ShowsQualification>FALSE</ShowsQualification>\n" +
                                 "          <RequiresPayslipPIN>FALSE</RequiresPayslipPIN>\n" +
                                 "          <DefaultQualification>\n" +
                                 "              <Minimum>"+strLoanTypeMin+"</Minimum>\n" +
                                 "              <Maximum>"+strLoanTypeMax+"</Maximum>\n" +
                                 "          </DefaultQualification>" +
                                 "</Product>";

                    }

                    strLoansXML += "</Loans>";

                    Element elLoans = docBuilder.parse(new ByteArrayInputStream(strLoansXML.getBytes())).getDocumentElement();
                    Node ndLoans = doc.importNode(elLoans, true);
                    elData.appendChild(ndLoans);

                } else {
                    elData.setTextContent("No Loans Found");
                    enResponseStatus = FAILED;
                }
            }

            /*
            <LoanApplicationPurposes><Purpose Id="1180" Title="Agriculture"/><Purpose Id="2220" Title="Trade"/><Purpose Id="3120" Title="Manufacturing and Services Industries"/><Purpose Id="4120" Title="Education"/><Purpose Id="5110" Title="Human Health"/><Purpose Id="6110" Title="Land and Housing"/><Purpose Id="7210" Title="Finance Investment and Insurance"/><Purpose Id="8210" Title="Consumption and Social activities"/></LoanApplicationPurposes>
            */

            //get loan purposes -- start
            TransactionWrapper<FlexicoreHashMap> getLoanPurposesWrapper = CBSAPI.getLoanPurposes(strUsername, "CUSTOMER_NO", getDefaultCustomerIdentifier(theMAPPRequest));
            FlexicoreHashMap getLoanPurposesMap = getLoanPurposesWrapper.getSingleRecord();

            if (getLoanPurposesWrapper.hasErrors()) {
                USSDAPIConstants.Condition endSession = getLoanPurposesMap.getValue("end_session");
                String strResponse = getLoanPurposesMap.getStringValue("display_message");

                elData.setTextContent(strResponse);
                enResponseStatus = FAILED;
            } else {

                FlexicoreArrayList loanPurposesList = getLoanPurposesMap.getFlexicoreArrayList("payload");

                if (loanPurposesList != null && !loanPurposesList.isEmpty()) {

                    enDataType = LIST;

                    /*Element elLoanPurposes = doc.createElement("LOAN_PURPOSES");
                    elData.appendChild(elLoanPurposes);

                    for (FlexicoreHashMap flexicoreHashMap : loanPurposesList) {
                        String strLoanPurposeCode = flexicoreHashMap.getStringValue("code");
                        String strLoanPurposeDescription = flexicoreHashMap.getStringValue("description");

                        Element elLoanPurpose = doc.createElement("LOAN_PURPOSE");

                        elLoanPurpose.setTextContent(strLoanPurposeDescription);
                        elLoanPurpose.setAttribute("CODE", strLoanPurposeCode);

                        elLoanPurposes.appendChild(elLoanPurpose);
                    }*/

                    String strLoanPurposesXML = "<LoanApplicationPurposes>";

                    for (FlexicoreHashMap flexicoreHashMap : loanPurposesList) {
                        String strLoanPurposeCode = flexicoreHashMap.getStringValue("code");
                        String strLoanPurposeDescription = flexicoreHashMap.getStringValue("description");

                        strLoanPurposesXML += "<Purpose Id=\""+strLoanPurposeCode+"\" Title=\""+strLoanPurposeDescription+"\"/>";
                    }

                    strLoanPurposesXML += "</LoanApplicationPurposes>";

                    Element elLoanPurposes = docBuilder.parse(new ByteArrayInputStream(strLoanPurposesXML.getBytes())).getDocumentElement();
                    Node ndLoanPurposes = doc.importNode(elLoanPurposes, true);
                    elData.appendChild(ndLoanPurposes);

                } else {
                    elData.setTextContent("No Loan purposes Found");
                    enResponseStatus = FAILED;
                }
            }
            //get loan purposes -- end

            String strMin = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.APPLY_LOAN).getMinimum();
            String strMax = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.APPLY_LOAN).getMaximum();

            //create element AMOUNT_LIMITS and append to element DATA
            Element elWithdrawalLimits = doc.createElement("AMOUNT_LIMITS");
            Element elMinAmount = doc.createElement("MIN_AMOUNT");
            elMinAmount.setTextContent(String.valueOf(strMin));
            Element elMaxAmount = doc.createElement("MAX_AMOUNT");
            elMaxAmount.setTextContent(String.valueOf(strMax));
            elWithdrawalLimits.appendChild(elMinAmount);
            elWithdrawalLimits.appendChild(elMaxAmount);
            elData.appendChild(elWithdrawalLimits);

            System.out.println("\n\nTHE LOAN TYPE REQUEST:::\n\n");
            System.out.println(XmlUtils.convertNodeToStr(elData));

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

    public MAPPResponse accountBalanceEnquiry_PREV(MAPPRequest theMAPPRequest) {

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
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

            MAPPConstants.ResponseStatus enResponseStatus = ERROR;

            String strProduct = "";
            String strDate = "";
            String strBookBalance = "";
            String strAvailableBalance = "";

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

           /* TransactionWrapper<FlexicoreHashMap> balanceEnquiryChargesWrapper = CBSAPI.accountBalanceEnquiryCharges(UUID.randomUUID().toString(), strUsername, strAccountNo);

            if (balanceEnquiryChargesWrapper.hasErrors()) {
                strTitle = "ERROR: Account Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";

                enResponseStatus = ERROR;
            } else {


            }
            */

            String strMemberName = getUserFullName(theMAPPRequest, strUsername);

            TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiryWrapper = CBSAPI.accountBalanceEnquirySINGLE(strUsername,
                    "MSISDN", strUsername,
                    "APP_ID", strAppID, strAccountNo, "MAPP");

            FlexicoreHashMap accountBalanceResultMap = accountBalanceEnquiryWrapper.getSingleRecord();

            String strOriginatorId = UUID.randomUUID().toString();

            ChannelService channelService = new ChannelService();
            channelService.setOriginatorId(strOriginatorId);
            channelService.setTransactionCategory(AppConstants.ChargeServices.SINGLE_ACCOUNT_BALANCE_ENQUIRY.getValue());

            String strAccountName = "";

            if (accountBalanceEnquiryWrapper.hasErrors()) {
                strTitle = "ERROR: Account Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";

                enResponseStatus = ERROR;

                channelService.setTransactionStatusCode(104);
                channelService.setTransactionStatusName("FAILED");
                channelService.setTransactionStatusDescription(accountBalanceResultMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));
                strAccountName = strAccountNo;

            } else {

                //FlexicoreHashMap accountBalanceMap = accountBalanceResultMap.getFlexicoreHashMap("account_balance");
                strAccountName = accountBalanceResultMap.getStringValueOrIfNull("account_label", "").trim();

                String strAccountBalance = accountBalanceResultMap.getStringValueOrIfNull("account_balance", "0").trim();
                strBookBalance = accountBalanceResultMap.getStringValueOrIfNull("book_balance", "0").trim();

                strAccountBalance = Utils.formatDouble(strAccountBalance, "#,##0.00");
                strBookBalance = Utils.formatDouble(strBookBalance, "#,##0.00");

                strTitle = strAccountName;
                strResponseText = "Book Balance: <b>KES " + strBookBalance + "</b><br/> Available Balance: <b>KES " + strAccountBalance + "</b>";
                //strResponseText = "Available Balance: <b>KES " + strAccountBalance + "</b>";
                enResponseStatus = SUCCESS;
                strCharge = "YES";

                CBSAPI.SMSMSG cbsMSG = accountBalanceResultMap.getValue("msg_object");

                //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), "BALANCE_ENQUIRY", theMAPPRequest);

                channelService.setTransactionStatusCode(102);
                channelService.setTransactionStatusName("SUCCESS");
                channelService.setTransactionStatusDescription("Balance Enquiry Completed Successfully");
            }

            elData.setTextContent(strResponseText);

            channelService.setBeneficiaryReference("");
            channelService.setSourceReference("");
            channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

            channelService.setInitiatorType("MSISDN");
            channelService.setInitiatorIdentifier(strUsername);
            channelService.setInitiatorAccount(strUsername);
            channelService.setInitiatorName(strMemberName);
            channelService.setInitiatorReference(theMAPPRequest.getTraceID());
            channelService.setInitiatorApplication("MAPP");
            channelService.setInitiatorOtherDetails("<DATA/>");

            channelService.setSourceType("ACCOUNT_NO");
            channelService.setSourceIdentifier(strAccountNo);
            channelService.setSourceAccount(strAccountNo);
            channelService.setSourceName(strAccountName);
            channelService.setSourceApplication("CBS");
            channelService.setSourceOtherDetails("<DATA/>");

            channelService.setBeneficiaryType("MSISDN");
            channelService.setBeneficiaryIdentifier(strUsername);
            channelService.setBeneficiaryAccount(strUsername);
            channelService.setBeneficiaryName(strMemberName);
            channelService.setBeneficiaryApplication("MSISDN");
            channelService.setBeneficiaryOtherDetails("<DATA/>");

            channelService.setTransactionCurrency("KES");
            channelService.setTransactionAmount(0.00);

            TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.SINGLE_ACCOUNT_BALANCE_ENQUIRY.getValue(),
                    0.00);

            if (chargesWrapper.hasErrors()) {
                channelService.setTransactionCharge(0.00);
                channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

            } else {
                channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                channelService.setTransactionOtherDetails("<DATA/>");
            }

            channelService.setTransactionRemark("Balance Enquiry for A/C: " + strAccountNo);
            ChannelService.insertService(channelService);

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

    public MAPPResponse loanBalanceEnquiry_PREV(MAPPRequest theMAPPRequest) {

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

            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            System.out.println("\n\n" + XmlUtils.convertNodeToStr(ndRequestMSG) + "\n\n");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strMemberName = getUserFullName(theMAPPRequest, strUsername);

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();

            if (strLoanNo.isBlank()) {
                strLoanNo = configXPath.evaluate("LOAN/@SERIAL_NO", ndRequestMSG).trim();
            }

            //String strLoanNo = configXPath.evaluate("LOAN/@SERIAL_NO", ndRequestMSG).trim();
            //String strLoanName = configXPath.evaluate("LOAN/@NAME", ndRequestMSG).trim();
            //String strLoanCd = configXPath.evaluate("LOAN/OTHER_DETAILS/ACCOUNT_CD", ndRequestMSG).trim();
            String strCharge = "NO";
            String strLoanBalance = "";

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiryWrapper = CBSAPI.loanBalanceEnquiry(strUsername,
                    "MSISDN", strUsername, "APP_ID", strAppID, strLoanNo, "MAPP");

            FlexicoreHashMap accountBalanceMap = accountBalanceEnquiryWrapper.getSingleRecord();

            String strTitle = "";
            String strResponseText = "";

            if (accountBalanceEnquiryWrapper.hasErrors()) {
                strTitle = "ERROR: Loan Balance";
                strResponseText = "An error occurred. Please try again after a few minutes.";
                enResponseStatus = ERROR;
                enResponseAction = CON;
            } else {

                String strAccountName = accountBalanceMap.getStringValueOrIfNull("loan_name", "").trim();
                String strAccountSerialNumber = accountBalanceMap.getStringValueOrIfNull("loan_serial_number", "").trim();

                String strAccountBalance = accountBalanceMap.getStringValueOrIfNull("loan_balance", "0").trim();
                String strAccountInterestAmount = accountBalanceMap.getStringValueOrIfNull("interest_amount", "0").trim();

                double dblLoanBalance = Double.parseDouble(strAccountBalance);
                double dblLoanInterestBalance = Double.parseDouble(strAccountInterestAmount);

                double dblTotalLoanBalance = dblLoanBalance + dblLoanInterestBalance;

                strAccountBalance = Utils.formatDouble(strAccountBalance, "#,##0.00");
                strAccountInterestAmount = Utils.formatDouble(strAccountInterestAmount, "#,##0.00");

                strTitle = "Loan Balance";
                strResponseText = "Loan: <b>" + strAccountName + "-" + strAccountSerialNumber + "</b><br/> " +
                        "Balance: <b>KES " + strAccountBalance + "</b> <br/>" +
                        "Interest Balance: <b>KES " + strAccountInterestAmount + "</b> <br/>" +
                        "Total: <b>KES " + Utils.formatDouble(dblTotalLoanBalance, "#,##0.00") + "</b>"
                ;
                strCharge = "YES";

                enResponseStatus = SUCCESS;

                CBSAPI.SMSMSG cbsMSG = accountBalanceMap.getValue("msg_object");

                //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), "LOAN_BALANCE_ENQUIRY", theMAPPRequest);

                String strOriginatorId = UUID.randomUUID().toString();

                ChannelService channelService = new ChannelService();
                channelService.setOriginatorId(strOriginatorId);
                channelService.setTransactionCategory(AppConstants.ChargeServices.LOAN_BALANCE_ENQUIRY.getValue());

                if (accountBalanceEnquiryWrapper.hasErrors()) {
                    channelService.setTransactionStatusCode(104);
                    channelService.setTransactionStatusName("FAILED");
                    channelService.setTransactionStatusDescription(accountBalanceMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));
                } else {
                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Loan Balance Enquiry Completed Successfully");
                    channelService.setBeneficiaryReference("");
                    channelService.setSourceReference("");
                }
                channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

                channelService.setInitiatorType("MSISDN");
                channelService.setInitiatorIdentifier(strUsername);
                channelService.setInitiatorAccount(strUsername);
                channelService.setInitiatorName(strMemberName);
                channelService.setInitiatorReference(theMAPPRequest.getTraceID());
                channelService.setInitiatorApplication("USSD");
                channelService.setInitiatorOtherDetails("<DATA/>");

                channelService.setSourceType("ACCOUNT_NO");
                channelService.setSourceIdentifier(strLoanNo);
                channelService.setSourceAccount(strLoanNo);
                channelService.setSourceName(strLoanNo);
                channelService.setSourceApplication("CBS");
                channelService.setSourceOtherDetails("<DATA/>");

                channelService.setBeneficiaryType("MSISDN");
                channelService.setBeneficiaryIdentifier(strUsername);
                channelService.setBeneficiaryAccount(strUsername);
                channelService.setBeneficiaryName(strMemberName);
                channelService.setBeneficiaryApplication("CBS");
                channelService.setBeneficiaryOtherDetails("<DATA/>");

                channelService.setTransactionCurrency("KES");
                channelService.setTransactionAmount(0.00);

                TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.LOAN_BALANCE_ENQUIRY.getValue(),
                        0.00);

                if (chargesWrapper.hasErrors()) {
                    channelService.setTransactionCharge(0.00);
                    channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

                } else {
                    channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                    channelService.setTransactionOtherDetails("<DATA/>");
                }

                channelService.setTransactionRemark("Loan Balance Enquiry for A/C: " + strLoanNo);
                ChannelService.insertService(channelService);
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

            e.printStackTrace();
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

    public MAPPResponse checkLoanLimit_PREV(MAPPRequest theMAPPRequest) {

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

    public MAPPResponse checkLoanLimit(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println("checkLoanLimit");
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                //Request
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = theMAPPRequest.getAppID();
                long lnSessionID = theMAPPRequest.getSessionID();
                String strGUID = UUID.randomUUID().toString();

                Node ndRequestMSG = theMAPPRequest.getMSG();
               /* System.out.println("checkLOanLimit>");
                printXmlFromNode(ndRequestMSG);*/

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                // Root element - MSG
                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = TEXT;
                MAPPConstants.ResponseAction enResponseAction = CON;
                MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

                System.out.println("\n\ncheckLoanLimit:");
                System.out.println(XmlUtils.convertNodeToStr(ndRequestMSG));

                String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", ndRequestMSG).trim();
                String strAccountNo = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

                //String strLoanNo = configXPath.evaluate("LOAN_TYPE/@ID", ndRequestMSG).trim();

                String strLoanApplicationMaximum = getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.APPLY_LOAN).getMaximum();

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());

                String strEntryNo = UUID.randomUUID().toString().toUpperCase();
                // BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                String strTitle = "";
                String strResponseText = "";

                String strCharge = "NO";

                String strLoanApplicationStatus = "ERROR";
                String strLoanApplicationStatusDescription = "ERROR";

                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());
                String strRequestApplication = "MBANKING_SERVER";
                String strSourceApplication = "MAPP";

                // Thread worker = new Thread(() -> {

                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                TransactionWrapper<FlexicoreHashMap> checkLoanLimitWrapper = CBSAPI.checkLoanLimit(strUsername,
                        "MSISDN", strUsername, "APP_ID", strAppID, strLoanNo);

                FlexicoreHashMap checkLoanLimitMap = checkLoanLimitWrapper.getSingleRecord();
                CBSAPI.SMSMSG cbsMSG = checkLoanLimitMap.getValue("msg_object");

                //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), "CHECK_LOAN_LIMIT", theMAPPRequest);

                String strOriginatorId = UUID.randomUUID().toString();

                ChannelService channelService = new ChannelService();
                channelService.setOriginatorId(strOriginatorId);
                channelService.setTransactionCategory(AppConstants.ChargeServices.CHECK_LOAN_LIMIT.getValue());

                if (checkLoanLimitWrapper.hasErrors()) {
                    channelService.setTransactionStatusCode(104);
                    channelService.setTransactionStatusName("FAILED");
                    channelService.setTransactionStatusDescription(checkLoanLimitMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

                    System.err.println("MAPPAPI.checkLoanLimit() - Response " + checkLoanLimitMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

                } else {
                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Loan Qualification Check Completed Successfully");
                    channelService.setBeneficiaryReference("");
                    channelService.setSourceReference("");
                }
                channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

                channelService.setInitiatorType("MSISDN");
                channelService.setInitiatorIdentifier(strUsername);
                channelService.setInitiatorAccount(strUsername);
                channelService.setInitiatorName(strMemberName);
                channelService.setInitiatorReference(theMAPPRequest.getTraceID());
                channelService.setInitiatorApplication("USSD");
                channelService.setInitiatorOtherDetails("<DATA/>");

                channelService.setSourceType("ACCOUNT_NO");
                channelService.setSourceIdentifier(strLoanNo);
                channelService.setSourceAccount(strLoanNo);
                channelService.setSourceName(strLoanNo);
                channelService.setSourceApplication("CBS");
                channelService.setSourceOtherDetails("<DATA/>");

                channelService.setBeneficiaryType("MSISDN");
                channelService.setBeneficiaryIdentifier(strUsername);
                channelService.setBeneficiaryAccount(strUsername);
                channelService.setBeneficiaryName(strMemberName);
                channelService.setBeneficiaryApplication("CBS");
                channelService.setBeneficiaryOtherDetails("<DATA/>");

                channelService.setTransactionCurrency("KES");
                channelService.setTransactionAmount(0.00);

                TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.CHECK_LOAN_LIMIT.getValue(),
                        0.00);

                if (chargesWrapper.hasErrors()) {
                    channelService.setTransactionCharge(0.00);
                    channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

                } else {
                    channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                    channelService.setTransactionOtherDetails("<DATA/>");
                }

                channelService.setTransactionRemark("Loan Qualification Check for Loan: " + strLoanNo);
                ChannelService.insertService(channelService);

                //});
                //.start();

                FlexicoreHashMap loanLimitMap = checkLoanLimitMap.getFlexicoreHashMap("payload");

                String eligibleAmount = loanLimitMap.getStringValue("eligible_amount");
                if (strLoanNo.isBlank()) {
                    eligibleAmount = "0";
                }

                String reason = loanLimitMap.getStringValueOrIfNull("reason", "");

                String strFormattedAmount = Utils.formatDouble(eligibleAmount, "#,##0.00");

                String strResponse = "Eligible Amount: KES " + strFormattedAmount + "<br/>";

                if (!reason.isBlank()) {
                    strResponse = strResponse + "Reason:<br/>" + reason;
                }

                strTitle = "Loan Qualification - " + strLoanNo;
                strResponseText = strResponse;
                strCharge = "YES";
                enResponseAction = CON;
                enResponseStatus = SUCCESS;

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

    public MAPPResponse mobileMoneyWithdrawal(MAPPRequest theMAPPRequest) {
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {

                String strCategory = "MPESA_WITHDRAWAL";

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

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

                //TODO: ENSURE THIS IS BACK. REMOVED BY VINCENT
                /*if (!strRecipientMobileNumber.equals(strUsername)) {
                    dblWithdrawalMin = Double.parseDouble(getParam(CASH_WITHDRAWAL_TO_OTHER).getMinimum());
                    dblWithdrawalMax = Double.parseDouble(getParam(CASH_WITHDRAWAL_TO_OTHER).getMaximum());
                }*/


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

                    String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                    TransactionWrapper<FlexicoreHashMap> validateAccountNumberWrapper = CBSAPI.validateAccountNumber(strUsername,
                            "MSISDN", strUsername, strAccountNo);

                    FlexicoreHashMap accountDetails = validateAccountNumberWrapper.getSingleRecord();

                    PESA pesa = new PESA();

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strAppID = String.valueOf(theMAPPRequest.getAppID());

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, ke.skyworld.mbanking.pesaapi.PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);

                    long getProductID = Long.parseLong(pesaParam.getProductId());
                    String strSenderIdentifier = pesaParam.getSenderIdentifier();
                    String strSenderAccount = pesaParam.getSenderAccount();
                    String strSenderName = pesaParam.getSenderName();

                    String strReceiverName = strMemberName;
                    if (strRecipientMobileNumber.equalsIgnoreCase(strUsername)) {
                        strReceiverName = strMemberName;
                    } else {
                        strReceiverName = strRecipientMobileNumber;
                    }

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
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strAccountNo);
                    pesa.setSourceAccount(strAccountNo);
                    pesa.setSourceName(accountDetails.getStringValue("account_label"));
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("MBANKING_SERVER");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("MSISDN");
                    pesa.setReceiverIdentifier(strRecipientMobileNumber);
                    pesa.setReceiverAccount(strRecipientMobileNumber);
                    pesa.setReceiverName(strReceiverName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strRecipientMobileNumber);
                    pesa.setBeneficiaryAccount(strRecipientMobileNumber);
                    pesa.setBeneficiaryName(strReceiverName);
                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setCategory(strCategory);

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

                    TransactionWrapper<FlexicoreHashMap> mobileMoneyWithdrawalWrapper = CBSAPI.mobileMoneyWithdrawal(
                            strUsername,
                            "MSISDN",
                            strUsername,
                            "APP_ID",
                            strAppID,
                            pesa.getOriginatorID(),
                            String.valueOf(pesa.getProductID()),
                            pesa.getPESAType().getValue(),
                            pesa.getPESAAction().getValue(),
                            pesa.getCommand(),
                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getInitiatorType())
                                    .putValue("identifier", pesa.getInitiatorIdentifier())
                                    .putValue("account", pesa.getInitiatorAccount())
                                    .putValue("name", pesa.getInitiatorName())
                                    .putValue("reference", pesa.getInitiatorReference())
                                    .putValue("other_details", pesa.getInitiatorOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getSourceType())
                                    .putValue("identifier", pesa.getSourceIdentifier())
                                    .putValue("account", pesa.getSourceAccount())
                                    .putValue("name", pesa.getSourceName())
                                    .putValue("reference", pesa.getSourceReference())
                                    .putValue("other_details", pesa.getSourceOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getSenderType())
                                    .putValue("identifier", pesa.getSenderIdentifier())
                                    .putValue("account", pesa.getSenderAccount())
                                    .putValue("name", pesa.getSenderName())
                                    .putValue("reference", pesa.getSenderReference())
                                    .putValue("other_details", pesa.getSenderOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getReceiverType())
                                    .putValue("identifier", pesa.getReceiverIdentifier())
                                    .putValue("account", pesa.getReceiverAccount())
                                    .putValue("name", pesa.getReceiverName())
                                    .putValue("reference", pesa.getReceiverReference())
                                    .putValue("other_details", pesa.getReceiverOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getBeneficiaryType())
                                    .putValue("identifier", pesa.getBeneficiaryIdentifier())
                                    .putValue("account", pesa.getBeneficiaryAccount())
                                    .putValue("name", pesa.getBeneficiaryName())
                                    .putValue("reference", pesa.getBeneficiaryReference())
                                    .putValue("other_details", pesa.getBeneficiaryOtherDetails()),

                            pesa.getTransactionAmount(),
                            strCategory,
                            pesa.getTransactionRemark(),
                            strTraceID,
                            "MAPP",
                            "MBANKING");

                    FlexicoreHashMap mobileMoneyWithdrawalMap = mobileMoneyWithdrawalWrapper.getSingleRecord();

                    CBSAPI.SMSMSG cbsMSG = mobileMoneyWithdrawalMap.getValue("msg_object");

                    if (mobileMoneyWithdrawalWrapper.hasErrors()) {
                        //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theMAPPRequest);

                        strTitle = "ERROR: Withdrawal Failed";
                        strResponseText = mobileMoneyWithdrawalMap.getStringValueOrIfNull("display_message", "Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later.");

                        enResponseStatus = FAILED;
                        enResponseAction = CON;
                    } else {

                        String strFormattedAmount = Utils.formatDouble(strAmount, "#,##0.00");
                        String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                        String strSourceReference = mobileMoneyWithdrawalMap.getFlexicoreHashMap("response_payload").getStringValue("transaction_reference");
                        pesa.setSourceReference(strSourceReference);

                        String strMSG = "";

                        strAmount = Utils.formatAmount(strAmount);
                        if (PESAProcessor.sendPESA(pesa) > 0) {
                            //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theUSSDRequest);

                                /*strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been received successfully. Kindly wait as it is being processed.";
                                sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);*/

                            strCharge = "YES";
                            strTitle = "Request for Withdrawal";
                            strResponseText = "Your request to withdraw <b>KES " + strAmount + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                            enResponseStatus = SUCCESS;
                            enResponseAction = CON;

                        } else {

                            String strRefKey = UUID.randomUUID().toString();

                            TransactionWrapper<FlexicoreHashMap> reversalCashWithdrawalWrapper =

                                    CBSAPI.reverseMobileMoneyWithdrawal(
                                            strUsername,
                                            "MSISDN",
                                            strUsername,
                                            pesa.getOriginatorID(),
                                            pesa.getBeneficiaryType(),
                                            pesa.getBeneficiaryIdentifier(),
                                            pesa.getBeneficiaryName(),
                                            pesa.getBeneficiaryOtherDetails(),
                                            "",
                                            DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"));

                            if (!reversalCashWithdrawalWrapper.hasErrors()) {
                                strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " has been REVERSED. Dial " + AppConstants.strMBankingUSSDCode + " to check your balance.";
                            } else {
                                strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.";
                            }

                            //sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);

                            // sendSMS(strMobileNumber, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theUSSDRequest);

                            enResponseStatus = FAILED;
                            enResponseAction = CON;
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
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

                double dblUtilityETopUpMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMinimum());
                double dblUtilityETopUpMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.AIRTIME_PURCHASE).getMaximum());

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

                    String strAppID = String.valueOf(theMAPPRequest.getAppID());

                    String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                    TransactionWrapper<FlexicoreHashMap> validateAccountNumberWrapper = CBSAPI.validateAccountNumber(strUsername,
                            "MSISDN", strUsername, strAccountNo);
                    FlexicoreHashMap accountDetails = validateAccountNumberWrapper.getSingleRecord();

                    String strReceiverName = strMemberName;
                    if (strRecipientMobileNumber.equalsIgnoreCase(strUsername)) {
                        strReceiverName = strMemberName;
                    } else {
                        strReceiverName = strRecipientMobileNumber;
                    }

                    String strTransaction = "Airtime Request";
                    String strTransactionDescription = "Airtime Purchase by " + strRecipientMobileNumber;

                    String strCategory = "AIRTIME_PURCHASE";

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
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strAccountNo);
                    pesa.setSourceAccount(strAccountNo);
                    pesa.setSourceName(accountDetails.getStringValue("account_label"));
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("MBANKING_SERVER");
                    pesa.setSourceOtherDetails("<DATA/>");

                    pesa.setSenderType("SHORT_CODE");
                    pesa.setSenderIdentifier(strSenderIdentifier);
                    pesa.setSenderAccount(strSenderAccount);
                    pesa.setSenderName(strSenderName);
                    pesa.setSenderOtherDetails("<DATA/>");

                    pesa.setReceiverType("MSISDN");
                    pesa.setReceiverIdentifier(strRecipientMobileNumber);
                    pesa.setReceiverAccount(strRecipientMobileNumber);
                    pesa.setReceiverName(strReceiverName);
                    pesa.setReceiverOtherDetails("<DATA/>");

                    pesa.setBeneficiaryType("MSISDN");
                    pesa.setBeneficiaryIdentifier(strRecipientMobileNumber);
                    pesa.setBeneficiaryAccount(strRecipientMobileNumber);
                    pesa.setBeneficiaryName(strReceiverName);
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

                    TransactionWrapper<FlexicoreHashMap> buyAirtimeWrapper = CBSAPI.buyAirtime(
                            strUsername,
                            "MSISDN",
                            strUsername,
                            "APP_ID",
                            strAppID,
                            pesa.getOriginatorID(),
                            String.valueOf(pesa.getProductID()),
                            pesa.getPESAType().getValue(),
                            pesa.getPESAAction().getValue(),
                            pesa.getCommand(),
                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getInitiatorType())
                                    .putValue("identifier", pesa.getInitiatorIdentifier())
                                    .putValue("account", pesa.getInitiatorAccount())
                                    .putValue("name", pesa.getInitiatorName())
                                    .putValue("reference", pesa.getInitiatorReference())
                                    .putValue("other_details", pesa.getInitiatorOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getSourceType())
                                    .putValue("identifier", pesa.getSourceIdentifier())
                                    .putValue("account", pesa.getSourceAccount())
                                    .putValue("name", pesa.getSourceName())
                                    .putValue("reference", pesa.getSourceReference())
                                    .putValue("other_details", pesa.getSourceOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getSenderType())
                                    .putValue("identifier", pesa.getSenderIdentifier())
                                    .putValue("account", pesa.getSenderAccount())
                                    .putValue("name", pesa.getSenderName())
                                    .putValue("reference", pesa.getSenderReference())
                                    .putValue("other_details", pesa.getSenderOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getReceiverType())
                                    .putValue("identifier", pesa.getReceiverIdentifier())
                                    .putValue("account", pesa.getReceiverAccount())
                                    .putValue("name", pesa.getReceiverName())
                                    .putValue("reference", pesa.getReceiverReference())
                                    .putValue("other_details", pesa.getReceiverOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getBeneficiaryType())
                                    .putValue("identifier", pesa.getBeneficiaryIdentifier())
                                    .putValue("account", pesa.getBeneficiaryAccount())
                                    .putValue("name", pesa.getBeneficiaryName())
                                    .putValue("reference", pesa.getBeneficiaryReference())
                                    .putValue("other_details", pesa.getBeneficiaryOtherDetails()),

                            pesa.getTransactionAmount(),
                            strCategory,
                            pesa.getTransactionRemark(),
                            strTraceID,
                            "MAPP",
                            "MBANKING");

                    FlexicoreHashMap buyAirtimeMap = buyAirtimeWrapper.getSingleRecord();

                    CBSAPI.SMSMSG cbsMSG = buyAirtimeMap.getValue("msg_object");

                    if (buyAirtimeWrapper.hasErrors()) {
                        // sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theMAPPRequest);

                        strTitle = "ERROR: Airtime Purchase Failed";
                        strResponseText = buyAirtimeMap.getStringValueOrIfNull("display_message", "Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later.");

                        enResponseStatus = FAILED;
                        enResponseAction = CON;
                    } else {

                        String strFormattedAmount = Utils.formatDouble(strAmount, "#,##0.00");
                        String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                        String strSourceReference = buyAirtimeMap.getFlexicoreHashMap("response_payload").getStringValue("transaction_reference");
                        pesa.setSourceReference(strSourceReference);

                        String strMSG = "";

                        strAmount = Utils.formatAmount(strAmount);

                        if (PESAProcessor.sendPESA(pesa) > 0) {
                            //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theUSSDRequest);

                                /*strMSG = "Dear member, your Airtime Purchase request of KES " + strAmount + " to " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been received successfully. Kindly wait as it is being processed.";

                                sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);*/

                            strCharge = "YES";
                            strTitle = "Request for Airtime Top-up";
                            strResponseText = "Your request to top up airtime of <b>KES " + strAmount + "</b><br/>For :<b>+" + pesa.getBeneficiaryIdentifier() + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                            enResponseStatus = SUCCESS;
                            enResponseAction = CON;

                        } else {

                            String strRefKey = UUID.randomUUID().toString();

                            TransactionWrapper<FlexicoreHashMap> reversalCashWithdrawalWrapper =
                                    CBSAPI.reverseMobileMoneyWithdrawal(
                                            strUsername,
                                            "MSISDN",
                                            strUsername,
                                            pesa.getOriginatorID(),
                                            pesa.getBeneficiaryType(),
                                            pesa.getBeneficiaryIdentifier(),
                                            pesa.getBeneficiaryName(),
                                            pesa.getBeneficiaryOtherDetails(),
                                            "",
                                            DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"));

                            if (!reversalCashWithdrawalWrapper.hasErrors()) {
                                strMSG = "Dear member, your Airtime Purchase request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " has been REVERSED. Dial " + AppConstants.strMBankingUSSDCode + " to check your balance.";
                            } else {
                                strMSG = "Dear member, your Airtime Purchase request of KES " + strAmount + " to " + strRecipientMobileNumber + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.";
                            }

                            //sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);

                            // sendSMS(strMobileNumber, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theUSSDRequest);

                            enResponseStatus = FAILED;
                            enResponseAction = CON;
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                strPassword = APIUtils.hashPIN(strPassword, strUsername);

                String strTraceID = theMAPPRequest.getTraceID();

                String strAppID = String.valueOf(theMAPPRequest.getAppID());

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

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum());

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

                    String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                    TransactionWrapper<FlexicoreHashMap> validateAccountNumberWrapper = CBSAPI.validateAccountNumber(strUsername,
                            "MSISDN", strUsername, strFromAccountNo);

                    FlexicoreHashMap accountDetails = validateAccountNumberWrapper.getSingleRecord();


                    String strCategory = "BANK_TRANSFER";

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
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strFromAccountNo);
                    pesa.setSourceAccount(strFromAccountNo);
                    pesa.setSourceName(accountDetails.getStringValue("account_label"));
                    pesa.setSourceReference(strMAPPSessionID);
                    pesa.setSourceApplication("MBANKING_SERVER");
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
                    pesa.setCategory(strCategory);

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

                    TransactionWrapper<FlexicoreHashMap> bankTransferWrapper =
                            CBSAPI.bankTransferViaB2B(
                                    strUsername,
                                    "MSISDN",
                                    strUsername,
                                    "APP_ID",
                                    strAppID,
                                    pesa.getOriginatorID(),
                                    String.valueOf(pesa.getProductID()),
                                    pesa.getPESAType().getValue(),
                                    pesa.getPESAAction().getValue(),
                                    pesa.getCommand(),
                                    new FlexicoreHashMap()
                                            .putValue("identifier_type", pesa.getInitiatorType())
                                            .putValue("identifier", pesa.getInitiatorIdentifier())
                                            .putValue("account", pesa.getInitiatorAccount())
                                            .putValue("name", pesa.getInitiatorName())
                                            .putValue("reference", pesa.getInitiatorReference())
                                            .putValue("other_details", pesa.getInitiatorOtherDetails()),

                                    new FlexicoreHashMap()
                                            .putValue("identifier_type", pesa.getSourceType())
                                            .putValue("identifier", pesa.getSourceIdentifier())
                                            .putValue("account", pesa.getSourceAccount())
                                            .putValue("name", pesa.getSourceName())
                                            .putValue("reference", pesa.getSourceReference())
                                            .putValue("other_details", pesa.getSourceOtherDetails()),

                                    new FlexicoreHashMap()
                                            .putValue("identifier_type", pesa.getSenderType())
                                            .putValue("identifier", pesa.getSenderIdentifier())
                                            .putValue("account", pesa.getSenderAccount())
                                            .putValue("name", pesa.getSenderName())
                                            .putValue("reference", pesa.getSenderReference())
                                            .putValue("other_details", pesa.getSenderOtherDetails()),

                                    new FlexicoreHashMap()
                                            .putValue("identifier_type", pesa.getReceiverType())
                                            .putValue("identifier", pesa.getReceiverIdentifier())
                                            .putValue("account", pesa.getReceiverAccount())
                                            .putValue("name", pesa.getReceiverName())
                                            .putValue("reference", pesa.getReceiverReference())
                                            .putValue("other_details", pesa.getReceiverOtherDetails()),

                                    new FlexicoreHashMap()
                                            .putValue("identifier_type", pesa.getBeneficiaryType())
                                            .putValue("identifier", pesa.getBeneficiaryIdentifier())
                                            .putValue("account", pesa.getBeneficiaryAccount())
                                            .putValue("name", pesa.getBeneficiaryName())
                                            .putValue("reference", pesa.getBeneficiaryReference())
                                            .putValue("other_details", pesa.getBeneficiaryOtherDetails()),

                                    pesa.getTransactionAmount(),
                                    strCategory,
                                    pesa.getTransactionRemark(),
                                    strTraceID,
                                    "MAPP",
                                    "MBANKING");

                    FlexicoreHashMap bankTransferMap = bankTransferWrapper.getSingleRecord();

                    CBSAPI.SMSMSG cbsMSG = bankTransferMap.getValue("msg_object");

                    if (bankTransferWrapper.hasErrors()) {
                        //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theMAPPRequest);

                        strTitle = "ERROR: Bank Transfer Failed";
                        strResponseText = bankTransferMap.getStringValueOrIfNull("display_message", "Sorry, an error occurred while processing your request. Please try again later.");

                        enResponseStatus = FAILED;
                        enResponseAction = CON;
                    } else {

                        String strFormattedAmount = Utils.formatDouble(strAmount, "#,##0.00");
                        String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                        String strSourceReference = bankTransferMap.getFlexicoreHashMap("response_payload").getStringValue("transaction_reference");
                        pesa.setSourceReference(strSourceReference);

                        String strMSG = "";

                        strAmount = Utils.formatAmount(strAmount);

                        if (PESAProcessor.sendPESA(pesa) > 0) {
                            //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theUSSDRequest);

                            strCharge = "YES";
                                /*strMSG = "Dear member, your Bank Transfer request of KES " + strAmount + " to " + strBankName + " - " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been received successfully. Kindly wait as it is being processed.";

                                sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);*/

                            strTitle = "Bank Transfer";
                            strResponseText = "Your request to transfer <b>KES " + strAmount + "</b> to has been received successfully.<br/>Kindly wait shortly as it is being processed";

                            enResponseStatus = SUCCESS;
                            enResponseAction = CON;

                        } else {

                            String strRefKey = UUID.randomUUID().toString();

                            TransactionWrapper<FlexicoreHashMap> reversalWrapper =
                                    CBSAPI.reverseMobileMoneyWithdrawal(
                                            strUsername,
                                            "MSISDN",
                                            strUsername,
                                            pesa.getOriginatorID(),
                                            pesa.getBeneficiaryType(),
                                            pesa.getBeneficiaryIdentifier(),
                                            pesa.getBeneficiaryName(),
                                            pesa.getBeneficiaryOtherDetails(),
                                            "",
                                            DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"));

                            if (!reversalWrapper.hasErrors()) {
                                strMSG = "Dear member, your Bank Transfer request of KES KES " + strFormattedAmount + " to " + pesa.getReceiverIdentifier() + " - " + pesa.getReceiverName() + ", A/C " + pesa.getReceiverAccount() + " on " + strFormattedDateTime + " has been REVERSED. Dial " + AppConstants.strMBankingUSSDCode + " to check your balance.";
                            } else {
                                strMSG = "Dear member, your Bank Transfer request of KES KES " + strFormattedAmount + " to " + pesa.getReceiverIdentifier() + " - " + pesa.getReceiverName() + ", A/C " + pesa.getReceiverAccount() + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.";
                            }

                            //sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);

                            enResponseStatus = FAILED;
                            enResponseAction = CON;
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

    public MAPPResponse payBill(MAPPRequest theMAPPRequest){
        MAPPResponse theMAPPResponse = null;

        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "()");
            XPath configXPath = XPathFactory.newInstance().newXPath();

            MAPPResponse mrOTPVerificationMappResponse = null;
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if(otp.isEnabled()){
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if(!strAction.equals("CON") || !strStatus.equals("SUCCESS")){
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if(otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
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

                double dblWithdrawalMin = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMinimum());
                double dblWithdrawalMax = Double.parseDouble(getParam(MAPPAPIConstants.MAPP_PARAM_TYPE.PAY_BILL).getMaximum());

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
                    String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                    TransactionWrapper<FlexicoreHashMap> validateAccountNumberWrapper = CBSAPI.validateAccountNumber(strUsername,
                            "MSISDN", strUsername, strFromAccountNo);

                    FlexicoreHashMap accountDetails = validateAccountNumberWrapper.getSingleRecord();

                    PESA pesa = new PESA();

                    String strCategory = "BILL_PAYMENT";

                    String strDate = MBankingDB.getDBDateTime().trim();
                    String strAppID = String.valueOf(theMAPPRequest.getAppID());

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
                    pesa.setInitiatorName(strMemberName);
                    pesa.setInitiatorReference(strTraceID);
                    pesa.setInitiatorApplication("MAPP");
                    pesa.setInitiatorOtherDetails("<DATA/>");

                    pesa.setSourceType("ACCOUNT_NO");
                    pesa.setSourceIdentifier(strFromAccountNo);
                    pesa.setSourceAccount(strFromAccountNo);
                    pesa.setSourceName(accountDetails.getStringValue("account_label"));
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
                    pesa.setBeneficiaryName(strMemberName);

                    pesa.setBeneficiaryOtherDetails("<DATA/>");

                    pesa.setBatchReference(strMAPPSessionID);
                    pesa.setCorrelationReference(strTraceID);
                    pesa.setCorrelationApplication("MAPP");
                    pesa.setTransactionCurrency("KES");
                    pesa.setTransactionAmount(Double.parseDouble(strAmount));
                    pesa.setTransactionRemark(strTransactionDescription);
                    pesa.setCategory("BILL_PAYMENT");

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

                    TransactionWrapper<FlexicoreHashMap> utilityPaymentWrapper = CBSAPI.utilitiesPayment(
                            strUsername,
                            "MSISDN",
                            strUsername,
                            "APP_ID",
                            strAppID,
                            pesa.getOriginatorID(),
                            String.valueOf(pesa.getProductID()),
                            pesa.getPESAType().getValue(),
                            pesa.getPESAAction().getValue(),
                            pesa.getCommand(),
                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getInitiatorType())
                                    .putValue("identifier", pesa.getInitiatorIdentifier())
                                    .putValue("account", pesa.getInitiatorAccount())
                                    .putValue("name", pesa.getInitiatorName())
                                    .putValue("reference", pesa.getInitiatorReference())
                                    .putValue("other_details", pesa.getInitiatorOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getSourceType())
                                    .putValue("identifier", pesa.getSourceIdentifier())
                                    .putValue("account", pesa.getSourceAccount())
                                    .putValue("name", pesa.getSourceName())
                                    .putValue("reference", pesa.getSourceReference())
                                    .putValue("other_details", pesa.getSourceOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getSenderType())
                                    .putValue("identifier", pesa.getSenderIdentifier())
                                    .putValue("account", pesa.getSenderAccount())
                                    .putValue("name", pesa.getSenderName())
                                    .putValue("reference", pesa.getSenderReference())
                                    .putValue("other_details", pesa.getSenderOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getReceiverType())
                                    .putValue("identifier", pesa.getReceiverIdentifier())
                                    .putValue("account", pesa.getReceiverAccount())
                                    .putValue("name", pesa.getReceiverName())
                                    .putValue("reference", pesa.getReceiverReference())
                                    .putValue("other_details", pesa.getReceiverOtherDetails()),

                            new FlexicoreHashMap()
                                    .putValue("identifier_type", pesa.getBeneficiaryType())
                                    .putValue("identifier", pesa.getBeneficiaryIdentifier())
                                    .putValue("account", pesa.getBeneficiaryAccount())
                                    .putValue("name", pesa.getBeneficiaryName())
                                    .putValue("reference", pesa.getBeneficiaryReference())
                                    .putValue("other_details", pesa.getBeneficiaryOtherDetails()),

                            pesa.getTransactionAmount(),
                            strCategory,
                            pesa.getTransactionRemark(),
                            strTraceID,
                            "MAPP",
                            "MBANKING");

                    FlexicoreHashMap utilityPaymentMap = utilityPaymentWrapper.getSingleRecord();

                    CBSAPI.SMSMSG cbsMSG = utilityPaymentMap.getValue("msg_object");

                    if (utilityPaymentWrapper.hasErrors()) {
                        //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theMAPPRequest);

                        strTitle = "ERROR: Payment Failed";
                        strResponseText = utilityPaymentMap.getStringValueOrIfNull("display_message", "Sorry, an error occurred while processing your Paybill request. Please try again later.");

                        enResponseStatus = FAILED;
                        enResponseAction = CON;
                    }
                    else {

                        String strFormattedAmount = Utils.formatDouble(strAmount, "#,##0.00");
                        String strFormattedDateTime = Utils.formatDate(strDate, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                        String strSourceReference = utilityPaymentMap.getFlexicoreHashMap("response_payload").getStringValue("transaction_reference");
                        pesa.setSourceReference(strSourceReference);

                        String strMSG = "";

                        strAmount = Utils.formatAmount(strAmount);

                        if (PESAProcessor.sendPESA(pesa) > 0) {
                            //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theUSSDRequest);

                                /*strMSG = "Dear member, your Bill Payment request of KES " + strAmount + " to " + pesa.getReceiverName() + ", beneficiary " + pesa.getBeneficiaryIdentifier() + " on " + strFormattedDateTime + " has been received successfully. Kindly wait as it is being processed.";

                                sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);*/

                            strCharge = "YES";
                            strTitle =  "Utility Payment";
                            strResponseText = "Your payment of <b>KES " + strAmount + "</b> has been received successfully.<br/>Kindly wait shortly as it is being processed";

                            enResponseStatus = SUCCESS;
                            enResponseAction = CON;

                        } else {

                            String strRefKey = UUID.randomUUID().toString();

                            TransactionWrapper<FlexicoreHashMap> reversalCashWithdrawalWrapper =
                                    CBSAPI.reverseMobileMoneyWithdrawal(
                                            strUsername,
                                            "MSISDN",
                                            strUsername,
                                            pesa.getOriginatorID(),
                                            pesa.getBeneficiaryType(),
                                            pesa.getBeneficiaryIdentifier(),
                                            pesa.getBeneficiaryName(),
                                            pesa.getBeneficiaryOtherDetails(),
                                            "",
                                            DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"));

                            if (!reversalCashWithdrawalWrapper.hasErrors()) {
                                strMSG = "Dear member, your Bill Payment request of KES " + strFormattedAmount + " to " + pesa.getReceiverIdentifier() + " - " + pesa.getReceiverName() + ", A/C " + pesa.getReceiverAccount() + " on " + strFormattedDateTime + " has been REVERSED. Dial " + AppConstants.strMBankingUSSDCode + " to check your balance.";
                            } else {
                                strMSG = "Dear member, your Bill Payment request of KES " + strFormattedAmount + " to " + pesa.getReceiverIdentifier() + " - " + pesa.getReceiverName() + ", A/C " + pesa.getReceiverAccount() + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.";
                            }

                            //sendSMS(strUsername, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theMAPPRequest);

                            enResponseStatus = FAILED;
                            enResponseAction = CON;
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
            String strRepaymentOption = configXPath.evaluate("REPAYMENT_OPTION/@TYPE", ndRequestMSG).trim();
            String strAccount = configXPath.evaluate("REPAYMENT_OPTION", ndRequestMSG).trim();
            //String strAccount = configXPath.evaluate("ACCOUNT_NO", ndRequestMSG).trim();

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


                    String strTransactionDescription = "Loan Repayment. Source A/C: " + strAccount + " - Destination A/C: " + strLoanId;

                   /* String strAction = "IFT_ACCOUNT_TO_ACCOUNT";

                    HashMap<String,String> hmRVal = CBSAPI.internalFundsTransfer(strTraceID, "MSISDN", strUsername, strPassword,"APP_ID", strAppID,
                            strTransactionReference, strSourceAccount, strDestinationAccount, strAmount, strTransactionID,
                            "MBANKING_SERVER", "MAPP", strTransactionDescription, MBankingDB.getDBDateTime(), strAction);*/


                    String strOriginatorId = UUID.randomUUID().toString();
                    TransactionWrapper<FlexicoreHashMap> loanPaymentViaSavingsWrapper = CBSAPI.loanPaymentViaSavings(
                            strUsername,
                            "MSISDN",
                            strUsername,
                            "APP_ID",
                            strAppID,
                            strOriginatorId,
                            strAccount,
                            strLoanId,
                            Double.parseDouble(strAmount),
                            strTransactionDescription,
                            theMAPPRequest.getTraceID(),
                            "MAPP",
                            "MBANKING");


                    FlexicoreHashMap loanPaymentViaSavingMap = loanPaymentViaSavingsWrapper.getSingleRecord();

                    String strTitle = "";
                    String strResponseText = "";

                    CBSAPI.SMSMSG cbsMSG = loanPaymentViaSavingMap.getValue("msg_object");

                    String strCharge = "NO";


                    ChannelService channelService = new ChannelService();
                    channelService.setOriginatorId(strOriginatorId);
                    channelService.setTransactionCategory("LOAN_PAYMENT_VIA_SAVINGS");

                    if (loanPaymentViaSavingsWrapper.hasErrors()) {
                        strTitle = loanPaymentViaSavingMap.getStringValue("title");
                        strResponseText = loanPaymentViaSavingMap.getStringValue("display_message");
                        enResponseAction = CON;
                        enResponseStatus = FAILED;

                        channelService.setTransactionStatusCode(104);
                        channelService.setTransactionStatusName("FAILED");
                        channelService.setTransactionStatusDescription(loanPaymentViaSavingMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));


                    } else {
                        strTitle = "Transaction Accepted";
                        strResponseText = "Your Loan Payment request has been completed successfully.";
                        strCharge = "YES";
                        enResponseAction = CON;
                        enResponseStatus = SUCCESS;

                        channelService.setTransactionStatusCode(102);
                        channelService.setTransactionStatusName("SUCCESS");
                        channelService.setTransactionStatusDescription("Transaction Completed Successfully");
                        channelService.setBeneficiaryReference(loanPaymentViaSavingMap.getStringValue("cbs_transaction_reference"));
                        channelService.setSourceReference(loanPaymentViaSavingMap.getStringValue("cbs_transaction_reference"));

                    }

                    /*if (cbsMSG != null) {
                        sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), "LOAN_PAYMENT", theMAPPRequest);
                    }*/

                    channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

                    channelService.setInitiatorType("MSISDN");
                    channelService.setInitiatorIdentifier(strUsername);
                    channelService.setInitiatorAccount(strUsername);
                    channelService.setInitiatorName(strUsername);
                    channelService.setInitiatorReference(theMAPPRequest.getTraceID());
                    channelService.setInitiatorApplication("MAPP");
                    channelService.setInitiatorOtherDetails("<DATA/>");

                    channelService.setSourceType("ACCOUNT_NO");
                    channelService.setSourceIdentifier(strAccount);
                    channelService.setSourceAccount(strAccount);
                    channelService.setSourceName(strAccount);
                    channelService.setSourceApplication("CBS");
                    channelService.setSourceOtherDetails("<DATA/>");

                    channelService.setBeneficiaryType("ACCOUNT_NO");
                    channelService.setBeneficiaryIdentifier(strLoanId);
                    channelService.setBeneficiaryAccount(strLoanId);
                    channelService.setBeneficiaryName(strLoanId);
                    channelService.setBeneficiaryApplication("CBS");
                    channelService.setBeneficiaryOtherDetails("<DATA/>");

                    channelService.setTransactionCurrency("KES");
                    channelService.setTransactionAmount(Double.parseDouble(strAmount));

                    TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.IFT_LOAN_REPAYMENT.getValue(),
                            Double.parseDouble(strAmount));

                    if (chargesWrapper.hasErrors()) {
                        channelService.setTransactionCharge(0.00);
                        channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

                    } else {
                        channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                        channelService.setTransactionOtherDetails("<DATA/>");
                    }

                    channelService.setTransactionRemark(strTransactionDescription);
                    ChannelService.insertService(channelService);

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

    public MAPPResponse fundsTransfer_PREV(MAPPRequest theMAPPRequest) {
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
                //Request
                String strUsername = theMAPPRequest.getUsername();
                String strPassword = theMAPPRequest.getPassword();
                String strAppID = theMAPPRequest.getAppID();

                Node ndRequestMSG = theMAPPRequest.getMSG();

                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                // Root element - MSG
                Document doc = docBuilder.newDocument();

                MAPPConstants.ResponsesDataType enDataType = TEXT;

                MAPPConstants.ResponseAction enResponseAction = CON;
                MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

                System.out.println("THE REQUEST: ");
                System.out.println(XmlUtils.convertNodeToStr(ndRequestMSG));

                String strFromAccountNo = configXPath.evaluate("FROM_ACCOUNT_NO", ndRequestMSG).trim();

                String[] strSourceArr = strFromAccountNo.split(Pattern.quote("||"));
                strFromAccountNo = strSourceArr[0];

                String strToAccountNo = configXPath.evaluate("TO_ACCOUNT_NO", ndRequestMSG).trim();
                String strToOption = configXPath.evaluate("TRANSFER_OPTION", ndRequestMSG).trim();
                String strAmount = configXPath.evaluate("AMOUNT", ndRequestMSG).trim();
                //String strAccountNo = strToAccountNo;

                //TODO: Ask Isaac for best way

                /*if(!(strToOption.equals("Account") || strToOption.equals("Account Number"))){
                    HashMap<Object, Object> accountDetails = getUserDetails(theMAPPRequest, strToOption, strAccountNo);

                    HashMap<String, HashMap <String, String>>  hmIFTDestAccounts = (HashMap<String, HashMap <String, String>>) accountDetails.get("accounts");
                    HashMap<String, String>  hmMemberDetails = (HashMap<String, String>) accountDetails.get("user_details");

                    if (hmMemberDetails != null && !hmMemberDetails.isEmpty()) {
                        strAccountNo = hmIFTDestAccounts.entrySet().iterator().next().getValue().get("number");
                    }
                }*/


                String strDestination = "ACCOUNT";

                if (strToOption.equals("ID Number")) {
                    strDestination = "CUSTOMER_NO";
                } else if (strToOption.equals("Mobile Number")) {
                    strDestination = "MSISDN";
                }

                //END

                BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

                String strSessionID = String.valueOf(theMAPPRequest.getSessionID());
                String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

                String strTransactionReference = strTransactionID;
                String strSourceAccount = strFromAccountNo;
                //String strDestinationAccount = strAccountNo;

                String strTraceID = getTraceID(theMAPPRequest);
                String strTransactionDescription = "Internal Funds Transfer. Source A/C: " + strSourceAccount + " - Destination A/C: " + strToAccountNo;

                String strTitle = "";
                String strResponseText = "";
                String strCharge = "NO";

                String strOriginatorId = UUID.randomUUID().toString();
                TransactionWrapper<FlexicoreHashMap> internalFundsTransferWrapper = CBSAPI.internalFundsTransfer(
                        strUsername,
                        "MSISDN",
                        strUsername,
                        "APP_ID",
                        strAppID,
                        strOriginatorId,
                        strSourceAccount,
                        strToAccountNo,
                        Double.parseDouble(strAmount),
                        strTransactionDescription,
                        theMAPPRequest.getTraceID(),
                        "MAPP",
                        "MBANKING");

                FlexicoreHashMap internalFundsTransferMap = internalFundsTransferWrapper.getSingleRecord();

                CBSAPI.SMSMSG cbsMSG = internalFundsTransferMap.getValue("msg_object");
                //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), "INTERNAL_FUNDS_TRANSFER", theMAPPRequest);

                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                ChannelService channelService = new ChannelService();
                channelService.setOriginatorId(strOriginatorId);
                channelService.setTransactionCategory("INTERNAL_FUNDS_TRANSFER");

                if (internalFundsTransferWrapper.hasErrors()) {

                    strTitle = "ERROR: Internal Funds Transfer";
                    strResponseText = internalFundsTransferMap.getStringValueOrIfNull("display_message", "An error occurred. Please try again after a few minutes.");

                    enResponseStatus = ERROR;

                    channelService.setTransactionStatusCode(104);
                    channelService.setTransactionStatusName("FAILED");
                    channelService.setTransactionStatusDescription(internalFundsTransferMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));
                } else {

                    strTitle = "Transaction Accepted";
                    strResponseText = "Your funds transfer has been received successfully. Kindly wait as it is being processed.";
                    strCharge = "YES";

                    enResponseStatus = SUCCESS;

                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Transaction Received Successfully");
                    channelService.setBeneficiaryReference(internalFundsTransferMap.getStringValue("cbs_transaction_reference"));
                    channelService.setSourceReference(internalFundsTransferMap.getStringValue("cbs_transaction_reference"));
                }
                channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

                channelService.setInitiatorType("MSISDN");
                channelService.setInitiatorIdentifier(strUsername);
                channelService.setInitiatorAccount(strUsername);
                channelService.setInitiatorName(strMemberName);
                channelService.setInitiatorReference(theMAPPRequest.getTraceID());
                channelService.setInitiatorApplication("MAPP");
                channelService.setInitiatorOtherDetails("<DATA/>");

                channelService.setSourceType("ACCOUNT_NO");
                channelService.setSourceIdentifier(strSourceAccount);
                channelService.setSourceAccount(strSourceAccount);
                channelService.setSourceName(strSourceAccount);
                channelService.setSourceApplication("CBS");
                channelService.setSourceOtherDetails("<DATA/>");

                channelService.setBeneficiaryType("ACCOUNT_NO");
                channelService.setBeneficiaryIdentifier(strToAccountNo);
                channelService.setBeneficiaryAccount(strToAccountNo);
                channelService.setBeneficiaryName(strToAccountNo);
                channelService.setBeneficiaryApplication("CBS");
                channelService.setBeneficiaryOtherDetails("<DATA/>");

                channelService.setTransactionCurrency("KES");
                channelService.setTransactionAmount(Double.parseDouble(strAmount));

                TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername,
                        AppConstants.ChargeServices.IFT_ACCOUNT_TO_ACCOUNT.getValue(),
                        Double.parseDouble(strAmount));

                if (chargesWrapper.hasErrors()) {
                    channelService.setTransactionCharge(0.00);
                    channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

                } else {
                    channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                    channelService.setTransactionOtherDetails("<DATA/>");
                }

                channelService.setTransactionRemark(strTransactionDescription);
                ChannelService.insertService(channelService);

                enResponseAction = CON;

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
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse accountStatement_PREV(MAPPRequest theMAPPRequest) {

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

    public MAPPResponse accountStatement(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            // strPassword = APIUtils.hashPIN(strPassword, strUsername);
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

            strMaximumTransactionCount = "6";

            intMaximumTransactionCount = Integer.parseInt("100");

            /*if (!strMaximumTransactionCount.equals("")) {
                intMaximumTransactionCount = Integer.parseInt("6");
            }

            intMaximumTransactionCount = Integer.parseInt("6");*/

            /*End of Duration Change*/

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());


            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Account Statement";

            Element elData = doc.createElement("DATA");
            String strCharge = "NO";

            MAPPConstants.ResponsesDataType enDataType = TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            TransactionWrapper<FlexicoreHashMap> miniStatementWrapper = CBSAPI.accountFullStatement(strUsername, "MSISDN", strUsername,
                    "APP_ID", strAppID, strAccountNo, "100", strStartDate + " 00:00:00", strEndDate + " 23:59:59");

            FlexicoreHashMap miniStatementMap = miniStatementWrapper.getSingleRecord();

            String strMemberName = getUserFullName(theMAPPRequest, strUsername);

            String strOriginatorId = UUID.randomUUID().toString();

            ChannelService channelService = new ChannelService();
            channelService.setOriginatorId(strOriginatorId);
            channelService.setTransactionCategory(AppConstants.ChargeServices.ACCOUNT_FULL_STATEMENT.getValue());

            if (miniStatementWrapper.hasErrors()) {
                strTitle = "Error: Account Statement Failed";
                elData.setTextContent(miniStatementMap.getStringValueOrIfNull("display_message", "Sorry, account statement could not be generated at the moment. Please try again later."));
                enResponseStatus = ERROR;

                channelService.setTransactionStatusCode(104);
                channelService.setTransactionStatusName("FAILED");
                channelService.setTransactionStatusDescription(miniStatementMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

            } else {
                FlexicoreArrayList allTransactionsList = miniStatementMap.getFlexicoreArrayList("payload");
                if (allTransactionsList.isEmpty()) {
                    enResponseStatus = FAILED;
                    strCharge = "NO";
                    strTitle = "Error: No Statements Found";
                    elData.setTextContent("You do not have any statements within this time period");

                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("You do not have any statements within this time period");

                } else {

                    String strAvailableBalance = miniStatementMap.getStringValue("account_available_balance");

                    strAvailableBalance = Utils.formatDouble(strAvailableBalance, "#,##0.00");

                    Element elBalance = doc.createElement("BALANCE");
                    elBalance.setTextContent(strAvailableBalance);
                    elData.appendChild(elBalance);

                    Element elAccountNo = doc.createElement("ACCOUNTNO");
                    elAccountNo.setTextContent(strAccountNo);
                    elData.appendChild(elAccountNo);

                    Element elAccountName = doc.createElement("NAME");
                    elAccountName.setTextContent(miniStatementMap.getStringValue("account_name"));
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

                    int i = 0;
                    for (FlexicoreHashMap transactionMap : allTransactionsList) {
                        String strMSGTransactionReference = transactionMap.getStringValue("transaction_reference");
                        String strMSGFormattedTransactionDateTime = transactionMap.getStringValue("transaction_date_time");
                        String strMSGFormattedTransactionAmount = transactionMap.getStringValue("transaction_amount");
                        String strTransactionType = transactionMap.getStringValue("transaction_type");
                        String strMSGTransactionDescription = transactionMap.getStringValueOrIfNull("transaction_description", "");
                        String strMSGRunningBalance = transactionMap.getStringValue("running_balance");

                        //strMSGRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                        Element elTrBody = doc.createElement("TR");
                        elTable.appendChild(elTrBody);

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strMSGTransactionDescription);
                        elTrBody.appendChild(elTDBody1);

                        Element elTDBody2 = doc.createElement("TD");
                        elTDBody2.setTextContent(strMSGFormattedTransactionAmount);
                        elTrBody.appendChild(elTDBody2);

                        Element elTDBody3 = doc.createElement("TD");
                        elTDBody3.setTextContent(strMSGFormattedTransactionDateTime);
                        elTrBody.appendChild(elTDBody3);

                        Element elTDBody4 = doc.createElement("TD");
                        elTDBody4.setTextContent(strMSGTransactionReference);
                        elTrBody.appendChild(elTDBody4);

                        Element elTDBody5 = doc.createElement("TD");
                        elTDBody5.setTextContent(strMSGRunningBalance);
                        elTrBody.appendChild(elTDBody5);

                        if (i >= intMaximumTransactionCount) {
                            break;
                        }

                        i++;
                    }
                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Account Statement Generated Successfully");

                }
            }

            channelService.setBeneficiaryReference("");
            channelService.setSourceReference("");
            channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

            channelService.setInitiatorType("MSISDN");
            channelService.setInitiatorIdentifier(strUsername);
            channelService.setInitiatorAccount(strUsername);
            channelService.setInitiatorName(strMemberName);
            channelService.setInitiatorReference(theMAPPRequest.getTraceID());
            channelService.setInitiatorApplication("MAPP");
            channelService.setInitiatorOtherDetails("<DATA/>");

            channelService.setSourceType("ACCOUNT_NO");
            channelService.setSourceIdentifier(strAccountNo);
            channelService.setSourceAccount(strAccountNo);
            channelService.setSourceName(strAccountNo);
            channelService.setSourceApplication("CBS");
            channelService.setSourceOtherDetails("<DATA/>");

            channelService.setBeneficiaryType("MSISDN");
            channelService.setBeneficiaryIdentifier(strUsername);
            channelService.setBeneficiaryAccount(strUsername);
            channelService.setBeneficiaryName(strMemberName);
            channelService.setBeneficiaryApplication("MSISDN");
            channelService.setBeneficiaryOtherDetails("<DATA/>");

            channelService.setTransactionCurrency("KES");
            channelService.setTransactionAmount(0.00);

            TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.ACCOUNT_FULL_STATEMENT.getValue(),
                    0.00);

            if (chargesWrapper.hasErrors()) {
                channelService.setTransactionCharge(0.00);
                channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

            } else {
                channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                channelService.setTransactionOtherDetails("<DATA/>");
            }

            channelService.setTransactionRemark("Account Full Statement for A/C: " + strAccountNo);
            ChannelService.insertService(channelService);


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

    public MAPPResponse accountStatementBase64_PREV(MAPPRequest theMAPPRequest) {

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

    public MAPPResponse accountStatementBase64(MAPPRequest theMAPPRequest) {

        MAPPResponse theMAPPResponse = null;

        try {

            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            XPath configXPath = XPathFactory.newInstance().newXPath();

            //Request
            String strUsername = theMAPPRequest.getUsername();
            String strPassword = theMAPPRequest.getPassword();
            // strPassword = APIUtils.hashPIN(strPassword, strUsername);
            String strAppID = theMAPPRequest.getAppID();
            long lnSessionID = theMAPPRequest.getSessionID();

            String strAccountNo = configXPath.evaluate("ACCOUNT_NO", theMAPPRequest.getMSG()).trim();
            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim();
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim();

            //TODO: REQUEST MOBILE TEAM TO CHECK ON LOAN STATEMENT BASE 64. It's calling ACCOUNT_STATEMENT_BASE64
            if(strAccountNo.startsWith("BLN") || strAccountNo.startsWith("ML")){
                return loanStatementBase64(theMAPPRequest);
            }

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

            strMaximumTransactionCount = "6";

            intMaximumTransactionCount = Integer.parseInt("100");

            /*End of Duration Change*/

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

           /* TransactionWrapper<FlexicoreHashMap> miniStatementWrapper = CBSAPI.accountMiniStatement(strTransactionID, "MSISDN", strUsername,
                    "APP_ID", strAppID, strAccountNo, strMaximumTransactionCount, "0001-01-01T00:00:00", "0001-01-01T00:00:00");
*/

            TransactionWrapper<FlexicoreHashMap> miniStatementWrapper = CBSAPI.accountFullStatement(strUsername, "MSISDN", strUsername,
                    "APP_ID", strAppID, strAccountNo, "100", strStartDate + " 00:00:00", strEndDate + " 23:59:59");

            FlexicoreHashMap miniStatementMap = miniStatementWrapper.getSingleRecord();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Account Statement";

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            Element elData = doc.createElement("DATA");
            String strCharge = "NO";


            String strMemberName = getUserFullName(theMAPPRequest, strUsername);

            String strOriginatorId = UUID.randomUUID().toString();

            ChannelService channelService = new ChannelService();
            channelService.setOriginatorId(strOriginatorId);
            channelService.setTransactionCategory(AppConstants.ChargeServices.ACCOUNT_FULL_STATEMENT.getValue());


            if (miniStatementWrapper.hasErrors()) {
                strTitle = "Error: Account Statement Failed";
                elData.setTextContent("An error occurred while processing your request. Please try again in a few minutes");

                channelService.setTransactionStatusCode(104);
                channelService.setTransactionStatusName("FAILED");
                channelService.setTransactionStatusDescription(miniStatementMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

            } else {
                FlexicoreArrayList allTransactionsList = miniStatementMap.getFlexicoreArrayList("payload");
                if (allTransactionsList.isEmpty()) {
                    enResponseStatus = FAILED;
                    strCharge = "NO";
                    strTitle = "Error: No Statements Found";
                    elData.setTextContent("You do not have any statements within this time period");


                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("You do not have any statements within this time period");

                } else {

                    String theAccountStatement = AccountStatements.getAccountStatementHTML();

                    String strFormattedPeriod = DateTime.convertStringToDateToString(strStartDate, "yyyy-MM-dd", "dd MMM yyyy");
                    strFormattedPeriod = strFormattedPeriod + " to ";
                    strFormattedPeriod = strFormattedPeriod + DateTime.convertStringToDateToString(strEndDate, "yyyy-MM-dd", "dd MMM yyyy");

                    theAccountStatement = theAccountStatement.replace("[STATEMENT_PERIOD]", Misc.escapeHtmlEntity(strFormattedPeriod));

                    String strAvailableBalance = miniStatementMap.getStringValue("account_available_balance");

                    strAvailableBalance = Utils.formatDouble(strAvailableBalance, "#,##0.00");

                    theAccountStatement = theAccountStatement.replace("[ACCOUNT_BALANCE]", "KES " + strAvailableBalance);

                    theAccountStatement = theAccountStatement.replace("[ACCOUNT_NAME]", Misc.escapeHtmlEntity(miniStatementMap.getStringValue("account_name")));
                    theAccountStatement = theAccountStatement.replace("[ACCOUNT_NUMBER]", Misc.escapeHtmlEntity(strAccountNo));
                    theAccountStatement = theAccountStatement.replace("[ACCOUNT_HOLDER]", Misc.escapeHtmlEntity(miniStatementMap.getStringValue("account_holder")));

                    StringBuilder builder = new StringBuilder();

                    double dblTotalPaidIn = 0;
                    double dblTotalPaidOut = 0;

                    int i = 0;
                    int size = allTransactionsList.size();
                    for (int index = size - 1; index >= 0; index--) {
                        FlexicoreHashMap transactionMap = allTransactionsList.get(index);

                        String strMSGTransactionReference = transactionMap.getStringValue("transaction_reference");
                        String strMSGFormattedTransactionDateTime = transactionMap.getStringValue("transaction_date_time");
                        String strMSGFormattedTransactionAmount = transactionMap.getStringValue("transaction_amount");
                        String strMSGTransactionDescription = transactionMap.getStringValueOrIfNull("transaction_description", "");
                        String strMSGRunningBalance = transactionMap.getStringValue("running_balance");

                        String strDebitCredit;

                        if (strMSGFormattedTransactionAmount.trim().startsWith("-")) {
                            strDebitCredit = "D";
                        } else {
                            strDebitCredit = "C";
                        }

                        strMSGFormattedTransactionAmount = strMSGFormattedTransactionAmount.replace("-", "");


                        //strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGFormattedTransactionDateTime, "yyyy-MM-dd'T'HH:mm:ss", "dd MMM yyyy");

                        builder.append("<tr>\n" +
                                "                <td class='statement-header-acc-stmnt-date'>" + Misc.escapeHtmlEntity(strMSGFormattedTransactionDateTime) + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-description'>" + Misc.escapeHtmlEntity(strMSGTransactionDescription) + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-paid-in'>" +
                                (strDebitCredit.equalsIgnoreCase("C") ? Utils.formatDouble(strMSGFormattedTransactionAmount, "#,##0.00") : "") + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-paid-out'>" +
                                (strDebitCredit.equalsIgnoreCase("D") ? Utils.formatDouble(strMSGFormattedTransactionAmount, "#,##0.00") : "") + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-balance'>" + Utils.formatDouble(strMSGRunningBalance, "#,##0.00") + "</td>\n" +
                                "            </tr>");

                        if (strDebitCredit.equalsIgnoreCase("C")) {
                            dblTotalPaidIn += Double.parseDouble(strMSGFormattedTransactionAmount);
                        } else {
                            dblTotalPaidOut += Double.parseDouble(strMSGFormattedTransactionAmount);
                        }


                        if (i >= intMaximumTransactionCount) {
                            break;
                        }

                        i++;
                    }

                    theAccountStatement = theAccountStatement.replace("[TOTAL_PAID_IN]", "KES " + Utils.formatDouble(dblTotalPaidIn, "#,##0.00"));
                    theAccountStatement = theAccountStatement.replace("[TOTAL_PAID_OUT]", "KES " + Utils.formatDouble(dblTotalPaidOut, "#,##0.00"));
                    theAccountStatement = theAccountStatement.replace("[THE_ACCOUNT_STATEMENT_DETAILS]", builder.toString());
                    theAccountStatement = AccountStatements.generateAccountStatementPDF(theAccountStatement, strAccountNo);

                    elData.setTextContent(theAccountStatement);


                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Account Statement Generated Successfully");

                }
            }


            channelService.setBeneficiaryReference("");
            channelService.setSourceReference("");
            channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

            channelService.setInitiatorType("MSISDN");
            channelService.setInitiatorIdentifier(strUsername);
            channelService.setInitiatorAccount(strUsername);
            channelService.setInitiatorName(strMemberName);
            channelService.setInitiatorReference(theMAPPRequest.getTraceID());
            channelService.setInitiatorApplication("MAPP");
            channelService.setInitiatorOtherDetails("<DATA/>");

            channelService.setSourceType("ACCOUNT_NO");
            channelService.setSourceIdentifier(strAccountNo);
            channelService.setSourceAccount(strAccountNo);
            channelService.setSourceName(strAccountNo);
            channelService.setSourceApplication("CBS");
            channelService.setSourceOtherDetails("<DATA/>");

            channelService.setBeneficiaryType("MSISDN");
            channelService.setBeneficiaryIdentifier(strUsername);
            channelService.setBeneficiaryAccount(strUsername);
            channelService.setBeneficiaryName(strMemberName);
            channelService.setBeneficiaryApplication("MSISDN");
            channelService.setBeneficiaryOtherDetails("<DATA/>");

            channelService.setTransactionCurrency("KES");
            channelService.setTransactionAmount(0.00);

            TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.ACCOUNT_FULL_STATEMENT.getValue(),
                    0.00);

            if (chargesWrapper.hasErrors()) {
                channelService.setTransactionCharge(0.00);
                channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

            } else {
                channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                channelService.setTransactionOtherDetails("<DATA/>");
            }

            channelService.setTransactionRemark("Account Full Statement for A/C: " + strAccountNo);
            ChannelService.insertService(channelService);


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

    public MAPPResponse changePassword_PREV(MAPPRequest theMAPPRequest) {
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
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strNewPassword = configXPath.evaluate("NEW_PASSWORD", ndRequestMSG).trim();

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());

            String strTitle = "";
            String strResponseText = "";

            String strCharge = "NO";

            TransactionWrapper<FlexicoreHashMap> currentUserWrapper = CBSAPI.getCurrentUserDetails(UUID.randomUUID().toString(), "MSISDN", strUsername, "APP_ID", strAppID);
            FlexicoreHashMap currentUserDetailsMap = currentUserWrapper.getSingleRecord();

            FlexicoreHashMap mobileBankingDetailsMap = currentUserDetailsMap.getFlexicoreHashMap("mobile_register_details");

            String previousPasswords = mobileBankingDetailsMap.getStringValueOrIfNull("previous_pins", "<PREVIOUS_PINS/>");

            Document docPrevPasswords = XmlUtils.parseXml(previousPasswords);
            NodeList allprevPasswordsList = null;

            try {
                allprevPasswordsList = XmlUtils.getNodesFromXpath(docPrevPasswords, "/PREVIOUS_PINS/PIN");
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean hasUsedPinBefore = false;

            if (allprevPasswordsList != null) {

                int intMin = 0;

                if (allprevPasswordsList.getLength() > 5) {
                    intMin = 5;
                }

                for (int i = allprevPasswordsList.getLength() - 1; i >= intMin; i--) {
                    Node node = allprevPasswordsList.item(i);
                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    Element element = (Element) node;
                    if (element.getTextContent().equalsIgnoreCase(MobileBankingCryptography.hashPIN(strUsername, strNewPassword))) {
                        hasUsedPinBefore = true;
                        break;
                    }
                }
            }

            if (hasUsedPinBefore) {
                strTitle = "Change Password Failed";
                strResponseText = "Please provide a new password that you have not used before for your mobile banking account.";

                enResponseAction = CON;
                enResponseStatus = ERROR;

            } else {

                TransactionWrapper<FlexicoreHashMap> changePINWrapper = CBSAPI.changeUserPIN(strTransactionID, "MSISDN", strUsername, strPassword, strNewPassword, "APP_ID", strAppID, USSDAPIConstants.MobileChannel.MOBILE_APP);
                FlexicoreHashMap changePINMap = changePINWrapper.getSingleRecord();
                if (changePINWrapper.hasErrors()) {
                    USSDAPIConstants.Condition endSession = changePINMap.getValue("end_session");
                    strTitle = changePINMap.getStringValue("display_message");
                    strResponseText = changePINMap.getStringValue("title");

                    if (endSession == USSDAPIConstants.Condition.YES) {
                        enResponseAction = END;
                        enResponseStatus = ERROR;
                    } else {
                        enResponseAction = CON;
                        enResponseStatus = ERROR;
                    }

                } else {
                    strTitle = "Password Changed Successfully";
                    strResponseText = "Your password has been changed successfully. You will be redirected to the login page.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = SUCCESS;
                }
            }

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);


            /*System.out.println("\n\nTHE CHANGE PASSWORD RESPONSE\n\n");
            System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));*/

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".changePassword() ERROR : " + e.getMessage());

            e.printStackTrace();
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
            Crypto crypto = new Crypto();
            strPassword = crypto.hash("MD5", strPassword);
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            MAPPConstants.ResponsesDataType enDataType = TEXT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strEncrypted = configXPath.evaluate("ENCRYPTED", ndRequestMSG).trim();
            String strTimestamp = configXPath.evaluate("TIMESTAMP", ndRequestMSG).trim();

            String strDecryptedText = strEncrypted;

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
                    elAccountDetails = getAccountElement(theMAPPRequest, strPhoneNumber, "Mobile", doc, "ENCRYPTION");
                    break;
                }
                case "DEPOSIT_MONEY":
                case "FUNDS_TRANSFER": {
                    strAccountNumber = arStrDecryptedText[4];
                    elAccountDetails = getAccountElement(theMAPPRequest, strAccountNumber, "ACCOUNT", doc, "ENCRYPTION");
                    break;
                }
                default: {
                    elAccountDetails = getAccountElement(theMAPPRequest, strPhoneNumber, "Mobile", doc, "ENCRYPTION");
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

            MAPPConstants.ResponsesDataType enDataType = OBJECT;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

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
            Element elAccountDetails = getAccountElement(theMAPPRequest, strAccount, strSource, doc, "GET_MEMBER_NAME");

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
           // Element elAccountDetails = getAccountElement(strUsername, strSource, doc, "GET_MEMBER_NAME");
           // Element elAccountDetails = getAccountElement(theMAPPRequest, strAccount, strSource, doc, "GET_MEMBER_NAME");


            Element elData = doc.createElement("DATA");

           // elData.appendChild(elAccountDetails);

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

    public Element getAccountElement(MAPPRequest theMAPPRequest, String theAccount, String theSource, Document doc, String theCategory) {
        try {
            //theSource (from MAPPAPI) -> Mobile / ID Number / Account Number / Member Number
            //theSource (to XTremeAPI) -> MEMBER_NUMBER / ID_NUMBER / ACCOUNT_NUMBER / MOBILE_NUMBER
            switch (theSource) {
                case "ID": {
                    theSource = "NATIONAL_ID";
                    break;
                }

                case "ACCOUNT": {
                    theSource = "ACCOUNT_NUMBER";
                    break;
                }

                case "Member Number": {
                    theSource = "MEMBER_NUMBER";
                    break;
                }

                case "Mobile":
                default: {
                    theSource = "MSISDN";
                    break;
                }
            }

            HashMap<String, String> hmMemberDetails = getUserDetails(theMAPPRequest, theSource, theAccount);

           /* HashMap<String, HashMap<String, String>> hmIFTDestAccounts = (HashMap<String, HashMap<String, String>>) accountDetails.get("accounts");
            HashMap<String, String> hmMemberDetails = (HashMap<String, String>) accountDetails.get("user_details");*/

            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strIDNumber = "";
            String strAccountStatus = "NOT_FOUND";

            if (hmMemberDetails != null && !hmMemberDetails.isEmpty()) {
                strAccountStatus = "FOUND";
                strAccountNo = hmMemberDetails.get("number");
                strAccountType = hmMemberDetails.get("type_name");
                strAccountMemberNo = hmMemberDetails.get("member_number");
                strAccountName = hmMemberDetails.get("full_name");
                strPhoneNo = hmMemberDetails.get("identifier");
                strIDNumber = hmMemberDetails.get("identity");
                //strAccountName = Utils.toTitleCase(strAccountName);

            }

            if (theCategory != null) {
                if (theCategory.equals("VALIDATE_PESA_IN")) {
                    elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                    Element elKYCDetails = doc.createElement("KYC_DETAILS");
                    elPesaOtherDetails.appendChild(elKYCDetails);

                    Element elKYCResponse = doc.createElement("RESPONSE");
                    elKYCDetails.appendChild(elKYCResponse);

                    Element elKYC = doc.createElement("KYC");
                    elKYC.setAttribute("TYPE", theSource);
                    elKYCResponse.appendChild(elKYC);

                    Element elIdentifier = doc.createElement("IDENTIFIER");
                    elIdentifier.setTextContent(theAccount);
                    elKYC.appendChild(elIdentifier);

                    Element elAccount = doc.createElement("ACCOUNT");
                    elAccount.setTextContent(strAccountNo);
                    elKYC.appendChild(elAccount);

                    Element elName = doc.createElement("NAME");
                    elName.setTextContent(strAccountName);
                    elKYC.appendChild(elName);

                    Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                    elKYC.appendChild(elOtherDetails);
                }
                else {
                    elPesaOtherDetails = doc.createElement("ACCOUNT");
                    elPesaOtherDetails.setAttribute("STATUS", strAccountStatus);
                    elPesaOtherDetails.setAttribute("ACCOUNT_NO", strAccountNo);
                    elPesaOtherDetails.setAttribute("ACCOUNT_NAME", strAccountType);
                    elPesaOtherDetails.setAttribute("NAME", strAccountName);
                    elPesaOtherDetails.setAttribute("MEMBER_NO", strAccountMemberNo);
                    elPesaOtherDetails.setAttribute("PHONE_NO", strPhoneNo);
                }
            }
            return elPesaOtherDetails;
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
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

                String strOriginatorId = UUID.randomUUID().toString();

                String strMemberName = getUserFullName(theMAPPRequest, strUsername);

                    /*TransactionWrapper<FlexicoreHashMap> checkLoanLimitWrapper = CBSAPI.checkLoanLimit(strUsername,
                            "MSISDN", strUsername, "APP_ID", strAppID, strLoanNo);*/

                //sendSMS(strUsername, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), "LOAN_APPLICATION", theMAPPRequest);
                ChannelService channelService = new ChannelService();
                channelService.setOriginatorId(strOriginatorId);
                channelService.setTransactionCategory(AppConstants.ChargeServices.LOAN_APPLICATION.getValue());

              /*  if(strLoanPurpose == null || strLoanPurpose.isBlank()) {
                    strTitle = "Loan Application Failed!";
                    strResponseText = "Please ensure you are using the latest version of the app by updating it on the Play Store or AppStore.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = ERROR;
                }*/

                if (strLoanPurpose == null || strLoanPurpose.isBlank()) {
                    strLoanPurpose = "";
                }

                Element elData = doc.createElement("DATA");


//                else {
                TransactionWrapper<FlexicoreHashMap> loanApplicationWrapper;

                loanApplicationWrapper = CBSAPI.loanApplication(strUsername,
                        "MSISDN", strUsername, "APP_ID", strAppID, strLoanType,
                        Double.parseDouble(strAmount), strOriginatorId,
                        "MAPP", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"),
                        strLoanPurpose);

                FlexicoreHashMap loanApplicationMap = loanApplicationWrapper.getSingleRecord();
                CBSAPI.SMSMSG cbsMSG = loanApplicationMap.getValue("msg_object");


                if (loanApplicationWrapper.hasErrors()) {
                    channelService.setTransactionStatusCode(104);
                    channelService.setTransactionStatusName("FAILED");
                    channelService.setTransactionStatusDescription(loanApplicationMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

                    System.err.println("MAPPAPI.applyLoan() - Response " + loanApplicationMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

                    strTitle = loanApplicationMap.getStringValueOrIfNull("title", "Unknown error occurred");
                    strResponseText = loanApplicationMap.getStringValueOrIfNull("display_message", "Unknown error occurred");
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = FAILED;

                } else {
                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Loan Application Completed Successfully");
                    channelService.setBeneficiaryReference("");
                    channelService.setSourceReference("");

                    strTitle = "Loan Application Successful";
                    strResponseText = "Your loan application was completed successfully.";
                    strCharge = "YES";
                    enResponseAction = CON;
                    enResponseStatus = SUCCESS;

                }
//                }

                channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

                channelService.setInitiatorType("MSISDN");
                channelService.setInitiatorIdentifier(strUsername);
                channelService.setInitiatorAccount(strUsername);
                channelService.setInitiatorName(strMemberName);
                channelService.setInitiatorReference(theMAPPRequest.getTraceID());
                channelService.setInitiatorApplication("USSD");
                channelService.setInitiatorOtherDetails("<DATA/>");

                channelService.setSourceType("ACCOUNT_NO");
                channelService.setSourceIdentifier(strLoanType);
                channelService.setSourceAccount(strLoanType);
                channelService.setSourceName(strLoanType);
                channelService.setSourceApplication("CBS");
                channelService.setSourceOtherDetails("<DATA/>");

                channelService.setBeneficiaryType("MSISDN");
                channelService.setBeneficiaryIdentifier(strUsername);
                channelService.setBeneficiaryAccount(strUsername);
                channelService.setBeneficiaryName(strMemberName);
                channelService.setBeneficiaryApplication("CBS");
                channelService.setBeneficiaryOtherDetails("<DATA/>");

                channelService.setTransactionCurrency("KES");
                channelService.setTransactionAmount(Double.parseDouble(strAmount));

                TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.LOAN_APPLICATION.getValue(),
                        Double.parseDouble(strAmount));

                if (chargesWrapper.hasErrors()) {
                    channelService.setTransactionCharge(0.00);
                    channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

                } else {
                    channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                    channelService.setTransactionOtherDetails("<DATA/>");
                }

                channelService.setTransactionRemark("Loan Application");
                ChannelService.insertService(channelService);

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

    public MAPPResponse applyLoan_PREV(MAPPRequest theMAPPRequest) {
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
            MAPPAPIConstants.OTP_VERIFICATION_STATUS otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS;

            APIUtils.OTP otp = checkOTPRequirement(theMAPPRequest, MAPPAPIConstants.OTP_CHECK_STAGE.VERIFICATION);
            if (otp.isEnabled()) {
                mrOTPVerificationMappResponse = validateOTP(theMAPPRequest, MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL);

                String strAction = configXPath.evaluate("@ACTION", mrOTPVerificationMappResponse.getMSG()).trim();
                String strStatus = configXPath.evaluate("@STATUS", mrOTPVerificationMappResponse.getMSG()).trim();

                if (!strAction.equals("CON") || !strStatus.equals("SUCCESS")) {
                    otpVerificationStatus = MAPPAPIConstants.OTP_VERIFICATION_STATUS.ERROR;
                }
            }

            if (otpVerificationStatus == MAPPAPIConstants.OTP_VERIFICATION_STATUS.SUCCESS) {
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



    public MAPPResponse loanStatement_PREV(MAPPRequest theMAPPRequest) {

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


    public MAPPResponse loanStatement(MAPPRequest theMAPPRequest) {

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
            String statementType = "FULL_STATEMENT";

            String strTrailerMessageXML = SystemParameters.getParameter(AppConstants.strSettingParamName);
            Document document = XmlUtils.parseXml(strTrailerMessageXML);

            String strNumberOfEntries = XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/LOAN_STATEMENT_ENTRIES");

            int intMaximumTransactionCount = Integer.parseInt(strNumberOfEntries);


            /*System.out.println("\n\nTHE LOAN STATEMENT REQUEST\n\n");
            System.out.println(XmlUtils.convertNodeToStr(theMAPPRequest.getMSG()));*/


            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();
            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim();
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim();

            String strLoanMinistatementStatus = "ERROR";

            TransactionWrapper<FlexicoreHashMap> miniStatementWrapper = CBSAPI.getLoanFullStatement(strUsername, "MSISDN", strUsername,
                    "APP_ID", strAppID, strLoanNo, "100",
                    strStartDate + " 00:00:00", strEndDate + " 23:59:59");

            FlexicoreHashMap miniStatementMap = miniStatementWrapper.getSingleRecord();

            String strMemberName = getUserFullName(theMAPPRequest, strUsername);

            String strOriginatorId = UUID.randomUUID().toString();

            ChannelService channelService = new ChannelService();
            channelService.setOriginatorId(strOriginatorId);
            channelService.setTransactionCategory(AppConstants.ChargeServices.LOAN_FULL_STATEMENT.getValue());


            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Statement";

            MAPPConstants.ResponsesDataType enDataType = TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (miniStatementWrapper.hasErrors()) {
                strTitle = "Error: Loan Statement Failed";
                elData.setTextContent("An error occurred while processing your request. Please try again in a few minutes");

                enResponseStatus = ERROR;

                channelService.setTransactionStatusCode(104);
                channelService.setTransactionStatusName("FAILED");
                channelService.setTransactionStatusDescription(miniStatementMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));


            } else {
                FlexicoreArrayList allTransactionsList = miniStatementMap.getFlexicoreArrayList("payload");
                if (allTransactionsList.isEmpty()) {
                    enResponseStatus = FAILED;
                    strCharge = "NO";
                    strTitle = "Error: No Statement Found";
                    elData.setTextContent("You do not have any loan transactions within this time period");

                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("You do not have any statements within this time period");


                } else {

                    String strLoanBalance = miniStatementMap.getStringValue("account_available_balance");

                    strLoanBalance = Utils.formatDouble(strLoanBalance, "#,##0.00");

                    Element elBalance = doc.createElement("BALANCE");
                    elBalance.setTextContent(strLoanBalance);
                    elData.appendChild(elBalance);

                    Element elAccountNo = doc.createElement("ACCOUNTNO");
                    elAccountNo.setTextContent(strLoanNo);
                    elData.appendChild(elAccountNo);

                    Element elAccountName = doc.createElement("NAME");
                    elAccountName.setTextContent(miniStatementMap.getStringValue("account_name"));
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

                    /*Element elThHeading4 = doc.createElement("TH");
                    elThHeading4.setTextContent("Ref");
                    elTrHeading.appendChild(elThHeading4);*/

                    Element elThHeading5 = doc.createElement("TH");
                    elThHeading5.setTextContent("Balance");
                    elTrHeading.appendChild(elThHeading5);

                    int i = 0;
                    for (FlexicoreHashMap transactionMap : allTransactionsList) {
                        //String strMSGTransactionReference = transactionMap.getStringValue("reference");

                        String strMSGTransactionReference = transactionMap.getStringValue("transaction_reference");
                        String strMSGFormattedTransactionDateTime = transactionMap.getStringValue("transaction_date_time");
                        // strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGFormattedTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                        String strMSGTransactionAmount = transactionMap.getStringValue("transaction_amount");

                        String strMSGTransactionDescription = transactionMap.getStringValueOrIfNull("transaction_description", "");
                        String strMSGRunningBalance = transactionMap.getStringValue("running_balance");
                        //String strMSGIntRunningBalance = transactionMap.getStringValue("int_running_balance").trim();

                        //strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");
                        //strMSGRunningBalance = strMSGRunningBalance.replace("-", "");
                        //strMSGIntRunningBalance = strMSGIntRunningBalance.replace("-", "");

                        String strMSGFormattedTransactionAmount = Utils.formatDouble(strMSGTransactionAmount, "#,##0.00");
                        String strMSGFormattedRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                        //strMSGRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                        Element elTrBody = doc.createElement("TR");
                        elTable.appendChild(elTrBody);

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strMSGTransactionDescription);
                        elTrBody.appendChild(elTDBody1);

                        Element elTDBody2 = doc.createElement("TD");
                        elTDBody2.setTextContent(strMSGFormattedTransactionAmount);
                        elTrBody.appendChild(elTDBody2);

                        Element elTDBody3 = doc.createElement("TD");
                        elTDBody3.setTextContent(strMSGFormattedTransactionDateTime);
                        elTrBody.appendChild(elTDBody3);

                        /*Element elTDBody4 = doc.createElement("TD");
                        elTDBody4.setTextContent("lkjhgfdsdfghjk");
                        elTrBody.appendChild(elTDBody4);*/

                        Element elTDBody5 = doc.createElement("TD");
                        elTDBody5.setTextContent(strMSGFormattedRunningBalance);
                        elTrBody.appendChild(elTDBody5);

                        if (i >= intMaximumTransactionCount) {
                            break;
                        }

                        i++;
                    }

                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Loan Statement Generated Successfully");
                }
            }

            channelService.setBeneficiaryReference("");
            channelService.setSourceReference("");
            channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

            channelService.setInitiatorType("MSISDN");
            channelService.setInitiatorIdentifier(strUsername);
            channelService.setInitiatorAccount(strUsername);
            channelService.setInitiatorName(strMemberName);
            channelService.setInitiatorReference(theMAPPRequest.getTraceID());
            channelService.setInitiatorApplication("MAPP");
            channelService.setInitiatorOtherDetails("<DATA/>");

            channelService.setSourceType("ACCOUNT_NO");
            channelService.setSourceIdentifier(strLoanNo);
            channelService.setSourceAccount(strLoanNo);
            channelService.setSourceName(strLoanNo);
            channelService.setSourceApplication("CBS");
            channelService.setSourceOtherDetails("<DATA/>");

            channelService.setBeneficiaryType("MSISDN");
            channelService.setBeneficiaryIdentifier(strUsername);
            channelService.setBeneficiaryAccount(strUsername);
            channelService.setBeneficiaryName(strMemberName);
            channelService.setBeneficiaryApplication("MSISDN");
            channelService.setBeneficiaryOtherDetails("<DATA/>");

            channelService.setTransactionCurrency("KES");
            channelService.setTransactionAmount(0.00);

            TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.LOAN_FULL_STATEMENT.getValue(),
                    0.00);

            if (chargesWrapper.hasErrors()) {
                channelService.setTransactionCharge(0.00);
                channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

            } else {
                channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                channelService.setTransactionOtherDetails("<DATA/>");
            }

            channelService.setTransactionRemark("Loan Full Statement for A/C: " + strLoanNo);
            ChannelService.insertService(channelService);

           /* if (strLoanMinistatementStatus.equals("SUCCESS")) {
                String strLoanBalance = "KES "+Utils.formatDouble(hmLoanStatementDetails.get("loan_balance"), "#,##0.00");
                if (hmLoanStatementTransactions != null && !hmLoanStatementTransactions.isEmpty()) {
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

                    for (String index : hmLoanStatementTransactions.keySet()) {
                        HashMap<String, String> hmTransaction = hmLoanStatementTransactions.get(index);
                        String strDate = hmTransaction.get("transaction_date_time");
                        String strDesc = hmTransaction.get("transaction_description");
                        String strAmount = "KES "+Utils.formatDouble(hmTransaction.get("transaction_amount"), "#,##0.00");
                        String strReference = hmTransaction.get("transaction_reference");
                        String strBalance = "KES "+Utils.formatDouble(hmTransaction.get("running_balance"), "#,##0.00");

                        Element elTrBody = doc.createElement("TR");
                        elTable.appendChild(elTrBody);

                        Element elTDBody1 = doc.createElement("TD");
                        elTDBody1.setTextContent(strDesc);
                        elTrBody.appendChild(elTDBody1);

                        Element elTDBody2 = doc.createElement("TD");
                        elTDBody2.setTextContent(strAmount);
                        elTrBody.appendChild(elTDBody2);

                        Element elTDBody3 = doc.createElement("TD");
                        elTDBody3.setTextContent(strDate);
                        elTrBody.appendChild(elTDBody3);

                        Element elTDBody4 = doc.createElement("TD");
                        elTDBody4.setTextContent(strReference);
                        elTrBody.appendChild(elTDBody4);

                        Element elTDBody5 = doc.createElement("TD");
                        elTDBody5.setTextContent(strBalance);
                        elTrBody.appendChild(elTDBody5);
                    }
                } else {
                    strCharge = "NO";
                    strTitle = "No Statements Found";
                    elData.setTextContent("No loan transactions found for the specified loan");
                }
            } else {
                strCharge = "NO";
                strTitle= "ERROR: Loan Statement";
                elData.setTextContent("An error occurred. Please try again after a few minutes.");
                enResponseStatus = MAPPConstants.ResponseStatus.FAILED;
            }*/

            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return theMAPPResponse;
    }

    public MAPPResponse loanStatementBase64(MAPPRequest theMAPPRequest) {

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
            int intMaxNumberOfTransactions = 100;
            String statementType = "FULL_STATEMENT";

            //String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();

            String strTrailerMessageXML = SystemParameters.getParameter(AppConstants.strSettingParamName);
            Document document = XmlUtils.parseXml(strTrailerMessageXML);

            String strNumberOfEntries = XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/LOAN_STATEMENT_ENTRIES");

            int intMaximumTransactionCount = Integer.parseInt(strNumberOfEntries);

            String strLoanNo = configXPath.evaluate("LOAN_SERIAL_NO", theMAPPRequest.getMSG()).trim();
            if(strLoanNo.isEmpty()){
                strLoanNo = configXPath.evaluate("ACCOUNT_NO", theMAPPRequest.getMSG()).trim();
            }

            String strStartDate = configXPath.evaluate("FROM", theMAPPRequest.getMSG()).trim();
            String strEndDate = configXPath.evaluate("TO", theMAPPRequest.getMSG()).trim();

            String strLoanMinistatementStatus = "ERROR";

            TransactionWrapper<FlexicoreHashMap> miniStatementWrapper = CBSAPI.getLoanFullStatement(strUsername, "MSISDN", strUsername,
                    "APP_ID", strAppID, strLoanNo, "100",
                    strStartDate + " 00:00:00", strEndDate + " 23:59:59");

            FlexicoreHashMap miniStatementMap = miniStatementWrapper.getSingleRecord();

            String strMemberName = getUserFullName(theMAPPRequest, strUsername);

            String strOriginatorId = UUID.randomUUID().toString();

            ChannelService channelService = new ChannelService();
            channelService.setOriginatorId(strOriginatorId);
            channelService.setTransactionCategory(AppConstants.ChargeServices.LOAN_FULL_STATEMENT.getValue());


            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strTitle = "Loan Statement";

            MAPPConstants.ResponsesDataType enDataType = TABLE;

            MAPPConstants.ResponseAction enResponseAction = CON;
            MAPPConstants.ResponseStatus enResponseStatus = SUCCESS;

            String strCharge = "NO";

            Element elData = doc.createElement("DATA");

            if (miniStatementWrapper.hasErrors()) {
                strTitle = "Error: Loan Statement Failed";
                elData.setTextContent("An error occurred while processing your request. Please try again in a few minutes");

                enResponseStatus = ERROR;

                channelService.setTransactionStatusCode(104);
                channelService.setTransactionStatusName("FAILED");
                channelService.setTransactionStatusDescription(miniStatementMap.getStringValueOrIfNull("cbs_api_error_message", "Unknown error occurred"));

            } else {
                FlexicoreArrayList allTransactionsList = miniStatementMap.getFlexicoreArrayList("payload");
                if (allTransactionsList.isEmpty()) {
                    enResponseStatus = FAILED;
                    strCharge = "NO";
                    strTitle = "Error: No Statement Found";
                    elData.setTextContent("You do not have any loan transactions within this time period");

                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("You do not have any statements within this time period");

                } else {

                    String theAccountStatement = AccountStatements.getLoanStatementHTML();

                    String strFormattedPeriod = DateTime.convertStringToDateToString(strStartDate, "yyyy-MM-dd", "dd MMM yyyy");
                    strFormattedPeriod = strFormattedPeriod + " to ";
                    strFormattedPeriod = strFormattedPeriod + DateTime.convertStringToDateToString(strEndDate, "yyyy-MM-dd", "dd MMM yyyy");

                    theAccountStatement = theAccountStatement.replace("[STATEMENT_PERIOD]", Misc.escapeHtmlEntity(strFormattedPeriod));

                    String strAvailableBalance = miniStatementMap.getStringValue("account_available_balance");

                    strAvailableBalance = Utils.formatDouble(strAvailableBalance, "#,##0.00");

                    theAccountStatement = theAccountStatement.replace("[LOAN_BALANCE]", "KES " + strAvailableBalance);

                    theAccountStatement = theAccountStatement.replace("[LOAN_NAME]", Misc.escapeHtmlEntity(miniStatementMap.getStringValue("account_name")));

                    theAccountStatement = theAccountStatement.replace("[LOAN_NUMBER]", Misc.escapeHtmlEntity(strLoanNo));

                    theAccountStatement = theAccountStatement.replace("[LOAN_CUSTOMER_NAME]", Misc.escapeHtmlEntity(miniStatementMap.getStringValue("account_holder")));

                    StringBuilder builder = new StringBuilder();

                    int i = 0;

                    int size = allTransactionsList.size();

                    int endIndex = Math.min(size, intMaximumTransactionCount);

                    List<FlexicoreHashMap> tempTransactionsList = allTransactionsList.subList(0, endIndex);

                    for (int index = tempTransactionsList.size() - 1; index >= 0; index--) {
                        FlexicoreHashMap transactionMap = tempTransactionsList.get(index);

                        //String strAmount = transactionMap.getStringValue("amount");

                        String strMSGTransactionReference = transactionMap.getStringValue("transaction_reference");
                        String strMSGFormattedTransactionDateTime = transactionMap.getStringValue("transaction_date_time");
                        //strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGFormattedTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                        String strMSGTransactionAmount = transactionMap.getStringValue("transaction_amount").replace(",", "");

                        //strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");

                        String strMSGTransactionDescription = transactionMap.getStringValueOrIfNull("transaction_description", "");
                        String strMSGRunningBalance = transactionMap.getStringValue("running_balance");
                        //String strMSGIntRunningBalance = transactionMap.getStringValue("int_running_balance").trim();

                        // strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");
                        //strMSGRunningBalance = strMSGRunningBalance.replace("-", "");
                        //strMSGIntRunningBalance = strMSGIntRunningBalance.replace("-", "");

                        //strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGFormattedTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd MMM yyyy");

                        builder.append("<tr>\n" +
                                "                <td class='statement-header-acc-stmnt-date'>" + Misc.escapeHtmlEntity(strMSGFormattedTransactionDateTime) + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-description'>" + Misc.escapeHtmlEntity(strMSGTransactionDescription) + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-amount'>" + Utils.formatDouble(strMSGTransactionAmount, "#,##0.00") + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-balance'>" + Utils.formatDouble(strMSGRunningBalance, "#,##0.00") + "</td>\n" +
                                "            </tr>");

                    }

                    //List<FlexicoreHashMap> tempTransactionsList = allTransactionsList.subList(startIndex, size);

                   /* for (int index = allTransactionsList.size() - 1; index >= 0; index--) {
                        FlexicoreHashMap transactionMap = allTransactionsList.get(index);

                        String strMSGFormattedTransactionDateTime = transactionMap.getStringValue("raw_date");
                        String strAmount = transactionMap.getStringValue("amount");
                        String strMSGTransactionDescription = transactionMap.getStringValue("description");
                        String strMSGRunningBalance = transactionMap.getStringValue("running_balance");

                        strAmount = strAmount.replace("-", "");

                        strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGFormattedTransactionDateTime, "yyyy-MM-dd'T'HH:mm:ss", "dd MMM yyyy");

                        builder.append("<tr>\n" +
                                "                <td class='statement-header-acc-stmnt-date'>" + Misc.escapeHtmlEntity(strMSGFormattedTransactionDateTime) + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-description'>" + Misc.escapeHtmlEntity(strMSGTransactionDescription) + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-amount'>" + Utils.formatDouble(strAmount, "#,##0.00") + "</td>\n" +
                                "                <td class='statement-header-acc-stmnt-balance'>" + Utils.formatDouble(strMSGRunningBalance, "#,##0.00") + "</td>\n" +
                                "            </tr>");

                    }*/


                    theAccountStatement = theAccountStatement.replace("[THE_LOAN_STATEMENT_DETAILS]", builder.toString());
                    theAccountStatement = AccountStatements.generateAccountStatementPDF(theAccountStatement, strLoanNo);
                    elData.setTextContent(theAccountStatement);


                    channelService.setTransactionStatusCode(102);
                    channelService.setTransactionStatusName("SUCCESS");
                    channelService.setTransactionStatusDescription("Loan Statement Generated Successfully");

                }
            }


            channelService.setBeneficiaryReference("");
            channelService.setSourceReference("");
            channelService.setTransactionStatusDate(DateTime.getCurrentDateTime());

            channelService.setInitiatorType("MSISDN");
            channelService.setInitiatorIdentifier(strUsername);
            channelService.setInitiatorAccount(strUsername);
            channelService.setInitiatorName(strMemberName);
            channelService.setInitiatorReference(theMAPPRequest.getTraceID());
            channelService.setInitiatorApplication("MAPP");
            channelService.setInitiatorOtherDetails("<DATA/>");

            channelService.setSourceType("ACCOUNT_NO");
            channelService.setSourceIdentifier(strLoanNo);
            channelService.setSourceAccount(strLoanNo);
            channelService.setSourceName(strLoanNo);
            channelService.setSourceApplication("CBS");
            channelService.setSourceOtherDetails("<DATA/>");

            channelService.setBeneficiaryType("MSISDN");
            channelService.setBeneficiaryIdentifier(strUsername);
            channelService.setBeneficiaryAccount(strUsername);
            channelService.setBeneficiaryName(strMemberName);
            channelService.setBeneficiaryApplication("MSISDN");
            channelService.setBeneficiaryOtherDetails("<DATA/>");

            channelService.setTransactionCurrency("KES");
            channelService.setTransactionAmount(0.00);

            TransactionWrapper<FlexicoreHashMap> chargesWrapper = CBSAPI.getCharges(strUsername, "MSISDN", strUsername, AppConstants.ChargeServices.LOAN_FULL_STATEMENT.getValue(),
                    0.00);

            if (chargesWrapper.hasErrors()) {
                channelService.setTransactionCharge(0.00);
                channelService.setTransactionOtherDetails(chargesWrapper.getSingleRecord().getStringValue("cbs_api_error_message"));

            } else {
                channelService.setTransactionCharge(Double.parseDouble(chargesWrapper.getSingleRecord().getStringValue("charge_amount")));
                channelService.setTransactionOtherDetails("<DATA/>");
            }

            channelService.setTransactionRemark("Loan Full Statement for A/C: " + strLoanNo);
            ChannelService.insertService(channelService);


            generateResponseMSGNode(doc, elData, theMAPPRequest, enResponseAction, enResponseStatus, strCharge, strTitle, enDataType);

            //Response
            Node ndResponseMSG = doc.getElementsByTagName("MSG").item(0);

            /*System.out.println("\n\nTHE LOAN BASE64\n\n");
            System.out.println(XmlUtils.convertNodeToStr(ndResponseMSG));*/

            theMAPPResponse = setMAPPResponse(ndResponseMSG, theMAPPRequest);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
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



    public MAPPResponse mandateNotActive(MAPPRequest theMAPPRequest, String strTitle) {

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
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strCharge = "NO";
            String strResponseText = AppConstants.strServiceUnavailable;

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strResponseText);

            generateResponseMSGNode(doc, elData, theMAPPRequest, CON, FAILED, strCharge, strTitle, TEXT);

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

    public MAPPResponse serviceOnMaintenance(MAPPRequest theMAPPRequest, String strTitle, String strMessage) {

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
            String strAppID = theMAPPRequest.getAppID();

            Node ndRequestMSG = theMAPPRequest.getMSG();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // Root element - MSG
            Document doc = docBuilder.newDocument();

            String strCharge = "NO";
            //String strResponseText = "Sorry, this service is on maintenance. Please try again later";

            Element elData = doc.createElement("DATA");
            elData.setTextContent(strMessage);

            generateResponseMSGNode(doc, elData, theMAPPRequest, CON, FAILED, strCharge, strTitle, TEXT);

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


    public MAPPAmountLimitParam getParam(MAPPAPIConstants.MAPP_PARAM_TYPE theMAPPParamType) {
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

    public void sendOTP(MAPPRequest theMAPPRequest, FlexicoreHashMap mobileBankingDetailsMap, FlexicoreHashMap signatoryDetailsMap, String smsMessage, String emailMessage) {
        String strUsername = theMAPPRequest.getUsername();
        long lnSessionID = theMAPPRequest.getSessionID();

        String strSessionID = String.valueOf(lnSessionID);
        String strTraceID = theMAPPRequest.getTraceID();

        String mfaModes = mobileBankingDetailsMap.getStringValue("mfa_modes");

        String strMFAModesSMSApplicable = "YES";
        String strMFAModesEmailApplicable = "NO";

        if (mfaModes != null && !mfaModes.isEmpty()) {
            Document docMFAModes = XmlUtils.parseXml(mfaModes);
            strMFAModesSMSApplicable = XmlUtils.getTagValue(docMFAModes, "/MFA_MODES/SMS/@APPLICABLE");
            strMFAModesEmailApplicable = XmlUtils.getTagValue(docMFAModes, "/MFA_MODES/EMAIL/@APPLICABLE");
        }

        if (strMFAModesSMSApplicable.equalsIgnoreCase("YES")) {
            fnSendSMS(strUsername, smsMessage, "YES", MSGConstants.MSGMode.EXPRESS, 200, "ONE_TIME_PASSWORD",
                    "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);

        }

        //DONE SEPARATELY IN CASE WE EVER SUPPORT SENDING TO BOTH CHANNELS
        if (strMFAModesEmailApplicable.equalsIgnoreCase("YES")) {

            if (signatoryDetailsMap.getStringValue("primary_email_address") != null) {
                EmailMessaging.sendEmail(signatoryDetailsMap.getStringValue("primary_email_address"), "Mobile Banking OTP", emailMessage, "MOBILE_BANKING_OTP");
                System.out.println("OTP sent via Email");
            } else {
                System.err.println("Signatory with Member Number '" + signatoryDetailsMap.getStringValue("identifier") + "' does not have a primary email address. Email OTP NOT Sent");
            }
        }
    }

    public String getTraceID(MAPPRequest theMAPPRequest) {
        //return theMAPPRequest.getTraceID(); //+APIUtils.getCurrentDate("yyyyMMddHHmmssSSS");
        return UUID.randomUUID().toString().toLowerCase();
    }

    public LinkedHashMap<String, String> getMemberAccountsList(MAPPRequest mappRequest, MAPPAPIConstants.AccountType theAccountType) {

        LinkedHashMap<String, String> accountsMap = new LinkedHashMap<>();

        String strUsername = mappRequest.getUsername();
        String strAppId = mappRequest.getAppID();

        try {

            String theCustomerIdentifier = getDefaultCustomerIdentifier(mappRequest);

            if (theCustomerIdentifier == null) {
                return accountsMap;
            }

            MAPPAPIConstants.AccountType theAccountTypeMain = theAccountType;

            if (theAccountType == MAPPAPIConstants.AccountType.WITHDRAWABLE_IFT) {
                theAccountTypeMain = MAPPAPIConstants.AccountType.WITHDRAWABLE;
            } else if (theAccountType == MAPPAPIConstants.AccountType.DEPOSIT_IFT) {
                theAccountTypeMain = MAPPAPIConstants.AccountType.DEPOSIT;
            }

            TransactionWrapper<FlexicoreHashMap> accountsListWrapper = CBSAPI.getCustomerAccounts(strUsername, "CUSTOMER_NO", theCustomerIdentifier, theAccountTypeMain.getValue());

            if (accountsListWrapper.hasErrors()) {
                System.err.println("MAPPAPI.LinkedHashMap<String, String> getAccountsListWithType() - ERROR:  " + accountsListWrapper.getErrors());
            } else {

                FlexicoreArrayList accountsList = accountsListWrapper.getSingleRecord().getValue("payload");

                System.out.println("ACCOUNTS FETCHED");
                System.out.println("--------------------------------------------------");

                for (FlexicoreHashMap accountMap : accountsList) {

                    String strAccountStatus = accountMap.getStringValue("account_status").trim();
                    String strAccountNumber = accountMap.getStringValue("account_number").trim();
                    String strAccountLabel = accountMap.getStringValue("account_label").trim();
                    String strAccountBookBalance = accountMap.getStringValue("account_balance").trim();
                    String strCanDeposit = accountMap.getStringValue("can_deposit").trim();
                    String strCanWithdraw = accountMap.getStringValue("can_withdraw").trim();
                    String strCanDepositIft = accountMap.getStringValue("can_deposit_ift").trim();
                    String strCanWithdrawIft = accountMap.getStringValue("can_withdraw_ift").trim();
                    String strProductId = accountMap.getStringValue("product_id").trim();

                    if (theAccountType == MAPPAPIConstants.AccountType.DEPOSIT) {
                        if (!strCanDeposit.equalsIgnoreCase("YES")) continue;

                    } else if (theAccountType == MAPPAPIConstants.AccountType.DEPOSIT_IFT) {
                        if (!strCanDepositIft.equalsIgnoreCase("YES")) continue;
                    } else if (theAccountType == MAPPAPIConstants.AccountType.WITHDRAWABLE) {

                        if (!strCanWithdraw.equalsIgnoreCase("YES")) {
                            continue;
                        }

                        if (strProductId.equalsIgnoreCase("080")) {
                            continue;
                        }

                        if (!strAccountStatus.equalsIgnoreCase("ACTIVE")) {
                            continue;
                        }
                    } else if (theAccountType == MAPPAPIConstants.AccountType.WITHDRAWABLE_IFT) {

                        if (!strCanWithdrawIft.equalsIgnoreCase("YES")) {
                            continue;
                        }

                        if (strProductId.equalsIgnoreCase("080")) {
                            continue;
                        }

                        if (!strAccountStatus.equalsIgnoreCase("ACTIVE")) {
                            continue;
                        }
                    }


                   /* if (isFundsTransfter && !strCanDeposit.equalsIgnoreCase("YES")) {
                        continue;
                    }*/

                    System.out.println(strAccountNumber + " - " + strAccountLabel);
                    accountsMap.put(strAccountNumber, strAccountLabel);
                }
            }

        } catch (Exception e) {
            System.err.println("MAPPAPI.getAccountsListWithType() - ERROR" + e.getMessage() + "\n");
            e.printStackTrace();
        }

        System.out.println();
        System.out.println("ACCOUNTS TO DISPLAY");
        System.out.println("--------------------------------------------------");
        accountsMap.forEach((strAccountNumber, strAccountLabel) -> System.out.println(strAccountNumber + " - " + strAccountLabel));

        return accountsMap;
    }

    public String getDefaultCustomerIdentifier(MAPPRequest mappRequest) {

        String strUsername = mappRequest.getUsername();
        String strAppId = mappRequest.getAppID();

        try {

            TransactionWrapper<FlexicoreHashMap> signatoryCustomersListWrapper = CBSAPI.getSignatoryCustomersList(getTraceID(mappRequest), "MSISDN", strUsername,
                    "APP_ID", strAppId);

            if (signatoryCustomersListWrapper.hasErrors()) {
                System.out.println("GOT HERE!!!");
                System.err.println("MAPPAPI.getDefaultCustomerIdentifier() - ERROR:  " + signatoryCustomersListWrapper.getErrors());
                FlexicoreHashMap flexicoreHashMap = signatoryCustomersListWrapper.getSingleRecord();
                flexicoreHashMap.printRecordVerticalLabelled();
                System.err.println("MAPPAPI.getDefaultCustomerIdentifier() - END!");
            } else {

                FlexicoreArrayList customersList = signatoryCustomersListWrapper.getSingleRecord().getValue("payload");

                if (customersList.isEmpty()) {
                    return null;
                }
                return customersList.getRecord(0).getStringValue("identifier");
            }

        } catch (Exception e) {
            System.err.println("MAPPAPI.getDefaultCustomerIdentifier() - ERROR" + e.getMessage() + "\n");
            e.printStackTrace();
        }

        return null;
    }
}
