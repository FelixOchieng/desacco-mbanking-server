package ke.skyworld.mbanking.nav.utils;

import ke.skyworld.mbanking.nav.Navision;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

public class HttpSOAPResponseHandler implements HttpClientResponseHandler<String> {

    @Override
    public String handleResponse(ClassicHttpResponse classicHttpResponse) {

        try {
            HttpEntity entity = classicHttpResponse.getEntity();
            if (entity == null) {
                throw new Exception("HttpSOAPResponseHandler.handleResponse(): Error. NULL entity object");
            }

            String responseBody = EntityUtils.toString(entity);
            String strReason = classicHttpResponse.getReasonPhrase();

            System.out.println("Response Status Code   : " + classicHttpResponse.getCode());
            System.out.println("Response Status Reason : " + strReason);

            if (classicHttpResponse.getCode() != HttpStatus.SC_OK) {
                if(Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                    System.err.println("HttpSOAPResponseHandler.handleResponse(): ERROR RESPONSE: "+responseBody);
                }
                throw new Exception("HttpSOAPResponseHandler.handleResponse(): Error. Request Status Code: "+classicHttpResponse.getCode());
            }

            return responseBody;

        } catch (Exception e) {
            System.err.println("HttpSOAP.handleResponse(): Error making HTTP SOAP request: " + e.getMessage());
            return null;
        }
    }
}