package priv.xm.xkcloud.message;

import priv.xm.xkcloud.model.User;

public class Message {
    //服务器相关
    public final static String SERVER_ERROR = "服务器繁忙，请稍后重试...";
    
    //注册相关
    public final static String USER_NAME_IS_EMPTY = "用户名不能为空!";
    public final static String PASSWORD_IS_EMPTY = "密码不能为空!";
    public final static String USER_NAME_ALREADY_EXIST = "该名字已经被使用！";
    public final static String PASSWORD_LENGTH_ILLEAGAL = "密码必须为6~16位！";
    public final static String REGISTER_SUCCESS = "注册成功！";
    
    //登录相关
    public final static String INPUT_IS_EMPTY = "用户名或密码为空！";
    public final static String LOGIN_SUCCESS = "登录成功！";
    public final static String LOGIN_FAIL = "用户名或密码错误！";
    public final static String LOGIN_STATE_INVALID = "loginInvalid";
    
    //文件操作相关
    public final static String SET_SUCCESS = "设置成功！";
    public final static String SET_FILE_NOT_EXIST = "设置失败！文件不存在，请尝试刷新页面";
    public final static String SET_USER_NO_PERMISSION = "设置失败，此文件不属于你！";
    public final static String DELETE_FILE_SUCCESS = "文件删除成功！";
    public final static String DELETE_FILE_NOT_EXIST = "删除失败，文件不存在！";
    public final static String DELETE_USER_NO_PERMISSION = "删除失败，此文件不属于你！";
    public final static String DOWNLOAD_SUCCESS = "文件下载完成！";
    public final static String DOWNLOAD_FILE_NOT_EXIST = "下载失败，文件不存在！";
    public final static String DOWNLOAD_FILE_DEU_MERGER = "下载失败！服务器正在处理刚上传的文件";
    public final static String OPEN_FILE_NOT_EXIST = "打开失败，文件不存在！";
    
    //文件上传相关
    public final static String UPLOAD_FAST_SUPPORT = "支持极速上传.";
    public final static String UPLOAD_FAST_NOT_SUPPORT = "此文件暂不支持极速上传！";
    public final static String UPLOAD_FAST_SUCCESS = "极速上传成功！";
    public final static String UPLOAD_FAST_FAIL = "不支持极速上传！";
    public final static String UPLOAD_SUCCESS = "上传成功！";
    public final static String UPLOAD_WITHOUT_SELECT_FILE = "未选择任何文件！";
    public final static String UPLOAD_FILE_SIZE_ZERO = "文件大小不能为0！";
    public final static String UPLOAD_FILE_SIZE_EXCEED_FOR_COMMON = "普通用户上传单个文件最大为" + User.COMMON_SINGLE_FILE_LIMIT + "MB.";
    public final static String UPLOAD_FILE_SIZE_EXCEED_FOR_VIP = "VIP用户上传单个文件最大为" + User.VIP_SINGLE_FILE_LIMIT + "MB.";
    public final static String UPLOAD_FAIL_DUE_TO_NO_SPACE = "剩余空间不足，无法上传！";
    public final static String UPLOAD_FILE_DAMAGE = "文件传输过程中损坏，请重新上传！";
    public final static String UPLOAD_IS_NOT_FILE_SLICE = "切片命名格式有误！";
    public final static String UPLOAD_FINISH = "ok";
    public final static String UPLOAD_REJECT = "reject";

    //视频解析相关
    public final static String VIDEO_FILE_NOT_EXIST = "播放失败，视频资源不存在！";
    public final static String VIDEO_ANALYSIS_TIMEOUT = "视频解析超时，请稍后重试！";
    public final static String VIDEO_ANALYSIS_FAIL = "在线解析失败！请下载至本地进行播放";
    public final static String VIDEO_SLICE_RECYCLE = "视频资源失效！请尝试退回主页重新进入";
    
    
}
