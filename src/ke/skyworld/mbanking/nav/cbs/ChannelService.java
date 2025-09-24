package ke.skyworld.mbanking.nav.cbs;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.logging.Log;

public class ChannelService {

    private long lnTransactionId;
    private String strOriginatorId;
    private String strTransactionCategory;
    private int intTransactionStatusCode;
    private String strTransactionStatusName;
    private String strTransactionStatusDescription;
    private String strTransactionStatusDate;
    private String strInitiatorType;
    private String strInitiatorIdentifier;
    private String strInitiatorAccount;
    private String strInitiatorName;
    private String strInitiatorReference;
    private String strInitiatorApplication;
    private String strInitiatorOtherDetails;
    private String strSourceType;
    private String strSourceIdentifier;
    private String strSourceAccount;
    private String strSourceName;
    private String strSourceReference;
    private String strSourceApplication;
    private String strSourceOtherDetails;
    private String strBeneficiaryType;
    private String strBeneficiaryIdentifier;
    private String strBeneficiaryAccount;
    private String strBeneficiaryName;
    private String strBeneficiaryReference;
    private String strBeneficiaryApplication;
    private String strBeneficiaryOtherDetails;
    private String strTransactionCurrency;
    private double dblTransactionAmount;
    private double dblTransactionCharge;
    private String strTransactionRemark;
    private String strTransactionOtherDetails;
    private String strTransactionIntegrityHash;
    private String strDateCreated;

    public long getTransactionId() {
        return lnTransactionId;
    }

    public String getOriginatorId() {
        return strOriginatorId;
    }

    public String getTransactionCategory() {
        return strTransactionCategory;
    }

    public int getTransactionStatusCode() {
        return intTransactionStatusCode;
    }

    public String getTransactionStatusName() {
        return strTransactionStatusName;
    }

    public String getTransactionStatusDescription() {
        return strTransactionStatusDescription;
    }

    public String getTransactionStatusDate() {
        return strTransactionStatusDate;
    }

    public String getInitiatorType() {
        return strInitiatorType;
    }

    public String getInitiatorIdentifier() {
        return strInitiatorIdentifier;
    }

    public String getInitiatorAccount() {
        return strInitiatorAccount;
    }

    public String getInitiatorName() {
        return strInitiatorName;
    }

    public String getInitiatorReference() {
        return strInitiatorReference;
    }

    public String getInitiatorApplication() {
        return strInitiatorApplication;
    }

    public String getInitiatorOtherDetails() {
        return strInitiatorOtherDetails;
    }

    public String getSourceType() {
        return strSourceType;
    }

    public String getSourceIdentifier() {
        return strSourceIdentifier;
    }

    public String getSourceAccount() {
        return strSourceAccount;
    }

    public String getSourceName() {
        return strSourceName;
    }

    public String getSourceReference() {
        return strSourceReference;
    }

    public String getSourceApplication() {
        return strSourceApplication;
    }

    public String getSourceOtherDetails() {
        return strSourceOtherDetails;
    }

    public String getBeneficiaryType() {
        return strBeneficiaryType;
    }

    public String getBeneficiaryIdentifier() {
        return strBeneficiaryIdentifier;
    }

    public String getBeneficiaryAccount() {
        return strBeneficiaryAccount;
    }

    public String getBeneficiaryName() {
        return strBeneficiaryName;
    }

    public String getBeneficiaryReference() {
        return strBeneficiaryReference;
    }

    public String getBeneficiaryApplication() {
        return strBeneficiaryApplication;
    }

    public String getBeneficiaryOtherDetails() {
        return strBeneficiaryOtherDetails;
    }

    public String getTransactionCurrency() {
        return strTransactionCurrency;
    }

    public double getTransactionAmount() {
        return dblTransactionAmount;
    }

    public double getTransactionCharge() {
        return dblTransactionCharge;
    }

    public String getTransactionRemark() {
        return strTransactionRemark;
    }

    public String getTransactionOtherDetails() {
        return strTransactionOtherDetails;
    }

    public String getTransactionIntegrityHash() {
        return strTransactionIntegrityHash;
    }

