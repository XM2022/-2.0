package priv.xm.xkcloud.util;

public class RunnableWrapper {

    /**使线程能正常打印出堆栈信息*/
    @Deprecated
    public static Runnable wrap(Runnable r) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                    System.out.println(Thread.currentThread().toString() + "线程任务完成.");
                }
                catch (Exception re) {
                    System.err.println("子线程异常堆栈:");
                    new Exception("调用者--" + Thread.currentThread().toString());
                    System.err.print("异常抛出处--");
                    throw re;
                }
            }
        };
    }
    
}
