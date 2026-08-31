# Pipker Framework Backend

Pipker Framework 的 Maven 后端工程。该工程以 **Starter 技术基础设施 + 业务模块 + Server 组装模块** 划分职责，所有模块继承根 POM 统一管理的 Spring Boot `4.1.1`、Java `21` 和内部模块版本。

## 模块结构

```text
backend/
├── pom.xml                                      # 根 Parent / Reactor
├── pipker-starters/                              # Starter 聚合模块（pom）
│   ├── pom.xml
│   ├── pipker-spring-boot-starter-redis/        # Redis 技术基础设施 Starter
│   ├── pipker-spring-boot-starter-log/          # 技术日志 Starter
│   ├── pipker-spring-boot-starter-satoken/      # Sa-Token 鉴权与会话 Starter
│   └── pipker-spring-boot-starter-security/     # 密码哈希与字段加密 Starter
├── pipker-business/                              # 业务聚合模块（pom）
│   ├── pipker-business-common/                   # 公共业务能力模块
│   └── pipker-business-api/                      # 单独业务能力模块
└── pipker-server/                                # 可执行 Spring Boot Server
```

## 模块职责

| 模块 | Maven 坐标 | 打包类型 | 职责 |
| --- | --- | --- | --- |
| `pipker-framework` | `com.pipker:pipker-framework` | `pom` | 根 Parent 与 Maven Reactor；统一管理 Spring Boot、Java、编码和内部模块版本。 |
| `pipker-starters` | `com.pipker:pipker-starters` | `pom` | 聚合 Pipker 的技术 Starter，仅参与 Reactor 构建，不产出业务 Jar。 |
| `pipker-spring-boot-starter-redis` | `com.pipker:pipker-spring-boot-starter-redis` | `jar` | Redis 技术基础设施边界；当前依赖 Spring Boot 官方 `spring-boot-starter-data-redis`，由 Spring Boot 提供 Redis 自动配置。 |
| `pipker-spring-boot-starter-log` | `com.pipker:pipker-spring-boot-starter-log` | `jar` | 技术日志基础设施边界；当前依赖 Spring Boot 官方 `spring-boot-starter-logging`，保留日志格式、MDC / TraceId、请求日志和日志脱敏的后续扩展位置。 |
| `pipker-spring-boot-starter-satoken` | `com.pipker:pipker-spring-boot-starter-satoken` | `jar` | 基于 Sa-Token `1.46.0` 的基础鉴权能力；提供 Bearer Token、统一登录会话门面、全局 API 过滤和内存/Redis DAO 切换。 |
| `pipker-spring-boot-starter-security` | `com.pipker:pipker-spring-boot-starter-security` | `jar` | 密码哈希与字段加解密边界；以配置选择 BCrypt/PBKDF2 与 AES-GCM/RSA-OAEP 的活动算法，对业务只暴露一个安全服务。 |
| `pipker-business` | `com.pipker:pipker-business` | `pom` | 聚合公共业务模块和单独业务模块，仅参与 Reactor 构建，不产出业务 Jar。 |
| `pipker-business-common` | `com.pipker:pipker-business-common` | `jar` | 公共业务能力边界，供单独业务模块复用；当前包含与 Sa-Token 解耦的 `LoginType`、`LoginIdentity` 身份契约，不依赖 Server、技术 Starter 或单独业务模块。 |
| `pipker-business-api` | `com.pipker:pipker-business-api` | `jar` | 单独业务能力边界，当前由 Server 组装，并依赖公共业务模块。 |
| `pipker-server` | `com.pipker:pipker-server` | `jar` | 可执行 HTTP Server；组合单独业务模块、Redis Starter、Log Starter，以及当前直接使用的 Web 与 Validation Starter。 |

## 模块关系

```mermaid
flowchart TD
    Framework["pipker-framework<br/>Parent / Reactor"]
    Starters["pipker-starters<br/>聚合模块"]
    Redis["pipker-spring-boot-starter-redis"]
    Log["pipker-spring-boot-starter-log"]
    SaToken["pipker-spring-boot-starter-satoken"]
    Security["pipker-spring-boot-starter-security"]
    Business["pipker-business<br/>业务聚合模块"]
    BusinessCommon["pipker-business-common"]
    BusinessApi["pipker-business-api"]
    Server["pipker-server"]
    SpringRedis["Spring Boot Data Redis Starter"]
    SpringLog["Spring Boot Logging Starter"]

    Framework --> Starters
    Framework --> Business
    Framework --> Server
    Starters --> Redis
    Starters --> Log
    Business --> BusinessCommon
    Business --> BusinessApi
    Server --> BusinessApi
    BusinessApi --> BusinessCommon
    Server --> Redis
    Server --> Log
    Server --> SaToken
    Server --> Security
    Redis --> SpringRedis
    Log --> SpringLog
```

依赖方向约束：

- `pipker-server` 依赖 `pipker-business-api` 和技术 Starter。
- `pipker-business-api` 可以依赖 `pipker-business-common`。
- `pipker-business-common` 不依赖单独业务模块或 `pipker-server`。
- 技术 Starter 不依赖 `pipker-business` 或 `pipker-server`。
- 业务模块不依赖 `pipker-server`。
- `pipker-starters` 只负责聚合，不作为 Server 的运行时依赖。

## 当前 Starter 边界

### Redis Starter

`pipker-spring-boot-starter-redis` 当前仅建立 Redis 基础依赖与 Spring Boot 自动配置能力，不包含：

