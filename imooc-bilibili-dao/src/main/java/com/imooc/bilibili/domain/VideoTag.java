package com.imooc.bilibili.domain;

import java.util.Date;

/**
 * 视频标签：一个视频可以有多个视频标签，可用于视频分类分区、搜索、用户推荐。
 * 注意：这里只是视频id与标签id的关联
 */
public class VideoTag {

    private Long id;

    private Long videoId;

    private Long tagId;

    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
