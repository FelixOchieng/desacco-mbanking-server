package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.mappapi.MAPPAPI;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MemberActionMenus {
    public default  USSDResponse displayMenu_UpdateEmail(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();

        try{
            String strHeader = "Update E-mail Address";

            switch (theParam){
                case "MENU": {
                    String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);
                    String strResponse = strHeader+"\nYour current e-mail address is "+strCurrentEmailAddress+"\n\nEnter new email address";
                    if(strCurrentEmailAddress.equals("")){
                        strResponse = strHeader+"\nEnter your email address";
                    }
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.UPDATE_EMAIL_INPUT, USSDConstants.USSDInputType.STRING,"NO");
                    break;
                }
                case "INPUT": {
                    String strResponse = "";
                    String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_INPUT.name());

                    if(strEmailAddress.equals("")){
                        strResponse = strHeader+"\n{Enter a valid e-mail address}\nEnter your e-mail address:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.UPDATE_EMAIL_INPUT, USSDConstants.USSDInputType.STRING,"NO");
                    } else {
                        String strEmailRegex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
                        Pattern ptEmailPattern = Pattern.compile(strEmailRegex);
                        Matcher mtEmailMatcher = ptEmailPattern.matcher(strEmailAddress);

                        if(mtEmailMatcher.matches()) {
                            strResponse = strHeader+"\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.UPDATE_EMAIL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                        } else {
                            strResponse = strHeader+"\n{Enter a valid e-mail address}\nEnter your e-mail address:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.UPDATE_EMAIL_INPUT, USSDConstants.USSDInputType.STRING,"NO");
                        }
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strNewEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_INPUT.name());

                        String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);

                        String strResponse =  "Confirm change of e-mail address\nFrom: " + strCurrentEmailAddress + "\nTo: "+strNewEmailAddress+"\n";

                        if(strCurrentEmailAddress.equals("")){
                            strResponse =  "Confirm that your are setting your e-mail address\nTo: "+strNewEmailAddress+"\n";
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.UPDATE_EMAIL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.UPDATE_EMAIL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_CONFIRMATION.name());
                    String strNewEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UPDATE_EMAIL_INPUT.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "";

                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.changeEmailAddress(theUSSDRequest);

                            switch (transactionReturnVal){
                                case SUCCESS:{
                                    strResponse = "Dear member, your request to change e-mail address has been COMPLETED SUCCESSFULLY.";
                                    break;
                                }
                                case ERROR: {
                                    strResponse = "Dear member, your request to change e-mail address has FAILED. Please try again. If changing e-mail address fails again, contact the SACCO for assistance.";
                                    break;
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.UPDATE_EMAIL_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO": {
                            String strResponse = "Dear member, your request to change e-mail address was NOT confirmed. E-mail address change request NOT COMPLETED.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.UPDATE_EMAIL_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);

                            String strResponse =  "{Select a valid menu}\nConfirm change of e-mail address\nFrom: " + strCurrentEmailAddress + "\nTo: "+strNewEmailAddress+"\n";

                            if(strCurrentEmailAddress.equals("")){
                                strResponse =  "{Select a valid menu}\nConfirm that your are setting your e-mail address\nTo: "+strNewEmailAddress+"\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.UPDATE_EMAIL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    strHeader = strHeader + "\n{Select a valid menu}\n";
                    String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);
                    String strResponse = strHeader+"\nYour current e-mail address is "+strCurrentEmailAddress+"\n\nEnter new email address";
                    if(strCurrentEmailAddress.equals("")){
                        strResponse = strHeader+"\nEnter your email address";
                    }
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.UPDATE_EMAIL_INPUT, USSDConstants.USSDInputType.STRING,"NO");
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

    public default  USSDResponse displayMenu_CheckBeneficiaries(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();

        try{
            String strHeader = "Check Beneficiaries";
            switch (theParam){
                case "MENU": {
                    strHeader = strHeader+"\nSelect mode of delivery\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "SMS", "1: Send via SMS");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "EMAIL", "2: Send via e-mail");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE, "NO",theArrayListUSSDSelectOption);

                    break;
                }
                case "MODE": {
                    String strMode = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE.name());

                    String strResponse = "";

                    switch (strMode) {
                        case "SMS": {
                            strResponse = strHeader+"\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_PIN, USSDConstants.USSDInputType.STRING,"NO");
                            break;
                        }
                        case "EMAIL": {
                            String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);

                            if(strCurrentEmailAddress.equals("")){
                                strResponse = strHeader+"\nEnter your e-mail address:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL, USSDConstants.USSDInputType.STRING,"NO");
                            } else {
                                strResponse = strHeader+"\nEnter your PIN:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_PIN, USSDConstants.USSDInputType.STRING,"NO");
                            }
                            break;
                        }
                        default:{
                            strHeader = strHeader + "\n{Select a valid menu}\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "SMS", "1: Send via SMS");
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "EMAIL", "2: Send via e-mail");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE, "NO",theArrayListUSSDSelectOption);
                        }
                    }

                    break;
                }
                case "EMAIL": {
                    String strResponse = "";
                    String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL.name());

                    if(strEmailAddress.equals("")){
                        strResponse = strHeader+"\n{Enter a valid e-mail address}\nEnter your e-mail address:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL, USSDConstants.USSDInputType.STRING,"NO");
                    } else {
                        String strEmailRegex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
                        Pattern ptEmailPattern = Pattern.compile(strEmailRegex);
                        Matcher mtEmailMatcher = ptEmailPattern.matcher(strEmailAddress);

                        if(mtEmailMatcher.matches()) {
                            strResponse = strHeader+"\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_PIN, USSDConstants.USSDInputType.STRING,"NO");
                        } else {
                            strResponse = strHeader+"\n{Enter a valid e-mail address}\nEnter your e-mail address:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL, USSDConstants.USSDInputType.STRING,"NO");
                        }
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_PIN.name());
                    if(strLoginPIN.equals(strPIN)){
                        String strResponse = "";
                        String strModeOfDelivery = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE.name());

                        if(strModeOfDelivery.equals("SMS")){
                            strModeOfDelivery = "SMS Message";

                            strResponse =  "Confirm request to check beneficiaries\nMode of delivery: " + strModeOfDelivery + "\nTo: +"+theUSSDRequest.getUSSDMobileNo()+"\n";
                        } else {
                            strModeOfDelivery = "E-mail";

                            String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);

                            if(strCurrentEmailAddress.equals("")){
                                strResponse =  "Confirm request to check beneficiaries\nMode of delivery: " + strModeOfDelivery + "\nTo: "+strCurrentEmailAddress+"\n";
                            } else {
                                String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL.name());
                                strResponse =  "Confirm request to check beneficiaries\nMode of delivery: " + strModeOfDelivery + "\nTo: "+strEmailAddress+"\n";
                            }
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.CHECK_BENEFICIARIES_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_CONFIRMATION.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "";

                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.checkBeneficiaries(theUSSDRequest);

                            switch (transactionReturnVal){
                                case SUCCESS:{
                                    String strAdditionalText;
                                    String strModeOfDelivery = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE.name());

                                    if(strModeOfDelivery.equals("SMS")){
                                        strAdditionalText =  "\nSMS sent to: +"+theUSSDRequest.getUSSDMobileNo()+"\n";
                                    } else {
                                        String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);

                                        if(strCurrentEmailAddress.equals("")){
                                            strAdditionalText =  "\nE-mail sent to: "+strCurrentEmailAddress+"\n";
                                        } else {
                                            String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL.name());
                                            strAdditionalText =  "\nE-mail sent to: "+strEmailAddress+"\n";
                                        }
                                    }

                                    strResponse = "Dear member, your request to check beneficiaries has been COMPLETED SUCCESSFULLY."+strAdditionalText;
                                    break;
                                }
                                case ERROR: {
                                    strResponse = "Dear member, your request to check beneficiaries has FAILED. Please try again. If changing e-mail address fails again, contact the SACCO for assistance.";
                                    break;
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO": {
                            String strResponse = "Dear member, your request to check beneficiaries was NOT confirmed. Request NOT COMPLETED.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strResponse = "";
                            String strModeOfDelivery = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE.name());

                            if(strModeOfDelivery.equals("SMS")){
                                strModeOfDelivery = "SMS Message";

                                strResponse =  "{Select a valid menu}\nConfirm request to check beneficiaries\nMode of delivery: " + strModeOfDelivery + "\nTo: +"+theUSSDRequest.getUSSDMobileNo()+"\n";
                            } else {
                                strModeOfDelivery = "E-mail";

                                String strCurrentEmailAddress = USSDAPI.getMemberEmailAddress(theUSSDRequest);

                                if(strCurrentEmailAddress.equals("")){
                                    strResponse =  "{Select a valid menu}\nConfirm request to check beneficiaries\nMode of delivery: " + strModeOfDelivery + "\nTo: "+strCurrentEmailAddress+"\n";
                                } else {
                                    String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHECK_BENEFICIARIES_EMAIL.name());
                                    strResponse =  "{Select a valid menu}\nConfirm request to check beneficiaries\nMode of delivery: " + strModeOfDelivery + "\nTo: "+strEmailAddress+"\n";
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                        }
                    }
                    break;
                }
                default: {
                    strHeader = strHeader+"\nSelect mode of delivery\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "SMS", "1: Send via SMS");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "EMAIL", "2: Send via e-mail");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHECK_BENEFICIARIES_MODE, "NO",theArrayListUSSDSelectOption);

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
