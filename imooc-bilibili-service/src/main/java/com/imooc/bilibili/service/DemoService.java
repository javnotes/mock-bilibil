package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.DemoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author luf
 * @date 2022/03/02 20:55
 **/
@Service
public class DemoService {
    @Autowired
    private DemoDao demoDao;

    public Map<String, Object> query(Long id) {
        return demoDao.query(id);
    }
}
