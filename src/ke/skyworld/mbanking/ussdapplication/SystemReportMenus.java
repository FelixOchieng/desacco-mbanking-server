package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface SystemReportMenus {
    public default  USSDResponse displayMenu_SystemReports(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();

        try{
            String strHeader = "System Reports";

            switch (theParam){
                case "MENU": {
                    String strResponse = strHeader+"\nSelect report type\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "USERS", "1: Users");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_TYPE, "NO",theArrayListUSSDSelectOption);
                    break;
                }
                case "TYPE": {
                    String strResponse = "";
                    String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SYSTEM_REPORTS_TYPE.name());

                    if(strEmailAddress.equals("")){
                        strResponse = strHeader+"\n{Select report type}\nSelect a valid type";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "USERS", "1: Users");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_TYPE, "NO",theArrayListUSSDSelectOption);
                    } else {
                        strResponse = strHeader+"\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SYSTEM_REPORTS_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SYSTEM_REPORTS_PIN.name());
                    if(strLoginPIN.equals(strPIN)){
                        String strEmailAddress = USSDAPI.fnGetReportUserData(theUSSDRequest, "EMAIL_ADDRESS");

                        String strResponse =  "Confirm request to fetch system reports\nAn email will be sent to: " + strEmailAddress+ "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SYSTEM_REPORTS_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SYSTEM_REPORTS_CONFIRMATION.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "";

                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.fnSendSystemReportsViaEmail(theUSSDRequest);
                            String strReportUserName = USSDAPI.fnGetReportUserData(theUSSDRequest, "NAME");
                            String strReportUserEmailAddress = USSDAPI.fnGetReportUserData(theUSSDRequest, "EMAIL_ADDRESS");
                            switch (transactionReturnVal){
                                case SUCCESS:{
                                    strResponse = "Dear "+strReportUserName+", your request to fetch system reports has been COMPLETED SUCCESSFULLY.\nAn email has been sent to "+strReportUserEmailAddress;
                                    break;
                                }
                                case ERROR: {
                                    strResponse = "Dear "+strReportUserName+", your request to fetch system reports has FAILED. Please try again.";
                                    break;
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO": {
                            String strResponse = "Dear member, your request to fetch system reports was NOT confirmed. E-mail address change request NOT COMPLETED.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strEmailAddress = USSDAPI.fnGetReportUserData(theUSSDRequest, "EMAIL_ADDRESS");

                            String strResponse =  "Confirm request to fetch system reports\nAn email will be sent to: " + strEmailAddress+ "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    String strResponse = strHeader+"\n{Select a valid menu}\nSelect report type\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "USERS", "1: Users");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.SYSTEM_REPORTS_TYPE, "NO",theArrayListUSSDSelectOption);
                    break;
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
