package priv.xm.xkcloud.entity;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.ThreadPoolExecutor.DiscardOldestPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import priv.xm.xkcloud.exception.RejectExecuteFastException;

@Configuration
public class ThreadPoolConfiguration {
    @Value("${videoSliceThreadPool.name}")
    private String videoSliceThreadPoolName;
    
    @Value("${videoSliceThreadPool.corePoolSize}")
    private int videoSliceThreadPoolCorePoolSize;
    
    @Value("${videoSliceThreadPool.maximumPoolSize}")
    private int videoSliceThreadPoolMaximumPoolSize;
    
    @Value("${videoSliceThreadPool.keepAliveTimeSecond}")
    private int videoSliceThreadPoolKeepAliveTime;
    
    @Value("${videoSliceThreadPool.workQueueLength}")
    private int videoSliceThreadPoolWorkQueueLength;
    
    @Bean
    @Scope("singleton")
    ThreadPoolExecutor videoSliceThreadPool() { 
        return new ThreadPoolExecutor(videoSliceThreadPoolCorePoolSize, videoSliceThreadPoolMaximumPoolSize,
                videoSliceThreadPoolKeepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(videoSliceThreadPoolWorkQueueLength),
                new CustomThreadFactory(videoSliceThreadPoolName),
                new DiscardOldestPolicy() { 
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        System.out.println(String.format("%s的%s视频切片任务被丢弃！%n", e.toString(), r.toString()));
                        super.rejectedExecution(r, e);
                    }
                });
    }
    
    
    @Value("${databaseThreadPool.name}")
    private String databaseThreadPoolName;
    
    @Value("${databaseThreadPool.corePoolSize}")
    private int databaseThreadPoolCorePoolSize;
    
    @Value("${databaseThreadPool.maximumPoolSize}")
    private int databaseThreadPoolMaximumPoolSize;
    
    @Value("${databaseThreadPool.keepAliveTimeSecond}")
    private int databaseThreadPoolKeepAliveTime;
    
    @Value("${databaseThreadPool.workQueueLength}")
    private int databaseThreadPoolWorkQueueLength;
    
    /**数据库读写线程池--用于涉及数据库耗时操作的异步处理*/
    @Bean
    @Scope("singleton") 
    ThreadPoolExecutor databaseThreadPool() {
        return new ThreadPoolExecutor(databaseThreadPoolCorePoolSize, databaseThreadPoolMaximumPoolSize, databaseThreadPoolKeepAliveTime, TimeUnit.SECONDS, 
                    new ArrayBlockingQueue<Runnable>(databaseThreadPoolWorkQueueLength), new CustomThreadFactory(databaseThreadPoolName),
                    new AbortPolicy() { /*该策略将降低任务提交速率,但造成用户线程性能下降, 用户响应慢！*/
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            throw new RejectExecuteFastException("数据库任务拒绝提交！"); //抛出异常通知用户线程
                        }
                    }
                );
    }
    
    @Value("${recycleThreadPool.name}")
    private String recycleThreadPoolName;
    
    @Value("${recycleThreadPool.corePoolSize}")
    private int recycleThreadPoolCorePoolSize;
    
    @Value("${recycleThreadPool.maximumPoolSize}")
    private int recycleThreadPoolMaximumPoolSize;
    
    @Value("${recycleThreadPool.keepAliveTimeSecond}")
    private int recycleThreadPoolKeepAliveTime;
    
    @Value("${recycleThreadPool.workQueueLength}")
    private int recycleThreadPoolWorkQueueLength;
    
    @Bean
    @Scope("singleton")
    ThreadPoolExecutor recycleThreadPool() {
        return new ThreadPoolExecutor(recycleThreadPoolCorePoolSize, recycleThreadPoolMaximumPoolSize,
                recycleThreadPoolKeepAliveTime, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(recycleThreadPoolWorkQueueLength),
                new CustomThreadFactory(recycleThreadPoolName),
                new AbortPolicy() { 
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        throw new RejectExecuteFastException("回收线程拒绝提交！");  //抛出异常通知用户线程
                    }
                });
    }
    
    @Value("${scheduledRecycleThreadPool.corePoolSize}")
    private int scheduledRecycleThreadPoolCorePoolSize;
    
    @Bean
    @Scope("singleton")
    ScheduledExecutorService scheduledRecycleThreadPool() {
        return Executors.newScheduledThreadPool(scheduledRecycleThreadPoolCorePoolSize);
    }
    
    
    private static class CustomThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public CustomThreadFactory(String threadPoolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();
            namePrefix = threadPoolName + "-" + poolNumber.getAndIncrement() + "-thread-" + threadNumber;  //自定义线程池名字
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
