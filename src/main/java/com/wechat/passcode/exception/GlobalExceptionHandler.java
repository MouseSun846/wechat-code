package com.wechat.passcode.exception;

import com.wechat.passcode.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的异常，返回标准的错误响应格式
 * 
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        
        log.warn("业务异常: code={}, message={}, uri={}", 
            e.getCode(), e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.valueOf(e.getCode() >= 400 && e.getCode() < 600 ? e.getCode() : 500);
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        BindingResult bindingResult = e.getBindingResult();
        
        // 收集所有字段错误
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        // 构建错误消息
        String message = fieldErrors.values().stream()
            .collect(Collectors.joining("; "));
        
        log.warn("参数验证异常: message={}, fields={}, uri={}", 
            message, fieldErrors, request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.error(400, message);
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        
        log.warn("非法参数异常: message={}, uri={}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(400, "参数错误: " + e.getMessage());
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(
            NullPointerException e, HttpServletRequest request) {
        
        log.error("空指针异常: uri={}", request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(500, "系统内部错误");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理Redis连接异常
     */
    @ExceptionHandler({
        org.springframework.data.redis.RedisConnectionFailureException.class,
        org.springframework.data.redis.RedisSystemException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleRedisException(
            Exception e, HttpServletRequest request) {
        
        log.error("Redis异常: message={}, uri={}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(503, "缓存服务暂时不可用，请稍后重试");
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * 处理微信相关异常
     */
    @ExceptionHandler({
        me.chanjar.weixin.common.error.WxErrorException.class,
        me.chanjar.weixin.common.error.WxRuntimeException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleWeChatException(
            Exception e, HttpServletRequest request) {
        
        log.error("微信服务异常: message={}, uri={}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(502, "微信服务异常，请稍后重试");
        
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
    }

/**
     * 处理通用运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
            RuntimeException e, HttpServletRequest request) {
        
        log.error("运行时异常: message={}, uri={}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(500, "系统内部错误，请稍后重试");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 处理所有其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception e, HttpServletRequest request) {
        
        log.error("未知异常: message={}, uri={}", e.getMessage(), request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.error(500, "系统异常，请联系管理员");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 构建错误详情
     */
    private Map<String, Object> buildErrorDetails(Exception e, HttpServletRequest request) {
        Map<String, Object> details = new HashMap<>();
        details.put("timestamp", LocalDateTime.now());
        details.put("path", request.getRequestURI());
        details.put("method", request.getMethod());
        details.put("exception", e.getClass().getSimpleName());
        
        // 只在开发环境输出详细错误信息
        String profile = System.getProperty("spring.profiles.active", "dev");
        if ("dev".equals(profile)) {
            details.put("trace", e.getStackTrace());
        }
        
        return details;
    }
}