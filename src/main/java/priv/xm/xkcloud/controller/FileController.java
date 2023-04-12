package priv.xm.xkcloud.controller;

import static priv.xm.xkcloud.message.Message.DELETE_FILE_NOT_EXIST;
import static priv.xm.xkcloud.message.Message.DELETE_FILE_SUCCESS;
import static priv.xm.xkcloud.message.Message.DELETE_USER_NO_PERMISSION;
import static priv.xm.xkcloud.message.Message.DOWNLOAD_FILE_NOT_EXIST;
import static priv.xm.xkcloud.message.Message.LOGIN_STATE_INVALID;
import static priv.xm.xkcloud.message.Message.OPEN_FILE_NOT_EXIST;
import static priv.xm.xkcloud.message.Message.SERVER_ERROR;
import static priv.xm.xkcloud.message.Message.SET_FILE_NOT_EXIST;
import static priv.xm.xkcloud.message.Message.SET_SUCCESS;
import static priv.xm.xkcloud.message.Message.SET_USER_NO_PERMISSION;
import static priv.xm.xkcloud.message.Message.UPLOAD_FAIL_DUE_TO_NO_SPACE;
import static priv.xm.xkcloud.message.Message.UPLOAD_FAST_NOT_SUPPORT;
import static priv.xm.xkcloud.message.Message.UPLOAD_FAST_SUCCESS;
import static priv.xm.xkcloud.message.Message.UPLOAD_FILE_DAMAGE;
import static priv.xm.xkcloud.message.Message.UPLOAD_FILE_SIZE_EXCEED_FOR_COMMON;
import static priv.xm.xkcloud.message.Message.UPLOAD_FILE_SIZE_EXCEED_FOR_VIP;
import static priv.xm.xkcloud.message.Message.UPLOAD_FILE_SIZE_ZERO;
import static priv.xm.xkcloud.message.Message.UPLOAD_FINISH;
import static priv.xm.xkcloud.message.Message.UPLOAD_IS_NOT_FILE_SLICE;
import static priv.xm.xkcloud.message.Message.UPLOAD_REJECT;
import static priv.xm.xkcloud.message.Message.UPLOAD_SUCCESS;
import static priv.xm.xkcloud.message.Message.UPLOAD_WITHOUT_SELECT_FILE;
import static priv.xm.xkcloud.message.Message.VIDEO_FILE_NOT_EXIST;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.bind.DatatypeConverter;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import priv.xm.xkcloud.exception.UploadSliceRecyleException;
import priv.xm.xkcloud.exception.FileNotExistException;
import priv.xm.xkcloud.exception.VideoSliceRecyleException;
import priv.xm.xkcloud.model.File;
import priv.xm.xkcloud.model.Page;
import priv.xm.xkcloud.model.PageBean;
import priv.xm.xkcloud.model.User;
import priv.xm.xkcloud.service.FileService;
import priv.xm.xkcloud.service.UserService;
import priv.xm.xkcloud.util.CommonUtil;

@Controller
@RequestMapping("/files")
public class FileController{
    @Autowired
    private FileService fileService;
    @Autowired
    private UserService userService;
    
    @Autowired
    private ConcurrentHashMap<String, Set<Integer>>  savedFileSlices;  //String:fileName+userId
    
    /**
     * 文件分片上传.
     * 注意:极端条件下--同一用户多处登录上传同一文件会可能造成文件损坏!*/
    @PostMapping
    @ResponseBody
    public String upload(@RequestParam("fileBlob") CommonsMultipartFile blobFile, @RequestParam("blobMD5")String blobMd5, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        try { 
            //前端数据校验
            if(userService.verifyFailUserLogState(session)) return CommonUtil.toJson(false, LOGIN_STATE_INVALID);
            if(verifyFailMd5(blobFile, blobMd5)) return CommonUtil.toJson(false, UPLOAD_FILE_DAMAGE);
            String sessionUserName = (String) session.getAttribute("user_name");
            String fileName = blobFile.getOriginalFilename();
            //System.out.println("正在上传文件切片:" + fileName);
            float fileSize = CommonUtil.ceilFileSize(blobFile.getSize());  //MB--保留两位小数; 便于前端展示
            if(verifyFailFile(fileName, fileSize, sessionUserName, request)) return CommonUtil.toJson(false, (String)request.getAttribute("message")); 
            //检查是否为文件切片命名格式
            int separatorPos = fileName.indexOf('-');
            if(separatorPos == -1 || fileName.indexOf('-', separatorPos+1)==-1) return CommonUtil.toJson(false, UPLOAD_IS_NOT_FILE_SLICE);
    
            //文件上传
            int userId = userService.findUserID(sessionUserName);
            if(fileService.upload(blobFile, fileName, userId, session)) return CommonUtil.toJson(true, UPLOAD_FINISH);
        }
        catch (UploadSliceRecyleException e) {
            return CommonUtil.toJson(false, UPLOAD_REJECT);
        }
        catch (Exception e) {
            return dealException(e);
        }

        return CommonUtil.toJson(true, UPLOAD_SUCCESS);
    }


