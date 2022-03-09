package com.imooc.api;

import com.imooc.api.support.UserSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author luf
 * @date 2022/03/09 20:58
 **/
@RestController
public class UserAuthApi {
    @Autowired
    private UserSupport userSupport;
    @Autowired


}
