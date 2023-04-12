package priv.xm.xkcloud.entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;;

/**用于实现删除操作的回调*/
public class FileOccupyCallback {
    /**保存处于占用(下载传输、正在切片)状态的文件md5列表--需要重复*/
    private List<String> occupyFileList;
    /**保存文件删除任务:fileMd5-文件绝对路径*/
    private HashMap<String, String> deleteTask;
    
    /**@param filePath 文件绝对路径*/
    synchronized public void registerDeleteTask(String md5, String filePath) {
        if(occupyFileList.contains(md5)) deleteTask.put(md5, filePath);
        else {
            try {
                Files.delete(Paths.get(filePath));
            } catch (IOException e) {
                System.err.println("文件删除失败:" + filePath);
                e.printStackTrace();
            }
        }
    }
    
    
    //单例模式
    private FileOccupyCallback() {
        occupyFileList = new LinkedList<String>();
        deleteTask = new HashMap<String, String>();
    }
    
    private static FileOccupyCallback self = new FileOccupyCallback();
    
    public static FileOccupyCallback getInstance() { return self; }

    /**
     * @param filePath: 文件绝对路径
     * @return false: 文件不存在, 可能已经被删除
     **/
    synchronized public boolean addOccupyFile(String md5, String filePath) {
        if(filePath == null) throw new NullPointerException();
        if(Files.notExists(Paths.get(filePath))) return false;
        else return occupyFileList.add(md5);
    }
    
    //HashMap不能并发删除, 所以要加synchronzied关键字
    synchronized public void removeOccupyFile(String fileMd5) {
        if(fileMd5 == null) throw new NullPointerException();
        occupyFileList.remove(fileMd5);
        if(occupyFileList.contains(fileMd5)) return;  //此文件还存在其它任务占用情况
        if(deleteTask.containsKey(fileMd5)) { //存在注册的删除任务
            String filePath = deleteTask.get(fileMd5);
            try {
                Files.delete(Paths.get(filePath));
                deleteTask.remove(fileMd5);
            } catch (IOException e) {
                System.err.println("文件删除失败:" + filePath);
                e.printStackTrace();
            }
        }
        
    }
}
