package com.imooc.bilibili.api;

import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.domain.annotation.ApiLimitedRole;
import com.imooc.bilibili.domain.annotation.DataLimited;
import com.imooc.bilibili.domain.constant.AuthRoleConstant;
import com.imooc.bilibili.service.UserMomentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author luf
 * @date 2022/03/08 00:33
 **/
@RestController
public class UserMomentsApi {

    @Autowired
    private UserMomentsService userMomentsService;

    @Autowired
    private UserSupport userSupport;

    /**
     * 新增用户动态
     * 用户a发布动态后，关注了a的用户b，就可以看到a的动态
     * @ApiLimitedRole：基于Api进行权限控制，AuthRoleConstant.ROLE_LV0不允许发送动态，权限不足时，抛出异常
     * @DataLimited：基于数据进行权限控制。切入方法，获取到切入方法所有参数，循环遍历，如果有指定对象实例，在对实例属性进行检查
     * @ApiLimitedRole和@DataLimited都是基于AOP实现的,权限控制的逻辑都在切面类里实现的
     * @RequestBody的作用是将前端传过来的json数据转换成java对象
     */
    @ApiLimitedRole(limitedRoleCodeList = {AuthRoleConstant.ROLE_LV0})
    @DataLimited
    @PostMapping("/user-moments")
    public JsonResponse<String> addUserMoments(@RequestBody UserMoment userMoment) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        userMoment.setUserId(userId);
        userMomentsService.addUserMoments(userMoment);
        return JsonResponse.success();
    }

    /**
     * 获取用户动态，关注的用户发布的动态
     * @return
     */
        @GetMapping("/user-subscribed-moments")
    public JsonResponse<List<UserMoment>> getUserSubscribedMoments() {
        Long userId = userSupport.getCurrentUserId();
        // 获取该用户关注的用户发布的动态
        List<UserMoment> list = userMomentsService.getUserSubscribedMoments(userId);
        return new JsonResponse<>(list);
    }

}