- Redisson
- 分布式锁、限流或缓存业务
- `RedisRepository`
- 自定义 `RedisTemplate` 或序列化策略

后续如确有需求，可在包 `com.pipker.starter.redis` 中扩展 Redis 基础设施实现。

### Log Starter

`pipker-spring-boot-starter-log` 当前只提供技术日志的依赖边界，不实现业务审计或用户操作日志。以下能力属于未来扩展方向，但尚未实现：

- 统一日志格式
- MDC / TraceId 传播
- 请求日志
- 日志脱敏

用户登录日志、用户操作日志、数据修改日志、审计日志与操作人记录属于业务能力，不在该 Starter 范围内。

## Sa-Token 鉴权 Starter

Server 默认保护 `/api/**`。目前唯一已确认的匿名接口是 `GET /api/ping`；其他公开路由必须在 `pipker.security.auth.public-routes` 中以 `METHOD path` 的形式明确配置，例如未来登录接口可增加 `POST /api/auth/login`。默认策略不会自动创建账号、用户表、注册接口或登录 Controller。

未来业务在完成自身的账号凭证校验后，应注入 `AuthSessionService` 并调用 `login(LoginIdentity)` 创建会话。公共模块中的 `LoginType` 和 `LoginIdentity` 只描述可扩展的账号域与业务账号 ID，不预设用户、管理员或角色类型，也不依赖 Sa-Token。

令牌仅从 HTTP 请求头读取，约定如下：

```http
Authorization: Bearer <token>
```

请求体和 Cookie 中的令牌读取已关闭。未登录或无效令牌访问受保护接口时，服务返回 HTTP `401` 和 `application/problem+json`。默认会话策略为：最长 7 天、30 分钟无访问失效、同一账号允许多端独立登录。

### 内存与 Redis 会话存储

默认 `pipker.security.auth.session-store=memory`，适合单实例开发；进程重启后会话自然失效。启用 Redis 模式时设置 `SPRING_PROFILES_ACTIVE=redis`，`pipker-server` 会加载 `application-redis.yml`，并通过 Spring Boot 标准 `spring.data.redis.*` 配置创建 Redis DAO。

Redis profile 必须提供以下环境变量：

| 变量 | 说明 |
| --- | --- |
| `PIPKER_REDIS_HOST` | Redis 主机地址。 |
| `PIPKER_REDIS_PORT` | 可选，默认 `6379`。 |
| `PIPKER_REDIS_DATABASE` | 可选，默认 `0`。 |
| `PIPKER_REDIS_USERNAME` | 可选 Redis ACL 用户名。 |
| `PIPKER_REDIS_PASSWORD` | 可选 Redis 密码。 |

Redis 模式选中却缺少 Spring Data Redis 基础设施时，应用会在启动阶段失败；项目不会静默回退到内存会话，避免多实例部署误失去共享会话能力。

## Security 加密 Starter

`SecurityCryptoService` 是业务模块唯一需要注入的密码学入口：

- `hashPassword`、`matchesPassword`、`needsPasswordUpgrade` 处理密码。
- `encrypt`、`decrypt` 处理敏感字段。

调用方不传递算法名。配置分别选择一个密码哈希算法和一个字段加密算法：

| 能力 | 可选算法 | 默认配置 | 行为 |
| --- | --- | --- | --- |
| 密码哈希 | `bcrypt`、`pbkdf2-sha256` | `bcrypt` | 结果保留算法前缀；验证可识别受支持的旧格式，`needsPasswordUpgrade` 用于渐进升级。 |
| 字段加密 | `aes-gcm`、`rsa-oaep-sha256` | `aes-gcm` | 新写入数据只使用活动算法；密文保留版本与算法标识，切换字段算法前必须自行迁移历史数据。 |

AES 使用 `AES/GCM/NoPadding`、256 位 Base64 密钥、96 位随机 nonce 和 128 位认证标签。RSA 使用 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`，公钥必须为 Base64 X.509 DER，私钥必须为 Base64 PKCS#8 DER。RSA 适合较短的跨系统敏感值或密钥封装，不应直接加密大字段。

活动字段加密算法的密钥不允许存在开发默认值；密钥缺失、格式错误或长度不符都会阻止服务启动，且密钥不会写入日志。默认 AES-GCM 模式需要：

| 变量 | 说明 |
| --- | --- |
| `PIPKER_SECURITY_AES_GCM_KEY` | Base64 编码且解码后恰好为 32 字节的 AES 密钥。 |
| `PIPKER_SECURITY_RSA_OAEP_PUBLIC_KEY` | 仅在选择 `rsa-oaep-sha256` 时需要的 Base64 X.509 DER 公钥。 |
| `PIPKER_SECURITY_RSA_OAEP_PRIVATE_KEY` | 仅在选择 `rsa-oaep-sha256` 时需要的 Base64 PKCS#8 DER 私钥。 |

本地启动默认 AES-GCM 模式前，可在当前终端生成临时密钥：

```bash
export PIPKER_SECURITY_AES_GCM_KEY="$(openssl rand -base64 32)"
mvn -pl pipker-server -am spring-boot:run
```

该命令仅适合本地开发；生产密钥必须由部署环境的密钥管理或安全注入机制提供。

## 构建与测试

前置条件：JDK `21` 和 Maven `3.9+`。

在本目录执行：

```bash
mvn clean test
```

该命令会按 Reactor 顺序构建根项目、两个 Starter、Starter 聚合模块、业务模块与 Server 模块，并运行已有的 Server 测试。
