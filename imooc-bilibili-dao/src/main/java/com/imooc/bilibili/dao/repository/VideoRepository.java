package com.imooc.bilibili.dao.repository;

import com.imooc.bilibili.domain.Video;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/25
 **/
public interface VideoRepository extends ElasticsearchRepository<Video, Long> {
    /**
     * 通过标题中的关键字查询视频
     */
    Video findByTitleLike(String keyword);
}
