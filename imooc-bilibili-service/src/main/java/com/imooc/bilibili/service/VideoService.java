package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.Video;
import com.imooc.bilibili.domain.VideoLike;
import com.imooc.bilibili.domain.VideoTag;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/20
 **/
@Service
public class VideoService {

    @Autowired
    private VideoDao videoDao;
    @Autowired
    private FastDFSUtil fastDFSUtil;


    /**
     * 保存视频，涉及两次数据库操作（添加），视频+视频标签
     */
    @Transactional
    public void addVideos(Video video) {
        Date now = new Date();
        video.setCreateTime(now);
        videoDao.addVideos(video);
        Long videoId = video.getId();
        List<VideoTag> videoTagList = video.getVideoTagList();
        videoTagList.forEach(item -> {
            item.setCreateTime(now);
            item.setVideoId(videoId);
        });
        videoDao.batchAddVideoTags(videoTagList);
    }

    /**
     * 分页查询视频列表
     *
     * @param size 分页大小
     * @param no   第几页，从1开始
     * @param area 分区
     */
    public PageResult<Video> pageListVideos(Integer size, Integer no, String area) {
        if (size == null || no == null) {
            throw new ConditionException("参数异常！");
        }
        // 使用Map保存多个查询条件，用于复合查询
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no - 1) * size); //第1页就是从第0条数据开始
        params.put("limit", size);
        params.put("area", area);
        //新建List是因为当没有查询到数据，返回的是一个空的List，而不是直接返回NULL
        List<Video> list = new ArrayList<>();
        //先查询是否有满足条件的数据，如果有，再进行分页查询
        Integer total = videoDao.pageCountVideos(params);
        if (total > 0) {
            list = videoDao.pageListVideos(params);
        }
        return new PageResult<>(total, list);
    }

    /**
     * 在线观看视频，通过分片的形式
     * 因为是通过流的形式进行文件的传输，所以流会写在HttpResponse中的输出流里。
     *
     * @param request
     * @param response
     * @param url
     */
    public void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String url) {
        try {
            fastDFSUtil.viewVideoOnlineBySlices(request, response, url);
        } catch (Exception ignored) {
        }
    }


    /**
     * 点赞视频
     * 需要检查参数videoId、userId的合法性
     */
    public void addVideoLike(Long videoId, Long userId) {
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        if (videoLike == null) {
            throw new ConditionException("已经赞过了！");
        }
        videoLike = new VideoLike();
        videoLike.setVideoId(videoId);
        videoLike.setUserId(userId);
        videoLike.setCreateTime(new Date());
        videoDao.addVideoLike(videoLike);
    }

    /**
     * 删除该用户的视频点赞记录(如果有的话)
     */
    public void deleteVideoLike(Long videoId, Long userId) {
        videoDao.deleteVideoLike(videoId, userId);
    }

    /**
     * 查询该视频的点赞数
     * 如果用户已登录，则查询该用户是否已点赞过；
     * 同时处理游客情形
     */
    public Map<String, Object> getVideoLikes(Long videoId, Long userId) {
        // 查询该视频的点赞数
        Long count = videoDao.getVideoLikes(videoId);

        // 如果用户已登录，则查询该用户是否已点赞过；
        // 同时处理游客情形：如果添加VideoLike记录，需要videoId和userId；
        // 如果用户没登录，则userId为null，自然也就不能查询出记录
        VideoLike videoLike = videoDao.getVideoLikeByVideoIdAndUserId(videoId, userId);
        boolean like = videoLike != null; //用户点赞过该视频

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }
}
