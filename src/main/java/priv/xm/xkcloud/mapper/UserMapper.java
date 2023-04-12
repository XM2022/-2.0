package priv.xm.xkcloud.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import priv.xm.xkcloud.model.User;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user LIMIT #{limitOffset}, #{entryNumber}")
    public List<User> findUsers(@Param("limitOffset")int limitOffset, @Param("entryNumber")int entryNumber);

    @Insert("INSERT INTO user(username,password,isvip) VALUES(#{username},#{password},#{isvip})")
    public void addNewUser(User user) throws Exception;
    
    @Select("SELECT id FROM user ORDER BY id DESC LIMIT 1")
    public int findUserStartId() throws Exception;

    /**聚合函数不能和limit同时使用*/
    @Select("SELECT COUNT(*) FROM user WHERE id > #{cacheMaxUserId} AND username=#{user.username} AND password=#{user.password}")
    public int verifyUserIdentity(@Param("user")User user, @Param("cacheMaxUserId")int cacheMaxUserId) throws Exception;
    
    @Select("SELECT COUNT(*) FROM user WHERE id > #{cacheMaxUserId} AND username=#{userName}")
    public int verifyUserExistence(@Param("userName")String userName, @Param("cacheMaxUserId")int cacheMaxUserId) throws Exception;

    @Select("SELECT user.id FROM user WHERE username=#{username}")
    public Integer findUserId(String username) throws Exception;
    
    @Select("SELECT * FROM user WHERE username=#{userName} LIMIT #{limitOffset}, #{entryNumber}")
    public User findUser(@Param("userName")String userName, @Param("limitOffset")int limitOffset, @Param("entryNumber")int entryNumber) throws Exception;

    @Select("SELECT isvip FROM user WHERE username=#{value}")
    public Integer isVip(String user_name)throws Exception;
    
    @Select("SELECT * FROM user WHERE id=#{id}")
    public User findUserById(int id) throws Exception;
    
    @Select("SELECT username FROM user WHERE id=#{id}")
    public String findUserNameById(int id) throws Exception;
    
    @Select("SELECT usedspace FROM user WHERE username=#{username}")
    public Integer findUsedSpace(String userName) throws Exception;
    
    /*--注意有外键约束, 在有引用的情况下应先禁用外键约束*/
    @Delete("DELETE FROM user WHERE id=#{id}") 
    public void deleteUserById(int id) throws Exception;
    
    /**@Param offset 正负值--内存空间变化量*/
    @Update("UPDATE user SET usedspace=usedspace + #{offset} WHERE id = #{userId}")
    public void updateUsedSpace(@Param("userId")int usedId, @Param("offset")int offset) throws Exception;
    
}
