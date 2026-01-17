package com.wechat.passcode.service;

import com.wechat.passcode.config.PasscodeConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;
import org.springframework.stereotype.Service;

/**
 * 微信消息处理服务类
 * 处理用户消息、生成回复、口令分发等
 *
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatMessageService {

    private final PasscodeService passcodeService;
    private final PasscodeConfig passcodeConfig;
    private final WxMpService wxMpService;

    /**
     * 处理文本消息
     *
     * @param wxMessage 微信消息
     * @return 回复消息
     */
    public WxMpXmlOutMessage handleTextMessage(WxMpXmlMessage wxMessage) {
        String openId = wxMessage.getFromUser();
        String content = wxMessage.getContent();

        log.info("收到文本消息: openId={}, content={}", openId, content);

        // 检查用户频率限制
        if (!passcodeService.checkUserRateLimit(openId)) {
            log.warn("用户请求频率超限: openId={}", openId);
            return createTextReply(wxMessage, "请求过于频繁，请稍后再试");
        }

        // 处理关键词
        if ("公众号排版".equals(content.trim()) || "口令".equals(content.trim())) {
            return handlePasscodeRequest(wxMessage);
        } else if (passcodeService.isKeywordMatch(content.trim())) {
            return handlePluginMessage(wxMessage);
        }

        // 处理其他消息
        return handleOtherMessage(wxMessage, content);
    }

    private WxMpXmlOutMessage handlePluginMessage(WxMpXmlMessage wxMessage) {
        String content = wxMessage.getContent().trim();
        String response = passcodeService.getKeywordResponse(content);
        if (response == null) {
            response = "";
        }
        return createTextReply(wxMessage, response);
    }

    /**
     * 处理关注事件
     *
     * @param wxMessage 微信消息
     * @return 回复消息
     */
    public WxMpXmlOutMessage handleSubscribeEvent(WxMpXmlMessage wxMessage) {
        String openId = wxMessage.getFromUser();
        log.info("用户关注公众号: openId={}", openId);

        String welcomeMessage = String.format(
                "欢迎关注！\n\n" +
                        "🎯 发送「%s」获取口令\n" +
                        "⏰ 口令有效期：%d分钟\n" +
                        "🔄 口令仅可使用一次\n\n" +
                        "如有问题，请联系客服。",
                passcodeConfig.getKeyword(),
                passcodeConfig.getTtl() / 60
        );

        return createTextReply(wxMessage, welcomeMessage);
    }

    /**
     * 处理取消关注事件
     *
     * @param wxMessage 微信消息
     * @return 回复消息
     */
    public WxMpXmlOutMessage handleUnsubscribeEvent(WxMpXmlMessage wxMessage) {
        String openId = wxMessage.getFromUser();
        log.info("用户取消关注公众号: openId={}", openId);

        // 清理用户数据
        try {
            String existingPasscode = passcodeService.getUserValidPasscode(openId);
            if (existingPasscode != null) {
                // 这里可以记录日志或做其他清理工作
                log.info("清理取消关注用户的口令: openId={}, passcode={}", openId, existingPasscode);
            }
        } catch (Exception e) {
            log.error("清理取消关注用户数据失败: openId={}, error={}", openId, e.getMessage(), e);
        }

        return null; // 取消关注事件无需回复
    }

    /**
     * 处理口令请求
     *
     * @param wxMessage 微信消息
     * @return 回复消息
     */
    private WxMpXmlOutMessage handlePasscodeRequest(WxMpXmlMessage wxMessage) {
        String openId = wxMessage.getFromUser();

        try {
            // 检查用户是否已有有效口令
            String existingPasscode = passcodeService.getUserValidPasscode(openId);
            if (existingPasscode != null) {
                String message = String.format(
                        "您已有有效口令：\n\n" +
                                "🔑 %s\n\n" +
                                "⏰ 请在%d分钟内使用\n" +
                                "🔄 口令仅可使用一次",
                        existingPasscode,
                        passcodeConfig.getTtl() / 60
                );
                return createTextReply(wxMessage, message);
            }

            // 生成新口令
            String passcode = passcodeService.generatePasscodeForUser(openId, passcodeConfig.getKeyword());

            String message = String.format(
                    "口令生成成功：\n\n" +
                            "🔑 %s\n\n" +
                            "⏰ 有效期：%d分钟\n" +
                            "🔄 仅可使用一次\n" +
                            "💡 请复制口令到网页进行验证",
                    passcode,
                    passcodeConfig.getTtl() / 60
            );

            log.info("口令发送成功: openId={}, passcode={}", openId, passcode);
            return createTextReply(wxMessage, message);

        } catch (Exception e) {
            log.error("生成口令失败: openId={}, error={}", openId, e.getMessage(), e);
            return createTextReply(wxMessage, "系统繁忙，请稍后重试");
        }
    }

    /**
     * 处理其他消息
     *
     * @param wxMessage 微信消息
     * @param content 消息内容
     * @return 回复消息
     */
    private WxMpXmlOutMessage handleOtherMessage(WxMpXmlMessage wxMessage, String content) {
        log.debug("处理其他消息: openId={}, content={}", wxMessage.getFromUser(), content);

        // 简单的关键词匹配
        if (content.contains("帮助") || content.contains("help")) {
            return handleHelpMessage(wxMessage);
        }

        if (content.contains("口令") || content.contains("验证码")) {
            String message = String.format(
                    "获取口令请发送：\n\n" +
                            "🎯 %s\n\n" +
                            "如需其他帮助，请发送「帮助」",
                    passcodeConfig.getKeyword()
            );
            return createTextReply(wxMessage, message);
        }

        // 默认回复
        String message = String.format(
                "您好！👋\n\n" +
                        "🎯 发送「%s」获取口令\n" +
                        "❓ 发送「帮助」查看使用说明\n\n" +
                        "如有其他问题，请联系客服。",
                passcodeConfig.getKeyword()
        );

        return createTextReply(wxMessage, message);
    }

    /**
     * 处理帮助消息
     *
     * @param wxMessage 微信消息
     * @return 回复消息
     */
    private WxMpXmlOutMessage handleHelpMessage(WxMpXmlMessage wxMessage) {
        String message = String.format(
                "📖 使用说明\n\n" +
                        "1️⃣ 发送「%s」获取口令\n" +
                        "2️⃣ 复制口令到网页进行验证\n" +
                        "3️⃣ 口令有效期%d分钟\n" +
                        "4️⃣ 口令仅可使用一次\n\n" +
                        "⚠️ 注意事项：\n" +
                        "• 请及时使用口令\n" +
                        "• 不要泄露口令给他人\n" +
                        "• 每分钟最多获取%d次口令\n\n" +
                        "如需联系客服，请回复「客服」",
                passcodeConfig.getKeyword(),
                passcodeConfig.getTtl() / 60,
                passcodeConfig.getRateLimit().getPerUser()
        );

        return createTextReply(wxMessage, message);
    }

    /**
     * 创建文本回复
     *
     * @param inMessage 输入消息
     * @param content 回复内容
     * @return 输出消息
     */
    private WxMpXmlOutMessage createTextReply(WxMpXmlMessage inMessage, String content) {
        return WxMpXmlOutMessage.TEXT()
                .content(content)
                .fromUser(inMessage.getToUser())
                .toUser(inMessage.getFromUser())
                .build();
    }

    /**
     * 主动发送模板消息（可选功能）
     *
     * @param openId 用户openId
     * @param templateId 模板ID
     * @param data 模板数据
     * @throws WxErrorException 微信API异常
     */
    public void sendTemplateMessage(String openId, String templateId, Object data) throws WxErrorException {
        try {
            // 这里可以实现模板消息发送逻辑
            log.info("发送模板消息: openId={}, templateId={}", openId, templateId);
        } catch (Exception e) {
            log.error("发送模板消息异常: openId={}, error={}", openId, e.getMessage(), e);
            throw new WxErrorException("发送模板消息失败: " + e.getMessage());
        }
    }

    /**
     * 验证微信签名
     *
     * @param signature 微信签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @return true表示验证通过，false表示验证失败
     */
    public boolean checkSignature(String signature, String timestamp, String nonce) {
        try {
            return wxMpService.checkSignature(timestamp, nonce, signature);
        } catch (Exception e) {
            log.error("验证微信签名失败: signature={}, error={}", signature, e.getMessage(), e);
            return false;
        }
    }
}