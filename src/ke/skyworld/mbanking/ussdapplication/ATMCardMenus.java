package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.ussdapi.APIConstants;
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

                    if (!strUserInput.equals("")) {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ATM_CARD_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        strHeader = "ATM Cards\n{Select a valid ATM card}";
                        theUSSDResponse = GeneralMenus.displayMenu_ATMCards(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.ATM_CARD_LIST);
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        String strCardType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ATM_CARD_LIST.name());
                        HashMap<String, String> hmCardType = Utils.toHashMap(strCardType);
                        String strCardID = hmCardType.get("ID");
                        String strCardName = hmCardType.get("NAME");

                        String strResponse = "My ATM Cards\nConfirm " + strShortHeader + "\nATM Card: " + strCardName+ "\n";

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

                    switch (strConfirmation){
                        case "YES":{
                            String strResponse;

                            APIConstants.TransactionReturnVal rVal = theUSSDAPI.manageATMCard(theUSSDRequest);

                            switch (rVal) {
                                case SUCCESS: {
                                    strResponse = strHeader+"\nYour request was completed successfully.\nSelect an option below to proceed.\n";
                                    break;
                                }
                                case ERROR: {
                                    strResponse = strHeader+"\nAn error occurred, please try again.\n";
                                    break;
                                }
                                default: {
                                    strResponse = strHeader+"\nSorry, this service is not available at the moment. Please try again.\n";
                                    break;
                                }
                            }

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
                            String strCardID = hmCardType.get("ID");
                            String strCardName = hmCardType.get("NAME");

                            String strResponse = "My ATM Cards\nConfirm " + strShortHeader + "\n{Select a valid menu}\nATM Card: " + strCardName+ "\n";

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
