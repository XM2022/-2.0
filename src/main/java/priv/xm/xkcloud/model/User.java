package priv.xm.xkcloud.model;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("user")
@Scope("prototype")
public class User  {
    /**单位统一用: MB*/
    public static final int COMMON_SINGLE_FILE_LIMIT = 2048;  
    public static final int COMMON_USER_SPACE_SIZE = 5*1024; 
    public static final int VIP_SINGLE_FILE_LIMIT =  2048;
    public static final int VIP_USER_SPACE_SIZE = 10* 1024; 
    
	private int id;
	private String password;
	private String username;
	private int isvip;  /**1是vip, 0不是*/
	private int usedspace;  /**已使用空间; MB, int最大支持约8T*/
	
	public User() {  }
	
	public User(String userName, String password, int isvip) {
	    this.username = userName;
	    this.password = password;
	    this.isvip = isvip;
	}
	
    public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public int getIsvip() {
		return isvip;
	}
	public void setIsvip(int isvip) {
		this.isvip = isvip;
	}
    public int getUsedspace() {
        return usedspace;
    }
    public void setUsedspace(int userspace) {
        this.usedspace = userspace;
    }
	
}
