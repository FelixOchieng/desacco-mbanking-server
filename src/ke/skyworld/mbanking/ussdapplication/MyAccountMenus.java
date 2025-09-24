package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface MyAccountMenus {

    public default USSDResponse displayMenu_MyAccount(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            String strUSSDDataType = theUSSDRequest.getUSSDDataType();

            if (strUSSDDataType.equalsIgnoreCase(AppConstants.USSDDataType.MAIN_IN_MENU.getValue())) {
                String strHeader = "My Account";
                theUSSDResponse = getMyAccountMenu(theUSSDRequest, strHeader);

            } else { //MY_ACCOUNT_MENU

                String strMY_ACCOUNT_MENU = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MY_ACCOUNT_MENU.name());

                switch (strMY_ACCOUNT_MENU) {
                    case "BALANCE_ENQUIRY": {
                        theUSSDResponse = theAppMenus.displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
                        break;
                    }
                    case "MINI_STATEMENT": {
                        theUSSDResponse = theAppMenus.displayMenu_MiniStatement(theUSSDRequest, theParam);
                        break;
                    }
                    case "MOBILE_APP": {
                        theUSSDResponse = theAppMenus.displayMenu_MobileApp(theUSSDRequest, theParam);
                        break;
                    }
                    case "CHANGE_PIN": {
                        theUSSDResponse = theAppMenus.displayMenu_ChangePIN(theUSSDRequest, theParam);
                        break;
                    }
                    case "ENROLL_NEW_MEMBER": {
                        theUSSDResponse = theAppMenus.displayMenu_AccountRegistration(theUSSDRequest, theParam);
                        break;
                    }
                    case "UPDATE_EMAIL": {
                        theUSSDResponse = theAppMenus.displayMenu_UpdateEmail(theUSSDRequest, theParam);
                        break;
                    }
                    case "ATM_CARD": {
                        theUSSDResponse = theAppMenus.displayMenu_ATMCard(theUSSDRequest, theParam);
                        break;
                    }
                    case "CHECK_BENEFICIARIES": {
                        theUSSDResponse = theAppMenus.displayMenu_CheckBeneficiaries(theUSSDRequest, theParam);
                        break;
                    }
                    case "SYSTEM_REPORTS": {
                        theUSSDResponse = theAppMenus.displayMenu_SystemReports(theUSSDRequest, theParam);
                        break;
                    }
                    case "DIVIDEND_PAYSLIP": {
                        theUSSDResponse = theAppMenus.displayMenu_DividendPayslip(theUSSDRequest, theParam);
                        break;
                    }

                    case "STATUS": {
                        theUSSDResponse = theAppMenus.displayMenu_CheckAccountStatus(theUSSDRequest, theParam);
                        break;
                    }

                    default: {
                        String strHeader = "My Account\n{Select a valid menu}";
                        theUSSDResponse = getMyAccountMenu(theUSSDRequest, strHeader);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_MyAccount() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    default USSDResponse getMyAccountMenu(USSDRequest theUSSDRequest, String theHeader) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();

        try {
            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, theHeader);
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "BALANCE_ENQUIRY", "1: Balance Enquiry");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "MINI_STATEMENT", "2: Mini Statement");

//            if (String.valueOf(theUSSDRequest.getUSSDMobileNo()).equals("254708608579") || String.valueOf(theUSSDRequest.getUSSDMobileNo()).equals("254712576168")) {
//            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "DIVIDEND_PAYSLIP", "3: Get Dividend Payslip");
//            }

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "3", "MOBILE_APP", "3: Mobile App");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "4", "CHANGE_PIN", "4: Change PIN");
            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "ATM_CARD", "5: ATM Card");

            /*USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "UPDATE_EMAIL", "5: Update E-mail Address");*/
            /*USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "6", "CHECK_BENEFICIARIES", "6: Check My Beneficiaries");*/

            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "6", "STATUS", "6: Check membership status");

            if (USSDAPI.fnCheckIfUserCanAccessReporting(theUSSDRequest)) {
                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "7", "SYSTEM_REPORTS", "7: System Reports");
            }
            //USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "5", "ENROLL_NEW_MEMBER", "5: Enroll New Member");

            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.MY_ACCOUNT_MENU, "NO", theArrayListUSSDSelectOption);

        } catch (Exception e) {
            System.err.println("theAppMenus.getMyAccountMenu() ERROR : " + e.getMessage());
        } finally {
            theAppMenus = null;
        }
        return theUSSDResponse;
    }


}
