package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.Danmu;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/23
 **/
@Mapper
public interface DanmuDao {

    Integer addDanmu(Danmu danmu);

    List<Danmu> getDanmus(Map<String,Object> params);
}
