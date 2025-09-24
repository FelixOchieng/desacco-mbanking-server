package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;
import ke.skyworld.lib.mbanking.ussd.USSDResponseSELECTOption;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.ArrayList;

public interface AccountRegistrationMenus {
    public default USSDResponse displayMenu_AccountRegistration(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader = "Enroll New Member";

        try{

            switch (theParam) {
                case "MENU": {
                    String strResponse = strHeader+"\nEnter Full Name:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME, USSDConstants.USSDInputType.STRING,"NO");
                    break;
                }
                case "NAME": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME.name());
                    if(!strUserInput.equalsIgnoreCase("")){
                        String strResponse = strHeader+"\nEnter Mobile Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid name}\nEnter The Person's Name:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "MOBILE_NUMBER": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER.name());
                    if(!strUserInput.equalsIgnoreCase("") && strUserInput.matches("[0-9]{9,13}$") && !APIUtils.sanitizePhoneNumber(strUserInput).equals("INVALID_MOBILE_NUMBER")){
                        String strResponse = strHeader+"\nEnter National ID Number / Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid mobile number}\nEnter Mobile Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "NATIONAL_ID_NUMBER": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER.name());

                    if(!strUserInput.equalsIgnoreCase("") && strUserInput.matches("[0-9]{5,15}$")){
                        String strResponse = strHeader+"\nEnter Date of Birth\nFormat: [DD MM YYYY]:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid ID number}\nEnter National ID Number / Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "DATE_OF_BIRTH": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH.name());

                    if(!strUserInput.equalsIgnoreCase("") && strUserInput.matches("^(?:(?:31(\\/|-|\\.|\\s)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.|\\s)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.|\\s)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.|\\s)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$")){
                        String strResponse = strHeader+"\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_PIN, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid date}\nEnter Date of Birth\nFormat: [DD MM YYYY]:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "PIN": {
                    String strLoginPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOGIN_PIN.name());
                    String strPIN = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_PIN.name());
                    if (strLoginPIN.equals(strPIN)) {
                        String strName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME.name());
                        String strMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER.name());
                        String strNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER.name());
                        String strDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH.name());

                        String strResponse = "Confirm " + strHeader + "\nName: " + strName + "\nMobile Number: " + strMobileNumber + "\n" + "National ID Number / Service Number: " + strNationalIDNumber + "\n" + "DOB: " + strDateOfBirth + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    } else {
                        String strResponse = strHeader + "\n{Please enter correct PIN}\nEnter your PIN:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest, strResponse, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_PIN, USSDConstants.USSDInputType.STRING, "NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_CONFIRMATION.name());

