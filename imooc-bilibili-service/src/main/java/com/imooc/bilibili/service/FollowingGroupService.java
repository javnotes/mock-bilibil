package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.FollowingGroupDao;
import com.imooc.bilibili.domain.FollowingGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author luf
 * @date 2022/03/06 01:01
 **/
@Service
public class FollowingGroupService {
    @Autowired
    private FollowingGroupDao followingGroupDao;

    public FollowingGroup getById(Long id) {
        return followingGroupDao.getById(id);
    }

    public FollowingGroup getByType(String type) {
        return followingGroupDao.getByType(type);
    }

    public List<FollowingGroup> getByUserId(Long userId) {
        return followingGroupDao.getByUserId(userId);
    }

    public void addFollowingGroup(FollowingGroup followingGroup) {
        followingGroupDao.addFollowingGroup(followingGroup);
    }

    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupDao.getUserFollowingGroups(userId);
    }
}