    public String getDateCreated() {
        return strDateCreated;
    }

    public void setTransactionId(long theTransactionId) {
        this.lnTransactionId = theTransactionId;
    }

    public void setOriginatorId(String theOriginatorId) {
        this.strOriginatorId = theOriginatorId;
    }

    public void setTransactionCategory(String theTransactionCategory) {
        this.strTransactionCategory = theTransactionCategory;
    }

    public void setTransactionStatusCode(int theTransactionStatusCode) {
        this.intTransactionStatusCode = theTransactionStatusCode;
    }

    public void setTransactionStatusName(String theTransactionStatusName) {
        this.strTransactionStatusName = theTransactionStatusName;
    }

    public void setTransactionStatusDescription(String theTransactionStatusDescription) {
        this.strTransactionStatusDescription = theTransactionStatusDescription;
    }

    public void setTransactionStatusDate(String theTransactionStatusDate) {
        this.strTransactionStatusDate = theTransactionStatusDate;
    }

    public void setInitiatorType(String theInitiatorType) {
        this.strInitiatorType = theInitiatorType;
    }

    public void setInitiatorIdentifier(String theInitiatorIdentifier) {
        this.strInitiatorIdentifier = theInitiatorIdentifier;
    }

    public void setInitiatorAccount(String theInitiatorAccount) {
        this.strInitiatorAccount = theInitiatorAccount;
    }

    public void setInitiatorName(String theInitiatorName) {
        this.strInitiatorName = theInitiatorName;
    }

    public void setInitiatorReference(String theInitiatorReference) {
        this.strInitiatorReference = theInitiatorReference;
    }

    public void setInitiatorApplication(String theInitiatorApplication) {
        this.strInitiatorApplication = theInitiatorApplication;
    }

    public void setInitiatorOtherDetails(String theInitiatorOtherDetails) {
        this.strInitiatorOtherDetails = theInitiatorOtherDetails;
    }

    public void setSourceType(String theSourceType) {
        this.strSourceType = theSourceType;
    }

    public void setSourceIdentifier(String theSourceIdentifier) {
        this.strSourceIdentifier = theSourceIdentifier;
    }

    public void setSourceAccount(String theSourceAccount) {
        this.strSourceAccount = theSourceAccount;
    }

    public void setSourceName(String theSourceName) {
        this.strSourceName = theSourceName;
    }

    public void setSourceReference(String theSourceReference) {
        this.strSourceReference = theSourceReference;
    }

    public void setSourceApplication(String theSourceApplication) {
        this.strSourceApplication = theSourceApplication;
    }

    public void setSourceOtherDetails(String theSourceOtherDetails) {
        this.strSourceOtherDetails = theSourceOtherDetails;
    }

    public void setBeneficiaryType(String theBeneficiaryType) {
        this.strBeneficiaryType = theBeneficiaryType;
    }

    public void setBeneficiaryIdentifier(String theBeneficiaryIdentifier) {
        this.strBeneficiaryIdentifier = theBeneficiaryIdentifier;
    }

    public void setBeneficiaryAccount(String theBeneficiaryAccount) {
        this.strBeneficiaryAccount = theBeneficiaryAccount;
    }

    public void setBeneficiaryName(String theBeneficiaryName) {
        this.strBeneficiaryName = theBeneficiaryName;
    }

    public void setBeneficiaryReference(String theBeneficiaryReference) {
        this.strBeneficiaryReference = theBeneficiaryReference;
    }

    public void setBeneficiaryApplication(String theBeneficiaryApplication) {
        this.strBeneficiaryApplication = theBeneficiaryApplication;
    }

    public void setBeneficiaryOtherDetails(String theBeneficiaryOtherDetails) {
        this.strBeneficiaryOtherDetails = theBeneficiaryOtherDetails;
    }

    public void setTransactionCurrency(String theTransactionCurrency) {
        this.strTransactionCurrency = theTransactionCurrency;
    }

    public void setTransactionAmount(double theTransactionAmount) {
        this.dblTransactionAmount = theTransactionAmount;
    }

    public void setTransactionCharge(double theTransactionCharge) {
        this.dblTransactionCharge = theTransactionCharge;
    }

