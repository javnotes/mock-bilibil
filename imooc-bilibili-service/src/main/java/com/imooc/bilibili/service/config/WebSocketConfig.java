package com.imooc.bilibili.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @description: WebSocket 配置类：把Websocket在服务器上的端点进行发现和导出，即发现Websocket服务
 * @author: luf
 * @date: 2022/3/23
 **/
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}
