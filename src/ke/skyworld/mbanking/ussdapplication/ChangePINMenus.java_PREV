package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface ChangePINMenus {
    public default USSDResponse displayMenu_ChangePIN(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{

            switch (theParam) {
                case "MENU": {
                    String strResponse = "Change PIN\nEnter your current PIN:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    break;
                }
                case "CURRENT_PIN": {
                    String strCurrentPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN.name());
                    if(strCurrentPIN.matches("^[0-9]{4,15}$")){
                        String strResponse = "Change PIN\nEnter your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = "Change PIN\n{Please enter a valid current PIN}\nEnter your current PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "NEW_PIN": {
                    String strIDNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name());
                    if(strIDNewPIN.matches("^[0-9]{4,15}$")){
                        String strResponse = "Change PIN\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = "Change PIN\n{Please enter a valid PIN}\nEnter your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRM_PIN": {

                    if(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name()))){

                        APIConstants.ChangePINReturnVal changePINReturnVal = theUSSDAPI.changeUserPIN(theUSSDRequest);


                        String strResponse ="";

                        if(changePINReturnVal.equals(APIConstants.ChangePINReturnVal.SUCCESS)){
                            strResponse = "Change PIN\nYour new PIN has been changed successfully.Select an option below to proceed.\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(),"00: Login");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO",theArrayListUSSDSelectOption);
                        }else {

                            switch (changePINReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Change PIN\nSorry the new PIN provided is incorrect.\n";
                                    break;
                                }
                                case INVALID_NEW_PIN: {
                                    strResponse = "Change PIN\nSorry the new PIN provided is invalid.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Change PIN\nSorry, this service is not available at the moment. Please try again later.";
                                    break;
                                }
                            }
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO",theArrayListUSSDSelectOption);
                        }

                    }else{
                        String strResponse = "Change PIN\n{New PIN mismatch}\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }

                case "END":{
                    String strResponse = "Change PIN\n{Select a valid option below}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO",theArrayListUSSDSelectOption);
                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_ChangePIN() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Change PIN\n{Sorry, an error has occurred while processing Change PIN}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_ChangePIN() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
