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
public @interface DataLimited {
}
