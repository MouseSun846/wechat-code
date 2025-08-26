package com.wechat.passcode;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 微信公众号口令验证服务主启动类
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
public class WeChatPasscodeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeChatPasscodeServiceApplication.class, args);
    }
}