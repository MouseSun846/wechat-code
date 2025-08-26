# 微信公众号口令验证服务

# 公众号
![光影织梦](images/光影织梦.jpg)

## 项目介绍

这是一个基于Spring Boot 3.x、Redis和WxJava开发的微信公众号口令验证系统。用户通过扫描二维码关注公众号，发送特定关键词获取口令，然后在网页端验证口令。

### 主要功能

- 🔗 **二维码获取**：前端页面获取微信公众号二维码
- 📱 **公众号交互**：用户关注公众号并发送关键词获取口令
- 🔐 **口令验证**：网页端输入口令进行验证
- ⏰ **生命周期管理**：口令有效期控制和一次性使用
- 🚀 **频率限制**：防止恶意请求的频率限制机制
- 📊 **实时缓存**：基于Redis的高性能口令存储

### 技术栈

- **后端框架**：Spring Boot 3.2.5
- **缓存系统**：Redis 7.x
- **微信SDK**：WxJava 4.6.0
- **数据库**：MySQL 8.x（可选）
- **构建工具**：Maven 3.9.x
- **Java版本**：JDK 17+

## 项目结构

```
wechat-code/
├── src/main/java/com/wechat/passcode/
│   ├── config/                 # 配置类
│   │   ├── PasscodeConfig.java
│   │   ├── RedisConfig.java
│   │   └── WeChatMpConfig.java
│   ├── controller/             # 控制器
│   │   ├── PasscodeController.java
│   │   └── WeChatController.java
│   ├── dto/                    # 数据传输对象
│   │   ├── ApiResponse.java
│   │   ├── PasscodeVerifyRequest.java
│   │   ├── PasscodeVerifyResponse.java
│   │   └── QrCodeResponse.java
│   ├── exception/              # 异常处理
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── model/                  # 数据模型
│   │   └── PasscodeInfo.java
│   ├── service/                # 业务服务
│   │   ├── PasscodeService.java
│   │   ├── RedisService.java
│   │   └── WeChatMessageService.java
│   ├── util/                   # 工具类
│   │   └── PasscodeGenerator.java
│   └── WeChatPasscodeServiceApplication.java
├── src/main/resources/
│   ├── static/
│   │   └── index.html          # 前端页面
│   ├── application.yml         # 主配置文件
│   ├── application-dev.yml     # 开发环境配置
│   └── application-prod.yml    # 生产环境配置
├── src/test/                   # 测试代码
└── pom.xml                     # Maven配置
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Redis 6.0+
- MySQL 8.0+（可选）

### 1. 克隆项目

```bash
git clone <repository-url>
cd wechat-code
```

### 2. 配置环境

#### 配置Redis

```bash
# 启动Redis服务
redis-server
```

#### 配置微信公众号

在 `application.yml` 中配置微信公众号信息：

```yaml
wechat:
  mp:
    app-id: your_app_id
    secret: your_app_secret
    token: your_token
    aes-key: your_aes_key
    qr-code-url: your_qr_code_url
```

### 3. 编译运行

```bash
# 编译项目
mvn clean compile

# 运行测试
mvn test

# 启动应用
mvn spring-boot:run
```

### 4. 访问应用

- 前端页面：http://localhost:8080
- API文档：http://localhost:8080/actuator/health
- 健康检查：http://localhost:8080/api/passcode/health

## 使用流程

### 1. 网页端操作

1. 打开首页 `http://localhost:8080`
2. 点击"获取二维码"按钮
3. 扫描弹出的微信公众号二维码

### 2. 微信端操作

1. 关注微信公众号
2. 发送关键词：`公众号排版`
3. 接收系统回复的6位口令

### 3. 口令验证

1. 返回网页，在口令输入框中输入收到的口令
2. 点击"验证口令"按钮
3. 查看验证结果

## API接口

### 获取二维码

```http
GET /api/passcode/qrcode
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "qrcodeUrl": "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=xxx",
    "tips": "请扫码关注公众号，发送'公众号排版'获取口令"
  }
}
```

### 验证口令

```http
POST /api/passcode/verify
Content-Type: application/json

{
  "passcode": "ABC123",
  "openId": "optional_openid"
}
```

