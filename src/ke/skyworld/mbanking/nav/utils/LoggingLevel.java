package ke.skyworld.mbanking.nav.utils;

/**
 * mbanking-server-harambee-USSD-v2 (ke.skyworld.mbanking.nav.utils)
 * Created by: dmutende
 * On: 08 Feb, 2024 16:43
 **/
public enum LoggingLevel {
    DEBUG("DEBUG"),
    INFO("INFO");

    private final String strValue;

    private LoggingLevel(String theValue) {
        this.strValue = theValue;
    }

    public String getValue() {
        return strValue;
    }
}
