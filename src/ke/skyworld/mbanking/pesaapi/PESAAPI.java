package ke.skyworld.mbanking.pesaapi;

import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.pesa.*;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.lib.mbanking.utils.Utils;
import ke.skyworld.mbanking.mappapi.MAPPAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import ke.skyworld.mbanking.ussdapplication.HomeMenus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import static ke.skyworld.mbanking.nav.cbs.CBSAPI.CBS_ERROR;
import static ke.skyworld.mbanking.ussdapi.APIUtils.*;

public class PESAAPI {
    public static void confirmPESA_IN(PESA thePESAIN, PESAINResponse thePESAINResponse) {
        String strResponse = "";
        String destinationMobileNumber = "";
        try {

            System.out.println("********************************************************");
            System.out.println("                 CONFIRM PESA IN");
            System.out.println("********************************************************");
            System.out.println("TraceID               : " + thePESAIN.getTraceID());
            System.out.println("OriginatorID          : " + thePESAIN.getOriginatorID());
            System.out.println("Command               : " + thePESAIN.getCommand());
            System.out.println("Category              : " + thePESAIN.getCategory());
            System.out.println("ProductID             : " + thePESAIN.getProductID());
            System.out.println("BatchReference        : " + thePESAIN.getBatchReference());
            System.out.println("TransactionRemark     : " + thePESAIN.getTransactionRemark());

            System.out.println("InitiatorAccount      : " + thePESAIN.getInitiatorAccount());
            System.out.println("InitiatorIdentifier   : " + thePESAIN.getInitiatorIdentifier());
            System.out.println("InitiatorAccount      : " + thePESAIN.getInitiatorAccount());
            System.out.println("InitiatorName         : " + thePESAIN.getInitiatorName());
            System.out.println("InitiatorReference    : " + thePESAIN.getInitiatorReference());

            System.out.println("SourceType            : " + thePESAIN.getSourceType());
            System.out.println("SourceIdentifier      : " + thePESAIN.getSourceIdentifier());
            System.out.println("SourceAccount         : " + thePESAIN.getSourceAccount());
            System.out.println("SourceName            : " + thePESAIN.getSourceName());
            System.out.println("SourceReference       : " + thePESAIN.getSourceReference());

            System.out.println("SenderType            : " + thePESAIN.getSenderType());
            System.out.println("SenderIdentifier      : " + thePESAIN.getSenderIdentifier());
            System.out.println("SenderAccount         : " + thePESAIN.getSenderAccount());
            System.out.println("SenderName            : " + thePESAIN.getSenderName());
            System.out.println("SenderReference       : " + thePESAIN.getSenderReference());

            System.out.println("ReceiverType          : " + thePESAIN.getReceiverType());
            System.out.println("ReceiverIdentifier    : " + thePESAIN.getReceiverIdentifier());
            System.out.println("ReceiverAccount       : " + thePESAIN.getReceiverAccount());
            System.out.println("ReceiverName          : " + thePESAIN.getReceiverName());
            System.out.println("ReceiverReference     : " + thePESAIN.getReceiverReference());

            System.out.println("BeneficiaryType       : " + thePESAIN.getBeneficiaryType());
            System.out.println("BeneficiaryIdentifier : " + thePESAIN.getBeneficiaryIdentifier());
            System.out.println("BeneficiaryAccount    : " + thePESAIN.getBeneficiaryAccount());
            System.out.println("BeneficiaryName       : " + thePESAIN.getBeneficiaryName());
            System.out.println("BeneficiaryReference  : " + thePESAIN.getBeneficiaryReference());
            System.out.println("*******************************************************");
            System.out.println();

            thePESAINResponse.setCategory("MPESA_C2B_DEPOSIT");

            String strPESAINCategory = thePESAIN.getCategory();
            if (strPESAINCategory.equalsIgnoreCase("COOPBANK_PAY_TO_FOSA") ||
                    strPESAINCategory.equalsIgnoreCase("COOPBANK_B2B_CONFIRMATION")) {
                thePESAINResponse.setCategory(strPESAINCategory);
            }

            String entryCode = thePESAIN.getOriginatorID();
            String transactionID = thePESAIN.getSourceReference();
            String transaction = "Paybill";
            String description = "Paybill - " + thePESAIN.getSenderIdentifier() + " " + thePESAIN.getSenderName();

            if (thePESAIN.getCommand().equals("PesaLink")) {
                transaction = "Bank Deposit";
                description = "PL|IN|" + thePESAIN.getSourceIdentifier() + "|" + thePESAIN.getBeneficiaryIdentifier();
            }

            String accountNo = thePESAIN.getReceiverAccount();
            if (accountNo.equals("")) {
                accountNo = thePESAIN.getSourceIdentifier();
            }
            String strBeneficiaryName = thePESAIN.getBeneficiaryName();
            String strReceiverName = thePESAIN.getReceiverName();

            double amount = thePESAIN.getTransactionAmount();
            String loanNo = "";
            String phoneNo = thePESAIN.getSenderIdentifier();
            String keyWord = "";

            //todo: to change this to use service code - to determine source i.e: coop bank, m-pesa etc.
            if (strPESAINCategory.equalsIgnoreCase("COOPBANK_PAY_TO_FOSA")) {
                accountNo = thePESAIN.getBeneficiaryAccount();
                phoneNo = "";
                if (accountNo.length() > 4) {
                    accountNo = accountNo.substring(4);
                    accountNo = getStringBeforeSpace(accountNo);
                }
                description = MBankingXMLFactory.getXPathValueFromXMLString("/PESA_OTHER_DETAILS/PROVIDER_DETAILS/Narration", thePESAIN.getPESAXMLData());
                if (description == null) {
                    description = "";
                }

                strBeneficiaryName = MBankingXMLFactory.getXPathValueFromXMLString("/PESA_OTHER_DETAILS/PROVIDER_DETAILS/CustMemoLine2", thePESAIN.getPESAXMLData());
                if (strBeneficiaryName == null) {
                    strBeneficiaryName = thePESAIN.getBeneficiaryName();
                }
                transaction = "CoopDeposit";
                transactionID = thePESAIN.getOriginatorID();
                description = thePESAIN.getSourceReference();
            } else if (strPESAINCategory.equalsIgnoreCase("COOPBANK_B2B_CONFIRMATION")) {
                phoneNo = "";
                accountNo = thePESAIN.getBeneficiaryAccount();
                transactionID = thePESAIN.getOriginatorID();
                description = thePESAIN.getSourceReference();

                transaction = "CoopDeposit";
                description = "COOP B2B|IN|" + thePESAIN.getSourceIdentifier() + "|" + thePESAIN.getBeneficiaryIdentifier();
            } else {

                String strMobileNumber = thePESAIN.getInitiatorIdentifier();
                String strAccountNumber = thePESAIN.getReceiverAccount();

                String strKey = strMobileNumber + "-PIN-RESET-" + strAccountNumber;

                if (InMemoryCache.exists(strKey)) {

                    double dbExpectedAmount = (double) InMemoryCache.retrieve(strKey);

                    if (thePESAIN.getTransactionAmount() >= (dbExpectedAmount)) {

                        accountNo = AppConstants.strLiveCollectionAccount;

                        destinationMobileNumber = "RESET_PIN";

                        String strNewPin = HomeMenus.generateRandomNumberAsString(4);

                        String strNewHashedPIN = hashPIN(strNewPin, strMobileNumber);

                        String strChangePinStatus = CBSAPI.resetPin(strMobileNumber, strNewHashedPIN);

                        if (strChangePinStatus.equals("SUCCESS")) {
                            String strMSG = "Dear Member, your PIN has successfully been reset. Your new PIN is " + strNewPin;

                            fnSendSMS(strMobileNumber, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "PIN_RESET",
                                    "USSD", "MBANKING_SERVER",
                                    UUID.randomUUID().toString(),
                                    UUID.randomUUID().toString());
                        } else {
                            String strMSG = "Dear Member, your PIN reset request has FAILED. Kindly try again later.";

                            fnSendSMS(strMobileNumber, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "PIN_RESET",
                                    "USSD", "MBANKING_SERVER",
                                    UUID.randomUUID().toString(),
                                    UUID.randomUUID().toString());
                        }

                        InMemoryCache.remove(strKey);
                    }
                }
            }

            if (description.length() > 50) {
                description = description.substring(0, 49);
            }

            if (accountNo.length() > 20) {
                accountNo = accountNo.substring(0, 20);
            }

            boolean isOtherNumber = false;

            XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

            String strTransactionStatus = "";

            if (phoneNo.length() > 20) {
                phoneNo = "";

                strTransactionStatus = CBSAPI.insertMpesaTransaction(entryCode, transactionID, xmlGregorianCalendar,
                        transaction, description, trimString(accountNo, 50), BigDecimal.valueOf(amount), phoneNo, "", "USSD",
                        transactionID, "MBANKING", accountNo, thePESAIN.getBeneficiaryName(), thePESAIN.getReceiverName(),
                        isOtherNumber, destinationMobileNumber);
            } else {
                strTransactionStatus = CBSAPI.insertMpesaTransaction(entryCode, transactionID, xmlGregorianCalendar,
                        transaction, description, trimString(accountNo, 50), BigDecimal.valueOf(amount), phoneNo, "", "USSD",
                        transactionID, "MBANKING", accountNo, thePESAIN.getBeneficiaryName(), thePESAIN.getReceiverName(),
                        isOtherNumber, destinationMobileNumber);
            }

            //String strTransactionStatus = CBSAPI.insertMpesaTransaction(entryCode, transactionID, xmlGregorianCalendar, transaction, description, accountNo, BigDecimal.valueOf(amount), phoneNo, "", "USSD", transactionID, "MBANKING", accountNo, thePESAIN.getBeneficiaryName(), thePESAIN.getReceiverName(), isOtherNumber,"");

            String strTransactionResponseStatus = getResponseStatus(strTransactionStatus);
            String strTransactionIDFromNavision = getResponseTransactionID(strTransactionStatus);

            strTransactionResponseStatus = (strTransactionResponseStatus != null) ? strTransactionResponseStatus : "";

            if (strTransactionResponseStatus.equalsIgnoreCase("SUCCESS")) {
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.PROCESSED.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                thePESAINResponse.setResponseDescription("NAV RESPONSE: " + strTransactionResponseStatus);
            } else if (strTransactionResponseStatus.equalsIgnoreCase("TRANSACTION_EXISTS")) {
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.PROCESSED.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                thePESAINResponse.setResponseDescription("NAV RESPONSE: DUPLICATE - " + strTransactionResponseStatus);
                thePESAINResponse.setChargeApplied(0.00);
            } else {
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                thePESAINResponse.setResponseDescription("NAV RESPONSE: " + strTransactionResponseStatus);
                thePESAINResponse.setChargeApplied(0.00);
            }

            strResponse = (transactionID != null) ? transactionID : "";
            thePESAINResponse.setBeneficiaryReference(strTransactionIDFromNavision);
            thePESAINResponse.setBeneficiaryApplication("CBS");
        } catch (Exception e) {
            System.err.println("PESAAPI.confirmPESA_IN() ERROR : " + e.getMessage());
            thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
            thePESAINResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            thePESAINResponse.setResponseDescription("System Exception error confirmPESA_IN(): " + e.getMessage() + ". Transaction NOT Accepted");
            thePESAINResponse.setChargeApplied(0.00);
        }
    }

