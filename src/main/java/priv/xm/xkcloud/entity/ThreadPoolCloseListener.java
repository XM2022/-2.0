package priv.xm.xkcloud.entity;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
public class ThreadPoolCloseListener implements ApplicationListener<ContextClosedEvent> {
    @Value("${recycleThreadPool.shutdownWaitTimeSecond}")
    private int databaseThreadPoolShutdownWaitTime;
    @Autowired
    @Qualifier("databaseThreadPool")
    private ThreadPoolExecutor databaseThreadPool;
    
    @Value("${videoSliceThreadPool.shutdownWaitTimeSecond}")
    private int videoSliceThreadPoolShutdownWaitTime;
    @Autowired
    @Qualifier("videoSliceThreadPool")
    private ThreadPoolExecutor videoSliceThreadPool;
    
    @Value("${recycleThreadPool.shutdownWaitTimeSecond}")
    private int recycleThreadPoolShutdownWaitTime;
    @Autowired
    @Qualifier("recycleThreadPool")
    private ThreadPoolExecutor recycleThreadPool;
    
    @Value("${scheduledRecycleThreadPool.shutdownWaitTimeSecond}")
    private int scheduleRecycleThreadPoolShutdownWaitTime;
    @Autowired
    @Qualifier("scheduledRecycleThreadPool")
    private ScheduledExecutorService scheduledRecycleThreadPool;

    @Override
    public void onApplicationEvent(ContextClosedEvent arg0) {
        databaseThreadPool.shutdown();
        try {
            boolean closeResult1 = databaseThreadPool.awaitTermination(databaseThreadPoolShutdownWaitTime, TimeUnit.SECONDS);
            boolean closeResult2 = videoSliceThreadPool.awaitTermination(videoSliceThreadPoolShutdownWaitTime, TimeUnit.SECONDS);
            boolean closeResult3 = recycleThreadPool.awaitTermination(recycleThreadPoolShutdownWaitTime, TimeUnit.SECONDS);
            boolean closeResult4 = scheduledRecycleThreadPool.awaitTermination(scheduleRecycleThreadPoolShutdownWaitTime, TimeUnit.SECONDS);
            if(closeResult1 && closeResult2 && closeResult3 && closeResult4) System.out.println("线程池已正常关闭.");
            else System.out.println("线程池被强制关闭!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
