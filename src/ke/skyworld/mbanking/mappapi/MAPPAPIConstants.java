package ke.skyworld.mbanking.mappapi;

import ke.co.skyworld.smp.query_manager.util.SystemParameters;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import org.w3c.dom.Document;

public class MAPPAPIConstants {
    public enum MAPP_PARAM_TYPE {
        CASH_WITHDRAWAL,
        CASH_WITHDRAWAL_TO_OTHER,
        AIRTIME_PURCHASE,
        PAY_BILL,
        EXTERNAL_FUNDS_TRANSFER,
        INTERNAL_FUNDS_TRANSFER,
        DEPOSIT,
        APPLY_LOAN,
        PAY_LOAN,
        PESALINK_TRANSFER,
        RTGS,
        EFT,
    }

    public enum OTP_VERIFICATION_STATUS {
        SUCCESS,
        ERROR,
    }

    public enum OTP_CHECK_STAGE {
        GENERATION,
        VERIFICATION,
    }

    public enum OTP_TYPE {
        LOGIN,
        ACTIVATION,
        TRANSACTIONAL,
        TRANSACTIONAL_WITH_AGENT_OTP,
        TRANSACTIONAL_WITH_CUSTOMER_OTP,
    }

    public enum CardValueType {
        TEXT("TEXT"),
        CURRENCY("CURRENCY"),
        INTEGER("INTEGER"),
        DOUBLE("DOUBLE");

        private final String strValue;

        CardValueType(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum MAPPService {
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
        LOAN_LIMIT("LOAN_LIMIT"),
        LOAN_STATEMENT("LOAN_STATEMENT"),
        LOAN_GUARANTORS("LOAN_GUARANTORS"),
        ADD_LOAN_GUARANTORS("ADD_LOAN_GUARANTORS"),
        LOANS_GUARANTEED("LOANS_GUARANTEED"),
        LOAN_GUARANTORSHIP_REQUESTS("LOAN_GUARANTORSHIP_REQUESTS"),
        BUY_GOODS("BUY_GOODS"),
        CONTACT_US("CONTACT_US");

        private final String strValue;

        MAPPService(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public enum AGNT_PARAM_TYPE {
        CASH_WITHDRAWAL,
        AIRTIME_PURCHASE,
        PAY_BILL,
        EXTERNAL_FUNDS_TRANSFER,
        INTERNAL_FUNDS_TRANSFER,
        DEPOSIT,
        APPLY_LOAN,
        PAY_LOAN,
    }


    public enum AccountType {
        FOSA("FOSA"),
        BOSA("BOSA"),
        MOBILE_LOAN("MOBILE_LOAN"),
        WITHDRAWABLE("WITHDRAWABLE"),
        WITHDRAWABLE_IFT("WITHDRAWABLE_IFT"),
        DEPOSIT("DEPOSIT"),
        DEPOSIT_IFT("DEPOSIT_IFT"),
        BALANCE("BALANCE"),
        ALL("ALL");

        private final String strValue;

        AccountType(String theValue) {
            this.strValue = theValue;
        }

        public String getValue() {
            return strValue;
        }
    }

    public static String getAttachmentsFolder(){

        String strSettingsXML = SystemParameters.getParameter(AppConstants.strSettingParamName);
        Document document = XmlUtils.parseXml(strSettingsXML);

        String strMbankingPortalRootDir = XmlUtils.getTagValue(document, "/MBANKING_SETTINGS/MBANKING_PORTAL_ROOT_DIR");

        return strMbankingPortalRootDir+"/files_storage/master/attachments/";
    }
}
