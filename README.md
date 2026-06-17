# login-auth

登录认证示例工程，演示两种主流方案：**JWT 无状态认证** 与 **Spring Session 有状态认证（Session 存 Redis）**。

技术栈：JDK 21、Spring Boot 3.5.7、MyBatis Plus + MySQL、Redis、Spring Session、前端 Vue2（CDN）。

## 模块结构

```
login-auth                  # 父工程（Maven 多模块，Spring Boot 3.5.7）
├── login-jwt               # JWT 登录：Access Token + Refresh Token（端口 8081）
└── login-session           # Spring Session 登录：Session 存 Redis，含敏感信息（端口 8082）
```

两个模块共用同一个数据库 `login_auth` 和同一张 `user` 表。

---

## login-jwt（端口 8081）

无状态认证，Token 自包含、服务端不存登录态（仅 Refresh Token 存 Redis 用于续期/吊销）。

- **双 Token**：Access Token（5 分钟）用于接口请求；Refresh Token（7 天）存 Redis 用于续期
- 登录/注册签发双 Token，`/api/auth/refresh` 刷新（带 Refresh Token Rotation，旧 Token 失效）
- 登出删除 Redis 中的 Refresh Token
- 受保护接口经 `JwtAuthInterceptor` 验签，失败返回 401
- 前端 `static/index.html`（Vue2）演示：注册、登录、获取信息、自动刷新、登出

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册即登录，返回双 Token |
| POST | /api/auth/login | 登录，返回双 Token |
| POST | /api/auth/refresh | 用 Refresh Token 换新 Access Token |
| POST | /api/auth/logout | 登出（请求头 `X-Refresh-Token`） |
| GET  | /api/user/profile | 受保护，需 `Bearer` Access Token |

---

## login-session（端口 8082）

有状态认证，登录态由服务端持有，借助 Spring Session 把 `HttpSession` 透明地存进 Redis，便于集群多节点共享。

### 核心特性

- **敏感信息存服务端**：登录后用户信息（含手机号、邮箱等敏感字段）写入 `HttpSession` → 经 Spring Session 存 Redis；客户端只持有 `SESSION_ID`（Cookie），看不到内容
- **30 分钟空闲过期**：`spring.session.timeout: 30m`，从最后一次访问开始计时，每次访问自动续期；连续 30 分钟无访问则 Redis 中 Session 自动删除，再访问视为未登录（401）
- **登出立即失效**：`session.invalidate()` 同步删除 Redis 中 Session
- **JSON 可读存储**：自定义 `springSessionDefaultRedisSerializer`（`GenericJackson2JsonRedisSerializer`），Redis 中 Session 内容为 JSON（含 `@class`），不再是 JDK 二进制乱码
- **key 前缀可控**：未使用 `@EnableRedisHttpSession` 注解，交由 Spring Boot 自动配置接管，使 `application.yml` 的 `spring.session.redis.namespace` 生效，key 形如 `spring:session:login-session:sessions:<id>`
- **Cookie 环境化配置**（`SessionConfig` + `app.cookie.*`）：
  - `secure`：本地 HTTP 设 `false`，生产 HTTPS 设 `true`（否则浏览器不回传 Cookie 导致登录态丢失）
  - `same-site`：同站/子域名共享用 `Lax`；前后端不同主域名跨站调用用 `None`（须配合 `secure=true`）
  - `domain-pattern`：单域名留空；多子域名共享时按真实域名配置
  - 固定 `HttpOnly`，Cookie 名 `SESSION_ID`
- **前端 401 统一处理**：`static/index.html`（Vue2，`withCredentials`）注册全局 axios 响应拦截器，任何接口返回 401（Session 过期/未登录）即清空登录态并弹出「登录已过期或未登录，请重新登录」横幅

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册即登录，建立 Session |
| POST | /api/auth/login | 登录，建立 Session |
| POST | /api/auth/logout | 登出，销毁 Session |
| GET  | /api/user/profile | 受保护，需有效 Session Cookie |

---

## 运行前需补全配置

两个模块的 `application.yml` 使用**环境变量占位符**读取敏感配置，启动前请提供对应环境变量（或自行改成实际值）：

| 环境变量 | 用途 |
|----------|------|
| `db_host` | MySQL 主机地址 |
| `db_user` | MySQL 用户名 |
| `db_password` | MySQL 密码 |
| `redis_host` | Redis 主机地址 |
| `redis_password` | Redis 密码 |

数据库与 Redis 端口默认 `3306` / `6379`，Redis 库默认 `database: 0`。

另外 login-jwt 的 `app.jwt.secret`（HS256 密钥，至少 32 字节）建议改为环境变量，勿提交真实值。

login-session 可按部署环境调整 `app.cookie.secure`/`same-site`/`domain-pattern`（本地默认 `secure=false`、`same-site=Lax`、`domain-pattern` 留空，开箱即用）。

### 建表

两模块共用数据库 `login_auth` 和同一张 `user` 表，用任一模块的 `src/main/resources/schema.sql` 手动建表即可（两份内容一致；MyBatis Plus 不会自动建表）。

---

## 启动

```bash
# 先设置环境变量（示例）
export db_host=127.0.0.1 db_user=root db_password=yourpwd
export redis_host=127.0.0.1 redis_password=yourredispwd

# 在 login-auth 目录分别启动两个模块
mvn -pl login-jwt spring-boot:run
mvn -pl login-session spring-boot:run
```

浏览器访问：

- JWT 演示：http://localhost:8081/
- Session 演示：http://localhost:8082/

### 验证 Session 已存入 Redis（login-session）

登录成功后查看 Redis（注意 namespace 前缀）：

```bash
redis-cli -h <host> -p 6379 -a <password> -n 0 KEYS 'spring:session:login-session:*'
# 查看某个 Session 内容（JSON 可读）
redis-cli -h <host> -p 6379 -a <password> -n 0 HGETALL 'spring:session:login-session:sessions:<sessionId>'
```
