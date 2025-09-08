package ke.skyworld.mbanking.pesaapi;

public class APIConstants {
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

    public static enum APPLICATION_TYPE {
        PESA,
        MSG,
        MAPP,
        USSD,
    }

    public enum TransactionType {
        MPESA_CASH_WITHDRAWAL("MPESA_CASH_WITHDRAWAL"),
        ATM_CASH_WITHDRAWAL("ATM_CASH_WITHDRAWAL"),
        AGENT_CASH_WITHDRAWAL("AGENT_CASH_WITHDRAWAL");

        private final String strValue;

        TransactionType(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }
}
