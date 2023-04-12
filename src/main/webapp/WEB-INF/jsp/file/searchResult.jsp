<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>星空网盘－搜索结果</title>
<link rel="shortcut icon" href="../images/xk.ico">
<link rel="icon" type="image/jpg" href="../images/xk.ico">
<link rel="stylesheet" href="../css/bootstrap4.1.1.min.css">
<link rel="stylesheet" href="../css/nav.css">
<link rel="stylesheet" href="../css/dialog.css">
<style type="text/css">
    .even {
        background-color: pink
    }
    
    .old {
        background-color: yellow
    }
	a {
	    font-size: 1.5em
	}
	tr.table-info:nth-of-type(-n+3){  /* 选择前三个td */
	    font-size:1.2em
	}
	#music2 {
        visibility: hidden;
        height: 5%;
        width: 50%;
        size: 50%;
        position: absolute;
        top: 11%;
        right: 15%;
    }
</style>
<script type="text/javascript" src="../js/dialog.js"></script>
</head>
<body>

	<a href="${pageContext.request.contextPath}/home">首页</a>
	<div style="text-align: center"></div>
	<br /><h3 align="left"><font size="100%">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;搜索结果</font></h3><br /> <br />
    <audio id="music2" preload="auto" controls autoplay>
        <source/>
        您的浏览器不支持播放此音频！
    </audio>
    <input type="hidden" id="searchContent" value="${requestScope.searchcontent}"/>

<div style="text-align: center">
    <table class="table table-hover table-borderless" frame="border" width="100%" align="center">
        <thead class="thead bg-success">
        <tr>
            <th scope="col">文件名</th>
            <th scope="col">文件大小</th>
            <th scope="col">上传日期</th>
            <th scope="col">下载文件</th>
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
                           href="${pageContext.request.contextPath}/files/download?id=${c.id }&filename=${c.filename }">下载</a>
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
	<input type="hidden" id="searchcontent" value="${searchcontent}">
</div>

<script type="text/javascript">
    const searchContent = document.getElementById("searchContent");
    var sepPos = location.pathname.lastIndexOf('/');  //方法
    
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
        case 'rmvb': case 'avi': case 'webm':
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
        
    function openFile(fileId) {
        window.open(prefixPath + "open/" + fileId);  //resultful风格
    }
    
    const audio = document.getElementById("music2");
    function playAudio(fileId) {
        //显示隐藏的音频播放条
        audio.style.visibility = "visible"; 
        audio.src = prefixPath + "open/" + fileId; 
    }
    
    function playVideoWithInform(fileId) {
        tips.show("此视频格式可能无法在线播放或存在加载时间较长的现象.<br>如果出现上述情况，请下载到本地打开.")
        setTimeout(()=>{
            playVideo(fileId);
        }, 2000);
    }
    function playVideo(fileId) {
        window.open(prefixPath + "onlinePlay/" + fileId); 
    }
    
    function turnPage(currentpage) {
       let pagesize = document.getElementById("pagesize").value;
       var searchcontent = document.getElementById("searchcontent").value;
       if (pagesize > 10 || pagesize >= ${pagebean.totalrecord - pagebean.pagesize * ( pagebean.currentpage - 1 )}) {
           pagesize = Math.min(10, ${pagebean.totalrecord});
           currentpage = 1;
       } else if (pagesize < 1) {
           pagesize = 1;
       }
       window.location.href = prefixPath + 'search?searchcontent=' + searchContent.value + '&currentpage=' + currentpage + '&pagesize=' + pagesize;

   }

   function skipPage(currentpage) {
       let pagesize = document.getElementById("pagesize").value;
       var searchcontent = document.getElementById("searchcontent").value;
       if (currentpage > ${pagebean.totalpage}) {
           currentpage = ${pagebean.totalpage};
           pagesize = ${pagebean.pagesize};
       } else if (currentpage < 1) {
           currentpage = 1;
           pagesize = ${pagebean.pagesize};
       }
       window.location.href = prefixPath + 'search?searchcontent=' + searchContent.value + '&currentpage=' + currentpage + '&pagesize=' + pagesize;
   }
</script>
</body>
</html>
