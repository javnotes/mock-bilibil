package com.imooc.bilibili.dao;


import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface DemoDao {

    public Long query(Long id);
    
    List list = new ArrayList();
}
