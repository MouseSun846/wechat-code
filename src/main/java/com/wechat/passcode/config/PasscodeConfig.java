package com.wechat.passcode.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 口令相关配置类
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "passcode")
public class PasscodeConfig {

    /**
     * 口令长度
     */
    private int length = 6;

    /**
     * 口令有效期（秒）
     */
    private int ttl = 300;

    /**
     * 最大验证尝试次数
     */
    private int maxAttempts = 3;

    /**
     * 触发关键词
     */
    private String keyword = "公众号排版";

    /**
     * 排版插件
     */
    private String layoutPlugin = "排版插件";

    /**
     * 频率限制配置
     */
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        /**
         * 每个IP每分钟最大请求数
         */
        private int perIp = 10;

        /**
         * 每个用户每分钟最大请求数
         */
        private int perUser = 3;

        /**
         * 时间窗口（秒）
         */
        private int window = 60;
    }
}