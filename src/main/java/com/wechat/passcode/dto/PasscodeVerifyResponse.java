package com.wechat.passcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 口令验证响应DTO
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasscodeVerifyResponse {

    /**
     * 验证是否有效
     */
    private boolean valid;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 验证消息
     */
    private String message;

    /**
     * 创建成功响应
     */
    public static PasscodeVerifyResponse success(LocalDateTime expiredAt) {
        return new PasscodeVerifyResponse(true, expiredAt, "口令验证成功");
    }

    /**
     * 创建失败响应
     */
    public static PasscodeVerifyResponse failure(String message) {
        return new PasscodeVerifyResponse(false, null, message);
    }
}