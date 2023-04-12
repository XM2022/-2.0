<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>星空网盘</title>
<link rel="shortcut icon" href="images/xk.ico">
<link rel="icon" type="image/jpg" href="images/xk.ico">
<link rel="stylesheet" type="text/css" href="css/bootstrap.css">
<link rel="stylesheet" href="css/style.css" />
<link rel="stylesheet" href="css/nav.css">
<style type="text/css">
h1, p {
	font-family: "华文楷体", "楷体", "宋体"
}
</style>
</head>
<body>
	<div id="bkgd-img"></div>
	<div id="parts">
		<nav class="top-right"> <a class="disc l1"
			href="${pageContext.request.contextPath}/users/login">
			<div>登录</div>
		</a> <a class="disc l2"
			href="${pageContext.request.contextPath}/users/login#toregister">
			<div>注冊</div>
		</a> <a class="disc l3"
			href="${pageContext.request.contextPath}/users/homePage">
			<div>用户主页</div>
		</a> <a class="disc l4"
			href="${pageContext.request.contextPath}/users/help">
			<div>帮助</div>
		</a> <a id="shine" class="disc l5 toggle"></a> <!-- 用于月辉效果 --> <a
			id="menu" class="disc l5 toggle"> 菜单 </a> </nav>
		<div id="content" class="container center-block">
			<div class="jumbotron">
				<div class="container">
					<h1>星空网盘</h1>
					<p>这是一个兴趣使然的免费个人网盘. 欢迎使用！</p>
					<form action="${pageContext.request.contextPath}/files/search"
						method="post">
						<div class="input-group input-group-lg">
							<span class="input-group-addon" id="sizing-addon1"><span
								class="glyphicon glyphicon-plus" aria-hidden="true"></span></span> <input
								type="text" name="searchcontent" class="form-control"
								id="inputFrame" placeholder="输入*号查询所有共享文件. 支持多关键字，请用空格分隔"
								aria-describedby="sizing-addon1" /> <span
								class="input-group-btn">
								<button id="searchBtn" class="btn btn-default" type="submit">搜
									索</button>
							</span>
						</div>
					</form>
				</div>
			</div>
		</div>
	</div>
	<script src="js/jquery.min.js"></script>
	<script src="js/ios-parallax.js"></script>
	<script src="js/nav.js"></script>
	<script type="text/javascript">
	    console.log('仰望星空，让我们共同奔赴远方...');
	    //背景图动态效果
	    const bkgdImg = document.getElementById("bkgd-img");
        window.onmousemove = function(e) {
            let ny2 = (e.pageX>e.pageY ? e.pageX :e.pageY) / 50 ; 
            bkgdImg.style.backgroundPosition = "0px " + -ny2 + "px";
        }
        var sepPos = location.pathname.lastIndexOf('/');  //方法
        var prefixPath = location.pathname.substring(0, sepPos==0 ? 1 : sepPos+1); //前后都含/
        const inputFrame = document.getElementById("inputFrame");
        const searchButton = document.getElementById("searchBtn");
        searchButton.addEventListener("click", ()=>{
        	let searchContent = inputFrame.value.trim();
        	if(searchContent.length > 0) {
        		window.location.href = prefixPath + "files/search?searchContent=" + searchContent;
        	}
        });
        
	</script>

</body>
</html>