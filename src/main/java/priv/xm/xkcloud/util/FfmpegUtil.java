package priv.xm.xkcloud.util;

import static priv.xm.xkcloud.model.File.VIDEO_SLICE_ROOT_DIRECTORY;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import priv.xm.xkcloud.entity.FfmpegRunResult;
import priv.xm.xkcloud.entity.FileOccupyCallback;

@Component
@Scope("singleton")
public class FfmpegUtil {
    public static final int PROCESS_MAX_RUN_TIME = 30;  //分钟
    public static final int USER_MAX_WAIT_TIME = 20;  //秒
    
    public enum FfmpegSliceCommand{
        ENTIRE_FILE("ffmpeg -i %s -c:v copy -c:a copy -f ssegment -segment_format mpegts -segment_list %s -segment_time 10 %s"),
        START_END_TIME("ffmpeg -i %s -ss %d -to %d -c copy -f ssegment -segment_format mpegts -segment_list %s -segment_time 10 %s");
        
        //切片文件名尽量简短,减少m3u8文件大小利于网络传输
        public static String splice(String absoluteInputPath, String absoluteOutputPath, String fragmentAbsolutePath) {
            return String.format(ENTIRE_FILE.toString(), absoluteInputPath, absoluteOutputPath, fragmentAbsolutePath);
        }
        
        public static String splice(String absoluteInputPath, String absoluteOutputPath, String fragmentAbsolutePath, int startSecond, int endSecond) {
            return String.format(START_END_TIME.toString(), absoluteInputPath, startSecond, endSecond, absoluteOutputPath, fragmentAbsolutePath);     
        }             
                      
        private String cmdLine;
        private FfmpegSliceCommand(String cmdLine) {
            this.cmdLine = cmdLine;
        }
        
        @Override
        public String toString() {
            return this.cmdLine;
        }
    }
    
    private static ThreadPoolExecutor videoSliceThreadPool;
    private static Set<String> videoRunFfmpegQueue;
    private static final FileOccupyCallback fileOccupyCallback = FileOccupyCallback.getInstance();
    
    @Autowired
    @Qualifier("videoSliceThreadPool")
    public void setVideoSliceThreadPool(ThreadPoolExecutor threadPool) {
        videoSliceThreadPool = threadPool;
    }
    
    @Autowired 
    @Qualifier("videoRunFfmpegSet")
    public void setVideoRunFfmpegQueue(Set<String> set) {
        videoRunFfmpegQueue = set;
    }

    /**
     * 对视频文件进行完整切片.(阻塞方法)
     * @param absoluteInputPath 视频文件绝对路径(含文件名)
     * @param absoluteOutputPath m3u8索引文件存放路径
     * @param fileMd5 用于移除视频文件的占用状态
     * @return ffmpeg的运行状态信息
     * @throws IOException 进程/创建文件夹IO异常
     * @throws InterruptedException  运行信息流输出中断
     */
    public static FfmpegRunResult runFfmpeg(String absoluteInputPath, String absoluteOutputPath, String fileMd5) throws IOException {
        String videoSliceDirectory = absoluteOutputPath.substring(0, absoluteOutputPath.lastIndexOf(File.separator)+1);
        Files.createDirectories(Paths.get(videoSliceDirectory)); 
        return executeFfmpeg(FfmpegSliceCommand.splice(absoluteInputPath, absoluteOutputPath, videoSliceDirectory+"%05d.ts"), fileMd5);
    }
    
    /**
     * 对视频文件的某一时间段进行部分切片.(阻塞方法)
     * @param absoluteInputPath 视频文件绝对路径(含文件名)
     * @param absoluteOutputPath m3u8索引文件存放路径
     * @param fileMd5 用于移除视频文件的占用状态
     * @return ffmpeg的运行状态信息
     * @throws IOException 进程/创建文件夹IO异常
     * @throws InterruptedException 运行信息流输出中断
     */
    public static FfmpegRunResult runFfmpeg(String absoluteInputPath, String absoluteOutputPath, int startSecond, int endSecond, String fileMd5) throws IOException {
        String videoSliceDirectory = absoluteOutputPath.substring(0, absoluteOutputPath.lastIndexOf(File.separator)+1);
        Files.createDirectories(Paths.get(videoSliceDirectory)); 
        return executeFfmpeg(FfmpegSliceCommand.splice(absoluteInputPath, absoluteOutputPath, videoSliceDirectory+"%05d.ts", startSecond, endSecond), fileMd5);
    }
    
