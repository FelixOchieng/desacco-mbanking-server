package ke.skyworld.mbanking.nav.cbs;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.utility_items.Misc;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.counters.Watch;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.net.HttpURLConnection;
import java.util.LinkedHashMap;

public class SOAPResponseHandler implements HttpClientResponseHandler<TransactionWrapper<FlexicoreHashMap>> {

    private final TransactionWrapper<FlexicoreHashMap> resultWrapper = new TransactionWrapper<>();

    private final String strRequestUUID;
    private final String strIdentifierType;
    private final String strIdentifier;
    private final String strAction;
    private final Watch watch;

    public SOAPResponseHandler(String strRequestUUID, String strIdentifierType, String strIdentifier, String strAction, Watch watch) {
        this.strRequestUUID = strRequestUUID;
        this.strIdentifierType = strIdentifierType;
        this.strIdentifier = strIdentifier;
        this.strAction = strAction;
        this.watch = watch;
    }

    @Override
    public TransactionWrapper<FlexicoreHashMap> handleResponse(ClassicHttpResponse classicHttpResponse) {

        try {
            int responseStatusCode = classicHttpResponse.getCode();

            if (classicHttpResponse.getCode() == HttpStatus.SC_UNAUTHORIZED) {
                System.out.println("-----------------| RESPONSE - (" + responseStatusCode + " - " + httpStatusCodes.get(responseStatusCode) + ") |-----------------\n" +
                                   "Request Log Ref  : " + strRequestUUID + "\n" +
                                   "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                   "Response Action  : " + strAction + "\n" +
                                   "Request Status   : ERROR\n" +
                                   "Time Taken       : " + watch.timeTaken() + "\n" +
                                   "Reason           : Authentication Failed! Invalid Credentials Provided\n" +
                                   "---------------------------------------------------------------------\n");

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(classicHttpResponse.getCode());
                resultWrapper.addError("Authentication Failed!");
                resultWrapper.addMessage("Invalid Credentials Provided");
                return resultWrapper;
            }

            HttpEntity entity = classicHttpResponse.getEntity();
            if (entity == null) {
                System.out.println("-----------------| RESPONSE - (" + responseStatusCode + " - " + httpStatusCodes.get(responseStatusCode) + ") |-----------------\n" +
                                   "Request Log Ref  : " + strRequestUUID + "\n" +
                                   "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                   "Response Action  : " + strAction + "\n" +
                                   "Request Status   : ERROR\n" +
                                   "Time Taken       : " + watch.timeTaken() + "\n" +
                                   "Response Body    : " + "\n" +
                                   "---------------------------------------------------------------------\nResult Response Entity is NULL\n");

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.addError("Result Response Entity is NULL");
                resultWrapper.addMessage("NULL Response Entity");
                return resultWrapper;
            }

            String responseBody = EntityUtils.toString(entity);

            Document document = XmlUtils.parseXml(responseBody);

            if (document == null) {
                System.out.println("-----------------| RESPONSE - (" + responseStatusCode + " - " + httpStatusCodes.get(responseStatusCode) + ") |-----------------\n" +
                                   "Request Log Ref  : " + strRequestUUID + "\n" +
                                   "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                   "Response Action  : " + strAction + "\n" +
                                   "Request Status   : ERROR\n" +
                                   "Time Taken       : " + watch.timeTaken() + "\n" +
                                   "Response Body    : " + "\n" +
                                   "---------------------------------------------------------------------\nFailed to Parse Response Body Below to XML Document: Response Body:\n" + responseBody);

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.addError("Failed to Parse Response Body to XML Document");
                resultWrapper.addMessage("NULL XML Document");
                return resultWrapper;
            }

            if (classicHttpResponse.getCode() != HttpStatus.SC_OK) {
                String strReason = classicHttpResponse.getReasonPhrase();

                Element elFaultString = XmlUtils.getElementNodeFromXpath(document, "//faultstring");

                String strResponseLog = "-----------------| RESPONSE - (" + responseStatusCode + " - " + httpStatusCodes.get(responseStatusCode) + ") |-----------------\n" +
                                        "Request Log Ref  : " + strRequestUUID + "\n" +
                                        "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                        "Response Action  : " + strAction + "\n" +
                                        "Request Status   : ERROR\n" +
                                        "Time Taken       : " + watch.timeTaken() + "\n" +
                                        "CBS Response     : " + (elFaultString == null ? "elFaultString is null": elFaultString.getTextContent()) + "\n" +
                                        "Response Body    : " + "\n" +
                                        "---------------------------------------------------------------------\n" + responseBody + "\n";

                /*if (!strAction.equalsIgnoreCase("CALL_SERVICE")) {

                }*/

                System.out.println(strResponseLog);

                Element elFaultCode = XmlUtils.getElementNodeFromXpath(document, "//faultcode");


                /*FlexicoreHashMap resultMap = new FlexicoreHashMap()
                        .putValue("status", "ERROR")
                        .putValue("description", elFaultString)
                        .putValue("errors", new FlexicoreArrayList())
                        .putValue("date_time", DateTime.getCurrentDateTime());*/

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
                resultWrapper.addError("Request Failed!");
                resultWrapper.addMessage(elFaultString == null ? "elFaultString is null": elFaultString.getTextContent());
                return resultWrapper;
            }

            Element elReturnValue = XmlUtils.getElementNodeFromXpath(document, "//return_value");

            if (elReturnValue == null) {

                String strResponseLog = "-----------------| RESPONSE - (" + responseStatusCode + " - " + httpStatusCodes.get(responseStatusCode) + ") |-----------------\n" +
                                        "Request Log Ref  : " + strRequestUUID + "\n" +
                                        "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                        "Response Action  : " + strAction + "\n" +
                                        "Request Status   : ERROR\n" +
                                        "Time Taken       : " + watch.timeTaken() + "\n" +
                                        "Message             : Could not find Element 'return_value' in the response\n" +
                                        "Response Body    : " + "\n" +
                                        "---------------------------------------------------------------------\n" + responseBody + "\n";

                if (!strAction.equalsIgnoreCase("CALL_SERVICE")) {
                    System.out.println(strResponseLog);
                }

                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                resultWrapper.addError("Could not find Element 'return_value' in the response");
                return resultWrapper;
            }

            String jsonContent = elReturnValue.getTextContent();
            FlexicoreHashMap resultMap = Misc.getBodyObjectWithGson(FlexicoreHashMap.class, jsonContent, StringRefs.APPLICATION_JSON);

            String strRequestStatus = resultMap.getStringValueOrIfNull("request_status", "");

            String strResponseLog = "-----------------| RESPONSE - (" + responseStatusCode + " - " + httpStatusCodes.get(responseStatusCode) + ") |-----------------\n" +
                                    "Request Log Ref  : " + strRequestUUID + "\n" +
                                    "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                    "Response Action  : " + strAction + "\n" +
                                    "Request Status   : SUCCESS\n" +
                                    "Time Taken       : " + watch.timeTaken() + "\n" +
                                    "Response Body    : " + "\n" +
                                    "---------------------------------------------------------------------\n";

            if (!strRequestStatus.equalsIgnoreCase("SUCCESS")) {
                strResponseLog = strResponseLog + jsonContent+"\n";
            } else {
                strResponseLog = strResponseLog + (DeSaccoCBSParams.isLogResponseEnabled() ? responseBody : "") + "\n";
            }

            if (!strAction.equalsIgnoreCase("CALL_SERVICE")) {
                System.out.println(strResponseLog);
            }

            /*if (strRequestStatus.equalsIgnoreCase("NOT_FOUND")) {
                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpStatus.SC_NOT_FOUND);
                resultWrapper.addError("Customer with identifier type '" + strIdentifierType + "' and identifier '" + strIdentifier + "' not found in CBS.");
                resultWrapper.addMessage("Customer not found");
                return resultWrapper;
            }*/

            /*if (!strRequestStatus.equalsIgnoreCase("SUCCESS") && !strRequestStatus.equalsIgnoreCase("INSUFFICIENT_BAL")) {
                resultWrapper.setHasErrors(true);
                resultWrapper.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                resultWrapper.addError("Error occurred!");
                resultWrapper.addMessage("An unexpected error occurred while processing your request");
            } else {

            }*/

            resultWrapper.setStatusCode(HttpURLConnection.HTTP_OK);
            resultWrapper.setData(resultMap);
            resultWrapper.addMessage(responseBody);

            return resultWrapper;

        } catch (Exception e) {

            String strResponseLog = "-----------------| RESP (EXCEPTION THROWN) |-----------------\n" +
                                    "Request Log Ref  : " + strRequestUUID + "\n" +
                                    "Response Entity  : " + strIdentifierType + " - " + strIdentifier + "\n" +
                                    "Response Action  : " + strAction + "\n" +
                                    "Request Status   : ERROR\n" +
                                    "Time Taken       : " + watch.timeTaken() + "\n" +
                                    "Message          : " + e.getMessage() + "\n" +
                                    "---------------------------------------------------\n";

            System.out.println(strResponseLog);

            resultWrapper.copyFrom(Misc.getTransactionWrapperStackTrace(e));
            resultWrapper.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            resultWrapper.setHasErrors(true);
        }
        return resultWrapper;
    }

