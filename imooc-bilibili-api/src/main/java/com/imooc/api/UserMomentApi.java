package com.imooc.api;

import com.imooc.service.UserMomentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author luf
 * @date 2022/03/08 00:33
 **/
@RestController
public class UserMomentApi {

    @Autowired
    private UserMomentService userMomentService;
}
