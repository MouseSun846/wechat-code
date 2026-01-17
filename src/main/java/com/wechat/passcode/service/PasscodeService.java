package com.wechat.passcode.service;

import com.wechat.passcode.config.KeywordConfigReader;
import com.wechat.passcode.config.PasscodeConfig;
import com.wechat.passcode.dto.PasscodeVerifyResponse;
import com.wechat.passcode.model.PasscodeInfo;
import com.wechat.passcode.util.PasscodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 口令服务类
 * 处理口令生成、验证、生命周期管理等核心业务逻辑
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasscodeService {

    private final RedisService redisService;
    private final PasscodeConfig passcodeConfig;
    private final KeywordConfigReader keywordConfigReader;

    /**
     * 为用户生成口令
     * 
     * @param openId 用户openId
     * @param keyword 触发关键词
     * @return 生成的口令
     */
    public String generatePasscodeForUser(String openId, String keyword) {
        log.info("为用户生成口令: openId={}, keyword={}", openId, keyword);
        
        // 检查用户是否已有有效口令
        String existingPasscode = redisService.getUserPasscode(openId);
        if (existingPasscode != null) {
            PasscodeInfo existingInfo = redisService.getPasscode(existingPasscode);
            if (existingInfo != null && !existingInfo.isExpired() && !existingInfo.isUsed()) {
                log.info("用户已有有效口令: openId={}, passcode={}", openId, existingPasscode);
                return existingPasscode;
            } else {
                // 清理过期或已使用的口令
                cleanupUserPasscode(openId, existingPasscode);
            }
        }
        
        // 生成新口令
        String passcode = generateUniquePasscode();
        
        // 创建口令信息
        PasscodeInfo passcodeInfo = PasscodeInfo.create(openId, keyword, passcodeConfig.getTtl());
        
        // 存储到Redis
        redisService.storePasscode(passcode, passcodeInfo, passcodeConfig.getTtl());
        redisService.storeUserPasscode(openId, passcode, passcodeConfig.getTtl());
        
        log.info("生成口令成功: openId={}, passcode={}, ttl={}", 
            openId, passcode, passcodeConfig.getTtl());
        
        return passcode;
    }

    /**
     * 验证口令
     * 
     * @param passcode 口令
     * @param clientOpenId 客户端提供的openId（可选）
     * @return 验证结果
     */
    public PasscodeVerifyResponse verifyPasscode(String passcode, String clientOpenId) {
        log.info("验证口令: passcode={}, clientOpenId={}", passcode, clientOpenId);
        
        // 基础格式验证
        if (!PasscodeGenerator.isValidFormat(passcode)) {
            log.warn("口令格式不正确: passcode={}", passcode);
            return PasscodeVerifyResponse.failure("口令格式不正确");
        }
        
        // 从Redis获取口令信息
        PasscodeInfo passcodeInfo = redisService.getPasscode(passcode);
        if (passcodeInfo == null) {
            log.warn("口令不存在或已过期: passcode={}", passcode);
            return PasscodeVerifyResponse.failure("口令不存在或已过期");
        }
        
        // 检查是否已使用
        if (passcodeInfo.isUsed()) {
            log.warn("口令已被使用: passcode={}", passcode);
            return PasscodeVerifyResponse.failure("口令已被使用");
        }
        
        // 检查是否过期
        if (passcodeInfo.isExpired()) {
            log.warn("口令已过期: passcode={}, expireTime={}", 
                passcode, passcodeInfo.getExpireTime());
            // 清理过期口令
            cleanupUserPasscode(passcodeInfo.getOpenId(), passcode);
            return PasscodeVerifyResponse.failure("口令已过期");
        }
        
        // 验证成功，标记为已使用
        passcodeInfo.markAsUsed();
        redisService.storePasscode(passcode, passcodeInfo, passcodeConfig.getTtl());
        
        // 删除口令（一次性使用）
        redisService.deletePasscode(passcode);
        redisService.deleteUserPasscode(passcodeInfo.getOpenId());
        
        log.info("口令验证成功: passcode={}, openId={}", passcode, passcodeInfo.getOpenId());
        
        return PasscodeVerifyResponse.success(passcodeInfo.getExpireTime());
    }

    /**
     * 检查IP频率限制
     * 
     * @param ip IP地址
     * @return true表示未超限，false表示超限
     */
    public boolean checkIpRateLimit(String ip) {
        String limitKey = "ip:" + ip;
        return redisService.checkRateLimit(
            limitKey, 
            passcodeConfig.getRateLimit().getPerIp(),
            passcodeConfig.getRateLimit().getWindow()
        );
    }

    /**
     * 检查用户频率限制
     * 
     * @param openId 用户openId
     * @return true表示未超限，false表示超限
     */
    public boolean checkUserRateLimit(String openId) {
        String limitKey = "user:" + openId;
        return redisService.checkRateLimit(
            limitKey,
            passcodeConfig.getRateLimit().getPerUser(),
            passcodeConfig.getRateLimit().getWindow()
        );
    }

    /**
     * 获取用户当前有效口令
     * 
     * @param openId 用户openId
     * @return 有效口令，如果没有则返回null
     */
    public String getUserValidPasscode(String openId) {
        String passcode = redisService.getUserPasscode(openId);
        if (passcode == null) {
            return null;
        }
        
        PasscodeInfo info = redisService.getPasscode(passcode);
        if (info == null || info.isExpired() || info.isUsed()) {
            // 清理无效口令
            cleanupUserPasscode(openId, passcode);
            return null;
        }
        
        return passcode;
    }

    /**
     * 生成唯一口令
     * 确保生成的口令在Redis中不存在
     * 
     * @return 唯一口令
     */
    private String generateUniquePasscode() {
        String passcode;
        int attempts = 0;
        final int maxAttempts = 10;
        
        do {
            passcode = PasscodeGenerator.generatePasscode(passcodeConfig.getLength());
            attempts++;
            
            if (attempts > maxAttempts) {
                log.warn("生成唯一口令重试次数过多: attempts={}", attempts);
                // 如果重试次数过多，使用带时间戳的方式确保唯一性
                passcode = PasscodeGenerator.generatePasscodeWithTimestamp(
                    passcodeConfig.getLength(), true);
                break;
            }
            
        } while (redisService.getPasscode(passcode) != null);
        
        log.debug("生成唯一口令: passcode={}, attempts={}", passcode, attempts);
        return passcode;
    }

    /**
     * 清理用户口令
     * 
     * @param openId 用户openId
     * @param passcode 口令
     */
    private void cleanupUserPasscode(String openId, String passcode) {
        if (passcode != null) {
            redisService.deletePasscode(passcode);
        }
        redisService.deleteUserPasscode(openId);
        log.debug("清理用户口令: openId={}, passcode={}", openId, passcode);
    }

    /**
     * 获取口令统计信息
     * 
     * @param passcode 口令
     * @return 口令信息，如果不存在则返回null
     */
    public PasscodeInfo getPasscodeInfo(String passcode) {
        return redisService.getPasscode(passcode);
    }

    /**
     * 检查关键词是否匹配
     * 
     * @param keyword 用户输入的关键词
     * @return true表示匹配，false表示不匹配
     */
    public boolean isKeywordMatch(String keyword) {
        return keywordConfigReader.containsKeyword(keyword);
    }

    /**
     * 获取关键词对应的响应内容
     * 
     * @param keyword 关键词
     * @return 响应内容，如果不存在返回null
     */
    public String getKeywordResponse(String keyword) {
        return keywordConfigReader.getResponse(keyword);
    }

    /**
     * 匹配 排版插件
     */
    public boolean isPluginMatch(String plugin) {
        return passcodeConfig.getLayoutPlugin().equals(plugin);
    }

    /**
     * 获取菜单消息
     *
     * @return 菜单消息字符串
     */
    public String getMenuMessage() {
        Map<String, String> keywords = keywordConfigReader.getAllKeywords();
        
        StringBuilder menu = new StringBuilder();
        menu.append("📋 可用关键词菜单\n\n");
        menu.append("发送以下关键词获取对应内容：\n\n");
        
        // 添加口令相关
        menu.append("🔑 口令相关：\n");
        menu.append("  公众号排版 / 口令 → 获取排版口令\n\n");
        
        // 添加配置中的关键词
        if (!keywords.isEmpty()) {
            menu.append("📚 其他功能：\n");
            for (String keyword : keywords.keySet()) {
                menu.append("  ").append(keyword).append(" → 获取对应内容\n");
            }
            menu.append("\n");
        }
        
        menu.append("💡 提示：直接发送关键词即可获取详情\n");
        menu.append("例如：发送「排版插件」获取插件信息");
        
        return menu.toString();
    }

}