package priv.xm.xkcloud.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import priv.xm.xkcloud.model.File;
import priv.xm.xkcloud.model.Page;

@Mapper
public interface FileMapper {

    /**不使用MySql提供的模糊查询*/
    //@Select("SELECT * FROM file WHERE canshare=1 AND filename LIKE #{searchcontent} LIMIT #{startindex},#{pagesize}")
    //public List<File> searchFile(@Param("page")Page page, @Param("maxCacheFileId")int maxCacheFileId) throws Exception;
    //查询全部共享文件, 让java代码使用正则表达式处理*/
    @Select("SELECT * FROM file WHERE canshare=1 order by createtime desc")
    public List<File> searchAllSharedFile() throws Exception;

    /*统计文件数*/
    @Select("SELECT COUNT(id) totalrecord FROM file WHERE canshare=1 AND filename LIKE #{searchcontent}")
    public int count(String searchcontent) throws Exception;
    
    /**用于预加载查询第一条记录--在file表存在记录的情况下*/
    @Select("SELECT id FROM file ORDER BY id DESC LIMIT 1")
    public int findFileStartId() throws Exception;

    @Select("SELECT * FROM file WHERE id=#{id} LIMIT #{limitOffset}, #{entryNumber}")
    public File findFileById(@Param("id")int id, @Param("limitOffset")int limitOffset, @Param("entryNumber")int entryNumber) throws Exception;
    
    @Select("SELECT * FROM file WHERE filename=#{fileName} LIMIT #{offset}, #{entryNumber}")
    public List<File> findFileByFileName(@Param("fileName")String fileName, @Param("offset")int offset, @Param("entryNumber")int entryNumber) throws Exception;
    
    @Select("SELECT * FROM file WHERE filename=#{fileName} AND user_id=#{userId} LIMIT #{offset}, #{entryNumber}")
    public List<File> findFileByFileNameAndUserId(@Param("fileName")String fileName, @Param("userId")int userId, @Param("offset")int offset, @Param("entryNumber")int entryNumber) throws Exception;

    /*插入文件*/
    @Insert("INSERT INTO icloud.file (filename,filepath,filesize,createtime,canshare,user_id,MD5) VALUES(#{filename},#{filepath},#{filesize},#{createtime},#{canshare},#{user_id},#{MD5})")
    public Integer insertFile(File file) throws Exception;

    /* 查询用户的文件*/
    @Select("SELECT * FROM file WHERE user_id=#{user_id} order by createtime desc LIMIT #{startindex},#{pagesize}")
    public List<File> getUserFiles(Page page) throws Exception;

    /*统计用户文件*/
    @Select("SELECT COUNT(*) totalrecord FROM file WHERE id>#{cacheMaxFileId} AND user_id=#{userId}")
    public int countFileQuantity(@Param("userId")int userId, @Param("cacheMaxFileId")int cacheMaxFileId) throws Exception;

    @Update("UPDATE FILE SET canshare=#{canshare} WHERE id=#{id}")
    public void updateFileById(@Param("id")int id, @Param("canshare")int shareState) throws Exception;
    
    @Update("UPDATE file SET filename = #{filename}, filepath=#{filepath}, filesize=#{filesize},  createtime=#{createtime}, canshare=#{canshare}, user_id=#{user_id}, MD5=#{MD5} WHERE id=#{id}")
    public void updateFileInfo(File file) throws Exception;

    @Delete("DELETE FROM FILE WHERE id=#{value}")
    public void deleteFileById(int id) throws Exception;

    @Select("SELECT file.filename FROM file WHERE id=#{value}")
    public String findFilenameById(int id) throws Exception;
    
    @Select("SELECT * FROM file LIMIT #{offset}, #{entryNumber}")
    public List<File> findFile(@Param("offset")int offset, @Param("entryNumber")int entryNumber) throws Exception;
    
    @Select("SELECT * FROM file WHERE user_id = #{userID} and MD5 = #{md5}")
    public List<File> findFileByUserIdAndMd5(@Param("usrID")int userID, @Param("md5")String md5) throws Exception;
    
    /*@Select("SELECT * FROM file WHERE user_id != #{userID} and MD5 = #{md5}")
    public List<File> findFileExclude(int excludeUserID, String md5) throws Exception;*/
    
    /**Limit从某一行开始查至表尾:Integer.MAX_VALUE*/
    @Select("SELECT * FROM file WHERE MD5 = #{md5} LIMIT  #{offset}, #{entryNumber}")
    public List<File> findFileByMd5(@Param("md5") String md5, @Param("offset") int offset, @Param("entryNumber")int entryNumber) throws Exception;
    
    /*@ResultMap("Md5CacheItemResultMap")
    @Select("SELECT MD5, user_id FROM file LIMIT #{offset}, #{entryNumber}")
    public List<Md5CacheItem> queryMd5Cache(int offset, int entryNumber) throws Exception;*/
    
    /**用于极速上传校验*/
    @Select("SELECT count(*) FROM file WHERE id > #{maxCacheFileId} and MD5=#{md5}")  
    public int findMd5(@Param("md5")String md5, @Param("maxCacheFileId")int maxCacheFileId) throws Exception;
    
}
