package com.wechat.passcode.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 口令模型类
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasscodeInfo {

    /**
     * 用户openId
     */
    private String openId;

    /**
     * 触发关键词
     */
    private String keyword;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 是否已使用
     */
    private boolean used;

    /**
     * 使用时间
     */
    private LocalDateTime useTime;

    /**
     * 创建新口令信息
     */
    public static PasscodeInfo create(String openId, String keyword, int ttlSeconds) {
        LocalDateTime now = LocalDateTime.now();
        return new PasscodeInfo(
            openId,
            keyword,
            now,
            now.plusSeconds(ttlSeconds),
            false,
            null
        );
    }

    /**
     * 标记为已使用
     */
    public void markAsUsed() {
        this.used = true;
        this.useTime = LocalDateTime.now();
    }

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }
}