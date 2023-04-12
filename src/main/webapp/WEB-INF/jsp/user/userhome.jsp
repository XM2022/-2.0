<%@page import="org.apache.jasper.runtime.PageContextImpl"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title></title>
    <link rel="shortcut icon" href="../images/xk.ico">
    <link rel="icon" type="image/jpg" href="../images/xk.ico">
    <link rel="stylesheet" href="../css/bootstrap4.1.1.min.css">
    <link rel="stylesheet" href="../css/nav.css">
    <link rel="stylesheet" href="../css/font-awesome.min.css">
    <script type="text/javascript" src="../js/jquery-3.2.1.min.js"></script>
    <script type="text/javascript" src="../js/spark-md5.js"></script>
    
    <link rel="stylesheet" href="../css/dialog.css">
    <script type="text/javascript" src="../js/dialog.js"></script>
    <link rel="stylesheet" href="../css/message-box.css">
    <script type="text/javascript" src="../js/message-box.js"></script>
    <link rel="stylesheet" href="../css/progress.css">
    <script type="text/javascript" src="../js/progress.js"></script>
    
    <style type="text/css">
        table{
            font-family: "微软雅黑", "宋体";
            font-size: 1.1em
        }
        body {
            margin-left: 10px;
            margin-right: 10px;
        }
    </style>
</head>
<body>
<nav class="top-right" id="top-home">
    <a class="disc l1" href="${pageContext.request.contextPath}/users/logout">
        <div>注销</div>
    </a>
    <a class="disc l2" href="${pageContext.request.contextPath}/home">
        <div>主页</div>
    </a>
    <a class="disc l3" href="${pageContext.request.contextPath}/users/help">
        <div>帮助</div>
    </a>
    <!-- <a class="disc l4 toggle"> -->
    <a class="disc l5 toggle" id="home-menu">
        菜单
    </a>
</nav>
<script src="../js/nav2.js"></script>

<div style="font-size: 24px ; text-align: center; font-family: 微软雅黑; font-size: 2em;">欢迎你！${user_name}
    <div id="spaceInfo" style="font-family: 微软雅黑; font-size: 23px"></div>
    <input type="hidden" id="usedSpace" name="usedSpace" value="${requestScope.usedSpace}">
    <input type="hidden" id="isvip" name="isvip" value="${isvip}">
</div>

<br/>

<form id="form_file" action="${pageContext.request.contextPath}/files" method="post" enctype="multipart/form-data">
     <!-- <input type="submit" class="btn btn-outline-danger" onclick="return checkFile()" value="上传文件"/> -->
     <!-- 自定义异步提交按钮 -->
    <input type="button" id="handin" class="btn btn-primary xmbtn" onclick="upload()" value="上传文件"/> 
    <input class="btn btn-outline-primary" type="file" onchange="checkFile()" id="fileupload" name="file"/><br/>
    <input type="hidden" name="MD5" id="md5" value=""/>
    <img id="tempimg" dynsrc="" src="" style="display:none"/>  <!-- 用于IE计算文件大小 -->
        ${message }
    <div id="box"></div>
    <button class="btn btn-warning xmbtn" id="cal" type="button" onclick="fastUpload()">极速上传</button>
    <div id="custom">
        <!-- 上传进度条 -->
	    <div id="progress">
	        <div id="progress-bar">0%</div>
	    </div>
	    <audio id="music" preload="auto" controls autoplay>
            <source/>
            您的浏览器不支持播放此音频！
        </audio>
	</div>
</form> 

<br/>
<br/>

