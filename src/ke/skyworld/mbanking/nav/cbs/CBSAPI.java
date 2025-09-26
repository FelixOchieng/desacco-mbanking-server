package ke.skyworld.mbanking.nav.cbs;

import ke.co.skyworld.smp.authentication_manager.MobileBankingCryptography;
import ke.co.skyworld.smp.authentication_manager.SMPCryptography;
import ke.co.skyworld.smp.comm_channels_manager.EmailTemplates;
import ke.co.skyworld.smp.permissions.SystemApplicationCodes;
import ke.co.skyworld.smp.query_manager.SystemTables;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_manager.query.QueryBuilder;
import ke.co.skyworld.smp.query_manager.util.SystemParameters;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.Misc;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.co.skyworld.smp.utility_items.memory.JvmManager;
import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.pesa.PESA;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.channelutils.EmailMessaging;
import ke.skyworld.mbanking.channelutils.Messaging;
import ke.skyworld.mbanking.mappapi.MAPPAPIConstants;
import ke.skyworld.mbanking.nav.Navision;
import ke.skyworld.mbanking.nav.utils.HttpSOAP;
import ke.skyworld.mbanking.nav.utils.LoggingLevel;
import ke.skyworld.mbanking.nav.utils.XmlObject;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ke.co.skyworld.smp.query_manager.SystemTables.*;
import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_WORKFLOWS;
import static ke.skyworld.mbanking.nav.utils.XmlObject.getNamespaceContext;
import static ke.skyworld.mbanking.ussdapi.APIUtils.fnSendSMS;

/**
 * mbanking-server-trans-nation-v2 (ke.skyworld.mbanking.nav.cbs)
 * Created by: dmutende
 * On: 17 Feb, 2024 06:01
 **/
public class CBSAPI {

    public static final String CBS_ERROR = "CGEx0E1";

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

