package com.imooc.api;

import com.imooc.bilibili.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author luf
 * @date 2022/03/02 21:03
 **/
@RestController
public class DemoApi {

    @Autowired
    DemoService demoService;

    @GetMapping("/query")
    public Map<String, Object> query(Long id) {
        return demoService.query(id);
    }
}