    public static void validatePESA_IN(PESA thePESAIN, PESAINResponse thePESAINResponse) {
        String strResponse = "";
        try {

            try {
                System.out.println("********************************************************");
                System.out.println("                 VALIDATE PESA IN");
                System.out.println("********************************************************");
                System.out.println("TraceID               : " + thePESAIN.getTraceID());
                System.out.println("OriginatorID          : " + thePESAIN.getOriginatorID());
                System.out.println("Command               : " + thePESAIN.getCommand());
                System.out.println("Category              : " + thePESAIN.getCategory());
                System.out.println("ProductID             : " + thePESAIN.getProductID());
                System.out.println("BatchReference        : " + thePESAIN.getBatchReference());
                System.out.println("TransactionRemark     : " + thePESAIN.getTransactionRemark());

                System.out.println("InitiatorAccount      : " + thePESAIN.getInitiatorAccount());
                System.out.println("InitiatorIdentifier   : " + thePESAIN.getInitiatorIdentifier());
                System.out.println("InitiatorAccount      : " + thePESAIN.getInitiatorAccount());
                System.out.println("InitiatorName         : " + thePESAIN.getInitiatorName());
                System.out.println("InitiatorReference    : " + thePESAIN.getInitiatorReference());

                System.out.println("SourceType            : " + thePESAIN.getSourceType());
                System.out.println("SourceIdentifier      : " + thePESAIN.getSourceIdentifier());
                System.out.println("SourceAccount         : " + thePESAIN.getSourceAccount());
                System.out.println("SourceName            : " + thePESAIN.getSourceName());
                System.out.println("SourceReference       : " + thePESAIN.getSourceReference());

                System.out.println("SenderType            : " + thePESAIN.getSenderType());
                System.out.println("SenderIdentifier      : " + thePESAIN.getSenderIdentifier());
                System.out.println("SenderAccount         : " + thePESAIN.getSenderAccount());
                System.out.println("SenderName            : " + thePESAIN.getSenderName());
                System.out.println("SenderReference       : " + thePESAIN.getSenderReference());

                System.out.println("ReceiverType          : " + thePESAIN.getReceiverType());
                System.out.println("ReceiverIdentifier    : " + thePESAIN.getReceiverIdentifier());
                System.out.println("ReceiverAccount       : " + thePESAIN.getReceiverAccount());
                System.out.println("ReceiverName          : " + thePESAIN.getReceiverName());
                System.out.println("ReceiverReference     : " + thePESAIN.getReceiverReference());

                System.out.println("BeneficiaryType       : " + thePESAIN.getBeneficiaryType());
                System.out.println("BeneficiaryIdentifier : " + thePESAIN.getBeneficiaryIdentifier());
                System.out.println("BeneficiaryAccount    : " + thePESAIN.getBeneficiaryAccount());
                System.out.println("BeneficiaryName       : " + thePESAIN.getBeneficiaryName());
                System.out.println("BeneficiaryReference  : " + thePESAIN.getBeneficiaryReference());
                System.out.println();
            } catch (Exception e) {
                e.printStackTrace();
            }

            thePESAINResponse.setCategory("C2B_VALIDATE");

            String strDestinationReference = UUID.randomUUID().toString().toUpperCase();
            String strBeneficiaryType = thePESAIN.getBeneficiaryType();
            String strSourceIdentifier = thePESAIN.getBeneficiaryAccount();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strSourceOnCBS = "ACCOUNT";

            if (String.valueOf(thePESAIN.getProductID()).equalsIgnoreCase("9")) {
                strSourceOnCBS = "MBANKING";

            }

            /*if (strBeneficiaryType.equals("NATIONAL_ID")) {
                strSourceOnCBS = "ID";
            } else if (strBeneficiaryType.equals("ACCOUNT") || strBeneficiaryType.equals("ACCOUNT_NO")) {
                strSourceOnCBS = "ACCOUNT";
            }*/

            Element elAccountElement = getValidate_PESA_IN_Element_NEW(strSourceIdentifier, strSourceOnCBS, strBeneficiaryType, doc);

            if (elAccountElement != null) {
                doc.appendChild(elAccountElement);
                String strXMLData = fnTransformXMLDocument(doc);
                thePESAINResponse.setOtherDetails(strXMLData);
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.PROCESSED.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                thePESAINResponse.setResponseDescription("Account Found - Validation Accepted.");
            } else {
                thePESAINResponse.setOtherDetails("<OTHER_DETAILS/>");
                thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
                thePESAINResponse.setResponseName(PESAConstants.PESAResponse.FAILED.getValue());
                thePESAINResponse.setResponseDescription("Account NOT Found - Validation NOT Accepted.");
            }
            thePESAINResponse.setBeneficiaryReference(strDestinationReference);
            thePESAINResponse.setBeneficiaryApplication("CBS");
        } catch (Exception e) {
            System.err.println("PESAAPI.validatePESA_IN() ERROR : " + e.getMessage());
            thePESAINResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
            thePESAINResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            thePESAINResponse.setResponseDescription("System Exception error validatePESA_IN(): " + e.getMessage() + ". Transaction NOT Accepted");
        }
    }