    public static TransactionWrapper<FlexicoreHashMap> checkUser(String theReferenceKey, String theIdentifierType, String theIdentifier, String theDeviceIdentifierType, String theDeviceIdentifier) {
        return checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier, false);
    }

    public static TransactionWrapper<FlexicoreHashMap> checkUser(String theReferenceKey, String theIdentifierType, String theIdentifier, String theDeviceIdentifierType, String theDeviceIdentifier, boolean isActivation) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            FilterPredicate signatoryFilterPredicate = null;
            FlexicoreHashMap queryArguments = new FlexicoreHashMap();

            if ("MSISDN".equals(theIdentifierType)) {
                signatoryFilterPredicate = new FilterPredicate("primary_mobile_number = :primary_mobile_number");
                queryArguments.addQueryArgument(":primary_mobile_number", theIdentifier);
            } else if ("CUSTOMER_NO".equals(theIdentifierType)) {
                signatoryFilterPredicate = new FilterPredicate("identifier_type = :identifier_type AND identifier = :identifier");
                queryArguments.addQueryArgument(":identifier_type", theIdentifierType);
                queryArguments.addQueryArgument(":identifier", theIdentifier);
            } else {
                signatoryFilterPredicate = new FilterPredicate("primary_identity_type = :primary_identity_type AND primary_identity_no = :primary_identity_no");
                queryArguments.addQueryArgument(":primary_identity_type", theIdentifierType);
                queryArguments.addQueryArgument(":primary_identity_no", theIdentifier);
            }

            TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                    SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES, signatoryFilterPredicate, queryArguments);

            if (signatoryDetailsWrapper.hasErrors()) {
                signatoryDetailsWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);

                signatoryDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("title", "An error occurred"));

                return signatoryDetailsWrapper;
            }

            FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

            if (signatoryDetailsMap == null || signatoryDetailsMap.isEmpty()) {
                signatoryDetailsWrapper.setHasErrors(true);
                signatoryDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.NOT_FOUND)
                        .putValue("display_message", "Sorry, you are not registered to use " + AppConstants.strMobileBankingName + "." + getTrailerMessage())
                        .putValue("title", "Account not found"));

                return signatoryDetailsWrapper;
            }

            String strSignatoryId = signatoryDetailsMap.getStringValue("signatory_id");

            TransactionWrapper<FlexicoreHashMap> mobileMappingDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                    SystemTables.TBL_MOBILE_BANKING_REGISTER,
                    new FilterPredicate("signatory_id = :signatory_id"),
                    new FlexicoreHashMap().addQueryArgument(":signatory_id", strSignatoryId));

            if (mobileMappingDetailsWrapper.hasErrors()) {
                mobileMappingDetailsWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);

                mobileMappingDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("title", "An error occurred"));

                return mobileMappingDetailsWrapper;
            }

            FlexicoreHashMap mobileBankingDetailsMap = mobileMappingDetailsWrapper.getSingleRecord();

            if (mobileBankingDetailsMap == null || mobileBankingDetailsMap.isEmpty()) {
                mobileMappingDetailsWrapper.setHasErrors(true);
                mobileMappingDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.NOT_FOUND)
                        .putValue("display_message", "Sorry, you are not registered to use " + AppConstants.strMobileBankingName + "." + getTrailerMessage())
                        .putValue("title", "An error occurred"));

                return signatoryDetailsWrapper;
            }

            String strPrimaryMobileNumber = signatoryDetailsMap.getStringValue("primary_mobile_number");
            String strMobileNumber = mobileBankingDetailsMap.getStringValue("mobile_number");

            if (!strPrimaryMobileNumber.equalsIgnoreCase(strMobileNumber)) {
                System.err.println("CBSAPI.checkUser() - Corrupt Signatory Mobile Channel Details found where Identifier Type = '" + theIdentifierType + "' and Identifier = '" + theIdentifier + "'");

                mobileMappingDetailsWrapper.setHasErrors(true);
                mobileMappingDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INTEGRITY_VIOLATION_MOBILE)
                        .putValue("display_message", "Sorry, an error occurred while processing your request.\n\nERR_MOBCH500" + getTrailerMessage())
                        .putValue("title", "An error occurred"));

                return mobileMappingDetailsWrapper;
            }

            String signatoryStatus = signatoryDetailsMap.getStringValue("status");

            if (!signatoryStatus.equalsIgnoreCase("ACTIVE") && !signatoryStatus.equalsIgnoreCase("NEW")) {
                signatoryStatus = signatoryStatus.toUpperCase();

                String strErrCode = "";

                switch (signatoryStatus) {
//                    case "NEW" -> strErrCode = "\n\nERR_CUST100";
                    case "DORMANT" -> strErrCode = "\n\nERR_CUST150";
                    case "CLOSED" -> strErrCode = "\n\nERR_CUST200";
                    case "DEFAULTED" -> strErrCode = "\n\nERR_CUST250";
                }

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.valueOf(signatoryStatus))
                        .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + "." + strErrCode + getTrailerMessage())
                        .putValue("title", "Login Failed"));

                return resultWrapper;
            }

            String mobileBankingStatus = mobileBankingDetailsMap.getStringValue("status");
            String mobileBankingStatusStartDate = mobileBankingDetailsMap.getStringValue("status_start_date");
            String mobileBankingStatusEndDate = mobileBankingDetailsMap.getStringValue("status_end_date");

            if (mobileBankingStatus.equalsIgnoreCase("BLOCKED")) {
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.BLOCKED)
                        .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB100" + getTrailerMessage())
                        .putValue("title", "Login Failed"));
                return resultWrapper;
            }

            if (mobileBankingStatusStartDate != null && !mobileBankingStatusStartDate.isBlank()
                && mobileBankingStatusEndDate != null && !mobileBankingStatusEndDate.isBlank()) {

                LocalDateTime startDateTime = LocalDateTime.parse(mobileBankingStatusStartDate, DateTimeFormatter.ofPattern(DateTime.DEFAULT_DATE_TIME_FORMAT));
                LocalDateTime endDateTime = LocalDateTime.parse(mobileBankingStatusEndDate, DateTimeFormatter.ofPattern(DateTime.DEFAULT_DATE_TIME_FORMAT));
                LocalDateTime currDateTime = LocalDateTime.now();

                boolean isStatusStillApplicable = currDateTime.isAfter(startDateTime) && currDateTime.isBefore(endDateTime);

                String strUserFriendlyEndDate = DateTime.convertStringToDateToString(mobileBankingStatusEndDate, DateTime.DEFAULT_DATE_TIME_FORMAT, "yyyy-MM-dd HH:mm");

                if (isStatusStillApplicable) {
                    mobileBankingStatus = mobileBankingStatus.toUpperCase();
                    switch (mobileBankingStatus) {
                        case "SUSPENDED": {
                            resultWrapper.setHasErrors(true);
                            resultWrapper.setData(new FlexicoreHashMap()
                                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                    .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB150" + getTrailerMessage())
                                    .putValue("title", "Login Failed"));

                            return resultWrapper;
                        }
                        case "INACTIVE": {
                            resultWrapper.setHasErrors(true);
                            resultWrapper.setData(new FlexicoreHashMap()
                                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INACTIVE)
                                    .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB200" + getTrailerMessage())
                                    .putValue("title", "Login Failed"));
                            return resultWrapper;
                        }
                    }
                } else {
                    mobileBankingStatus = mobileBankingStatus.toUpperCase();
                    /*if ("ACTIVE" .equals(mobileBankingStatus)) {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account ACTIVE period has EXPIRED and cannot use " + AppConstants.strMobileBankingName + " services." + getTrailerMessage())
                                .putValue("title", "Account Status Expiry"));
                        return resultWrapper;
                    }else*/

                    {

                        FlexicoreHashMap updateMap = new FlexicoreHashMap();
                        updateMap.putValue("status", "ACTIVE");
                        updateMap.putValue("status_start_date", null);
                        updateMap.putValue("status_end_date", null);
                        updateMap.putValue("date_modified", DateTime.getCurrentDateTime());

                        mobileBankingDetailsMap.copyFrom(updateMap);

                        String strIntegrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingDetailsMap);
                        updateMap.putValue("integrity_hash", strIntegrityHash);

                        Repository.update(StringRefs.SENTINEL,
                                TBL_MOBILE_BANKING_REGISTER,
                                updateMap,
                                new FilterPredicate("mobile_register_id = :mobile_register_id"),
                                new FlexicoreHashMap().addQueryArgument(":mobile_register_id", mobileBankingDetailsMap.getStringValue("mobile_register_id")));
                    }
                }
            } else {
                mobileBankingStatus = mobileBankingStatus.toUpperCase();
                switch (mobileBankingStatus) {
                    case "SUSPENDED": {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB150" + getTrailerMessage())
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }
                    case "INACTIVE": {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INACTIVE)
                                .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB200" + getTrailerMessage())
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }
                }
            }

            String loginAuthAction = mobileBankingDetailsMap.getStringValue("login_auth_action");
            String loginAuthActionValidDate = mobileBankingDetailsMap.getStringValue("login_auth_action_valid_date");

            Date currentDate = DateTime.getCurrentJavaUtilDateTime();

            switch (loginAuthAction) {
                case "SUSPEND": {

                    if (loginAuthActionValidDate == null || loginAuthActionValidDate.isBlank()) {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " due to many Login Attempts." + getTrailerMessage())
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }

                    Date dateLoginAuthActionValidDate = DateTime.convertDateStringToDate(loginAuthActionValidDate);

                    if (currentDate.before(dateLoginAuthActionValidDate)) {
                        Date dtNow = new Date();
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " due to many Login Attempts. " +
                                                             "Please try again in " + DateTime.getPrettyDateTimeDifferenceRoundedUp(dtNow, dateLoginAuthActionValidDate))
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }
                    break;
                }
                case "LOCK": {
                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.LOCKED)
                            .putValue("display_message", "Sorry, your " + AppConstants.strSACCOName + " account is LOCKED due to many Login Attempts." + getTrailerMessage())
                            .putValue("title", "Login Failed"));
                    return resultWrapper;
                }
            }

            String otpAuthAction = mobileBankingDetailsMap.getStringValue("otp_auth_action");
            String otpAuthActionValidDate = mobileBankingDetailsMap.getStringValue("otp_auth_action_valid_date");

            currentDate = DateTime.getCurrentJavaUtilDateTime();

            switch (otpAuthAction) {
                case "SUSPEND": {
                    if (otpAuthActionValidDate == null || otpAuthActionValidDate.isBlank()) {

                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " due to many OTP Attempts." + getTrailerMessage())
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }

                    Date dateOtpAuthActionValidDate = DateTime.convertDateStringToDate(otpAuthActionValidDate);

                    if (currentDate.before(dateOtpAuthActionValidDate)) {
                        Date dtNow = new Date();

                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " due to many OTP Attempts." +
                                                             " Please try again in " + DateTime.getPrettyDateTimeDifferenceRoundedUp(dtNow, dateOtpAuthActionValidDate))
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }
                    break;
                }
                case "LOCK": {

                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.LOCKED)
                            .putValue("display_message", "Sorry, your " + AppConstants.strSACCOName + " account is LOCKED due to many OTP Attempts." + getTrailerMessage())
                            .putValue("title", "Login Failed"));
                    return resultWrapper;
                }
            }

            String kycAuthAction = mobileBankingDetailsMap.getStringValue("kyc_auth_action");
            String kycAuthActionValidDate = mobileBankingDetailsMap.getStringValue("kyc_auth_action_valid_date");

            currentDate = DateTime.getCurrentJavaUtilDateTime();

            switch (kycAuthAction) {
                case "SUSPEND": {

                    if (kycAuthActionValidDate == null || kycAuthActionValidDate.isBlank()) {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " due to many National ID Attempts." + getTrailerMessage())
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }

                    Date dateLoginAuthActionValidDate = DateTime.convertDateStringToDate(kycAuthActionValidDate);

                    if (currentDate.before(dateLoginAuthActionValidDate)) {
                        Date dtNow = new Date();
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUSPENDED)
                                .putValue("display_message", "Sorry, your account is SUSPENDED from using " + AppConstants.strSACCOName + " due to many National ID Attempts. " +
                                                             "Please try again in " + DateTime.getPrettyDateTimeDifferenceRoundedUp(dtNow, dateLoginAuthActionValidDate))
                                .putValue("title", "Login Failed"));
                        return resultWrapper;
                    }
                    break;
                }
                case "LOCK": {

                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.LOCKED)
                            .putValue("display_message", "Sorry, your " + AppConstants.strSACCOName + " account is LOCKED due to many National ID Attempts.")
                            .putValue("title", "Login Failed"));
                    return resultWrapper;
                }
            }

            try {
                String strLastLogInDate = mobileBankingDetailsMap.getStringValue("last_login_date");
                String strStatus = mobileBankingDetailsMap.getStringValue("status");
                String strStatusDate = mobileBankingDetailsMap.getStringValue("status_date");

                if (strLastLogInDate != null) {
                    String strXML = SystemParameters.getParameter(AppConstants.strSettingParamName);
                    Document document = XmlUtils.parseXml(strXML);

                    String strTimeUnits = XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/ACCOUNT_MAX_INACTIVE_PERIOD/@UNIT");
                    String strPeriod = XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/ACCOUNT_MAX_INACTIVE_PERIOD");

                    Date dateLastLogin = DateTime.convertDateStringToDate(strLastLogInDate);
                    Date dateStatusDate = DateTime.convertDateStringToDate(strStatusDate);

                    Date expirationDate = DateTime.add(dateLastLogin, Integer.parseInt(strPeriod), DateTime.getPeriodUnit(strTimeUnits));
                    Date futureStatusDate = DateTime.add(dateStatusDate, Integer.parseInt(strPeriod), DateTime.getPeriodUnit(strTimeUnits));

                    Date dtCurrDate = new Date();

                    /*System.out.println("CURRENT DATE:     "+ dtCurrDate);
                    System.out.println("EXPIRATION DATE:  "+ expirationDate);
                    System.out.println("FUTURE DATE:      "+ futureStatusDate);*/

                    if (dtCurrDate.after(expirationDate) && dtCurrDate.after(futureStatusDate)) {

                        FlexicoreHashMap updateMap = new FlexicoreHashMap();
                        updateMap.putValue("status", "INACTIVE");
                        updateMap.putValue("status_start_date", null);
                        updateMap.putValue("status_end_date", null);
                        updateMap.putValue("date_modified", DateTime.getCurrentDateTime());

                        mobileBankingDetailsMap.copyFrom(updateMap);

                        String strIntegrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingDetailsMap);
                        updateMap.putValue("integrity_hash", strIntegrityHash);

                        Repository.update(StringRefs.SENTINEL,
                                TBL_MOBILE_BANKING_REGISTER,
                                updateMap,
                                new FilterPredicate("mobile_register_id = :mobile_register_id"),
                                new FlexicoreHashMap().addQueryArgument(":mobile_register_id", mobileBankingDetailsMap.getStringValue("mobile_register_id")));

                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INACTIVE)
                                .putValue("display_message", "Sorry, your account cannot use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB200" + getTrailerMessage())
                                .putValue("title", "Account is Inactive"));
                        return resultWrapper;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (theDeviceIdentifier != null && !theDeviceIdentifier.isBlank()) {
                if (theDeviceIdentifierType.equalsIgnoreCase("IMSI") || theDeviceIdentifierType.equalsIgnoreCase("ICCID")) {
                    String strSimIdentifier = mobileBankingDetailsMap.getStringValue("sim_identifier");
                    if (strSimIdentifier != null && !strSimIdentifier.equalsIgnoreCase(theDeviceIdentifier)) {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INVALID_IMSI)
                                .putValue("display_message", "Sorry, your SIM Card is not allowed to use " + AppConstants.strMobileBankingName + ".\n\nERR_MOB300" + getTrailerMessage())
                        );
                        return resultWrapper;
                    }
                } else if (theDeviceIdentifierType.equalsIgnoreCase("IMEI") || theDeviceIdentifierType.equalsIgnoreCase("APP_ID")) {
                    String strAppIdentifier = mobileBankingDetailsMap.getStringValue("app_identifier");

                    /*if (strAppIdentifier == null || strAppIdentifier.isBlank()) {
                        resultWrapper.setHasErrors(true);
                        resultWrapper.setData(new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INVALID_APP_ID)
                                .putValue("display_message", "Sorry, your Mobile Phone is not allowed to use " + AppConstants.strMobileBankingName + " services." + getTrailerMessage())
                                .putValue("title", "Mobile App not Activated"));
                        return resultWrapper;
                    }*/

                    if (!isActivation && !theIdentifier.equalsIgnoreCase("254706405989")) {
                        if (strAppIdentifier == null || !strAppIdentifier.equalsIgnoreCase(theDeviceIdentifier)) {
                            resultWrapper.setHasErrors(true);
                            resultWrapper.setData(new FlexicoreHashMap()
                                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INVALID_APP_ID)
                                    .putValue("signatory_details", signatoryDetailsMap)
                                    .putValue("mobile_register_details", mobileBankingDetailsMap)
                                    .putValue("display_message", "Sorry, your Mobile Phone is not allowed to use " + AppConstants.strMobileBankingName + "." + getTrailerMessage())
                                    .putValue("title", "Mobile App not Activated")
                            );

                            return resultWrapper;
                        }
                    }
                }
            } else {

                if ((theDeviceIdentifierType.equalsIgnoreCase("IMEI") || theDeviceIdentifierType.equalsIgnoreCase("APP_ID")) && !isActivation) {
                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INVALID_APP_ID)
                            .putValue("signatory_details", signatoryDetailsMap)
                            .putValue("mobile_register_details", mobileBankingDetailsMap)
                            .putValue("display_message", "Sorry, your Mobile Phone is not allowed to use " + AppConstants.strMobileBankingName + "." + getTrailerMessage())
                            .putValue("title", "Mobile App not Activated")
                    );

                    return resultWrapper;
                }
            }

            FlexicoreHashMap userMobileBankingDetailsMap = new FlexicoreHashMap();
            userMobileBankingDetailsMap.putValue("signatory_details", signatoryDetailsMap);
            userMobileBankingDetailsMap.putValue("mobile_register_details", mobileBankingDetailsMap);

            resultWrapper.setData(userMobileBankingDetailsMap);

        } catch (Exception e) {
            System.err.println("CBSAPI.checkUser(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "An error occurred"));

        }

        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> userLogin(String theReferenceKey, String theIdentifierType, String theIdentifier, String thePIN, String theDeviceIdentifierType, String theDeviceIdentifier, USSDAPIConstants.MobileChannel theDevice) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                if (theDevice == USSDAPIConstants.MobileChannel.MOBILE_APP) {

                    if (!checkUserResultMap.getStringValueOrIfNull("cbs_api_return_val", "")
                            .equalsIgnoreCase("INVALID_APP_ID")) {

                        return checkUserResultMapWrapper;
                    }
                } else {
                    return checkUserResultMapWrapper;
                }
            }

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            String strProperPIN = mobileBankingMap.getStringValue("pin");

            thePIN = MobileBankingCryptography.hashPIN(signatoryDetailsMap.getStringValue("primary_mobile_number"), thePIN);

            if (thePIN.equalsIgnoreCase(strProperPIN)) {
                String strPinSettings = SystemParameters.getParameter("PIN_PARAMETERS");
                Document document = XmlUtils.parseXml(strPinSettings);

                Element elPinSettings = XmlUtils.getElementNodeFromXpath(document, "/PIN_PARAMETERS/PIN[@TYPE='" + mobileBankingMap.getStringValue("pin_status") + "']");

                if (mobileBankingMap.getStringValue("pin_status").equalsIgnoreCase("ACTIVE")) {
                    String strExpiry = elPinSettings.getAttribute("EXPIRY");
                    String strExpiryTimeunit = elPinSettings.getAttribute("EXPIRY_TIMEUNIT");

                    Date datePinSetDate = DateTime.convertDateStringToDate(mobileBankingMap.getStringValue("pin_set_date"));

                    Date expirationDate = DateTime.add(datePinSetDate, Integer.parseInt(strExpiry), DateTime.getPeriodUnit(strExpiryTimeunit));

                    if (new Date().after(expirationDate)) {
                        resultWrapper.setHasErrors(true);

                        FlexicoreHashMap resultMap = new FlexicoreHashMap().putValue("end_session",
                                        USSDAPIConstants.Condition.YES)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.EXPIRED_PIN);

                        if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                            resultMap.putValue("display_message", "Your current PIN has expired. Please contact the SACCO to have your PIN reset.");
                        } else {
                            resultMap.putValue("display_message", "Your current Password has expired. Please contact the SACCO to have your PIN reset.");
                            resultMap.putValue("title", "Password Expired");
                        }

                        resultWrapper.setData(resultMap);

                        return resultWrapper;
                    }
                }

                theUpdateLoginParamsMap.put("login_attempts", 0);
                theUpdateLoginParamsMap.put("login_auth_action", "NONE");
                theUpdateLoginParamsMap.put("login_auth_action_valid_date", null);
                theUpdateLoginParamsMap.put("login_auth_flag", null);
                theUpdateLoginParamsMap.put("last_login_date", DateTime.getCurrentDateTime());
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                if (theDeviceIdentifier != null && !theDeviceIdentifier.isBlank()) {
                    if (theDeviceIdentifierType.equalsIgnoreCase("IMSI") || theDeviceIdentifierType.equalsIgnoreCase("ICCID")) {
                        String strSimIdentifier = mobileBankingMap.getStringValue("sim_identifier");

                        if (strSimIdentifier == null || strSimIdentifier.isBlank()) {
                            theUpdateLoginParamsMap.put("sim_identifier", theDeviceIdentifier);
                            theUpdateLoginParamsMap.put("sim_identifier_set_date", DateTime.getCurrentDateTime());
                        }

                    } /*else if (theDeviceIdentifierType.equalsIgnoreCase("IMEI") || theDeviceIdentifierType.equalsIgnoreCase("APP_ID")) {
                        String strAppIdentifier = mobileBankingMap.getStringValue("app_identifier");

                        if (strAppIdentifier == null || strAppIdentifier.isBlank()) {
                            theUpdateLoginParamsMap.put("app_identifier", theDeviceIdentifier);
                            theUpdateLoginParamsMap.put("app_identifier_set_date", DateTime.getCurrentDateTime());
                        }
                    }*/
                }

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                            .putValue("title", "Error"));
                }

                resultWrapper.setData(checkUserResultMap);

                if (theDevice == USSDAPIConstants.MobileChannel.MOBILE_APP) {
                    if (checkUserResultMap.getStringValueOrIfNull("cbs_api_return_val", "").equalsIgnoreCase("INVALID_APP_ID")) {
                        return checkUserResultMapWrapper;
                    }
                }

                return resultWrapper;
            } else {

                FlexicoreHashMap resultAuthMap = new FlexicoreHashMap();

                int loginAttempts = Integer.parseInt(mobileBankingMap.getStringValue("login_attempts"));
                loginAttempts = loginAttempts + 1;

                String strMessage;

                String strTitle = "Invalid Credentials";

                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                    strMessage = "{Sorry the PIN provided is NOT correct}\nPlease enter your PIN to proceed:";
                } else {
                    strMessage = "You have entered an incorrect username or password, please try again.";
                }

                USSDAPIConstants.Condition endSession = USSDAPIConstants.Condition.NO;

                resultAuthMap.putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INCORRECT_PIN)
                        .putValue("display_message", strMessage)
                        .putValue("title", strTitle);

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(resultAuthMap);

                String strFirstName = signatoryDetailsMap.getStringValue("full_name").trim().split("\\s")[0];

                HashMap<String, String> hmMSGPlaceholders = new HashMap<>();
                hmMSGPlaceholders.put("[MOBILE_NUMBER]", signatoryDetailsMap.getStringValue("primary_mobile_number"));
                hmMSGPlaceholders.put("[LOGIN_ATTEMPTS]", String.valueOf(loginAttempts));
                // hmMSGPlaceholders.put("[FIRST_NAME]", strFirstName);
                hmMSGPlaceholders.put("[FIRST_NAME]", "Member");

                String strPasswordAttemptParameters = SystemParameters.getParameter("MBANKING_AUTH_ATTEMPTS");

                HashMap<String, HashMap<String, String>> authenticationAttemptsAction = MBankingXMLFactory.getAuthenticationAttemptsAction(
                        loginAttempts,
                        hmMSGPlaceholders,
                        strPasswordAttemptParameters,
                        MBankingConstants.AuthType.PASSWORD);

                HashMap<String, String> currentAuthenticationAttemptsAction = authenticationAttemptsAction.get("CURRENT_ATTEMPT");
                HashMap<String, String> futureAuthenticationAttemptsAction = authenticationAttemptsAction.get("NEXT_ATTEMPT");

                // Check if action is needed
                if (!currentAuthenticationAttemptsAction.isEmpty()) {
                    String loginAction = currentAuthenticationAttemptsAction.get("ACTION");
                    String loginActionTag = currentAuthenticationAttemptsAction.get("NAME");

                    // Check action
                    switch (loginAction) {
                        case "SUSPEND": {
                            int loginActionDuration = Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION"));
                            String loginActionDurationUnit = currentAuthenticationAttemptsAction.get("UNIT");
                            loginActionDuration = DateTime.convertToSeconds(loginActionDuration, loginActionDurationUnit);
                            Date loginActionValidDate = DateTime.add(loginActionDuration, Calendar.SECOND);
                            String strLoginActionValidDate = DateTime.convertDateToDateString(loginActionValidDate);

                            theUpdateLoginParamsMap.put("login_attempts", loginAttempts);
                            theUpdateLoginParamsMap.put("login_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("login_auth_action_valid_date", strLoginActionValidDate);
                            theUpdateLoginParamsMap.put("login_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                            String friendlyActionDuration = currentAuthenticationAttemptsAction.get("DURATION") + " " + loginActionDurationUnit;
                            if (Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION")) != 1)
                                friendlyActionDuration = friendlyActionDuration + "S";

                            strTitle = "Account Suspended";
                            strMessage = "Your " + AppConstants.strMobileBankingName + " account has been SUSPENDED for " + friendlyActionDuration + " due to many Login Attempts. Please try again later.";
                            endSession = USSDAPIConstants.Condition.YES;

                            break;
                        }
                        case "LOCK": {

                            theUpdateLoginParamsMap.put("login_attempts", loginAttempts);
                            theUpdateLoginParamsMap.put("login_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("login_auth_action_valid_date", null);
                            theUpdateLoginParamsMap.put("login_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                            strTitle = "Account Locked";
                            strMessage = "Your " + AppConstants.strMobileBankingName + " account has been LOCKED due to many Login Attempts." + getTrailerMessage();
                            endSession = USSDAPIConstants.Condition.YES;

                            break;
                        }
                        default: {
                            theUpdateLoginParamsMap.put("login_attempts", loginAttempts);
                            theUpdateLoginParamsMap.put("login_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("login_auth_action_valid_date", null);
                            theUpdateLoginParamsMap.put("login_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());
                            endSession = USSDAPIConstants.Condition.YES;
                            break;
                        }
                    }
                }

                String currentLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
                if (currentLoginAction == null) currentLoginAction = "NONE";

                if (!currentLoginAction.equals("LOCK")) {

                    if (!currentLoginAction.equals("SUSPEND")) {

                        // Check future action
                        if (!futureAuthenticationAttemptsAction.isEmpty()) {
                            String futureLoginAction = futureAuthenticationAttemptsAction.get("ACTION");
                            String futureLoginActionDurationUnit = futureAuthenticationAttemptsAction.get("UNIT");
                            String friendlyFutureActionDuration = futureAuthenticationAttemptsAction.get("DURATION") + " " + futureLoginActionDurationUnit;

                            String attemptsRemainingToFutureLoginAction = futureAuthenticationAttemptsAction.get("ATTEMPTS_REMAINING");

                            // Override Incorrect PIN message
                            if (futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")) {
                                int intFutureActionDuration = Integer.parseInt(futureAuthenticationAttemptsAction.get("DURATION"));
                                if (intFutureActionDuration != 1)
                                    friendlyFutureActionDuration = friendlyFutureActionDuration + "S";

                                int intAttemptsRemainingToFutureLoginAction = Integer.parseInt(attemptsRemainingToFutureLoginAction);
                                String strAttempts = "attempt";
                                if (intAttemptsRemainingToFutureLoginAction != 1) strAttempts = strAttempts + "s";

                                strTitle = "Invalid Credentials";

                                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                                    strMessage = "{Sorry the PIN provided is NOT correct}\nYou have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your " + AppConstants.strMobileBankingName + " account is SUSPENDED for " + friendlyFutureActionDuration + ".\nPlease enter your PIN:";
                                } else {
                                    strMessage = "You have " + attemptsRemainingToFutureLoginAction + " " + strAttempts + " before your " + AppConstants.strMobileBankingName + " account is SUSPENDED for " + friendlyFutureActionDuration + ".";
                                }

                                endSession = USSDAPIConstants.Condition.NO;

                            } else if (futureLoginAction.equals("LOCK")) {
                                int intAttemptsRemainingToFutureLoginAction = Integer.parseInt(attemptsRemainingToFutureLoginAction);
                                String strAttempts = "attempt";
                                if (intAttemptsRemainingToFutureLoginAction != 1) strAttempts = strAttempts + "s";

                                strTitle = "Invalid Credentials";
                                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                                    strMessage = "{Sorry the PIN provided is NOT correct}\nYou have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your " + AppConstants.strMobileBankingName + " account is LOCKED.\nPlease enter your PIN:";

                                } else {
                                    strMessage = "You have " + attemptsRemainingToFutureLoginAction + " " + strAttempts + " before your " + AppConstants.strMobileBankingName + " account is LOCKED.";
                                }

                                endSession = USSDAPIConstants.Condition.NO;
                            }
                        }
                    }
                } else {

                    String primaryEmailAddress = signatoryDetailsMap.getStringValue("primary_email_address");

                    if (primaryEmailAddress != null && !primaryEmailAddress.isBlank()) {

                        int finalLoginAttempts = loginAttempts;
                        new Thread(() -> {
                            String strEmail = EmailTemplates.mobileBankingAccountLockedTemplate();
                            strEmail = strEmail.replace("[FULL_NAME]", signatoryDetailsMap.getStringValue("full_name"));
                            strEmail = strEmail.replace("[ATTEMPT_TYPE]", "Login");
                            strEmail = strEmail.replace("[ATTEMPTS]", String.valueOf(finalLoginAttempts));
                            strEmail = strEmail.replace("[PHONE_NUMBER]", signatoryDetailsMap.getStringValue("primary_mobile_number"));

                            EmailMessaging.sendEmail(primaryEmailAddress, "" + AppConstants.strMobileBankingName + " Account LOCKED", strEmail, "ACCOUNT_LOCKED");
                        }).start();
                    }
                }

                theUpdateLoginParamsMap.put("login_attempts", loginAttempts);
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                resultAuthMap.putValue("end_session", endSession);
                resultAuthMap.putValue("display_message", strMessage);
                resultAuthMap.putValue("title", strTitle);

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultAuthMap
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                            .putValue("title", "Error");

                }

                JvmManager.gc(checkUserResultMap, checkUserResultMapWrapper);

                return resultWrapper;
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.userLogin(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "Error"));
        }
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> validateOTP(String theReferenceKey,
                                                                   String theIdentifierType,
                                                                   String theIdentifier,
                                                                   String theDeviceIdentifierType,
                                                                   String theDeviceIdentifier,
                                                                   MAPPAPIConstants.OTP_TYPE theOTPType,
                                                                   String theOTP) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier, true);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {

               /* USSDAPIConstants.StandardReturnVal theReturnVal = checkUserResultMapWrapper.getSingleRecord().getValue("cbs_api_return_val");

                System.out.println("theReturnVal:  "+theReturnVal);
                System.out.println("theOTPType:  "+theOTPType.name());


                if (theReturnVal == INVALID_APP_ID && theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) {
                    //continue
                }else{
                    return checkUserResultMapWrapper;
                }*/

                return checkUserResultMapWrapper;
            }


            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            String strProperOTP = mobileBankingMap.getStringValue("otp");
            String strOTExpiryDate = mobileBankingMap.getStringValueOrIfNull("otp_expiry_date", "1976-01-01 00:00:00.000000");

            boolean isValidOTP = false;
            if (strProperOTP != null) {

                LocalDateTime currDateTime = LocalDateTime.now();
                LocalDateTime expiryDateTime = LocalDateTime.parse(strOTExpiryDate, DateTimeFormatter.ofPattern(DateTime.DEFAULT_DATE_TIME_FORMAT));

                theOTP = MobileBankingCryptography.hashPIN(signatoryDetailsMap.getStringValue("primary_mobile_number"), theOTP);

                isValidOTP = currDateTime.isBefore(expiryDateTime) && theOTP.equalsIgnoreCase(strProperOTP);
            }

            if (isValidOTP) {
                theUpdateLoginParamsMap.put("otp", null);
                theUpdateLoginParamsMap.put("otp_expiry_date", null);
                theUpdateLoginParamsMap.put("otp_attempts", 0);
                theUpdateLoginParamsMap.put("otp_auth_action", "NONE");
                theUpdateLoginParamsMap.put("otp_auth_action_valid_date", null);
                theUpdateLoginParamsMap.put("otp_auth_flag", null);
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
                }

                resultWrapper.setData(checkUserResultMap);

                return resultWrapper;
            } else {

                FlexicoreHashMap resultAuthMap = new FlexicoreHashMap();

                int otpAttempts = Integer.parseInt(mobileBankingMap.getStringValue("otp_attempts"));
                otpAttempts = otpAttempts + 1;


                String strTitle = "Incorrect Activation Code";
                String strDescription = "The activation code you entered is either incorrect or has expired. Please confirm the activation code and try again.";

                if (theOTPType == MAPPAPIConstants.OTP_TYPE.TRANSACTIONAL) {
                    strTitle = "Incorrect One Time Password";
                    strDescription = "You entered an incorrect/expired One Time Password";
                }

                USSDAPIConstants.Condition endSession = USSDAPIConstants.Condition.NO;

                resultAuthMap.putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INCORRECT_OTP)
                        .putValue("display_message", strDescription)
                        .putValue("title", strTitle);

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(resultAuthMap);

                String strFirstName = signatoryDetailsMap.getStringValue("full_name").trim();

                HashMap<String, String> hmMSGPlaceholders = new HashMap<>();
                hmMSGPlaceholders.put("[MOBILE_NUMBER]", signatoryDetailsMap.getStringValue("primary_mobile_number"));
                hmMSGPlaceholders.put("[OTP_ATTEMPTS]", String.valueOf(otpAttempts));
                //hmMSGPlaceholders.put("[FIRST_NAME]", strFirstName);
                hmMSGPlaceholders.put("[FIRST_NAME]", strFirstName);

                String strOTPAttemptParameters = SystemParameters.getParameter("MBANKING_AUTH_ATTEMPTS");

                HashMap<String, HashMap<String, String>> authenticationAttemptsAction = MBankingXMLFactory.getAuthenticationAttemptsAction(
                        otpAttempts,
                        hmMSGPlaceholders,
                        strOTPAttemptParameters,
                        MBankingConstants.AuthType.OTP);

                HashMap<String, String> currentAuthenticationAttemptsAction = authenticationAttemptsAction.get("CURRENT_ATTEMPT");
                HashMap<String, String> futureAuthenticationAttemptsAction = authenticationAttemptsAction.get("NEXT_ATTEMPT");

                //Check if action is needed
                if (!currentAuthenticationAttemptsAction.isEmpty()) {
                    String loginAction = currentAuthenticationAttemptsAction.get("ACTION");
                    String loginActionTag = currentAuthenticationAttemptsAction.get("NAME");
                    String resetOTP = currentAuthenticationAttemptsAction.get("RESET_OTP");

                    //Check action
                    switch (loginAction) {
                        case "SUSPEND": {
                            int loginActionDuration = Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION"));
                            String loginActionDurationUnit = currentAuthenticationAttemptsAction.get("UNIT");
                            loginActionDuration = DateTime.convertToSeconds(loginActionDuration, loginActionDurationUnit);
                            Date loginActionValidDate = DateTime.add(loginActionDuration, Calendar.SECOND);
                            String strLoginActionValidDate = DateTime.convertDateToDateString(loginActionValidDate);

                            if (resetOTP.equalsIgnoreCase("YES")) {
                                theUpdateLoginParamsMap.put("otp", null);
                                theUpdateLoginParamsMap.put("otp_expiry_date", null);
                            }

                            theUpdateLoginParamsMap.put("otp_attempts", otpAttempts);
                            theUpdateLoginParamsMap.put("otp_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("otp_auth_action_valid_date", strLoginActionValidDate);
                            theUpdateLoginParamsMap.put("otp_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                            String friendlyActionDuration = currentAuthenticationAttemptsAction.get("DURATION") + " " + loginActionDurationUnit;
                            if (Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION")) != 1)
                                friendlyActionDuration = friendlyActionDuration + "S";

                            strTitle = "Account Suspended";
                            strDescription = "Your " + AppConstants.strMobileBankingName + " account has been SUSPENDED for " + friendlyActionDuration + " due to many OTP Attempts." + getTrailerMessage();
                            endSession = USSDAPIConstants.Condition.YES;

                            break;
                        }
                        case "LOCK": {

                            if (resetOTP.equalsIgnoreCase("YES")) {
                                theUpdateLoginParamsMap.put("otp", null);
                                theUpdateLoginParamsMap.put("otp_expiry_date", null);
                            }

                            theUpdateLoginParamsMap.put("otp_attempts", otpAttempts);
                            theUpdateLoginParamsMap.put("otp_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("otp_auth_action_valid_date", null);
                            theUpdateLoginParamsMap.put("otp_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                            strTitle = "Account Locked";
                            strDescription = "Your " + AppConstants.strMobileBankingName + " account has been LOCKED due to many OTP Attempts." + getTrailerMessage();
                            endSession = USSDAPIConstants.Condition.YES;

                            break;
                        }
                        default: {
                            theUpdateLoginParamsMap.put("otp_attempts", otpAttempts);
                            theUpdateLoginParamsMap.put("otp_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("otp_auth_action_valid_date", null);
                            theUpdateLoginParamsMap.put("otp_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());
                            endSession = USSDAPIConstants.Condition.YES;
                            break;
                        }
                    }
                }

                String currentLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
                if (currentLoginAction == null) currentLoginAction = "NONE";

                if (!currentLoginAction.equals("LOCK")) {

                    if (!currentLoginAction.equals("SUSPEND")) {

                        //Check future action
                        if (!futureAuthenticationAttemptsAction.isEmpty()) {
                            String futureLoginAction = futureAuthenticationAttemptsAction.get("ACTION");
                            String futureLoginActionDurationUnit = futureAuthenticationAttemptsAction.get("UNIT");
                            String friendlyFutureActionDuration = futureAuthenticationAttemptsAction.get("DURATION") + " " + futureLoginActionDurationUnit;

                            String attemptsRemainingToFutureLoginAction = futureAuthenticationAttemptsAction.get("ATTEMPTS_REMAINING");

                            //Override Incorrect PIN message
                            if (futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")) {
                                int intFutureActionDuration = Integer.parseInt(futureAuthenticationAttemptsAction.get("DURATION"));
                                if (intFutureActionDuration != 1)
                                    friendlyFutureActionDuration = friendlyFutureActionDuration + "S";

                                int intAttemptsRemainingToFutureLoginAction = Integer.parseInt(attemptsRemainingToFutureLoginAction);
                                String strAttempts = "attempt";
                                if (intAttemptsRemainingToFutureLoginAction != 1) strAttempts = strAttempts + "s";

                                strTitle = ((theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) ? "Incorrect Activation Code" : "Incorrect One Time Password");

                                strDescription = "You have " + attemptsRemainingToFutureLoginAction + " " + strAttempts + " before your " + AppConstants.strMobileBankingName + " account is SUSPENDED for " + friendlyFutureActionDuration + ".";

                                endSession = USSDAPIConstants.Condition.NO;

                            } else if (futureLoginAction.equals("LOCK")) {
                                int intAttemptsRemainingToFutureLoginAction = Integer.parseInt(attemptsRemainingToFutureLoginAction);
                                String strAttempts = "attempt";
                                if (intAttemptsRemainingToFutureLoginAction != 1) strAttempts = strAttempts + "s";

                                strTitle = ((theOTPType == MAPPAPIConstants.OTP_TYPE.ACTIVATION) ? "Incorrect Activation Code" : "Incorrect One Time Password");
                                strDescription = "You have " + attemptsRemainingToFutureLoginAction + " " + strAttempts + " before your " + AppConstants.strMobileBankingName + " account is LOCKED.";

                                endSession = USSDAPIConstants.Condition.NO;
                            }
                        }
                    }
                } else {
                    String primaryEmailAddress = signatoryDetailsMap.getStringValue("primary_email_address");

                    if (primaryEmailAddress != null && !primaryEmailAddress.isBlank()) {

                        int finalOtpAttempts = otpAttempts;
                        new Thread(() -> {
                            String strEmail = EmailTemplates.mobileBankingAccountLockedTemplate();
                            strEmail = strEmail.replace("[FULL_NAME]", signatoryDetailsMap.getStringValue("full_name"));
                            strEmail = strEmail.replace("[ATTEMPT_TYPE]", "OTP");
                            strEmail = strEmail.replace("[ATTEMPTS]", String.valueOf(finalOtpAttempts));
                            strEmail = strEmail.replace("[PHONE_NUMBER]", signatoryDetailsMap.getStringValue("primary_mobile_number"));

                            EmailMessaging.sendEmail(primaryEmailAddress, "" + AppConstants.strMobileBankingName + " Account LOCKED", strEmail, "ACCOUNT_LOCKED");
                        }).start();
                    }
                }

                theUpdateLoginParamsMap.put("otp_attempts", otpAttempts);
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                resultAuthMap.putValue("end_session", endSession);
                resultAuthMap.putValue("display_message", strDescription);
                resultAuthMap.putValue("title", strTitle);

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultAuthMap
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                            .putValue("title", "Error");
                }

                resultAuthMap.putValue("mobile_register_details", mobileBankingMap);

                JvmManager.gc(checkUserResultMap, checkUserResultMapWrapper);

                return resultWrapper;
            }


        } catch (Exception e) {
            System.err.println("CBSAPI.validateOTP(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "Error"));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> isValidKYCDetails(String theReferenceKey, String theIdentifierType,
                                                                         String theIdentifier,
                                                                         String theDeviceIdentifierType,
                                                                         String theDeviceIdentifier, String thePrimaryIdentifierType,
                                                                         String thePrimaryIdentifier,
                                                                         USSDAPIConstants.MobileChannel theDevice) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                return checkUserResultMapWrapper;
            }

            FlexicoreHashMap signatoryMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            String primaryIdentityNo = signatoryMap.getStringValue("primary_identity_no");

            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            if (primaryIdentityNo.equalsIgnoreCase(thePrimaryIdentifier)) {

                theUpdateLoginParamsMap.put("kyc_attempts", 0);
                theUpdateLoginParamsMap.put("kyc_auth_action", "NONE");
                theUpdateLoginParamsMap.put("kyc_auth_action_valid_date", null);
                theUpdateLoginParamsMap.put("kyc_auth_flag", null);
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                            .putValue("title", "Error"));

                    return resultWrapper;
                }

                resultWrapper.setHasErrors(false);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.Condition.YES));

                return resultWrapper;

            } else {

                FlexicoreHashMap resultAuthMap = new FlexicoreHashMap();

                int kycAttempts = Integer.parseInt(mobileBankingMap.getStringValue("kyc_attempts"));
                kycAttempts = kycAttempts + 1;

                String strMessage;

                String strTitle = "Invalid ID Number";

                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                    strMessage = "{Incorrect ID Number Provided}\nEnter your National ID:";
                } else {
                    strMessage = "You have entered an incorrect National ID, please try again.";
                }

                USSDAPIConstants.Condition endSession = USSDAPIConstants.Condition.NO;

                resultAuthMap.putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INCORRECT_PIN)
                        .putValue("display_message", strMessage)
                        .putValue("title", strTitle);

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(resultAuthMap);

                String strFirstName = signatoryMap.getStringValue("full_name").trim().split("\\s")[0];

                HashMap<String, String> hmMSGPlaceholders = new HashMap<>();
                hmMSGPlaceholders.put("[MOBILE_NUMBER]", signatoryMap.getStringValue("primary_mobile_number"));
                hmMSGPlaceholders.put("[LOGIN_ATTEMPTS]", String.valueOf(kycAttempts));
                // hmMSGPlaceholders.put("[FIRST_NAME]", strFirstName);
                hmMSGPlaceholders.put("[FIRST_NAME]", "Member");

                String strPasswordAttemptParameters = SystemParameters.getParameter("MBANKING_AUTH_ATTEMPTS");

                HashMap<String, HashMap<String, String>> authenticationAttemptsAction = MBankingXMLFactory.getAuthenticationAttemptsAction(
                        kycAttempts,
                        hmMSGPlaceholders,
                        strPasswordAttemptParameters,
                        MBankingConstants.AuthType.PASSWORD);

                HashMap<String, String> currentAuthenticationAttemptsAction = authenticationAttemptsAction.get("CURRENT_ATTEMPT");
                HashMap<String, String> futureAuthenticationAttemptsAction = authenticationAttemptsAction.get("NEXT_ATTEMPT");

                // Check if action is needed
                if (!currentAuthenticationAttemptsAction.isEmpty()) {
                    String loginAction = currentAuthenticationAttemptsAction.get("ACTION");
                    String loginActionTag = currentAuthenticationAttemptsAction.get("NAME");

                    // Check action
                    switch (loginAction) {
                        case "SUSPEND": {
                            int loginActionDuration = Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION"));
                            String loginActionDurationUnit = currentAuthenticationAttemptsAction.get("UNIT");
                            loginActionDuration = DateTime.convertToSeconds(loginActionDuration, loginActionDurationUnit);
                            Date loginActionValidDate = DateTime.add(loginActionDuration, Calendar.SECOND);
                            String strLoginActionValidDate = DateTime.convertDateToDateString(loginActionValidDate);

                            theUpdateLoginParamsMap.put("kyc_attempts", kycAttempts);
                            theUpdateLoginParamsMap.put("kyc_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("kyc_auth_action_valid_date", strLoginActionValidDate);
                            theUpdateLoginParamsMap.put("kyc_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                            String friendlyActionDuration = currentAuthenticationAttemptsAction.get("DURATION") + " " + loginActionDurationUnit;
                            if (Integer.parseInt(currentAuthenticationAttemptsAction.get("DURATION")) != 1)
                                friendlyActionDuration = friendlyActionDuration + "S";

                            strTitle = "Account Suspended";
                            strMessage = "Your " + AppConstants.strMobileBankingName + " account has been SUSPENDED for " + friendlyActionDuration + " due to many National ID Attempts. Please try again later.";
                            endSession = USSDAPIConstants.Condition.YES;

                            break;
                        }
                        case "LOCK": {

                            theUpdateLoginParamsMap.put("kyc_attempts", kycAttempts);
                            theUpdateLoginParamsMap.put("kyc_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("kyc_auth_action_valid_date", null);
                            theUpdateLoginParamsMap.put("kyc_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                            strTitle = "Account Locked";
                            strMessage = "Your " + AppConstants.strMobileBankingName + " account has been LOCKED due to many National ID Attempts." + getTrailerMessage();
                            endSession = USSDAPIConstants.Condition.YES;

                            break;
                        }
                        default: {
                            theUpdateLoginParamsMap.put("kyc_attempts", kycAttempts);
                            theUpdateLoginParamsMap.put("kyc_auth_action", loginAction);
                            theUpdateLoginParamsMap.put("kyc_auth_action_valid_date", null);
                            theUpdateLoginParamsMap.put("kyc_auth_flag", loginActionTag);
                            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());
                            endSession = USSDAPIConstants.Condition.YES;
                            break;
                        }
                    }
                }

                String currentLoginAction = currentAuthenticationAttemptsAction.get("ACTION");
                if (currentLoginAction == null) currentLoginAction = "NONE";

                if (!currentLoginAction.equals("LOCK")) {

                    if (!currentLoginAction.equals("SUSPEND")) {

                        // Check future action
                        if (!futureAuthenticationAttemptsAction.isEmpty()) {
                            String futureLoginAction = futureAuthenticationAttemptsAction.get("ACTION");
                            String futureLoginActionDurationUnit = futureAuthenticationAttemptsAction.get("UNIT");
                            String friendlyFutureActionDuration = futureAuthenticationAttemptsAction.get("DURATION") + " " + futureLoginActionDurationUnit;

                            String attemptsRemainingToFutureLoginAction = futureAuthenticationAttemptsAction.get("ATTEMPTS_REMAINING");

                            // Override Incorrect PIN message
                            if (futureLoginAction.equals("SUSPEND") && !currentLoginAction.equals("SUSPEND")) {
                                int intFutureActionDuration = Integer.parseInt(futureAuthenticationAttemptsAction.get("DURATION"));
                                if (intFutureActionDuration != 1)
                                    friendlyFutureActionDuration = friendlyFutureActionDuration + "S";

                                int intAttemptsRemainingToFutureLoginAction = Integer.parseInt(attemptsRemainingToFutureLoginAction);
                                String strAttempts = "attempt";
                                if (intAttemptsRemainingToFutureLoginAction != 1) strAttempts = strAttempts + "s";

                                strTitle = "Invalid Credentials";

                                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                                    strMessage = "{Incorrect ID Number Provided}\nYou have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your " + AppConstants.strMobileBankingName + " account is SUSPENDED for " + friendlyFutureActionDuration + ".\nPlease enter your National ID:";
                                } else {
                                    strMessage = "You have " + attemptsRemainingToFutureLoginAction + " " + strAttempts + " before your " + AppConstants.strMobileBankingName + " account is SUSPENDED for " + friendlyFutureActionDuration + ".";
                                }
                                endSession = USSDAPIConstants.Condition.NO;

                            } else if (futureLoginAction.equals("LOCK")) {
                                int intAttemptsRemainingToFutureLoginAction = Integer.parseInt(attemptsRemainingToFutureLoginAction);
                                String strAttempts = "attempt";
                                if (intAttemptsRemainingToFutureLoginAction != 1) strAttempts = strAttempts + "s";

                                strTitle = "Invalid ID Number";
                                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                                    strMessage = "{Incorrect ID Number Provided}\nYou have " + attemptsRemainingToFutureLoginAction + " attempt(s) before your " + AppConstants.strMobileBankingName + " account is LOCKED.\nPlease enter your National ID:";

                                } else {
                                    strMessage = "You have " + attemptsRemainingToFutureLoginAction + " " + strAttempts + " before your " + AppConstants.strMobileBankingName + " account is LOCKED.";
                                }

                                endSession = USSDAPIConstants.Condition.NO;
                            }
                        }
                    }

                } else {

                    String primaryEmailAddress = signatoryMap.getStringValue("primary_email_address");

                    /*if (primaryEmailAddress != null && !primaryEmailAddress.isBlank()) {

                        int finalLoginAttempts = kycAttempts;
                        new Thread(() -> {
                            String strEmail = EmailTemplates.mobileBankingAccountLockedTemplate();
                            strEmail = strEmail.replace("[FULL_NAME]", signatoryMap.getStringValue("full_name"));
                            strEmail = strEmail.replace("[ATTEMPT_TYPE]", "National ID");
                            strEmail = strEmail.replace("[ATTEMPTS]", String.valueOf(finalLoginAttempts));
                            strEmail = strEmail.replace("[PHONE_NUMBER]", signatoryMap.getStringValue("primary_mobile_number"));

                            EmailMessaging.sendEmail(primaryEmailAddress, "" + AppConstants.strMobileBankingName + " Account LOCKED", strEmail, "ACCOUNT_LOCKED");
                        }).start();
                    }*/
                }

                theUpdateLoginParamsMap.put("kyc_attempts", kycAttempts);
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

                resultAuthMap.putValue("end_session", endSession);
                resultAuthMap.putValue("display_message", strMessage);
                resultAuthMap.putValue("title", strTitle);

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultAuthMap
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                            .putValue("title", "Error");

                }
            }

            return resultWrapper;

        } catch (Exception e) {
            System.err.println("CBSAPI.isValidKYCDetails(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> isCorrectPIN(String theReferenceKey, String theIdentifierType, String theIdentifier,
                                                                    String thePIN, String theDeviceIdentifierType,
                                                                    String theDeviceIdentifier, USSDAPIConstants.MobileChannel theDevice) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                return checkUserResultMapWrapper;
            }

            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            String strProperPIN = mobileBankingMap.getStringValue("pin");
            thePIN = MobileBankingCryptography.hashPIN(theIdentifier, thePIN);

            if (thePIN.equalsIgnoreCase(strProperPIN)) {
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.Condition.YES));

            } else {
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.Condition.NO));
            }

            return resultWrapper;

        } catch (Exception e) {
            System.err.println("CBSAPI.isCorrectPIN(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> changeUserPIN(String theReferenceKey, String theIdentifierType, String theIdentifier,
                                                                     String thePIN,
                                                                     String theNewPIN, String theDeviceIdentifierType,
                                                                     String theDeviceIdentifier, USSDAPIConstants.MobileChannel theDevice) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                return checkUserResultMapWrapper;
            }

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            String strProperPIN = mobileBankingMap.getStringValue("pin");
            thePIN = MobileBankingCryptography.hashPIN(signatoryDetailsMap.getStringValue("primary_mobile_number"), thePIN);
            if (thePIN.equalsIgnoreCase(strProperPIN)) {

                if (theNewPIN.equalsIgnoreCase(thePIN)) {

                    String strMessage;
                    if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                        strMessage = "Change PIN\n{Invalid New PIN}\nYour new PIN cannot be the same as your previous PIN.\nPlease enter your New PIN:";
                    } else {
                        strMessage = "Your new password cannot be the same as your previous password.";
                    }

                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.NO)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INVALID_NEW_PIN)
                            .putValue("display_message", strMessage)
                            .putValue("title", "Invalid New Password"))
                    ;

                    return resultWrapper;
                }


                String strNewPIN = MobileBankingCryptography.hashPIN(signatoryDetailsMap.getStringValue("primary_mobile_number"), theNewPIN);

                String previousPins = mobileBankingMap.getStringValueOrIfNull("previous_pins", "<PREVIOUS_PINS/>");

                try {
                    Document docPrevPasswords = XmlUtils.parseXml(previousPins);

                    Element elPassword = docPrevPasswords.createElement("PIN");
                    elPassword.setTextContent(strNewPIN);
                    docPrevPasswords.getDocumentElement().appendChild(elPassword);

                    previousPins = XmlUtils.convertXmlDocToStr(docPrevPasswords);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                theUpdateLoginParamsMap.put("previous_pins", previousPins);

                theUpdateLoginParamsMap.put("pin", strNewPIN);
                theUpdateLoginParamsMap.put("pin_set_date", DateTime.getCurrentDateTime());
                theUpdateLoginParamsMap.put("pin_status", "ACTIVE");
                theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

               /* String strPreviousPinStatus = mobileBankingMap.getStringValue("pin_status");
                if (strPreviousPinStatus.equalsIgnoreCase("RESET")) {
                    theUpdateLoginParamsMap.put("ussd_activation_kyc", "ENABLED");
                    theUpdateLoginParamsMap.put("ussd_activation_kyc_set_date", null);
                    theUpdateLoginParamsMap.put("app_activation_kyc", "ENABLED");
                    theUpdateLoginParamsMap.put("app_activation_kyc_set_date", null);
                }*/

                mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

                String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
                theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

                TransactionWrapper<?> updateWrapper = Repository.update(
                        StringRefs.SENTINEL,
                        TBL_MOBILE_BANKING_REGISTER,
                        theUpdateLoginParamsMap,
                        new FilterPredicate("mobile_register_id = :mobile_register_id"),
                        new FlexicoreHashMap()
                                .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
                );

                if (updateWrapper.hasErrors()) {
                    resultWrapper.setHasErrors(true);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.YES)
                            .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                            .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                            .putValue("title", "ERROR: Change Password"));
                } else {
                    resultWrapper.setData(checkUserResultMap);
                }

                return resultWrapper;
            } else {

                String strMessage;
                if (theDevice == USSDAPIConstants.MobileChannel.USSD) {
                    strMessage = "Change PIN\n{Please enter a valid current PIN}\nEnter your current PIN:";
                } else {
                    strMessage = "Incorrect password. Please enter a valid current password.";
                }

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(
                        new FlexicoreHashMap()
                                .putValue("end_session", USSDAPIConstants.Condition.NO)
                                .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.INCORRECT_PIN)
                                .putValue("display_message", strMessage)
                                .putValue("title", "Incorrect Current Password")
                );

                JvmManager.gc(checkUserResultMap, checkUserResultMapWrapper);
                return resultWrapper;
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.changeUserPIN(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "ERROR: Change Password"));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getCurrentUserDetails(String theReferenceKey, String theIdentifierType, String theIdentifier, String theDeviceIdentifierType, String theDeviceIdentifier) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            FilterPredicate signatoryFilterPredicate = null;
            FlexicoreHashMap queryArguments = new FlexicoreHashMap();

            if ("MSISDN".equals(theIdentifierType)) {
                signatoryFilterPredicate = new FilterPredicate("primary_mobile_number = :primary_mobile_number");
                queryArguments.addQueryArgument(":primary_mobile_number", theIdentifier);
            } else {
                signatoryFilterPredicate = new FilterPredicate("primary_identity_type = :primary_identity_type AND primary_identity_no = :primary_identity_no");
                queryArguments.addQueryArgument(":primary_identity_type", theIdentifierType);
                queryArguments.addQueryArgument(":primary_identity_no", theIdentifier);
            }

            TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                    SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES, signatoryFilterPredicate, queryArguments);

            if (signatoryDetailsWrapper.displayQueriesExecuted().hasErrors()) {
                signatoryDetailsWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);

                signatoryDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "An error occurred"));

                return signatoryDetailsWrapper;
            }

            FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

            if (signatoryDetailsMap == null || signatoryDetailsMap.isEmpty()) {
                signatoryDetailsWrapper.setHasErrors(true);
                signatoryDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.NOT_FOUND)
                        .putValue("display_message", "Sorry, you are not registered to use " + AppConstants.strMobileBankingName + "." + getTrailerMessage())
                        .putValue("title", "Account not found"));

                return signatoryDetailsWrapper;
            }

            String strSignatoryId = signatoryDetailsMap.getStringValue("signatory_id");

            TransactionWrapper<FlexicoreHashMap> mobileMappingDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                    SystemTables.TBL_MOBILE_BANKING_REGISTER,
                    new FilterPredicate("signatory_id = :signatory_id"),
                    new FlexicoreHashMap().addQueryArgument(":signatory_id", strSignatoryId));

            if (mobileMappingDetailsWrapper.hasErrors()) {
                mobileMappingDetailsWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);

                mobileMappingDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "An error occurred"));

                return mobileMappingDetailsWrapper;
            }

            FlexicoreHashMap mobileBankingDetailsMap = mobileMappingDetailsWrapper.getSingleRecord();

            if (mobileBankingDetailsMap == null || mobileBankingDetailsMap.isEmpty()) {
                mobileMappingDetailsWrapper.setHasErrors(true);
                mobileMappingDetailsWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.NOT_FOUND)
                        .putValue("display_message", "Sorry, you are not registered to use " + AppConstants.strMobileBankingName + "." + getTrailerMessage())
                        .putValue("title", "An error occurred"));

                return signatoryDetailsWrapper;
            }

            FlexicoreHashMap userMobileBankingDetailsMap = new FlexicoreHashMap();
            userMobileBankingDetailsMap.putValue("signatory_details", signatoryDetailsMap);
            userMobileBankingDetailsMap.putValue("mobile_register_details", mobileBankingDetailsMap);

            resultWrapper.setData(userMobileBankingDetailsMap);

        } catch (Exception e) {
            System.err.println("CBSAPI.getCurrentUserDetails(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "An error occurred"));

        }

        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> acceptTermsAndConditions(FlexicoreHashMap mobileBankingMap, USSDAPIConstants.MobileChannel theDevice) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            theUpdateLoginParamsMap.put("accepted_terms_and_conditions", "YES");
            theUpdateLoginParamsMap.put("channel_accepted_terms_and_conditions", theDevice.getValue());
            theUpdateLoginParamsMap.put("date_accepted_terms_and_conditions", DateTime.getCurrentDateTime());
            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

            mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

            String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
            theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

            TransactionWrapper<?> updateWrapper = Repository.update(
                    StringRefs.SENTINEL,
                    TBL_MOBILE_BANKING_REGISTER,
                    theUpdateLoginParamsMap,
                    new FilterPredicate("mobile_register_id = :mobile_register_id"),
                    new FlexicoreHashMap()
                            .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
            );

            if (updateWrapper.hasErrors()) {
                System.err.println("CBSAPI.acceptTermsAndConditions() - Update ERROR: " + updateWrapper.getErrors() + "\n");

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "An error occurred"));
            }

            return resultWrapper;

        } catch (Exception e) {
            System.err.println("CBSAPI.acceptTermsAndConditions(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "An error occurred"));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getSignatoryCustomersList(String theReferenceKey, String theIdentifierType, String theIdentifier, String theDeviceIdentifierType, String theDeviceIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                return checkUserResultMapWrapper;
            }

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            QueryBuilder queryBuilder = new QueryBuilder()
                    .select()
                    .selectColumn("cr.*,\n" +
                            "       mscm.mandates")
                    .from()
                    .joinPhrase(TBL_MOBILE_SIGNATORY_CUSTOMER_MAPPING + " mscm\n" +
                            "   LEFT JOIN " + TBL_CUSTOMER_REGISTER + " cr ON mscm.customer_register_id = cr.customer_register_id")
                    .where("mscm.mobile_register_id = :mobile_register_id");


            TransactionWrapper<FlexicoreArrayList> customersListWrapper = Repository.joinSelectQuery(
                    StringRefs.SENTINEL,
                    queryBuilder,
                    new FlexicoreHashMap().addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
            );

            if (customersListWrapper.hasErrors()) {
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            FlexicoreArrayList customersList = customersListWrapper.getData();

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", customersList));

            return resultWrapper;
        } catch (Exception e) {
            System.err.println("CBSAPI.getSignatoryCustomersList(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }


    public static String userCheck(String mobileNo, String iMSIIMEI, boolean uSSD, String sessionID) {
        String response = CBS_ERROR;

        String SOAPFunction = "UserCheck";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:UserCheck>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:iMSI_IMEI></sky:iMSI_IMEI>
                            <sky:uSSD></sky:uSSD>
                            <sky:sessionID></sky:sessionID>
                        </sky:UserCheck>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:iMSI_IMEI", iMSIIMEI);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:uSSD", String.valueOf(uSSD));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sessionID", sessionID);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.userCheck() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static XMLGregorianCalendar getUserLoginAttemptExpiry(String strMobileNumber, String type) {
        String response = CBS_ERROR;
        XMLGregorianCalendar xmlGregorianCalendar = null;

        String SOAPFunction = "GetUserLoginAttemptExpiry";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUserLoginAttemptExpiry>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:type></sky:type>
                        </sky:GetUserLoginAttemptExpiry>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:type", type);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return xmlGregorianCalendar;
    }

    public static String getUserLoginAttemptAction(String strMobileNumber, String type) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetUserLoginAttemptAction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUserLoginAttemptAction>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:type></sky:type>
                        </sky:GetUserLoginAttemptAction>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:type", type);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    //getUserLoginAttemptCount
    public static int getUserLoginAttemptCount(String strMobileNumber, String type) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetUserLoginAttemptCount";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetUserLoginAttemptCount>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:type></sky:type>
                        </sky:GetUserLoginAttemptCount>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:type", type);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Integer.parseInt(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return 0;
    }

    public static String updateAuthAttempts(String strMobileNumber, String type, int intUserLoginAttemptsCount, String strDescription,
                                            String strAction, XMLGregorianCalendar gcValidity, boolean clearValidity) {
        String response = CBS_ERROR;

        String SOAPFunction = "UpdateAuthAttempts";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:UpdateAuthAttempts>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:type></sky:type>
                            <sky:count></sky:count>
                            <sky:tag></sky:tag>
                            <sky:action></sky:action>
                            <sky:validity></sky:validity>
                            <sky:clearValidity></sky:clearValidity>
                        </sky:UpdateAuthAttempts>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:type", type);
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:count", String.valueOf(intUserLoginAttemptsCount));
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:tag", strDescription);
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:action", strAction);
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:validity", gcValidity.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:UpdateAuthAttempts/sky:clearValidity", String.valueOf(clearValidity));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/UpdateAuthAttempts_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.updateAuthAttempts() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String ussdLogin(String strMobileNumber, String strPIN, String strSIMID, boolean uSSD, String strUSSDSessionID) {
        String response = CBS_ERROR;

        String SOAPFunction = "USSDLogin";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:USSDLogin>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pIN></sky:pIN>
                            <sky:iMSI_IMEI></sky:iMSI_IMEI>
                            <sky:uSSD></sky:uSSD>
                            <sky:sessionID></sky:sessionID>
                        </sky:USSDLogin>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:USSDLogin/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:USSDLogin/sky:pIN", strPIN);
            requestXml.update("/x:Envelope/x:Body/sky:USSDLogin/sky:iMSI_IMEI", strSIMID);
            requestXml.update("/x:Envelope/x:Body/sky:USSDLogin/sky:uSSD", String.valueOf(uSSD));
            requestXml.update("/x:Envelope/x:Body/sky:USSDLogin/sky:sessionID", strUSSDSessionID);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/USSDLogin_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.ussdLogin() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String validateKYCdetails(String strMobileNumber, String strIDNo, String strNewPIN, String strPIN,
                                            String strSIMID, boolean uSSD) {
        String response = CBS_ERROR;

        String SOAPFunction = "ValidateKYCdetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ValidateKYCdetails>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:iDNo></sky:iDNo>
                            <sky:newPIN></sky:newPIN>
                            <sky:oTCPIN></sky:oTCPIN>
                            <sky:iMEI_IMSI></sky:iMEI_IMSI>
                            <sky:uSSD></sky:uSSD>
                        </sky:ValidateKYCdetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:iDNo", strIDNo);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:newPIN", strNewPIN);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:oTCPIN", strPIN);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:iMEI_IMSI", strSIMID);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:uSSD", String.valueOf(uSSD));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/ValidateKYCdetails_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.validateKYCdetails() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String validateKYCdetails(String strMobileNumber, String serviceNo, String strIDNo, String strNewPIN, String strPIN,
                                            String strSIMID, boolean uSSD) {
        String response = CBS_ERROR;

        String SOAPFunction = "ValidateKYCdetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ValidateKYCdetails>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:serviceNo></sky:serviceNo>
                            <sky:iDNo></sky:iDNo>
                            <sky:newPIN></sky:newPIN>
                            <sky:oTCPIN></sky:oTCPIN>
                            <sky:iMEI_IMSI></sky:iMEI_IMSI>
                            <sky:uSSD></sky:uSSD>
                        </sky:ValidateKYCdetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:serviceNo", serviceNo);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:iDNo", strIDNo);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:newPIN", strNewPIN);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:oTCPIN", strPIN);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:iMEI_IMSI", strSIMID);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateKYCdetails/sky:uSSD", String.valueOf(uSSD));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/ValidateKYCdetails_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.validateKYCdetails() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    //TODO:: CREATE THIS FUNCTION BELOW IN BC
    public static String validateKYCdetails_WithoutPIN(String strMobileNumber, String serviceNo, String strIDNo) {

        String response = CBS_ERROR;

        String SOAPFunction = "ValidateMobileKYCdetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ValidateMobileKYCdetails>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:serviceNo></sky:serviceNo>
                            <sky:iDNo></sky:iDNo>
                        </sky:ValidateMobileKYCdetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:ValidateMobileKYCdetails/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateMobileKYCdetails/sky:serviceNo", serviceNo);
            requestXml.update("/x:Envelope/x:Body/sky:ValidateMobileKYCdetails/sky:iDNo", strIDNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/ValidateMobileKYCdetails_Result/return_value");
            System.out.println("Processed Response     : " + response);

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI.validateKYCdetails() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String setNewPin(String strMobileNumber, String strPIN, String strNewPIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "SetNewPin";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:SetNewPin>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:oldPin></sky:oldPin>
                            <sky:newPin></sky:newPin>
                        </sky:SetNewPin>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:oldPin", strPIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:newPin", strNewPIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;

    }

    //TODO:: CREATE THIS FUNCTION BELOW IN BC
    public static String resetPin(String strMobileNumber, String strNewPIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "MobilePinReset";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:MobilePinReset>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:newPin></sky:newPin>
                        </sky:MobilePinReset>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:newPin", strNewPIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;

    }

    public static String getSavingsAccountList(String strMobileNumber, boolean blWithdrawable, String strAccountCategory) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetSavingsAccountList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetSavingsAccountList>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:withdrawable></sky:withdrawable>
                            <sky:type></sky:type>
                        </sky:GetSavingsAccountList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:withdrawable", String.valueOf(blWithdrawable));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:type", strAccountCategory);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> getCustomerAccounts(String strRequestingMobileNumber,
                                                                           String theIdentifierType,
                                                                           String theIdentifier,
                                                                           String theAccountType) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreArrayList> customerAccountsListWrapper = DeSaccoCBS.getMemberDepositAccounts(theIdentifierType, theIdentifier, theAccountType, false);

            if (customerAccountsListWrapper.hasErrors() && customerAccountsListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCustomerAccounts() - " + customerAccountsListWrapper.getErrors() + "\n" + customerAccountsListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(customerAccountsListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (customerAccountsListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, the customer accounts could not be found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, the customer accounts could not be found." + getTrailerMessage()));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", customerAccountsListWrapper.getData())
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.getCustomerAccounts(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.addError(e.getMessage());
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getSavingsAccountList_V2(String strRequestingMobileNumber, String theIdentifierType, String theIdentifier, String theAccountType) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreArrayList> customerAccountsListWrapper = DeSaccoCBS.getMemberDepositAccounts(theIdentifierType, theIdentifier, theAccountType, false);

            if (customerAccountsListWrapper.hasErrors() && customerAccountsListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCustomerAccounts() - " + customerAccountsListWrapper.getErrors() + "\n" + customerAccountsListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(customerAccountsListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (customerAccountsListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, the customer accounts could not be found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, the customer accounts could not be found." + getTrailerMessage()));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", customerAccountsListWrapper.getData())
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.getCustomerAccounts(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.addError(e.getMessage());
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public static String getSavingsAccountList(String strMobileNumber, String strAccountCategory) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetSavingsAccountList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetSavingsAccountList>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:category></sky:category>
                        </sky:GetSavingsAccountList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:category", strAccountCategory);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getAccountTransferRecipientXML(String strCriteria, String strSource) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAccountTransferRecipientXML";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAccountTransferRecipientXML>
                            <sky:criteria></sky:criteria>
                            <sky:source></sky:source>
                        </sky:GetAccountTransferRecipientXML>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:criteria", strCriteria);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:source", String.valueOf(strSource));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }



    public static String accountBalanceEnquiry(String strEntryCode, String strUSSDSessionID, String strMobileNumber,
                                               String strPIN, String strAccountType) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountBalanceEnquiry";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountBalanceEnquiry>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:accType></sky:accType>
                        </sky:AccountBalanceEnquiry>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", strEntryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", strUSSDSessionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", strPIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accType", strAccountType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }


    public static TransactionWrapper<FlexicoreHashMap> loanBalanceEnquiry(String strRequestingMobileNumber,
                                                                          String theIdentifierType,
                                                                          String theIdentifier,
                                                                          String theDeviceIdentifierType,
                                                                          String theDeviceIdentifier,
                                                                          String theAccountNumber,
                                                                          String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                System.err.println("=> Check User Failed for checkUser(..., " + theIdentifierType + ", " + theIdentifier + ", " + theDeviceIdentifierType + ", " + theDeviceIdentifier + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());

                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> accountBalanceWrapper = DeSaccoCBS.getLoanAccountBalance(theIdentifierType, theIdentifier, theAccountNumber, theSourceApplication);

            if (accountBalanceWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanAccountBalance() - " + accountBalanceWrapper.getErrors() + "\n" + accountBalanceWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", accountBalanceWrapper.getErrors() + " - " + accountBalanceWrapper.getMessages())
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            FlexicoreHashMap accountBalanceResultMap = accountBalanceWrapper.getSingleRecord();
            String requestStatus = accountBalanceResultMap.getStringValue("request_status");

            FlexicoreHashMap accountBalResponseMap = accountBalanceResultMap.getFlexicoreHashMap("response_payload");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanAccountBalance(" + theAccountNumber + ")");
                accountBalanceResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Loan Balance Enquiry request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Loan Balance Enquiry request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + accountBalResponseMap.getStringValue("error_message") + " - " + accountBalResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanAccountBalance(" + theAccountNumber + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + accountBalResponseMap.getStringValue("error_message") + " - " + accountBalResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strAccountName = accountBalResponseMap.getStringValueOrIfNull("loan_type_name", "").trim();
            String strAccountSerialNumber = accountBalResponseMap.getStringValueOrIfNull("loan_serial_number", "").trim();
            String strAccountBalance = accountBalResponseMap.getStringValueOrIfNull("loan_balance", "0").trim();
            String strAccountInterestAmount = accountBalResponseMap.getStringValueOrIfNull("interest_amount", "0").trim();

            double dblLoanBalance = Double.parseDouble(strAccountBalance);
            double dblLoanInterestBalance = Double.parseDouble(strAccountInterestAmount);
            double dblTotalLoanBalance = dblLoanBalance + dblLoanInterestBalance;

            String strTotalLoanBalance = Utils.formatDouble(dblLoanBalance, "#,##0.00");

            /**
             * Dear [MEMBER_NAME],
             * Your balance for [ACCOUNT_LABEL], A/C No.: [ACCOUNT_NO] is KES: [AMOUNT]. Charges: KES [AMOUNT].
             * Ref: [REFERENCE] Date: [DATE]
             *
             * You can always view your full statement for free on USSD and APP.
             */

            HashMap<String, String> memberDetails = CBSAPI.getMemberDetails(strRequestingMobileNumber);
            String strFullName = memberDetails.get("full_name");
            String firstName = strFullName.split(" ")[1];

            String strMSG = "Dear %s, Your Balance for %s A/C No.: %s is KES %s";
            strMSG = String.format(strMSG, firstName, strAccountName, strAccountSerialNumber,  strTotalLoanBalance);

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            accountBalanceResultMap
                    .putValue("msg_object", cbsMSG)
                    .putValue("loan_balance", accountBalResponseMap.getStringValueOrIfNull("loan_balance", "0").trim())
                    .putValue("interest_amount", accountBalResponseMap.getStringValueOrIfNull("interest_amount", "0").trim())
                    .putValue("loan_name", strAccountName)
                    .putValue("loan_serial_number", strAccountSerialNumber);

            return accountBalanceWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.loanBalanceEnquiry(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Loan Balance Enquiry request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())
            );
        }
        return resultWrapper;
    }

    public static String accountBalanceEnquiry_V2(String strEntryCode, String strUSSDSessionID, String strMobileNumber, String strPIN, String strAccountType) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountBalanceEnquiry";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountBalanceEnquiry>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:accType></sky:accType>
                        </sky:AccountBalanceEnquiry>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", strEntryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", strUSSDSessionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", strPIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accType", strAccountType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> accountBalanceEnquirySINGLE(String strRequestingMobileNumber,
                                                                                   String theIdentifierType,
                                                                                   String theIdentifier,
                                                                                   String theDeviceIdentifierType,
                                                                                   String theDeviceIdentifier,
                                                                                   String theAccountNumber,
                                                                                   String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                System.err.println("=> Check User Failed for checkUser(..., " + theIdentifierType + ", " + theIdentifier + ", " + theDeviceIdentifierType + ", " + theDeviceIdentifier + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());

                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> accountBalanceWrapper = DeSaccoCBS.getDepositAccountBalance(theIdentifierType, theIdentifier, theAccountNumber, theSourceApplication);

            if (accountBalanceWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountBalance() - " + accountBalanceWrapper.getErrors() + "\n" + accountBalanceWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", accountBalanceWrapper.getErrors() + " - " + accountBalanceWrapper.getMessages())
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            FlexicoreHashMap accountBalanceResultMap = accountBalanceWrapper.getSingleRecord();
            String requestStatus = accountBalanceResultMap.getStringValue("request_status");

            FlexicoreHashMap accountBalResponseMap = accountBalanceResultMap.getFlexicoreHashMap("response_payload");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountBalance(" + theAccountNumber + ")");
                accountBalanceResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Balance Enquiry request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Balance Enquiry request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + accountBalResponseMap.getStringValue("error_message") + " - " + accountBalResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountBalance(" + theAccountNumber + ")");
                accountBalanceResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + accountBalResponseMap.getStringValue("error_message") + " - " + accountBalResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strAccountName = accountBalResponseMap.getStringValueOrIfNull("account_label", "").trim();
            String strAccountType = accountBalResponseMap.getStringValueOrIfNull("account_type", "").trim();
            String strAccountNumber = accountBalResponseMap.getStringValueOrIfNull("account_number", "").trim();
            String strAccountWithdrawableBalance = accountBalResponseMap.getStringValueOrIfNull("account_balance", "0").trim();
            String strAccountBookBalance = accountBalResponseMap.getStringValueOrIfNull("book_balance", "0").trim();
            strAccountWithdrawableBalance = Utils.formatDouble(strAccountWithdrawableBalance, "#,##0.00");
            strAccountBookBalance = Utils.formatDouble(strAccountBookBalance, "#,##0.00");

            /**
             * Dear MIDURI,
             * Your Current Account Balance as at 16-02-2022 19:30 is:
             * Available Bal: 0.
             * Book Balance: 0.
             * REF: 11XMQ1U6
             */

            // String strAccountBalance = strAccountType.equalsIgnoreCase("FOSA") ? strAccountWithdrawableBalance : strAccountBookBalance;

            HashMap<String, String> memberDetails = CBSAPI.getMemberDetails(strRequestingMobileNumber);
            String strFullName = memberDetails.get("full_name");
            String firstName = strFullName.split(" ")[1];
            String strTimestamp = DateTime.getCurrentDate("dd-MM-yyyy HH:mm");
            String strMSG = "Dear " + firstName + ",\nYour " + strAccountName + " Balance as at "  + strTimestamp + " is:\nAvailable Bal: " + strAccountWithdrawableBalance + ".\nBook Balance: " + strAccountBookBalance +".";

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            accountBalResponseMap.putValue("msg_object", cbsMSG);

            accountBalanceWrapper.setData(accountBalResponseMap);

            return accountBalanceWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.accountBalanceEnquiry(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())
            );
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiry(String strRequestingMobileNumber,
                                                                             String theIdentifierType,
                                                                             String theIdentifier,
                                                                             String theDeviceIdentifierType,
                                                                             String theDeviceIdentifier,
                                                                             String theAccountType) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                System.err.println("=> Check User Failed for checkUser(..., " + theIdentifierType + ", " + theIdentifier + ", " + theDeviceIdentifierType + ", " + theDeviceIdentifier + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());

                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreArrayList> customerAccountsListWrapper = DeSaccoCBS.getMemberDepositAccounts(theIdentifierType, theIdentifier, theAccountType, false);

            if (customerAccountsListWrapper.hasErrors() && customerAccountsListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCustomerAccountsBALANCE_EQUIRY() - " + customerAccountsListWrapper.getErrors() + "\n" + customerAccountsListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(customerAccountsListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (customerAccountsListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, the member accounts could not be found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, the member accounts could not be found." + getTrailerMessage()));

                return resultWrapper;
            }

            FlexicoreArrayList customersList = customerAccountsListWrapper.getData();

            String strAccountDisplay = (theAccountType.equalsIgnoreCase("ALL") ? "" : theAccountType);
            if (strAccountDisplay.equalsIgnoreCase("BOSA")) {
                strAccountDisplay = "Shares And Deposits Accounts";
            }

            StringBuilder accountsMSGBuilder = new StringBuilder("Dear member, your " + strAccountDisplay + " Account Balances are:\n");
            for (FlexicoreHashMap customerAccount : customersList) {
                String strAccountName = customerAccount.getStringValueOrIfNull("account_type_name", "").trim();
                String strAccountNumber = customerAccount.getStringValueOrIfNull("account_number", "").trim();
                String strAccountWithdrawableBalance = customerAccount.getStringValueOrIfNull("account_balance", "0").trim();

                double dbAccountBalance = Utils.stringToDouble(strAccountWithdrawableBalance.replace("-", ""));

                if (dbAccountBalance <= 0) {
                    continue;
                }

                strAccountWithdrawableBalance = Utils.formatDouble(strAccountWithdrawableBalance, "#,##0.00");

                accountsMSGBuilder.append("Name: " + strAccountName + " - " + strAccountNumber + "\n");
                accountsMSGBuilder.append("Bal : KES " + strAccountWithdrawableBalance + "\n\n");
            }

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(accountsMSGBuilder.toString());
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", customersList)
                    .putValue("msg_object", cbsMSG));

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.accountBalanceEnquiry(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Balance Enquiry request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Balance Enquiry request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())
            );
        }
        return resultWrapper;
    }

    public static String loanMiniStatement(String strEntryCode, String strTransactionID, int intMaxNumberRows,
                                           String loanType, String strMobileNumber, String strPIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "LoanMiniStatement";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:LoanMiniStatement>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:maxNumberRows></sky:maxNumberRows>
                            <sky:loanType></sky:loanType>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pin></sky:pin>
                        </sky:LoanMiniStatement>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", strEntryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", strTransactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:maxNumberRows", String.valueOf(intMaxNumberRows));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pin", strPIN);


            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> accountMiniStatement(String strRequestingMobileNumber,
                                                                            String theIdentifierType,
                                                                            String theIdentifier,
                                                                            String theDeviceIdentifierType,
                                                                            String theDeviceIdentifier,
                                                                            String theAccountNumber,
                                                                            String theNumberOfEntries) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Mini-Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());

                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> accountMiniStatementListWrapper = DeSaccoCBS.getAccountMiniStatement(theIdentifierType, theIdentifier, theAccountNumber, theNumberOfEntries);
            //TransactionWrapper<FlexicoreHashMap> accountMiniStatementListWrapper = ProfitsCBS.getAccountMiniStatementTEMP(theAccountNumber);

            if (accountMiniStatementListWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountMiniStatement() - " + accountMiniStatementListWrapper.getErrors() + "\n" + accountMiniStatementListWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Mini-Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Mini-Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", accountMiniStatementListWrapper.getErrors() + " - " + accountMiniStatementListWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap accountMiniStatementMap = accountMiniStatementListWrapper.getSingleRecord();
            FlexicoreHashMap miniStatementResponseMap = accountMiniStatementMap.getFlexicoreHashMap("response_payload");

            String requestStatus = accountMiniStatementMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountMiniStatement(" + theAccountNumber + ")");
                accountMiniStatementMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Account Mini-Statement request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Account Mini-Statement request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + miniStatementResponseMap.getStringValue("error_message") + " - " + miniStatementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountMiniStatement(" + theAccountNumber + ")");
                accountMiniStatementMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Account Mini-Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Account Mini-Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + miniStatementResponseMap.getStringValue("error_message") + " - " + miniStatementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strAccountName = miniStatementResponseMap.getStringValueOrIfNull("account_name", "").trim();

            String strMSG = "Dear member, your Mini-Statement request has FAILED. Please try again later.";

            FlexicoreArrayList allTransactionsList = miniStatementResponseMap.getFlexicoreArrayList("transactions");
            if (allTransactionsList == null || allTransactionsList.isEmpty()) {
                strMSG = "Dear member, your Mini-Statement request has FAILED. No transaction(s) found for account " + strAccountName + "-" + theAccountNumber + ".";
            } else {

                StringBuilder miniStatementMsg = new StringBuilder("Dear member, your account " + strAccountName + "-" + theAccountNumber + " Mini-Statement:\n");

                for (FlexicoreHashMap transactionMap : allTransactionsList) {

                    String strMSGTransactionReference = transactionMap.getStringValueOrIfNull("transaction_reference", "").trim();
                    String strMSGTransactionDate = transactionMap.getStringValueOrIfNull("transaction_date", "").trim();
                    String strMSGTransactionTime = transactionMap.getStringValueOrIfNull("transaction_time", "").trim();

                    String strMSGRunningBalance = transactionMap.getStringValue("running_balance").trim();
                    String strMSGTransactionAmount = transactionMap.getStringValue("transaction_amount").trim();
                    String strMSGTransactionComments = transactionMap.getStringValueOrIfNull("transaction_description", "").trim();

                    strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");

                    String strMSGFormattedTransactionAmount = Utils.formatDouble(strMSGTransactionAmount, "#,##0.00");
                    String strMSGFormattedRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                    String strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGTransactionDate, "yyyy-MM-dd", "dd-MMM-yyyy");
                    strMSGFormattedTransactionDateTime = strMSGFormattedTransactionDateTime + " " + strMSGTransactionTime;

                    miniStatementMsg.append("Ref: ").append(strMSGTransactionReference).append("\n");
                    miniStatementMsg.append("Date: ").append(strMSGFormattedTransactionDateTime).append("\n");
                    miniStatementMsg.append("Amnt: ").append(strMSGFormattedTransactionAmount).append("\n");
                    miniStatementMsg.append("Descr: ").append(strMSGTransactionComments).append("\n");
                    miniStatementMsg.append("Run. Bal: KES ").append(strMSGFormattedRunningBalance).append("\n\n");
                }

                strMSG = miniStatementMsg.toString();
            }

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", allTransactionsList)
                    .putValue("account_name", miniStatementResponseMap.getStringValueOrIfNull("account_name", "").trim())
                    .putValue("account_holder", miniStatementResponseMap.getStringValueOrIfNull("account_name", "").trim())
                    .putValue("account_available_balance", miniStatementResponseMap.getStringValue("available_balance").trim())
                    .putValue("msg_object", cbsMSG));

            return resultWrapper;
        } catch (Exception e) {

            System.err.println(strRequestingMobileNumber + " => CBSAPI.accountMiniStatement(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Mini Statement request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Mini Statement request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())

            );
        }
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> getLoanMiniStatement(String strRequestingMobileNumber,
                                                                            String theIdentifierType,
                                                                            String theIdentifier,
                                                                            String theDeviceIdentifierType,
                                                                            String theDeviceIdentifier,
                                                                            String theLoanSerialNumber,
                                                                            String theNumberOfEntries) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> accountStatementListWrapper = DeSaccoCBS.getLoanMiniStatement(theIdentifierType, theIdentifier, theLoanSerialNumber,
                    theNumberOfEntries);

            if (accountStatementListWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanMiniStatement() - " + accountStatementListWrapper.getErrors() + "\n" + accountStatementListWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", accountStatementListWrapper.getErrors() + " - " + accountStatementListWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap accountStatementMap = accountStatementListWrapper.getSingleRecord();
            FlexicoreHashMap statementResponseMap = accountStatementMap.getFlexicoreHashMap("response_payload");

            String requestStatus = accountStatementMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanMiniStatement(" + theLoanSerialNumber + ")");
                accountStatementMap.printRecordVerticalLabelled();


                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Loan Mini-Statement request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Loan Mini-Statement request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + statementResponseMap.getStringValue("error_message") + " - " + statementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanMiniStatement(" + theLoanSerialNumber + ")");
                accountStatementMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + statementResponseMap.getStringValue("error_message") + " - " + statementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strMSG = "Dear member, your Mini-Statement request has FAILED. Please try again later.";

            FlexicoreArrayList allTransactionsList = statementResponseMap.getFlexicoreArrayList("transactions");
            if (allTransactionsList == null || allTransactionsList.isEmpty()) {
                strMSG = "Dear member, your Loan Mini-Statement request has FAILED. No transaction(s) found for account " + theLoanSerialNumber + ".";
            } else {

                StringBuilder miniStatementMsg = new StringBuilder("Dear member, your Loan " + theLoanSerialNumber + " Statement:\n");

                for (FlexicoreHashMap transactionMap : allTransactionsList) {

                    String strMSGTransactionReference = transactionMap.getStringValueOrIfNull("transaction_reference", "").trim();
                    String strMSGTransactionDate = transactionMap.getStringValueOrIfNull("transaction_date", "").trim();
                    String strMSGTransactionTime = transactionMap.getStringValueOrIfNull("transaction_time", "").trim();
                    String strTransactionType = transactionMap.getStringValueOrIfNull("transaction_type", "").trim();

                    String strMSGRunningBalance = transactionMap.getStringValue("running_balance").trim();
                    String strMSGTransactionAmount = transactionMap.getStringValue("transaction_amount").trim();
                    String strMSGTransactionComments = transactionMap.getStringValueOrIfNull("transaction_description", "").trim();

                    strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");

                    if (strTransactionType.equalsIgnoreCase("CREDIT")) {
                        strMSGTransactionAmount = "-" + strMSGTransactionAmount;
                    }

                    transactionMap.putValue("transaction_amount", strMSGTransactionAmount);


                    //strMSGRunningBalance = strMSGRunningBalance.replace("-", "");
                    //strMSGIntRunningBalance = strMSGIntRunningBalance.replace("-", "");

                    //double totalPayable = Double.parseDouble(strMSGRunningBalance) + Double.parseDouble(strMSGIntRunningBalance);
                    //String strFormattedLoanTotalPayableAmount = Utils.formatDouble(String.valueOf(totalPayable), "#,##0.00");

                    String strMSGFormattedTransactionAmount = Utils.formatDouble(strMSGTransactionAmount, "#,##0.00");
                    String strMSGFormattedRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                    String strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGTransactionDate, "yyyy-MM-dd", "dd-MMM-yyyy");
                    strMSGFormattedTransactionDateTime = strMSGFormattedTransactionDateTime + " " + strMSGTransactionTime;

                    //String strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

                    transactionMap.putValue("transaction_date_time", strMSGFormattedTransactionDateTime);

                    miniStatementMsg.append("Ref: ").append(strMSGTransactionReference).append("\n");
                    miniStatementMsg.append("Date: ").append(strMSGFormattedTransactionDateTime).append("\n");
                    miniStatementMsg.append("Amnt: ").append(strMSGFormattedTransactionAmount).append("\n");
                    miniStatementMsg.append("Descr: ").append(strMSGTransactionComments).append("\n");
                    miniStatementMsg.append("Run. Bal: KES ").append(strMSGFormattedRunningBalance).append("\n\n");
                }

                strMSG = miniStatementMsg.toString();
            }

            String strRunningBalance = statementResponseMap.getStringValue("loan_balance").trim();
