# 本地Linux验证进度

日期：2026-10-23。目标：隔离的本机Linux环境内验证现有Java后端、Vue构建产物、MySQL5.7.44、Redis候选5.0.14及三角色业务。当前按用户要求暂停Linux实测，等待支持WSL2的实体电脑或已有Linux环境。尚未取得Linux实测结果，不能将Windows/Git Bash检查计为Linux通过。

## 当前检查

- Windows的wsl.exe存在；首次`wsl --status`退出50并提示安装WSL。随后`wsl --list --quiet`返回`Wsl/EnumerateDistros/Service/E_ACCESSDENIED`。当前会话未能访问可用Linux，不能据此确认发行版已安装或运行。
- 当前自动化进程不是Windows管理员，无法替用户启用系统组件或完成重启。
- C盘当前可用约111.5GiB。拟使用独立Ubuntu22.04本地WSL2环境，不购买服务器，不对公网开放，不连接参考项目。
- 用户已下载Linux JDK8u231；已找到Downloads中的`jdk-8u231-linux-x64.tar.gz`，194,151,339字节。尚未在Linux解压或运行，不能据此标记JDK运行通过。

## 必须由电脑所有者完成的系统步骤

微软安装说明要求管理员PowerShell安装并可能重启：[官方安装说明](https://learn.microsoft.com/en-us/windows/wsl/install)。请保存现有工作，在**Windows管理员PowerShell**执行：

```powershell
wsl --install -d Ubuntu-22.04 --no-launch
```

若系统要求重启，先保存工作再自行重启；本轮不会自动重启电脑。若提示功能/虚拟化失败，保留错误原文，不重复安装或修改BIOS。安装后可运行`wsl --list --verbose`确认。初次Ubuntu如要求创建用户，请自行设置，密码不用发到聊天。

## 后续隔离规则

Linux使用独立数据库、Redis数据目录、凭证和上传目录；不复用Windows3306/6379的数据源。不改Windows root密码，不导入真实学生数据；优先使用测试账号及非个人素材。浏览器入口使用独立本机端口，避免与Windows8080/5173冲突。

待Linux可访问后再验证准确JDK版本、MySQL真实服务端VERSION、Redis真实运行版本、Flyway空库V1～V6、前后端代理、上传/播放/作业/心情/进度/权限、重启与备份恢复。只有实际执行后才填写通过结果。

当前没有购买、云部署、数据库改写或修改参考项目。


## 已下载、尚未安装的软件

独立目录：`C:\Users\eggma\Documents\Codex\local-env\linux-validation`。

- `mysql-5.7.44-linux-glibc2.12-x86_64.tar.gz`：694,785,800字节；已与MySQL官方CDN的MD5信息比对，另记录本地SHA-256。
- `redis-5.0.14.tar.gz`：2,000,179字节；已与Redis官方redis-hashes中的SHA-256比对。
- 来源和校验值记录在相邻`.verified.json`文件。校验通过只证明下载包一致，不代表安装或运行通过。
- Oracle Linux JDK8u231已由用户下载，尚未执行；没有用其他版本替换。

下载来源：[MySQL官方5.7.44说明](https://dev.mysql.com/doc/relnotes/mysql/5.7/en/news-5-7-44.html)、[Redis官方校验清单](https://raw.githubusercontent.com/redis/redis-hashes/master/README)、[Oracle官方归档](https://www.oracle.com/java/technologies/javase/javase8u211-later-archive-downloads.html)。

## 重启后的结果与暂停交接（2026-10-23）

- WSLService处于Running；用户本人执行`wsl --list --verbose`确认尚无已安装发行版。
- 用户执行`wsl --install -d Ubuntu-22.04`，下载后在创建虚拟机阶段失败，错误为`HCS_E_HYPERV_NOT_INSTALLED`，未运行到Java、MySQL、Redis或前端。
- 当前系统型号读取为Alibaba Cloud ECS；用户确认是云电脑或虚拟机。需核查宿主平台的嵌套虚拟化支持及Windows相关功能，不能仅凭此错误断言应用不兼容Linux，也不能断言所有云电脑都不支持WSL2。
- 用户明确选择：暂停Linux实测，等支持WSL2的实体电脑或已有Linux环境再继续。本轮不再安装发行版、启用系统功能或重启电脑；没有切换为WSL1。
- 已重新执行现有Windows Maven测试和Vue生产构建，构建完成；新发布包为`.local/linux-release/up-self-20261023-152617.tar.gz`及相邻SHA-256。这仍是Windows构建结果，不是Linux运行验证。
- 原有Windows数据库、root密码、应用配置和参考项目未修改；没有购买服务器、发布公网服务或推送仓库。

恢复时先确认新环境实际为Linux x86_64或可运行WSL2，核对JDK8u231、MySQL5.7.44和Redis Linux候选5.0.14，再用独立数据库、上传目录及端口验证。前后端闭环、Flyway、持久化及恢复仍待执行，不能直接勾选通过。

依据：[微软WSL排查说明](https://learn.microsoft.com/en-us/windows/wsl/troubleshooting)、[WSL1与WSL2差异](https://learn.microsoft.com/en-us/windows/wsl/compare-versions)。