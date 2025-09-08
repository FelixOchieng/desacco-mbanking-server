package ke.skyworld.mbanking.ussdapi;

public class APIConstants {
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
        INVALID_SERVICE_NUMBER("INVALID_SERVICE_NUMBER"),
        INVALID_ID_NUMBER("INVALID_ID_NUMBER"),
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
        ACCOUNT_NOT_ACTIVE("ACCOUNT_NOT_ACTIVE"),
        INVALID_MOBILE_NUMBER("INVALID_MOBILE_NUMBER"),
        MOBILE_NUMBER_NOT_WHITELISTED("MOBILE_NUMBER_NOT_WHITELISTED"),
        LOAN_APPLICATION_EXISTS("LOAN_APPLICATION_EXISTS"),
        BLOCKED("BLOCKED"),
        LOAN_DEFAULTER("LOAN_DEFAULTER"),
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
        INVALID_IMSI("INVALID_IMSI"),
        INVALID_IMEI("INVALID_IMEI"),
        BLOCKED("BLOCKED"),
        SUSPENDED("SUSPENDED"),
        NOT_IN_WHITELIST("NOT_IN_WHITELIST"),
        NOT_FOUND("NOT_FOUND"),
        UNDER_MAINTENANCE("UNDER_MAINTENANCE"),
        INVALID("INVALID"),
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
        WITHDRAWABLE("WITHDRAWABLE"),
        TRANSFERRABLE("TRANSFERRABLE"),
        FOSA("FOSA"),
        BOSA("BOSA"),
        INVESTMENT("INVESTMENT"),
        ALL_LOANS("ALL_LOANS"),
        FOSA_LOANS("FOSA_LOANS"),
        BOSA_LOANS("BOSA_LOANS"),
        ALL("ALL"),
        LOANS_SETUP("LOANS_SETUP"),
        LOAN("LOAN");

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
        CASH_WITHDRAWAL_TO_OTHER,
        AGENT_CASH_WITHDRAWAL,
        AIRTIME_PURCHASE,
        PAY_BILL,
        EXTERNAL_FUNDS_TRANSFER,
        INTERNAL_FUNDS_TRANSFER,
        DEPOSIT,
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

    public static enum MobileBankingTransactionType {
        MPESA_WITDHDRAWAL("Mpesa Withdrawal"),
        UTILITY_PAYMENT("Utility Payment"),
        AIRTIME_PURCHASE("Airtime"),
        BANK_TRANSFER("Bank Transfer"),
        ACCOUNT_TRANSFER("Account Transfer"),
        BALANCE_ENQUIRY("Balance Enquiry"),
        MINI_STATEMENT("Mini-Statement");

        private String strValue;

        private MobileBankingTransactionType(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    };

    public static enum DepositOption {
        MY_ACCOUNT("MY_ACCOUNT"),
        OTHER_ACCOUNT("OTHER_ACCOUNT");

        private String strValue;

        private DepositOption(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    };
}
