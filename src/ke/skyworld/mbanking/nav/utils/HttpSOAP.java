package ke.skyworld.mbanking.nav.utils;

import ke.skyworld.mbanking.nav.Navision;
import ke.skyworld.mbanking.nav.NavisionAgency;
import ke.skyworld.mbanking.nav.utils.memory.JvmManager;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.time.Duration;
import java.time.Instant;

public class HttpSOAP {

    public static String sendRequest(String SOAPFunction, String theRequestBody) throws Exception {

        try {

            System.out.println();
            System.out.println("----------------------------------------------------");
            System.out.println("Making SOAP Request...");
            System.out.println("URL                    : "+Navision.params.getCoreBankingUrl());
            System.out.println("Username               : "+Navision.params.getCoreBankingUsername());
            System.out.println("Domain                 : "+Navision.params.getCoreBankingDomain());
            System.out.println("SOAP Action            : "+Navision.params.getCoreBankingSOAPActionPrefix()+SOAPFunction);


            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Request Body           :\n------\n" + theRequestBody + "\n------");
            }

            HttpPost httpPost = new HttpPost(Navision.params.getCoreBankingUrl());
            httpPost.setEntity(new StringEntity(theRequestBody));
            httpPost.setHeader("Content-type", "application/xml");
            httpPost.setHeader("SOAPAction", Navision.params.getCoreBankingSOAPActionPrefix()+SOAPFunction);


            /*API CALL TO CBS START*/
            Instant instStart, instEnd;
            instStart = Instant.now();

            CloseableHttpClient closeableHttpClient = Navision.getHttpClient();
            String response = null;

            if(closeableHttpClient != null) {
                response = closeableHttpClient.execute(httpPost, new HttpSOAPResponseHandler());
            } else {
                System.err.println("HttpSOAP.sendRequest() - ERROR: CloseableHttpClient is NULL");
            }

            instEnd = Instant.now();
            Duration durTimeElapsed = Duration.between(instStart, instEnd);

            if (Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Response Body          :\n------\n" + response+ "\n------");
            }

            System.out.println("Response Time (ms)     : " + durTimeElapsed.toMillis());
            System.out.println("----------------------------------------------------");
            System.out.println();

            JvmManager.gc(instEnd, instStart, durTimeElapsed);

            return response;

        } catch (Exception e) {
            String error = "HttpSOAP.sendRequest(): Error making HTTP SOAP request: " + e.getMessage();
            System.err.println(error);
            if(Navision.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) e.printStackTrace();
            throw new Exception(error);
        }
    }

    public static String sendAgencyRequest(String SOAPFunction, String theRequestBody) throws Exception {

        try {

            System.out.println();
            System.out.println("----------------------------------------------------");
            System.out.println("Making SOAP Request...");
            System.out.println("URL                    : "+NavisionAgency.params.getCoreBankingUrl());
            System.out.println("Username               : "+NavisionAgency.params.getCoreBankingUsername());
            System.out.println("Domain                 : "+NavisionAgency.params.getCoreBankingDomain());
            System.out.println("SOAP Action            : "+NavisionAgency.params.getCoreBankingSOAPActionPrefix()+SOAPFunction);


            if (NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Request Body           :\n------\n" + theRequestBody + "\n------\n");
            }

            HttpPost httpPost = new HttpPost(NavisionAgency.params.getCoreBankingUrl());
            httpPost.setEntity(new StringEntity(theRequestBody));
            httpPost.setHeader("Content-type", "application/xml");
            httpPost.setHeader("SOAPAction", NavisionAgency.params.getCoreBankingSOAPActionPrefix()+SOAPFunction);


            /*API CALL TO CBS START*/
            Instant instStart, instEnd;
            instStart = Instant.now();

            CloseableHttpClient closeableHttpClient = NavisionAgency.getHttpClient();
            String response = null;

            if(closeableHttpClient != null) {
                response = closeableHttpClient.execute(httpPost, new HttpSOAPResponseHandler());
            } else {
                System.err.println("HttpSOAP.sendAgencyRequest() - ERROR: CloseableHttpClient is NULL");
            }

            instEnd = Instant.now();
            Duration durTimeElapsed = Duration.between(instStart, instEnd);

            if (NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) {
                System.out.println("Response Body          :\n------\n" + response+ "\n------\n");
            }

            System.out.println("Response Time (ms)     : " + durTimeElapsed.toMillis());
            System.out.println("----------------------------------------------------");
            System.out.println();

            JvmManager.gc(instEnd, instStart, durTimeElapsed);

            return response;

        } catch (Exception e) {
            String error = "HttpSOAP.sendAgencyRequest(): Error making HTTP SOAP request: " + e.getMessage();
            System.err.println(error);
            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) e.printStackTrace();
            throw new Exception(error);
        }
    }
}
