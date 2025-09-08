package ke.skyworld.mbanking.nav;

import ke.skyworld.mbanking.nav.utils.LoggingLevel;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.NTCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.net.URL;

public class NavisionAgency {

    public static NavisionLocalParams params;

    public static BasicCredentialsProvider getCredentialsProvider(){
        try {
            String strURL = NavisionAgency.params.getCoreBankingUrl();

            URL url = new URL(strURL);

            String strHost = extractHostFromURL(url);
            int strPort = extractPortFromURL(url);

            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            credentialsProvider.setCredentials(
                    new AuthScope(new AuthScope(strHost, strPort)),
                    new NTCredentials(
                            NavisionAgency.params.getCoreBankingUsername(),
                            NavisionAgency.params.getCoreBankingPassword().toCharArray(),
                            NavisionAgency.params.getCoreBankingWorkstation(),
                            NavisionAgency.params.getCoreBankingDomain()
                    ));

            return credentialsProvider;
        } catch (Exception e){
            System.err.println("NavisionAgency.getCredentialsProvider(): Error getting Credentials Provider Object: " + e.getMessage());
            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) e.printStackTrace();
        }

        return null;
    }

    public static CloseableHttpClient getHttpClient(){

        try {
            return HttpClients.custom().setDefaultCredentialsProvider(getCredentialsProvider()).build();
        } catch (Exception e) {
            System.err.println("NavisionAgency.getHttpClient(): Error getting Http Client Object: " + e.getMessage());
            if(NavisionAgency.params.getCoreBankingLoggingLevel() == LoggingLevel.DEBUG) e.printStackTrace();
        }

        return null;
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
}