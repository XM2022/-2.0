package priv.xm.xkcloud.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import priv.xm.xkcloud.mapper.FileMapper;
import priv.xm.xkcloud.model.File;

@Component(value="fileCache")
@Scope(value="singleton")
public class FileCache implements InitializingBean{
    public static final int LOAD_SIZE = 10000;
    //private AtomicInteger size = new AtomicInteger(0);
    private volatile int maxFileId; /**用于手动设置fileId*/
    //所有类型缓存都是同一个文件集
    @Autowired      /**设计缺陷:多个用户并发上传同一份MD5文件, 前面完成的文件信息将被后面的覆盖, 造成用户文件丢失假象！*/
    private ConcurrentHashMap<String, List<File>> md5Cache;  //MD5-File--用于极速上传检测重复文件
    @Autowired
    private ConcurrentHashMap<String, List<File>> fileNameCache;  //fileName-File--用于检测文件旧版本
    @Autowired
    private ConcurrentHashMap<Integer, File> idCache;  //id-File--(主键id)用于下载/删除文件
    
    @Autowired
    @Qualifier("databaseThreadPool")
    private ThreadPoolExecutor databaseThreadPool;
    
    @Autowired
    private FileMapper fileMapper;
    
    @Override
    /**从数据库预加载部分连续数据进内存*/
    public void afterPropertiesSet() throws Exception {
        System.err.println("预加载文件数据...");  
        List<priv.xm.xkcloud.model.File> cacheFileList = fileMapper.findFile(0, FileCache.LOAD_SIZE);
        for (priv.xm.xkcloud.model.File cacheFile : cacheFileList) {
            addItem(md5Cache, cacheFile.getMD5(), cacheFile);
            addItem(fileNameCache, cacheFile.getFilename(), cacheFile);
            addItem(cacheFile);
        }
        //无论如何,插入并删除一条新纪录来获取起始id,防止删除用户产生id间隔的问题
        fileMapper.insertFile(new File("", "", 0f, null, 0, 4, ""));
        maxFileId = fileMapper.findFileStartId();
        fileMapper.deleteFileById(maxFileId);

    }
    
    public FileCache() {
        /*md5Cache = new ConcurrentHashMap<String, List<File>>(LOAD_SIZE);
        fileNameCache = new ConcurrentHashMap<String, List<File>>(LOAD_SIZE);
        idCache = new ConcurrentHashMap<Integer, File>(LOAD_SIZE);*/
    }
    
    /**
     * 当缓存容量达到LOAD_SIZE时, 不再进行增加新记录. 
     * 以保证缓存大小稳定和记录行的连续性、使得LIMIT语句不会失效.
     * @Param file 该参数不携带主键信息(id=0)！
     * @return 此文件id
     */
    public void addFile(File file) {
        if(idCache.size() >= LOAD_SIZE) return ;  //说明缓存容量已满(数据连续)
        
        file.setId(++maxFileId);
        addItem(md5Cache, file.getMD5(), file);
        addItem(fileNameCache, file.getFilename(), file);
        addItem(file);
    }

