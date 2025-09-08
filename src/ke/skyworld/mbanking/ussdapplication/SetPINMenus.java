package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface SetPINMenus {
    public default USSDResponse displayMenu_SetPIN(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            switch (theParam) {
                case "PIN": {
                    String strResponse = "Provide the information below to set your PIN\nEnter your Service Number:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING,"NO");
                    break;
                }
                case "SERVICE_NO": {
                    String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_SERVICE_NO.name());
                    if(strIDNo.matches("^[0-9]{1,15}$")){
                        String strResponse = "Set PIN\nEnter your National ID Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = "Set PIN\n{Please enter a valid Service Number}\nEnter your Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_SERVICE_NO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "ID_NO": {
                    String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());
                    if(strIDNo.matches("^[0-9]{4,11}$")){
                        String strResponse = "Set PIN\nEnter your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = "Set PIN\n{Please enter a valid National ID Number}\nEnter your National ID Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_ID_NO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "NEW_PIN": {
                    String strIDNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name());
                    if(strIDNewPIN.matches("^[0-9]{4,15}$")){
                        String strResponse = "Set PIN\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = "Set PIN\n{Please enter a valid PIN}\nEnter your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRM_PIN": {
                    if(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_NEW_PIN.name()))){
                        String strResponse =  "Set PIN\nI confirm that I have read and understood the terms and conditions of using " + AppConstants.strMobileBankingName + "\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "YES", "1: Yes");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "NO", "2: No");
                        //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "SEND", "3: Read T&Cs");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_TC, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strResponse = "Set PIN\n{New PIN mismatch}\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "TC":{
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_TC.name());
                    if(strConfirmation.equalsIgnoreCase("YES")){
                        APIConstants.SetPINReturnVal setPINReturnVal = theUSSDAPI.setUserPIN(theUSSDRequest);

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                        String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        String strIDNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SET_PIN_ID_NO.name());

                        if(setPINReturnVal.equals(APIConstants.SetPINReturnVal.SUCCESS)){
                            String strResponse = "Set PIN\nYour new PIN has been set successfully. Select an option below to proceed.\n";
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(),"00: Login");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
                        }else {
                            String strResponse ="";

                            switch (setPINReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Set PIN\nSorry, current PIN provided ("+strPIN+") is NOT correct.\nPlease enter the PIN sent to you via SMS";
                                    break;
                                }
                                case INVALID_SERVICE_NUMBER: {
                                    strResponse = "Set PIN\nSorry, the service number / ID number provided is invalid.\n";
                                    break;
                                }
                                case INVALID_ID_NUMBER: {
                                    strResponse = "Set PIN\nSorry, the service number / ID number provided is invalid.\n";
                                    break;
                                }
                                case INVALID_NEW_PIN: {
                                    strResponse = "Set PIN\nSorry, the new PIN provided is invalid.\n";
                                    break;
                                }
                                case INVALID_ACCOUNT: {
                                    strResponse = "Set PIN\nSorry, the National ID Number / Service Number you entered ("+strIDNo+") does NOT match your account details in our system.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Set PIN\nSorry, your account has been blocked. Please visit one of our branches for assistance or contact us.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Set PIN\nSorry, this service is not available at the moment. Please try again later.\n";
                                    break;
                                }
                            }

                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            //LINK OPTION
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(),"00: Retry to set PIN");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
                        }
                    }else{
                        String strResponse = "Set PIN\nSorry, you did not the accept terms and conditions for using " + AppConstants.strMobileBankingName + ".\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "0", "_LINK", AppConstants.USSDDataType.SET_PIN_CONFIRM_PIN.name(),"0: Back");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(),"00: Retry to set PIN");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
                    }
                    break;
                }
                case "END":{
                    String strResponse = "Set PIN\n{Invalid menu selected}\nPlease select an option below\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    //LINK OPTION - Force user to login after error at the end.
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(),"00: Login");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);
                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_SetPIN() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Set PIN\n{Sorry, an error has occurred while processing Set PIN}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    //LINK OPTION
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.LOGIN_PIN.name(),"00: Retry to set PIN");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SET_PIN_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_SetPIN() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }


} // End interface AppAuthMenus