    //极速上传原理: 仅拷贝文件磁盘路径到数据库, 无需经过网络传输
    @PostMapping("/fastUpload")
    @ResponseBody
    public String fastUpload(@RequestParam("MD5")String md5,  HttpServletRequest request, HttpSession session, 
            @RequestParam("fileName")String fileName, @RequestParam("fileSize") Long fileSize) {
        try {
            //前端数据校验
            if(md5==null || fileSize==null) CommonUtil.toJson(false, UPLOAD_FAST_NOT_SUPPORT);
            String sessionUserName = (String) session.getAttribute("user_name");
            if(userService.verifyFailUserLogState(session)) return CommonUtil.toJson(false, LOGIN_STATE_INVALID);
            float filSizeMB = CommonUtil.ceilFileSize(fileSize);
            if (verifyFailFile(fileName, filSizeMB, sessionUserName, request)) return CommonUtil.toJson(false, (String)request.getAttribute("message")); 
    
            int userId = userService.findUserID(sessionUserName);
            if(fileService.fastUpload(md5, userId, fileName, filSizeMB, session)) {
                System.out.println("极速上传成功!");
                return CommonUtil.toJson(true, UPLOAD_FAST_SUCCESS);
            }
            else {
                System.out.println("不支持极速上传!");
                Set<Integer> sliceList = savedFileSlices.get(fileName+"-"+Integer.toString(userId));
                return CommonUtil.toJson(false, sliceList);
            }
        }catch (Exception e) {
            return dealException(e);
        }
    }


    @RequestMapping("/search")
    public String searchFile(Page page, Model mv) {
        try {
            String searchContent = page.getSearchcontent();
            if(searchContent==null || (searchContent=searchContent.trim()).length() == 0) return "redirect:/home";
            PageBean pageBean  = fileService.searchFile(page);
            mv.addAttribute("pagebean", pageBean);
            mv.addAttribute("searchcontent", searchContent);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/home";
        }
        return "file/searchResult";
    }


    @GetMapping("/download")
    public String download(Integer id, String filename, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        if(id==null || id < 0) return CommonUtil.toJson(false, DOWNLOAD_FILE_NOT_EXIST);  //防止缓存恶意击穿
        priv.xm.xkcloud.model.File dwFile = null;
        try {
            //前端数据校验
            dwFile = fileService.findFileById(id);
            if(verifyFailExistenceOfFile(dwFile)) return "redirect:/resourcesNotFound.html";
            if(dwFile.getCanshare() == 0) { //文件处于私有状态
                if(userService.verifyFailUserLogState(session)) return "redirect:/users/login";
                if(verifyFailUserPermission(session, dwFile)) return "redirect:/users/help";
            }
            String dwFilePath = dwFile.getFilepath();  //绝对路径
            if (Files.notExists(Paths.get(dwFilePath))) {  //文件资源不存在
                //response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return "redirect:/resourcesNotFound.html";
            }
            
            //下载文件
            try {
                if(!fileService.downloadFile(dwFile, request, response)) return "redirect:/users/help";;   //加入占用列表, 避免下载过程被删除.
            } 
            catch (SecurityException e) {
                //response.setStatus(HttpServletResponse.SC_NOT_FOUND); || return CommonUtil.toJson(false, DOWNLOAD_FILE_DEU_MERGER);
                System.err.print("可能是由于刚上传的文件后台正在处理, 不能同时读写:");
                e.printStackTrace();
                return "redirect:/users/help";
            }
            catch(IOException ioe) {
                /**在写数据的时候， 对于 ClientAbortException 之类的异常， 是因为客户端取消了下载，
                而服务器端继续向浏览器写入数据时，则会抛出这个异常，这是正常的.*/
                System.out.println("服务器提前传输了数据,浏览器还未准备好!--浏览器暂停/取消了下载" );
            }
        }
        catch (Exception e) {
            dealException(e);
        }
        return null;
    }

