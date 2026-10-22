# UP-SELF 学习平台

最新交付见 [每周心情模块交付与验收](docs/每周心情模块交付与验收.md)，已有学习与作业说明见 [三角色与作业模块交付验收](docs/三角色与作业模块交付验收.md)。

换电脑安装、首次空库启动、完整恢复数据库与上传文件，先看 [跨电脑环境安装与启动教程](docs/跨电脑环境安装与启动教程.md)。版本集合见 `environment/versions.json`。真实迁移快照位于 `migration/local-snapshot`。

准备真实上线前，查看 [正式上线全流程与费用预算](docs/正式上线全流程与费用预算.md)：包含云厂商网址、月租与备份、域名首年及续费、税和汇差、完整上线步骤及年度现金预算。

向导师简报可直接使用 [上线配套费用清单](docs/导师简报_上线配套费用清单.md)，已配好最低价试跑、省钱学生试用、DigitalOcean及AWS四套方案。

独立毕业项目。沿用参考项目的六模块 Java 后端架构和框架依赖版本，当前实现登录、三角色权限、账号管理、Flyway 初始化及学习模块后端。学习模块包含课程分配、加入与发布、章节、视频/图片/字幕和导师帖子。Vue 学习页面已接通真实后端，学生使用单独学习布局；学生、导师、管理员各有首页和菜单；导师与管理员复用简洁管理组件，maintenance 只保留内部入口。作业发布、草稿、上传提交、退回重交、批改与反馈已接通；每周心情、历史/趋势、当前课程导师只读查看与私人备注隔离已完成；群聊尚未开发。

## 配置和密码在哪里

直接打开 **`config/application-local.yml`**。本机 MySQL、Redis 的连接与密码、JWT 密钥、TOTP 加密密钥，以及初始账号密码都集中在这个文件中。

初始账号密码位于文件底部的 `bootstrap.passwords`。`admin01` 已重置并写回配置，数据库 root 密码未修改：

| 账号 | 身份 | 登录入口 |
|---|---|---|
| student01 | 学生 | 学生入口 |
| mentor01 | 导师 | 选择 Mentor |
| admin01 | 平台管理员 | 选择 Administrator |
| maintenance | 内部维护账号，非日常账号 | `/#/internal/login` |

这些是初始化密码，登录后修改密码不会自动回写配置。本次前端开发没有重置现有账号密码或验证器。学生、导师、管理员现在都只需账号密码登录，不需要绑定验证器或输入动态码。

两步验证暂时关闭：`custom.totp.enabled: false`。该设置已在本机配置、示例配置和应用默认配置中保持一致。密码错误限制、角色入口检查、账号停用、JWT/Redis、接口权限及退出失效均保留。原有绑定数据和密钥不删除，以便以后需要时重新启用。

## VS Code 启动

1. 首次运行 `scripts/configure-local.ps1` 配置安装目录，再用 `open-vscode.cmd` 打开本机生成的 `.local/up-self.code-workspace`，等待 Java 导入完成。根目录同名文件是共享模板。
2. 电脑重启后先运行任务 **UP-SELF: 启动数据库与Redis**。
3. 选择 **UP-SELF · Backend + Frontend**，按 F5，同时启动Java和Vue。
4. 出现 `Started UpSelfApplication` 和 Vite的Local地址后，访问 **http://127.0.0.1:5173/**。8080根页仍是旧账号验证页。
5. 分别启动时可选择 **UP-SELF · 本地登录权限服务** 或 **UP-SELF · Frontend**。停掉旧进程后再启动，避免端口占用。

也可双击 `start.cmd` 启动已打包应用；修改 Java 源码后先运行 `build.cmd`。不要同时运行 F5 和 start.cmd，避免争用 8080。VS Code 启动日志在调试控制台查看，命令启动日志在当前终端查看，不再默认生成应用日志文件。

## 目录

