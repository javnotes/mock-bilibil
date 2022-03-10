package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserRoleDao;
import com.imooc.bilibili.domain.auth.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户-角色相关服务，如查询用户有哪些角色
 *
 * @author luf
 * @date 2022/03/10 01:04
 **/
@Service
public class UserRoleService {
    @Autowired
    private UserRoleDao userRoleDao;

    public List<UserRole> getUserRolesByUserId(Long userId) {
        return userRoleDao.getUserRolesByUserId(userId);
    }
}