//            String strIntRunningBalance = statementResponseMap.getStringValue("interest_amount").trim();

            //strRunningBalance = strRunningBalance.replace("-", "");
            //strIntRunningBalance = strIntRunningBalance.replace("-", "");

//            double totalPayable = Double.parseDouble(strRunningBalance) + Double.parseDouble(strIntRunningBalance);

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getFlexicoreHashMap("signatory_details");

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", allTransactionsList)
                    .putValue("account_name", statementResponseMap.getStringValueOrIfNull("loan_name", "").trim())
                    .putValue("account_holder", signatoryDetailsMap.getStringValueOrIfNull("full_name", "").trim())
                    .putValue("account_available_balance", strRunningBalance)
                    .putValue("msg_object", cbsMSG));

            return resultWrapper;
        } catch (Exception e) {

            System.err.println(strRequestingMobileNumber + " => CBSAPI.getLoanMiniStatement(" + theLoanSerialNumber + "): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Loan Mini-Statement request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())
            );
        }
        return resultWrapper;
    }

    public static String accountMiniStatement(String strEntryCode, String strTransactionID, int intMaxNumberRows,
                                              String strStatementAccount, String strMobileNumber, String strPIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountMiniStatement";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountMiniStatement>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:maxNumberRows></sky:maxNumberRows>
                            <sky:statementAccount></sky:statementAccount>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pin></sky:pin>
                        </sky:AccountMiniStatement>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", strEntryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", strTransactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:maxNumberRows", String.valueOf(intMaxNumberRows));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:statementAccount", strStatementAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", strMobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pin", strPIN);


            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> accountFullStatement(String strRequestingMobileNumber,
                                                                            String theIdentifierType,
                                                                            String theIdentifier,
                                                                            String theDeviceIdentifierType,
                                                                            String theDeviceIdentifier,
                                                                            String theAccountNumber,
                                                                            String theNumberOfEntries,
                                                                            String theStartDate,
                                                                            String theEndDate) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Account Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> accountFullStatementListWrapper = DeSaccoCBS.getAccountFullStatement(theIdentifierType, theIdentifier, theAccountNumber,
                    theNumberOfEntries, theStartDate, theEndDate);
            //TransactionWrapper<FlexicoreHashMap> accountFullStatementListWrapper = ProfitsCBS.getAccountMiniStatementTEMP(theAccountNumber);

            if (accountFullStatementListWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountFullStatement() - " + accountFullStatementListWrapper.getErrors() + "\n" + accountFullStatementListWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Account Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Account Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", accountFullStatementListWrapper.getErrors() + " - " + accountFullStatementListWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap accountMiniStatementMap = accountFullStatementListWrapper.getSingleRecord();
            FlexicoreHashMap miniStatementResponseMap = accountMiniStatementMap.getFlexicoreHashMap("response_payload");


            String requestStatus = accountMiniStatementMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountFullStatement(" + theAccountNumber + ")");
                accountMiniStatementMap.printRecordVerticalLabelled();


                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Account Statement request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Account Statement request.")
                        .putValue("cbs_api_error_message", requestStatus + ": " + miniStatementResponseMap.getStringValue("error_message") + " - " + miniStatementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountFullStatement(" + theAccountNumber + ")");
                accountMiniStatementMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Account Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Account Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + miniStatementResponseMap.getStringValue("error_message") + " - " + miniStatementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strAccountName = miniStatementResponseMap.getStringValueOrIfNull("account_name", "").trim();


            String strMSG = "Dear member, your Mini-Statement request has FAILED. Please try again later.";

            FlexicoreArrayList allTransactionsList = miniStatementResponseMap.getFlexicoreArrayList("transactions");
            if (allTransactionsList == null || allTransactionsList.isEmpty()) {
                strMSG = "Dear member, your Statement request has FAILED. No transaction(s) found for account " + strAccountName + "-" + theAccountNumber + ".";
            } else {

                StringBuilder miniStatementMsg = new StringBuilder("Dear member, your account " + strAccountName + "-" + theAccountNumber + " Mini-Statement:\n");

                for (FlexicoreHashMap transactionMap : allTransactionsList) {

                    String strMSGTransactionReference = transactionMap.getStringValueOrIfNull("transaction_reference", "").trim();
                    String strMSGTransactionDate = transactionMap.getStringValueOrIfNull("transaction_date", "").trim();
                    String strMSGTransactionTime = transactionMap.getStringValueOrIfNull("transaction_time", "").trim();
                    String strTransactionType = transactionMap.getStringValueOrIfNull("transaction_type", "").trim();

                    String strMSGRunningBalance = transactionMap.getStringValue("running_balance").trim();
                    String strMSGTransactionAmount = transactionMap.getStringValue("transaction_amount").trim();
                    String strMSGTransactionComments = transactionMap.getStringValueOrIfNull("transaction_description", "").trim();

                    strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");

                    if (strTransactionType.equalsIgnoreCase("DEBIT")) {
                        strMSGTransactionAmount = "-" + strMSGTransactionAmount;
                    }

                    transactionMap.putValue("transaction_amount", strMSGTransactionAmount);

                    String strMSGFormattedTransactionAmount = Utils.formatDouble(strMSGTransactionAmount, "#,##0.00");
                    String strMSGFormattedRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                    String strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGTransactionDate, "yyyy-MM-dd", "dd-MMM-yyyy");
                    strMSGFormattedTransactionDateTime = strMSGFormattedTransactionDateTime + " " + strMSGTransactionTime;

                    transactionMap.putValue("transaction_date_time", strMSGFormattedTransactionDateTime);

                    miniStatementMsg.append("Ref: ").append(strMSGTransactionReference).append("\n");
                    miniStatementMsg.append("Date: ").append(strMSGFormattedTransactionDateTime).append("\n");
                    miniStatementMsg.append("Amnt: ").append(strMSGFormattedTransactionAmount).append("\n");
                    miniStatementMsg.append("Descr: ").append(strMSGTransactionComments).append("\n");
                    miniStatementMsg.append("Run. Bal: KES ").append(strMSGFormattedRunningBalance).append("\n\n");
                }

                strMSG = miniStatementMsg.toString();
            }

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", allTransactionsList)
                    .putValue("account_name", miniStatementResponseMap.getStringValueOrIfNull("account_name", "").trim())
                    .putValue("account_holder", miniStatementResponseMap.getStringValueOrIfNull("account_name", "").trim())
                    .putValue("account_available_balance", miniStatementResponseMap.getStringValue("available_balance").trim())
                    .putValue("msg_object", cbsMSG));

            return resultWrapper;
        } catch (Exception e) {

            System.err.println(strRequestingMobileNumber + " => CBSAPI.getAccountFullStatement(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Account Statement request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Account Statement request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())
            );
        }
        return resultWrapper;
    }


    public static String insertMpesaTransaction(String entryCode, String transactionID, XMLGregorianCalendar transactionDate,
                                                String transaction, String description, String accountNo, BigDecimal amount,
                                                String phoneNo, String pIN, String requestApplication, String requestCorrelationID,
                                                String sourceApplication, String destinationAcc, String destinationName,
                                                String destinationOrg, String clientPhoneNumber, String customerName) {
        String response = CBS_ERROR;

        String SOAPFunction = "InsertMpesaTransaction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:InsertMpesaTransaction>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:transactionDate></sky:transactionDate>
                            <sky:transaction></sky:transaction>
                            <sky:description></sky:description>
                            <sky:accountNo></sky:accountNo>
                            <sky:amount></sky:amount>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:requestApplication></sky:requestApplication>
                            <sky:requestCorrelationID></sky:requestCorrelationID>
                            <sky:sourceApplication></sky:sourceApplication>
                            <sky:destinationAcc></sky:destinationAcc>
                            <sky:destinationName></sky:destinationName>
                            <sky:destinationOrg></sky:destinationOrg>
                            <sky:clientPhoneNumber></sky:clientPhoneNumber>
                            <sky:customerName></sky:customerName>
                        </sky:InsertMpesaTransaction>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionDate", transactionDate.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transaction", transaction);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:description", description);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:amount", amount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:requestApplication", requestApplication);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:requestCorrelationID", requestCorrelationID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sourceApplication", sourceApplication);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationAcc", destinationAcc);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationName", destinationName);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationOrg", destinationOrg);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:clientPhoneNumber", clientPhoneNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:customerName", customerName);


            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String insertMpesaTransaction(String entryCode, String transactionID, String transaction, String description,
                                                String accountNo, BigDecimal amount, String phoneNo, String pIN,
                                                String requestApplication, String requestCorrelationID, String sourceApplication) {
        String response = CBS_ERROR;

        String SOAPFunction = "InsertMpesaTransaction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:InsertMpesaTransaction>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:transaction></sky:transaction>
                            <sky:description></sky:description>
                            <sky:accountNo></sky:accountNo>
                            <sky:amount></sky:amount>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:requestApplication></sky:requestApplication>
                            <sky:requestCorrelationID></sky:requestCorrelationID>
                            <sky:sourceApplication></sky:sourceApplication>
                        </sky:InsertMpesaTransaction>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transaction", transaction);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:description", description);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:amount", amount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:requestApplication", requestApplication);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:requestCorrelationID", requestCorrelationID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sourceApplication", sourceApplication);


            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String insertMpesaTransaction(String entryCode, String transactionID, XMLGregorianCalendar transactionDate,
                                                String transaction, String description, String accountNo, BigDecimal amount,
                                                String phoneNo, String pIN, String requestApplication, String requestCorrelationID,
                                                String sourceApplication, String destinationAcc, String destinationName,
                                                String destinationOrg, boolean otherNumber, String destinationMobileNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "InsertMpesaTransaction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:InsertMpesaTransaction>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:transactionDate></sky:transactionDate>
                            <sky:transaction></sky:transaction>
                            <sky:description></sky:description>
                            <sky:accountNo></sky:accountNo>
                            <sky:amount></sky:amount>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:requestApplication></sky:requestApplication>
                            <sky:requestCorrelationID></sky:requestCorrelationID>
                            <sky:sourceApplication></sky:sourceApplication>
                            <sky:destinationAcc></sky:destinationAcc>
                            <sky:destinationName></sky:destinationName>
                            <sky:destinationOrg></sky:destinationOrg>
                            <sky:otherNumber></sky:otherNumber>
                            <sky:destinationMobileNumber></sky:destinationMobileNumber>
                        </sky:InsertMpesaTransaction>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionDate", transactionDate.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transaction", transaction);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:description", description);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:amount", amount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:requestApplication", requestApplication);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:requestCorrelationID", requestCorrelationID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sourceApplication", sourceApplication);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationAcc", destinationAcc);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationName", destinationName);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationOrg", destinationOrg);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:otherNumber", String.valueOf(otherNumber));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationMobileNumber", destinationMobileNumber);


            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static boolean reverseWithdrawalRequest(String entryCode) {
        String response = CBS_ERROR;

        String SOAPFunction = "ReverseWithdrawalRequest";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ReverseWithdrawalRequest>
                            <sky:entryCode></sky:entryCode>
                        </sky:ReverseWithdrawalRequest>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return false;
    }

    public static String getMemberName(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberName";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberName>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetMemberName>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanLimit(String phoneNo, String loanProductType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanLimit";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanLimit>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:loanProductType></sky:loanProductType>
                        </sky:GetLoanLimit>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanProductType", loanProductType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanLimit(String phoneNo, String loanProductType, String acctNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanLimit";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanLimit>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:loanProductType></sky:loanProductType>
                            <sky:acctNo></sky:acctNo>
                        </sky:GetLoanLimit>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanProductType", loanProductType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:acctNo", acctNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String applyLoan(String entryCode, String transactionID, String phone, String loanType,
                                   BigDecimal loanAmount, String pIN, int loanPeriod, String loanPurpose, String password,
                                   String branch) {
        String response = CBS_ERROR;

        String SOAPFunction = "ApplyLoan";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ApplyLoan>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phone></sky:phone>
                            <sky:loanType></sky:loanType>
                            <sky:loanAmount></sky:loanAmount>
                            <sky:pIN></sky:pIN>
                            <sky:loanPeriod></sky:loanPeriod>
                            <sky:loanPurpose></sky:loanPurpose>
                            <sky:password></sky:password>
                            <sky:branch></sky:branch>
                        </sky:ApplyLoan>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanAmount", loanAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPeriod", String.valueOf(loanPeriod));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPurpose", loanPurpose);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:password", password);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:branch", branch);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanPurposes(String strRequestingMobileNumber,
                                                                       String theIdentifierType,
                                                                       String theIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreArrayList> loanPurposesListWrapper = DeSaccoCBS.getLoanPurposes(theIdentifierType, theIdentifier);

            if (loanPurposesListWrapper.hasErrors() && loanPurposesListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCustomerLoanAccounts() - " + loanPurposesListWrapper.getErrors() + "\n" + loanPurposesListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(loanPurposesListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (loanPurposesListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, no loan purposes were found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, no loan purposes were found."));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", loanPurposesListWrapper.getData())
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.getLoanPurposes(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.addError(e.getMessage());
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later."));
        }
        return resultWrapper;
    }

    public static String applyLoan_RECOVERY_MODE(String entryCode, String transactionID, String phone, String loanType, BigDecimal loanAmount,
                                                 String pIN, int loanPeriod, String loanPurpose, String password, String recoveryMode,
                                                 String accountNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "ApplyLoan";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ApplyLoan>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phone></sky:phone>
                            <sky:loanType></sky:loanType>
                            <sky:loanAmount></sky:loanAmount>
                            <sky:pIN></sky:pIN>
                            <sky:loanPeriod></sky:loanPeriod>
                            <sky:loanPurpose></sky:loanPurpose>
                            <sky:password></sky:password>
                            <sky:recoveryMode></sky:recoveryMode>
                            <sky:accountNo></sky:accountNo>
                        </sky:ApplyLoan>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanAmount", loanAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPeriod", String.valueOf(loanPeriod));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPurpose", loanPurpose);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:password", password);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:recoveryMode", recoveryMode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String applyLoan_BRANCH(String entryCode, String transactionID, String phone, String loanType, BigDecimal loanAmount,
                                          String pIN, int loanPeriod, String loanPurpose, String password, String branch,
                                          String accountNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "ApplyLoan";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ApplyLoan>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phone></sky:phone>
                            <sky:loanType></sky:loanType>
                            <sky:loanAmount></sky:loanAmount>
                            <sky:pIN></sky:pIN>
                            <sky:loanPeriod></sky:loanPeriod>
                            <sky:loanPurpose></sky:loanPurpose>
                            <sky:password></sky:password>
                            <sky:branch></sky:branch>
                            <sky:acctNo></sky:acctNo>
                        </sky:ApplyLoan>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanAmount", loanAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPeriod", String.valueOf(loanPeriod));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPurpose", loanPurpose);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:password", password);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:branch", branch);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:acctNo", accountNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> loanApplication(String strRequestingMobileNumber,
                                                                       String theIdentifierType,
                                                                       String theIdentifier,
                                                                       String theDeviceIdentifierType,
                                                                       String theDeviceIdentifier,
                                                                       String theLoanTypeId,
                                                                       double theAmount,
                                                                       String theSourceReference,
                                                                       String theRequestApplication,
                                                                       String theTransactionDateTime,
                                                                       String theMobileLoanPurpose
    ) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Loan Application request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);
                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> loanApplicationWrapper = DeSaccoCBS.loanApplication(
                    theIdentifierType,
                    theIdentifier,
                    theLoanTypeId,
                    theAmount,
                    theSourceReference,
                    theRequestApplication,
                    theTransactionDateTime,
                    theMobileLoanPurpose);

            if (loanApplicationWrapper.hasErrors()) {
                System.err.println(theIdentifier + " => DeSaccoCBS.loanApplication() - " + loanApplicationWrapper.getErrors() + "\n" + loanApplicationWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Loan Application request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "Transaction Error")
                        .putValue("cbs_api_error_message", loanApplicationWrapper.getErrors() + " - " + loanApplicationWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap loanApplicationResultMap = loanApplicationWrapper.getSingleRecord();

            String requestStatus = loanApplicationResultMap.getStringValue("request_status");
            FlexicoreHashMap applicationMap = loanApplicationResultMap.getFlexicoreHashMap("response_payload");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.loanApplication(" + theLoanTypeId + ")");
                loanApplicationResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Application request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", requestStatus + ": " + applicationMap.getStringValue("error_message"))
                        .putValue("cbs_api_error_message", requestStatus + ": " + applicationMap.getStringValue("error_message") + " - " + applicationMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String loanName = applicationMap.getStringValue("loan_name");
            String amount = applicationMap.getStringValue("amount");
            /*String eligibleAmount = applicationMap.getStringValue("eligible_amount");

            String strMSG;
            if (Double.parseDouble(eligibleAmount) < 10) {
                String strFormattedAmount = Utils.formatDouble(eligibleAmount, "#,##0.00");
                strMSG = "Dear member, you do not qualify to apply for Loan " + loanName + "";
            } else {
                String strFormattedAmount = Utils.formatDouble(eligibleAmount, "#,##0.00");
                strMSG = "Dear member, you are qualified to apply for Loan " + loanName + ", amount KES " + strFormattedAmount + ".";
            }*/

            String strFormattedAmount = Utils.formatDouble(amount, "#,##0.00");

            String strMSG = "Dear member, your Loan Application of KES " + strFormattedAmount + " for Loan " + loanName + " has been received successfully.";


            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Loan Application received successfully and is being processed.")
                    .putValue("title", "Transaction Accepted")
                    .putValue("msg_object", cbsMSG)
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(theIdentifier + " => CBSAPI.loanApplication(): " + e.getMessage());
            e.printStackTrace();


            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Application request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("title", "Transaction Error")
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("cbs_api_error_message", e.getMessage())
                    .putValue("msg_object", cbsMSG)

            );
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> loanPaymentViaSavings(String strRequestingMobileNumber,
                                                                             String theIdentifierType,
                                                                             String theIdentifier,
                                                                             String theDeviceIdentifierType,
                                                                             String theDeviceIdentifier,
                                                                             String theOriginatorId,
                                                                             String theSourceAccount,
                                                                             String theDestinationAccount,
                                                                             double theAmount,
                                                                             String theTransactionDescription,
                                                                             String theSourceReference,
                                                                             String theRequestApplication,
                                                                             String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Loan Payment via Savings Account request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);
                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> loanPaymentViaSavingsWrapper = DeSaccoCBS.loanPaymentViaSavings(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    theSourceAccount,
                    theDestinationAccount,
                    theAmount,
                    theTransactionDescription,
                    theSourceReference,
                    theRequestApplication,
                    theSourceApplication);

            if (loanPaymentViaSavingsWrapper.hasErrors()) {
                System.err.println(theIdentifier + " => DeSaccoCBS.loanPaymentViaSavings() - " + loanPaymentViaSavingsWrapper.getErrors() + "\n" + loanPaymentViaSavingsWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Loan Payment via Savings Account request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "Transaction Error")
                        .putValue("cbs_api_error_message", loanPaymentViaSavingsWrapper.getErrors() + " - " + loanPaymentViaSavingsWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap loanPaymentViaSavingsResultMap = loanPaymentViaSavingsWrapper.getSingleRecord();

            String requestStatus = loanPaymentViaSavingsResultMap.getStringValue("request_status");
            FlexicoreHashMap loanPaymentViaSavingsMap = loanPaymentViaSavingsResultMap.getFlexicoreHashMap("response_payload");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.loanPaymentViaSavings(" + theSourceAccount + ")");
                loanPaymentViaSavingsResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, the source account " + theSourceAccount + " balance is insufficient to process your Loan Payment via Savings Account request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("title", "Insufficient Balance")
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Loan Payment via Savings Account request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + loanPaymentViaSavingsMap.getStringValue("error_message") + " - " + loanPaymentViaSavingsMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.loanPaymentViaSavings(" + theSourceAccount + ")");
                loanPaymentViaSavingsResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Payment via Savings Account request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("title", "Transaction Error")
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Payment via Savings Account request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + loanPaymentViaSavingsMap.getStringValue("error_message") + " - " + loanPaymentViaSavingsMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strFormattedDateTime = DateTime.getCurrentDateTime("dd-MMM-yyyy HH:mm:ss");
            //String strFormattedDateTime = Utils.formatDate(strDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");
            String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");

            String strMSG = "Dear member, your Request for Loan Payment via Savings Account of KES " + strFormattedAmount + " to account " + theDestinationAccount + " on " + strFormattedDateTime + " has been received and is being processed.";

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Loan Payment via Savings Account received successfully.")
                    .putValue("title", "Transaction Accepted")
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_transaction_reference", loanPaymentViaSavingsMap.getStringValue("transaction_reference"))
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(theIdentifier + " => CBSAPI.loanPaymentViaSavings(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("title", "Transaction Error")
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("cbs_api_error_message", e.getMessage())

            );
        }
        return resultWrapper;
    }


    public static String agentWithdrawal(String entryCode, String transactionID, XMLGregorianCalendar transactionDate,
                                         String mobileNo, String agentAccount, String destination, BigDecimal transAmount, String pIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "AgentWithdrawal";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AgentWithdrawal>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:transactionDate></sky:transactionDate>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:agentAccount></sky:agentAccount>
                            <sky:destination></sky:destination>
                            <sky:transAmount></sky:transAmount>
                            <sky:pIN></sky:pIN>
                        </sky:AgentWithdrawal>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionDate", transactionDate.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:agentAccount", agentAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destination", destination);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transAmount", transAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMemberLoanList(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberLoanList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberLoanList>
                            <sky:phone></sky:phone>
                        </sky:GetMemberLoanList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> getCustomerLoanAccounts(String strRequestingMobileNumber,
                                                                               String theIdentifierType,
                                                                               String theIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreArrayList> customerAccountsListWrapper = DeSaccoCBS.getMemberLoansInService(theIdentifierType, theIdentifier);

            if (customerAccountsListWrapper.hasErrors() && customerAccountsListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCustomerLoanAccounts() - " + customerAccountsListWrapper.getErrors() + "\n" + customerAccountsListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(customerAccountsListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (customerAccountsListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, no loans in service were found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, no loans in service were found."));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", customerAccountsListWrapper.getData())
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.getCustomerLoanAccounts(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.addError(e.getMessage());
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later."));
        }
        return resultWrapper;
    }

    public static String getMemberLoanList_V2(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberLoanList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberLoanList>
                            <sky:phone></sky:phone>
                        </sky:GetMemberLoanList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoansWithGuarantors(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoansWithGuarantors";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoansWithGuarantors>
                            <sky:phoneNo></sky:phoneNo>
                      </sky:GetLoansWithGuarantors>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanWithGuarantorDetails(int loanEntryNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanWithGuarantorDetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanWithGuarantorDetails>
                            <sky:loanEntryNo></sky:loanEntryNo>
                        </sky:GetLoanWithGuarantorDetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanEntryNo", String.valueOf(loanEntryNo));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanPurpose() {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanPurpose";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanPurpose></sky:GetLoanPurpose>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            //requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:loanEntryNo", String.valueOf(loanEntryNo));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getBranches() {
        String response = CBS_ERROR;

        String SOAPFunction = "GetBranches";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetBranches></sky:GetBranches>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            //requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:loanEntryNo", String.valueOf(loanEntryNo));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoaneesAwaitingGuarantorship(String phoneNo, String status) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoaneesAwaitingGuarantorship";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoaneesAwaitingGuarantorship>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:status></sky:status>
                        </sky:GetLoaneesAwaitingGuarantorship>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:status", status);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getDetailsForSpecificLoanGuaranteed(String phoneNo, int loanEntryNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetDetailsForSpecificLoanGuaranteed";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetDetailsForSpecificLoanGuaranteed>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:loanEntryNo></sky:loanEntryNo>
                        </sky:GetDetailsForSpecificLoanGuaranteed>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanEntryNo", String.valueOf(loanEntryNo));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMobileLoanList(String phoneNo, String category) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMobileLoanList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMobileLoanList>
                            <sky:phone></sky:phone>
                            <sky:category></sky:category>
                        </sky:GetMobileLoanList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:category", category);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMobileLoanList(String phoneNo, String category, String accNo) {

        String response = CBS_ERROR;

        String SOAPFunction = "GetMobileLoanList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="urn:microsoft-dynamics-schemas/codeunit/SkyMobile">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMobileLoanList>
                            <sky:phone></sky:phone>
                            <sky:category></sky:category>
                            <sky:accNo></sky:accNo>
                        </sky:GetMobileLoanList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:category", category);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accNo", accNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getErroneousTransactions(String phoneNo) {


        String response = CBS_ERROR;

        String SOAPFunction = "GetErroneousTransactions";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="urn:microsoft-dynamics-schemas/codeunit/SkyMobile">
                    <x:Header/>
                    <x:Body>
                        <sky:GetErroneousTransactions>
                            <sky:phone></sky:phone>
                        </sky:GetErroneousTransactions>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String UpdateErroneousTransactions(String theId, String theAccount) {

        String response = CBS_ERROR;

        String SOAPFunction = "UpdateErroneousTransactions";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="urn:microsoft-dynamics-schemas/codeunit/SkyMobile">
                    <x:Header/>
                    <x:Body>
                        <sky:UpdateErroneousTransactions>
                            <sky:id></sky:id>
                            <sky:account></sky:account>
                        </sky:UpdateErroneousTransactions>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:id", theId);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:account", theAccount);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoansGuaranteed(String transactionID, String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoansGuaranteed";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoansGuaranteed>
                            <sky:transactionID></sky:transactionID>
                            <sky:phone></sky:phone>
                        </sky:GetLoansGuaranteed>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountTransfer_DEBITACCOUNT(String entryCode, String transactionID, String mobileNo, String toAccount,
                                                      String destination, BigDecimal transAmount, String pIN, boolean payLoan,
                                                      boolean toBOSA, String debitAccount) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountTransfer";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountTransfer>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:toAccount></sky:toAccount>
                            <sky:destination></sky:destination>
                            <sky:transAmount></sky:transAmount>
                            <sky:pIN></sky:pIN>
                            <sky:payLoan></sky:payLoan>
                            <sky:toBOSA></sky:toBOSA>
                            <sky:debitAccount></sky:debitAccount>
                        </sky:AccountTransfer>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:toAccount", toAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destination", destination);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transAmount", transAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:payLoan", String.valueOf(payLoan));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:toBOSA", String.valueOf(toBOSA));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:debitAccount", debitAccount);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountTransfer_SOURCEACCOUNT(String entryCode, String transactionID, String mobileNo, String toAccount,
                                                       String destination, BigDecimal transAmount, String pIN, boolean payLoan,
                                                       boolean toBOSA, String sourceAccount) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountTransfer";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountTransfer>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:toAccount></sky:toAccount>
                            <sky:destination></sky:destination>
                            <sky:transAmount></sky:transAmount>
                            <sky:pIN></sky:pIN>
                            <sky:payLoan></sky:payLoan>
                            <sky:toBOSA></sky:toBOSA>
                            <sky:sourceAccount></sky:sourceAccount>
                        </sky:AccountTransfer>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:toAccount", toAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destination", destination);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transAmount", transAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:payLoan", String.valueOf(payLoan));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:toBOSA", String.valueOf(toBOSA));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sourceAccount", sourceAccount);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountTransfer(String entryCode, String transactionID, String mobileNo, String toAccount,
                                         String destination, BigDecimal transAmount, String pIN, boolean payLoan,
                                         boolean toBOSA, String srcAccount, String narration) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountTransfer";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountTransfer>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:toAccount></sky:toAccount>
                            <sky:destination></sky:destination>
                            <sky:transAmount></sky:transAmount>
                            <sky:pIN></sky:pIN>
                            <sky:payLoan></sky:payLoan>
                            <sky:toBOSA></sky:toBOSA>
                            <sky:srcAccount></sky:srcAccount>
                            <sky:narration></sky:narration>
                        </sky:AccountTransfer>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:toAccount", toAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destination", destination);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transAmount", transAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:payLoan", String.valueOf(payLoan));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:toBOSA", String.valueOf(toBOSA));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:srcAccount", srcAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:narration", narration);
            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getAgentDetails(String agentID) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAgentDetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAgentDetails>
                            <sky:agentID></sky:agentID>
                        </sky:GetAgentDetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:agentID", agentID);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String updateMemberData(String phoneNo, String newData, String dataType, String pIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAgentDetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:UpdateMemberData>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:newData></sky:newData>
                            <sky:dataType></sky:dataType>
                            <sky:pIN></sky:pIN>
                        </sky:UpdateMemberData>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:newData", newData);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:dataType", dataType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String addRemoveMobileLoanGuarantor(int loanEntryNo, String mobileNo, String action) {
        String response = CBS_ERROR;

        String SOAPFunction = "AddRemoveMobileLoanGuarantor";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AddRemoveMobileLoanGuarantor>
                            <sky:loanEntryNo></sky:loanEntryNo>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:action></sky:action>
                        </sky:AddRemoveMobileLoanGuarantor>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanEntryNo", String.valueOf(loanEntryNo));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String checkBeneficiaries(String entryCode, String transactionID, String phoneNo, String pIN, String mode,
                                            String emailAddress) {
        String response = CBS_ERROR;

        String SOAPFunction = "CheckBeneficiaries";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CheckBeneficiaries>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:mode></sky:mode>
                            <sky:emailAddress></sky:emailAddress>
                        </sky:CheckBeneficiaries>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mode", mode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:emailAddress", emailAddress);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String createSavingsAcounts(String phoneNumber, String accountName, String businessLocation, String productCode,
                                              XMLGregorianCalendar dateOfBirth, String birthCertificateNumber, String gender,
                                              BigDecimal sTOAmount, String duration) {
        String response = CBS_ERROR;

        String SOAPFunction = "CreateSavingsAcounts";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CreateSavingsAcounts>
                            <sky:phoneNumber></sky:phoneNumber>
                            <sky:accountName></sky:accountName>
                            <sky:businessLocation></sky:businessLocation>
                            <sky:productCode></sky:productCode>
                            <sky:dateOfBirth></sky:dateOfBirth>
                            <sky:birthCertificateNumber></sky:birthCertificateNumber>
                            <sky:gender></sky:gender>
                            <sky:sTOAmount></sky:sTOAmount>
                            <sky:duration></sky:duration>
                        </sky:CreateSavingsAcounts>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNumber", phoneNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountName", accountName);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:businessLocation", businessLocation);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:productCode", productCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:dateOfBirth", dateOfBirth.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:birthCertificateNumber", birthCertificateNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:gender", gender);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sTOAmount", sTOAmount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:duration", duration);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String actionLoanGuarantorship(String mobileNo, int loanEntryNo, String pIN, String action) {
        String response = CBS_ERROR;

        String SOAPFunction = "ActionLoanGuarantorship";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ActionLoanGuarantorship>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:loanEntryNo></sky:loanEntryNo>
                            <sky:pIN></sky:pIN>
                            <sky:action></sky:action>
                        </sky:ActionLoanGuarantorship>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanEntryNo", String.valueOf(loanEntryNo));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static boolean employerRestriction(String phoneNo, String transaction) {
        String response = CBS_ERROR;

        String SOAPFunction = "EmployerRestriction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:EmployerRestriction>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:transaction></sky:transaction>
                        </sky:EmployerRestriction>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transaction", transaction);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return false;
    }

    public static TransactionWrapper<FlexicoreHashMap> getATMCards(String strRequestingMobileNumber,
                                                                       String theIdentifierType,
                                                                       String theIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreArrayList> atmCardsListWrapper = DeSaccoCBS.getATMCards(theIdentifierType, theIdentifier);

            if (atmCardsListWrapper.hasErrors() && atmCardsListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getATMCards() - " + atmCardsListWrapper.getErrors() + "\n" + atmCardsListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(atmCardsListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (atmCardsListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, no ATM Cards were found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, no ATM Cards were found."));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", atmCardsListWrapper.getData())
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.getATMCards(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.addError(e.getMessage());
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later."));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> delinkATMCard(String strRequestingMobileNumber,
                                                                     String theIdentifierType,
                                                                     String theIdentifier,
                                                                     String theCardId,
                                                                     String delinkReason) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreHashMap> delinkATMCardWrapper = DeSaccoCBS.delinkATMCards(theIdentifierType, theIdentifier, theCardId, delinkReason);

            if (delinkATMCardWrapper.hasErrors()) {
                System.err.println(theIdentifier + " => DeSaccoCBS.delinkATMCards() - " + delinkATMCardWrapper.getErrors() + "\n" + delinkATMCardWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(delinkATMCardWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            FlexicoreHashMap delinkATMCardResultMap = delinkATMCardWrapper.getSingleRecord();

            String requestStatus = delinkATMCardResultMap.getStringValue("status");
            String statusDescription = delinkATMCardResultMap.getStringValue("status_description"); // Capture status_description
            FlexicoreHashMap responsePayload = delinkATMCardResultMap.getFlexicoreHashMap("data"); // Changed from response_payload to data based on your expected response

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.delinkATMCards() failed: " + statusDescription);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("ATM Card delink failed: " + statusDescription);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Failed to delink ATM card: " + statusDescription + getTrailerMessage()));

                return resultWrapper;
            }

            System.out.println(strRequestingMobileNumber + " => ATM Card delink successful: " + statusDescription);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", statusDescription) // Display the actual status description
                    .putValue("status", requestStatus)
                    .putValue("status_description", statusDescription)
                    .putValue("response_data", responsePayload)
            );

            return resultWrapper;

        } catch (Exception e) {
            System.err.println(theIdentifier + " => CBSAPI.delinkATMCard() " + e.getMessage());
            e.printStackTrace();

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("title", "Transaction Error")
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
            );
        }
        return resultWrapper;
    }

    public static String getLoanAccessSetup(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanAccessSetup";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanAccessSetup>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetLoanAccessSetup>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getBusinessDetails(String theShortCode) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetBusinessDetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetBusinessDetails>
                            <sky:shortCode></sky:shortCode>
                        </sky:GetBusinessDetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:shortCode", theShortCode);

            System.out.println("Request XML     : " + requestXml.read("/"));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMemberStatus(String thePhoneNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMembershipDetails";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMembershipDetails>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetMembershipDetails>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", thePhoneNumber);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String mobileBankingReports() {
        String response = CBS_ERROR;

        String SOAPFunction = "MobileBankingReports";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:MobileBankingReports></sky:MobileBankingReports>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            //requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String creatMortgageEntry(String sessionID, String productType, String names, String ageGroup, String sourceOfIncome,
                                            String grossMonthlyIncome, String countyOfResidence, String phoneNumber, String email) {
        String response = CBS_ERROR;

        String SOAPFunction = "CreatMortgageEntry";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CreatMortgageEntry>
                            <sky:sessionID></sky:sessionID>
                            <sky:productType></sky:productType>
                            <sky:names></sky:names>
                            <sky:ageGroup></sky:ageGroup>
                            <sky:sourceOfIncome></sky:sourceOfIncome>
                            <sky:grossMonthlyIncome></sky:grossMonthlyIncome>
                            <sky:countyOfResidence></sky:countyOfResidence>
                            <sky:phoneNumber></sky:phoneNumber>
                            <sky:email></sky:email>
                        </sky:CreatMortgageEntry>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sessionID", sessionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:productType", productType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:names", names);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:ageGroup", ageGroup);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sourceOfIncome", sourceOfIncome);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:grossMonthlyIncome", grossMonthlyIncome);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:countyOfResidence", countyOfResidence);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNumber", phoneNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:email", email);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String creatInsuranceEntry(String sessionID, String phoneNumber, String names, String purpose, String email) {
        String response = CBS_ERROR;

        String SOAPFunction = "CreatInsuranceEntry";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CreatInsuranceEntry>
                            <sky:sessionID></sky:sessionID>
                            <sky:phone_Number></sky:phone_Number>
                            <sky:names></sky:names>
                            <sky:purpose></sky:purpose>
                            <sky:email></sky:email>
                        </sky:CreatInsuranceEntry>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sessionID", sessionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone_Number", phoneNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:names", names);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:purpose", purpose);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:email", email);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String virtualMemberRegistration(String sessionID, String telephone, String iDNo, String names,
                                                   XMLGregorianCalendar dOB, String recruitedBy, String email, String serviceNumber,
                                                   String employer, String nameOnIPRS, String gender) {
        String response = CBS_ERROR;

        String SOAPFunction = "VirtualMemberRegistration";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:VirtualMemberRegistration>
                            <sky:sessionID></sky:sessionID>
                            <sky:telephone></sky:telephone>
                            <sky:iD_No></sky:iD_No>
                            <sky:names></sky:names>
                            <sky:dOB></sky:dOB>
                            <sky:recruited_By></sky:recruited_By>
                            <sky:email></sky:email>
                            <sky:service_Number></sky:service_Number>
                            <sky:employer></sky:employer>
                            <sky:nameOnIPRS></sky:nameOnIPRS>
                            <sky:gender></sky:gender>
                        </sky:VirtualMemberRegistration>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sessionID", sessionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:telephone", telephone);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:iD_No", iDNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:names", names);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:dOB", dOB.toXMLFormat());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:recruited_By", recruitedBy);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:email", email);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:service_Number", serviceNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:employer", employer);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:nameOnIPRS", nameOnIPRS);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:gender", gender);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getSelfEnrollmentStatus(String mobileNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetSelfEnrollmentStatus";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetSelfEnrollmentStatus>
                            <sky:mobileNumber></sky:mobileNumber>
                        </sky:GetSelfEnrollmentStatus>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNumber", mobileNumber);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String memberRejoinRequest(String entryNo, String mobileNumber, String memberNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "MemberRejoinRequest";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:MemberRejoinRequest>
                            <sky:entryNo></sky:entryNo>
                            <sky:mobileNumber></sky:mobileNumber>
                            <sky:memberNumber></sky:memberNumber>
                        </sky:MemberRejoinRequest>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryNo", entryNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNumber", mobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:memberNumber", memberNumber);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getAccountMaturityPeriods(String accountType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAccountMaturityPeriods";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAccountMaturityPeriods>
                            <sky:accountType></sky:accountType>
                        </sky:GetAccountMaturityPeriods>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountType", accountType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getDividendPayslip(String phoneNumber, int period, String email) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetDividendPayslip";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetDividendPayslip>
                            <sky:phoneNumber></sky:phoneNumber>
                            <sky:period></sky:period>
                            <sky:email></sky:email>
                        </sky:GetDividendPayslip>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNumber", phoneNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:period", String.valueOf(period));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:email", email);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getDividendPayslip(String mobileNumber, String destinationEmaillAddress) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetDividendPayslip";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetDividendPayslip>
                            <sky:mobileNumber></sky:mobileNumber>
                            <sky:destinationEmaillAddress></sky:destinationEmaillAddress>
                        </sky:GetDividendPayslip>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNumber", mobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:destinationEmaillAddress", destinationEmaillAddress);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }


    public static String getDividendPayslipCurrent(String mobileNumber, String destinationEmaillAddress) {
        String response = CBS_ERROR;

        String SOAPFunction = "DividendPayslipMobileAppB64";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:DividendPayslipMobileAppB64>
                            <sky:mobileNumber></sky:mobileNumber>
                            <sky:year></sky:year>
                        </sky:DividendPayslipMobileAppB64>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNumber", mobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:year", destinationEmaillAddress);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String callServiceFunction(int i) {
        String response = CBS_ERROR;

        String SOAPFunction = "CallServiceFunction";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CallServiceFunction>
                            <sky:i></sky:i>
                        </sky:CallServiceFunction>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:i", String.valueOf(i));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static boolean checkService(String serviceName) {
        String response = CBS_ERROR;

        String SOAPFunction = "CheckService";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CheckService>
                            <sky:serviceName></sky:serviceName>
                        </sky:CheckService>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:serviceName", serviceName);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return false;
    }

    public static String getLoanBalance(String loanNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanBalance";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanBalance>
                            <sky:loanNo></sky:loanNo>
                        </sky:GetLoanBalance>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanNo", loanNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
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
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            //requestXml.update("/x:Envelope/x:Body/sky:"+SOAPFunction+"/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String setHashedPIN(String accountNumber, String phoneNumber, String pINNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "SetHashedPIN";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:SetHashedPIN>
                            <sky:account_Number></sky:account_Number>
                            <sky:phone_Number></sky:phone_Number>
                            <sky:pIN_Number></sky:pIN_Number>
                        </sky:SetHashedPIN>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:account_Number", accountNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone_Number", phoneNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN_Number", pINNumber);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getBusinessAccounts(String phoneNumber) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetBusinessAccounts";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetBusinessAccounts>
                            <sky:phoneNumber></sky:phoneNumber>
                        </sky:GetBusinessAccounts>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNumber", phoneNumber);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getTemporarySavingsAccountNumber(String mobileNumber, String productType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetTemporarySavingsAccountNumber";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetTemporarySavingsAccountNumber>
                            <sky:mobileNumber></sky:mobileNumber>
                            <sky:productType></sky:productType>
                        </sky:GetTemporarySavingsAccountNumber>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNumber", mobileNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:productType", productType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String deactivateMobileApp(String mobileNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "DeactivateMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:DeactivateMobileApp>
                            <sky:mobileNo></sky:mobileNo>
                        </sky:DeactivateMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> activateMobileApp(String theReferenceKey, String theIdentifierType, String theIdentifier,
                                                                         String theDeviceIdentifierType, String theDeviceIdentifier, FlexicoreHashMap mobileBankingMap) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            theUpdateLoginParamsMap.put("app_identifier", theDeviceIdentifier);
            theUpdateLoginParamsMap.put("app_identifier_set_date", DateTime.getCurrentDateTime());

            theUpdateLoginParamsMap.put("app_activation_kyc", "DISABLED");
            theUpdateLoginParamsMap.put("app_activation_kyc_set_date", DateTime.getCurrentDateTime());
            if (mobileBankingMap.getStringValue("app_activation_date") == null) {
                theUpdateLoginParamsMap.put("app_activation_date", DateTime.getCurrentDateTime());
            }

            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

            mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

            String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
            theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

            TransactionWrapper<?> updateWrapper = Repository.update(
                    StringRefs.SENTINEL,
                    TBL_MOBILE_BANKING_REGISTER,
                    theUpdateLoginParamsMap,
                    new FilterPredicate("mobile_register_id = :mobile_register_id"),
                    new FlexicoreHashMap()
                            .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
            );

            if (updateWrapper.hasErrors()) {
                System.err.println("CBSAPI.activateMobileApp() - Update ERROR: " + updateWrapper.getErrors() + "\n");

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "An error occurred"));
            }

            return resultWrapper;

        } catch (Exception e) {
            System.err.println("CBSAPI.activateMobileApp(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "An error occurred"));
        }
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> deactivateMobileApp(String theReferenceKey, String theIdentifierType, String theIdentifier,
                                                                           String theDeviceIdentifierType, String theDeviceIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                return checkUserResultMapWrapper;
            }

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();

            theUpdateLoginParamsMap.put("app_identifier", null);
            theUpdateLoginParamsMap.put("app_identifier_set_date", null);

            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

            mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

            String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
            theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

            TransactionWrapper<?> updateWrapper = Repository.update(
                    StringRefs.SENTINEL,
                    TBL_MOBILE_BANKING_REGISTER,
                    theUpdateLoginParamsMap,
                    new FilterPredicate("mobile_register_id = :mobile_register_id"),
                    new FlexicoreHashMap()
                            .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
            );

            if (updateWrapper.hasErrors()) {
                System.err.println("CBSAPI.deactivateMobileApp() - Update ERROR: " + updateWrapper.getErrors() + "\n");

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "An error occurred"));
            }

            return resultWrapper;

        } catch (Exception e) {
            System.err.println("CBSAPI.deactivateMobileApp(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("title", "An error occurred"));
        }
        return resultWrapper;
    }



    public static String mappSetIMEI(String mobileNo, String iMEI) {
        String response = CBS_ERROR;

        String SOAPFunction = "MAPPSetIMEI";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:MAPPSetIMEI>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:iMEI></sky:iMEI>
                        </sky:MAPPSetIMEI>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:iMEI", iMEI);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static boolean checkKYCByNationalIDNo(String phoneNoa46, String nationalIDNoa46, String currentPIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "CheckKYCByNationalIDNo";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CheckKYCByNationalIDNo>
                            <sky:phoneNoa46></sky:phoneNoa46>
                            <sky:nationalIDNoa46></sky:nationalIDNoa46>
                            <sky:currentPIN></sky:currentPIN>
                        </sky:CheckKYCByNationalIDNo>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNoa46", phoneNoa46);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:nationalIDNoa46", nationalIDNoa46);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:currentPIN", currentPIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return false;
    }

    public static String getMemberLoanListMobileApp(String phone) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberLoanListMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberLoanListMobileApp>
                            <sky:phone></sky:phone>
                        </sky:GetMemberLoanListMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanPendingGuarantor(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanPendingGuarantor";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanPendingGuarantor>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetLoanPendingGuarantor>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountBalanceEnquiryMobileApp(String entryCode, String transactionID, String phoneNo, String pIN, String accountToCheck) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountBalanceEnquiryMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountBalanceEnquiryMobileApp>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:accountToCheck></sky:accountToCheck>
                        </sky:AccountBalanceEnquiryMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountToCheck", accountToCheck);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }
    public static String dividendPayslip(String entryCode, String transactionID, String phoneNo, String pIN, String accountToCheck) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountBalanceEnquiryMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountBalanceEnquiryMobileApp>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:accountToCheck></sky:accountToCheck>
                        </sky:AccountBalanceEnquiryMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountToCheck", accountToCheck);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanLimitMobileApp(String phoneNo, String loanProductType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanLimitMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanLimitMobileApp>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:loanProductType></sky:loanProductType>
                        </sky:GetLoanLimitMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanProductType", loanProductType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanLimitMobileApp(String phoneNo, String loanProductType, String acctNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanLimitMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanLimitMobileApp>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:loanProductType></sky:loanProductType>
                            <sky:acctNo></sky:acctNo>
                        </sky:GetLoanLimitMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanProductType", loanProductType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:acctNo", acctNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountMiniStatementMobileApp(String entryCode, String transactionID, int maxNumberRows,
                                                       String startDate, String endDate,
                                                       String statementAccount, String mobileNo, String pin) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountMiniStatementMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountMiniStatementMobileApp>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:maxNumberRows></sky:maxNumberRows>
                            <sky:startDate></sky:startDate>
                            <sky:endDate></sky:endDate>
                            <sky:statementAccount></sky:statementAccount>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pin></sky:pin>
                        </sky:AccountMiniStatementMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:maxNumberRows", String.valueOf(maxNumberRows));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDate", startDate);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDate", endDate);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:statementAccount", statementAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pin", pin);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountMiniStatementMobileApp(String entryCode, String transactionID, int maxNumberRows,
                                                       int startDateDay, int startDateMonth, int startDateYear, int endDateDay, int endDateMonth, int endDateYear,
                                                       String statementAccount, String mobileNo, String pin) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountMiniStatementMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountMiniStatementMobileApp>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:maxNumberRows></sky:maxNumberRows>
                            <sky:startDateDay></sky:startDateDay>
                            <sky:startDateMonth></sky:startDateMonth>
                            <sky:startDateYear></sky:startDateYear>
                            <sky:endDateDay></sky:endDateDay>
                            <sky:endDateMonth></sky:endDateMonth>
                            <sky:endDateYear></sky:endDateYear>
                            <sky:statementAccount></sky:statementAccount>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pin></sky:pin>
                        </sky:AccountMiniStatementMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:maxNumberRows", String.valueOf(maxNumberRows));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateDay", String.valueOf(startDateDay));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateMonth", String.valueOf(startDateMonth));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateYear", String.valueOf(startDateYear));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateDay", String.valueOf(endDateDay));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateMonth", String.valueOf(endDateMonth));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateYear", String.valueOf(endDateYear));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:statementAccount", statementAccount);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pin", pin);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String loanMiniStatementMobileApp(String entryCode, String transactionID, int maxNumberRows,
                                                    String sDate, String eDate, String loanType,
                                                    String mobileNo, String pin) {
        String response = CBS_ERROR;

        String SOAPFunction = "LoanMiniStatementMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:LoanMiniStatementMobileApp>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:maxNumberRows></sky:maxNumberRows>
                            <sky:sDate></sky:sDate>
                            <sky:eDate></sky:eDate>
                            <sky:loanType></sky:loanType>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pin></sky:pin>
                        </sky:LoanMiniStatementMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:maxNumberRows", String.valueOf(maxNumberRows));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:sDate", sDate);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:eDate", eDate);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pin", pin);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String loanMiniStatementMobileApp(String entryCode, String transactionID, int maxNumberRows, int startDateDay, int startDateMonth,
                                                    int startDateYear, int endDateDay, int endDateMonth, int endDateYear, String loanType,
                                                    String mobileNo, String pin) {
        String response = CBS_ERROR;

        String SOAPFunction = "LoanMiniStatementMobileApp";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="urn:microsoft-dynamics-schemas/codeunit/SkyMobile">
                    <x:Header/>
                    <x:Body>
                        <sky:LoanMiniStatementMobileApp>
                            <sky:entryCode></sky:entryCode>
                            <sky:transactionID></sky:transactionID>
                            <sky:maxNumberRows></sky:maxNumberRows>
                            <sky:startDateDay></sky:startDateDay>
                            <sky:startDateMonth></sky:startDateMonth>
                            <sky:startDateYear></sky:startDateYear>
                            <sky:endDateDay></sky:endDateDay>
                            <sky:endDateMonth></sky:endDateMonth>
                            <sky:endDateYear></sky:endDateYear>
                            <sky:loanType></sky:loanType>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:pin></sky:pin>
                        </sky:LoanMiniStatementMobileApp>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:entryCode", entryCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:maxNumberRows", String.valueOf(maxNumberRows));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateDay", String.valueOf(startDateDay));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateMonth", String.valueOf(startDateMonth));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateYear", String.valueOf(startDateYear));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateDay", String.valueOf(endDateDay));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateMonth", String.valueOf(endDateMonth));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateYear", String.valueOf(endDateYear));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pin", pin);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanGuarantors(String transactionID, String loanNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanGuarantors";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanGuarantors>
                            <sky:transactionID></sky:transactionID>
                            <sky:loanNo></sky:loanNo>
                        </sky:GetLoanGuarantors>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionID", transactionID);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanNo", loanNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanToConfirmGuarantoship(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanToConfirmGuarantoship";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanToConfirmGuarantoship>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetLoanToConfirmGuarantoship>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getNeedsChange(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetNeedsChange";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetNeedsChange>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetNeedsChange>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getAdvancedMobileLoanList(String phone, String category, String accNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetAdvancedMobileLoanList";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetAdvancedMobileLoanList>
                            <sky:phone></sky:phone>
                            <sky:category></sky:category>
                            <sky:accNo></sky:accNo>
                        </sky:GetAdvancedMobileLoanList>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:category", category);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accNo", accNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getLoanPeriods(String phoneNo, String loanCode, int duration) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetLoanPeriods";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetLoanPeriods>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:loanCode></sky:loanCode>
                            <sky:duration></sky:duration>
                        </sky:GetLoanPeriods>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanCode", loanCode);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:duration", String.valueOf(duration));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static boolean checkIfServiceIsEnabledForUser(String accNo, String service) {
        String response = CBS_ERROR;

        String SOAPFunction = "CheckIfServiceIsEnabledForUser";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CheckIfServiceIsEnabledForUser>
                            <sky:accNo></sky:accNo>
                            <sky:service></sky:service>
                        </sky:CheckIfServiceIsEnabledForUser>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accNo", accNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:service", service);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return false;
    }

    public static String getATMCards(String phoneNo, String action) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetATMCards";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetATMCards>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:action></sky:action>
                        </sky:GetATMCards>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String actionATMCard(String action, String aTMCardNumber, String phoneNo, String pIN, String accountNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "ActionATMCard";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ActionATMCard>
                            <sky:action></sky:action>
                            <sky:aTMCardNumber></sky:aTMCardNumber>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:pIN></sky:pIN>
                            <sky:accountNo></sky:accountNo>
                        </sky:ActionATMCard>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:aTMCardNumber", aTMCardNumber);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String tradeShares(String action, String accountNo, BigDecimal price, int shares, String pIN, String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "TradeShares";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:TradeShares>
                            <sky:action></sky:action>
                            <sky:accountNo></sky:accountNo>
                            <sky:price></sky:price>
                            <sky:shares></sky:shares>
                            <sky:pIN></sky:pIN>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:TradeShares>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:price", price.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:shares", String.valueOf(shares));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String correctNeedsChange(String phoneNo, String transactionId, String correctAccountNo, String narration) {
        String response = CBS_ERROR;

        String SOAPFunction = "CorrectNeedsChange";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:CorrectNeedsChange>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:transactionId></sky:transactionId>
                            <sky:correctAccountNo></sky:correctAccountNo>
                            <sky:narration></sky:narration>
                        </sky:CorrectNeedsChange>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactionId", transactionId);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:correctAccountNo", correctAccountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:narration", narration);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String actionBankTransfer(String action, String accountNo, String pIN, String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "ActionBankTransfer";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ActionBankTransfer>
                            <sky:action></sky:action>
                            <sky:accountNo></sky:accountNo>
                            <sky:pIN></sky:pIN>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:ActionBankTransfer>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static boolean actionServiceForUser(String action, String accNo, String service) {
        String response = CBS_ERROR;

        String SOAPFunction = "ActionServiceForUser";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ActionServiceForUser>
                            <sky:action></sky:action>
                            <sky:accNo></sky:accNo>
                            <sky:service></sky:service>
                        </sky:ActionServiceForUser>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accNo", accNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:service", service);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

            return Boolean.parseBoolean(response);

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return false;
    }

    public static String getRecoveryModes() {
        String response = CBS_ERROR;

        String SOAPFunction = "GetRecoveryModes";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetRecoveryModes></sky:GetRecoveryModes>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getSalaryAdvanceLoanQualification(String accountNo, String loanProductType, BigDecimal qualAmt,
                                                           int salType, int loanPeriod) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetSalaryAdvanceLoanQualification";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetSalaryAdvanceLoanQualification>
                            <sky:accountNo></sky:accountNo>
                            <sky:loanProductType></sky:loanProductType>
                            <sky:qualAmt></sky:qualAmt>
                            <sky:salType></sky:salType>
                            <sky:loanPeriod></sky:loanPeriod>
                        </sky:GetSalaryAdvanceLoanQualification>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanProductType", loanProductType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:qualAmt", qualAmt.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:salType", String.valueOf(salType));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanPeriod", String.valueOf(loanPeriod));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String benefitConfirmed(String loanType, BigDecimal amount, String accountNo, BigDecimal qualAmt,
                                          int loanperiod) {
        String response = CBS_ERROR;

        String SOAPFunction = "BenefitConfirmed";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:BenefitConfirmed>
                            <sky:loanType></sky:loanType>
                            <sky:amount></sky:amount>
                            <sky:accountNo></sky:accountNo>
                            <sky:qualAmt></sky:qualAmt>
                            <sky:loanperiod></sky:loanperiod>
                        </sky:BenefitConfirmed>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanType", loanType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:amount", amount.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:qualAmt", qualAmt.toString());
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanperiod", String.valueOf(loanperiod));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMemberAlerts(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberAlerts";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberAlerts>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetMemberAlerts>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String addMobileLoanGuarantor(int loanEntryNo, String mobileNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "AddMobileLoanGuarantor";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AddMobileLoanGuarantor>
                            <sky:loanEntryNo></sky:loanEntryNo>
                            <sky:mobileNo></sky:mobileNo>
                        </sky:AddMobileLoanGuarantor>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanEntryNo", String.valueOf(loanEntryNo));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMemberBalances(String phoneNo, String accountType) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberBalances";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberBalances>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:accountType></sky:accountType>
                        </sky:GetMemberBalances>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountType", accountType);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountMiniStatementMobileAppB64(String startDate, String endDate,
                                                          String statementAccount) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountMiniStatementMobileAppB64";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountMiniStatementMobileAppB64>
                            <sky:startDate></sky:startDate>
                            <sky:endDate></sky:endDate>
                            <sky:statementAccount></sky:statementAccount>
                        </sky:AccountMiniStatementMobileAppB64>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDate", startDate);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDate", endDate);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:statementAccount", statementAccount);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String accountMiniStatementMobileAppB64(int startDateDay, int startDateMonth, int startDateYear, int endDateDay, int endDateMonth, int endDateYear,
                                                          String statementAccount) {
        String response = CBS_ERROR;

        String SOAPFunction = "AccountMiniStatementMobileAppB64";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:AccountMiniStatementMobileAppB64>
                            <sky:startDateDay></sky:startDateDay>
                            <sky:startDateMonth></sky:startDateMonth>
                            <sky:startDateYear></sky:startDateYear>
                            <sky:endDateDay></sky:endDateDay>
                            <sky:endDateMonth></sky:endDateMonth>
                            <sky:endDateYear></sky:endDateYear>
                            <sky:statementAccount></sky:statementAccount>
                        </sky:AccountMiniStatementMobileAppB64>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateDay", String.valueOf(startDateDay));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateMonth", String.valueOf(startDateMonth));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:startDateYear", String.valueOf(startDateYear));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateDay", String.valueOf(endDateDay));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateMonth", String.valueOf(endDateMonth));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:endDateYear", String.valueOf(endDateYear));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:statementAccount", statementAccount);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMemberYOB(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberYOB";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberYOB>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetMemberYOB>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMemberNationalID(String phoneNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMemberNationalID";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMemberNationalID>
                            <sky:phoneNo></sky:phoneNo>
                        </sky:GetMemberNationalID>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String updateGuarantorResponse(int loanEntryNo, String mobileNo, boolean accepted) {
        String response = CBS_ERROR;

        String SOAPFunction = "UpdateGuarantorResponse";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:UpdateGuarantorResponse>
                            <sky:loanEntryNo></sky:loanEntryNo>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:accepted></sky:accepted>
                        </sky:UpdateGuarantorResponse>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:loanEntryNo", String.valueOf(loanEntryNo));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accepted", String.valueOf(accepted));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String shareTradingLimits() {
        String response = CBS_ERROR;

        String SOAPFunction = "ShareTradingLimits";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ShareTradingLimits></sky:ShareTradingLimits>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getMaxSharesToTrade(String action, String accountNo, BigDecimal price) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetMaxSharesToTrade";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetMaxSharesToTrade>
                            <sky:action></sky:action>
                            <sky:accountNo></sky:accountNo>
                            <sky:price></sky:price>
                        </sky:GetMaxSharesToTrade>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:action", action);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:accountNo", accountNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:price", price.toString());

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    //validateIDNumber
    public static String validateIDNumber(String mobileNo, String iDNo) {
        String response = CBS_ERROR;

        String SOAPFunction = "ValidateIDNumber";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ValidateIDNumber>
                            <sky:mobileNo></sky:mobileNo>
                            <sky:iDNo></sky:iDNo>
                        </sky:ValidateIDNumber>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:mobileNo", mobileNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:iDNo", iDNo);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String getATMCard(String phone) {
        String response = CBS_ERROR;

        String SOAPFunction = "GetATMCard";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GetATMCard>
                            <sky:phone></sky:phone>
                        </sky:GetATMCard>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String stopATM(String phone, String pIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "StopATM";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="urn:microsoft-dynamics-schemas/codeunit/SkyMobile">
                    <x:Header/>
                    <x:Body>
                        <sky:StopATM>
                            <sky:phone></sky:phone>
                            <sky:pIN></sky:pIN>
                        </sky:StopATM>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phone", phone);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }


    public static TransactionWrapper<FlexicoreHashMap> getLoanFullStatement(String strRequestingMobileNumber,
                                                                            String theIdentifierType,
                                                                            String theIdentifier,
                                                                            String theDeviceIdentifierType,
                                                                            String theDeviceIdentifier,
                                                                            String theLoanSerialNumber,
                                                                            String theNumberOfEntries,
                                                                            String theStartDate,
                                                                            String theEndDate) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> accountFullStatementListWrapper = DeSaccoCBS.getLoanFullStatement(theIdentifierType, theIdentifier, theLoanSerialNumber,
                    theNumberOfEntries, theStartDate, theEndDate);

            if (accountFullStatementListWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanFullStatement() - " + accountFullStatementListWrapper.getErrors() + "\n" + accountFullStatementListWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", accountFullStatementListWrapper.getErrors() + " - " + accountFullStatementListWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap accountMiniStatementMap = accountFullStatementListWrapper.getSingleRecord();
            FlexicoreHashMap miniStatementResponseMap = accountMiniStatementMap.getFlexicoreHashMap("response_payload");


            String requestStatus = accountMiniStatementMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanFullStatement(" + theLoanSerialNumber + ")");
                accountMiniStatementMap.printRecordVerticalLabelled();


                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Loan Statement request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Loan Statement request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + miniStatementResponseMap.getStringValue("error_message") + " - " + miniStatementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanFullStatement(" + theLoanSerialNumber + ")");
                accountMiniStatementMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Statement request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Statement request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + miniStatementResponseMap.getStringValue("error_message") + " - " + miniStatementResponseMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String strMSG = "Dear member, your Mini-Statement request has FAILED. Please try again later.";

            FlexicoreArrayList allTransactionsList = miniStatementResponseMap.getFlexicoreArrayList("transactions");
            if (allTransactionsList == null || allTransactionsList.isEmpty()) {
                strMSG = "Dear member, your Loan Statement request has FAILED. No transaction(s) found for account " + theLoanSerialNumber + ".";
            } else {

                StringBuilder miniStatementMsg = new StringBuilder("Dear member, your Loan " + theLoanSerialNumber + " Statement:\n");

                for (FlexicoreHashMap transactionMap : allTransactionsList) {

                    String strMSGTransactionReference = transactionMap.getStringValueOrIfNull("transaction_reference", "").trim();
                    String strMSGTransactionDate = transactionMap.getStringValueOrIfNull("transaction_date", "").trim();
                    String strMSGTransactionTime = transactionMap.getStringValueOrIfNull("transaction_time", "").trim();
                    String strTransactionType = transactionMap.getStringValueOrIfNull("transaction_type", "").trim();

                    String strMSGRunningBalance = transactionMap.getStringValue("running_balance").trim();
                    //String strMSGIntRunningBalance = transactionMap.getStringValue("int_running_balance").trim();
                    String strMSGTransactionAmount = transactionMap.getStringValue("transaction_amount").trim();
                    String strMSGTransactionComments = transactionMap.getStringValueOrIfNull("transaction_description", "").trim();

                    strMSGTransactionAmount = strMSGTransactionAmount.replace("-", "");

                    if (strTransactionType.equalsIgnoreCase("DEBIT")) {
                        strMSGTransactionAmount = "-" + strMSGTransactionAmount;
                    }

                    transactionMap.putValue("transaction_amount", strMSGTransactionAmount);

                    //strMSGRunningBalance = strMSGRunningBalance.replace("-", "");
                    //strMSGIntRunningBalance = strMSGIntRunningBalance.replace("-", "");

                    //double totalPayable = Double.parseDouble(strMSGRunningBalance) + Double.parseDouble(strMSGIntRunningBalance);
                    //String strFormattedLoanTotalPayableAmount = Utils.formatDouble(String.valueOf(totalPayable), "#,##0.00");


                    String strMSGFormattedTransactionAmount = Utils.formatDouble(strMSGTransactionAmount, "#,##0.00");
                    String strMSGFormattedRunningBalance = Utils.formatDouble(strMSGRunningBalance, "#,##0.00");

                    //String strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");
                    String strMSGFormattedTransactionDateTime = DateTime.convertStringToDateToString(strMSGTransactionDate, "yyyy-MM-dd", "dd-MMM-yyyy");
                    strMSGFormattedTransactionDateTime = strMSGFormattedTransactionDateTime + " " + strMSGTransactionTime;

                    transactionMap.putValue("transaction_date_time", strMSGFormattedTransactionDateTime);

                    miniStatementMsg.append("Ref: ").append(strMSGTransactionReference).append("\n");
                    miniStatementMsg.append("Date: ").append(strMSGFormattedTransactionDateTime).append("\n");
                    miniStatementMsg.append("Amnt: ").append(strMSGFormattedTransactionAmount).append("\n");
                    miniStatementMsg.append("Descr: ").append(strMSGTransactionComments).append("\n");
                    miniStatementMsg.append("Run. Bal: KES ").append(strMSGFormattedRunningBalance).append("\n\n");
                }

                strMSG = miniStatementMsg.toString();
            }

            String strRunningBalance = miniStatementResponseMap.getStringValue("loan_balance").trim();
            // String strIntRunningBalance = accountMiniStatementMap.getStringValue("interest_balance").trim();

            //strRunningBalance = strRunningBalance.replace("-", "");
            //strIntRunningBalance = strIntRunningBalance.replace("-", "");

            // double totalPayable = Double.parseDouble(strRunningBalance) + Double.parseDouble(strIntRunningBalance);

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getFlexicoreHashMap("signatory_details");

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", allTransactionsList)
                    .putValue("account_name", miniStatementResponseMap.getStringValueOrIfNull("loan_name", "").trim())
                    .putValue("account_holder", signatoryDetailsMap.getStringValueOrIfNull("full_name", "").trim())
                    .putValue("account_available_balance", strRunningBalance)
                    .putValue("msg_object", cbsMSG));

            return resultWrapper;
        } catch (Exception e) {

            System.err.println(strRequestingMobileNumber + " => CBSAPI.getLoanFullStatement(" + theLoanSerialNumber + "): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Statement request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your Loan Statement request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_api_error_message", e.getMessage())
            );
        }
        return resultWrapper;
    }


    public static String changeEmailAddress(String phoneNo, String email, String pIN) {
        String response = CBS_ERROR;

        String SOAPFunction = "ChangeEmailAddress";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:ChangeEmailAddress>
                            <sky:phoneNo></sky:phoneNo>
                            <sky:email></sky:email>
                            <sky:pIN></sky:pIN>
                        </sky:ChangeEmailAddress>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:phoneNo", phoneNo);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:email", email);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:pIN", pIN);

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static String geTransactionCharges(String transactioType, BigDecimal amount) {
        String response = CBS_ERROR;

        String SOAPFunction = "GeTransactionCharges";
        String strRequestXml = """
                <x:Envelope
                    xmlns:x="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:sky="%s">
                    <x:Header/>
                    <x:Body>
                        <sky:GeTransactionCharges>
                            <sky:transactioType></sky:transactioType>
                            <sky:amount></sky:amount>
                        </sky:GeTransactionCharges>
                    </x:Body>
                </x:Envelope>
                """.formatted(Navision.params.getCoreBankingSKYPrefix());

        try {
            XmlObject requestXml = new XmlObject(strRequestXml, true, getNamespaceContext(XmlObject.MOBILE_SERVICE));
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:transactioType", transactioType);
            requestXml.update("/x:Envelope/x:Body/sky:" + SOAPFunction + "/sky:amount", amount.toString());

            String strResponseXml = HttpSOAP.sendRequest(SOAPFunction, requestXml.format(true));
            if (strResponseXml == null) throw new Exception("NULL response");

            XmlObject responseXml = new XmlObject(strResponseXml);
            response = responseXml.read("/Envelope/Body/" + SOAPFunction + "_Result/return_value");

            response = replaceChars(response);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Processed Response     : " + response);
            }

        } catch (Exception e) {
            System.err.println("CBSAPI." + SOAPFunction + "() Error making CBS API Request: " + e.getMessage());
        }

        return response;
    }

    public static TransactionWrapper<FlexicoreHashMap> setUserPIN(String theReferenceKey, String theIdentifierType, String theIdentifier, String theNewPIN,
                                                                  String theDeviceIdentifierType, String theDeviceIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(theReferenceKey, theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                return checkUserResultMapWrapper;
            }

            FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getValue("signatory_details");
            FlexicoreHashMap mobileBankingMap = checkUserResultMap.getValue("mobile_register_details");

            String strNewPIN = MobileBankingCryptography.hashPIN(signatoryDetailsMap.getStringValue("primary_mobile_number"), theNewPIN);


            FlexicoreHashMap theUpdateLoginParamsMap = new FlexicoreHashMap();
            theUpdateLoginParamsMap.put("pin", strNewPIN);
            theUpdateLoginParamsMap.put("pin_set_date", DateTime.getCurrentDateTime());
            theUpdateLoginParamsMap.put("ussd_activation_kyc", "DISABLED");
            theUpdateLoginParamsMap.put("ussd_activation_kyc_set_date", DateTime.getCurrentDateTime());

            if (mobileBankingMap.getStringValue("ussd_activation_date") == null) {
                theUpdateLoginParamsMap.put("ussd_activation_date", DateTime.getCurrentDateTime());
            }

            if (mobileBankingMap.getStringValue("accepted_terms_and_conditions").equalsIgnoreCase("NO")) {
                theUpdateLoginParamsMap.put("accepted_terms_and_conditions", "YES");
                theUpdateLoginParamsMap.put("channel_accepted_terms_and_conditions", "USSD");
                theUpdateLoginParamsMap.put("date_accepted_terms_and_conditions", DateTime.getCurrentDateTime());
            }

            String previousPins = mobileBankingMap.getStringValueOrIfNull("previous_pins", "<PREVIOUS_PINS/>");

            try {
                Document docPrevPasswords = XmlUtils.parseXml(previousPins);

                Element elPassword = docPrevPasswords.createElement("PIN");
                elPassword.setTextContent(strNewPIN);
                docPrevPasswords.getDocumentElement().appendChild(elPassword);

                previousPins = XmlUtils.convertXmlDocToStr(docPrevPasswords);

            } catch (Exception e) {
                e.printStackTrace();
            }

            theUpdateLoginParamsMap.put("previous_pins", previousPins);

            theUpdateLoginParamsMap.put("pin_status", "ACTIVE");
            theUpdateLoginParamsMap.put("date_modified", DateTime.getCurrentDateTime());

            mobileBankingMap.copyFrom(theUpdateLoginParamsMap);

            String integrityHash = MobileBankingCryptography.calculateIntegrityHash(mobileBankingMap);
            theUpdateLoginParamsMap.putValue("integrity_hash", integrityHash);

            TransactionWrapper<?> updateWrapper = Repository.update(
                    StringRefs.SENTINEL,
                    TBL_MOBILE_BANKING_REGISTER,
                    theUpdateLoginParamsMap,
                    new FilterPredicate("mobile_register_id = :mobile_register_id"),
                    new FlexicoreHashMap()
                            .addQueryArgument(":mobile_register_id", mobileBankingMap.getStringValue("mobile_register_id"))
            );

            if (updateWrapper.hasErrors()) {
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
            } else {
                resultWrapper.setData(checkUserResultMap);
            }

            return resultWrapper;

        } catch (Exception e) {
            System.err.println("CBSAPI.setPIN(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    private static String replaceChars(String xml) {
        if (xml != null && !xml.isEmpty()) {
            xml = xml.replaceAll("&lt;", "<");
            xml = xml.replaceAll("&gt;", ">");
        }
        return xml;
    }

    public static HashMap<String, String> getMemberDetails(String strIdentifierType, String strIdentifier) {
        HashMap<String, String> userIdentifierDetails = new HashMap<>();

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                SystemTables.TBL_CUSTOMER_REGISTER, "identifier_type, identifier, full_name",
                new FilterPredicate("identifier_type = :identifier_type AND identifier = :identifier"),
                new FlexicoreHashMap()
                        .addQueryArgument(":identifier_type", strIdentifierType)
                        .addQueryArgument(":identifier", strIdentifier));

        if (signatoryDetailsWrapper.hasErrors()) {
            return userIdentifierDetails;
        }

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        if (signatoryDetailsMap != null && !signatoryDetailsMap.isEmpty()) {
            userIdentifierDetails.put("identifier_type", signatoryDetailsMap.getStringValue("identifier_type"));
            userIdentifierDetails.put("identifier", signatoryDetailsMap.getStringValue("identifier"));
            userIdentifierDetails.put("full_name", signatoryDetailsMap.getStringValue("full_name"));
            return userIdentifierDetails;
        }

        return userIdentifierDetails;
    }

    public static HashMap<String, String> getMemberDetails(String strUserPhoneNumber) {
        HashMap<String, String> userIdentifierDetails = new HashMap<>();

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES, "identifier_type, identifier, full_name",
                new FilterPredicate("primary_mobile_number = :primary_mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":primary_mobile_number", strUserPhoneNumber));

        if (signatoryDetailsWrapper.hasErrors()) {
            return userIdentifierDetails;
        }

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        if (signatoryDetailsMap != null && !signatoryDetailsMap.isEmpty()) {
            userIdentifierDetails.put("identifier_type", signatoryDetailsMap.getStringValue("identifier_type"));
            userIdentifierDetails.put("identifier", signatoryDetailsMap.getStringValue("identifier"));
            userIdentifierDetails.put("full_name", signatoryDetailsMap.getStringValue("full_name"));
            return userIdentifierDetails;
        }

        return userIdentifierDetails;
    }

    public static TransactionWrapper<FlexicoreHashMap> validateAccountNumber(String strRequestingMobileNumber,
                                                                             String theIdentifierType,
                                                                             String theIdentifier,
                                                                             String theAccountNumber) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreHashMap> accountDetailsWrapper = DeSaccoCBS.getAccountDetails(theIdentifierType, theIdentifier, theAccountNumber);

            if (accountDetailsWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountDetails() - " + accountDetailsWrapper.getErrors() + "\n" + accountDetailsWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("cbs_api_error_message", accountDetailsWrapper.getErrors() + " - " + accountDetailsWrapper.getMessages())
                );

                return resultWrapper;
            }

            FlexicoreHashMap accountDetailsResultMap = accountDetailsWrapper.getSingleRecord();

            String requestStatus = accountDetailsResultMap.getStringValue("request_status");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getAccountDetails(" + theAccountNumber + ")");
                accountDetailsResultMap.printRecordVerticalLabelled();

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, account " + theAccountNumber + " not found")
                        .putValue("cbs_api_error_message", "Sorry, account " + theAccountNumber + " not found")
                );

                return resultWrapper;
            }

            FlexicoreHashMap accountDetailsResponseMap = accountDetailsResultMap.getFlexicoreHashMap("response_payload");

            resultWrapper.setData(accountDetailsResponseMap);

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(theIdentifier + " => CBSAPI.validateAccountNumber(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> mobileMoneyResult(
            PESA pesa,
            String strRequestingMobileNumber,
            String theIdentifierType,
            String theIdentifier,
            String theOriginatorId,
            String theBeneficiaryIdentifierType,
            String theBeneficiaryIdentifier,
            String theBeneficiaryName,
            String theBeneficiaryOtherDetails,
            String theBeneficiaryReference,
            String theTransactionDateTime) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> withdrawalResultWrapper = DeSaccoCBS.withdrawalResult(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    "CONFIRMED",
                    "Transaction Completed Successfully",
                    theBeneficiaryIdentifierType,
                    theBeneficiaryIdentifier,
                    theBeneficiaryName,
                    theBeneficiaryOtherDetails,
                    theBeneficiaryReference,
                    theTransactionDateTime
            );

            if (withdrawalResultWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.mobileMoneyResult() - " + withdrawalResultWrapper.getErrors() + "\n" + withdrawalResultWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                );

                return resultWrapper;
            }

            FlexicoreHashMap withdrawalResultMap = withdrawalResultWrapper.getSingleRecord();
            String requestStatus = withdrawalResultMap.getStringValue("request_status");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                String errorMessage = withdrawalResultMap.getFlexicoreHashMap("response_payload").getStringValue("error_message");

                if (requestStatus.equalsIgnoreCase("DUPLICATE")) {
                    resultWrapper.setHasErrors(false);
                    resultWrapper.setData(new FlexicoreHashMap()
                            .putValue("end_session", USSDAPIConstants.Condition.NO)
                            .putValue("cbs_api_return_val", "TRANSACTION_EXISTS")
                            .putValue("cbs_response_message", errorMessage)
                            .putValue("display_message", errorMessage + ". Originator ID " + theOriginatorId + "." + getTrailerMessage()));

                    return resultWrapper;
                }

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.mobileMoneyResult(" + theOriginatorId + ")");
                withdrawalResultMap.printRecordVerticalLabelled();

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("cbs_response_message", errorMessage)
                        .putValue("display_message", "Failed to Complete Mobile Money for Originator ID " + theOriginatorId + "." + getTrailerMessage()));

                return resultWrapper;
            }


            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", "SUCCESS")
                    .putValue("cbs_response_message", "Transaction Completed Successfully"));

            //resultWrapper.setData(withdrawalResultMap.getFlexicoreHashMap("response_payload"));
            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.mobileMoneyResult(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG));
        }
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> mobileMoneyWithdrawal(String strRequestingMobileNumber,
                                                                             String theIdentifierType,
                                                                             String theIdentifier,
                                                                             String theDeviceIdentifierType,
                                                                             String theDeviceIdentifier,
                                                                             String theOriginatorId,
                                                                             String theProductId,
                                                                             String thePesaType,
                                                                             String theAction,
                                                                             String theCommand,
                                                                             FlexicoreHashMap theInitiatorDetailsMap,
                                                                             FlexicoreHashMap theSourceDetailsMap,
                                                                             FlexicoreHashMap theSenderDetailsMap,
                                                                             FlexicoreHashMap theReceiverDetailsMap,
                                                                             FlexicoreHashMap theBeneficiaryDetailsMap,
                                                                             double theAmount,
                                                                             String theCategory,
                                                                             String theTransactionDescription,
                                                                             String theSourceReference,
                                                                             String theRequestApplication,
                                                                             String theSourceApplication) {
        TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
        FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

        if (checkUserResultMapWrapper.hasErrors()) {
            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Dear member, an error occurred while processing your Cash Withdrawal request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            checkUserResultMap.putValue("msg_object", cbsMSG);

            return checkUserResultMapWrapper;
        }

        FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getFlexicoreHashMap("signatory_details");

        return mobileMoneyWithdrawal(signatoryDetailsMap, strRequestingMobileNumber,
                theIdentifierType,
                theIdentifier,
                theDeviceIdentifierType,
                theDeviceIdentifier,
                theOriginatorId,
                theProductId,
                thePesaType,
                theAction,
                theCommand,
                theInitiatorDetailsMap,
                theSourceDetailsMap,
                theSenderDetailsMap,
                theReceiverDetailsMap,
                theBeneficiaryDetailsMap,
                theAmount,
                theCategory,
                theTransactionDescription,
                theSourceReference,
                theRequestApplication,
                theSourceApplication);
    }

    public static TransactionWrapper<FlexicoreHashMap> mobileMoneyWithdrawal(
            FlexicoreHashMap signatoryDetailsMap,
            String strRequestingMobileNumber,
            String theIdentifierType,
            String theIdentifier,
            String theDeviceIdentifierType,
            String theDeviceIdentifier,
            String theOriginatorId,
            String theProductId,
            String thePesaType,
            String theAction,
            String theCommand,
            FlexicoreHashMap theInitiatorDetailsMap,
            FlexicoreHashMap theSourceDetailsMap,
            FlexicoreHashMap theSenderDetailsMap,
            FlexicoreHashMap theReceiverDetailsMap,
            FlexicoreHashMap theBeneficiaryDetailsMap,
            double theAmount,
            String theCategory,
            String theTransactionDescription,
            String theSourceReference,
            String theRequestApplication,
            String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> cashWithdrawalWrapper = DeSaccoCBS.withdrawal(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    theProductId,
                    thePesaType,
                    theAction,
                    theCommand,
                    theInitiatorDetailsMap,
                    theSourceDetailsMap,
                    theSenderDetailsMap,
                    theReceiverDetailsMap,
                    theBeneficiaryDetailsMap,
                    theAmount,
                    theCategory,
                    theTransactionDescription,
                    theSourceReference,
                    theRequestApplication,
                    theSourceApplication,
                    "MPESA");

            if (cashWithdrawalWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => UnSaccoCBS.mobileMoneyWithdrawal() - " + cashWithdrawalWrapper.getErrors() + "\n" + cashWithdrawalWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Cash Withdrawal request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap cashWithdrawalResultMap = cashWithdrawalWrapper.getSingleRecord();

            String requestStatus = cashWithdrawalResultMap.getStringValue("request_status");


            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {
                final USSDAPI theUSSDAPI = new USSDAPI();
                String strMobileNumber = String.valueOf(strRequestingMobileNumber);
                String strFirstName = theUSSDAPI.getUserFirstName(strMobileNumber);

                System.err.println(strRequestingMobileNumber + " => UnSaccoCBS.mobileMoneyWithdrawal(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                cashWithdrawalResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Cash Withdrawal request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Cash Withdrawal request.")
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (requestStatus.equalsIgnoreCase("BLOCKED")) {
                System.err.println(strRequestingMobileNumber + " => UnSaccoCBS.mobileMoneyWithdrawal(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                cashWithdrawalResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n")
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => UnSaccoCBS.mobileMoneyWithdrawal(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                cashWithdrawalResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            FlexicoreHashMap withdrawalMap = cashWithdrawalResultMap.getFlexicoreHashMap("response_payload");

            String strTransactionDateTime = withdrawalMap.getStringValueOrIfNull("transaction_date_time", "").trim();
            String strFormattedDateTime = Utils.formatDate(strTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");
            String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");

            HashMap<String, String> memberDetails = new HashMap<>();

            if(theIdentifierType.equalsIgnoreCase("MSISDN")){
                memberDetails = CBSAPI.getMemberDetails(theIdentifier);
            }else{
                memberDetails = CBSAPI.getMemberDetails(theIdentifierType, theIdentifier);
            }

            String firstName = "Member";

            // String strFullName = memberDetails.get("full_name");
            //
            // if(strFullName != null && !strFullName.isBlank()){
            //     firstName = strFullName.split(" ")[1];
            // }

            String strMSG = "Dear "+ firstName+", your Cash Withdrawal Request of KES " + strFormattedAmount + " to " + theBeneficiaryDetailsMap.getStringValue("account") + " has been received and is being processed.";

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);


            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Cash Withdrawal has been received successfully and is being processed.")
                    .putValue("msg_object", cbsMSG)
                    .putValue("response_payload", withdrawalMap)
                    .putValue("customer_full_name", signatoryDetailsMap.getStringValue("full_name"))
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.mobileMoneyWithdrawal(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Cash Withdrawal request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> reverseMobileMoneyWithdrawal(
            String strRequestingMobileNumber,
            String theIdentifierType,
            String theIdentifier,
            String theOriginatorId,
            String theBeneficiaryIdentifierType,
            String theBeneficiaryIdentifier,
            String theBeneficiaryName,
            String theBeneficiaryOtherDetails,
            String theBeneficiaryReference,
            String theTransactionDateTime) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> reversalWrapper = DeSaccoCBS.withdrawalResult(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    "REVERSE_CONFIRMED",
                    "Reversal Request",
                    theBeneficiaryIdentifierType,
                    theBeneficiaryIdentifier,
                    theBeneficiaryName,
                    theBeneficiaryOtherDetails,
                    theBeneficiaryReference,
                    theTransactionDateTime
            );

            if (reversalWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => UnSaccoCBS.reverseMobileMoneyWithdrawal() - " + reversalWrapper.getErrors() + "\n" + reversalWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                );

                return resultWrapper;
            }

            FlexicoreHashMap reversalResultMap = reversalWrapper.getSingleRecord();
            String requestStatus = reversalResultMap.getStringValue("request_status");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => UnSaccoCBS.reverseMobileMoneyWithdrawal(" + theOriginatorId + ")");
                reversalResultMap.printRecordVerticalLabelled();

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Failed to Reverse Mobile Money for Originator ID " + theOriginatorId + "." + getTrailerMessage()));

                return resultWrapper;
            }

            resultWrapper.setData(reversalResultMap.getFlexicoreHashMap("response_payload"));
            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.reverseMobileMoneyWithdrawal(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG));
        }
        return resultWrapper;
    }

    /* ******************************************************************************************************************* */
    /* ******************************************************************************************************************* */
    /* ******************************************************************************************************************* */
    /* ******************************************************************************************************************* */


    public static TransactionWrapper<FlexicoreHashMap> bankTransferViaB2B(String strRequestingMobileNumber,
                                                                          String theIdentifierType,
                                                                          String theIdentifier,
                                                                          String theDeviceIdentifierType,
                                                                          String theDeviceIdentifier,
                                                                          String theOriginatorId,
                                                                          String theProductId,
                                                                          String thePesaType,
                                                                          String theAction,
                                                                          String theCommand,
                                                                          FlexicoreHashMap theInitiatorDetailsMap,
                                                                          FlexicoreHashMap theSourceDetailsMap,
                                                                          FlexicoreHashMap theSenderDetailsMap,
                                                                          FlexicoreHashMap theReceiverDetailsMap,
                                                                          FlexicoreHashMap theBeneficiaryDetailsMap,
                                                                          double theAmount,
                                                                          String theCategory,
                                                                          String theTransactionDescription,
                                                                          String theSourceReference,
                                                                          String theRequestApplication,
                                                                          String theSourceApplication) {

        //theDeviceIdentifier = null;

        /*TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
        FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

        if (checkUserResultMapWrapper.hasErrors()) {
            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Dear member, an error occurred while processing your Transfer to Bank request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            checkUserResultMap.putValue("msg_object", cbsMSG);

            return checkUserResultMapWrapper;
        }

                FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getFlexicoreHashMap("signatory_details");

         */

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES, new FilterPredicate("primary_mobile_number = :primary_mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":primary_mobile_number", theIdentifier));

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        return bankTransferViaB2B(signatoryDetailsMap, strRequestingMobileNumber,
                theIdentifierType,
                theIdentifier,
                theDeviceIdentifierType,
                theDeviceIdentifier,
                theOriginatorId,
                theProductId,
                thePesaType,
                theAction,
                theCommand,
                theInitiatorDetailsMap,
                theSourceDetailsMap,
                theSenderDetailsMap,
                theReceiverDetailsMap,
                theBeneficiaryDetailsMap,
                theAmount,
                theCategory,
                theTransactionDescription,
                theSourceReference,
                theRequestApplication,
                theSourceApplication);
    }

    public static TransactionWrapper<FlexicoreHashMap> bankTransferViaB2B(
            FlexicoreHashMap signatoryDetailsMap,
            String strRequestingMobileNumber,
            String theIdentifierType,
            String theIdentifier,
            String theDeviceIdentifierType,
            String theDeviceIdentifier,
            String theOriginatorId,
            String theProductId,
            String thePesaType,
            String theAction,
            String theCommand,
            FlexicoreHashMap theInitiatorDetailsMap,
            FlexicoreHashMap theSourceDetailsMap,
            FlexicoreHashMap theSenderDetailsMap,
            FlexicoreHashMap theReceiverDetailsMap,
            FlexicoreHashMap theBeneficiaryDetailsMap,
            double theAmount,
            String theCategory,
            String theTransactionDescription,
            String theSourceReference,
            String theRequestApplication,
            String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> bankTransferWrapper = DeSaccoCBS.withdrawal(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    theProductId,
                    thePesaType,
                    theAction,
                    theCommand,
                    theInitiatorDetailsMap,
                    theSourceDetailsMap,
                    theSenderDetailsMap,
                    theReceiverDetailsMap,
                    theBeneficiaryDetailsMap,
                    theAmount,
                    theCategory,
                    theTransactionDescription,
                    theSourceReference,
                    theRequestApplication,
                    theSourceApplication,
                    "BANK_TRANSFER");

            if (bankTransferWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.bankTransferViaB2B() - " + bankTransferWrapper.getErrors() + "\n" + bankTransferWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Transfer to Bank request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap bankTransferResultMap = bankTransferWrapper.getSingleRecord();

            String requestStatus = bankTransferResultMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.bankTransferViaB2B(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                bankTransferResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Transfer to Bank request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Transfer to Bank request." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (requestStatus.equalsIgnoreCase("BLOCKED")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.bankTransferViaB2B(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n")
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.bankTransferViaB2B(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                bankTransferResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Transfer to Bank request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Transfer to Bank request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            FlexicoreHashMap bankTransferMap = bankTransferResultMap.getFlexicoreHashMap("response_payload");

            String strTransactionDateTime = bankTransferMap.getStringValueOrIfNull("transaction_date_time", "").trim();
            String strFormattedDateTime = Utils.formatDate(strTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");
            String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");

            String strMSG = "Dear member, your Bank Transfer Request of KES " + strFormattedAmount + " to " +
                    theReceiverDetailsMap.getStringValue("name") + " - " +
                    theReceiverDetailsMap.getStringValue("identifier") + ", " +
                    "for account " +
                    theReceiverDetailsMap.getStringValue("account") + " has been received and is being processed.";

            /*
                String strMSG = "Dear member, Cash Withdrawal completed Successfully.\n";
                strMSG += "Source A/C: " + theSourceAccountNumber + "\n";
                strMSG += "Mobile: " + theBeneficiaryMobileNo + "\n";
                strMSG += "Amount: KES " + Utils.formatDouble(theAmount, "#,##0.00") + "\n";
                // strMSG += "Date: "+strBeneficiaryName+"";
            */

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Transfer to Bank has been received successfully and is being processed.")
                    .putValue("msg_object", cbsMSG)
                    .putValue("response_payload", bankTransferMap)
                    .putValue("customer_full_name", signatoryDetailsMap.getStringValue("full_name"))
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.bankTransferViaB2B(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Transfer to Bank request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> mobileMoneyDeposit(PESA thePESAIN) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> mobileMoneyDepositWrapper = DeSaccoCBS.cashDeposit(
                    "MSISDN",
                    thePESAIN.getInitiatorIdentifier(),
                    thePESAIN.getOriginatorID(),
                    String.valueOf(thePESAIN.getProductID()),
                    thePESAIN.getPESAType().getValue(),
                    thePESAIN.getPESAAction().getValue(),
                    thePESAIN.getCommand(),
                    thePESAIN.getSensitivity().getValue(),
                    String.valueOf(thePESAIN.getChargeApplied()),
                    //initiator
                    new FlexicoreHashMap()
                            .putValue("identifier_type", thePESAIN.getInitiatorType())
                            .putValue("identifier", thePESAIN.getInitiatorIdentifier())
                            .putValue("account", thePESAIN.getInitiatorAccount())
                            .putValue("name", thePESAIN.getInitiatorName())
                            .putValue("reference", thePESAIN.getInitiatorReference())
                            .putValue("other_details", thePESAIN.getInitiatorOtherDetails()),
                    //source
                    new FlexicoreHashMap()
                            .putValue("identifier_type", thePESAIN.getSourceType())
                            .putValue("identifier", thePESAIN.getSourceIdentifier())
                            .putValue("account", thePESAIN.getSourceAccount())
                            .putValue("name", thePESAIN.getSourceName())
                            .putValue("reference", thePESAIN.getSourceReference())
                            .putValue("other_details", thePESAIN.getSourceOtherDetails()),
                    //sender
                    new FlexicoreHashMap()
                            .putValue("identifier_type", thePESAIN.getSenderType())
                            .putValue("identifier", thePESAIN.getSenderIdentifier())
                            .putValue("account", thePESAIN.getSenderAccount())
                            .putValue("name", thePESAIN.getSenderName())
                            .putValue("reference", thePESAIN.getSenderReference())
                            .putValue("other_details", thePESAIN.getSenderOtherDetails()),
                    //receiver
                    new FlexicoreHashMap()
                            .putValue("identifier_type", thePESAIN.getReceiverType())
                            .putValue("identifier", thePESAIN.getReceiverIdentifier())
                            .putValue("account", thePESAIN.getReceiverAccount())
                            .putValue("name", thePESAIN.getReceiverName())
                            .putValue("reference", thePESAIN.getReceiverReference())
                            .putValue("other_details", thePESAIN.getReceiverOtherDetails()),

                    //beneficiary
                    new FlexicoreHashMap()
                            .putValue("identifier_type", "ACCOUNT_NO")
                            .putValue("identifier", thePESAIN.getReceiverAccount()) //DONE INTENTIONALLY
                            .putValue("account", thePESAIN.getReceiverAccount())
                            .putValue("name", thePESAIN.getBeneficiaryName())
                            .putValue("reference", thePESAIN.getReceiverReference())
                            .putValue("other_details", thePESAIN.getReceiverOtherDetails()),

                    thePESAIN.getTransactionAmount(),
                    thePESAIN.getCategory(),
                    thePESAIN.getTransactionRemark() + "-" + thePESAIN.getBeneficiaryName(),
                    thePESAIN.getReceiverReference(),
                    "MBANKING",
                    thePESAIN.getCorrelationApplication());

            if (mobileMoneyDepositWrapper.hasErrors()) {
                System.err.println(thePESAIN.getInitiatorIdentifier() + " => DeSaccoCBS.mobileMoneyDeposit() - " + mobileMoneyDepositWrapper.getErrors() + "\n" + mobileMoneyDepositWrapper.getMessages());

                //USSDAPIConstants.StandardReturnVal standardReturnVal = insertToDepositsReconciliation(thePESAIN, theTransactionDestinationRefForFailure, mobileMoneyDepositWrapper.getErrors());

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("cbs_response_message", mobileMoneyDepositWrapper.getErrors()));

                return resultWrapper;
            }

            FlexicoreHashMap mobileMoneyDepositMap = mobileMoneyDepositWrapper.getSingleRecord();

            String requestStatus = mobileMoneyDepositMap.getStringValue("request_status");

            FlexicoreHashMap depositMap = mobileMoneyDepositMap.getFlexicoreHashMap("response_payload");

            if (requestStatus.equalsIgnoreCase("DUPLICATE")) {
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                        .putValue("cbs_response_message", "Transaction already exists.")
                        .putValue("cbs_transaction_reference", thePESAIN.getReceiverReference()));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(thePESAIN.getInitiatorIdentifier() + " => DeSaccoCBS.mobileMoneyDeposit(" + thePESAIN.getBeneficiaryIdentifier() + ", " + thePESAIN.getReceiverAccount() + ")");
                mobileMoneyDepositMap.printRecordVerticalLabelled();

                String strMessage = depositMap.getStringValueOrIfNull("error_message", "") + " - " + depositMap.getStringValueOrIfNull("devMessage", "");

                //USSDAPIConstants.StandardReturnVal standardReturnVal = insertToDepositsReconciliation(thePESAIN, theTransactionDestinationRefForFailure, strMessage);

                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("cbs_response_message", strMessage));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("cbs_response_message", "Cash Deposit has been received successfully and is being processed.")
                    .putValue("cbs_transaction_reference", depositMap.getStringValue("transaction_reference")));

            String strFormattedDateTime = Utils.formatDate(thePESAIN.getPesaDateCreated(), "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");

           /* String strMSG = "Dear member, account " + thePESAIN.getReceiverAccount() + " has been credited with amount KES " + Utils.formatDouble(thePESAIN.getTransactionAmount(), "#,##0.00") +
                            ". MPesa Ref: " + thePESAIN.getOriginatorID();

            int intMSGSent = fnSendSMS(thePESAIN.getSourceIdentifier(), strMSG, "YES", MSGConstants.MSGMode.SAF, 210, "MPESA_DEPOSIT", "MBANKING_SERVER", "MPESA_BROKER",
                    thePESAIN.getOriginatorID(), thePESAIN.getSourceReference());*/

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(thePESAIN.getInitiatorIdentifier() + " => CBSAPI.mobileMoneyDeposit(): " + e.getMessage());
            e.printStackTrace();

            // USSDAPIConstants.StandardReturnVal standardReturnVal = insertToDepositsReconciliation(thePESAIN, theTransactionDestinationRefForFailure, Misc.stringifyStackTrace(e));

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("cbs_response_message", e.getMessage()));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getCharges(String strRequestingMobileNumber,
                                                                  String theIdentifierType,
                                                                  String theIdentifier,

                                                                  String theChargeAction,
                                                                  double theAmount
    ) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> getChargesWrapper = DeSaccoCBS.getCharges(
                    theIdentifierType,
                    theIdentifier,
                    "",
                    theChargeAction,
                    theAmount);

            if (getChargesWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCharges(" + theChargeAction + ", " + theAmount + ") - " + getChargesWrapper.getErrors() + "\n" + getChargesWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("title", "Transaction Error")
                        .putValue("cbs_api_error_message", getChargesWrapper.getErrors() + " - " + getChargesWrapper.getMessages())
                );

                return getChargesWrapper;
            }

            FlexicoreHashMap getChargesResultMap = getChargesWrapper.getSingleRecord();
            String requestStatus = getChargesResultMap.getStringValue("request_status");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCharges(" + theChargeAction + ", " + theAmount + ")");
                getChargesResultMap.printRecordVerticalLabelled();

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("cbs_api_error_message", getChargesResultMap.getStringValue("error_message") + " - " + getChargesResultMap.getStringValue("devMessage"))
                );

                return resultWrapper;
            }
            FlexicoreHashMap getChargesMap = getChargesResultMap.getFlexicoreHashMap("response_payload");

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS.getValue())
                    .putValue("charge_amount", getChargesMap.getValue("charge_amount")));

            return resultWrapper;

        } catch (Exception e) {
            System.out.println(theIdentifier + " => CBSAPI.getCharges(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR.getValue())
                    .putValue("cbs_api_error_message", e.getMessage()));
        }

        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getCharges(String strRequestingMobileNumber,
                                                                  String theIdentifierType,
                                                                  String theIdentifier,
                                                                  String accountNumber,
                                                                  String theChargeAction,
                                                                  double theAmount
    ) {
        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> getChargesWrapper = DeSaccoCBS.getCharges(
                    theIdentifierType,
                    theIdentifier,
                    accountNumber,
                    theChargeAction,
                    theAmount);

            if (getChargesWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCharges(" + theChargeAction + ", " + theAmount + ") - " + getChargesWrapper.getErrors() + "\n" + getChargesWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("title", "Transaction Error")
                        .putValue("cbs_api_error_message", getChargesWrapper.getErrors() + " - " + getChargesWrapper.getMessages())
                );

                return getChargesWrapper;
            }

            FlexicoreHashMap getChargesResultMap = getChargesWrapper.getSingleRecord();
            String requestStatus = getChargesResultMap.getStringValue("request_status");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getCharges(" + theChargeAction + ", " + theAmount + ")");
                getChargesResultMap.printRecordVerticalLabelled();

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later.")
                        .putValue("cbs_api_error_message", getChargesResultMap.getStringValue("error_message") + " - " + getChargesResultMap.getStringValue("devMessage"))
                );

                return resultWrapper;
            }
            FlexicoreHashMap getChargesMap = getChargesResultMap.getFlexicoreHashMap("response_payload");

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS.getValue())
                    .putValue("charge_amount", getChargesMap.getValue("charge_amount")));

            return resultWrapper;

        } catch (Exception e) {
            System.out.println(theIdentifier + " => CBSAPI.getCharges(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR.getValue())
                    .putValue("cbs_api_error_message", e.getMessage()));
        }

        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> internalFundsTransfer(String strRequestingMobileNumber,
                                                                             String theIdentifierType,
                                                                             String theIdentifier,
                                                                             String theDeviceIdentifierType,
                                                                             String theDeviceIdentifier,
                                                                             String theOriginatorId,
                                                                             String theSourceAccount,
                                                                             String theDestinationAccount,
                                                                             double theAmount,
                                                                             String theTransactionDescription,
                                                                             String theSourceReference,
                                                                             String theRequestApplication,
                                                                             String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Funds Transfer request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);
                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> internalFundsTransferWrapper = DeSaccoCBS.internalFundsTransfer(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    theSourceAccount,
                    theDestinationAccount,
                    theAmount,
                    theTransactionDescription,
                    theSourceReference,
                    theRequestApplication,
                    theSourceApplication);

            if (internalFundsTransferWrapper.hasErrors()) {
                System.err.println(theIdentifier + " => DeSaccoCBS.internalFundsTransfer() - " + internalFundsTransferWrapper.getErrors() + "\n" + internalFundsTransferWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Funds Transfer request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "Transaction Error")
                        .putValue("cbs_api_error_message", internalFundsTransferWrapper.getErrors() + " - " + internalFundsTransferWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap internalFundsTransferResultMap = internalFundsTransferWrapper.getSingleRecord();

            internalFundsTransferResultMap.printRecordVerticalLabelled();

            String requestStatus = internalFundsTransferResultMap.getStringValue("request_status");
            FlexicoreHashMap fundsTransferMap = internalFundsTransferResultMap.getFlexicoreHashMap("response_payload");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.internalFundsTransfer(" + theSourceAccount + ")");
                internalFundsTransferResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, the source account " + theSourceAccount + " balance is insufficient to process your Funds Transfer request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Funds Transfer request." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + fundsTransferMap.getStringValue("error_message") + " - " + fundsTransferMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.internalFundsTransfer(" + theSourceAccount + ")");
                internalFundsTransferResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();

                /*if (fundsTransferMap.getStringValue("error_message").contains("Failed to determine transaction destination")) {
                    String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");
                    cbsMSG.setMessage("Dear member, your Request for Funds Transfer of KES " + strFormattedAmount + " to account " + theDestinationAccount + " FAILED. Destination Account provided not found.");
                } else {
                    cbsMSG.setMessage("Sorry, an error occurred while processing your Funds Transfer request. Please try again later.");
                }*/

                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Funds Transfer request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + fundsTransferMap.getStringValue("error_message") + " - " + fundsTransferMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            //String strDateTime = MBankingDB.getDBDateTime();
            String strFormattedDateTime = DateTime.getCurrentDateTime("dd-MMM-yyyy HH:mm:ss");
            String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");

            String strMSG = "Dear member, your Request for Funds Transfer of KES " + strFormattedAmount + " to account " + theDestinationAccount + " on " + strFormattedDateTime + " has been received. Kindly wait as it is being processed.";

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Funds transfer received successfully.")
                    .putValue("title", "Transaction Accepted")
                    .putValue("msg_object", cbsMSG)
                    .putValue("cbs_transaction_reference", fundsTransferMap.getStringValue("transaction_reference"))
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(theIdentifier + " => CBSAPI.internalFundsTransfer(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Qualification Check request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("title", "Transaction Error")
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("cbs_api_error_message", e.getMessage())
                    .putValue("msg_object", cbsMSG)

            );
        }
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> buyAirtime(String strRequestingMobileNumber,
                                                                  String theIdentifierType,
                                                                  String theIdentifier,
                                                                  String theDeviceIdentifierType,
                                                                  String theDeviceIdentifier,
                                                                  String theOriginatorId,
                                                                  String theProductId,
                                                                  String thePesaType,
                                                                  String theAction,
                                                                  String theCommand,
                                                                  FlexicoreHashMap theInitiatorDetailsMap,
                                                                  FlexicoreHashMap theSourceDetailsMap,
                                                                  FlexicoreHashMap theSenderDetailsMap,
                                                                  FlexicoreHashMap theReceiverDetailsMap,
                                                                  FlexicoreHashMap theBeneficiaryDetailsMap,
                                                                  double theAmount,
                                                                  String theCategory,
                                                                  String theTransactionDescription,
                                                                  String theSourceReference,
                                                                  String theRequestApplication,
                                                                  String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
        FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

        if (checkUserResultMapWrapper.hasErrors()) {
            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Dear member, an error occurred while processing your Airtime Purchase request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            checkUserResultMap.putValue("msg_object", cbsMSG);

            return checkUserResultMapWrapper;
        }

        FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getFlexicoreHashMap("signatory_details");

        return buyAirtime(signatoryDetailsMap, strRequestingMobileNumber,
                theIdentifierType,
                theIdentifier,
                theDeviceIdentifierType,
                theDeviceIdentifier,
                theOriginatorId,
                theProductId,
                thePesaType,
                theAction,
                theCommand,
                theInitiatorDetailsMap,
                theSourceDetailsMap,
                theSenderDetailsMap,
                theReceiverDetailsMap,
                theBeneficiaryDetailsMap,
                theAmount,
                theCategory,
                theTransactionDescription,
                theSourceReference,
                theRequestApplication,
                theSourceApplication);
    }

    public static TransactionWrapper<FlexicoreHashMap> buyAirtime(
            FlexicoreHashMap signatoryDetailsMap,
            String strRequestingMobileNumber,
            String theIdentifierType,
            String theIdentifier,
            String theDeviceIdentifierType,
            String theDeviceIdentifier,
            String theOriginatorId,
            String theProductId,
            String thePesaType,
            String theAction,
            String theCommand,
            FlexicoreHashMap theInitiatorDetailsMap,
            FlexicoreHashMap theSourceDetailsMap,
            FlexicoreHashMap theSenderDetailsMap,
            FlexicoreHashMap theReceiverDetailsMap,
            FlexicoreHashMap theBeneficiaryDetailsMap,
            double theAmount,
            String theCategory,
            String theTransactionDescription,
            String theSourceReference,
            String theRequestApplication,
            String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {


            TransactionWrapper<FlexicoreHashMap> buyAirtimeWrapper = DeSaccoCBS.withdrawal(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    theProductId,
                    thePesaType,
                    theAction,
                    theCommand,
                    theInitiatorDetailsMap,
                    theSourceDetailsMap,
                    theSenderDetailsMap,
                    theReceiverDetailsMap,
                    theBeneficiaryDetailsMap,
                    theAmount,
                    theCategory,
                    theTransactionDescription,
                    theSourceReference,
                    theRequestApplication,
                    theSourceApplication,
                    "BUY_AIRTIME");

            if (buyAirtimeWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.buyAirtime() - " + buyAirtimeWrapper.getErrors() + "\n" + buyAirtimeWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Airtime Purchase request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap buyAirtimeResultMap = buyAirtimeWrapper.getSingleRecord();

            String requestStatus = buyAirtimeResultMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.buyAirtime(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                buyAirtimeResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Airtime Purchase request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Airtime Purchase request." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (requestStatus.equalsIgnoreCase("BLOCKED")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.buyAirtime(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n")
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.buyAirtime(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                buyAirtimeResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Airtime Purchase request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Airtime Purchase request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            FlexicoreHashMap buyAirtimeMap = buyAirtimeResultMap.getFlexicoreHashMap("response_payload");

            String strTransactionDateTime = buyAirtimeMap.getStringValueOrIfNull("transaction_date_time", "").trim();
            String strFormattedDateTime = Utils.formatDate(strTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");
            String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");

            String strMSG = "Dear member, your Airtime Purchase Request of KES " + Utils.formatDouble(theAmount, "#,##0.00") + " to " +
                    theBeneficiaryDetailsMap.getStringValue("account") + " has been received and is being processed.";


            /*
                String strMSG = "Dear member, Cash Withdrawal completed Successfully.\n";
                strMSG += "Source A/C: " + theSourceAccountNumber + "\n";
                strMSG += "Mobile: " + theBeneficiaryMobileNo + "\n";
                strMSG += "Amount: KES " + Utils.formatDouble(theAmount, "#,##0.00") + "\n";
                // strMSG += "Date: "+strBeneficiaryName+"";
            */

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Airtime Purchase has been received successfully and is being processed.")
                    .putValue("msg_object", cbsMSG)
                    .putValue("response_payload", buyAirtimeMap)
                    .putValue("customer_full_name", signatoryDetailsMap.getStringValue("full_name"))
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.buyAirtime(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Airtime Purchase request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG));
        }
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> utilitiesPayment(String strRequestingMobileNumber,
                                                                        String theIdentifierType,
                                                                        String theIdentifier,
                                                                        String theDeviceIdentifierType,
                                                                        String theDeviceIdentifier,
                                                                        String theOriginatorId,
                                                                        String theProductId,
                                                                        String thePesaType,
                                                                        String theAction,
                                                                        String theCommand,
                                                                        FlexicoreHashMap theInitiatorDetailsMap,
                                                                        FlexicoreHashMap theSourceDetailsMap,
                                                                        FlexicoreHashMap theSenderDetailsMap,
                                                                        FlexicoreHashMap theReceiverDetailsMap,
                                                                        FlexicoreHashMap theBeneficiaryDetailsMap,
                                                                        double theAmount,
                                                                        String theCategory,
                                                                        String theTransactionDescription,
                                                                        String theSourceReference,
                                                                        String theRequestApplication,
                                                                        String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
        FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

        if (checkUserResultMapWrapper.hasErrors()) {
            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Dear member, an error occurred while processing your Utility Payment request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            checkUserResultMap.putValue("msg_object", cbsMSG);

            return checkUserResultMapWrapper;
        }

        FlexicoreHashMap signatoryDetailsMap = checkUserResultMap.getFlexicoreHashMap("signatory_details");

        return utilitiesPayment(signatoryDetailsMap, strRequestingMobileNumber,
                theIdentifierType,
                theIdentifier,
                theDeviceIdentifierType,
                theDeviceIdentifier,
                theOriginatorId,
                theProductId,
                thePesaType,
                theAction,
                theCommand,
                theInitiatorDetailsMap,
                theSourceDetailsMap,
                theSenderDetailsMap,
                theReceiverDetailsMap,
                theBeneficiaryDetailsMap,
                theAmount,
                theCategory,
                theTransactionDescription,
                theSourceReference,
                theRequestApplication,
                theSourceApplication);
    }

    public static TransactionWrapper<FlexicoreHashMap> utilitiesPayment(
            FlexicoreHashMap signatoryDetailsMap,
            String strRequestingMobileNumber,
            String theIdentifierType,
            String theIdentifier,
            String theDeviceIdentifierType,
            String theDeviceIdentifier,
            String theOriginatorId,
            String theProductId,
            String thePesaType,
            String theAction,
            String theCommand,
            FlexicoreHashMap theInitiatorDetailsMap,
            FlexicoreHashMap theSourceDetailsMap,
            FlexicoreHashMap theSenderDetailsMap,
            FlexicoreHashMap theReceiverDetailsMap,
            FlexicoreHashMap theBeneficiaryDetailsMap,
            double theAmount,
            String theCategory,
            String theTransactionDescription,
            String theSourceReference,
            String theRequestApplication,
            String theSourceApplication) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> utilityPaymentWrapper = DeSaccoCBS.withdrawal(
                    theIdentifierType,
                    theIdentifier,
                    theOriginatorId,
                    theProductId,
                    thePesaType,
                    theAction,
                    theCommand,
                    theInitiatorDetailsMap,
                    theSourceDetailsMap,
                    theSenderDetailsMap,
                    theReceiverDetailsMap,
                    theBeneficiaryDetailsMap,
                    theAmount,
                    theCategory,
                    theTransactionDescription,
                    theSourceReference,
                    theRequestApplication,
                    theSourceApplication,
                    "UTILITY_PAYMENT");

            if (utilityPaymentWrapper.hasErrors()) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.utilitiesPayment() - " + utilityPaymentWrapper.getErrors() + "\n" + utilityPaymentWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Utility Payment request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap utilityPaymentResultMap = utilityPaymentWrapper.getSingleRecord();

            String requestStatus = utilityPaymentResultMap.getStringValue("request_status");

            if (requestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.utilitiesPayment(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                utilityPaymentResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your account balance is insufficient to process your Utility Payment request");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, your account balance is insufficient to process your Utility Payment request." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            if (requestStatus.equalsIgnoreCase("BLOCKED")) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.utilitiesPayment(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.NO)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Dear member, your request could not be processed at the moment. " + getTrailerMessage() + "\n\nERR_MEMBL100\n")
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }


            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.utilitiesPayment(" + theOriginatorId + ", " + theSourceDetailsMap.getStringValue("account") + ")");
                utilityPaymentResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Utility Payment request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Utility Payment request. Please try again later." + getTrailerMessage())
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            FlexicoreHashMap withdrawalMap = utilityPaymentResultMap.getFlexicoreHashMap("response_payload");

            String strTransactionDateTime = withdrawalMap.getStringValueOrIfNull("transaction_date_time", "").trim();
            String strFormattedDateTime = Utils.formatDate(strTransactionDateTime, "yyyy-MM-dd HH:mm:ss", "dd-MMM-yyyy HH:mm:ss");
            String strFormattedAmount = Utils.formatDouble(theAmount, "#,##0.00");

            String strMSG;
            if (theCategory.equalsIgnoreCase("BANK_TRANSFER")) {
                strMSG = "Dear member, your Bank Transfer Request of KES " + strFormattedAmount + " to " +
                        theReceiverDetailsMap.getStringValue("name") + " - " +
                        theReceiverDetailsMap.getStringValue("identifier") + ", " +
                        "for account " +
                        theReceiverDetailsMap.getStringValue("account") + " has been received and is being processed.";
            } else if (theCategory.equalsIgnoreCase("TILL_PAYMENT")) {
                strMSG = "Dear member, your Till Payment Request of KES " + strFormattedAmount + " to " +
                        theReceiverDetailsMap.getStringValue("name") + " - " +
                        theReceiverDetailsMap.getStringValue("identifier") + " has been received and is being processed.";
            } else {
                strMSG = "Dear member, your Bill Payment Request of KES " + strFormattedAmount + " to " +
                        theReceiverDetailsMap.getStringValue("name") + " - " +
                        theReceiverDetailsMap.getStringValue("identifier") + ", " +
                        "for account " +
                        theReceiverDetailsMap.getStringValue("account") + " has been received and is being processed.";
            }

            /*
                String strMSG = "Dear member, Cash Withdrawal completed Successfully.\n";
                strMSG += "Source A/C: " + theSourceAccountNumber + "\n";
                strMSG += "Mobile: " + theBeneficiaryMobileNo + "\n";
                strMSG += "Amount: KES " + Utils.formatDouble(theAmount, "#,##0.00") + "\n";
                // strMSG += "Date: "+strBeneficiaryName+"";
            */

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Utility Payment has been received successfully and is being processed.")
                    .putValue("msg_object", cbsMSG)
                    .putValue("response_payload", withdrawalMap)
                    .putValue("customer_full_name", signatoryDetailsMap.getStringValue("full_name"))
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.utilitiesPayment(): " + e.getMessage());
            e.printStackTrace();

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Utility Payment request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("msg_object", cbsMSG));
        }
        return resultWrapper;
    }

    public static boolean isMandateInactive(Object strMobileNumber, AppConstants.MobileMandates mobileMandate) {

        QueryBuilder queryBuilder = new QueryBuilder()
                .select()
                .selectColumn("msm.*")
                .from()
                .joinPhrase("""
                        mobile_banking.mobile_signatory_mandates msm
                             LEFT JOIN mobile_banking.mobile_banking_register mbr
                             ON mbr.mobile_register_id = msm.mobile_register_id""")
                .where("mbr.mobile_number = :mobile_number AND msm.mandate_code = :mandate_code");

        TransactionWrapper<FlexicoreHashMap> wrapper = Repository.joinSelectQuery(StringRefs.SENTINEL,
                queryBuilder,
                new FlexicoreHashMap()
                        .addQueryArgument(":mobile_number", strMobileNumber.toString())
                        .addQueryArgument(":mandate_code", mobileMandate.getValue())
        );

        if (wrapper.displayQueriesExecuted().hasErrors()) {
            return true;
        }

        FlexicoreHashMap flexicoreHashMap = wrapper.getSingleRecord();

        if (flexicoreHashMap == null || flexicoreHashMap.isEmpty()) {
            System.err.println("No Mandate found for " + AppConstants.strMobileBankingName + " Number '" + strMobileNumber + "' AND mandate_code = '" + mobileMandate.name() + ": " + mobileMandate.getValue() + "'");
            return true;
        }

        return !flexicoreHashMap.getStringValue("status").equalsIgnoreCase("ACTIVE");
    }

    public static FlexicoreHashMap getServiceStatusDetails(AppConstants.MobileBankingChannel mobileBankingChannel, AppConstants.MobileBankingServices mobileBankingService) {
        try {
            String strSettingsXML = SystemParameters.getParameter("MBANKING_SERVICES_MANAGEMENT");
            Document document = XmlUtils.parseXml(strSettingsXML);

            Element elService = XmlUtils.getElementNodeFromXpath(document, "/MBANKING_SERVICES/" + mobileBankingChannel.getValue() + "/SERVICE[@CODE='" + mobileBankingService.getValue() + "']");
            if (elService != null) {

                FlexicoreHashMap serviceMap = new FlexicoreHashMap();
                serviceMap.putValue("code", elService.getAttribute("CODE"));
                serviceMap.putValue("label", elService.getAttribute("LABEL"));
                serviceMap.putValue("status", elService.getAttribute("STATUS"));
                serviceMap.putValue("status_date", elService.getAttribute("STATUS_DATE"));
                serviceMap.putValue("display_message", elService.getAttribute("DISPLAY_MESSAGE"));

                return serviceMap;
            } else {
                return new FlexicoreHashMap().putValue("status", "ACTIVE");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new FlexicoreHashMap().putValue("status", "ACTIVE");
        }
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanTypes(String strRequestingMobileNumber,
                                                                    String theIdentifierType,
                                                                    String theIdentifier) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {
            TransactionWrapper<FlexicoreArrayList> loanTypesListWrapper = DeSaccoCBS.getLoanTypes(theIdentifierType, theIdentifier);

            if (loanTypesListWrapper.hasErrors() && loanTypesListWrapper.getStatusCode() != HttpsURLConnection.HTTP_NOT_FOUND) {
                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.getLoanTypes() - " + loanTypesListWrapper.getErrors() + "\n" + loanTypesListWrapper.getMessages());

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.addError(loanTypesListWrapper.getErrors());
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage()));

                return resultWrapper;
            }

            if (loanTypesListWrapper.getStatusCode() == HttpsURLConnection.HTTP_NOT_FOUND) {
                resultWrapper.setHasErrors(true);
                resultWrapper.addError("Sorry, no loans found.");
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, no loans found."));

                return resultWrapper;
            }

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.NO)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("payload", loanTypesListWrapper.getData())
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(strRequestingMobileNumber + " => CBSAPI.getLoanTypes(): " + e.getMessage());
            e.printStackTrace();
            resultWrapper.setHasErrors(true);
            resultWrapper.addError(e.getMessage());
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later."));
        }
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> checkLoanLimit(String strRequestingMobileNumber,
                                                                      String theIdentifierType,
                                                                      String theIdentifier,
                                                                      String theDeviceIdentifierType,
                                                                      String theDeviceIdentifier,
                                                                      String theLoanTypeId) {

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            TransactionWrapper<FlexicoreHashMap> checkUserResultMapWrapper = checkUser(UUID.randomUUID().toString(), theIdentifierType, theIdentifier, theDeviceIdentifierType, theDeviceIdentifier);
            FlexicoreHashMap checkUserResultMap = checkUserResultMapWrapper.getSingleRecord();

            if (checkUserResultMapWrapper.hasErrors()) {
                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Loan Qualification Check request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);
                checkUserResultMap.putValue("msg_object", cbsMSG);
                checkUserResultMap.putValue("cbs_api_error_message", checkUserResultMapWrapper.getErrors() + " - " + checkUserResultMapWrapper.getMessages());
                return checkUserResultMapWrapper;
            }

            TransactionWrapper<FlexicoreHashMap> checkLoanLimitWrapper = DeSaccoCBS.checkLoanLimit(
                    theIdentifierType,
                    theIdentifier,
                    theLoanTypeId);

            if (checkLoanLimitWrapper.hasErrors()) {
                System.err.println(theIdentifier + " => DeSaccoCBS.checkLoanLimit() - " + checkLoanLimitWrapper.getErrors() + "\n" + checkLoanLimitWrapper.getMessages());

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Dear member, an error occurred while processing your Loan Qualification Check request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                        .putValue("title", "Transaction Error")
                        .putValue("cbs_api_error_message", checkLoanLimitWrapper.getErrors() + " - " + checkLoanLimitWrapper.getMessages())
                        .putValue("msg_object", cbsMSG));

                return resultWrapper;
            }

            FlexicoreHashMap checkLoanLimitResultMap = checkLoanLimitWrapper.getSingleRecord();

            String requestStatus = checkLoanLimitResultMap.getStringValue("request_status");
            FlexicoreHashMap limitMap = checkLoanLimitResultMap.getFlexicoreHashMap("response_payload");

            if (!requestStatus.equalsIgnoreCase("SUCCESS")) {

                System.err.println(strRequestingMobileNumber + " => DeSaccoCBS.checkLoanLimit(" + theLoanTypeId + ")");
                checkLoanLimitResultMap.printRecordVerticalLabelled();

                SMSMSG cbsMSG = new SMSMSG();
                cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Qualification Check request. Please try again later.");
                cbsMSG.setMode(MSGConstants.MSGMode.SAF);
                cbsMSG.setPriority(210);
                cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

                resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.setHasErrors(true);
                resultWrapper.setData(new FlexicoreHashMap()
                        .putValue("end_session", USSDAPIConstants.Condition.YES)
                        .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                        .putValue("display_message", "Sorry, an error occurred while processing your Loan Qualification Check request. Please try again later." + getTrailerMessage())
                        .putValue("cbs_api_error_message", requestStatus + ": " + limitMap.getStringValue("error_message") + " - " + limitMap.getStringValue("devMessage"))
                        .putValue("msg_object", cbsMSG)
                );

                return resultWrapper;
            }

            String loanName = limitMap.getStringValue("loan_name");
            String eligibleAmount = limitMap.getStringValue("eligible_amount");

            String strMSG;
            if (Double.parseDouble(eligibleAmount) < 10) {
                String strFormattedAmount = Utils.formatDouble(eligibleAmount, "#,##0.00");
                strMSG = "Dear member, you do not qualify to apply for Loan " + loanName + "";
            } else {
                String strFormattedAmount = Utils.formatDouble(eligibleAmount, "#,##0.00");
                strMSG = "Dear member, you are qualified to apply for Loan " + loanName + ", amount KES " + strFormattedAmount + ".";
            }

            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage(strMSG);
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.SUCCESS)
                    .putValue("display_message", "Loan Qualification Check has been received successfully and is being processed.")
                    .putValue("title", "Transaction Accepted")
                    .putValue("msg_object", cbsMSG)
                    .putValue("payload", limitMap)
            );

            return resultWrapper;
        } catch (Exception e) {
            System.err.println(theIdentifier + " => CBSAPI.checkLoanLimit(): " + e.getMessage());
            e.printStackTrace();


            SMSMSG cbsMSG = new SMSMSG();
            cbsMSG.setMessage("Sorry, an error occurred while processing your Loan Qualification Check request. Please try again later.");
            cbsMSG.setMode(MSGConstants.MSGMode.SAF);
            cbsMSG.setPriority(210);
            cbsMSG.setSensitivity(MSGConstants.Sensitivity.NORMAL);

            resultWrapper.setHasErrors(true);
            resultWrapper.setData(new FlexicoreHashMap()
                    .putValue("end_session", USSDAPIConstants.Condition.YES)
                    .putValue("cbs_api_return_val", USSDAPIConstants.StandardReturnVal.ERROR)
                    .putValue("title", "Transaction Error")
                    .putValue("display_message", "Sorry, an error occurred while processing your request. Please try again later." + getTrailerMessage())
                    .putValue("cbs_api_error_message", e.getMessage())
                    .putValue("msg_object", cbsMSG)

            );
        }
        return resultWrapper;
    }


    public static void resetMbankingPIN(String strMobileNumber, String sourceReference, String correlationReference) {
        Random random = new Random();

        TransactionWrapper<FlexicoreHashMap> wrapperSelect = Repository.selectWhere(StringRefs.SENTINEL, TBL_MOBILE_BANKING_REGISTER,
                new FilterPredicate("mobile_number = :mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":mobile_number", strMobileNumber));

        FlexicoreHashMap theMobileBankingDetailsMap = wrapperSelect.getSingleRecord();

        int number = random.nextInt(9999);
        String strPin = String.format("%04d", number);

        String hashedPin = MobileBankingCryptography.hashPIN(theMobileBankingDetailsMap.getStringValue("mobile_number"), strPin);

        String previousPins = theMobileBankingDetailsMap.getStringValueOrIfNull("previous_pins", "<PREVIOUS_PINS/>");

        try {
            Document docPrevPasswords = XmlUtils.parseXml(previousPins);

            Element elPassword = docPrevPasswords.createElement("PIN");
            elPassword.setTextContent(hashedPin);
            docPrevPasswords.getDocumentElement().appendChild(elPassword);

            previousPins = XmlUtils.convertXmlDocToStr(docPrevPasswords);

        } catch (Exception e) {
            e.printStackTrace();
        }

        String pinSetDate = DateTime.getCurrentDateTime();

        String workflowContent = "<?xml version=\"1.0\"?>\n" +
                "<APP ACTION=\"EDIT\" APP_CODE=\"" + SystemApplicationCodes.APP_CODE_MOBILE_BANKING_SELF_PIN_RESET + "\" TYPE=\"MOBILE_BANKING_SELF_PIN_RESET\">\n" +
                "</APP>";

        String auditTrailContent = "<CONTENT>\n" +
                "     <APP APP_CODE=\"" + SystemApplicationCodes.APP_CODE_MOBILE_BANKING_SELF_PIN_RESET + "\" MOBILE_REGISTER_ID=\"" + theMobileBankingDetailsMap.getStringValue("mobile_register_id") + "\">\n" +
                "          <FIELD NEW_VALUE=\"*********\" PREV_VALUE=\"************\">pin</FIELD>\n" +
                "          <FIELD NEW_VALUE=\"RESET\" PREV_VALUE=\"" + theMobileBankingDetailsMap.getStringValue("pin_status") + "\">pin_status</FIELD>\n" +
                "          <FIELD NEW_VALUE=\"" + pinSetDate + "\" PREV_VALUE=\"" + theMobileBankingDetailsMap.getStringValue("pin_set_date") + "\">pin_status_date</FIELD>\n" +
                "     </APP>\n" +
                "</CONTENT>\n";

        FlexicoreHashMap workflowMap = CBSAPI.createWorkflow(SystemApplicationCodes.APP_CODE_MOBILE_BANKING_SELF_PIN_RESET,
                "Mobile Banking Self PIN Reset -" + strMobileNumber,
                "EDIT", workflowContent, auditTrailContent);

        if (workflowMap == null) {
            System.err.println("Failed to reset password for members portal account. An error occurred while processing your request. Please try again later.");
            return;
        }

        String workflowId = workflowMap.getStringValue("workflow_id");

        FlexicoreHashMap updateMap = new FlexicoreHashMap();

        updateMap.putValue("previous_pins", previousPins);
        updateMap.putValue("pin", hashedPin);
        updateMap.putValue("pin_status", "RESET");
        updateMap.putValue("pin_set_date", pinSetDate);
        updateMap.putValue("app_identifier", null);
        updateMap.putValue("app_identifier_set_date", null);
        updateMap.putValue("login_attempts", 0);
        updateMap.putValue("login_auth_action", "NONE");
        updateMap.putValue("login_auth_action_valid_date", null);
        updateMap.putValue("login_auth_flag", null);
        updateMap.putValue("otp_attempts", 0);
        updateMap.putValue("otp_auth_action", "NONE");
        updateMap.putValue("otp_auth_action_valid_date", null);
        updateMap.putValue("otp_auth_flag", null);
        updateMap.putValue("kyc_attempts", 0);
        updateMap.putValue("kyc_auth_action", "NONE");
        updateMap.putValue("kyc_auth_action_valid_date", null);
        updateMap.putValue("kyc_auth_flag", null);
        updateMap.putValue("updated_workflow_id", workflowId);

        String previousUpdatedWorkflowIds = theMobileBankingDetailsMap.getStringValue("previous_updated_workflow_ids") + ",";
        previousUpdatedWorkflowIds = previousUpdatedWorkflowIds + updateMap.getValue("updated_workflow_id");
        updateMap.putValue("previous_updated_workflow_ids", previousUpdatedWorkflowIds);

        theMobileBankingDetailsMap.copyFrom(updateMap);

        String integrityHash = SMPCryptography.calculateIntegrityHash(theMobileBankingDetailsMap);
        updateMap.putValue("integrity_hash", integrityHash);

        TransactionWrapper<?> wrapper = Repository.update(StringRefs.SENTINEL, TBL_MOBILE_BANKING_REGISTER, updateMap,
                new FilterPredicate("mobile_register_id = :mobile_register_id"),
                new FlexicoreHashMap().addQueryArgument(":mobile_register_id", theMobileBankingDetailsMap.getStringValue("mobile_register_id")));

        if (wrapper.hasErrors()) {
            System.err.println("Failed to Reset Mbanking PIN: " + wrapper.getErrors());
            return;
        }

        String theMobileNo = Misc.sanitizePhoneNumber(strMobileNumber);

        String strPinResetMessage = Messaging.getMessagingTemplate("SMS", "MOBILE_BANKING_PIN_RESET");
        strPinResetMessage = strPinResetMessage.replace("[PIN]", strPin);

        if (!theMobileNo.equalsIgnoreCase("INVALID")) {
            int intMSGSent = fnSendSMS(theMobileNo, strPinResetMessage, "YES",
                    MSGConstants.MSGMode.SAF, 200, "MOBILE_BANKING_PIN_RESET", "USSD", "MBANKING_SERVER",
                    sourceReference, correlationReference);
        } else {
            System.err.println("resetMBankingPIN() - Failed to send SMS to user with mobile number '" + strMobileNumber + "'");
        }
    }

    public static FlexicoreHashMap createWorkflow(String applicationCode, String workflowTitle,
                                                  String applicationAction,
                                                  String workflowContent, String auditTrailContent) {

        FlexicoreHashMap userInternetBankingMap = Repository.selectWhere(StringRefs.SENTINEL,
                TBL_USER_ACCOUNTS,
                new FilterPredicate("username = :username"),
                new FlexicoreHashMap().addQueryArgument(":username", "internetbanking@smp")).getSingleRecord();

        if (userInternetBankingMap == null || userInternetBankingMap.isEmpty()) {
            System.err.println("FAILED TO CREATE WORKFLOW FOR APPLICATION: " + applicationCode + ". USER 'internetbanking@smp' not found");
            return null;
        }

        FlexicoreHashMap workflow = new FlexicoreHashMap();
        workflow.putValue("application_code", applicationCode);
        workflow.putValue("approval_levels", 0);
        workflow.putValue("user_id", userInternetBankingMap.getStringValue("user_id"));
        workflow.putValue("approval_level", 0);
        workflow.putValue("approval_status", "APPROVED");
        workflow.putValue("date_created", DateTime.getCurrentDateTime());
        workflow.putValue("date_modified", DateTime.getCurrentDateTime());
        workflow.putValue("workflow_status", "COMPLETED");
        workflow.putValue("workflow_title", workflowTitle);
        workflow.putValue("application_action", applicationAction);
        workflow.putValue("workflow_content", workflowContent);
        workflow.putValue("audit_trail_content", auditTrailContent);
        workflow.putValue("workflow_type", "SINGLE");
        workflow.putValue("branch_code", userInternetBankingMap.getStringValue("branch_code"));
        workflow.putValue("branch", userInternetBankingMap.getStringValue("branch"));
        workflow.putValue("dictionary_version", "1.0");
        workflow.putValue("integrity_hash", "PENDING_CALCULATION");

        TransactionWrapper<FlexicoreHashMap> workflowWrapper = Repository.insertAutoIncremented(StringRefs.SENTINEL, TBL_WORKFLOWS, workflow);

        if (workflowWrapper.hasErrors()) {
            return null;
        }

        workflow = workflowWrapper.getSingleRecord();
        String workflowId = workflow.getValue("workflow_id").toString();

        String integrityHash = SMPCryptography.calculateIntegrityHash(workflow);

        Repository.update(StringRefs.SENTINEL,
                TBL_WORKFLOWS, new FlexicoreHashMap().addQueryArgument("integrity_hash", integrityHash),
                new FilterPredicate("workflow_id = :workflow_id"),
                new FlexicoreHashMap().addQueryArgument(":workflow_id", workflowId));

        return workflow;
    }

    public static void addOrUpdateMobileAppVersion(String mobileNumber, String version) {

        if (Repository.exists(StringRefs.SENTINEL, "tmp.tmp_mobile_app_versions",
                new FilterPredicate("mobile_number = :mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":mobile_number", mobileNumber))) {

            Repository.update(StringRefs.SENTINEL, "tmp.tmp_mobile_app_versions",
                    new FlexicoreHashMap()
                            .putValue("mobile_app_version", version)
                            .putValue("date_modified", DateTime.getCurrentDateTime()),
                    new FilterPredicate("mobile_number = :mobile_number"),
                    new FlexicoreHashMap().addQueryArgument(":mobile_number", mobileNumber));

        } else {
            FlexicoreHashMap insertMap = new FlexicoreHashMap();
            insertMap.putValue("mobile_number", mobileNumber);
            insertMap.putValue("mobile_app_version", version);
            insertMap.putValue("date_created", DateTime.getCurrentDateTime());
            insertMap.putValue("date_modified", DateTime.getCurrentDateTime());

            Repository.insertAutoIncremented(StringRefs.SENTINEL, "tmp.tmp_mobile_app_versions", insertMap);
        }
    }


    public static class SMSMSG {
        private String strMessage;
        private int intPriority;
        private String strCharge;
        private String strCategory;
        private MSGConstants.Sensitivity sensitivity;
        private String strRequestApplication;
        private String strSourceApplication;
        private String strSessionID;
        private String strCorrelationID;
        private MSGConstants.MSGMode theMode;

        public String getMessage() {
            return strMessage;
        }

        public void setMessage(String theMessage) {
            this.strMessage = theMessage;
        }

        public int getPriority() {
            return intPriority;
        }

        public void setPriority(int thePriority) {
            this.intPriority = thePriority;
        }

        public String getCharge() {
            return strCharge;
        }

        public void setCharge(String theCharge) {
            this.strCharge = theCharge;
        }

        public String getCategory() {
            return strCategory;
        }

        public void setCategory(String theCategory) {
            this.strCategory = theCategory;
        }

        public String getRequestApplication() {
            return strRequestApplication;
        }

        public void setRequestApplication(String theRequestApplication) {
            this.strRequestApplication = theRequestApplication;
        }

        public String getSourceApplication() {
            return strSourceApplication;
        }

        public void setSourceApplication(String theSourceApplication) {
            this.strSourceApplication = theSourceApplication;
        }

        public String getSessionID() {
            return strSessionID;
        }

        public void setSessionID(String theSessionID) {
            this.strSessionID = theSessionID;
        }

        public String getCorrelationID() {
            return strCorrelationID;
        }

        public void setCorrelationID(String theCorrelationID) {
            this.strCorrelationID = theCorrelationID;
        }

        public MSGConstants.MSGMode getMode() {
            return theMode;
        }

        public void setMode(MSGConstants.MSGMode theMode) {
            this.theMode = theMode;
        }

        public MSGConstants.Sensitivity getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(MSGConstants.Sensitivity theSensitivity) {
            this.sensitivity = theSensitivity;
        }

        @Override
        public String toString() {
            return "SMSMSG{" +
                   "strMessage='" + strMessage + '\'' +
                   ", intPriority=" + intPriority +
                   ", strCharge='" + strCharge + '\'' +
                   ", strCategory='" + strCategory + '\'' +
                   ", sensitivity=" + sensitivity +
                   ", strRequestApplication='" + strRequestApplication + '\'' +
                   ", strSourceApplication='" + strSourceApplication + '\'' +
                   ", strSessionID='" + strSessionID + '\'' +
                   ", strCorrelationID='" + strCorrelationID + '\'' +
                   ", theMode=" + theMode +
                   '}';
        }
    }

}