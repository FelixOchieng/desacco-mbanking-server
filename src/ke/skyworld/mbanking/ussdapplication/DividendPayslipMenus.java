package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface DividendPayslipMenus {
    default USSDResponse displayMenu_DividendPayslip(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        AppMenus theAppMenus = new AppMenus();
        final USSDAPI theUSSDAPI = new USSDAPI();

        try {
            String strHeader = "Get Dividend Slip";

            switch (theParam) {

                case "MENU": {
                    String strResponse = strHeader + "\n";
                    strResponse = strResponse + "\nEnter Email Address:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT, USSDConstants.USSDInputType.STRING, "NO");

                    break;
                }

                case "EMAIL_INPUT": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT.name());

                    String strEmailRegex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
                    Pattern ptEmailPattern = Pattern.compile(strEmailRegex);
                    Matcher mtEmailMatcher = ptEmailPattern.matcher(strUserInput);

                    if (mtEmailMatcher.matches()) {
                        String strResponse = strHeader + "\n";
                        strResponse = strResponse + "Enter Year:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_YEAR_INPUT, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strHeader + "\n{Please enter a valid email address}\nEnter email address:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }

                case "YEAR_INPUT": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_YEAR_INPUT.name());

                    String strYearRegex = "^(19|20)\\d{2}$"; // Regex for year (1900–2099)
                    int currentYear = Calendar.getInstance().get(Calendar.YEAR); // Get current year

                    // Check if the input matches the regex and is not greater than the current year
                    if (strUserInput.matches(strYearRegex) && Integer.parseInt(strUserInput) <= currentYear) {
//                    if (mtEmailMatcher.matches()) {
                        String strResponse = strHeader + "\n";
                        strResponse = strResponse + "Enter Mobile Banking PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    } else {
                        String strResponse = strHeader + "\n{Please enter a valid Year}\nEnter Year:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_YEAR_INPUT, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }

                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {

                        String strEmailAddress = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT.name());
                        String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_YEAR_INPUT.name());

                        String strResponse = "Confirm Dividend Slip Request\nYear: " + strUserInput + "\nEmail To: " + strEmailAddress + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_CONFIRMATION, "NO", theArrayListUSSDSelectOption);

                    } else {
                        String strResponse = strHeader + "\n{Please enter a correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }

                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.DIVIDEND_PAYSLIP_CONFIRMATION.name());

                    switch (strConfirmation) {
                        case "YES": {
                            String strResponse = "Dear member, your dividend Slip request has been received successfully. Please wait shortly as it's being processed.";

                            new Thread(() -> {
                                theUSSDAPI.generateDividendsPayslipcCurrent(theUSSDRequest);
                            }).start();

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO": {
                            String strResponse = "Dear member, your request to get dividend Slip was NOT confirmed. Dividend payslip request NOT COMPLETED.\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_END, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                        default: {
                            String strResponse = strHeader + "\n{Select a valid menu}\nProceed to get your dividend Slip?\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                            break;
                        }
                    }
                    break;
                }

                default: {
                    String strResponse = strHeader + "\n{Select a valid menu}\n";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.DIVIDEND_PAYSLIP_EMAIL_INPUT, USSDConstants.USSDInputType.STRING, "NO");
                }

            }

        } catch (Exception e) {

            System.err.println("theAppMenus.displayMenu_ActivateMobileApp() ERROR : " + e.getMessage());

        } finally {

            theAppMenus = null;

        }
        return theUSSDResponse;
    }
}
