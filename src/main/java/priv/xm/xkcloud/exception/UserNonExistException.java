package priv.xm.xkcloud.exception;

public class UserNonExistException extends FastException {
    private static final long serialVersionUID = 6443801199025220309L;

    public UserNonExistException() {
        super();
    }

    public UserNonExistException(String message) {
        super(message);
    }
    
}
