package com.imooc.service;

import com.imooc.bilibili.dao.UserMomentDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author luf
 * @date 2022/03/08 00:34
 **/
@Service
public class UserMomentService {

    @Autowired
    private UserMomentDao userMomentDao;

}
