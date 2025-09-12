package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.lib.mbanking.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

import static ke.skyworld.mbanking.ussdapi.APIConstants.*;

public interface DepositMenus {
    default USSDResponse displayMenu_Deposit(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        PesaParam pesaParam = PESAAPI.getPesaParam(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.PESA, ke.skyworld.mbanking.pesaapi.APIConstants.PESA_PARAM_TYPE.MPESA_C2B);
        String strSender = pesaParam.getSenderIdentifier();
        String strHeader = "Payments and Deposit\n";
        
        try {
            switch (theParam) {
                //Home->Deposit->Category->Account / Account Number->Amount->Confirmation
                case "MENU": {
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                    strHeader += "Select Deposit Option";
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "MY_ACCOUNT", "1: MY Own Account");
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "OTHER_ACCOUNT", "2: OTHER Member's Account");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_OPTION, "NO", theArrayListUSSDSelectOption);
                    break;
                }
                case "OPTION": {
                    String strDepositOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_OPTION.name());

                    switch (strDepositOption) {
                        case "MY_ACCOUNT": {
                            strHeader += "Select Deposit Account Type";
                            theUSSDResponse = displayMenu_DepositCategories(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_CATEGORY);
                            break;
                        }
                        case "OTHER_ACCOUNT": {
                            strHeader += "Enter member's account number";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        default: {
                            strHeader += "{Select a valid menu}";
                            theUSSDResponse = displayMenu_Deposit(theUSSDRequest, "MENU");
                            break;
                        }
                    }
                    break;
                }
                case "CATEGORY": {
                    String strDepositCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_CATEGORY.name());

