package com.imooc.bilibili.api;

import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author luf
 * @date 2022/11/03 11:39
 **/

@RestController
public class RESTfulAPi {

    private final Map<Integer, Map<String, Object>> dataMap;

    public RESTfulAPi() {
        dataMap = new HashMap<>();
        for (int i = 1; i < 3; i++) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", i);
            data.put("name", "name" + i);
            dataMap.put(i, data);
        }
    }
}
