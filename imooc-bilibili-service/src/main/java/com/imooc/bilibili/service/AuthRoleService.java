package com.imooc.bilibili.service;

import com.imooc.bilibili.domain.auth.AuthRole;
import com.imooc.bilibili.domain.auth.AuthRoleElementOperation;
import com.imooc.bilibili.domain.auth.AuthRoleMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * 角色-权限相关服务，如查询角色对应有哪些权限
 * @author luf
 * @date 2022/03/10 01:05
 **/
@Service
public class AuthRoleService {

    @Autowired
    private AuthRoleElementOperationService authRoleElementOperationService;
    @Autowired
    private AuthRoleMenuService authRoleMenuService;

    public List<AuthRoleElementOperation> getRoleElementOperationsByRoleIds(Set<Long> roleIdSet) {
         return authRoleElementOperationService.getRoleElementOperationsByRoleIds(roleIdSet);
    }


    public List<AuthRoleMenu> getRoleMenusByRoleIds(Set<Long> roleIdSet) {
        return authRoleMenuService.getRoleMenusByRoleIds(roleIdSet);
    }

    public AuthRole getRoleByCode(String code) {
        return authRoleMenuService.getRoleByCode(code);
    }
}
