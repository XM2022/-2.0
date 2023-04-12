package priv.xm.xkcloud.service.imp;

import static priv.xm.xkcloud.message.Message.VIDEO_ANALYSIS_FAIL;
import static priv.xm.xkcloud.message.Message.VIDEO_ANALYSIS_TIMEOUT;
import static priv.xm.xkcloud.message.Message.VIDEO_FILE_NOT_EXIST;
import static priv.xm.xkcloud.model.File.M3U8_FILE_NAME;
import static priv.xm.xkcloud.model.File.SERVICE_ROOT_PATH;
import static priv.xm.xkcloud.model.File.UPLOAD_FILE_SLICE_ROOT_PATH;
import static priv.xm.xkcloud.model.File.VIDEO_SLICE_ROOT_DIRECTORY;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import priv.xm.xkcloud.entity.FfmpegRunResult;
import priv.xm.xkcloud.entity.FfmpegRunResult.StateCode;
import priv.xm.xkcloud.entity.FileCache;
import priv.xm.xkcloud.entity.FileOccupyCallback;
import priv.xm.xkcloud.exception.FfmpegTryLockFailException;
import priv.xm.xkcloud.exception.FileNotExistException;
import priv.xm.xkcloud.exception.UploadSliceRecyleException;
import priv.xm.xkcloud.exception.VideoSliceRecyleException;
import priv.xm.xkcloud.mapper.FileMapper;
import priv.xm.xkcloud.mapper.UserMapper;
import priv.xm.xkcloud.model.File;
import priv.xm.xkcloud.model.Page;
import priv.xm.xkcloud.model.PageBean;
import priv.xm.xkcloud.service.FileService;
import priv.xm.xkcloud.util.CommonUtil;
import priv.xm.xkcloud.util.FfmpegUtil;


@Service
@Scope("singleton")
public class FileServiceImp implements FileService, InitializingBean {
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TransactionDefinition transactionDefinition;
	@Autowired
	private FileMapper fileMapper;
	@Autowired
	private UserMapper userMapper;
	@Autowired
	private FileCache fileCache; 
    @Autowired
    @Qualifier("databaseThreadPool")
    private ThreadPoolExecutor databaseThreadPool;
    @Autowired
    @Qualifier("videoSliceThreadPool")
    private ThreadPoolExecutor videoSliceThreadPool;
    @Autowired
    @Qualifier("recycleThreadPool")
    private ThreadPoolExecutor recycleThreadPool;
    @Autowired
    private ScheduledExecutorService scheduledRecycleThreadPool;
    @Autowired
    private FileOccupyCallback fileOccupyCallback;
    
    /**保存每个视频文件对应的唯一ReentrantReadLock*/
    @Autowired
    private ConcurrentHashMap<String, ReadLock> videoFileReadLock;  //String: MD5
    
    /**切片传输流量监控开关: 1>开启 0>关闭 
     * -1>锁住监控期间没有访问过的视频切片文件夹,拒绝新请求访问,准备删除这些文件夹*/
    private static volatile int isMonitorringVideoFile = 1; 
    /**保存视频切片目录访问记录--用于回收线程*/
    @Autowired
    @Qualifier("videoSliceFileSet")
    private ConcurrentSkipListSet<String> currentVisitedVideoSliceFolders;
    
    /**切片传输流量监控开关: 1>开启 0>关闭 
     * -1>锁住监控期间没有访问过的视频切片文件夹,拒绝新请求访问,准备删除这些文件夹*/
    private static volatile int isMonitorringUploadFile = 0; 
    /**保存视频切片目录访问记录--用于回收线程*/
    @Autowired
    @Qualifier("uploadSliceFileSet")
    private ConcurrentSkipListSet<String> currentVisitedUploadSliceFolders;
    /**
     * 保存已上传过的文件切片序号列表
     * String: fileName+userId
     * 写操作频繁而且需要有序存储，可以选择 ConcurrentSkipListSet.
     * 读取操作远远超过写操作，可以选择 CopyOnWriteArrayList.
     * 而synchronized包装hashMap并发量较高时性能较低.
     * 综上, Set<Integer>集合插入删除频繁, 应选用ConcurrentSkipListSet.
     */
    @Autowired
    private ConcurrentHashMap<String, Set<Integer>>  savedFileSlices;  //String:fileName+userId
    /**保存用户的视频切片线程任务*/
    @Autowired
    private ConcurrentHashMap<String, Future<FfmpegRunResult>>  videoSliceTasks;
    
