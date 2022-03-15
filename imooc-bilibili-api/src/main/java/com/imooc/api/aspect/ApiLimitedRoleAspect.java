package com.imooc.api.aspect;

import com.imooc.api.support.UserSupport;
import com.imooc.bilibili.domain.annotation.ApiLimitedRole;
import com.imooc.bilibili.domain.auth.UserRole;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.UserRoleService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 1.获取userId 2.获取用户角色role 3.
 * @author luf
 * @date 2022/03/13 08:57
 **/
@Order(1)
@Component
@Aspect
public class ApiLimitedRoleAspect {
    @Autowired
    private UserSupport userSupport;
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 切点：告诉SpringBoot在注解被执行时候切入
     */
    @Pointcut("@annotation(com.imooc.bilibili.domain.annotation.ApiLimitedRole)")
    public void check() {
    }

    // 切点切入后的执行逻辑

    /**
     * 1.检查角色是否有权限操作接口，通过受限制的角色列表来检查
     * @annotation(apiLimitedRole)：需要获取到切面中获取到自定义的注解，以用来获取到传入的受限制的角色列表
     */
    @Before("check() && @annotation(apiLimitedRole)")
    public void doBefore(JoinPoint joinPoint, ApiLimitedRole apiLimitedRole) {
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        String[] limitedRoleCodeList = apiLimitedRole.limitedRoleCodeList();
        //比对两个列表，因为set不会存储相同的元素，自带去重
        Set<String> userRoleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        Set<String> limitedRoleCodeSet = Arrays.stream(limitedRoleCodeList).collect(Collectors.toSet());
        //取交集
        userRoleCodeSet.retainAll(limitedRoleCodeSet);
        if (userRoleCodeSet.size() > 0) {
            throw new ConditionException("权限不足！");
        }
    }
}
