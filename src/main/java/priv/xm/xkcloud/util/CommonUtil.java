package priv.xm.xkcloud.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

/**根据文件后缀名转换为响应的HTTP文件类型*/
public class CommonUtil {

    public static String setContentType(String fileName) {
        String contentType = "application/octet-stream";
        if ( fileName.lastIndexOf(".") < 0)
            return contentType;
        fileName = fileName .toLowerCase();
        fileName = fileName.substring(fileName .lastIndexOf("." ) + 1);

        if ( fileName.equals("html") || fileName.equals("htm") || fileName .equals("shtml" )) {
            contentType = "text/html";
        } else if ( fileName.equals("apk")) {
            contentType = "application/vnd.android.package-archive" ;
        } else if ( fileName.equals("sis")) {
            contentType = "application/vnd.symbian.install";
        } else if ( fileName.equals("sisx")) {
            contentType = "application/vnd.symbian.install";
        } else if ( fileName.equals("exe")) {
            contentType = "application/x-msdownload";
        } else if ( fileName.equals("msi")) {
            contentType = "application/x-msdownload";
        } else if ( fileName.equals("css")) {
            contentType = "text/css";
        } else if ( fileName.equals("xml")) {
            contentType = "text/xml";
        } else if ( fileName.equals("gif")) {
            contentType = "image/gif";
        } else if ( fileName.equals("jpeg") || fileName.equals("jpg")) {
            contentType = "image/jpeg";
        } else if ( fileName.equals("js")) {
            contentType = "application/x-javascript";
        } else if ( fileName.equals("atom")) {
            contentType = "application/atom+xml";
        } else if ( fileName.equals("rss")) {
            contentType = "application/rss+xml";
        } else if ( fileName.equals("mml")) {
            contentType = "text/mathml";
        } else if ( fileName.equals("txt")) {
            contentType = "text/plain";
        } else if ( fileName.equals("jad")) {
            contentType = "text/vnd.sun.j2me.app-descriptor" ;
        } else if ( fileName.equals("wml")) {
            contentType = "text/vnd.wap.wml";
        } else if ( fileName.equals("htc")) {
            contentType = "text/x-component";
        } else if ( fileName.equals("png")) {
            contentType = "image/png";
        } else if ( fileName.equals("tif") || fileName.equals("tiff")) {
            contentType = "image/tiff";
        } else if ( fileName.equals("wbmp")) {
            contentType = "image/vnd.wap.wbmp";
        } else if ( fileName.equals("ico")) {
            contentType = "image/x-icon";
        } else if ( fileName.equals("jng")) {
            contentType = "image/x-jng";
        } else if ( fileName.equals("bmp")) {
            contentType = "image/x-ms-bmp";
        } else if ( fileName.equals("svg")) {
            contentType = "image/svg+xml";
        } else if ( fileName.equals("jar") || fileName.equals("var") || fileName .equals("ear" )) {
            contentType = "application/java-archive";
        } else if ( fileName.equals("doc")) {
            contentType = "application/msword";
        } else if ( fileName.equals("pdf")) {
            contentType = "application/pdf";
        } else if ( fileName.equals("rtf")) {
            contentType = "application/rtf";
        } else if ( fileName.equals("xls")) {
            contentType = "application/vnd.ms-excel";
        } else if ( fileName.equals("ppt")) {
            contentType = "application/vnd.ms-powerpoint";
        } else if ( fileName.equals("7z")) {
            contentType = "application/x-7z-compressed";
        } else if ( fileName.equals("rar")) {
            contentType = "application/x-rar-compressed";
        } else if ( fileName.equals("swf")) {
            contentType = "application/x-shockwave-flash";
        } else if ( fileName.equals("rpm")) {
            contentType = "application/x-redhat-package-manager" ;
        } else if ( fileName.equals("der") || fileName.equals("pem") || fileName .equals("crt" )) {
            contentType = "application/x-x509-ca-cert";
        } else if ( fileName.equals("xhtml")) {
            contentType = "application/xhtml+xml";
        } else if ( fileName.equals("zip")) {
            contentType = "application/zip";
        } else if ( fileName.equals("mid") || fileName.equals("midi") || fileName .equals("kar" )) {
            contentType = "audio/midi";
        } else if ( fileName.equals("mp3")) {
            contentType = "audio/mpeg";
        } else if ( fileName.equals("ogg")) {
            contentType = "audio/ogg";
        } else if ( fileName.equals("m4a")) {
            contentType = "audio/x-m4a";
        } else if ( fileName.equals("ra")) {
            contentType = "audio/x-realaudio";
        } else if ( fileName.equals("3gpp") || fileName.equals("3gp")) {
            contentType = "video/3gpp";
        } else if ( fileName.equals("mp4")) {
            contentType = "video/mp4";
        } else if ( fileName.equals("mpeg") || fileName.equals("mpg")) {
            contentType = "video/mpeg";
        } else if ( fileName.equals("mov")) {
            contentType = "video/quicktime";
        } else if ( fileName.equals("flv")) {
            contentType = "video/x-flv";
        } else if ( fileName.equals("m4v")) {
            contentType = "video/x-m4v";
        } else if ( fileName.equals("mng")) {
            contentType = "video/x-mng";
        } else if ( fileName.equals("asx") || fileName.equals("asf")) {
            contentType = "video/x-ms-asf";
        } else if ( fileName.equals("wmv")) {
            contentType = "video/x-ms-wmv";
        } else if ( fileName.equals("avi")) {
            contentType = "video/x-msvideo";
        }

        return contentType;
    }
    
