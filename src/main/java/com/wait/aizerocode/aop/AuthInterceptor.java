package com.wait.aizerocode.aop;


import com.wait.aizerocode.annotation.AuthCheck;
import com.wait.aizerocode.exception.BusinessException;
import com.wait.aizerocode.exception.ErrorCode;
import com.wait.aizerocode.model.entity.User;
import com.wait.aizerocode.model.enums.UserRoleEnum;
import com.wait.aizerocode.model.vo.UserVO;
import com.wait.aizerocode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;


    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request=((ServletRequestAttributes) requestAttributes).getRequest();


        UserVO loginUser = userService.getUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        //不需要权限的们，直接放行
        if(mustRoleEnum==null)
            return joinPoint.proceed();

        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        if(userRoleEnum==null)
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);

        if(UserRoleEnum.ADMIN.equals(mustRoleEnum)&&!UserRoleEnum.ADMIN.equals(userRoleEnum))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);


        return joinPoint.proceed();


    }


}
