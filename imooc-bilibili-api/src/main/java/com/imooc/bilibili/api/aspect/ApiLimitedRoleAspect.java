package com.imooc.bilibili.api.aspect;

import com.imooc.bilibili.api.support.UserSupport;
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
 * 接口的权限控制,切面类,切入点是被注解的方法
 * 1.获取userId 2.获取用户角色role 3.
 * @Aspect：告诉SpringBoot这是一个切面类
 * @Component：告诉SpringBoot这是一个组件
 * @Order(1)：切面的优先级，数字越小优先级越高
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
     *  @Pointcut()：切点表达式，告诉SpringBoot在什么时候切入,这里是在被注解的方法执行时候切入,@annotation()：注解的全路径
     *  check()：切点的方法名，可以随便写，但是要和下面的切点切入后的执行逻辑中的方法名一致,这里是在被注解的方法执行时候切入
     */
    @Pointcut("@annotation(com.imooc.bilibili.domain.annotation.ApiLimitedRole)")
    public void check() {
    }

    // 下面是切点切入后的执行逻辑

    /**
     * 1.检查角色是否有权限操作接口，通过受限制的角色列表来检查
     * @Before()：切点切入后的执行逻辑，这里是在被注解的方法执行之前执行
     * check()作用是告诉SpringBoot在什么时候切入，这里是在被注解的方法执行之前切入
     * 使用&&连接@annotation(apiLimitedRole)：告诉SpringBoot在切入的时候，需要获取到被注解的方法，以用来获取到传入的参数
     * JoinPoint：需要获取到切面中获取到被注解的方法，以用来获取到传入的参数
     * ApiLimitedRole：需要获取到切面中获取到自定义的注解，以用来获取到传入的受限制的角色列表
     */
    @Before("check() && @annotation(apiLimitedRole)")
    public void doBefore(JoinPoint joinPoint, ApiLimitedRole apiLimitedRole) {
        Long userId = userSupport.getCurrentUserId();
        //获取用户角色列表
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        //获取受限制的角色列表
        String[] limitedRoleCodeList = apiLimitedRole.limitedRoleCodeList();
        //比对两个列表，因为set不会存储相同的元素，自带去重
        Set<String> userRoleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        //Arrays.stream(limitedRoleCodeList)：将数组转换为流
        Set<String> limitedRoleCodeSet = Arrays.stream(limitedRoleCodeList).collect(Collectors.toSet());
        //retainAll()：取交集，如果交集为空，说明用户角色列表中没有受限制的角色，如果交集不为空，说明用户角色列表中有受限制的角色,抛出异常
        userRoleCodeSet.retainAll(limitedRoleCodeSet);
        if (userRoleCodeSet.size() > 0) {
            throw new ConditionException("权限不足！");
        }
    }
}
