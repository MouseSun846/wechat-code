package com.wechat.passcode.service;

import com.wechat.passcode.config.PasscodeConfig;
import com.wechat.passcode.dto.PasscodeVerifyResponse;
import com.wechat.passcode.model.PasscodeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PasscodeService单元测试
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class PasscodeServiceTest {

    @Mock
    private RedisService redisService;

    @Mock
    private PasscodeConfig passcodeConfig;

    @InjectMocks
    private PasscodeService passcodeService;

    private PasscodeConfig.RateLimit rateLimit;

    @BeforeEach
    void setUp() {
        // 设置默认配置
        rateLimit = new PasscodeConfig.RateLimit();
        rateLimit.setPerIp(10);
        rateLimit.setPerUser(3);
        rateLimit.setWindow(60);

        when(passcodeConfig.getLength()).thenReturn(6);
        when(passcodeConfig.getTtl()).thenReturn(300);
        when(passcodeConfig.getKeyword()).thenReturn("公众号排版");
        when(passcodeConfig.getRateLimit()).thenReturn(rateLimit);
    }

    @Test
    void testGeneratePasscodeForUser_NewUser() {
        // 准备数据
        String openId = "test_open_id";
        String keyword = "公众号排版";

        // Mock行为
        when(redisService.getUserPasscode(openId)).thenReturn(null);
        when(redisService.getPasscode(anyString())).thenReturn(null);

        // 执行测试
        String passcode = passcodeService.generatePasscodeForUser(openId, keyword);

        // 验证结果
        assertNotNull(passcode);
        assertEquals(6, passcode.length());
        assertTrue(passcode.matches("[A-Z0-9]+"));

        // 验证方法调用
        verify(redisService).storePasscode(eq(passcode), any(PasscodeInfo.class), eq(300));
        verify(redisService).storeUserPasscode(eq(openId), eq(passcode), eq(300));
    }

    @Test
    void testGeneratePasscodeForUser_ExistingValidPasscode() {
        // 准备数据
        String openId = "test_open_id";
        String keyword = "公众号排版";
        String existingPasscode = "ABC123";
        PasscodeInfo existingInfo = PasscodeInfo.create(openId, keyword, 300);

        // Mock行为
        when(redisService.getUserPasscode(openId)).thenReturn(existingPasscode);
        when(redisService.getPasscode(existingPasscode)).thenReturn(existingInfo);

        // 执行测试
        String passcode = passcodeService.generatePasscodeForUser(openId, keyword);

        // 验证结果
        assertEquals(existingPasscode, passcode);

        // 验证不会创建新口令
        verify(redisService, never()).storePasscode(anyString(), any(PasscodeInfo.class), anyInt());
        verify(redisService, never()).storeUserPasscode(anyString(), anyString(), anyInt());
    }

    @Test
    void testVerifyPasscode_Success() {
        // 准备数据
        String passcode = "ABC123";
        String openId = "test_open_id";
        PasscodeInfo passcodeInfo = PasscodeInfo.create(openId, "公众号排版", 300);

        // Mock行为
        when(redisService.getPasscode(passcode)).thenReturn(passcodeInfo);

        // 执行测试
        PasscodeVerifyResponse response = passcodeService.verifyPasscode(passcode, null);

        // 验证结果
        assertTrue(response.isValid());
        assertEquals("口令验证成功", response.getMessage());
        assertNotNull(response.getExpiredAt());

        // 验证方法调用
        verify(redisService).deletePasscode(passcode);
        verify(redisService).deleteUserPasscode(openId);
    }

    @Test
    void testVerifyPasscode_InvalidFormat() {
        // 执行测试
        PasscodeVerifyResponse response = passcodeService.verifyPasscode("abc", null);

        // 验证结果
        assertFalse(response.isValid());
        assertEquals("口令格式不正确", response.getMessage());

        // 验证不会调用Redis
        verify(redisService, never()).getPasscode(anyString());
    }

    @Test
    void testVerifyPasscode_NotExists() {
        // 准备数据
        String passcode = "ABC123";

        // Mock行为
        when(redisService.getPasscode(passcode)).thenReturn(null);

        // 执行测试
        PasscodeVerifyResponse response = passcodeService.verifyPasscode(passcode, null);

        // 验证结果
        assertFalse(response.isValid());
        assertEquals("口令不存在或已过期", response.getMessage());
    }

    @Test
    void testVerifyPasscode_AlreadyUsed() {
        // 准备数据
        String passcode = "ABC123";
        String openId = "test_open_id";
        PasscodeInfo passcodeInfo = PasscodeInfo.create(openId, "公众号排版", 300);
        passcodeInfo.markAsUsed();

        // Mock行为
        when(redisService.getPasscode(passcode)).thenReturn(passcodeInfo);

        // 执行测试
        PasscodeVerifyResponse response = passcodeService.verifyPasscode(passcode, null);

        // 验证结果
        assertFalse(response.isValid());
        assertEquals("口令已被使用", response.getMessage());
    }

    @Test
    void testVerifyPasscode_Expired() {
        // 准备数据
        String passcode = "ABC123";
        String openId = "test_open_id";
        PasscodeInfo passcodeInfo = new PasscodeInfo(
            openId, 
            "公众号排版", 
            LocalDateTime.now().minusMinutes(10), 
            LocalDateTime.now().minusMinutes(5), // 5分钟前过期
            false, 
            null
        );

        // Mock行为
        when(redisService.getPasscode(passcode)).thenReturn(passcodeInfo);

        // 执行测试
        PasscodeVerifyResponse response = passcodeService.verifyPasscode(passcode, null);

        // 验证结果
        assertFalse(response.isValid());
        assertEquals("口令已过期", response.getMessage());
    }

    @Test
    void testCheckIpRateLimit() {
        // 准备数据
        String ip = "192.168.1.1";

        // Mock行为
        when(redisService.checkRateLimit("ip:" + ip, 10, 60)).thenReturn(true);

        // 执行测试
        boolean result = passcodeService.checkIpRateLimit(ip);

        // 验证结果
        assertTrue(result);
        verify(redisService).checkRateLimit("ip:" + ip, 10, 60);
    }

    @Test
    void testCheckUserRateLimit() {
        // 准备数据
        String openId = "test_open_id";

        // Mock行为
        when(redisService.checkRateLimit("user:" + openId, 3, 60)).thenReturn(false);

        // 执行测试
        boolean result = passcodeService.checkUserRateLimit(openId);

        // 验证结果
        assertFalse(result);
        verify(redisService).checkRateLimit("user:" + openId, 3, 60);
    }

    @Test
    void testGetUserValidPasscode_ValidPasscode() {
        // 准备数据
        String openId = "test_open_id";
        String passcode = "ABC123";
        PasscodeInfo passcodeInfo = PasscodeInfo.create(openId, "公众号排版", 300);

        // Mock行为
        when(redisService.getUserPasscode(openId)).thenReturn(passcode);
        when(redisService.getPasscode(passcode)).thenReturn(passcodeInfo);

        // 执行测试
        String result = passcodeService.getUserValidPasscode(openId);

        // 验证结果
        assertEquals(passcode, result);
    }

    @Test
    void testGetUserValidPasscode_InvalidPasscode() {
        // 准备数据
        String openId = "test_open_id";
        String passcode = "ABC123";

        // Mock行为
        when(redisService.getUserPasscode(openId)).thenReturn(passcode);
        when(redisService.getPasscode(passcode)).thenReturn(null);

        // 执行测试
        String result = passcodeService.getUserValidPasscode(openId);

        // 验证结果
        assertNull(result);
        verify(redisService).deletePasscode(passcode);
        verify(redisService).deleteUserPasscode(openId);
    }

    @Test
    void testIsKeywordMatch() {
        // 执行测试
        assertTrue(passcodeService.isKeywordMatch("公众号排版"));
        assertFalse(passcodeService.isKeywordMatch("其他关键词"));
        assertFalse(passcodeService.isKeywordMatch(null));
    }
}