                    if (!strDepositCategory.equals("")) {
                        switch (strDepositCategory) {
                            case "FOSA": {
                                strHeader = "FOSA Account Deposit via M-PESA\nSelect Account";
                                theUSSDResponse = getBankAccounts(theUSSDRequest, "LOAN", strHeader);
                                break;
                            }
                            case "BOSA": {
                                strHeader = "BOSA Account Deposit via M-PESA\nSelect Account";
                                theUSSDResponse = getBankAccounts(theUSSDRequest, "LOAN", strHeader);
                                break;
                            }
                            case "PAY_LOAN": {
                                theUSSDResponse = theAppMenus.displayMenu_LoanRepayment(theUSSDRequest, "MENU");
                                break;
                            }
                            default: {
                                strHeader += "{Select a valid menu}";
                                theUSSDResponse = displayMenu_Deposit(theUSSDRequest, "OPTION");
                                break;
                            }
                        }
                    } else {
                        strHeader += "Select deposit option\n{Select a valid menu}";
                        theUSSDResponse = displayMenu_DepositCategories(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_CATEGORY);
                    }
                    break;
                }
                case "OTHER_ACCOUNT": {
                    strHeader += "Enter member's account number";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());

                    if (!Objects.equals(strAccount, "")) {
                        String strToOption = "Account No";
                        LinkedHashMap<String, String> account = theUSSDAPI.getMemberAccountDetails(theUSSDRequest, strToOption, strAccount);

                        if(account!=null) {
                            strHeader += "Enter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            strHeader += "{Invalid account number}\nEnter member's account number:\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        strHeader += "{Select a valid menu}";
                        theUSSDResponse = getBankAccounts(theUSSDRequest, theParam, strHeader);
                    }
                    break;
                }
                case "AMOUNT": {
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                    if (strAmount.matches("^[1-9][0-9]*$")) {
                        String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strDepositOption = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_OPTION.name());

                        String strMemberName = "";

                        if(strDepositOption.equals("OTHER_ACCOUNT")){
                            LinkedHashMap<String, String> account = theUSSDAPI.getMemberAccountDetails(theUSSDRequest, "Account No", strAccount);

                            if(account!=null) {
                                String theAccountName = account.values().toArray()[0].toString();
                                strMemberName = "\nName: "+theAccountName;
                            }
                        }

                        String strResponse = "Confirm " + strHeader + "\nPaybill no.: " + strSender + "\nAccount: " + strAccount + strMemberName + "\n" + "Amount: KES " + strAmount + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        String strDepositMinimum = theUSSDAPI.getParam(USSD_PARAM_TYPE.DEPOSIT).getMinimum();
                        String strDepositMaximum = theUSSDAPI.getParam(USSD_PARAM_TYPE.DEPOSIT).getMaximum();

                        double dblDepositMinimum = Double.parseDouble(strDepositMinimum);
                        double dblDepositMaximum = Double.parseDouble(strDepositMaximum);

                        double dblAmountEntered = Double.parseDouble(strAmount.replaceAll(",", ""));

                        if (dblAmountEntered < dblDepositMinimum) {
                            strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strDepositMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        if (dblAmountEntered > dblDepositMaximum) {
                            strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strDepositMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        strHeader += "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_CONFIRMATION.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse;

                            String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());

                            if (theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()) {
                                strResponse = "You will be prompted by M-PESA for payment\nPaybill no: " + strSender + "\n" + "A/C: " + strAccount + "\n" + "Amount: KES " + strAmount + "\n";

                                String strOriginatorID = Long.toString(theUSSDRequest.getUSSDSessionID());
                                String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                double lnAmount = Utils.stringToDouble(strAmount);
                                String strReceiverDetails = strReceiver;

                                String strSessionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
                                String strTraceID = theUSSDRequest.getUSSDTraceID();

                                String strReference = strReceiver;

                                Thread worker = new Thread(() -> {
                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(strOriginatorID, strReceiver, strReceiverDetails, strAccount, "KES", lnAmount, "ACCOUNT_DEPOSIT", strReference, "USSD", "MBANKING", strTraceID, strSessionID);
                                });
                                worker.start();
                            } else {
                                strResponse = "Use the details below to pay via M-PESA\nPaybill no: " + strSender + "\n" + "A/C: " + strAccount + "\n" + "Amount: KES " + strAmount + "\n";
                            }

                            //End USSD.
                            theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");

                            /*Cont USSD
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_END, "NO",theArrayListUSSDSelectOption);
                              */
                            break;
                        }
                        case "NO": {
                            String strResponse = "Dear member, your " + strHeader + " request NOT confirmed. " + strHeader + " request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_ACCOUNT.name());
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_AMOUNT.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");

                            String strResponse = "Confirm " + strHeader + "\n{Select a valid menu}\nPaybill no.: " + strSender + "\nAccount: " + strAccount + "\n" + "Amount: KES " + strAmount + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_AccountsDeposit() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    strHeader += "\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DEPOSIT_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_AccountsDeposit() ERROR : " + e.getMessage()+" PARAM : "+theParam);
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getBankAccounts(USSDRequest theUSSDRequest, String theParam, String strHeader) {
        USSDResponse theUSSDResponse = null;

        try {
            String strAccountTypes = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DEPOSIT_CATEGORY.name());
            switch (strAccountTypes) {
                case "FOSA": {
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, AccountType.FOSA, AppConstants.USSDDataType.DEPOSIT_ACCOUNT, AppConstants.USSDDataType.DEPOSIT_END);
                    break;
                }
                case "BOSA": {
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, AccountType.BOSA, AppConstants.USSDDataType.DEPOSIT_ACCOUNT, AppConstants.USSDDataType.DEPOSIT_END);
                    break;
                }
                case "PAY_LOAN": {
                    theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, AccountType.LOAN, AppConstants.USSDDataType.DEPOSIT_ACCOUNT, AppConstants.USSDDataType.DEPOSIT_END);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.getBankAccounts() ERROR : " + e.getMessage()+" PARAM : "+theParam);
        } finally {

        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_DepositCategories(USSDRequest theUSSDRequest, String strHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try{
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FOSA", "1: FOSA Deposit via M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "BOSA", "2: BOSA Deposit via M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "PAY_LOAN", "3: Pay Loan");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO",theArrayListUSSDSelectOption);
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_DepositOptions() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
