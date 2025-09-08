package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;

public interface BalanceEnquiryMenus {

    default USSDResponse displayMenu_BalanceEnquiry(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try{
            String  strHeader = "Balance Enquiry";
            APIConstants.AccountType accountType = APIConstants.AccountType.ALL;

            switch (theParam){
                case "MENU": {
                    strHeader = "Balance Enquiry";
                    theUSSDResponse = GeneralMenus.displayMenu_AccountTypes(theUSSDRequest, theParam, strHeader+"\nSelect a category", AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE);
                    break;
                }
                default: {

                    String strAccountType = null;

                    AppConstants.USSDDataType ussdDataType = getBalanceEnquiryCallerMenu(theUSSDRequest.getUSSDData().toString());

                    switch (ussdDataType){
                        case MY_ACCOUNT_BALANCE_ACCOUNT_TYPE:{
                            strAccountType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
                            break;
                        }
                        case LOAN_MENU:{
                            strAccountType = APIConstants.AccountType.LOAN.getValue();
                            break;
                        }
                    }

                    if (strAccountType != null) {
                        switch (strAccountType){
                            case "FOSA":{
                                strHeader = "FOSA Account Balance Enquiry";
                                accountType = APIConstants.AccountType.FOSA;
                                break;
                            }
                            case "BOSA":{
                                strHeader = "BOSA Account Balance Enquiry";
                                accountType = APIConstants.AccountType.BOSA;
                                break;
                            }
                            case "ALL":{
                                strHeader = "All Account Balance Enquiry";
                                accountType = APIConstants.AccountType.BOSA;
                                break;
                            }
                            case "LOAN":{
                                strHeader = "Loans Status Enquiry";
                                accountType = APIConstants.AccountType.LOAN;
                                break;
                            }
                        }
                    }
                    theUSSDResponse =  displayMenu_BalanceEnquiryMenus(theUSSDRequest, theParam , accountType, strHeader);
                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_BalanceEnquiry() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_BalanceEnquiryMenus(USSDRequest theUSSDRequest, String theParam, APIConstants.AccountType theAccountType, String theHeader) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try{
            String strHeader = theHeader;

            switch (theParam) {
                case "ACCOUNT_TYPE": {
                    String strAccountType = theAccountType.getValue();
                    if(!strAccountType.equals("")){
                        switch (theAccountType){
                            case FOSA:{
                                String strResponse  = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT);
                                break;
                            }
                            case  BOSA:{
                                String strResponse  = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT);
                                break;
                            }
                            case LOAN:{
                                String strResponse  = strHeader + "\nSelect Loan";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, "");
                                break;
                            }
                        }
                        break;
                    }else{
                        strHeader = strHeader+"\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_AccountTypes(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE);
                    }
                    break;
                }
                case "ACCOUNT_CATEGORY": {
                    String strAccountCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_CATEGORY.name());
                    if(!strAccountCategory.equals("")){
                        switch (theAccountType){
                            case FOSA:{
                                String strResponse  = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT);
                                break;
                            }
                            case  BOSA:{
                                String strResponse  = strHeader + "\nSelect account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT);
                                break;
                            }
                            case LOAN:{
                                String strResponse  = strHeader + "\nSelect Loan";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, "");
                                break;
                            }
                        }
                        break;
                    }else{
                        strHeader = strHeader+"\n{Select a valid menu}";
                        if(theAccountType == APIConstants.AccountType.LOAN){
                            theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_CATEGORY);
                        } else {
                            theUSSDResponse = GeneralMenus.displayMenu_AccountCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_CATEGORY);
                        }
                    }
                    break;
                }
                case "ACCOUNT": {
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT.name());

                    if(!strAccount.equals("")){
                        String strResponse = strHeader+"\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse  = strHeader + "\n{Select a valid menu}";
                        switch (theAccountType){
                            case FOSA:{
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.FOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT);
                                break;
                            }
                            case  BOSA:{
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT);
                                break;
                            }
                            case LOAN:{
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strResponse, APIConstants.AccountType.BOSA, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT, strAccount);
                                break;
                            }
                        }
                    }

                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN.name());
                    if(strLoginPIN.equals(strPIN)){

                        String strResponse;
                        String strAccountNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT.name());

                        if(theAccountType.equals(APIConstants.AccountType.LOAN)){
                            HashMap<String, String> hmLoan = Utils.toHashMap(strAccountNumber);
                            strAccountNumber = hmLoan.get("LOAN_ID");
                        }

                        strResponse = theUSSDAPI.accountBalanceEnquiry(theUSSDRequest, strAccountNumber);
                        System.out.println("accountBalanceEnquiry: "+strResponse);

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO",theArrayListUSSDSelectOption);
                    }else{
                        String strResponse = strHeader+"\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_BalanceEnquiryMenus() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader+"\n{Sorry, an error has occurred while processing your request}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_BalanceEnquiryMenus() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default AppConstants.USSDDataType getBalanceEnquiryCallerMenu(String theUSSDData){

        AppConstants.USSDDataType ussdDataType = AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE;

        try{

            int intMY_ACCOUNT_BALANCE_ACCOUNT_TYPE = theUSSDData.lastIndexOf(AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE.name());
            int intLOAN_MENU = theUSSDData.lastIndexOf(AppConstants.USSDDataType.LOAN_MENU.name());

            ussdDataType = (intMY_ACCOUNT_BALANCE_ACCOUNT_TYPE > intLOAN_MENU) ? AppConstants.USSDDataType.MY_ACCOUNT_BALANCE_ACCOUNT_TYPE : AppConstants.USSDDataType.LOAN_MENU;
        }catch(Exception e){
            System.err.println("theAppMenus.getBalanceEnquiryCallerMenu() ERROR : " + e.getMessage());
        }
        finally{

        }
        return ussdDataType;
    }
}
