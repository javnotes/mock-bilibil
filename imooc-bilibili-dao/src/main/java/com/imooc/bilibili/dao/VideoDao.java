package com.imooc.bilibili.dao;

import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.domain.VideoTag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/20
 **/
@Mapper
public interface VideoDao {

    Integer addVideos(Video video);
    Integer batchAddVideoTags(List<VideoTag> videoTagList);

    Integer pageCountVideos(Map<String, Object> params);
    List<Video> pageListVideos(Map<String, Object> params);
}