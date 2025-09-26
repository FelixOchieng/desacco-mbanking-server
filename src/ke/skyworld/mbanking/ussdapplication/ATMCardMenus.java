package ke.skyworld.mbanking.ussdapplication;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;

public interface ATMCardMenus {

    public default USSDResponse displayMenu_ATMCard(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader = "My ATM Cards";
        String strShortHeader = "My ATM Cards";

        String strATMCardAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_ACTION.name());

        if(strATMCardAction != null){
            if (!strATMCardAction.equals("")) {
                if(strATMCardAction.equals("ACTIVATE_ATM_CARD")){
                    strShortHeader = "ATM Card Activation";
                    strHeader += "\nActivate ATM Card";
                } else {
                    strShortHeader = "ATM Card Deactivation";
                    strHeader += "\nDeactivate ATM Card";
                }
            }
        }

        try{

            switch (theParam) {
                case "MENU": {
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACTIVATE_ATM_CARD", "1: ACTIVATE ATM Card");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "DEACTIVATE_ATM_CARD", "1: DEACTIVATE ATM Card");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_ACTION, "NO",theArrayListUSSDSelectOption);

                    break;
                }
                case "ACTION": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_ACTION.name());

                    if (!strUserInput.equals("")) {
                        strHeader = strHeader + "\nSelect ATM Card:";
                        theUSSDResponse = GeneralMenus.displayMenu_ATMCards(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.ATM_CARD_LIST);
                    } else {
                        strHeader = "ATM Cards\n{Select a valid menu}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACTIVATE_ATM_CARD", "1: ACTIVATE ATM Card");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "DEACTIVATE_ATM_CARD", "2: DEACTIVATE ATM Card");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_ACTION, "NO",theArrayListUSSDSelectOption);
                    }
                    break;
                }
                case "LIST": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_LIST.name());
                    System.out.println("strUserInput in LIST: "+strUserInput);

                    if (!strUserInput.equals("")) {
                        String strCardType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_LIST.name());
                        HashMap<String, String> hmCardType = Utils.toHashMap(strCardType);
                        String strCardStatus = hmCardType.get("cardStatus");
                        String strCardIsLinked = hmCardType.get("isLinked");

                        if(strCardStatus.equals("Active") && strCardIsLinked.equals("true")){
                            String strResponse = strHeader + "\nEnter your reason for deactivating the card:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ATM_CARD_REASON, USSDConstants.USSDInputType.STRING, "NO");

                        }else {
                            String strResponse = strHeader + "\nThis card is not active or not linked to your account.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO",theArrayListUSSDSelectOption);
                        }

                    } else {
                        strHeader = "ATM Cards\n{Select a valid ATM card}";
                        theUSSDResponse = GeneralMenus.displayMenu_ATMCards(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.ATM_CARD_LIST);
                    }
                    break;
                }

                case "REASON": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_REASON.name());

                    if (strUserInput.matches("^(?:\\S+\\s+){2}\\S+.*$")) {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ATM_CARD_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        strHeader = strHeader + "\n{MUST be 3 or more words}\nEnter your reason for deactivating the card:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.ATM_CARD_REASON, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        String strCardType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_LIST.name());
                        HashMap<String, String> hmCardType = Utils.toHashMap(strCardType);
                        String strCardNumber = hmCardType.get("cardNumber");

                        String strResponse = "My ATM Cards\nConfirm " + strShortHeader + "\nATM Card: " + strCardNumber+ "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ATM_CARD_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_CONFIRMATION.name());

                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_REASON.name());


                    String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                    HashMap<String, String> userIdentifierDetails = APIUtils.getUserIdentifierDetails(strMobileNumber);
                    String strIdentifierType = userIdentifierDetails.get("identifier_type");
                    String strIdentifier = userIdentifierDetails.get("identifier");
                    switch (strConfirmation){
                        case "YES":{
                            String strCardType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_LIST.name());
                            System.out.println("strCardType in YES: " + strCardType);
                            HashMap<String, String> hmCardType = Utils.toHashMap(strCardType);
                            String strCardID = hmCardType.get("cardId");
                            System.out.println("strCardID in YES: " + strCardID);


//                            APIConstants.TransactionReturnVal rVal = theUSSDAPI.manageATMCard(theUSSDRequest);
                            TransactionWrapper<FlexicoreHashMap> result  = CBSAPI.delinkATMCard(strMobileNumber, strIdentifierType, strIdentifier, strCardID, strUserInput);

                            if (result.hasErrors()) {
                                System.err.println("Error occurred: " + result.getErrors());
                                FlexicoreHashMap errorData = result.getSingleRecord();
                                String displayMessage = errorData.getStringValue("display_message");
                                System.err.println("Display Message: " + displayMessage);
                            } else {
                                FlexicoreHashMap responseData = result.getSingleRecord();
                                String status = responseData.getStringValue("status");
                                String statusDescription = responseData.getStringValue("status_description");
                                String displayMessage = responseData.getStringValue("display_message");

                                System.out.println("Status: " + status);
                                System.out.println("Status Description: " + statusDescription);
                                System.out.println("Display Message: " + displayMessage);
                            }



                            String strResponse = strHeader + "\nYour request was completed successfully.\nSelect an option below to proceed.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO":{
                            String strResponse = "Dear member, your "+strHeader+" request NOT confirmed. "+strHeader+" request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strCardType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_LIST.name());
                            HashMap<String, String> hmCardType = Utils.toHashMap(strCardType);
                            String strCardNumber = hmCardType.get("cardNumber");
                            String strResponse = "My ATM Cards\nConfirm " + strShortHeader + "\n{Select a valid menu}\nATM Card: " + strCardNumber+ "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                case "END":{
                    String strResponse = "My ATM Cards\n{Select a valid option below}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO",theArrayListUSSDSelectOption);
                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_ATMCard() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "My ATM Cards\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_ATMCard() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
