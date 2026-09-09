package com.wait.aizerocode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.wait.aizerocode.constant.UserConstant;
import com.wait.aizerocode.exception.BusinessException;
import com.wait.aizerocode.exception.ErrorCode;
import com.wait.aizerocode.model.dto.user.UserLoginRequest;
import com.wait.aizerocode.model.dto.user.UserQueryRequest;
import com.wait.aizerocode.model.dto.user.UserRegisterRequest;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.mapper.UserMapper;
import com.wait.aizerocode.model.enums.UserRoleEnum;
import com.wait.aizerocode.model.vo.LoginUserVO;
import com.wait.aizerocode.model.vo.UserVO;
import com.wait.aizerocode.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 用户 服务层实现。
 *
 * @author waitwait
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object userObj= request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(userObj==null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未登录");
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true ;
    }

    @Override
    public User getUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(attribute==null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户未登录");
        LoginUserVO loginuserVO = (LoginUserVO)attribute;
        User user = new User();
        BeanUtils.copyProperties(loginuserVO,user);
        return user;
    }

    @Override
    public LoginUserVO getLoginUser(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if(userLoginRequest == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        if(StrUtil.isEmpty(userLoginRequest.getUserAccount())||StrUtil.isEmpty(userLoginRequest.getUserPassword()))
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号或密码不能为空");
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount",userLoginRequest.getUserAccount());
        User user = userMapper.selectOneByQuery(queryWrapper);
        if(user == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"当前帐号为注册");
        //密码加密比对
        String password = getEncryptPassword(userLoginRequest.getUserPassword());
        if(!user.getUserPassword().equals(password))
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码错误");
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user,loginUserVO);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,loginUserVO);

        return loginUserVO;
    }

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        //1 校验数据
        if(userRegisterRequest==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if(userRegisterRequest.getUserAccount().length()<4)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名不能小于4位");
        if(userRegisterRequest.getUserPassword().length()<8||userRegisterRequest.getCheckPassword().length()<8)
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码不能小于8位");
        if(!userRegisterRequest.getCheckPassword().equals(userRegisterRequest.getUserPassword()))
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"密码输入不一致");
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount",userRegisterRequest.getUserAccount());
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if(count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号已经存在");
        }

        //密码加密写到数据库
        String password=getEncryptPassword(userRegisterRequest.getUserPassword());
        User  user=new User();
        user.setUserAccount(userRegisterRequest.getUserAccount());
        user.setUserPassword(password);
        user.setUserName(RandomUtil.randomString(10));
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        if(!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"用户注册失败");
        }

        return user.getId();
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest){
        if (userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole =userQueryRequest.getUserRole();
        String sortField= userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id",id)
                .eq("userRole",userRole)
                .like("userAccount",userAccount)
                .like("userName",userName)
                .like("userProfile",userProfile)
                .orderBy(sortField,"ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptPassword(String password){
        final String salt="wait";
        return DigestUtils.md5DigestAsHex((password+salt).getBytes());
    }

    @Override
    public UserVO getUserVo(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user,userVO);
        return userVO;
    }


    @Override
    public List<UserVO> getUserVOList(List<User> userList){
        if (CollUtil.isEmpty(userList)){
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

}