    public void setTransactionRemark(String theTransactionRemark) {
        this.strTransactionRemark = theTransactionRemark;
    }

    public void setTransactionOtherDetails(String theTransactionOtherDetails) {
        this.strTransactionOtherDetails = theTransactionOtherDetails;
    }

    public void setTransactionIntegrityHash(String theTransactionIntegrityHash) {
        this.strTransactionIntegrityHash = theTransactionIntegrityHash;
    }

    public void setDateCreated(String theDateCreated) {
        this.strDateCreated = theDateCreated;
    }

    public static TransactionWrapper<FlexicoreHashMap> insertService(ChannelService theChannelService){
        FlexicoreHashMap insertMap = new FlexicoreHashMap();
   
        insertMap.putValue("originator_id", theChannelService.getOriginatorId());
        insertMap.putValue("transaction_category", theChannelService.getTransactionCategory());
        insertMap.putValue("transaction_status_code", theChannelService.getTransactionStatusCode());
        insertMap.putValue("transaction_status_name", theChannelService.getTransactionStatusName());
        insertMap.putValue("transaction_status_description", theChannelService.getTransactionStatusDescription());
        insertMap.putValue("transaction_status_date", theChannelService.getTransactionStatusDate());
        insertMap.putValue("initiator_type", theChannelService.getInitiatorType());
        insertMap.putValue("initiator_identifier", theChannelService.getInitiatorIdentifier());
        insertMap.putValue("initiator_account", theChannelService.getInitiatorAccount());
        insertMap.putValue("initiator_name", theChannelService.getInitiatorName());
        insertMap.putValue("initiator_reference", theChannelService.getInitiatorReference());
        insertMap.putValue("initiator_application", theChannelService.getInitiatorApplication());
        insertMap.putValue("initiator_other_details", theChannelService.getInitiatorOtherDetails());
        insertMap.putValue("source_type", theChannelService.getSourceType());
        insertMap.putValue("source_identifier", theChannelService.getSourceIdentifier());
        insertMap.putValue("source_account", theChannelService.getSourceAccount());
        insertMap.putValue("source_name", theChannelService.getSourceName());
        insertMap.putValue("source_reference", theChannelService.getSourceReference());
        insertMap.putValue("source_application", theChannelService.getSourceApplication());
        insertMap.putValue("source_other_details", theChannelService.getSourceOtherDetails());
        insertMap.putValue("beneficiary_type", theChannelService.getBeneficiaryType());
        insertMap.putValue("beneficiary_identifier", theChannelService.getBeneficiaryIdentifier());
        insertMap.putValue("beneficiary_account", theChannelService.getBeneficiaryAccount());
        insertMap.putValue("beneficiary_name", theChannelService.getBeneficiaryName());
        insertMap.putValue("beneficiary_reference", theChannelService.getBeneficiaryReference());
        insertMap.putValue("beneficiary_application", theChannelService.getBeneficiaryApplication());
        insertMap.putValue("beneficiary_other_details", theChannelService.getBeneficiaryOtherDetails());
        insertMap.putValue("transaction_currency", theChannelService.getTransactionCurrency());
        insertMap.putValue("transaction_amount", theChannelService.getTransactionAmount());
        insertMap.putValue("transaction_charge", theChannelService.getTransactionCharge());
        insertMap.putValue("transaction_remark", theChannelService.getTransactionRemark());
        insertMap.putValue("transaction_other_details", theChannelService.getTransactionOtherDetails());
        insertMap.putValue("transaction_integrity_hash", theChannelService.getTransactionIntegrityHash());
        insertMap.putValue("date_created", DateTime.getCurrentDateTime());

        TransactionWrapper<FlexicoreHashMap> wrapper = Repository.insertAutoIncremented(StringRefs.SENTINEL,
                "mbanking_logs.channel_services_log", insertMap);

        if(wrapper.hasErrors()){
            Log.error(ChannelService.class, "insertService()", "Failed to Log Service: "+ wrapper.getErrors()+" - "+wrapper.getMessages());
        }
        return wrapper;
    }

}