    private static final LinkedHashMap<Integer, String> httpStatusCodes = new LinkedHashMap<>();

    static {
        httpStatusCodes.put(100, "CONTINUE");
        httpStatusCodes.put(101, "SWITCHING_PROTOCOLS");
        httpStatusCodes.put(102, "PROCESSING");
        httpStatusCodes.put(103, "EARLY_HINTS");
        httpStatusCodes.put(200, "OK");
        httpStatusCodes.put(201, "CREATED");
        httpStatusCodes.put(202, "ACCEPTED");
        httpStatusCodes.put(203, "NON_AUTHORITATIVE_INFORMATION");
        httpStatusCodes.put(204, "NO_CONTENT");
        httpStatusCodes.put(205, "RESET_CONTENT");
        httpStatusCodes.put(206, "PARTIAL_CONTENT");
        httpStatusCodes.put(207, "MULTI_STATUS");
        httpStatusCodes.put(208, "ALREADY_REPORTED");
        httpStatusCodes.put(226, "IM_USED");
        httpStatusCodes.put(300, "MULTIPLE_CHOICES");
        httpStatusCodes.put(301, "MOVED_PERMANENTLY");
        httpStatusCodes.put(302, "MOVED_TEMPORARILY");
        httpStatusCodes.put(303, "SEE_OTHER");
        httpStatusCodes.put(304, "NOT_MODIFIED");
        httpStatusCodes.put(305, "USE_PROXY");
        httpStatusCodes.put(307, "TEMPORARY_REDIRECT");
        httpStatusCodes.put(308, "PERMANENT_REDIRECT");
        httpStatusCodes.put(400, "BAD_REQUEST");
        httpStatusCodes.put(401, "UNAUTHORIZED");
        httpStatusCodes.put(402, "PAYMENT_REQUIRED");
        httpStatusCodes.put(403, "FORBIDDEN");
        httpStatusCodes.put(404, "NOT_FOUND");
        httpStatusCodes.put(405, "METHOD_NOT_ALLOWED");
        httpStatusCodes.put(406, "NOT_ACCEPTABLE");
        httpStatusCodes.put(407, "PROXY_AUTHENTICATION_REQUIRED");
        httpStatusCodes.put(408, "REQUEST_TIMEOUT");
        httpStatusCodes.put(409, "CONFLICT");
        httpStatusCodes.put(410, "GONE");
        httpStatusCodes.put(411, "LENGTH_REQUIRED");
        httpStatusCodes.put(412, "PRECONDITION_FAILED");
        httpStatusCodes.put(413, "REQUEST_TOO_LONG");
        httpStatusCodes.put(414, "REQUEST_URI_TOO_LONG");
        httpStatusCodes.put(415, "UNSUPPORTED_MEDIA_TYPE");
        httpStatusCodes.put(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
        httpStatusCodes.put(417, "EXPECTATION_FAILED");
        httpStatusCodes.put(421, "MISDIRECTED_REQUEST");
        httpStatusCodes.put(419, "INSUFFICIENT_SPACE_ON_RESOURCE");
        httpStatusCodes.put(420, "METHOD_FAILURE");
        httpStatusCodes.put(422, "UNPROCESSABLE_ENTITY");
        httpStatusCodes.put(423, "LOCKED");
        httpStatusCodes.put(424, "FAILED_DEPENDENCY");
        httpStatusCodes.put(425, "TOO_EARLY");
        httpStatusCodes.put(426, "UPGRADE_REQUIRED");
        httpStatusCodes.put(428, "PRECONDITION_REQUIRED");
        httpStatusCodes.put(429, "TOO_MANY_REQUESTS");
        httpStatusCodes.put(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");
        httpStatusCodes.put(451, "UNAVAILABLE_FOR_LEGAL_REASONS");
        httpStatusCodes.put(500, "INTERNAL_SERVER_ERROR");
        httpStatusCodes.put(501, "NOT_IMPLEMENTED");
        httpStatusCodes.put(502, "BAD_GATEWAY");
        httpStatusCodes.put(503, "SERVICE_UNAVAILABLE");
        httpStatusCodes.put(504, "GATEWAY_TIMEOUT");
        httpStatusCodes.put(505, "HTTP_VERSION_NOT_SUPPORTED");
        httpStatusCodes.put(506, "VARIANT_ALSO_NEGOTIATES");
        httpStatusCodes.put(507, "INSUFFICIENT_STORAGE");
        httpStatusCodes.put(508, "LOOP_DETECTED");
        httpStatusCodes.put(510, "NOT_EXTENDED");
        httpStatusCodes.put(511, "NETWORK_AUTHENTICATION_REQUIRED");

    }

}
