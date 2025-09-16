package ke.skyworld.mbanking.pesaapi;

public class PESAAPIConstants {
    public static String MSG_XML_PARAMS_PATH  = "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/PRODUCT_ID";

    public enum STATUS {
        SUCCESS("SUCCESS"),
        TRANSACTION_EXISTS("TRANSACTION_EXISTS"),
        ERROR("ERROR"),
        FAILED("FAILED");

        private final String strValue;

        STATUS(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum PESA_PARAM_TYPE {
        MPESA_B2C,
        MPESA_C2B,
        MPESA_B2B,
        FAMILY_BANK_PESALINK,
        AIRTIME,
    }
}
