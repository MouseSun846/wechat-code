package com.wechat.passcode.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.passcode.model.PasscodeInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务类
 * 处理口令的存储、查询、删除等操作
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Key前缀
    private static final String PASSCODE_PREFIX = "passcode:";
    private static final String USER_PASSCODE_PREFIX = "user_passcode:";
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 存储口令信息
     * 
     * @param passcode 口令
     * @param passcodeInfo 口令信息
     * @param ttlSeconds 过期时间（秒）
     */
    public void storePasscode(String passcode, PasscodeInfo passcodeInfo, int ttlSeconds) {
        try {
            String key = PASSCODE_PREFIX + passcode;
            String value = objectMapper.writeValueAsString(passcodeInfo);
            
            stringRedisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("存储口令成功: passcode={}, openId={}, ttl={}", 
                passcode, passcodeInfo.getOpenId(), ttlSeconds);
        } catch (JsonProcessingException e) {
            log.error("存储口令失败: passcode={}, error={}", passcode, e.getMessage(), e);
            throw new RuntimeException("存储口令失败", e);
        }
    }

    /**
     * 获取口令信息
     * 
     * @param passcode 口令
     * @return 口令信息，如果不存在则返回null
     */
    public PasscodeInfo getPasscode(String passcode) {
        try {
            String key = PASSCODE_PREFIX + passcode;
            String value = stringRedisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.debug("口令不存在: passcode={}", passcode);
                return null;
            }
            
            PasscodeInfo info = objectMapper.readValue(value, PasscodeInfo.class);
            log.debug("获取口令成功: passcode={}, openId={}", passcode, info.getOpenId());
            return info;
        } catch (JsonProcessingException e) {
            log.error("获取口令失败: passcode={}, error={}", passcode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 删除口令
     * 
     * @param passcode 口令
     */
    public void deletePasscode(String passcode) {
        String key = PASSCODE_PREFIX + passcode;
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("删除口令: passcode={}, deleted={}", passcode, deleted);
    }

    /**
     * 存储用户口令映射
     * 
     * @param openId 用户openId
     * @param passcode 口令
     * @param ttlSeconds 过期时间（秒）
     */
    public void storeUserPasscode(String openId, String passcode, int ttlSeconds) {
        String key = USER_PASSCODE_PREFIX + openId;
        stringRedisTemplate.opsForValue().set(key, passcode, ttlSeconds, TimeUnit.SECONDS);
        log.debug("存储用户口令映射: openId={}, passcode={}", openId, passcode);
    }

    /**
     * 获取用户的口令
     * 
     * @param openId 用户openId
     * @return 用户的口令，如果不存在则返回null
     */
    public String getUserPasscode(String openId) {
        String key = USER_PASSCODE_PREFIX + openId;
        String passcode = stringRedisTemplate.opsForValue().get(key);
        log.debug("获取用户口令: openId={}, passcode={}", openId, passcode);
        return passcode;
    }

    /**
     * 删除用户口令映射
     * 
     * @param openId 用户openId
     */
    public void deleteUserPasscode(String openId) {
        String key = USER_PASSCODE_PREFIX + openId;
        Boolean deleted = stringRedisTemplate.delete(key);
        log.debug("删除用户口令映射: openId={}, deleted={}", openId, deleted);
    }

    /**
     * 检查频率限制
     * 
     * @param limitKey 限制key
     * @param maxRequests 最大请求数
     * @param windowSeconds 时间窗口（秒）
     * @return true表示未超限，false表示超限
     */
    public boolean checkRateLimit(String limitKey, int maxRequests, int windowSeconds) {
        String key = RATE_LIMIT_PREFIX + limitKey;
        
        // 获取当前计数
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (currentCount >= maxRequests) {
            log.warn("频率限制超限: key={}, current={}, max={}", limitKey, currentCount, maxRequests);
            return false;
        }
        
        // 增加计数
        Long newCount = stringRedisTemplate.opsForValue().increment(key);
        
        // 如果是第一次设置，设置过期时间
        if (newCount == 1) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        
        log.debug("频率限制检查: key={}, count={}, max={}", limitKey, newCount, maxRequests);
        return true;
    }

    /**
     * 检查key是否存在
     * 
     * @param key Redis key
     * @return true表示存在，false表示不存在
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    /**
     * 设置key的过期时间
     * 
     * @param key Redis key
     * @param seconds 过期时间（秒）
     */
    public void expire(String key, int seconds) {
        stringRedisTemplate.expire(key, Duration.ofSeconds(seconds));
    }

    /**
     * 获取key的剩余过期时间
     * 
     * @param key Redis key
     * @return 剩余过期时间（秒），-1表示永不过期，-2表示key不存在
     */
    public long getExpire(String key) {
        return stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}