    public static void confirmPESA_OUT(PESA thePESAOUT, RequestPESAResponse theRequestPESAResponse) {
        String strResponse = "";
        try {
            theRequestPESAResponse.setCategory("CONFIRM_B2C");

            //todo - Implement Integration to CBS
            //String strTransactionStatus = CBSAPI.insertMpesaTransaction(entryCode, transactionID, transaction, description, accountNo, BigDecimal.valueOf(amount), phoneNo, "", "USSD", transactionID, "MBANKING");
            String strTransactionStatus = "SUCCESS";

            String strDestinationReference = UUID.randomUUID().toString().toLowerCase(); //todo: Get this from CBS
            String strBeneficiaryType = thePESAOUT.getBeneficiaryType();
            String strBeneficiaryIdentifier = thePESAOUT.getBeneficiaryIdentifier();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            String strBeneficiaryOnCBS = "Mobile";

            if (strBeneficiaryType.equalsIgnoreCase("NATIONAL_ID") || strBeneficiaryType.equalsIgnoreCase("ID_NUMBER")) {
                strBeneficiaryOnCBS = "ID";
            } else if (strBeneficiaryType.equalsIgnoreCase("ACCOUNT")) {
                strBeneficiaryOnCBS = "ACCOUNT";
            }

            String strTransactionID = "";//thePESAOUT.getPESAID() = "";

            String strProviderResponse = "ERROR";

            Element elPesaOtherDetails = getValidate_PESA_OUT_Element(strBeneficiaryIdentifier, strBeneficiaryOnCBS, doc);

            if (elPesaOtherDetails != null) {
                if (strTransactionStatus.equalsIgnoreCase("SUCCESS") || strTransactionStatus.equalsIgnoreCase("TRANSACTION_EXISTS")) {

                    /*
                        OTP Amount Expiry Date
                        111222 500 2021-05-05 12:30:00
                        222333 100 2021-05-05 12:45:00
                        333444 800 2021-05-05 13:00:00
                        333444 800 2021-05-05 13:15:00
                     */

                    /*
                    <OTHER_DETAILS>
                        <PESA_OTHER_DETAILS>
                            <PASS_KEY_DETAILS>
                                <PASS_KEY>34567</PASS_KEY>
                            </PASS_KEY_DETAILS>
                            <KYC_DETAILS/>
                            <PROVIDER_OTHER_DETAILS/>
                        </PESA_OTHER_DETAILS>
                    </OTHER_DETAILS>

                    APPROVED - Everything is OK. OTP is Valid & OTP Amount is <= Amount requested & Balance Sufficient
                    INVALID_OTP - OTP is Wrong or OTP Amount > Amount Requested
                    EXPIRED_OTP - OTP has expired (You can use INVALID OTP)
                    INSUFFICIENT_BALANCE - Amount Requested is <= Amount tied to OTP but account does not have enough funds
                    UNKNOWN_IDENTIFIER - The Beneficiary (Mostly MSISDN) is NOT found
                    INVALID_IDENTIFIER_TYPE - The Beneficiary Identifier Type is NOT MSISDN/ACCOUNT_NO/ID_NUMBER
                    ERROR - Error
                     */

                    double lnAmount = thePESAOUT.getTransactionAmount();
                    String strPassKey = MBankingXMLFactory.getXPathValueFromXMLString("/PESA_OTHER_DETAILS/PASS_KEY_DETAILS/PASS_KEY", thePESAOUT.getPESAXMLData());
                    String strBeneficiary = thePESAOUT.getBeneficiaryIdentifier();

                    String strCurrentDateTime = MBankingDB.getDBDateTime();


                    String strMobileNumber = thePESAOUT.getBeneficiaryIdentifier();

                    strMobileNumber = APIUtils.sanitizePhoneNumber(strMobileNumber);

                    String strMemoryCacheKey = strMobileNumber + ":" + APIConstants.TransactionType.ATM_CASH_WITHDRAWAL.getValue();

                    boolean blUserCanValidateOTP = false;
                    boolean blOTPValidationFailed = true;

                    String strUserLoginAttemptAction = CBSAPI.getUserLoginAttemptAction(strMobileNumber, "OTP");

                    if (strUserLoginAttemptAction.equalsIgnoreCase("SUSPENDED")) {
                        XMLGregorianCalendar gcExpiryDate = CBSAPI.getUserLoginAttemptExpiry(strMobileNumber, "OTP");
                        Date dtExpiryDate = gcExpiryDate.toGregorianCalendar().getTime();

                        if (dtExpiryDate.getTime() <= new Date().getTime()) {
                            blUserCanValidateOTP = true;
                        } else {
                            theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.PROCESSED.getValue());
                            theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                            strProviderResponse = "EXPIRED_OTP";
                            theRequestPESAResponse.setResponseDescription(strProviderResponse);
                            theRequestPESAResponse.setChargeApplied(0d);
                        }
                    } else {
                        blUserCanValidateOTP = true;
                    }

                    if (blUserCanValidateOTP) {
                        boolean blKeyExistsInMemory = InMemoryCache.exists(strMemoryCacheKey);

                        if (blKeyExistsInMemory) {
                            String strValueStoredInMemory = (String) InMemoryCache.retrieve(strMemoryCacheKey);

                            //todo: remove on production
                            System.out.println(strValueStoredInMemory);

                            HashMap<String, String> hmLoanType = Utils.toHashMap(strValueStoredInMemory);
                            String strApprovedAmount = hmLoanType.get("AMOUNT");
                            double lnApprovedAmount = Double.parseDouble(strApprovedAmount);
                            String strApprovedPassKey = hmLoanType.get("PASS_KEY_VALUE");
                            String strOTPTime = hmLoanType.get("PASS_KEY_TIME");
                            String strPIN = hmLoanType.get("MOBILE_BANKING_PIN");
                            String strApprovedBeneficiary = hmLoanType.get("BENEFICIARY_IDENTIFIER");
                            String strSessionID = hmLoanType.get("SESSION_ID");
                            String strAccountFrom = hmLoanType.get("ACCOUNT_NUMBER");
                            String strRequestApplication = hmLoanType.get("REQUEST_APPLICATION");

                            if ((strOTPTime.compareTo(strCurrentDateTime) >= 0) && (lnAmount <= lnApprovedAmount) && (strPassKey.equals(strApprovedPassKey)) && (strBeneficiary.equals(strApprovedBeneficiary))) {
                                //todo: revert to ATM Withdrawal request
                                String strTransaction = "ATM Withdrawal Request";
                                String strTransactionDescription = "ATM Withdrawal Request";
                                String strMobileNumberFrom = thePESAOUT.getBeneficiaryIdentifier();

                                XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

                                String strWithdrawalStatus = CBSAPI.insertMpesaTransaction(thePESAOUT.getOriginatorID(), strSessionID, xmlGregorianCalendar, strTransaction, strTransactionDescription, strAccountFrom, BigDecimal.valueOf(Double.parseDouble(strApprovedAmount)), strMobileNumberFrom, strPIN, strRequestApplication, strSessionID, "MBANKING", strMobileNumber, strMobileNumber, "M-Pesa", false, trimString(strMobileNumberFrom, 50));
                                String[] arrWithdrawalStatus = strWithdrawalStatus.split("%&:");

                                System.out.println("Withdrawal Status: " + Arrays.toString(arrWithdrawalStatus));

                                if (arrWithdrawalStatus[0].equals("SUCCESS")) {
                                    theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SENT.getValue());
                                    theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                                    strProviderResponse = "APPROVED";
                                    theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                    theRequestPESAResponse.setChargeApplied(0d);

                                    blOTPValidationFailed = false;
                                } else {
                                    theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                                    theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                                    strProviderResponse = "ERROR";
                                    theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                    theRequestPESAResponse.setChargeApplied(0d);
                                }
                            } else if (lnAmount > lnApprovedAmount) {
                                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                                strProviderResponse = "INSUFFICIENT_BALANCE";
                                theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                theRequestPESAResponse.setChargeApplied(0d);
                            } else if (!strPassKey.equals(strApprovedPassKey)) {
                                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                                strProviderResponse = "INVALID_OTP";
                                theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                theRequestPESAResponse.setChargeApplied(0d);
                            } else if (!strBeneficiary.equals(strApprovedBeneficiary)) {
                                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                                strProviderResponse = "UNKNOWN_IDENTIFIER";
                                theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                theRequestPESAResponse.setChargeApplied(0d);
                            } else if (strCurrentDateTime.compareTo(strOTPTime) > 0) { //If Current time is past strOTPTime
                                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                                strProviderResponse = "EXPIRED_OTP";
                                theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                theRequestPESAResponse.setChargeApplied(0d);
                            } else {
                                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                                strProviderResponse = "ERROR";
                                theRequestPESAResponse.setResponseDescription(strProviderResponse);
                                theRequestPESAResponse.setChargeApplied(0d);
                            }
                        } else {
                            theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                            theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                            strProviderResponse = "INVALID_OTP";
                            theRequestPESAResponse.setResponseDescription(strProviderResponse);
                            theRequestPESAResponse.setChargeApplied(0d);
                        }
                    } else {
                        theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                        theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                        strProviderResponse = "INVALID_OTP";
                        theRequestPESAResponse.setResponseDescription(strProviderResponse);
                        theRequestPESAResponse.setChargeApplied(0d);
                    }

                    if (blOTPValidationFailed) {
                        int intUserLoginAttemptsCount = CBSAPI.getUserLoginAttemptCount(strMobileNumber, "OTP");
                        intUserLoginAttemptsCount = intUserLoginAttemptsCount + 1;
                        new MAPPAPI().suspendUserAccess(strMobileNumber, intUserLoginAttemptsCount, "OTP", strMobileNumber, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);
                        System.out.println("Memory Search By Key " + strMemoryCacheKey + " was NOT FOUND");
                    } else {
                        InMemoryCache.remove(strMemoryCacheKey);
                        System.out.println("Memory Search By Key " + strMemoryCacheKey + " was FOUND");
                    }
                } else {
                    theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                    theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                    strProviderResponse = "ERROR";
                    theRequestPESAResponse.setResponseDescription("CBS RESPONSE: " + strTransactionStatus);
                    theRequestPESAResponse.setChargeApplied(0d);
                }

                Element elProviderResponse = doc.createElement("PROVIDER_RESPONSE");
                elProviderResponse.setTextContent(strProviderResponse);
                elPesaOtherDetails.appendChild(elProviderResponse);

                doc.appendChild(elPesaOtherDetails);
                String strXMLData = fnTransformXMLDocument(doc);
                theRequestPESAResponse.setOtherDetails(strXMLData);

            } else {
                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                strProviderResponse = "UNKNOWN_IDENTIFIER";
                theRequestPESAResponse.setResponseDescription(strProviderResponse);
                theRequestPESAResponse.setChargeApplied(0d);

                elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                Element elProviderResponse = doc.createElement("PROVIDER_RESPONSE");
                elProviderResponse.setTextContent(strProviderResponse);

                elPesaOtherDetails.appendChild(elProviderResponse);

                doc.appendChild(elPesaOtherDetails);
                String strXMLData = fnTransformXMLDocument(doc);
                theRequestPESAResponse.setOtherDetails(strXMLData);
            }

