package com.imooc.bilibili.api;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.api.support.UserSupport;
import com.imooc.bilibili.domain.JsonResponse;
import com.imooc.bilibili.domain.PageResult;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.service.UserFollowingService;
import com.imooc.bilibili.service.UserService;
import com.imooc.bilibili.service.util.RSAUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

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
    @Autowired
    private UserFollowingService userFollowingService;

    /**
     * 获取当前登录用户的信息，虽然根据 userId 来获取，但该方法不需要参数，是在token中获取 userId
     * 查询某一用户信息可设置为： @GetMapping("/users/{id}")
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
     * 用户注册，成功注册会赋以默认角色
     */
    @PostMapping("/users")
    public JsonResponse<String> addUser(@RequestBody User user) {
        userService.addUser(user);
        return JsonResponse.success();
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/users")
    public JsonResponse<String> updateUsers(@RequestBody User user) throws Exception {
        Long userId = userSupport.getCurrentUserId();
        user.setId(userId);
        userService.updateUsers(user);
        return JsonResponse.success();
    }

    /**
     * 用户登录:成功后，会获取到该用户的用户凭证，也就是用户令牌(tokens)
     */
    @PostMapping("/user-tokens")
    public JsonResponse<String> login(@RequestBody User user) throws Exception {
        String token = userService.login(user);
        return new JsonResponse<>(token);
    }

    /**
     * 登录：双token：1.处理用户退出后，token有效的问题 2.在用户无感的情况下，更新token
     */
    @PostMapping("/user-dts")
    public JsonResponse<Map<String, Object>> loginForDts(@RequestBody User user) throws Exception {
        Map<String, Object> map = userService.loginForDts(user);
        return new JsonResponse<>(map);
    }

    /**
     * 根据（数据库中的）refreshToken（中的userId）来重新生成accessToken
     * 前端验证refreshToken是否过期，如果过期，重新登录，否则，重新生成accessToken
     */
    @PostMapping("/access-tokens")
    public JsonResponse<String> refreshAccessToken(HttpServletRequest request) throws Exception {
        // 获取请求头中的refreshToken
        String refreshToken = request.getHeader("refreshToken");

        String accessToken = userService.refreshAccessToken(refreshToken);
        return new JsonResponse<>(accessToken);
    }

    /**
     * 用户退出登录，删除refreshToken。
     * HttpServletRequest是SpringMVC中的，可以直接在方法参数中使用，
     * 它是系统自动注入的，自动封装好的一个关于请求所有相关的参数的集合对象，包括请求头、请求体、请求参数等，可以通过它来获取请求的相关信息。
     * 这里是用于获取请求头中的refreshToken
     */
    @DeleteMapping("/refresh-tokens")
    public JsonResponse<String> logout(HttpServletRequest request) {
        String refreshToken = request.getHeader("refreshToken");
        Long userId = userSupport.getCurrentUserId();
        userService.logout(refreshToken, userId);
        return JsonResponse.success();
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
     * 分页查询用户（UP主）列表
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
        //pageListUserInfos 实际执行分页查询
        PageResult<UserInfo> result = userService.pageListUserInfos(params);
        if (result.getTotal() > 0) {
            // 查询该页中的用户，是否已经关注了
            List<UserInfo> checkUserInfoList = userFollowingService.checkFollowingStatus(result.getList(), userId);
            result.setList(checkUserInfoList);

        }
        return new JsonResponse<>(result);
    }
}
