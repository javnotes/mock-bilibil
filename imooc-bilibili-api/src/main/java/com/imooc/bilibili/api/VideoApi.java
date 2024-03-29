package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.*;
import com.imooc.bilibili.service.ElasticSearchService;
import com.imooc.bilibili.service.VideoService;
import org.apache.mahout.cf.taste.common.TasteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/20
 **/
@RestController
public class VideoApi {

    @Autowired
    private VideoService videoService;
    @Autowired
    private UserSupport userSupport;
    @Autowired
    private ElasticSearchService elasticSearchService;

    /**
     * 视频投稿
     */
    @PostMapping("/videos")
    public JsonResponse<String> addVideos(@RequestBody Video video) {
        Long userId = userSupport.getCurrentUserId();
        video.setUserId(userId);
        videoService.addVideos(video);
        // 在es中添加一条视频
        // 必须先在数据库中添加，数据库会生成 Video 的主键
        elasticSearchService.addVideo(video);
        return JsonResponse.success();
    }

    /**
     * （分区内）分页查询视频列表-瀑布流
     * 首页中，每个分区都先查询部分数据。
     */
    @GetMapping("/videos")
    public JsonResponse<PageResult<Video>> pageListVideos(Integer size, Integer no, String area) {
        PageResult<Video> result = videoService.pageListVideos(size, no, area);
        return new JsonResponse<>(result);
    }

    /**
     * 在线观看视频，通过分片的形式
     * 因为是通过流的形式进行文件的传输，所以流会写在HttpResponse中的输出流里。
     */
    @GetMapping("/video-slices")
    public void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String url) {
        videoService.viewVideoOnlineBySlices(request, response, url);
    }

    /**
     * 点赞视频
     */
    @PostMapping("/video-likes")
    public JsonResponse<String> addVideoLike(@RequestParam Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * 取消视频点赞
     */
    @DeleteMapping("/video-likes")
    public JsonResponse<String> deleteVideoLike(@RequestParam Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        // userId 已合法，有点赞记录才会删除
        videoService.deleteVideoLike(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * 查询该视频点赞数量
     * 登录用户、游客均可以查看视频、查看视频的点赞数，
     * 但如果用户已登录，需要查看该用户视频点赞过该视频
     */
    @GetMapping("/video-likes")
    public JsonResponse<Map<String, Object>> getVideoLikes(@RequestParam Long videoId) {
        Long userId = null; //当前用户可能为游客
        try {
            userId = userSupport.getCurrentUserId();
        } catch (Exception ignored) {
        }

        Map<String, Object> result = videoService.getVideoLikes(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 收藏视频
     */
    @PostMapping("video-colllections")
    public JsonResponse<String> addVideoCollection(@RequestBody VideoCollection videoCollection) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCollection(videoCollection, userId);
        return JsonResponse.success();
    }

    /**
     * 取消收藏视频
     */
    @DeleteMapping("/video-collections")
    public JsonResponse<String> deleteVideoCollection(@RequestParam Long videoId) {
        Long userId = userSupport.getCurrentUserId();
        videoService.deleteVideoCollection(videoId, userId);
        return JsonResponse.success();
    }

    /**
     * 查询视频收藏数量
     * 游客、用户均可查看
     */
    @GetMapping("/video-collections")
    public JsonResponse<Map<String, Object>> getVideoCollections(@RequestParam Long videoId) {
        Long userId = null;

        //若为登录用户，则获取到对应userId；若为游客，则忽略异常，userId还是为NULL
        try {
            userId = userSupport.getCurrentUserId();
        } catch (Exception ignored) {
        }

        Map<String, Object> result = videoService.getVideoCollections(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 视频投币
     */
    @PostMapping("/video-conis")
    public JsonResponse<String> addVideoCoins(@RequestBody VideoCoin videoCoin) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoCoins(videoCoin, userId);
        return JsonResponse.success();
    }

    /**
     * 查询视频投币数量
     */
    @GetMapping("/video-conis")
    public JsonResponse<Map<String, Object>> getVideoCoins(@RequestParam Long videoId) {
        Long userId = null;
        try {
            userId = userSupport.getCurrentUserId();
        } catch (Exception ignored) {
        }
        Map<String, Object> result = videoService.getVideoCoins(videoId, userId);
        return new JsonResponse<>(result);
    }

    /**
     * 添加视频评论
     */
    @PostMapping("/video-comments")
    public JsonResponse<String> addVideoComment(@RequestBody VideoComment videoComment) {
        Long userId = userSupport.getCurrentUserId();
        videoService.addVideoComment(videoComment, userId);
        return JsonResponse.success();
    }

    /**
     * 分页查询视频评论
     */
    @GetMapping("/video-comments")
    public JsonResponse<PageResult<VideoComment>> pageListVideoCommments(@RequestParam Integer size, @RequestParam Integer no, @RequestParam Long videoId) {
        PageResult<VideoComment> result = videoService.pageListVideoComments(size, no, videoId);
        return new JsonResponse<>(result);
    }


    /**
     * 获取视频详情
     */
    @GetMapping("/video-details")
    public JsonResponse<Map<String, Object>> getVideoDetails(@RequestParam Long videoId) {
        Map<String, Object> result = videoService.getVideoDetails(videoId);
        return new JsonResponse<>(result);
    }

    /**
     * 视频观看记录：同一用户，退出登录后再观看，也算一次播放量，而且clientId 是相同的，一个『用户』一天只有一次播放量
     * request：需要通过request获取游客的ip、clientId
     * 观看记录可用于统计视频的播放量
     * 播放量计数规则：同一个用户一天1次，游客一天1次（用户退出登录后观看也参与计算播放量）
     * 游客区分规则：操作系统+浏览器+IP
     */
    @PostMapping("/video-views")
    public JsonResponse<String> addVideoView(@RequestBody VideoView videoView, HttpServletRequest request) {
        Long userId = null;
        try {
            //登录用户需统计userId
            userId = userSupport.getCurrentUserId();
            videoView.setUserId(userId);
            videoService.addVideoView(videoView, request);
        } catch (Exception e) {
            //游客需统计 clientId+ip
            videoService.addVideoView(videoView, request);
        }
        return JsonResponse.success();
    }


    /**
     * 查询视频播放量，根据观看记录来统计
     */
    @GetMapping("/video-view-counts")
    public JsonResponse<Integer> getVideoViewCounts(@RequestParam Long videoId) {
        Integer count = videoService.getVideoViewCounts(videoId);
        return new JsonResponse<>(count);
    }

    /**
     * 视频内容推荐
     */
    @GetMapping("/recommendations")
    public JsonResponse<List<Video>> recommend() throws TasteException {
        Long userId = userSupport.getCurrentUserId();
        List<Video> list = videoService.recommend(userId);
        return new JsonResponse<>(list);
    }

    /**
     * 视频帧截取生成黑白剪影
     */

    /**
     * 查询视频黑白剪影
     */

}
