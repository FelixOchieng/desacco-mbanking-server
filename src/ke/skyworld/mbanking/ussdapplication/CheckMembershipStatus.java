package ke.skyworld.mbanking.ussdapplication;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.nav.cbs.DeSaccoCBS;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;

public interface CheckMembershipStatus {
    public default USSDResponse displayMenu_CheckAccountStatus(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            switch (theParam) {
                case "MENU": {
                    String strResponse = "Membership status\nEnter PIN:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ACCOUNT_STATUS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    break;
                }
                case "PIN": {
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_STATUS_PIN.name());

                    if (strPIN.matches("^[0-9]{4,15}$")) {

                        if (theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name()).equals(strPIN)) {

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                            String strResponse = "Membership status\nKindly confirm you would like to check your Membership status";
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_STATUS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        } else {
                            String strResponse = "Membership status\n{Wrong PIN}\nEnter PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ACCOUNT_STATUS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    } else {
                        String strResponse = "Membership status\n{Please enter a valid PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ACCOUNT_STATUS_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_STATUS_CONFIRMATION.name());

                    switch (strConfirmation) {
                        case "YES": {

                            String strResponse = "Membership status\nAccount Details:\n";

                            TransactionWrapper memberDetailsWrapper = DeSaccoCBS.getMemberDetails("MSISDN", String.valueOf(theUSSDRequest.getUSSDMobileNo()));

                            if (!memberDetailsWrapper.hasErrors()) {
                                FlexicoreHashMap memberDetailsMap = memberDetailsWrapper.getSingleRecord();
                                strResponse += "Name: " + memberDetailsMap.getStringValue("full_name") + "\n";
                                strResponse += "Member Number: " + memberDetailsMap.getStringValue("identifier") + "\n";
                                strResponse += "Service Number: " + memberDetailsMap.getStringValue("service_no") + "\n";
                                strResponse += "Phone Number: " + theUSSDRequest.getUSSDMobileNo();
                                //strResponse += "Balance: " + accountStatus.get("Balance");
                            }else{
                                strResponse += "Account Details not found. Please try again later";
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_STATUS_END, "NO", theArrayListUSSDSelectOption);

                            break;
                        }
                        case "NO": {
                            String strResponse = "Membership status\nYour Membership status request has been stopped successfully";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ACCOUNT_STATUS_END, USSDConstants.USSDInputType.STRING, "NO");
                            break;
                        }
                        default: {

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<>();
                            String strResponse = "Account status\n{Select a valid menu}\nKindly confirm you would like to check your Membership status";
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_STATUS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                case "END": {
                    String strResponse = "Membership status\n{Select a valid option below}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_STATUS_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_CheckAccountStatus() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Membership status\n{Sorry, an error has occurred while processing Account status}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_STATUS_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_CheckAccountStatus() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}