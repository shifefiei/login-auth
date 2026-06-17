# login-auth

登录认证示例工程，对应面试题整理中的两篇文档：
- `JWT登录认证详解.md`
- `Session登录与Spring Session.md`

技术栈：JDK 21、Spring Boot 3.5.7、MyBatis Plus + MySQL、Redis。

## 模块结构

```
login-auth                  # 父工程（Maven 多模块）
├── login-jwt               # JWT 登录：Access Token + Refresh Token
└── login-session           # Spring Session 登录：Session 存 Redis，含敏感信息
```

## login-jwt（端口 8081）

- 无状态认证：Access Token（5 分钟）用于接口请求；Refresh Token（7 天）存 Redis 用于续期
- 登录/注册签发双 Token，`/api/auth/refresh` 刷新（带 Refresh Token Rotation），登出删除 Redis 中 Refresh Token
- 受保护接口经 `JwtAuthInterceptor` 验签，失败返回 401
- 前端 `static/index.html`（Vue2）演示：注册、登录、获取信息、自动刷新、登出

接口：
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册即登录，返回双 Token |
| POST | /api/auth/login | 登录，返回双 Token |
| POST | /api/auth/refresh | 用 Refresh Token 换新 Access Token |
| POST | /api/auth/logout | 登出（请求头 X-Refresh-Token） |
| GET  | /api/user/profile | 受保护，需 Bearer Access Token |

## login-session（端口 8082）

- 有状态认证：登录后用户信息（含手机号、邮箱等敏感字段）写入 HttpSession，底层经 Spring Session 存 Redis
- 客户端只持有 SessionID（Cookie），看不到敏感内容
- 登出 `session.invalidate()` 销毁 Redis 中 Session，立即失效
- 前端 `static/index.html`（Vue2，`withCredentials`）演示：注册、登录、获取信息、登出

接口：
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册即登录，建立 Session |
| POST | /api/auth/login | 登录，建立 Session |
| POST | /api/auth/logout | 登出，销毁 Session |
| GET  | /api/user/profile | 受保护，需有效 Session Cookie |

## 运行前需补全配置

两个模块的 `src/main/resources/application.yml` 中已留好占位（标注 `TODO`），请补全：
- 数据库 `spring.datasource.username / password`（两模块共用一个库 `login_auth` 和同一张 `user` 表）
- Redis `spring.data.redis.host / port / password`
- login-jwt 的 `app.jwt.secret`（HS256 密钥，至少 32 字节，生产放环境变量）

两模块共用一个数据库 `login_auth` 和同一张 `user` 表，用任一模块的 `schema.sql` 手动建表即可（两份内容一致；MyBatis Plus 不会自动建表）。

## 启动

```bash
# 在 login-auth 目录
mvn -pl login-jwt spring-boot:run
mvn -pl login-session spring-boot:run
```

浏览器访问：
- JWT：http://localhost:8081/
- Session：http://localhost:8082/
