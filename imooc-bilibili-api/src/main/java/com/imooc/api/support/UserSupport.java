package com.imooc.api.support;

import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.UserService;
import com.imooc.bilibili.service.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author luf
 * @date 2022/03/05 11:56
 **/
@Component
public class UserSupport {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户的 userId，通过验证 token 的方式(调用方法 verifyToken)，而token是来源在请求的请求头
     */
    public Long getCurrentUserId() {
        // RequestContextHolder.getRequestAttributes()：框架提供的，抓取请求上下文的方法
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();

        String token = request.getHeader("token");
        Long userId = TokenUtil.verifyToken(token);

        if (userId < 1) {
            throw new ConditionException("非法用户");
        }
        return userId;
    }
}
