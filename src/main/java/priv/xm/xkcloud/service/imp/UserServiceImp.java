package priv.xm.xkcloud.service.imp;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import priv.xm.xkcloud.entity.UserCache;
import priv.xm.xkcloud.exception.CallNotSupport;
import priv.xm.xkcloud.exception.UserNonExistException;
import priv.xm.xkcloud.mapper.UserMapper;
import priv.xm.xkcloud.model.User;
import priv.xm.xkcloud.service.UserService;


@Service 
public class UserServiceImp implements UserService {
	@Autowired
	private UserMapper userMapper;
	
	@Autowired
	private UserCache userCache;
    
    /**不存在则返回null*/
    private User findUserFromCacheOrDatabase(String userName) throws Exception{
        User user = userCache.getWithUserName(userName);  //先从缓存中查询
        if(user == null) user = userMapper.findUser(userName, userCache.size(), Integer.MAX_VALUE);  //再进数据库中查询
        return user;
    }
    
    @Override
    public boolean verifyFailUserIdentity(User user) throws Exception{
        return userCache.verifyFailUserIdentity(user) 
            && userMapper.verifyUserIdentity(user, userCache.getMaxUserId())==0;
    }
    
    /**同步方法防止缓存中的id与数据库中的不一定*/
	@Override
    synchronized public void createUser(User user) throws Exception{
		userMapper.addNewUser(user);
		userCache.addUser(user);
	}
	
    @Override
    public int findUserID(String username) throws Exception{
        User user = findUserFromCacheOrDatabase(username);
        if(user == null) throw new UserNonExistException();
        return user.getId();
    }
    
    @Override
    public boolean existUser(String username) throws Exception{
        User user = userCache.getWithUserName(username);
        if(user == null) return userMapper.verifyUserExistence(username, userCache.getMaxUserId())>0 ? true : false;
		return true;
	}

	@Override
    public boolean isVIP(String userName)throws Exception {
		return findUserFromCacheOrDatabase(userName).getIsvip()==1 ? true : false;
	}

	@Override
    public User findUserById(int id) throws Exception {
	    return userMapper.findUserById(id);
	}
	
	@Override
    public String findUserNameById(int id) throws Exception {
	    return userMapper.findUserNameById(id);
	}
	
	/**@return 单位:MB*/
	@Override
    public int findUserUsedSpace(String userName) throws Exception {
        /*User user = userCache.getWithUserName(userName);  //不查缓存没有更新用户剩余空间功能
        if(user == null) user = userMapper.findUser(userName, userCache.size(), Integer.MAX_VALUE); //进数据库查
        */	    
	    User user = userMapper.findUser(userName, 0, Integer.MAX_VALUE);
	    if(user == null) throw new UserNonExistException();
	    return user.getUsedspace();
	}
	
	@Override
    public int findUserStartId() throws Exception {
	    return userMapper.findUserStartId();
	}

	@Override
    /*用户未登录或session已过期或用户名不符, 均要求重新登陆.*/
    public boolean verifyFailUserLogState(HttpSession session) {
	    Object sessionUserName = session.getAttribute("user_name");
        return sessionUserName == null || "".equals(sessionUserName);
    }
	
	@Override
	public void deleteUser(int id) {
	    //应先删除此用户所有的服务器器磁盘文件
	    ;
	    
	    //再删除数据库的用户信息
	    ;
	    
	    throw new CallNotSupport("删除用户功能暂未开发完成！");
	}
	
}
