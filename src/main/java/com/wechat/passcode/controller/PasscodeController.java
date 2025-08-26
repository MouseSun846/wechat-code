package com.wechat.passcode.controller;

import com.wechat.passcode.config.WeChatMpConfig;
import com.wechat.passcode.dto.ApiResponse;
import com.wechat.passcode.dto.PasscodeVerifyRequest;
import com.wechat.passcode.dto.PasscodeVerifyResponse;
import com.wechat.passcode.dto.QrCodeResponse;
import com.wechat.passcode.service.PasscodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * API控制器
 * 提供口令相关的REST API接口
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/passcode")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class PasscodeController {

    private final PasscodeService passcodeService;
    private final WeChatMpConfig weChatMpConfig;

    /**
     * 获取二维码
     * 
     * @param request HTTP请求
     * @return 二维码信息
     */
    @GetMapping("/qrcode")
    public ApiResponse<QrCodeResponse> getQrCode(HttpServletRequest request) {
        String clientIp = getClientIpAddress(request);
        log.info("获取二维码请求: ip={}", clientIp);
        
        // 检查IP频率限制
        if (!passcodeService.checkIpRateLimit(clientIp)) {
            log.warn("IP请求频率超限: ip={}", clientIp);
            return ApiResponse.error(429, "请求过于频繁，请稍后再试");
        }
        
        try {
            QrCodeResponse response = new QrCodeResponse(
                weChatMpConfig.getQrCodeUrl(),
                String.format("请扫码关注公众号，发送「%s」获取口令", "公众号排版")
            );
            
            log.info("获取二维码成功: ip={}, qrcodeUrl={}", clientIp, response.getQrcodeUrl());
            return ApiResponse.success(response);
            
        } catch (Exception e) {
            log.error("获取二维码失败: ip={}, error={}", clientIp, e.getMessage(), e);
            return ApiResponse.error("获取二维码失败，请稍后重试");
        }
    }

    /**
     * 验证口令
     * 
     * @param request 验证请求
     * @param httpRequest HTTP请求
     * @return 验证结果
     */
    @PostMapping("/verify")
    public ApiResponse<PasscodeVerifyResponse> verifyPasscode(
            @Valid @RequestBody PasscodeVerifyRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        String passcode = request.getPasscode();
        String openId = request.getOpenId();
        
        log.info("验证口令请求: ip={}, passcode={}, openId={}", clientIp, passcode, openId);
        
        // 检查IP频率限制
        if (!passcodeService.checkIpRateLimit(clientIp)) {
            log.warn("IP请求频率超限: ip={}", clientIp);
            return ApiResponse.error(429, "请求过于频繁，请稍后再试");
        }
        
        try {
            PasscodeVerifyResponse response = passcodeService.verifyPasscode(passcode, openId);
            
            if (response.isValid()) {
                log.info("口令验证成功: ip={}, passcode={}", clientIp, passcode);
                return ApiResponse.success("验证成功", response);
            } else {
                log.warn("口令验证失败: ip={}, passcode={}, reason={}", 
                    clientIp, passcode, response.getMessage());
                return ApiResponse.error(400, response.getMessage());
            }
            
        } catch (Exception e) {
            log.error("口令验证异常: ip={}, passcode={}, error={}", 
                clientIp, passcode, e.getMessage(), e);
            return ApiResponse.error("验证失败，请稍后重试");
        }
    }

    /**
     * 获取口令状态（可选接口，用于调试）
     * 
     * @param passcode 口令
     * @param httpRequest HTTP请求
     * @return 口令状态
     */
    @GetMapping("/status/{passcode}")
    public ApiResponse<Object> getPasscodeStatus(
            @PathVariable String passcode,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        log.info("查询口令状态: ip={}, passcode={}", clientIp, passcode);
        
        // 检查IP频率限制
        if (!passcodeService.checkIpRateLimit(clientIp)) {
            return ApiResponse.error(429, "请求过于频繁，请稍后再试");
        }
        
        try {
            var info = passcodeService.getPasscodeInfo(passcode);
            if (info == null) {
                return ApiResponse.error(404, "口令不存在");
            }
            
            // 只返回必要的状态信息，不暴露敏感数据
            var status = new Object() {
                public final boolean exists = true;
                public final boolean expired = info.isExpired();
                public final boolean used = info.isUsed();
                public final String createTime = info.getCreateTime().toString();
                public final String expireTime = info.getExpireTime().toString();
            };
            
            return ApiResponse.success(status);
            
        } catch (Exception e) {
            log.error("查询口令状态失败: ip={}, passcode={}, error={}", 
                clientIp, passcode, e.getMessage(), e);
            return ApiResponse.error("查询失败，请稍后重试");
        }
    }

    /**
     * 健康检查接口
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ApiResponse<Object> health() {
        var health = new Object() {
            public final String status = "UP";
            public final long timestamp = System.currentTimeMillis();
            public final String version = "1.0.0";
        };
        
        return ApiResponse.success(health);
    }

    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}