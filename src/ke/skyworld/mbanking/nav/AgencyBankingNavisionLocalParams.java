package ke.skyworld.mbanking.nav;

import ke.skyworld.mbanking.nav.utils.LoggingLevel;

public class AgencyBankingNavisionLocalParams {

    private LoggingLevel coreBankingLoggingLevel;
    private String coreBankingType;
    private String coreBankingUrl;
    private String coreBankingUsername;
    private String coreBankingPassword;
    private String coreBankingDomain;

    private String coreBankingWorkstation;
    private String coreBankingSOAPActionPrefix;


    public LoggingLevel getCoreBankingLoggingLevel() {
        return coreBankingLoggingLevel;
    }

    public String getCoreBankingType() {
        return coreBankingType;
    }

    public String getCoreBankingUrl() {
        return coreBankingUrl;
    }

    public String getCoreBankingUsername() {
        return coreBankingUsername;
    }

    public String getCoreBankingPassword() {
        return coreBankingPassword;
    }

    public String getCoreBankingDomain() {
        return coreBankingDomain;
    }

    public String getCoreBankingWorkstation() {
        return coreBankingWorkstation;
    }

    public String getCoreBankingSOAPActionPrefix() {
        return coreBankingSOAPActionPrefix;
    }


    public void setCoreBankingLoggingLevel(LoggingLevel  coreBankingLoggingLevel) {
        this.coreBankingLoggingLevel = coreBankingLoggingLevel;
    }

    public void setCoreBankingType(String coreBankingType) {
        this.coreBankingType = coreBankingType;
    }

    public void setCoreBankingUrl(String coreBankingUrl) {
        this.coreBankingUrl = coreBankingUrl;
    }

    public void setCoreBankingUsername(String coreBankingUsername) {
        this.coreBankingUsername = coreBankingUsername;
    }

    public void setCoreBankingPassword(String coreBankingPassword) {
        this.coreBankingPassword = coreBankingPassword;
    }

    public void setCoreBankingDomain(String coreBankingDomain) {
        this.coreBankingDomain = coreBankingDomain;
    }

    public void setCoreBankingWorkstation(String coreBankingWorkstation) {
        this.coreBankingWorkstation = coreBankingWorkstation;
    }

    public void setCoreBankingSOAPActionPrefix(String coreBankingSOAPActionPrefix) {
        this.coreBankingSOAPActionPrefix = coreBankingSOAPActionPrefix;
    }

    @Override
    public String toString() {
        return "NavisionLocalParams{" +
                "coreBankingType='" + coreBankingType + '\'' +
                ", coreBankingUrl='" + coreBankingUrl + '\'' +
                ", coreBankingUsername='" + coreBankingUsername + '\'' +
                ", coreBankingPassword='" + coreBankingPassword + '\'' +
                ", coreBankingNamespaceUrl='" + coreBankingDomain + '\'' +
                ", coreBankingLocalPort='" + coreBankingSOAPActionPrefix + '\'' +
                '}';
    }
}
