package com.imooc;

import com.imooc.bilibili.service.websocket.WebSocketService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author luf
 * @date 2022/03/01 23:43
 **/
@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
//@EnableFeignClients(basePackages="com.imooc.bilibili.service.feign")
public class ImoocBilibiliApp {
    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(ImoocBilibiliApp.class, args);
        WebSocketService.setApplicationContext(applicationContext);
    }
}
