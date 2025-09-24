package ke.skyworld.mbanking.ussdapplication;

import ke.co.skyworld.smp.authentication_manager.MobileBankingCryptography;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.co.skyworld.smp.utility_items.memory.InMemoryCache;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.nav.cbs.DeSaccoCBS;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.net.ssl.HttpsURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface SetPINMenus {
    // default USSDResponse displayMenu_SetPIN(USSDRequest theUSSDRequest, String theParam) {
    //     USSDResponse theUSSDResponse = null;
    //     USSDAPI theUSSDAPI = new USSDAPI();
    //     AppMenus theAppMenus = new AppMenus();
    //
    //     try{
    //         switch (theParam) {
    //             case "PIN": {
    //                 String strResponse = "Provide the information below to set your PIN\nEnter your Service Number:";
    //                 theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING,"NO");
    //                 break;
    //             }
    //             case "SERVICE_NO": {
    //                 String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_SERVICE_NO.name());
    //                 if(strIDNo.matches("^[0-9]{1,15}$")){
    //                     String strResponse = "Set PIN\nEnter your National ID Number:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING,"NO");
    //                 }else{
    //                     String strResponse = "Set PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING,"NO");
    //                 }
    //                 break;
    //             }
    //             case "ID_NO": {
    //                 String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());
    //                 if(strIDNo.matches("^[0-9]{4,11}$")){
    //                     String strResponse = "Set PIN\nEnter your new PIN:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING,"NO");
    //                 }else{
    //                     String strResponse = "Set PIN\n{Please enter a valid National ID Number}\nEnter your National ID Number:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING,"NO");
    //                 }
    //                 break;
    //             }
    //             case "NEW_PIN": {
    //                 String strIDNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name());
    //                 if(strIDNewPIN.matches("^[0-9]{4,15}$")){
    //                     String strResponse = "Set PIN\nConfirm your new PIN:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING,"NO");
    //                 }else{
    //                     String strResponse = "Set PIN\n{Please enter a valid PIN}\nEnter your new PIN:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING,"NO");
    //                 }
    //
    //                 break;
    //             }
    //             case "CONFIRM_PIN": {
    //                 if(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name()))){
    //                     String strResponse =  "Set PIN\nI confirm that I have read and understood the terms and conditions of using " + AppConstants.strMobileBankingName + "\n";
    //                     ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
    //                     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
    //                     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "YES", "1: Yes");
    //                     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "NO", "2: No");
    //                     //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "SEND", "3: Read T&Cs");
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_TC, "NO",theArrayListUSSDSelectOption);
    //                 }else{
    //                     String strResponse = "Set PIN\n{New PIN mismatch}\nConfirm your new PIN:";
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING,"NO");
    //                 }
    //                 break;
    //             }
    //             case "TC":{
    //                 String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_TC.name());
    //                 if(strConfirmation.equalsIgnoreCase("YES")){
    //                     APIConstants.SetPINReturnVal setPINReturnVal = theUSSDAPI.setUserPIN(theUSSDRequest);
    //
    //                     ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
    //
    //                     String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
    //                     String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());
    //
    //                     if(setPINReturnVal.equals(APIConstants.SetPINReturnVal.SUCCESS)){
    //                         String strResponse = "Set PIN\nYour new PIN has been set successfully. Select an option below to proceed.\n";
    //                         USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
    //                         USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(),"00: Login");
    //                         theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
    //                     }else {
    //                         String strResponse ="";
    //
    //                         switch (setPINReturnVal) {
    //                             case INCORRECT_PIN: {
    //                                 strResponse = "Set PIN\nSorry, current PIN provided ("+strPIN+") is NOT correct.\nPlease enter the PIN sent to you via SMS";
    //                                 break;
    //                             }
    //                             case INVALID_SERVICE_NUMBER: {
    //                                 strResponse = "Set PIN\nSorry, the service number / ID number provided is invalid.\n";
    //                                 break;
    //                             }
    //                             case INVALID_ID_NUMBER: {
    //                                 strResponse = "Set PIN\nSorry, the service number / ID number provided is invalid.\n";
    //                                 break;
    //                             }
    //                             case INVALID_NEW_PIN: {
    //                                 strResponse = "Set PIN\nSorry, the new PIN provided is invalid.\n";
    //                                 break;
    //                             }
    //                             case INVALID_ACCOUNT: {
    //                                 strResponse = "Set PIN\nSorry, the National ID Number / Service Number you entered ("+strIDNo+") does NOT match your account details in our system.\n";
    //                                 break;
    //                             }
    //                             case BLOCKED: {
    //                                 strResponse = "Set PIN\nSorry, your account has been blocked. Please visit one of our branches for assistance or contact us.\n";
    //                                 break;
    //                             }
    //                             default: {
    //                                 strResponse = "Set PIN\nSorry, this service is not available at the moment. Please try again later.\n";
    //                                 break;
    //                             }
    //                         }
    //
    //                         USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
    //                         //LINK OPTION
    //                         USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(),"00: Retry to set PIN");
    //                         theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
    //                     }
    //                 }else{
    //                     String strResponse = "Set PIN\nSorry, you did not the accept terms and conditions for using " + AppConstants.strMobileBankingName + ".\n";
    //                     ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
    //                     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
    //
    //                     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "0", "_LINK", AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name(),"0: Back");
    //                     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(),"00: Retry to set PIN");
    //                     theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
    //                 }
    //                 break;
    //             }
    //             case "END":{
    //                 String strResponse = "Set PIN\n{Invalid menu selected}\nPlease select an option below\n";
    //                 ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
    //                 USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
    //                 //LINK OPTION - Force user to login after error at the end.
    //                 USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(),"00: Login");
    //                 theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
    //                 break;
    //             }
    //             default:{
    //                 System.err.println("theAppMenus.displayMenu_SetPIN() UNKNOWN PARAM ERROR : theParam = " + theParam);
    //
    //                 String strResponse = "Set PIN\n{Sorry, an error has occurred while processing Set PIN}\n";
    //                 ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
    //                 USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
    //                 //LINK OPTION
    //                 USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(),"00: Retry to set PIN");
    //                 theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
    //
    //                 break;
    //             }
    //         }
    //
    //     }
    //     catch(Exception e){
    //         System.err.println("theAppMenus.displayMenu_SetPIN() ERROR : " + e.getMessage());
    //     }
    //     finally{
    //         theUSSDAPI = null;
    //         theAppMenus = null;
    //     }
    //     return theUSSDResponse;
    // }

    default USSDResponse displayMenu_SetPIN(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            switch (theParam) {
                case "PIN": {
                    String strResponse = "Provide information below to set your PIN\nEnter your Service Number:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                    break;
                }

                case "SERVICE_NO": {
                    String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_SERVICE_NO.name());
                    if (strIDNo.matches("^[0-9]{1,15}$")) {
                        TransactionWrapper memberDetailsWrapper = DeSaccoCBS.getMemberDetails("SERVICE_NO", strIDNo);

                        if (!memberDetailsWrapper.hasErrors()) {
                            FlexicoreHashMap memberDetailsMap = memberDetailsWrapper.getSingleRecord();

                            if (memberDetailsMap.getStringValue("service_no").equals(strIDNo)) {
                                if (memberDetailsMap.getStringValue("primary_mobile_number").equalsIgnoreCase(String.valueOf(theUSSDRequest.getUSSDMobileNo()))) {
                                    String strResponse = "Set PIN\nEnter your National ID Number:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING, "NO");
                                } else {
                                    String strResponse = "Set PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                                }

                            } else {
                                String strResponse = "Set PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                                break;
                            }
                        } else {
                            if (memberDetailsWrapper.getSingleRecord().getStringValue("request_status").equalsIgnoreCase("NOT_FOUND")) {
                                String strResponse = "Set PIN\n{Account Not Found}\nEnter your Service Number:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                            } else {
                                String strResponse = "Set PIN\nSorry, an error occurred while processing your request. Please try again later:";
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                            }
                        }

                    } else {
                        String strResponse = "Set PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "ID_NO": {
                    String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());
                    if (strIDNo.matches("^[A-Za-z0-9]{4,15}$")) {

                        TransactionWrapper<FlexicoreHashMap> validKYCDetailsWrapper = theUSSDAPI.isValidKYCDetails(theUSSDRequest, strIDNo);
                        FlexicoreHashMap validKYCDetailsMap = validKYCDetailsWrapper.getSingleRecord();

                        if (validKYCDetailsWrapper.hasErrors()) {

                            String strResponse = "Set PIN\n" + validKYCDetailsMap.getStringValue("display_message");

                            USSDAPIConstants.Condition endSession = validKYCDetailsMap.getValue("end_session");

                            if (endSession == USSDAPIConstants.Condition.NO) {
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING, "NO");

                            } else {
                                strResponse = validKYCDetailsMap.getStringValue("display_message");

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                            }
                        } else {
                            String strResponse = "Set PIN\nEnter your new PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = "Set PIN\n{Please enter a valid National ID Number}\nEnter your National ID:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "NEW_PIN": {
                    String strIDNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name());
                    String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());
                    String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

                    TransactionWrapper<FlexicoreHashMap> currentUserWrapper = theUSSDAPI.getCurrentUserDetails(theUSSDRequest);
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
                            if (element.getTextContent().equalsIgnoreCase(MobileBankingCryptography.hashPIN(String.valueOf(theUSSDRequest.getUSSDMobileNo()), strIDNewPIN))) {
                                hasUsedPinBefore = true;
                                break;
                            }
                        }
                    }

                    if (strIDNewPIN.matches("^[0-9]{4,15}$")) {
                        String strResponse = "Set PIN\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        if (theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name()))) {
                            strResponse = "{Your start PIN is not accepted as your new PIN}\nEnter your new PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            try {
                                String strDateOfBirth = theUSSDAPI.getUserDateOfBirth(strMobileNumber);
                                if (!strDateOfBirth.equals("")) {
                                    strDateOfBirth = strDateOfBirth.split("-")[0];
                                    System.out.println("strDateOfBirth: " + strDateOfBirth);
                                    if (strIDNewPIN.equals(strDateOfBirth)) {
                                        strResponse = "{Your year of birth is not accepted as a PIN}\nEnter your new PIN:";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                                    } else if (strIDNo.contains(strIDNewPIN)) {
                                        System.out.println("strIDNo: " + strIDNo);
                                        strResponse = "{Part of your ID Number is not accepted as a PIN}\nEnter your new PIN:";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                                    } else if (hasUsedPinBefore) {
                                        strResponse = "Set PIN\n{Please provide a new PIN that you have not used before}\nEnter your new PIN:";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                                    } else {
                                        if (strIDNewPIN.equals("1234")
                                                || strIDNewPIN.equals("4321")
                                                || strIDNewPIN.equals("2345")
                                                || strIDNewPIN.equals("3456")
                                                || strIDNewPIN.equals("4567")
                                                || strIDNewPIN.equals("5678")
                                                || strIDNewPIN.equals("6789")) {
                                            strResponse = "{Your PIN is not complex enough}\nEnter your new PIN:";
                                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                                        } else {
                                            Pattern ptPattern = Pattern.compile("^([0-9])\\1*$");
                                            Matcher mtMatcher = ptPattern.matcher(strIDNewPIN);
                                            if (mtMatcher.find()) {
                                                strResponse = "{Your PIN is not complex enough}\nEnter your new PIN:";
                                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } else {
                        String strResponse = "Set PIN\n{Please enter a valid PIN}\nEnter your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "CONFIRM_PIN": {
                    if (theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name()))) {

                        // TransactionWrapper<FlexicoreHashMap> setPINWrapper = theUSSDAPI.setPIN(theUSSDRequest);
                        // FlexicoreHashMap setPINMap = setPINWrapper.getSingleRecord();
                        // if (setPINWrapper.hasErrors()) {
                        //     //USSDAPIConstants.Condition endSession = setPINMap.getValue("end_session");
                        //     String strResponse = setPINMap.getStringValue("display_message");
                        //
                        //     ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        //     theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                        //
                        // } else {
                        //     ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        //
                        //     String strResponse = "Set PIN\nYour new PIN has been set successfully. Select an option below to proceed.\n";
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(), "00: Login");
                        //     theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                        // }

                        String strResponse = "Set PIN\nI confirm that I have read and understood the terms and conditions of using " + AppConstants.strMobileBankingName + "\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "YES", "1: Yes");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "NO", "2: No");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_TC, "NO", theArrayListUSSDSelectOption);

                    } else {
                        String strResponse = "Set PIN\n{New PIN mismatch}\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "TC": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_TC.name());
                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        // APIConstants.SetPINReturnVal setPINReturnVal = theUSSDAPI.setUserPIN(theUSSDRequest);
                        //
                        // ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        //
                        // String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        // String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());
                        //
                        // if (setPINReturnVal.equals(APIConstants.SetPINReturnVal.SUCCESS)) {
                        //     String strResponse = "Set PIN\nYour new PIN has been set successfully. Select an option below to proceed.\n";
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(), "00: Login");
                        //     theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                        // } else {
                        //     String strResponse = "";
                        //
                        //     switch (setPINReturnVal) {
                        //         case INCORRECT_PIN: {
                        //             strResponse = "Set PIN\nSorry, current PIN provided (" + strPIN + ") is NOT correct.\nPlease enter the PIN sent to you via SMS";
                        //             break;
                        //         }
                        //         case INVALID_SERVICE_NUMBER: {
                        //             strResponse = "Set PIN\nSorry, the service number / ID number provided is invalid.\n";
                        //             break;
                        //         }
                        //         case INVALID_ID_NUMBER: {
                        //             strResponse = "Set PIN\nSorry, the service number / ID number provided is invalid.\n";
                        //             break;
                        //         }
                        //         case INVALID_NEW_PIN: {
                        //             strResponse = "Set PIN\nSorry, the new PIN provided is invalid.\n";
                        //             break;
                        //         }
                        //         case INVALID_ACCOUNT: {
                        //             strResponse = "Set PIN\nSorry, the National ID Number / Service Number you entered (" + strIDNo + ") does NOT match your account details in our system.\n";
                        //             break;
                        //         }
                        //         case BLOCKED: {
                        //             strResponse = "Set PIN\nSorry, your account has been blocked. Please visit one of our branches for assistance or contact us.\n";
                        //             break;
                        //         }
                        //         default: {
                        //             strResponse = "Set PIN\nSorry, this service is not available at the moment. Please try again later.\n";
                        //             break;
                        //         }
                        //     }
                        //
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        //     // LINK OPTION
                        //     USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(), "00: Retry to set PIN");
                        //     theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                        // }

                        TransactionWrapper<FlexicoreHashMap> setPINWrapper = theUSSDAPI.setPIN(theUSSDRequest);
                        FlexicoreHashMap setPINMap = setPINWrapper.getSingleRecord();
                        if (setPINWrapper.hasErrors()) {
                            String strResponse = setPINMap.getStringValue("display_message");

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);

                        } else {
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

                            String strResponse = "Set PIN\nYour new PIN has been set successfully. Select an option below to proceed.\n";
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(), "00: Login");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                        }
                    } else {
                        String strResponse = "Set PIN\nSorry, you did not the accept terms and conditions for using " + AppConstants.strMobileBankingName + ".\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "0", "_LINK", AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name(), "0: Back");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(), "00: Retry to set PIN");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                    }
                    break;
                }

                case "END": {
                    String strResponse = "Set PIN\n{Invalid menu selected}\nPlease select an option below\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    // LINK OPTION - Force user to login after error at the end.
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(), "00: Login");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_SetPIN() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Set PIN\n{Sorry, an error has occurred while processing Set PIN}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    // LINK OPTION
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(), "00: Retry to set PIN");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_SetPIN() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

} // End interface AppAuthMenus
