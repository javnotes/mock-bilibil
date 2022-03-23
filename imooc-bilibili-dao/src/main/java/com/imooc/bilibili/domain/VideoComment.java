package com.imooc.bilibili.domain;

import java.util.Date;
import java.util.List;

/**
 * @author chinalife
 */
public class VideoComment {

    private Long id;

    private Long videoId;

    /**
     * 投稿视频的用户Id
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String comment;

    /**
     * 回复用户id，即回复谁的评论
     */
    private Long replyUserId;

    /**
     * 根节点评论id，要记录一级评论的评论id
     */
    private Long rootId;

    private Date createTime;

    private Date updateTime;

    /**
     * 每一个一级评论(rootId为null)，都有一个列表List来存储其下的所有二级评论
     * 冗余字段
     * 一级评论才可以用此List，非一级评论的此 List 为 null
     */
    private List<VideoComment> childList;

    /**
     * 评论人的用户信息，即谁创建了本条评论
     */
    private UserInfo userInfo;

    /**
     * 回复谁的评论，即被评论的用户信息
     */
    private UserInfo replyUserInfo;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getReplyUserId() {
        return replyUserId;
    }

    public void setReplyUserId(Long replyUserId) {
        this.replyUserId = replyUserId;
    }

    public Long getRootId() {
        return rootId;
    }

    public void setRootId(Long rootId) {
        this.rootId = rootId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public List<VideoComment> getChildList() {
        return childList;
    }

    public void setChildList(List<VideoComment> childList) {
        this.childList = childList;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public UserInfo getReplyUserInfo() {
        return replyUserInfo;
    }

    public void setReplyUserInfo(UserInfo replyUserInfo) {
        this.replyUserInfo = replyUserInfo;
    }
}
