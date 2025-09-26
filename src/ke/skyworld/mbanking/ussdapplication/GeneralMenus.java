package ke.skyworld.mbanking.ussdapplication;


import ke.co.skyworld.smp.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.utility_items.data_formatting.Converter;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.ussd.*;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;
import org.w3c.dom.NodeList;

import java.util.*;

public interface GeneralMenus {

    static USSDResponse displayMenu_BankAccounts(USSDRequest theUSSDRequest, String theParam, String theHeader, APIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType, AppConstants.USSDDataType theUSSD_END_DataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);


            String strGroup = theUSSDRequest.getUSSDData().getOrDefault(AppConstants.USSDDataType.MY_ACCOUNT_MINI_STATEMENT_ACCOUNT_GROUP.name(), "");

            FlexicoreArrayList accountsList = theUSSDAPI.getBankAccounts_V2(theUSSDRequest, theAccountType, strGroup);

            if (accountsList != null && !accountsList.isEmpty()) {
                /**
                 *{
                 *   "account_name": "PETER MAKAU MUTUA",
                 *   "account_label": "CURRENT",
                 *   "account_number": "1081086730",
                 *   "account_type": "FOSA",
                 *   "account_balance": 459539.04,
                 *   "book_balance": 459603.76,
                 *   "account_status": "ACTIVE",
                 *   "can_withdraw": "YES",
                 *   "max_withdrawable_amount": 459539.04,
                 *   "can_deposit": "YES",
                 *   "max_deposit_amount": 999999999999,
                 *   "can_withdraw_ift": "YES",
                 *   "can_deposit_ift": "YES",
                 *   "product_id": "CURRENT",
                 *   "product_name": "Current Account",
                 *   "show_balance": true
                 *}
                 */

                boolean foundAccounts = false;

                int intOptionMenu = 1;
                for (FlexicoreHashMap accountMap : accountsList) {

                    String strAccountStatus = accountMap.getStringValue("account_status").trim();
                    String strAccountNumber = accountMap.getStringValue("account_number").trim();
                    String strAccountLabel = accountMap.getStringValue("account_label").trim();
                    String strAccountBookBalance = accountMap.getStringValue("account_balance").trim();
                    String strCanDeposit = accountMap.getStringValue("can_deposit").trim();
                    String strCanWithdraw = accountMap.getStringValue("can_withdraw").trim();


                    if (theUSSDDataType == AppConstants.USSDDataType.DEPOSIT_ACCOUNT) {
                        if (!strCanDeposit.equalsIgnoreCase("YES")) continue;
                    }


                    if (theAccountType.getValue().equalsIgnoreCase(USSDAPIConstants.AccountType.WITHDRAWABLE.getValue())) {
                        if (!strCanWithdraw.equalsIgnoreCase("YES") || !strAccountStatus.equalsIgnoreCase("ACTIVE")) {
                            continue;
                        }
                    }

                    if (theAccountType.getValue().equalsIgnoreCase(USSDAPIConstants.AccountType.WITHDRAWABLE_IFT.getValue())) {
                        if (!strAccountStatus.equalsIgnoreCase("ACTIVE")) {
                            continue;
                        }
                    }

                    foundAccounts = true;

                    HashMap<String, String> hmOption = new HashMap<>();
                    hmOption.put("ac_no", strAccountNumber);
                    hmOption.put("ac_name", strAccountLabel);
                    hmOption.put("ac_label", strAccountLabel);
                    hmOption.put("ac_bal", strAccountBookBalance);

                    String optionName = Converter.toJson(hmOption);

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), optionName, intOptionMenu + ": " + strAccountLabel + "-" + strAccountNumber);

