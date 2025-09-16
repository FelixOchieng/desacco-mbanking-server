package ke.skyworld.mbanking.ussdapi;

import ke.co.skyworld.smp.query_manager.SystemTables;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_manager.util.SystemParameters;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.email.*;
import ke.skyworld.lib.mbanking.mapp.MAPPLocalParameters;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.pesa.PESA;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESAProcessor;
import ke.skyworld.lib.mbanking.ussd.USSDLocalParameters;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.mappapi.MAPPAPIDB;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.mbankingapi.PDF;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static ke.skyworld.mbanking.nav.cbs.CBSAPI.getDividendPayslip;
import static ke.skyworld.mbanking.ussdapi.APIUtils.*;

public class USSDAPI {
    public USSDAPI() {
    }

    public static String getTrailerMessage() {
        String strTrailerMessage;
        try {
            String strTrailerMessageXML = SystemParameters.getParameter(AppConstants.strSettingParamName);
            Document document = XmlUtils.parseXml(strTrailerMessageXML);

            String strNewlines = XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/TRAILER_MESSAGE/@NEWLINES");
            int intNewLines = Integer.parseInt(strNewlines);

            strTrailerMessage = "";
            for (int i = 0; i < intNewLines; i++) {
                strTrailerMessage = strTrailerMessage + "\n";
            }

            strTrailerMessage += XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/TRAILER_MESSAGE");

        } catch (Exception e) {
            e.printStackTrace();
            strTrailerMessage = "\n\nQueries? Please visit one of our branches or contact us for assistance.";
        }

        return strTrailerMessage;
    }

    private final LinkedHashMap<String, String> payBillCodesLinkedHashMap = new LinkedHashMap<>();

    // public APIConstants.CheckUserReturnVal checkUser(USSDRequest theUSSDRequest) {
    //     APIConstants.CheckUserReturnVal rVal = APIConstants.CheckUserReturnVal.ERROR;
    //     try {
    //         String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
    //         String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
    //
    //         String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);
    //
    //         System.out.println("Checking user");
    //
    //         String strCheckStatus = CBSAPI.userCheck(strMobileNumber, strSIMID, true, strUSSDSessionID);
    //         strCheckStatus = strCheckStatus.split(":::")[0];
    //
    //         System.out.println("NAV Returned: " + strCheckStatus);
    //
    //         switch (strCheckStatus) {
    //             case "ACTIVE": {
    //                 rVal = APIConstants.CheckUserReturnVal.ACTIVE;
    //                 break;
    //             }
    //             case "INVALID_IMSI": {
    //                 rVal = APIConstants.CheckUserReturnVal.INVALID_IMSI;
    //                 break;
    //             }
    //             case "INVALID_IMEI": {
    //                 rVal = APIConstants.CheckUserReturnVal.INVALID_IMEI;
    //                 break;
    //             }
    //             case "BLOCKED": {
    //                 rVal = APIConstants.CheckUserReturnVal.BLOCKED;
    //                 break;
    //             }
    //             case "SUSPENDED": {
    //                 rVal = APIConstants.CheckUserReturnVal.SUSPENDED;
    //                 break;
    //             }
    //             case "NOT_FOUND": {
    //                 rVal = APIConstants.CheckUserReturnVal.NOT_FOUND;
    //                 break;
    //             }
    //             case "ERROR": {
    //                 rVal = APIConstants.CheckUserReturnVal.ERROR;
    //                 break;
    //             }
    //             default: {
    //                 rVal = APIConstants.CheckUserReturnVal.ERROR;
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.err.println(this.getClass().getSimpleName() + "." + new Object() {
    //         }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
    //         e.printStackTrace();
    //     } finally {
    //     }
    //     return rVal;
    // }

    public TransactionWrapper<FlexicoreHashMap> checkUser(USSDRequest theUSSDRequest) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());


            return CBSAPI.checkUser(strReferenceKey, "MSISDN", strMobileNumber, "IMSI", strSIMID);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    // public APIConstants.CheckUserReturnVal checkUser(String theUserMSISDN) {
    //
    //     APIConstants.CheckUserReturnVal rVal = APIConstants.CheckUserReturnVal.ERROR;
    //     try {
    //         String strSIMID = "";
    //         String strCheckStatus = CBSAPI.userCheck(theUserMSISDN, strSIMID, true, "");
    //
    //         switch (strCheckStatus) {
    //             case "ACTIVE": {
    //                 rVal = APIConstants.CheckUserReturnVal.ACTIVE;
    //                 break;
    //             }
    //             case "INVALID_IMSI": {
    //                 rVal = APIConstants.CheckUserReturnVal.INVALID_IMSI;
    //                 break;
    //             }
    //             case "INVALID_IMEI": {
    //                 rVal = APIConstants.CheckUserReturnVal.INVALID_IMEI;
    //                 break;
    //             }
    //             case "BLOCKED": {
    //                 rVal = APIConstants.CheckUserReturnVal.BLOCKED;
    //                 break;
    //             }
    //             case "SUSPENDED": {
    //                 rVal = APIConstants.CheckUserReturnVal.SUSPENDED;
    //                 break;
    //             }
    //             case "NOT_FOUND": {
    //                 rVal = APIConstants.CheckUserReturnVal.NOT_FOUND;
    //                 break;
    //             }
    //             case "ERROR": {
    //                 rVal = APIConstants.CheckUserReturnVal.ERROR;
    //                 break;
    //             }
    //             default: {
    //                 rVal = APIConstants.CheckUserReturnVal.ERROR;
    //             }
    //         }
    //     } catch (Exception e) {
    //         System.err.println(this.getClass().getSimpleName() + "." + new Object() {
    //         }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
    //         e.printStackTrace();
    //     }
    //     return rVal;
    // }

    public TransactionWrapper<FlexicoreHashMap> userLogin(USSDRequest theUSSDRequest) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());


            return CBSAPI.userLogin(strReferenceKey, "MSISDN", strMobileNumber, strPIN, "IMSI", strSIMID, USSDAPIConstants.MobileChannel.USSD);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public TransactionWrapper<FlexicoreHashMap> getCurrentUserDetails(USSDRequest theUSSDRequest) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            return CBSAPI.getCurrentUserDetails(strReferenceKey, "MSISDN", strMobileNumber, "IMSI", strSIMID);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public String getUserAuthActionExpiryTime(USSDRequest theUSSDRequest) {
        String rVal = "";
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            XMLGregorianCalendar gcExpiryDate = CBSAPI.getUserLoginAttemptExpiry(strMobileNumber, "LOGIN");


            Date dtExpiryDate = gcExpiryDate.toGregorianCalendar().getTime();

            Date dtNow = new Date();

            rVal = "Please try again in " + APIUtils.getPrettyDateTimeDifferenceRoundedUp(dtNow, dtExpiryDate);


        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return rVal;
    }

    public TransactionWrapper<FlexicoreHashMap> acceptTermsAndConditions(USSDRequest theUSSDRequest, FlexicoreHashMap mobileBankingMap) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            return CBSAPI.acceptTermsAndConditions(mobileBankingMap, USSDAPIConstants.MobileChannel.USSD);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    // public LinkedHashMap<String, String> userLogin(USSDRequest theUSSDRequest) {
    //     LinkedHashMap<String, String> loginReturnVal = new LinkedHashMap<>();
    //     String rVal;
    //     loginReturnVal.put("LOGIN_RETURN_VALUE", "ERROR");
    //     loginReturnVal.put("LOGIN_ATTEMPT_MESSAGE", "");
    //     String loginAttemptMessage = "";
    //     try {
    //         String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
    //         String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
    //
    //         String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);
    //
    //         String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
    //         strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
    //
    //         String strNavResponse = CBSAPI.ussdLogin(strMobileNumber, strPIN, strSIMID, true, strUSSDSessionID);
    //
    //         System.out.println("strNavResponse: " + strNavResponse);
    //
    //
    //         String strLoginStatus = strNavResponse.split(":::")[0];
    //         switch (strLoginStatus) {
    //             case "SET_PIN": {
    //                 rVal = "SET_PIN";
    //                 break;
    //             }
    //             case "SUCCESS": {
    //                 rVal = "SUCCESS";
    //                 break;
    //             }
    //             case "INCORRECT_PIN": {
    //                 rVal = "INCORRECT_PIN";
    //
    //                 int intUserLoginAttemptsCount = Integer.parseInt(strNavResponse.split(":::")[1]);
    //                 String strName = strNavResponse.split(":::")[2];
    //
    //                 LinkedHashMap<String, String> hmMSGPlaceholders = new LinkedHashMap<>();
    //
    //                 hmMSGPlaceholders.put("[MOBILE_NUMBER]", strMobileNumber);
    //                 hmMSGPlaceholders.put("[LOGIN_ATTEMPTS]", String.valueOf(intUserLoginAttemptsCount));
    //                 hmMSGPlaceholders.put("[FIRST_NAME]", strName);
    //
    //                 String strAuthenticationParametersXML = USSDLocalParameters.getClientXMLParameters();
    //
    //                 HashMap<String, HashMap<String, String>> hmMBankingResponse = MBankingXMLFactory.getAuthenticationAttemptsAction(intUserLoginAttemptsCount, hmMSGPlaceholders, strAuthenticationParametersXML, MBankingConstants.AuthType.PASSWORD);
    //
    //                 if (!hmMBankingResponse.isEmpty()) {
    //                     HashMap<String, String> hmCurrentAttempt = hmMBankingResponse.get("CURRENT_ATTEMPT");
    //                     HashMap<String, String> hmNextAttempt = hmMBankingResponse.get("NEXT_ATTEMPT");
    //
    //                     String strUnit = hmCurrentAttempt.get("UNIT") != null ? hmCurrentAttempt.get("UNIT") : "";
    //                     String strAction = hmCurrentAttempt.get("ACTION") != null ? hmCurrentAttempt.get("ACTION") : "WARN";
    //                     String strDuration = hmCurrentAttempt.get("DURATION") != null ? hmCurrentAttempt.get("DURATION") : "";
    //                     String strDescription = hmCurrentAttempt.get("NAME") != null ? hmCurrentAttempt.get("NAME") : "";
    //
    //                     int intUnit = Calendar.DAY_OF_MONTH;
    //                     int intDuration = 0;
    //                     if (strDuration != null) {
    //                         if (!strDuration.equals("")) {
    //                             intDuration = Integer.parseInt(strDuration);
    //                         }
    //                     }
    //
    //                     if (strAction != null) {
    //                         if (strAction.equalsIgnoreCase("SUSPEND")) {
    //                             if (strUnit.equalsIgnoreCase("SECOND")) {
    //                                 intUnit = Calendar.SECOND;
    //                             } else if (strUnit.equalsIgnoreCase("MINUTE")) {
    //                                 intUnit = Calendar.MINUTE;
    //                             } else if (strUnit.equalsIgnoreCase("HOUR")) {
    //                                 intUnit = Calendar.HOUR;
    //                             } else if (strUnit.equalsIgnoreCase("DAY")) {
    //                                 intUnit = Calendar.DAY_OF_YEAR;
    //                             } else if (strUnit.equalsIgnoreCase("MONTH")) {
    //                                 intUnit = Calendar.MONTH;
    //                             } else if (strUnit.equalsIgnoreCase("YEAR")) {
    //                                 intUnit = Calendar.YEAR;
    //                             }
    //                             rVal = "SUSPENDED";
    //                         }
    //                         if (!hmNextAttempt.isEmpty()) {
    //                             String futureLoginAction = hmNextAttempt.get("ACTION");
    //                             String futureLoginActionDurationUnit = hmNextAttempt.get("UNIT");
    //                             String friendlyFutureActionDuration = hmNextAttempt.get("DURATION") + " " + futureLoginActionDurationUnit + "(S)";
    //                             String attemptsRemainingToFutureLoginAction = hmNextAttempt.get("ATTEMPTS_REMAINING");
    //
    //                             String currentLoginAction = hmCurrentAttempt.get("ACTION");
    //                             if (currentLoginAction == null) currentLoginAction = "NONE";
    //
    //                             //Override Incorrect PIN message
    //                             if (futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")) {
    //                                 loginAttemptMessage = "{Sorry the PIN provided is NOT correct}\nYou have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your mobile banking account is SUSPENDED for " + friendlyFutureActionDuration + ".\nPlease enter your PIN:";
    //                             } else if (futureLoginAction.equals("LOCK") && !currentLoginAction.equals("LOCK")) {
    //                                 loginAttemptMessage = "{Sorry the PIN provided is NOT correct}\nYou have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your mobile banking account is LOCKED. Please enter your PIN:";
    //                             }
    //                         }
    //                     }
    //
    //                     DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
    //                     GregorianCalendar gregorianCalendar = new GregorianCalendar();
    //                     gregorianCalendar.add(intUnit, intDuration);
    //                     XMLGregorianCalendar gcValidity = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
    //
    //                     String strResponse = CBSAPI.updateAuthAttempts(strMobileNumber, "LOGIN", intUserLoginAttemptsCount, strDescription, strAction, gcValidity, false);
    //                     System.out.println("Response: " + strResponse);
    //                 }
    //
    //                 break;
    //             }
    //             case "ERROR": {
    //                 rVal = "ERROR";
    //                 break;
    //             }
    //             default: {
    //                 rVal = "ERROR";
    //             }
    //         }
    //
    //         loginReturnVal.put("LOGIN_RETURN_VALUE", rVal);
    //         loginReturnVal.put("LOGIN_ATTEMPT_MESSAGE", loginAttemptMessage);
    //
    //     } catch (Exception e) {
    //         System.err.println(this.getClass().getSimpleName() + "." + new Object() {
    //         }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
    //         e.printStackTrace();
    //     }
    //
    //     return loginReturnVal;
    // }

