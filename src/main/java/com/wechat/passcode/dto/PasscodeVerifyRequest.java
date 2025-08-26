package com.wechat.passcode.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 口令验证请求DTO
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasscodeVerifyRequest {

    /**
     * 口令
     */
    @NotBlank(message = "口令不能为空")
    @Size(min = 6, max = 10, message = "口令长度必须在6-10位之间")
    private String passcode;

    /**
     * 用户openId（可选）
     */
    private String openId;
}