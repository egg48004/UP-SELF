> 当前代码已追加V7个人学习目标和V8课程收藏。本文V6发布包和预备检查属于前轮历史记录，不能直接当作最新代码包。未来重新打包时清单标记V8；Linux实测仍按用户要求暂停。本轮只同步版本元数据，没有重新生成发布包或声称完成Linux运行验证。

# Linux部署适配准备（尚未部署）

日期：2026-10-23。当前状态：**已准备脚本、配置模板和本地发布包；未购买、未连接云主机、未安装Linux环境、未对Linux服务执行部署。** Windows本地功能和启动方式保持原样。

先阅读[当前61个FR状态](当前61个FR完成状态与报告差异.md)及[答辩清单](答辩演示清单.md)。本文件承接[购买与域名绑定教程](购买与域名绑定简明教程.md)，费用仍见[配套费用清单](导师简报_上线配套费用清单.md)，不是新的付款清单。

## 1. 保持的架构

未来一台Linux服务器运行Nginx（Vue静态页面＋反向代理）、现有Java单体、MySQL和Redis。上传文件仍由现有后端鉴权，不公开整个目录。代码、数据库和文件分开存放，更新程序不覆盖数据。

不引入Docker、容器编排、微服务、额外消息队列、CI/CD平台或云数据库。服务器上不运行Vite开发服务，不必安装Node/Maven来接收构建结果。Linux必须先在实际目标环境验收，不能把Windows测试结果当作Linux实测。

## 2. 版本锁定与待确认事项

| 项目 | 实际依据/本次处理 |
|---|---|
| Java源码目标 | 父POM Java 1.8，保持 |
| 当前独立项目JDK | `scripts/configure-local.ps1`和`environment/versions.json`锁定Oracle 1.8.0_231；Linux启动检查暂保持这个版本，不自动换OpenJDK/Corretto |
| 参考项目Docker | 只读检查到 `FROM amazoncorretto:8u362`，与当前Windows JDK不是同一构建。之前费用文档中的Corretto是候选，未切换。若实际部署需采用它，应先说明差异并完成兼容测试，本轮不自行变更 |
| MySQL | 5.7.44，不能用apt默认版本或MariaDB替代；Linux安装包、OS依赖和真实服务端VERSION需核对 |
| Redis | 本地是Windows移植版5.0.14.1；Linux候选为上游5.0.14，**不是相同二进制或已确认相同版本号**。Linux候选必须专项验证现有Lettuce、登录、限流、媒体票据及重启行为 |
| Spring Boot / Security | 2.5.14 / 5.5.8，保持现有POM及实际依赖 |
| MyBatis / PageHelper | 保持XML Mapper和现有依赖；不替换分页方案 |
| Flyway | 7.7.3，当前V1～V6；不修改旧迁移 |
| Windows构建工具 | Maven3.10.0、Node24.22.1、npm11.19.1和package-lock继续沿用；无需搬Windows工具目录到Linux |
| Linux系统 | 拟验收x86_64、支持systemd的Linux；Ubuntu22.04 LTS是之前候选，不代表已经验证与旧MySQL/JDK安装包兼容。不选ARM镜像规避不必要的平台差异 |
| Nginx / systemd / OpenSSL /证书客户端 | 不是现有Java依赖中锁定的版本，准确版本待目标系统确定后记录。本轮不编造版本，不自动安装。systemd模板使用常规指令，仍需目标机verify |

Oracle官方归档提供8u231 Linux x64，但归档许可和公开服务用途需要核对；不能把“开发可用”当作“任何正式用途都免费”。旧运行时维护状态及目标OS兼容性需在付款前确认，不通过自行升级解决。[Oracle归档与许可说明](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html)

