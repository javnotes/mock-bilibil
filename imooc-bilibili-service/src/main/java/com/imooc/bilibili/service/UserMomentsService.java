package com.imooc.bilibili.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.imooc.bilibili.dao.UserMomentsDao;
import com.imooc.bilibili.domain.UserMoment;
import com.imooc.bilibili.domain.constant.UserMomentsConstant;
import com.imooc.bilibili.service.util.RocketMQUtil;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * @author luf
 * @date 2022/03/08 22:07
 **/

@Service
public class UserMomentsService {

    @Autowired
    private UserMomentsDao userMomentsDao;

    //整个应用的上下文：可以提供所有的配置和Bean，这里主要是用于获取RocketMQConfig中定义的Bean momentsProducer、momentsConsumer
    //ApplicationContext: SpringBoot 框架提供的，用于获取整个应用的上下文,可以提供所有的配置和Bean,
    //这里主要是用于获取RocketMQConfig中定义的Bean momentsProducer、momentsConsumer
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 添加动态
     * 将动态推送给用户的粉丝,向MQ中发送一条消息,由MQ消费者消费，将动态存入Redis，也可同时将动态存入MySQL
     * 高级流程如下：1.将动态存入数据库
     * 2.将动态推送给用户的粉丝
     * 3.将动态存入MySQL
     * 4.将动态存入Redis,并设置过期时间,由MQ消费者消费，将动态存入Redis，同时将动态存入MySQL
     * 5.将动态存入ElasticSearch
     * 6.将动态存入MongoDB
     * 7.将动态存入HBase
     * 8.将动态存入Solr
     */
    public void addUserMoments(UserMoment userMoment) throws Exception {
        userMoment.setCreateTime(new Date());
        // 将动态存入数据库MySQL
        userMomentsDao.addUserMoments(userMoment);
        // 获取生产者
        DefaultMQProducer producer = (DefaultMQProducer) applicationContext.getBean("momentsProducer");
        // 将userMoment封装为消息,主题+消息内容（需要先将userMoment转为字符串，再转为byte数组）
        Message msg = new Message(UserMomentsConstant.TOPIC_MOMENTS, JSONObject.toJSONString(userMoment).getBytes(StandardCharsets.UTF_8));
        // 发送消息至MQ
        RocketMQUtil.syncSendMsg(msg, producer);
    }

    public List<UserMoment> getUserSubscribedMoments(Long userId) {
        String key = "subscribed-" + userId;
        String listStr = redisTemplate.opsForValue().get(key);
        return JSONArray.parseArray(listStr, UserMoment.class);
    }
}
