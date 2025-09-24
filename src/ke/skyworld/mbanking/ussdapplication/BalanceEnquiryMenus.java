package ke.skyworld.mbanking.ussdapplication;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;

public interface BalanceEnquiryMenus {

    default USSDResponse displayMenu_BalanceEnquiry(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            String strHeader = "Balance Enquiry";
            APIConstants.AccountType accountType = APIConstants.AccountType.ALL;

            switch (theParam) {
                case "MENU": {

                    FlexicoreHashMap getServiceStatusDetails = CBSAPI.getServiceStatusDetails(AppConstants.MobileBankingChannel.USSD, AppConstants.MobileBankingServices.BALANCE_ENQUIRY);
                    String strServiceStatus = getServiceStatusDetails.getStringValue("status");

                    if (!strServiceStatus.equalsIgnoreCase("ACTIVE")) {
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "Balance Enquiry\n" + getServiceStatusDetails.getStringValue("display_message"));
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO", theArrayListUSSDSelectOption);
                        return theUSSDResponse;

                    } else if (CBSAPI.isMandateInactive(theUSSDRequest.getUSSDMobileNo(), AppConstants.MobileMandates.BALANCE_ENQUIRY)) {

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "Balance Enquiry\n" + AppConstants.strServiceUnavailable);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO", theArrayListUSSDSelectOption);
                        return theUSSDResponse;
                    }


                    strHeader = "Balance Enquiry";
                    theUSSDResponse = GeneralMenus.displayMenu_AccountTypes(theUSSDRequest, theParam, strHeader + "\nSelect a category", AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE);
                    break;
                }
                default: {

                    String strAccountType = null;

                    AppConstants.USSDDataType ussdDataType = getBalanceEnquiryCallerMenu(theUSSDRequest.getUSSDData().toString());

                    switch (ussdDataType) {
                        case MY_ACCOUNT_BALANCE_ACCOUNT_TYPE: {
                            strAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
                            break;
                        }
                        case LOAN_MENU: {
                            strAccountType = APIConstants.AccountType.LOAN.getValue();
                            break;
                        }
                    }

                    if (strAccountType != null) {
                        switch (strAccountType) {
                            case "FOSA": {
                                strHeader = "FOSA Account Balance Enquiry";
                                accountType = APIConstants.AccountType.FOSA;
                                break;
                            }
                            case "BOSA": {
                                strHeader = "BOSA Account Balance Enquiry";
                                accountType = APIConstants.AccountType.BOSA;
                                break;
                            }
                            case "ALL": {
                                strHeader = "All Account Balance Enquiry";
                                accountType = APIConstants.AccountType.BOSA;
                                break;
                            }
                            case "LOAN": {
                                strHeader = "Loans Status Enquiry";
                                accountType = APIConstants.AccountType.LOAN;
                                break;
                            }
                        }
                    }
                    theUSSDResponse = displayMenu_BalanceEnquiryMenus(theUSSDRequest, theParam, accountType, strHeader);
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_BalanceEnquiry() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_BalanceEnquiryMenus(USSDRequest theUSSDRequest, String theParam, APIConstants.AccountType theAccountType, String theHeader) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strHeader = theHeader;

            switch (theParam) {
                case "ACCOUNT_TYPE": {
                    String strAccountType = theAccountType.getValue();
                    if (!strAccountType.equals("")) {
                        switch (theAccountType) {
                            case FOSA: {
                                String strResponse = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                            case BOSA: {
                                String strResponse = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                            case LOAN: {
                                String strResponse = strHeader + "\nSelect Loan";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.LOAN, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                        }
                        break;
                    } else {
                        strHeader = strHeader + "\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_AccountTypes(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE);
                    }
                    break;
                }
                case "ACCOUNT_CATEGORY": {
                    String strAccountCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_CATEGORY.name());
                    if (!strAccountCategory.equals("")) {
                        switch (theAccountType) {
                            case FOSA: {
                                String strResponse = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                            case BOSA: {
                                String strResponse = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                            case LOAN: {
                                String strResponse = strHeader + "\nSelect Loan";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                        }
                        break;
                    } else {
                        strHeader = strHeader + "\n{Select a valid menu}";
                        if (theAccountType == APIConstants.AccountType.LOAN) {
                            theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_CATEGORY);
                        } else {
                            theUSSDResponse = GeneralMenus.displayMenu_AccountCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_CATEGORY);
                        }
                    }
                    break;
                }

                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT.name());

                    if (!strAccount.equals("")) {
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strHeader + "\n{Select a valid menu}";
                        switch (theAccountType) {
                            case FOSA: {
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                            case BOSA: {
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                            case LOAN: {
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END);
                                break;
                            }
                        }
                    }

                    break;
                }

                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {

                        String strAccountNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT.name());
                        HashMap<String, String> hmAccount = Utils.toHashMap(strAccountNumber);
                        strAccountNumber = hmAccount.get("ac_no");

                        // if (theAccountType.equals(APIConstants.AccountType.LOAN)) {
                        //     HashMap<String, String> hmLoan = Utils.toHashMap(strAccountNumber);
                        //     strAccountNumber = hmLoan.get("ac_no");
                        // }
                        //
                        // if(theAccountType.equals(APIConstants.AccountType.FOSA) || theAccountType.equals(APIConstants.AccountType.BOSA)){
                        //     HashMap<String, String> hmAccount = Utils.toHashMap(strAccountNumber);
                        //     strAccountNumber = hmAccount.get("ac_no");
                        // }

                        String strResponse = "Dear member, your Balance Enquiry request for Account " + strAccountNumber + " has been received successfully. Please wait shortly as it's being processed.\n";

                        USSDAPI finalTheUSSDAPI = theUSSDAPI;

                        if (theAccountType.equals(APIConstants.AccountType.LOAN)) {
                            String finalStrAccountNumber = strAccountNumber;
                            Thread worker = new Thread(() -> {
                                TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiryWrapper = finalTheUSSDAPI.loanBalanceEnquiry(theUSSDRequest, finalStrAccountNumber);
                                if (accountBalanceEnquiryWrapper.hasErrors()) {
                                    FlexicoreHashMap accountBalanceEnquiryMap = accountBalanceEnquiryWrapper.getSingleRecord();
                                    String strErrorMessage = accountBalanceEnquiryMap.getValue("cbs_api_return_val").toString()+"\n";
                                    strErrorMessage += accountBalanceEnquiryMap.getStringValue("display_message");
                                    System.err.println("BalanceEnquiryMenus.displayMenu_BalanceEnquiry() - Response " + strErrorMessage);
                                }
                            });
                            worker.start();
                        }

                        if(theAccountType.equals(APIConstants.AccountType.FOSA) || theAccountType.equals(APIConstants.AccountType.BOSA)){
                            String finalStrAccountNumber = strAccountNumber;
                            Thread worker = new Thread(() -> {
                                TransactionWrapper<FlexicoreHashMap> accountBalanceEnquiryWrapper = finalTheUSSDAPI.accountBalanceEnquiry(theUSSDRequest, finalStrAccountNumber);
                                if (accountBalanceEnquiryWrapper.hasErrors()) {
                                    FlexicoreHashMap accountBalanceEnquiryMap = accountBalanceEnquiryWrapper.getSingleRecord();
                                    String strErrorMessage = accountBalanceEnquiryMap.getValue("cbs_api_return_val").toString()+"\n";
                                    strErrorMessage += accountBalanceEnquiryMap.getStringValue("display_message");
                                    System.err.println("BalanceEnquiryMenus.displayMenu_BalanceEnquiry() - Response " + strErrorMessage);
                                }
                            });
                            worker.start();
                        }


                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO", theArrayListUSSDSelectOption);

                    } else {
                        String strResponse = strHeader + "\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_BalanceEnquiryMenus() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader + "\n{Sorry, an error has occurred while processing your request}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_BalanceEnquiryMenus() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default AppConstants.USSDDataType getBalanceEnquiryCallerMenu(String theUSSDData) {

        AppConstants.USSDDataType ussdDataType = AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE;

        try {

            int intMY_ACCOUNT_BALANCE_ACCOUNT_TYPE = theUSSDData.lastIndexOf(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
            int intLOAN_MENU = theUSSDData.lastIndexOf(AppConstants.USSDDataType.LOAN_MENU.name());

            ussdDataType = (intMY_ACCOUNT_BALANCE_ACCOUNT_TYPE > intLOAN_MENU) ? AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE : AppConstants.USSDDataType.LOAN_MENU;
        } catch (Exception e) {
            System.err.println("theAppMenus.getBalanceEnquiryCallerMenu() ERROR : " + e.getMessage());
        } finally {

        }
        return ussdDataType;
    }
}