    /**@param id --文件主键*/
    @RequestMapping("/share")
    @ResponseBody
    public String shareFile(Integer id, Integer shareState, HttpSession session) {
        if(id==null || id < 0 || shareState==null) return CommonUtil.toJson(false, SET_FILE_NOT_EXIST);  //防止缓存恶意击穿
        priv.xm.xkcloud.model.File shareFile;
        try {
            shareFile = fileService.findFileById(id);
            if(userService.verifyFailUserLogState(session)) return CommonUtil.toJson(false, LOGIN_STATE_INVALID);
            if(verifyFailExistenceOfFile(shareFile)) return CommonUtil.toJson(false, SET_FILE_NOT_EXIST);
            if(verifyFailUserPermission(session, shareFile)) return CommonUtil.toJson(false, SET_USER_NO_PERMISSION);
        } catch (Exception e) {
            return dealException(e);
        }
        
        fileService.shareFile(shareFile, shareState);
        return CommonUtil.toJson(true, SET_SUCCESS);
    }

    /**@Param id 文件主键*/
    @DeleteMapping("/{id}")
    @ResponseBody
    public String deleteFile(@PathVariable Integer id, HttpSession session) {
        if(id==null || id < 0) return CommonUtil.toJson(false, DELETE_FILE_NOT_EXIST);  //防止缓存恶意击穿
        //throw new Exception("测试");
        try {
            if(userService.verifyFailUserLogState(session)) return CommonUtil.toJson(false, LOGIN_STATE_INVALID);
            priv.xm.xkcloud.model.File delFile = fileService.findFileById(id);
            if(verifyFailExistenceOfFile(delFile))  return CommonUtil.toJson(false, DELETE_FILE_NOT_EXIST);
            if (verifyFailUserPermission(session, delFile)) return CommonUtil.toJson(false, DELETE_USER_NO_PERMISSION);  //不通过;可能是人为篡改url请求数据
            
            fileService.deleteFile(delFile);
        } catch (Exception e) {
            return dealException(e);
        }
        return CommonUtil.toJson(true, DELETE_FILE_SUCCESS);
    }
    
    /**打开文本、图片、音频等小文件*/
    @GetMapping("/open/{id}")
    public String openFile(@PathVariable("id")Integer id, HttpServletRequest request, HttpServletResponse response) {
        if(id==null || id<0) return CommonUtil.toJson(false, OPEN_FILE_NOT_EXIST);
        try{
            File file = fileService.findFileById(id);
            fileService.openFile(file, request, response);
        } 
        catch (IOException e) {
            System.err.println("用户中断了音频播放.");
        }
        catch (Exception e) {
            dealException(e);
        }
        return null;
    }
    
    @GetMapping("onlinePlay/{id}") //resultful风格
    public String onlinePlay(@PathVariable("id")Integer fileId, HttpServletRequest request, HttpServletResponse response, Model model, HttpSession session){  
        if (fileId==null || fileId < 0) return null /*CommonUtil.toJson(false, DELETE_FILE_NOT_EXIST)*/;  //防止缓存恶意击穿
        priv.xm.xkcloud.model.File videoFile = null;
        try {
            if (userService.verifyFailUserLogState(session)) return "redirect:/users/login"; //CommonUtil.toJson(false, LOGIN_STATE_INVALID);
            videoFile = fileService.findFileById(fileId);
            if (verifyFailExistenceOfFile(videoFile)) return "redirect:/resourcesNotFound.html";
            
            String m3u8Path = fileService.sliceVideo(videoFile); //开启后台线程进行视频切片
            //request.setAttribute("m3u8_address", m3u8Path);
            model.addAttribute("m3u8", m3u8Path);
            model.addAttribute("videoSize", videoFile.getFilesize());  //MB
        } 
        catch (FileNotExistException e) {
            //return CommonUtil.toJson(false, VIDEO_FILE_NOT_EXIST);
            return "redirect:/resourcesNotFound.html";
        }
        catch (Exception e) { 
            dealException(e);
        } /*finally {  
            fileOccupyCallback.removeOccupyFile(videoFile);  应该在fileService.getVideoSliceResult()中移除
          }*/
        return "file/video/playVideo";
    }
        
    @RequestMapping("getVideoSliceResult")
    @ResponseBody
    public String getVideoSliceResult(String fileMd5) {
        if(fileMd5==null) return CommonUtil.toJson(false, VIDEO_FILE_NOT_EXIST);
        try {
            return fileService.getVideoSliceResult(fileMd5);  //会阻塞等待一定时间
        } catch (Exception e) {
            return dealException(e);
        }
    }

