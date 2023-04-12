package priv.xm.xkcloud.controller;

import static priv.xm.xkcloud.message.Message.INPUT_IS_EMPTY;
import static priv.xm.xkcloud.message.Message.LOGIN_FAIL;
import static priv.xm.xkcloud.message.Message.LOGIN_SUCCESS;
import static priv.xm.xkcloud.message.Message.PASSWORD_LENGTH_ILLEAGAL;
import static priv.xm.xkcloud.message.Message.REGISTER_SUCCESS;
import static priv.xm.xkcloud.message.Message.SERVER_ERROR;
import static priv.xm.xkcloud.message.Message.USER_NAME_ALREADY_EXIST;
import static priv.xm.xkcloud.message.Message.USER_NAME_IS_EMPTY;

import java.util.List;
import java.util.Objects;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import priv.xm.xkcloud.model.File;
import priv.xm.xkcloud.model.Page;
import priv.xm.xkcloud.model.PageBean;
import priv.xm.xkcloud.model.User;
import priv.xm.xkcloud.service.FileService;
import priv.xm.xkcloud.service.UserService;
import priv.xm.xkcloud.util.CommonUtil;

@Controller
@RequestMapping("/users")
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    FileService fileService;
    
    /**
     * 注册:用户名必须确保唯一性, 否则当多个用户名和密码相同时将无法区分!
     */
    @RequestMapping("/register")
    @ResponseBody
    public String register(String usernamesignup,String passwordsignup, String VIPcode, HttpSession session, HttpServletRequest req, HttpServletResponse rep){
        try {
            if(usernamesignup == null || usernamesignup.length()==0) {
                return CommonUtil.toJson(false, USER_NAME_IS_EMPTY);
            }
            if(userService.existUser(usernamesignup)) {   //用户名重复性检查
                return CommonUtil.toJson(false, USER_NAME_ALREADY_EXIST);
            }
            if (passwordsignup==null || passwordsignup.length()<6 || 16<passwordsignup.length()) {
                return CommonUtil.toJson(false, PASSWORD_LENGTH_ILLEAGAL);
            } 
            
            User newUser = null;
            newUser = Objects.equals(VIPcode, "VIP") ? new User(usernamesignup, passwordsignup, 1)
                    : new User(usernamesignup, passwordsignup, 0);
            userService.createUser(newUser);
            
            //将用户名存入seesion免登陆, 直接进入用户空间
            session.setMaxInactiveInterval(120);  //2分钟仅用于演示; 正式环境30分钟为宜
            session.setAttribute("user_name", usernamesignup);
        }catch (Exception e) {
            dealException(e, req);
        }
        return CommonUtil.toJson(true, REGISTER_SUCCESS);
    }

    /**登陆功能*/
    @RequestMapping("/loginVerify")
    @ResponseBody
    public String loginVerify(User user, String keepState, HttpSession session, HttpServletRequest req, HttpServletResponse response) {
        try {
            if(user.getUsername()==null || user.getPassword() == null) {
                return CommonUtil.toJson(false, INPUT_IS_EMPTY);
            }
            if (userService.verifyFailUserIdentity(user)) {
                /*req.setAttribute("error", "JSP:用户名或密码错误");
                req.getRequestDispatcher("login").forward(req, rep);*/
                return CommonUtil.toJson(false, LOGIN_FAIL);
            }
 
        } catch (Exception e) {
            e.printStackTrace();
            return CommonUtil.toJson(false, SERVER_ERROR);
        }
        //登陆成功
        if(Objects.equals(keepState, "true")) {  //勾选了"保持登陆"选项
            Cookie cookie = new Cookie("JSESSIONID", session.getId());  //同名覆盖
            cookie.setMaxAge(7*24*3600);  //保持7天登陆状态--浏览器关闭依然不会丢失
            session.setMaxInactiveInterval(7*24*3600);  //相应的,session也要设置为7天
            response.addCookie(cookie);
        }
        else {
            session.setMaxInactiveInterval(120);  //2分钟仅用于演示; 正式环境30分钟为宜
        }
        //把用户名存入session域, 供前端显示和验证身份使用.
        session.setAttribute("user_name", user.getUsername());
        return CommonUtil.toJson(true, LOGIN_SUCCESS);
    }

    /**
     * 用户主页
     * @param progress 仅用作页面刷新传递进度条信息
     */
    @RequestMapping("/homePage")
    public String searchFiles(HttpSession session, Page page, HttpServletRequest req, Model model, Integer progress) throws Throwable {
        if(userService.verifyFailUserLogState(session))  return "user/login";
        
        String sessionUserName = (String) session.getAttribute("user_name");
        Integer isvip = (Integer) req.getAttribute("isvip");
        if (isvip == null) {  //没有上传文件之前会调用到这里的代码，上传的时候在uploadAction里会添加isvip
            try {
                isvip = userService.isVIP(sessionUserName) ? 1 :0;
                //将vip的信息传到userhome主页
                req.setAttribute("isvip", isvip);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        //拿到每页的数据，每个元素就是一条记录
        page.setUser_id(userService.findUserID(sessionUserName));
        List<File> list = fileService.getUserFiles(page);
        int totalRecord = fileService.countUserFiles(sessionUserName);
        int pageSize = page.getPagesize(); 
        int currentPage = page.getCurrentpage();
        model.addAttribute("pagebean", new PageBean(list, totalRecord, pageSize, currentPage));
        double usedSpace = userService.findUserUsedSpace(sessionUserName)/1024.0;
        req.setAttribute("usedSpace", usedSpace);
        return "user/userhome";
    }
    
    @RequestMapping("/login")
    public String login() {
        return "user/login";
    }
    
    @RequestMapping("/help")
    public String help(){
        return"user/help";
    }
    
    @RequestMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();  //使session失效
        return "redirect:/home";  //重定向SpringMVC会自动追加server.context-path(xkcloud)
    }

    private void dealException(Exception e, HttpServletRequest req) {
        e.printStackTrace();
        req.setAttribute("globalmessage", "服务器开小差了...");
    }

}
