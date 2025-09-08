package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface TermsAndConditionsMenus {
    default USSDResponse displayMenu_TermsAndConditionsMenus(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        final USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        try{
            String strHeader = "Loans T&Cs";

            switch (theParam) {
                case "MENU": {
                    theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_CATEGORY);
                    break;
                }
                case "CATEGORY": {
                    String strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_CATEGORY.name());

                    if (!strLoanApplicationCategory.equals("")) {
                        strHeader += "\nSelect a loan";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, strLoanApplicationCategory, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_TYPE, strLoanAccount);
                    } else {
                        strHeader += "\n{Select a valid menu}";
                        theUSSDResponse = GeneralMenus.displayMenu_LoanCategories(theUSSDRequest, theParam, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_CATEGORY);
                    }
                    break;
                }
                case "TYPE": {
                    String strLoanApplicationType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_TYPE.name());

                    if (!strLoanApplicationType.equals("")) {
                        String strResponse = "Select mode of delivery";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "SMS", "1: Send via SMS");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "EMAIL", "2: Send via E-mail");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_DELIVERY, "NO",theArrayListUSSDSelectOption);
                        break;
                    } else {
                        String strLoanApplicationCategory = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_CATEGORY.name());
                        strHeader += "\n{Select a valid menu}";
                        String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                        theUSSDResponse = GeneralMenus.displayMenu_LoanTypes(theUSSDRequest, strLoanApplicationCategory, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_TYPE, strLoanAccount);
                    }

                    break;
                }
                case "DELIVERY": {
                    String strDelivery = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_DELIVERY.name());
                    String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_TYPE.name());

                    String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                    String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);

                    if(!strDelivery.equals("")){
                        if(strDelivery.equals("SMS")){
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();

                            strHeader += "\nAn SMS message with terms & conditions URL for "+strLoanName+" has been sent to "+theUSSDRequest.getUSSDMobileNo()+"\n";
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest,AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_END, "NO", theArrayListUSSDSelectOption);

                            Thread worker = new Thread(() -> {
                                theUSSDAPI.sendLoansTnCsViaSMS(theUSSDRequest, strLoanType, strLoanName);
                            });
                            worker.start();
                        } else if (strDelivery.equals("EMAIL")){
                            String strEmail = USSDAPI.getMemberEmailAddress(theUSSDRequest);
                            if(!strEmail.equals("")){
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();

                                strHeader += "\nAn e-mail message with terms & conditions document for "+strLoanName+" has been sent to "+strEmail+"\n";
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest,AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_END, "NO", theArrayListUSSDSelectOption);

                                Thread worker = new Thread(() -> {
                                    theUSSDAPI.sendLoansTnCsViaEmail(theUSSDRequest, strEmail, strLoanType, strLoanName);
                                });
                                worker.start();
                            } else {
                                strHeader += "\nEnter your e-mail address";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_EMAIL, USSDConstants.USSDInputType.STRING, "NO");
                            }
                        }
                    }else{
                        strHeader += "Select mode of delivery";
                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();

                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "1", "SMS", "1: Send via SMS");
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "2", "EMAIL", "2: Send via E-mail");
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_DELIVERY, "NO",theArrayListUSSDSelectOption);
                        break;
                    }

                    break;
                }
                case "EMAIL": {
                    String strEmail = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_EMAIL.name());
                    if(!strEmail.equals("")){
                        String strEmailRegex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
                        Pattern ptEmailPattern = Pattern.compile(strEmailRegex);
                        Matcher mtEmailMatcher = ptEmailPattern.matcher(strEmail);

                        if(mtEmailMatcher.matches()) {
                            String strLoanType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_TYPE.name());

                            String strLoanTypeXML = APIUtils.getLoanTypeFromMemory(strLoanType, theUSSDRequest);
                            String strLoanName = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Type", strLoanTypeXML);

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();

                            strHeader += "\nAn e-mail message with terms & conditions document for "+strLoanName+" has been sent to "+strEmail+"\n";
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest,AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_END, "NO", theArrayListUSSDSelectOption);

                            Thread worker = new Thread(() -> {
                                theUSSDAPI.sendLoansTnCsViaEmail(theUSSDRequest, strEmail, strLoanType, strLoanName);
                            });
                            worker.start();
                        } else {
                            strHeader += "\n{Enter a valid e-mail address}\nEnter your e-mail address:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_EMAIL, USSDConstants.USSDInputType.STRING,"NO");
                        }
                    }else{
                        strHeader = "\n{Enter a valid email address}\nEnter your e-mail address";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strHeader, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_EMAIL, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "END": {
                    String strResponse = "Set PIN\n{Invalid menu selected}\nPlease select an option below\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    //LINK OPTION - Force user to login after error at the end.
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(),"00: Login");
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_TermsAndConditionsMenus() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    strHeader += "\n{Sorry, an error has occurred while processing your request}\n";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strHeader);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.LOAN_TERMS_AND_CONDITIONS_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }
        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_TermsAndConditionsMenus() ERROR : " + e.getMessage());
        }
        finally{
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
