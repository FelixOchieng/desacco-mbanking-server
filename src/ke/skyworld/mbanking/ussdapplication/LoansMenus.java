package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.pesaapi.PESAAPI;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.lib.mbanking.utils.Utils;
import org.w3c.dom.NodeList;

import java.util.*;

public interface LoansMenus {
    default USSDResponse displayMenu_Loan(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            if (strUSSDDataType.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.getValue())) {
                String strHeader = "Loans";
                theUSSDResponse = getLoansMenus(theUSSDRequest, strHeader);
            } else { //LOAN_MENU

                String strLOAN_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_MENU.name());

                switch (strLOAN_MENU) {
                    case "CHECK_QUALIFICATION": {
                        theUSSDResponse = theAppMenus.displayMenu_CheckLoanQualification(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_APPLICATION": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanApplication(theUSSDRequest, theParam, "");
                        break;
                    }
                    case "LOAN_REPAYMENT": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanRepayment(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_BALANCE": {
                        theParam = "ACCOUNT_TYPE"; //OVERRIDE and start at ACCOUNT_TYPE
                        theUSSDResponse = theAppMenus.displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_MINI_STATEMENT": {
                        theParam = "ACCOUNT_TYPE"; //OVERRIDE and start at ACCOUNT_TYPE
                        theUSSDResponse = theAppMenus.displayMenu_MiniStatement(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_GUARANTORS": {
                        theUSSDResponse = theAppMenus.displayMenu_LoanGuarantors(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOANS_GUARANTEED": {
                        theUSSDResponse = theAppMenus.displayMenu_LoansGuaranteed(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOAN_ACTION_GUARANTORSHIP": {
                        theUSSDResponse = theAppMenus.displayMenu_LoansActionGuarantorship(theUSSDRequest, theParam);
                        break;
                    }
                    case "LOANS_TERMS_AND_CONDITIONS": {
                        theUSSDResponse = theAppMenus.displayMenu_TermsAndConditionsPrivacyMenus(theUSSDRequest, theParam);
                        break;
                    }

                    default: {
                        String strHeader = "Loans\n{Select a valid menu}";
                        theUSSDResponse = getLoansMenus(theUSSDRequest, strHeader);

                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getLoansMenus(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            String strMemberLoansSetup = USSDAPI.getMemberLoansSetup(theUSSDRequest).trim();

            boolean blMemberHasAccessToLoansMenus = MBankingXMLFactory.getXPathValueFromXMLString("/LoansSetup/CanAccessLoans", strMemberLoansSetup).equals("TRUE");
            boolean blMemberHasLoansPendingGuarantorship = MBankingXMLFactory.getXPathValueFromXMLString("/LoansSetup/HasPendingGuarantorship", strMemberLoansSetup).equals("TRUE");
            String strAccessMessage = MBankingXMLFactory.getXPathValueFromXMLString("/LoansSetup/AccessMessage", strMemberLoansSetup);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (blMemberHasAccessToLoansMenus) {
                LinkedList<APIUtils.MenuItem> llMenus = new LinkedList<>();
                /*if (blMemberHasLoansPendingGuarantorship) {
                    llMenus.add(new APIUtils.MenuItem("Accept / Reject Guarantorship", "LOAN_ACTION_GUARANTORSHIP"));
                }*/

                llMenus.add(new APIUtils.MenuItem("Check Loan Limit", "CHECK_QUALIFICATION"));
                llMenus.add(new APIUtils.MenuItem("Apply Loan", "LOAN_APPLICATION"));
                llMenus.add(new APIUtils.MenuItem("Pay Loan", "LOAN_REPAYMENT"));
                llMenus.add(new APIUtils.MenuItem("Loan Balance", "LOAN_BALANCE"));
                llMenus.add(new APIUtils.MenuItem("Loan Mini-Statement", "LOAN_MINI_STATEMENT"));

             /* llMenus.add(new APIUtils.MenuItem("Add / Remove Loan Guarantors", "LOAN_GUARANTORS"));
                llMenus.add(new APIUtils.MenuItem("Loans Guaranteed", "LOANS_GUARANTEED"));*/

                if (blMemberHasLoansPendingGuarantorship) {
                    llMenus.add(new APIUtils.MenuItem("T&Cs", "LOANS_TERMS_AND_CONDITIONS"));
                } else {
                    llMenus.add(new APIUtils.MenuItem("T&Cs", "LOANS_TERMS_AND_CONDITIONS"));
                }

                int i = 1;
                for (APIUtils.MenuItem miMenu : llMenus) {
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(i), miMenu.getValue(), i + ": " + miMenu.getTitle());
                    i++;
                }
            } else {
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "\n" + strAccessMessage + "\n");
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_MENU, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.getLoansMenus() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_CheckLoanQualification(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            switch (theParam) {
                case "MENU": {
                    String strHeader = "Check Loan Limit";
                    strHeader += "\nSelect an account";
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_QUALIFICATION_ACCOUNT);
                    break;
                }
                case "ACCOUNT": {
                    String strHeader = "Check Loan Limit";
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_ACCOUNT.name());
                    if (!Objects.equals(strAccount, "")) {
                        strHeader += "\nSelect loan type";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, "", strHeader, AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE, strAccount);
                    } else {
                        strHeader += "\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_QUALIFICATION_ACCOUNT);
                    }
                    break;
                }
                case "TYPE": {
                    String strLoanQualificationType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE.name());

                    if (!Objects.equals(strLoanQualificationType, "")) {

                        String strResponse = "Dear member, your Loan Qualification request has been received successfully. Please wait shortly as it's being processed.";

                        Thread worker = new Thread(() -> {
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.checkLoanQualification(theUSSDRequest);
                            System.out.println("checkLoanQualification: " + transactionReturnVal.getValue());
                        });
                        worker.start();

                        /*APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.checkLoanQualification(theUSSDRequest);

                        String strResponse ="";

                        if(transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)){
                            strResponse = "Dear member, your Loan Qualification request has been received successfully. Please wait shortly as it's being processed.";
                        }else {


                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your Loan Qualification request CANNOT be completed.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your Loan Qualification request CANNOT be completed.\n";
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your Loan Qualification request CANNOT be completed at the moment. Please try again later.\n";
                                    break;
                                }
                            }
                        }
                        */
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_QUALIFICATION_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strHeader = "Check Loan Limit\n{Select a valid menu}";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_QUALIFICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, "", strHeader, AppConstants.USSDDataType.LOAN_QUALIFICATION_TYPE, strLoanAccount);
                    }

                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_CheckLoanQualification() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Check Loan Limit\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_QUALIFICATION_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_CheckLoanQualification() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoanApplication(USSDRequest theUSSDRequest, String theParam, String theLoanType) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            switch (theParam) {
                case "MENU": {
                    String strHeader = "Loan Application";
                    strHeader += "\nSelect an account";
                    theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT);
                    break;
                }
                case "ACCOUNT": {
                    String strHeader = "Loan Application";
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                    if (!Objects.equals(strAccount, "")) {
                        strHeader += "\nSelect loan type";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, "", strHeader, AppConstants.USSDDataType.LOAN_APPLICATION_TYPE, strLoanAccount);
                    } else {
                        strHeader += "\n{Select a valid account}\n";
                        theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT);
                    }
                    break;
                }
                case "CATEGORY": {
                    String strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CATEGORY.name());

                    if (!strLoanApplicationCategory.equals("")) {
                        String strResponse = "Loan Application\nSelect Loan:";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, strLoanApplicationCategory, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_TYPE, strLoanAccount);
                    } else {
                        String strHeader = "Loan Application\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.LOAN_APPLICATION_CATEGORY);
                    }
                    break;
                }
                case "TYPE": {
                    String strLoanApplicationCategory = "M-Boosta";

                    String strLoanApplicationType = theLoanType;
                    if (strLoanApplicationType != null) {
                        if (!strLoanApplicationType.equals("")) {
                            String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                            LinkedList<String> loanTypes = theUSSDAPI.getLoanTypes(theUSSDRequest, "ALL", strLoanAccount);

                            if (loanTypes != null) {
                                for (String loanType : loanTypes) {
                                    String strCode = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", loanType);

                                    if (strCode.equals(strLoanApplicationType)) {
                                        APIUtils.setLoanTypeInMemory(strCode, loanType, theUSSDRequest);
                                        strLoanApplicationType = strCode;
                                        break;
                                    }
                                }
                            }
                        } else {
                            strLoanApplicationType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                            if (strLoanApplicationType == null) {
                                strLoanApplicationType = theLoanType;
                            }
                            strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CATEGORY.name());
                            if (strLoanApplicationCategory == null) {
                                strLoanApplicationCategory = "M-Boosta Loan ";
                            }
                        }
                    } else {
                        strLoanApplicationType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                        if (strLoanApplicationType == null) {
                            strLoanApplicationType = theLoanType;
                        }
                        strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CATEGORY.name());
                        if (strLoanApplicationCategory == null) {
                            strLoanApplicationCategory = "M-Boosta Loan ";
                        }
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanApplicationType, theUSSDRequest);
                    String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);

                    if (!strLoanApplicationType.equals("")) {
                        String strResponse = strLoanName + " Application\nDo you accept the terms and conditions for this loan?";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACCEPT", "1: Accept T&Cs");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "REJECT", "2: Reject T&Cs");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "ACCESS", "3: Access Loan T&Cs");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_TNCS, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strHeader = "Loan Application\n{Select a valid menu}";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, strLoanApplicationCategory, strHeader, AppConstants.USSDDataType.LOAN_APPLICATION_TYPE, strLoanAccount);
                    }

                    break;
                }
                case "TNCS": {
                    String strLoanApplicationTNCs = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TNCS.name());

                    if (!strLoanApplicationTNCs.equals("")) {
                        if (strLoanApplicationTNCs.equals("ACCEPT")) {
                            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                            if (strLoanType == null) {
                                strLoanType = theLoanType;
                            }

                            String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                            String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                            String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                            String strInstallmentsType = MBankingXMLFactory.getXPathValueFromXMLString("/Product/InstallmentsType", strLoanTypeXML);
                            String strUserCanApply = MBankingXMLFactory.getXPathValueFromXMLString("/Product/UserCanApply", strLoanTypeXML);

                            APIUtils.LoanAmountLimits loanAmountLimits = APIUtils.getLoanInstallmentAmounts(null, strLoanTypeXML);

                            if (loanAmountLimits.getMaximum().equals("0") || strUserCanApply.equals("FALSE")) {
                                String strLoanMessage = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Message", strLoanTypeXML);
                                String strResponse = strLoanName + " Application\n" + strLoanMessage + ".\n";

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);
                            } else {
                                if (strInstallmentsType.equals("PRESET")) {
                                    String strResponse = strLoanName + " Application\nSelect duration (in months)";
                                    theUSSDResponse = GeneralMenus.displayMenu_LoanInstallments(theUSSDRequest, theParam, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS, strLoanTypeXML);
                                } else if (strInstallmentsType.equals("INPUT")) {
                                    String strResponse = strLoanName + " Application\nSelect duration (in months)";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS, USSDConstants.USSDInputType.STRING, "NO");
                                } else {
                                    String strMinimum = Utils.formatDouble(loanAmountLimits.getMinimum(), "#,###.##");
                                    String strMaximum = Utils.formatDouble(loanAmountLimits.getMaximum(), "#,###.##");

                                    String strResponse = strLoanName + " Application\nMinimum: KES " + strMinimum + "\nMaximum: KES " + strMaximum + "\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }
                            }
                        } else if (strLoanApplicationTNCs.equals("REJECT")) {
                            String strHeader = "Loan Application\nSorry, your loan application has been declined.\nPlease accept T&Cs to continue.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);
                        } else if (strLoanApplicationTNCs.equals("ACCESS")) {
                            String strHeader = "Loan Application\nYou access the loan terms and conditions on the Loans Menu of this USSD application.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CATEGORY.name());
                            if (strLoanApplicationCategory == null) {
                                strLoanApplicationCategory = "M-Boosta Loan ";
                            }
                            String strHeader = "Loan Application\n{Select a valid menu}";
                            String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                            theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, strLoanApplicationCategory, strHeader, AppConstants.USSDDataType.LOAN_APPLICATION_TYPE, strLoanAccount);
                        }
                    } else {
                        String strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CATEGORY.name());
                        if (strLoanApplicationCategory == null) {
                            strLoanApplicationCategory = "M-Boosta Loan ";
                        }
                        String strHeader = "Loan Application\n{Select a valid menu}";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, strLoanApplicationCategory, strHeader, AppConstants.USSDDataType.LOAN_APPLICATION_TNCS, strLoanAccount);
                    }
                    break;
                }
                case "INSTALLMENTS": {
                    String strLoanApplicationInstallments = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS.name());

                    if (strLoanApplicationInstallments != null) {
                        if (!strLoanApplicationInstallments.equals("")) {
                            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                            if (strLoanType == null) {
                                strLoanType = theLoanType;
                            }

                            String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                            String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                            String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                            String strLoanInterest = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                            String strUserCanApply = MBankingXMLFactory.getXPathValueFromXMLString("/Product/UserCanApply", strLoanTypeXML);

                            APIUtils.LoanAmountLimits loanAmountLimits = APIUtils.getLoanInstallmentAmounts(strLoanApplicationInstallments, strLoanTypeXML);

                            if (loanAmountLimits.getMaximum().equals("0") || strUserCanApply.equals("FALSE")) {
                                String strLoanMessage = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Message", strLoanTypeXML);
                                String strResponse = strLoanName + " Application\n" + strLoanMessage + ".\n";

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);
                            } else {
                                String strResponse = strLoanName + " Application\nInterest Rate: " + strLoanInterest + "\nMinimum: KES " + loanAmountLimits.getMinimum() + "\nMaximum: KES " + loanAmountLimits.getMaximum() + "\n\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        } else {
                            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                            if (strLoanType == null) {
                                strLoanType = theLoanType;
                            }

                            String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                            String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                            String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                            String strInstallmentsType = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresGuarantors", strLoanTypeXML);
                            String strLoanInterest = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                            String strUserCanApply = MBankingXMLFactory.getXPathValueFromXMLString("/Product/UserCanApply", strLoanTypeXML);

                            if (strInstallmentsType.equals("PRESET")) {
                                String strResponse = strLoanName + " Application\n{Select a valid menu}";
                                theUSSDResponse = GeneralMenus.displayMenu_LoanInstallments(theUSSDRequest, theParam, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS, strLoanTypeXML);
                            } else if (strInstallmentsType.equals("INPUT")) {
                                String strResponse = strLoanName + " Application\n{Select a valid duration (in months)}";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS, USSDConstants.USSDInputType.STRING, "NO");
                            } else {
                                if (strUserCanApply.equals("TRUE")) {
                                    APIUtils.LoanAmountLimits loanAmountLimits = APIUtils.getLoanInstallmentAmounts(strLoanApplicationInstallments, strLoanTypeXML);

                                    String strResponse = strLoanName + " Application\nInterest Rate: " + strLoanInterest + "\nMinimum: KES " + loanAmountLimits.getMinimum() + "\nMaximum: KES " + loanAmountLimits.getMaximum() + "\n\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }
                            }
                        }
                    } else {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                        if (strLoanType == null) {
                            strLoanType = theLoanType;
                        }
                        String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                        String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);

                        APIUtils.LoanAmountLimits loanAmountLimits = APIUtils.getLoanInstallmentAmounts(strLoanApplicationInstallments, strLoanTypeXML);

                        String strResponse = strLoanName + " Application\n{Select a valid duration (in months)}";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "AMOUNT": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    if (strLoanType == null) {
                        strLoanType = theLoanType;
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                    String strLoanRequiresPurpose = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresPurpose", strLoanTypeXML);
                    String strLoanBranch = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresBranch", strLoanTypeXML);
                    String strLoanRequiresPayslipPassword = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresPayslipPIN", strLoanTypeXML);

                    String strLoanApplicationInstallments = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS.name());

                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());
                    if (strAmount.matches("^[1-9][0-9]*$")) {
                        String strResponse;

                        if (strLoanRequiresPurpose.equals("TRUE")) {
                            strResponse = strLoanName + " Application\nSelect purpose for application";
                            theUSSDResponse = GeneralMenus.displayMenu_LoanPurposes(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PURPOSE);
                        } else {
                            if (strLoanBranch.equals("TRUE")) {
                                strResponse = strLoanName + " Application\nSelect your branch";
                                theUSSDResponse = GeneralMenus.displayMenu_LoanBranches(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_BRANCH);
                            } else {
                                if (strLoanRequiresPayslipPassword.equals("TRUE")) {
                                    strResponse = strLoanName + " Application\nEnter your GHRIS payslip password:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PAYSLIP_PASSWORD, USSDConstants.USSDInputType.STRING, "NO");
                                } else {
                                    strResponse = strLoanName + " Application\nEnter your PIN:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                                }
                            }
                        }

                        APIUtils.LoanAmountLimits loanAmountLimits = APIUtils.getLoanInstallmentAmounts(strLoanApplicationInstallments, strLoanTypeXML);
                        double dblLoanApplicationMinimum = Double.parseDouble(loanAmountLimits.getMinimum().replaceAll(",", ""));
                        double dblLoanApplicationMaximum = Double.parseDouble(loanAmountLimits.getMaximum().replaceAll(",", ""));

                        double dblAmountEntered = Double.parseDouble(strAmount);

                        if (dblAmountEntered < dblLoanApplicationMinimum) {
                            strResponse = strLoanName + " Application\n{MINIMUM amount allowed is KES " + Utils.formatDouble(dblLoanApplicationMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }

                        if (dblAmountEntered > dblLoanApplicationMaximum) {
                            strResponse = strLoanName + " Application\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(dblLoanApplicationMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = strLoanName + " Application\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "PURPOSE": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    if (strLoanType == null) {
                        strLoanType = theLoanType;
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                    String strLoanBranch = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresBranch", strLoanTypeXML);
                    String strLoanRequiresPayslipPassword = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresPayslipPIN", strLoanTypeXML);

                    String strPurpose = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PURPOSE.name());
                    if (!strPurpose.equals("")) {
                        if (strLoanBranch.equals("TRUE")) {
                            String strResponse = strLoanName + " Application\nSelect your branch";
                            theUSSDResponse = GeneralMenus.displayMenu_LoanBranches(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_BRANCH);
                        } else {
                            if (strLoanRequiresPayslipPassword.equals("TRUE")) {
                                String strResponse = strLoanName + " Application\nEnter your GHRIS payslip password:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PAYSLIP_PASSWORD, USSDConstants.USSDInputType.STRING, "NO");
                            } else {
                                String strResponse = strLoanName + " Application\nEnter your PIN:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        }
                    } else {
                        String strResponse = strLoanName + " Application\n{Select a valid purpose}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanPurposes(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PURPOSE);
                    }
                    break;
                }
                case "BRANCH": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    if (strLoanType == null) {
                        strLoanType = theLoanType;
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                    String strLoanRequiresPayslipPassword = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresPayslipPIN", strLoanTypeXML);

                    String strBranch = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_BRANCH.name());
                    if (!strBranch.equals("")) {
                        if (strLoanRequiresPayslipPassword.equals("TRUE")) {
                            String strResponse = strLoanName + " Application\nEnter your GHRIS payslip password:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PAYSLIP_PASSWORD, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            String strResponse = strLoanName + " Application\nEnter your PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = strLoanName + " Application\n{Select a valid branch}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanBranches(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_BRANCH);
                    }
                    break;
                }
                case "PAYSLIP_PASSWORD": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    if (strLoanType == null) {
                        strLoanType = theLoanType;
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);

                    String strBranch = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PAYSLIP_PASSWORD.name());
                    if (!strBranch.equals("")) {
                        String strResponse = strLoanName + " Application\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strLoanName + " Application\n{Enter a valid password}\nEnter your GHRIS payslip password:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PAYSLIP_PASSWORD, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    if (strLoanType == null) {
                        strLoanType = theLoanType;
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);

                    String strLoanInstallmentId = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS.name());
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PIN.name());

                    String strDuration = "";
                    String strInstallmentsType = MBankingXMLFactory.getXPathValueFromXMLString("/Product/InstallmentsType", strLoanTypeXML);

                    if (strInstallmentsType.equals("PRESET")) {
                        String strLoanInstallmentName = APIUtils.getLoanInstallmentLabel(strLoanInstallmentId, strLoanTypeXML);
                        strDuration = ("\nDuration: " + strLoanInstallmentName);
                    } else if (strInstallmentsType.equals("INPUT")) {
                        strDuration = strDuration + " Months";
                    }

                    if (strLoginPIN.equals(strPIN)) {

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        String strResponse = "Confirm " + strLoanName + " Application" + strDuration + "\nAmount: KES " + strAmount + "\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strResponse = strLoanName + " Application\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_APPLICATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "CONFIRMATION": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
                    if (strLoanType == null) {
                        strLoanType = theLoanType;
                    }

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanID = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", strLoanTypeXML);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);
                    String strLoanRequiresGuarantors = MBankingXMLFactory.getXPathValueFromXMLString("/Product/RequiresGuarantors", strLoanTypeXML);

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_CONFIRMATION.name());
                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());
                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PIN.name());

                    String strLoanPeriod = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS.name());
                    String strLoanPurpose = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PURPOSE.name());
                    String strLoanPassword = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_PAYSLIP_PASSWORD.name());
                    String strLoanBranch = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_BRANCH.name());

                    if (strConfirmation == null) {
                        strConfirmation = "";
                    }
                    if (strAmount == null) {
                        strAmount = "0";
                    }
                    if (strPIN == null) {
                        strPIN = "";
                    }
                    if (strLoanPeriod == null) {
                        strLoanPeriod = "0";
                    }
                    if (strLoanPurpose == null) {
                        strLoanPurpose = "";
                    }
                    if (strLoanPassword == null) {
                        strLoanPassword = "";
                    }
                    if (strLoanBranch == null) {
                        strLoanBranch = "";
                    }

                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        String strResponse = "Dear member, your " + strLoanName + " Application request has been received successfully. Please wait shortly as it's being processed.";

                        if (strLoanRequiresGuarantors.equals("TRUE")) {
                            strResponse = "Dear member, your " + strLoanName + " Application request has been received successfully.\nTo add a guarantor, please proceed to Home -> Loans -> Add / Remove Loan Guarantors.";
                        }

                        String finalStrAmount = strAmount;
                        String finalStrPIN = strPIN;
                        String finalStrLoanPeriod = strLoanPeriod;
                        String finalStrLoanPurpose = strLoanPurpose;
                        String finalStrLoanPassword = strLoanPassword;
                        String finalStrLoanBranch = strLoanBranch;

                        String finalStrLoanType = strLoanType;
                        Thread worker = new Thread(() -> {
                            APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanApplication(theUSSDRequest, false, strLoanID, finalStrAmount, finalStrPIN, finalStrLoanPeriod, finalStrLoanPurpose, finalStrLoanPassword, finalStrLoanBranch, strAccount);
                            System.out.println("loanApplication: " + transactionReturnVal.getValue());
                            APIUtils.removeLoanTypeFromMemory(finalStrLoanType, theUSSDRequest);
                        });
                        worker.start();

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);

                    } else if (strConfirmation.equalsIgnoreCase("NO")) {
                        String strResponse = "Dear member, your " + strLoanName + " Application request NOT confirmed. Loan Application request NOT COMPLETED.";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strLoanInstallmentId = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_INSTALLMENTS.name());

                        strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                        String strDuration = "";
                        String strInstallmentsType = MBankingXMLFactory.getXPathValueFromXMLString("/Product/InstallmentsType", strLoanTypeXML);

                        if (strInstallmentsType.equals("PRESET")) {
                            String strLoanInstallmentName = APIUtils.getLoanInstallmentLabel(strLoanInstallmentId, strLoanTypeXML);
                            strDuration = "\nDuration: " + strLoanInstallmentName;
                        } else if (strInstallmentsType.equals("INPUT")) {
                            strDuration = "\nDuration: " + strDuration + " Months";
                        }

                        strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_AMOUNT.name());
                        strAmount = Utils.formatDouble(strAmount, "#,###");
                        String strResponse = "Confirm " + strLoanName + " Application\n{Select a valid menu}" + strDuration + "\nAmount: KES " + strAmount + "\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    }

                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_LoanApplication() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Loan Application\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_APPLICATION_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanApplication() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoanRepayment(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        PesaParam pesaParam = PESAAPI.getPesaParam(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.PESA, ke.skyworld.mbanking.pesaapi.APIConstants.PESA_PARAM_TYPE.MPESA_C2B);
        String strSender = pesaParam.getSenderIdentifier();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();
            if (theParam.equalsIgnoreCase("MENU")) {
                String strHeader = "Pay Loan";
                theUSSDResponse = getLoanRepaymentOption(theUSSDRequest, strHeader);

            } else { //LOAN_REPAYMENT_MENU
                String strLOAN_REPAYMENT_OPTION = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_OPTION.name());

                AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromValue(theUSSDRequest.getUSSDDataType());

                switch (ussdDataType) {
                    case LOAN_REPAYMENT_OPTION: {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_OPTION.name());

                        if (!strLoanType.equals("")) {
                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                String strHeader = "Pay Loan\nSelect an account";
                                theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_REPAYMENT_ACCOUNT);
                            } else {
                                boolean blGroupBankingIsEnabled = theUSSDAPI.checkIfGroupBankingIsEnabled(theUSSDRequest);
                                if (blGroupBankingIsEnabled) {
                                    String strHeader = "Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\nSelect Loan Category";
                                    theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.LOAN_REPAYMENT_CATEGORY);
                                } else {
                                    String strHeader = "Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\nSelect Loan";
                                    theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN, "");
                                }
                            }
                        } else {
                            String strHeader = "Pay Loan\n{Select a valid menu}";
                            theUSSDResponse = getLoanRepaymentOption(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_ACCOUNT: {
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_ACCOUNT.name());

                        if (!strLoanAccount.equals("")) {
                            String strHeader = "Pay loan from savings account\nSelect Loan";
                            theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN, "");
                        } else {
                            String strHeader = "Pay Loa\n{Select a valid account}\nSelect an account";
                            theUSSDResponse = GeneralMenus.displayMenu_BankAccounts(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.WITHDRAWABLE, AppConstants.USSDDataType.LOAN_REPAYMENT_ACCOUNT);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_CATEGORY: {
                        String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_CATEGORY.name());

                        if (!strUserInput.equals("")) {
                            if (strUserInput.equals("GROUP")) {
                                String strHeader = "Pay Loan\nSelect Group";
                                theUSSDResponse = GeneralMenus.displayMenu_AccountGroups(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.LOAN, AppConstants.USSDDataType.LOAN_REPAYMENT_GROUP);
                            } else {
                                String strHeader = "Pay Loan\nSelect Loan";
                                theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.LOAN, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN, "");
                            }
                        } else {
                            String strHeader = "Pay Loan\n{Select a valid menu}";
                            theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.LOAN_REPAYMENT_CATEGORY);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_GROUP: {
                        String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_GROUP.name());

                        if (!strUserInput.equals("")) {
                            String strHeader = "Pay Loan \nSelect Loan";
                            theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.LOAN, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN, strUserInput);
                        } else {
                            String strHeader = "Pay Loan\n{Select a valid menu}";
                            theUSSDResponse = getLoanRepaymentOption(theUSSDRequest, strHeader);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_LOAN: {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        HashMap<String, String> hmLoan = Utils.toHashMap(strLoanType);
                        String strLoanName = hmLoan.get("LOAN_NAME");
                        String strLoanID = hmLoan.get("LOAN_ID");

                        String strLoan = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        if (!strLoan.equals("")) {
                            String strLoanBalance = "";
                            strLoanBalance = CBSAPI.getLoanBalance(strLoanID);
                            if (strLoanBalance == null) {
                                strLoanBalance = "";
                            }

                            if (!strLoanBalance.equals("")) {
                                strLoanBalance = strLoanBalance + "\n";
                            }

                            String strResponse = "Pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + "\n" + strLoanBalance + "Enter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            String strLoanGroup = theUSSDRequest.getUSSDData().getOrDefault(AppConstants.USSDDataType.LOAN_REPAYMENT_GROUP.name(), "");
                            String strHeader = "Pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + "\n{Select a valid option}\nSelect Loan";
                            theUSSDResponse = GeneralMenus.displayMenu_Loans(theUSSDRequest, theParam, strHeader, APIConstants.AccountType.ALL, AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN, strLoanGroup);
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_AMOUNT: {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        HashMap<String, String> hmLoan = Utils.toHashMap(strLoanType);
                        String strLoanName = hmLoan.get("LOAN_NAME");
                        String strLoanID = hmLoan.get("LOAN_ID");

                        String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());
                        if (strAmount.matches("^[1-9][0-9]*$")) {
                            String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_ACCOUNT.name());
                            String strLoan = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "";

                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                strResponse = "Confirm Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\nLoan: " + strLoanName + "\n" + "Amount: KES " + strAmount + "\n";
                                //strResponse = "Confirm Pay Loan via "+strLOAN_REPAYMENT_OPTION+"\nAccount: " + strAccountNo + "\n" + "Loan: " + strLoan + "\n" + "Amount: KES " + strAmount + "\n";
                            } else {
                                strResponse = "Confirm Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\nPaybill no.: " + strSender + "\nLoan: " + strLoanName + "\nAmount: KES " + strAmount + "\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);


                            String strPayLoanMinimum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.PAY_LOAN).getMinimum();
                            String strPayLoanMaximum = theUSSDAPI.getParam(APIConstants.USSD_PARAM_TYPE.PAY_LOAN).getMaximum();

                            double dblPayLoanMinimum = Double.parseDouble(strPayLoanMinimum);
                            double dblPayLoanMaximum = Double.parseDouble(strPayLoanMaximum);

                            strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());
                            double dblAmountEntered = Double.parseDouble(strAmount);

                            if (dblAmountEntered < dblPayLoanMinimum) {
                                strResponse = "Pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strPayLoanMinimum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }
                            if (dblAmountEntered > dblPayLoanMaximum) {
                                strResponse = "Pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strPayLoanMaximum, "#,###.##") + "}\nEnter amount:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        } else {
                            String strLoanBalance = String.valueOf(CBSAPI.getLoanBalance(strLoanID));
                            strLoanBalance = Utils.formatDouble(strLoanBalance, "#,###");

                            String strResponse = "Pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + "\n{Please enter a valid amount}\nLoan Balance: KES " + strLoanBalance + "\nEnter amount:";

                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        break;
                    }
                    case LOAN_REPAYMENT_CONFIRMATION: {
                        String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                        HashMap<String, String> hmLoan = Utils.toHashMap(strLoanType);
                        String strLoanName = hmLoan.get("LOAN_NAME");
                        String strLoanID = hmLoan.get("LOAN_ID");

                        String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_CONFIRMATION.name());
                        if (strConfirmation.equalsIgnoreCase("YES")) {

                            String strLoan = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());

                            String strResponse = "";

                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                strResponse = "Dear member, your request to Pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + " has been received successfully. Please wait shortly as it's being processed.";

                                Thread worker = new Thread(() -> {
                                    APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.loanRepayment(theUSSDRequest);
                                    System.out.println("loanRepayment: " + transactionReturnVal.getValue());
                                });
                                worker.start();

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);

                            } else {
                                if (theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()) {
                                    strResponse = "You will be prompted by " + strLOAN_REPAYMENT_OPTION + " for payment\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanName + "\n" + "Amount: KES " + strAmount + "\n";

                                    String strOriginatorID = Long.toString(theUSSDRequest.getUSSDSessionID());
                                    String strReceiver = Long.toString(theUSSDRequest.getUSSDMobileNo());
                                    String strReceiverDetails = strReceiver;
                                    String strAccount = "LOAN" + strLoanID;
                                    Double lnAmount = Utils.stringToDouble(strAmount);
                                    String strReference = strReceiver;

                                    String strSessionID = MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD, theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
                                    String strTraceID = theUSSDRequest.getUSSDTraceID();

                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(strOriginatorID, strReceiver, strReceiverDetails, strAccount, "KES", lnAmount, "ACCOUNT_DEPOSIT", strReference, "USSD", "MBANKING", strTraceID, strSessionID);
                                } else {
                                    strResponse = "Use the details below to pay via " + strLOAN_REPAYMENT_OPTION + "\nPaybill no: " + strSender + "\n" + "Loan: " + strLoanName + "\n" + "Amount: KES " + strAmount + "\n";
                                }

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                                //theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);
                            }

                        } else if (strConfirmation.equalsIgnoreCase("NO")) {
                            String strResponse = "Dear member, your request to pay " + strLoanName + " via " + strLOAN_REPAYMENT_OPTION + " was NOT confirmed. Pay Loan request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strAccountNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_ACCOUNT.name());
                            String strLoan = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_LOAN.name());
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_REPAYMENT_AMOUNT.name());
                            strAmount = Utils.formatDouble(strAmount, "#,###");
                            String strResponse = "";

                            if (strLOAN_REPAYMENT_OPTION.equalsIgnoreCase("Savings Account")) {
                                strResponse = "Confirm Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\n{Select a valid menu}\nLoan: " + strLoanName + "\n" + "Amount: KES " + strAmount + "\n";
                                //strResponse = "Confirm Pay Loan via "+strLOAN_REPAYMENT_OPTION+"\nAccount: " + strAccountNo + "\n" + "Loan: " + strLoan + "\n" + "Amount: KES " + strAmount + "\n";
                            } else {
                                strResponse = "Confirm Pay Loan via " + strLOAN_REPAYMENT_OPTION + "\n{Select a valid menu}\nPaybill no.: " + strSender + "\nLoan: " + strLoanName + "\nAmount: KES " + strAmount + "\n";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                        }

                        break;
                    }
                    default: {
                        System.err.println("theAppMenus.displayMenu_LoanRepayment() UNKNOWN PARAM ERROR : theParam = " + theParam);

                        String strResponse = "Pay Loan\n{Sorry, an error has occurred while processing your request}";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_END, "NO", theArrayListUSSDSelectOption);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanRepayment() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse getLoanRepaymentOption(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "M-PESA", "1: M-PESA");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "Savings Account", "2: FOSA Savings Account");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_REPAYMENT_OPTION, "NO", theArrayListUSSDSelectOption);
            return theUSSDResponse;
        } catch (Exception e) {
            System.err.println("theAppMenus.getLoanRepaymentOption() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;

    }

    default USSDResponse displayMenu_LoanGuarantors(USSDRequest theUSSDRequest, String theParam) {

        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLoan = "";
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            switch (theParam) {
                case "MENU": {
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();

                    String strHeader = "Loan Guarantors\nSelect a loan";
                    theUSSDResponse = GeneralMenus.displayMenu_LoansWithGuarantors(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE);

                    break;
                }
                case "TYPE": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());

                    if (!strLoanType.equals("")) {
                        String strHeader = "Loan Guarantors";
                        theUSSDResponse = getLoanDetailsForGuarantorship(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION, true);
                    } else {
                        String strHeader = "Loan Guarantors\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoansWithGuarantors(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE);
                    }
                    break;
                }
                case "OPTION": {
                    String loans = theUSSDAPI.getLoanAwaitingGuarantorship(theUSSDRequest);

                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Name", loans);
                    String strResponse;
                    String strAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());

                    switch (strAction) {
                        case "ADD": {
                            strResponse = strLoanName + "\nAdd Guarantor\n" + "Enter Guarantor Mobile No.\nPlease enter one guarantor mobile number at a time.";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        case "REMOVE": {
                            strResponse = strLoanName + "\nRemove Guarantor\n" + "Select a guarantor to remove";
                            theUSSDResponse = getLoanDetailsForGuarantorship(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTOR, false);
                            break;
                        }
                        case "DISCARD": {
                            strResponse = strLoanName + "\nDiscard Loan Application\n" + "Enter PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        default: {
                            String strHeader = "Loan Guarantors\n{Select a valid menu}";
                            theUSSDResponse = getLoanDetailsForGuarantorship(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION, true);
                            break;
                        }
                    }
                    break;
                }
                case "GUARANTOR": {
                    String strGuarantorAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());
                    String strGuarantor = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTOR.name());
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanName = hmLoanType.get("LOAN_NAME");

                    if (!strGuarantor.equals("")) {
                        String strResponse = strLoanName + "\n" + strGuarantorAction + " Guarantor\nEnter your PIN";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strLoanName + "\n" + strGuarantorAction + " Guarantor\n{Select a valid menu}";
                        theUSSDResponse = getLoanDetailsForGuarantorship(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTOR, false);
                    }
                    break;
                }
                case "MOBILE_NUMBER": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanName = hmLoanType.get("LOAN_NAME");
                    String strResponse = strLoanName + "\nAdd Guarantor\n";

                    String strMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER.name());
                    strMobileNo = APIUtils.sanitizePhoneNumber(strMobileNo);

                    if (!strMobileNo.equalsIgnoreCase("INVALID_MOBILE_NUMBER")) {
                        String strMemberAccount = USSDAPI.getMemberAccount(strMobileNo);

                        if (!strMemberAccount.equals("")) {
                            strResponse = strResponse + "\nEnter PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        } else {
                            strResponse = strResponse + "\n{Member with mobile number not found}\nEnter Guarantor Mobile No.\n";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        strResponse = strResponse + "\n{Enter a valid mobile number}\nEnter Guarantor Mobile No.\nPlease enter one guarantor mobile number at a time.";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "PIN": {
                    String strGuarantorAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());
                    String strLoanXML = theUSSDAPI.getLoanAwaitingGuarantorship(theUSSDRequest);

                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Name", strLoanXML);

                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        theUSSDResponse = addGuarantorConfirmation(theUSSDRequest, strLoanXML);
                    } else {
                        String strResponse = strLoanName + " " + strGuarantorAction + " Guarantor\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_GUARANTORS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_TYPE.name());
                    HashMap<String, String> hmLoanType = Utils.toHashMap(strLoanType);
                    String strLoanName = hmLoanType.get("LOAN_NAME");

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_CONFIRMATION.name());
                    String strGuarantorAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());

                    String strAdditionalText = "guarantor";

                    if (strGuarantorAction.equals("DISCARD")) {
                        strAdditionalText = "loan application";
                    }

                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        String strResponse = "";

                        String transactionReturnVal = theUSSDAPI.addRemoveLoanGuarantor(theUSSDRequest);
                        String transactionStatus = transactionReturnVal.split("::::")[0];
                        String transactionMessage = transactionReturnVal.split("::::")[1];


                        if (transactionStatus.equals("SUCCESS")) {
                            //strResponse = "Dear member, your request to "+strGuarantorAction+" "+strAdditionalText+" for "+strLoanName+" has been received successfully. Please wait shortly as it's being processed.";

                            String strHeader = "Loan Guarantors";
                            theUSSDResponse = getLoanDetailsForGuarantorship(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION, true);
                        } else {
                            strResponse = transactionMessage;
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);
                        }
                    } else if (strConfirmation.equalsIgnoreCase("NO")) {
                        String strResponse = "Dear member, your request to " + strGuarantorAction + " " + strAdditionalText + " for " + strLoanName + " NOT confirmed. Request NOT COMPLETED.";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strLoanXML = theUSSDAPI.getLoanAwaitingGuarantorship(theUSSDRequest);

                        String strResponse = "Confirm Guarantor Details for " + strLoanName + ":\n{Select a valid menu}\n";
                        theUSSDResponse = addGuarantorConfirmation(theUSSDRequest, strLoanXML);
                    }
                    break;
                }
                case "END": {
                    String strResponse = "Loan Guarantors\n{Select a valid menu}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }

                default: {
                    System.err.println("theAppMenus.displayMenu_LoanGuarantors() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Loan Guarantors\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanGuarantors() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse addGuarantorConfirmation(USSDRequest theUSSDRequest, String strLoan) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        String strResponse = "";
        String strGuarantorAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_OPTION.name());
        String strLoanGuarantor = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_GUARANTOR.name());
        String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Name", strLoan);
        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();

        try {
            switch (strGuarantorAction) {
                case "ADD": {
                    strResponse = "Add Guarantor\nConfirm Guarantor Details for " + strLoanName + "\n";

                    String strMobileNo = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_GUARANTORS_MOBILE_NUMBER.name());
                    String strMemberAccount = USSDAPI.getMemberAccount(strMobileNo);

                    String strName = MBankingXMLFactory.getXPathValueFromXMLString("/Account/AccountName", strMemberAccount);
                    String stMobileNumber = MBankingXMLFactory.getXPathValueFromXMLString("/Account/PhoneNo", strMemberAccount);

                    strResponse = strResponse + "\nName: " + strName;
                    strResponse = strResponse + "\nMobile: " + stMobileNumber;
                    break;
                }
                case "REMOVE": {
                    NodeList guarantors = APIUtils.getXMLNodeListFromPath("/Loan/Guarantors/Guarantor", strLoan.trim());
                    String strGuarantorName = "";
                    if (guarantors != null) {
                        for (int i = 1; i <= guarantors.getLength(); i++) {
                            strGuarantorName = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Guarantors/Guarantor[" + i + "]/Name", strLoan.trim());
                            String strGuarantorMemberNumber = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Guarantors/Guarantor[" + i + "]/Mobile", strLoan.trim());
                            if (strGuarantorMemberNumber.equals(strLoanGuarantor)) {
                                break;
                            }
                        }
                    }
                    strResponse = "Remove Guarantor\nConfirm you are REMOVING " + strGuarantorName + " as a guarantor for " + strLoanName + "\n";
                    break;
                }
                case "DISCARD": {
                    strResponse = "Discard Loan Application\nConfirm you are DISCARDING loan application for " + strLoanName + "\n";
                    break;
                }
            }

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_GUARANTORS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("LoansMenus.addGuarantorConfirmation() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoansGuaranteed(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strLoan = "";
            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            switch (theParam) {
                case "MENU": {
                    String strHeader = "Loans Guaranteed\nSelect a loanee";
                    theUSSDResponse = GeneralMenus.displayMenu_LoaneesGuaranteed(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOANS_GUARANTEED_LOANEE);

                    break;
                }
                case "LOANEE": {
                    String strLoanee = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_LOANEE.name());

                    if (!strLoanee.equals("")) {
                        String strResponse = "Loans Guaranteed\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOANS_GUARANTEED_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strHeader = "Loans Guaranteed\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoaneesGuaranteed(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOANS_GUARANTEED_LOANEE);
                    }
                    break;
                }
                case "PIN": {
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strLoansGuaranteedPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOANS_GUARANTEED_PIN.name());

                    if (strLoansGuaranteedPIN.equals(strPIN)) {
                        theUSSDResponse = loanGuaranteedDetails(theUSSDRequest, "Loan Guaranteed");
                    } else {
                        String strResponse = "Loans Guaranteed\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOANS_GUARANTEED_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "END": {
                    String strResponse = "Loans Guaranteed\n{Select a valid menu}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }

                default: {
                    System.err.println("theAppMenus.displayMenu_LoansGuaranteed() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Loans Guaranteed\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanGuarantors() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getLoanDetailsForGuarantorship(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType, boolean theDisplayActionMenus) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try {
            String loans = theUSSDAPI.getLoanAwaitingGuarantorship(theUSSDRequest);

            String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("Loan/Name", loans);
            String strLoanAmount = MBankingXMLFactory.getXPathValueFromXMLString("Loan/Amount", loans);
            String strAvailableGuarantors = MBankingXMLFactory.getXPathValueFromXMLString("Loan/Guarantors/@Current", loans);
            String strRequiredGuarantors = MBankingXMLFactory.getXPathValueFromXMLString("Loan/Guarantors/@Required", loans);

            if (strAvailableGuarantors.equals("")) {
                strAvailableGuarantors = "0";
            }

            if (strRequiredGuarantors.equals("")) {
                strRequiredGuarantors = "0";
            }

            if (theDisplayActionMenus) {
                theHeader += "\n" + strLoanName + "\nKES " + strLoanAmount + "\n" + strAvailableGuarantors + " of " + strRequiredGuarantors + " Guarantors\n";
            }

            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            theUSSDResponse = GeneralMenus.displayMenu_LoansAwaitingGuarantorship(theUSSDRequest, theHeader, theUSSDDataType, loans, theArrayListUSSDSelectOption);

            if (theDisplayActionMenus) {
                int intAvailableGuarantors = Integer.parseInt(strAvailableGuarantors);
                int intRequiredGuarantors = Integer.parseInt(strRequiredGuarantors);

                if (intAvailableGuarantors < intRequiredGuarantors) {
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "88", "ADD", "88: Add");
                }
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "99", "REMOVE", "99: Remove");
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "101", "DISCARD", "101: Discard Application");
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("LoansMenus.getAllLoaners_PENDING() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse loanGuaranteedDetails(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            String strLoaneesXML = new USSDAPI().getLoansGuaranteedForViewOnly(theUSSDRequest).trim();

            String strLoanSerial = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Number", strLoaneesXML);
            String strName = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/LoanName", strLoaneesXML);
            String strMemberName = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/MemberName", strLoaneesXML);
            String strMobileNumber = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Mobile", strLoaneesXML);
            String strAmount = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Amount", strLoaneesXML);
            String strDate = MBankingXMLFactory.getXPathValueFromXMLString("/LoanGuaranteed/Date", strLoaneesXML);

            //theHeader = theHeader + "\nSerial: " + strLoanSerial;
            theHeader = theHeader + "\nLoanee: " + strMemberName;
            theHeader = theHeader + "\nMobile: " + strMobileNumber + "\n";
            theHeader = theHeader + "\nLoan: " + strName;
            theHeader = theHeader + "\nAmount: " + strAmount;
            theHeader = theHeader + "\nDate: " + strDate + "\n";

            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOANS_GUARANTEED_END, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("LoansMenus.confirmLoanGuarantee() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse displayMenu_LoansActionGuarantorship(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try {
            String strHeader = "Accept / Reject Guarantorship\n";
            switch (theParam) {
                case "MENU": {
                    strHeader += "Select a loanee\n";
                    theUSSDResponse = GeneralMenus.displayMenu_LoaneesPendingGuarantorship(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_LOANEE);
                    break;
                }
                case "LOANEE": {
                    String strLoanee = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_LOANEE.name());
                    if (!strLoanee.equals("")) {
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

                        LinkedHashMap<String, String> hmLoan = theUSSDAPI.getLoansPendingGuarantorship(theUSSDRequest);
                        String strMemberName = hmLoan.get("MEMBER_NAME");
                        String strLoanName = hmLoan.get("LOAN_NAME");
                        String strNumber = hmLoan.get("LOAN_NUMBER");
                        String strAmount = hmLoan.get("LOAN_AMOUNT");
                        String strMobile = hmLoan.get("LOAN_MOBILE_NUMBER");
                        String strDate = hmLoan.get("LOAN_DATE");

                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        String strResponse = strHeader + "\nLoanee: " + strMemberName;

                        strResponse = strResponse + "\nLoan: " + strLoanName;
                        strResponse = strResponse + "\nAmount: KES " + strAmount;
                        strResponse = strResponse + "\nMobile: " + strMobile + "\n";
                        //strResponse = strResponse + "\nDate: " + strDate+"\n";

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACCEPT", "1: Accept");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "REJECT", "2: Reject");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION, "NO", theArrayListUSSDSelectOption);
                    } else {
                        strHeader += "{Select a valid menu}\nSelect a loanee";
                        theUSSDResponse = GeneralMenus.displayMenu_LoaneesPendingGuarantorship(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_LOANEE);
                    }
                    break;
                }
                case "ACTION": {
                    String strLoanAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION.name());

                    if (!strLoanAction.isEmpty()) {
                        if (strLoanAction.equals("ACCEPT")) {
                            strHeader = "Accept Loan Guarantorship\n";
                        } else {
                            strHeader = "Reject Loan Guarantorship\n";
                        }
                        String strResponse = strHeader + "\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

                        strHeader += "{Select a valid menu}\n";
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "ACCEPT", "1: Accept");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "REJECT", "2: Reject");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION, "NO", theArrayListUSSDSelectOption);
                    }
                    break;
                }
                case "PIN": {
                    String strLoanAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION.name());

                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        switch (strLoanAction) {
                            case "ACCEPT": {
                                strHeader = "Accept Loan Guarantorship\n\nConfirm you are ACCEPTING";
                                break;
                            }
                            case "REJECT": {
                                strHeader = "Reject Loan Guarantorship\n\nConfirm you are REJECTING";
                                break;
                            }
                        }

                        String strResponse = strHeader + " guarantorship for ";

                        LinkedHashMap<String, String> hmLoan = theUSSDAPI.getLoansPendingGuarantorship(theUSSDRequest);
                        String strMemberName = hmLoan.get("MEMBER_NAME");
                        String strLoanName = hmLoan.get("LOAN_NAME");
                        String strAmount = hmLoan.get("LOAN_AMOUNT");

                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        strResponse = strResponse + strMemberName + "'s ";

                        strResponse = strResponse + strLoanName;
                        strResponse = strResponse + " of KES " + strAmount + "\n";


                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                    } else {
                        if (strLoanAction.equals("ACCEPT")) {
                            strHeader = "Accept Guarantorship\n";
                        } else {
                            strHeader = "Reject Guarantorship\n";
                        }
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strLoanAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION.name());
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_CONFIRMATION.name());
                    if (strConfirmation.equalsIgnoreCase("YES")) {
                        String strResponse;

                        APIConstants.TransactionReturnVal transactionReturnVal = theUSSDAPI.actionLoanGuarantorship(theUSSDRequest);

                        if (transactionReturnVal.equals(APIConstants.TransactionReturnVal.SUCCESS)) {
                            strResponse = "Dear member, your request to " + strLoanAction + " loan guarantorship has been received successfully. Please wait shortly as it's being processed.\n";
                        } else {
                            switch (transactionReturnVal) {
                                case INCORRECT_PIN: {
                                    strResponse = "Sorry the PIN provided is incorrect. Your request to " + strLoanAction + " loan guarantorship CANNOT be completed.\n";
                                    break;
                                }
                                case BLOCKED: {
                                    strResponse = "Dear member, your account has been blocked. Your request to " + strLoanAction + " loan guarantorship CANNOT be completed.\n";
                                    ;
                                    break;
                                }
                                default: {
                                    strResponse = "Sorry, your request to " + strLoanAction + " loan guarantorship CANNOT be completed.\nPlease try again later.\n";
                                    break;
                                }
                            }
                        }
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_END, "NO", theArrayListUSSDSelectOption);

                    } else if (strConfirmation.equalsIgnoreCase("NO")) {
                        String strResponse = "Dear member, your request to " + strLoanAction + " loan guarantorship was NOT confirmed. Request NOT COMPLETED.";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_END, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strResponse = "Confirm Details for " + strLoanAction + ":\n{Select a valid menu}\n";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    }
                    break;
                }
                case "END": {
                    String strLoanAction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_ACTION.name());
                    String strResponse = strLoanAction + " loan guarantorship\n{Select a valid menu}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }

                default: {
                    System.err.println("theAppMenus.displayMenu_LoansActionGuarantorship() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Accept / Reject Loan Guarantorship\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_ACTION_GUARANTORSHIP_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoansActionGuarantorship() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}

