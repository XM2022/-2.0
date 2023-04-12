package priv.xm.xkcloud.exception;

/**
 * 此异常类来实现控制流跳转.
 * 关闭异常的调用栈信息生成提升程序的运行效率.
 */
public class FastException extends RuntimeException {
    private static final long serialVersionUID = 6443801199025220309L;

    public FastException() {
        super(null, null, false, false);
    }

    public FastException(String message) {
        super(message, null, false, false);
    }
    
    

}