    /**
     * 阻塞方法. 运行完成状态通过Proccess获取.
     * @param cmdParameter 完整命令行参数. 请用标准命令行写法, 不同参数使用一个空格分隔
     * @return ffmpeg的运行状态信息
     * @throws IOException 进程IO异常
     * @IOException ffmpeg进程异常
     * @InterruptedException 运行信息流输出中断
     */
    private static FfmpegRunResult executeFfmpeg(String commandLine, String fileMd5) throws IOException {
        /**ffmpeg只会使用正常流和错误流之一进行信息输出,所以没必要使用StringBuffer*/
        StringBuilder runInfo = new StringBuilder(); 
        
        List<String> cmdArgs = Arrays.asList(commandLine.trim().split(" "));
        Process process = new ProcessBuilder(cmdArgs).start(); 
        // 处理InputStream
        Thread normalStreamThread = new Thread(() -> {
            BufferedReader buffer = null;
            try {
                buffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String inputLine = "";
                while ((inputLine = buffer.readLine()) != null) {
                    runInfo.append(inputLine + System.getProperty("line.separator"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (buffer != null) {
                        buffer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        // 处理ErrorStream
        Thread errorStreamThread = new Thread(() -> {
            BufferedReader buffer = null;
            try {
                buffer = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine = "";
                while ((errorLine = buffer.readLine()) != null) {
                    runInfo.append(errorLine + System.getProperty("line.separator"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (buffer != null) {
                        buffer.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        normalStreamThread.setName("normalStreamThread");
        normalStreamThread.start();
        errorStreamThread.setName("errorStreamThread");
        errorStreamThread.start();

        //阻塞等待ffmpeg转换完成后，才往下执行
        /*normalStreamThread.join();
        errorStreamThread.join();  //两个线程同时进行和结束,等待一个即可*/
        int runResult = waitForComplete(process, runInfo, fileMd5);
        
        return new FfmpegRunResult(runResult, runInfo);
    }
    
    /**这一切的设计都是为了保证fileOccupyCallback.removeOccupyFile(fileMd5)的执行
     * 之前是把Process运行结果放在Service层等待用户下次请求取出,
     * 这是不可靠的: 如果用户网络中断无法发起第二次请求怎么办？
     *               --该视频文件将永远处于占用状态无法被删除！
     * @return 切片任务超时未完成立即返回, 开启后台线程等待30分钟.
     *    1:成功 0:超时 -1:解析失败 
     */
    private static int waitForComplete(Process ffmpeg, StringBuilder runInfo, String fileMd5) {
        System.out.println("等待视频切片任务完成...");
        int runResult = 0;
        boolean waitResult = false;
        try {
            if(waitResult = ffmpeg.waitFor(USER_MAX_WAIT_TIME, TimeUnit.SECONDS)) {
                runResult = ffmpeg.exitValue()==0 ? 1 : -1;
            }
            else runResult = 0;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            //如果为false暂时不能移除占用, 由后台线程等待半小时后移除.
            if(waitResult)  {
                fileOccupyCallback.removeOccupyFile(fileMd5);
                videoRunFfmpegQueue.remove(fileMd5);
            }
        }
        
        if(!waitResult) { //视频切片任务仍未完成(可能是一个大视频文件)
                //开启一个线程等待半小时(应该够了吧). 如果仍未完成,强行结束进程,并做好文件的清理
                videoSliceThreadPool.execute(()->{  
                    try {
                        if(ffmpeg.waitFor(PROCESS_MAX_RUN_TIME, TimeUnit.MINUTES)) {
                            if(ffmpeg.exitValue() == 0) System.out.println(fileMd5 + "切片完成.");
                            else System.err.println("ffmpeg运行出现异常！运行信息:\n" + runInfo);
                        }
                        else {
                            ffmpeg.destroyForcibly();
                            System.err.println("任务因超时被强制终止！ffmpeg运行状态信息:\n" + runInfo);
                            //文件清理--直接删:因为.m3u8没有生成, 不会发起传输请求
                            File[] slices = new File(VIDEO_SLICE_ROOT_DIRECTORY.getPath(), fileMd5).listFiles();
                            for(int i=0; i<slices.length; ++i) {
                                if(!slices[i].delete()) System.err.println("切片文件删除失败:" + slices[i].getPath());
                            }
                        }
                        
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        fileOccupyCallback.removeOccupyFile(fileMd5);
                        videoRunFfmpegQueue.remove(fileMd5);
                    }
                });
                System.out.println("结束等待视频切片任务");
        }
        return runResult;
    }
    
}
