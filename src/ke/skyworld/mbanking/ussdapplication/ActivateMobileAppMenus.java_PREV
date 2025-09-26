package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.mbanking.mappapi.MAPPAPI;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface ActivateMobileAppMenus {

    public default  USSDResponse displayMenu_MobileApp(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();

        try{
            String strHeader = "Mobile App";
            APIConstants.AccountType accountType = APIConstants.AccountType.ALL;

            switch (theParam){
                case "MENU": {
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACTIVATE_MOBILE_APP", "1: ACTIVATE Mobile App");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "DISABLE_MOBILE_APP", "2: DISABLE Mobile App");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_ACTION, "NO",theArrayListUSSDSelectOption);

                    break;
                }
                case "ACTION": {

                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAPP_ACTIVATION_ACTION.name());

                    String strResponse = "";

                    switch (strAction) {
                        case "ACTIVATE_MOBILE_APP": {
                            strResponse =  strHeader + "\nProceed to ACTIVATE your mobile application?\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "DISABLE_MOBILE_APP": {
                            strResponse =  strHeader + "\nProceed to DISABLE your mobile application?\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            strHeader = strHeader + "\n{Select a valid menu}\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACTIVATE_MOBILE_APP", "1: ACTIVATE Mobile App");
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "DISABLE_MOBILE_APP", "2: DISABLE Mobile App");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_ACTION, "NO",theArrayListUSSDSelectOption);
                        }
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAPP_ACTIVATION_CONFIRMATION.name());
                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAPP_ACTIVATION_ACTION.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "";
                            String strMobileNo = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                            String strSessionID = Long.toString(theUSSDRequest.getUSSDSessionID());
                            strSessionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());

                            if (strAction.equalsIgnoreCase("ACTIVATE_MOBILE_APP")) {
                                strResponse = theUSSDAPI.generateAndSendOTP(strMobileNo, strSessionID, theUSSDRequest);
                            } else {
                                String finalStrSessionID = strSessionID;
                                Thread worker = new Thread(() -> {
                                    String strMSG = "";

                                    String strCategory = "MAPP_DEACTIVATION";
                                    MAPPAPI theMAPPAPI = new MAPPAPI();

                                    MAPPConstants.ResponseStatus theResponseStatus = theMAPPAPI.deactivateMobileApp(theUSSDRequest.getUSSDMobileNo());
                                    theMAPPAPI = null;

                                    switch (theResponseStatus){
                                        case SUCCESS:{
                                            strMSG = "Dear member, your request to DISABLE Mobile App has been completed successfully.";
                                            break;
                                        }
                                        case ERROR:
                                        case FAILED:
                                        default:{
                                            strMSG = "Dear member, your request to DISABLE Mobile App has FAILED. Please try again. If disabling Mobile App fails again, contact the SACCO for assistance.";
                                            break;
                                        }
                                    }

                                    theUSSDAPI.sendSMS(strMobileNo, strMSG, MSGConstants.MSGMode.SAF, 210, "MAPP_ACTIVATION", theUSSDRequest);
                                });
                                worker.start();

                                strResponse = "Your request to DISABLE Mobile App has been received successfully. Confirmation will be sent to you via SMS.";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO": {
                            String strResponse = "";

                            if (strAction.equalsIgnoreCase("ACTIVATE_MOBILE_APP")) {
                                strResponse = "Dear member, your request to ACTIVATE Mobile App was NOT confirmed. Mobile App Activation request NOT COMPLETED.\n";
                            } else {
                                strResponse = "Dear member, your request to DISABLE Mobile App was NOT confirmed.\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strResponse = "";
                            if (strAction.equalsIgnoreCase("ACTIVATE_MOBILE_APP")) {
                                strResponse = strHeader + "\n{Select a valid menu}\nProceed to ACTIVATE your mobile application?\n";
                            } else {
                                strResponse = strHeader + "\n{Select a valid menu}\nProceed to DISABLE your mobile application?\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }
                default: {

                    strHeader = strHeader + "\n{Select a valid menu}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACTIVATE_MOBILE_APP", "1: ACTIVATE Mobile App");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "DISABLE_MOBILE_APP", "2: DISABLE Mobile App");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MAPP_ACTIVATION_ACTION, "NO",theArrayListUSSDSelectOption);

                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_ActivateMobileApp() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
