package com.imooc.bilibili.dao.repository;

import com.imooc.bilibili.domain.UserInfo;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/25
 **/
public interface UserInfoRepository extends ElasticsearchRepository<UserInfo, Long> {
}
