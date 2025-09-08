package ke.skyworld.mbanking.ussdapi;

public class USSDAmountLimitParam {
    private String strMinimum;
    private String strMaximum;

    public USSDAmountLimitParam() {
    }

    public String getMinimum() {
        return strMinimum;
    }

    public void setMinimum(String theMinimum) {
        this.strMinimum = theMinimum;
    }

    public String getMaximum() {
        return strMaximum;
    }

    public void setMaximum(String theMaximum) {
        this.strMaximum = theMaximum;
    }
}
