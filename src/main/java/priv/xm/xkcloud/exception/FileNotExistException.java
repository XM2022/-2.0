package priv.xm.xkcloud.exception;

public class FileNotExistException extends FastException {
    private static final long serialVersionUID = 6912106722324594053L;

    public FileNotExistException() {
        super();
    }

    public FileNotExistException(String message) {
        super(message);
    }
    
}
