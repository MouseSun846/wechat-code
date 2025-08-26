package com.wechat.passcode.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasscodeGenerator工具类测试
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
class PasscodeGeneratorTest {

    @Test
    void testGeneratePasscode() {
        // 测试生成6位口令
        String passcode = PasscodeGenerator.generatePasscode(6);
        
        assertNotNull(passcode);
        assertEquals(6, passcode.length());
        assertTrue(passcode.matches("[A-Z0-9]+"));
    }

    @Test
    void testGeneratePasscode_DifferentLengths() {
        // 测试不同长度
        for (int length = 4; length <= 10; length++) {
            String passcode = PasscodeGenerator.generatePasscode(length);
            assertEquals(length, passcode.length());
            assertTrue(passcode.matches("[A-Z0-9]+"));
        }
    }

    @Test
    void testGeneratePasscode_Uniqueness() {
        // 测试生成的口令是否唯一（概率测试）
        int count = 100;
        java.util.Set<String> passcodes = new java.util.HashSet<>();
        
        for (int i = 0; i < count; i++) {
            String passcode = PasscodeGenerator.generatePasscode(6);
            passcodes.add(passcode);
        }
        
        // 由于随机性，不应该有太多重复（允许少量重复）
        assertTrue(passcodes.size() > count * 0.9, "生成的口令重复率过高");
    }

    @Test
    void testGeneratePasscodeWithTimestamp() {
        // 测试不包含时间戳
        String passcode1 = PasscodeGenerator.generatePasscodeWithTimestamp(6, false);
        assertEquals(6, passcode1.length());
        assertTrue(passcode1.matches("[A-Z0-9]+"));
        
        // 测试包含时间戳
        String passcode2 = PasscodeGenerator.generatePasscodeWithTimestamp(6, true);
        assertEquals(6, passcode2.length());
        assertTrue(passcode2.matches("[A-Z0-9]+"));
    }

    @Test
    void testGenerateUserBasedPasscode() {
        String openId = "test_open_id_123";
        
        // 测试基于用户的口令生成
        String passcode1 = PasscodeGenerator.generateUserBasedPasscode(openId, 6);
        String passcode2 = PasscodeGenerator.generateUserBasedPasscode(openId, 6);
        
        assertNotNull(passcode1);
        assertNotNull(passcode2);
        assertEquals(6, passcode1.length());
        assertEquals(6, passcode2.length());
        assertTrue(passcode1.matches("[A-Z0-9]+"));
        assertTrue(passcode2.matches("[A-Z0-9]+"));
        
        // 注意：由于包含时间戳，同一用户在不同时间生成的口令应该不同
        // 这里我们只测试格式和长度
    }

    @Test
    void testIsValidFormat() {
        // 测试有效格式
        assertTrue(PasscodeGenerator.isValidFormat("ABC123"));
        assertTrue(PasscodeGenerator.isValidFormat("ABCD"));
        assertTrue(PasscodeGenerator.isValidFormat("1234567890"));
        assertTrue(PasscodeGenerator.isValidFormat("A1B2C3D4E5"));
        
        // 测试无效格式
        assertFalse(PasscodeGenerator.isValidFormat(null));
        assertFalse(PasscodeGenerator.isValidFormat(""));
        assertFalse(PasscodeGenerator.isValidFormat("   "));
        assertFalse(PasscodeGenerator.isValidFormat("abc")); // 小写字母
        assertFalse(PasscodeGenerator.isValidFormat("ABC")); // 太短
        assertFalse(PasscodeGenerator.isValidFormat("ABC123DEF456")); // 太长
        assertFalse(PasscodeGenerator.isValidFormat("ABC@123")); // 包含特殊字符
        assertFalse(PasscodeGenerator.isValidFormat("ABC 123")); // 包含空格
    }

    @Test
    void testIsValidFormat_EdgeCases() {
        // 测试边界情况
        assertTrue(PasscodeGenerator.isValidFormat("ABCD")); // 最短有效长度
        assertTrue(PasscodeGenerator.isValidFormat("ABCD123456")); // 最长有效长度
        assertFalse(PasscodeGenerator.isValidFormat("ABC")); // 短于最小长度
        assertFalse(PasscodeGenerator.isValidFormat("ABCD1234567")); // 长于最大长度
    }

    @Test
    void testCharacterSet() {
        // 测试字符集
        int count = 1000;
        java.util.Set<Character> usedChars = new java.util.HashSet<>();
        
        for (int i = 0; i < count; i++) {
            String passcode = PasscodeGenerator.generatePasscode(10);
            for (char c : passcode.toCharArray()) {
                usedChars.add(c);
            }
        }
        
        // 验证只使用了预期的字符
        for (Character c : usedChars) {
            assertTrue(Character.isUpperCase(c) || Character.isDigit(c), 
                "发现非预期字符: " + c);
        }
        
        // 验证使用了足够多样的字符（至少包含字母和数字）
        boolean hasLetter = usedChars.stream().anyMatch(Character::isLetter);
        boolean hasDigit = usedChars.stream().anyMatch(Character::isDigit);
        
        assertTrue(hasLetter, "生成的口令应该包含字母");
        assertTrue(hasDigit, "生成的口令应该包含数字");
    }
}