            theRequestPESAResponse.setBeneficiaryReference(strDestinationReference);

        } catch (Exception e) {
            System.err.println("PESAAPI.confirmPESA_OUT() ERROR : " + e.getMessage());

            theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_FAILED.getValue());
            theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            theRequestPESAResponse.setResponseDescription("System Exception error confirmPESA_IN(): " + e.getMessage() + ". Transaction NOT Accepted");
            theRequestPESAResponse.setChargeApplied(0d);
        }
    }

    public static void validatePESA_OUT(PESA thePESAOUT, RequestPESAResponse theRequestPESAResponse) {
        String strResponse = "";
        try {
            theRequestPESAResponse.setCategory("B2C_VALIDATE");

            String strDestinationReference = UUID.randomUUID().toString().toUpperCase(); //todo: Get this from CBS
            String strBeneficiaryType = thePESAOUT.getBeneficiaryType();
            String strBeneficiaryIdentifier = thePESAOUT.getBeneficiaryIdentifier();

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();


            String strBeneficiaryOnCBS = "Mobile";

            if (strBeneficiaryType.equalsIgnoreCase("NATIONAL_ID") || strBeneficiaryType.equalsIgnoreCase("ID_NUMBER")) {
                strBeneficiaryOnCBS = "ID";
            } else if (strBeneficiaryType.equalsIgnoreCase("ACCOUNT")) {
                strBeneficiaryOnCBS = "ACCOUNT";
            }

            Element elAccountElement = getValidate_PESA_OUT_Element(strBeneficiaryType, strBeneficiaryOnCBS, doc);

            if (elAccountElement != null) {
                doc.appendChild(elAccountElement);
                String strXMLData = fnTransformXMLDocument(doc);
                theRequestPESAResponse.setOtherDetails(strXMLData);
                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.PROCESSED.getValue());
                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.getValue());
                theRequestPESAResponse.setResponseDescription("Account Found - Validation Accepted.");
            } else {
                theRequestPESAResponse.setOtherDetails("<OTHER_DETAILS/>");
                theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
                theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
                theRequestPESAResponse.setResponseDescription("Account NOT Found - Validation NOT Accepted.");
            }
            theRequestPESAResponse.setBeneficiaryReference(strDestinationReference);
        } catch (Exception e) {
            System.err.println("PESAAPI.validatePESA_OUT() ERROR : " + e.getMessage());

            theRequestPESAResponse.setResponseCode(PESAConstants.PESAStatusCode.FORWARD_ERROR.getValue());
            theRequestPESAResponse.setResponseName(PESAConstants.PESAResponse.ERROR.getValue());
            theRequestPESAResponse.setResponseDescription("System Exception error validatePESA_OUT(): " + e.getMessage() + ". Transaction NOT Accepted");
        }
    }

    public static void processPESAResult(PESAResult thePESAResult, PESAResultResponse thePESAResultResponse) {

        try {
            String strPesaType = "";

            String strOriginatorID = thePESAResult.getOriginatorID();

            String strEntryNumber = thePESAResult.getBeneficiaryReference();

            String strCategory = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "pesa_category");

            String strDescription = "";
            String strDescriptionPrefix = "";


            switch (strCategory) {
                case "MPESA_WITHDRAWAL":
                    strPesaType = "Mpesa Withdrawal";
                    strDescriptionPrefix = "Withdrawal";
                    break;
                case "AIRTIME_PURCHASE":
                    strPesaType = "Airtime Purchase";
                    strDescriptionPrefix = "Airtime Purchase";
                    break;
                case "UTILITY_BILL_PAYMENT":
                case "BILL_PAYMENT":
                    strPesaType = "Utility Payment";
                    strDescriptionPrefix = "Utility Payment";
                    break;
                case "BANK_TRANSFER":
                case "EXTERNAL_BANK_TRANSFER":
                    strPesaType = "Bank Transfer";
                    strDescriptionPrefix = "Bank Transfer";
                    break;
                case "PESALINK_TRANSFER":
                    strPesaType = "Pesalink Transfer";
                    strDescriptionPrefix = "BT";
                    break;
                case "CARDLESS_ATM_WITHDRAWAL":
                    strPesaType = "Cardless ATM";
                    strDescriptionPrefix = "CAT";
                    break;
                default:
                    strPesaType = "Mpesa Withdrawal";
                    strDescriptionPrefix = "Withdrawal";
            }


            String strReceiverType = "";
            String strInitiatorIdentifier = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "initiator_identifier");
            String strReceiverIdentifier = "";
            String strReceiverOrganization = "";
            String strReceiverAccount = "";
            String strReceiverName = "";

            if (thePESAResult.getResultCode() == 105) {
                //Get MPESA Result Name
                if (thePESAResult.getPESAType().equals("PESA_OUT")) {
                    //TODO: TEST THIS AFTER COMPILATION

                    switch (strCategory) {
                        case "CASH_WITHDRAWAL":
                        case "MPESA_WITHDRAWAL":
                            strDescription = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "transaction_remark");

                            String strName = MBankingXMLFactory.getXPathValueFromXMLString("/RESULT/RECEIVER/NAME", thePESAResult.getOtherDetails());
                            strDescription = strDescription + "|" + strName;
                            break;
                        case "PESA_AUTO_PAYMENT":

                            strDescription = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "transaction_remark");
                            strEntryNumber = thePESAResult.getOriginatorID();
                            break;
                        case "CARDLESS_ATM_WITHDRAWAL":
                            strDescription = "Cardless ATM Withdrawal";
                            strEntryNumber = thePESAResult.getOriginatorID();
                            break;
                        case "BILL_PAYMENT":
                        case "BANK_TRANSFER":
                        case "PESALINK_TRANSFER":
                            strEntryNumber = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "source_reference");
                            /*HashMap<String, String> hmPesaResultName = PESAXMLFactory.getPESAResultReceiverDetails(thePESAResult.getOtherDetails());
                            strReceiverType = hmPesaResultName.get("RECEIVER_TYPE");
                            strReceiverIdentifier = hmPesaResultNamSavie.get("RECEIVER_IDENTIFIER");
                            strReceiverAccount = hmPesaResultName.get("RECEIVER_ACCOUNT");
                            strReceiverName = hmPesaResultName.get("RECEIVER_NAME");
                            strDescription = strDescriptionPrefix+" to "+strReceiverName;*/

                            //PesaLink to Standard Chartered Bank A/C 1213345687787 from 254722554433 Isaac Kiptoo Mulwa Jacob Orao
                            //PesaLink to 1213345687787 Standard Chartered Bank
                            //PL|BT|11|01109089263400|254720259655|JACOB AYIEKE

                            strDescription = PESAAPIDB.getPESATransaction(strEntryNumber, PESAConstants.PESAType.PESA_OUT, "transaction_remark");
                            break;
                        default:
                            strDescription = strDescriptionPrefix;
                    }

                    strDescription = shortenName(strDescription);
                }

                String strBeneficiaryIdentifier = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "beneficiary_identifier");

                String beneficiaryName = strReceiverName;
                boolean otherNumber = !(strBeneficiaryIdentifier.equals(strInitiatorIdentifier));

                XMLGregorianCalendar xmlGregorianCalendar = fnGetCurrentDateInGregorianFormat();

