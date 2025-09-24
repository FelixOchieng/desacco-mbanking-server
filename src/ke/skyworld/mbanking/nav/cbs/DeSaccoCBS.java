package ke.skyworld.mbanking.nav.cbs;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreArrayList;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.Misc;
import ke.co.skyworld.smp.utility_items.counters.Watch;
import ke.co.skyworld.smp.utility_items.data_formatting.Converter;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.w3c.dom.Document;

import javax.net.ssl.*;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.UUID;

public class DeSaccoCBS {

    static {
        try {
            disableSSLVerification();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void disableSSLVerification() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    public static TransactionWrapper<FlexicoreHashMap> getMemberDetails(String theIdentifierType, String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_MEMBER_DETAILS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        TransactionWrapper<FlexicoreHashMap> resultWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (resultWrapper.hasErrors()) {
            return resultWrapper;
        }

        FlexicoreHashMap resultMap = resultWrapper.getSingleRecord();

        if (resultMap == null) {
            resultWrapper.setHasErrors(true);
            resultWrapper.addError("Failed to fetch customer details.");
            resultWrapper.addMessage("Result from CBS could not be parsed.");
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
            return resultWrapper;
        }

        String requestStatus = resultMap.getStringValue("request_status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.addError("Customer not found.");
            resultWrapper.addMessage("Customer with identifier type '" + theIdentifierType + "' and identifier '" + theIdentifier + "' not found in CBS.");
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreHashMap customerDetails = resultMap.getFlexicoreHashMap("response_payload");

        if (customerDetails == null || customerDetails.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.addError("Customer not found.");
            resultWrapper.addMessage("Customer with identifier type '" + theIdentifierType + "' and identifier '" + theIdentifier + "' not found in CBS.");
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        customerDetails.putValue("identity_document_reference_xml", null);
        resultWrapper.setData(customerDetails);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreArrayList> getMemberDepositAccounts(String theIdentifierType, String theIdentifier, String theAccountType, boolean applyCharges) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "GET_DEPOSIT_ACCOUNTS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("account_type", theAccountType)
                        .putValue("apply_charges", applyCharges)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        TransactionWrapper<FlexicoreArrayList> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("request_status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreArrayList customerAccounts = apiResponseMap.getFlexicoreArrayList("response_payload");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getAccountDetails(String theIdentifierType, String theIdentifier, String theAccountNumber) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "VALIDATE_ACCOUNT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("account_number", theAccountNumber)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getDepositAccountBalance(String theIdentifierType, String theIdentifier, String theAccountNumber, String theSourceApplication) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "BALANCE_ENQUIRY";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("account_number", theAccountNumber)
                        .putValue("source_application", theSourceApplication)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getAccountMiniStatement(String theIdentifierType, String theIdentifier,
                                                                               String theAccountNumber, String theNumberOfTransactions) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "ACCOUNT_MINI_STATEMENT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("account_number", theAccountNumber)
                        .putValue("number_of_transactions", theNumberOfTransactions)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getAccountFullStatement(String theIdentifierType, String theIdentifier,
                                                                               String theAccountNumber,
                                                                               String theNumberOfTransactions,
                                                                               String theStartDate,
                                                                               String theEndDate) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "ACCOUNT_FULL_STATEMENT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("account_number", theAccountNumber)
                        .putValue("start_date", theStartDate)
                        .putValue("end_date", theEndDate)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> withdrawal(String theIdentifierType,
                                                                  String theIdentifier,
                                                                  String theOriginatorId,
                                                                  String theProductId,
                                                                  String thePesaType,
                                                                  String theAction,
                                                                  String theCommand,
                                                                  FlexicoreHashMap theInitiatorDetailsMap,
                                                                  FlexicoreHashMap theSourceDetailsMap,
                                                                  FlexicoreHashMap theSenderDetailsMap,
                                                                  FlexicoreHashMap theReceiverDetailsMap,
                                                                  FlexicoreHashMap theBeneficiaryDetailsMap,
                                                                  double theAmount,
                                                                  String theCategory,
                                                                  String theTransactionDescription,
                                                                  String theSourceReference,
                                                                  String theRequestApplication,
                                                                  String theSourceApplication,
                                                                  String theWithdrawalType) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "WITHDRAWAL";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", theOriginatorId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theOriginatorId)
                        .putValue("product_id", theProductId)
                        .putValue("pesa_type", thePesaType)
                        .putValue("action", theAction)
                        .putValue("command", theCommand)
                        .putValue("transaction_initiator_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theInitiatorDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theInitiatorDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theInitiatorDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theInitiatorDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theInitiatorDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theInitiatorDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_source_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theSourceDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theSourceDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theSourceDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theSourceDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theSourceDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theSourceDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_sender_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theSenderDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theSenderDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theSenderDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theSenderDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theSenderDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theSenderDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_receiver_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theReceiverDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theReceiverDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theReceiverDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theReceiverDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theReceiverDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theReceiverDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_beneficiary_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theBeneficiaryDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theBeneficiaryDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theBeneficiaryDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theBeneficiaryDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theBeneficiaryDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theBeneficiaryDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("amount", theAmount)
                        .putValue("category", theCategory)
                        .putValue("transaction_description", theTransactionDescription)
                        .putValue("source_reference", theSourceReference)
                        .putValue("request_application", theRequestApplication)
                        .putValue("source_application", theSourceApplication)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction + " - " + theWithdrawalType);
    }

    public static TransactionWrapper<FlexicoreHashMap> withdrawalResult(String theIdentifierType,
                                                                        String theIdentifier,
                                                                        String theOriginatorId,
                                                                        String theTransactionStatus,
                                                                        String theTransactionStatusDescription,
                                                                        String theBeneficiaryIdentifierType,
                                                                        String theBeneficiaryIdentifier,
                                                                        String theBeneficiaryName,
                                                                        String theBeneficiaryOtherDetails,
                                                                        String theBeneficiaryReference,
                                                                        String theTransactionDateTime) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "WITHDRAWAL_RESULT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", theOriginatorId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theOriginatorId)
                        .putValue("transaction_status", theTransactionStatus)
                        .putValue("transaction_status_description", theTransactionStatusDescription)
                        .putValue("transaction_beneficiary_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theBeneficiaryIdentifierType)
                                .putValue("identifier", theBeneficiaryIdentifier)
                                .putValue("name", theBeneficiaryName)
                                .putValue("other_details", theBeneficiaryOtherDetails)
                        )
                        .putValue("beneficiary_reference", theBeneficiaryReference)
                        .putValue("transaction_date_time", theTransactionDateTime)
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction + "-" + theTransactionStatus);
    }

    public static TransactionWrapper<FlexicoreArrayList> getDividendPayslipYears(String theIdentifierType, String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "GET_DIVIDEND_PAYSLIP_YEARS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                );

        TransactionWrapper<FlexicoreArrayList> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreArrayList customerAccounts = apiResponseMap.getFlexicoreArrayList("data");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getDividendPayslip(String theIdentifierType, String theIdentifier, String period) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "GET_DIVIDEND_PAYSLIP";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("period", period)
                );

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreHashMap customerAccounts = apiResponseMap.getFlexicoreHashMap("data");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }


    public static TransactionWrapper<FlexicoreHashMap> cashDeposit(String theIdentifierType,
                                                                   String theIdentifier,
                                                                   String theOriginatorId,
                                                                   String theProductId,
                                                                   String thePesaType,
                                                                   String theAction,
                                                                   String theCommand,
                                                                   String theSensitivity,
                                                                   String theCharge,
                                                                   FlexicoreHashMap theInitiatorDetailsMap,
                                                                   FlexicoreHashMap theSourceDetailsMap,
                                                                   FlexicoreHashMap theSenderDetailsMap,
                                                                   FlexicoreHashMap theReceiverDetailsMap,
                                                                   FlexicoreHashMap theBeneficiaryDetailsMap,
                                                                   double theAmount,
                                                                   String theCategory,
                                                                   String theTransactionDescription,
                                                                   String theSourceReference,
                                                                   String theRequestApplication,
                                                                   String theSourceApplication) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "DEPOSIT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theOriginatorId)
                        .putValue("product_id", theProductId)
                        .putValue("pesa_type", thePesaType)
                        .putValue("action", theAction)
                        .putValue("command", theCommand)
                        .putValue("sensitivity", theSensitivity)
                        .putValue("charge", theCharge)
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("transaction_initiator_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theInitiatorDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theInitiatorDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theInitiatorDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theInitiatorDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theInitiatorDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theInitiatorDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_source_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theSourceDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theSourceDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theSourceDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theSourceDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theSourceDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theSourceDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_sender_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theSenderDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theSenderDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theSenderDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theSenderDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theSenderDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theSenderDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_receiver_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theReceiverDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theReceiverDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theReceiverDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theReceiverDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theReceiverDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theReceiverDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("transaction_beneficiary_details", new FlexicoreHashMap()
                                .putValue("identifier_type", theBeneficiaryDetailsMap.getStringValueOrIfNull("identifier_type", ""))
                                .putValue("identifier", theBeneficiaryDetailsMap.getStringValueOrIfNull("identifier", ""))
                                .putValue("account", theBeneficiaryDetailsMap.getStringValueOrIfNull("account", ""))
                                .putValue("name", theBeneficiaryDetailsMap.getStringValueOrIfNull("name", ""))
                                .putValue("reference", theBeneficiaryDetailsMap.getStringValueOrIfNull("reference", ""))
                                .putValue("other_details", theBeneficiaryDetailsMap.getStringValueOrIfNull("other_details", ""))
                        )
                        .putValue("amount", theAmount)
                        .putValue("category", theCategory)
                        .putValue("transaction_description", theTransactionDescription)
                        .putValue("source_reference", theSourceReference)
                        .putValue("request_application", theRequestApplication)
                        .putValue("source_application", theSourceApplication)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> internalFundsTransfer(
            String theIdentifierType,
            String theIdentifier,
            String theOriginatorId,
            String theSourceAccount,
            String theDestinationAccount,
            double theAmount,
            String theTransactionDescription,
            String theSourceReference,
            String theRequestApplication,
            String theSourceApplication) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "IFT_ACCOUNT_TO_ACCOUNT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theOriginatorId)
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("source_account", theSourceAccount)
                        .putValue("destination_account", theDestinationAccount)
                        .putValue("amount", theAmount)
                        .putValue("transaction_description", theTransactionDescription)
                        .putValue("source_reference", theSourceReference)
                        .putValue("request_application", theRequestApplication)
                        .putValue("source_application", theSourceApplication)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> loanPaymentViaSavings(
            String theIdentifierType,
            String theIdentifier,
            String theOriginatorId,
            String theSourceAccount,
            String theDestinationAccount,
            double theAmount,
            String theTransactionDescription,
            String theSourceReference,
            String theRequestApplication,
            String theSourceApplication) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "IFT_LOAN_REPAYMENT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theOriginatorId)
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("source_account", theSourceAccount)
                        .putValue("destination_account", theDestinationAccount)
                        .putValue("amount", theAmount)
                        .putValue("transaction_description", theTransactionDescription)
                        .putValue("source_reference", theSourceReference)
                        .putValue("request_application", theRequestApplication)
                        .putValue("source_application", theSourceApplication)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanMiniStatement(String theIdentifierType, String theIdentifier,
                                                                            String theLoanSerialNumber,
                                                                            String theNumberOfTransactions) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "LOAN_MINI_STATEMENT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_serial_number", theLoanSerialNumber)
                        .putValue("number_of_transactions", theNumberOfTransactions)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanFullStatement(String theIdentifierType,
                                                                            String theIdentifier,
                                                                            String theLoanSerialNumber,
                                                                            String theNumberOfTransactions,
                                                                            String theStartDate,
                                                                            String theEndDate) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "LOAN_FULL_STATEMENT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_serial_number", theLoanSerialNumber)
                        .putValue("number_of_transactions", theNumberOfTransactions)
                        .putValue("start_date", theStartDate)
                        .putValue("end_date", theEndDate)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }
    public static TransactionWrapper<FlexicoreHashMap> getLoanRepaymentSchedule(String theIdentifierType, String theIdentifier,String loanSerialNumber) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "LOAN_REPAYMENT_SCHEDULE";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_serial_number", loanSerialNumber)
                );

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreHashMap customerAccounts = apiResponseMap.getFlexicoreHashMap("data");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> getCharges(
            String theIdentifierType,
            String theIdentifier,
            String theAccountNumber,
            String theChargeAction,
            double theAmount) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "GET_CHARGES";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("account_number", theAccountNumber)
                        .putValue("charge_action", theChargeAction)
                        .putValue("amount", theAmount)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        /*TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        resultWrapper.setData(new FlexicoreHashMap()
                .putValue("api_request_id", strRequestId)
                .putValue("request_status", "SUCCESS")
                .putValue("response_payload", new FlexicoreHashMap()
                        .putValue("charge_amount", 21.00)
                )
        );

        return resultWrapper;*/
    }

    public static TransactionWrapper<FlexicoreHashMap> checkLoanLimit(String theIdentifierType,
                                                                      String theIdentifier,
                                                                      String theLoanTypeId) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "CHECK_LOAN_LIMIT";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", strRequestId)
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_type_id", theLoanTypeId)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreArrayList> getMemberLoansInService(String theIdentifierType, String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "GET_LOANS_IN_SERVICE";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        TransactionWrapper<FlexicoreArrayList> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("request_status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreArrayList customerAccounts = apiResponseMap.getFlexicoreArrayList("response_payload");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> loanApplication(String theIdentifierType,
                                                                       String theIdentifier,
                                                                       String theLoanTypeId,
                                                                       double theAmount,
                                                                       String theSourceReference,
                                                                       String theRequestApplication,
                                                                       String theTransactionDateTime,
                                                                       String theMobileLoanPurpose
    ) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "LOAN_APPLICATION";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theSourceReference)
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_type_id", theLoanTypeId)
                        .putValue("amount", theAmount)
                        .putValue("source_reference", theSourceReference)
                        .putValue("request_application", theRequestApplication)
                        .putValue("transaction_date_time", theTransactionDateTime)
                        .putValue("mobile_loan_purpose", theMobileLoanPurpose)
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> loanApplicationWithGuarantors(String theIdentifierType,
                                                                       String theIdentifier,
                                                                       String theLoanTypeId,
                                                                       double theAmount,
                                                                       String theSourceReference,
                                                                       String theRequestApplication,
                                                                       FlexicoreArrayList theGuarantors,
                                                                       String theTransactionDateTime,
                                                                       String theSectorCode,
                                                                       String theSubSectorCode,
                                                                       String thePurposeCode
    ) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "CREATE_LOAN_APPLICATION_REQUEST";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("originator_id", theSourceReference)
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_type_id", theLoanTypeId)
                        .putValue("amount", theAmount)
                        .putValue("source_reference", theSourceReference)
                        .putValue("request_application", theRequestApplication)
                        .putValue("guarantors", theGuarantors)
                        .putValue("transaction_date_time", theTransactionDateTime)
                        .putValue("sector", theSectorCode)
                        .putValue("sub_sector", theSubSectorCode)
                        .putValue("mobile_loan_purpose", thePurposeCode)
                        .putValue("installments", null)
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }


    public static TransactionWrapper<FlexicoreHashMap> getLoanAccountBalance(String theIdentifierType, String theIdentifier, String theAccountNumber, String theSourceApplication) {

        String strRequestId = UUID.randomUUID().toString();


        String strAction = "LOAN_BALANCE_ENQUIRY";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("loan_serial_number", theAccountNumber)
                        .putValue("source_application", theSourceApplication)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreArrayList> getLoanTypes(String theIdentifierType, String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOAN_TYPES";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        TransactionWrapper<FlexicoreArrayList> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("request_status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreArrayList customerAccounts = apiResponseMap.getFlexicoreArrayList("response_payload");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> addDividendsCapitalizationInstruction(String theIdentifierType,
                                                                                             String theIdentifier,
                                                                                             FlexicoreHashMap instructionsMap) {
        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "DIVIDENDS_CAPITALIZATION";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .copyFrom(instructionsMap)
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getDividendsCapitalizationInstruction(
            String theIdentifierType,
            String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        

        String strAction = "GET_DIVIDEND_CAPITALIZATION_INSTRUCTIONS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

    }

    public static TransactionWrapper<FlexicoreArrayList> callBC365Service(String theServiceName) {

        String strRequestId = UUID.randomUUID().toString();
        String strAction = "CALL_SERVICE";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("service_id", theServiceName)
                );

        TransactionWrapper<FlexicoreArrayList> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theServiceName, theServiceName, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("request_status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreArrayList customerAccounts = apiResponseMap.getFlexicoreArrayList("response_payload");

        if (customerAccounts == null || customerAccounts.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(customerAccounts);
        return resultWrapper;
    }

    /*private static TransactionWrapper<FlexicoreHashMap> makeAPICall(String requestBody, String functionInvoked) {
        String strCredentials = DeSaccoCBSParams.getTheUsername() + ":" + DeSaccoCBSParams.getThePassword();
        strCredentials = HashUtils.base64Encode(strCredentials);

        LinkedHashMap<String, String> hmHeaders = new LinkedHashMap<>();
        hmHeaders.put("Authorization", "Basic " + strCredentials);
        hmHeaders.put("Content-Type", "application/json");

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = new TransactionWrapper<>();

        HTTPResponse httpResponse = HttpClient.httpPOST(DeSaccoCBSParams.getTheCBS_URL(), hmHeaders, null, requestBody, functionInvoked);
        System.out.println("----------------------------------------------------------");
        System.out.println(httpResponse);

        if (httpResponse.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            apiResponseWrapper.setHasErrors(true);
            apiResponseWrapper.addError("Request failed. Status Code: " + httpResponse.getResponseCode());
            apiResponseWrapper.addMessage("Unsuccessful Request. Error: " + httpResponse.getResponseMessage());
            apiResponseWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
            return apiResponseWrapper;
        }

        FlexicoreHashMap apiResponseMap = httpResponse.getResponseBodyAsMap();

        if (apiResponseMap == null) {
            apiResponseWrapper.setHasErrors(true);
            apiResponseWrapper.addError("Failed to parse CBS Response.");
            apiResponseWrapper.addMessage("Result from CBS could not be parsed.");
            apiResponseWrapper.setStatusCode(HttpsURLConnection.HTTP_INTERNAL_ERROR);
            return apiResponseWrapper;
        }

        apiResponseWrapper.setData(apiResponseMap);
        return apiResponseWrapper;
    }*/

    public static TransactionWrapper<FlexicoreHashMap> sendSoapRequest(String theIdentifierType, String theIdentifier, String theRequestUUID, String theRequestJSON, String theAction) {

        Watch watch = new Watch();
        watch.start();

        String requestXML = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:dyn="urn:microsoft-dynamics-schemas/codeunit/SkyMBankingAPI">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <dyn:HandleRequest>
                            <dyn:request/>
                        </dyn:HandleRequest>
                    </soapenv:Body>
                </soapenv:Envelope>""";

        Document document = XmlUtils.parseXml(requestXML);

        FlexicoreHashMap updateMap = new FlexicoreHashMap();
        updateMap.putValue("/Envelope/Body/HandleRequest/request", theRequestJSON);

        String theRequestBody = XmlUtils.updateXMLTags(document, updateMap);

        /*theRequestBody =  theRequestBody.replace("254790491947", "254712576168");
        theRequestBody =  theRequestBody.replace("254717650883", "254712576168");
        theRequestBody =  theRequestBody.replace("108673-03002", "21774-03857");*/

        /*theRequestBody =  theRequestBody.replace("254790491947", "254712576168");
        theRequestBody =  theRequestBody.replace("108673-03002", "21774-03857");*/

        TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

        try {

            String strURL = DeSaccoCBSParams.getSOAPURL();

            URL url = new URL(strURL);

            String strHost = extractHostFromURL(url);
            int strPort = extractPortFromURL(url);

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            /*System.out.println("Win auth available: " + WinHttpClients.isWinAuthAvailable());
            if (WinHttpClients.isWinAuthAvailable()) {
                ClassicHttpRequest request = new HttpGet("http://localhost:8080/ssotestwebapp/webapp");
                CloseableHttpClient client = WinHttpClients.createDefault();
                HttpClientContext context = HttpClientContext.create();

                Collection<String> targetPreferredAuthSchemes = Collections.unmodifiableList(
                        Arrays.asList(StandardAuthScheme.NTLM, StandardAuthScheme.KERBEROS, StandardAuthScheme.SPNEGO,
                                StandardAuthScheme.BEARER, StandardAuthScheme.DIGEST, StandardAuthScheme.BASIC));
                RequestConfig config = RequestConfig.custom().setTargetPreferredAuthSchemes(targetPreferredAuthSchemes)
                        .build();
                context.setRequestConfig(config);

                client.execute(request, context, response -> {
                    System.out.println("----------------------------------------");
                    System.out.println(new StatusLine(response));

                    HttpEntity entity = response.getEntity();
                    String s = EntityUtils.toString(entity);
                    System.out.println(s);
                    EntityUtils.consume(response.getEntity());
                    return null;
                });
            }*/

            credentialsProvider.setCredentials(
                    new AuthScope(new AuthScope(strHost, strPort)),
                    new NTCredentials(DeSaccoCBSParams.getUser(),
                            DeSaccoCBSParams.getPassword().toCharArray(), ".", DeSaccoCBSParams.getDomain()));

            // credentialsProvider.setCredentials(
            //         new AuthScope(new AuthScope(strHost, strPort)), new UsernamePasswordCredentials(DeSaccoCBSParams.getUser(),
            //                 DeSaccoCBSParams.getPassword().toCharArray()));

            if (DeSaccoCBSParams.isLogRequestEnabled()) {
                if (!theAction.equalsIgnoreCase("CALL_SERVICE")) {
                    System.out.println("\n----------------| REQUEST |----------------\n" +
                                       "Request Log Ref : " + theRequestUUID + "\n" +
                                       "Requester Type  : " + theIdentifierType + "\n" +
                                       "Requester       : " + theIdentifier + "\n" +
                                       "Request Action  : " + theAction + "\n" +
                                       "Request Body    : " + "\n" +
                                       "-------------------------------------------\n" + theRequestBody + "\n");
                }
            } else {

                if (!theAction.equalsIgnoreCase("CALL_SERVICE")) {
                    System.out.println("\n----------------| REQUEST |----------------\n" +
                                       "Request Log Ref : " + theRequestUUID + "\n" +
                                       "Requester Type  : " + theIdentifierType + "\n" +
                                       "Requester       : " + theIdentifier + "\n" +
                                       "Request Action  : " + theAction + "\n" +
                                       "Request Body    : " + "\n" +
                                       "-------------------------------------------\n");
                }
            }

            try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build()) {
                HttpPost httpPost = new HttpPost(strURL);
                httpPost.setEntity(new StringEntity(theRequestBody));
                httpPost.setHeader("Content-type", "application/xml");
                httpPost.setHeader("SOAPAction", DeSaccoCBSParams.getSOAPAction());

                return httpClient.execute(httpPost, new SOAPResponseHandler(theRequestUUID, theIdentifierType, theIdentifier, theAction, watch));

            } catch (Exception e) {
                resultWrapper.setHasErrors(true);
                resultWrapper.copyFrom(Misc.getTransactionWrapperStackTrace(e));
                resultWrapper.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return resultWrapper;
            }

        } catch (Exception e) {
            resultWrapper.setHasErrors(true);
            resultWrapper.copyFrom(Misc.getTransactionWrapperStackTrace(e));
            resultWrapper.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            return resultWrapper;
        }
    }

    public static String extractHostFromURL(URL url) {
        return url.getHost();
    }

    // Function to extract the port from a URL
    public static int extractPortFromURL(URL url) {
        int port = url.getPort();

        if (port == -1) {
            if (url.getProtocol().equals("http")) {
                port = 80;
            } else if (url.getProtocol().equals("https")) {
                port = 443;
            }
        }

        return port;
    }


    public static String getTheIdentifier(String theIdentifierType, String theIdentifier) {
        if (theIdentifierType != null && theIdentifierType.equalsIgnoreCase("MSISDN")) {
            if (!theIdentifier.contains("+")) {
                theIdentifier = "+" + theIdentifier;
            }
        }

        return theIdentifier;
    }

    public static TransactionWrapper<FlexicoreHashMap> createStandingOrder(String theIdentifierType, String theIdentifier, String theSourceAccount, String theSourceAccountType, int theFrequency, int theDuration, String theStartDate, String theStoName, double theAmount, String theDestinationAccountType, String theDestinationAccount) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "CREATE_STANDING_ORDER_REQUEST";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("source_account", theSourceAccount)
                        .putValue("source_account_type", theSourceAccountType)
                        .putValue("frequency", theFrequency)
                        .putValue("start_date", theStartDate)
                        .putValue("duration", theDuration)
                        .putValue("description", theStoName)
                        .putValue("amount", theAmount)
                        .putValue("destination_account_type", theDestinationAccountType)
                        .putValue("destination_account_no", theDestinationAccount)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getStandingOrderRequests(String theIdentifierType, String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_STANDING_ORDER_REQUESTS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }


    public static TransactionWrapper<FlexicoreHashMap> cancelStandingOrderRequest(String theIdentifierType, String theIdentifier, String theStandingOrderDetails) {
        String strRequestId = UUID.randomUUID().toString();

        String strAction = "CANCEL_STANDING_ORDER_REQUEST";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                        .putValue("sto_serial_no", theStandingOrderDetails)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getAccountFullStatementPdf(String strIdentifierType, String strIdentifier, String strAccount, String strStartDate, String strEndDate) {
        String strRequestId = UUID.randomUUID().toString();

        String strAction = "ACCOUNT_STATEMENT_PDF";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("account_number", strAccount)
                        .putValue("start_date", strStartDate)
                        .putValue("end_date", strEndDate)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanFullStatementPdf(String strIdentifierType, String strIdentifier, String strAccount, String strStartDate, String strEndDate) {
        String strRequestId = UUID.randomUUID().toString();

        String strAction = "LOAN_STATEMENT_PDF";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("account_number", strAccount)
                        .putValue("start_date", strStartDate)
                        .putValue("end_date", strEndDate)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanGuarantors(String strIdentifierType, String strIdentifier, String strLoanSerialNumber) {
        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOAN_GUARANTORS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoansGuaranteed(String strIdentifierType, String strIdentifier) {
        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOANS_GUARANTEED";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> guarantorSubstitution(String strIdentifierType, String strIdentifier, String strLoanSerialNumber, String strOldGuarantorIdentifier, String strNewGuarantorIdentifier, String strReason) {
        /** 003830
         * REQ:
         * {
         *     "action": "GUARANTOR_SUBSTITUTION",
         *     "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *     "payload": {
         *       "identifier_type": "CUSTOMER_NO",
         *       "identifier": "000023",
         *       "loan_serial_number": "LN00721572",
         *       "old_guarantor": "000049",
         *       "new_guarantor": "038281",
         *       "reason": "Old guarantor requested release"
         *     }
         *   }
         *
         * RES:
         * {
         *     "request_status": "SUCCESS",
         *     "response_payload": {}
         *   }
         */

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GUARANTOR_SUBSTITUTION";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                        .putValue("old_guarantor", strOldGuarantorIdentifier)
                        .putValue("new_guarantor", strNewGuarantorIdentifier)
                        .putValue("reason", strReason)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getGuarantorSubstitutionRequests(String strIdentifierType, String strIdentifier) {
        /** 003830
         * REQ:
         * {
         *     "action": "GET_GUARANTOR_SUBSTITUTION_REQUESTS",
         *     "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *     "payload": {
         *       "identifier_type": "CUSTOMER_NO",
         *       "identifier": "038281"
         *     }
         *   }
         *
         * RES:
         * {
         *     "request_status": "SUCCESS",
         *     "response_payload": {
         *       "guarantor_substitution_requests": [
         *         {
         *           "full_name": "MIRIAM KARIMI MAJAU",
         *           "loan_serial_number": "LN00721572",
         *           "loan_type_id": "PREMIUM",
         *           "loan_type_name": "PREMIUM LOAN",
         *           "requester_full_name": "PETERSON MUTWIRI KINYUA",
         *           "requester_identifier_type": "MSISDN",
         *           "requester_identifier": "+254726563563",
         *           "amount_to_guarantee": 0.0,
         *           "loan_amount": 1680000.0,
         *           "loan_balance": 1662450.0,
         *           "guarantorship_status": "PENDING",
         *           "guarantorship_status_date": "2025-06-28"
         *         }
         *       ]
         *     }
         *   }
         */

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_GUARANTOR_SUBSTITUTION_REQUESTS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoansPendingGuarantorship(String strIdentifierType, String strIdentifier) {
        /** 003830
         * REQ:
         * {
         *   "action": "GET_LOANS_PENDING_GUARANTORSHIP",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "000023"
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "loans_pending_guarantorship": [
         *     {
         *       "loan_serial_number": "{edb1b690-9bd7-4893-8ab3-5850c70681dc}",
         *       "loan_type_id": "SAL ADV",
         *       "loan_type_name": "Salary Advance",
         *       "loan_amount": 15000.0
         *     },
         *     {
         *       "loan_serial_number": "75467dbe-7795-413d-bec3-c013da971510",
         *       "loan_type_id": "SAL ADV",
         *       "loan_type_name": "Salary Advance",
         *       "loan_amount": 12222.0
         *     }
         *   ]
         * }
         * }
         */

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOANS_PENDING_GUARANTORSHIP";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanGuarantorshipRequests(String strIdentifierType, String strIdentifier) {
        /** 003830
         * REQ:
         * {
         *   "action": "GET_LOAN_GUARANTORSHIP_REQUESTS",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "003830"
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "guarantorship_requests": [
         *     {
         *       "loan_serial_number": "{EDB1B690-9BD7-4893-8AB3-5850C70681DC}",
         *       "loan_type_id": "SAL ADV",
         *       "loan_type_name": "Salary Advance",
         *       "loan_amount": 15000.0,
         *       "proposed_amount_to_guarantee": 3000.0,
         *       "requester_full_name": "",
         *       "requester_identifier_type": "MSISDN",
         *       "requester_identifier": "+254726563563",
         *       "guarantorship_status": "PENDING",
         *       "guarantorship_status_date": "2025-06-25"
         *     }
         *   ]
         * }
         * }
         */

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOAN_GUARANTORSHIP_REQUESTS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("transaction_date_time", DateTime.getCurrentDateTime("yyyy-MM-dd HH:mm:ss"))
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> actionGuarantorSubstitutionRequest(String strIdentifierType, String strIdentifier, String strLoanSerialNumber, String guarantorshipStatus, String strRequestDate) {
        /** 003830
         * REQ:
         * {
         *     "action": "ACTION_GUARANTOR_SUBSTITUTION_REQUEST",
         *     "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *     "payload": {
         *       "identifier_type": "CUSTOMER_NO",
         *       "identifier": "038281",
         *       "loan_serial_number": "LN00721572",
         *       "guarantorship_status": "ACCEPTED",
         *       "guarantorship_status_date": "2021-01-02 13:45:15"
         *
         *     }
         *   }
         *
         * RES:
         * {
         *     "request_status": "SUCCESS",
         *     "response_payload": {
         *       "status": "SUCCESS",
         *       "status_description": "Success"
         *     }
         *   }
         */

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "ACTION_GUARANTOR_SUBSTITUTION_REQUEST";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                        .putValue("guarantorship_status", guarantorshipStatus)
                        .putValue("guarantorship_status_date", strRequestDate)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> getLoanBufferGuarantors(String strIdentifierType, String strIdentifier, String strLoanSerialNumber) {
        /** 003830
         * REQ:
         * {
         *   "action": "GET_LOAN_BUFFER_GUARANTORS",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "000023",
         *     "loan_serial_number": "{EDB1B690-9BD7-4893-8AB3-5850C70681DC}"
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "transaction_reference": "00000000-0000-0000-0000-000000000000",
         *   "loan_serial_number": "",
         *   "loan_type_id": "",
         *   "loan_name": "",
         *   "loan_amount": 0.0,
         *   "guarantors": [
         *     {
         *       "amount_guaranteed": 5000.0,
         *       "full_name": "DEREK MUTENDE TEST",
         *       "identifier_type": "MSISDN",
         *       "identifier": "+254713000249",
         *       "member_number": "000024"
         *       "guarantorship_status": "PENDING",
         *       "guarantorship_status_date": "2025-06-25"
         *     },
         *     {
         *       "amount_guaranteed": 2000.0,
         *       "full_name": "FELIX OCHIENG TEST",
         *       "identifier_type": "MSISDN",
         *       "identifier": "+254790342037",
         *       "member_number": "000023"
         *       "guarantorship_status": "ACCEPTED",
         *       "guarantorship_status_date": "2025-06-25"
         *     }
         *   ]
         * }
         * }
         */

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOAN_BUFFER_GUARANTORS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> addLoanGuarantor(String strIdentifierType, String strIdentifier, String strGuarantorIdentifierType, String strGuarantorIdentifier, String strLoanSerialNumber, double dblProposedAmountToGuarantee, String strRequestDate) {
        String strRequestId = UUID.randomUUID().toString();

        /** 003830
         * REQ:
         * {
         *   "action": "ADD_LOAN_GUARANTOR",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "000023",
         *     "guarantor_identifier_type": "MSISDN",
         *     "guarantor_identifier": "254713000249",
         *     "loan_serial_number": "{EDB1B690-9BD7-4893-8AB3-5850C70681DC}",
         *     "proposed_amount_to_guarantee": 3000.00,
         *     "request_date_time": "2021-01-04 10:15:35"
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "message": "Guarantor added successfully"
         * }
         * }
         *
         * {
         *   "request_status": "FAILED",
         *   "response_payload": {
         *   "data": {
         *     "message": "This member cannot guarantee you at the moment"
         *   }
         * }
         * }
         */

        String strAction = "ADD_LOAN_GUARANTOR";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("guarantor_identifier_type", strGuarantorIdentifierType)
                        .putValue("guarantor_identifier", strGuarantorIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                        .putValue("proposed_amount_to_guarantee", dblProposedAmountToGuarantee)
                        .putValue("request_date_time", strRequestDate)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> removeLoanGuarantor(String strIdentifierType, String strIdentifier, String strGuarantorIdentifierType, String strGuarantorIdentifier, String strLoanSerialNumber, String strRequestDate) {
        String strRequestId = UUID.randomUUID().toString();

        /** 003830
         * REQ:
         * {
         *   "action": "REMOVE_LOAN_GUARANTOR",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "000023",
         *     "guarantor_identifier_type": "MSISDN",
         *     "guarantor_identifier": "254713000249",
         *     "loan_serial_number": "{EDB1B690-9BD7-4893-8AB3-5850C70681DC}",
         *     "request_date_time": "2021-01-04 10:15:35"
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "message": "Request was successful"
         * }
         * }
         */

        String strAction = "REMOVE_LOAN_GUARANTOR";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("guarantor_identifier_type", strGuarantorIdentifierType)
                        .putValue("guarantor_identifier", strGuarantorIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                        .putValue("request_date_time", strRequestDate)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> actionLoanGuarantorshipRequest(String strIdentifierType, String strIdentifier, String strLoanSerialNumber, double dblProposedAmountToGuarantee, String guarantorshipStatus, String strRequestDate) {
        String strRequestId = UUID.randomUUID().toString();

        /** 003830
         * REQ:
         * {
         *   "action": "ACTION_LOAN_GUARANTORSHIP_REQUESTS",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "003830",
         *     "loan_serial_number": "{EDB1B690-9BD7-4893-8AB3-5850C70681DC}",
         *     "amount_guaranteed": 2000.00,
         *     "guarantorship_status": "ACCEPTED",
         *     "guarantorship_status_date": "2021-01-02 13:45:15"
         *
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "message": "Request was successful"
         * }
         * }
         */

        String strAction = "ACTION_LOAN_GUARANTORSHIP_REQUESTS";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("loan_serial_number", strLoanSerialNumber)
                        .putValue("amount_guaranteed", dblProposedAmountToGuarantee)
                        .putValue("guarantorship_status", guarantorshipStatus)
                        .putValue("guarantorship_status_date", strRequestDate)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

    public static TransactionWrapper<FlexicoreHashMap> referralMemberOnboarding(String strIdentifierType, String strIdentifier, String strOnboardingType, String strFullName, String strPhoneNumber, String strEmail, String strRelationship) {
        String strRequestId = UUID.randomUUID().toString();

        /** 003830
         * REQ:
         * {
         *     "action":"INITIATE_MEMBER_ONBOARDING",
         *     "api_request_id":"ac80e6f7-f8cf-422c-b11d-499d26d161fc",
         *     "payload":{
         *         "identifier_type":"CUSTOMER_NO",
         *         "identifier":"000023",
         *         "onboarding_type":"REFERRAL_MEMBER_ONBOARDING",
         *         "full_name":"Martin Njagi",
         *         "phone_number":"254728152774",
         *         "email": "test@test.com",
         *         "relationship": "FAMILY/SON(DAUGHTER)/COLEAGUE/SPOUSE"
         *     }
         * }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "message": "Request was successful"
         * }
         * }
         */

        String strAction = "INITIATE_MEMBER_ONBOARDING";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                        .putValue("onboarding_type", strOnboardingType)
                        .putValue("full_name", strFullName)
                        .putValue("phone_number", strPhoneNumber)
                        .putValue("email", strEmail)
                        .putValue("relationship", strRelationship)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }


    public static TransactionWrapper<FlexicoreArrayList> getLoanPurposes(String theIdentifierType, String theIdentifier) {

        String strRequestId = UUID.randomUUID().toString();

        String strAction = "GET_LOAN_PURPOSES";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", theIdentifierType)
                        .putValue("identifier", theIdentifier)
                );

        TransactionWrapper<FlexicoreArrayList> resultWrapper = new TransactionWrapper<>();

        TransactionWrapper<FlexicoreHashMap> apiResponseWrapper = sendSoapRequest(theIdentifierType, theIdentifier, strRequestId, Converter.toJson(requestBody), strAction);

        if (apiResponseWrapper.hasErrors()) {
            resultWrapper.copyFrom(apiResponseWrapper);
            return resultWrapper;
        }

        FlexicoreHashMap apiResponseMap = apiResponseWrapper.getSingleRecord();

        String requestStatus = apiResponseMap.getStringValue("request_status");

        if (!requestStatus.equalsIgnoreCase("SUCCESS")) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }

        FlexicoreArrayList loanPurposes = apiResponseMap.getFlexicoreArrayList("response_payload");

        if (loanPurposes == null || loanPurposes.isEmpty()) {
            resultWrapper.setHasErrors(true);
            resultWrapper.setStatusCode(HttpsURLConnection.HTTP_NOT_FOUND);
            return resultWrapper;
        }
        resultWrapper.setData(loanPurposes);
        return resultWrapper;
    }

    public static TransactionWrapper<FlexicoreHashMap> checkGuarantorCapability(String strIdentifierType, String strIdentifier, String strLoanSerialNumber, double dblProposedAmountToGuarantee, String guarantorshipStatus, String strRequestDate) {
        String strRequestId = UUID.randomUUID().toString();

        /** 003830
         * REQ:
         * {
         *   "action": "CHECK_GUARANTORSHIP_CAPABILITY",
         *   "api_request_id": "df3e7cf5-1e4b-41ef-a22f-e755be665432",
         *   "payload": {
         *     "identifier_type": "CUSTOMER_NO",
         *     "identifier": "003830",
         *
         *   }
         * }
         *
         * RES:
         * {
         *   "request_status": "SUCCESS",
         *   "response_payload": {
         *   "message": "Request was successful"
         * }
         * }
         */

        String strAction = "CHECK_GUARANTORSHIP_CAPABILITY";

        FlexicoreHashMap requestBody = new FlexicoreHashMap()
                .putValue("action", strAction)
                .putValue("api_request_id", strRequestId)
                .putValue("payload", new FlexicoreHashMap()
                        .putValue("identifier_type", strIdentifierType)
                        .putValue("identifier", strIdentifier)
                );

        return sendSoapRequest(strIdentifierType, strIdentifier, strRequestId, Converter.toJson(requestBody), strAction);
    }

}