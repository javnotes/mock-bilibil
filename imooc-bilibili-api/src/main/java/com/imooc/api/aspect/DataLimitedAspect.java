package com.imooc.api.aspect;

import com.imooc.api.support.UserSupport;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.domain.auth.UserRole;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.UserRoleService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用动态类型UserMoment.type进行权限控制
 *
 * @author luf
 * @date 2022/03/13 08:57
 **/
@Order(1)
@Component
@Aspect
public class DataLimitedAspect {
    @Autowired
    private UserSupport userSupport;
    @Autowired
    private UserRoleService userRoleService;

    /**
     * 切点：告诉SpringBoot在什么时候切入，注解被执行时
     */
    @Pointcut("@annotation(com.imooc.bilibili.domain.annotation.DataLimited)")
    public void check() {
    }

    // 切点切入后的执行逻辑

    /**
     * 权限限制：UserMoment.type，其中视频投稿type=0
     */
    @Before("check()")
    public void doBefore(JoinPoint joinPoint) {
        Long userId = userSupport.getCurrentUserId();
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        Set<String> userRoleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        // 获取当前切到的方法中的参数
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UserMoment) {
                UserMoment userMoment = (UserMoment) arg;
                String type = userMoment.getType();
                if (userRoleCodeSet.contains(AuthRoleConstant.ROLE_LV0) && !"0".equals(type)) {
                    // 角色为Lv0且动态类型为0，则该用户有权限
                    throw new ConditionException("权限不足！");
                }
            }
        }
    }
}
