package ke.skyworld.mbanking.mappapi;

public class MAPPAmountLimitParam {
    private String strMinimum;
    private String strMaximum;

    public MAPPAmountLimitParam() {
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
