package ke.skyworld.mbanking.ussdapplication;


import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.pesaapi.APIConstants;
import ke.skyworld.mbanking.pesaapi.PESAAPI;

import ke.skyworld.mbanking.pesaapi.PESAAPIConstants;
import ke.skyworld.mbanking.pesaapi.PesaParam;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;


public interface BuyGoodsMenu {
    default USSDResponse displayMenu_BuyGoodsMenus(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        String strMobileNumber = String.valueOf(theUSSDRequest.getUSSDMobileNo());

        try {
            String strHeader = "Lipa Na DESACCO";

            switch (theParam) {
                case "MENU": {
                    String strResponse = strHeader + "\n";
                    strResponse = strResponse + "Enter Business Short Code:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    break;
                }

                case "ACCOUNT": {

                    String strShortCode = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.BUY_GOODS_ACCOUNT.name());

                    HashMap<String, String> businessDetails = USSDAPI.getBusinessDetails(strShortCode);

                    String accountNo = "";
                    String AccountCode = "";
                    String strBusinessName = "";
                    String strBusinessPhone = "";


                    if (!businessDetails.isEmpty()) {

                        InMemoryCache.store("Business Details", businessDetails, 300);

                        accountNo = businessDetails.get("ACCOUNT_NO");
                        AccountCode = businessDetails.get("ACCOUNT_CODE");
                        strBusinessName = businessDetails.get("BUSINESS_NAME");
                        strBusinessPhone = businessDetails.get("BUSINESS_PHONE");

                        String strResponse = strHeader + "\n" +
                                "Business Name: " + strBusinessName + "\n" +
                                "Short Code: " + AccountCode + "\n" +
                                "Enter Amount: ";


                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");


                    } else {
                        String strResponse = strHeader + "\n{Error validating Business Short Code. Please retry}\nEnter Business Short Code:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_ACCOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;

                }

                case "AMOUNT": {

                    String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.BUY_GOODS_AMOUNT.name());
                    if (strAmount.matches("^[1-9][0-9]*$")) {
                        strAmount = Utils.formatDouble(strAmount, "#,###");

                        LinkedHashMap<String, String> businessDetails = (LinkedHashMap<String, String>) InMemoryCache.retrieve("Business Details");

                        String AccountCode = businessDetails.get("ACCOUNT_CODE");
                        String strBusinessName = businessDetails.get("BUSINESS_NAME");

                        String strResponse = "Confirm " + strHeader + "\n" +
                                "Business Name: " + strBusinessName + "\n" +
                                "Short Code: " + AccountCode + "\n" +
                                "Amount: KES " + strAmount + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.BUY_GOODS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                        String strDepositMinimum = "1";
                        String strDepositMaximum = "15";

                        double dblDepositMinimum = Double.parseDouble(strDepositMinimum);
                        double dblDepositMaximum = Double.parseDouble(strDepositMaximum);

                        double dblAmountEntered = Double.parseDouble(strAmount);

                        if (dblAmountEntered < dblDepositMinimum) {
                            strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strDepositMinimum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }
                        if (dblAmountEntered > dblDepositMaximum) {
                            strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strDepositMaximum, "#,###.##") + "}\nEnter amount:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                        }

                    } else {
                        String strResponse = strHeader + "\n{Please enter a valid amount}\nEnter amount:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DEPOSIT_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "CONFIRMATION": {

                    PesaParam pesaParam = PESAAPI.getPesaParam(MBankingConstants.ApplicationType.PESA, PESAAPIConstants.PESA_PARAM_TYPE.MPESA_C2B);

                    String strSender = pesaParam.getSenderIdentifier();

                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.BUY_GOODS_CONFIRMATION.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "";
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.BUY_GOODS_AMOUNT.name());

                            LinkedHashMap<String, String> businessDetails = (LinkedHashMap<String, String>) InMemoryCache.retrieve("Business Details");

                            String AccountCode = businessDetails.get("ACCOUNT_CODE");
                            String strBusinessName = businessDetails.get("BUSINESS_NAME");

                            if (theUSSDRequest.getUSSDProviderCode() == AppConstants.USSDProvider.SAFARICOM.getValue()) {

                                strResponse = "You will be prompted by M-PESA for payment\nPaybill no: " + strSender + " \nA / C:" + AccountCode + "\n " + " Amount: KES " + strAmount + "\n ";

                                String strOriginatorID = String.valueOf(theUSSDRequest.getUSSDSessionID());
                                String strBeneficiaryMobileNo = String.valueOf(theUSSDRequest.getUSSDMobileNo());
                                double dblAmount = Utils.stringToDouble(strAmount);
                                String strTraceID = theUSSDRequest.getUSSDTraceID();

                                Thread worker = new Thread(() -> {

                                    PESAAPI thePESAAPI = new PESAAPI();
                                    thePESAAPI.pesa_C2B_Request(
                                            String.valueOf(theUSSDRequest.getUSSDMobileNo()),
                                            strBusinessName,
                                            strTraceID,
                                            "USSD",
                                            AccountCode,
                                            strBusinessName,
                                            "MBANKING_SERVER",
                                            strBeneficiaryMobileNo,
                                            strBeneficiaryMobileNo,
                                            strBusinessName,
                                            AccountCode,
                                            dblAmount,
                                            "PIN_RESET");
                                });
                                worker.start();


                            } else {
                                strResponse = "Use the details below to pay via M-PESA\nPaybill no: " + strSender + " \n" + "A/C: " + AccountCode + "\n" + "Amount: KES " + strAmount + "\n";
                            }

                            //End USSD.
                            theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");

                            break;
                        }
                        case "NO": {
                            String strResponse = "Dear member, your " + strHeader + " request NOT confirmed. " + strHeader + " request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralDisplay(theUSSDRequest, strResponse, "NO");
                            break;
                        }
                        default: {
                            String strAmount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.BUY_GOODS_AMOUNT.name());
                            if (strAmount.matches("^[1-9][0-9]*$")) {
                                strAmount = Utils.formatDouble(strAmount, "#,###");


                                LinkedHashMap<String, String> businessDetails = (LinkedHashMap<String, String>) InMemoryCache.retrieve("Business Details");

                                String AccountCode = businessDetails.get("ACCOUNT_CODE ");
                                String strBusinessName = businessDetails.get("BUSINESS_NAME");

                                String strResponse = "{Invalid input}\nConfirm " + strHeader + "\n" +
                                        "Business Name: " + strBusinessName + "\n" +
                                        "Short Code: " + AccountCode + "\n" +
                                        "Amount: KES " + strAmount + "\n";

                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.BUY_GOODS_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                                String strDepositMinimum = "1";
                                String strDepositMaximum = "10";

                                double dblDepositMinimum = Double.parseDouble(strDepositMinimum);
                                double dblDepositMaximum = Double.parseDouble(strDepositMaximum);

                                double dblAmountEntered = Double.parseDouble(strAmount);

                                if (dblAmountEntered < dblDepositMinimum) {
                                    strResponse = strHeader + "\n{MINIMUM amount allowed is KES " + Utils.formatDouble(strDepositMinimum, "#,###.##") + "}\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }
                                if (dblAmountEntered > dblDepositMaximum) {
                                    strResponse = strHeader + "\n{MAXIMUM amount allowed is KES " + Utils.formatDouble(strDepositMaximum, "#,###.##") + "}\nEnter amount:";
                                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.BUY_GOODS_AMOUNT, USSDConstants.USSDInputType.STRING, "NO");
                                }

                            }
                        }
                        break;
                    }
                    break;
                }


                default: {

                    System.err.println("theAppMenus.displayMenu_BuyGoodsMenus() UNKNOWN PARAM ERROR : theParam = " + theParam);

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