    public TransactionWrapper<FlexicoreHashMap> isValidKYCDetails(USSDRequest theUSSDRequest, String thePrimaryIdentityNo) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            return CBSAPI.isValidKYCDetails(strReferenceKey, "MSISDN", strMobileNumber, "IMSI", strSIMID, "NATIONAL_ID", thePrimaryIdentityNo, USSDAPIConstants.MobileChannel.USSD);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }

        return resultWrapper;
    }

    public APIConstants.SetPINReturnVal setUserPIN(USSDRequest theUSSDRequest) {

        APIConstants.SetPINReturnVal rVal = APIConstants.SetPINReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
            String strNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name());
            String strServiceNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_SERVICE_NO.name());
            String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());

            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            strNewPIN = APIUtils.hashPIN(strNewPIN, strMobileNumber);

            String strValidateKycDetailsStatus = CBSAPI.validateKYCdetails(strMobileNumber, strServiceNo, strIDNo, strNewPIN, strPIN, strSIMID, true);
            System.out.println("strValidateKycDetailsStatus: " + strValidateKycDetailsStatus);

            switch (strValidateKycDetailsStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.SetPINReturnVal.SUCCESS;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    rVal = APIConstants.SetPINReturnVal.INVALID_ACCOUNT;
                    break;
                }
                case "INVALID_SERVICE_NUMBER": {
                    rVal = APIConstants.SetPINReturnVal.INVALID_SERVICE_NUMBER;
                    break;
                }
                case "INVALID_ID_NUMBER": {
                    rVal = APIConstants.SetPINReturnVal.INVALID_ID_NUMBER;
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.SetPINReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_NEW_PIN": {
                    rVal = APIConstants.SetPINReturnVal.INVALID_NEW_PIN;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.SetPINReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.SetPINReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.ChangePINReturnVal changeUserPIN(USSDRequest theUSSDRequest) {
        APIConstants.ChangePINReturnVal rVal = APIConstants.ChangePINReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN.name());
            String strNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name());

            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            strNewPIN = APIUtils.hashPIN(strNewPIN, String.valueOf(theUSSDRequest.getUSSDMobileNo()));

            String strChangePinStatus = CBSAPI.setNewPin(strMobileNumber, strPIN, strNewPIN);

            switch (strChangePinStatus) {

                case "SUCCESS": {
                    rVal = APIConstants.ChangePINReturnVal.SUCCESS;
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.ChangePINReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_NEW_PIN": {
                    rVal = APIConstants.ChangePINReturnVal.INVALID_NEW_PIN;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.ChangePINReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.ChangePINReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.AccountRegistrationReturnVal accountRegistration(USSDRequest theUSSDRequest) {
        APIConstants.AccountRegistrationReturnVal rVal = APIConstants.AccountRegistrationReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strMemberName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME.name());
            String strMemberMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER.name());
            String strMemberNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER.name());
            String strMemberDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH.name());

            DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

            strMemberDateOfBirth = strMemberDateOfBirth.replaceAll("\\D", "/");

            Date dtMemberDateOfBirth = format.parse(strMemberDateOfBirth);

            GregorianCalendar calMemberDateOfBirth = new GregorianCalendar();
            calMemberDateOfBirth.setTime(dtMemberDateOfBirth);
            XMLGregorianCalendar xmlGregCalMemberDateOfBirth = DatatypeFactory.newInstance().newXMLGregorianCalendar(calMemberDateOfBirth);

            strMemberMobileNumber = APIUtils.sanitizePhoneNumber(strMemberMobileNumber);

            String strEntryNumber = fnModifyUSSDSessionID(theUSSDRequest);

            String strMemberVirtualRegistrationStatus = "";// Navision.getPort().registerVirtualMember(strMemberName, strMemberNationalIDNumber, strMemberMobileNumber, xmlGregCalMemberDateOfBirth, strMobileNumber, strEntryNumber);
            switch (strMemberVirtualRegistrationStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.AccountRegistrationReturnVal.SUCCESS;
                    break;
                }
                case "MEMBER_EXISTS": {
                    rVal = APIConstants.AccountRegistrationReturnVal.MEMBER_EXISTS;
                    break;
                }
                case "ENTRY_EXISTS": {
                    rVal = APIConstants.AccountRegistrationReturnVal.ENTRY_EXISTS;
                    break;
                }
                case "PIN_MISMATCH": {
                    rVal = APIConstants.AccountRegistrationReturnVal.PIN_MISMATCH;
                    break;
                }
                case "INVALID_PIN": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_PIN;
                    break;
                }
                case "INVALID_FIRSTNAME": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_FIRSTNAME;
                    break;
                }
                case "INVALID_LASTNAME": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_LASTNAME;
                    break;
                }
                case "INVALID_IDNO": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_IDNO;
                    break;
                }
                case "INVALID_DOB": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_DOB;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.AccountRegistrationReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.AccountRegistrationReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.AccountRegistrationReturnVal selfRegistration(USSDRequest theUSSDRequest) {
        APIConstants.AccountRegistrationReturnVal rVal = APIConstants.AccountRegistrationReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strMemberName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NAME.name());
            String strMemberNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER.name());
            String strMemberDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH.name());

            DateFormat format = new SimpleDateFormat("dd/MM/yyyy");

            strMemberDateOfBirth = strMemberDateOfBirth.replaceAll("\\D", "/");

            Date dtMemberDateOfBirth = format.parse(strMemberDateOfBirth);

            GregorianCalendar calMemberDateOfBirth = new GregorianCalendar();
            calMemberDateOfBirth.setTime(dtMemberDateOfBirth);
            XMLGregorianCalendar xmlGregCalMemberDateOfBirth = DatatypeFactory.newInstance().newXMLGregorianCalendar(calMemberDateOfBirth);

            String strEntryNumber = fnModifyUSSDSessionID(theUSSDRequest);

            String strMemberVirtualRegistrationStatus = "";// Navision.getPort().registerVirtualMember(strMemberName, strMemberNationalIDNumber, strMobileNumber, xmlGregCalMemberDateOfBirth, "", strEntryNumber);
            switch (strMemberVirtualRegistrationStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.AccountRegistrationReturnVal.SUCCESS;
                    break;
                }
                case "MEMBER_EXISTS": {
                    rVal = APIConstants.AccountRegistrationReturnVal.MEMBER_EXISTS;
                    break;
                }
                case "ENTRY_EXISTS": {
                    rVal = APIConstants.AccountRegistrationReturnVal.ENTRY_EXISTS;
                    break;
                }
                case "PIN_MISMATCH": {
                    rVal = APIConstants.AccountRegistrationReturnVal.PIN_MISMATCH;
                    break;
                }
                case "INVALID_PIN": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_PIN;
                    break;
                }
                case "INVALID_FIRSTNAME": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_FIRSTNAME;
                    break;
                }
                case "INVALID_LASTNAME": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_LASTNAME;
                    break;
                }
                case "INVALID_IDNO": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_IDNO;
                    break;
                }
                case "INVALID_DOB": {
                    rVal = APIConstants.AccountRegistrationReturnVal.INVALID_DOB;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.AccountRegistrationReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.AccountRegistrationReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public LinkedHashMap<String, String> getBankAccounts(USSDRequest theUSSDRequest, APIConstants.AccountType theAccountType, String theGroup) {
        LinkedHashMap<String, String> accounts = null;
        try {
            String strAccountCategory;
            boolean blWithdrawable = false;
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            accounts = new LinkedHashMap<>();

            String strAccountXML;

            switch (theAccountType) {
                case WITHDRAWABLE: {
                    strAccountCategory = "WITHDRAWABLE_ACCOUNTS";
                    blWithdrawable = true;
                    break;
                }
                case FOSA: {
                    strAccountCategory = "FOSA_ACCOUNTS";
                    break;
                }
                case BOSA: {
                    strAccountCategory = "BOSA_ACCOUNTS";
                    break;
                }
                case INVESTMENT: {
                    strAccountCategory = "INVESTMENT_ACCOUNTS";
                    break;
                }
                case LOAN: {
                    strAccountCategory = "ALL_LOANS";
                    break;
                }
                case ALL:
                default: {
                    strAccountCategory = "ALL_ACCOUNTS";
                    break;
                }
            }

            strAccountXML = CBSAPI.getSavingsAccountList(strMobileNumber, blWithdrawable, strAccountCategory);

            InputSource source = new InputSource(new StringReader(strAccountXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
            accounts = new LinkedHashMap<>();

            for (int i = 0; i < nlAccounts.getLength(); i++) {
                NodeList nlAccount = ((NodeList) configXPath.evaluate("Account", nlAccounts, XPathConstants.NODESET)).item(i).getChildNodes();
                accounts.put(nlAccount.item(0).getTextContent(), nlAccount.item(1).getTextContent());
            }
            accounts = (accounts.size() > 0) ? accounts : null;

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return accounts;
    }

    public FlexicoreArrayList getBankAccounts_V2(USSDRequest theUSSDRequest, APIConstants.AccountType theAccountType, String theGroup) {
        FlexicoreArrayList accounts = null;
        try {
            String strAccountCategory;
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            switch (theAccountType) {
                case WITHDRAWABLE: {
                    strAccountCategory = "WITHDRAWABLE";
                    break;
                }
                case FOSA: {
                    strAccountCategory = "FOSA";
                    break;
                }
                case BOSA: {
                    strAccountCategory = "BOSA";
                    break;
                }
                case LOAN: {
                    strAccountCategory = "ALL_LOANS";
                    break;
                }
                case ALL:
                default: {
                    strAccountCategory = "ALL_ACCOUNTS";
                    break;
                }
            }

            HashMap<String, String> userIdentifierDetails = APIUtils.getUserIdentifierDetails(strMobileNumber);
            String strIdentifierType = userIdentifierDetails.get("identifier_type");
            String strIdentifier = userIdentifierDetails.get("identifier");

            TransactionWrapper<FlexicoreHashMap> accountsListWrapper = CBSAPI.getSavingsAccountList_V2(strMobileNumber, strIdentifierType, strIdentifier, strAccountCategory);

            if (!accountsListWrapper.hasErrors()) {
                FlexicoreArrayList accountsList = accountsListWrapper.getSingleRecord().getValue("payload");
                accounts = accountsList;
            }

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return accounts;
    }

    public LinkedHashMap<String, String> getAccountGroups(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, String> accounts = null;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            accounts = new LinkedHashMap<>();

            String strAccountsXML = CBSAPI.getSavingsAccountList(strMobileNumber, true, "");

            InputSource source = new InputSource(new StringReader(strAccountsXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            NodeList nlAccounts = ((NodeList) configXPath.evaluate("/Accounts", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
            accounts = new LinkedHashMap<>();

            for (int i = 0; i < nlAccounts.getLength(); i++) {
                NodeList nlAccount = ((NodeList) configXPath.evaluate("Account", nlAccounts, XPathConstants.NODESET)).item(i).getChildNodes();
                // accounts.put(nlAccount.item(0).getTextContent(), nlAccount.item(1).getTextContent());
                // accounts.put(nlAccount.item(0).getTextContent(), nlAccount.item(1).getTextContent());
            }

            accounts.put("G001", "Bidii Youth Group");
            accounts.put("G002", "Umoja Youth Group");

            accounts = (accounts.size() > 0) ? accounts : null;

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return accounts;
    }

    public LinkedHashMap<String, String> getMemberAccountDetails(USSDRequest theUSSDRequest, String theToOption, String theAccountID) {
        LinkedHashMap<String, String> account = null;
        try {

            String strAccountNumberXML;
            String strSource = "Mobile";

            if (theToOption.equals("ID Number")) {
                strSource = "ID";
            }
            if (theToOption.equals("Account No")) {
                strSource = "ACCOUNT";
            }

            if (strSource.equals("Mobile")) {
                theAccountID = APIUtils.sanitizePhoneNumber(theAccountID);
            }

            strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(theAccountID, strSource);
            System.out.println("strAccountNumberXML: " + strAccountNumberXML);

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList nlAccount = ((NodeList) configXPath.evaluate("/Account", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                account = new LinkedHashMap<>();

                account.put(nlAccount.item(0).getTextContent(), nlAccount.item(1).getTextContent());
            }

            if (account != null) {
                account = (account.size() > 0) ? account : null;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return account;
    }

    public TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiry(USSDRequest theUSSDRequest, String strAccountNumber) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            HashMap<String, String> userIdentifierDetails = APIUtils.getUserIdentifierDetails(strMobileNumber);
            String strIdentifierType = userIdentifierDetails.get("identifier_type");
            String strIdentifier = userIdentifierDetails.get("identifier");

            TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiryWrapper =  CBSAPI.accountBalanceEnquirySINGLE(strMobileNumber, strIdentifierType, strIdentifier, "IMSI", strSIMID, strAccountNumber, "USSD");

            FlexicoreHashMap accountBalanceMap = accountBalanceEnquiryWrapper.getSingleRecord();
            CBSAPI.SMSMSG cbsMSG = accountBalanceMap.getValue("msg_object");
            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            String strBalanceEnquiryMessage = cbsMSG.getMessage();

            strBalanceEnquiryMessage += "\nREF: 11XMQ1U6";

            sendSMS(String.valueOf(theUSSDRequest.getUSSDMobileNo()), strBalanceEnquiryMessage, cbsMSG.getMode(), cbsMSG.getPriority(), "BALANCE_ENQUIRY", theUSSDRequest);

            return accountBalanceEnquiryWrapper;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage()));

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            sendSMS(strMobileNumber, "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage(),
                    MSGConstants.MSGMode.SAF, 210, "BALANCE_ENQUIRY", theUSSDRequest);
        }

        return resultWrapper;
    }

    public TransactionWrapper<FlexicoreHashMap> loanBalanceEnquiry(USSDRequest theUSSDRequest, String strAccountNumber) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            String strMemberName = getUserFullName(strMobileNumber).trim();

            TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiryWrapper = CBSAPI.loanBalanceEnquiry(strMobileNumber,
                    "MSISDN", strMobileNumber, "IMSI", strSIMID, strAccountNumber, "USSD");

            FlexicoreHashMap accountBalanceMap = accountBalanceEnquiryWrapper.getSingleRecord();
            CBSAPI.SMSMSG cbsMSG = accountBalanceMap.getValue("msg_object");

            String strOriginatorId = UUID.randomUUID().toString();

            String strBalanceEnquiryMessage = cbsMSG.getMessage();
            // String strCharges = Utils.formatDouble(charges, "#,##0.00");
            // strBalanceEnquiryMessage += " Charges: KES " + strCharges + ".\n";
            // strBalanceEnquiryMessage += "Ref: " + strReferenceKey +" Date: " + DateTime.getCurrentDate("dd-MMM-yy' at 'hh:mm aaa") + "\n";
            // strBalanceEnquiryMessage += "You can always view your full statement for free on USSD and APP.";

            sendSMS(strMobileNumber, strBalanceEnquiryMessage, cbsMSG.getMode(), cbsMSG.getPriority(), "LOAN_BALANCE_ENQUIRY", theUSSDRequest);

            return accountBalanceEnquiryWrapper;
        } catch (Exception e) {
            e.printStackTrace();

            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later." + getTrailerMessage()));

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            sendSMS(strMobileNumber, "Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later." + getTrailerMessage(),
                    MSGConstants.MSGMode.SAF, 210, "LOAN_BALANCE_ENQUIRY", theUSSDRequest);
        }

        return resultWrapper;
    }

    public APIConstants.TransactionReturnVal accountMiniStatement(USSDRequest theUSSDRequest, APIConstants.AccountType theAccountType) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strAccountType = theAccountType.getValue();
            String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            int intMaxNumberRows = 5;

            String strAccountMiniStatementStatus;
            if (strAccountType.equals(APIConstants.AccountType.LOAN.getValue())) {
                HashMap<String, String> hmAccount = Utils.toHashMap(strAccount);
                strAccount = hmAccount.get("LOAN_ID");
                strAccount = strAccount.replace("LOAN", "");

                strAccountMiniStatementStatus = CBSAPI.loanMiniStatement(fnModifyUSSDSessionID(theUSSDRequest), strUSSDSessionID, intMaxNumberRows, strAccount, strMobileNumber, strPIN);

            } else {
                strAccountMiniStatementStatus = CBSAPI.accountMiniStatement(fnModifyUSSDSessionID(theUSSDRequest), strUSSDSessionID, intMaxNumberRows, strAccount, strMobileNumber, strPIN);
            }

            switch (strAccountMiniStatementStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "BLOCKED": {
                    rVal = APIConstants.TransactionReturnVal.BLOCKED;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    // public APIConstants.TransactionReturnVal mobileMoneyWithdrawal(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType) {
    //     APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
    //     try {
    //         String strMobileNumberFrom = String.valueOf(theUSSDRequest.getUSSDMobileNo());
    //         String strMobileNumberTo = strMobileNumberFrom;
    //         String strBeneficiaryName = "";
    //
    //         String strDate = MBankingDB.getDBDateTime().trim();
    //         String strGUID = fnModifyUSSDSessionID(theUSSDRequest);
    //
    //         String strTraceID = theUSSDRequest.getUSSDTraceID();
    //
    //         String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);
    //
    //         int intPriority = 200;
    //
    //         String strWithdrawalToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());
    //
    //         if (!strWithdrawalToOption.equals("MY_NUMBER")) {
    //             String strMobileNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
    //             HashMap<String, String> hmAccount = Utils.toHashMap(strMobileNumberHashMap);
    //             strMobileNumberTo = hmAccount.get("ACCOUNT_IDENTIFIER");
    //             strBeneficiaryName = hmAccount.get("ACCOUNT_NAME");
    //         }
    //
    //         strMobileNumberTo = APIUtils.sanitizePhoneNumber(strMobileNumberTo);
    //
    //         String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
    //         String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
    //         String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());
    //         strPIN = APIUtils.hashPIN(strPIN, strMobileNumberFrom);
    //         String strTransaction = "Withdrawal Request";
    //         String strTransactionDescription = "M-Pesa Cash Withdrawal to " + strMobileNumberFrom;
    //         strTransactionDescription = PESAAPI.shortenName(strTransactionDescription);
    //         XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();
    //
    //         PesaParam pesaParam = PESAAPI.getPesaParam(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.PESA, ke.skyworld.mbanking.pesaapi.APIConstants.PESA_PARAM_TYPE.MPESA_B2C);
    //
    //         long getProductID = Long.parseLong(pesaParam.getProductId());
    //         String strCategory = "MOBILE_MONEY_WITHDRAWAL";
    //
    //         String strSenderIdentifier = pesaParam.getSenderIdentifier();
    //         String strSenderAccount = pesaParam.getSenderAccount();
    //         String strSenderName = pesaParam.getSenderName();
    //
    //         PESA pesa = new PESA();
    //
    //         pesa.setOriginatorID(strGUID);
    //         pesa.setProductID(getProductID);
    //         pesa.setPESAType(thePESAType);
    //         pesa.setPESAAction(PESAConstants.PESAAction.B2C);
    //         pesa.setCommand("BusinessPayment");
    //         pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
    //         // pesa.setChargeProposed(null);
    //
    //         pesa.setInitiatorType("MSISDN");
    //         pesa.setInitiatorIdentifier(strMobileNumberFrom);
    //         pesa.setInitiatorAccount(strMobileNumberFrom);
    //         // pesa.setInitiatorName(""); - Set after getting name from CBS
    //         pesa.setInitiatorReference(strTraceID);
    //         pesa.setInitiatorApplication("USSD");
    //         pesa.setInitiatorOtherDetails("<DATA/>");
    //
    //         pesa.setSourceType("ACCOUNT_NO");
    //         pesa.setSourceIdentifier(strAccountFrom);
    //         pesa.setSourceAccount(strAccountFrom);
    //         // pesa.setSourceName(""); - Set after getting name from CBS
    //         pesa.setSourceReference(strUSSDSessionID);
    //         pesa.setSourceApplication("CBS");
    //         pesa.setSourceOtherDetails("<DATA/>");
    //
    //         pesa.setSenderType("SHORT_CODE");
    //         pesa.setSenderIdentifier(strSenderIdentifier);
    //         pesa.setSenderAccount(strSenderAccount);
    //         pesa.setSenderName(strSenderName);
    //         pesa.setSenderOtherDetails("<DATA/>");
    //
    //         pesa.setReceiverType("MSISDN");
    //         pesa.setReceiverIdentifier(strMobileNumberTo);
    //         pesa.setReceiverAccount(strMobileNumberTo);
    //         // pesa.setReceiverName(""); - Set after getting name from CBS
    //         pesa.setReceiverOtherDetails("<DATA/>");
    //
    //         pesa.setBeneficiaryType("MSISDN");
    //         pesa.setBeneficiaryIdentifier(strMobileNumberTo);
    //         pesa.setBeneficiaryAccount(strMobileNumberTo);
    //         // pesa.setBeneficiaryName(""); - Set after getting name from CBS
    //         pesa.setBeneficiaryOtherDetails("<DATA/>");
    //
    //         pesa.setBatchReference(strGUID);
    //         pesa.setCorrelationReference(strTraceID);
    //         pesa.setCorrelationApplication("USSD");
    //         pesa.setTransactionCurrency("KES");
    //         pesa.setTransactionAmount(Double.parseDouble(strAmount));
    //         pesa.setTransactionRemark(strTransactionDescription);
    //         pesa.setCategory("CASH_WITHDRAWAL");
    //
    //         pesa.setPriority(200);
    //         pesa.setSendCount(0);
    //
    //         pesa.setSchedulePesa(PESAConstants.Condition.NO);
    //         pesa.setPesaDateScheduled(strDate);
    //         pesa.setPesaDateCreated(strDate);
    //         pesa.setPESAXMLData("<DATA/>");
    //
    //         pesa.setPESAStatusCode(10);
    //         pesa.setPESAStatusName("QUEUED");
    //         pesa.setPESAStatusDescription("New PESA");
    //         pesa.setPESAStatusDate(strDate);
    //
    //         String strWithdrawalStatus = "";
    //         boolean isOtherNumber = false;
    //
    //         try {
    //             if (strMobileNumberFrom.equals(strMobileNumberTo)) {
    //                 isOtherNumber = true;
    //             }
    //
    //             strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strGUID, strUSSDSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumberFrom, strPIN, "USSD", strUSSDSessionID, "MBANKING", strMobileNumberTo, strMobileNumberTo, "M-Pesa", isOtherNumber, strMobileNumberTo);
    //         } catch (Exception e) {
    //
    //             e.printStackTrace();
    //         }
    //
    //         System.out.println("Withdrawal Status: " + strWithdrawalStatus);
    //         String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");
    //
    //         switch (arrWithdrawalStatus[0]) {
    //             case "SUCCESS": {
    //
    //                 String strMemberName = arrWithdrawalStatus[1].trim();
    //                 pesa.setSourceName(strMemberName);
    //
    //                 if (strWithdrawalToOption.equalsIgnoreCase("OTHER_NUMBER")) {
    //                     pesa.setReceiverName(strMobileNumberTo);
    //                     pesa.setBeneficiaryName(strBeneficiaryName);
    //                 } else {
    //                     pesa.setReceiverName(strMemberName);
    //                     pesa.setBeneficiaryName(strMemberName);
    //                 }
    //
    //                 pesa.setInitiatorName(strMemberName);
    //
    //                 if (PESAProcessor.sendPESA(pesa) > 0) {
    //                     rVal = APIConstants.TransactionReturnVal.SUCCESS;
    //                 } else {
    //                     CBSAPI.reverseWithdrawalRequest(strGUID);
    //                 }
    //                 break;
    //             }
    //             case "INCORRECT_PIN": {
    //                 rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
    //                 break;
    //             }
    //             case "INVALID_ACCOUNT": {
    //                 rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
    //                 break;
    //             }
    //             case "INSUFFICIENT_BAL": {
    //                 rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
    //                 break;
    //             }
    //             case "ACCOUNT_NOT_ACTIVE": {
    //                 rVal = APIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
    //                 break;
    //             }
    //             case "BLOCKED": {
    //                 rVal = APIConstants.TransactionReturnVal.BLOCKED;
    //                 break;
    //             }
    //             default: {
    //                 rVal = APIConstants.TransactionReturnVal.ERROR;
    //             }
    //         }
    //     } catch (Exception e) {
    //
    //         System.err.println(this.getClass().getSimpleName() + "." + new Object() {
    //         }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
    //         e.printStackTrace();
    //     }
    //
    //     return rVal;
    // }

    public TransactionWrapper<FlexicoreHashMap> mobileMoneyWithdrawal(USSDRequest theUSSDRequest) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();
        String strCategory = "MPESA_WITHDRAWAL";

        // USSDAPIConstants.TransactionReturnVal rVal = USSDAPIConstants.TransactionReturnVal.ERROR;
        try {
            String strDateTime = MBankingDB.getDBDateTime();
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

            String strMobileNumberFrom = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strMobileNumberTo = strMobileNumberFrom;

            // String strOriginatorID = theUSSDRequest.getUSSDTraceID();
            String strTraceID = theUSSDRequest.getUSSDTraceID();

            String strMemberName = getUserFullName(strMobileNumber).trim();

            String strTransactionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            int intPriority = 200;

            String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_OPTION.name());
            String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

            // System.out.println("strOption = " + strOption);
            // System.out.println("strToOption = " + strToOption);

            APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel("M-PESA");
            if (withdrawalChannel != null) {
                if (withdrawalChannel.hasWithdrawalToOtherNumberEnabled()) {
                    if (strToOption != null) {
                        if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                            String strMobileNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                            HashMap<String, String> hmAccount = Utils.toHashMap(strMobileNumberHashMap);
                            strMobileNumberTo = hmAccount.get("ACCOUNT_IDENTIFIER");

                            strMobileNumberTo = APIUtils.sanitizePhoneNumber(strMobileNumberTo);
                        }
                    }
                }
            }

            strMobileNumberTo = APIUtils.sanitizePhoneNumber(strMobileNumberTo);

            String strAccountDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
            HashMap<String, String> hmAccountDetails = Utils.toHashMap(strAccountDetails);
            String strSourceCustomerIdentifier = hmAccountDetails.get("cust_id");
            String strSourceAccountNo = hmAccountDetails.get("ac_no");
            String strSourceAccountName = hmAccountDetails.get("ac_name");
            String strSourceAccountLabel = hmAccountDetails.get("ac_label");
            String strSourceAccountAvailableBalance = hmAccountDetails.get("ac_bal");

            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());

            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2C);

            long getProductID = Long.parseLong(pesaParam.getProductId());

            String strSenderIdentifier = pesaParam.getSenderIdentifier();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strSenderName = pesaParam.getSenderName();

            String strSourceName = strSourceAccountName;
            String strReceiverName = strSourceAccountName;
            String strBeneficiaryName = strSourceAccountName;

            if (strToOption != null) {
                if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                    strSourceName = strMobileNumberTo;
                    strReceiverName = strMobileNumberTo;
                    strBeneficiaryName = strMobileNumberTo;
                }
            }

            PESA pesa = new PESA();

            String strOriginatorID = strTransactionID;

            pesa.setOriginatorID(strOriginatorID);
            pesa.setProductID(getProductID);

            pesa.setPESAType(PESAConstants.PESAType.PESA_OUT);
            pesa.setPESAAction(PESAConstants.PESAAction.B2C);
            pesa.setCommand("BusinessPayment");
            pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);

            pesa.setPESAStatusCode(10);
            pesa.setPESAStatusName("QUEUED");
            pesa.setPESAStatusDescription("New PESA");
            pesa.setPESAStatusDate(strDateTime);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumber);
            pesa.setInitiatorAccount(strMobileNumber);
            pesa.setInitiatorName(strMemberName);
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

            pesa.setSourceType("ACCOUNT_NO");
            pesa.setSourceIdentifier(strSourceAccountNo);
            pesa.setSourceAccount(strSourceAccountNo);
            pesa.setSourceName(strSourceAccountName);
            // deferred to below
            pesa.setSourceReference(strOriginatorID);
            pesa.setSourceApplication("CBS");
            pesa.setSourceOtherDetails("<DATA/>");

            pesa.setSenderType("SHORT_CODE");
            pesa.setSenderIdentifier(strSenderIdentifier);
            pesa.setSenderAccount(strSenderAccount);
            pesa.setSenderName(strSenderName);
            pesa.setSenderOtherDetails("<DATA/>");

            pesa.setReceiverType("MSISDN");
            pesa.setReceiverIdentifier(strMobileNumberTo);
            pesa.setReceiverAccount(strMobileNumberTo);
            pesa.setReceiverName(strReceiverName);
            pesa.setReceiverOtherDetails("<DATA/>");

            pesa.setBeneficiaryType("MSISDN");
            pesa.setBeneficiaryIdentifier(strMobileNumberTo);
            pesa.setBeneficiaryAccount(strMobileNumberTo);
            pesa.setBeneficiaryName(strReceiverName);
            pesa.setBeneficiaryOtherDetails("<DATA/>");


            if (strToOption != null) {
                if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                    pesa.setReceiverName(strMobileNumberTo);
                    pesa.setBeneficiaryName(strMobileNumberTo);
                } else {
                    pesa.setReceiverName(strMemberName);
                    pesa.setBeneficiaryName(strMemberName);
                }
            } else {
                pesa.setReceiverName(strMemberName);
                pesa.setBeneficiaryName(strMemberName);
            }

            pesa.setBatchReference(strOriginatorID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
            pesa.setTransactionCurrency("KES");
            pesa.setTransactionAmount(Double.parseDouble(strAmount));
            pesa.setTransactionRemark("Mobile withdrawal to " + strMobileNumberTo);
            pesa.setCategory(strCategory);

            pesa.setPriority(200);
            pesa.setSendCount(0);

            pesa.setPESAXMLData("<DATA/>");

            pesa.setSchedulePesa(PESAConstants.Condition.NO);
            pesa.setPesaDateScheduled(strDateTime);
            pesa.setPesaDateCreated(strDateTime);

            TransactionWrapper<FlexicoreHashMap> mobileMoneyWithdrawalWrapper = CBSAPI.mobileMoneyWithdrawal(
                    strMobileNumber,
                    "MSISDN",
                    strMobileNumber,
                    "IMSI",
                    strSIMID,
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
                    "USSD",
                    "MBANKING");

            FlexicoreHashMap mobileMoneyWithdrawalMap = mobileMoneyWithdrawalWrapper.getSingleRecord();

            CBSAPI.SMSMSG cbsMSG = mobileMoneyWithdrawalMap.getValue("msg_object");

            if (mobileMoneyWithdrawalWrapper.hasErrors()) {
                sendSMS(strMobileNumber, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theUSSDRequest);
            } else {

                String strFormattedAmount = Utils.formatDouble(strAmount, "#,##0.00");
                String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                String strSourceReference = mobileMoneyWithdrawalMap.getFlexicoreHashMap("response_payload").getStringValue("transaction_reference");
                pesa.setSourceReference(strSourceReference);

                if (PESAProcessor.sendPESA(pesa) > 0) {
                    // Substituted with the one for results

                    sendSMS(strMobileNumber, cbsMSG.getMessage(), cbsMSG.getMode(), cbsMSG.getPriority(), strCategory, theUSSDRequest);
                } else {

                    TransactionWrapper<FlexicoreHashMap> reversalCashWithdrawalWrapper =
                            CBSAPI.reverseMobileMoneyWithdrawal(
                                    strMobileNumber,
                                    "MSISDN",
                                    strMobileNumber,
                                    pesa.getOriginatorID(),
                                    pesa.getBeneficiaryType(),
                                    pesa.getBeneficiaryIdentifier(),
                                    pesa.getBeneficiaryName(),
                                    pesa.getBeneficiaryOtherDetails(),
                                    "",
                                    DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"));

                    String strMSG;
                    if (!reversalCashWithdrawalWrapper.hasErrors()) {
                        strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " has been REVERSED. Dial " + AppConstants.strGeneralMenusUSSDCode + " to check your balance.";
                    } else {
                        strMSG = "Dear member, your M-PESA Withdrawal request of KES " + strFormattedAmount + " to " + strMobileNumberTo + " on " + strFormattedDateTime + " REVERSAL FAILED. Please contact the SACCO for assistance.";
                    }

                    sendSMS(strMobileNumber, strMSG, MSGConstants.MSGMode.SAF, 210, strCategory, theUSSDRequest);

                    reversalCashWithdrawalWrapper.getSingleRecord().putValue("display_message", strMSG);

                    return reversalCashWithdrawalWrapper;
                }
            }

            mobileMoneyWithdrawalWrapper.getSingleRecord().putValue("display_message", cbsMSG.getMessage());

            return mobileMoneyWithdrawalWrapper;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later." + getTrailerMessage()))
            ;

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());



			/*sendSMS(strMobileNumber, "Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later."+getTrailerMessage(),
					MSGConstants.MSGMode.SAF, 210, strCategory, theUSSDRequest);*/

        }

        return resultWrapper;
    }

    public APIConstants.TransactionReturnVal atmCashWithdrawal(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strMobileNumberToReceiveSMS = strMobileNumber;
            long lnMobileNumber = Long.parseLong(strMobileNumber);

            String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_OPTION.name());
            String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

            APIUtils.WithdrawalChannel withdrawalChannel = APIUtils.getWithdrawalChannel(strOption);
            if (withdrawalChannel != null) {
                if (withdrawalChannel.hasWithdrawalToOtherNumberEnabled()) {
                    if (strToOption != null) {
                        if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                            strMobileNumberToReceiveSMS = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                        }
                    }
                }
            }

            strMobileNumberToReceiveSMS = APIUtils.sanitizePhoneNumber(strMobileNumberToReceiveSMS);
            long lnMobileNumberToReceiveSMS = Long.parseLong(strMobileNumberToReceiveSMS);

            long lnSessionID = theUSSDRequest.getUSSDSessionID();

            String strGUID = fnModifyUSSDSessionID(theUSSDRequest);

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
            long lnAmount = Long.parseLong(strAmount);
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            String strTransaction = "Withdrawal Request";

            String strMemberNames = CBSAPI.getMemberName(strMobileNumber);

            String strTransactionDescription = "ATM Cash Withdrawal by " + strMobileNumber + " - " + strMemberNames + " to " + strMobileNumber;

            XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();
            boolean isOtherNumber = false;

            String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strGUID, strUSSDSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumber, strPIN, "USSD", strUSSDSessionID, "MBANKING", strMobileNumber, strMemberNames, "ATM", isOtherNumber, strMobileNumber);

            System.out.println("Withdrawal Status: " + strWithdrawalStatus);
            String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

            switch (arrWithdrawalStatus[0]) {
                case "SUCCESS": {
                    String strMemberName = arrWithdrawalStatus[1].split(" ")[0];

                    String strSMS = "InterSwitchAPI.sendRequest(lnMobileNumberToReceiveSMS, lnAmount, lnSessionID, strMemberName)";
                    if (strSMS.equalsIgnoreCase("ERROR")) {
                        /*API CALL TO CBS START*/
                        // instStart = Instant.now();
                        // System.out.println("Making API Call To NAV");
                        CBSAPI.reverseWithdrawalRequest(strGUID);
                        // instEnd = Instant.now();
                        // durTimeElapsed = Duration.between(instStart, instEnd);
                        // USSDAPIDB.fnProfileCallsToCBS("ReverseWithdrawalRequest", durTimeElapsed, "CBS");
                        /*instEnd = null;
                        instStart = null;
                        durTimeElapsed = null;*/
                        /*API CALL TO CBS END*/
                        rVal = APIConstants.TransactionReturnVal.ERROR;
                    } else {
                        sendSMS(strMobileNumberToReceiveSMS, strSMS, MSGConstants.MSGMode.EXPRESS, 210, "ATM_WITHDRAWAL_CODE", theUSSDRequest);
                        rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    }
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "ACCOUNT_NOT_ACTIVE": {
                    rVal = APIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
                    break;
                }
                case "BLOCKED": {
                    rVal = APIConstants.TransactionReturnVal.BLOCKED;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal airtimePurchase(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumberFrom = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strDate = MBankingDB.getDBDateTime().trim();
            String strGUID = fnModifyUSSDSessionID(theUSSDRequest);

            String strTraceID = theUSSDRequest.getUSSDTraceID();

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            int intPriority = 200;

            String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumberFrom);
            String strTransaction = "Airtime Request";
            String strTransactionDescription = "Airtime Purchase by " + strMobileNumberFrom;
            strTransactionDescription = PESAAPI.shortenName(strTransactionDescription);
            XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, ke.skyworld.mbanking.pesaapi.PESAAPIConstants.PESA_PARAM_TYPE.AIRTIME);

            long getProductID = Long.parseLong(pesaParam.getProductId());
            String strCategory = "AIRTIME_PURCHASE";

            String strReceiverIdentifier = pesaParam.getSenderIdentifier();
            String strReceiverAccount = pesaParam.getSenderAccount();
            String strReceiverName = pesaParam.getSenderName();

            PESA pesa = new PESA();

            pesa.setOriginatorID(strGUID);
            pesa.setProductID(getProductID);
            pesa.setPESAType(thePESAType);
            pesa.setPESAAction(PESAConstants.PESAAction.B2C);
            pesa.setCommand("E-TOPUP");
            pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
            // pesa.setChargeProposed(null);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumberFrom);
            pesa.setInitiatorAccount(strMobileNumberFrom);
            // pesa.setInitiatorName(""); - Set after getting name from CBS
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

            pesa.setSourceType("ACCOUNT_NO");
            pesa.setSourceIdentifier(strAccountFrom);
            pesa.setSourceAccount(strAccountFrom);
            // pesa.setSourceName(""); - Set after getting name from CBS
            pesa.setSourceReference(strUSSDSessionID);
            pesa.setSourceApplication("CBS");
            pesa.setSourceOtherDetails("<DATA/>");

            pesa.setSenderType("SHORT_CODE");
            pesa.setSenderIdentifier(strReceiverIdentifier);
            pesa.setSenderAccount(strReceiverAccount);
            pesa.setSenderName(strReceiverName);
            pesa.setSenderOtherDetails("<DATA/>");

            pesa.setReceiverType("MSISDN");
            pesa.setReceiverIdentifier(strMobileNumberFrom);
            pesa.setReceiverAccount(strMobileNumberFrom);
            // pesa.setReceiverName(""); - Set after getting name from CBS
            pesa.setReceiverOtherDetails("<DATA/>");

            pesa.setBeneficiaryType("MSISDN");
            pesa.setBeneficiaryIdentifier(strMobileNumberFrom);
            pesa.setBeneficiaryAccount(strMobileNumberFrom);
            // pesa.setBeneficiaryName(""); - Set after getting name from CBS
            pesa.setBeneficiaryOtherDetails("<DATA/>");

            pesa.setBatchReference(strGUID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
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
            boolean isOtherNumber = false;


            String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strGUID, strUSSDSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumberFrom, strPIN, "USSD", strUSSDSessionID, "MBANKING", strMobileNumberFrom, strMobileNumberFrom, "Safaricom Airtime", isOtherNumber, "");

            System.out.println("Withdrawal Status: " + strWithdrawalStatus);
            String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

            switch (arrWithdrawalStatus[0]) {
                case "SUCCESS": {
                    String strMemberName = arrWithdrawalStatus[1].trim();
                    pesa.setSourceName(strMemberName);
                    pesa.setReceiverName(strMemberName);
                    pesa.setBeneficiaryName(strMemberName);
                    pesa.setInitiatorName(strMemberName);

                    if (PESAProcessor.sendPESA(pesa) > 0) {
                        rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    } else {
                        System.out.println("Making API Call To NAV");
                        CBSAPI.reverseWithdrawalRequest(strGUID);
                    }
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "ACCOUNT_NOT_ACTIVE": {
                    rVal = APIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
                    break;
                }
                case "BLOCKED": {
                    rVal = APIConstants.TransactionReturnVal.BLOCKED;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal payBill(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strDate = MBankingDB.getDBDateTime().trim();
            String strGUID = fnModifyUSSDSessionID(theUSSDRequest);

            String strTraceID = theUSSDRequest.getUSSDTraceID();

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            int intPriority = 200;

            String strBillAccountNumberLinkedHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());

            HashMap<String, String> hmAccount = Utils.toHashMap(strBillAccountNumberLinkedHashMap);
            String strBillerAccountToName = hmAccount.get("ACCOUNT_NAME");
            String strBillerAccountTo = hmAccount.get("ACCOUNT_IDENTIFIER");

            String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            String strReceiverDetails = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());
            String strReceiverBillerShortcode = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("UTILITY_CODE");
            String strBillerName = "";

            for (APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts) {
                String strProviderIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
                if (strProviderIdentifier.equals(strReceiverDetails)) {
                    strBillerName = serviceProviderAccount.getProviderAccountLongTag();
                    break;
                }
            }

            String strTransactionType = "Utility Request";
            String strTransactionDescription = "B2B Bill Payment to " + strBillerName;
            strTransactionDescription = PESAAPI.shortenName(strTransactionDescription);
            XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, ke.skyworld.mbanking.pesaapi.PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

            long getProductID = Long.parseLong(pesaParam.getProductId());
            String strCategory = "BILL_PAYMENT";

            String strSenderIdentifier = pesaParam.getSenderIdentifier();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strSenderName = pesaParam.getSenderName();

            PESA pesa = new PESA();

            pesa.setOriginatorID(strGUID);
            pesa.setProductID(getProductID);
            pesa.setPESAType(thePESAType);
            pesa.setPESAAction(PESAConstants.PESAAction.B2B);
            pesa.setCommand("BusinessPayBill");
            pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
            // pesa.setChargeProposed(null);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumber);
            pesa.setInitiatorAccount(strMobileNumber);
            // pesa.setInitiatorName(""); - Set after getting name from CBS
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

            pesa.setSourceType("ACCOUNT_NO");
            pesa.setSourceIdentifier(strAccountFrom);
            pesa.setSourceAccount(strAccountFrom);
            // pesa.setSourceName(""); - Set after getting name from CBS
            pesa.setSourceReference(strUSSDSessionID);
            pesa.setSourceApplication("CBS");
            pesa.setSourceOtherDetails("<DATA/>");

            pesa.setSenderType("SHORT_CODE");
            pesa.setSenderIdentifier(strSenderIdentifier);
            pesa.setSenderAccount(strSenderAccount);
            pesa.setSenderName(strSenderName);
            pesa.setSenderOtherDetails("<DATA/>");


            pesa.setReceiverType("SHORT_CODE");
            pesa.setReceiverIdentifier(strReceiverBillerShortcode);
            pesa.setReceiverAccount(strBillerAccountTo);
            pesa.setReceiverName(strBillerName);
            pesa.setReceiverOtherDetails("<DATA/>");

            pesa.setBeneficiaryType("MSISDN");
            pesa.setBeneficiaryIdentifier(strMobileNumber);
            pesa.setBeneficiaryAccount(strMobileNumber);
            pesa.setBeneficiaryName(strBillerAccountToName);
            pesa.setBeneficiaryOtherDetails("<DATA/>");

            pesa.setBatchReference(strGUID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
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
            boolean isOtherNumber = false;


            String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strGUID, strUSSDSessionID, xmlGregorianCalendar, strTransactionType, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumber, strPIN, "USSD", strUSSDSessionID, "MBANKING", strBillerAccountTo, strBillerAccountTo, strBillerName, isOtherNumber, "");

            System.out.println("Withdrawal Status: " + strWithdrawalStatus);
            String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

            switch (arrWithdrawalStatus[0]) {
                case "SUCCESS": {
                    String strMemberName = arrWithdrawalStatus[1].trim();
                    pesa.setSourceName(strMemberName);
                    pesa.setBeneficiaryName(strMemberName);
                    pesa.setInitiatorName(strMemberName);

                    if (PESAProcessor.sendPESA(pesa) > 0) {
                        rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    } else {
                        CBSAPI.reverseWithdrawalRequest(strGUID);
                    }
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "ACCOUNT_NOT_ACTIVE": {
                    rVal = APIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
                    break;
                }
                case "BLOCKED": {
                    rVal = APIConstants.TransactionReturnVal.BLOCKED;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal bankTransferViaB2B(USSDRequest theUSSDRequest, PESAConstants.PESAType thePESAType) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strDate = MBankingDB.getDBDateTime().trim();
            String strGUID = fnModifyUSSDSessionID(theUSSDRequest);

            String strTraceID = theUSSDRequest.getUSSDTraceID();

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            int intPriority = 200;

            String strAccountFrom = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());


            String strReceiverBankShortcode = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

            String strToBankAccountNoLinkedHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

            HashMap<String, String> hmAccount = Utils.toHashMap(strToBankAccountNoLinkedHashMap);
            String strBankAccountToName = hmAccount.get("ACCOUNT_NAME");
            String strBankAccountTo = hmAccount.get("ACCOUNT_IDENTIFIER");


            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            String strBankName = "";

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("BANK_SHORT_CODE");

            for (APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts) {
                String strProviderIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
                if (strProviderIdentifier.equals(strReceiverBankShortcode)) {
                    strBankName = serviceProviderAccount.getProviderAccountLongTag();
                }
            }

            String strTransaction = "Bank Transfer Request";
            String strTransactionDescription = "BT|B2B|" + strBankName + "|" + strBankAccountTo + "|" + strBankAccountToName + "|" + strMobileNumber;
            strTransactionDescription = PESAAPI.shortenName(strTransactionDescription);
            XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

            PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, ke.skyworld.mbanking.pesaapi.PESAAPIConstants.PESA_PARAM_TYPE.MPESA_B2B);

            long getProductID = Long.parseLong(pesaParam.getProductId());
            String strCategory = "BANK_TRANSFER";

            String strSenderIdentifier = pesaParam.getSenderIdentifier();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strSenderName = pesaParam.getSenderName();

            PESA pesa = new PESA();

            pesa.setOriginatorID(strGUID);
            pesa.setProductID(getProductID);
            pesa.setPESAType(thePESAType);
            pesa.setPESAAction(PESAConstants.PESAAction.B2B);
            pesa.setCommand("BusinessPayBill");
            pesa.setSensitivity(PESAConstants.Sensitivity.NORMAL);
            // pesa.setChargeProposed(null);

            pesa.setInitiatorType("MSISDN");
            pesa.setInitiatorIdentifier(strMobileNumber);
            pesa.setInitiatorAccount(strMobileNumber);
            // pesa.setInitiatorName(""); - Set after getting name from CBS
            pesa.setInitiatorReference(strTraceID);
            pesa.setInitiatorApplication("USSD");
            pesa.setInitiatorOtherDetails("<DATA/>");

            pesa.setSourceType("ACCOUNT_NO");
            pesa.setSourceIdentifier(strAccountFrom);
            pesa.setSourceAccount(strAccountFrom);
            // pesa.setSourceName(""); - Set after getting name from CBS
            pesa.setSourceReference(strUSSDSessionID);
            pesa.setSourceApplication("CBS");
            pesa.setSourceOtherDetails("<DATA/>");

            pesa.setSenderType("SHORT_CODE");
            pesa.setSenderIdentifier(strSenderIdentifier);
            pesa.setSenderAccount(strSenderAccount);
            pesa.setSenderName(strSenderName);
            pesa.setSenderOtherDetails("<DATA/>");

            pesa.setReceiverType("SHORT_CODE");
            pesa.setReceiverIdentifier(strReceiverBankShortcode);
            pesa.setReceiverAccount(strBankAccountTo);
            pesa.setReceiverName(strBankName);
            pesa.setReceiverOtherDetails("<DATA/>");

            pesa.setBeneficiaryType("MSISDN");
            pesa.setBeneficiaryIdentifier(strMobileNumber);
            pesa.setBeneficiaryAccount(strMobileNumber);
            pesa.setBeneficiaryName(strBankAccountToName);
            pesa.setBeneficiaryOtherDetails("<DATA/>");

            pesa.setBatchReference(strGUID);
            pesa.setCorrelationReference(strTraceID);
            pesa.setCorrelationApplication("USSD");
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
            boolean isOtherNumber = false;

            String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(strGUID, strUSSDSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strAmount)), strMobileNumber, strPIN, "USSD", strUSSDSessionID, "MBANKING", strBankAccountTo, strBankAccountToName, strBankName, isOtherNumber, "");            // instEnd = Instant.now();

            System.out.println("NAV Status: " + strWithdrawalStatus);
            String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

            switch (arrWithdrawalStatus[0]) {
                case "SUCCESS": {
                    String strMemberName = arrWithdrawalStatus[1].trim();
                    pesa.setSourceName(strMemberName);
                    pesa.setInitiatorName(strMemberName);

                    if (PESAProcessor.sendPESA(pesa) > 0) {
                        rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    } else {
                        CBSAPI.reverseWithdrawalRequest(strGUID);
                    }
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "INVALID_ACCOUNT": {
                    rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "ACCOUNT_NOT_ACTIVE": {
                    rVal = APIConstants.TransactionReturnVal.ACCOUNT_NOT_ACTIVE;
                    break;
                }
                case "BLOCKED": {
                    rVal = APIConstants.TransactionReturnVal.BLOCKED;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal checkLoanQualification(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_ACCOUNT.name());

            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE.name());

            String strCheckLoanQualificationStatus = CBSAPI.getLoanLimit(strMobileNumber, strLoanType, strLoanAccount);

            if (strCheckLoanQualificationStatus.equals("SUCCESS")) {
                rVal = APIConstants.TransactionReturnVal.SUCCESS;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public HashMap<String, HashMap<String, String>> getATMCards(USSDRequest theUSSDRequest) {
        HashMap<String, HashMap<String, String>> accounts = new HashMap<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            accounts = new HashMap<>();

            String strAccountsXML = CBSAPI.getATMCard(strMobileNumber);

            if (!strAccountsXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountsXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();


                NodeList nlAccounts = ((NodeList) configXPath.evaluate("/ATM_CARDS", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();
                accounts = new HashMap<>();

                for (int i = 0; i < nlAccounts.getLength(); i++) {
                    NodeList nlAccount = ((NodeList) configXPath.evaluate("CARD", nlAccounts, XPathConstants.NODESET)).item(i).getChildNodes();

                    HashMap<String, String> account = new HashMap<>();
                    account.put("ID", nlAccount.item(0).getTextContent());
                    account.put("NAME", nlAccount.item(1).getTextContent());
                    accounts.put(nlAccount.item(0).getTextContent(), account);
                }
                accounts = (accounts.size() > 0) ? accounts : null;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return accounts;
    }

    public APIConstants.TransactionReturnVal manageATMCard(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_ACTION.name());

            String strMemberVirtualRegistrationStatus = CBSAPI.stopATM(strMobileNumber, strPIN);
            System.out.println("stopATM: " + strMemberVirtualRegistrationStatus);

            switch (strMemberVirtualRegistrationStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return rVal;
    }

    public void generateDividendsPayslipcCurrent(USSDRequest theUSSDRequest) {
        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            String strSessionId = fnModifyUSSDSessionID(theUSSDRequest);
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT.name());
            String strYear = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_YEAR_INPUT.name());


            String strDividendSlip = CBSAPI.getDividendPayslipCurrent(strMobileNumber, strYear);


            if (strDividendSlip == null || strDividendSlip.isBlank()) {

                String strMessage = "Dear Member,\nWe could not find your dividends for the selected year";

                int intMSGSent = fnSendSMS(strMobileNumber, strMessage, "YES", MSGConstants.MSGMode.SAF, 210, "DIVIDEND_PAYSLIP", "USSD", "MBANKING_SERVER",
                        strSessionId, theUSSDRequest.getUSSDTraceID());

            } else {
                try {

                    String strFilePath = "files" + File.separator + "pdf" + File.separator + strYear + " Dividend Slip - " + fnModifyUSSDSessionID(theUSSDRequest) + ".pdf";

                    Path filePath = Paths.get(strFilePath);
                    Files.createDirectories(filePath.getParent()); // Creates the directory structure if it doesn’t exist

                    byte[] data = Base64.decodeBase64(strDividendSlip);
                    try (OutputStream stream = Files.newOutputStream(Paths.get(strFilePath))) {
                        stream.write(data);
                    }

                    String strEmailSubject = "Dividend Slip for Year " + strYear;
                    String strEmailMessage = "Dear Member,\n" +
                                             "Kindly find your dividend slip for year " + strYear + " attached.\n" +
                                             "\n" +
                                             "Kind Regards, D SACCO Society LTD.";

                    MBankingAPI.processSendEmail(strEmailAddress, strEmailSubject, strEmailMessage, strFilePath);

                    File flFile = new File(strFilePath);
                    flFile.deleteOnExit();
                } catch (Exception e) {

                }
            }

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
    }

    public void generateDividendsPayslip(USSDRequest theUSSDRequest) {
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT.name());

            String strMemberVirtualRegistrationStatus = getDividendPayslip(strMobileNumber, strEmailAddress);
            System.out.println("generateDividendsPayslip: " + strMemberVirtualRegistrationStatus);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
    }

    public APIConstants.TransactionReturnVal loanApplication(USSDRequest theUSSDRequest, boolean theMinimumDetailsLoan, String theLoanID, String theAmount, String thePIN, String theLoanPeriod, String theLoanPurpose, String theLoanPassword, String theLoanBranch, String theAccount) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoanInstallments = "";

            String strLoanPurpose = "";
            if (theLoanPurpose != null) {
                HashMap<String, String> hmLoanPurpose = Utils.toHashMap(theLoanPurpose);
                strLoanPurpose = hmLoanPurpose.get("LOAN_PURPOSE_ID");
            }

            String strLoanBranch = "";
            if (theLoanPeriod != null) {
                HashMap<String, String> hmLoanBranch = Utils.toHashMap(theLoanBranch);
                strLoanBranch = hmLoanBranch.get("B_ID");
            }

            strLoanInstallments = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS.name());

            if (strLoanInstallments == null) {
                strLoanInstallments = "0";
            }

            String strPIN = APIUtils.hashPIN(thePIN, String.valueOf(theUSSDRequest.getUSSDMobileNo()));

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            if (strLoanBranch == null) {
                strLoanBranch = "";
            }

            if (strLoanPurpose == null) {
                strLoanPurpose = "";
            }

            if (theLoanPassword == null) {
                theLoanPassword = "";
            }

            int intLoanPeriod = 0;
            if (theLoanPeriod != null) {
                intLoanPeriod = Integer.parseInt(theLoanPeriod);
            }

            String strLoanApplicationResponse = CBSAPI.applyLoan_BRANCH(fnModifyUSSDSessionID(theUSSDRequest), strUSSDSessionID, strMobileNumber, theLoanID, BigDecimal.valueOf(Double.parseDouble(theAmount)), strPIN, intLoanPeriod, strLoanPurpose, theLoanPassword, strLoanBranch, theAccount);

            String strLoanApplicationStatus = strLoanApplicationResponse;
            if (strLoanApplicationResponse.contains(":::")) {
                strLoanApplicationStatus = strLoanApplicationResponse.split(":::")[0];
            }

            switch (strLoanApplicationStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "LOAN_APPLICATION_EXISTS": {
                    rVal = APIConstants.TransactionReturnVal.LOAN_APPLICATION_EXISTS;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return rVal;
    }

    public APIConstants.TransactionReturnVal agentCashWithdrawal(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO.name());
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT.name());
            BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(strAmount));

            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

            /*API CALL TO CBS START*/
            /*Instant instStart;
            Instant instEnd;
            instStart = Instant.now();
            System.out.println("Making API Call To NAV");*/
            String strLoanApplicationStatus = "";// Navision.getPort().agentWithdrawal(strUSSDSessionID, strUSSDSessionID, xmlGregorianCalendar, strMobileNumber, strAgentNumber, strAgentNumber, bdAmount, strPIN);
            // instEnd = Instant.now();
            // Duration durTimeElapsed = Duration.between(instStart, instEnd);
            // USSDAPIDB.fnProfileCallsToCBS("AgentWithdrawal", durTimeElapsed, "CBS");
            /*instEnd = null;
            instStart = null;
            durTimeElapsed = null;*/
            /*API CALL TO CBS END*/

            switch (strLoanApplicationStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                case "LOAN_APPLICATION_EXISTS": {
                    rVal = APIConstants.TransactionReturnVal.LOAN_APPLICATION_EXISTS;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return rVal;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getLoans(USSDRequest theUSSDRequest, String theLoanGroup) {
        LinkedHashMap<String, LinkedHashMap<String, String>> loans = new LinkedHashMap<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansXML = CBSAPI.getMemberLoanList(strMobileNumber);

            NodeList nlLoans = APIUtils.getXMLNodeListFromPath("/Loans/Product", strLoansXML.trim());

            loans = new LinkedHashMap<>();

            if (nlLoans != null) {
                for (int i = 1; i <= nlLoans.getLength(); i++) {
                    String strLoanId = MBankingXMLFactory.getXPathValueFromXMLString("/Loans/Product[" + i + "]/LoanNo", strLoansXML.trim());
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Loans/Product[" + i + "]/Type", strLoansXML.trim());

                    LinkedHashMap<String, String> loan = new LinkedHashMap<>();
                    loan.put("LOAN_ID", strLoanId);
                    loan.put("LOAN_NAME", strLoanName);
                    loans.put("LOAN" + strLoanId, loan);
                }
            }

            loans = (loans.size() > 0) ? loans : null;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public TransactionWrapper<FlexicoreHashMap> getLoans_V2(USSDRequest theUSSDRequest) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            HashMap<String, String> userIdentifierDetails = APIUtils.getUserIdentifierDetails(strMobileNumber);
            String strIdentifierType = userIdentifierDetails.get("identifier_type");
            String strIdentifier = userIdentifierDetails.get("identifier");
            return CBSAPI.getCustomerLoanAccounts(strMobileNumber, strIdentifierType, strIdentifier);


        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }

        return resultWrapper;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getLoansWithGuarantors(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, LinkedHashMap<String, String>> loans = new LinkedHashMap<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansXML = CBSAPI.getLoansWithGuarantors(strMobileNumber);

            NodeList nlLoans = APIUtils.getXMLNodeListFromPath("/Loans/Loan", strLoansXML.trim());

            loans = new LinkedHashMap<>();

            if (nlLoans != null) {
                for (int i = 1; i <= nlLoans.getLength(); i++) {
                    String strLoanId = MBankingXMLFactory.getXPathValueFromXMLString("/Loans/Loan[" + i + "]/EntryCode", strLoansXML.trim());
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Loans/Loan[" + i + "]/Name", strLoansXML.trim());

                    LinkedHashMap<String, String> loan = new LinkedHashMap<>();
                    loan.put("LOAN_ID", strLoanId);
                    loan.put("LOAN_NAME", strLoanName);
                    loans.put(strLoanId, loan);
                }
            }

            loans = (loans.size() > 0) ? loans : null;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public String getLoanAwaitingGuarantorship(USSDRequest theUSSDRequest) {
        String loans = "";
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());

            HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
            strLoanType = hmLoanType.get("LOAN_ID");

            loans = CBSAPI.getLoanWithGuarantorDetails(Integer.parseInt(strLoanType));

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getLoanPurposes(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, LinkedHashMap<String, String>> loans = new LinkedHashMap<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansXML = CBSAPI.getLoanPurpose();

            NodeList nlLoans = APIUtils.getXMLNodeListFromPath("/LoanApplicationPurposes/Purpose", strLoansXML);

            loans = new LinkedHashMap<>();

            if (nlLoans != null) {
                for (int i = 1; i <= nlLoans.getLength(); i++) {
                    LinkedHashMap<String, String> loan = new LinkedHashMap<>();
                    loan.put("LOAN_PURPOSE_ID", MBankingXMLFactory.getXPathValueFromXMLString("LoanApplicationPurposes/Purpose[" + i + "]/@Id", strLoansXML));
                    loan.put("LOAN_PURPOSE_NAME", MBankingXMLFactory.getXPathValueFromXMLString("LoanApplicationPurposes/Purpose[" + i + "]/@Title", strLoansXML));
                    loans.put(MBankingXMLFactory.getXPathValueFromXMLString("LoanApplicationPurposes/Purpose[" + i + "]/@Id", strLoansXML), loan);
                }
            }

            loans = (loans.size() > 0) ? loans : null;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getLoanBranches(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, LinkedHashMap<String, String>> loans = new LinkedHashMap<>();
        try {

            String strBranchesXML = CBSAPI.getBranches();

            NodeList nlLoans = APIUtils.getXMLNodeListFromPath("/Branches/Branch", strBranchesXML);

            loans = new LinkedHashMap<>();

            if (nlLoans != null) {
                for (int i = 1; i <= nlLoans.getLength(); i++) {
                    LinkedHashMap<String, String> loan = new LinkedHashMap<>();
                    loan.put("B_ID", MBankingXMLFactory.getXPathValueFromXMLString("Branches/Branch[" + i + "]/Code", strBranchesXML));
                    loan.put("B_NAME", MBankingXMLFactory.getXPathValueFromXMLString("Branches/Branch[" + i + "]/Name", strBranchesXML));
                    loans.put(MBankingXMLFactory.getXPathValueFromXMLString("Branches/Branch[" + i + "]/Code", strBranchesXML), loan);
                }
            }

            loans = (loans.size() > 0) ? loans : null;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }


    public LinkedHashMap<String, LinkedHashMap<String, String>> getLoaneesPendingGuarantorship(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, LinkedHashMap<String, String>> loans = new LinkedHashMap<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoaneesXML = CBSAPI.getLoaneesAwaitingGuarantorship(strMobileNumber, "PENDING");

            NodeList nlLoanees = APIUtils.getXMLNodeListFromPath("/Loanees/Loanee", strLoaneesXML);

            loans = new LinkedHashMap<>();

            if (nlLoanees != null) {
                for (int i = 1; i <= nlLoanees.getLength(); i++) {
                    LinkedHashMap<String, String> loan = new LinkedHashMap<>();
                    loan.put("LOANEE_ID", MBankingXMLFactory.getXPathValueFromXMLString("Loanees/Loanee[" + i + "]/@Id", strLoaneesXML));
                    loan.put("LOANEE_NAME", MBankingXMLFactory.getXPathValueFromXMLString("Loanees/Loanee[" + i + "]/@Name", strLoaneesXML));
                    loans.put("LOANEE" + MBankingXMLFactory.getXPathValueFromXMLString("Loanees/Loanee[" + i + "]/@Id", strLoaneesXML), loan);
                }
            }

            loans = (loans.size() > 0) ? loans : null;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public LinkedHashMap<String, String> getLoansPendingGuarantorship(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, String> loan = new LinkedHashMap<>();
        try {
            String strLoanee = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_LOANEE.name());

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            HashMap<String, String> hmLonee = Utils.toHashMap(strLoanee);
            strLoanee = hmLonee.get("LOANEE_ID");

            String strLoanXML = CBSAPI.getDetailsForSpecificLoanGuaranteed(strMobileNumber, Integer.parseInt(strLoanee));

            String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/LoanName", strLoanXML);
            String strMemberName = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/MemberName", strLoanXML);
            String strNumber = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Number", strLoanXML);
            String strAmount = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Amount", strLoanXML);
            String strMobile = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Mobile", strLoanXML);
            String strDate = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Date", strLoanXML);

            loan.put("MEMBER_NAME", strMemberName);
            loan.put("LOAN_NAME", strLoanName);
            loan.put("LOAN_NUMBER", strNumber);
            loan.put("LOAN_AMOUNT", strAmount);
            loan.put("LOAN_MOBILE_NUMBER", strMobile);
            loan.put("LOAN_DATE", strDate);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loan;
    }

    public LinkedList<String> getLoanTypes(USSDRequest theUSSDRequest, String theLoanCategory, String theLoanAccount) {

        LinkedList<String> loans = new LinkedList<String>();
        try {
            String strLoanCategory = "ALL";
            APIConstants.AccountType atAccountType = APIConstants.AccountType.ALL_LOANS;
            if (theLoanCategory.equals("FOSA")) {
                strLoanCategory = "FOSA";
                atAccountType = APIConstants.AccountType.FOSA_LOANS;
            } else if (theLoanCategory.equals("BOSA")) {
                strLoanCategory = "BOSA";
                atAccountType = APIConstants.AccountType.BOSA_LOANS;
            }

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansXML = "";// USSDAPIDB.getUserAccountDetails(theUSSDRequest, atAccountType);

            strLoansXML = CBSAPI.getMobileLoanList(strMobileNumber, strLoanCategory, theLoanAccount);
//
//            System.out.println("--------------------------------------------------------------------------------------------------------");
//            System.out.println("StrLoansXML :: " + strLoansXML);
//            System.out.println("--------------------------------------------------------------------------------------------------------");

            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            NodeList nlLoans = ((NodeList) configXPath.evaluate("/Loans", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            for (int i = 0; i < nlLoans.getLength(); i++) {
                Node ndLoan = ((Node) configXPath.evaluate("/Loans/Product[" + (i + 1) + "]", xmlDocument, XPathConstants.NODE));
                loans.add(APIUtils.nodeToString(ndLoan));
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public LinkedList<String> getErroneousTransactions(USSDRequest theUSSDRequest) {

        LinkedList<String> loans = new LinkedList<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansXML = "";// USSDAPIDB.getUserAccountDetails(theUSSDRequest, atAccountType);

            strLoansXML = CBSAPI.getErroneousTransactions(strMobileNumber);

            if (!strLoansXML.equalsIgnoreCase("PHONE_NOT_FOUND")) {

                InputSource source = new InputSource(new StringReader(strLoansXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList nlLoans = ((NodeList) configXPath.evaluate("/ErroneousTransactions", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                for (int i = 0; i < nlLoans.getLength(); i++) {

                    String expression = "/ErroneousTransactions/ErroneousTransaction[" + (i + 1) + "]";

                    Node ndLoan = ((Node) configXPath.evaluate(expression, xmlDocument, XPathConstants.NODE));
                    loans.add(APIUtils.nodeToString(ndLoan));
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public LinkedList<String> UpdateErroneousTransactions(String theId, String theAccount) {

        LinkedList<String> loans = new LinkedList<String>();
        try {

            String strLoansXML = "";// USSDAPIDB.getUserAccountDetails(theUSSDRequest, atAccountType);

            strLoansXML = CBSAPI.UpdateErroneousTransactions(theId, theAccount);

            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            NodeList nlLoans = ((NodeList) configXPath.evaluate("/Response", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

            for (int i = 0; i < nlLoans.getLength(); i++) {
                Node ndLoan = ((Node) configXPath.evaluate("/Response", xmlDocument, XPathConstants.NODE));
                loans.add(APIUtils.nodeToString(ndLoan));
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getLoansGuaranteed(USSDRequest theUSSDRequest) {
        LinkedHashMap<String, LinkedHashMap<String, String>> loans = new LinkedHashMap<>();
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansXML = CBSAPI.getLoaneesAwaitingGuarantorship(strMobileNumber, "ACCEPTED");

            NodeList nlLoans = APIUtils.getXMLNodeListFromPath("/Loanees/Loanee", strLoansXML.trim());

            loans = new LinkedHashMap<>();

            if (nlLoans != null) {
                for (int i = 1; i <= nlLoans.getLength(); i++) {
                    String strLoanId = MBankingXMLFactory.getXPathValueFromXMLString("/Loanees/Loanee[" + i + "]/@Id", strLoansXML.trim());
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Loanees/Loanee[" + i + "]/@Name", strLoansXML.trim());

                    LinkedHashMap<String, String> loan = new LinkedHashMap<>();
                    loan.put("LOANEE_ID", strLoanId);
                    loan.put("LOANEE_NAME", strLoanName);
                    loans.put(strLoanId, loan);
                }
            }

            loans = (loans.size() > 0) ? loans : null;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return loans;
    }

    public String getLoansGuaranteedForViewOnly(USSDRequest theUSSDRequest) {
        try {
            String strLoanee = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_LOANEE.name());
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoansGuaranteed = CBSAPI.getLoansGuaranteed(strLoanee, strMobileNumber);

            return strLoansGuaranteed;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public APIConstants.TransactionReturnVal loanRepayment(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());

            String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_ACCOUNT.name());
            HashMap<String, String> hmLoan = Utils.toHashMap(strLoanType);
            String strLoanID = hmLoan.get("LOAN_ID");
            strLoanID = strLoanID.replace("LOAN", "");

            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            String strLoanRepayment = CBSAPI.accountTransfer_SOURCEACCOUNT(strUSSDSessionID + "T", strUSSDSessionID + "T", strMobileNumber, strLoanID, "", BigDecimal.valueOf(Double.parseDouble(strAmount)), strPIN, true, false, strLoanAccount);

            switch (strLoanRepayment) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "INCORRECT_PIN": {
                    rVal = APIConstants.TransactionReturnVal.INCORRECT_PIN;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal fundsTransfer(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);
            String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
            String strToAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);

            String strDestination = "ACCOUNT";

            if (strToOption != null) {
                if (strToOption.equals("ID Number")) {
                    strDestination = "ID";
                } else if (strToOption.equals("Mobile No")) {
                    strDestination = "Mobile";
                } else {
                    strDestination = "ACCOUNT";
                }
            }

            String strFundsTransferStatus = CBSAPI.accountTransfer_SOURCEACCOUNT(fnModifyUSSDSessionID(theUSSDRequest) + "T", strUSSDSessionID + "T", strMobileNumber, strToAccountNo, strDestination, BigDecimal.valueOf(Double.parseDouble(strAmount)), strPIN, false, strToOption == null, strFromAccount);

            switch (strFundsTransferStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                    break;
                }
                case "INSUFFICIENT_BAL": {
                    rVal = APIConstants.TransactionReturnVal.INSUFFICIENT_BAL;
                    break;
                }
                case "ACC_NOT_FOUND": {
                    rVal = APIConstants.TransactionReturnVal.INVALID_ACCOUNT;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public void sendSMS(String theMobileNo, String theMSG, MSGConstants.MSGMode theMode, int thePriority, String theCategory, USSDRequest theUSSDRequest) {
        try {
            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);
            String strTraceID = theUSSDRequest.getUSSDTraceID();
            fnSendSMS(theMobileNo, theMSG, "YES", theMode, thePriority, theCategory, "USSD", "MBANKING_SERVER", strUSSDSessionID, strTraceID);
        } catch (Exception e) {
            System.err.println("USSDAPI.sendSMS() ERROR : " + e.getMessage());
        }
    }

    public String generateAndSendOTP(String theMobileNo, String theSessionID, USSDRequest theUSSDRequest) {
        String rVal = "";
        try {
            String strMAPPConfigXML = USSDLocalParameters.getClientXMLParameters();

            InputSource source = new InputSource(new StringReader(strMAPPConfigXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            String strLength = configXPath.evaluate("/OTHER_DETAILS/CUSTOM_PARAMETERS/MAPP_ACTIVATION_CODE/@LENGTH", xmlDocument, XPathConstants.STRING).toString();
            String strTTL = configXPath.evaluate("/OTHER_DETAILS/CUSTOM_PARAMETERS/MAPP_ACTIVATION_CODE/@TTL", xmlDocument, XPathConstants.STRING).toString();

            int intLength = Integer.parseInt(strLength);
            long lnTTL = Integer.parseInt(strTTL);

            long lnTTLMinutes = lnTTL / 60;

            String strMobileAppStartKey = Utils.generateRandomString(intLength);

            MAPPAPIDB.fnInsertOTPData(theMobileNo, strMobileAppStartKey, Integer.parseInt(strTTL));

            SimpleDateFormat sdSimpleDateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            Timestamp tsCurrentTimestamp = new Timestamp(System.currentTimeMillis());
            Timestamp tsCurrentTimestampPlusTime = new Timestamp(System.currentTimeMillis() + (lnTTL * 1000));

            String strTimeGenerated = sdSimpleDateFormat.format(tsCurrentTimestamp);
            String strExpiryDate = sdSimpleDateFormat.format(tsCurrentTimestampPlusTime);


            String strMSG = "Dear Member,\n" + strMobileAppStartKey + " is your mobile app activation code generated at " + strTimeGenerated + ". This activation code is valid up to " + strExpiryDate + ".\n";

            String strCategory = "MAPP_ACTIVATION";

            sendSMS(theMobileNo, strMSG, MSGConstants.MSGMode.EXPRESS, 200, strCategory, theUSSDRequest);
            rVal = "Your " + intLength + " digit Mobile App Activation Code has been sent to you via SMS. Complete your Mobile App Activation within " + lnTTLMinutes + " minutes.";
        } catch (Exception e) {
            System.err.println("USSDAPI.generateAndSendOTP() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public USSDAmountLimitParam getParam(APIConstants.USSD_PARAM_TYPE theUSSDParamType) {
        USSDAmountLimitParam rVal = new USSDAmountLimitParam();
        try {
            String strUSSDParamType = "OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/AMOUNT_LIMITS";

            switch (theUSSDParamType) {
                case CASH_WITHDRAWAL: {
                    strUSSDParamType += "/CASH_WITHDRAWAL";
                    break;
                }
                case CASH_WITHDRAWAL_TO_OTHER: {
                    strUSSDParamType += "/CASH_WITHDRAWAL_TO_OTHER";
                    break;
                }
                case AGENT_CASH_WITHDRAWAL: {
                    strUSSDParamType += "/CASH_WITHDRAWAL";
                    break;
                }
                case AIRTIME_PURCHASE: {
                    strUSSDParamType += "/AIRTIME_PURCHASE";
                    break;
                }
                case PAY_BILL: {
                    strUSSDParamType += "/PAY_BILL";
                    break;
                }
                case EXTERNAL_FUNDS_TRANSFER: {
                    strUSSDParamType += "/EXTERNAL_FUNDS_TRANSFER";
                    break;
                }
                case INTERNAL_FUNDS_TRANSFER: {
                    strUSSDParamType += "/INTERNAL_FUNDS_TRANSFER";
                    break;
                }
                case DEPOSIT: {
                    strUSSDParamType += "/DEPOSIT";
                    break;
                }
                case PAY_LOAN: {
                    strUSSDParamType += "/PAY_LOAN";
                    break;
                }
            }

            String strMinimum = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.USSD, strUSSDParamType + "/MIN_AMOUNT");
            String strMaximum = MBankingAPI.getValueFromLocalParams(MBankingConstants.ApplicationType.USSD, strUSSDParamType + "/MAX_AMOUNT");

            rVal.setMinimum(strMinimum);
            rVal.setMaximum(strMaximum);
        } catch (Exception e) {
            System.err.println("USSDAPI.getParam() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static String validateAgentNumber(String strAgentCode) {
        try {
            /*API CALL TO CBS START*/
            /*Instant instStart;
            Instant instEnd;
            instStart = Instant.now();
            System.out.println("Making API Call To NAV");*/
            String strLoansXML = "";// Navision.getPort().getAgentDetails(strAgentCode);
            // instEnd = Instant.now();
            // Duration durTimeElapsed = Duration.between(instStart, instEnd);
            // USSDAPIDB.fnProfileCallsToCBS("GetAgentDetails", durTimeElapsed, "CBS");
            /*instEnd = null;
            instStart = null;
            durTimeElapsed = null;*/
            /*API CALL TO CBS END*/

            if (!strLoansXML.equals("")) {
                return MBankingXMLFactory.getXPathValueFromXMLString("/Agent/Name", strLoansXML.trim());
            } else {
                return "";
            }
        } catch (Exception e) {
            System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public static String getMemberEmailAddress(USSDRequest theUSSDRequest) {
        try {
            String strAccountXML = CBSAPI.getAccountTransferRecipientXML(String.valueOf(theUSSDRequest.getUSSDMobileNo()), "Mobile");

            if (!strAccountXML.equals("")) {
                return MBankingXMLFactory.getXPathValueFromXMLString("/Account/Email", strAccountXML.trim());
            } else {
                return "";
            }
        } catch (Exception e) {
            System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public static String getMemberAccount(String strMobileNumber) {
        try {

            String strReturn = CBSAPI.getAccountTransferRecipientXML(strMobileNumber, "Mobile");

            return strReturn;
        } catch (Exception e) {
            System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public APIConstants.TransactionReturnVal changeEmailAddress(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strNewEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_INPUT.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            String strFundsTransferStatus = CBSAPI.changeEmailAddress(strMobileNumber, strNewEmailAddress, strPIN);

            switch (strFundsTransferStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public String addRemoveLoanGuarantor(USSDRequest theUSSDRequest) {
        String rVal = "ERROR::::An error occurred while processing your request. Kindly contact us for more information";
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strGuarantorLoanNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
            String strGuarantorMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTOR.name());

            if (strGuarantorMobileNumber == null) {
                strGuarantorMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER.name());
            }

            String strGuarantorAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());
            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_PIN.name());

            switch (strGuarantorAction) {
                case "ADD": {
                    strGuarantorMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER.name());
                    strGuarantorMobileNumber = sanitizePhoneNumber(strGuarantorMobileNumber);
                    break;
                }
                case "DISCARD": {
                    strGuarantorMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                    break;
                }
                case "REMOVE":
                default: {
                    strGuarantorMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTOR.name());
                    break;
                }
            }

            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            HashMap<String, String> hmLoanType = Utils.toHashMap(strGuarantorLoanNumber);
            strGuarantorLoanNumber = hmLoanType.get("LOAN_ID");


            System.out.println("Guarantor Mobile Number: " + strGuarantorMobileNumber);

            rVal = CBSAPI.addRemoveMobileLoanGuarantor(Integer.parseInt(strGuarantorLoanNumber), strGuarantorMobileNumber, strGuarantorAction);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal checkBeneficiaries(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strNewEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL.name());

            if (strNewEmailAddress == null) {
                strNewEmailAddress = "";
            }

            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_PIN.name());
            String strMode = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE.name());

            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            String strSessionId = fnModifyUSSDSessionID(theUSSDRequest);

            String strFundsTransferStatus = CBSAPI.checkBeneficiaries(strSessionId, strSessionId, strMobileNumber, strPIN, strMode, strNewEmailAddress);

            switch (strFundsTransferStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public APIConstants.TransactionReturnVal actionLoanGuarantorship(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            LinkedHashMap<String, String> hmLoan = getLoansPendingGuarantorship(theUSSDRequest);
            String strMemberName = hmLoan.get("MEMBER_NAME");
            String strLoanName = hmLoan.get("LOAN_NAME");
            String strNumber = hmLoan.get("LOAN_NUMBER");
            String strAmount = hmLoan.get("LOAN_AMOUNT");
            String strMobile = hmLoan.get("LOAN_MOBILE_NUMBER");
            String strDate = hmLoan.get("LOAN_DATE");
            String strLoanAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION.name());

            String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_PIN.name());
            strPIN = APIUtils.hashPIN(strPIN, strMobileNumber);

            String strFundsTransferStatus = CBSAPI.actionLoanGuarantorship(strMobileNumber, Integer.parseInt(strNumber), strPIN, strLoanAction);

            switch (strFundsTransferStatus) {
                case "SUCCESS": {
                    rVal = APIConstants.TransactionReturnVal.SUCCESS;
                    break;
                }
                case "ERROR": {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                    break;
                }
                default: {
                    rVal = APIConstants.TransactionReturnVal.ERROR;
                }
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return rVal;
    }

    public boolean checkEmployerFunctionalityEnabled(USSDRequest theUSSDRequest, String theTransaction) {
        boolean blCheckEmployerRestriction = false;
        try {
            String strUserPhoneNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            blCheckEmployerRestriction = CBSAPI.employerRestriction(strUserPhoneNumber, theTransaction);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return blCheckEmployerRestriction;
    }

    public boolean checkIfGroupBankingIsEnabled(USSDRequest theUSSDRequest) {
        boolean blRval = false;
        try {
            String strUserPhoneNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            blRval = false;// Navision.getPort().employerRestriction(strUserPhoneNumber, theTransaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return blRval;
    }

    public static String getMemberLoansSetup(USSDRequest theUSSDRequest) {
        try {
            String strUserPhoneNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            String strLoanAccessSetup = CBSAPI.getLoanAccessSetup(strUserPhoneNumber);

            return strLoanAccessSetup;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static HashMap<String, String> getBusinessDetails(String theShortCode) {

        HashMap<String, String> businessDetails = new LinkedHashMap<>();

        try {

            String strBusinessDetails = CBSAPI.getBusinessDetails(theShortCode);

            if (strBusinessDetails == null || strBusinessDetails.isBlank()) {

                return new HashMap<>();

            } else {
                businessDetails.put("ACCOUNT_CODE", theShortCode);
                businessDetails.put("BUSINESS_NAME", MBankingXMLFactory.getXPathValueFromXMLString("/BusinessDetails/BusinessName", strBusinessDetails));

            }
            return businessDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return businessDetails;
    }

    public static HashMap<String, String> getMemberStatus(String theShortCode) {

        HashMap<String, String> memberStatus = new LinkedHashMap<>();

        try {

            String cbsMemberStatus = CBSAPI.getMemberStatus(theShortCode);

            String strName = MBankingXMLFactory.getXPathValueFromXMLString("/MembershipStatus/Name", cbsMemberStatus);
            String strMemberNo = MBankingXMLFactory.getXPathValueFromXMLString("/MembershipStatus/MemberNo", cbsMemberStatus);
            String strServiceNo = MBankingXMLFactory.getXPathValueFromXMLString("/MembershipStatus/ServiceNo", cbsMemberStatus);
            String strBalance = MBankingXMLFactory.getXPathValueFromXMLString("/MembershipStatus/Balance", cbsMemberStatus);

            memberStatus.put("Balance", strBalance);
            memberStatus.put("Name", strName);
            memberStatus.put("MemberNo", strMemberNo);
            memberStatus.put("ServiceNo", strServiceNo);

            return memberStatus;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return memberStatus;
    }

    public void sendLoansTnCsViaEmail(USSDRequest theUSSDRequest, String theEmailAddress, String strLoanTypeID, String strLoanTypeName) {
        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            String strTraceId = theUSSDRequest.getUSSDTraceID();
            String strSessionId = fnModifyUSSDSessionID(theUSSDRequest);

            String strDateNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            String strMemberName = CBSAPI.getMemberName(String.valueOf(theUSSDRequest.getUSSDMobileNo()));

            String strSalutations = "Dear " + strMemberName + ",<br/>\n" +
                                    "Thank you for showing interest in our loan products.<br/>\n" +
                                    "Kindly find the terms and conditions for " + strLoanTypeName + " attached in this email.<br/>\n" +
                                    "</div>";

            String strFileName = "files/Defence SACCO Terms and Conditions.pdf";
            String strFilePath = "" + strFileName;

            StringBuilder sbHeader = new StringBuilder();
            BufferedReader brHeader = new BufferedReader(new FileReader(new File("files/loans_tncs/html/header.html")));
            String strHeader;
            while ((strHeader = brHeader.readLine()) != null) {
                sbHeader.append(strHeader);
            }
            brHeader.close();

            StringBuilder sbFooter = new StringBuilder();
            BufferedReader brFooter = new BufferedReader(new FileReader(new File("files/loans_tncs/html/footer.html")));
            String strFooter;
            while ((strFooter = brFooter.readLine()) != null) {
                sbFooter.append(strFooter);
            }
            brFooter.close();

            String strMessageBOdy = sbHeader + strSalutations + sbFooter;

            EMail eMail = new EMail();

            eMail.setOriginatorID(strSessionId);
            eMail.setStatusCode(10);
            eMail.setStatusName("QUEUED");
            eMail.setStatusDescription("New EMail");
            eMail.setStatusDate(strDateNow);
            eMail.setSenderEMailID(1);
            eMail.setSenderEMailName("Defence SACCO");
            eMail.setSenderEMailAddress("estatements@timesusacco.com");
            eMail.setReceiverEMailAddresses("<CONTACTS><CONTACT NAME=\"" + strMemberName + "\" ACTION=\"TO\" NOTE=\"\">" + theEmailAddress + "</CONTACT></CONTACTS>");
            eMail.setEMailType(EMailConstants.EMailType.OUTBOUND_EMAIL);
            eMail.setEMailSubject("Loan Terms & Conditions for " + strLoanTypeName);
            eMail.setEMailContentType("HTML");
            eMail.setEMailMessageBody(strMessageBOdy);
            eMail.setAttachment(MBankingConstants.Condition.YES);
            eMail.setDeleteAttachment(MBankingConstants.Condition.NO);

            String strAttachment = "<LINKS><LINK NAME=\"Defence SACCO Terms and Conditions.pdf\">" + strFilePath + "</LINK></LINKS>";
            eMail.setAttachmentLinks(strAttachment);
            eMail.setSensitivity(EMailConstants.Sensitivity.PERSONAL);
            eMail.setCategory("LOANS_TERMS_AND_CONDITIONS");
            eMail.setPriority(200);
            eMail.setSendCount(0);
            eMail.setRequestApplication("USSD");
            eMail.setRequestCorrelationID(strTraceId);
            eMail.setSourceApplication("MBANKING");
            eMail.setSourceReference(strSessionId);
            eMail.setScheduleEMail(MBankingConstants.Condition.NO);
            eMail.setDateCreated(strDateNow);

            Thread worker = new Thread(() -> {
                EMailDB.insertEMailLog(eMail);
            });
            worker.start();
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
    }

    public void sendLoansTnCsViaSMS(USSDRequest theUSSDRequest, String theLoanType, String strLoanName) {
        try {
            // todo:get loan type url
            String strLoanTNCsURL = "https://timesusacco.com";// Navision.getPort().applyLoan();

            String strMemberName = CBSAPI.getMemberName(String.valueOf(theUSSDRequest.getUSSDMobileNo()));

            String strMSG = "" +
                            "Dear " + strMemberName + ",\n" +
                            "Kindly visit " + strLoanTNCsURL + " to read the TERMS & CONDITIONS for " + strLoanName + ".\n" +
                            "REF: ." + APIUtils.fnModifyUSSDSessionID(theUSSDRequest);

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);
            String strTraceID = theUSSDRequest.getUSSDTraceID();
            fnSendSMS(String.valueOf(theUSSDRequest.getUSSDMobileNo()), strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "LOANS_TERMS_AND_CONDITIONS", "USSD", "MBANKING_SERVER", strUSSDSessionID, strTraceID);
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
    }

    public static boolean fnCheckIfUserCanAccessReporting(USSDRequest theUSSDRequest) {
        try {
            String strMAPPXMLParams = MAPPLocalParameters.getClientXMLParameters();

            String strReportingUsersXPath = "/OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/CONFIGURATION/REPORTING/USERS/USER";

            NodeList nlReportingUsers = APIUtils.getXMLNodeListFromPath(strReportingUsersXPath, strMAPPXMLParams);

            if (nlReportingUsers != null) {
                for (int i = 1; i <= nlReportingUsers.getLength(); i++) {
                    String strMobileNumber = MBankingXMLFactory.getXPathValueFromXMLString(strReportingUsersXPath + "[" + i + "]/@MOBILE_NUMBER", strMAPPXMLParams.trim());
                    if (String.valueOf(theUSSDRequest.getUSSDMobileNo()).equals(strMobileNumber)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static String fnGetReportUserData(USSDRequest theUSSDRequest, String theData) {
        try {
            String strMAPPXMLParams = MAPPLocalParameters.getClientXMLParameters();

            String strReportingUsersXPath = "/OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/CONFIGURATION/REPORTING/USERS/USER";

            NodeList nlReportingUsers = APIUtils.getXMLNodeListFromPath(strReportingUsersXPath, strMAPPXMLParams);

            if (nlReportingUsers != null) {
                for (int i = 1; i <= nlReportingUsers.getLength(); i++) {
                    String strMobileNumber = MBankingXMLFactory.getXPathValueFromXMLString(strReportingUsersXPath + "[" + i + "]/@MOBILE_NUMBER", strMAPPXMLParams.trim());
                    if (String.valueOf(theUSSDRequest.getUSSDMobileNo()).equals(strMobileNumber)) {
                        return MBankingXMLFactory.getXPathValueFromXMLString(strReportingUsersXPath + "[" + i + "]/@" + theData, strMAPPXMLParams.trim());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public APIConstants.TransactionReturnVal fnSendSystemReportsViaEmail(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rVal = APIConstants.TransactionReturnVal.ERROR;
        try {
            System.out.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "()");

            String strTraceId = theUSSDRequest.getUSSDTraceID();
            String strSessionId = fnModifyUSSDSessionID(theUSSDRequest);

            String strDateNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            String strMobileBankingReportXML = CBSAPI.mobileBankingReports();

            String strName = fnGetReportUserData(theUSSDRequest, "NAME");
            String strEmailAddress = fnGetReportUserData(theUSSDRequest, "EMAIL_ADDRESS");
            String strMobileNumber = fnGetReportUserData(theUSSDRequest, "MOBILE_NUMBER");

            String strMessage = "Dear " + strName.split(" ")[0] + ",<br/>Kindly find the mobile banking report you requested for attached<br/>";

            String strTotalMobileBankingUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/TotalUsers", strMobileBankingReportXML);
            String strTotalUSSDUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/TotalUSSDUsers", strMobileBankingReportXML);
            String strTotalMAPPUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/TotalMAPPUsers", strMobileBankingReportXML);

            int intTotalMobileBankingUsers = Integer.parseInt(strTotalMobileBankingUsers);
            int intActiveUSSDUsers = Integer.parseInt(strTotalUSSDUsers);
            String strActiveUSSDUsers = String.valueOf(intActiveUSSDUsers);
            int intInActiveUSSDUsers = intTotalMobileBankingUsers - intActiveUSSDUsers;
            String strInActiveUSSDUsers = String.valueOf(intInActiveUSSDUsers);
            int intActiveMAPPUsers = Integer.parseInt(strTotalMAPPUsers);
            String strActiveMAPPUsers = String.valueOf(intActiveMAPPUsers);
            int intInActiveMAPPUsers = intTotalMobileBankingUsers - intActiveMAPPUsers;
            String strInActiveMAPPUsers = String.valueOf(intInActiveMAPPUsers);

            String strLoginFirstSuspendedUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Security/Login/FirstSuspension", strMobileBankingReportXML);
            String strLoginSecondSuspendedUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Security/Login/SecondSuspension", strMobileBankingReportXML);
            String strLoginBlockedUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Security/Login/Blocked", strMobileBankingReportXML);

            int intLoginFirstSuspendedUsers = Integer.parseInt(strLoginFirstSuspendedUsers);
            int intLoginSecondSuspendedUsers = Integer.parseInt(strLoginSecondSuspendedUsers);
            int intLoginBlockedUsers = Integer.parseInt(strLoginBlockedUsers);


            String strOTPFirstSuspendedUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Security/OTP/FirstSuspension", strMobileBankingReportXML);
            String strOTPSecondSuspendedUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Security/OTP/SecondSuspension", strMobileBankingReportXML);
            String strOTPBlockedUsers = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Security/OTP/Blocked", strMobileBankingReportXML);

            int intOTPFirstSuspendedUsers = Integer.parseInt(strOTPFirstSuspendedUsers);
            int intOTPSecondSuspendedUsers = Integer.parseInt(strOTPSecondSuspendedUsers);
            int intOTPBlockedUsers = Integer.parseInt(strOTPBlockedUsers);

            String strGrowthStatisticsPastDay = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Progress/Growth/Today", strMobileBankingReportXML);
            String strGrowthStatisticsPastWeek = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Progress/Growth/Week", strMobileBankingReportXML);
            String strGrowthStatisticsPastMonth = MBankingXMLFactory.getXPathValueFromXMLString("/Reports/Progress/Growth/Month", strMobileBankingReportXML);

            int intGrowthStatisticsPastDay = Integer.parseInt(strGrowthStatisticsPastDay);
            int intGrowthStatisticsPastWeek = Integer.parseInt(strGrowthStatisticsPastWeek);
            int intGrowthStatisticsPastMonth = Integer.parseInt(strGrowthStatisticsPastMonth);

            NumberFormat nfNumberFormat = NumberFormat.getInstance();
            nfNumberFormat.setGroupingUsed(true);

            strTotalMobileBankingUsers = nfNumberFormat.format(intTotalMobileBankingUsers);
            strActiveUSSDUsers = nfNumberFormat.format(intActiveUSSDUsers);
            strInActiveUSSDUsers = nfNumberFormat.format(intInActiveUSSDUsers);
            strActiveMAPPUsers = nfNumberFormat.format(intActiveMAPPUsers);
            strInActiveMAPPUsers = nfNumberFormat.format(intInActiveMAPPUsers);

            strLoginFirstSuspendedUsers = nfNumberFormat.format(intLoginFirstSuspendedUsers);
            strLoginSecondSuspendedUsers = nfNumberFormat.format(intLoginSecondSuspendedUsers);
            strLoginBlockedUsers = nfNumberFormat.format(intLoginBlockedUsers);

            strOTPFirstSuspendedUsers = nfNumberFormat.format(intOTPFirstSuspendedUsers);
            strOTPSecondSuspendedUsers = nfNumberFormat.format(intOTPSecondSuspendedUsers);
            strOTPBlockedUsers = nfNumberFormat.format(intOTPBlockedUsers);

            strGrowthStatisticsPastDay = nfNumberFormat.format(intGrowthStatisticsPastDay);
            strGrowthStatisticsPastWeek = nfNumberFormat.format(intGrowthStatisticsPastWeek);
            strGrowthStatisticsPastMonth = nfNumberFormat.format(intGrowthStatisticsPastMonth);

            Date dtCurrentDate = new Date();
            DateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

            String strCurrentDate = format.format(dtCurrentDate);

            String strDocumentName = "Mobile Banking System Reports";
            String strDocumentReference = APIUtils.fnModifyUSSDSessionID(theUSSDRequest);

            String workingDir = System.getProperty("user.dir");

            // String strFileName = "File Reports as at "+strCurrentDate+".pdf".replaceAll(" ", "_");
            // String strFilePath = workingDir+File.separator+"files"+File.separator+strFileName;
            String strFileName = "O:\\skyworld\\temp\\" + strSessionId + ".pdf";
            // String strFilePath = "E:\\MobileBanking\\Services\\MBankingServer\\workspace\\files\\"+strFileName;

            StringBuilder sbAllHtml = new StringBuilder();
            BufferedReader brFooter = new BufferedReader(new FileReader(new File("files/reports/both.html")));
            String strFooter;
            while ((strFooter = brFooter.readLine()) != null) {
                sbAllHtml.append(strFooter);
            }
            brFooter.close();

            String strPassword = Utils.generateRandomString(6);

            String strMSG = "" +
                            "Dear " + strName.split(" ")[0] + ",\n" +
                            "Kindly use " + strPassword + " as the password for the file sent to you via e-mail\n" +
                            "REF: ." + APIUtils.fnModifyUSSDSessionID(theUSSDRequest);

            String strUSSDSessionID = fnModifyUSSDSessionID(theUSSDRequest);
            String strTraceID = theUSSDRequest.getUSSDTraceID();
            fnSendSMS(String.valueOf(theUSSDRequest.getUSSDMobileNo()), strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "LOANS_TERMS_AND_CONDITIONS", "USSD", "MBANKING_SERVER", strUSSDSessionID, strTraceID);

            String strReportBody = "" +
                                   "<tr>\n" +
                                   "                <td class=\"table-column-title\" style=\"border-left: 1px solid transparent; border-right: 1px solid transparent; color: transparent;  visibility: hidden;\" colspan=\"6\">.</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title table-column-title-colored\" colspan=\"6\">Mobile Banking Users</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title\">Product Name</td>\n" +
                                   "                <td class=\"bold-text\">Total Users</td>\n" +
                                   "                <td class=\"bold-text\">Active</td>\n" +
                                   "                <td class=\"bold-text\">Inactive</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr class=\"right-align\">\n" +
                                   "                <td class=\"left-align\">All Mobile Banking</td>\n" +
                                   "                <td>" + strTotalMobileBankingUsers + "</td>\n" +
                                   "                <td colspan=\"2\"></td>\n" +
                                   "            </tr>\n" +
                                   "            <tr class=\"right-align\">\n" +
                                   "                <td class=\"left-align\">USSD</td>\n" +
                                   "                <td>" + strTotalMobileBankingUsers + "</td>\n" +
                                   "                <td>" + strActiveUSSDUsers + "</td>\n" +
                                   "                <td>" + strInActiveUSSDUsers + "</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr class=\"right-align\">\n" +
                                   "                <td class=\"left-align\">Mobile App</td>\n" +
                                   "                <td>" + strTotalMobileBankingUsers + "</td>\n" +
                                   "                <td>" + strActiveMAPPUsers + "</td>\n" +
                                   "                <td>" + strInActiveMAPPUsers + "</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title\" style=\"border-left: 1px solid transparent; border-right: 1px solid transparent; color: transparent;  visibility: hidden;\" colspan=\"6\">.</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title table-column-title-colored\" colspan=\"6\">System Security</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title\">Product Name</td>\n" +
                                   "                <td class=\"bold-text\">First Suspension</td>\n" +
                                   "                <td class=\"bold-text\">Second Suspension</td>\n" +
                                   "                <td class=\"bold-text\">Blocked</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr class=\"right-align\">\n" +
                                   "                <td class=\"left-align\">Mobile Banking PIN</td>\n" +
                                   "                <td>" + strLoginFirstSuspendedUsers + "</td>\n" +
                                   "                <td>" + strLoginSecondSuspendedUsers + "</td>\n" +
                                   "                <td>" + strLoginBlockedUsers + "</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr class=\"right-align\">\n" +
                                   "                <td class=\"left-align\">One Time Password (OTP)</td>\n" +
                                   "                <td>" + strOTPFirstSuspendedUsers + "</td>\n" +
                                   "                <td>" + strOTPSecondSuspendedUsers + "</td>\n" +
                                   "                <td>" + strOTPBlockedUsers + "</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title\" style=\"border-left: 1px solid transparent; border-right: 1px solid transparent; color: transparent; visibility: hidden;\" colspan=\"6\">.</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title table-column-title-colored\" colspan=\"6\">User Growth Statistics</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr>\n" +
                                   "                <td class=\"table-column-title\">Statistic</td>\n" +
                                   "                <td class=\"bold-text\">Past 1 Day</td>\n" +
                                   "                <td class=\"bold-text\">Past 1 Week</td>\n" +
                                   "                <td class=\"bold-text\">Past 1 Month</td>\n" +
                                   "            </tr>\n" +
                                   "            <tr class=\"right-align\">\n" +
                                   "                <td class=\"left-align\">New Users</td>\n" +
                                   "                <td>" + strGrowthStatisticsPastDay + "</td>\n" +
                                   "                <td>" + strGrowthStatisticsPastWeek + "</td>\n" +
                                   "                <td>" + strGrowthStatisticsPastMonth + "</td>\n" +
                                   "            </tr>";

            String strAllHtml = sbAllHtml.toString().replaceAll("REPORT_BODY", strReportBody);

            strAllHtml = strAllHtml.replaceAll("REPORT_NAME", strDocumentName);
            strAllHtml = strAllHtml.replaceAll("REPORT_DATE_GENERATED", strCurrentDate);

            strAllHtml = strAllHtml.replaceAll("DOCUMENT_DATE_GENERATED", strCurrentDate);
            strAllHtml = strAllHtml.replaceAll("DOCUMENT_REFERENCE", strDocumentReference);
            strAllHtml = strAllHtml.replaceAll("DOCUMENT_RECEIVER_NAME", strName);
            strAllHtml = strAllHtml.replaceAll("DOCUMENT_RECEIVER_EMAIL", strEmailAddress);

            PDF.fnCreate(strFileName, strAllHtml, strPassword);

            EMail eMail = new EMail();

            eMail.setOriginatorID(strSessionId);
            eMail.setStatusCode(10);
            eMail.setStatusName("QUEUED");
            eMail.setStatusDescription("New EMail");
            eMail.setStatusDate(strDateNow);
            eMail.setSenderEMailID(1);
            eMail.setSenderEMailName("Defence SACCO");
            eMail.setSenderEMailAddress("estatements@timesusacco.com");
            eMail.setReceiverEMailAddresses("<CONTACTS><CONTACT NAME=\"" + strName + "\" ACTION=\"TO\" NOTE=\"\">" + strEmailAddress + "</CONTACT></CONTACTS>");
            eMail.setEMailType(EMailConstants.EMailType.OUTBOUND_EMAIL);
            eMail.setEMailSubject("Mobile Banking Reports as at " + strCurrentDate);
            eMail.setEMailContentType("HTML");
            eMail.setEMailMessageBody(strMessage);
            eMail.setAttachment(MBankingConstants.Condition.YES);
            eMail.setDeleteAttachment(MBankingConstants.Condition.NO);

            String strAttachment = "<LINKS><LINK NAME=\"" + strFileName + "\">" + strFileName + "</LINK></LINKS>";
            eMail.setAttachmentLinks(strAttachment);
            eMail.setSensitivity(EMailConstants.Sensitivity.PERSONAL);
            eMail.setCategory("SYSTEM_REPORTS");
            eMail.setPriority(200);
            eMail.setSendCount(0);
            eMail.setRequestApplication("USSD");
            eMail.setRequestCorrelationID(strTraceId);
            eMail.setSourceApplication("MBANKING");
            eMail.setSourceReference(strSessionId);
            eMail.setScheduleEMail(MBankingConstants.Condition.NO);
            eMail.setDateCreated(strDateNow);

            Thread worker = new Thread(() -> {
                EMailDB.insertEMailLog(eMail);
            });
            worker.start();
            rVal = APIConstants.TransactionReturnVal.SUCCESS;
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static String fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType theTransactionType, String theAmount) {
        String strRVal = "";
        try {
            BigDecimal bdAmount = BigDecimal.valueOf(Double.parseDouble(theAmount.replaceAll(",", "")));
            strRVal = CBSAPI.geTransactionCharges(theTransactionType.getValue(), bdAmount);
            if (!strRVal.equals("0") && strRVal.length() < 10) {
                strRVal = "\nTransaction Charge: KES " + strRVal;
                return strRVal;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public APIConstants.TransactionReturnVal enableDisableCashWithdrawal(USSDRequest theUSSDRequest) {
        APIConstants.TransactionReturnVal rval = APIConstants.TransactionReturnVal.ERROR;
        try {
            String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());
            String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());

            if (strAction.equals("ENABLE_CASH_WITHDRAWAL")) {
                strAction = "ENABLE";
            } else {
                strAction = "DISABLE";
            }

            System.out.println("strAction: " + strAction);
            System.out.println("strAccount: " + strAccount);

            // boolean blDeactivationStatus = Navision.getPort().actionServiceForUser(strAction, strAccount, "WD_TO_OTHER");
            boolean blDeactivationStatus = true;

            if (blDeactivationStatus) {
                rval = APIConstants.TransactionReturnVal.SUCCESS;
            } else {
                rval = APIConstants.TransactionReturnVal.ERROR;
            }
        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + ".deactivateMobileApp() ERROR : " + e.getMessage());
        }

        return rval;
    }

    public LinkedHashMap<String, String> getPayMODBillTypeBranches(USSDRequest theUSSDRequest, String MODBillType) {
        LinkedHashMap<String, String> MODBillTypeBranches = null;
        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            MODBillTypeBranches = new LinkedHashMap<>();

            switch (MODBillType) {
                case "DOD_CAU": {
                    MODBillTypeBranches.put("1002000123", "Officers' Mess Restaurant");
                    MODBillTypeBranches.put("2000301200", "Officers' Mess Bar");
                    MODBillTypeBranches.put("2000301201", "WOS & SGTS Mess Restaurant");
                    MODBillTypeBranches.put("2000301202", "WOS & SGTS Mess Bar");
                    break;
                }
                case "EMBAKASI": {
                    MODBillTypeBranches.put("2000301200", "Officers' Mess Restaurant");
                    MODBillTypeBranches.put("3000301200", "Officers' Mess Bar");
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }

        return MODBillTypeBranches;
    }

    public String getUserDateOfBirth(String strUserPhoneNumber) {
        String strDateOfBirth = "";

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES, "date_of_birth",
                new FilterPredicate("primary_mobile_number = :primary_mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":primary_mobile_number", strUserPhoneNumber));

        if (signatoryDetailsWrapper.hasErrors()) {
            return strDateOfBirth;
        }

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        if (signatoryDetailsMap != null && !signatoryDetailsMap.isEmpty()) {
            strDateOfBirth = signatoryDetailsMap.getStringValue("date_of_birth");
        }

        return strDateOfBirth;
    }

    public TransactionWrapper<FlexicoreHashMap> setPIN(USSDRequest theUSSDRequest) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());
            String strNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name());

            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            return CBSAPI.setUserPIN(strReferenceKey, "MSISDN", strMobileNumber, strNewPIN, "IMSI", strSIMID);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }

        return resultWrapper;
    }

    public String getUserFullName(String strUserPhoneNumber) {
        String strAccountName = "";

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES, "full_name",
                new FilterPredicate("primary_mobile_number = :primary_mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":primary_mobile_number", strUserPhoneNumber));

        if (signatoryDetailsWrapper.hasErrors()) {
            return "";
        }

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        if (signatoryDetailsMap != null && !signatoryDetailsMap.isEmpty()) {
            strAccountName = signatoryDetailsMap.getStringValue("full_name").trim();
            return Utils.toTitleCase(strAccountName);
        }

        return strAccountName;
    }

    public TransactionWrapper<FlexicoreHashMap> validateAccount(USSDRequest theUSSDRequest, String strAccountNumber) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            String strReferenceKey = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

            HashMap<String, String> userIdentifierDetails = APIUtils.getUserIdentifierDetails(strMobileNumber);
            String strIdentifierType = userIdentifierDetails.get("identifier_type");
            String strIdentifier = userIdentifierDetails.get("identifier");

            return CBSAPI.validateAccountNumber(strMobileNumber, strIdentifierType, strIdentifier, strAccountNumber);

        } catch (Exception e) {
            System.err.println(this.getClass().getSimpleName() + "." + new Object() {
            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }

        return resultWrapper;
    }

    public String getUserFirstName(String theUserPhoneNumber) {
        String strUserFullName = getUserFullName(theUserPhoneNumber);

        return strUserFullName.split(" ")[0];
    }
}
