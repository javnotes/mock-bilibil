package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.AuthRoleElementOperationDao;
import com.imooc.bilibili.domain.auth.AuthRoleElementOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/10
 **/
@Service
public class AuthRoleElementOperationService {
    @Autowired
    private AuthRoleElementOperationDao authRoleElementOperationDao;

    public List<AuthRoleElementOperation> getAuthRoleElementOperationsByRoleIds(Set<Long> roleIdSet) {
        return authRoleElementOperationDao.getAuthRoleElementOperationsByRoleIds(roleIdSet);

    }
}
