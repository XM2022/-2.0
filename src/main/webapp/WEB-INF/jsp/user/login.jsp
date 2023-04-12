<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd"><html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>星空网盘</title>
<link rel="shortcut icon" href="../images/xk.ico">
<link rel="icon" type="image/jpg" href="../images/xk.ico">
<link rel="stylesheet" type="text/css" href="../css/demo.css">

<!--必要样式-->
<link rel="stylesheet" type="text/css" href="../css/style4.css">
<link rel="stylesheet" type="text/css" href="../css/animate-custom.css">
<script type="text/javascript" src="../js/jquery-3.2.1.min.js"></script>
<link rel="stylesheet" type="text/css" href="../css/message-box.css">
<script type="text/javascript" src="../js/message-box.js"></script>
</head>
<body>

<div id="container_demo" >
	<!-- hidden anchor to stop jump http://www.css3create.com/Astuce-Empecher-le-scroll-avec-l-utilisation-de-target#wrap4  -->
	<a class="hiddenanchor" id="toregister"></a>
	<a class="hiddenanchor" id="tologin"></a>
	<div id="wrapper">
		<div id="login" class="animate form">
			<form  action="${pageContext.request.contextPath}/users/loginVerify"
			method="POST" autocomplete="on"> 
				<h1>登录</h1> 
				<p> 
					<label for="username" class="uname" data-icon="u" >您的用户名</label>
					<input id="username" name="username" required="required" type="text"/>
				</p>
				<p> 
					<label for="password" class="youpasswd" data-icon="p">你的密码</label>
					<input id="password" name="password" required="required" type="password"/> 
				</p>
				<p class="keeplogin"> 
					<input type="checkbox" name="keepState" id="loginkeeping" value="true" /> 
					<label for="loginkeeping">保持登录状态</label>
				</p>
				<p class="login button"> 
					<!-- <input type="submit" value="登录" />  -->
					<input type="button" value="登录" onclick="login()"/>
				</p>
				<p class="change_link">
					还没有账号?<a href="#toregister" class="to_register">去注册</a>
				</p>
			</form>
		</div>

		<div id="register" class="animate form">
			<form  form action="${pageContext.request.contextPath}/users/register" method="post"  autocomplete="on">
				<h1>注册</h1> 
				<p> 
					<label for="usernamesignup" class="uname" data-icon="u">用户名</label>
					<input id="usernamesignup" name="usernamesignup" required="required" type="text" placeholder="请不要包含特殊字符"/>
				</p>
				<!-- <p> 
					<label for="emailsignup" class="youmail" data-icon="e" >邮箱</label>
					<input id="emailsignup" name="emailsignup" required="required" type="email" placeholder="mysupermail@mail.com"/> 
				</p> -->
				<p> 
					<label for="passwordsignup" class="youpasswd" data-icon="p">密码</label>
					<input id="passwordsignup" name="passwordsignup" required="required" type="password" placeholder="请输入至少6位密码"/>
				</p>
				<p> 
					<label for="passwordsignup_confirm" class="youpasswd" data-icon="p">确认密码</label>
					<input id="passwordsignup_confirm" name="passwordsignup_confirm" required="required" type="password" placeholder="请输入至少6位密码"/>
				</p>
				<p> 
					<label for="VIPcode" class="youpasswd" data-icon="p">VIP邀请码</label>
					<input id="VIPcode" name="VIPcode"  type="text"/>
				</p>
				<p class="signin button"> 
					<!-- <input type="submit" value="注册"/>  -->
				    <input type="button" value="注册" onclick="register()"/>
				</p>
				<p class="change_link">  
					已经注册过?<a href="#tologin" class="to_register"> 去登录 </a>
				</p>
				<br/>
				<br/>
				<br/>
			</form>
		</div>
		
	</div>
	
</div>

<script type="text/javascript">
    /* console.log(${requestScope.usernameerror});
    (function(){
    	showToast(${requestScope.usernameerror}, 'error');
    })(); */
    var sepPos = location.pathname.lastIndexOf('/');
    var prefixPath = location.pathname.substring(0, sepPos==0 ? 1 : sepPos+1); //前后都含/
    function register() {
    	 if($('#passwordsignup').val() != $('#passwordsignup_confirm').val()) {
    		 showToast('密码输入不一致！请重新输入', 'error');
    		 return;
    	 } 
    	 
    	 $.ajax({
            url: location.origin + prefixPath + "register",
            type: "POST",
            data: {
            	usernamesignup: $('#usernamesignup').val(),
            	passwordsignup: $('#passwordsignup').val(),
            	VIPcode: $('#VIPcode').val()
            },
             success: function (res) {  
            	response = JSON.parse(res);
                if (response.result) {
                    showToast(response.msg, "success");
                    window.location.replace(prefixPath + 'homePage');
                }
                else {
                	showToast(response.msg, "error");
                }
            }
    	})
    }
    
    function login() {
    	 $.ajax({
            url: location.origin + prefixPath +'loginVerify',
            type: "POST",
            data: {
            	username: $('#username').val(),
            	password: $('#password').val(),
            },
             success: function (res) {  
            	response = eval('(' + res + ')');
            	response = JSON.parse(res);
                if (response.result) {
                    showToast(response.msg, "success");
                    //使用replace()避免用户回退到登录界面带来不良体验
                    window.location.replace(prefixPath + 'homePage');
                }
                else {
                	showToast(response.msg, "error");
                }
            }
    	})
    }

</script>

</body>
</html>