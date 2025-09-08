package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.*;

import java.util.ArrayList;

public class AppMenus implements GeneralMenus, HomeMenus, SetPINMenus, ChangePINMenus, WithdrawalMenus, DepositMenus, MyAccountMenus, BalanceEnquiryMenus, MiniStatementMenus, ATMCardMenus, ActivateMobileAppMenus, UtilitiesMenus, LoansMenus, FundsTransferMenus, AccountRegistrationMenus, MemberActionMenus, TermsAndConditionsMenus, SystemReportMenus, DividendPayslipMenus, BuyGoodsMenu, CheckMembershipStatus, ErroneousTransactions {
    AppMenus() {
    }

    public USSDResponse displayMenu_GeneralDisplay(USSDRequest theUSSDRequest, String strResponse, String theUSSDCharge) {
        USSDResponseTEXT theUSSDResponse = new USSDResponseTEXT();
        try {
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.END);
            theUSSDResponse.setUSSDCharge(theUSSDCharge);

            theUSSDResponse.setUSSDResponseText(strResponse);
            return theUSSDResponse;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }
        return theUSSDResponse;
    }

    public USSDResponse displayMenu_GeneralInput(USSDRequest theUSSDRequest, String theResponse, AppConstants.USSDDataType theUSSDDataType, USSDConstants.USSDInputType theUSSDInputType, String theUSSDCharge) {
        USSDResponseINPUT theUSSDResponse = new USSDResponseINPUT();
        try {

            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDCharge(theUSSDCharge);

            //INPUT
            theUSSDResponse.setUSSDInputDataType(theUSSDDataType.getValue());
            theUSSDResponse.setUSSDInputName(theUSSDDataType.name());
            theUSSDResponse.setUSSDInputType(theUSSDInputType);

            theUSSDResponse.setUSSDInputDisplayText(theResponse);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }
        return theUSSDResponse;
    }

    public USSDResponse displayMenu_GeneralSelectWithExit(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType, String theUSSDCharge, ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {
            //SELECT
            theUSSDResponse.setUSSDAction(USSDConstants.USSDAction.CON);
            theUSSDResponse.setUSSDSessionID(theUSSDRequest.getUSSDSessionID());
            theUSSDResponse.setUSSDCharge(theUSSDCharge);

            theUSSDResponse.setUSSDSelectDataType(theUSSDDataType.getValue());
            theUSSDResponse.setUSSDSelectName(theUSSDDataType.name());

            //DEFAULT OPTIONS
            USSDResponseSELECTOption.setUSSDSelectOptionEXIT(theArrayListUSSDSelectOption, AppConstants.USSDDiplayText.EXIT.getValue());

            //SELECT OPTIONS
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }

        return theUSSDResponse;
    }

    public USSDResponse displayMenu_GeneralSelectWithHomeAndExit(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType, String theUSSDCharge, ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {

            USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
            theUSSDResponse = (USSDResponseSELECT) displayMenu_GeneralSelectWithExit(theUSSDRequest, theUSSDDataType, theUSSDCharge, theArrayListUSSDSelectOption);
            //SELECT OPTIONS
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }

        return theUSSDResponse;
    }

    public USSDResponse displayMenu_GeneralSelectWithConfirmation(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType, String theUSSDCharge,
                                                                  ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "YES", "1: Yes");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "NO", "2: No");
            USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
            theUSSDResponse = (USSDResponseSELECT) displayMenu_GeneralSelectWithExit(theUSSDRequest, theUSSDDataType, theUSSDCharge, theArrayListUSSDSelectOption);
            //SELECT OPTIONS
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }

        return theUSSDResponse;
    }

    public USSDResponse displayMenu_GeneralSelectWithConfirmationWithoutHome(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType, String theUSSDCharge,
                                                                             ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption) {
        USSDResponseSELECT theUSSDResponse = new USSDResponseSELECT();
        try {

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "YES", "1: Yes");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "NO", "2: No");
            theUSSDResponse = (USSDResponseSELECT) displayMenu_GeneralSelectWithExit(theUSSDRequest, theUSSDDataType, theUSSDCharge, theArrayListUSSDSelectOption);
            //SELECT OPTIONS
            theUSSDResponse.setUSSDSelectOption(theArrayListUSSDSelectOption);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
        }

        return theUSSDResponse;
    }
} // End Class MainMenu
