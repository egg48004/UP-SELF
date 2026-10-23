# 登录权限 API

本地基址 `http://127.0.0.1:8080`，JSON 请求。除登录入口外都要带 `Authorization: Bearer <token>`，且入口身份一致。API 不接受客户端指定可信身份。

## 登录与个人中心

| 方法与路径 | 输入 | 说明 |
|---|---|---|
| POST `/client/login` | username、password | 仅学生；成功顶层 token |
| POST `/empLogin` | username、password | 导师/管理员；当前账号密码通过后直接返回顶层 token |
| POST `/empLogin/totp` | ticket、code | 当前关闭，返回 code=403，不接受旧 ticket 换 token |
| GET `/client/getInfo`、`/getInfo` | 无 | 对应入口返回 user、roles、permissions；不返回密码和 TOTP 密钥 |
| GET `/client/getRouters`、`/getRouters` | 无 | 当前角色已授权菜单/按钮数据，保持原 M/C/F 类型；返回扁平记录供界面使用 |
| POST `/client/logout`、`/logout` | 无 | 注销当前登录态，旧 token 随即无效 |
| PUT `/client/profile/password`、`/system/user/profile/updatePwd` | oldPassword、newPassword | 本人改密，新密码 10–64 字符，改后重新登录 |

当前 `custom.totp.enabled=false`：学生、导师、管理员均用账号密码登录，不返回 ticket、绑定密钥或动态码要求。已有 TOTP 绑定数据保留但暂不参与登录；重新启用时才恢复两步流程。初始密码见 `config/application-local.yml` 的 `bootstrap.passwords`。

## 账号管理

以下接口位于管理认证链；普通导师不具有权限。平台管理员仅能分配 roleId 10=学生、20=导师、30=平台管理员。

| 方法与路径 | 所需权限 | 输入/输出 |
|---|---|---|
| GET `/system/user/list` | system:user:list | current、pageSize（最多100；暂兼容pageNum）、可选 userName；响应顶层 rows/total |
| GET `/system/user/roles` | system:user:edit | 可分配角色列表，不含维护角色 |
| POST `/system/user` | system:user:add | userName、nickName、password、roleId；data=新用户ID |
| PUT `/system/user` | system:user:edit 及 changeStatus | id、nickName、roleId、status；status 0正常/1停用；保存后撤销目标现有登录态 |
| PUT `/system/user/resetPwd` | system:user:resetPwd | id、password；禁止普通管理员恢复其他管理员凭证 |
| PUT `/system/user/{id}/resetTotp` | system:user:resetTotp | 仅维护账号可用，清除绑定并撤销目标登录态 |

账号格式 `[A-Za-z0-9_.@-]{3,100}`；显示名最多60字符；新密码10–64字符。用户名创建后本版不支持改名。普通管理员不能修改维护账号，也不能调整另一个平台管理员的角色/状态以绕过凭证保护；自己的停用/降级也被拒绝。

角色和菜单数据先用独立 Flyway 管理，不提供可任意授权通配权限的后台编辑接口；学习接口见 Learning-backend-API.md；作业和心情尚未开发。

## 返回约定

沿用 AjaxResult 的 code/msg/data 风格，登录顶层 token、列表顶层 rows/total。

- code=200：成功。
- code=401：未认证或登录态无效；与参考一致，此认证入口当前 HTTP 状态为500。
- code=403：已认证但无权执行；方法鉴权拒绝通常 HTTP 200。
- code=400：参数无效/用户名重复；404：目标不存在。
- code=1010：账号密码错误或锁定；1018：ticket失效/已使用；1019：TOTP错误/重放。
- code=429：登录入口超过每IP限流；500：系统或登录服务故障。

前端需要同时读取成功及非2xx响应中的 JSON code，不能只判断 HTTP 状态。跨入口 token 因不属于当前认证链，按未认证返回401。权限细节以确认方案为准。

## P0对齐（2026-10-22）

列表统一使用 BaseController/startPage/getDataTable 和 TableDataInfo，顶层code/msg/rows/total保持不变。current为正式页码，暂兼容旧pageNum；两者同时出现以current为准。

GET `/system/user/candidates` 要求 `learning:course:assign`，参数role仅student/mentor，keyword、current、pageSize。只返回有效角色下启用账号的id/userName/nickName，不返回密码。getInfo增加布尔totpEnabled，前端据此隐藏未启用的验证器维护操作。


## V6：课程信息、学习进度与导师概览（2026-10-23）

课程创建/更新增加 `difficulty`（BEGINNER/INTERMEDIATE/ADVANCED，默认 BEGINNER）和 `estimatedMinutes`（1～10080，默认30）。旧课程的默认值需要管理员按真实课程编辑，不是系统测量。

| 方法 | 路径 | 返回/权限 |
|---|---|---|
| GET | /client/learning/courses/{id}/progress | AjaxResult.data：total、completed、percent、lastChapterId、chapters；学生本人、已加入已发布课程 |
| POST | /client/learning/chapters/{id}/visit | 记录本人最后访问时间；不标记完成 |
| PUT | /client/learning/chapters/{id}/completion | body `{ "completed": true }`，false 可撤销；重复 true 不重写完成时间 |
| GET | /client/learning/resume | 最近可访问课程/章节；无记录遵循原 AjaxResult 返回 data={}，前端归一成 null |
| GET | /client/learning/history | TableDataInfo rows/total；current/pageSize；当前仍可访问的已完成章节 |
| GET | /learning/students/overview | TableDataInfo；current/pageSize、keyword、可选courseId；仅当前负责已发布课程的导师 |

导师概览每行对应一个学生和课程，含章节完成数、已发布作业总数、待批改/已评分/退回数及最近共享心情的等级和周。没有作业正文、附件、私人备注。学生提交状态 DRAFT/无提交计入未提交；归档作业不计数。管理员/maintenance 通配权限也无法绕过 Service 的导师角色检查。
