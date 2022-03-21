package com.imooc.bilibili.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/21
 **/

@Mapper
public interface UserCoinDao {
    Integer getUserCoinsAmount(Long userId);

    Integer updateUserCoinAmount(@Param("userId") Long userId, @Param("amount") Integer amount, @Param("updateTime") Date updateTime);
}
