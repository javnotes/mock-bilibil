package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.Danmu;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.service.DanmuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @description: TODO 类描述
 * @author: luf
 * @date: 2022/3/24
 **/
@RestController
public class DanmuApi {

    @Autowired
    private DanmuService danmuService;
    @Autowired
    private UserSupport userSupport;

    @GetMapping("/danmus")
    public JsonResponse<List<Danmu>> getDanmus(@RequestParam Long videoId, String startTime,
                                               String endTime) throws Exception {
        List<Danmu> list;
        try {
            //判断当前是游客还是登录用户
            userSupport.getCurrentUserId();
            //若用户登录了，则允许用按时间段筛选弹幕
            list = danmuService.getDanmus(videoId, startTime, endTime);
        } catch (Exception ignored) {
            //若为游客，则不允许按时间段来筛选弹幕
            list = danmuService.getDanmus(videoId, null, null);
        }
        return new JsonResponse<>(list);
    }
}
