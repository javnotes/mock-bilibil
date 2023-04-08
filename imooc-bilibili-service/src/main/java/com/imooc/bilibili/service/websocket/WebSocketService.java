package com.imooc.bilibili.service.websocket;

import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.domain.Danmu;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.service.DanmuService;
import com.imooc.bilibili.service.util.RocketMQUtil;
import com.imooc.bilibili.service.util.TokenUtil;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket可以实现服务端与客户端的全双工通信，即服务端可以主动向客户端推送消息，客户端也可以主动向服务端发送消息，而且是实时的，
 * 不需要客户端或服务端进行轮询，这样就大大节省了服务器资源，提高了系统的性能。
 * WebSocket实现了客户端与服务端的全双工通信，是HTML5开始提供的一种在单个TCP连接上进行全双工通讯的协议。
 * WebSocket的出现，使得客户端和服务端之间的数据交换变得更加简单，允许服务端主动向客户端推送数据。
 * 全双工通信：即客户端和服务端都可以同时向对方发送或接收数据。
 * @ServerEndpoint标识WebSocket服务类，访问路径：“/imserver{token}”
 * @author: luf
 * @date: 2022/3/23
 **/
@Component
@ServerEndpoint("/imserver/{token}")
public class WebSocketService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 统计在线人数
     */
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    /**
     * 存放所有的客户端对应的WebSocketService，key为 sessionId
     * SpringBoot 默认是单例模式，只会注入一次；但一个客户端对应一个WebSocketService，这是多例模式
     */
    public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>();

    /**
     * Session：服务端和客户端进行通信的一个会话，主要用于长连接通信，一个客户端对应一个Session，由WebSocket创建并把它保存在WebSocketService
     */
    private Session session;

    private String sessionId;

    private Long userId;

    /**
     * 这个就是启动类里的 applicationContext
     * ApplicationContext applicationContext =
     * SpringApplication.run(ImoocBilibiliApp.class, args);  applicationContext 也是单例的
     * staic：多例共用
     */
    private static ApplicationContext APPLICATION_CONTEXT;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketService.APPLICATION_CONTEXT = applicationContext;
    }

    /**
     * 客户端和服务端成功建立连接后，自动调用 @OnOpen 标识的方法
     * Session由WebSocket自动建立
     */
    @OnOpen
    public void openConnection(Session session, @PathParam("token") String token) {
        try {
            // 游客则没有 session
            this.userId = TokenUtil.verifyToken(token);
        } catch (Exception ignored) {
        }
        // Session 赋值
        this.session = session;
        this.sessionId = session.getId();
        // 更新session，并更新在线人数。如果sessionId已存在，则删除掉session再添加session
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
        } else {
            ONLINE_COUNT.getAndIncrement();
        }
        // this：当前的 WebSocketService 实例
        WEBSOCKET_MAP.put(sessionId, this);
        logger.info("用户连接成功：" + sessionId + "，当前在线人数：" + ONLINE_COUNT.get());

        // 告知前端连接成功
        try {
            this.sendMessage("0");
        } catch (Exception e) {
            logger.error("连接异常！");
        }
    }

    /**
     * 关闭：服务端断了，刷新、关闭当前长连接的页面
     * 执行到此方法时，是在一个WebSocketService中，所以可以直接获取到变量 sessionId
     */
    @OnClose
    public void closeConnection() {
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
            ONLINE_COUNT.getAndDecrement();
        }
        logger.info("用户退出：" + sessionId + "，当前在线人数为：" + ONLINE_COUNT.get());
    }

    /**
     * 进行消息通讯时，如前端发送消息
     */
    @OnMessage
    public void onMessage(String message) {
        logger.info("用户信息：" + sessionId + "，报文：" + message);
        // 某一客户端发送弹幕后，要群发给所有客户端
        if (!StringUtil.isNullOrEmpty(message)) {
            try {
                // 群发消息：某一个客户端将消息推送了弹幕后，此弹幕需要推送给所有连接中的客户端
                //通过获取到每一个连接中的客户端对应的 WebSocketService，来获取所有客户端的 Session
                for (Map.Entry<String, WebSocketService> entry : WEBSOCKET_MAP.entrySet()) {
                    // 获取到每一个长连的客户端对应的 WebSocketService
                    WebSocketService webSocketService = entry.getValue();
                    //消费者发送消息
                    // if(webSocketService.session.isOpen()) {
                    //     webSocketService.sendMessage(message);
                    // }

                    // 在一个客户端对应的 WebSocketService 中，获取该客户端对应的 Session

                    DefaultMQProducer danmuProducer = (DefaultMQProducer) APPLICATION_CONTEXT.getBean("danmuProducer");

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("message", message);
                    jsonObject.put("sessionId", webSocketService.getSessionId());
                    Message msg = new Message(UserMomentsConstant.TOPIC_DANMUS, jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8));
                    RocketMQUtil.asyncSendMsg(msg, danmuProducer);
                }

                if (this.userId != null) { // 保存弹幕，需要用户id
                    // 保存弹幕至数据库
                    Danmu danmu = JSONObject.parseObject(message, Danmu.class);
                    danmu.setUserId(userId);
                    danmu.setCreateTime(new Date());

                    DanmuService danmuService = (DanmuService) APPLICATION_CONTEXT.getBean("danmuService");
                    danmuService.asyncAddDanmu(danmu);
                    //保存弹幕到redis
                    danmuService.addDanmusToRedis(danmu);
                }
            } catch (Exception e) {
                logger.error("弹幕接收出现问题！");
                e.printStackTrace();
            }
        }
    }

    @OnError
    public void onError(Throwable error) {
    }

    /**
     * 发送消息，this：WebSocketService实例
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 计划任务，统计当前在线人数
     * 指定了时间间隔，例如：5秒
     */
    @Scheduled(fixedRate = 5000)
    private void noticeOnlineCount() throws IOException {
        for (Map.Entry<String, WebSocketService> entry : WebSocketService.WEBSOCKET_MAP.entrySet()) {
            WebSocketService webSocketService = entry.getValue();
            if (webSocketService.session.isOpen()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("onlineCount", ONLINE_COUNT.get());
                jsonObject.put("msg", "当前在线人数为" + ONLINE_COUNT.get());
                webSocketService.sendMessage(jsonObject.toJSONString());
            }
        }
    }

    public Session getSession() {
        return session;
    }

    public String getSessionId() {
        return sessionId;
    }

}
