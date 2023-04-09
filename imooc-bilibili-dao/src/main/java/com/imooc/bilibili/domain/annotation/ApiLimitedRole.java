package com.imooc.bilibili.domain.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 针对角色的接口权限, 用于标记接口是否受限制, 受限制的角色列表
 * @interface是一个标记注解，用于标记某个类是一个注解类型
 * @Documented：说明该注解将被包含在javadoc中
 * @Target({ElementType.METHOD})：说明该注解可以被添加在方法上
 * @Retention(RetentionPolicy.RUNTIME)：说明该注解将被保留到运行时
 * 使用：@ApiLimitedRole(limitedRoleCodeList = {AuthRoleConstant.ROLE_LV0})
 * @author luf
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@Component
public @interface ApiLimitedRole {
    /**
     * 角色编码列表：受限制的角色列表
     */
    String[] limitedRoleCodeList() default {};
}
