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

---

## 安全：CSRF 防护

### 什么是 CSRF

CSRF（跨站请求伪造）：攻击者诱导**已登录用户的浏览器**，向目标站点发出用户本意之外的请求，借用浏览器**自动携带的登录凭证**完成操作（如转账、改密码）。它利用的不是“偷到凭证”，而是“浏览器会自动带上凭证”这一行为，所以是否易受 CSRF 取决于**凭证是不是浏览器自动携带的**。

### login-jwt 方案：凭证不入 Cookie + 重点防 XSS

- **Token 不写 Cookie**：Access/Refresh Token 存于前端，经 `Authorization: Bearer` 头**手动**携带。浏览器不会自动带上 Authorization 头，攻击者的跨站页面也拼不出该头，因此 JWT 模式**天然规避 CSRF**。
- **代价转移到 XSS**：Token 存在前端（localStorage）会暴露在 XSS 下，故本模块安全重点是防 XSS。`SecurityHeadersFilter` 统一下发安全响应头：
  - `Content-Security-Policy`（限制脚本来源，防 XSS 核心）
  - `X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`、`Referrer-Policy: no-referrer`
  - 配合 Vue 模板默认输出转义，进一步降低注入风险。
- 结论：**只要 Token 不落 Cookie，就不需要额外的 CSRF Token 机制**；务必守住 XSS 这条线。

### login-session 方案：引入 Spring Security CSRF + 保持 SameSite

Session 用 `SESSION_ID` Cookie 存登录态，而 Cookie 是浏览器自动携带的，正中 CSRF 利用条件，必须专门防护。本模块采用**双重防护**：

- **第一道防线 · SameSite Cookie**：`SessionConfig` 给 `SESSION_ID` 设 `SameSite`（默认 `Lax`），跨站请求默认不带 Cookie，挡住大部分 CSRF。
- **核心方案 · Spring Security 双提交 Cookie**：引入 `spring-boot-starter-security`，`SecurityConfig` 仅用于 CSRF（认证仍由 `AuthInterceptor` + Session 负责，故 `permitAll`）。具体怎么发 token、怎么校验，见下面的详细流程。

> `XSRF-TOKEN` 的 `SameSite`/`Secure` 跟随 `app.cookie` 配置，与会话 Cookie 保持一致。
> 一旦因前后端分离把 `SameSite` 改为 `None`（跨站），第一道防线失效，此时全靠 CSRF Token 兜底，且需配合正确的 CORS（显式 Origin + 允许携带凭证）。

#### 双提交 Cookie 详细流程

涉及到的名字：

| 名字 | 是什么 | 谁设置 / 谁读 |
|---|---|---|
| `XSRF-TOKEN` | **Cookie 名**，存 CSRF token（非 HttpOnly） | 服务端写，前端 JS 读 |
| `X-XSRF-TOKEN` | **请求头名**，回传 CSRF token | 前端 axios 写，服务端读 |
| `SESSION_ID` | 会话 Cookie（登录态，HttpOnly） | Spring Session 管 |
| `CookieCsrfTokenRepository` | 把 token 写入/读出 `XSRF-TOKEN` cookie | 服务端 |
| `CsrfFilter` | 比对 token 的过滤器 | 服务端 |

> 注意：cookie 名 `XSRF-TOKEN` 与请求头名 `X-XSRF-TOKEN` 长得像但不是一个东西，一个在 Cookie、一个在 Header。

**第 1 步：浏览器首次访问，服务端下发 token**

```
GET / HTTP/1.1
Host: localhost:8082
```

服务端 `CsrfFilter` 生成随机 token（如 `abc123def456`），`CsrfCookieFilter` 触发写出，`CookieCsrfTokenRepository` 写进响应 cookie：

```
HTTP/1.1 200 OK
Set-Cookie: XSRF-TOKEN=abc123def456; Path=/; SameSite=Lax          # 无 HttpOnly，JS 可读
Set-Cookie: SESSION_ID=xxxx; Path=/; HttpOnly; SameSite=Lax        # HttpOnly，JS 读不到
```

**第 2 步：前端发写请求，从 cookie 取 token 放进请求头**

axios 按配置 `xsrfCookieName: 'XSRF-TOKEN'` / `xsrfHeaderName: 'X-XSRF-TOKEN'` / `withXSRFToken: true` 自动完成：从 `XSRF-TOKEN` cookie 读出值，放进 `X-XSRF-TOKEN` 请求头。实际请求里 token 出现两份：

```
POST /api/auth/login HTTP/1.1
Host: localhost:8082
X-XSRF-TOKEN: abc123def456                              # A：JS 从 cookie 读出后手动放的（请求头）
Cookie: XSRF-TOKEN=abc123def456; SESSION_ID=xxxx        # B：浏览器自动带的（cookie）
Content-Type: application/json

{"username":"bob","password":"123456"}
```

**第 3 步：服务端比对两份 token**

`CsrfFilter` 执行：

1. 从 **Cookie `XSRF-TOKEN`** 读期望值 B（`CookieCsrfTokenRepository.loadToken`）；
2. 从 **请求头 `X-XSRF-TOKEN`** 取实际值 A（`CsrfTokenRequestAttributeHandler` 解析）；
3. 比较 `A.equals(B)`：相等放行进入 Controller，不等或缺失返回 **403**。

```
A (X-XSRF-TOKEN 请求头) == B (XSRF-TOKEN cookie)  →  放行
A != B 或 A 缺失                                   →  403
```

**攻击者为什么过不了**：跨站页面发请求时，浏览器仍会自动带上 `XSRF-TOKEN` cookie（B 有），但攻击者的 JS 受同源策略限制**读不到该 cookie 的值**，拼不出 `X-XSRF-TOKEN` 请求头（A 缺失）→ `A != B` → 403。

> 一句话：同一个 token 走两条路到服务端——浏览器自动带的 **Cookie** 和 JS 主动读 cookie 后填的 **请求头**。攻击者能借用前者，但读不到 cookie 值、填不出后者，于是露馅。
