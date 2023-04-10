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
 * 在 WebSocket API 中，浏览器和服务器只需要完成一次握手，两者之间就直接可以创建持久性的连接， 并进行双向数据传输。
 * WebSocket可以实现服务端与客户端的全双工通信，即服务端可以主动向客户端推送消息，客户端也可以主动向服务端发送消息，而且是实时的，
 * 不需要客户端或服务端进行轮询，这样就大大节省了服务器资源，提高了系统的性能。
 * WebSocket实现了客户端与服务端的全双工通信，是HTML5开始提供的一种在单个TCP连接上进行全双工通讯的协议。
 * WebSocket的出现，使得客户端和服务端之间的数据交换变得更加简单，允许服务端主动向客户端推送数据。
 * 全双工通信：即客户端和服务端都可以同时向对方发送或接收数据。
 * 注意：WebSocket是多例模式，每个客户端对应一个WebSocketService
 *
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
     * Session：服务端和客户端进行通信的一个会话，主要用于长连接通信，一个客户端对应一个Session，由WebSocket创建并把它保存在WebSocketService
     */
    private String sessionId;

    private Session session;

    private Long userId;

    /**
     * 存放所有的客户端对应的WebSocketService，key为 sessionId
     * SpringBoot 默认是单例模式，只会注入一次；但一个客户端对应一个WebSocketService，这是多例模式
     */
    public static final ConcurrentHashMap<String, WebSocketService> WEBSOCKET_MAP = new ConcurrentHashMap<>();

    /**
     * 这个就是启动类里的 applicationContext,是单例的. ApplicationContext applicationContext = SpringApplication.run(ImoocBilibiliApp.class, args);
     * staic：多例共用, 也就是说，所有的客户端的WebSocketService都共用这个 applicationContext.而且是静态的，所以只会注入一次
     * 然而@Autowired注解是按类型注入的，所以只会注入一次，也就是说，所有的客户端都是同一个 applicationContext
     */
    private static ApplicationContext APPLICATION_CONTEXT;
    // 通过静态方法来注入WebSocketService的applicationContext，这样就可以在WebSocketService中获取到applicationContext，从而获取到bean,
    // 在应用启动时，让Spring会调用setApplicationContext()方法，将applicationContext注入进来
    public static void setApplicationContext(ApplicationContext applicationContext) {
        WebSocketService.APPLICATION_CONTEXT = applicationContext;
    }

    /**
     * @OnOpen：标识该方法是一个连接建立成功的回调方法,当客户端连接成功后，会自动调用该方法,并传入Session,用于后续通信,可以通过Session给客户端发送消息,也可以通过Session获取客户端的信息,如IP地址等 Session由WebSocket自动建立, 并传入, 作用是：服务端和客户端进行通信的一个会话，主要用于长连接通信
     */
    @OnOpen
    public void openConnection(Session session, @PathParam("token") String token) {
        try {
            // 游客则没有 session
            this.userId = TokenUtil.verifyToken(token);
        } catch (Exception ignored) {
            // ...
        }
        // Session 赋值
        this.session = session;
        this.sessionId = session.getId();
        // 更新session，并更新在线人数。如果sessionId已存在，则删除掉session再添加session
        if (WEBSOCKET_MAP.containsKey(sessionId)) {
            WEBSOCKET_MAP.remove(sessionId);
        } else {
            // 说明是新的连接，所以在线人数加一
            // 该方法是原子性的，线程安全的
            ONLINE_COUNT.getAndIncrement();
        }
        // this：当前的 WebSocketService 实例
        WEBSOCKET_MAP.put(sessionId, this);
        logger.info("用户连接成功：" + sessionId + "，当前在线人数：" + ONLINE_COUNT.get());

        // 连接成功后，发送消息给客户端. 本项目中，0：表示连接成功, 1：表示连接失败
        try {

            this.sendMessage("0");
        } catch (Exception e) {
            logger.error("连接异常！");
        }
    }

    /**
     * @OnClose：标识该方法是一个连接关闭的回调方法,当客户端连接关闭后,会自动调用该方法.与@OnOpen方法对应
     * 关闭：服务端断了，刷新、关闭当前长连接的页面
     * 执行到此方法时，是对应某个客户的 WebSocketService 中，所以可以直接获取到变量 sessionId
     * WebSocketService 中的变量是多例的，所以不会影响其他客户的 WebSocketService.
     * WebSocket的连接是长连接，所以当客户端关闭连接时，服务端也要关闭连接，否则会造成资源浪费，所以需要在服务端关闭连接，同时也要在客户端关闭连接，否则会造成连接泄露。
     * 而且WebSocket的关闭是自动的，不需要手动关闭，只需要在客户端关闭连接即可。
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
     * @OnMessage：标识该方法是一个接收客户端消息的回调方法,当客户端发送消息后,会自动调用该方法,并传入客户端发送的消息
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

    /**
     * @OnError：标识该方法是一个连接错误的回调方法,当客户端连接错误后,会自动调用该方法
     * @param error
     */
    @OnError
    public void onError(Throwable error) {
    }

    /**
     * 发送消息，this：WebSocketService实例
     */
    public void sendMessage(String message) throws IOException {
        // 通过 WebSocketService 中的 Session 来发送消息
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
