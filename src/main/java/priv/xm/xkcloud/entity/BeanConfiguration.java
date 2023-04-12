package priv.xm.xkcloud.entity;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import priv.xm.xkcloud.model.File;
import priv.xm.xkcloud.model.User;

@Configuration
public class BeanConfiguration {

    @Bean
    @Scope("prototype")
    public ConcurrentHashMap<String, Future<FfmpegRunResult>> videoTaskHashMap() {
        return new ConcurrentHashMap<String, Future<FfmpegRunResult>>();
    }
    
    @Bean
    @Scope("prototype")
    public ConcurrentHashMap<String, List<File>> fileCacheHashMap() {
        return new ConcurrentHashMap<String, List<File>>(FileCache.LOAD_SIZE);
    }
    
    @Bean
    @Scope("prototype")
    public ConcurrentHashMap<Integer, File> fileIdHashMap() {
        return new ConcurrentHashMap<Integer, File>(FileCache.LOAD_SIZE);
    }
    
    @Bean
    @Scope("prototype")
    public ConcurrentHashMap<String, User> userHashMap() {
        return new ConcurrentHashMap<String, User>(UserCache.LOAD_SIZE);
    }
    
    @Bean
    @Scope("singleton")
    public ConcurrentHashMap<String, Set<Integer>> fileSliceHashMap() {
        return new ConcurrentHashMap<String, Set<Integer>>();
    }
    
    @Bean
    @Scope("singleton")
    public FileOccupyCallback fileOccupyCallback() {
        return FileOccupyCallback.getInstance();
    }
    
    @Bean
    @Scope("singleton")
    public ConcurrentSkipListSet<String> videoSliceFileSet() {
        return new ConcurrentSkipListSet<String>();
    }
    
    @Bean
    @Scope("singleton")
    public ConcurrentSkipListSet<String> uploadSliceFileSet() {
        return new ConcurrentSkipListSet<String>();
    }
    
    @Bean
    @Scope("singleton")
    public ConcurrentSkipListSet<String> videoRunFfmpegSet() {
        return new ConcurrentSkipListSet<String>();
    }
    
    @Bean
    @Scope("singleton")
    public ConcurrentHashMap<String, ReadLock> videoFileReadLock() {
        return new ConcurrentHashMap<String, ReadLock>();
    }
}
