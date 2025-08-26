package com.wechat.passcode.controller;

import com.wechat.passcode.service.WeChatMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.web.bind.annotation.*;

/**
 * 微信消息接收控制器
 * 处理微信公众号的消息和事件回调
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
public class WeChatController {

    private final WxMpService wxMpService;
    private final WeChatMessageService weChatMessageService;

    /**
     * 验证微信服务器配置
     * 微信服务器会发送GET请求进行验证
     * 
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param echostr 随机字符串
     * @return 验证成功返回echostr，失败返回error
     */
    @GetMapping("/message")
    public String verifySignature(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {
        
        log.info("微信服务器验证请求: signature={}, timestamp={}, nonce={}, echostr={}", 
            signature, timestamp, nonce, echostr);
        
        try {
            if (weChatMessageService.checkSignature(signature, timestamp, nonce)) {
                log.info("微信服务器验证成功");
                return echostr;
            } else {
                log.warn("微信服务器验证失败: 签名不匹配");
                return "error";
            }
        } catch (Exception e) {
            log.error("微信服务器验证异常: error={}", e.getMessage(), e);
            return "error";
        }
    }

    /**
     * 接收微信消息和事件
     * 微信服务器会发送POST请求推送消息
     * 
     * @param requestBody 请求体（XML格式）
     * @param signature 微信加密签名
     * @param timestamp 时间戳
     * @param nonce 随机数
     * @param openid 用户openid（可选）
     * @param encType 加密类型（可选）
     * @param msgSignature 消息签名（加密模式下使用）
     * @return XML格式的回复消息
     */
    @PostMapping(value = "/message", produces = "application/xml; charset=UTF-8")
    public String handleMessage(
            @RequestBody String requestBody,
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam(value = "openid", required = false) String openid,
            @RequestParam(value = "encrypt_type", required = false) String encType,
            @RequestParam(value = "msg_signature", required = false) String msgSignature) {
        
        log.info("收到微信消息: openid={}, encType={}, bodyLength={}", 
            openid, encType, requestBody.length());
        log.debug("微信消息内容: {}", requestBody);
        
        try {
            // 验证签名
            if (!weChatMessageService.checkSignature(signature, timestamp, nonce)) {
                log.warn("微信消息签名验证失败");
                return "success"; // 即使验证失败也返回success，避免微信重试
            }
            
            // 解析消息
            WxMpXmlMessage inMessage;
            if ("aes".equals(encType)) {
                // 加密消息
                inMessage = WxMpXmlMessage.fromEncryptedXml(requestBody, 
                    wxMpService.getWxMpConfigStorage(), timestamp, nonce, msgSignature);
            } else {
                // 明文消息
                inMessage = WxMpXmlMessage.fromXml(requestBody);
            }
            
            // 处理消息
            WxMpXmlOutMessage outMessage = handleIncomingMessage(inMessage);
            
            if (outMessage == null) {
                return "success";
            }
            
            // 加密回复消息（如果需要）
            if ("aes".equals(encType)) {
                return outMessage.toEncryptedXml(wxMpService.getWxMpConfigStorage());
            } else {
                return outMessage.toXml();
            }
            
        }  catch (Exception e) {
            log.error("处理微信消息异常: error={}", e.getMessage(), e);
        }
        
        return "success";
    }

    /**
     * 处理接收到的消息
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage handleIncomingMessage(WxMpXmlMessage inMessage) {
        String msgType = inMessage.getMsgType();
        String fromUser = inMessage.getFromUser();
        
        log.info("处理微信消息: fromUser={}, msgType={}", fromUser, msgType);
        
        try {
            switch (msgType) {
                case "text":
                    // 处理文本消息
                    return weChatMessageService.handleTextMessage(inMessage);
                    
                case "event":
                    // 处理事件消息
                    return handleEventMessage(inMessage);
                    
                case "image":
                    // 处理图片消息
                    return handleImageMessage(inMessage);
                    
                case "voice":
                    // 处理语音消息
                    return handleVoiceMessage(inMessage);
                    
                default:
                    log.debug("未处理的消息类型: msgType={}", msgType);
                    return createDefaultReply(inMessage);
            }
        }  catch (Exception e) {
            log.error("处理消息异常: fromUser={}, msgType={}, error={}", 
                fromUser, msgType, e.getMessage(), e);
            return createErrorReply(inMessage);
        }
    }

    /**
     * 处理事件消息
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage handleEventMessage(WxMpXmlMessage inMessage) {
        String event = inMessage.getEvent();
        String fromUser = inMessage.getFromUser();
        
        log.info("处理微信事件: fromUser={}, event={}", fromUser, event);
        
        try {
            switch (event) {
                case "subscribe":
                    // 关注事件
                    return weChatMessageService.handleSubscribeEvent(inMessage);
                    
                case "unsubscribe":
                    // 取消关注事件
                    return weChatMessageService.handleUnsubscribeEvent(inMessage);
                    
                case "CLICK":
                    // 菜单点击事件
                    return handleClickEvent(inMessage);
                    
                case "VIEW":
                    // 菜单跳转事件
                    return handleViewEvent(inMessage);
                    
                default:
                    log.debug("未处理的事件类型: event={}", event);
                    return null;
            }
        }catch (Exception e) {
            log.error("处理事件异常: fromUser={}, event={}, error={}", 
                fromUser, event, e.getMessage(), e);
            return createErrorReply(inMessage);
        }
    }

    /**
     * 处理图片消息
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage handleImageMessage(WxMpXmlMessage inMessage) {
        log.info("收到图片消息: fromUser={}, picUrl={}", 
            inMessage.getFromUser(), inMessage.getPicUrl());
        
        return WxMpXmlOutMessage.TEXT()
            .content("收到您的图片，如需获取口令请发送「公众号排版」")
            .fromUser(inMessage.getToUser())
            .toUser(inMessage.getFromUser())
            .build();
    }

    /**
     * 处理语音消息
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage handleVoiceMessage(WxMpXmlMessage inMessage) {
        log.info("收到语音消息: fromUser={}, mediaId={}", 
            inMessage.getFromUser(), inMessage.getMediaId());
        
        return WxMpXmlOutMessage.TEXT()
            .content("收到您的语音，如需获取口令请发送「公众号排版」")
            .fromUser(inMessage.getToUser())
            .toUser(inMessage.getFromUser())
            .build();
    }

    /**
     * 处理菜单点击事件
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage handleClickEvent(WxMpXmlMessage inMessage) {
        String eventKey = inMessage.getEventKey();
        log.info("菜单点击事件: fromUser={}, eventKey={}", 
            inMessage.getFromUser(), eventKey);
        
        try {
            // 根据不同的菜单key处理
            switch (eventKey) {
                case "GET_PASSCODE":
                    // 如果设置了获取口令的菜单，创建一个模拟的文本消息
                    WxMpXmlMessage simulatedTextMessage = new WxMpXmlMessage();
                    simulatedTextMessage.setFromUser(inMessage.getFromUser());
                    simulatedTextMessage.setToUser(inMessage.getToUser());
                    simulatedTextMessage.setMsgType("text");
                    simulatedTextMessage.setContent("公众号排版");
                    simulatedTextMessage.setCreateTime(inMessage.getCreateTime());
                    
                    return weChatMessageService.handleTextMessage(simulatedTextMessage);
                default:
                    return createDefaultReply(inMessage);
            }
        }  catch (Exception e) {
            log.error("处理菜单点击事件异常: fromUser={}, eventKey={}, error={}", 
                inMessage.getFromUser(), eventKey, e.getMessage(), e);
            return createErrorReply(inMessage);
        }
    }

    /**
     * 处理菜单跳转事件
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage handleViewEvent(WxMpXmlMessage inMessage) {
        String eventKey = inMessage.getEventKey();
        log.info("菜单跳转事件: fromUser={}, url={}", 
            inMessage.getFromUser(), eventKey);
        
        // 菜单跳转事件通常不需要回复
        return null;
    }

    /**
     * 创建默认回复
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage createDefaultReply(WxMpXmlMessage inMessage) {
        return WxMpXmlOutMessage.TEXT()
            .content("您好！发送「公众号排版」获取口令，发送「帮助」查看使用说明。")
            .fromUser(inMessage.getToUser())
            .toUser(inMessage.getFromUser())
            .build();
    }

    /**
     * 创建错误回复
     * 
     * @param inMessage 输入消息
     * @return 输出消息
     */
    private WxMpXmlOutMessage createErrorReply(WxMpXmlMessage inMessage) {
        return WxMpXmlOutMessage.TEXT()
            .content("系统繁忙，请稍后重试。如有问题请联系客服。")
            .fromUser(inMessage.getToUser())
            .toUser(inMessage.getFromUser())
            .build();
    }
}