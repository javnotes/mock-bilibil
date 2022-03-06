package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserFollowingDao;
import com.imooc.bilibili.domain.FollowingGroup;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserFollowing;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author luf
 * @date 2022/03/06 01:00
 **/
@Service
public class UserFollowingService {
    @Autowired
    private UserFollowingDao userFollowingDao;

    @Autowired
    private FollowingGroupService followingGroupService;

    @Autowired
    private UserService userService;

    @Transactional
    public void addUserFollowing(UserFollowing userFollowing) {
        // 将关注的up主保存至该分组
        Long groupId = userFollowing.getGroupId();
        FollowingGroup followingGroup = null;
        if (groupId == null) {
            // 默认分组
            followingGroup = followingGroupService.getByType(UserConstant.DEFAULT_USER_FOLLOWING_GROUP_TYPE);
        } else {
            followingGroup = followingGroupService.getById(groupId);
            if (followingGroup == null) {
                //根据groupId没有查询出该分组
                throw new ConditionException("分组不存在");
            }
        }
        // 获取关注的up主
        Long followingId = userFollowing.getFollowingId();
        User user = userService.getUserById(followingId);
        if (user == null) {
            throw new ConditionException("关注的up主不存在");
        }
        // 如果之前关注过该UP主，则先删除该记录，再插入新纪录，所以方法要使用@Transactional
        userFollowingDao.deleteUserFollowing(userFollowing.getUserId(), followingId);
        userFollowing.setCreateTime(new Date());
        userFollowingDao.addUserFollowing(userFollowing);
    }

    /**
     * 获取用户的关注的UP主
     * 1.获取关注的用户列表(根据userId查询该用户关注了哪些UP主，得到集合list，在提取UP主的userId至集合followingIdSet)
     * 2.根据关注用户的id查询关注用户的基本信息
     * 3.将关注用户按关注分组进行分类
     */
    public List<FollowingGroup> getUserFollowings(Long userId) {
        //list保存了用户关注了哪些UP主，关注一个UP主，就是一个UserFollowing记录
        List<UserFollowing> list = userFollowingDao.getUserFollowings(userId);
        Set<Long> followingIdSet = list.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        // 关注的UP主们的信息
        List<UserInfo> userInfoList = new ArrayList<>();
        if (followingIdSet.size() > 0) {
            //根据Id获取用户信息
            userInfoList = userService.getUserInfoByUserIds(followingIdSet);
        }

        //把UP主的userId与其用户信息对应上
        for (UserFollowing userFollowing : list) {
            for (UserInfo userInfo : userInfoList) {
                if (userFollowing.getUserId().equals(userInfo.getUserId())) {
                    userFollowing.setUserInfo(userInfo);
                }
            }
        }

        // 用户用户的关注分组有哪些
        List<FollowingGroup> groupList = followingGroupService.getByUserId(userId);
        //全部关注，由保存在数据表中的各个分组聚合而成
        FollowingGroup allGroup = new FollowingGroup();
        allGroup.setName(UserConstant.USER_FOLLOWING_GROUP_ALL_NAME);
        allGroup.setFollowingUserInfoList(userInfoList);
        // `全部关注`分组+保存在数据库中的分组
        List<FollowingGroup> result = new ArrayList<>();
        result.add(allGroup);

        //按分组，填充对应分组FollowingGroup中关注的UP主UserInfo
        for (FollowingGroup group : groupList) {
            //该分组group中保存哪些UP主
            List<UserInfo> infoList = new ArrayList<>();
            for (UserFollowing userFollowing : list) {
                if (group.getId().equals(userFollowing.getGroupId())) {
                    infoList.add(userFollowing.getUserInfo());
                }
            }
            group.setFollowingUserInfoList(infoList);
            result.add(group);
        }
        return result;
    }

    /**
     * 获取用户的粉丝
     * 1.获取当前用户的粉丝列表
     * 2.根据粉丝的用户id查询基本信息
     * 3.查询当前用户是否已经关注该粉丝
     */
    public List<UserFollowing> getUserFans(Long userId) {
        //UserFollowing 中的followingId->userId，中的userId->为粉丝的userId
        List<UserFollowing> fanList = userFollowingDao.getUserFans(userId);
        //粉丝的userId集合
        Set<Long> fanIdSet = fanList.stream().map(UserFollowing::getUserId).collect(Collectors.toSet());
        List<UserInfo> fanInfoList = new ArrayList<>();
        if (fanIdSet.size() > 0) {
            //获取粉丝的用户信息
            fanInfoList = userService.getUserInfoByUserIds(fanIdSet);
        }
        //获取关注的UP主，用于后续的相互关注
        List<UserFollowing> followingList = userFollowingDao.getUserFollowings(userId);

        for (UserFollowing fan : fanList) {
            for (UserInfo fanInfo : fanInfoList) {
                //fan.getUserId()、fanInfo.getUserId()均为用户id，
                if (fan.getUserId().equals(fanInfo.getUserId())) {
                    fanInfo.setFollowed(false);
                    fan.setUserInfo(fanInfo);
                }
            }
            for (UserFollowing following : followingList) {
                //检查该用户关注的UP主是否回关，也就是是否相互关注
                if (following.getFollowingId().equals(fan.getUserId())) {
                    fan.getUserInfo().setFollowed(true);
                }
            }
        }
        return fanList;
    }


    public Long addUserFollowingGroup(FollowingGroup followingGroup) {
        followingGroup.setCreateTime(new Date());
        followingGroup.setType(UserConstant.USER_FOLLOWING_GROUP_TYPE_USER);
        followingGroupService.addFollowingGroup(followingGroup);
        return followingGroup.getId();

    }

    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupService.getUserFollowingGroups(userId);
    }
}
