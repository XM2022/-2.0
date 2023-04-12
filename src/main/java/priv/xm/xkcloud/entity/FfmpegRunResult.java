package priv.xm.xkcloud.entity;

public class FfmpegRunResult {
    private int stateCode;
    private StringBuilder runInfo;
    
    public FfmpegRunResult(int stateCode, StringBuilder runInfo) {
        super();
        this.stateCode = stateCode;
        this.runInfo = runInfo;
    }

    public int getStateCode() {
        return stateCode;
    }

    public void setStateCode(int stateCode) {
        this.stateCode = stateCode;
    }

    public StringBuilder getRunInfo() {
        return runInfo;
    }

    public void setRunInfo(StringBuilder runInfo) {
        this.runInfo = runInfo;
    }


    public enum StateCode {
        SUCCESS, TIME_OUT, FAIL;
        public static StateCode map(int code) {
            switch (code) {
            case 1:
                return SUCCESS;
            case 0:
                return TIME_OUT;
            case -1:
                return FAIL;
            default:
                return FAIL;
            }
        }
    }
    
}