<div style="text-align: center">
    <table class="table table-hover table-borderless" frame="border" width="100%" align="center">
        <thead class="thead bg-success">
        <tr>
            <th scope="col">文件名</th>
            <th scope="col">文件大小</th>
            <th scope="col">上传日期</th>
            <th scope="col">下载文件</th>
            <th scope="col">设置共享</th>
            <th scope="col">删除文件</th>
            <th scope="col">在线操作</th>
        </tr>
        </thead>
        <tr>
            <tbody>
            <c:forEach var="c" items="${requestScope.pagebean.list}" varStatus="stat">
                <tr class="table-info">
                    <td>${c.filename }</td>
                    <td>${c.filesize }MB</td>
                    <td>${c.createtime }</td>
                    <td>
                        <a class="btn btn-success"
                           href="${pageContext.request.contextPath}/files/download?id=${c.id}&filename=${c.filename }">下载</a>
                    </td>
                    <td>
                        <form>
                            <select class="custom-select" id="selector${c.id}"  onchange="setShare(${pagebean.currentpage},${c.id})">
                                <c:if test="${c.canshare==0 }">
                                    <option value="0" selected="selected">私有</option>
                                    <option value="1">共享</option>
                                </c:if>
                                <c:if test="${c.canshare==1 }">
                                    <option value="0">私有</option>
                                    <option value="1" selected="selected">共享</option>
                                </c:if>
                            </select>
                        </form>
                    </td >
                    <td>
                        <button class="btn btn-danger" onclick="deleteFile(${pagebean.currentpage},${c.id})">删除</button> <!-- c.id是文件主键 -->
                    </td>
                    <td id="${c.id}">
                        <!-- 动态创建打开按钮栏 -->
                    </td>
                </tr>
            </c:forEach>
            </tbody>

    </table>
    <br/>
    <p class="text-secondary">
        共[${requestScope.pagebean.totalrecord}]条记录,
        每页 <input type="text" id="pagesize" value="${requestScope.pagebean.pagesize }" onchange="turnPage(${requestScope.pagebean.currentpage})"
                  style="10px; width=40%" maxlength="5">条
        共[${requestScope.pagebean.totalpage}]页,
        当前是第[${requestScope.pagebean.currentpage}]页,
    </p>     
    <a class="btn btn-success" href="javascript:void(0)" onclick="turnPage(1)">回到首页</a>
    <a class="btn btn-primary" href="javascript:void(0)"
       onclick="turnPage(${requestScope.pagebean.previouspage})">上一页</a>
    <c:forEach var="pagenum" items="${requestScope.pagebean.pagebar}">
        <c:if test="${pagenum==pagebean.currentpage }">
            <font color="red">${pagenum }</font>
        </c:if>
        <c:if test="${pagenum!=pagebean.currentpage }">
            <a href="javascript:void(0)" onclick="turnPage(${pagenum})">${pagenum}</a>
        </c:if>
    </c:forEach>
    <a class="btn btn-primary" href="javascript:void(0)" onclick="turnPage(${requestScope.pagebean.nextpage})">下一页</a>
    <input class="btn btn-warning xmbtn" type="button" value="跳转至第"
           onclick="skipPage(document.getElementById('pagenum').value)"/>
    <input type="text" style="10px ; width=40%" maxlength="5" id="pagenum">页
</div>

