package ke.skyworld.mbanking.ussdapplication;

import ke.co.skyworld.smp.authentication_manager.MobileBankingCryptography;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.co.skyworld.smp.utility_items.security.HashUtils;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.USSDAPI;
import ke.skyworld.mbanking.ussdapi.USSDAPIConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;

public interface ChangePINMenus {
    public default USSDResponse displayMenu_ChangePIN(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();

        try {

            switch (theParam) {
                case "MENU": {
                    String strResponse = "Change PIN\nEnter your current PIN:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    break;
                }
                case "CURRENT_PIN": {
                    String strCurrentPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN.name());

                    TransactionWrapper<FlexicoreHashMap> isCorrectPINWrapper = theUSSDAPI.isCorrectPIN(theUSSDRequest, strCurrentPIN);
                    FlexicoreHashMap isCorrectPINMap = isCorrectPINWrapper.getSingleRecord();

                    if (isCorrectPINWrapper.hasErrors()) {
                        String strResponse = isCorrectPINMap.getStringValue("display_message");

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO", theArrayListUSSDSelectOption);

                    } else {

                        USSDAPIConstants.Condition isCorrectPIN = isCorrectPINMap.getValue("cbs_api_return_val");
                        if (isCorrectPIN == USSDAPIConstants.Condition.YES) {
                            String strResponse = "Change PIN\nEnter your new PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");

                        } else {
                            String strResponse = "Change PIN\n{Please enter a valid current PIN}\nEnter your current PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN, USSDConstants.USSDInputType.STRING, "NO");
                        }
                    }

                    break;
                }
                case "NEW_PIN": {
                    String strNewPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name());

                    if (strNewPIN.matches("^[0-9]{4,15}$")) {

                        if (theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CURRENT_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name()))) {
                            String strResponse = "Change PIN\n{New PIN should not be same as Current PIN}\nEnter your new PIN:";
                            theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");

                        } else {

                            TransactionWrapper<FlexicoreHashMap> currentUserWrapper = theUSSDAPI.getCurrentUserDetails(theUSSDRequest);
                            FlexicoreHashMap currentUserDetailsMap = currentUserWrapper.getSingleRecord();
                            FlexicoreHashMap mobileBankingDetailsMap = currentUserDetailsMap.getFlexicoreHashMap("mobile_register_details");

                            String previousPasswords = mobileBankingDetailsMap.getStringValueOrIfNull("previous_pins", "<PREVIOUS_PINS/>");

                            Document docPrevPasswords = XmlUtils.parseXml(previousPasswords);
                            NodeList allprevPasswordsList = null;

                            try {
                                allprevPasswordsList = XmlUtils.getNodesFromXpath(docPrevPasswords, "/PREVIOUS_PINS/PIN");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            boolean hasUsedPinBefore = false;

                            if (allprevPasswordsList != null) {

                                int intMin = 0;

                                if(allprevPasswordsList.getLength() > 5){
                                    intMin = 5;
                                }

                                System.out.println("\n\nTHE MIN: "+intMin+"\n\n");

                                for (int i = allprevPasswordsList.getLength() - 1; i >= intMin; i--) {
                                    System.out.println("\n\nTHE INDEX: "+i+"\n");
                                    Node node = allprevPasswordsList.item(i);
                                    if (node.getNodeType() != Node.ELEMENT_NODE) {
                                        System.out.println("\n\nHAS CONTINUED\n\n");
                                        continue;
                                    }

                                    Element element = (Element) node;
                                    System.out.println("\n\nPIN : "+element.getTextContent()+"\n\n");

                                    if (element.getTextContent().equalsIgnoreCase(MobileBankingCryptography.hashPIN(String.valueOf(theUSSDRequest.getUSSDMobileNo()), strNewPIN))) {
                                        hasUsedPinBefore = true;
                                        break;
                                    }
                                }
                            }

                            String strResponse;
                            if (hasUsedPinBefore) {
                                strResponse = "Change PIN\n{Please provide a new PIN that you have not used before}\nEnter your new PIN:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");

                            } else {
                                strResponse = "Change PIN\nConfirm your new PIN:";
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING, "NO");

                            }
                        }
                    } else {
                        String strResponse = "Change PIN\n{Please enter a valid PIN}\nEnter your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }

                    break;
                }
                case "CONFIRM_PIN": {

                    if (theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN.name()).equals(theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.CHANGE_PIN_NEW_PIN.name()))) {

                        TransactionWrapper<FlexicoreHashMap> changePINWrapper = theUSSDAPI.changeUserPIN(theUSSDRequest);
                        FlexicoreHashMap changePINMap = changePINWrapper.getSingleRecord();
                        if (changePINWrapper.hasErrors()) {
                            USSDAPIConstants.Condition endSession = changePINMap.getValue("end_session");
                            String strResponse = changePINMap.getStringValue("display_message");

                            if (endSession == USSDAPIConstants.Condition.YES) {
                                ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                                USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                                theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO", theArrayListUSSDSelectOption);

                            } else {
                                theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING, "NO");
                            }

                        } else {

                            String strResponse = "Change PIN\nYour new PIN has been changed successfully. Select an option below to proceed.\n";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, "00", "_LINK", AppConstants.USSDDataType.INIT.name(), "00: Login");
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO", theArrayListUSSDSelectOption);

                        }

                    } else {
                        String strResponse = "Change PIN\n{New PIN mismatch}\nConfirm your new PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.CHANGE_PIN_CONFIRM_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }

                case "END": {
                    String strResponse = "Change PIN\n{Select a valid option below}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO", theArrayListUSSDSelectOption);
                    break;
                }
                default: {
                    System.err.println("theAppMenus.displayMenu_ChangePIN() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Change PIN\n{Sorry, an error has occurred while processing Change PIN}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.CHANGE_PIN_END, "NO", theArrayListUSSDSelectOption);

                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("theAppMenus.displayMenu_ChangePIN() ERROR : " + e.getMessage());
        } finally {
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
