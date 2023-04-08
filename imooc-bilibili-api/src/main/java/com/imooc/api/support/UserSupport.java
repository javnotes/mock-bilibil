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
 * 通过验证 token 的方式，获取当前用户的 userId
 *
 * @author luf
 * @date 2022/03/05 11:56
 **/
@Component
public class UserSupport {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户的 userId，通过验证 token 的方式(调用方法 verifyToken)，而token是来源在浏览器请求的请求头
     */
    public Long getCurrentUserId() {
        // 获取当前请求的请求头，从中获取 token，再通过 token 获取 userId，最后返回 userId
        //RequestContextHolder.getRequestAttributes()：SpringBoot 框架提供的，抓取请求上下文的方法
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        //获取请求头
        HttpServletRequest request = requestAttributes.getRequest();

        //获取请求头中的token
        String token = request.getHeader("token");
        //`verifyToken`方法：验证 token，返回 userId
        Long userId = TokenUtil.verifyToken(token);

        if (userId < 1) {
            throw new ConditionException("非法用户");
        }
        return userId;
    }

    //验证刷新令牌
    private void verifyRefreshToken(Long userId){
        //getRequestAttributes()：SpringBoot 框架提供的，抓取请求上下文的方法
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        //获取请求头中的刷新令牌
        String refreshToken = requestAttributes.getRequest().getHeader("refreshToken");
        //根据用户id获取数据库中的刷新令牌
        String dbRefreshToken = userService.getRefreshTokenByUserId(userId);
        if(!dbRefreshToken.equals(refreshToken)){
            throw new ConditionException("非法用户！");
        }
    }

}
