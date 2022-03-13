package com.imooc.bilibili.domain.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
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