    /**注意缓存删除操作必须数据库操作放在后面！*/
    public void deleteFile(File file) {
        if(file == null) throw new NullPointerException();

        //id缓存中存在,则其它类型缓存也一定存在.
        if(idCache.containsKey(file.getId())) { 
            removeItem(md5Cache, file.getMD5(), file);
            removeItem(fileNameCache, file.getFilename(), file);
            removeItem(file.getId());
            
            //对应的,按顺序进数据库查询一条数据进缓存
            databaseThreadPool.execute(()->{
                try { 
                    List<File> cacheItem = fileMapper.findFile(size(), 1);
                    if(cacheItem.size() > 0) {
                        File databaseFile = cacheItem.get(0);
                        addItem(md5Cache, databaseFile.getMD5(), databaseFile);
                        addItem(fileNameCache, databaseFile.getFilename(), databaseFile);
                        addItem(databaseFile);
                        maxFileId = databaseFile.getId();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**调用前需要存在判断缓存存在key,否则.size()将产生NullPointerException*/
    private <E>void removeItem(ConcurrentHashMap<E, List<File>> cache, E key, File file) {
        List<File> fileList = cache.get(key);
        if(fileList.size() > 1) fileList.remove(file);
        else cache.remove(key);
    }
    
    /**the previous value associated with key, 
     * or null if there was no mapping for key*/
    private void removeItem(int id) {
        idCache.remove(id);
    }
    
    /**
     * 注意如果file更新了MD5, 请调用带有oldMd5参数的重载版本！
     * 根据文件主键(不变性)进行查找; 文件的任何信息都可能发生修改(除了主键),
     * 都要调用此方法进行缓存信息的更新,否则前端文件列表将得到过时的信息.
     * @param file 携带最新修改信息的文件
     */
    public void updateFileInfo(File file) {
        updateFileInfo(file, null);
    }
    
    public void updateFileInfo(File file, String oldMd5) {
        if(file == null) throw new NullPointerException();
        
        int id = file.getId();
        File oldFile = this.getWithId(id);
        if(oldFile != null) oldFile.copyInfo(file);   //所有缓存类型都引用同一文件对象, 同步更新.
        if(oldMd5 == null) return; //MD5不需要更新
        /**MD5缓存更新涉及到Map主键, 需要单独更新.*/
        if(md5Cache.containsKey(oldMd5)) { 
            if(md5Cache.containsKey(oldMd5)) removeItem(md5Cache, oldMd5, file);
            addItem(md5Cache, file.getMD5(), file);
        }
        
    }
    
    public int size() {
        return idCache.size();
    }
    
    /**没有则返回null*/
    public List<File> getWithMd5(String md5) {
        return md5Cache.get(md5);
    }
    
    /**没有则返回null*/
    public List<File> getWithFileName(String md5) {
        return fileNameCache.get(md5);
    }
    
    /**没有则返回null*/
    public File getWithFileNameAndUserId(String fileName, int userId) {
        List<File> sameNameFiles = fileNameCache.get(fileName);
        if(sameNameFiles == null) return null;
        for (priv.xm.xkcloud.model.File sameNameFile : sameNameFiles) {
            if(sameNameFile.getUser_id() == userId)  {
                return sameNameFile;  //同一用户只存在一份同名文件
            }
        }
        return null;
    }
    
    /**没有则返回null*/
    public File getWithId(int id) {
        return idCache.get(id);
    }
    
    public int getMaxFileId() {
        return maxFileId;
    }
    
    public boolean containsKey(String md5) {
        return md5Cache.containsKey(md5);
    }
    
    public int countFileQuantity(int userId) {
        int counter = 0;
        Collection<File> allCacheFiles = idCache.values();
        //hash表使用forEach/迭代器性能更好
        for (Iterator<File> iterator = allCacheFiles.iterator(); iterator.hasNext();) {
            if(iterator.next().getUser_id() == userId) ++counter;
        }
        return counter;
    }
    
    /**startindex从0开始*/
    @Deprecated
    public List<File> getUserFiles(int startindex, int pagesize, int userId) {
        /*此处实现有问题,未考虑id间隔*/
        int startId = (this.maxFileId - idCache.size() + 1) + startindex;
        List<File> userFiles = new ArrayList<File>(pagesize);
        File tempFile = null;
        for (int i = 0; i < pagesize; i++) {
            tempFile = idCache.get(startId+i);
            if(tempFile!=null && userId==tempFile.getUser_id()) userFiles.add(tempFile);
        }
        return userFiles;
    }
    
    private <E>void addItem(ConcurrentHashMap<E, List<File>> cache, E key, File file) {
        if(cache.containsKey(key)) {  //同一个MD5文件可能有不同名字和用户Id
            cache.get(key).add(file);
        }
        else{
            LinkedList<File> fileList = new LinkedList<File>();
            fileList.add(file);
            cache.put(key, fileList);
        }
    }
    
    private void addItem(File file) {
        idCache.put(file.getId(), file);
    }

}
