package priv.xm.xkcloud.service;

import javax.servlet.http.HttpSession;

import priv.xm.xkcloud.model.User;

public interface UserService {

    boolean verifyFailUserIdentity(User user) throws Exception;

    void createUser(User user) throws Exception;

    int findUserID(String username) throws Exception;

    boolean existUser(String username) throws Exception;

    boolean isVIP(String userName) throws Exception;

    User findUserById(int id) throws Exception;

    String findUserNameById(int id) throws Exception;

    int findUserUsedSpace(String userName) throws Exception;

    int findUserStartId() throws Exception;

    boolean verifyFailUserLogState(HttpSession session);

    void deleteUser(int id);

}