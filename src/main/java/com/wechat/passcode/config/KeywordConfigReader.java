package com.wechat.passcode.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
@EnableScheduling
public class KeywordConfigReader implements ApplicationRunner {

    private Map<String, String> keywordResponses = new HashMap<>();

    private static final List<String> CONFIG_URLS = Arrays.asList(
        "https://cdn.gh-proxy.org/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json",
        "https://g.blfrp.cn/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json",
        "https://fastly.jsdelivr.net/gh/MouseSun846/wechat-code@master/config.json",
        "https://gh.catmak.name/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json",
        "https://ghfast.top/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json",
        "https://hub.glowp.xyz/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json",
        "https://hk.gh-proxy.org/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json",
        "https://wget.la/https://raw.githubusercontent.com/MouseSun846/wechat-code/master/config.json"
    );

    @Override
    public void run(ApplicationArguments args) throws Exception {
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        for (int i = 0; i < CONFIG_URLS.size(); i++) {
            String url = CONFIG_URLS.get(i);
            try {
                log.info("尝试从 URL {} 加载配置 ({}/{})\n", url, i + 1, CONFIG_URLS.size());
                
                String configContent = fetchConfigFromUrl(url);
                if (configContent != null && !configContent.isEmpty()) {
                    KeywordConfig config = objectMapper.readValue(configContent, KeywordConfig.class);
                    this.keywordResponses = config.getKeywords();
                    log.info("成功从 URL {} 加载关键词配置，共 {} 个关键词", url, keywordResponses.size());
                    
                    // 打印加载的关键词列表
                    if (!keywordResponses.isEmpty()) {
                        log.info("已加载的关键词列表：");
                        for (String keyword : keywordResponses.keySet()) {
                            log.info("  - {}", keyword);
                        }
                    }
                    
                    return;
                } else {
                    log.warn("URL {} 返回内容为空，尝试下一个 URL", url);
                }
            } catch (IOException e) {
                log.warn("从 URL {} 加载配置失败: {}，尝试下一个 URL", url, e.getMessage());
            }
        }
        
        log.error("所有配置 URL 都尝试过了，均无法加载配置");
        log.error("用户将无法使用关键词功能，请检查网络连接或配置 URL");
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
     * 定时同步配置
     * 每天凌晨 2 点执行一次
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncConfig() {
        log.info("开始定时同步配置...");
        loadConfig();
        log.info("定时同步配置完成");
    }

    /**
     * 关键词配置类
     */
    @Data
    public static class KeywordConfig {
        private Map<String, String> keywords = new HashMap<>();
    }
}
