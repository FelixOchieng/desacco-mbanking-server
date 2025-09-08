package ke.skyworld.mbanking.ussdapplication;



import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;

import java.util.*;

public interface FundsTransferMenus {
    default USSDResponse displayMenu_FundTransfer(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.name())) {
                String strHeader = "Funds Transfer\nSelect Funds Transfer option\n";
                theUSSDResponse = getFundsTransferOptions(theUSSDRequest, strHeader);

            }else {
                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){
                    case FUNDS_TRANSFER_MENU:{
                        String strFundsTransferOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_MENU.name());
                        if (strFundsTransferOption.equalsIgnoreCase("FUNDS_TRANSFER_INTERNAL")) {
                            theUSSDResponse = displayMenu_FundTransferInternal(theUSSDRequest, theParam);
                        }else if (strFundsTransferOption.equalsIgnoreCase("FUNDS_TRANSFER_EXTERNAL")) {
                            theUSSDResponse = displayMenu_FundTransferExternal(theUSSDRequest, theParam);
                        }else {
                            String strHeader = "Funds Transfer\n{Select a valid Funds Transfer option}\n";
                            theUSSDResponse = getFundsTransferOptions(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    default:{
                        System.err.println("theAppMenus.displayMenu_FundTransfer() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Funds Transfer\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }

            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransfer() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getFundsTransferOptions(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FUNDS_TRANSFER_INTERNAL", "1: " + AppConstants.strSACCOName + " account");
            if(CBSAPI.checkService("Bank Transfer")){
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "FUNDS_TRANSFER_EXTERNAL", "2: Other Bank Account");
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_MENU, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getFundsTransferOptions() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getMODBillTypes(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "DOD_CAU", "1: DOD Cau");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "EMBAKASI", "2: Embakasi");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_TYPE, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getMODBillTypes() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    //Internal Fund Transfer Menus
    default USSDResponse displayMenu_FundTransferInternal(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.FUNDS_TRANSFER_MENU.name())) {
                String strHeader = "Funds Transfer\nSelect source account";
                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT);
            }else {
                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){

                    case FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT:{
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                        if(strFromAccount != ""){
                            String strHeader = "Funds Transfer\nSelect Funds Transfer option\n";
                            theUSSDResponse = getFundTransferInternalOptionMenu(theUSSDRequest, strHeader);
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid source account}";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_OPTION:{

                        String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());
                        if (strOption.equalsIgnoreCase("MY_ACCOUNT")) {
                            String strHeader = "Funds Transfer\nSelect destination account";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT);

                        } else if (strOption.equalsIgnoreCase("OTHER_ACCOUNT")) {
                            String strHeader = "Funds Transfer\nSelect transfer option\n";
                            theUSSDResponse = getFundTransferInternalToOptionMenu(theUSSDRequest, strHeader);
                        }else {
                            String strHeader = "Funds Transfer\n{Select a valid menu}\nSelect Funds Transfer option\n";
                            theUSSDResponse = getFundTransferInternalOptionMenu(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_TO_OPTION:{
                        String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                        if(strToOption.equalsIgnoreCase("Mobile No")){
                            String strResponse = "Funds Transfer\nEnter Mobile No. of the destination acct.\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else if(strToOption.equalsIgnoreCase("ID Number")){
                            String strResponse = "Funds Transfer\nEnter ID Number / Service Number of the destination acct.\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else if(strToOption.equalsIgnoreCase("Account No")){
                            String strResponse = "Funds Transfer\nEnter Account Number of the destination acct.\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else{
                            String strHeader = "Funds Transfer\n{Select a valid menu}\nSelect transfer option\n";
                            theUSSDResponse = getFundTransferInternalToOptionMenu(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT:{
                        String strToAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                        if (strToAccount.equals("")) {
                            String strHeader = "Funds Transfer\n{Invalid destination account}\nSelect Funds Transfer option\n";
                            theUSSDResponse = getFundTransferInternalOptionMenu(theUSSDRequest, strHeader);

                        }else if (strToAccount.equalsIgnoreCase(strFromAccount)) {

                            String strHeader = "Funds Transfer\n{Source and destination account MUST be different}\nSelect Funds Transfer option\n";
                            theUSSDResponse = getFundTransferInternalOptionMenu(theUSSDRequest, strHeader);

                        } else {
                            String strResponse = "Funds Transfer\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_AMOUNT:{
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());
                        if (strAmount.matches("^[1-9][0-9]*$")) {
                            String strResponse = "Funds Transfer\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");

                            String strFundsTransferMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMinimum();
                            String strFundsTransferMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMaximum();

                            double dblFundsTransferMinimum = Double.parseDouble(strFundsTransferMinimum);
                            double dblFundsTransferMaximum = Double.parseDouble(strFundsTransferMaximum);

                            double dblAmountEntered = Double.parseDouble(strAmount);

                            if (dblAmountEntered < dblFundsTransferMinimum) {
                                strResponse = "Funds Transfer\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMinimum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }

                            if (dblAmountEntered > dblFundsTransferMaximum) {
                                strResponse = "Funds Transfer\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMaximum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        } else {
                            String strResponse = "Funds Transfer\n{Please enter a valid amount}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_PIN:{
                        String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN.name());
                        if (strLoginPIN.equals(strPIN)) {

                            String strFromAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                            String strToAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());

                            String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());

                            String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.ACCOUNT_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Funds Transfer\nFrom A/C: " + strFromAccountNo + "\n" + "To A/C: " + strToAccount + "\n" + "Amount: KES " + strAmount +strCharge+ "\n";

                            if(strOption.equalsIgnoreCase("OTHER_ACCOUNT")){
                                String strOptionTo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());

                                String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                                String strAccountID = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                                LinkedHashMap<String, String> account = theUSSDAPI.getMemberAccountDetails(theUSSDRequest, strToOption, strAccountID);

                                if(account!=null) {
                                    String theAccount = account.keySet().toArray()[0].toString();
                                    String theAccountName = account.values().toArray()[0].toString();
                                    strResponse = "Confirm Funds Transfer\nFrom A/C: " + strFromAccountNo + "\nTo " + strOptionTo + ": " + strToAccount + "\nA/C: " + theAccount + "\nName: " + theAccountName + "\n" + "Amount: KES " + strAmount+strCharge + "\n";

                                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                                    if (theAccount.equalsIgnoreCase(strFromAccountNo)) {
                                        if (strToOption.equalsIgnoreCase("Mobile No")) {
                                            strResponse = "Funds Transfer\n{Enter a different Mobile No. of the destination acct.}\n";
                                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                        } else if (strToOption.equalsIgnoreCase("ID Number")) {
                                            strResponse = "Funds Transfer\n{Enter a different ID Number / Service Number of the destination acct.}\n";
                                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                        } else if (strToOption.equalsIgnoreCase("Account No")) {
                                            strResponse = "Funds Transfer\n{Enter a different Account Number of the destination acct.}\n";
                                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                        }
                                    }
                                } else {
                                    if(strToOption.equalsIgnoreCase("Mobile No")){
                                        strResponse = "Funds Transfer\n{Enter a valid Mobile No. of the destination acct.}\n";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                    } else if (strToOption.equalsIgnoreCase("ID Number")) {
                                        strResponse = "Funds Transfer\n{Enter a valid ID Number / Service Number of the destination acct.}\n";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                    } else if (strToOption.equalsIgnoreCase("Account No")) {
                                        strResponse = "Funds Transfer\n{Enter a valid Account Number of the destination acct.}\n";
                                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                    } else {
                                        strResponse = "Funds Transfer\n{Select a valid menu}\nSelect transfer option\n";
                                        theUSSDResponse = getFundTransferInternalToOptionMenu(theUSSDRequest, strResponse);
                                    }
                                }
                            } else {
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            }

                        } else {
                            String strResponse = "Funds Transfer\n{Please enter correct PIN}\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_INTERNAL_CONFIRMATION:{
                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION.name());
                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";

                            /*Thread worker = new Thread(() -> {
                                APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.fundsTransfer(theUSSDRequest);
                                System.out.println("fundsTransfer: "+transactionReturnVal.getValue());
                            });
                            worker.start();*/

                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.fundsTransfer(theUSSDRequest);


                            if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";
                            }else {
                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN:
                                    case INVALID_ACCOUNT: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Sorry the source acount has insufficient balance for this transaction. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    default: {
                                        strResponse = "Sorry, your Funds Transfer request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO",theArrayListUSSDSelectOption);

                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your Funds Transfer request NOT confirmed. Funds Transfer request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strFromAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_FROM_ACCOUNT.name());
                            String strToAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_AMOUNT.name());

                            String strOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION.name());

                            String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.ACCOUNT_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Funds Transfer\n{Select a valid menu}\nFrom A/C: " + strFromAccountNo + "\n" + "To A/C: " + strToAccount + "\n" + "Amount: KES " + strAmount+strCharge + "\n";

                            if(strOption.equalsIgnoreCase("OTHER_ACCOUNT")){
                                String strOptionTo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                                String strToOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION.name());
                                String strAccountID = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_ACCOUNT.name());
                                LinkedHashMap<String, String> account = theUSSDAPI.getMemberAccountDetails(theUSSDRequest, strToOption, strAccountID);

                                String theAccount = account.keySet().toArray()[0].toString();
                                String theAccountName = account.values().toArray()[0].toString();
                                strResponse = "Confirm Funds Transfer\n{Select a valid menu}\nFrom A/C: " + strFromAccountNo + "\nTo " + strOptionTo + ": " + strToAccount + "\nA/C: " + theAccount + "\nName: " + theAccountName + "\n" + "Amount: KES " + strAmount+strCharge + "\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        }
                        break;
                    }
                    default:{
                        System.err.println("theAppMenus.displayMenu_FundTransferInternal() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Funds Transfer\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }

            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransferInternal() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getFundTransferInternalToOptionMenu(USSDRequest theUSSDRequest, String strHeader) {

        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "Mobile No", "1: Mobile No");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "ID Number", "2: ID Number / Service Number");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "Account No", "3: Account Number");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_TO_OPTION, "NO",theArrayListUSSDSelectOption);

        }catch(Exception e){
            System.err.println("theAppMenus.getFundTransferInternalOptionMenu() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }

        return theUSSDResponse;
    }

    default USSDResponse getFundTransferInternalOptionMenu(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_ACCOUNT", "1: My account");
            if(CBSAPI.checkService("Transfer to Other Account")){
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_ACCOUNT", "2: Other member's account");
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_INTERNAL_OPTION, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getFundTransferInternalOptionMenu() ERROR : " + e.getMessage());
        }
            finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    //External Fund Transfer Menus

    default USSDResponse displayMenu_FundTransferExternalBanks(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("BANK_SHORT_CODE");

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            for(APIUtils.ServiceProviderAccount serviceProviderAccount : llSPAAccounts){
                int intOptionMenu = llSPAAccounts.indexOf(serviceProviderAccount);
                intOptionMenu = intOptionMenu+1;
                String strOptionMenu = String.valueOf(intOptionMenu);
                String strProviderAccountIdentifier = serviceProviderAccount.getProviderAccountIdentifier();
                String strProviderAccountLongTag = serviceProviderAccount.getProviderAccountLongTag();
                String strOptionDisplayText = strOptionMenu+": "+strProviderAccountLongTag;
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strProviderAccountIdentifier, strOptionDisplayText);
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK, "NO",theArrayListUSSDSelectOption);
        }catch(Exception e){
            System.err.println("theAppMenus.getFundsTransferOptions() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }


    default USSDResponse displayMenu_FundTransferExternal(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.FUNDS_TRANSFER_MENU.name())) {
                String strHeader = "Funds Transfer\nSelect Bank to transfer funds";
                theUSSDResponse = displayMenu_FundTransferExternalBanks(theUSSDRequest, strHeader);

            }else {

                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){
                    case FUNDS_TRANSFER_EXTERNAL_BANK:{
                        String strToBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                        if(strToBank != ""){
                            theUSSDResponse = displayMenu_FundTransferExternal_Maintain_Accounts(theUSSDRequest, theParam);
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid Bank to transfer funds}";
                            theUSSDResponse = displayMenu_FundTransferExternalBanks(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO:{

                        String strToBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());

                        String theAccountType = strToBank;
                        APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(strToBank);
                        String strToBankName = serviceProviderAccount.getProviderAccountLongTag();
                        String strSPProviderAccountCode = serviceProviderAccount.getProviderAccountCode();
                        String strAccountNaming = "Account No.";

                        String strMenuOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

                        String strAction = "";
                        if(!strMenuOption.isEmpty()){
                            HashMap<String, String> hmMenuOption = Utils.toHashMap(strMenuOption);
                            strAction = hmMenuOption.get("ACTION");
                        }

                        switch (strAction) {
                            case "CHOICE": {
                                String strHeader = "Funds Transfer\nSelect funds source account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT);
                                break;
                            }case "ADD": {

                                String strResponse = "Add " + strToBankName + " " + strAccountNaming + "\nEnter " + strAccountNaming + ":";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                break;
                            }
                            case "REMOVE": {
                                String strHeader2 = "Remove " + strToBankName + " " + strAccountNaming +"\nSelect " + strAccountNaming + " to Remove:";

                                String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                                theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader2, USSDConstants.Condition.NO);
                                break;
                            }
                            default:{

                                System.err.println("theAppMenus.displayMenu_FundTransferExternal() UNKNOWN PARAM ERROR : strAction = " + strAction);

                                String strHeader = "Funds Transfer\n{Select a VALID MENU}:\n";
                                String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                                theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                                break;
                            }
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME:{
                        String strToBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                        APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(strToBank);
                        String strToBankName = serviceProviderAccount.getProviderAccountLongTag();
                        String strSPProviderAccountCode = serviceProviderAccount.getProviderAccountCode();
                        String strToBankAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME.name());
                        if(strToBankAccountName.length() >= 3){ //Three or more Characters
                            String strHeader = "Funds Transfer\nSelect funds source account";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT);
                        }else{
                            String strHeader = "Funds Transfer\n{Enter a valid "+strToBankName+" account name to receive funds}:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT:{
                        String strFromAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());
                        if(strFromAccount != ""){
                            String strResponse = "Funds Transfer\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }else{
                            String strHeader = "Funds Transfer\n{Select a valid funds source account}";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT);
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_AMOUNT:{
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());
                        if (strAmount.matches("^[1-9][0-9]*$")) {
                            String strResponse = "Funds Transfer\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");

                            String strFundsTransferMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMinimum();
                            String strFundsTransferMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.EXTERNAL_FUNDS_TRANSFER).getMaximum();

                            double dblFundsTransferMinimum = Double.parseDouble(strFundsTransferMinimum);
                            double dblFundsTransferMaximum = Double.parseDouble(strFundsTransferMaximum);

                            double dblAmountEntered = Double.parseDouble(strAmount);

                            if (dblAmountEntered < dblFundsTransferMinimum) {
                                strResponse = "Funds Transfer\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMinimum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }

                            if (dblAmountEntered > dblFundsTransferMaximum) {
                                strResponse = "Funds Transfer\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMaximum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        } else {
                            String strResponse = "Funds Transfer\n{Please enter a valid amount}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_PIN:{
                        String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN.name());
                        if (strLoginPIN.equals(strPIN)) {

                            String strBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                            APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(strBank);
                            String strToBankName = serviceProviderAccount.getProviderAccountLongTag();

                            //String strToBankAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

                            String strToBankAccountNoHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());

                            HashMap<String, String> hmAccount = Utils.toHashMap(strToBankAccountNoHashMap);
                            String strAccountID = hmAccount.get("ACCOUNT_ID");
                            String strAccountName = hmAccount.get("ACCOUNT_NAME");
                            String strAccountIdentifier = hmAccount.get("ACCOUNT_IDENTIFIER");

                            //String strToBankAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME.name());

                            String strFromAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

                            String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.BANK_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strResponse = "Confirm Funds Transfer to "+ strToBankName +"\nA/C: " + strAccountIdentifier + " - " + strAccountName + "\nFrom A/C: " + strFromAccountNo + "\nAmount: KES " + strAmount+strCharge + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strResponse = "Funds Transfer\n{Please enter correct PIN}\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case FUNDS_TRANSFER_EXTERNAL_CONFIRMATION: {
                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION.name());
                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";

                            //APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.bankTransferViaB2B(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.bankTransferViaB2B(theUSSDRequest, PESAConstants.PESAType.PESA_OUT);

                            if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                                strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";
                            }else {
                                switch (transactionReturnVal) {
                                    case INCORRECT_PIN: {
                                        strResponse = "Sorry the PIN provided is incorrect. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case BLOCKED: {
                                        strResponse = "Dear member, your account has been blocked. Your Funds Transfer request CANNOT be completed.\n";
                                        break;
                                    }
                                    case INSUFFICIENT_BAL: {
                                        strResponse = "Dear member, you have insufficient balance to complete this request. Please deposit to your account and try again.\n";
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
                                        strResponse = "Sorry, your Funds Transfer request CANNOT be completed at the moment. Please try again later.\n";
                                        break;
                                    }
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_END, "NO",theArrayListUSSDSelectOption);

                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your Funds Transfer request NOT confirmed. Funds Transfer request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
                            String strToBankAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO.name());
                            String strToBankAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NAME.name());

                            APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(strBank);
                            String strToBankName = serviceProviderAccount.getProviderAccountLongTag();

                            String strFromAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_FROM_ACCOUNT.name());

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_AMOUNT.name());

                            String strCharge = USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.BANK_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strResponse = "Confirm Funds Transfer to "+ strToBankName +"\n{Select a valid menu}\nA/C: " + strToBankAccountNo + " - " + strToBankAccountName + "\nFrom A/C: " + strFromAccountNo + "\nAmount: KES " + strAmount +strCharge+ "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                        }
                        break;
                    }
                    default:{
                        System.err.println("theAppMenus.displayMenu_FundTransfer() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Funds Transfer\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransfer() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse displayMenu_FundTransferExternal_Maintain_Accounts(USSDRequest theUSSDRequest, String theParam){
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{

            AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());
            String strToBank = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_BANK.name());
            String theAccountType = strToBank;
            APIUtils.ServiceProviderAccount serviceProviderAccount = getProviderAccountCode(strToBank);
            String strToBankName = serviceProviderAccount.getProviderAccountLongTag();
            String strSPProviderAccountCode = serviceProviderAccount.getProviderAccountCode();
            String strAccountNaming = "Account No.";

            switch (ussdDataType) {
                case FUNDS_TRANSFER_EXTERNAL_BANK: {
                    if(!Objects.equals(strToBank, "")){
                        String strHeader = "Funds Transfer\nSelect "+strToBankName+" "+ strAccountNaming+" to receive funds:\n";
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    }else{
                        String strHeader = "Funds Transfer\n{Select a valid Bank to transfer funds}";
                        theUSSDResponse = displayMenu_FundTransferExternalBanks(theUSSDRequest, strHeader);
                    }

                    break;
                }
                case FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT: {
                    String theAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT.name());

                    if( theAccountNo.matches("^\\d{6,24}$")  ) { //6 - 24 Digits

                        String strResponse = "Add " + strToBankName + " " + strAccountNaming + "\nEnter the NAME of the account HOLDER\n(" + strToBankName + " "  +strAccountNaming+ " " + theAccountNo + "):";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME, USSDConstants.USSDInputType.STRING, "NO");
                    }else{
                        String strResponse = "Add " + strToBankName + " " + strAccountNaming + "\n{Enter a VALID "+strAccountNaming+"}:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;

                }
                case FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME: {
                    //ADD Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_ACCOUNT.name());
                    String strAccountName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_NAME.name());

                    try{
                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");

                        String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                        SPManager spManager = new SPManager(strIntegritySecret);
                        spManager.createUserSavedAccount(SPManagerConstants.UserIdentifierType.MSISDN,strMobileNo,strSPProviderAccountCode, SPManagerConstants.AccountIdentifierType.ACCOUNT_NO,strAccountNo, strAccountName);


                    }catch (Exception e){
                        System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() ERROR : " + e.getMessage());
                    }

                    String strHeader = "Funds Transfer\nSelect "+strToBankName+" "+ strAccountNaming+" to receive funds:\n";
                    String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);
                    break;
                }
                case FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE: {
                    //REMOVE Account
                    String strMobileNo = String.valueOf( theUSSDRequest.getUSSDMobileNo() );
                    String strAccountHashMap = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE.name());

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
                            System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() ERROR : " + e.getMessage());
                        }

                        String strHeader = "Funds Transfer\nSelect "+strToBankName+" "+ strAccountNaming+" to receive funds:\n";
                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    }else{
                        String strHeader = "Remove " + strToBankName + " " + strAccountNaming +"\n{Select a VALID MENU}:";

                        String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                        theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT_REMOVE, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.NO);
                    }

                    break;
                }
                default:{
                    String strHeader = "Funds Transfer\n{Select a VALID "+strToBankName+" "+ strAccountNaming+" to receive funds}:\n";
                    System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() UNKNOWN PARAM ERROR : strUSSDDataType = " + ussdDataType.name());

                    String strSPProviderAccount = theAccountType.replaceAll(" ","_");
                    theUSSDResponse = GeneralMenus.getAccountMaintenanceMenus(theUSSDRequest, AppConstants.USSDDataType.FUNDS_TRANSFER_EXTERNAL_TO_BANK_ACCOUNT_NO, theAccountType, strAccountNaming, strSPProviderAccountCode, strHeader, USSDConstants.Condition.YES);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_FundTransferExternal_Maintain_Accounts() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_PayMODBills(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        PesaParam pesaParam = PESAAPI.getPesaParam(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.PESA, ke.skyworld.mbanking.pesaapi.APIConstants.PESA_PARAM_TYPE.MPESA_C2B);
        String strSender = pesaParam.getSenderIdentifier();

        try {

            String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() - 1];
            //String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

            if(strLastKey.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.name())) {
                String strHeader = "Pay MOD Bills\nSelect unit\n";
                theUSSDResponse = getMODBillTypes(theUSSDRequest, strHeader);

            }else {
                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType){
                    case PAY_MOD_BILLS_TYPE:{
                        String strMODBILLType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_TYPE.name());
                        System.out.println("MOD Bill Type: "+strMODBILLType);
                        if (strMODBILLType != null && !strMODBILLType.isEmpty()) {
                            String strHeader = "Pay MOD Bills\nSelect branch\n";
                            theUSSDResponse = GeneralMenus.displayMenu_PayMODBillBranches(theUSSDRequest, theParam, strHeader, strMODBILLType, AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH);
                        } else {
                            String strHeader = "Pay MOD Bills\n{Select a valid bill type}\n";
                            theUSSDResponse = getMODBillTypes(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case PAY_MOD_BILLS_BRANCH:{
                        String strMODBILLType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_TYPE.name());
                        String strMODBILLTypeBranch = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH.name());
                        if (strMODBILLTypeBranch != null && !strMODBILLTypeBranch.isEmpty()) {
                            //String strHeader = "Pay MOD Bills\nSelect source account\n";
                            //theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT);

                            String strHeader = "Pay MOD Bills\nSelect payment option\n";
                            theUSSDResponse = getPayMODBillsRepaymentOption(theUSSDRequest, strHeader);

                        } else {
                            String strHeader = "Pay MOD Bills\n{Select a valid branch}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_PayMODBillBranches(theUSSDRequest, theParam, strHeader, strMODBILLType, AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH);
                        }
                        break;
                    }

                    case PAY_MOD_BILLS_PAYMENT_OPTION:{
                        String strMODBILLPaymentOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_PAYMENT_OPTION.name());
                        if (strMODBILLPaymentOption != null && !strMODBILLPaymentOption.isEmpty()) {

                            if(strMODBILLPaymentOption.equals("Savings Account")) {
                                String strHeader = "Pay MOD Bills\nSelect source account\n";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT);
                            } else {
                                String strResponse = "Pay MOD Bills\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }

                        } else {
                            String strHeader = "Pay MOD Bills\n{Select a valid payment option}\n";
                            theUSSDResponse = getPayMODBillsRepaymentOption(theUSSDRequest, strHeader);
                        }
                        break;
                    }

                    case PAY_MOD_BILLS_FROM_ACCOUNT:{
                        String strMODBILLTypeSourceAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT.name());
                        if (strMODBILLTypeSourceAccount != null && !strMODBILLTypeSourceAccount.isEmpty()) {
                            String strResponse = "Pay MOD Bills\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            String strHeader = "Pay MOD Bills\n{Select a valid source account}\n";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT);
                        }
                        break;
                    }

                    case PAY_MOD_BILLS_AMOUNT:{
                        String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                        String strMODBILLPaymentOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_PAYMENT_OPTION.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT.name());
                        if (strAmount.matches("^[1-9][0-9]*$")) {

                            String[] strMODBillBranchCodeAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH.name()).split("\\|");
                            String strMODBillBranchCode = strMODBillBranchCodeAndName[0];
                            String strMODBillBranchName = strMODBillBranchCodeAndName[1];

                            String strSourceAccountNumber = "";
                            String strSourceAccountName = "";

                            if(strMODBILLPaymentOption.equals("M-PESA")) {
                                strSourceAccountName = "M-PESA ("+strMobileNumber+")";
                            } else {
                                String[] strSourceAccountNumberAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT.name()).split("\\|");
                                strSourceAccountNumber = strSourceAccountNumberAndName[0];
                                strSourceAccountName = "A/C: "+strSourceAccountNumberAndName[1]+" ("+strSourceAccountNumber+")";
                            }

                            String strCharge = "\nTransaction Charge: KES 0.00";//USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.ACCOUNT_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Pay " + strMODBillBranchName + "\n" + "From " + strSourceAccountName + "\n" + "Amount: KES " + strAmount+ "\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                            String strFundsTransferMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMinimum();
                            String strFundsTransferMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.INTERNAL_FUNDS_TRANSFER).getMaximum();
                            String strDepositMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.DEPOSIT).getMinimum();
                            String strDepositMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.DEPOSIT).getMaximum();

                            double dblFundsTransferMinimum = Double.parseDouble(strFundsTransferMinimum);
                            double dblFundsTransferMaximum = Double.parseDouble(strFundsTransferMaximum);
                            double dblDepositMinimum = Double.parseDouble(strDepositMinimum);
                            double dblDepositMaximum = Double.parseDouble(strDepositMaximum);

                            double dblAmountEntered = Double.parseDouble(strAmount);

                            if(strMODBILLPaymentOption.equals("Savings Account")) {
                                if (dblAmountEntered < dblFundsTransferMinimum) {
                                    strResponse = "Pay MOD Bills\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMinimum, "#,###.##") + "}\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }

                                if (dblAmountEntered > dblFundsTransferMaximum) {
                                    strResponse = "Pay MOD Bills\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strFundsTransferMaximum, "#,###.##") + "}\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }
                            } else {
                                if (dblAmountEntered < dblDepositMinimum) {
                                    strResponse = "Pay MOD Bills\n{MINIMUM amount allowed is KES " + Utils.formatDouble(dblDepositMinimum, "#,###.##") + "}\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }

                                if (dblAmountEntered > dblDepositMaximum) {
                                    strResponse = "Pay MOD Bills\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(dblDepositMaximum, "#,###.##") + "}\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }
                            }
                        } else {
                            String strResponse = "Pay MOD Bills\n{Please enter a valid amount}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }

                    /*case PAY_MOD_BILLS_PIN:{
                        String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                        String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                        String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_PIN.name());
                        String strMODBILLPaymentOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_PAYMENT_OPTION.name());

                        if (strLoginPIN.equals(strPIN)) {

                            String[] strMODBillBranchCodeAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH.name()).split("\\|");
                            String strMODBillBranchCode = strMODBillBranchCodeAndName[0];
                            String strMODBillBranchName = strMODBillBranchCodeAndName[1];

                            String[] strSourceAccountNumberAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT.name()).split("\\|");
                            String strSourceAccountNumber = strSourceAccountNumberAndName[0];
                            String strSourceAccountName = "A/C: "+strSourceAccountNumberAndName[1]+" ("+strSourceAccountNumber+")";

                            if(strMODBILLPaymentOption.equals("M-PESA")) {
                                strSourceAccountName = "M-PESA ("+strMobileNumber+")";
                            }

                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT.name());

                            String strCharge = "\nTransaction Charge: KES 5.00";//USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.ACCOUNT_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Pay " + strMODBillBranchName + "\n" + "From " + strSourceAccountName + "\n" + "Amount: KES " + strAmount +strCharge+ "\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        } else {
                            String strResponse = "Pay MOD Bills\n{Please enter correct PIN}\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.PAY_MOD_BILLS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }*/

                    case PAY_MOD_BILLS_CONFIRMATION:{
                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_CONFIRMATION.name());
                        String strMODBILLPaymentOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_PAYMENT_OPTION.name());
                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_AMOUNT.name());

                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String[] strMODBillBranchCodeAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH.name()).split("\\|");
                            String strMODBillBranchCode = strMODBillBranchCodeAndName[0];
                            String strMODBillBranchName = strMODBillBranchCodeAndName[1];
                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strResponse = "Dear member, your Funds Transfer request has been received successfully. Please wait shortly as it's being processed.";

                            /*Thread worker = new Thread(() -> {
                                APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.fundsTransfer(theUSSDRequest);
                                System.out.println("fundsTransfer: "+transactionReturnVal.getValue());
                            });
                            worker.start();*/

                            if(strMODBILLPaymentOption.equals("M-PESA")) {

                                if (theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()) {
                                    strResponse = "You will be prompted by " + strMODBILLPaymentOption + " for payment\nPaybill no: " + strSender + "\n" + "Paying: " + strMODBillBranchName + "\n" + "Amount: KES " + strAmount + "\n";

                                    String strOriginatorID = Long.toString(theUSSDRequest.getUSSDSessionID());
                                    String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                    String strReceiverDetails = strReceiver;
                                    String strAccount = strMODBillBranchCode;
                                    Double lnAmount = Utils.stringToDouble(strAmount);
                                    String strReference = strReceiver;

                                    String strSessionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
                                    String strTraceID = theUSSDRequest.getUSSDTraceID();

                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(strOriginatorID, strReceiver, strReceiverDetails, strAccount, "KES", lnAmount, "MOD_BILL_PAYMENT", strReference, "USSD", "MBANKING", strTraceID, strSessionID);
                                } else {
                                    strResponse = "Use the details below to pay via " + strMODBILLPaymentOption + "\nPaybill no: " + strSender + "\n" + "Paying: " + strMODBillBranchName + "\n" + "Amount: KES " + strAmount + "\n";
                                }

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");

                            } else {

                                APIConstants.TransactionReturnVal transactionReturnVal = APIConstants.TransactionReturnVal.SUCCESS; //theUSSDAPI.fundsTransfer(theUSSDRequest);


                                if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                                    strResponse = "Dear member, your payment request to pay "+strMODBillBranchName+" has been received successfully. Please wait shortly as it's being processed.";
                                }else {
                                    switch (transactionReturnVal) {
                                        case INCORRECT_PIN:
                                        case INVALID_ACCOUNT: {
                                            strResponse = "Sorry the PIN provided is incorrect. Your MOD Bill Payment request CANNOT be completed.\n";
                                            break;
                                        }
                                        case INSUFFICIENT_BAL: {
                                            strResponse = "Sorry the source acount has insufficient balance for this transaction. Your MOD Bill Payment request CANNOT be completed.\n";
                                            break;
                                        }
                                        case BLOCKED: {
                                            strResponse = "Dear member, your account has been blocked. Your MOD Bill Payment request CANNOT be completed.\n";
                                            break;
                                        }
                                        default: {
                                            strResponse = "Sorry, your MOD Bill Payment request CANNOT be completed at the moment. Please try again later.\n";
                                            break;
                                        }
                                    }
                                }

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_END, "NO",theArrayListUSSDSelectOption);
                            }
                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your MOD Bill Payment request NOT confirmed. MOD Bill Payment request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                            String[] strMODBillBranchCodeAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_BRANCH.name()).split("\\|");
                            String strMODBillBranchCode = strMODBillBranchCodeAndName[0];
                            String strMODBillBranchName = strMODBillBranchCodeAndName[1];

                            String[] strSourceAccountNumberAndName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.PAY_MOD_BILLS_FROM_ACCOUNT.name()).split("\\|");
                            String strSourceAccountNumber = strSourceAccountNumberAndName[0];
                            String strSourceAccountName = "A/C: "+strSourceAccountNumberAndName[1]+" ("+strSourceAccountNumber+")";

                            if(strMODBILLPaymentOption.equals("M-PESA")) {
                                strSourceAccountName = "M-PESA ("+strMobileNumber+")";
                            }


                            String strCharge = "\nTransaction Charge: KES 5.00";//USSDAPI.fnGetServiceChargeAmount(APIConstants.MobileBankingTransactionType.ACCOUNT_TRANSFER, strAmount);

                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "Confirm Pay " + strMODBillBranchName + "\n" + "From " + strSourceAccountName + "\n" + "Amount: KES " + strAmount + "\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        }
                        break;
                    }

                    default:{
                        System.err.println("theAppMenus.displayMenu_PayMODBills() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Pay MOD Bills\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }

            }
        }
        catch(Exception e){
            e.printStackTrace();
            System.err.println("theAppMenus.displayMenu_PayMODBills() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getPayMODBillsRepaymentOption(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "M-PESA", "1: M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "Savings Account", "2: FOSA Savings Account");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.PAY_MOD_BILLS_PAYMENT_OPTION, "NO", theArrayListUSSDSelectOption);
            return theUSSDResponse;
        } catch (Exception e) {
            System.err.println("theAppMenus.getPayMODBillsRepaymentOption() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    static APIUtils.ServiceProviderAccount getProviderAccountCode(String theSPProviderAccount){
        APIUtils.ServiceProviderAccount rVal = null;
        try {
            LinkedList<APIUtils.ServiceProviderAccount> llSPAAccounts = APIUtils.getSPAccounts("BANK_SHORT_CODE");
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
}