**响应示例：**
```json
{
  "code": 200,
  "message": "验证成功",
  "data": {
    "valid": true,
    "expiredAt": "2024-01-15T10:30:00",
    "message": "口令验证成功"
  }
}
```

### 微信回调接口

```http
# 验证微信服务器
GET /wechat/message?signature=xxx&timestamp=xxx&nonce=xxx&echostr=xxx

# 接收微信消息
POST /wechat/message
Content-Type: application/xml
```

## 配置说明

### 核心配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `passcode.length` | 口令长度 | 6 |
| `passcode.ttl` | 口令有效期（秒） | 300 |
| `passcode.keyword` | 触发关键词 | "公众号排版" |
| `passcode.rate-limit.per-ip` | IP频率限制 | 10/分钟 |
| `passcode.rate-limit.per-user` | 用户频率限制 | 3/分钟 |

### 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `WECHAT_APP_ID` | 微信应用ID | wx1234567890abcdef |
| `WECHAT_APP_SECRET` | 微信应用密钥 | your_app_secret |
| `WECHAT_TOKEN` | 微信Token | your_token |
| `REDIS_HOST` | Redis主机 | localhost |
| `REDIS_PORT` | Redis端口 | 6379 |
| `REDIS_PASSWORD` | Redis密码 | |

## 部署指南

### Docker部署

1. 构建镜像：

```dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

2. 构建和运行：

```bash
# 构建JAR包
mvn clean package -DskipTests

# 构建Docker镜像
docker build -t wechat-passcode-service .

# 运行容器
docker run -d \
  --name wechat-passcode \
  -p 8080:8080 \
  -e WECHAT_APP_ID=your_app_id \
  -e WECHAT_APP_SECRET=your_secret \
  -e REDIS_HOST=redis \
  wechat-passcode-service
```

### 生产环境部署

1. 设置生产环境配置：

```bash
export SPRING_PROFILES_ACTIVE=prod
export WECHAT_APP_ID=your_app_id
export WECHAT_APP_SECRET=your_secret
export REDIS_HOST=your_redis_host
export DB_HOST=your_db_host
```

2. 启动应用：

```bash
java -jar target/passcode-service-1.0.0.jar
```

## 监控与日志

### 健康检查

```bash
# 应用健康状态
curl http://localhost:8080/actuator/health

# 自定义健康检查
curl http://localhost:8080/api/passcode/health
```

### 日志配置

日志文件位置：`logs/wechat-passcode-service.log`

日志级别配置：
```yaml
logging:
  level:
    com.wechat.passcode: DEBUG
    org.springframework.data.redis: DEBUG
    me.chanjar.weixin: INFO
```

## 测试

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=PasscodeServiceTest

# 生成测试报告
mvn jacoco:report
```

### 测试覆盖率

- 单元测试覆盖率目标：≥85%
- 集成测试覆盖率目标：≥70%
- 核心业务逻辑覆盖率：100%

## 故障排除

### 常见问题

1. **Redis连接失败**
   - 检查Redis服务是否启动
   - 验证连接配置和密码

2. **微信回调验证失败**
   - 检查Token配置是否正确
   - 确认服务器URL可外网访问

3. **口令生成失败**
   - 检查Redis存储是否正常
   - 查看应用日志排查错误

4. **前端页面无法访问**
   - 确认静态资源配置正确
   - 检查防火墙和端口设置

### 日志分析

关键日志关键词：
- `口令生成成功`：口令创建
- `验证口令`：口令验证
- `频率限制超限`：请求频率过高
- `微信消息`：微信交互

## 贡献指南

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m 'Add amazing feature'`
4. 推送分支：`git push origin feature/amazing-feature`
5. 创建Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 联系方式

- 项目作者：WeChatPasscodeService Team
- 邮箱：support@example.com
- 项目主页：https://github.com/your-org/wechat-passcode-service

---

**⚠️ 注意事项：**

1. 生产环境请务必修改默认配置
2. 定期备份Redis数据
3. 监控系统资源使用情况
4. 及时更新依赖版本以修复安全漏洞

## 回调地址
https://lgy-in-dev.cnbita.com/dxjewkl7l-d7fe-2559-ab62-c3b95a066a82/wechat/message