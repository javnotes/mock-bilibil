package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserAuthDao;
import com.imooc.bilibili.domain.auth.UserAuthorities;
import com.imooc.bilibili.domain.auth.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author luf
 * @date 2022/03/10 00:55
 **/
@Service
public class UserAuthService {

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private AuthRoleService authRoleService;

    @Autowired
    private UserAuthDao userAuthDao;

    public UserAuthorities getUserAuthorities(Long userId) {
        //一个用户可以有多种角色，通过用户Id查询该用户有哪些角色
        List<UserRole> userRoles = userRoleService.getUserRolesByUserId(userId);

    }
}