                    switch (strConfirmation){
                        case "YES":{
                            String strResponse;

                            APIConstants.AccountRegistrationReturnVal accountRegistrationReturnVal = theUSSDAPI.accountRegistration(theUSSDRequest);

                            switch (accountRegistrationReturnVal) {
                                case SUCCESS: {
                                    strResponse = strHeader+"\nThe member registration entry was created successfully.\nSelect an option below to proceed.\n";
                                    break;
                                }
                                case MEMBER_EXISTS: {
                                    strResponse = strHeader+"\nThe member already exists, please try again.\n";
                                    break;
                                }
                                case ENTRY_EXISTS: {
                                    strResponse = strHeader+"\nThe registration entry already exists, please try again.\n";
                                    break;
                                }
                                case PIN_MISMATCH: {
                                    strResponse = strHeader+"\nIncorrect PIN entered, please try again.\n";
                                    break;
                                }
                                case INVALID_PIN: {
                                    strResponse = strHeader+"\nInvalid PIN entered, please try again.\n";
                                    break;
                                }
                                case INVALID_FIRSTNAME: {
                                    strResponse = strHeader+"\nInvalid first name, please try again.\n";
                                    break;
                                }
                                case INVALID_LASTNAME: {
                                    strResponse = strHeader+"\nInvalid last name, please try again.\n";
                                    break;
                                }
                                case INVALID_IDNO: {
                                    strResponse = strHeader+"\nInvalid National ID Number / Service Number, please try again.\n";
                                    break;
                                }
                                case INVALID_DOB: {
                                    strResponse = strHeader+"\nInvalid date of birth, please try again.\n";
                                    break;
                                }
                                case ERROR: {
                                    strResponse = strHeader+"\nAn error occurred, please try again.\n";
                                    break;
                                }
                                default: {
                                    strResponse = strHeader+"\nSorry, this service is not available at the moment. Please try again.\n";
                                    break;
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO":{
                            String strResponse = "Dear member, your "+strHeader+" request NOT confirmed. "+strHeader+" request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithHomeAndExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NAME.name());
                            String strMobileNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_MOBILE_NUMBER.name());
                            String strNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_NATIONAL_ID_NUMBER.name());
                            String strDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.ACCOUNT_REGISTRATION_DATE_OF_BIRTH.name());

                            String strResponse = "Confirm " + strHeader + "\n{Select a valid menu}\nName: " + strName + "\nMobile Number: " + strMobileNumber + "\n" + "National ID Number / Service Number: " + strNationalIDNumber + "\n"+ "DOB: " + strDateOfBirth + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmation(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                case "END":{
                    String strResponse = "Self Registration\n{Select a valid option below}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);
                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_ChangePIN() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Self Registration\n{Sorry, an error has occurred while processing Self Registration}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    USSDResponseSELECTOption.setUSSDSelectOptionHOME(theArrayListUSSDSelectOption, AppConstants.USSDDataType.MAIN_IN_MENU.name());
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.ACCOUNT_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_ChangePIN() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }

    public default USSDResponse displayMenu_SelfRegistration(USSDRequest theUSSDRequest, String theParam) {
        USSDResponse theUSSDResponse = null;
        USSDAPI theUSSDAPI = new USSDAPI();
        AppMenus theAppMenus = new AppMenus();
        String strHeader = "Self Registration";

        try{

            switch (theParam) {
                case "ACTION": {
                    String strResponse = strHeader+"\nEnter Full Name:";
                    theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SELF_REGISTRATION_NAME, USSDConstants.USSDInputType.STRING,"NO");
                    break;
                }
                case "NAME": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NAME.name());
                    if(!strUserInput.equalsIgnoreCase("")){
                        String strResponse = strHeader+"\nEnter National ID Number / Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid name}\nEnter The Person's Name:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SELF_REGISTRATION_NAME, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "NATIONAL_ID_NUMBER": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER.name());

                    if(!strUserInput.equalsIgnoreCase("") && strUserInput.matches("[0-9]{5,15}$")){
                        String strResponse = strHeader+"\nEnter Date of Birth\nFormat: [DD MM YYYY]:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH, USSDConstants.USSDInputType.STRING,"NO");
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid ID number}\nEnter National ID Number / Service Number:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "DATE_OF_BIRTH": {
                    String strUserInput = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH.name());

                    if(!strUserInput.equalsIgnoreCase("") && strUserInput.matches("^(?:(?:31(\\/|-|\\.|\\s)(?:0?[13578]|1[02]))\\1|(?:(?:29|30)(\\/|-|\\.|\\s)(?:0?[13-9]|1[0-2])\\2))(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$|^(?:29(\\/|-|\\.|\\s)0?2\\3(?:(?:(?:1[6-9]|[2-9]\\d)?(?:0[48]|[2468][048]|[13579][26])|(?:(?:16|[2468][048]|[3579][26])00))))$|^(?:0?[1-9]|1\\d|2[0-8])(\\/|-|\\.|\\s)(?:(?:0?[1-9])|(?:1[0-2]))\\4(?:(?:1[6-9]|[2-9]\\d)?\\d{2})$")){
                        String strName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NAME.name());
                        String strMobileNumber = "+"+ theUSSDRequest.getUSSDMobileNo();
                        String strNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER.name());
                        String strDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH.name());

                        String strResponse = "Confirm " + strHeader + "\nName: " + strName + "\nMobile Number: " + strMobileNumber + "\n" + "National ID Number / Service Number: " + strNationalIDNumber + "\n" + "DOB: " + strDateOfBirth + "\n";

                        ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption = new ArrayList<USSDResponseSELECTOption>();
                        USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                        theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_CONFIRMATION, "NO", theArrayListUSSDSelectOption);
                    }else{
                        String strResponse = strHeader+"\n{Please enter a valid date}\nEnter Date of Birth\nFormat: [DD MM YYYY]:";
                        theUSSDResponse = theAppMenus.displayMenu_GeneralInput(theUSSDRequest,strResponse, AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH, USSDConstants.USSDInputType.STRING,"NO");
                    }
                    break;
                }
                case "CONFIRMATION": {
                    String strConfirmation = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_CONFIRMATION.name());

                    switch (strConfirmation){
                        case "YES":{
                            String strResponse;

                            APIConstants.AccountRegistrationReturnVal accountRegistrationReturnVal = theUSSDAPI.selfRegistration(theUSSDRequest);

                            switch (accountRegistrationReturnVal) {
                                case SUCCESS: {
                                    strResponse = strHeader+"\nThe member registration entry was created successfully.\nSelect an option below to proceed.\n";
                                    break;
                                }
                                case MEMBER_EXISTS: {
                                    strResponse = strHeader+"\nThe member already exists, please try again.\n";
                                    break;
                                }
                                case ENTRY_EXISTS: {
                                    strResponse = strHeader+"\nThe registration entry already exists, please try again.\n";
                                    break;
                                }
                                case PIN_MISMATCH: {
                                    strResponse = strHeader+"\nIncorrect PIN entered, please try again.\n";
                                    break;
                                }
                                case INVALID_PIN: {
                                    strResponse = strHeader+"\nInvalid PIN entered, please try again.\n";
                                    break;
                                }
                                case INVALID_FIRSTNAME: {
                                    strResponse = strHeader+"\nInvalid first name, please try again.\n";
                                    break;
                                }
                                case INVALID_LASTNAME: {
                                    strResponse = strHeader+"\nInvalid last name, please try again.\n";
                                    break;
                                }
                                case INVALID_IDNO: {
                                    strResponse = strHeader+"\nInvalid National ID Number / Service Number, please try again.\n";
                                    break;
                                }
                                case INVALID_DOB: {
                                    strResponse = strHeader+"\nInvalid date of birth, please try again.\n";
                                    break;
                                }
                                case ERROR: {
                                    strResponse = strHeader+"\nAn error occurred, please try again.\n";
                                    break;
                                }
                                default: {
                                    strResponse = strHeader+"\nSorry, this service is not available at the moment. Please try again.\n";
                                    break;
                                }
                            }

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        case "NO":{
                            String strResponse = "Dear user, your "+strHeader+" request NOT confirmed. "+strHeader+" request NOT COMPLETED.";
                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                        default:{
                            String strName = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NAME.name());
                            String strMobileNumber = "+"+ theUSSDRequest.getUSSDMobileNo();
                            String strNationalIDNumber = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_NATIONAL_ID_NUMBER.name());
                            String strDateOfBirth = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.SELF_REGISTRATION_DATE_OF_BIRTH.name());

                            String strResponse = "Confirm " + strHeader + "\n{Select a valid menu}\nName: " + strName + "\nMobile Number: " + strMobileNumber + "\n" + "National ID Number / Service Number: " + strNationalIDNumber + "\n"+ "DOB: " + strDateOfBirth + "\n";

                            ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                            USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                            theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithConfirmationWithoutHome(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_CONFIRMATION, "NO",theArrayListUSSDSelectOption);
                            break;
                        }
                    }

                    break;
                }

                case "END":{
                    String strResponse = "Self Registration\n{Select a valid option below}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);
                    break;
                }
                default:{
                    System.err.println("theAppMenus.displayMenu_ChangePIN() UNKNOWN PARAM ERROR : theParam = " + theParam);

                    String strResponse = "Self Registration\n{Sorry, an error has occurred while processing Self Registration}";
                    ArrayList<USSDResponseSELECTOption> theArrayListUSSDSelectOption  = new ArrayList<USSDResponseSELECTOption>();
                    USSDResponseSELECTOption.setUSSDSelectOption(theArrayListUSSDSelectOption, strResponse);
                    theUSSDResponse = theAppMenus.displayMenu_GeneralSelectWithExit(theUSSDRequest, AppConstants.USSDDataType.SELF_REGISTRATION_END, "NO",theArrayListUSSDSelectOption);

                    break;
                }
            }

        }
        catch(Exception e){
            System.err.println("theAppMenus.displayMenu_ChangePIN() ERROR : " + e.getMessage());
        }
        finally{
            theUSSDAPI = null;
            theAppMenus = null;
        }
        return theUSSDResponse;
    }
}
