package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.VideoDao;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.domain.exception.ConditionException;
import com.imooc.bilibili.service.util.FastDFSUtil;
import com.imooc.bilibili.service.util.ImageUtil;
import com.imooc.bilibili.service.util.IpUtil;
import eu.bitwalker.useragentutils.UserAgent;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.model.GenericDataModel;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.UncenteredCosineSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.PreferenceArray;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
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

    @Autowired
    private ImageUtil imageUtil;
    @Autowired
    private FileService fileService;
    private static final int FRAME_NO = 256;


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
     * 添加视频观看记录
     */
    public void addVideoView(VideoView videoView, HttpServletRequest request) {
        Long userId = videoView.getUserId();
        Long videoId = videoView.getVideoId();

        String ip = IpUtil.getIP(request);
        // 通过 UserAgent 获取clientId
        String agent = request.getHeader("User-Agent");
        UserAgent userAgent = UserAgent.parseUserAgentString(agent);
        String clientId = String.valueOf(userAgent.getId());

        Map<String, Object> params = new HashMap<>();
        if(userId != null){ //登录用户
            params.put("userId", userId);
        } else { //游客
            params.put("ip", ip);
            params.put("clientId", clientId);
        }

        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        params.put("today", sdf.format(today));
        params.put("videoId", videoId);

        //添加观看记录，如果今日已有观看记录，就不再添加
        VideoView dbVideoView = videoDao.getVideoView(params);
        if(dbVideoView == null) {
            // 虽然设置了ip、clientId，但在Dao层，保证了游客才会用到
            videoView.setIp(ip);
            videoView.setClientId(clientId);
            videoView.setCreateTime(new Date());
            videoDao.addVideoView(videoView);
        }
    }

    public Integer getVideoViewCounts(Long videoId) {
        return videoDao.getVideoViewCounts(videoId);
    }


    /**
     * 基于用户的协同推荐
     * @param userId 用户id
     */
    public List<Video> recommend(Long userId) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        //创建数据模型
        DataModel dataModel = this.createDataModel(list);
        //获取用户相似程度
        UserSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        System.out.println(similarity.userSimilarity(11, 12));
        //获取用户邻居(最接近的2个)
        UserNeighborhood userNeighborhood = new NearestNUserNeighborhood(2, similarity, dataModel);
        long[] ar = userNeighborhood.getUserNeighborhood(userId);
        //构建推荐器(基于用户的)
        Recommender recommender = new GenericUserBasedRecommender(dataModel, userNeighborhood, similarity);
        //推荐视频(推荐的数量是 5)
        List<RecommendedItem> recommendedItems = recommender.recommend(userId, 5);
        List<Long> itemIds = recommendedItems.stream().map(RecommendedItem::getItemID).collect(Collectors.toList());
        return videoDao.batchGetVideosByIds(itemIds);
    }

    /**
     * 基于内容的协同推荐
     * @param userId 用户id
     * @param itemId 参考内容id（根据该内容进行相似内容推荐）
     * @param howMany 需要推荐的数量
     */
    public List<Video> recommendByItem(Long userId, Long itemId, int howMany) throws TasteException {
        List<UserPreference> list = videoDao.getAllUserPreference();
        //创建数据模型
        DataModel dataModel = this.createDataModel(list);
        //获取内容相似程度
        ItemSimilarity similarity = new UncenteredCosineSimilarity(dataModel);
        GenericItemBasedRecommender genericItemBasedRecommender = new GenericItemBasedRecommender(dataModel, similarity);
        // 物品推荐相拟度，计算两个物品同时出现的次数，次数越多任务的相拟度越高
        List<Long> itemIds = genericItemBasedRecommender.recommendedBecause(userId, itemId, howMany)
                .stream()
                .map(RecommendedItem::getItemID)
                .collect(Collectors.toList());
        //推荐视频
        return videoDao.batchGetVideosByIds(itemIds);
    }

    /**
     * 传入用户的偏好列表
     */
    private DataModel createDataModel(List<UserPreference> userPreferenceList) {
        FastByIDMap<PreferenceArray> fastByIdMap = new FastByIDMap<>();
        //Map：key-userId，value-user的所有UserPreference(喜好得分)，注意value为list
        Map<Long, List<UserPreference>> map = userPreferenceList.stream().collect(Collectors.groupingBy(UserPreference::getUserId));

        Collection<List<UserPreference>> list = map.values();
        // 将每一个UserPreference转为GenericPreference
        for(List<UserPreference> userPreferences : list){
            // list-> array，将数据库中存储的实体类映射到推荐引擎中
            GenericPreference[] array = new GenericPreference[userPreferences.size()];
            for(int i = 0; i < userPreferences.size(); i++){
                UserPreference userPreference = userPreferences.get(i);
                GenericPreference item = new GenericPreference(userPreference.getUserId(), userPreference.getVideoId(), userPreference.getValue());
                array[i] = item;
            }
            fastByIdMap.put(array[0].getUserID(), new GenericUserPreferenceArray(Arrays.asList(array)));
        }
        return new GenericDataModel(fastByIdMap);
    }

    public List<VideoBinaryPicture> convertVideoToImage(Long videoId, String fileMd5) throws Exception{
        com.imooc.bilibili.domain.File file = fileService.getFileByMd5(fileMd5);
        String filePath = "/Users/hat/tmpfile/fileForVideoId" + videoId + "." + file.getType();
        fastDFSUtil.downLoadFile(file.getUrl(), filePath);
        FFmpegFrameGrabber fFmpegFrameGrabber = FFmpegFrameGrabber.createDefault(filePath);
        fFmpegFrameGrabber.start();
        int ffLength = fFmpegFrameGrabber.getLengthInFrames();
        Frame frame;
        Java2DFrameConverter converter = new Java2DFrameConverter();
        int count = 1;
        List<VideoBinaryPicture> pictures = new ArrayList<>();
        for(int i=1; i<= ffLength; i ++){
            long timestamp = fFmpegFrameGrabber.getTimestamp();
            frame = fFmpegFrameGrabber.grabImage();
            if(count == i){
                if(frame == null){
                    throw new ConditionException("无效帧");
                }
                BufferedImage bufferedImage = converter.getBufferedImage(frame);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", os);
                InputStream inputStream = new ByteArrayInputStream(os.toByteArray());
                //输出黑白剪影文件
                java.io.File outputFile = java.io.File.createTempFile("convert-" + videoId + "-", ".png");
                BufferedImage binaryImg = imageUtil.getBodyOutline(bufferedImage, inputStream);
                ImageIO.write(binaryImg, "png", outputFile);
                //有的浏览器或网站需要把图片白色的部分转为透明色，使用以下方法可实现
                imageUtil.transferAlpha(outputFile, outputFile);
                //上传视频剪影文件
                String imgUrl = fastDFSUtil.uploadCommonFile(outputFile, "png");
                VideoBinaryPicture videoBinaryPicture = new VideoBinaryPicture();
                videoBinaryPicture.setFrameNo(i);
                videoBinaryPicture.setUrl(imgUrl);
                videoBinaryPicture.setVideoId(videoId);
                videoBinaryPicture.setVideoTimestamp(timestamp);
                pictures.add(videoBinaryPicture);
                count += FRAME_NO;
                //删除临时文件
                outputFile.delete();
            }
        }
        //删除临时文件
        java.io.File tmpFile = new java.io.File(filePath);
        tmpFile.delete();
        //批量添加视频剪影文件
        videoDao.batchAddVideoBinaryPictures(pictures);
        return pictures;
    }

}
