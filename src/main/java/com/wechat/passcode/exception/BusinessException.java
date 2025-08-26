package com.wechat.passcode.exception;

import lombok.Getter;

/**
 * 业务异常基类
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message) {
        this(500, message);
    }

    public BusinessException(String message, Throwable cause) {
        this(500, message, cause);
    }
}

/**
 * 参数验证异常
 */
class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(400, message);
    }
}

/**
 * 频率限制异常
 */
class RateLimitException extends BusinessException {
    public RateLimitException(String message) {
        super(429, message);
    }
}

/**
 * 口令相关异常
 */
class PasscodeException extends BusinessException {
    public PasscodeException(String message) {
        super(400, message);
    }
    
    public PasscodeException(int code, String message) {
        super(code, message);
    }
}

/**
 * 微信相关异常
 */
class WeChatException extends BusinessException {
    public WeChatException(String message) {
        super(500, message);
    }
    
    public WeChatException(String message, Throwable cause) {
        super(500, message, cause);
    }
}

/**
 * Redis相关异常
 */
class RedisException extends BusinessException {
    public RedisException(String message) {
        super(500, message);
    }
    
    public RedisException(String message, Throwable cause) {
        super(500, message, cause);
    }
}