    /**用于保证同一个视频文件只有一个ffmpeg进程在进行切片*/
    @Autowired 
    @Qualifier("videoRunFfmpegSet")
    private Set<String> videoRunFfmpegQueue;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        this.startupScheduledRecycle();
    }
    
	/**不存在返回null*/
	@Override
    public File findFileById(int id) throws Exception{
	    //先从缓存中查询文件路径
        File file = fileCache.getWithId(id);
        //如果没有,再从数据库中查询
        if(file == null) file = fileMapper.findFileById(id, fileCache.size(), Integer.MAX_VALUE);
		return file;
	}
	
    /**此方法直接到数据库查询,不走缓存*/
	@Override
    public List<File> findFile(int limitOffset, int entryNumber) throws Exception {
        return fileMapper.findFile(limitOffset, entryNumber);
    }
	
	/**此方法直接到数据库查询,不走缓存*/
	@Override
    public int findFileStartId() throws Exception {
	    return fileMapper.findFileStartId();
	}
	
	@Override
    public boolean containsMd5(String md5) throws Exception {
        return fileCache.containsKey(md5) 
            || fileMapper.findMd5(md5, fileCache.getMaxFileId()) > 0;
    }
	
	@Override
    public int countUserFiles(String userName) throws Exception {
	    Integer userId = userMapper.findUserId(userName);
	    return fileCache.countFileQuantity(userId)
	            + fileMapper.countFileQuantity(userId, fileCache.size());
	}
	
	/**必须从数据库中查询,不能走缓存--按最新创建时间排序*/
    @Override
    public  List<File> getUserFiles(Page page) throws Exception {
        /*int pageSize = page.getPageSize();
        List<File> userFiles = fileCache.getUserFiles(page.getStartindex(), pageSize, page.getUserId());
        if(userFiles.size() < pageSize) {  //未查询到足够记录数量, 进数据库查询
            fileMapper.getUser
        }
        //对元素按最新创建时间重排
        userFiles.sort((f1, f2)->{
            return f1.getCreatetime().compareTo(f2.getCreatetime());
        });*/
        return fileMapper.getUserFiles(page);
    }
	
	@Override
    //@Transactional(rollbackFor = Exception.class) //删除文件和更新使用空间是两次数据库操作,需要开启事务
    public void deleteFile(File file) throws Exception {
	    TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
	    try {
	        //设计缺陷1:必须先进行文件删除--即使可能造成文件访问不存在
	        deleteOrRetainFile(file.getFilename(), file.getUser_id()); 
            fileMapper.deleteFileById(file.getId());
            userMapper.updateUsedSpace(file.getUser_id(),  -(int)Math.round(file.getFilesize()));
            transactionManager.commit(transaction);  //必须先提交事务, 否则造成缓存问题
            transaction = null;
            fileCache.deleteFile(file); //设计缺陷2:注意缓存删除操作需要放在数据库操作之后!
	    } catch (Exception e) {
            if(transaction!=null) transactionManager.rollback(transaction);
	        throw e;
        }
	}
	
	@Override
    public void shareFile(File file, int shareState) {
        file.setCanshare(shareState);
        databaseThreadPool.execute(()-> {
            try {
                fileMapper.updateFileById(file.getId(), shareState);
                fileCache.updateFileInfo(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
	}
	
	/**必须从数据库中查询,不能走缓存--按最新创建时间排序.
	 * 并且HashMap每次取出的次序都不一致, 造成翻页混乱*/
	@Override
    public PageBean searchFile(Page page) throws Exception {  
	    List<File> shareFileList = fileMapper.searchAllSharedFile();  //优化点.
	    //int preQueryNumber = pageSize + 1000;  //预搜索记录数, 不够再查
	    int pageSize = page.getPagesize();
	    int currentpage = page.getCurrentpage();
	    String searchContent = page.getSearchcontent();
        List<File> matchList;
	    if(!"*".equals(searchContent)) {  //星号查询所有共享文件, 不用进行匹配.
            String[] keyWords = searchContent.split("\\s+");
            System.out.printf("提取到的关键字: %s%n", Arrays.toString(keyWords));
            /**正则表达式--正向前瞻技术(高级特性)
             * 示例: "^(?=.*a)(?=.*b)(?=.*c).*$" */
            StringBuilder regBuilder = new StringBuilder("^");
            for (int i = 0; i < keyWords.length; i++) regBuilder.append(String.format("(?=.*(%s))", keyWords[i])); 
            regBuilder.append(".*$");
            String regularExpression = regBuilder.toString();
            System.out.println("正则字符:" + regularExpression);
            Pattern pattern = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            matchList = new LinkedList<>();
            for (File file : shareFileList) { 
                if(pattern.matcher(file.getFilename()).matches()) matchList.add(file);
            }
            shareFileList.clear();
	    }
	    else {
	        matchList = shareFileList;
	        shareFileList = new LinkedList<File>();
	    }
	    
	    //实现翻页
        int counter = 0;     
	    int startItem = (currentpage-1)*pageSize;
        for (File file : matchList) {
            if(counter++ >= startItem) shareFileList.add(file);
            if(shareFileList.size() >= pageSize) break;
        }
        //拿到每页的数据，每个元素就是一条记录
        PageBean pageBean = new PageBean();
        pageBean.setList(shareFileList);
        pageBean.setCurrentpage(currentpage);
        pageBean.setPagesize(pageSize);
        try {
            pageBean.setTotalrecord(matchList.size());
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        
        return pageBean;
	}
	
    /**检查服务器中是否存在相同文件(同MD5).
     * 如果存在执行极速上传并返回true,不存在则返回false.
     */
    @Override
    @Transactional(rollbackFor=Exception.class) //上传文件和更新使用空间是两次数据库操作,需要开启事务
    public boolean fastUpload(String md5, int userId, String fileName, float fileSize, HttpSession session) throws Exception {
        boolean result = false;
        List<priv.xm.xkcloud.model.File> sameMd5FileList = fileCache.getWithMd5(md5);
        if(sameMd5FileList != null) {  //缓存中存在同一文件
            /**
             * 优先采用更新记录(如果存在同名同MD5文件), 若不存在拷贝文件再进行插入新记录.
             * 注意先确认数据库中没有同名名MD5文件后再从缓存中进行拷贝信息 */
            if(!updateFileInfo(sameMd5FileList, userId, fileName)) {
                if(!updateOrCopyFileInfoFromDatabase(md5, fileName, userId)) copyFileInfoForUpload(sameMd5FileList.get(0), fileName, userId);  
            }
            result = true;
        }
        else {
            result = updateOrCopyFileInfoFromDatabase(md5, fileName, userId);
            if(!result) {
                //将整个文件的md5和fileSize存入session供实际上传时备用.
                session.setAttribute(fileName+"md5", md5);
                session.setAttribute(fileName+"size", fileSize);
                //定时移除属性, 节约内存资源
                scheduledRecycleThreadPool.schedule(()->{
                    session.removeAttribute(fileName+"md5");
                    session.removeAttribute(fileName+"size");
                }, 30, TimeUnit.MINUTES);
            }
        }
        return result;
    }
    
    /**
     * updateOrCopy--尽量一次性完成操作,避免反复查询数据库,提高性能
     * @return true: 完成上传-数据库中含有相同MD5文件
     *         false: 未完成上传-数据库中没有相同MD5文件
     */
    private boolean updateOrCopyFileInfoFromDatabase(String md5, String fileName, int userId) throws Exception {
        List<priv.xm.xkcloud.model.File> fileList= fileMapper.findFileByMd5(md5, fileCache.size(), Integer.MAX_VALUE);
        if(fileList.size() > 0) {  //数据库中含有相同MD5文件
            if(!updateFileInfo(fileList, userId, fileName)) { 
                priv.xm.xkcloud.model.File editItem = fileList.get(0);
                copyFileInfoForUpload(editItem, fileName, userId);
            }
            return true;
        }
        else {
            return false;  
        }
    }
    
    /**
     * 存在同用户、同名文件完成上传并更新数据库和缓存.
     * @return true: 完成上传,存在同MD5同名文件,;
     *         false: 未完成上传,不存在同MD5同名文件.
     * @throws Exception 
     */
    private boolean updateFileInfo(List<priv.xm.xkcloud.model.File> sameMd5FileList, int userId, String fileName) throws Exception {
        for (priv.xm.xkcloud.model.File f : sameMd5FileList) {
            if(f.getUser_id() == userId && f.getFilename().equals(fileName)) { //用户上传过同一文件(同MD5同名)
                f.setCreatetime(new Date(new java.util.Date().getTime()));
                fileMapper.updateFileInfo(f);
                fileCache.updateFileInfo(f);
                return true;
            }
        }
        return false;
    }
    
    /**上层调用方法必须使用@Transactional注解进行事务回滚*/
    private void copyFileInfoForUpload(priv.xm.xkcloud.model.File file, String fileName, int userId) {
        //注意! file可能来自于缓存,直接修改会造成缓存中数据的同步修改, 需拷贝新对象
        final priv.xm.xkcloud.model.File copyFile = file.clone();
        copyFile.setCanshare(0);  //默认私有
        copyFile.setUser_id(userId);
        copyFile.setFilename(fileName);
        copyFile.setCreatetime(new Date(new java.util.Date().getTime()));
        databaseThreadPool.execute(()-> {
            try {
                synchronized(this) {
                    fileMapper.insertFile(copyFile);
                    userMapper.updateUsedSpace(userId, +(int)Math.round(file.getFilesize()));
                    fileCache.addFile(copyFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } 
        });
    }
    
    /**
     * 文件分片实际上传.
     * @fileSize MB--保留两位小数
     * @return 不是代表传输成功与否.
     *    true:最后一块切片 false:非最后切片
     */
    @Override
    @Transactional(rollbackFor=Exception.class) //上传文件和更新使用空间是两次数据库操作,需要开启事务
    public boolean upload(CommonsMultipartFile file, String sliceName, int userId, HttpSession session) throws Exception {
        int separatorPos1 = sliceName.indexOf('-'),
            separatorPos2 = sliceName.indexOf('-', separatorPos1+1);
        //注意装箱拆箱的性能消耗
        Integer number = Integer.valueOf(sliceName.substring(0, separatorPos1)),
                total = Integer.parseInt(sliceName.substring(separatorPos1+1, separatorPos2));
        String fileName = sliceName.substring(separatorPos2+1),
               folderName = fileName + "-" + String.valueOf(userId), //追加用户id:避免不同用户同名文件产生混淆
               slicesDirectory = UPLOAD_FILE_SLICE_ROOT_PATH + java.io.File.separator + folderName;  
        
        if(isMonitorringUploadFile == -1 && !currentVisitedUploadSliceFolders.contains(folderName)) throw new UploadSliceRecyleException();
        else if(isMonitorringUploadFile == 1) {
            currentVisitedUploadSliceFolders.add(folderName);
            //System.out.printf("%s文件夹上传记录被保存.%n", folderName);
        }
        Files.createDirectories(Paths.get(slicesDirectory));  //上传文件切片临时目录
        
        /**
         * CommonsMultipartFile.transferTo()解析:
         * 1.会覆盖删除同名文件夹!
         * 2.会覆盖写入同名文件, 而不会抛文件存在异常.
         */
        file.transferTo(new java.io.File(slicesDirectory, sliceName)); //切片文件写入磁盘
        
        Set<Integer> sliceIdSet = savedFileSlices.get(folderName);
        if(sliceIdSet==null && total>1) {
            //ConcurrentSkipListSet<Integer> newSliceSet = Collections.synchronizedSet(new HashSet<Integer>());
            ConcurrentSkipListSet<Integer> newSliceSet = new ConcurrentSkipListSet<Integer>();
            newSliceSet.add(number);  //不要忘记添加新元素！
            savedFileSlices.put(folderName, newSliceSet);
        }
        else if(sliceIdSet!=null && sliceIdSet.size()+1<total) sliceIdSet.add(number);
        else  {  //文件切片上传完毕
            currentVisitedUploadSliceFolders.add(folderName);  //声明使用状态合并文件; 无论isMonitor变量开启与否.
            File oldFile = deleteOrRetainFile(fileName, userId);
            //磁盘绝对路径: 服务器根路径/日期/
            String dirPath = SERVICE_ROOT_PATH + java.io.File.separator + DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDate.now()) + java.io.File.separator;
            Files.createDirectories(Paths.get(dirPath)); //如果不存在则创建(多级)目录
            //通过添加版本号解决文件同名冲突. 注意: 数据库中文件名仍使用原名映射!
            String uniqueFileName = fileName;
            while(Files.exists(Paths.get(dirPath, uniqueFileName))) {  //同名不同MD5文件
                uniqueFileName = fileName;  //避免循环返回叠加多个版本号
                int separatorPos = uniqueFileName.lastIndexOf('.');
                if(separatorPos == -1) uniqueFileName += CommonUtil.getUniqueVersion(); //无文件后缀
                else uniqueFileName = uniqueFileName.substring(0, separatorPos) + CommonUtil.getUniqueVersion() + "." + (uniqueFileName.length()-1>separatorPos ? uniqueFileName.substring(separatorPos+1) : "");
            }
            //先完成文件上传, 再写入数据库和缓存！
            //先临时用一下相关前端提供的数据
            String md5 = (String)session.getAttribute(fileName +"md5"); //null强转还是null, 不会发生异常
            Float fileSize = (Float)session.getAttribute(fileName +"size"); 
            //设计缺陷: 上传任务超过半小时一下属性将得到空值！
            if(fileSize == null) fileSize = 0f;
            if(md5 == null) md5 = "";
            File newFile;
            if(oldFile != null)  {//该用户存在旧版本(同名)文件, 更新记录
                String oldMd5 = oldFile.getMD5();
                newFile = oldFile;
                newFile.setMD5(md5);
                newFile.setFilepath(dirPath + uniqueFileName);
                newFile.setCreatetime(new Date(new java.util.Date().getTime()));
                newFile.setFilesize(fileSize);
                fileMapper.updateFileInfo(newFile);  //更新数据库记录
                fileCache.updateFileInfo(newFile, oldMd5);  //同步更新缓存
            }
            else {  //该用户不存在旧版本(同名)文件, 插入记录
                newFile = new File();
                newFile.setUser_id(userId);
                newFile.setCreatetime(new Date(new java.util.Date().getTime()));
                newFile.setFilename(fileName);
                newFile.setFilepath(dirPath + uniqueFileName);  //f.setFilepath(userName);
                newFile.setFilesize(fileSize);
                newFile.setCanshare(0);  //默认私有
                newFile.setMD5(md5);
                synchronized (this) {
                    fileMapper.insertFile(newFile);
                    fileCache.addFile(newFile);
                }
            }
            mergerFileSlice(folderName, newFile);  //后台线程进行文件合并
            savedFileSlices.remove(folderName); 
            userMapper.updateUsedSpace(userId, +(int)Math.round(fileSize));
            return true;
        }
        return false;
    }
    
    /**
     * @param mergerFileAbsolutePath 含uniqueFileName
     */
    private void mergerFileSlice(String folderName, File newFile) {
        databaseThreadPool.execute(()-> {
            TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
            String slicesDirectory = UPLOAD_FILE_SLICE_ROOT_PATH + java.io.File.separator + folderName;
            System.out.println("执行文件: " + newFile.getFilename());
            try {
                int bufSize = 1024*1024; //1MB
                byte[] bytes = new byte[bufSize];
                long realSize = 0L;
                MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                java.io.File sliceDir = new java.io.File(slicesDirectory);
                java.io.File[] slices = sliceDir.listFiles();
                Arrays.sort(slices, (s1, s2)->{
                    String name1 = s1.getName(),
                           name2 = s2.getName();
                    int num1 = Integer.parseInt(name1.substring(0, name1.indexOf('-'))),
                        num2 = Integer.parseInt(name2.substring(0, name2.indexOf('-')));
                    return num1 - num2;
                });
                BufferedOutputStream mergerOutput = new BufferedOutputStream(new FileOutputStream(newFile.getFilepath(), false), bufSize);
                for (java.io.File slice : slices) {
                    BufferedInputStream sliceInput = new BufferedInputStream(new FileInputStream(slice), bufSize);
                    int byteNum = 0;
                    while(true) {
                        byteNum = sliceInput.read(bytes);
                        if(byteNum == -1) break;
                        mergerOutput.write(bytes, 0, byteNum);
                        messageDigest.update(bytes, 0, byteNum);
                    }
                    realSize += slice.length();
                    sliceInput.close();
                    System.out.println(slice.getName() + "切片合并就完成.");
                    if(!slice.delete()) System.err.println(slice + "切片删除失败！");
                }
                //更新数据库和缓存相关前端临时数据
                float realMbSize = CommonUtil.ceilFileSize(realSize);
                String realMd5 = DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
                boolean sizeIsDifferent = false;
                boolean md5IsDifferent = false;
                String oldMd5 = newFile.getMD5();;
                if(realMbSize != newFile.getFilesize()) {
                    System.err.printf("fileSize不一致！前端:%f 后端:%f%n", newFile.getFilesize(), realMbSize);
                    newFile.setFilesize(realMbSize);
                    sizeIsDifferent = true;
                }
                if(!realMd5.equals(oldMd5)) {
                    System.err.printf("MD5不一致！%n前端:%s %n后端:%s%n", oldMd5, realMd5); //可能是前端伪造MD5; 不可能是传输损坏, 因为每个文件切片都进行了校验.
                    newFile.setMD5(realMd5);
                    md5IsDifferent = true;
                }
                if(md5IsDifferent || sizeIsDifferent) {
                    //因为对象引用, 缓存插入时已经做了同步更新newFile的id.
                    //newFile.setId(fileMapper.findFileId(newFile.getUser_id(), newFile.getFilename()));
                    fileMapper.updateFileInfo(newFile);  
                    fileCache.updateFileInfo(newFile, oldMd5); 
                    if(sizeIsDifferent) userMapper.updateUsedSpace(newFile.getUser_id(), +(int)Math.round(realMbSize));
                }
                if(!sliceDir.delete()) System.err.println(sliceDir + "切片文件夹删除失败！");
                mergerOutput.close();
                transactionManager.commit(transaction);
            }catch (Exception e) { 
                if(transaction != null) transactionManager.rollback(transaction);
                System.err.print("文件合并失败:");
                e.printStackTrace();
            } finally {
                currentVisitedUploadSliceFolders.remove(folderName); //与upload()方法中相呼应; 便于合并发生异常时回收线程尽快删除损坏切片.
            }
        });
    }
    
    /**
     * 进行删除/保留旧版本文件, 不涉及缓存和数据库的操作.
     * 注意:需要先调用此方法, 再进行缓存和数据库相关操作!
     * @return 存在返回旧版本文件(即使文件被删除), 不存在返回null.
     */
    private priv.xm.xkcloud.model.File deleteOrRetainFile(String fileName, int userId) throws Exception, IOException {
        //先从缓存中查询是否存在同userId、同名文件
        priv.xm.xkcloud.model.File delFile = fileCache.getWithFileNameAndUserId(fileName, userId);  
        if(delFile != null) {  //缓存中存在
            deleteFileIfReferenceIsOnlyOne(delFile);
        }  
        else { //从数据库中查询
            List<priv.xm.xkcloud.model.File> fileList = fileMapper.findFileByFileNameAndUserId(fileName, userId, fileCache.size(), Integer.MAX_VALUE);
            if(fileList.size() > 0) {
                delFile = fileList.get(0);
                deleteFileIfReferenceIsOnlyOne(delFile);
            }
        }
        return delFile;
    }
    
    /** @param file 同userId、同名文件, 不能为null*/
    private void deleteFileIfReferenceIsOnlyOne(priv.xm.xkcloud.model.File file) throws Exception {
        final String md5 = file.getMD5();
        //先从缓存中验证引用数量
        List<priv.xm.xkcloud.model.File> sameMd5Files = fileCache.getWithMd5(md5);
        if(sameMd5Files == null) {
            sameMd5Files =  fileMapper.findFileByMd5(md5, fileCache.size(), Integer.MAX_VALUE);
            if(sameMd5Files.size() == 1) fileOccupyCallback.registerDeleteTask(md5, file.getFilepath());
        }
        else if(sameMd5Files.size() >= 2) {
            return;          //尽量避免从数据库中查询, 提高性能和并发量
        }
        else {  //sameMd5Files.size() == 1
            /*从数据库中进一步验证*/
            sameMd5Files =  fileMapper.findFileByMd5(md5, fileCache.size(), Integer.MAX_VALUE);
            if(sameMd5Files.size() == 0) fileOccupyCallback.registerDeleteTask(md5, file.getFilepath());
        }

    }
    
    @Override
    public boolean downloadFile(File dwFile, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if(!fileOccupyCallback.addOccupyFile(dwFile.getMD5(), dwFile.getFilepath())) return false;
        try {
            java.io.File file = new java.io.File(dwFile.getFilepath());
            String filename = file.getName();
            
            //设置响应头
            response.setHeader("Accept-Ranges", "bytes");  //通知客户端允许断点续传多线程连接下载,响应的格式是:Accept-Ranges: bytes
            response.setContentType(CommonUtil.setContentType(filename)); 
            //解决下载文件名乱码
            if(request.getHeader("User-Agent").toLowerCase().indexOf("firefox") != -1) {  //火狐浏览器
                //实测只有PC火狐有效,安卓端无效.
                //filename = "=?UTF-8?B?" + new String(Base64.encodeBase64(filename.getBytes("UTF-8"))) + "?="; 
                //正解!
                filename = new String(filename.getBytes(), StandardCharsets.ISO_8859_1);
            }
            else { //其它浏览器
                filename = URLEncoder.encode(filename, "UTF-8");
            }
            response.addHeader("Content-Disposition", "attachment; filename=" + filename);
            //开始下载
            breakpointTransfer(file, request, response);
        }
        finally {
            fileOccupyCallback.removeOccupyFile(dwFile.getMD5());
        }
        return true;
    }
    
    /**针对于文件直接在浏览器打开的请求*/
    @Override
    public void openFile(File file, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if(!fileOccupyCallback.addOccupyFile(file.getMD5(), file.getFilepath())) throw new FileNotExistException();   //加入占用列表, 避免下载过程被删除.
        try {
            java.io.File transferedFile = new java.io.File(file.getFilepath());
            long fileSize = transferedFile.length();
            String fileName = transferedFile.getName();
            //解决下载文件名乱码
            if(request.getHeader("User-Agent").toLowerCase().indexOf("firefox") != -1) fileName = new String(fileName.getBytes(), StandardCharsets.ISO_8859_1);
            else fileName = URLEncoder.encode(fileName, "UTF-8");
            response.setContentType(CommonUtil.setContentType(fileName)+";charset=UTF-8");
            response.setContentLength((int)fileSize);
            response.setHeader("Content-Disposition", "inline; filename=" + fileName);  //inline:让浏览器直接显示打开.
            //禁止浏览器缓存文件--避免占用用户磁盘空间
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            breakpointTransfer(transferedFile, request, response);
        } finally {
            fileOccupyCallback.removeOccupyFile(file.getMD5());
        }
    }
    

    /**用于视频切片等小文件完整传输
     * @return true:正常传输 false:文件夹被锁住,中断传输.
     * @throws ExecutionException ffmpeg进程异常
     * @throws InterruptedException 任务中断异常
     * @throws IOException 数据库异常
     */
    @Override
    public void transferFragment(String folderName, String videoFragment, HttpServletResponse response) throws IOException  {
        if(isMonitorringVideoFile == -1 && !currentVisitedVideoSliceFolders.contains(folderName)) throw new VideoSliceRecyleException();
        else if(isMonitorringVideoFile == 1) {
            currentVisitedVideoSliceFolders.add(folderName);  //不用移除, 由回收线程清空.
            System.out.printf("%s文件夹播放记录被保存.%n", folderName);
        }
        
        String filePath = VIDEO_SLICE_ROOT_DIRECTORY.getPath() + java.io.File.separator + folderName + java.io.File.separator + videoFragment;
        java.io.File file = new java.io.File(filePath);
        long fileLength = file.length();
        
        //设置响应头
        if(".m3u8".equals(videoFragment.substring(videoFragment.lastIndexOf(".")))) { //.m3u8索引文件
            response.addHeader("content-type", "application/vnd.apple.mpegurl");
        }
        else {  //.ts文件
            response.addHeader("content-type", "text/vnd.trolltech.linguist; charset=utf-8");
            response.addHeader("Content-Disposition", "attachment; filename=" + videoFragment); //将文件中文名编码为16进制供浏览器解析
        }
        response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK);
        response.setHeader("Accept-Ranges", "bytes"); 
        response.setHeader("Content-Length", String.valueOf(fileLength));
        //禁止浏览器缓存视频
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        //文件传输
        universalTransfer(file, file.length()-1, response.getOutputStream(), 1024);
    }
    
    
    /**对universalTransfer的更上层封装*/
    private void breakpointTransfer(java.io.File file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int buffSize = 1024;  //性能调优
        String rangeBytes = ""; // 记录客户端传来的形如“bytes=27000-”或者“bytes=27000-39000”的内容
        long fileSize = file.length();
        ServletOutputStream reponseOutput = response.getOutputStream();
        //响应格式:Content-Range: bytes [文件块的开始字节]-[文件的总大小 - 1]/[文件的总大小]; -1是因为从0字节开始
        if (request.getHeader("Range") == null) { 
            System.out.println("文件全文下载");
            long endByte = fileSize-1;
            response.setStatus(HttpServletResponse.SC_OK);  //200
            response.addHeader("Content-Length", String.valueOf(fileSize));
            response.setHeader("Content-Range",  String.format("bytes %d-%d/%d", 0, endByte, fileSize));
            universalTransfer(file, endByte, reponseOutput, buffSize);
        }
        else {
            response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);  //206
            rangeBytes = request.getHeader("Range").replaceAll("bytes=", "");
            long startByte = Long.parseLong(rangeBytes.substring(0, rangeBytes.indexOf('-')).trim()); // bytes=1275856879-1275877358，从第 1275856879 个字节开始下载
            long endByte = 0L;
            if (rangeBytes.indexOf('-') == rangeBytes.length() - 1) { // bytes=969998336-
                System.out.println("文件断点续传:某处至文件尾");
                endByte = fileSize-1;
            } else { // bytes=1275856879-1275877358
                System.out.println("文件断点续传:某处至某处");
                endByte = Long.parseLong(rangeBytes.substring(rangeBytes.indexOf('-') + 1, rangeBytes.length()).trim()); // bytes=1275856879-1275877358，到第 1275877358 个字节结束
            }
            response.addHeader("Content-Length", String.valueOf(endByte-startByte+1));  //起始字节也包括:+1
            response.setHeader("Content-Range",  String.format("bytes %d-%d/%d", startByte, endByte, fileSize));
            universalTransfer(file, startByte, endByte, reponseOutput, buffSize);
        }
    }
    
    /**缓冲流版本--startPos=0*
     * @param endByte 注意传输包括此字节, 所以传参需要file.length-1！(设计缺陷)
     * @param buffSize 性能调优点
     */
    private void universalTransfer(java.io.File file, long endByte, OutputStream os, int buffSize) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(os, buffSize); //Response的输出流不用手动关闭, 由JSP容器在服务器关闭时自动关闭.
        try(BufferedInputStream raf = new BufferedInputStream(new FileInputStream(file), buffSize)) {
            long contentLength = 0L; // 客户端请求下载的字节数
            byte bytes[] = new byte[buffSize];//暂存容器
            
            int n = 0;
            long readLength = 0L; // 记录已读字节数
            contentLength = endByte + 1;  //起始字节也包括:+1
            while (readLength+buffSize <= contentLength) { //整块字节在这里读取
                n = raf.read(bytes); 
                //readLength += 1024;  文件损坏点--不能保证读取到的字节数一定是1024!
                readLength += n;
                out.write(bytes, 0, n);  
            }
            while (readLength < contentLength) { // 余下的不足buffSize个字节在这里读取
                n = raf.read(bytes, 0, (int) (contentLength - readLength));  
                readLength += n;
                out.write(bytes, 0, n);
            }
            out.flush();
        } 
    }
    
    /**
     * 文件随机流版本
     * @param endByte 注意传输包括此字节, 所以传参需要file.length-1！(设计缺陷)
     * @param buffSize  性能调优点
     */
    private void universalTransfer(java.io.File file, long startByte, long endByte, OutputStream os, int buffSize) throws IOException {
        BufferedOutputStream out = new BufferedOutputStream(os);  //Response的输出流不用手动关闭, 由JSP容器在服务器关闭时自动关闭.
        try(RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long contentLength = 0L; // 客户端请求下载的字节数
            byte bytes[] = new byte[buffSize];//暂存容器
            
            int n = 0;
            long readLength = 0L; // 记录已读字节数
            contentLength = endByte - startByte + 1;  //起始字节也包括:+1
            raf.seek(startByte); // 形如 bytes=1275856879-1275877358 的客户端请求，找到第 1275856879 个字节
            while (readLength+buffSize <= contentLength) { //整块字节在这里读取
                n = raf.read(bytes); 
                //readLength += 1024;  文件损坏点--不能保证读取到的字节数一定是1024!
                readLength += n;
                out.write(bytes, 0, n);  
            }
            while (readLength < contentLength) { // 余下的不足buffSize个字节在这里读取
                n = raf.read(bytes, 0, (int) (contentLength - readLength));  
                readLength += n;
                out.write(bytes, 0, n);
            }
            out.flush();
        } 
        
    }

    /**@return 相对VIDEO_SLICE_ROOT_DIRECTORY的路径: "文件夹/.m3u8文件名"*/
    @Override
    public String sliceVideo(File videoFile) throws Exception {
        //目录名使用视频文件的md5值--好处多多!
        String videoFileMd5 = videoFile.getMD5();
        String videoFilePath = videoFile.getFilepath();
        String folderName = videoFileMd5;
        String sliceRelavitePath = folderName + java.io.File.separator + M3U8_FILE_NAME; 
        String[] videoFolders = VIDEO_SLICE_ROOT_DIRECTORY.list((dir, fileName)->{ return fileName.equals(folderName); });
        if(videoFolders!=null && videoFolders.length>0) return sliceRelavitePath; //视频切片文件已存在
        if(!videoRunFfmpegQueue.add(videoFileMd5)) return sliceRelavitePath;  //未获得同一个视频文件的竞态ffmpeg进程运行权
        if(!fileOccupyCallback.addOccupyFile(videoFileMd5, videoFilePath)) throw new FileNotExistException();
        Files.createDirectory(Paths.get(VIDEO_SLICE_ROOT_DIRECTORY.getPath(), folderName));
        //开启后台线程进行视频切片
        Future<FfmpegRunResult> task = videoSliceThreadPool.submit(()->{
            System.out.println("执行视频切片任务.");
            ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
            WriteLock writeLock = readWriteLock.writeLock();
            if(!writeLock.tryLock()) throw new FfmpegTryLockFailException("获取锁失败，同一个视频运行ffmpeg不唯一！absoluteInputPath：" + videoFilePath);
            try {
                videoFileReadLock.put(videoFileMd5, readWriteLock.readLock());
                //阻塞等待
                FfmpegRunResult result = FfmpegUtil.runFfmpeg(videoFilePath, VIDEO_SLICE_ROOT_DIRECTORY.getPath() +java.io.File.separator+sliceRelavitePath, videoFileMd5);
                return result;
            } finally {
                writeLock.unlock();
            }
        });
        this.videoSliceTasks.put(folderName, task);
        return sliceRelavitePath;
    }
    
    @Override
    public String getVideoSliceResult(final String fileMd5) throws Exception {
        Future<FfmpegRunResult> videoSliceTask = this.videoSliceTasks.get(fileMd5);
        this.videoSliceTasks.remove(fileMd5);
        if(videoSliceTask == null) { //视频切片任务未创建. 说明视频文件夹已存在; 或由其它线程提交切片任务.
            java.io.File videoFolder = new java.io.File(VIDEO_SLICE_ROOT_DIRECTORY, fileMd5);
            String[] m3u8Files = videoFolder.list((dir, fileName)->{ return fileName.endsWith(".m3u8"); });
            if(m3u8Files!=null && m3u8Files.length>0) { //视频切片任务已完成
                System.out.println("找到已存在视频切片.");
                return CommonUtil.toJson(true, "");
            }
            else {
                System.out.println("等待其它线程后台视频切片任务完成...");
                //Thread.sleep(20000);   //等待20S. 优化:设置回调唤醒而不是固定等待
                //if(!videoFileReadLock.contains(fileMd5)) Thread.sleep(10);;   --应该不用自旋, 前面文件查找挺耗时的//自旋等待一会
                ReadLock readLock = videoFileReadLock.get(fileMd5);
                if(readLock == null) return CommonUtil.toJson(false, VIDEO_FILE_NOT_EXIST);  //猜测偶发现象--可能是提交任务的线程被线程切换导致还未调用putput()放入readWrite.
                readLock.tryLock(FfmpegUtil.USER_MAX_WAIT_TIME, TimeUnit.SECONDS);  //阻塞等待; 获取该锁单纯只是为了起到休眠唤醒的效果
                try { 
                    System.out.println("结束等待");
                    m3u8Files = videoFolder.list((dir, fileName)->{ return fileName.endsWith(".m3u8"); });
                    if(m3u8Files!=null && m3u8Files.length>0) return CommonUtil.toJson(true, "");
                    else return CommonUtil.toJson(false, VIDEO_ANALYSIS_TIMEOUT);
                }
                finally {
                    readLock.unlock();
                }
            }
            /*finally {
                fileOccupyCallback.removeOccupyFile(fileMd5); --此处移除不可靠, 用户有可能无法发起第二次请求.
            }*/
        }
        else { //videoSliceTask已创建
            System.out.println("等待后台视频切片任务完成...");
            FfmpegRunResult resuslt = videoSliceTask.get();  //阻塞
            System.out.println("结束等待");
            StateCode stateCode = StateCode.map(resuslt.getStateCode());
            if(stateCode == StateCode.SUCCESS) return CommonUtil.toJson(true, "");
            else if(stateCode == StateCode.TIME_OUT) return CommonUtil.toJson(false, VIDEO_ANALYSIS_TIMEOUT); //提醒用户解析时间过长
            else { //FAIL
                System.err.println("ffmpg运行错误信息: \n" + resuslt.getRunInfo());
                return CommonUtil.toJson(false, VIDEO_ANALYSIS_FAIL); 
            }
        }
    }
    
    /**不存在同useId文件则返回null*/
    /*private priv.xm.xkcloud.model.File existSameUseIdFile(List<priv.xm.xkcloud.model.File> sameNameFiles, int userId) {
        for (priv.xm.xkcloud.model.File sameNameFile : sameNameFiles) {
            if(sameNameFile.getUser_id() == userId)  return sameNameFile;
        }
        return null;
    }*/
    
    /**校验非userId用户是否存在此同名文件*/
    /*private boolean otherUserExistSameNameFile(List<priv.xm.xkcloud.model.File> sameNameFiles, int excludedUserId) {
        for (priv.xm.xkcloud.model.File sameNameFile : sameNameFiles) {
            if(sameNameFile.getUser_id() != excludedUserId) return true;
        }
        return false;
    }*/
    
    /**只调用一次此方法.*/
    public void startupScheduledRecycle() { 
         /**
          * 视频切片回收策略:
          * 对于半小时没有播放的视频切片进行删除.
          * 如果期间被播放则刷新计时. 
          */
        scheduledRecycleThreadPool.scheduleAtFixedRate(()->{
            //System.out.printf("回收线程%s开始运行.%n", Thread.currentThread().getName());
            try {  //做好异常处理, 防止周期回收任务被中断
                //Thread.sleep(2000);  //统计2S时间--不够, 正常播放情况下ts预传输间隔为一个切片长度.
                isMonitorringVideoFile = -1;  //进入文件夹锁住状态
                String videoFolderPath = VIDEO_SLICE_ROOT_DIRECTORY.getPath();
                String[] folderList = new java.io.File(videoFolderPath).list();
                System.out.printf("执行视频切片清理回收线程.监控记录:%n%s%n", currentVisitedVideoSliceFolders);
                for (int i=0; i<folderList.length; ++i) {
                    String folderName = folderList[i];
                    if(currentVisitedVideoSliceFolders.contains(folderName) || videoRunFfmpegQueue.contains(folderName)
                       /**后一个判断针对于"接近回收时间,但ffmpeg刚开始运行,未完成切片没有访问记录"的临界条件*/) continue;
                    java.io.File sliceFolder = new java.io.File(videoFolderPath, folderName);
                    java.io.File[] slices = sliceFolder.listFiles();
                    for (java.io.File slice : slices) {
                        if(!slice.delete()) System.err.println("视频切片删除失败:" + slice.getPath());
                    }
                    if(sliceFolder.delete()) System.out.printf("已清理%s视频切片文件夹.%n", sliceFolder.getName());
                    else System.err.println("视频切片文件夹删除失败:" + sliceFolder.getPath());
                }
            } finally {
                isMonitorringVideoFile = 1;   //恢复流量监控状态
                //isMonitorringVideoFile = 0;  //不关闭, 没有监控间隙.
                currentVisitedVideoSliceFolders.clear();  //重置刷新记录
            }
            
            //System.out.printf("回收线程%s结束运行.%n", Thread.currentThread().getName());
        }, 35, 30, TimeUnit.SECONDS);  //用于演示; 正式环境下时间修改为30分钟
        
        /*
        * 上传文件回收策略:
        * 一天之内如果用户没有再进行续传, 
        * 则删除未完成的文件切片.
        */
        scheduledRecycleThreadPool.scheduleAtFixedRate(()->{
            //System.out.printf("回收线程%s开始运行.%n", Thread.currentThread().getName());
            try {
                //已修复--文件合并时监测不到问题！造成合并期间切片将可能回收删除！--解决:合并时也加入currentVistiedUploadSliceFolders;
                isMonitorringUploadFile = 1;   
                //极端条件下会出现问题: 恰好到回收临界时间、用户网速极差,一个切片需要上传很久(超过统计时间)--基本不可能发生.
                Thread.sleep(30000);  //统计30S时间; 尽量大于一个切片的上传时间(尤其时用户网速很差时),避免监控疏漏.
                isMonitorringUploadFile = -1;  
                
                System.out.printf("执行上传切片清理回收线程.监控记录:%n%s%n", currentVisitedUploadSliceFolders);
                java.io.File[] uploadFileFolderList = new java.io.File(UPLOAD_FILE_SLICE_ROOT_PATH).listFiles();
                for(int i=0; i<uploadFileFolderList.length; ++i) {
                    java.io.File uploadFileFolder = uploadFileFolderList[i];
                    FileTime creationTime = Files.readAttributes(uploadFileFolder.toPath(), BasicFileAttributes.class).creationTime();
                    //用于演示
                    /*long saveSecond = System.currentTimeMillis()/1000 - creationTime.to(TimeUnit.SECONDS);
                    if(saveSecond > 60) {*/
                    //正式环境
                    long saveDays = System.currentTimeMillis()/1000/(24*3600) - creationTime.to(TimeUnit.DAYS);
                    if(saveDays >= 1) {  //保存超过一天                     
                        try {
                            if(currentVisitedUploadSliceFolders.contains(uploadFileFolder.getName())) continue;
                            java.io.File[] slices = uploadFileFolder.listFiles();
                            for (java.io.File file : slices) {
                                if(!file.delete()) System.err.println("上传切片删除失败:" + file.getPath());
                            }
                            if(uploadFileFolder.delete()) System.out.printf("已清理%s上传切片文件夹.%n", uploadFileFolder.getName());
                            else System.err.println("上传切片文件夹删除失败:" + uploadFileFolder.getPath());
                        } finally {
                            savedFileSlices.remove(uploadFileFolder.getName());  //清空已保留上传切片序号列表,避免虚假的续传
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isMonitorringUploadFile = 0;
                currentVisitedUploadSliceFolders.clear();
            }
            //System.out.printf("回收线程%s结束运行.%n", Thread.currentThread().getName());
        }, 15, 10, TimeUnit.SECONDS); //用于演示; 正式环境下时间修改为1天
    }
    
}
