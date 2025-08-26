package com.wechat.passcode.config;

import lombok.Data;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.config.impl.WxMpDefaultConfigImpl;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号配置类
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat.mp")
public class WeChatMpConfig {

    /**
     * 微信公众号应用ID
     */
    private String appId;

    /**
     * 微信公众号应用密钥
     */
    private String secret;

    /**
     * 微信公众号token
     */
    private String token;

    /**
     * 微信公众号AES密钥
     */
    private String aesKey;

    /**
     * 公众号二维码URL
     */
    private String qrCodeUrl;

    /**
     * 配置微信MP服务
     */
    @Bean
    public WxMpService wxMpService() {
        WxMpService wxMpService = new WxMpServiceImpl();
        wxMpService.setWxMpConfigStorage(wxMpConfigStorage());
        return wxMpService;
    }

    /**
     * 配置微信MP配置存储
     */
    @Bean
    public WxMpDefaultConfigImpl wxMpConfigStorage() {
        WxMpDefaultConfigImpl config = new WxMpDefaultConfigImpl();
        config.setAppId(this.appId);
        config.setSecret(this.secret);
        config.setToken(this.token);
        config.setAesKey(this.aesKey);
        return config;
    }
}