<script type="text/javascript">
    var isvip = $("#isvip").val();
    spaceInfo.innerText = "（" +  parseFloat($("#usedSpace").val()).toFixed(2) + "GB/" + (isvip?10:5)  +"GB）";
    var isUploading = false;  //记录上传状态
    var fileInfo = {
        isLegal: false, //保存checkFile()检测结果
	   	file: null,
	   	size: null,
	   	promise: null
    }
    var sepPos = location.pathname.lastIndexOf('/');  //方法
    sepPos = location.pathname.lastIndexOf('/', sepPos-1);  //模块
    var prefixPath = location.pathname.substring(0, sepPos==0 ? 1 : sepPos+1); //前后都含/
    //针对不同文件类型--显示打开按钮
    let fileNameList = document.querySelectorAll('.table-info>td:first-of-type');
    let openTdList = document.querySelectorAll('.table-info>td:last-of-type');
    for(let i=0; i<fileNameList.length; ++i) {
        let fileName = fileNameList[i].innerText;
        let dotPos = fileName.lastIndexOf('.');
        if(dotPos == -1 || dotPos == fileName.length-1) break;  //文件没有后缀名, 类型无法判断
        let suffix = fileName.substring(dotPos+1).toLowerCase();
        let buttonText;
        let clickEvent;
        switch(suffix) {
        //图片
        case 'jpg': case 'jpeg': case 'png': case 'webp': case 'bmp': case 'gif': 
            buttonText = '查看';
            clickEvent = openFile;
            break;
        //视频
        case 'mp4': 
            buttonText = '播放';
            clickEvent = playVideo;
            break;
        case 'rmvb': case 'avi': case 'webm': case 'mov':
	         buttonText = '播放';
	         clickEvent = playVideoWithInform;
	            break;
        //文本
        case 'txt': case 'html': case 'htm': case 'css': case 'js': case 'json': case 'xml': case 'md':
            buttonText = '打开';
            clickEvent = openFile;
            break;
        //音频    
        case 'mp3': case 'wav': case 'ogg': case 'flac':
            buttonText = '播放';
            clickEvent = playAudio;
            break;
            
        default:
            //不支持在线打开
            buttonText = "";  
        }
        //创建文件打开按钮
        if(buttonText != "") {
	        let button = document.createElement('button');
	        button.className = "btn btn-info";
	        button.innerHTML = buttonText;
	        button.addEventListener("click", ()=>{
	            clickEvent(openTdList[i].id);
	        });
	        openTdList[i].appendChild(button);
        }
    }

    //创建选择对话框
    const dialog = new ModelBox({
       id:1,
       title:"警告",
        confirm:"确认",
        cancel:"取消",
        isShowCancel:true
     });
       //创建提示对话框
    const tips = new ModelBox({
       id:2,
       title:"提示",
        confirm:"我已知晓",
        isShowCancel:false
     });
        
    //创建进度条
    var progressBar = getProgress({
	    id: "progress",  //进度条轨道id
	    barId: "progress-bar",  //进度条id
	    width: '25%',  //进度条长度
	    height: '80%',  //进度条高度
	    box: false,     //true:方型  false/null:圆角
	});
    var params = new URLSearchParams(window.location.search);
    var progressValue = parseInt(params.get("progress"), 10);
    progressBar.set(progressValue ? progressValue : 0);

    function openFile(fileId) {
        window.open(prefixPath + "files/open/" + fileId);  //resultful风格
    }
    
    const audio = document.getElementById("music");
    function playAudio(fileId) {
    	//显示隐藏的音频播放条
    	audio.style.visibility = "visible"; 
    	audio.src = prefixPath + "files/open/" + fileId; 
    }
    
    function playVideoWithInform(fileId) {
        tips.show("此视频格式可能无法在线播放或存在加载时间较长的现象.<br>如果出现上述情况，请下载到本地打开.")
        setTimeout(()=>{
        	playVideo(fileId);
        }, 2000);
    }
    function playVideo(fileId) {
        window.open(prefixPath + "files/onlinePlay/" + fileId); 
    }

    function deleteFile(currentpage, fileId) {
        var pagesize = document.getElementById("pagesize").value;
        
        if (pagesize > 10 || pagesize >= ${pagebean.totalrecord - pagebean.pagesize * ( pagebean.currentpage - 1 )}) {
            pagesize = Math.min(pagesize, ${pagebean.totalrecord});
            currentpage = 1;
        } else if (pagesize < 1) {
            pagesize = 1;
        }
        dialog.setCallBack(
            function(){ //确认
	            $.ajax({
	                 url: prefixPath + "files/" + fileId,
	                 type: "DELETE",
	                 success: function (res) {  
	                	 response = JSON.parse(res);
	                     if (response.result) window.location.href= prefixPath + 'users/homePage?currentpage=' + currentpage + '&pagesize=' + pagesize;
	                     else if(response.msg == "loginInvalid") location.assign(prefixPath + "users/login");
	                     else showToast(response.msg, "error");
	                 }
	             })
            }
        );
        dialog.show("确定删除该文件吗？");
    }

    function setShare(currentpage, fileid) {
        let selectedValue = $('#selector'+fileid).val();
        let pagesize = $('#pagesize').val();
        dialog.setCallBack(
            function(){  //确认
             $.ajax({
                 url: prefixPath + 'files/share?id='+fileid+'&shareState='+selectedValue,
                 type: "GET",
                 success: function (res) {  
                	 response = JSON.parse(res);
                     if (response.result) showToast(response.msg, "success");
                     else if(response.msg == "loginInvalid") location.assign(prefixPath + "users/login");
                     else {
                         showToast(response.msg, "error");
                         selectedValue ? $('#'+fileid).val(0) : $('#'+fileid).val(1);  //回退选中项
                     }
                 }
             })
            },
            function(){ //取消--回退选中项
                selectedValue=="1" ? $('#selector'+fileid).val(0) : $('#selector'+fileid).val(1);
           }
        );
        dialog.show("确定将文件设置为"+(selectedValue=='1'?'共享':'私有')+"吗？");
        
        //window.location.href = '${pageContext.request.contextPath}/Share?currentpage=' + currentpage + '&pagesize=' + pagesize + '&id=' + fileid + '&selectedValue=' + selectedValue;

    }

    function fastUpload() {
        if (!uploadFileGroup.value) {
            showToast("请先选择上传文件", "warn");
            return;
        }
        if(!fileInfo.isLegal) {
            showToast("此文件不支持上传, 请重新选择.", "error");
            return ;
        }
        if(isUploading) {
            showToast("正在上传，请勿重复提交！", "warn");
            return;
        }
        isUploading = true;
        progressBar.set(30);  //用于提示用户处于上传状态
        fileInfo.promise.then((md5Value)=>{  //当md5计算完成时调用
            $.ajax({
                url: prefixPath + "files/fastUpload",
                type: "POST",
                data: {
                    "MD5": md5Value,
                    "fileName":  fileInfo.file.name,
                    "fileSize": fileInfo.size
                }, 
                success: function (res) {
                	response = JSON.parse(res); 
                    if(response.result) {
                    	progressBar.set(100);  
                    	showToast(response.msg, "success");
                    	uploadFileGroup.value = null;
                    	uploadFileGroup.files = null;
                    	setTimeout(()=>{
                    		　window.location.href= prefixPath + "users/homePage?progress=" + 100;
                    	}, 500);
                    }
                    else {
                        if(response.msg == "loginInvalid") location.assign(prefixPath + "users/login");
                        else {
                        	progressBar.set(0);
                        	showToast("此文件不支持极速上传！", "error");
                        }
                    	//不进行文件选择框的清空:
                    	//用户可能选择删除其它文件腾出空间, 从而再次点击可以顺利上传
                    }
                    isUploading = false;
                },
                error: function(xhr, status, error) {
                    showToast("网络错误，文件上传失败！", "error");  
                    console.error(status, error);
                    isUploading = false;
                }
            }) 
                    
        });  //fileInfo.promise.then(
    }
    
    //var md5IsCalculating = true;  --无法更新
    /* function upload()  {
        //修改同步MD5值
        calculateMD5();
        console.log("1")
        while(document.getElementById("md5").value.length==0) ; //阻塞等待md5异步计算事件执行完成
        console.log("2")
        //while(md5IsCalculating);  //阻塞等待md5异步计算事件执行完成
        //md5IsCalculating = true;  //恢复原状态使之能重复调用
        return checkFile();         
    }  */

    var uploadFinish = false; //fileReader是乱序上传完成的, 需要uploadFininsh确定是最后一块切片.
    var uploadError = false; //用于通知所有的FileReader终止上传.
    function upload() 
    {
        if (!uploadFileGroup.value) {
            showToast("请先选择上传文件", "warn");
            return;
        }
        if(!fileInfo.isLegal) {
            showToast("此文件不支持上传, 请重新选择.", "error");
            return;
        }
        if(isUploading) {
            showToast("正在上传，请勿重复提交！", "warn")
            return;
        }
        
        isUploading = true;
        progressBar.set(5);  //用于提示用户处于上传状态
        fileInfo.promise.then((md5Value)=>{ //异步提交表单数据
            //document.getElementById("form_file").submit();  
            //先发送md5验证服务器是否存在相同文件, 同时获取已上传的切片列表
            $.ajax({
                url: prefixPath + "files/fastUpload",
                type: "POST",
                data: {
                	"MD5": md5Value,
                	"fileName": fileInfo.file.name,
                	"fileSize": fileInfo.size,
                },
                success: function (res) {
                    response = JSON.parse(res);         
                    if(response.result) { //极速上传
                        progressBar.set(100);  
                    	showToast(response.msg, "success");
                        isUploading = false;  //复位
                        setTimeout(()=>{
                           　window.location.href= prefixPath + "users/homePage?progress=" + 100;
                        }, 500);
                    }
                    else if(response.msg == "loginInvalid") { //登录状态失效
                    	location.assign(prefixPath + "users/login");
                    }
                    else { //进行实际上传
                        sliceFileAndUpload(response.msg);  //msg包含服务器中存在的文件切片数组, 用于断点续传
                    }
                },
                error: function(xhr, status, error) { //一般是网络连接错误
                    showToast("网络错误，文件上传失败！", "error");  
                    console.error(status, error);
                    //复位(不复位文件选择框:用户可能选择删除其它文件腾出空间, 从而再次点击可以顺利上传)
                    isUploading = false;
                }
            });
        });
    }
        
    //文件切片并发上传
    //uploadedSliceIds: 已上传的切片id, 为空则上传完整文件
    function sliceFileAndUpload(uploadedSliceIds) 
    {
        //文件分割方法（注意兼容性）
        const blobSlice = File.prototype.mozSlice || File.prototype.webkitSlice || File.prototype.slice;
        const chunkSize = 10*1024*1024;  //10MB
        const chunkNum = Math.ceil(fileInfo.size/chunkSize);
        let allowAjaxMaxNum = 5;  //用于限制并发文件读取和上传数量
        let fileName = fileInfo.file.name;  //此方法貌似IE不行
       	let runningReaderNum = 0;
        let sliceList = {
        	array: new Array(chunkNum),
            count: 0
        }
        for(let i of uploadedSliceIds) {
        	sliceList.array[i] = i; //保存填充已上传切片数量
        	++sliceList.count;
        }
        const concurrentReaderNum = Math.min(chunkNum-sliceList.count, allowAjaxMaxNum)
        let completeNum = sliceList.count;  //用于进度条的正确显示
        console.log("服务器已存在切片:\n" + uploadedSliceIds.toString());
        console.log("切片初始列表:\n" + sliceList.array);
        console.log("Ajax开启数量:" + concurrentReaderNum);
        progressBar.set(Math.round(uploadedSliceIds.length/chunkNum*100));
       	let startTime = performance.now();
        try{
	        for(let i=0; i<concurrentReaderNum; ++i) { 
	            let chunkIndex;
	            const fileReader = new FileReader();  //注意控制资源消耗
	            ++runningReaderNum;
	            //先注册onload(异步事件!)--每块文件读取完毕之后的处理
	            fileReader.onload = function (e) {
	                //每块交由sparkMD5进行计算
	                let spark = new SparkMD5();
	                spark.appendBinary(e.target.result); 
	                let md5Value = spark.end();  //注意spark.end()每次调用产生不同结果.
	                //注意每个文件块读取是乱序完成的, i记录着其切片序号
	                new Promise((resolve, reject)=> {
		                formData = new FormData();
		                fileBlob =  new File([blob], chunkIndex + "-" + chunkNum + "-" + fileName);  //切片序号(0开始)+切片总数+文件名:序号在前方便后端排序
		                formData.append("fileBlob", fileBlob);
		                formData.append("blobMD5", md5Value);
		                sendFileBlob(formData, resolve, allowRetryCount);  //异步传输文件块
		                console.log("正在传输:", fileBlob.name, "\nMD5:", md5Value);
	                }).then(()=>{  //传输完成时
	                	if(uploadError) {  //中断后续传输
	                		if(--runningReaderNum <= 0) {
	                			progressBar.set(0);
	                			uploadError = false;
	                			isUploading = false;
	                		}
	                		return;
	                	}

	                	++completeNum;
	                    progressBar.set(Math.round(completeNum/chunkNum*100)); //设置进度条显示
	                    if(sliceList.count >= chunkNum){
	                        //刷新文件列表
	                        if(uploadFinish) {  //fileReader是乱序上传完成的.
		                  		//复位
		                        uploadFileGroup.value = null;
		                        uploadFileGroup.files = null;
		                        isUploading = false;
	                        	showToast("文件上传成功！", "success");
	                        	uploadFinish = false;
	                            setTimeout(()=>{
	                               　window.location.href= prefixPath + "users/homePage?progress=" + 100;
	                            }, 500);
	                            console.log("文件上传完成！共耗时", ((performance.now()-startTime)/1000).toFixed(2), "S.");
	                        }
	                  	    return; 
	                    }
	                    for(let k=0; k<sliceList.array.length; ++k) {
	                          if(sliceList.array[k] === undefined) {
	                              chunkIndex = k;
	                              sliceList.array[k] = k;
	                              ++sliceList.count;
	                              console.log("上传切片列表:\n" + sliceList.array);
	                              break;
	                          }
	                     }
	                    start = chunkIndex*chunkSize;
	                    end = chunkIndex+1===chunkNum ? fileInfo.size : start+chunkSize;  //处理文件最后一块切片
	                    blob = blobSlice.call(fileInfo.file, start, end);
	                    fileReader.readAsBinaryString(blob);
	                }).catch((error)=>{
	                	console.error(error);
	                })
	            }
	            fileReader.onerror = function (e) {
	            	showToast("上传失败！文件读取出错.", "error");
	            	console.error(fileReader.error.message);
	            	if(fileReader.readState == FileReader.LOADING) fileReader.abort();
	            	if(--runningReaderNum > 0) uploadError = true;
	            }
	            

	            for(let k=0; k<sliceList.array.length; ++k) {
	            	if(sliceList.array[k] === undefined) {
	            		chunkIndex = k;
	            		sliceList.array[k] = k;
	            		++sliceList.count;
                        console.log("上传切片列表:\n" + sliceList.array);
                        break;
	            	}
	            }
	            let formData, fileBlob;
	            let start = chunkIndex*chunkSize;
	            let end = chunkIndex+1===chunkNum ? fileInfo.size : start+chunkSize;  //处理文件最后一块切片
	            let blob = blobSlice.call(fileInfo.file, start, end);
	            fileReader.readAsBinaryString(blob);
	        	
	        }
        } catch(error) {  //终止文件上传
            console.error("文件上传终止:", error);
            if(--runningReaderNum > 0) uploadError = true;
            //复位
            isUploading = false; 
            uploadError = false;
        } 

    }
    
    const allowRetryCount = 2;  //上传失败重试次数
    function sendFileBlob(formData, resolve, remainRetryCount) {
    	/* let boundary;
        switch(browserKernel) {
        case "WebKit":
            boundary = "----WebKitFormBoundary" + encodeURIComponent((new Date()).getTime().toString(16));
            break;
        case "Gecko":
            boundary = "---------------------------" + encodeURIComponent((new Date()).getTime().toString(16));
            break;
        case "Presto":
            boundary = "--------" + encodeURIComponent((new Date()).getTime().toString(16));
            break;
        case "Trident":
            boundary = "---------------------------" + encodeURIComponent((new Date()).getTime().toString(16));
            break;
        default:
            tips.show("不支持此浏览器的文件上传，请换个浏览器进行操作.");
            return;
        } */
        $.ajax({
            url: prefixPath + "files",
            type: "POST",
            cache: false,
            processData: false,
            contentType: false,  //由$.ajax()自动设定
            //contentType: "multipart/form-data; boundary=" + boundary,
            //contentType: "multipart/form-data", --$.ajax()不需要也不能手动指定contentType
            data: formData, 
            //data: new FormData($('#form_file')[0]), 
            success: function (res) {
                response = JSON.parse(res);
                if(response.result) {
                	console.log("文件块", formData.get("fileBlob").name, "上传成功！");
                	if(response.msg == "ok") uploadFinish = true;
                	resolve();  
                }
                else if(response.msg == "reject"){
                    uploadError = true; //终止文件上传
                    showToast("服务器繁忙，请稍后重试！", "error"); 
                    resolve();  //此调用是为了--readerNumber,从而使开关变量复位
                }
                else if(remainRetryCount-- > 0){ //服务器校验md5不通过
                    console.warn("正在重传文件块:", formData.get("fileBlob").name, "\nMD5:", formData.get("blobMD5"));
                    console.warn("剩余重传次数:", remainRetryCount);
                    sendFileBlob(formData, resolve, remainRetryCount);
                }
                else { //重传次数耗尽
                    uploadError = true; //终止文件上传
                	showToast(response.msg, "error");
                    resolve();  //此调用是为了--readerNumber,从而使开关变量复位
                }
            },
            error: function(xhr, state, error) {
            	if(remainRetryCount-- > 0) {  //网络连接中断
                    console.warn("正在重传文件块:", formData.get("fileBlob").name, "\nMD5:", formData.get("blobMD5"));
                    console.warn("剩余重传次数:", remainRetryCount);
                    sendFileBlob(formData, resolve, remainRetryCount);
            	}
            	else { //网络环境差
            		uploadError = true; //终止文件上传
            		showToast("文件上传失败，网络不稳定，请稍后再试！", "error");
            		console.error(error);  
            		resolve();  //此调用是为了--readerNumber,从而使开关变量复位
            	}
            }
        }); //$.ajax
    }

    function turnPage(currentpage) {

        let pagesize = document.getElementById("pagesize").value;

        if (pagesize > 10 || pagesize >= ${pagebean.totalrecord - pagebean.pagesize * ( pagebean.currentpage - 1 )}) {
            pagesize = Math.min(10, ${pagebean.totalrecord});
            currentpage = 1;
        } else if (pagesize < 1) {
            pagesize = 1;
        }
        window.location.href = prefixPath + 'users/homePage?currentpage=' + currentpage + '&pagesize=' + pagesize;

    }

    function skipPage(currentpage) {
        let pagesize = document.getElementById("pagesize").value;
        if (currentpage > ${pagebean.totalpage}) {
            currentpage = ${pagebean.totalpage};
            pagesize = ${pagebean.pagesize};
        } else if (currentpage < 1) {
            currentpage = 1;
            pagesize = ${pagebean.pagesize};
        }
        window.location.href = prefixPath + 'users/homePage?currentpage=' + currentpage + '&pagesize=' + pagesize;
    }

    /* //下面一段鉴别使用者的浏览器
    var browserCfg = {};
    var ua = window.navigator.userAgent;
    if (ua.indexOf("MSIE") >= 1) {
        browserCfg.ie = true;
    } else if (ua.indexOf("Firefox") >= 1) {
        browserCfg.firefox = true;
    } else if (ua.indexOf("Chrome") >= 1) {
        browserCfg.chrome = true;
    } */
    //判断浏览器内核
    var browserKernel = "Unknown";
    if (navigator.userAgent.indexOf("Trident") != -1) { // IE 内核
        browserKernel = "Trident";
    } else if (navigator.userAgent.indexOf("Presto") != -1) { // Opera 内核
        browserKernel = "Presto";
    } else if (navigator.userAgent.indexOf("AppleWebKit") != -1) { // WebKit 内核
        browserKernel = "WebKit";
    } else if (navigator.userAgent.indexOf("Gecko") != -1 && navigator.userAgent.indexOf("KHTML") == -1) { // Gecko 内核
        browserKernel = "Gecko";
    } 
 
    var uploadFileGroup = document.getElementById("fileupload");
    var vipmaxsize = 10 * 1024 * 1024 * 1024;  //10GB
    var normalmaxsize = 5 * 1024 * 1024 * 1024; //5GB
    var viperrMsg = "VIP用户上传单个文件不能超过10GB！";
    var normalerrMsg = "普通用户上传单个文件不能超过5GB！";
    
    function checkFile() {
        try { 
            if (uploadFileGroup.value == "") return;
            
            fileInfo.file = uploadFileGroup.files[0];
            /* if (browserCfg.firefox || browserCfg.chrome) {
                fileInfo.size = fileInfo.file.size;  //chrome等浏览器支持这个方法拿到文件大小
            } else if (browserCfg.ie) {
                var obj_img = document.getElementById('tempimg');
                obj_img.dynsrc = fileInfo.file.value;
                fileInfo.size = obj_img.fileSize;
            }  */
            if(browserKernel=="WebKit" || browserKernel=="Presto" || browserKernel == "Gecko") {
                fileInfo.size = fileInfo.file.size;
            } 
            else if(browserKernel == "Trident") {  //IE
                var obj_img = document.getElementById('tempimg');
                obj_img.dynsrc = fileInfo.file.value;
                fileInfo.size = obj_img.fileSize;
            }
            else { //其它内核
                tips.show("不支持此浏览器的文件上传，请换个浏览器进行操作.");
                fileInfo.isLegal = false;
                return;
            }
            
            if (fileInfo.size == -1) {
                tips.show("文件大小不合法！");
                fileInfo.isLegal = false;
                return;
            } else if (isvip == 1 && fileInfo.size > vipmaxsize) {
                tips.show(viperrMsg);
                fileInfo.isLegal = false;
                return;
            } else if (isvip == 0 && fileInfo.size > normalmaxsize) {
                tips.show(normalerrMsg);
                fileInfo.isLegal = false;
                return;
            } 
            	
            fileInfo.isLegal = true;
            fileInfo.promise = calculateMD5();  //返回Promise实例
            progressBar.set(0);
        }  catch (e) {
            alert(e);
            return false;
        } 
    }
    
    function calculateMD5() {
        //通过Promise包装
        return new Promise((resolve, reject)=>{   
	         //声明必要的变量
	         let file = fileInfo.file,
	             fileReader = new FileReader(),
	             //文件分割方法（注意兼容性）
	             blobSlice = File.prototype.mozSlice || File.prototype.webkitSlice || File.prototype.slice,
	             //文件每块分割M，计算分割详情
                 chunkSize = 10*1024*1024,  //10MB
	             chunks = Math.ceil(fileInfo.size / chunkSize),
	             currentChunk = 0,
	             spark = new SparkMD5();
	
	         //定义处理单片文件上传的函数
	         function loadNext() {
	             var start = currentChunk * chunkSize,
	                 end = start + chunkSize >= fileInfo.size ? fileInfo.size : start + chunkSize;
	
	             fileReader.readAsBinaryString(blobSlice.call(file, start, end));
	         }
	         
	         //先注册onload(异步事件!)--每块文件读取完毕之后的处理
	         fileReader.onload = function (e) {
	             console.log("正在读取文件：", currentChunk + 1, "/", chunks);
	             //每块交由sparkMD5进行计算
	             spark.appendBinary(e.target.result);
	             currentChunk++;
	
	             //如果文件处理完成计算MD5，如果还有分片继续处理
	             if (currentChunk < chunks) {
	                 loadNext();  //循环
	             } else {
	                 let md5Value = spark.end();  //注意spark.end()每次调用产生不同结果.
	                 $("#md5").val(md5Value); //给hidden-md5字段赋值. 
	                 console.info("文件MD5: ", md5Value); 
	                 console.info("文件大小: ", fileInfo.size); 
	                 resolve(md5Value);
	             }
	         }
	         //在第一次执行loadNext
	         loadNext();
	    });
    }

</script>

</body>
</html>

        
        
        

