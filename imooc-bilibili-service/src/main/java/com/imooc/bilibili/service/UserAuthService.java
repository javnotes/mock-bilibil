package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserAuthDao;
import com.imooc.bilibili.domain.auth.*;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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


    public UserAuthorities getUserAuthorities(Long userId) {
        //一个用户可以有多种角色，通过用户Id查询该用户有哪些角色
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        // 该用户所有角色的id，roleId
        Set<Long> roleIdSet = userRoleList.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        //根据roleId查询页面级的访问权限
        List<AuthRoleElementOperation> roleElementOperationList = authRoleService.getRoleElementOperationsByRoleIds(roleIdSet);
        //根据roleId查询页面菜单按钮的操作权限
        List<AuthRoleMenu> roleMenuList = authRoleService.getAuthRoleMenusByRoleIds(roleIdSet);

        UserAuthorities userAuthorities = new UserAuthorities();
        userAuthorities.setRoleElementOperationList(roleElementOperationList);
        userAuthorities.setRoleMenuList(roleMenuList);
        return userAuthorities;
    }

/**
 * 添加用户默认权限角色
 */
//    public void addUserDefaultRole(Long userId) {
//        UserRole userRole = new UserRole();
//        //查询默认角色Id
//        AuthRole role = authRoleService.getRoleByCode(AuthRoleConstant.ROLE_LV0);
//        userRole.setUserId(userId);
//        userRole.setRoleId(role.getId());
//        userRoleService.addUserRole(userRole);
//    }
}
