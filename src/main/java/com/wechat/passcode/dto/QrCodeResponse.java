package com.wechat.passcode.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二维码响应DTO
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QrCodeResponse {

    /**
     * 二维码URL
     */
    private String qrcodeUrl;

    /**
     * 提示信息
     */
    private String tips;
}