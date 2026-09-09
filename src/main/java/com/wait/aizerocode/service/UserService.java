package com.wait.aizerocode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.wait.aizerocode.model.dto.user.UserLoginRequest;
import com.wait.aizerocode.model.dto.user.UserQueryRequest;
import com.wait.aizerocode.model.dto.user.UserRegisterRequest;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.vo.LoginUserVO;
import com.wait.aizerocode.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author waitwait
 */
public interface UserService extends IService<User> {

    boolean userLogout(HttpServletRequest request);
    //获取当前用户
    User getUser(HttpServletRequest request);

    //用户登录
    LoginUserVO getLoginUser(UserLoginRequest userLoginRequest, HttpServletRequest request);

    long userRegister(UserRegisterRequest userRegisterRequest);

    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String password);

    UserVO getUserVo(User user);

    List<UserVO> getUserVOList(List<User> userList);
}
