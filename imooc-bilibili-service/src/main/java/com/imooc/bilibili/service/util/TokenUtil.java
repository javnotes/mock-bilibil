package com.imooc.bilibili.service.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;

import java.util.Calendar;
import java.util.Date;

/**
 * 生成用户令牌，表明身份，因为直接传输userId不太安全
 *
 * @author luf
 * @date 2022/03/03 23:44
 **/
public class TokenUtil {

    private static final String ISSURE = UserConstant.TOKEN_ISSURE;

    /**
     * 创建(用户令牌) JWT
     */
    public static String generateToken(Long userId) throws Exception {
        Algorithm algorithm = Algorithm.RSA256(RSAUtil.getPublicKey(), RSAUtil.getPrivateKey());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.HOUR, 1);//过期时间
        return JWT.create().withKeyId(String.valueOf(userId))
                .withIssuer(ISSURE)
                .withExpiresAt(calendar.getTime())
                .sign(algorithm);
    }

    /**
     * 验证token，返回userId
     */
    public static Long verifyToken(String token) {
        try {//不直接返回异常，可能可以刷新异常
            Algorithm algorithm = Algorithm.RSA256(RSAUtil.getPublicKey(), RSAUtil.getPrivateKey());
            // 生成验证类
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);
            String userId = jwt.getKeyId();
            return Long.valueOf(userId);
        } catch (TokenExpiredException e) {
            throw new ConditionException("555", "token过期！");
        } catch (Exception e) {
            throw new ConditionException("非法用户token！");
        }
    }

    /**
     * 双token，刷新令牌
     */

    public static String generateRefreshToken(Long userId) throws Exception {
        Algorithm algorithm = Algorithm.RSA256(RSAUtil.getPublicKey(), RSAUtil.getPrivateKey());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.DAY_OF_MONTH, 5);
        return JWT.create().withKeyId(String.valueOf(userId))
                .withIssuer(ISSURE)
                .withExpiresAt(calendar.getTime())
                .sign(algorithm);
    }
}
