package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.pesaapi.APIConstants;
import ke.skyworld.mbanking.pesaapi.PESAAPI;

import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static ke.skyworld.mbanking.nav.cbs.CBSAPI.getAccountTransferRecipientXML;

public interface ErroneousTransactions {
    default USSDResponse displayMenu_ErroneousTransactions(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

        try {


            /*
            MENU
            TRANSACTION
            ACCOUNT
            CONFIRMATION
            END
            */

            String strHeader = "Erroneous Deposit Correction.";

            switch (theParam) {
                case "MENU": {
                    String strResponse = strHeader + "\n";
                    strResponse = strResponse + "Select transaction:";
                    theUSSDResponse = GeneralMenus.displayMenu_ErroneousTransactions(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_TRANSACTION);
                    break;
                }

                case "TRANSACTION": {

                    String strTransaction = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ERRONEOUS_TRANSACTION.name());

                    if (!strTransaction.isBlank()) {

                        String strResponse = strHeader + "\nEnter Correct Account: ";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");


                    } else {
                        String strResponse = strHeader + "\n";
                        strResponse = strResponse + "{Select A Valid Option}\nSelect transaction:";
                        theUSSDResponse = GeneralMenus.displayMenu_ErroneousTransactions(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_TRANSACTION);
                        break;
                    }
                    break;

                }

                case "ACCOUNT": {

                    String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ERRONEOUS_ACCOUNT.name());
                    if (!strAccount.isBlank()) {


                        String accountExists = getAccountTransferRecipientXML(strAccount, "ACCOUNT");

                        if (!accountExists.isBlank()) {

                            String strResponse = strHeader + "\nKindly confirm that " + strAccount + " is the correct account number ";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.ERRONEOUS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                        } else {
                            String strResponse = strHeader + "\n{Account does not exist}\nEnter Correct Account: ";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }

                    } else {
                        String strResponse = strHeader + "\n{Invalid Account}\nEnter Correct Account: ";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");

                    }
                    break;
                }

                case "CONFIRMATION": {

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ERRONEOUS_CONFIRMATION.name());
                    String strTransactionID = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ERRONEOUS_TRANSACTION.name());

                    switch (strConfirmation) {
                        case "YES": {

                            //Update Transaction
                            LinkedHashMap<String, String> response = GeneralMenus.displayMenu_UpdateErroneousTransactions(theUSSDRequest, strTransactionID, theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ERRONEOUS_ACCOUNT.name()), strHeader + "\nTransaction updated successfully", AppConstants.USSDDataType.ERRONEOUS_END);

                            if (!response.isEmpty()) {
                                if (response.get("status").equalsIgnoreCase("SUCCESS")) {
                                    String strResponse = strHeader + "\n" + response.get("description");
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                                } else {
                                    String strResponse = "Erroneous Deposit Correction Service" + "\n{Invalid Account}\nEnter Correct Account: ";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }

                            } else {
                                String strResponse = strHeader + "\n{Invalid Account}\nEnter Correct Account: ";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ERRONEOUS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                            }

                            break;
                        }
                        case "NO": {

                            String strResponse = "Your Erroneous Deposit Correction request has NOT been confirmed. Request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                            break;
                        }
                        default: {
                            String strAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ERRONEOUS_ACCOUNT.name());
                            String accountExists = getAccountTransferRecipientXML(strAccount, "ACCOUNT");
                            if (!accountExists.isBlank()) {
                                String strResponse = strHeader + "\n{Select a valid option}\nKindly confirm that " + strAccount + " is the correct account number ";
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.ERRONEOUS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            }
                        }
                        break;
                    }
                    break;
                }

                case "END": {

                    String strResponse = "Confirm " + strHeader + "\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ERRONEOUS_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }

                default: {

                    System.err.println("theAppMenus.displayMenu_ErroneousTransactions() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = strHeader + "\n{Sorry, an error has occurred while processing your request}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("theAppMenus.displayMenu_BuyGoodsMenus() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }

        return theUSSDResponse;
    }
}
