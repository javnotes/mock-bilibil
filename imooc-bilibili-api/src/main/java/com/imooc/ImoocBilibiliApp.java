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
        // 初始化WebSocketService,将ApplicationContext传入,方便后续使用,比如获取bean,获取配置文件等,不用再通过静态方法获取
        //该方法在WebSocketService中,在启动时就会执行,所以不用担心WebSocketService中的bean为空
        //而且该方法是静态方法,所以不用担心多线程问题
        WebSocketService.setApplicationContext(applicationContext);
    }
}
