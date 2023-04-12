package priv.xm.xkcloud.model;

import java.sql.Date;
import java.util.Objects;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("file")
@Scope("prototype")
public class File implements Cloneable {
    public static final String SERVICE_ROOT_PATH = "D:\\MyWebDiskServer"; 
    public static final String UPLOAD_FILE_SLICE_ROOT_PATH = SERVICE_ROOT_PATH + "\\UploadFileSlices"; 
    public static final java.io.File VIDEO_SLICE_ROOT_DIRECTORY = new java.io.File(SERVICE_ROOT_PATH + java.io.File.separator + "VideoSlices"); 
    public static final String M3U8_FILE_NAME = "video.m3u8";
	
	private int id;  //文件ID
	private String filename; //文件名--注意不一定会和路径上文件名的相同!
	private String filepath; //磁盘上完整的绝对路径:服务器根路径/日期/文件名(+版本序号)
	private float filesize; /*MB--保留两位小数; 注意不是精确文件大小!*/
	private Date createtime;
	private int canshare;  //文件是否共享:0私有 1公有
	private int user_id;
	private String MD5;
	
	public File() { }
	
	public File(String filename, String filepath, float filesize, Date createtime, int canshare, int user_id,
            String mD5) {
        super();
        this.filename = filename;
        this.filepath = filepath;
        this.filesize = filesize;
        this.createtime = createtime;
        this.canshare = canshare;
        this.user_id = user_id;
        this.MD5 = mD5;
    }
	
    public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	/**@return: 磁盘上完整的绝对路径:服务器根路径/日期/文件名(+版本序号)*/
	public String getFilepath() {
		return filepath;
	}
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
	public float getFilesize() {
		return filesize;
	}
	public void setFilesize(float filesize) {
		this.filesize = filesize;
	}
	public Date getCreatetime() {
		return createtime;
	}
	public void setCreatetime(Date createtime) {
		this.createtime = createtime;
	}
	/**0私有 1公有*/
	public int getCanshare() {
		return canshare;
	}
	public void setCanshare(int canshare) {
		this.canshare = canshare;
	}


	public int getUser_id() {
		return user_id;
	}

	public void setUser_id(int user_id) {
		this.user_id = user_id;
	}

	public String getMD5() {
		return MD5;
	}

	public void setMD5(String MD5) {
		this.MD5 = MD5;
	}
	
	
	/**拷贝所有属性(除了id!)*/
	public void copyInfo(File file) {
	    this.MD5 = file.getMD5();
	    this.user_id = file.getUser_id();
	    this.canshare = file.getCanshare();
	    this.filename = file.getFilename();
	    this.filepath = file.getFilepath();
	    this.filesize = file.getFilesize();
	    this.createtime = file.getCreatetime();
	}
	
    @Override
    public int hashCode() { 
        return 31*canshare + 31*id + (int)(31*filesize) + 31*user_id + 33*createtime.hashCode()
                + 37*filename.hashCode() + 31*filepath.hashCode() + 31*MD5.hashCode();
    }
    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(this.getClass() != obj.getClass()) return false;
        //if(super.equals(obj)) --Object.equal()
        File otherObj = (File)obj; 
        return this.id==otherObj.id && this.user_id==otherObj.user_id && this.filesize==otherObj.filesize
            && this.canshare==otherObj.canshare && Objects.equals(this.createtime, otherObj.createtime)
            && Objects.equals(this.filename, otherObj.filename) && Objects.equals(this.filepath, otherObj.filepath)
            && Objects.equals(this.MD5, otherObj.MD5);
    }
    
    @Override
    public File clone() {
        File cloneFile = null;
        try {
            cloneFile = (File)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        cloneFile.id = id;
        cloneFile.filename = filename;
        cloneFile.filepath = filepath;
        cloneFile.filesize = filesize;
        cloneFile.createtime = (Date)createtime.clone();
        cloneFile.canshare = canshare;
        cloneFile.user_id = user_id;
        cloneFile.MD5 = MD5;
        return cloneFile;
    }
}
