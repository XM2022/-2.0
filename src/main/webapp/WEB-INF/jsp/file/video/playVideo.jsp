<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>播放视频</title>
<link rel="shortcut icon" href="../../images/xk.ico">
<link rel="icon" type="image/jpg" href="../../images/xk.ico">
<script type="text/javascript" src="../../js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" src="../../js/hls.min.js"></script>

<link rel="stylesheet" href="../../css/dialog.css">
<script type="text/javascript" src="../../js/dialog.js"></script>
<link rel="stylesheet" href="../../css/progress2.css">
<script type="text/javascript" src="../../js/progress.js"></script>
<style type="text/css">
	html, body {
	    width: 100%;
	    height: 100%;
	    margin: 0;   /* 不清零页面有滑动条产生 */
	}
	video{
	    position: relative;
	    top: 47%;
	    left: 50%;
	    transform: translate(-50%, -50%);
	}
    body>div:first-of-type {
        width: 55%;
        position: absolute;
        left: 22%;
        bottom: 5%; 
    }
</style>
</head>

<body>
    <video id='v' width="55%" controls autoplay preload="auto" ></video>
    <input type="hidden" id="m3u8_address" value="${requestScope.m3u8}">
    <input type="hidden" id="video_size" value="${requestScope.videoSize}">
    <div>
	    <!-- 加载进度条 -->
	    <div id="progress">
	        <div id="progress-bar">0%</div>
	    </div>
    </div>
	<script type="text/javascript">
	    //创建选择对话框
	    var dialog = new ModelBox({
	        id:3,
	        title:"提示",
	        confirm:"我已知晓",
	        isShowCancel:false
	     });
	    //创建进度条
	    var progressBar = getProgress({
	        id: "progress",  //进度条轨道id
	        barId: "progress-bar",  //进度条id
	        width: '100%',  //进度条长度
	        height: '85%',  //进度条高度
	        box: false,     //true:方型  false/null:圆角
	    });
	    var  video = document.getElementById('v');
	    var  m3u8_address = document.getElementById("m3u8_address").value;
	    var  videoSize = document.getElementById("video_size").value;
	    //进度条伪刷新
	    var counter = 0;
	    var timer = setInterval(()=>{
	    	counter += 100;    //默认服务器1S解析100MB视频.
	    	if(counter < videoSize) progressBar.set(Math.floor(counter/videoSize*100));  //需要使用floor避免提前出现100%的情况.
	    }, 1000);  //每隔1秒刷新一次进度条.
	    
	    //从m3u8_address中获取fileMd5
	    //const separator = getSystemSeparator();
	    var fileMd5 = m3u8_address.substring(0, m3u8_address.indexOf('\\'));   //windows上的服务器
	    var sepPos = location.pathname.lastIndexOf('/');  //id
	    sepPos = location.pathname.lastIndexOf('/', sepPos-1);  //onlinePlay
	    var prefixPath = location.pathname.substring(0, sepPos+1); //前后都含/
	   	$.ajax({ 
	           //url: prefixPath + 'getVideoSliceResult?fileMd5=' + fileMd5,
	           url: location.origin + prefixPath + 'getVideoSliceResult?fileMd5=' + fileMd5,
	           type: "GET",
	           success: function (res) {  
	        	   response = JSON.parse(res);
	               if (!response.result) { //解析失败
	                   dialog.show(response.msg);
	                   return;
	               } 
		           if(Hls.isSupported()) {
	                    clearInterval(timer);
	                    progressBar.set(100);
		                var hls = new Hls();
		                hls.loadSource(location.origin + prefixPath + 'video/' + m3u8_address);
		                hls.attachMedia(video);
		                //浏览器禁止js自动播放行为
		                /* hls.on(Hls.Events.MANIFEST_PARSED,function() {
		                  video.play();
		                }); */
                   }
                   else if (video.canPlayType('application/vnd.apple.mpegurl')) {  //尝试使用浏览器原生播放器
	                     clearInterval(timer);
	                     progressBar.set(100);
	                     video.src = m3u8_address;
	                     /* video.addEventListener('loadedmetadata',function() {
	                       video.play();
	                     }); */
                   } 
                   else {
                       dialog.show("您的浏览器不支持播放该视频！");
                   }
	           }
	    });
	   	
	   	/* function getSystemSeparator() {
	   		let separator;
	   		const userAgent = navigator.userAgent.toLowerCase();
	   		if (userAgent.indexOf('win') !== -1) {
	   		    separator = '\\';
	   		} else {
	   		    separator = '/';
	   		}
	   		return separator;
	   	} */
	    
	</script>
</body>
</html>