package priv.xm.xkcloud.exception;

public class FfmpegTryLockFailException extends FastException{
    private static final long serialVersionUID = 5803442355368727384L;

    public FfmpegTryLockFailException() {
        super();
    }

    public FfmpegTryLockFailException(String message) {
        super(message);
    }

}
