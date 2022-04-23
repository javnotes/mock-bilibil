package com.imooc.bilibili.service;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserDao;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.RefreshTokenDetail;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.MD5Util;
import com.imooc.bilibili.service.util.RSAUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import com.mysql.cj.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author luf
 * @date 2022/03/03 21:08
 **/
@Service
public class UserService {

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserAuthService userAuthService;

    /**
     * 用户注册，指定默认角色
     */
    public void addUser(User user) {
        String phone = user.getPhone();
        if (StringUtils.isNullOrEmpty(phone)) {
            throw new ConditionException("手机号不能为空！");
        }
        User dbUser = this.getUserByPhone(phone);
        if (dbUser != null) {
            throw new ConditionException("该手机号已注册，请使用手机号登录！");
        }

        Date now = new Date();
        // 对注册时填写的密码解密(前端RSA加密)，再加密(存至数据库)
        String salt = String.valueOf(now.getTime());
        String password = user.getPassword();
        String rawPassword;
        try {
            rawPassword = RSAUtil.decrypt(password);
        } catch (Exception e) {
            throw new ConditionException("密码解析失败！");
        }
        // md5加密密码，即不以明文的方式存储密码
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        user.setSalt(salt);
        user.setPassword(md5Password);
        user.setCreateTime(now);
        userDao.addUser(user);

        //需要关联处理用户信息表，注册时一些信息给予默认值
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(user.getId());
        userInfo.setNick(UserConstant.DEFAULT_NICK);
        userInfo.setBirth(UserConstant.DEFAULT_BIRTH);
        userInfo.setGender(UserConstant.GENDER_MALE);
        userInfo.setCreateTime(now);
        userDao.addUserInfo(userInfo);

        //添加新用户的默认角色权限
        userAuthService.addUserDefaultRole(user.getId());
    }

    User getUserByPhone(String phone) {
        return userDao.getUserByPhone(phone);
    }

    /**
     * 用户登录
     */
    public String login(User user) throws Exception {

        String phone = user.getPhone();
        if (StringUtils.isNullOrEmpty(phone)) {
            throw new ConditionException("手机号不能为空！");
        }
        // 验证账号
        User dbUser = this.getUserByPhone(phone);
        if (dbUser == null) {
            throw new ConditionException("当前用户不存在！");
        }
        // 验证密码，此时存在前端传来的user，和保存在数据库中的dbUser
        String password = user.getPassword();
        String rawPassword;
        try {
            rawPassword = RSAUtil.decrypt(password);
        } catch (Exception e) {
            throw new ConditionException("密码解析失败！");
        }
        // 数据库中保存的是注册时，md5加密后的密码，所以登录时需要将用户填写的密码加密后与数据库中保存的密码去对比验证
        String salt = dbUser.getSalt();
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        if (!md5Password.equals(dbUser.getPassword())) {
            throw new ConditionException("密码错误！");
        }
        // 此时密码正确
        return TokenUtil.generateToken(dbUser.getId());
    }

    /**
     * 获取用户信息
     * 注意：user表中的id就是userId，也就是UserInfo中的userId
     **/
    public User getUserInfo(Long userId) {
        // 根据userId，查询此用户
        User user = userDao.getUserById(userId);
        UserInfo userInfo = userDao.getUserInfoByUserId(userId);
        user.setUserInfo(userInfo);
        return user;
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfos(UserInfo userInfo) {
        userDao.updateUserInfos(userInfo);
    }

    /**
     * 根据id获取用户
     */
    public User getUserById(Long id) {
        return userDao.getUserById(id);
    }

    public List<UserInfo> getUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.getUserInfoByUserIds(userIdList);
    }

    /**
     * 实际的分页查询方法（查询用户列表）
     */
    public PageResult<UserInfo> pageListUserInfos(JSONObject params) {
        //计算在数据中的查询时的起始位置和数据条数(查询多少条数据)
        Integer pageNum = params.getInteger("pageNum");
        Integer pageSize = params.getInteger("pageSize");
        params.put("pageStart", (pageNum - 1) * pageSize);
        params.put("pageLimit", pageSize);
        // 先获取符合条件的总记录数
        Integer total = userDao.pageCountUserInfos(params);
        List<UserInfo> list = new ArrayList<>();
        if (total > 0) {
            //pageListUserInfos：真正的分页查询
            list = userDao.pageListUserInfos(params);
        }
        return new PageResult<>(total, list);
    }

    /**
     * 双token登录
     *
     * @param user
     * @return
     */
    public Map<String, Object> loginForDts(User user) throws Exception {

        String phone = user.getPhone();
        if (StringUtils.isNullOrEmpty(phone)) {
            throw new ConditionException("手机号不能为空！");
        }
        // 验证账号
        User dbUser = this.getUserByPhone(phone);
        if (dbUser == null) {
            throw new ConditionException("当前用户不存在！");
        }
        // 验证密码，此时存在前端传来的user，和保存在数据库中的dbUser
        String password = user.getPassword();
        String rawPassword;
        try {
            rawPassword = RSAUtil.decrypt(password);
        } catch (Exception e) {
            throw new ConditionException("密码解析失败！");
        }
        // 数据库中保存的是注册时，md5加密后的密码，所以登录时需要将用户填写的密码加密后与数据库中保存的密码去对比验证
        String salt = dbUser.getSalt();
        String md5Password = MD5Util.sign(rawPassword, salt, "UTF-8");
        if (!md5Password.equals(dbUser.getPassword())) {
            throw new ConditionException("密码错误！");
        }
        // 开始双token
        Long userId = dbUser.getId();
        String accessToken = TokenUtil.generateToken(userId);
        String refreshToken = TokenUtil.generateRefreshToken(userId);
        // 保存 refreshToken 至数据库
        userDao.deleteRefreshTokenByUserId(userId);
        userDao.addRefreshToken(refreshToken, userId, new Date());
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", accessToken);
        result.put("refreshToken", refreshToken);
        return result;
    }

    /**
     * 用户退出：删除该用户的refreshToken
     */
    public void logout(String refreshToken, Long userId) {
        userDao.deleteRefreshToken(refreshToken, userId);
    }

    /**
     * 根据（数据库中的）refreshToken（中的userId）来重新生成accessToken
     */
    public String refreshAccessToken(String refreshToken) throws Exception {
        RefreshTokenDetail refreshTokenDetail = userDao.getRefreshTokenDetail(refreshToken);
        if (refreshTokenDetail == null) {
            throw new ConditionException("555", "token过期！");
        }
        Long userId = refreshTokenDetail.getUserId();
        return TokenUtil.generateToken(userId);
    }

    /**
     * 通过用户id，批量查询用户信息
     */
    public List<UserInfo> batchGetUserInfoByUserIds(Set<Long> userIdList) {
        return userDao.batchGetUserInfoByUserIds(userIdList);
    }
}
