package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserCoinDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/21
 **/

@Service
public class UserCoinService {
    @Autowired
    private UserCoinDao userCoinDao;

    /**
     * 查询当前用户的硬币数量
     */
    public Integer getUserCoinsAmount(Long userId) {
        return userCoinDao.getUserCoinsAmount(userId);
    }

    /**
     * 更新当前用户的硬币数量
     */
    public void updateUserCoinsAmout(Long userId, Integer amount) {
        Date updateTime = new Date();
        userCoinDao.updateUserCoinAmount(userId, amount, updateTime);
    }
}
