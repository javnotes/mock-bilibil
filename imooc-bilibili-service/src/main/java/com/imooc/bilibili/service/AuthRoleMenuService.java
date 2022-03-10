package com.imooc.bilibili.service;

import com.imooc.bilibili.domain.auth.AuthRole;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/10
 **/
@Service
public class AuthRoleMenuService {
    public List<AuthRoleMenu> getRoleMenusByRoleIds(Set<Long> roleIdSet) {
        
    }

    public AuthRole getRoleByCode(String code) {
    }
}
