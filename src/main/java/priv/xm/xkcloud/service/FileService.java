package priv.xm.xkcloud.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.multipart.commons.CommonsMultipartFile;

import priv.xm.xkcloud.model.File;
import priv.xm.xkcloud.model.Page;
import priv.xm.xkcloud.model.PageBean;

public interface FileService {

    File findFileById(int id) throws Exception;

    List<File> findFile(int limitOffset, int entryNumber) throws Exception;

    int findFileStartId() throws Exception;

    int countUserFiles(String userName) throws Exception ;

    List<File> getUserFiles(Page page) throws Exception;

    void shareFile(File file, int shareState);

    PageBean searchFile(Page page) throws Exception;

    void deleteFile(File file) throws Exception;
    
    boolean fastUpload(String md5, int userId, String fileName, float fileSize, HttpSession session) throws Exception;

    boolean upload(CommonsMultipartFile file, String sliceName, int userId, HttpSession session) throws Exception;
    
    boolean containsMd5(String md5) throws Exception;

    boolean downloadFile(File file, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String sliceVideo(File videoFile) throws Exception;
    
    String getVideoSliceResult(String fileMd5) throws Exception;

    void transferFragment(String folderName, String videoFragment, HttpServletResponse response) throws IOException;

    void openFile(File file, HttpServletRequest request, HttpServletResponse response) throws Exception;

}