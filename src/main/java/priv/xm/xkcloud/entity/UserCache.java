package priv.xm.xkcloud.entity;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import priv.xm.xkcloud.mapper.UserMapper;
import priv.xm.xkcloud.model.User;

/**设计缺陷: 没有提供更新用户剩余空间功能*/
@Component
@Scope(value="singleton")
public class UserCache implements InitializingBean{
    //所有类型缓存都是同一个用户集
    @Autowired
    private ConcurrentHashMap<String, User> userNameCache;  //userName-User
    public static final int LOAD_SIZE = 10000;
    private volatile int maxUserId;  //用于数据库查询偏移量
    
    @Autowired
    UserMapper userMapper;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.err.println("预加载用户数据...");
        List<User> cacheUserList = userMapper.findUsers(0, FileCache.LOAD_SIZE);
        for (User user : cacheUserList) {
            userNameCache.put(user.getUsername(), user);
        }
        //插入一条新纪录获取起始id, 在将其删除
        userMapper.addNewUser(new User("", "", 0));
        maxUserId = userMapper.findUserStartId();
        userMapper.deleteUserById(maxUserId);
    }
    
    public UserCache() {
        //userNameCache = new ConcurrentHashMap<String, User>(LOAD_SIZE);
    }
    
    /**@User 不含主键信息!*/
    public void addUser(User newUser) {
        if(userNameCache.size() >= LOAD_SIZE) return; 
        
        newUser.setId(++maxUserId);
        userNameCache.put(newUser.getUsername(), newUser);
    }
    
    /**没有则返回null*/
    public User getWithUserName(String userName) {
        return userNameCache.get(userName);
    }
    
    public int getMaxUserId() {
        return maxUserId;
    }
    
    public boolean verifyFailUserIdentity(User user) {
        User cacheUser = userNameCache.get(user.getUsername());
        return (cacheUser==null || !Objects.equals(user.getPassword(), cacheUser.getPassword())) ?  true : false;
    }
    
    public int size() {
        return userNameCache.size();
    }
}
