# Fenglin Blog 后端接口说明

## 技术栈

- Java 17（本机 Java 21 可直接运行）
- Spring Boot 3.4.5
- MyBatis-Plus 3.5.17
- MySQL 8.0
- BCrypt 密码摘要 + 数据库存储的随机访问令牌

## 初始化数据库

建库建表语句保存在：

```text
src/main/resources/db/schema.sql
```

执行示例：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

数据库名为 `fenglin_blog`。回滚脚本位于 `src/main/resources/db/rollback.sql`，执行它会删除整个数据库，使用前必须备份。

## 数据库连接

开发配置从环境变量读取：

- `BLOG_DB_URL`：默认 `jdbc:mysql://127.0.0.1:3306/fenglin_blog?...`
- `BLOG_DB_USERNAME`：默认 `root`
- `BLOG_DB_PASSWORD`：无默认值，请在 IDEA 或终端中设置
- `BLOG_SERVER_PORT`：默认 `8081`

## 启动

可双击根目录的 `start-blog.cmd`（会提示输入本机 MySQL 密码），或执行：

```bash
mvnw.cmd spring-boot:run
```

健康检查：`GET http://localhost:8081/api/auth/ping`

## 统一返回体

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## 认证接口

| 方法   | 路径                                     | 说明                                   |
| ---- | -------------------------------------- | ------------------------------------ |
| POST | `/api/auth/register`                   | 注册；兼容旧注册页多传的 `faction` 字段，但后端不再保存    |
| POST | `/api/auth/login`                      | 登录；返回用户信息、`token` 与 `tokenExpiresAt` |
| GET  | `/api/auth/check-username?username=xx` | 用户名查重                                |
| POST | `/api/auth/forgot/verify`              | 用户名与邮箱验证，返回一次性重置令牌                   |
| POST | `/api/auth/forgot/reset`               | 使用重置令牌修改密码                           |
| GET  | `/api/auth/me`                         | 获取当前用户，需 Bearer Token                |
| POST | `/api/auth/logout`                     | 退出当前会话，需 Bearer Token                |
| GET  | `/api/auth/ping`                       | 健康检查                                 |

受保护接口请求头：

```text
Authorization: Bearer <login response token>
```

## 博客接口

| 方法     | 路径                             | 说明                                                 | 权限  |
| ------ | ------------------------------ | -------------------------------------------------- | --- |
| GET    | `/api/posts`                   | 已发布文章分页；支持 `current,size,keyword,categoryId,tagId` | 公开  |
| GET    | `/api/posts/slug/{slug}`       | 已发布文章详情并增加浏览量                                      | 公开  |
| GET    | `/api/posts/mine`              | 当前用户的文章（含草稿）                                       | 登录  |
| POST   | `/api/posts`                   | 新建文章                                               | 登录  |
| PUT    | `/api/posts/{id}`              | 编辑自己的文章，管理员可编辑全部                                   | 登录  |
| DELETE | `/api/posts/{id}`              | 逻辑删除自己的文章，管理员可删除全部                                 | 登录  |
| GET    | `/api/categories`              | 分类列表                                               | 公开  |
| GET    | `/api/tags`                    | 标签列表                                               | 公开  |
| GET    | `/api/posts/{postId}/comments` | 评论列表                                               | 公开  |
| POST   | `/api/posts/{postId}/comments` | 发布评论                                               | 登录  |
| DELETE | `/api/comments/{id}`           | 删除自己的评论，管理员可删除全部                                   | 登录  |

新建/编辑文章请求示例：

```json
{
  "categoryId": 1,
  "title": "第一篇博客",
  "slug": "first-post",
  "summary": "文章摘要",
  "content": "正文内容",
  "coverUrl": "",
  "status": "PUBLISHED",
  "tagIds": [1, 2]
}
```

## 本地前端兼容说明

现有 Vue 注册页继续使用 `/api/auth/*`，多传的 `faction` 会被安全忽略。登录响应仍把 `username`、`nickname` 等用户字段直接放在 `data` 下，同时新增 `token` 字段，不会破坏当前页面读取方式。后续博客写接口需把该 token 放入 `Authorization` 请求头。

> 当前“用户名 + 邮箱直接签发重置令牌”的流程仅适合本地开发。生产环境应改为邮件验证码或邮件重置链接，并增加频率限制。

## 社交接口（头像 / 资料 / 点赞 / 关注 / 私信）

建表与字段扩展脚本：`src/main/resources/db/schema_social.sql`（幂等，可重复执行）。
执行：`mysql -u root -p < src/main/resources/db/schema_social.sql`

| 方法   | 路径                                      | 说明                                    | 权限              |
| ---- | --------------------------------------- | ------------------------------------- | --------------- |
| PUT  | `/api/auth/profile`                     | 更新昵称 / 头像 / 签名 ，字段传 null 表示不修改        | 登录              |
| GET  | `/api/users/{userId}`                   | 用户公开资料（含文章数、粉丝数、关注数、followed、self）    | 公开（带令牌时返回个性化字段） |
| GET  | `/api/users/search?keyword=xx&limit=10` | 按昵称或用户名搜索用户                           | 公开              |
| GET  | `/api/users/{userId}/posts`             | 该用户已发布文章分页                            | 公开              |
| POST | `/api/users/{userId}/follow`            | 关注 / 取关切换，返回 followed 与 followerCount | 登录              |
| GET  | `/api/users/{userId}/following`         | 关注列表                                  | 公开              |
| GET  | `/api/users/{userId}/followers`         | 粉丝列表                                  | 公开              |
| POST | `/api/posts/{postId}/like`              | 点赞 / 取消点赞切换，返回 likeCount 与 liked      | 登录              |
| GET  | `/api/messages`                         | 会话列表（对方资料 + 最近一条 + 未读数）               | 登录              |
| GET  | `/api/messages/unread`                  | 未读消息总数                                | 登录              |
| GET  | `/api/messages/{peerId}?limit=100`      | 与某人的聊天记录（正序），读取即标为已读                  | 登录              |
| POST | `/api/messages/{peerId}`                | 发送私信                                  | 登录              |

数据约定：`；前端 `src/assets/avatars.js` 容外链图片（`http(s)://` 或 `data:image`）。

- 阵营 `faction` 只有 `ninja` / `shinigami`，非法值统一回落 `ninja`。
- 评论支持一层回复：`POST /api/posts/{id}/comments` 传 `parentId`，返回体带 `replyToUserId` / `replyToName`；删除父评论时子评论会被摘成顶层评论。

鉴权改动：`AuthInterceptor` 现在对**所有**请求都会尝试解析 `Authorization` 头（令牌无效时按游客处理，不阻断请求），因此公开接口也能返回 `liked`、`followed`、`self` 等个性化字段；`@RequireAuth` 仍然是强制登录的唯一开关。

## 已知限制

- 登录、用户名查重、找回密码接口尚未做频率限制，公网部署前需补充限流或验证码。
- 私信没有 WebSocket 推送，前端靠轮询或手动刷新获取新消息。
- 文章关键词搜索使用 `LIKE '%关键词%'`，输入中的 `%`、`_` 会被当成 SQL 通配符，且大数据量下不走索引；后续可接入全文索引或搜索引擎。
- 过期访问令牌没有定时清理任务，`blog_access_token` 会随时间增长，可定期手工清理过期行。
- 文章浏览量每次读取详情即 +1，未做同用户去重，存在刷量可能。
- 项目根目录仍保留旧启动脚本 `启动后端.bat`（未被本次清理清单覆盖），请改用 `start-blog.cmd`。
