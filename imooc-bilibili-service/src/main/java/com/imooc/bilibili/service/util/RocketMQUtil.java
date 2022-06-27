package com.imooc.bilibili.service.util;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RocketMQ工具类：调用MQ，发送消息
 *
 * @author luf
 */
public class RocketMQUtil {

    /**
     * 同步发送消息
     */
    public static void syncSendMsg(DefaultMQProducer producer, Message msg) throws Exception {
        SendResult result = producer.send(msg);
        System.out.println(result);
    }

    /**
     * 异步发送消息
     */
    public static void asyncSendMsg(DefaultMQProducer producer, Message msg) throws Exception {
        // 回调：SendCallback
        producer.send(msg, new SendCallback() {
            @Override
            //消息发送成功后的回调
            public void onSuccess(SendResult sendResult) {
                Logger logger = LoggerFactory.getLogger(RocketMQUtil.class);
                logger.info("异步发送消息成功，消息id：" + sendResult.getMsgId());
            }

            @Override
            //消息发送失败后的回调
            public void onException(Throwable e) {
                e.printStackTrace();
            }
        });
    }
}
