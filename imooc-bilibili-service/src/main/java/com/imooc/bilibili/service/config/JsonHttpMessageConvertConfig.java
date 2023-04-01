package com.imooc.bilibili.service.config;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Json信息转换配置，设置输出json信息的格式，其中使用到了 fastjson
 * @author luf
 * @date 2022/03/03 19:50
 **/
@Configuration
public class JsonHttpMessageConvertConfig {

    public static void main(String[] args) {
        List<Object> list = new ArrayList<>();
        Object obj = new Object();
        list.add(obj);
        list.add(obj);
        System.out.println(list.size());
        System.out.println(JSONObject.toJSONString(list));
        // 关闭循环引用检测
        System.out.println(JSONObject.toJSONString(list, SerializerFeature.DisableCircularReferenceDetect));
    }



    @Bean
    @Primary
    public HttpMessageConverters fastJsonHttpMessageConverts() {
        // FastJson-HttpMessageConverter 是 HttpMessageConverter 的实例
        FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
        FastJsonConfig fastJsonConfig = new FastJsonConfig();
        fastJsonConfig.setDateFormat("yyyy-MM-dd HH:mm:ss");
        // json 序列化配置：不省略空的String、List、Map，转为空字符串，
        // 排序：MapSortField，禁用循环引用：DisableCircularReferenceDetect
        fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat,
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullListAsEmpty,
                SerializerFeature.WriteMapNullValue,
                SerializerFeature.MapSortField,
                SerializerFeature.DisableCircularReferenceDetect);
        fastConverter.setFastJsonConfig(fastJsonConfig);
        return new HttpMessageConverters(fastConverter);
    }
}
