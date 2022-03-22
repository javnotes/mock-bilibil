package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.FastDFSUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private UserCoinService userCoinService;
    @Autowired
    private UserService userService;


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

    /**
     * 添加视频收藏
     */
    @Transactional
    public void addVideoCollection(VideoCollection videoCollection, Long userId) {
        Long videoId = videoCollection.getVideoId();
        Long groupId = videoCollection.getGroupId();
        if (videoId == null || groupId == null) {
            throw new ConditionException("参数异常！");
        }

        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        // 删除原有视频收藏
        // 收藏的视频对应一个收藏分组，先添加再删除，相当于更新或修改
        videoDao.deleteVideoCollection(videoId, userId);

        videoCollection.setUserId(userId);
        videoCollection.setCreateTime(new Date());
        videoDao.addVideoCollection(videoCollection);
    }

    /**
     * 删除视频收藏
     */

    public void deleteVideoCollection(Long videoId, Long userId) {
        // 这里不用验证参数videoId是因为只有存在videoId-userId这条记录时，才会被删除
        videoDao.deleteVideoCollection(videoId, userId);
    }

    /**
     * 获取视频的收藏数量
     */
    public Map<String, Object> getVideoCollections(Long videoId, Long userId) {
        // 获取视频的收藏数
        Long count = videoDao.getVideoCollections(videoId);

        // 若为登录用户，需要查看该用户是否收藏过该视频
        VideoCollection videoCollection = videoDao.getVideoCollectionsByVideoIdAndUserId(videoId, userId);
        //true-已收藏该视频 false-未收藏过该视频-未在数据库中查询出对应记录
        boolean like = videoCollection != null;

        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    /**
     * 投币会涉及多个数据表的更新
     */
    @Transactional
    public void addVideoCoins(VideoCoin videoCoin, Long userId) {
        Long videoId = videoCoin.getVideoId();
        // 用户想要投币的数目
        Integer amount = videoCoin.getAmount();
        if (videoId == null) {
            throw new ConditionException("参数异常！");
        }
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        //查询当前用户是否拥有足够的硬币
        Integer userCoinsAmount = userCoinService.getUserCoinsAmount(userId);
        userCoinsAmount = userCoinsAmount == null ? 0 : userCoinsAmount;
        if (amount > userCoinsAmount) {
            throw new ConditionException("硬币数量不足！");
        }

        // 查询当前用户已经对该视频投了多少硬币，用户对某一视频的投币记录只有一条，多次投币会对记录进行更新
        VideoCoin dbVideoCoin = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        // 新增视频投币
        if (dbVideoCoin == null) { //当前用户未对该视频投币过
            videoCoin.setUserId(userId);
            videoCoin.setCreateTime(new Date());
            videoDao.addVideoCoin(videoCoin);
        } else { //当前用户对该视频投币过
            Integer dbAmount = dbVideoCoin.getAmount();
            dbAmount += amount;

            //更新该用户对该视频的投币记录
            videoCoin.setUserId(userId);
            videoCoin.setAmount(dbAmount);
            videoCoin.setUpdateTime(new Date());
            videoDao.updateVideoCoin(videoCoin);
        }

        // 还要更新当前用户的硬币数量
        userCoinService.updateUserCoinsAmout(userId, (userCoinsAmount - amount));

    }

    /**
     * 查询视频投币数量
     */
    public Map<String, Object> getVideoCoins(Long videoId, Long userId) {
        Long count = videoDao.getVideoCoinsAmount(videoId);

        // 还要查询登录用户是否对该视频投币过
        VideoCoin videoCoin = videoDao.getVideoCoinByVideoIdAndUserId(videoId, userId);
        boolean like = videoCoin != null;
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("like", like);
        return result;
    }

    /**
     * 添加视频评论
     */
    public void addVideoComment(VideoComment videoComment, Long userId) {
        Long videoId = videoComment.getVideoId();
        if (videoId == null) {
            throw new ConditionException("参数异常！");
        }

        // 查看数据库中是否有此视频
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        videoComment.setUserId(userId);
        videoComment.setCreateTime(new Date());
        videoDao.addVideoComment(videoComment);
    }

    /**
     * 分页查询视频评论
     */
    public PageResult<VideoComment> pageListVideoComments(Integer size, Integer no, Long videoId) {
        Video video = videoDao.getVideoById(videoId);
        if (video == null) {
            throw new ConditionException("非法视频！");
        }

        // 分页查询参数
        Map<String, Object> params = new HashMap<>();
        params.put("start", (no - 1) * size);
        params.put("limit", size);
        params.put("videoId", videoId);
        //分页查询（同条件下）一级评论总数
        // select count(1) from t_video_comment where videoId = #{videoId} and rootId is null
        Integer total = videoDao.pageCountVideoComments(params);
        //即使没有评论，要返回一个空的List，而不是返回Null
        List<VideoComment> list = new ArrayList<>();
        if (total > 0) {
            //查询一级评论
            list = videoDao.pageListVideoComments(params);
            //批量查询二级评论
            //该视频下所有的一级评论的Id，rootId 为 null
            List<Long> parentIdList = list.stream().map(VideoComment::getId).collect(Collectors.toList());
            // 非一级评论，rootId 不为 null
            List<VideoComment> childCommentList = videoDao.batchGetVideoCommentsByRootIds(parentIdList);

            //批量查询发出评论的用户的信息，一个用户可能发送不止一条评论，使用Set可自动去重
            // 一级评论用户的userId
            Set<Long> userIdList = list.stream().map(VideoComment::getUserId).collect(Collectors.toSet());

            // 非一级评论的用户的userId
            Set<Long> replyUserIdList = childCommentList.stream().map(VideoComment::getUserId).collect(Collectors.toSet());

            // 被评论的用户的userId
            Set<Long> childUserIdList = childCommentList.stream().map(VideoComment::getReplyUserId).collect(Collectors.toSet());

            // .addAll：如果指定的集合也是一个集合，那么 addAll 操作会有效地修改这个集合，使其值是两个集合的并集
            userIdList.addAll(replyUserIdList);
            userIdList.addAll(childUserIdList);

            //查询所有评论人的用户信息
            List<UserInfo> userInfoList = userService.batchGetUserInfoByUserIds(userIdList);

            //讲userId-userInfo对应，方便后续快速查询
            // Collectors.toMap：将元素收集到 Map 中的 Collector，其键和值是将映射函数应用于输入元素的结果
            Map<Long, UserInfo> userInfoMap = userInfoList.stream().collect(Collectors.toMap(UserInfo::getUserId, userInfo -> userInfo));

            // 一级评论：list = videoDao.pageListVideoComments(params);
            list.forEach(comment -> {
                // 获取一级评论的id
                Long id = comment.getId();
                // 每个一级评论中都有一个列表来存储其下的所有二级评论，开始填充此List
                List<VideoComment> childList = new ArrayList<>();
                // childCommentList：非一级评论，List<VideoComment> childCommentList = videoDao.batchGetVideoCommentsByRootIds(parentIdList);
                childCommentList.forEach(child -> {
                    // 将一级评论id与非一级评论的rootId进行比较，确定一级评论下的评论
                    if (id.equals(child.getRootId())) {
                        // 填充非一级评论的评论用户的信息
                        child.setUserInfo(userInfoMap.get(child.getReplyUserId()));
                        childList.add(child);
                    }
                });

                // 非一级评论的用户的信息
                comment.setChildList(childList);
                // 主评论的
                comment.setUserInfo(userInfoMap.get(comment.getUserId()));
            });
        }
        return new PageResult<>(total, list);
    }

    /**
     * 获取视频详情信息
     */
    public Map<String, Object> getVideoDetails(Long videoId) {
        Video video = videoDao.getVideoDetails(videoId);

        // 还要查询视频作者的用户信息
        Long userId = video.getUserId();
        User user = userService.getUserInfo(userId);
        UserInfo userInfo = user.getUserInfo();

        Map<String, Object> result = new HashMap<>();
        result.put("video", video);
        result.put("userInfo", userInfo);
        return result;
    }

    /**
     * 添加(登录用户、游客？？)视频观看记录
     */
    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();

        // 生成 clientId
        String agent = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
    }
}