MySQL从[官方归档](https://downloads.mysql.com/archives/community/)手动选择5.7.44及对应Linux包，页面默认可能显示其他版本；这不是已取得且验过哈希的安装包。[Redis上游5.0.14发布](https://github.com/redis/redis/releases/tag/5.0.14)证明候选版本存在，不证明Windows移植版完全等价。

**当前未完成的具体信息：** 目标OS/系统库、可合法使用的JDK包及校验值、MySQL Linux包/校验值、Redis Linux兼容结果、Nginx准确版本、域名、正式凭证和备份存放地区。未确认前不把模板宣称为可直接公开上线的最终配置。模板 `UPSELF_LINUX_RUNTIME_REVIEWED=no` 会阻止误启动；只有上述实际审查和测试通过后才改为yes。

## 3. 新增文件及职责

| 文件 | 用途 |
|---|---|
| [package-linux.ps1](../scripts/package-linux.ps1) | 用现有Windows工具构建，按白名单打包本地tar.gz和SHA-256，不上传 |
| [run.sh](../deploy/linux/run.sh) | Java前台进程、prod配置、固定内存上限，供systemd管理 |
| [preflight.sh](../deploy/linux/preflight.sh) | 发布包检查；未来Linux二进制版本/必要环境变量/路径检查 |
| [application-prod.yml.example](../deploy/linux/application-prod.yml.example) | 与现有Druid主库、独立Flyway账号、Redis2号库、两个token通道匹配 |
| [up-self.env.example](../deploy/linux/up-self.env.example) | 正式凭证环境变量模板，无真实密码；默认不初始化账号 |
| [up-self.service.example](../deploy/linux/up-self.service.example) | 普通upself用户运行、开机启动/失败重试模板 |
| [nginx-http.conf.example](../deploy/linux/nginx-http.conf.example) | 仅IP/DNS阶段的HTTP测试，不收集真实学生数据 |
| [nginx-https.conf.example](../deploy/linux/nginx-https.conf.example) | 域名/证书就绪后启用的HTTPS模板 |
| [backup.sh](../deploy/linux/backup.sh)、[mysql-backup.cnf.example](../deploy/linux/mysql-backup.cnf.example) | 未来显式执行的数据库＋文件加密备份，要求服务已停止，不自动部署/停止服务 |
| [verify-linux-preparation.py](../scripts/verify-linux-preparation.py) | 本地语法、配置、归档/校验和、凭证排除等预备检查 |

`.gitattributes`仅给上述shell和模板文件固定LF，避免Windows检出CRLF导致Linux脚本错误。没有修改业务源码、POM或已执行的迁移。

## 4. 本地如何生成待部署包（本轮已执行）

在项目根目录Windows PowerShell：

```powershell
.\scripts\package-linux.ps1
```

默认先使用现有缓存离线Maven package（含Java测试）并运行Vue生产构建，不npm install、不下载/安装新技术。缓存不齐会报错，按原依赖准备，不自动换版本。`-SkipBuild`只用于明确已刚构建的产物，本轮也验证了该选项；发布前通常使用默认方式。

输出在 `.local/linux-release/`：

```text
up-self-时间戳.tar.gz
up-self-时间戳.tar.gz.sha256
up-self-时间戳/
  app/up-self-admin.jar
  web/index.html 和 assets/
  linux/脚本与example模板
  release.json
  SHA256SUMS
```

本轮最后生成的本地包为 `up-self-20261023-150059.tar.gz`，复用了同轮默认构建生成的产物；清单内如实标记buildSkipped。发布包状态是prepared-not-linux-integration-tested。外层sha256检查传输，内部SHA256SUMS逐文件校验。它不包含本地config、DEMO密码、数据库快照、上传文件或node_modules。另行保管代码对应的提交、数据库和文件备份；程序包不等于完整数据迁移包。

## 5. 未来服务器目录（现在没有创建）

```text
/opt/up-self/releases/<release-id>/   每次上传的新程序目录
/opt/up-self/current -> releases/... 当前版本链接
/etc/up-self/application-prod.yml    由example另存的正式配置
/etc/up-self/up-self.env             真实凭证，由root保管，不进Git
/etc/up-self/mysql-backup.cnf        备份数据库账号，root 0600
/etc/up-self/backup.key              备份加密密钥，另存离线副本
/var/lib/up-self/uploads/            现有媒体及assignments子目录
/var/backups/up-self/                本机加密备份，再复制到异地
```

应用账号upself只需读取程序和prod YAML、写入上传目录；不以root运行Java。Nginx只读web目录，不给它整个上传目录权限。EnvironmentFile可root 0600由systemd读取；prod YAML可root:upself 0640；目录所有权在正式准备时核对。不要执行会覆盖整个/opt或/var的递归权限命令。

## 6. 将来实际部署顺序（本轮不执行这些操作）

1. 完成第2节的OS/版本/许可验证，再按费用清单确认付款；本轮未购买。
2. 在批准的Linux环境安装准确版本的JDK、MySQL和Redis及所需Nginx，记录真实版本；不要`apt install default-jdk/mysql-server`直接带入不同版本。mysql和redis-server命令需在服务PATH内。
3. 创建upself运行用户和上节目录；准备SQL应用用户（库内SELECT/INSERT/UPDATE/DELETE）及Flyway迁移用户（仅目标库的DDL/DML权限）。数据库/Redis仅127.0.0.1访问，3306/6379/8080不对外开放。
4. 创建独立的`up_self_prod`，字符集utf8mb4。不手工建业务表，由Flyway或完整快照恢复。创建用户用原数据库管理员，不修改本地Windows root密码，不将root用作Java应用账号。
5. **明确选空库还是恢复：** 空库运行V1～V6；恢复只导入已核验、同时间点的完整SQL和上传文件，SQL含Flyway历史。现有V5快照恢复后由新程序补V6；它不含本轮之后的新数据。目标必须为空，不能用旧快照覆盖已有学生数据。
6. 上传程序包并验证SHA，解压到新release目录，不解压到上传目录。建立current链接。将example分别另存到/etc/up-self和系统服务配置位置，替换example.org及所有REPLACE值。
7. 为数据库、Redis和两个token通道生成正式独立凭证；token至少32字符且两通道不同。不要沿用已公开的开发凭证。本地配置保持原样，不把它复制为生产配置。
8. 正式文件中 `UPSELF_LINUX_RUNTIME_REVIEWED=yes` 只能在实际验证完成后设置。空库若需要初始化四个原角色账号，**仅第一次**设置`UPSELF_BOOTSTRAP_ENABLED=true`并填写四个独立初始密码；预检拒绝空值/REPLACE值。原初始化器只在用户表为空时创建账号，不重置已有账号。确认账号建立后改回false并清除初始密码变量，重启。恢复已有账号时始终false。
9. 先检查版本、配置和连接，再将systemd模板安装为up-self.service。未来先做`systemd-analyze verify`，随后才daemon-reload/start；看journal日志、Flyway V6和接口。模板不替你安装MySQL/Redis，也不假设发行版服务名称。
10. 先用HTTP模板做测试数据的IP/DNS检查；确认域名A记录后签发证书，再切换HTTPS模板并`nginx -t`、reload。模板和证书路径必须按真实域名修改。
11. 按第9节完整验收三角色、上传、播放/拖动、权限和持久化；做一次真实备份恢复后才邀请学生。

代码内的空库初始化仍是原四个账号名称和角色机制，不新增账户框架；正式部署前是否保留演示账号，应由项目实际数据选择决定。模板默认不灌DEMO数据。

## 7. Nginx为何这样配置

- Vue使用hash路由；Nginx提供dist静态文件，页面刷新有index回退，不使用开发端口5173。
- 前端所有API仍为`/api/...`。`proxy_pass http://127.0.0.1:8080/;`的尾部斜杠剥离`/api/`，匹配当前Vite代理行为；不能删掉斜杠变成后端不存在的路径。
- 保留Authorization、Range等请求机制及原业务JSON错误，不把后端401/403/500替换成静态页面。原项目部分未认证响应是HTTP500＋业务401，前端已有处理，本轮不重设计。
- 不缓存API/媒体/私有附件，媒体票据访问不写入Nginx访问日志，不用公开/static/uploads绕开鉴权。单个应用视频上限100MB、Spring请求105MB，Nginx采用105m留出multipart空间；作业仍由业务限制20MB/文件。
- HTTPS只在有效证书就绪后启用；证书自动续期与80端口ACME验证要实际测试，不购买额外付费证书。

[官方Nginx反向代理说明](https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/)、[systemd执行环境说明](https://www.freedesktop.org/software/systemd/man/latest/systemd.exec.html)、[Let's Encrypt入门](https://letsencrypt.org/getting-started/)。这些是模板依据，不代表本机已运行相同软件版本。

## 8. 备份、恢复、更新和回退

**备份目标：** SQL（包括账号、角色、Flyway历史）＋同一时点uploads。Redis登录会话不作为业务恢复源，重启或迁移后必要时重新登录。

`backup.sh`仅未来Linux人工调用：要求up-self.service完全inactive、真实MySQL服务5.7.44、匹配客户端、专用cnf和密钥存在。它不会自行停服务，也不删除旧备份。操作者先安排短维护窗口，停应用并确认没有其他写入者，再执行备份，无论成功与否都应恢复服务并核查。脚本以mysqldump单事务导出，SQL和uploads一起tar/gzip，再用OpenSSL AES-256-CBC＋PBKDF2加密，输出加密包及SHA-256。当前尚未执行过该Linux备份。

- 每日备份、保留最近7天是原费用方案目标；**本轮没有安装定时任务或自动轮换**。正式上线前再安排调度及异常恢复，人工备份脚本不能冒充已自动运行。
- 将加密包和checksum复制到已确认地区的异地备份存储（原方案R2）；密钥独立保管，不和备份包放在同一可公开位置。实际免费额度、容量和请求费按账单，不在这里重复报价。
- 外层SHA-256用于完整性检查，不是恶意篡改的签名证明；此处没有另加签名/密钥管理平台。
- 恢复演练：校验外层hash → 用独立密钥解密到受控临时目录 → 检查tar条目 → 解压并校验SQL-SHA256SUMS → 创建新的空恢复数据库 → 导入SQL → 恢复匹配uploads到独立目录 → 临时配置指向恢复副本 → 验证。不要直接覆盖正在使用的库或共享uploads。
- 更新新功能：本地测试/打包 → 维护窗口 → 备份数据和文件 → 解压新release → 切current → 重启Java → Flyway只执行新增脚本 → 验收。程序切换不移动/清空持久数据目录。
- 回退：只有数据库结构向后兼容时才切回旧程序；不能用旧JAR自动逆转新迁移。不调用Flyway clean/随意repair。需要恢复数据时先保全新数据，按维护窗口恢复成套副本，说明可能丢失备份后的写入。

## 9. Linux正式验收清单（当前均待执行）

- [ ] `uname`确认为目标Linux/x86_64，OS、JDK许可/版本和系统库记录完整。
- [ ] 实际Java 8u231、MySQL服务端5.7.44；Redis候选5.0.14与Windows移植版差异已确认。`preflight --runtime`通过不等于实际服务认证通过。
- [ ] SQL应用/迁移账号、Redis认证成功，Flyway V1～V6状态正确。
- [ ] 目标机`systemd-analyze verify`、启动/退出/重启/开机启动实测。
- [ ] `nginx -t`、DNS、证书、续期、HTTP跳转HTTPS实测。
- [ ] 管理员建课分配、导师上传视频/图片/字幕和发帖、学生观看/拖动/字幕正常。
- [ ] 章节完成/撤销、刷新、首页继续学习、历史分页、导师概览正确。
- [ ] 上传作业、提交、退回重交、批改、下载反馈正常；大文件限制与断网提示正确。
- [ ] 首次心情确认、每周一次、历史缺失周、导师只读、私人备注隔离正常。
- [ ] 跨用户/跨课程越权被拒绝；文件不可通过静态路径绕过。
- [ ] 手机、键盘、字号/对比和语音降级检查；实际学生网络试播与磁盘/内存/带宽观察。
- [ ] 加密备份、异地副本和新空库恢复成功，重启后数据/文件仍在。

## 10. 本轮实际验证结果与限制

- 默认package脚本在当前Windows完成Maven package（57项Java测试通过）及Vue类型检查/生产构建，无依赖升级；随后验证SkipBuild复用刚构建产物和外层checksum输出。
- `verify-linux-preparation.py`通过 **89项预备检查**，其中包含逐文件校验，不能称为89项Linux业务测试。包括Git Bash语法/LF、tar白名单/路径、内外hash、损坏包拒绝、YAML真实解析、独立迁移凭证结构、Windows误运行拒绝、原长凭证不夹带、systemd字段和Nginx静态契约、61个FR编号唯一性。
- 当前没有可用WSL/Linux环境；Git Bash是Windows工具，不等于Linux。没有执行nginx -t、systemd服务、Linux数据库/Redis连接、Linux备份/恢复、证书签发或真实并发试播。
- Nginx配置目前只有静态检查，不能因此宣称语法在目标机通过。systemd同理。脚本运行期依赖检查的具体错误需在目标机保留并解决，不盲目更新版本。
- 没有新建/导入/清空本地数据库，没有修改现有账号密码、Windows启动配置或参考项目。业务源码、POM、锁文件、V1～V6迁移保持原样。

检查日志在 `.local/linux-preparation-checks.log`，发布日志在 `.local/linux-package-preparation.log`。复核参考项目836个文件及16个受保护的版本/配置/迁移文件哈希；最终结果随本次交付记录核对。本轮没有推送GitHub，文档和脚本可随下一次正常提交一起保存。

最终复核结果：参考项目836个文件哈希完全一致；16个POM/前端锁及包配置/本地配置/V1～V6脚本哈希一致；新文档本地链接有效，Git空白检查通过。environment/versions.json仅把已有V6及新增模块补入元数据，没有改变任何软件版本。
