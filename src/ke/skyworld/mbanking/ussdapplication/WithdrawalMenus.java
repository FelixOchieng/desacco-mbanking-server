package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.register.MemberRegisterResponse;
import ke.skyworld.lib.mbanking.register.RegisterConstants;
import ke.skyworld.lib.mbanking.register.RegisterProcessor;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

public interface WithdrawalMenus {

    default USSDResponse displayMenu_Withdrawal(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();
            switch (theParam) {
                case "MENU": {
                    String strHeader = "Withdraw to M-Pesa\nSelect Source Account\n";
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT, AppConstants.USSDDataType.WITHDRAWAL_END);
                    break;
                }

                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());
                    if (strAccount.length() > 0){
                        MemberRegisterResponse registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.ACCOUNT_NO, strAccount, RegisterConstants.MemberRegisterType.BLACKLIST);
                        if(registerResponse.getResponseType().equals(APIConstants.RegisterViewResponse.VALID.getValue())) {
                            String strHeader = "M-Pesa Cash Withdrawal\nSorry the selected account is not allowed to perform this transaction.\n{Select a valid account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT, AppConstants.USSDDataType.WITHDRAWAL_END);
                        } else {
                            String strCashWithdrawalAction = "";
                            String strResponse = "M-Pesa Cash Withdrawal\nSelect Withdrawal Option";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_NUMBER", "1: Withdraw to MY Number");
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_NUMBER", "2: Withdraw to OTHER Number");

                            /*if(Navision.getPort().checkIfServiceIsEnabledForUser(strAccount, "WD_TO_OTHER")) {
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_NUMBER", "2: Withdraw to OTHER Number");
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "DISABLE_CASH_WITHDRAWAL", "3: Disable Withdrawal to OTHER Number");
                            } else {
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ENABLE_CASH_WITHDRAWAL", "2: Enable Withdrawal to OTHER Number");
                            }*/
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION, "NO", theArrayListUSSDSelectOption);
                        }
                    }else{
                        String strHeader = "Withdraw to M-Pesa\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT, AppConstants.USSDDataType.WITHDRAWAL_END);
                    }
                    break;
                }
                case "TO_OPTION": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());
                    if (strUserInput.length() > 0){
                        if(strUserInput.equals("MY_NUMBER")){
                            String strResponse = "Withdraw to M-Pesa\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                        } else if(strUserInput.equals("OTHER_NUMBER")){
                            theUSSDResponse = displayMenu_CashWithdrawal_Maintain_Accounts(theUSSDRequest, theParam);
                        } else {
                            theUSSDResponse = displayMenu_ActivateWithdrawal(theUSSDRequest, "ACTION");
                        }
                    }else{
                        theUSSDResponse = displayMenu_Withdrawal(theUSSDRequest, "ACCOUNT");
                    }
                    break;
                }
                case "TO": {
                    String strMenuOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                    String strAccountNaming = "Mobile Number";
                    String strSPProviderAccountCode = AppConstants.SPProviderAccountCode.SAFARICOM_B2C.getValue();

                    String strAction = "";
                    if(!strMenuOption.isEmpty()){
                        HashMap<String, String> hmMenuOption = Utils.toHashMap(strMenuOption);
                        strAction = hmMenuOption.get("ACTION");
                    }

                    switch (strAction) {
                        case "CHOICE": {
                            String strResponse = "Withdraw to M-Pesa\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                            break;
                        }
                        case "ADD": {
                            String strResponse = "Add Mobile Number\nEnter " + strAccountNaming + ":";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        case "REMOVE": {
                            String strHeader2 = "Remove Mobile Number\nSelect " + strAccountNaming + " to Remove:";

                            theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_REMOVE, "0000", strAccountNaming, strSPProviderAccountCode, strHeader2, USSDConstants.Condition.NO);
                            break;
                        }
                        default:{
                            String strHeader2 = "M-Pesa Cash Withdrawal\n{Select a VALID MENU}:";

                            System.err.println("theAppMenus.displayMenu_Withdrawal() UNKNOWN PARAM ERROR : strAction = " + strAction);

                            theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO, "0000", strAccountNaming, strSPProviderAccountCode, strHeader2, USSDConstants.Condition.YES);
                            break;
                        }
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
                    boolean blAmountIsMultipleOf100 = false;
                    boolean blExceedsLimit = false;

                    double dblMinimumAmount = Double.parseDouble(theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.CASH_WITHDRAWAL).getMinimum());
                    double dblMaximumAmount = Double.parseDouble(theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.CASH_WITHDRAWAL).getMaximum());

                    String strWithdrawalToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

                    if(!strWithdrawalToOption.equals("MY_NUMBER")){
                        dblMinimumAmount = Double.parseDouble(theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.CASH_WITHDRAWAL_TO_OTHER).getMinimum());
                        dblMaximumAmount = Double.parseDouble(theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.CASH_WITHDRAWAL_TO_OTHER).getMaximum());
                    }


                    String strResponse = "Withdraw to M-Pesa\nEnter your PIN:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_PIN, USSDConstants.USSDInputType.STRING,"NO");

                    if(!strAmount.matches("^[1-9][0-9]*$")){
                        strResponse = "Withdraw to M-Pesa\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if(Double.parseDouble(strAmount) < dblMinimumAmount){
                        blExceedsLimit = true;
                        strResponse = "Withdraw to M-Pesa\n{MINIMUM amount allowed is KES "+Utils.formatDouble(dblMinimumAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if(Double.parseDouble(strAmount) > dblMaximumAmount){
                        blExceedsLimit = true;
                        strResponse = "Withdraw to M-Pesa\n{MAXIMUM amount allowed is KES "+Utils.formatDouble(dblMaximumAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());

                        String strMobileNo = Long.toString(theUSSDRequest.getUSSDMobileNo());
                        String strName = "";

                        String strWithdrawalToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

                        if(!strWithdrawalToOption.equals("MY_NUMBER")){
                            String strMobileNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                            HashMap<String, String> hmAccount = Utils.toHashMap(strMobileNumberHashMap);
                            strMobileNo = hmAccount.get("ACCOUNT_IDENTIFIER");
                            strName = " ("+hmAccount.get("ACCOUNT_NAME")+")";
                        }

                        strMobileNo = APIUtils.sanitizePhoneNumber(strMobileNo);

                        String strResponse =  "Confirm M-Pesa Withdrawal\nTo: "+strMobileNo + strName+"\nAmount: KES "+strAmount+"\nAccount: "+strAccount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strResponse = "Withdraw to M-Pesa\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.WITHDRAWAL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_CONFIRMATION.name());

                    switch (strConfirmation){
                        case "YES":{
                            String strResponse = "Dear member, your Cash Withdrawal request has been received successfully. Please wait shortly as it's being processed.";

                            APIConstants.TransactionReturnVal transactionReturnVal = null;

                            transactionReturnVal = theUSSDAPI.mobileMoneyWithdrawal(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);

                            assert transactionReturnVal != null;
                            if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your M-Pesa Cash Withdrawal request has been received successfully. Please wait shortly as it's being processed.";
                            }else {
                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your M-Pesa Cash Withdrawal request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your M-Pesa Cash Withdrawal request CANNOT be completed.\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                        break;
                                    }
                                    case INVALID_MOBILE_NUMBER: {
                                        strResponse = "Dear member, you have entered an invalid phone number. Please check the phone number and try again.\n";
                                        break;
                                    }

                                    default: {
                                        strResponse = "Sorry, your M-Pesa Cash Withdrawal request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO":{
                            String strResponse = "Dear member, your M-Pesa Cash Withdrawal request NOT confirmed. Cash Withdrawal request NOT COMPLETED.\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_AMOUNT.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_ACCOUNT.name());

                            String strMobileNo = Long.toString(theUSSDRequest.getUSSDMobileNo());

                            String strWithdrawalToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

                            if(!strWithdrawalToOption.equals("MY_NUMBER")){
                                String strMobileNumberHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO.name());
                                HashMap<String, String> hmAccount = Utils.toHashMap(strMobileNumberHashMap);
                                strMobileNo = hmAccount.get("ACCOUNT_IDENTIFIER");
                            }

                            strMobileNo = APIUtils.sanitizePhoneNumber(strMobileNo);

                            String strResponse =  "Confirm M-Pesa Cash Withdrawal\n{Select a valid menu}\nTo:"+strMobileNo+"\nAmount: KES "+strAmount+"\nAccount: "+strAccount+"\n";


                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                default:{
                    System.err.println("theAppMenus.displayMenu_Withdrawal() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Cash Withdrawal\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.err.println("theAppMenus.displayMenu_Withdrawal() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_CashWithdrawal_Maintain_Accounts(USSDRequest theUSSDRequest, String theParam){
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{

            AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());
            String theAccountType = "0000";
            String strSPProviderAccountCode = AppConstants.SPProviderAccountCode.SAFARICOM_B2C.getValue();
            String strAccountNaming = "Receiving Mobile Number";

            switch (ussdDataType) {
                case MAIN_IN_MENU:
                case WITHDRAWAL_TO_OPTION: {
                    String strHeader = "Cash Withdrawal to M-Pesa\nSelect " + strAccountNaming + ":\n";

                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT: {
                    //ADD Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT.name());

                    if(!strAccountNo.equals("")){
                        strAccountNo = APIUtils.sanitizePhoneNumber(strAccountNo);
                        if(strAccountNo.equals("INVALID_MOBILE_NUMBER")){
                            String strResponse = "Add Mobile Number\n{Please enter a valid mobile number}\nEnter " + strAccountNaming + ":";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            try{
                                String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                                SPManager spManager = new SPManager(strIntegritySecret);

                                LinkedList<LinkedHashMap<String, String>> listAccounts = spManager.getUserSavedAccountsByProvider(SPManagerConstants.UserIdentifierType.MSISDN, strMobileNo, strSPProviderAccountCode);

                                for (int i = 1; i <= listAccounts.size() ; i++) {
                                    LinkedHashMap<String, String> account = listAccounts.get((i - 1));

                                    String strUserAccountIdentifier = account.get("user_account_identifier");

                                    strUserAccountIdentifier = APIUtils.sanitizePhoneNumber(strUserAccountIdentifier);

                                    if(strAccountNo.equals(strUserAccountIdentifier)){
                                        String strResponse = "Add Mobile Number\n{The mobile number already exists}\nEnter " + strAccountNaming + ":";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                        break;
                                    }
                                }
                            }catch (Exception e){
                                System.err.println("theAppMenus.displayMenu_CashWithdrawal_Maintain_Accounts() ERROR : " + e.getMessage());
                            }

                            String strResponse = "Add Mobile Number\nEnter Recipient Name";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = "Add Mobile Number\nEnter " + strAccountNaming + ":";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_NAME: {
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_ACCOUNT.name());
                    String strAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_NAME.name());

                    if(!strAccountName.equals("")){
                        String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                        SPManager spManager = new SPManager(strIntegritySecret);
                        spManager.createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN, strMobileNo,strSPProviderAccountCode, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO, strAccountNo, strAccountName);

                        String strHeader = "M-Pesa Cash Withdrawal\nSelect " + strAccountNaming + ":";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    } else {
                        String strResponse = "Add Mobile Number\nEnter Recipient Name";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_REMOVE: {
                    //REMOVE Account

                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_REMOVE.name());

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
                            System.err.println("theAppMenus.displayMenu_CashWithdrawal_Maintain_Accounts() ERROR : " + e.getMessage());
                        }


                        String strHeader = "M-Pesa Cash Withdrawal\nSelect " + strAccountNaming + ":";
                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    }else{
                        String strHeader = "M-Pesa Cash Withdrawal\n{Select a VALID MENU}:";

                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT_REMOVE, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.NO);
                    }

                    break;
                }
                default:{

                    String strHeader = "M-Pesa Cash Withdrawal\n{Select a VALID " + strAccountNaming + "}:";

                    System.err.println("theAppMenus.displayMenu_CashWithdrawal_Maintain_Accounts() UNKNOWN PARAM ERROR : strUSSDDataType = " + ussdDataType.name());

                    String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_CashWithdrawal_Maintain_Accounts() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static APIUtils.ServiceProviderAccount getProviderAccountCode(String theSPProviderAccount){
        APIUtils.ServiceProviderAccount rVal = null;
        try {
            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("SHORT_CODE");
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

    public default USSDResponse displayMenu_ActivateWithdrawal(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();

        try{
            String strHeader = "M-Pesa Withdrawal to OTHER Number Control";

            switch (theParam){
                case "ACTION": {
                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());

                    if(!strAction.equals("")){
                        if(strAction.equals("ENABLE_CASH_WITHDRAWAL")){
                            strAction = "enable";
                        } else {
                            strAction = "disable";
                        }

                        strHeader = strAction.toUpperCase()+" M-Pesa Withdrawal to OTHER Number\nEnter your Mobile Banking PIN";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    } else {
                        String strResponse = "M-Pesa Withdrawal to OTHER Number\nSelect Withdrawal Option";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_NUMBER", "1: Withdraw to MY Number");

                        /*if(new USSDAPI().checkIfFunctionalityEnabledForUser(String.valueOf(theUSSDRequest.getUSSDMobileNo()), "WD_TO_OTHER")) {
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_NUMBER", "2: Withdraw to OTHER Number");
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "DISABLE_CASH_WITHDRAWAL", "3: Disable Withdrawal to OTHER Number");
                        } else {
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ENABLE_CASH_WITHDRAWAL", "2: Enable Withdrawal to OTHER Number");
                        }*/

                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION, "NO", theArrayListUSSDSelectOption);
                    }

                    break;
                }
                case "PIN": {
                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_PIN.name());
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());

                    String strActionName = "";

                    if(strLoginPIN.equals(strPIN)){

                        String strResponse;

                        if(strAction.equals("ENABLE_CASH_WITHDRAWAL")){
                            strActionName = "enable";
                        } else {
                            strActionName = "disable";
                        }

                        switch (strAction) {
                            case "ENABLE_CASH_WITHDRAWAL": {
                                strResponse =  strActionName.toUpperCase() + " M-Pesa Withdrawal to OTHER Number\nDo you wish to proceed to ENABLE M-Pesa Withdrawal to OTHER Number?\n";
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                                break;
                            }
                            case "DISABLE_CASH_WITHDRAWAL": {
                                strResponse =  strActionName.toUpperCase() + " M-Pesa Withdrawal to OTHER Number\nDo you wish to proceed to DISABLE M-Pesa Withdrawal to OTHER Number?\n";
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                                break;
                            }
                        }
                    } else {
                        if(strAction.equals("ENABLE_CASH_WITHDRAWAL")){
                            strAction = "enable";
                        } else {
                            strAction = "disable";
                        }
                        strHeader = strAction.toUpperCase()+" M-Pesa Withdrawal to OTHER Number\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_CONFIRMATION.name());
                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION.name());
                    if(strAction.equals("ENABLE_CASH_WITHDRAWAL")){
                        strAction = "enable";
                    } else {
                        strAction = "disable";
                    }

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "";

                            APIConstants.TransactionReturnVal theResponseStatus = theUSSDAPI.enableDisableCashWithdrawal(theUSSDRequest);

                            switch (theResponseStatus){
                                case SUCCESS:{
                                    strResponse = "Dear member, your request to "+strAction.toUpperCase()+" M-Pesa Withdrawal to OTHER Number has been completed successfully.";
                                    break;
                                }
                                case ERROR:
                                default:{
                                    strResponse = "Dear member, your request to "+strAction.toUpperCase()+" M-Pesa Withdrawal to OTHER Number has FAILED. Please try again. If this action fails again, contact the SACCO for assistance.";
                                    break;
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO": {
                            String strResponse = "";

                            strResponse = "Dear member, your request to "+strAction.toUpperCase()+" M-Pesa Withdrawal to OTHER Number was NOT confirmed. M-Pesa Withdrawal Control request NOT COMPLETED.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strResponse = "";

                            strResponse = "Dear member, your request to "+strAction.toUpperCase()+" M-Pesa Withdrawal to OTHER Number was NOT confirmed. M-Pesa Withdrawal Control request NOT COMPLETED.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CASH_WITHDRAWAL_ACTIVATION_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }
                default: {

                    strHeader = strHeader + "\n{Select a valid menu}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ENABLE_CASH_WITHDRAWAL", "1: ENABLE M-Pesa Withdrawal to OTHER Number");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "DISABLE_CASH_WITHDRAWAL", "2: DISABLE M-Pesa Withdrawal to OTHER Number");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.WITHDRAWAL_TO_OPTION, "NO",theArrayListUSSDSelectOption);
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_ActivateBankTransfer() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_AgentWithdrawal(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            //The Flow:
            //Withdraw via Agent -> Cash Withdrawal Enter Agent Number -> Select Source Account -> Input Amount -> Input PIN -> Confirm Transactions
            switch (theParam) {
                case "OPTION": {
                    String strResponse = "Harambee Pesa Agent Cash Withdrawal\nEnter agent number:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO, USSDConstants.USSDInputType.STRING,"NO");
                    break;
                }
                case "AGENT_NO": {
                    String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO.name());
                    if (strAgentNumber.length() > 0) {
                        String strAgentName = USSDAPI.validateAgentNumber(strAgentNumber);
                        if(strAgentName.equals("")){
                            String strResponse = "Harambee Pesa Agent Cash Withdrawal\n{Agent not found}\nEnter agent number:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO, USSDConstants.USSDInputType.STRING,"NO");
                        } else {
                            String strHeader = "Harambee Pesa Agent Cash Withdrawal\nSelect account\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.AGENT_WITHDRAWAL_ACCOUNT, AppConstants.USSDDataType.AGENT_WITHDRAWAL_END);
                        }
                    }
                    else {
                        String strResponse = "Harambee Pesa Agent Cash Withdrawal\n{Agent not found}\nEnter agent number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_ACCOUNT.name());
                    if (strAccount.length() > 0){
                        String strResponse = "Harambee Pesa Agent Cash Withdrawal\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strHeader = "Harambee Pesa Agent Cash Withdrawal\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.AGENT_WITHDRAWAL_ACCOUNT, AppConstants.USSDDataType.AGENT_WITHDRAWAL_END);
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT.name());

                    double dblMinimumAmount = Double.parseDouble(theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.AGENT_CASH_WITHDRAWAL).getMinimum());
                    double dblMaximumAmount = Double.parseDouble(theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.AGENT_CASH_WITHDRAWAL).getMaximum());

                    String strResponse = "Harambee Pesa Agent Cash Withdrawal\nEnter your PIN:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_PIN, USSDConstants.USSDInputType.STRING,"NO");

                    if(!strAmount.matches("^[1-9][0-9]*$")){
                        strResponse = "Harambee Pesa Agent Cash Withdrawal\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if(Double.parseDouble(strAmount) < dblMinimumAmount){
                        strResponse = "Harambee Pesa Agent Cash Withdrawal\n{MINIMUM amount allowed is KES "+Utils.formatDouble(dblMinimumAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    } else if(Double.parseDouble(strAmount) > dblMaximumAmount){
                        strResponse = "Harambee Pesa Agent Cash Withdrawal\n{MAXIMUM amount allowed is KES "+Utils.formatDouble(dblMaximumAmount,"#,###.##")+"}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_PIN.name());
                    String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO.name());
                    String strAgentName = USSDAPI.validateAgentNumber(strAgentNumber);
                    if(strLoginPIN.equals(strPIN)){

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse =  "Confirm Harambee Pesa Agent Cash Withdrawal\nAgent Name: "+strAgentName+"\nAmount: KES "+strAmount+"\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.AGENT_WITHDRAWAL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);

                    }else{
                        String strResponse = "Harambee Pesa Agent Cash Withdrawal\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.AGENT_WITHDRAWAL_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_CONFIRMATION.name());

                    switch (strConfirmation){
                        case "YES":{

                            String strResponse;

                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.agentCashWithdrawal(theUSSDRequest);

                            assert transactionReturnVal != null;
                            if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your Harambee Pesa Agent Cash Withdrawal request has been received successfully. Please wait shortly as it's being processed.";
                            }else {
                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your Harambee Pesa Agent Cash Withdrawal request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your Harambee Pesa Agent Cash Withdrawal request CANNOT be completed.\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Dear member, you have insufficient balance to complete this request. Please check your account balance and try again.\n";
                                        break;
                                    }
                                    case INVALID_MOBILE_NUMBER: {
                                        strResponse = "Dear member, you have entered an invalid phone number. Please check the phone number and try again.\n";
                                        break;
                                    }

                                    default: {
                                        strResponse = "Sorry, your Harambee Pesa Agent Cash Withdrawal request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.AGENT_WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO":{
                            String strResponse = "Dear member, your Harambee Pesa Agent Cash Withdrawal request NOT confirmed. Cash Withdrawal request NOT COMPLETED.\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.AGENT_WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AMOUNT.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strAgentNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.AGENT_WITHDRAWAL_AGENT_NO.name());
                            String strAgentName = USSDAPI.validateAgentNumber(strAgentNumber);

                            String strResponse =  "Confirm Harambee Pesa Agent Cash Withdrawal\n{Select a valid menu}\nAgent Name: "+strAgentName+"\nAmount: KES "+strAmount+"\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.AGENT_WITHDRAWAL_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                default:{
                    System.err.println("theAppMenus.displayMenu_Withdrawal() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Cash Withdrawal\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.AGENT_WITHDRAWAL_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_AgentWithdrawal() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
