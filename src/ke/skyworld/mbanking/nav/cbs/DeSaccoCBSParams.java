package ke.skyworld.mbanking.nav.cbs;

import ke.co.skyworld.smp.authentication_manager.MobileBankingCryptography;
import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_manager.util.SystemParameters;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.co.skyworld.smp.utility_items.security.Encryption;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_SYSTEM_PARAMETERS;


/**
 * <PARAMETERS>
 * <URL></URL>
 * <CHANNEL_ID></CHANNEL_ID>
 * <AUTH_PARAMETERS>
 * <SSL_CERTIFICATES ENABLED="YES/NO"/>
 * <PASSWORD TYPE="ENCRYPTED/CLEARTEXT"></PASSWORD>
 * </AUTH_PARAMETERS>
 * </PARAMETERS>
 */
public class DeSaccoCBSParams {

    private static String theSOAPURL;
    private static String theSOAPAction;
    private static boolean doSSLValidation;
    private static String theDomain;
    private static String theUser;
    private static String thePassword;

    /*These two are used in case we want to log the request / response*/
    private static boolean theLogRequest;
    private static boolean theLogResponse;

    private DeSaccoCBSParams() {
    }

    public static void initialize() throws Exception {
        String DYNAMICS_CBS_PARAMETERS = "CBS_PARAMETERS";

        FlexicoreHashMap parametersMap = SystemParameters.getParameterMap(DYNAMICS_CBS_PARAMETERS);

        if (parametersMap == null
            || parametersMap.isEmpty()
            || parametersMap.getStringValue("parameter_value") == null
            || parametersMap.getStringValue("parameter_value").trim().isEmpty()) {
            throw new IllegalStateException("Missing System Parameter '" + DYNAMICS_CBS_PARAMETERS + "'");
        }

        /*if(MobileBankingCryptography.isRecordIntegrityViolated(parametersMap)){
            throw new IllegalStateException("Corrupt Details found for System Parameter: "+ DYNAMICS_CBS_PARAMETERS);
        }*/

        String strCBSParameters = parametersMap.getStringValue("parameter_value");

        Document document = XmlUtils.parseXml(strCBSParameters);
        if (document == null) {
            throw new RuntimeException("Unable to convert Parameter Value for " + DYNAMICS_CBS_PARAMETERS + " to XML document.");
        }

        Element elSOAPURL = XmlUtils.getElementNodeFromXpath(document, "/PARAMETERS/URL");
        Element elSOAPAction = XmlUtils.getElementNodeFromXpath(document, "/PARAMETERS/SOAP_ACTION");
        Element elSSLCertificates = XmlUtils.getElementNodeFromXpath(document, "/PARAMETERS/AUTH_PARAMETERS/SSL_CERTIFICATES");
        Element elDomain = XmlUtils.getElementNodeFromXpath(document, "/PARAMETERS/AUTH_PARAMETERS/DOMAIN");
        Element elUser = XmlUtils.getElementNodeFromXpath(document, "/PARAMETERS/AUTH_PARAMETERS/USER");
        Element elPassword = XmlUtils.getElementNodeFromXpath(document, "/PARAMETERS/AUTH_PARAMETERS/PASSWORD");

        theSOAPURL = elSOAPURL.getTextContent();
        theSOAPAction = elSOAPAction.getTextContent();
        theDomain = elDomain.getTextContent();
        theUser = elUser.getTextContent();
        thePassword = elPassword.getTextContent();
        doSSLValidation = elSSLCertificates.getAttribute("ENABLED").equalsIgnoreCase("YES");

        String strPasswordType = elPassword.getAttribute("TYPE");

        if (strPasswordType.equalsIgnoreCase("ENCRYPTED")) {
            thePassword = Encryption.decrypt(elPassword.getTextContent());
        } else {
            thePassword = elPassword.getTextContent();

            FlexicoreHashMap tempUpdateMap = new FlexicoreHashMap();
            tempUpdateMap.putValue("/PARAMETERS/AUTH_PARAMETERS/PASSWORD/@TYPE", "ENCRYPTED");
            tempUpdateMap.putValue("/PARAMETERS/AUTH_PARAMETERS/PASSWORD", Encryption.encrypt(thePassword));
            String strUpdatedParamValue = XmlUtils.updateXMLTags(document, tempUpdateMap);

            FlexicoreHashMap updateMap = new FlexicoreHashMap();
            updateMap.putValue("parameter_value", strUpdatedParamValue);
            updateMap.putValue("date_modified", DateTime.getCurrentDateTime());

            parametersMap.copyFrom(updateMap);
            String integrityHash = MobileBankingCryptography.calculateIntegrityHash(parametersMap);

            updateMap.putValue("integrity_hash", integrityHash);

            Repository.update(StringRefs.SENTINEL, TBL_SYSTEM_PARAMETERS,
                    updateMap,
                    new FilterPredicate("parameter_id = :parameter_id"),
                    new FlexicoreHashMap().addQueryArgument(":parameter_id", parametersMap.getStringValue("parameter_id"))
            );
        }

        try (InputStream isPropertiesFile = new FileInputStream("props.properties")) {
            Properties ptProperties = new Properties();

            ptProperties.load(isPropertiesFile);

            String strLogRequest = ptProperties.getProperty("cbs.request.logs.display");
            String strLogResponse = ptProperties.getProperty("cbs.response.logs.display");

            theLogRequest = strLogRequest.equalsIgnoreCase("true");
            theLogResponse = strLogResponse.equalsIgnoreCase("true");

        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public static String getSOAPURL() {
        return theSOAPURL;
    }

    public static String getSOAPAction() {
        return theSOAPAction;
    }

    public static boolean getSSLValidation() {
        return doSSLValidation;
    }

    public static String getDomain() {
        return theDomain;
    }

    public static String getUser() {
        return theUser;
    }

    public static String getPassword() {
        return thePassword;
    }

    public static boolean isLogRequestEnabled() {

        try (InputStream isPropertiesFile = new FileInputStream("props.properties")) {
            Properties ptProperties = new Properties();

            ptProperties.load(isPropertiesFile);

            String strLogRequest = ptProperties.getProperty("cbs.request.logs.display");
            String strLogResponse = ptProperties.getProperty("cbs.response.logs.display");

            theLogRequest = strLogRequest.equalsIgnoreCase("true");
            theLogResponse = strLogResponse.equalsIgnoreCase("true");

        } catch (IOException io) {
            io.printStackTrace();
        }

        return theLogRequest;
    }

    public static boolean isLogResponseEnabled() {

        try (InputStream isPropertiesFile = new FileInputStream("props.properties")) {
            Properties ptProperties = new Properties();

            ptProperties.load(isPropertiesFile);

            String strLogRequest = ptProperties.getProperty("cbs.request.logs.display");
            String strLogResponse = ptProperties.getProperty("cbs.response.logs.display");

            theLogRequest = strLogRequest.equalsIgnoreCase("true");
            theLogResponse = strLogResponse.equalsIgnoreCase("true");

        } catch (IOException io) {
            io.printStackTrace();
        }

        return theLogResponse;
    }
}
