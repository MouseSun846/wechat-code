package com.wechat.passcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.passcode.config.WeChatMpConfig;
import com.wechat.passcode.dto.PasscodeVerifyRequest;
import com.wechat.passcode.dto.PasscodeVerifyResponse;
import com.wechat.passcode.service.PasscodeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PasscodeController集成测试
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@WebMvcTest(PasscodeController.class)
class PasscodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasscodeService passcodeService;

    @MockBean
    private WeChatMpConfig weChatMpConfig;

    @Test
    void testGetQrCode_Success() throws Exception {
        // Mock配置
        when(weChatMpConfig.getQrCodeUrl()).thenReturn("https://example.com/qrcode");
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(true);

        // 执行请求
        mockMvc.perform(get("/api/passcode/qrcode"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.qrcodeUrl").value("https://example.com/qrcode"))
            .andExpect(jsonPath("$.data.tips").value("请扫码关注公众号，发送「公众号排版」获取口令"));
    }

    @Test
    void testGetQrCode_RateLimited() throws Exception {
        // Mock配置
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(false);

        // 执行请求
        mockMvc.perform(get("/api/passcode/qrcode"))
            .andExpect(status().is(429))
            .andExpect(jsonPath("$.code").value(429))
            .andExpect(jsonPath("$.message").value("请求过于频繁，请稍后再试"));
    }

    @Test
    void testVerifyPasscode_Success() throws Exception {
        // 准备数据
        PasscodeVerifyRequest request = new PasscodeVerifyRequest("ABC123", null);
        PasscodeVerifyResponse response = PasscodeVerifyResponse.success(LocalDateTime.now().plusMinutes(5));

        // Mock服务
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(true);
        when(passcodeService.verifyPasscode(eq("ABC123"), isNull())).thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/api/passcode/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("验证成功"))
            .andExpect(jsonPath("$.data.valid").value(true))
            .andExpect(jsonPath("$.data.message").value("口令验证成功"));
    }

    @Test
    void testVerifyPasscode_InvalidPasscode() throws Exception {
        // 准备数据
        PasscodeVerifyRequest request = new PasscodeVerifyRequest("ABC123", null);
        PasscodeVerifyResponse response = PasscodeVerifyResponse.failure("口令不存在或已过期");

        // Mock服务
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(true);
        when(passcodeService.verifyPasscode(eq("ABC123"), isNull())).thenReturn(response);

        // 执行请求
        mockMvc.perform(post("/api/passcode/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("口令不存在或已过期"));
    }

    @Test
    void testVerifyPasscode_InvalidInput() throws Exception {
        // 准备数据 - 空口令
        PasscodeVerifyRequest request = new PasscodeVerifyRequest("", null);

        // 执行请求
        mockMvc.perform(post("/api/passcode/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void testVerifyPasscode_RateLimited() throws Exception {
        // 准备数据
        PasscodeVerifyRequest request = new PasscodeVerifyRequest("ABC123", null);

        // Mock服务
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(false);

        // 执行请求
        mockMvc.perform(post("/api/passcode/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is(429))
            .andExpect(jsonPath("$.code").value(429))
            .andExpect(jsonPath("$.message").value("请求过于频繁，请稍后再试"));
    }

    @Test
    void testGetPasscodeStatus_Exists() throws Exception {
        // 准备数据
        String passcode = "ABC123";

        // Mock服务
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(true);
        when(passcodeService.getPasscodeInfo(passcode)).thenReturn(
            new com.wechat.passcode.model.PasscodeInfo(
                "test_open_id",
                "公众号排版",
                LocalDateTime.now(),
                LocalDateTime.now().plusMinutes(5),
                false,
                null
            )
        );

        // 执行请求
        mockMvc.perform(get("/api/passcode/status/{passcode}", passcode))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.exists").value(true))
            .andExpect(jsonPath("$.data.expired").value(false))
            .andExpect(jsonPath("$.data.used").value(false));
    }

    @Test
    void testGetPasscodeStatus_NotExists() throws Exception {
        // 准备数据
        String passcode = "ABC123";

        // Mock服务
        when(passcodeService.checkIpRateLimit(anyString())).thenReturn(true);
        when(passcodeService.getPasscodeInfo(passcode)).thenReturn(null);

        // 执行请求
        mockMvc.perform(get("/api/passcode/status/{passcode}", passcode))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("口令不存在"));
    }

    @Test
    void testHealth() throws Exception {
        // 执行请求
        mockMvc.perform(get("/api/passcode/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.version").value("1.0.0"));
    }
}