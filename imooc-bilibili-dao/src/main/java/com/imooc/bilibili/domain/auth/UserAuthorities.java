package com.imooc.bilibili.domain.auth;

import java.util.List;

/**
 * 存放角色对应的所有权限
 */
public class UserAuthorities {

    //前端操作权限列表,包括按钮权限和元素权限
    List<AuthRoleElementOperation> roleElementOperationList;

    //前端页面菜单权限列表,包括菜单权限和页面权限
    List<AuthRoleMenu> roleMenuList;
    //...


    public List<AuthRoleElementOperation> getRoleElementOperationList() {
        return roleElementOperationList;
    }

    public void setRoleElementOperationList(List<AuthRoleElementOperation> roleElementOperationList) {
        this.roleElementOperationList = roleElementOperationList;
    }

    public List<AuthRoleMenu> getRoleMenuList() {
        return roleMenuList;
    }

    public void setRoleMenuList(List<AuthRoleMenu> roleMenuList) {
        this.roleMenuList = roleMenuList;
    }
}
