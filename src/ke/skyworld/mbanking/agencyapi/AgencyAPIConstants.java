package ke.skyworld.mbanking.agencyapi;

public class AgencyAPIConstants {
    public enum AgencyAPIService {
        CASH_WITHDRAWAL("CASH_WITHDRAWAL"),
        BUY_AIRTIME("BUY_AIRTIME"),
        DEPOSIT_MONEY("DEPOSIT_MONEY"),
        ACCOUNT_BALANCE("ACCOUNT_BALANCE"),
        BANK_TRANSFER("BANK_TRANSFER"),
        APPLY_LOAN("APPLY_LOAN"),
        PAY_BILL("PAY_BILL"),
        QR_TRANSACTION("QR_TRANSACTION"),
        INTERNAL_FUNDS_TRANSFER("INTERNAL_FUNDS_TRANSFER"),
        ACCOUNT_STATEMENT("ACCOUNT_STATEMENT"),
        CHANGE_PASSWORD("CHANGE_PASSWORD"),
        PAY_LOAN("PAY_LOAN"),
        LOAN_BALANCE("LOAN_BALANCE"),
        CONTACT_US("CONTACT_US");

        private final String strValue;

        AgencyAPIService(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum AgencyAPIResponseStatus {
        SUCCESS("SUCCESS"),
        FAILED("FAILED"),
        ERROR("ERROR");

        private final String strValue;

        AgencyAPIResponseStatus(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }
    public enum AgencyAPIVersion {
        PRODUCTION("PRODUCTION"),
        DEVELOPMENT("DEVELOPMENT");

        private final String strValue;

        AgencyAPIVersion(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public static AgencyAPIVersion AGENCY_API_VERSION = AgencyAPIVersion.DEVELOPMENT;
}
