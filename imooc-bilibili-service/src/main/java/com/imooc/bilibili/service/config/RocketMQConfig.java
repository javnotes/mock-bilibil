package com.imooc.bilibili.service.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.domain.UserFollowing;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.service.UserFollowingService;
import com.imooc.bilibili.service.websocket.WebSocketService;
import io.netty.util.internal.StringUtil;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * RocketMQ 配置
 *
 * @author luf
 * @date 2022/03/07 22:50
 **/
@Configuration
public class RocketMQConfig {

    @Value("${rocketmq.name.server.address}")
    private String nameServerAddr;

    //RedisTemplate 是 SpringBoot 框架提供的，用于操作 Redis 的工具类
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserFollowingService userFollowingService;

    /**
     * 用户动态的生产者
     *
     * @Bean("momentsProducer")：将该方法返回的对象，注册到 SpringBoot 的容器中，名称为 momentsProducer
     */
    @Bean("momentsProducer")
    public DefaultMQProducer momentsProducer() throws Exception {
        // DefaultMQProducer：MQ的生产者,用于发送消息,消息发送到MQ后，MQ会将消息推给消费者,消费者通过监听器来抓取消息，并进一步地对消息进行处理
        // UserMomentsConstant.GROUP_MOMENTS：生产者组名,用于标识生产者,生产者组名相同的生产者，属于同一个生产者组,同一个生产者组的生产者，只会将消息推送给同一个消费者组的消费者
        DefaultMQProducer producer = new DefaultMQProducer(UserMomentsConstant.GROUP_MOMENTS);
        // 设置nameserver地址,用于连接MQ
        producer.setNamesrvAddr(nameServerAddr);
        // 启动生产者
        producer.start();
        // 返回生产者
        return producer;
    }

    /**
     * 用户动态的消费者:创建用户动态,用于监听用户动态相关的消息,并将消息推送给用户,用户通过 WebSocket 进行接收,并进行处理,如：推送给用户的浏览器
     */
    @Bean("momentsConsumer")
    public DefaultMQPushConsumer momentsConsumer() throws Exception {
        // DefaultMQPushConsumer：MQ的消费者,用于接收消息,消息发送到MQ后，MQ会将消息推给消费者,消费者通过监听器来抓取消息，并进一步地对消息进行处理
        // push 类型：订阅发布模式：代理人推送给消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(UserMomentsConstant.GROUP_MOMENTS);
        consumer.setNamesrvAddr(nameServerAddr);
        // 消费者需要订阅生产者，主题+二级主题/内容，*代表该主题下的所有内容
        // 此处订阅的是生产者的主题：UserMomentsConstant.TOPIC_MOMENTS，二级主题：*，代表该主题下的所有内容
        consumer.subscribe(UserMomentsConstant.TOPIC_MOMENTS, "*");
        // registerMessageListener：注册监听器,用于监听消息,消息发送到MQ后，MQ会将消息推给消费者,消费者通过监听器来抓取消息
        // 生产者将消息推给MQ后，MQ就将相关的消息推给消费者，消费者通过监听器来抓取消息，并进一步地对消息进行处理

        //MessageListenerConcurrently：并行监听器,用于监听消息,消息发送到MQ后，MQ会将消息推给消费者,消费者通过监听器来抓取消息，
        // 并进一步地对消息进行处理,并行监听器，可以同时处理多条消息,其中方法参数：消息+上下文
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            /**
             * consumeMessage:消费消息,用于消费消息,消息发送到MQ后，MQ会将消息推给消费者(Redis),消费者通过监听器来抓取消息，并进一步地对消息进行处理
             * ConsumeConcurrentlyStatus：消息消费状态,用于标识消息消费的状态,消息消费成功或者失败,消息消费成功后，MQ会将消息从队列中删除,消息消费失败后，MQ会将消息重新放回队列中
             * List<MessageExt>:消息列表,用于存储消息
             * ConsumeConcurrentlyContext：消息上下文,用于存储消息的上下文信息
             * 发布动态,每次发布动态，就可认为是向MQ中发送一条消息,List<MessageExt> msgs：消息列表中最多存储了一条消息
             * JSONObject.parseObject：是将str转化为相应的JSONObject对象，其中str是“键值对”形式的json字符串，转化为JSONObject对象之后就可以使用其内置的方法，进行各种处理了。
             * SONObject.toJavaObject：fastjson方法，将JSON对象转换为Java对象
             */
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt msg = msgs.get(0);
                if (msg == null) {
                    // 返回一个状态：消息消费成功
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
                // 消息不为NULL，从中获取实例userMoment，其中 msg.getBody() 为 byte 数组。
                String bodyStr = new String(msg.getBody());

                UserMoment userMoment = JSONObject.toJavaObject(JSONObject.parseObject(bodyStr), UserMoment.class);
                // 获取该用户Id目的是查找其粉丝，即哪些人关注了此人
                Long userId = userMoment.getUserId();
                List<UserFollowing> fanList = userFollowingService.getUserFans(userId);
                //遍历粉丝列表，将动态推送给粉丝
                for (UserFollowing fan : fanList) {
                    // Redis中的key，标识粉丝的动态列表--前端页面显示为我的-动态
                    String key = "subscribed-" + fan.getUserId();

                    // 动态列表 subscribedList
                    List<UserMoment> subscribedList;
                    // 在Redis中获取粉丝的动态列表,并将新动态添加到列表中
                    String subscribedListStr = redisTemplate.opsForValue().get(key);
                    if (StringUtil.isNullOrEmpty(subscribedListStr)) {
                        subscribedList = new ArrayList<>();
                    } else {
                        subscribedList = JSONArray.parseArray(subscribedListStr, UserMoment.class);
                    }
                    subscribedList.add(userMoment);
                    // 将该粉丝的动态列表存入Redis中,并设置过期时间
                    redisTemplate.opsForValue().set(key, JSONObject.toJSONString(subscribedList));
                }
                // 返回一个状态：消息消费成功
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        return consumer;
    }

    @Bean("danmuProducer")
    public DefaultMQProducer danmuProducer() throws Exception {
        // 实例化消息生产者 Producer
        DefaultMQProducer producer = new DefaultMQProducer(UserMomentsConstant.GROUP_DANMUS);
        // 设置 NameServer 的地址
        producer.setNamesrvAddr(nameServerAddr);
        // 启动 Producer 实例
        producer.start();
        return producer;
    }

    @Bean("danmuConsumer")
    public DefaultMQPushConsumer danmuConsumer() throws Exception {
        // 实例化消费者
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(UserMomentsConstant.GROUP_DANMUS);
        // 设置 NameServer 地址
        consumer.setNamesrvAddr(nameServerAddr);
        // 订阅一个/多个 Topic，以及 Tag 来过滤需要消费的消息
        consumer.subscribe(UserMomentsConstant.TOPIC_DANMUS, "*");

        // 注册回调实现类，处理从 broker 拉取回来的消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                MessageExt msg = msgs.get(0);
                byte[] msgByte = msg.getBody();
                String bodyStr = new String(msgByte);
                JSONObject jsonObject = JSONObject.parseObject(bodyStr);

                String sessionId = jsonObject.getString("sessionId");
                String message = jsonObject.getString("message");

                WebSocketService webSocketService = WebSocketService.WEBSOCKET_MAP.get(sessionId);
                if (webSocketService.getSession().isOpen()) {
                    try {
                        webSocketService.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 标记该消息已经被成功消费
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        consumer.start();
        return consumer;
    }
}