| 目录 | 职责 |
|---|---|
| up-self-admin | 启动、Controller、参数对象、应用配置、Flyway、登录验证页、单元测试 |
| up-self-framework | Security、登录、TOTP、token、数据源、MyBatis、异常与切面 |
| up-self-system | 账号 Service/ServiceImpl、Mapper/XML |
| up-self-common | 通用响应、登录用户模型、工具、注解 |
| up-self-biz | 学习、作业及心情模块 Domain、DTO、Service/ServiceImpl、Mapper/XML 与私有文件存储 |
| up-self-aliyun-oss | OSS 模块边界预留，当前未启用 |
| config | 可直接查看的本机配置及示例 |
| docs | 开发标准、权限方案、接口说明、环境说明及依赖版本清单 |
| scripts | 构建、启动、环境启动及集成验证脚本 |
| frontend | Vue 3 + TypeScript学习页面、公共请求/身份/偏好及自动化测试 |
| .local/maven-repository | 本机依赖缓存，保留以避免重新下载 |

## 核心版本

Spring Boot 2.5.14、Spring Security 5.5.8、MyBatis 3.5.9、MyBatis-Plus 3.3.1、PageHelper 5.3.0、Druid 1.2.11、MySQL Connector 8.0.29、Flyway 7.7.3、JJWT 0.9.1、Fastjson2 2.0.57。完整运行依赖见 `docs/runtime-dependencies.json`。

本机保留 Java 8u231、Maven 3.10.0、MySQL 5.7.44、Redis 5.0.14.1。参考 POM 要求 Java 1.8；参考 Docker 使用 Corretto8u362，不能声称 JDK 补丁号完全相同。Java 扩展自带 Java21 只用于语言服务器，项目仍使用 Java8。

Flyway 脚本位于 `up-self-admin/src/main/resources/db/draft`，已执行脚本不改动，新变更追加版本。角色为 student、mentor、platform_admin，维护角色独立保留；平台管理员没有通配超级权限。

## 构建与检查

双击 `build.cmd` 或按 Ctrl+Shift+B，包含单元测试。完整集成检查可在停止应用、保留数据库与 Redis 的情况下，用 Python3 运行 `scripts/verify-local.py`。检查时按需重新生成临时日志和结果，不保留旧验收产物。

接口见 `docs/API接口说明.md`。学习模块接口见 `docs/Learning-backend-API.md`。当前数据库版本为 V5：V3 学习、V4 作业、V5 每周心情；原迁移保持不变，后端启动只应用缺失版本。换电脑可选择空库首次迁移或完整恢复匹配的数据和文件。

## 前端环境与命令

使用 Node.js 24.22.1、npm 11.19.1。换电脑安装对应版本后运行 `scripts/configure-local.ps1 -NodeHome "你的实际安装目录"`；工作区为新终端配置此 PATH，无需全局安装 Vue。已有终端需关闭后新开。没有修改系统 PATH 或数据库密码。

在项目根目录运行：

- `frontend.cmd dev`：开发服务器5173（另需后端8080）。
- `frontend.cmd build`：类型检查与生产构建。
- `frontend.cmd typecheck`、`frontend.cmd lint`、`frontend.cmd test`：静态与行为检查。
- `frontend.cmd verify`：在已配置完整验收工具的本机，前后端均运行时执行真实 Chrome 学习、管理、作业与心情闭环；清理仅限本次专属测试记录。部分旧可选验收工具仍有本机路径，不是换电脑普通启动依赖。
- `frontend.cmd install`：按package-lock.json恢复依赖。

Vite使用Node24原生配置加载，避免额外打包配置；常规开发依赖预处理仍由Vite管理。此设备的受限工具进程运行原生esbuild时曾遇目录读取限制，VS Code实际启动已验证。不要关闭TLS验证或修改权限来绕过。

入口账号：student01 选择 Student；mentor01 选择 Mentor；admin01 选择 Administrator；maintenance 仅内部登录入口。初始密码在 config/application-local.yml 的 bootstrap.passwords，完整恢复后以数据库现有密码为准。课程中的示例视频和明确标注的会议示例可替换为实际教学内容。

具体实现、按钮检查范围和 FR 差异见学习、作业、心情各模块交付说明。前端按功能组织，HTTP 不依赖业务 store 或 router；学习、作业与心情均已接通后端。此次换电脑恢复及发布验证见 [本次发布与迁移验收](docs/本次发布与迁移验收.md)。