    private static AtomicLong version = new AtomicLong(1L);
    /**主要用于避免并发条件下多个用户同时创建写入相同文件名*/
    public static long getUniqueVersion() {
        return version.getAndIncrement();
    }
    
    //用于调试
    public static void printHttpServletRequestInfo(HttpServletRequest req) {
        System.out.println("HttpServletRequest信息(部分)：");
        System.out.println("getHeader(Range):" + req.getHeader("Range"));
        System.out.println("AuthType:" + req.getAuthType());
        System.out.println("CharacterEncoding:" + req.getCharacterEncoding());
        System.out.println("ContentLength:" + req.getContentLength());
        System.out.println("ContentLengthLong:" + req.getContentLengthLong());
        System.out.println("ContentType:" + req.getContentType());
        System.out.println("ContextPath:" + req.getContextPath());
        System.out.println("LocalAddr:" + req.getLocalAddr());
        System.out.println("LocalName:" + req.getLocalName());
        System.out.println("LocalPort:" + req.getLocalPort());
        System.out.println("Method:" + req.getMethod());
        System.out.println("PathInfo:" + req.getPathInfo());
        System.out.println("PathTranslated:" + req.getPathTranslated());
        System.out.println("Protocol:" + req.getProtocol());
        System.out.println("QueryString:" + req.getQueryString());
        System.out.println("RemoteAddr:" + req.getRemoteAddr());
        System.out.println("RemoteHost:" + req.getRemoteHost());
        System.out.println("RemoteUser:" + req.getRemoteUser());
        System.out.println("RemotePort:" + req.getRemotePort());
        System.out.println("RequestedSessionId:" + req.getRequestedSessionId());
        System.out.println("RequestURI:" + req.getRequestURI());
        System.out.println("Scheme:" + req.getScheme());
        System.out.println("ServerName:" + req.getServerName());
        System.out.println("ServerPort:" + req.getServerPort());
        System.out.println("ServletPath:" + req.getServletPath());
        System.out.println("Cookies:" + req.getCookies());
        System.out.println("DispatcherType:" + req.getDispatcherType());
        System.out.println("Locale:" + req.getLocale());
        System.out.println("RequestURL:" + req.getRequestURL());
        Enumeration<String> parameterNames = req.getParameterNames();
        while(parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            System.out.print(String.format("ParameterName=%s ParameterValues=[", parameterName));
            for (String value : req.getParameterValues(parameterName)) {
                System.out.print(value + "  ");
            }
            System.out.println("]");
        }
        System.out.println();
    }
    
    /**
     * @return 两位小数MB的文件大小.
     */
    public static float ceilFileSize(long fileByteSize) {
        return new BigDecimal(fileByteSize/1048576.0)
                .setScale(2, RoundingMode.CEILING).floatValue();
    }
    
    public static String toJson(boolean result, String msg) {
        ///使用String.valueOf避免自动装箱操作，提高并发下的执行效率
        return String.format("{\"result\": %s, \"msg\": \"%s\"}", String.valueOf(result), msg);
    }
    
    public static String toJson(boolean result, Set<Integer> sliceList) {
        return String.format("{\"result\": %s, \"msg\": %s}", String.valueOf(result), 
            sliceList==null ? "[]" : sliceList.toString());
    }
    
    public interface Execute<T>{
        T execute() throws Exception;
    }
    
    public static <T> T timing(Execute<T> process) throws Exception{
        long startTime = System.currentTimeMillis();
        T result = process.execute();
        long endTime = System.currentTimeMillis();
        System.out.printf("共耗时:%.2fS%n", (endTime - startTime)/1000.0);
        return result;
    }
    
}