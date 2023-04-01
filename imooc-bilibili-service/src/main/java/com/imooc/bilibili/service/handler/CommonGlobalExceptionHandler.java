package com.imooc.bilibili.service.handler;

import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.exception.ConditionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器：@ControllerAdvice 配合 @ExceptionHandler 实现全局异常处理
 * Ordered.HIGHEST_PRECEDENCE：最高优先级
 * @author luf
 * @date 2022/03/03 20:41
 **/
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonGlobalExceptionHandler {
    /**
     * 全局异常处理器
     * HttpServletRequest 封装的前端传来的请求
     */
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public JsonResponse<String> commonExceptionHandler(HttpServletRequest request, Exception e) {
        String errorMsg = e.getMessage();
        if (e instanceof ConditionException) {
            String errorCode = ((ConditionException) e).getCode();
            // 在抛出异常时，返回响应数据JsonResponse
            return new JsonResponse<>(errorCode, errorMsg);
        } else {
            return new JsonResponse<>("500", errorMsg);
        }
    }
}
