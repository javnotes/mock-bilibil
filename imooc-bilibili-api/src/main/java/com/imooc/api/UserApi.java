package com.imooc.api;

import com.alibaba.fastjson.JSONObject;
import com.imooc.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.service.UserService;
import com.imooc.bilibili.service.util.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author luf
 * @date 2022/03/03 21:06
 **/
@RestController
public class UserApi {

    @Autowired
    private UserService userService;
    @Autowired
    private UserSupport userSupport;

    /**
     * 获取当前登录用户的信息，虽然根据 userId 来获取，但该方法不需要参数，是在token中获取 userId
     **/
    @GetMapping("/users")
    public JsonResponse<User> getUserInfo() {
        // 获取当前登录用户
        Long userId = userSupport.getCurrentUserId();
        User user = userService.getUserInfo(userId);
        return new JsonResponse<>(user);
    }


    @GetMapping("/rsa-pks")
    public JsonResponse<String> getRsaPublicKey() {
        String pk = RSAUtil.getPublicKeyStr();
        return new JsonResponse<>(pk);
    }

    /**
     * 注册
     */
    @PostMapping("/users")
    public JsonResponse<String> addUser(@RequestBody User user) {
        userService.addUser(user);
        return JsonResponse.success();
    }

    /**
     * 用户登录成功后，会获取到该用户的用户凭证，也就是用户令牌(tokens)
     */
    @PostMapping("/user-tokens")
    public JsonResponse<String> login(@RequestBody User user) throws Exception {
        String token = userService.login(user);
        return new JsonResponse<>(token);
    }

    /**
     * 更新用户信息
     * 注意：一般在token中获取userId，不是从前端中传输，防止伪造
     */
    @PostMapping("/user-infos")
    public JsonResponse<String> updateUserInfo(@RequestBody UserInfo userInfo) {
        Long userId = userSupport.getCurrentUserId();
        userInfo.setUserId(userId);
        userService.updateUserInfos(userInfo);
        return JsonResponse.success();
    }

    /**
     * 分页查询用户列表
     * 方法参数：当前页码、每页展示的数据条数、用户昵称（可用于模糊查询）
     * 专门用于封装分页查询的结果
     */
    @GetMapping("/user-infos")
    public JsonResponse<PageResult<UserInfo>> pageListUserInfos(@RequestParam Integer pageNum, @RequestBody Integer pageSize, String nick) {
        Long userId = userSupport.getCurrentUserId();
        //JSONObject：相当于是个map,public JSONObject(Map<String, Object> map) {}
        JSONObject params = new JSONObject();
        params.put("pageNum", pageNum);
        params.put("pageSize", pageSize);
        params.put("nick", nick);
        params.put("userId", userId);
        PageResult<UserInfo> result = userService.pageListUserInfos(params);

    }
}
