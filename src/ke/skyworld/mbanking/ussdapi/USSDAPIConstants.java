package ke.skyworld.mbanking.ussdapi;

public class USSDAPIConstants {
    public enum MobileUserStatus {
        ACTIVE("ACTIVE"),
        LOCKED("LOCKED"),
        INACTIVE("INACTIVE"),
        SUSPENDED("SUSPENDED");

        private final String strValue;

        MobileUserStatus(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum Condition {
        YES("YES"),
        NO("NO");

        private final String strValue;

        Condition(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    //These USSDMiddlewareConstants NOT Used .. Just Samples ...
    public enum MSGTemplate {
        /*
        Withdrawal("Withdrawal"),

        Account_Balance("Account_Balance"),
        Mini_Statement("Mini_Statement"),

        Loan_Status("Loan_Status"),
        Loan_Application_Status("Loan_Application_Status"),
        Loan_Balance("Loan_Balance"),
        Loan_Mini_Statement("Loan_Mini_Statement"),

        Forgot_PIN("Forgot_PIN"),
        Change_PIN("Change_PIN"),g
        */
        Trailer_Message("Trailer_Message2");

        private final String strValue;

        MSGTemplate(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum FunctionStatusReturnVal {
        TRUE("TRUE"),
        FALSE("FALSE"),
        ERROR("ERROR");

        private final String strValue;

        FunctionStatusReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum LoginReturnVal {
        SUCCESS("SUCCESS"),
        INCORRECT_PIN("INCORRECT_PIN"),
        EXPIRED_PIN("EXPIRED_PIN"),
        INVALID_IMSI("INVALID_IMSI"),
        SET_PIN("SET_PIN"),
        BLOCKED("BLOCKED"),
        SUSPENDED("SUSPENDED"),
        ERROR("ERROR");

        private final String strValue;

        LoginReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum AccountRegistrationReturnVal {
        SUCCESS("SUCCESS"),
        PIN_MISMATCH("PIN_MISMATCH"),
        MEMBER_EXISTS("MEMBER_EXISTS"),
        ENTRY_EXISTS("ENTRY_EXISTS"),
        INVALID_PIN("INVALID_PIN"),
        INVALID_FIRSTNAME("INVALID_FIRSTNAME"),
        INVALID_LASTNAME("INVALID_LASTNAME"),
        INVALID_IDNO("INVALID_IDNO"),
        INVALID_DOB("INVALID_DOB"),
        ERROR("ERROR");

        private final String strValue;

        AccountRegistrationReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }


    public enum SetPINReturnVal {
        SUCCESS("SUCCESS"),
        PIN_MISMATCH("PIN_MISMATCH"),
        INVALID_PIN("INVALID_PIN"),
        INCORRECT_PIN("INCORRECT_PIN"),
        INVALID_NEW_PIN("INVALID_NEW_PIN"),
        INVALID_ACCOUNT("INVALID_ACCOUNT"),
        BLOCKED("BLOCKED"),
        ERROR("ERROR");

        private final String strValue;

        SetPINReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum ChangePINReturnVal {
        SUCCESS("SUCCESS"),
        PIN_MISMATCH("PIN_MISMATCH"),
        INVALID_PIN("INVALID_PIN"),
        INCORRECT_PIN("INCORRECT_PIN"),
        INVALID_NEW_PIN("INVALID_NEW_PIN"),
        ERROR("ERROR");

        private final String strValue;

        ChangePINReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum TransactionReturnVal {
        SUCCESS("SUCCESS"),
        INCORRECT_PIN("INCORRECT_PIN"),
        INVALID_ACCOUNT("INVALID_ACCOUNT"),
        INSUFFICIENT_BAL("INSUFFICIENT_BAL"),
        WITHDRAWAL_LIMIT_VIOLATION("WITHDRAWAL_LIMIT_VIOLATION"),
        ACCOUNT_NOT_ACTIVE("ACCOUNT_NOT_ACTIVE"),
        INVALID_MOBILE_NUMBER("INVALID_MOBILE_NUMBER"),
        LOAN_APPLICATION_EXISTS("LOAN_APPLICATION_EXISTS"),
        BLOCKED("BLOCKED"),
        ERROR("ERROR");

        private final String strValue;

        TransactionReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum CheckUserReturnVal {
        ACTIVE("ACTIVE"),
        NEW("NEW"),
        DORMANT("DORMANT"),
        CLOSED("CLOSED"),
        DEFAULTED("DEFAULTED"),
        INACTIVE("INACTIVE"),
        INTEGRITY_VIOLATION_CUSTOMER("INTEGRITY_VIOLATION_CUSTOMER"),
        INTEGRITY_VIOLATION_SIGNATORY("INTEGRITY_VIOLATION_SIGNATORY"),
        INTEGRITY_VIOLATION_MOBILE("INTEGRITY_VIOLATION_MOBILE"),
        INVALID_IMSI("INVALID_IMSI"),
        INVALID_IMEI("INVALID_IMEI"),
        BLOCKED("BLOCKED"),
        SUSPENDED("SUSPENDED"),
        LOCKED("LOCKED"),
        NOT_IN_WHITELIST("NOT_IN_WHITELIST"),
        NOT_FOUND("NOT_FOUND"),
        ERROR("ERROR");

        private final String strValue;

        CheckUserReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum AccountType {
        FOSA("FOSA"),
        BOSA("BOSA"),
        LOAN("LOAN"),
        BUSINESS("BUSINESS"),
        WITHDRAWABLE("WITHDRAWABLE"),
        WITHDRAWABLE_IFT("WITHDRAWABLE_IFT"),
        WITHDRAWABLE_STO("WITHDRAWABLE_STO"),
        ALL("ALL");

        private final String strValue;

        AccountType(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum AccountCategory {
        PERSONAL("PERSONAL"),
        GROUP("GROUP");

        private final String strValue;

        AccountCategory(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum MNO {
        SAFARICOM("SAFARICOM"),
        AIRTEL("AIRTEL"),
        TELCOM("TELCOM"),
        ORANGE("ORANGE");

        private final String strValue;

        MNO(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum MobileMoney {
        MPESA("MPESA"),
        AIRTEL_MONEY("AIRTEL_MONEY");

        private final String strValue;

        MobileMoney(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum USSD_PARAM_TYPE {
        CASH_WITHDRAWAL,
        AIRTIME_PURCHASE,
        PAY_BILL,
        EXTERNAL_FUNDS_TRANSFER,
        INTERNAL_FUNDS_TRANSFER,
        DEPOSIT,
        APPLY_LOAN,
        PAY_LOAN,
    }

    public static enum RegisterViewResponse {
        VALID("VALID"),
        INVALID("INVALID"),
        EXPIRED("EXPIRED"),
        INACTIVE("INACTIVE"),
        NOT_FOUND("NOT_FOUND"),
        ERROR("ERROR"),
        FAILED("FAILED");

        private String strValue;

        private RegisterViewResponse(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    };

    public static enum MobileChannel {
        USSD("USSD"),
        MOBILE_APP("MOBILE_APP");

        private String strValue;

        private MobileChannel(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    };


    public enum StandardReturnVal {
        ACTIVE("ACTIVE"),
        NEW("NEW"),
        DORMANT("DORMANT"),
        CLOSED("CLOSED"),
        DEFAULTED("DEFAULTED"),
        INACTIVE("INACTIVE"),
        INTEGRITY_VIOLATION_CUSTOMER("INTEGRITY_VIOLATION_CUSTOMER"),
        INTEGRITY_VIOLATION_SIGNATORY("INTEGRITY_VIOLATION_SIGNATORY"),
        INTEGRITY_VIOLATION_MOBILE("INTEGRITY_VIOLATION_MOBILE"),
        INVALID_IMSI("INVALID_IMSI"),
        INVALID_APP_ID("INVALID_APP_ID"),
        BLOCKED("BLOCKED"),
        SUSPENDED("SUSPENDED"),
        LOCKED("LOCKED"),
        NOT_IN_WHITELIST("NOT_IN_WHITELIST"),
        NOT_FOUND("NOT_FOUND"),
        ERROR("ERROR"),
        BUFFER_SAVE_SUCCESS("BUFFER_SAVE_SUCCESS"),
        SUCCESS("SUCCESS"),
        FAILED("FAILED"),
        DUPLICATE("DUPLICATE"),
        BUFFER_DUPLICATE("BUFFER_DUPLICATE"),

        INCORRECT_PIN("INCORRECT_PIN"),
        INCORRECT_OTP("INCORRECT_OTP"),
        INVALID_ACCOUNT("INVALID_ACCOUNT"),
        INSUFFICIENT_BAL("INSUFFICIENT_BAL"),
        WITHDRAWAL_LIMIT_VIOLATION("WITHDRAWAL_LIMIT_VIOLATION"),
        ACCOUNT_NOT_ACTIVE("ACCOUNT_NOT_ACTIVE"),
        INVALID_MOBILE_NUMBER("INVALID_MOBILE_NUMBER"),
        LOAN_APPLICATION_EXISTS("LOAN_APPLICATION_EXISTS"),
        PIN_MISMATCH("PIN_MISMATCH"),
        INVALID_PIN("INVALID_PIN"),
        INVALID_NEW_PIN("INVALID_NEW_PIN"),
        EXPIRED_PIN("EXPIRED_PIN"),
        SET_PIN("SET_PIN");

        private final String strValue;

        StandardReturnVal(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

}
