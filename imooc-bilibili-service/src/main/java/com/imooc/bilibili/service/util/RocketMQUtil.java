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
    public static void syncSendMsg(Message msg, DefaultMQProducer producer) throws Exception {
        SendResult result = producer.send(msg);
        System.out.println(result);
    }

    /**
     * 异步发送消息
     */
    public static void asyncSendMsg(Message msg, DefaultMQProducer producer) throws Exception {
        // 回调：SendCallback,用于异步发送消息后的回调,当消息发送成功后，会调用 onSuccess() 方法,当消息发送失败后，会调用 onException() 方法
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
