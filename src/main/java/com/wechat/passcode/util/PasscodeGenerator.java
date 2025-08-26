package com.wechat.passcode.util;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 口令生成工具类
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
public class PasscodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成指定长度的随机口令
     * 
     * @param length 口令长度
     * @return 随机口令
     */
    public static String generatePasscode(int length) {
        StringBuilder passcode = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            passcode.append(CHARACTERS.charAt(index));
        }
        
        return passcode.toString();
    }

    /**
     * 生成带时间戳的口令
     * 
     * @param length 基础长度
     * @param includeTimestamp 是否包含时间戳
     * @return 口令
     */
    public static String generatePasscodeWithTimestamp(int length, boolean includeTimestamp) {
        String basePasscode = generatePasscode(length);
        
        if (includeTimestamp) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("mmss"));
            return basePasscode.substring(0, length - 2) + timestamp.substring(0, 2);
        }
        
        return basePasscode;
    }

    /**
     * 生成基于用户信息的口令
     * 
     * @param openId 用户openId
     * @param length 口令长度
     * @return 口令
     */
    public static String generateUserBasedPasscode(String openId, int length) {
        // 使用openId的hash作为种子，确保同一用户生成的口令具有一定规律但仍然随机
        int seed = Math.abs(openId.hashCode());
        SecureRandom userRandom = new SecureRandom();
        userRandom.setSeed(seed + System.currentTimeMillis());
        
        StringBuilder passcode = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = userRandom.nextInt(CHARACTERS.length());
            passcode.append(CHARACTERS.charAt(index));
        }
        
        return passcode.toString();
    }

    /**
     * 验证口令格式是否正确
     * 
     * @param passcode 口令
     * @return true表示格式正确，false表示格式错误
     */
    public static boolean isValidFormat(String passcode) {
        if (passcode == null || passcode.trim().isEmpty()) {
            return false;
        }
        
        // 检查长度
        if (passcode.length() < 4 || passcode.length() > 10) {
            return false;
        }
        
        // 检查字符是否合法
        return passcode.matches("[A-Z0-9]+");
    }
}