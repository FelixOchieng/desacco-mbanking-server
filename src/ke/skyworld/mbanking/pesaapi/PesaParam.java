package ke.skyworld.mbanking.pesaapi;

public class PesaParam {
    private String strProductId;
    private String strSenderIdentifier;
    private String strSenderAccount;
    private String strSenderName;

    public PesaParam() {
    }

    public String getProductId() {
        return strProductId;
    }

    public void setProductId(String theProductId) {
        this.strProductId = theProductId;
    }

    public String getSenderIdentifier() {
        return strSenderIdentifier;
    }

    public void setSenderIdentifier(String theSenderIdentifier) {
        this.strSenderIdentifier = theSenderIdentifier;
    }

    public String getSenderAccount() {
        return strSenderAccount;
    }

    public void setSenderAccount(String theSenderAccount) {
        this.strSenderAccount = theSenderAccount;
    }

    public String getSenderName() {
        return strSenderName;
    }

    public void setSenderName(String theSenderName) {
        this.strSenderName = theSenderName;
    }
}
