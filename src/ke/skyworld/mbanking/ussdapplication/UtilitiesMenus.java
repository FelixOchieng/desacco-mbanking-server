package ke.skyworld.mbanking.ussdapplication;


import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.register.MemberRegisterResponse;
import ke.skyworld.lib.mbanking.register.RegisterConstants;
import ke.skyworld.lib.mbanking.register.RegisterProcessor;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;



public interface UtilitiesMenus {

    default USSDResponse displayMenu_UtilitiesMenu(USSDRequest theUSSDRequest, String theParam, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("UTILITY_CODE");

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "Buy Airtime", "1: Buy Airtime");
            if(CBSAPI.checkService("Utility Bill Payment")){
                for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
                    int intOptionMenu = llSPAAccounts.indexOf(serviceProviderAccount)+1;
                    intOptionMenu = intOptionMenu+1;
                    String strOptionMenu = String.valueOf(intOptionMenu);
                    String strProviderAccountIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
                    String strProviderAccountLongTag = serviceProviderAccount.getProviderAccountLongTag();
                    String strOptionDisplayText = strOptionMenu+": "+strProviderAccountLongTag;
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strProviderAccountIdentifier, strOptionDisplayText);
                }
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.UTILITIES_MENU, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_UtilitiesMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Utilities(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            if (strUSSDDataType.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.getValue())) {
                String strHeader = "Utilities";
                theUSSDResponse = displayMenu_UtilitiesMenu(theUSSDRequest, theParam, strHeader);
            } else {

                String strUTILITIES_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());

                if (strUTILITIES_MENU != null && !strUTILITIES_MENU.isEmpty()) {
                    switch (strUTILITIES_MENU) {
                        case "Buy Airtime": {
                            theUSSDResponse = theAppMenus.displayMenu_Etopup(theUSSDRequest, theParam);
                            break;
                        }
                        default: {
                            theUSSDResponse = theAppMenus.displayMenu_PayBill(theUSSDRequest, theParam);
                            break;
                        }
                    }
                } else {
                    String strHeader = "Utilities\n{Select a valid menu}";
                    theUSSDResponse = displayMenu_UtilitiesMenu(theUSSDRequest, theParam, strHeader);
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Utilities() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Etopup(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader = "Buy Airtime";
        try{
            switch (theParam) {
                case "MENU": {
                    strHeader += " \nSelect account\n";
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.ETOPUP_ACCOUNT);
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
                    MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strAccount, RegisterConstants.MemberRegisterType.BLACKLIST);
                    if(registerResponse.getResponseType().equals(APIConstants.RegisterViewResponse.VALID.getValue()) && 1 != 1) {
                        strHeader = strHeader + "\nSorry the selected account is not registered to perform this transaction.\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.ETOPUP_ACCOUNT);
                    } else {
                        if (strAccount.length() > 0){
                            /*String strResponse = strHeader+"\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");*/
                            strHeader = "Buy Airtime\nSelect destination\n";
                            theUSSDResponse = getEtopupToOptionMenu(theUSSDRequest, strHeader);

                        }else{
                            String strHeader2 = strHeader + " \n{Select a valid account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.ETOPUP_ACCOUNT);
                        }
                    }
                    break;
                }
                case "TO_OPTION": {
                    String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_TO_OPTION.name());
                    if (strToOption.equalsIgnoreCase("MY_NUMBER")) {
                        strHeader += "\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                        strHeader += "\nEnter Other Mobile No.\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader, AppConstants.USSDDataType.ETOPUP_TO, USSDConstants.USSDInputType.STRING,"NO");
                    }else {
                        strHeader += "\n{Select a valid menu}\nSelect etopup option?\n";
                        theUSSDResponse = displayMenu_Etopup(theUSSDRequest, "ACCOUNT");
                    }
                    break;
                }
                case "TO":{
                    String strOtherMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_TO.name());
                    strOtherMobileNo = APIUtils.sanitizePhoneNumber(strOtherMobileNo);
                    if(!strOtherMobileNo.equalsIgnoreCase("INVALID_MOBILE_NUMBER")){
                        strHeader += "\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else {
                        strHeader += "\n{Enter a valid mobile number}\nEnter Other Mobile No.\n";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strHeader, AppConstants.USSDDataType.ETOPUP_TO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());

                    String strMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.AIRTIME_PURCHASE).getMinimum();
                    String strMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.AIRTIME_PURCHASE).getMaximum();

                    if (!strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else if (Double.parseDouble(strAmount) < Double.parseDouble(strMinimum)) {
                        String strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strMinimum, "#,###.##") + "}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else if (Double.parseDouble(strAmount) > Double.parseDouble(strMaximum)) {
                        String strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strMaximum, "#,###.##") + "}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ETOPUP_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());
                        String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.AIRTIME_PURCHASE, strAmount);

                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strBeneficiary = "\nMobile No.: +"+theUSSDRequest.getUSSDMobileNo();
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_TO_OPTION.name());
                        if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                            String strOtherMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_TO.name());
                            strOtherMobileNo = APIUtils.sanitizePhoneNumber(strOtherMobileNo);

                            strBeneficiary = "\nMobile No.: +"+strOtherMobileNo;
                        }

                        String strResponse =  "Confirm "+strHeader + "\n" + "Paying Account No: " + strAccount + strBeneficiary + "\nAmount: KES "+strAmount+strCharge+"\n"; //With Account No

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ETOPUP_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_CONFIRMATION.name());
                    if(strConfirmation.equalsIgnoreCase("YES")){
                        String  strResponse = "Dear member, your " +strHeader+ " request has been received successfully. Please wait shortly as it's being processed.";

                        /*Thread worker = new Thread(() -> {
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.airtimePurchase(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);
                            System.out.println("etopup: "+transactionReturnVal.getValue());
                        });
                        worker.start();*/

                        APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.airtimePurchase(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your " +strHeader+ " request has been received successfully. Please wait shortly as it's being processed.";
                        }else {
                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your " +strHeader+ " request CANNOT be completed.\n";
                                    break;
                                }
                                case INSUFFICIENT_BAL: {
                                    strResponse = "Dear member, you have insufficient balance to complete this request. Please deposit to your account and try again.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your " +strHeader+ " request CANNOT be completed.\n";
                                    break;
                                }
                                case LOAN_DEFAULTER: {
                                    strResponse = "Dear member, you have a loan that is in arrears.\nPlease contact us on 0793281989 for more information.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your " +strHeader+ " request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);

                    }else if(strConfirmation.equalsIgnoreCase("NO")){
                        String strResponse = "Dear member, your " +strHeader+ " request NOT confirmed. " +strHeader+ "  request NOT COMPLETED.\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_ACCOUNT.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_AMOUNT.name());
                        String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.AIRTIME_PURCHASE, strAmount);

                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        String strBeneficiary = "\nMobile No.: +"+theUSSDRequest.getUSSDMobileNo();
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_TO_OPTION.name());
                        if (strToOption.equalsIgnoreCase("OTHER_NUMBER")) {
                            String strOtherMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ETOPUP_TO.name());
                            strOtherMobileNo = APIUtils.sanitizePhoneNumber(strOtherMobileNo);

                            strBeneficiary = "\nMobile No.: +"+strOtherMobileNo;
                        }
                        String strResponse =  "Confirm "+strHeader + "\n{Select a valid menu}\nPaying Account No: " + strAccount + strBeneficiary + "\nAmount: KES "+strAmount+strCharge+"\n"; //With Account No

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_Etopup() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Etopup() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_PayBill(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strBiller = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());
        APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(strBiller);
        String strBillerName = serviceProviderAccount.getProviderAccountLongTag();
        String strSPProviderAccountCode = serviceProviderAccount.getProviderAccountCode();
        String strHeader = "Pay for " + strBillerName;
        try{
            switch (theParam) {
                case "MENU": {
                    //USE MENUs
                    theUSSDResponse = displayMenu_Paybill_Maintain_Accounts(theUSSDRequest, theParam);

                    break;
                }
                case "BILLER_ACCOUNT": {

                    String strMenuOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());
                    String theAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());
                    String strAccountNaming = getBillerAccountNaming(theAccountType);

                    String strAction = "";
                    if(!strMenuOption.isEmpty()){
                        HashMap<String, String> hmMenuOption = Utils.toHashMap(strMenuOption);
                        strAction = hmMenuOption.get("ACTION");
                    }

                    switch (strAction) {
                        case "CHOICE": {
                            String strHeader2 = strHeader + " \nSelect paying account\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT);
                            break;
                        }
                        case "ADD": {
                            String strResponse = "Add " + strBillerName +"\nEnter " + strAccountNaming + ":";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        case "REMOVE": {
                            String strHeader2 = "Remove " + strBillerName +"\nSelect " + strAccountNaming + " to Remove:";

                            String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                            theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader2, USSDConstants.Condition.NO);
                            break;
                        }
                        default:{
                            String strHeader2 = "Pay for " + strBillerName +"\n{Select a VALID MENU}:";

                            System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() UNKNOWN PARAM ERROR : strAction = " + strAction);

                            String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                            theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader2, USSDConstants.Condition.YES);
                            break;
                        }
                    }
                    break;
                }
                case "FROM_ACCOUNT": {
                    String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
                    MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strFromAccount, RegisterConstants.MemberRegisterType.BLACKLIST);
                    if(registerResponse.getResponseType().equals(APIConstants.RegisterViewResponse.VALID.getValue()) && 1 != 1) {
                        strHeader = strHeader + "\nSorry the selected account is not registered to perform this transaction.\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT);
                    } else {
                        if( strFromAccount.length() > 0 ){
                            String strResponse = strHeader+"\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                        }else {
                            String strHeader2 = strHeader + " \n{Select a valid paying account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader2, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT);
                        }
                    }

                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());

                    if (strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_PIN, USSDConstants.USSDInputType.STRING, "NO");

                        String strPayBillMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.PAY_BILL).getMinimum();
                        String strPayBillMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.PAY_BILL).getMaximum();

                        double dblPayBillMinimum = Double.parseDouble(strPayBillMinimum);
                        double dblPayBillMaximum = Double.parseDouble(strPayBillMaximum);

                        double dblAmountEntered = Double.parseDouble(strAmount);

                        if (dblAmountEntered < dblPayBillMinimum) {
                            strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strPayBillMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }

                        if (dblAmountEntered > dblPayBillMaximum) {
                            strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strPayBillMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strBillAccountNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());

                        HashMap<String, String> hmAccount = Utils.toHashMap(strBillAccountNumberHashMap);
                        String strAccountID = hmAccount.get("ACCOUNT_ID");
                        String strAccountName = hmAccount.get("ACCOUNT_NAME");
                        String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                        String strAccountNaming = getBillerAccountNaming(strBiller);
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());
                        String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.UTILITY_PAYMENT, strAmount);

                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse =  "Confirm "+strHeader + "\n\nBill " + strAccountNaming + ": " + strAccountIdentifier + "\nName: " + strAccountName + "\nPaying Acct No: " + strFromAccount + "\nAmount: KES "+strAmount+strCharge+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.PAY_BILL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_CONFIRMATION.name());
                    if(strConfirmation.equalsIgnoreCase("YES")){

                        String  strResponse;

                        /*Thread worker = new Thread(() -> {
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.payBill(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);
                            System.out.println("etopup: "+transactionReturnVal.getValue());
                        });
                        worker.start();*/

                        APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.payBill(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your request to " +strHeader+ " has been received successfully. Please wait shortly as it's being processed.";
                        } else {
                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your request to " +strHeader+ " CANNOT be completed.\n";
                                    break;
                                }
                                case INSUFFICIENT_BAL: {
                                    strResponse = "Dear member, you have insufficient balance to complete this request. Please deposit to your account and try again.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your request to " +strHeader+ " CANNOT be completed.\n";
                                    break;
                                }
                                case MOBILE_NUMBER_NOT_WHITELISTED: {
                                    strResponse = "Dear member, your mobile number is not registered to perform this transaction.\nPlease contact us on 0793281989 for more information.\n";
                                    break;
                                }
                                case LOAN_DEFAULTER: {
                                    strResponse = "Dear member, you have a loan that is in arrears.\nPlease contact us on 0793281989 for more information.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your " +strHeader+ " request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_END, "NO",theArrayListUSSDSelectOption);

                    }else if(strConfirmation.equalsIgnoreCase("NO")){
                        String strResponse = "Dear member, your " +strHeader+ " request NOT confirmed. " +strHeader+ "  request NOT COMPLETED.\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_END, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_FROM_ACCOUNT.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_AMOUNT.name());
                        String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.UTILITY_PAYMENT, strAmount);

                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strBillAccountNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT.name());
                        String strAccountNaming = getBillerAccountNaming(strBiller);

                        String strResponse =  "Confirm "+strHeader +  "\n{Select a valid menu}\n\nBill " + strAccountNaming + ": " + strBillAccountNumber + "\nPaying Acct No: "+strFromAccount+"\nAmount: KES "+strAmount+strCharge+"\n";


                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_Etopup() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Etopup() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_Paybill_Maintain_Accounts(USSDRequest theUSSDRequest, String theParam){
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{

            AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());
            String theAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.UTILITIES_MENU.name());
            APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(theAccountType);
            String strBillerName = serviceProviderAccount.getProviderAccountLongTag();
            String strSPProviderAccountCode = serviceProviderAccount.getProviderAccountCode();
            String strAccountNaming = getBillerAccountNaming(theAccountType);

            switch (ussdDataType) {
                case UTILITIES_MENU: {
                    String strHeader = "Pay for " + strBillerName +"\nSelect " + strAccountNaming + ":";

                    String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT: {

                    String theAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT.name());

                    if( theAccountNo.matches("^\\d{4,24}$")  ) { //4 - 24 Digits
                        String strResponse = "Add " + strBillerName +"\nEnter " + strAccountNaming + " NAME:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                    }else{
                        String strResponse = "Add " + strBillerName + "\n{Enter a VALID "+strAccountNaming+"}:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case PAY_BILL_MAINTENANCE_ACCOUNT_NAME: {
                    //ADD Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_ACCOUNT.name());
                    String strAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_NAME.name());

                    try{
                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");

                        String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                        SPManager spManager = new SPManager(strIntegritySecret);
                        spManager.createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN,strMobileNo,strSPProviderAccountCode, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO,strAccountNo, strAccountName);


                    } catch (Exception e){
                        System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() ERROR : " + e.getMessage());
                    }

                    String strHeader = "Pay for " + strBillerName +"\nSelect " + strAccountNaming + ":";

                    String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE: {
                    //REMOVE Account

                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE.name());

                    if(!strAccountHashMap.isEmpty()){
                        try{

                            HashMap<String, String> hmAccount = Utils.toHashMap(strAccountHashMap);
                            String strAccountID = hmAccount.get("ACCOUNT_ID");
                            //String strAccountName = hmAccount.get("ACCOUNT_NAME");
                            //String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                            String strSPProviderAccount = theAccountType.replaceAll(" ","_");

                            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                            SPManager spManager = new SPManager(strIntegritySecret);
                            spManager.removeUserSavedAccountsByAccountId(strAccountID);

                        }catch (Exception e){
                            System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() ERROR : " + e.getMessage());
                        }


                        String strHeader = "Pay for " + strBillerName +"\nSelect " + strAccountNaming + ":";
                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    }else{
                        String strHeader = "Remove " + strBillerName +"\n{Select a VALID MENU}:";

                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_MAINTENANCE_ACCOUNT_REMOVE, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.NO);
                    }

                    break;
                }
                default:{

                    String strHeader = "Pay for " + strBillerName +"\n{Select a VALID " + strAccountNaming + "}:";

                    System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() UNKNOWN PARAM ERROR : strUSSDDataType = " + ussdDataType.name());
                    
                    String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.PAY_BILL_BILLER_ACCOUNT, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_Paybill_Maintain_Accounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
    
    default String getBillerAccountNaming(String strBiller) {

        String strNaming = "Account No";

        try {

            switch (strBiller) {
                case "888880": {
                    strNaming = "Meter No";
                    break;
                }

                case "888888":
                case "444400":
                case "444900":
                case "320320":
                case "423655":
                case "585858":
                case "808200":{
                    strNaming = "Account No";
                    break;
                }
                default:{
                    strNaming = "Account No";
                    break;
                }
            }
        }catch(Exception e){
            System.err.println("theAppMenus.getBillerAccountNaming() ERROR : " + e.getMessage());
        }
        finally{
        }

        return strNaming;
    }

    default boolean isValidBillerAccountNumber(String strBiller, String strBillAccountNumber) {

        boolean isValidAccountNumber = false;

        try {
            switch (strBiller) {
                case "888880": {
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{8,14}$");  //11 Digits
                    break;
                }
                case "888888":{
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{6,10}$"); //8 Digits
                    break;
                }
                case "444400":{
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{5,10}$"); //7 Digits
                    break;
                }
                case "444900": {
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{6,20}$"); //16 Digits ???
                    break;
                }
                case "320320":{
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{4,10}$"); //6 Digits
                    break;
                }
                case "423655":{
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{6,12}$"); //8 Digits
                    break;
                }
                case "585858":{
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{8,14}$"); //12 Digits
                    break;
                }
                default:{
                    isValidAccountNumber = strBillAccountNumber.matches("^\\d{4,20}$");
                    break;
                }
            }
        }catch(Exception e){
            System.err.println("theAppMenus.isValidBillerAccountNumber() ERROR : " + e.getMessage());
        }
        finally{
        }

        return isValidAccountNumber;
    }

    static APIUtils.ServiceProviderAccount getProviderAccountCode(String theSPProviderAccount){
        APIUtils.ServiceProviderAccount rVal = null;
        try {
            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("UTILITY_CODE");
            for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
                String strProviderIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
                if(strProviderIdentifier.equals(theSPProviderAccount)){
                    rVal = serviceProviderAccount;
                }
            }
        } catch (Exception e){
            System.err.println("theAppMenus.getProviderAccountCode() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    default USSDResponse getEtopupToOptionMenu(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_NUMBER", "1: MY phone number");
            //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_NUMBER", "2: OTHER phone number");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ETOPUP_TO_OPTION, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getEtopupOptionMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
