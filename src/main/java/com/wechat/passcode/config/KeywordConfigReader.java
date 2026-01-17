package com.wechat.passcode.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 关键词配置读取器
 * 从根路径 config.json 文件中读取关键词和响应内容
 *
 * @author WeChatPasscodeService
 * @version 1.0.0
 */
@Slf4j
@Component
public class KeywordConfigReader implements ApplicationRunner {

    private Map<String, String> keywordResponses = new HashMap<>();

    private static final String CONFIG_URL = "https://r.jina.ai/https://github.com/MouseSun846/wechat-code/blob/master/config.json";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 从远程 URL 加载配置
            String configContent = fetchConfigFromUrl(CONFIG_URL);
            if (configContent != null && !configContent.isEmpty()) {
                KeywordConfig config = objectMapper.readValue(configContent, KeywordConfig.class);
                this.keywordResponses = config.getKeywords();
                log.info("成功从远程 URL 加载关键词配置，共 {} 个关键词", keywordResponses.size());
            } else {
                log.error("远程配置内容为空");
            }
        } catch (IOException e) {
            log.error("从远程 URL 加载配置失败: {}", e.getMessage());
        }
    }

    /**
     * 从远程 URL 获取配置内容
     *
     * @param url 配置文件 URL
     * @return 配置内容，如果获取失败返回 null
     */
    private String fetchConfigFromUrl(String url) throws IOException {
        URL configUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) configUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } else {
            log.warn("远程配置请求失败，响应码: {}", responseCode);
            return null;
        }
    }


    /**
     * 获取关键词对应的响应内容
     *
     * @param keyword 关键词
     * @return 响应内容，如果不存在返回 null
     */
    public String getResponse(String keyword) {
        return keywordResponses.get(keyword);
    }

    /**
     * 检查是否包含指定关键词
     *
     * @param keyword 关键词
     * @return 是否包含
     */
    public boolean containsKeyword(String keyword) {
        return keywordResponses.containsKey(keyword);
    }

    /**
     * 获取所有关键词
     *
     * @return 关键词列表
     */
    public Map<String, String> getAllKeywords() {
        return new HashMap<>(keywordResponses);
    }

    /**
     * 关键词配置类
     */
    @Data
    public static class KeywordConfig {
        private Map<String, String> keywords = new HashMap<>();
    }
}