    /**
     * 无状态->因为需要播放的流畅性, 请求参数只有切片信息, 没有加入任何状态验证信息.
     * @param videoFragment 仅接受传入m3u8/ts文件名
     * @return 禁止向Response.body写入其他信息造成视频无法播放
     */
    @RequestMapping("video/{folderName}/{videoFragment}")  //加video路径防止捕获到其它请求
    public void transferFragment(@PathVariable("folderName")String folderName, @PathVariable("videoFragment")String videoFragment, @Param("response")HttpServletResponse response){
        //System.out.println("流式传输: " + videoFragment);
        try {
            fileService.transferFragment(folderName, videoFragment, response);
        }
        catch (VideoSliceRecyleException e) {
            /**优化点: 
             * 请求成功的视频可能被回收线程删除导致刚进入页面播放失败(正在播放的页面不会存在此问题), 
             * 需要用户重新发起播放请求. */
            System.err.println("文件夹被回收线程锁住,切片传输中断"); 
            //如何向前端传递文件夹被锁住中断传输的消息? 
            //return CommonUtil.toJson(false, VIDEO_SLICE_RECYCLE);
        }
        catch (IOException e) {
            System.err.print("用户中断了视频播放.");
            System.err.println(e);
            //e.printStackTrace();
        }
    }

    /**@param fileSize MB*/
    private boolean verifyFailFile(String fileName, double fileSize, String userName, HttpServletRequest request) throws Exception {
        if(fileName == null) {
            request.setAttribute("message", UPLOAD_WITHOUT_SELECT_FILE);
            return true;
        }
        
        if(fileSize == 0) {
            request.setAttribute("message", UPLOAD_FILE_SIZE_ZERO);
            return true;
        } 
        
        if (userService.isVIP(userName)) {
            //检查单个文件上传限制
            if(fileSize > User.VIP_SINGLE_FILE_LIMIT) {
                request.setAttribute("message", UPLOAD_FILE_SIZE_EXCEED_FOR_VIP);
                return true;
            }
            //检查用户的网盘空间是否超过限额
            if(userService.findUserUsedSpace(userName)+fileSize > User.VIP_USER_SPACE_SIZE) {
                request.setAttribute("message", UPLOAD_FAIL_DUE_TO_NO_SPACE);
                return true;
            }
        } 
        else {
            //检查单个文件上传限制
            if (fileSize > User.COMMON_SINGLE_FILE_LIMIT) {
                request.setAttribute("message", UPLOAD_FILE_SIZE_EXCEED_FOR_COMMON);
                return true;
            }
            //检查用户的网盘空间是否超过限额
            if(userService.findUserUsedSpace(userName)+fileSize > User.COMMON_USER_SPACE_SIZE) {
                request.setAttribute("message", UPLOAD_FAIL_DUE_TO_NO_SPACE);
                return true;
            }
        }
        
        return false;
    }
    
    private boolean verifyFailUserPermission(HttpSession session, priv.xm.xkcloud.model.File operatedFile) throws Exception {
        String sessionUserName = (String)session.getAttribute("user_name");
        String fileUserName = userService.findUserNameById(operatedFile.getUser_id());
        return !Objects.equals(sessionUserName, fileUserName);
    }
    
    private boolean verifyFailExistenceOfFile(priv.xm.xkcloud.model.File file) {
        return file==null ? true : false;
    }
    
    private boolean verifyFailMd5(MultipartFile file, String md5) {
        try {
            MessageDigest messageDigest;
            messageDigest = MessageDigest.getInstance("MD5");
            byte[] digest = messageDigest.digest(file.getBytes());
            //String realMd5 = new BigInteger(1, digest).toString(16);  //前面0丢失！
            String realMd5 = DatatypeConverter.printHexBinary(digest).toLowerCase();
            if(!realMd5.equals(md5)) {
                System.out.printf("切片%s的MD5校验失败！前端:%s 后端:%s%n", file.getOriginalFilename(), md5, realMd5);
                return true;
            }
            return false;
            //return !realMd5.equals(md5);
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.print("MD5计算出错:");
            e.printStackTrace();
            return true;
        }
    }
    
    private String dealException(Exception e) {
        e.printStackTrace();
        return CommonUtil.toJson(false, SERVER_ERROR);
    }
    
}
