package com.imooc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author luf
 * @date 2022/03/01 23:43
 **/
@SpringBootApplication
public class ImoocBilibiliApp {
    public static void main(String[] args) {
        ApplicationContext applicationContext = SpringApplication.run(ImoocBilibiliApp.class, args);
    }
}
