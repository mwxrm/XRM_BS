# XRM_BS · 内容社区平台（毕业设计）

> 基于 **Spring Boot + Vue 3** 的前后端分离内容社区系统（B/S 架构），面向「内容创作 + 社交互动」场景，支持文章发布、标签、评论、点赞、收藏、关注、通知、推荐、热榜等社区功能。

## AI 辅助开发声明（请先阅读）

本项目为**毕业设计**，代码主要由 **DeepSeek**（AI）辅助生成与迭代；本 README 文档亦由 AI 生成。特此如实声明，以尊重学术诚信与开源透明原则。

- 绝大多数业务代码、接口、前端页面由 DeepSeek 辅助生成，经人工审查、调试与整合。
- 架构设计、需求拆解与最终实现由作者主导。
- 引用或二次开发本项目时，请同样如实标注 AI 辅助。

## 项目简介

XRM_BS 是一个前后端分离的内容社区 Web 应用，覆盖内容创作、社交互动与运营管理：

- 创作者可发布富文本文章、打标签、管理收藏夹；
- 读者可点赞、评论、关注作者、接收通知；
- 平台提供个性化推荐、热榜、搜索、轮播广告与后台管理。

后端基于 Spring Boot 提供 RESTful API，使用 JWT 鉴权、Redis 缓存与会话；前端基于 Vue 3 + Element Plus 构建响应式单页应用，使用 wangEditor 富文本编辑器、DOMPurify 防 XSS。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.2.0 · Java 17 · Spring Security（JWT）· Spring Data JPA · Spring Data Redis · Spring Session Redis · Spring Mail · Validation · Actuator |
| 前端 | Vue 3.5 · Vite 7 · TypeScript 5.9 · Element Plus 2.13 · Pinia 3 · Vue Router 4 · Axios · wangEditor 5（富文本）· DOMPurify · highlight.js · lodash-es |
| 数据库 / 中间件 | MySQL 8 · Redis |
| 安全 | JWT(HS512) · BCrypt · 全局异常处理 · XSS 过滤(DOMPurify) · 图形验证码 · 邮箱验证码 |
| 测试 / 工程 | Vitest · Playwright(e2e) · JMeter(压测) · ESLint · Prettier · Lombok |

## 功能特性

- **用户系统**：注册 / 登录（JWT）、邮箱验证码、图形验证码、关注 / 粉丝、个人资料
- **内容创作**：富文本文章发布（wangEditor）、标签体系（含版本）、系统设置项、收藏夹（文件夹）
- **社交互动**：点赞、评论、评论点赞、浏览历史（RecentView）
- **内容发现**：个性化推荐、热榜、搜索、轮播广告、通知（分组）
- **后台管理**：仪表盘、轮播管理、标签管理
- **工程能力**：异步任务（AsyncConfig）、Redis 缓存与 Session、用户行为日志、用户偏好 / 屏蔽标签

## 目录结构

```
XRM_BS/
├── pom.xml                      # Maven 配置（Spring Boot 3.2.0）
├── mvnw / mvnw.cmd              # Maven Wrapper
├── SQL/xrm_bs.sql               # 数据库建表脚本
├── src/main/java/com/xrm/xrm_bs/
│   ├── XrmBsApplication.java    # 启动入口
│   ├── config/                  # Security / Redis / Web / Async 配置
│   ├── controller/              # Article/Auth/Comment/Hot/Recommend/Tag/User... 控制器
│   ├── service/                 # 业务服务（含推荐、热榜、邮件、验证码）
│   ├── entity/                  # JPA 实体
│   ├── repository/              # Spring Data JPA 仓库
│   ├── dto/                     # 请求 / 响应对象
│   ├── security/                # JWT 过滤器、UserDetails、全局异常
│   └── util/                    # JwtUtil、SecurityUtil
├── src/main/resources/
│   └── application.properties.example   # 脱敏配置模板（真实配置已 gitignore）
├── test/test.jmx                # JMeter 压测脚本
└── XRM_BS_UI/                   # Vue 3 前端
    ├── package.json
    ├── vite.config.ts
    ├── index.html
    ├── src/
    │   ├── App.vue / main.ts
    │   ├── router/              # 路由
    │   ├── stores/              # Pinia 状态
    │   ├── views/               # 页面（Home/Login/Register/Article/Admin...）
    │   ├── views/components/    # AppHeader / CommentSection / RichTextEditor...
    │   ├── utils/axios.ts       # Axios 封装（带 JWT）
    │   └── styles/
    └── e2e/                     # Playwright e2e
```

## 快速开始

### 环境要求

- JDK 17、Maven 3.9+
- Node.js 20.19+ / 22.12+
- MySQL 8、Redis

### 后端

1. 建库：在 MySQL 中执行 `SQL/xrm_bs.sql`（库名 `xrm_bs`）。
2. 配置：复制 `src/main/resources/application.properties.example` 为 `src/main/resources/application.properties`，填入数据库密码、JWT secret、邮箱授权码等。
3. 启动：`mvn spring-boot:run`（默认端口 8080）。

> 真实的 `application.properties` 含明文密钥，已被 `.gitignore` 排除，仓库仅提供脱敏模板。

### 前端

```bash
cd XRM_BS_UI
npm install
npm run dev      # 开发服务器
npm run build    # 生产构建
```

开发环境默认通过 `VITE_API_BASE_URL=http://localhost:8080` 访问后端。

## 配置说明

`application.properties.example` 中需要替换的占位项：

| 占位符 | 说明 |
|---|---|
| `<REPLACE:SPRING_DATASOURCE_PASSWORD>` | MySQL 密码 |
| `<REPLACE:JWT_SECRET>` | JWT 签名密钥（建议 ≥ 64 字符随机串） |
| `<REPLACE:SPRING_DATA_REDIS_PASSWORD>` | Redis 密码（无则留空） |
| `<REPLACE:SPRING_MAIL_PASSWORD>` | 邮箱授权码（QQ 邮箱 SMTP） |

## 测试

- 前端单元：`cd XRM_BS_UI && npm run test:unit`（Vitest）
- 前端 e2e：`npm run test:e2e`（Playwright）
- 压测：`test/test.jmx`（JMeter，使用前请替换其中的测试用 JWT）

## License

[MIT](LICENSE) · Copyright (c) 2026 mwxrm

## 致谢

本项目在开发过程中借助了 **DeepSeek** 进行代码生成与调试，在此说明并致谢。
