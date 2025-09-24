package ke.skyworld.mbanking.agencyapi.models;

import ke.skyworld.lib.mbanking.mapp.MAPPConstants;

public class AgencyAPIResponse {
    private MAPPConstants.ResponseAction raResponseAction;
    private MAPPConstants.ResponseStatus enResponseStatus;
    private String strTitle;
    private String strResponseText;
    private String strCharge;
    private String strResponseXML;
    private String strMobileNumber;

    public AgencyAPIResponse() {}

    public AgencyAPIResponse(MAPPConstants.ResponseAction raResponseAction, MAPPConstants.ResponseStatus enResponseStatus, String strTitle, String strResponseText, String strCharge, String strResponseXML, String strMobileNumber) {
        this.raResponseAction = raResponseAction;
        this.enResponseStatus = enResponseStatus;
        this.strTitle = strTitle;
        this.strResponseText = strResponseText;
        this.strCharge = strCharge;
        this.strResponseXML = strResponseXML;
        this.strMobileNumber = strMobileNumber;
    }

    public MAPPConstants.ResponseAction getResponseAction() {
        return raResponseAction;
    }

    public void setResponseAction(MAPPConstants.ResponseAction theResponseAction) {
        this.raResponseAction = theResponseAction;
    }

    public MAPPConstants.ResponseStatus getResponseStatus() {
        return enResponseStatus;
    }

    public void setResponseStatus(MAPPConstants.ResponseStatus enResponseStatus) {
        this.enResponseStatus = enResponseStatus;
    }

    public String getTitle() {
        return strTitle;
    }

    public void setTitle(String strTitle) {
        this.strTitle = strTitle;
    }

    public String getResponseText() {
        return strResponseText;
    }

    public void setResponseText(String strResponseText) {
        this.strResponseText = strResponseText;
    }

    public String getCharge() {
        return strCharge;
    }

    public void setCharge(String strCharge) {
        this.strCharge = strCharge;
    }

    public String getResponseXML() {
        return strResponseXML;
    }

    public void setResponseXML(String strResponseXML) {
        this.strResponseXML = strResponseXML;
    }

    public String getMobileNumber() {
        return strMobileNumber;
    }

    public void setMobileNumber(String strMobileNumber) {
        this.strMobileNumber = strMobileNumber;
    }
}
