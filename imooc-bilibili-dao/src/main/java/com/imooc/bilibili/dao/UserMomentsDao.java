package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.UserMoment;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author luf
 * @date 2022/03/08 22:08
 **/
@Mapper
public interface UserMomentsDao {
    Integer addUserMoments(UserMoment userMoment);
}