//                String strTransactionStatus = CBSAPI.insertMpesaTransaction(thePESAResult.getOriginatorID(), thePESAResult.getReceiverReference(), xmlGregorianCalendar, strPesaType, strDescription, "", BigDecimal.valueOf(0), "", "", "USSD", thePESAResult.getReceiverReference(), "MBANKING", "M-PESA", strBeneficiaryIdentifier, beneficiaryName, otherNumber, beneficiaryName);

                String strTransactionStatus = CBSAPI.insertMpesaTransaction(thePESAResult.getOriginatorID(), thePESAResult.getReceiverReference(), xmlGregorianCalendar, strPesaType, strDescription, "", BigDecimal.valueOf(0), "", "", "USSD", thePESAResult.getReceiverReference(), "MBANKING", "M-PESA", strBeneficiaryIdentifier, beneficiaryName, otherNumber, strBeneficiaryIdentifier);

                String strTransactionResponseStatus = MBankingXMLFactory.getXPathValueFromXMLString("/Response/Status", strTransactionStatus);
                System.out.println(strPesaType + " Transaction sent to Nav with status: " + strTransactionStatus);
                if (strTransactionResponseStatus.equals("SUCCESS")) {
                    MAPPAPI mappapi = new MAPPAPI();
                    String strSourceIdentifier = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "source_identifier");
                    String strSourceName = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "source_name");
                    String strSenderName = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "sender_name");
                    String strSenderIdentifier = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "sender_identifier");
                    String strAmount = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "transaction_amount");
                    strAmount = Utils.formatDouble(strAmount, "#,###.##");
                    String strSourceMobileNumber = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "initiator_identifier");

                    InputSource source = new InputSource(new StringReader(thePESAResult.getOtherDetails()));
                    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                    Document xmlDocument = builder.parse(source);
                    XPath configXPath = XPathFactory.newInstance().newXPath();

                    String strPESARecipientMobileNumber = PESAAPIDB.getPESATransaction(strOriginatorID, PESAConstants.PESAType.PESA_OUT, "beneficiary_identifier");
                    String strPESADestinationReference = thePESAResult.getBeneficiaryReference();

                    if (!strPESARecipientMobileNumber.equals(APIUtils.sanitizePhoneNumber(strSourceMobileNumber))) {
                        String strServiceProvider = "M-Pesa";

                        String strMSG = strSourceName + " has sent KSh " + strAmount + " to your " + strServiceProvider + ". The " + strServiceProvider + " receipt number is " + strPESADestinationReference + " and transaction reference is " + strOriginatorID;
                        String strSessionID = strPESADestinationReference + "_MSG";
                        String strTraceID = UUID.randomUUID().toString();
                        fnSendSMS(strPESARecipientMobileNumber, strMSG, "YES", MSGConstants.MSGMode.EXPRESS, 200, "TRANSACTION_RESULT", "MAPP", "MBANKING_SERVER", strSessionID, strTraceID);
                    }

                    thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.COMPLETED.getValue());
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.name());
                    thePESAResultResponse.setResponseDescription("SUCCESS");
                } else if (strTransactionResponseStatus.equals("TRANSACTION_EXISTS")) {
                    thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.COMPLETED.getValue());
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.name());
                    thePESAResultResponse.setResponseDescription("TRANSACTION_EXISTS");
                } else {
                    thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_ERROR.getValue());
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.ERROR.name());
                    thePESAResultResponse.setResponseDescription(strTransactionStatus);
                    thePESAResultResponse.setChargeApplied(0.00);
                }
            } else {
                boolean blTransactionStatus = CBSAPI.reverseWithdrawalRequest(thePESAResult.getOriginatorID());
                System.out.println(strPesaType + "Transaction reversed on Nav with status: " + blTransactionStatus);
                if (blTransactionStatus) {
                    thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.REVERSE_COMPLETED.getValue());
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.name());
                    thePESAResultResponse.setResponseDescription("Reversal Succeeded");
                } else {
                    thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.REVERSE_ERROR.getValue());
                    thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.ERROR.name());
                    thePESAResultResponse.setResponseDescription("Transaction Reversal Error");
                    thePESAResultResponse.setChargeApplied(0.00);
                }
            }
        } catch (Exception e) {
            if (e.getMessage().contains("The Sky Transactions already exists.")) {
                thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.COMPLETED.getValue());
                thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.SUCCESS.name());
                thePESAResultResponse.setResponseDescription("TRANSACTION_EXISTS");
            } else {
                System.err.println("PESAAPI.processPESAResult() ERROR : " + e.getMessage());
                thePESAResultResponse.setResponseCode(PESAConstants.PESAStatusCode.SEND_ERROR.getValue());
                thePESAResultResponse.setResponseName(PESAConstants.PESAResponse.ERROR.name());
                thePESAResultResponse.setResponseDescription("System Exception error: " + e.getMessage() + ". Transaction NOT Accepted");
                thePESAResultResponse.setChargeApplied(0.00);
            }
        }
    }

    public static PesaParam getPesaParam(APIConstants.APPLICATION_TYPE theApplicationType, APIConstants.PESA_PARAM_TYPE thePesaParamType) {
        PesaParam rVal = new PesaParam();
        try {
            String strPesaParamType = "OTHER_DETAILS/CUSTOM_PARAMETERS/PAYMENT_CHANNELS";

            switch (thePesaParamType) {
                case MPESA_B2C: {
                    strPesaParamType += "/SAFARICOM/MPESA_B2C";
                    break;
                }
                case MPESA_C2B: {
                    strPesaParamType += "/SAFARICOM/MPESA_C2B";
                    break;
                }
                case MPESA_B2B: {
                    strPesaParamType += "/SAFARICOM/MPESA_B2B";
                    break;
                }
                case FAMILY_BANK_PESALINK: {
                    strPesaParamType += "/FAMILY_BANK/PESALINK";
                    break;
                }
                case AIRTIME: {
                    strPesaParamType += "/GLOBAL/AIRTIME";
                    break;
                }
            }

            String strProductId = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/PRODUCT_ID");
            String strSenderIdentifier = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/SENDER_IDENTIFIER");
            String strSenderAccount = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/SENDER_ACCOUNT");
            String strSenderName = MBankingAPI.getValueFromLocalParams(theApplicationType, strPesaParamType + "/SENDER_NAME");

            rVal.setProductId(strProductId);
            rVal.setSenderIdentifier(strSenderIdentifier);
            rVal.setSenderAccount(strSenderAccount);
            rVal.setSenderName(strSenderName);
        } catch (Exception e) {
            System.err.println("PESADB.getPesaParam() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    private static String getResponseStatus(String strXML) {
        String strStatus = "Unexpected Response Format.";
        try {
            if (strXML != null && !strXML.isEmpty() && !strXML.equalsIgnoreCase(CBS_ERROR)) {
                InputSource source = new InputSource(new StringReader(strXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList nlResponse = ((NodeList) configXPath.evaluate("/Response", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                strStatus = nlResponse.item(0).getTextContent();
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.getResponseStatus() ERROR : " + e.getMessage());
        }
        return strStatus;
    }

    private static String getResponseTransactionID(String strXML) {
        String strTransactionID = null;
        try {
            if (strXML != null && !strXML.isEmpty() && !strXML.equalsIgnoreCase(CBS_ERROR)) {
                InputSource source = new InputSource(new StringReader(strXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList nlResponse = ((NodeList) configXPath.evaluate("/Response", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                strTransactionID = nlResponse.item(2).getTextContent().replaceAll("[{}]", "");
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.getResponseTransactionID() ERROR : " + e.getMessage());
        }
        return strTransactionID;
    }

    private static String getBeneficiaryDetailsFromDescription(String strXML) {
        String strRval = "";
        try {
            if (!strXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strRval = configXPath.evaluate("/OTHER_DETAILS/ReceiverName", xmlDocument, XPathConstants.STRING).toString();
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.getBeneficiaryDetailsFromDescription() ERROR : " + e.getMessage());
        }
        return strRval;
    }

    public static String shortenName(String theDescription) {
        StringBuilder rValPre = new StringBuilder();
        StringBuilder rVal = new StringBuilder();
        if (theDescription.length() > 50) {
            for (int i = 0; i < theDescription.split(" ").length - 1; i++) {
                rValPre.append(theDescription.split(" ")[i]);
                rValPre.append(" ");
                if (rValPre.toString().trim().length() < 50) {
                    rVal.append(theDescription.split(" ")[i]);
                    rVal.append(" ");
                } else {
                    break;
                }
            }
        } else {
            return theDescription;
        }

        return rVal.toString().trim();
    }

    public boolean pesa_C2B_Request(String theOriginatorID, String theReceiver, String theReceiverDetails, String theAccount, String theCurrency, double theAmount, String theCategory, String theReference, String theRequestApplication, String theSourceApplication, String theTraceID, String theSessionID) {

        boolean bRVal = false;

        PESA thePESA = new PESA();

        try {
            PesaParam pesaParam = PESAAPI.getPesaParam(APIConstants.APPLICATION_TYPE.PESA, APIConstants.PESA_PARAM_TYPE.MPESA_C2B);

            long lnProductID = Long.parseLong(pesaParam.getProductId());
            String strSender = pesaParam.getSenderIdentifier();
            String strSenderDetails = pesaParam.getSenderName();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strPesaCommand = "CustomerPayBillOnline";
            String strTransactionRemark = "C2B Payment Request by " + strSenderDetails + " to " + theReceiver;
            String strDate = MBankingDB.getDBDateTime().trim();

            thePESA.setOriginatorID(theOriginatorID);
            thePESA.setProductID(lnProductID);
            thePESA.setPESAType(PESAConstants.PESAType.PESA_IN);
            thePESA.setPESAAction(PESAConstants.PESAAction.C2B);
            thePESA.setCommand(strPesaCommand);
            thePESA.setSensitivity(PESAConstants.Sensitivity.NORMAL);
            //thePESA.setChargeProposed(null);

            thePESA.setInitiatorType("MSISDN");
            thePESA.setInitiatorIdentifier(theReceiver);
            thePESA.setInitiatorAccount(theReceiver);
            thePESA.setInitiatorName("");
            thePESA.setInitiatorReference(theTraceID);
            thePESA.setInitiatorApplication(theRequestApplication);
            thePESA.setInitiatorOtherDetails("<DATA/>");

            thePESA.setSourceType("ACCOUNT_NO");
            thePESA.setSourceIdentifier(theAccount);
            thePESA.setSourceAccount(theAccount);
            thePESA.setSourceName("");
            thePESA.setSourceReference(theSessionID);
            thePESA.setSourceApplication("CBS");
            thePESA.setSourceOtherDetails("<DATA/>");

            thePESA.setSenderType("SHORT_CODE");
            thePESA.setSenderIdentifier(strSender);
            thePESA.setSenderAccount(strSenderAccount);
            thePESA.setSenderName(strSenderDetails);
            thePESA.setSenderOtherDetails("<DATA/>");

            thePESA.setReceiverType("SHORT_CODE");
            thePESA.setReceiverIdentifier(strSender);
            thePESA.setReceiverAccount(theAccount);
            thePESA.setReceiverName(strSenderDetails);
            thePESA.setReceiverOtherDetails("<DATA/>");

            thePESA.setBeneficiaryType("MSISDN");
            thePESA.setBeneficiaryIdentifier(theReceiver);
            thePESA.setBeneficiaryAccount(theReceiver);
            thePESA.setBeneficiaryName(theReceiverDetails);
            thePESA.setBeneficiaryOtherDetails("<DATA/>");

            thePESA.setBatchReference(theOriginatorID);
            thePESA.setCorrelationReference(theTraceID);
            thePESA.setCorrelationApplication(theRequestApplication);
            thePESA.setTransactionCurrency("KES");
            thePESA.setTransactionAmount(theAmount);
            thePESA.setTransactionRemark(strTransactionRemark);
            thePESA.setCategory(theCategory);

            thePESA.setPriority(200);
            thePESA.setSendCount(0);

            thePESA.setSchedulePesa(PESAConstants.Condition.NO);
            thePESA.setPesaDateScheduled(strDate);
            thePESA.setPesaDateCreated(strDate);
            thePESA.setPESAXMLData("<DATA/>");

            PESAINRequestResponse thePESAC2BRequestResponse = PESAProcessor.sendPESAINPaymentRequest(thePESA);

            if (thePESAC2BRequestResponse.getResponseCode() == 500 || thePESAC2BRequestResponse.getResponseCode() == 102) {
                bRVal = true;
            }
        } catch (Exception e) {
            System.err.println("PESAAPI.pesa_C2B_Request() ERROR : " + e.getMessage());
        }

        return bRVal;
    }

    public boolean pesa_C2B_Request(String theInitiatorMobileNumber,
                                    String theInitiatorName,
                                    String theInitiatorTraceId,
                                    String theInitiatorApplication,
                                    String theSourceAccount,
                                    String theSourceAccountName,
                                    String theSourceApplication,
                                    String theSourceReference,
                                    String theBeneficiaryMobileNumber,
                                    String theBeneficiaryName,
                                    String theReceiverAccount,
                                    double theAmount,
                                    String theCategory) {

        boolean bRVal = false;

        PESA thePESA = new PESA();

        try {
            PesaParam pesaParam = PESAAPI.getPesaParam(APIConstants.APPLICATION_TYPE.PESA, APIConstants.PESA_PARAM_TYPE.MPESA_C2B);

            long lnProductID = Long.parseLong(pesaParam.getProductId());
            String strSender = pesaParam.getSenderIdentifier();
            String strSenderDetails = pesaParam.getSenderName();
            String strSenderAccount = pesaParam.getSenderAccount();
            String strPesaCommand = "CustomerPayBillOnline";
            String strDateTime = MBankingDB.getDBDateTime().trim();

            String strOriginatorID = UUID.randomUUID().toString();

            thePESA.setOriginatorID(strOriginatorID);
            thePESA.setProductID(lnProductID);

            thePESA.setPESAType(PESAConstants.PESAType.PESA_IN);
            thePESA.setPESAAction(PESAConstants.PESAAction.C2B);
            thePESA.setCommand(strPesaCommand);
            thePESA.setSensitivity(PESAConstants.Sensitivity.NORMAL);

            thePESA.setInitiatorType("MSISDN");
            thePESA.setInitiatorIdentifier(theInitiatorMobileNumber);
            thePESA.setInitiatorAccount(theInitiatorMobileNumber);
            thePESA.setInitiatorName(theInitiatorName);
            thePESA.setInitiatorReference(theInitiatorTraceId);
            thePESA.setInitiatorApplication(theInitiatorApplication);
            thePESA.setInitiatorOtherDetails("<DATA/>");

            thePESA.setSourceType("ACCOUNT_NO");
            thePESA.setSourceIdentifier(theSourceAccount);
            thePESA.setSourceAccount(theSourceAccount);
            thePESA.setSourceName(theSourceAccountName);
            thePESA.setSourceReference(theSourceReference);
            thePESA.setSourceApplication(theSourceApplication);
            thePESA.setSourceOtherDetails("<DATA/>");

            thePESA.setSenderType("SHORT_CODE");
            thePESA.setSenderIdentifier(strSender);
            thePESA.setSenderAccount(strSender);
            thePESA.setSenderName(strSenderDetails);
            thePESA.setSenderOtherDetails("<DATA/>");

            thePESA.setReceiverType("SHORT_CODE");
            thePESA.setReceiverIdentifier(strSender);
            thePESA.setReceiverAccount(theReceiverAccount);
            thePESA.setReceiverName(strSenderDetails);
            thePESA.setReceiverOtherDetails("<DATA/>");

            thePESA.setBeneficiaryType("MSISDN");
            thePESA.setBeneficiaryIdentifier(theBeneficiaryMobileNumber);
            thePESA.setBeneficiaryAccount(theBeneficiaryMobileNumber);
            thePESA.setBeneficiaryName(theBeneficiaryName);
            thePESA.setBeneficiaryOtherDetails("<DATA/>");

            thePESA.setBatchReference(strOriginatorID);
            thePESA.setCorrelationReference(theInitiatorTraceId);
            thePESA.setCorrelationApplication(theInitiatorApplication);
            thePESA.setTransactionCurrency("KES");
            thePESA.setTransactionAmount(theAmount);
            thePESA.setTransactionRemark("C2B Payment Request by " + strSenderDetails + " to " + theBeneficiaryMobileNumber);
            thePESA.setCategory(theCategory);

            thePESA.setPriority(200);
            thePESA.setSendCount(0);

            thePESA.setSchedulePesa(PESAConstants.Condition.NO);
            thePESA.setPesaDateScheduled(strDateTime);
            thePESA.setPesaDateCreated(strDateTime);
            thePESA.setPESAXMLData("<DATA/>");

            System.out.println("\n\n*******************************************************");
            System.out.println("            DETAILS FROM processPESA_IN()");
            System.out.println("*******************************************************");
            System.out.println("Originator ID                  :" + thePESA.getOriginatorID() + "|");
            System.out.println("PESA ID                        :" + thePESA.getPESAID() + "|");
            System.out.println("Server ID                      :" + thePESA.getServerID() + "|");
            System.out.println("Product ID                     :" + thePESA.getProductID() + "|");
            System.out.println("PESA Type                      :" + thePESA.getPESAType().toString() + "|");
            System.out.println("PESA Action                    :" + thePESA.getPESAAction().toString() + "|");

            System.out.println("Initiator Type                 :" + thePESA.getInitiatorType() + "|");
            System.out.println("Initiator Identifier           :" + thePESA.getInitiatorIdentifier() + "|");
            System.out.println("Initiator Account              :" + thePESA.getInitiatorAccount() + "|");
            System.out.println("Initiator Name                 :" + thePESA.getInitiatorName() + "|");
            System.out.println("Initiator Reference            :" + thePESA.getInitiatorReference() + "|");
            System.out.println("Initiator Application          :" + thePESA.getInitiatorApplication() + "|");
            System.out.println("Initiator Other Details        :" + thePESA.getInitiatorOtherDetails() + "|");

            System.out.println("Source Type                    :" + thePESA.getSourceType() + "|");
            System.out.println("Source Identifier              :" + thePESA.getSourceIdentifier() + "|");
            System.out.println("Source Account                 :" + thePESA.getSourceAccount() + "|");
            System.out.println("Source Name                    :" + thePESA.getSourceName() + "|");
            System.out.println("Source Reference               :" + thePESA.getSourceReference() + "|");
            System.out.println("Source Application             :" + thePESA.getSourceApplication() + "|");
            System.out.println("Source Other Details           :" + thePESA.getSourceOtherDetails() + "|");

            System.out.println("Sender Type                    :" + thePESA.getSenderType() + "|");
            System.out.println("Sender Identifier              :" + thePESA.getSenderIdentifier() + "|");
            System.out.println("Sender Account                 :" + thePESA.getSenderAccount() + "|");
            System.out.println("Sender Name                    :" + thePESA.getSenderName() + "|");
            System.out.println("Sender Other Details           :" + thePESA.getSenderOtherDetails() + "|");
            System.out.println("Receiver Type                  :" + thePESA.getReceiverType() + "|");
            System.out.println("Receiver Identifier            :" + thePESA.getReceiverIdentifier() + "|");
            System.out.println("Receiver Account               :" + thePESA.getReceiverAccount() + "|");
            System.out.println("Receiver Name                  :" + thePESA.getReceiverName() + "|");
            System.out.println("Receiver Other Details         :" + thePESA.getReceiverOtherDetails() + "|");
            System.out.println("Beneficiary Type               :" + thePESA.getBeneficiaryType() + "|");
            System.out.println("Beneficiary Identifier         :" + thePESA.getBeneficiaryIdentifier() + "|");
            System.out.println("Beneficiary Account            :" + thePESA.getBeneficiaryAccount() + "|");
            System.out.println("Beneficiary Name               :" + thePESA.getBeneficiaryName() + "|");
            System.out.println("Beneficiary Other Details      :" + thePESA.getBeneficiaryOtherDetails() + "|");

            System.out.println("Batch Reference                :" + thePESA.getBatchReference() + "|");
            System.out.println("Correlation Reference          :" + thePESA.getCorrelationReference() + "|");
            System.out.println("Correlation Application        :" + thePESA.getCorrelationApplication() + "|");

            System.out.println("Transaction Currency           :" + thePESA.getTransactionCurrency() + "|");
            System.out.println("Transaction Amount             :" + thePESA.getTransactionAmount() + "|");
            System.out.println("Transaction Remark             :" + thePESA.getTransactionRemark() + "|");

            System.out.println("Command                        :" + thePESA.getCommand() + "|");
            System.out.println("Sensitivity                    :" + thePESA.getSensitivity() + "|");
            System.out.println("Category                       :" + thePESA.getCategory() + "|");
            System.out.println("Priority                       :" + thePESA.getPriority() + "|");
            System.out.println("Send Count                     :" + thePESA.getSendCount() + "|");
            System.out.println("PESA XML Data                  :" + thePESA.getPESAXMLData() + "|");
            //System.out.println("Send Integrity Hash            :" + thePESA.getSendIntegrityHash()+"|);
            System.out.println("Schedule Pesa                  :" + thePESA.getSchedulePesa() + "|");
            System.out.println("Date Scheduled                 :" + thePESA.getPesaDateScheduled() + "|");
            System.out.println("General Flag                   :" + thePESA.getGeneralFlag() + "|");
            System.out.println("Transaction Date               :" + thePESA.getPesaDateCreated() + "|");
            System.out.println("\n\n*******************************************************");
            System.out.println("            DETAILS FROM processPESA_IN()");
            System.out.println("*******************************************************");

            //bRVal = PESAProcessor.sendC2BPaymentRequest(thePESA);

            PESAINRequestResponse thePESAINRequestResponse = PESAProcessor.sendPESAINPaymentRequest(thePESA);

            if (thePESAINRequestResponse.getResponseCode() == 500 || thePESAINRequestResponse.getResponseCode() == 102) {
                bRVal = true;
            }

            System.out.println("thePESAINRequestResponse.getResponseCode() : " + thePESAINRequestResponse.getResponseCode());
            System.out.println("thePESAINRequestResponse.getResponseName() : " + thePESAINRequestResponse.getResponseName());
            System.out.println("thePESAINRequestResponse.getResponseDescription(): " + thePESAINRequestResponse.getResponseDescription());
        } catch (Exception e) {
            System.err.println("PESAAPI.pesa_C2B_Request() ERROR : " + e.getMessage());
        }

        return bRVal;
    }

    public static String fnTransformXMLDocument(Document xmlDocument) {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xmlDocument), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Element getValidate_PESA_IN_Element(String theAccount, String theSource, Document doc) {
        try {
            String strAccountNumberXML = "<Account><AccountNo>5000000800000</AccountNo><AccountName>ISAAC KIPTOO MULWA</AccountName><Name>ISAAC KIPTOO MULWA</Name><MemberNo>0000800</MemberNo><PhoneNo>+254706405989</PhoneNo></Account>";


            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                String strBeneficiaryType = "";
                if (theSource.equals("Mobile")) {
                    strBeneficiaryType = "MSISDN";
                } else if (theSource.equals("ID")) {
                    strBeneficiaryType = "NATIONAL_ID";
                }

                elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", strBeneficiaryType);
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(theAccount);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Element getValidate_PESA_IN_Element_NEW(String theAccount, String theSource, String strSourceType, Document doc) {

        try {

            String strAccountNumberXML;

            if (theSource.equalsIgnoreCase("MBANKING")) {

                strAccountNumberXML = "" +
                        "<Account>" +
                        "    <AccountNo>500023100023222</AccountNo>" +
                        "    <AccountName>FOSA SAVINGS A/C</AccountName>" +
                        "    <Name>MBANKING</Name>" +
                        "    <MemberNo>00000</MemberNo>" +
                        "    <PhoneNo>254712345678</PhoneNo>" +
                        "</Account>";

            } else {
                strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(theAccount, theSource);
            }


            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                String strBeneficiaryType = "";
                if (theSource.equals("Mobile")) {
                    strBeneficiaryType = "MSISDN";
                } else if (theSource.equals("ID")) {
                    strBeneficiaryType = "NATIONAL_ID";
                }

                elPesaOtherDetails = doc.createElement("RESPONSE");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", strSourceType);
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(theAccount);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

//    public static Element getValidate_PESA_IN_Element_NEW(String theAccount, String theSource, String strSourceType, Document doc) {
//        try {
//            /**
//             * <Account>
//             *     <AccountNo>500023100023222</AccountNo>
//             *     <AccountName>FOSA SAVINGS A/C</AccountName>
//             *     <Name>DEREK PRINCE MUTENDE</Name>
//             *     <MemberNo>23122</MemberNo>
//             *     <PhoneNo>254713000249</PhoneNo>
//             * </Account>
//             */
//            String strAccountNumberXML = "" +
//                    "<Account>" +
//                    "    <AccountNo>500023100023222</AccountNo>" +
//                    "    <AccountName>FOSA SAVINGS A/C</AccountName>" +
//                    "    <Name>DEREK PRINCE MUTENDE</Name>" +
//                    "    <MemberNo>23122</MemberNo>" +
//                    "    <PhoneNo>254713000249</PhoneNo>" +
//                    "</Account>";
//            //String strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(theAccount, theSource);;
//
//
//            Element elPesaOtherDetails = null;
//
//            String strAccountNo = "";
//            String strAccountType = "";
//            String strAccountName = "";
//            String strAccountMemberNo = "";
//            String strPhoneNo = "";
//            String strAccountStatus = "NOT_FOUND";
//
//            if (!strAccountNumberXML.equals("")) {
//                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
//                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder builder = builderFactory.newDocumentBuilder();
//                Document xmlDocument = builder.parse(source);
//                XPath configXPath = XPathFactory.newInstance().newXPath();
//
//                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
//                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
//                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
//                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
//                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
//                strAccountName = Utils.toTitleCase(strAccountName);
//                strAccountStatus = "FOUND";
//
//                String strBeneficiaryType = "";
//                if (theSource.equals("Mobile")) {
//                    strBeneficiaryType = "MSISDN";
//                } else if (theSource.equals("ID")) {
//                    strBeneficiaryType = "NATIONAL_ID";
//                }
//
//                /**
//                 * <RESPONSE>
//                 *     <KYC_DETAILS>
//                 *         <RESPONSE>
//                 *             <KYC TYPE="MSISDN/NATIONAL_ID">
//                 *                 <IDENTIFIER>254713000249</IDENTIFIER>
//                 *                 <ACCOUNT>500023100023222</ACCOUNT>
//                 *                 <NAME>DEREK PRINCE MUTENDE</NAME>
//                 *                 <OTHER_DETAILS />
//                 *             </KYC>
//                 *         </RESPONSE>
//                 *     </KYC_DETAILS>
//                 * </RESPONSE>
//                 */
//                elPesaOtherDetails = doc.createElement("RESPONSE");
//
//                Element elKYCDetails = doc.createElement("KYC_DETAILS");
//                elPesaOtherDetails.appendChild(elKYCDetails);
//
//                Element elKYCResponse = doc.createElement("RESPONSE");
//                elKYCDetails.appendChild(elKYCResponse);
//
//                Element elKYC = doc.createElement("KYC");
//                elKYC.setAttribute("TYPE", strSourceType);
//                elKYCResponse.appendChild(elKYC);
//
//                Element elIdentifier = doc.createElement("IDENTIFIER");
//                elIdentifier.setTextContent(theAccount);
//                elKYC.appendChild(elIdentifier);
//
//                Element elAccount = doc.createElement("ACCOUNT");
//                elAccount.setTextContent(strAccountNo);
//                elKYC.appendChild(elAccount);
//
//                Element elName = doc.createElement("NAME");
//                elName.setTextContent(strAccountName);
//                elKYC.appendChild(elName);
//
//                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
//                elKYC.appendChild(elOtherDetails);
//            }
//            return elPesaOtherDetails;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    public static Element getValidate_PESA_OUT_Element(String theAccount, String theSource, Document doc) {
        try {
            //todo - Implement Integration to CBS
            String strAccountNumberXML = CBSAPI.getAccountTransferRecipientXML(theAccount, theSource);
            //String strAccountNumberXML = "<Account><AccountNo>5000000800000</AccountNo><AccountName>ISAAC KIPTOO MULWA</AccountName><Name>ISAAC KIPTOO MULWA</Name><MemberNo>0000800</MemberNo><PhoneNo>+254706405989</PhoneNo></Account>";
            /*
            <Account>
                <AccountNo>5000000127000</AccountNo>
                <AccountName>ADAN IBRAHIM KINTOMA</AccountName>
                <Name>ADAN IBRAHIM KINTOMA</Name>
                <MemberNo>0000127</MemberNo>
                <PhoneNo>+254706405989</PhoneNo>
            </Account>
             */

            /*
            <OTHER_DETAILS>
                <PESA_OTHER_DETAILS>
                    <KYC_DETAILS>
                        <RESPONSE>
                            <KYC TYPE="MSISDN">
                                <IDENTIFIER>254720000000</IDENTIFIER>
                                <ACCOUNT>254720000000</ACCOUNT>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS/>
                            </KYC>
                            <KYC TYPE="NATIONAL_ID">
                                <IDENTIFIER>1232131131</IDENTIFIER>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS/>
                            </KYC>
                            <KYC TYPE="ACCOUNT_NO">
                                <IDENTIFIER>10101010101</IDENTIFIER>
                                <ACCOUNT>10101010101</ACCOUNT>
                                <NAME>Peter Jones</NAME>
                                <OTHER_DETAILS/>
                            </KYC>
                        </RESPONSE>
                    </KYC_DETAILS>
                </PESA_OTHER_DETAILS>
            </OTHER_DETAILS>
             */


            Element elPesaOtherDetails = null;

            String strAccountNo = "";
            String strAccountType = "";
            String strAccountName = "";
            String strAccountMemberNo = "";
            String strPhoneNo = "";
            String strAccountStatus = "NOT_FOUND";

            if (!strAccountNumberXML.equals("")) {
                InputSource source = new InputSource(new StringReader(strAccountNumberXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                strAccountNo = configXPath.evaluate("Account/AccountNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountType = configXPath.evaluate("Account/AccountName", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = configXPath.evaluate("Account/Name", xmlDocument, XPathConstants.STRING).toString();
                strAccountMemberNo = configXPath.evaluate("Account/MemberNo", xmlDocument, XPathConstants.STRING).toString();
                strPhoneNo = configXPath.evaluate("Account/PhoneNo", xmlDocument, XPathConstants.STRING).toString();
                strAccountName = Utils.toTitleCase(strAccountName);
                strAccountStatus = "FOUND";

                String strBeneficiaryType = "";
                if (theSource.equals("Mobile")) {
                    strBeneficiaryType = "MSISDN";
                } else if (theSource.equals("ID")) {
                    strBeneficiaryType = "NATIONAL_ID";
                }

                elPesaOtherDetails = doc.createElement("PESA_OTHER_DETAILS");

                Element elKYCDetails = doc.createElement("KYC_DETAILS");
                elPesaOtherDetails.appendChild(elKYCDetails);

                Element elKYCResponse = doc.createElement("RESPONSE");
                elKYCDetails.appendChild(elKYCResponse);

                Element elKYC = doc.createElement("KYC");
                elKYC.setAttribute("TYPE", strBeneficiaryType);
                elKYCResponse.appendChild(elKYC);

                Element elIdentifier = doc.createElement("IDENTIFIER");
                elIdentifier.setTextContent(theAccount);
                elKYC.appendChild(elIdentifier);

                Element elAccount = doc.createElement("ACCOUNT");
                elAccount.setTextContent(strAccountNo);
                elKYC.appendChild(elAccount);

                Element elName = doc.createElement("NAME");
                elName.setTextContent(strAccountName);
                elKYC.appendChild(elName);

                Element elOtherDetails = doc.createElement("OTHER_DETAILS");
                elKYC.appendChild(elOtherDetails);
            }
            return elPesaOtherDetails;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static String trimString(String theInput, int theLength) {
        if (theInput.length() > theLength) {
            theInput = theInput.substring(0, theLength);
        }
        return theInput;
    }

    public static String getStringBeforeSpace(String input) {
        // Find the index of the first space
        int spaceIndex = input.indexOf(' ');
        // If no space is found, return the original string
        if (spaceIndex == -1) {
            return input;
        }
        // Return the substring from the start to the first space
        return input.substring(0, spaceIndex);
    }
}