                    intOptionMenu++;
                }

                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);


            }else{
                String strResponse = "Sorry, no accounts found." + CBSAPI.getTrailerMessage();
                theArrayListUSSDSelectOption = new ArrayList<>();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSD_END_DataType, "NO", theArrayListUSSDSelectOption);
                return theUSSDResponse;
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_BankAccounts() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_Loans(USSDRequest theUSSDRequest, String theParam, String theHeader, APIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType, AppConstants.USSDDataType theUSSD_END_DataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            TransactionWrapper<FlexicoreHashMap> accountsListWrapper = theUSSDAPI.getLoans_V2(theUSSDRequest);

            if (accountsListWrapper.hasErrors()) {
                FlexicoreHashMap customersListMap = accountsListWrapper.getSingleRecord();
                USSDAPIConstants.Condition endSession = customersListMap.getValue("end_session");
                String strResponse = accountsListWrapper.getSingleRecord().getStringValue("display_message");

                theArrayListUSSDSelectOption = new ArrayList<>();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSD_END_DataType, "NO", theArrayListUSSDSelectOption);

                return theUSSDResponse;
            }

            FlexicoreArrayList accountsList = accountsListWrapper.getSingleRecord().getFlexicoreArrayList("payload");

            if (accountsList.isEmpty()) {

                String strResponse = "No loans found";

                theArrayListUSSDSelectOption = new ArrayList<>();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSD_END_DataType, "NO", theArrayListUSSDSelectOption);

            }
            else {

                int intOptionMenu = 1;
                for (FlexicoreHashMap accountMap : accountsList) {

                    String strAccountName = accountMap.getStringValue("loan_type_name").trim();
                    String strAccountNumber = accountMap.getStringValue("loan_serial_number").trim();
                    String strAccountBalance = accountMap.getStringValue("loan_balance").trim();
                    String strInterestBalance = accountMap.getStringValue("interest_amount").trim();
                    //String strAccountLabel = accountMap.getStringValue("account_label").trim();

                    /*strAccountBalance = strAccountBalance.replaceFirst("-", "");
                    strInterestBalance = strInterestBalance.replaceFirst("-", "");*/

                    double dblAccountBalance = Double.parseDouble(strAccountBalance);
                    double dblInterestBalance = Double.parseDouble(strInterestBalance);

                    dblAccountBalance = APIUtils.roundUp(dblAccountBalance);
                    dblInterestBalance = APIUtils.roundUp(dblInterestBalance);

                    if (dblAccountBalance + dblInterestBalance <= 0.00) {
                        continue;
                    }

                    HashMap<String, String> hmOption = new HashMap<>();
                    hmOption.put("ac_no", strAccountNumber);
                    hmOption.put("ac_name", strAccountName);
                    hmOption.put("ac_label", strAccountName);
                    hmOption.put("bal", Utils.formatDouble(dblAccountBalance, "#,##0.00"));
                    hmOption.put("intr", Utils.formatDouble(dblInterestBalance, "#,##0.00"));

                    String optionName = Converter.toJson(hmOption);

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(intOptionMenu), optionName, intOptionMenu + ": " + strAccountName + "-" + strAccountNumber);
                    intOptionMenu++;
                }

                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoansWithGuarantors(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedHashMap<String, LinkedHashMap<String, String>> loans = theUSSDAPI.getLoansWithGuarantors(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loans != null) {
                int i = 0;
                for (String loan : loans.keySet()) {
                    i++;
                    System.out.println("loan: " + loan + " loan type: " + loans.get(loan));
                    String strLoan = loan;
                    LinkedHashMap<String, String> hmLoan = loans.get(loan);

                    String strOptionValue = Utils.serialize(hmLoan);
                    String strLoanName = hmLoan.get("LOAN_NAME");
                    String strLoanID = hmLoan.get("LOAN_ID");
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strLoanName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoaneesGuaranteed(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedHashMap<String, LinkedHashMap<String, String>> loans = theUSSDAPI.getLoansGuaranteed(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loans != null) {
                int i = 0;
                for (String loan : loans.keySet()) {
                    i++;
                    System.out.println("loan: " + loan + " loan type: " + loans.get(loan));
                    String strLoan = loan;
                    LinkedHashMap<String, String> hmLoan = loans.get(loan);

                    String strOptionValue = Utils.serialize(hmLoan);
                    String strLoanName = hmLoan.get("LOANEE_NAME");
                    String strLoanID = hmLoan.get("LOANEE_ID");
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strLoanName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoansAwaitingGuarantorship(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType, String theLoans, ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            if (!theLoans.equals("")) {
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

                NodeList guarantors = APIUtils.getXMLNodeListFromPath("/Loan/Guarantors/Guarantor", theLoans.trim());

                if (guarantors != null) {
                    for (int i = 1; i <= guarantors.getLength(); i++) {
                        String strGuarantorName = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Guarantors/Guarantor[" + i + "]/Name", theLoans);
                        String strGuarantorMemberNumber = MBankingXMLFactory.getXPathValueFromXMLString("/Loan/Guarantors/Guarantor[" + i + "]/Mobile", theLoans);
                        String strOptionMenu = Integer.toString(i);
                        String strOptionDisplayText = strOptionMenu + ": " + strGuarantorName;

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strGuarantorMemberNumber, strOptionDisplayText);
                    }
                } else {
                    theHeader += "You do not have any guarantors for this loan product\nEnter 88 to add.\n";
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
                }
            } else {
                theHeader += "You do not have any pending loan applications that require guarantorship";
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanPurposes(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedHashMap<String, LinkedHashMap<String, String>> loanPurposes = theUSSDAPI.getLoanPurposes(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loanPurposes != null) {
                int i = 0;
                for (String loanPurpose : loanPurposes.keySet()) {
                    i++;
                    System.out.println("purpose: " + loanPurpose + " purpose type: " + loanPurposes.get(loanPurpose));
                    LinkedHashMap<String, String> hmLoanPurpose = loanPurposes.get(loanPurpose);

                    String strOptionValue = Utils.serialize(hmLoanPurpose);
                    String strLoanPurposeName = hmLoanPurpose.get("LOAN_PURPOSE_NAME");
                    String strLoanPurposeID = hmLoanPurpose.get("LOAN_PURPOSE_ID");
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strLoanPurposeName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanPurposes(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType, AppConstants.USSDDataType theUSSD_END_DataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        String strMobileNo = String.valueOf(theUSSDRequest.getUSSDMobileNo());

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            //get customer no of the user dialing -- end

            TransactionWrapper<FlexicoreHashMap> getLoanPurposesWrapper = theUSSDAPI.getLoanPurposes(theUSSDRequest, "MSISDN", strMobileNo);

            FlexicoreHashMap getLoanPurposesMap = getLoanPurposesWrapper.getSingleRecord();

            int count = 0;

            if (!getLoanPurposesWrapper.hasErrors()) {

                FlexicoreArrayList loanPurposesList = getLoanPurposesMap.getFlexicoreArrayList("payload");

                if (loanPurposesList != null && !loanPurposesList.isEmpty()) {

                    for (FlexicoreHashMap loanPurposeItemMap : loanPurposesList) {
                        count++;

                        HashMap<String, String> hmLoanPurpose = new HashMap<>();

                        String strLoanPurposeCode = loanPurposeItemMap.getStringValue("code");
                        String strLoanPurposeDescription = loanPurposeItemMap.getStringValue("description");

                        hmLoanPurpose.put("code", strLoanPurposeCode);
                        hmLoanPurpose.put("description", strLoanPurposeDescription);

                        String strOptionValue = Utils.serialize(hmLoanPurpose);

                        String strOptionMenu = Integer.toString(count);
                        String strOptionDisplayText = strOptionMenu + ": " + strLoanPurposeDescription;

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                    }
                }
            }

            if (count > 0) {
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
                ((USSDResponseSELECT) theUSSDResponse).setUSSDSelectOptionCustomCount(count);

            } else {
                String strResponse = "Sorry, no loan purposes found.";
                theArrayListUSSDSelectOption = new ArrayList<>();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSD_END_DataType, "NO", theArrayListUSSDSelectOption);
                return theUSSDResponse;
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanBranches(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedHashMap<String, LinkedHashMap<String, String>> loanBranches = theUSSDAPI.getLoanBranches(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loanBranches != null) {
                int i = 0;
                for (String loanBranch : loanBranches.keySet()) {
                    i++;
                    System.out.println("branch: " + loanBranch + " branch name: " + loanBranches.get(loanBranch));
                    LinkedHashMap<String, String> hmLoanBranch = loanBranches.get(loanBranch);

                    String strOptionValue = Utils.serialize(hmLoanBranch);
                    String strLoanBranchName = hmLoanBranch.get("B_NAME");
                    String strLoanBranchID = hmLoanBranch.get("B_ID");
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strLoanBranchName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    /*static USSDResponse displayMenu_ATMCards(USSDRequest theUSSDRequest, String theParam, String theHeader, APIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            HashMap<String, HashMap<String, String>> atmCards = theUSSDAPI.getATMCards(theUSSDRequest);

            if (atmCards != null) {
                if (!atmCards.isEmpty()) {
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

                    int i = 0;
                    for (String loanType : atmCards.keySet()) {
                        i++;
                        String strAccount = loanType;
                        HashMap<String, String> hmLoanType = atmCards.get(loanType);

                        String strOptionValue = Utils.serialize(hmLoanType);
                        String strLoanName = hmLoanType.get("NAME");
                        String strLoanID = hmLoanType.get("ID");
                        String strOptionMenu = Integer.toString(i);
                        String strOptionDisplayText = strOptionMenu + ": " + strLoanName;

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                    }

                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
                } else {
                    String strResponse = "You do not have any ATM cards under your account.\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO", theArrayListUSSDSelectOption);
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }*/

    static USSDResponse displayMenu_ATMCards(USSDRequest theUSSDRequest, String theParam, String theHeader, APIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {

            String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());
            HashMap<String, String> userIdentifierDetails = APIUtils.getUserIdentifierDetails(strMobileNumber);
            String strIdentifierType = userIdentifierDetails.get("identifier_type");
            String strIdentifier = userIdentifierDetails.get("identifier");
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            TransactionWrapper<FlexicoreHashMap> atmCardsWrapper = theUSSDAPI.getATMCards(theUSSDRequest, strIdentifierType, strIdentifier);

            FlexicoreHashMap atmCardsMap = atmCardsWrapper.getSingleRecord();

            int count = 0;

            if (!atmCardsWrapper.hasErrors()) {

                FlexicoreArrayList atmCardsList = atmCardsMap.getFlexicoreArrayList("payload");

                if (atmCardsList != null && !atmCardsList.isEmpty()) {

                    for (FlexicoreHashMap atmCardItemMap : atmCardsList) {
                        count++;

                        HashMap<String, String> hmATMCards = new HashMap<>();

                        String strATMCardsNumber = atmCardItemMap.getStringValue("card_number");
                        String strATMCardsMemberName = atmCardItemMap.getStringValue("member_name");
                        String strATMCardsId = atmCardItemMap.getStringValue("card_id");
                        String strATMCardsIsLinked= atmCardItemMap.getStringValue("is_linked");
                        String strATMCardsCardStatus= atmCardItemMap.getStringValue("card_status");

                        hmATMCards.put("cardNumber", strATMCardsNumber);
                        hmATMCards.put("memberName", strATMCardsMemberName);
                        hmATMCards.put("cardId", strATMCardsId);
                        hmATMCards.put("isLinked", strATMCardsIsLinked);
                        hmATMCards.put("cardStatus", strATMCardsCardStatus);

                        String strOptionValue = Utils.serialize(hmATMCards);

                        String strOptionMenu = Integer.toString(count);
                        String strOptionDisplayText = strOptionMenu + ": " + strATMCardsMemberName + " - " + strATMCardsNumber;

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                    }
                }
            }

            if (count > 0) {
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
                ((USSDResponseSELECT) theUSSDResponse).setUSSDSelectOptionCustomCount(count);

            } else {
                String strResponse = "Sorry, no loan purposes found.";
                theArrayListUSSDSelectOption = new ArrayList<>();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ATM_CARD_END, "NO", theArrayListUSSDSelectOption);
                return theUSSDResponse;
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }


    static USSDResponse displayMenu_LoanTypes(USSDRequest theUSSDRequest, String theLoanType, String theHeader, AppConstants.USSDDataType theUSSDDataType, String theLoanAccount) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedList<String> loanTypes = theUSSDAPI.getLoanTypes(theUSSDRequest, theLoanType, theLoanAccount);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loanTypes != null) {
                int i = 0;
                for (String loanType : loanTypes) {
                    i++;

                    String strCode = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", loanType);
                    String strName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", loanType);

                    APIUtils.setLoanTypeInMemory(strCode, loanType, theUSSDRequest);

                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strCode, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }


    static USSDResponse displayMenu_LoanTypes(USSDRequest theUSSDRequest, String theLoanType, String theHeader, AppConstants.USSDDataType theUSSDDataType, AppConstants.USSDDataType theUSSD_END_DataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        String strMobileNo = String.valueOf(theUSSDRequest.getUSSDMobileNo());

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            TransactionWrapper<FlexicoreHashMap> getLoanTypesWrapper = CBSAPI.getLoanTypes(strMobileNo, "MSISDN", strMobileNo);

            FlexicoreHashMap getLoanTypesMap = getLoanTypesWrapper.getSingleRecord();
            int count = 0;

            if (!getLoanTypesWrapper.hasErrors()) {

                FlexicoreArrayList mobileEnabledLoansList = getLoanTypesMap.getFlexicoreArrayList("payload");

                if (mobileEnabledLoansList != null && !mobileEnabledLoansList.isEmpty()) {

                    for (FlexicoreHashMap loanTypesItemMap : mobileEnabledLoansList) {
                        count++;

                        HashMap<String, String> hmLoanType = new HashMap<>();

                        String strLoanTypeName = loanTypesItemMap.getStringValue("loan_type_name");
                        String strLoanTypeID = loanTypesItemMap.getStringValue("loan_type_id");

                        if(theLoanType!=null && !theLoanType.isEmpty() && !theLoanType.equalsIgnoreCase(strLoanTypeID)){
                            continue;
                        }

                        String strLoanTypeLabel = loanTypesItemMap.getStringValue("loan_type_name");
                        String strLoanTypeMin = loanTypesItemMap.getStringValue("loan_type_min_amount");
                        String strLoanTypeMax = loanTypesItemMap.getStringValue("loan_type_max_amount");
                        //String strLoanTypeMaxInstallments = loanTypesItemMap.getStringValue("loan_type_max_installments");
                        //String strLoanRequiresInstallments = loanTypesItemMap.getStringValue("requires_installments");

                        hmLoanType.put("id", strLoanTypeID);
                        hmLoanType.put("name", strLoanTypeName);
                        hmLoanType.put("label", strLoanTypeLabel);
                        hmLoanType.put("min", strLoanTypeMin);
                        hmLoanType.put("max", strLoanTypeMax);
                        //hmLoanType.put("requires_installments", strLoanRequiresInstallments);
                        //hmLoanType.put("max_installments", strLoanTypeMaxInstallments);

                        String strOptionValue = Utils.serialize(hmLoanType);

                        String strOptionMenu = Integer.toString(count);
                        String strOptionDisplayText = strOptionMenu + ": " + strLoanTypeLabel;

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                    }
                }
            }

            if (count > 0) {
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
                ((USSDResponseSELECT) theUSSDResponse).setUSSDSelectOptionCustomCount(count);

            } else {
                String strResponse = "Sorry, no loans found.";
                theArrayListUSSDSelectOption = new ArrayList<>();
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSD_END_DataType, "NO", theArrayListUSSDSelectOption);
                return theUSSDResponse;
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }



    static USSDResponse displayMenu_ErroneousTransactions(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedList<String> loanTypes = theUSSDAPI.getErroneousTransactions(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loanTypes != null && !loanTypes.isEmpty()) {
                int i = 0;
                for (String loanType : loanTypes) {
                    i++;

                    String strTransactionID = MBankingXMLFactory.getXPathValueFromXMLString("/ErroneousTransaction/TransactionID", loanType);
                    String strAccount = MBankingXMLFactory.getXPathValueFromXMLString("/ErroneousTransaction/AccountNo", loanType);
                    String strAmount = MBankingXMLFactory.getXPathValueFromXMLString("/ErroneousTransaction/Amount", loanType);


                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": A/C: " + strAccount + " Amount : " + strAmount;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strTransactionID, strOptionDisplayText);
                }
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

            if (loanTypes != null && loanTypes.isEmpty()) {
                String strOptionDisplayText = theHeader +".\nYou do not have any hanging deposits at the moment";
                theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strOptionDisplayText, "NO");
            }


        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static LinkedHashMap<String, String> displayMenu_UpdateErroneousTransactions(USSDRequest theUSSDRequest, String theId, String theAccount, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        LinkedHashMap<String, String> response = new LinkedHashMap<>();
        try {

            LinkedList<String> loanTypes = theUSSDAPI.UpdateErroneousTransactions(theId, theAccount);


            int i = 0;
            String loanType = loanTypes.get(0);

            String strStatus = MBankingXMLFactory.getXPathValueFromXMLString("/Response/Status", loanType);
            String strDescription = MBankingXMLFactory.getXPathValueFromXMLString("/Response/StatusDescription", loanType);

            response.put("status", strStatus);
            response.put("description", strDescription);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return response;
    }

    static USSDResponse displayMenu_LoaneesPendingGuarantorship(USSDRequest theUSSDRequest, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedHashMap<String, LinkedHashMap<String, String>> loanees = theUSSDAPI.getLoaneesPendingGuarantorship(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (loanees != null) {
                int i = 0;
                for (String loanee : loanees.keySet()) {
                    i++;
                    LinkedHashMap<String, String> hmloanee = loanees.get(loanee);

                    String strOptionValue = Utils.serialize(hmloanee);
                    String strLoaneeName = hmloanee.get("LOANEE_NAME");
                    String strLoaneeID = hmloanee.get("LOANEE_ID");
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strLoaneeName;

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_Loans() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanCategories(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FOSA", "1: FOSA Loans");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "BOSA", "2: BOSA Loans");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanCategories() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanInstallments(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType, String theLoanXML) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            NodeList nlInstallments = APIUtils.getXMLNodeListFromPath("/Product/PresetInstallments/Installment", theLoanXML);

            if (nlInstallments != null) {
                for (int i = 1; i <= nlInstallments.getLength(); i++) {
                    String strInstallmentId = MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment[" + i + "]/@Id", theLoanXML);
                    String strInstallmentLabel = MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment[" + i + "]/@Label", theLoanXML);
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strInstallmentId, strInstallmentId, strInstallmentId + ": " + strInstallmentLabel);
                }
            }
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_LoanCategories() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountGroups(USSDRequest theUSSDRequest, String theParam, String theHeader, APIConstants.AccountType theAccountType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            LinkedHashMap<String, String> accounts = theUSSDAPI.getAccountGroups(theUSSDRequest);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (accounts != null) {
                int i = 0;
                for (String account : accounts.keySet()) {
                    i++;
                    System.out.println("group: " + account + " group id: " + accounts.get(account));
                    String strAccount = account;
                    String strAccountType = accounts.get(account);

                    String strOptionValue = strAccount;
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strAccountType;// + " (" + strAccount + ")"; //"1: Member Acct.(10101010101)"

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_BankAccounts() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountTypes(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "FOSA", "1: FOSA Accounts");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "BOSA", "2: BOSA Accounts");
            //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "ALL", "3: All Accounts");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "LOAN", "3: Loans");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_AccountTypes() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_AccountCategories(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "PERSONAL", "1: Personal Account");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "GROUP", "2: Group Account");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_AccountTypes() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_LoanGroupCategories(USSDRequest theUSSDRequest, String theParam, String theHeader, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "PERSONAL", "1: Personal Loan");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "GROUP", "2: Group Loan");
            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_AccountTypes() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse getAccountMaintenanceMenus(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType, String theAccountType, String theAccountNaming, String theSPProviderAccountCode, String theHeader, USSDConstants.Condition theDesplayAddRemove) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {

            String strMobileNo = String.valueOf(theUSSDRequest.getUSSDMobileNo());

            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            boolean blAccountsMax = false;

            try {

                String strIntegritySecret = PESALocalParameters.getIntegritySecret();
                SPManager spManager = new SPManager(strIntegritySecret);

                System.out.println("strMobileNo: "+strMobileNo);
                System.out.println("theSPProviderAccountCode: "+theSPProviderAccountCode);

                LinkedList<LinkedHashMap<String, String>> listAccounts = spManager.getUserSavedAccountsByProvider(SPManagerConstants.UserIdentifierType.MSISDN, strMobileNo, theSPProviderAccountCode);

                blAccountsMax = listAccounts.size() >= 7;

                for (int i = 1; i <= listAccounts.size(); i++) {
                    LinkedHashMap<String, String> account = listAccounts.get((i - 1));

                    String strUserAccountID = account.get("user_account_id");
                    String strUserAccountName = account.get("user_account_name");
                    String strUserAccountIdentifier = account.get("user_account_identifier");
                    String strIntegrityHashViolated = account.get("integrity_hash_violated");

                    LinkedHashMap<String, String> hmOptionValueAccount = new LinkedHashMap<>();
                    hmOptionValueAccount.put("ACTION", "CHOICE");
                    hmOptionValueAccount.put("ACCOUNT_ID", strUserAccountID);
                    hmOptionValueAccount.put("ACCOUNT_NAME", strUserAccountName);
                    hmOptionValueAccount.put("ACCOUNT_IDENTIFIER", strUserAccountIdentifier);
                    String strOptionValueAccount = Utils.serialize(hmOptionValueAccount);

                    String strOptionDisplayText = i + ": " + strUserAccountName + " (" + strUserAccountIdentifier + ")";

                    //TODO: confirm why this has an issue
                    /*if (strIntegrityHashViolated.equalsIgnoreCase(USSDConstants.Condition.NO.getValue())) {
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(i), strOptionValueAccount, strOptionDisplayText);
                    }else{
                        System.out.println("Integrity has been violated");
                    }*/
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, String.valueOf(i), strOptionValueAccount, strOptionDisplayText);
                }

            } catch (Exception e) {
                System.err.println("GeneralMenus.getAccountMaintenanceMenus() ERROR : " + e.getMessage());
            }


            if (theDesplayAddRemove.equals(USSDConstants.Condition.YES)) {
                LinkedHashMap<String, String> hmOptionValueADD = new LinkedHashMap<>();
                hmOptionValueADD.put("ACTION", "ADD");
                hmOptionValueADD.put("ACCOUNT_TYPE", theAccountType);
                String strOptionValueADD = Utils.serialize(hmOptionValueADD);

                LinkedHashMap<String, String> hmOptionValueREMOVE = new LinkedHashMap<>();
                hmOptionValueREMOVE.put("ACTION", "REMOVE");
                hmOptionValueREMOVE.put("ACCOUNT_TYPE", theAccountType);
                String strOptionValueREMOVE = Utils.serialize(hmOptionValueREMOVE);

                if (!blAccountsMax) {
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "88", strOptionValueADD, "88: Add " + theAccountNaming);
                }

                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "99", strOptionValueREMOVE, "99: Remove " + theAccountNaming);
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);


        } catch (Exception e) {
            System.err.println("GeneralMenus.getAccountMaintenanceMenus() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    static USSDResponse displayMenu_PayMODBillBranches(USSDRequest theUSSDRequest, String theParam, String theHeader, String MODBillType, AppConstants.USSDDataType theUSSDDataType) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        USSDAPI theUSSDAPI = new USSDAPI();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
            LinkedHashMap<String, String> MODBillTypeBranches = theUSSDAPI.getPayMODBillTypeBranches(theUSSDRequest, MODBillType);

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);

            if (MODBillTypeBranches != null) {
                int i = 0;
                for (String branchCode : MODBillTypeBranches.keySet()) {
                    i++;
                    String strBranchCode = branchCode;
                    String strBranchName = MODBillTypeBranches.get(branchCode);

                    String strOptionValue = strBranchCode + "|" + strBranchName;
                    String strOptionMenu = Integer.toString(i);
                    String strOptionDisplayText = strOptionMenu + ": " + strBranchName;// + " (" + strBranchCode + ")"; //"1: Sergeants’ MESS DOD Cau.(203)"

                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strOptionMenu, strOptionValue, strOptionDisplayText);
                }
            }

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, theUSSDDataType, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_PayMODBillBranches() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
            theUSSDAPI = null;
        }
        return theUSSDResponse;
    }

}

