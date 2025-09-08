package ke.skyworld.mbanking.ussdapplication;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_manager.util.SystemParameters;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.co.skyworld.smp.utility_items.memory.InMemoryCache;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.ussd.*;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.nav.cbs.DeSaccoCBS;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

public interface HomeMenus {

    // default USSDResponse displayMenu_Init(USSDRequest theUSSDRequest, String theParam) {
    //
    //     USSDResponse theUSSDResponse = null;
    //     USSDAPI theUSSDAPI = new USSDAPI();
    //     AppMenus theAppMenus = new AppMenus();
    //     try {
    //
    //         String strUSSDCode = String.valueOf(theUSSDRequest.getUSSDCode());
    //         String strUSSDSubCode = String.valueOf(theUSSDRequest.getUSSDSubCode());
    //
    //         if ((strUSSDCode.equals(AppConstants.strMBankingUSSDCode) && strUSSDSubCode.equals(AppConstants.strMBankingUSSDSubCode)) ||
    //                 (strUSSDCode.equals(AppConstants.strMBankingUSSDCode2) && strUSSDSubCode.equals(AppConstants.strMBankingUSSDSubCode2)) ||
    //                 (strUSSDCode.equals(AppConstants.strMBankingUSSDCode2) && strUSSDSubCode.equals(AppConstants.strMBankingUSSDSubCode2_1))) {
    //
    //             APIConstants.CheckUserReturnVal rVal = theUSSDAPI.checkUser(theUSSDRequest);
    //             System.out.println("rVal: " + rVal);
    //
    //             String strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
    //             switch (rVal) {
    //
    //                 case ACTIVE: {
    //                     strResponse = AppConstants.strHomeMenuHeader + ".\nPlease enter your PIN to proceed.\n(Forgot PIN? Reply with 55)";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");
    //                     break;
    //                 }
    //                 case INVALID_IMSI: {
    //                     strResponse = "Sorry, your SIM Card is not registered for " + AppConstants.strMobileBankingName + " mobile banking services. Please visit one of our branches for assistance or contact us.";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case INVALID_IMEI: {
    //                     strResponse = "Sorry, your Mobile Phone is not registered for " + AppConstants.strMobileBankingName + " mobile banking services. Please visit one of our branches for assistance or contact us.";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case BLOCKED: {
    //                     strResponse = "Sorry, your account has been blocked from using " + AppConstants.strMobileBankingName + " mobile banking services. Please visit one of our branches for assistance or contact us.";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case SUSPENDED: {
    //                     strResponse = "Sorry, your account is suspended from using " + AppConstants.strSACCOName + " mobile banking services. Please contact us for assistance.";
    //                     String strTryAgainIn = theUSSDAPI.getUserAuthActionExpiryTime(theUSSDRequest);
    //                     if (!strTryAgainIn.equals("")) {
    //                         strResponse = "Sorry, your account is suspended from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn;
    //                     }
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case INVALID: {
    //                     strResponse = "Sorry, the system is not able to process the request at the moment.\nPlease try again later.\nIf the problem persists, kindly contact us.";
    //
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case UNDER_MAINTENANCE: {
    //                     strResponse = "Sorry, the system is under MAINTENANCE.\nPlease try again later.";
    //
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case NOT_FOUND: {
    //                     strResponse = "Sorry, you are not registered for " + AppConstants.strMobileBankingName + " mobile banking services. Please contact us on 0793281989 for more information\n";
    //
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case NOT_IN_WHITELIST: {
    //                     strResponse = "Sorry, your account is not whitelisted for " + AppConstants.strMobileBankingName + " mobile banking services. Please visit one of our branches for assistance or contact us.";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 case ERROR: {
    //                     strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //                 default: {
    //                     strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance. UNKNOWN ERROR";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
    //                     break;
    //                 }
    //             }
    //         } else if (
    //                 strUSSDCode.equals(AppConstants.strGeneralMenusUSSDCode) &&
    //                         strUSSDSubCode.equals(AppConstants.strErroneousUSSDSubCode)
    //         ) {
    //
    //             theUSSDResponse = theAppMenus.displayMenu_InitAction(theUSSDRequest, "MENU", "Welcome to " + AppConstants.strSACCOName);
    //         }
    //
    //     } catch (Exception e) {
    //         System.err.println(e.getMessage());
    //     } finally {
    //         if (theUSSDResponse != null) {
    //
    //         }
    //     }
    //     return theUSSDResponse;
    // }

    default USSDResponse displayMenu_Init(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {

            String strUSSDCode = String.valueOf(theUSSDRequest.getUSSDCode());
            String strUSSDSubCode = String.valueOf(theUSSDRequest.getUSSDSubCode());
            // InMemoryCache.remove(theUSSDRequest.getUSSDMobileNo() + "_UTILITY_PAYMENT");

            String strResponse;

            String strSettingsXML = SystemParameters.getParameter("MBANKING_SERVICES_MANAGEMENT");
            Document document = XmlUtils.parseXml(strSettingsXML);

            String strUssdEnabled = XmlUtils.getTagValue(document, "/MBANKING_SERVICES/USSD/@STATUS");
            String strUssdDisplayMessage = XmlUtils.getTagValue(document, "/MBANKING_SERVICES/USSD/@MESSAGE");

            if (!strUssdEnabled.equalsIgnoreCase("ACTIVE")) {
                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strUssdDisplayMessage, "NO");
            } else {

                if ((strUSSDCode.equals(AppConstants.strMBankingUSSDCode) && strUSSDSubCode.equals(AppConstants.strMBankingUSSDSubCode)) ||
                    (strUSSDCode.equals(AppConstants.strMBankingUSSDCode2) && strUSSDSubCode.equals(AppConstants.strMBankingUSSDSubCode2)) ||
                    (strUSSDCode.equals(AppConstants.strMBankingUSSDCode2) && strUSSDSubCode.equals(AppConstants.strMBankingUSSDSubCode2_1))) {

                    TransactionWrapper<FlexicoreHashMap> checkUserWrapper = theUSSDAPI.checkUser(theUSSDRequest);
                    if (checkUserWrapper.hasErrors()) {
                        FlexicoreHashMap checkUserMap = checkUserWrapper.getSingleRecord();
                        strResponse = checkUserMap.getStringValue("display_message");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    } else {
                        // strResponse = "Welcome to " + AppConstants.strMobileBankingName + " Mobile Banking Services.\n\nPlease enter your PIN to proceed.\n(Forgot PIN? Reply with 99)";
                        // // strResponse = "Welcome to " + AppConstants.strMobileBankingName + " Mobile Banking Services.\n\nPlease enter your PIN to proceed.";
                        // theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        strResponse = AppConstants.strHomeMenuHeader + ".\nPlease enter your PIN to proceed.\n(Forgot PIN? Reply with 55)";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                } else if (strUSSDCode.equals(AppConstants.strGeneralMenusUSSDCode) && strUSSDSubCode.equals(AppConstants.strErroneousUSSDSubCode)){
                    theUSSDResponse = theAppMenus.displayMenu_InitAction(theUSSDRequest, "MENU", "Welcome to " + AppConstants.strSACCOName);
                }

            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            if (theUSSDResponse != null) {

            }
        }
        return theUSSDResponse;
    }


    default USSDResponse displayMenu_InitAction(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {

            //SELECT
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDCharge("NO");

            theUSSDResponse.setUSSDSelectDataType(AppConstants.USSDDataType.GENERAL_MENU.getValue());
            theUSSDResponse.setUSSDSelectName(AppConstants.USSDDataType.GENERAL_MENU.name());


            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "BUY_GOODS", "1: Lipa Na " + AppConstants.strSACCOProductName);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ERRONEOUS", "2: Correct Erroneous Deposit.");

            USSDResponseSELECTOption.setUSSDSelectOptionEXIT(theArrayListUSSDSelectOption, AppConstants.USSDDiplayText.EXIT.getValue());
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }

        return theUSSDResponse;
    }

    default USSDResponse displayMenu_General(USSDRequest theUSSDRequest, String theParam) {


        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() - 1];

            if (strLastValue.equalsIgnoreCase(AppConstants.USSDDataType.GENERAL_MENU.name())) {
                theUSSDResponse = theAppMenus.displayMenu_InitAction(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
            } else {
                String strGENERAL_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.GENERAL_MENU.name());
                System.out.println("strGENERAL_MENU: " + strGENERAL_MENU);
                switch (strGENERAL_MENU) {
                    case "ERRONEOUS": {
                        theUSSDResponse = theAppMenus.displayMenu_ErroneousTransactions(theUSSDRequest, "MENU");
                        break;
                    }
                    case "BUY_GOODS": {
                        theUSSDResponse = theAppMenus.displayMenu_BuyGoodsMenus(theUSSDRequest, theParam);
                        break;
                    }
                    default: {
                        String strHeader = AppConstants.strHomeMenuHeader + "\n{Select a valid menu}";
                        theUSSDResponse = theAppMenus.displayMenu_InitAction(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_ResetPin(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strResponse;

        try {
            switch (theParam) {
                case "MENU": {
                    strResponse = "Reset PIN\nEnter your Service Number:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");

                    break;
                }

                case "SERVICE_NO": {
                    String strServiceNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.RESET_PIN_SERVICE_NO.name());
                    if (strServiceNumber.matches("^[0-9]{1,15}$")) {
                        TransactionWrapper memberDetailsWrapper = DeSaccoCBS.getMemberDetails("SERVICE_NO", strServiceNumber);

                        if(!memberDetailsWrapper.hasErrors()){
                            FlexicoreHashMap memberDetailsMap = memberDetailsWrapper.getSingleRecord();

                            if (memberDetailsMap.getStringValue("service_no").equals(strServiceNumber)) {
                                InMemoryCache.store(theUSSDRequest.getUSSDMobileNo() + "_RESET_PIN_MEMBER", memberDetailsMap, 600);

                                strResponse = "Reset PIN\nEnter your National ID Number:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_KYC_NATIONAL_ID, USSDConstants.USSDInputType.STRING, "NO");

                            } else {
                                strResponse = "Reset PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                                break;
                            }


                        }else{
                            if (memberDetailsWrapper.getSingleRecord().getStringValue("request_status").equalsIgnoreCase("NOT_FOUND")) {
                                strResponse = "Reset PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                            } else {
                                strResponse = " Pin Reset\nSorry, an error occurred while processing your request. Please try again later:";

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.RESET_PIN_END, "NO", theArrayListUSSDSelectOption);
                            }
                        }


                    } else {
                        strResponse = "Reset PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "ID_NO": {
                    String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.RESET_PIN_KYC_NATIONAL_ID.name());

                    if (strIDNo.matches("^[0-9]{4,11}$")) {

                        String strServiceNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.RESET_PIN_SERVICE_NO.name());
                        String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                        String strSIMID = String.valueOf(theUSSDRequest.getUSSDIMSI());

                        FlexicoreHashMap member = (FlexicoreHashMap) InMemoryCache.retrieve(theUSSDRequest.getUSSDMobileNo() + "_RESET_PIN_MEMBER");

                        if(member != null){
                            String strIdNumber = member.getStringValue("primary_identity_no");

                            if(strIdNumber != null && strIDNo.equalsIgnoreCase(strIdNumber)){
                                strResponse = "Dear Customer your PIN reset request has been received.\nPlease enter your M-PESA PIN in the next menu:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");

                                String strBeneficiaryMobileNo = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                String strReference = strBeneficiaryMobileNo;
                                double dbAmount = Utils.stringToDouble("15.0");

                                String strMemberName = APIUtils.shortenMemberName(String.valueOf(theUSSDRequest.getUSSDMobileNo()));
                                String strTraceID = theUSSDRequest.getUSSDTraceID();

                                String strAccountNumber = "For PIN Reset"; //AppConstants.strLiveCollectionAccount;

                                InMemoryCache.store(strMobileNumber + "-PIN-RESET-" + strAccountNumber, dbAmount, -1);

                                Thread worker = new Thread(() -> {

                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(
                                            String.valueOf(theUSSDRequest.getUSSDMobileNo()),
                                            strMemberName,
                                            strTraceID,
                                            "USSD",
                                            strAccountNumber,
                                            strMemberName,
                                            "MBANKING_SERVER",
                                            strReference,
                                            strBeneficiaryMobileNo,
                                            strMemberName,
                                            strAccountNumber,
                                            dbAmount,
                                            "PIN_RESET");
                                });
                                worker.start();
                            }else{
                                strResponse = "Reset PIN\n{Incorrect Details Provided}\nEnter your National ID Number:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_KYC_NATIONAL_ID, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        }else{
                            strResponse = "Reset PIN\n{Incorrect Details Provided}\nEnter your National ID Number:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_KYC_NATIONAL_ID, USSDConstants.USSDInputType.STRING, "NO");
                        }

                    } else {
                        strResponse = "Reset PIN\n{Please enter a valid National ID Number}\nEnter your National ID Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_KYC_NATIONAL_ID, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "STK_PUSH": {

                    strResponse = "Reset PIN\nDo M-PESA stk push:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.RESET_PIN_KYC_NATIONAL_ID, USSDConstants.USSDInputType.STRING, "NO");

                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_ResetPin() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    strResponse = "Privacy Statement\n{Sorry, an error has occurred while processing Privacy Statement}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    //LINK OPTION
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(), "00: Login");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.RESET_PIN, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_TermsAndConditions() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Login(USSDRequest theUSSDRequest, String theParam) {
        USSDAPI theUSSDAPI = new USSDAPI();
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {

            String strPINResponse = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

            if (strPINResponse == null || strPINResponse.isBlank()) {

                String strResponse = AppConstants.strHomeMenuHeader + ".\n{Invalid input}. Please enter your PIN to proceed.\n(Forgot PIN? Reply with 55)";
                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");

            } else if (strPINResponse.equalsIgnoreCase("55")) {
                theUSSDResponse = theAppMenus.displayMenu_ResetPin(theUSSDRequest, "MENU");

            } else {
                LinkedHashMap<String, String> hmLoginReturnVal = theUSSDAPI.userLogin(theUSSDRequest);
                String rvalLoginReturnVal = hmLoginReturnVal.get("LOGIN_RETURN_VALUE");
                switch (rvalLoginReturnVal) {

                    case "SUCCESS": {


                        String strHeader = AppConstants.strHomeMenuHeader;

                        try {
                            //Get Member Name and Service Number
                            String strAccountXML = CBSAPI.getAccountTransferRecipientXML(String.valueOf(theUSSDRequest.getUSSDMobileNo()), "Mobile");

                            if (!strAccountXML.isEmpty()) {
                                String memberName = MBankingXMLFactory.getXPathValueFromXMLString("/Account/AccountName", strAccountXML.trim());
                                memberName = APIUtils.shortenMemberNameProvidedName(memberName);
                                String memberNumber = MBankingXMLFactory.getXPathValueFromXMLString("/Account/MemberNo", strAccountXML.trim());
                                strHeader = "Welcome " + memberName + " (" + memberNumber + ")";
                            }
                        } catch (Exception e) {
                            System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {
                            }.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
                            e.printStackTrace();
                        }

                        theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                    case "INCORRECT_PIN": {
                        String strResponse = "{Sorry the PIN provided is NOT correct}\nPlease enter your PIN to proceed:";

                        String strLoginAttemptMessage = hmLoginReturnVal.get("LOGIN_ATTEMPT_MESSAGE");
                        if (!strLoginAttemptMessage.equals("")) {
                            strResponse = strLoginAttemptMessage;
                        }

                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOGIN_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        break;
                    }
                    case "SET_PIN": {
                        theUSSDResponse = theAppMenus.displayMenu_SetPIN(theUSSDRequest, theParam);
                        break;
                    }
                    case "BLOCKED": {
                        String strResponse = "Dear member, your account has been blocked from accessing mobile banking services. Please visit one of our branches for assistance or contact us.";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case "SUSPENDED": {
                        String strResponse = "Sorry, your account is suspended from using " + AppConstants.strSACCOName + " mobile banking services. Please contact us for assistance.";
                        String strTryAgainIn = theUSSDAPI.getUserAuthActionExpiryTime(theUSSDRequest);
                        if (!strTryAgainIn.equals("")) {
                            strResponse = "Sorry, your account is suspended from using " + AppConstants.strSACCOName + " mobile banking services. " + strTryAgainIn;
                        }
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    case "ERROR": {
                        String strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance.";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                    default: {
                        String strResponse = strResponse = "Sorry, this service is not available at the moment. Please try again later. If the problem persist kindly contact us for assistance. UNKNOWN ERROR";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_MainInMenus(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {
            //SELECT
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDCharge("NO");

            theUSSDResponse.setUSSDSelectDataType(AppConstants.USSDDataType.MAIN_IN_MENU.getValue());
            theUSSDResponse.setUSSDSelectName(AppConstants.USSDDataType.MAIN_IN_MENU.name());

            //OPTIONS
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "BALANCE_ENQUIRY", "1: Balance Enquiry");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "WITHDRAWAL", "2: Cash Withdrawal");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "DEPOSIT", "3: Payments and Deposit");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "4", "LOAN", "4: Loans");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "MY_ACCOUNT", "5: My Account");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "6", "FUNDS_TRANSFER", "6: Funds Transfer");
            //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "7", "PAY_MOD_BILLS", "7: Pay MOD Bills");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "7", "UTILITIES", "7: Utilities");
            USSDResponseSELECTOption.setUSSDSelectOptionEXIT(theArrayListUSSDSelectOption, AppConstants.USSDDiplayText.EXIT.getValue());

            //SELECT OPTIONSequalsIgnoreCase
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);

            System.out.println("Displaying Home Menus");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }

        return theUSSDResponse;

    }

    default USSDResponse displayMenu_MainIn(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //System.out.println("MAIN IN strLastKey: " +strLastKey);
            //System.out.println("MAIN IN strLastValue: " +strLastValue);
            if (strLastValue.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.name()) && (USSDConstants.arrBranchOptionNames.contains(strLastKey))) { //If the last entry is from LOGIN_PIN then display MAIN_IN_MENU
                theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, AppConstants.strHomeMenuHeader);
            } else {
                String strMainInMenu = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAIN_IN_MENU.name());
                switch (strMainInMenu) {
                    case "WITHDRAWAL": {
                        theUSSDResponse = theAppMenus.displayMenu_Withdrawal(theUSSDRequest, theParam);
                        break;
                    }
                    case "UTILITIES": {
                        theUSSDResponse = theAppMenus.displayMenu_Utilities(theUSSDRequest, theParam);
                        break;
                    }
                    case "DEPOSIT": {
                        theUSSDResponse = theAppMenus.displayMenu_Deposit(theUSSDRequest, theParam);
                        break;
                    }
                    case "MY_ACCOUNT": {
                        theUSSDResponse = theAppMenus.displayMenu_MyAccount(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN": {
                        theUSSDResponse = theAppMenus.displayMenu_Loan(theUSSDRequest, theParam);
                        break;
                    }
                    case "FUNDS_TRANSFER": {
                        theUSSDResponse = theAppMenus.displayMenu_FundTransfer(theUSSDRequest, theParam);
                        break;
                    }
                    case "PAY_MOD_BILLS": {
                        theUSSDResponse = theAppMenus.displayMenu_PayMODBills(theUSSDRequest, theParam);
                        break;
                    }
                    case "CHANGE_PIN": {
                        theUSSDResponse = theAppMenus.displayMenu_ChangePIN(theUSSDRequest, theParam);
                        break;
                    }
                    case "BALANCE_ENQUIRY": {
                        theUSSDResponse = theAppMenus.displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
                        break;
                    }
                    case "M_BOOSTA_LOAN": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanApplication(theUSSDRequest, "TYPE", "126");
                        break;
                    }
                    case "DIVIDEND_ADVANCE_LOAN": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanApplication(theUSSDRequest, "TYPE", "136");
                        break;
                    }
                    default: {
                        String strHeader = AppConstants.strHomeMenuHeader + "\n{Select a valid menu}";
                        theUSSDResponse = theAppMenus.displayMenu_MainInMenus(theUSSDRequest, theParam, strHeader);
                        break;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static String generateRandomNumberAsString(int length) {
        Random random = new Random(System.nanoTime());
        StringBuilder sb = new StringBuilder(length);

        String CHARACTERS = "1234567890";

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }

        return sb.toString();
    }

}
