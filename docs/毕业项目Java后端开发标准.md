# 毕业项目 Java 后端开发标准

**版本：1.0（源码核验稿，待用户确认）**  
**分析日期：2026-10-21（Asia/Shanghai）**  
**唯一技术基准：`C:\Users\eggma\Desktop\project\fts_admin_ac`**  
**参考 Git 提交：`baae5eabe80849ef71ef18d85eb3cf7ddfb3adb6`**

本标准用于建立独立的本科毕业设计后端工程。复用参考项目的 Maven 组织、技术版本、基础组件和实现模式；业务实体、业务表、角色名称和功能由毕业项目需求决定。本次只交付分析与标准，不创建业务工程、不执行数据库变更、不启动参考应用。

## 0. 适用范围、证据与复用边界

### 0.1 证据范围

- 已确认参考目录可访问，核查根 POM 和全部 6 个子模块 POM、三个应用 YAML、MyBatis/日志配置、启动与部署入口。
- 对源码目录进行检索，识别 684 个 Java 文件、34 个迁移 SQL；精读认证授权、异常、分页、数据源、基类、AOP、事务和代表性业务调用链。这里的完整架构分析不等于逐条审计所有交易业务规则。
- 将 POM 与本机已有 Maven 缓存复制到独立工作目录，在副本上提取 Maven 有效模型并成功运行全部模块的 `dependency:tree`；未对参考目录运行构建。附录 B 保留入口模块的完整解析依赖树。
- 直接读取本机缓存的 `spring-boot-dependencies-2.5.14.pom` 等 BOM/POM，交叉确认继承版本；未用网上常见版本填空。
- 使用本地 `flyway-core-7.7.3.jar` 的配置对象和文件名解析器核验迁移默认值与脚本名。只解析名称，不连接数据库、不执行 SQL。
- 未连接参考项目 MySQL、Redis、CRM、OSS 或交易服务；未验证部署服务器、真实数据库结构、迁移历史与线上运行产物。文档中“解析版本”指当前源码 POM 的 Maven 解析结果，不等同于已检查线上 JAR。
- 源码依据在各节及附录 A 列出。附录 A 的相对路径均相对于上述唯一参考根目录，不代表新工程应继续使用旧业务名称。

### 0.2 规则来源标记

| 标记 | 含义 | 执行方式 |
|---|---|---|
| **沿用** | 参考项目已经存在的有效结构或实现 | 原则上保留实现、依赖位置、接口形状和版本 |
| **业务替换** | 项目身份、数据、业务语义或外部业务系统差异 | 只替换毕业项目必要内容，不增加技术体系 |
| **待核实／最小修正** | 缺失证据、空实现、明显不适用于独立工程的内容 | 明确原因，确认后仅处理相关位置，不借机改造架构 |

没有在参考中找到的组件不作为毕业项目必装项。依赖树中的传递包也不自动构成需要单独部署的新服务。

### 0.3 必须先知道的核验结论

1. 工程是 **6 模块 Maven 单体**，只有 `admin` 为可执行入口。`ac-fts-quartz`、`ac-fts-generator` 仅剩 `target` 目录，未列入当前 reactor；`admin/pom.xml` 明确说明已移除，不能按通用若依模板加回。
2. Spring Boot 依赖为 **2.5.14**，Boot 打包插件却是 **2.1.1.RELEASE**。两者分别锁定，不擅自“统一”。
3. PageHelper 和 MyBatis-Plus 分页均有有效调用。新列表优先参考项目中更常见的 PageHelper 模式；保留已有 MP 模式，同一 SQL 不叠加两种分页。
4. HTTP 分页字段是 **`current`、`pageSize`**，不是通用模板的 `pageNum`。
5. 后台认证是 Spring Security + JWT + Redis，当前开发配置开启 TOTP 两步登录；客户端还依赖外部 CRM，不能直接将 CRM 账户体系带进独立毕业项目。
6. 用户主角色位于 **`sys_user.role_id`**。不能误写成所有用户均通过 `sys_user_role` 多对多授权。
7. 部分数据权限注解存在，但切面为空或条件未接入；不能宣称已有完整组织、部门、字段级数据隔离。
8. Flyway 目录是 **`classpath:db/draft`**，`V1__baseline.sql` 是空占位。直接复制全部 SQL 到空库不能完成初始化。
9. 某些依赖在 common 模块的声明／解析版本与 admin 解析版本不同。第 2 节记录真实差异，不按单个 POM 的字面值代替全工程解析结果。

## 1. 参考项目架构概述

### 1.1 实际模块与职责【沿用】

| 模块 | 实际职责 | 直接依赖的内部模块 |
|---|---|---|
| 根 `ac-fts` | `packaging=pom`；聚合模块、属性、BOM 与编译插件 | 无 |
| `ac-fts-common` | 通用返回值、异常、常量、实体基类、分页、Redis 工具、注解、工具及共用依赖 | 无 |
| `ac-fts-system` | 用户、角色、菜单、字典、参数、日志等基础系统 Service/Mapper/XML | common |
| `ac-fts-biz` | 参考项目业务 Domain/DTO/VO、Service、Mapper/XML、业务任务与外部业务协议 | common、system |
| `ac-fts-framework` | Security、认证服务、过滤器、拦截器、配置、异常处理、AOP、数据源、线程池 | system、biz |
| `ac-fts-aliyun-oss` | OSS SDK 接入、上传 Controller、上传日志 Domain/Service/Mapper | common |
| `ac-fts-admin` | 启动类、Controller、应用配置、Flyway SQL、Swagger 配置和最终可执行 JAR | framework、aliyun-oss、biz |

根 POM 的模块声明顺序是 admin、framework、system、common、biz、aliyun-oss；Maven 根据依赖拓扑实际按 common → system → biz → framework／oss → admin 构建。模块之间并非理想化的严格单向业务分层：framework 实际依赖 biz，这一点应记录并复用，不在迁移时擅自重构。

### 1.2 核心调用链【沿用】

```text
HTTP 请求
  → Servlet 过滤器 / Spring Security 链
  → JWT 校验及 Redis 登录态 → SecurityContext
  → MVC 拦截器（重复提交、请求日志）
  → Controller（权限、入参校验、分页、返回封装）
  → Service 接口 → ServiceImpl（业务规则、事务、归属校验）
  → Mapper 接口 → Mapper XML / MyBatis-Plus CRUD
  → MybatisSqlSessionFactoryBean → 动态数据源 → Druid → MySQL

异常 → MVC 全局异常处理器，或 Security 过滤器链自己的异常出口
审计 → @Log → LogAspect → AsyncManager → 系统日志存储
启动 → Spring Boot 自动配置 Flyway → 独立迁移连接 → db/draft
```

HTTP 认证处理与 MVC 异常处理分属不同位置；不能假设一个 `@RestControllerAdvice` 能接住所有过滤器异常。

### 1.3 运行方式【沿用＋业务替换】

- 启动类 `com.saas.FtsAdminApplication` 排除 `DataSourceAutoConfiguration`，使用项目自己的 Druid/动态数据源配置。
- 一个 Spring Boot 应用，以 `java -jar` 运行；默认 profile 为 `dev,druid-dev`，当前开发端口为 `88`，context-path 为 `/ftsApi`。
- 配置、端口、应用名称、上传目录及日志目录可替换为毕业项目独立值；修改 context-path 时必须同步检查过滤器中的硬编码路径。
- 基础运行需要 JDK 8、MySQL、Redis。Maven 是构建工具；不要求运行服务器额外安装完整开发环境。
- 原仓库同时有 Docker、Jenkins/Kubernetes 相关配置、Windows NSSM 部署和 Linux 单机 JAR 脚本。这表示存在不同部署入口，不表示毕业项目必须同时部署这些平台。单机 JAR 方式已有依据，可直接沿用。
- CRM、MT4/MT5、Netty/WebSocket 交易端点、邮件和 OSS 是参考业务所用能力；只有毕业需求确实使用对应能力时才启用其配置。不能把它们变成所有新功能的前置环境。

**源码依据：** [S01]–[S07]、[S08]、[S09]、[S35]。

## 2. 完整技术栈与实际版本清单

### 2.1 版本核验口径

“声明”来自根或子 POM，“管理”来自 BOM/有效模型，“解析”来自对应模块依赖树。项目父版本 `3.8.3` 是工程版本，不是 Spring Boot 版本；`ruoyi.version` 属性也用于内部模块版本。配置中的 `ruoyi.version: 1.0.1` 是应用展示版本，不参与 Maven 依赖选择。

本次有效模型使用本机 Maven **3.10.0** 生成，仅作为核验工具版本；参考仓库没有 Maven Wrapper 或明确固定 Maven 发行版本，不能将 3.10.0 当作已经确认的生产构建标准。

### 2.2 运行、核心框架及基础能力

| 技术／坐标 | 实际核验版本 | 依据及约束 |
|---|---|---|
| Java source/target | `1.8` | 根 POM；业务代码保持 Java 8 语法/API |
| 容器基础镜像 | `amazoncorretto:8u362` | Dockerfile；仅确认容器方案固定此标签，其他服务器补丁版未知 |
| Spring Boot BOM 与所有已声明 Boot starter | `2.5.14` | 根 BOM；包括 Web、AOP、Security、Validation、Redis、Mail、WebSocket、Test |
| Spring Framework | `5.3.20` | Boot BOM、admin 依赖树 |
| Spring Security | `5.5.8` | 同上；保留 `WebSecurityConfigurerAdapter`／`@EnableGlobalMethodSecurity` 用法 |
| Spring Data Redis | `2.5.11` | Spring Data BOM 与依赖树 |
| 内嵌 Tomcat | `9.0.63` | admin 依赖树 |
| MyBatis Boot starter / autoconfigure | `2.2.2` | 根属性管理；由 PageHelper starter 引入 |
| MyBatis | `3.5.9` | 所有业务模块依赖树；不是 PageHelper 上游父 POM 的 3.5.7，也不是 MP 上游的 3.5.3 |
| MyBatis-Spring | `2.0.7` | 依赖树 |
| MyBatis-Plus / core / extension / annotation | `3.3.1` | common 直接依赖与依赖树 |
| `com.baomidou:mybatisplus-spring-boot-starter` | `1.0.5` | 原坐标及原版本；不得改成另一个 starter 名称 |
| PageHelper Boot starter / autoconfigure | `1.4.1` | 根属性与依赖树 |
| PageHelper core | `5.3.0` | 依赖树 |
| JSqlParser | `4.2` | PageHelper 传递解析结果 |
| Druid starter / pool | `1.2.11` | 根属性与 DruidConfig；当前实际连接池 |
| `mysql:mysql-connector-java` | `8.0.29` | Boot BOM 与 admin 解析；驱动类 `com.mysql.cj.jdbc.Driver` |
| Flyway core | `7.7.3` | Boot BOM、有效模型、依赖树和本地 JAR |
| Lettuce | `6.1.8.RELEASE` | Redis starter 的实际客户端；不新增 Jedis |
| commons-pool2 | `2.9.0` | common 直接声明，由 BOM 管理 |
| Jackson databind | `2.12.6.1` | 有效模型与依赖树 |
| Jackson core / annotations / JDK8 / JSR310 / parameter-names | `2.12.6` | 依赖树；Jackson BOM 为 `2.12.6.20220326`，不要把 BOM 版本误填为每个 JAR 版本 |
| Fastjson2 | `2.0.57` | `com.alibaba.fastjson2:fastjson2`；不是 fastjson 1.x |
| Hibernate Validator | `6.2.3.Final` | Validation starter 传递 |
| Validation API | `jakarta.validation:jakarta.validation-api:2.0.2` | 此代源码使用 `javax.validation.*`，不能改成 Boot 3 风格的 `jakarta.validation.*` 导入 |
| JJWT | `0.9.1` | 根管理；HS512 JWT |
| SLF4J / Logback | `1.7.36` / `1.2.11` | Boot 日志体系与依赖树 |
| AspectJ weaver | `1.9.7` | AOP starter 传递 |
| Springfox starter | `3.0.0` | Swagger/OAS 文档；保留对旧 swagger-models 的 exclusion |
| `io.swagger:swagger-models` | `1.6.2` | admin 显式补入 |
| `io.swagger.core.v3` annotations / models | `2.1.2` | Springfox 传递；与 `io.swagger:*` 是不同坐标 |
| Netty | `4.1.77.Final` | Lettuce 传递，业务网络代码也有使用；不因此增加网络服务 |
| JUnit Jupiter / Mockito / AssertJ | `5.7.2` / `3.9.0` / `3.19.0` | admin 的 Boot Test 测试依赖 |

MySQL **服务端**与 Redis **服务端**版本未在已核查配置中固定，不能根据 JDBC/Lettuce 版本推断。没有查询原环境版本。

### 2.3 其余直接声明的外部依赖

除 2.2 已列组件外，下面覆盖子 POM 中其余直接外部依赖；完整坐标、scope、optional 和 exclusions 以附录 B 的机器提取表及原 POM 为准。

| 组件 | 声明／管理版本 | 位置或用途 |
|---|---|---|
| Hutool all | `5.8.5` | common；工具、JSON、加密等 |
| Lombok | common 显式 `1.18.30` | 存在模块间解析差异，见 2.4 |
| Apache HttpClient | common 显式 `4.5.6` | 存在模块间解析差异，见 2.4 |
| `io.swagger:swagger-annotations` | common 显式 `1.5.21` | admin 最终不同，见 2.4 |
| commons-lang3 | `3.12.0` | BOM 管理 |
| commons-io | `2.11.0` | 根属性 |
| commons-fileupload | `1.4` | 根属性 |
| POI OOXML | `4.1.2` | 根属性；Excel 工具 |
| EasyExcel | `2.2.1` | common 显式 |
| SnakeYAML | `1.28` | BOM 管理 |
| Guava | `18.0` | common 显式 |
| UserAgentUtils | `1.21` | 根属性 |
| Kaptcha | `2.3.2` | framework 引入 |
| OSHI core | `6.1.6` | framework 引入 |
| Aliyun OSS SDK | `3.15.1` | oss 模块；不携带原账户和存储桶 |
| Java-WebSocket | `1.3.8` | common 显式；业务客户端 |
| `javax.mail:javax.mail-api` | `1.6.2` | common 显式；邮件实现传递为 `com.sun.mail:jakarta.mail:1.6.7` |
| `javax.xml.bind:jaxb-api` | `2.3.1` | common；与测试树的 `jakarta.xml.bind-api:2.3.3` 是不同坐标 |
| `javax.servlet:javax.servlet-api` | `4.0.1` | common；保留当前声明位置 |
| Boot devtools | `2.5.14`，optional | admin；开发热重启 |
| Boot configuration-processor | `2.5.14`，optional | oss 直接声明；另有传递路径 |

`commons-collections:3.2.2` 仅在根 dependencyManagement 声明，未出现在 admin 解析树中，不能因此要求新模块主动依赖它。`commons-collections4:4.4` 是 POI 的实际传递依赖。

HikariCP `4.0.3`、Ehcache `3.9.9` 虽出现在传递依赖树，当前数据源实际为 Druid，缓存实现主要为 Redis；未发现对应 Hikari 数据源配置或业务 `@Cacheable/@CachePut/@CacheEvict` 使用。不要将传递存在解释为需新增第二连接池／缓存架构。Springfox 引入的 MapStruct `1.3.1.Final` 也不等于项目要求新增对象映射层。

### 2.4 必须保留的模块间版本差异【沿用，禁止擅自整理】

| 坐标 | common 自身解析 | system / biz / framework / oss 解析 | admin 解析 |
|---|---|---|---|
| `org.projectlombok:lombok` | `1.18.30` | `1.18.24` | `1.18.24` |
| `org.apache.httpcomponents:httpclient` | `4.5.6` | `4.5.13` | `4.5.13` |
| `io.swagger:swagger-annotations` | `1.5.21` | `1.5.21` | `1.6.2` |

原因是 common 的显式直接版本，在下游变成传递依赖后受下游的依赖管理和路径仲裁影响；admin 又直接依赖 swagger-models。后续按原 POM 结构复用并对比每个模块的树，不应只将 admin 版本硬塞回 common，也不应只把 common 版本写进统一清单。

### 2.5 Maven 构建配置

| 配置 | 核验值 | 规则 |
|---|---|---|
| 编码 | source/reporting UTF-8 | 沿用 |
| maven-compiler-plugin | `3.1`；source/target `${java.version}` | 沿用，不改用更高语言级别 |
| spring-boot-maven-plugin | `2.1.1.RELEASE`；`fork=true`；`repackage` execution | 沿用；与 Boot BOM 分开记录 |
| maven-war-plugin | `3.1.0`；`failOnMissingWebXml=false` | admin 虽配置此插件，packaging 实际为 jar，不据此改成 WAR |
| finalName | `${project.artifactId}` | 当前产物 `ac-fts-admin.jar`；新工程按新 artifactId 命名 |
| maven-jar-plugin.version 属性 | `3.1.1` | 根 POM 仅声明属性，未发现插件引用；**不能说实际 jar 插件已固定 3.1.1** |
| Surefire、Resources、Clean、Install 等 | 仓库未显式固定 | 取决于真实 Maven 发行版的默认生命周期；列为待核实 |
| Maven 仓库 | 阿里云 public，另有 Maven 默认仓库继承 | 沿用 POM 管理方式；本机镜像/代理不视为项目标准 |

本机 Maven 3.10.0 的有效模型生成了 jar `3.5.1`、Surefire `3.6.0` 等默认值。这些是本次工具环境结果，**不是已核实的原项目部署构建版本**，不作为毕业项目的新版本要求。

**源码依据：** [S01]–[S07]、[S09]；本机 BOM 位置见附录 A；完整解析树见附录 B。

## 3. Maven 模块划分与依赖管理规范

### 3.1 原项目方式【沿用】

根 POM 自行定义 `groupId/artifactId/version`，没有继承 `spring-boot-starter-parent`，而是在 `dependencyManagement` 中 import `spring-boot-dependencies:2.5.14`。内部模块版本通过 `${ruoyi.version}` 管理，所有子模块继承同一个根工程。

### 3.2 毕业项目规则

1. 建立独立聚合根和相应的 6 类模块，名称可统一替换为毕业项目名称；保持第 1 节依赖关系，不改为微服务、另起 API/DAL/DDD 模块或抽象平台。
2. 复用根属性、BOM import、dependencyManagement 和子模块声明位置；不是复制大量带版本的依赖到每个子 POM。
3. 本次不为了“清理”而改变 common 中的历史直接版本或挪动依赖；新增业务优先使用已有组件。
4. 新增第三方依赖必须先证明现有依赖无法满足已确认需求，再列出最小改动等待确认。不能自动换分页、JSON、认证、数据库迁移框架。
5. 内部模块名称、根 groupId、包前缀可统一业务化，但必须同步替换 parent、dependencyManagement、modules、内部 dependencies、扫描表达式、XML namespace/type、启动类等。不要仅改目录名。
6. oss 模块可保留既有接入位置；是否实际配置独立 OSS 账号取决于毕业需求。不能为“六个模块齐全”而连接参考项目 OSS。
7. 不把 `target`、历史构建产物、真实 YAML、数据导出、账户密码或 Git 历史作为新项目模板内容复制。

**验收：** 父子版本一致；reactor 模块与依赖图相符；无新增中间件；新旧依赖树差异仅为已批准的业务模块/功能依赖差异。依据 [S01]–[S07]。

## 4. 项目目录结构与分层职责

### 4.1 参考目录骨架【沿用】

```text
根目录/pom.xml
├─ ac-fts-admin/
│  ├─ src/main/java/com/saas/FtsAdminApplication.java
│  ├─ src/main/java/com/saas/web/controller/{system,admin,client,...}/
│  ├─ src/main/java/com/saas/web/core/config/
│  ├─ src/main/java/com/saas/web/task/
│  └─ src/main/resources/
│     ├─ application.yml / application-dev.yml / application-druid-dev.yml
│     ├─ mybatis/mybatis-config.xml
│     ├─ logback-spring.xml / i18n/
│     └─ db/draft/
├─ ac-fts-framework/src/main/java/com/saas/framework/
│  └─ config/ security/ web/service/ web/exception/ aspectj/ datasource/ interceptor/
├─ ac-fts-system/src/main/
│  ├─ java/com/saas/system/{domain,service,service/impl,mapper,...}/
│  └─ resources/mapper/system/
├─ ac-fts-biz/src/main/
│  ├─ java/com/saas/biz/{domain,dto,vo,service,mapper,...}/
│  └─ resources/mapper/<业务域>/
├─ ac-fts-common/src/main/java/com/saas/common/
│  └─ core/ annotation/ constant/ enums/ exception/ utils/ config/ filter/
└─ ac-fts-aliyun-oss/src/main/java/
```

以上是组织骨架，不要求复制参考项目交易业务目录。biz 中既有 `service/<业务域>/impl`，也有历史 `service/impl/<业务域>`；新功能选用同业务域现有位置，普通新域按 `service/<业务域>/impl` 的已存在模式放置，不批量重排旧文件。

### 4.2 层职责与对象规则

| 层／对象 | 参考方式 | 毕业项目规则 |
|---|---|---|
| Controller | 主要在 admin 内，oss 模块也自带上传及日志 Controller；常继承 BaseController | 负责 HTTP 契约；新普通业务接口放 admin，已有 OSS 内聚结构保留 |
| Service 接口 | system/biz；如 `ISignalInfoChangeRecordService` | 按业务能力定义方法，不为了每个工具建立接口 |
| ServiceImpl | `@Service`；可直接 implements 接口，也可继承 BaseServiceImpl | 放业务判断、归属校验、事务和 Mapper 调用；按需要选已有模式 |
| Mapper | 普通接口、`BaseDao<T>`、少量注解 SQL | 放数据访问定义；SQL 主要在 resources/mapper 下 |
| Domain | 表结构对象；有 `@TableName/@TableId/@TableField` 与 XML resultMap 两类 | 按字段语义映射，不把所有表一律继承同一基类 |
| DTO | 请求条件、业务输入或中间数据 | 列表条件可包含 current/pageSize；不信任客户端传入的身份/审计字段 |
| VO | 展示结果、跨表报表输出 | 只暴露接口需要的字段；可用 XML 显式别名映射 |
| common 工具 | 通用响应、分页、错误码、Redis、日期等 | 先复用；毕业业务常量放业务模块，不把全部业务逻辑塞入 common |

代表链路可直接阅读 `SignalInfoChangeRecordController → ISignalInfoChangeRecordService → SignalInfoChangeRecordServiceImpl → SignalInfoChangeRecordMapper → SignalInfoChangeRecordMapper.xml`。[S22]

## 5. Java 命名、代码组织与对象设计规范

### 5.1 命名与代码风格【沿用】

- 包名小写；类名 PascalCase；字段、方法 lowerCamelCase；常量大写下划线；类后缀按现有 `Controller/Service/ServiceImpl/Mapper/DTO/VO`。
- 接口有 `I` 前缀与无 `I` 前缀两种实际形式，跟随所在模块已有同类代码，不为统一风格重命名全部接口。
- 表/列一般使用下划线小写；Java 属性用驼峰。XML 显式 resultMap 或 SQL `AS propertyName` 是明确可复用的映射方式。
- 保持 UTF-8 和 Java 8。可使用现有 Lombok `@Data/@Builder/@RequiredArgsConstructor/@Slf4j`；不引入 records、sealed class、`jakarta.*` 新版 API 或新的代码生成工具。
- 原项目同时使用字段注入和构造器注入，按相邻代码保持一致，不把注入风格改造变成毕业功能的前置任务。
- 原项目金额/比例大量使用 `BigDecimal`，主键和大账号使用 `Long`。毕业项目按业务精度选择现有类型，SQL 与 Java 字段类型同时变更，不以 double 保存需要精确计算的金额。
- 注释说明字段语义、单位、枚举值和非显然的事务/并发约束；不要为每个简单 getter 加模板化说明。

### 5.2 两套实体基类不能混用【沿用】

`BaseEntity` 含 `createBy/updateBy/createTime/updateTime/remark/searchValue/params`，params 懒初始化，searchValue/params 有 `@JsonIgnore`。

`SupperEntity`（源码就是这个拼写）含 `creator/updator/createTime/updateTime/params`，并定义 `Select/Update/Delete/Add` 校验分组，params 标记 `@TableField(exist=false)`。不要擅自将 `SupperEntity` 改名为 `SuperEntity`，也不要假定它的 params getter 与 BaseEntity 一样会自动初始化。

`BaseDao<T extends SupperEntity>` 继承 MP `BaseMapper<T>`；`BaseService<T>` 继承 `IService<T>`；`BaseServiceImpl` 的 `insert/update` 设置当前操作人和时间。直接调用 Mapper 时，这些自定义审计填充不会自动执行。

反射生成 UpdateWrapper 的工具仍要求调用者设置正确 WHERE；不能因为使用基类就视为已防止全表更新。针对普通新功能优先使用已有明确字段的 XML/Wrapper 方法。

**源码依据：** [S10]、[S11]、[S22]；代表实体 `SysUser`、`SignalInfoChangeRecordVO`。

## 6. Controller 接口开发规范

### 6.1 已有接口模式与毕业规则【沿用】

1. `@RestController` + 资源级 `@RequestMapping`，后台放 admin 的相应业务 Controller 包。
2. 列表 `GET /list`，详情 `GET /{id}`，新增 `POST`，编辑 `PUT`，删除 `DELETE /{ids}` 是已有主要模式；业务动作按现有动作接口方式表达，不统一强改所有历史路由。
3. 后台受控功能写 `@PreAuthorize("@ss.hasPermi('模块:资源:动作')")`。Controller、菜单 `perms`、前端权限标识逐字相同。
4. 请求体用 `@RequestBody`；Bean Validation 用 `@Validated` 或现有 `@Valid`，按需使用 `SupperEntity.Add/Update` 分组。
5. 当前用户来自 `SecurityUtils`／BaseController，不接受请求中的 userId 作为当前登录人；新增/修改时由服务端设置审计字段。
6. 写操作按现有模式加 `@Log(title=..., businessType=...)`；敏感入参/响应按第 12 节处理。
7. 普通结果返回 `AjaxResult`；列表返回 `TableDataInfo`；不要改成新的 `Result<T>` 或再包一层 `data.rows`。
8. 不在 Controller 全面包裹 `try/catch(Exception)` 再返回成功；业务异常向现有全局处理器传播。
9. 不是所有现有 Controller 都严格不碰 Mapper，现有少量历史例外不应扩散为新功能默认模式。

### 6.2 可复用的分页端点片段

下面只是文档中的实现模板；`ExampleDTO/ExampleVO/exampleService` 是待需求确认后替换的名称，当前未生成任何对应业务代码。

```java
@PreAuthorize("@ss.hasPermi('biz:example:list')")
@GetMapping("/list")
public TableDataInfo list(ExampleDTO dto) {
    int current = dto.getCurrent() == null ? 1 : dto.getCurrent();
    int pageSize = dto.getPageSize() == null ? 10 : dto.getPageSize();
    if (current < 1) {
        throw ServiceException.fieldError("current", ErrorCodeConstants.CODE_1013);
    }
    if (pageSize < 1 || pageSize > 1000) {
        throw ServiceException.fieldError("pageSize", ErrorCodeConstants.CODE_1013);
    }
    PageHelper.startPage(current, pageSize);
    try {
        List<ExampleVO> list = exampleService.selectExampleList(dto);
        return getDataTable(list);
    } finally {
        PageHelper.clearPage();
    }
}
```

该片段直接对应 [S22] 的有效代码模式；`1000` 是该参考接口的限制，不是全局分页组件已自动实现的上限。毕业新接口采用该边界时需明确写出；不同业务只有确有需要才调整。

## 7. 统一响应与异常处理规范

### 7.1 响应结构【沿用】

`AjaxResult extends HashMap<String,Object>`，键是 `code/msg/data`；成功业务码来自 `HttpStatus.SUCCESS=200`，默认失败业务码为 500。三参数构造遇到 null data 会置为 Hutool 的空 `JSONObject`，常见成功响应为：

```json
{"code":200,"msg":"操作成功","data":{}}
```

`AjaxResult.success(Object)` 返回业务数据；`toAjax(int)` 判断影响行数是否大于 0；`toAjax(boolean)` 判断业务操作结果。`put` 支持增加同级键，登录和部分统计接口确有扩展字段，因此不能宣称全工程严格只有三个字段。

分页结构是同级四字段：

```json
{"code":200,"msg":"查询成功","rows":[],"total":0}
```

common 中还存在 `R<T>`，但本标准选取后台主流 `AjaxResult/TableDataInfo` 模式作为新接口依据，不另造返回类，也不批量改写已有特例。

### 7.2 异常处理器的实际行为

| 异常 | `GlobalExceptionHandler` 当前返回 |
|---|---|
| `AccessDeniedException` | AjaxResult 业务 code=403，固定无权限消息 |
| `ServiceException` | 使用异常 code；code 为空时按默认失败处理；记录异常日志 |
| `RuntimeException` | 业务 code=500，消息 `Unknown Error`；服务器记录堆栈 |
| 其他 `Exception` | 业务 code=500，消息 `System Error`；服务器记录堆栈 |
| `BindException` | 取第一条 defaultMessage，返回默认错误码 500 |
| `MethodArgumentNotValidException` | 取第一字段错误；message 命中映射则用对应业务码，否则 code=1003；msg 为原 message |
| 请求方法不支持 | AjaxResult 默认失败码及异常消息 |
| `DemoModeException` | 提示演示模式不允许操作 |

**HTTP 状态与 JSON code 不一致是当前契约，必须如实记录。** 多个 handler 使用未指定参数的 `@ResponseStatus`，其默认响应状态为 HTTP 500，包括返回业务 code=403 的方法；认证入口 `AuthenticationEntryPointImpl` 返回业务 code=401，但它调用的 `ServletUtils.renderExceptionString` 明确设置 HTTP 500。不要写成“统一返回 HTTP 401/403”。过滤器链中已认证但 authority 不足的拒绝仍可能走 Security 默认 HTTP 403，当前未配置统一的自定义 AccessDeniedHandler。前端与验收需覆盖两类出口。

本次保留这些已存在的契约；若毕业前端要求不同 HTTP 状态，先确认具体兼容需求，再作局部变更，不在本标准中暗改。

### 7.3 `ServiceException` 构造规则：必须按本项目签名使用

```java
// 一个 String 参数表示数字错误码，不是任意中文消息。
throw new ServiceException(ErrorCodeConstants.CODE_1003);

// 参数次序：错误码字符串、自定义消息。
throw new ServiceException(ErrorCodeConstants.CODE_1013, "无效的参数");

// 按项目已有字段错误格式：msg 放字段名，code 放业务码。
throw ServiceException.fieldError("current", ErrorCodeConstants.CODE_1013);
```

`new ServiceException("操作失败")` 会在构造中执行 `Integer.valueOf(...)`，产生 NumberFormatException。源码部分旧调用仍这样使用，例如 `RateLimiterAspect` 的超限分支；毕业项目移植这些基础类时需要列入**最小修正项**，将此处改为已有数字错误码构造。不得因此新建另一套异常框架。

错误码集中在 `ErrorCodeConstants` 的字符串常量及 `CODE_MAP`。通用码含 1000（权限不足）、1003（必填项）、1013（无效参数）、2000（频率过高）。1001 等许多码带原交易业务语义，毕业业务需按自身语义定义/映射，不能直接沿用其业务文案。参数校验映射表同样只保留毕业业务字段。

### 7.4 数据库、事务与异常传播

没有专门覆盖所有数据库异常的 handler；普通数据库异常通常经 RuntimeException 分支记录并返回通用错误。Service 可参考现有 DuplicateKeyException 处理将明确业务冲突转换为 ServiceException，但未知错误必须继续抛出，不能吞掉后提交事务。

`GlobalException`、`BaseException` 等类存在，不等于都有单独处理分支。新业务正常使用已有 ServiceException；不要根据类名推断未实现的异常码体系。

**源码依据：** [S10]、[S12]、[S13]、[S14]、[S15]、[S30]。

## 8. 分页查询规范

### 8.1 实际参数与默认值【沿用】

| HTTP 参数 | 对应内部字段 | 实际行为 |
|---|---|---|
| `current` | PageDomain.pageNum | `TableSupport.PAGE_NUM="current"`；转换失败／缺省默认 1 |
| `pageSize` | pageSize | 默认 10；全局未统一限制最大值 |
| `orderByColumn` | orderByColumn | 转下划线命名，再经 SqlUtil 排序字符检查 |
| `isAsc` | isAsc | 支持 `asc/desc`、`ascend/descend`；TableSupport 对其他非空值回退 asc |
| `reasonable` | reasonable | 缺省经 TableSupport 置为 null 后 getter 返回 true；PageDomain 字段初值 false 并不等于实际请求默认 false |

直接 `PageHelper.startPage(current,pageSize)` 的接口不经过 PageUtils 的排序及 reasonable 设置，不应将两条路径的行为写成完全相同。`ascending/descending` 在 PageDomain setter 有适配，但 TableSupport 入口对非 asc/desc/ascend/descend 会先规范为 asc，不能承诺该入口支持所有前端排序字符串。

### 8.2 PageHelper 主流路径

`startPage()` → `PageUtils.startPage()` → `TableSupport` 读取 HTTP 参数 → `PageHelper.startPage(...).setReasonable(...)` → 紧接目标 Mapper SELECT → `getDataTable(list)` → `new PageInfo(list).getTotal()`。

- 配置为 `helperDialect=mysql`、`supportMethodsArguments=true`、`params=count=countSql`。
- starter 自动配置将 PageInterceptor 注册至 SqlSessionFactory；自定义 MyBatisConfig 中显式的分页拦截器是 MP PaginationInterceptor，不要误认为 PageHelper 不存在或由同一个拦截器实现。
- 分页应紧贴真正的列表 SELECT。若 Service 在列表前先查权限、字典、配置或数据源信息，应在这些查询之后才开启分页；不要让前置查询消耗本次分页状态。
- 需要提前返回、条件分支或先处理参数的接口参照 [S22] 使用 try/finally 清理。不得让 ThreadLocal 分页状态残留到后续同线程查询。
- 保留 Mapper 返回的 Page/List 对象；不要在 Controller 返回前 `stream().collect(...)`、new ArrayList 或空页时换成空集合，否则 `PageInfo` 可能丢失真实 total。
- 原项目已有在原列表中逐项回填字段的模式；如确实必须重组结果，先保存原 total，再使用既有 TableDataInfo 封装，不能用当前页行数替代总数。
- 分页 SELECT 不再手写 LIMIT；导出全量查询不启动分页。

### 8.3 MyBatis-Plus 现存路径

`SysRoleController.list` 调用 `beginPage()` 建 `IPage`，Service 把 page 传给 `SysRoleMapper.selectRoleList(@Param("info") SysRole, IPage<SysRole>)`，XML 执行查询，`PaginationInterceptor` 更新 page.total；Service 返回 `TableDataInfo`。`BaseServiceImpl.findPageForWrapper` 也提供同类封装。

保留 MP 3.3.1 的旧 `PaginationInterceptor`，不改成较新版本的 `MybatisPlusInterceptor/PaginationInnerInterceptor`。同一调用只选这一条或 PageHelper 条，不在 `IPage` Mapper 前再 startPage。

### 8.4 复杂 SQL 和汇总

- 多表关联、动态条件仍放 XML，参数用 `#{}`。按业务确认一条结果对应什么主实体，确保 JOIN 不造成重复行和错误总数。
- 复杂 GROUP BY/UNION/去重查询必须核对插件 count 与真实结果数；当前未发现统一的自定义 count 实现可直接套用，不能因为有 `params=count=countSql` 就声称每个复杂 SQL 都有专用 count。
- 全量过滤集汇总独立查询，不受当前页 LIMIT 影响。参考 `ISignalTradeDataReportServiceImpl` 对列表与汇总的分开处理。
- 内存分页 `PageUtils.getPageDateList/getTableDate` 实际存在，其越界行为可回退到最后一页；仅在原有内存集合业务确有需要时复用，普通数据库列表优先 SQL 分页。
- 排序检查仅保证允许字符，不验证业务字段存在；新接口依照实际输出字段接受排序，固定排序可参考 `ORDER BY record_time DESC,id DESC`。

**源码依据：** [S16]、[S17]、[S18]、[S22]、[S23]、[S24]。

## 9. 认证、角色及权限验证规范

### 9.1 两条 Security 链【沿用】

`SecurityConfig` 启用 `prePostEnabled=true,securedEnabled=true`，使用两个 `WebSecurityConfigurerAdapter`：

| 链 | 匹配及门槛 | 实现 |
|---|---|---|
| 客户端链 | `@Order(1)`；`/client/**`；除放行路由外要求 authority=`crm` | ClientJwtAuthenticationTokenFilter、ClientLogoutSuccessHandlerImpl |
| 后台链 | 其他请求；除放行路由外要求 authority=`admin` | AdminJwtAuthenticationTokenFilter、AdminLogoutSuccessHandlerImpl |

两条链均无状态 `STATELESS`，关闭 CSRF，JWT 过滤器置于 UsernamePasswordAuthenticationFilter 之前，使用既有 CORS 配置。后台链中的 `/client/**.permitAll()` 不表示客户端完全无需认证，因为优先客户端链先匹配该路径。

authority=`admin` 表示后台登录身份，不等于业务超级管理员；具体菜单权限通过 `@ss.hasPermi` 校验。不要把所有后台登录用户当作拥有 `*:*:*`。

实际放行项包括 `/empLogin`、`/empLogin/totp`、`/captchaImage`、部分静态资源、`/openApi/**`、`/open/manager/**`、客户端登录和 `@Anonymous` 收集出的路径。毕业项目只保留确认需要的公开接口及基础登录入口；参考项目的业务开放路径不能无审查复制。

`AdminJwtAuthenticationTokenFilter` 还硬编码匹配 `/ftsApi/client/**`。context-path 改名属于必要业务适配时，必须同步处理该位置并验证链路，不得只改 YAML。

### 9.2 后台登录、密码、JWT 与 Redis【沿用】

1. 当前后台入口 `POST /empLogin` 在 Controller 对明文输入执行 MD5 并转小写，再交给 AuthenticationManager。
2. `UserDetailsServiceImpl` 查询 SysUserAuth，检查用户存在/停用，构造 LoginUser，赋予后台 authority，并装载菜单权限。
3. 数据库存储的后台密码由 `BCrypt(MD5(password))` 这条项目既有流程产生；新增、重置和修改密码必须与登录预处理一致。不能只在某一个入口更换处理算法，也不能把参考库密码散列带入新库。
4. `SysLoginService.empLogin` 检查后台 userType，当前只接受 `userType=0`。有 Redis 错误计数和锁定逻辑，常量为最多 5 次尝试、锁定 30 分钟；按原算法复用并覆盖边界测试。
5. TOTP 当前开启。密码通过后返回 ticket；未绑定者返回绑定所需数据，已绑定者返回 needTotp。第二步 `/empLogin/totp` 校验后才发正式 token。ticket 有效期 5 分钟，时间片使用记录通过 Redis `setIfAbsent` 防重复。
6. TotpService 使用已有 JDK 加密实现：HmacSHA1、6 位、30 秒时间片，密钥经 AES/GCM 保存；不需要引入新的认证 SDK。毕业项目使用独立 encryptKey；改 key 会影响已存密文，不能随意轮换后假定旧绑定仍可解密。
7. TokenService 使用 JJWT 0.9.1 的 HS512；JWT 保存登录 UUID，真实 LoginUser 在 Redis。当前没有在该签发代码中设置 JWT `exp`；有效期由 Redis TTL 与服务端登录态控制，不能称为完全离线 JWT。
8. 请求头 `Authorization`，支持 `Bearer ` 前缀；admin expireTime 为 120 **分钟**。`verifyToken` 实际每次调用都 refreshToken，并非“仅剩 20 分钟才续期”，不要被遗留常量名误导。
9. token secret 启动时要求非空且至少 32 字符；独立工程生成自己的值。注销删 Redis 登录态；角色权限变更使用现有刷新方法，TOTP 重置后踢出该用户已有 token。

当前 empLogin 的有效实现没有调用其参数中的验证码校验方法；虽然 `/captchaImage` 和 validateCaptcha 方法存在，也不能声称所有后台登录已强制校验图形验证码。若毕业需求明确要求图形验证码，按已有方法接入并单独确认，不另引入验证码体系。

### 9.3 客户端认证的业务差异【业务替换】

ClientTokenService 兼容本地 JWT 和 CRM token 直通：后者从 Authorization 或配置的 cookie 读取，向 CRM 解析身份并缓存 300 **秒**，绝对过期不续期；本地 JWT 的 expireTime 为分钟、采用滑动续期。清除本地 CRM 缓存不等于 CRM token 已在上游失效。

毕业项目是独立网站，不能为了复用登录机制而强制依赖原 CRM、复制 CRM 数据源或调用其真实端点。确认用户体系后，在原有 Security/JWT/Redis/用户加载结构中接入毕业项目自己的用户数据；保留需要的后台和客户端链划分，角色名按业务调整。是否需要独立客户端链及哪些登录方式属于需求确认项，本次不预设全部角色或注册流程。

### 9.4 用户、角色、菜单与权限标识【沿用＋业务替换】

- 当前主要关系是 `sys_user.role_id → sys_role.role_id → sys_role_menu → sys_menu`。角色菜单含选中与半选处理，新增角色菜单逻辑按现有 Service/Mapper 复用。
- SysUser 的 `roles` 集合以及部分旧 SQL 中出现 `sys_user_role`，不改变当前主要单 role_id 查询链的事实。
- 菜单类型 `M/C/F` 分别用于目录、菜单、按钮权限。是否显示菜单与是否允许调用接口是不同判断，不能仅隐藏按钮作为授权。
- `PermissionService` bean 名是 `ss`；`hasPermi` 使用 permissions 集合，支持 `*:*:*`；`hasAnyPermi` 支持逗号分隔。权限形式如 `system:user:list`、`signal:infoChangeRecord:list`，毕业业务替换资源名称。
- `SysUser.isSuperAdmin` 判断 userId=1 或 `SysRole.isAdmin(roleId)`；后者实际判断 roleId=1。初始化时保证该约定与新库一致，不给普通毕业角色使用这些特定 ID。
- `SysPermissionService.getRolePermission` 与其他旧重载存在不同管理员判断；`@ss.hasRole` 还依赖 LoginUser.user.roles 是否装载。新接口优先参考已实际使用的 hasPermi 路径，不因方法存在就假定所有角色判断路径均经过验证。
- 角色/用户权限编辑后调用 `refreshOnlineByRole/refreshOnlineByUser` 更新在线权限；菜单权限数据通过 Flyway 初始化或变更，业务角色授权通过现有管理逻辑完成。

### 9.5 数据权限的真实边界【待核实／最小修正】

| 位置 | 已核验状态 | 毕业项目执行规则 |
|---|---|---|
| DataScopeAspect | 定义了范围 1–5，但循环内 `String dataScope=""`，没有实际取 role 的范围；部分部门分支为空 | 不能宣称此注解已实现完整部门范围隔离 |
| DataPowerAspect | doBefore、handleDataScope、过滤和清理方法为空 | 不将 `@DataPower` 当作有效的数据安全措施 |
| FieldAspect | 返回值字段处理为空 | 不宣称 `@DataField` 已自动脱敏或删字段 |
| MyBatis OrgStatisticsInterceptor | 根据参数 orgId 改写部分 `if_statistics=1` 条件 | 属于原业务统计规则，不是通用租户/组织隔离 |
| 业务 Service | 明确取 SecurityUtils.userId、查询账户归属，禁止操作他人记录 | 新业务的数据归属规则直接采用这一有效方式 |

例如 `ApplyFtsMtServiceImpl.createWithdrawalOrder` 从登录态取 userId，并通过 Mapper 校验账户所有权。毕业项目有“只能看/改自己的记录”的需求时，Controller、Service、SQL 的详情、更新、删除、导出都要带服务端确认的归属条件；不能依赖客户端传来的 ownerId 或空切面。

若确实需要部门多级范围或字段级权限，先补充明确需求，再判断对原切面做什么最小修正。本次不预建多租户平台或另一套权限框架。

**源码依据：** [S19]、[S20]、[S21]、[S25]、[S27]、[S28]、[S31]。

## 10. 数据库访问、SQL 与事务规范

### 10.1 Mapper 与 XML【沿用】

- Mapper 扫描是 `@MapperScan("com.saas.**.mapper")`；XML 位置 `classpath*:mapper/**/*Mapper.xml`；别名扫描 `com.saas.**.domain`。
- XML 的 namespace 对应 Mapper 全限定类名，id 对应方法；DTO/VO 不在 domain 别名扫描范围时用全限定类名。
- 查询参数使用 `#{}`；多个参数显式 `@Param`，XML 路径一致。动态条件使用 `<if>/<where>/<trim>/<set>/<foreach>/<include>` 等现有 MyBatis 方式。
- 当前 mybatis-config.xml 中 cacheEnabled=true、useGeneratedKeys=true、executor=SIMPLE、logImpl=SLF4J；`mapUnderscoreToCamelCase` 在该文件中是注释。MP 工厂还可能提供自身默认配置，不应据此断言所有路径完全关闭驼峰转换；为避免依赖隐含默认，复杂查询沿用明确 resultMap／AS 别名。
- cacheEnabled=true 只是允许缓存机制，不等于每个 Mapper 已启用二级缓存；本次不为新功能添加 `<cache>` 或另一种缓存。
- 简单 CRUD 复用 BaseDao/MP；复杂联表、汇总、批量和状态更新放 XML；不新建一套 JPA Repository。
- `BaseServiceImpl.insert/update` 与直接 Mapper 写入的审计填充不同，必须按所选路径确认 creator/updator/time 字段是否设置。

### 10.2 SQL 安全和业务适配

- 原代码存在 `${info.params.dataScope}`、动态库名和 `$crm_db$` 替换。`${}` 是文本拼接，不是参数绑定；不得让外部输入直接控制这类 SQL 片段。
- `$crm_db$` 由 MyBatisConfig.CrmSchemaInterceptor 替换，原业务还通过 MtServerDbResolver 解析外部数据库。毕业项目普通单库查询不应复制 CRM/MT 库前缀和真实库名。
- 只有确有业务需要的动态标识符才能走原有服务器端校验路径；普通新表名/列名固定在 XML 中。
- LikeEscapeInterceptor 会对它识别到的 LIKE 参数转义 `%/_/反斜线`，新查询要用包含这些字符的输入验证结果；不能同时无条件重复转义。
- 日期、状态、枚举、逻辑删除条件按业务明确。参考业务有手工逻辑删除模式，未建立“所有实体自动逻辑删除”的统一配置，不假设全局 `@TableLogic` 自动生效。
- 变更字段长度/类型、索引和约束时，迁移脚本、Domain、DTO/VO、XML 与校验范围同一次提交更新。
- 不能直接复制引用原库、原表、原固定菜单 ID 或原业务数据的 SQL。

### 10.3 Druid 与数据源路由【沿用】

`DruidConfig` 创建 masterDataSource；slave 仅在 `spring.datasource.druid.slave.enabled=true` 时创建；`dynamicDataSource` 为 @Primary，默认 master。当前配置 slave=false，只需要一个业务主库，不要求额外准备从库。

`@DataSource` → DataSourceAspect（`@Order(1)`）→ ThreadLocal → DynamicDataSource；finally 清理 ThreadLocal。数据源选定应在事务获取连接之前完成，不能假定一个已开启事务中随意切换注解就更换数据库；不在新功能中引入分布式事务。

现有池参数为 initialSize=5、minIdle=10、maxActive=20、maxWait=60000ms，validationQuery=`SELECT 1 FROM DUAL`，testWhileIdle=true。Druid 监控 Servlet 当前关闭，慢 SQL 阈值 3000ms，wall multi-statement-allow=true。先保留这些原始配置项，独立连接地址和账号必须替换。

### 10.4 事务与回滚【沿用】

1. 多表一致性写操作放 Service 的 public 方法，主流注解为 `@Transactional(rollbackFor=Exception.class)`；简单单条写操作按既有实现处理，不为全部查询加事务。
2. 业务异常继承 RuntimeException，通常会触发回滚。未知数据库错误和写入失败应继续抛出；记录日志后吞异常会使事务误提交。
3. 同类 `this.method()` 不经过 Spring 代理。参考项目采用 `AopContext` 或从 SpringUtils 获取代理后调用，已有 `@EnableAspectJAutoProxy(exposeProxy=true)`；迁移时保留必要代理调用，不把注解留在失效的内部调用上。
4. 原 `InvestorServiceImpl` 某些方法明确设置 `noRollbackFor=ServiceException.class`。这是特定业务需要，不是项目所有业务异常均不回滚。毕业新方法默认不复制该例外，只有需求明确需要保留已完成步骤时逐项确认。
5. 原钱包流程把外部调用与部分本地事务分开，存在已提交后处理/补偿流程。若毕业功能没有外部资金系统，不复制整套交易流程；若确实有外部调用，不声称一个本地数据库事务能回滚外部副作用。
6. 定时任务/异步线程中的数据库操作，同样通过 Spring 代理触发事务；不要假定请求线程事务会自动跨线程传播。

### 10.5 Redis 与缓存复用【沿用】

- RedisConfig 创建 RedisTemplate，key/hash key 用 StringRedisSerializer，value/hash value 用 FastJson2JsonRedisSerializer。后者序列化写类名，反序列化启用 SupportAutoType；新工程使用自己的可信缓存空间，改包名后不复用旧缓存对象。
- RedisCache 封装对象、List、Set、Hash、TTL、计数和 SCAN 等操作；带 TTL 的方法要看明确 TimeUnit，不把秒和分钟混用。
- GoRedisCache 使用 StringRedisTemplate、JSON 与 EMPTY_CACHE 空值标记，提供 supplier 回源、分钟级 TTL 和按 key 的进程内 synchronized；部分空列表/集合占位使用 Lua，避免覆盖并发写入。它仍是同一个 Redis 技术体系，不是新部署的第二缓存平台。
- 业务缓存 key/TTL 参照 CacheConstants/CacheTimeConstants 集中定义；登录态、验证码、TOTP ticket 和业务缓存用途分开，禁止在业务变更时清空整个共享 Redis。
- 数据写入后根据原业务模式删除/刷新对应缓存；分页缓存键必须包含实际筛选/排序/页码维度，不能照搬 PageUtils.buildCacheKey 中原交易报表的默认排序。
- 不为所有 CRUD 强制加缓存，不引入额外缓存框架；只有业务确有必要时采用当前工具。

**源码依据：** [S11]、[S17]、[S18]、[S22]、[S25]、[S26]、[S31]。

## 11. Flyway 数据库版本管理规范

### 11.1 实际配置及执行机制【沿用】

admin 依赖 `org.flywaydb:flyway-core`，版本由 Boot 2.5.14 BOM 管理为 7.7.3；未声明独立 Flyway Maven 插件，也未发现自定义迁移执行器。启动由 Spring Boot 自动配置触发迁移。

毕业工程保留配置结构，只将连接信息替换为自己的库。下例是脱敏配置形状，不是可直接运行的真实环境配置：

```yaml
# application.yml
spring:
  profiles:
    active: dev,druid-dev
  flyway:
    enabled: true

# application-dev.yml 中相应 spring 节点下合并以下内容
spring:
  flyway:
    enabled: true
    url: <毕业项目独立数据库的JDBC地址>
    user: <毕业项目迁移账号>
    password: <毕业项目独立密码>
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: true
    placeholder-replacement: false
    locations:
      - classpath:db/draft
```

这是两个文件的示意，不能把两个 spring 根键原样复制到同一个 YAML 中。

| 项目 | 核验结果 | 开发规则 |
|---|---|---|
| 连接方式 | 显式 flyway.url/user/password，独立于动态数据源路由 | Flyway 与业务 master 指向毕业项目预期的同一 schema；账号需具备迁移需要的权限 |
| 目录 | admin/src/main/resources/db/draft | 名字虽叫 draft，实际会在启动执行，不能在这里放未准备执行的草稿 |
| 历史表 | 未显式改名；Flyway 7.7.3 默认 `flyway_schema_history` | 每个独立数据库有自己的历史；不导入参考历史 |
| baseline-version | 未显式设置；本地 JAR 确认为 `1` | 区分空库迁移与已有非空库 baseline |
| validate-on-migrate | true | 已执行脚本校验，不靠改旧文件消除差异 |
| out-of-order | true | 可执行迟到的较低版本脚本，仍须检查脚本前置关系 |
| placeholder-replacement | false | 原邮件 SQL 含 `${...}`；保留关闭，不误替换业务模板变量 |
| validate-migration-naming | 未设置；JAR 默认 false | 命名不合法的 SQL 可能不作为迁移执行，必须在交付检查中验证文件名 |
| clean-disabled | 未设置；JAR 默认 false | 不能声称原配置已经禁用 clean；本任务不调用 clean，开发规程禁止对持久业务库执行 clean |

### 11.2 空库与存量库必须分开理解【业务替换】

参考 `V1__baseline.sql` 只有注释，是给存量库接入 Flyway 的起点。目录中的后续脚本大量 ALTER/UPDATE/INSERT 已有表，**没有形成可直接初始化完整独立空库的基础建表集**。

- **毕业项目新空库：** 保持 Flyway 路径和自动迁移方式，但编写毕业项目自己的初始版本脚本，包含经确认的必要系统表与基础数据，再按需求增加业务表。可以使用新工程自己的 `V1__baseline.sql` 存放实际初始化内容，因为这是全新独立迁移历史；不修改参考工程的 V1。本阶段不提前设计所有毕业业务表。
- **已有非空库首次接入：** `baseline-on-migrate=true` 会将既有库纳入基线，低于或等于基线的迁移不执行。必须先确认库和现有结构，不能将它理解为会自动补齐缺失的基础表。
- **隔离要求：** 使用独立 schema、独立连接配置和自己的迁移历史；不能使用参考数据库测试初始化，也不复制原账号、用户、业务记录和环境密钥。

### 11.3 文件命名和版本号【沿用】

V1 注释给出的规范是：

```text
V<yyyyMMdd_HHmmss>__<说明或任务号>.sql
例如：V20261021_140000__add_example_index.sql
```

规则：大写 V；日期与时间用一个下划线；版本与说明之间用 **两个下划线**；同一迁移目录版本唯一；不沿用参考业务当前最后版本号作为毕业项目必须起点。新增版本描述准确表达改动。

对全部 34 个 SQL 使用参考 Flyway 7.7.3 文件名解析器检查，发现：

| 文件 | 实际解析结果 | 迁移标准处理 |
|---|---|---|
| `V20261016_170000_order_lifecycle_permissions.sql` | 缺少 `__`，解析为无效版本化迁移名称 | 不能当作已自动执行的合格范本；需原历史核实后才能决定如何补迁移，不直接在参考项目重命名 |
| `V20260715__1400__mail.sql` | 有效，但版本是 `20260715`；`1400__mail` 属于说明部分 | 已存在文件保持不动；毕业新增文件严格按 yyyyMMdd_HHmmss 规则 |

文件在目录中存在不代表在任何环境执行成功，只有相应环境的历史表可以证实。

### 11.4 新建/变更表、权限和数据【沿用】

1. 一次独立数据库变更新增一个清晰脚本；写明用途、影响表、前置条件、数据影响、幂等处理及必要的补偿说明。
2. 新增字段/索引可参考已有 information_schema 检查 + PREPARE/EXECUTE 的模式；不要把特定 MySQL 新版本语法未经验证直接用于未知服务端。
3. 原脚本有 INSERT ... ON DUPLICATE KEY UPDATE、NOT EXISTS 等写法。幂等并不代表不会覆盖错误对象；固定菜单 ID 和唯一键必须在毕业库中重新确认。
4. 迁移目标由连接决定，普通脚本不写原环境的 `USE` 或固定数据库名；跨库业务 SQL 不自动带入。
5. 一旦某脚本在共享/正式数据库执行，内容、名称、版本和校验和视为不可变。结构再变就新增脚本；不要删除历史再重跑。
6. out-of-order=true 不替你处理依赖。迟到脚本必须兼容当前结构，避免依赖尚未存在的列或已被删除的表。
7. 一个功能提交同时包含 Java、XML、SQL、权限菜单、需要的配置和对应验收说明，避免“代码先上、表以后手改”。
8. 原 SQL 中的回滚注释只是操作说明，不是 Flyway 已提供自动 undo。毕业数据库修复优先用经过确认的前向补偿迁移。

### 11.5 迁移失败处置【沿用现有校验机制的操作规程】

原仓库没有提供完整的自动 repair/undo 流水线，以下是保证其现有 Flyway 管理方式可执行的必要步骤，不代表原代码已经自动完成这些动作。

1. 保留失败日志、失败版本和错误 SQL；停止把该发布认定为成功，不通过关闭 Flyway 继续发布。
2. 先核对实际连接的 schema、用户权限、前置表/字段、字符集、索引和历史记录。区分连接错误、语法错误、命名跳过与 checksum 不一致。
3. 对 MySQL DDL 不假设自动完整回滚，核查是否已部分落地；恢复或补偿前先保全需要的数据。
4. checksum 不一致先恢复已经发布的原始脚本，后续修正另写新版本；不能用 repair 让被随意修改的旧脚本“合法化”。
5. 确需 repair 时，先人工确认真实 schema 与历史状态，再通过**同版本 Flyway**的受控操作处理元数据。仓库没有可直接声称已配置好的 `mvn flyway:repair` 命令，不自动给新工程加迁移插件。
6. repair 不会撤销 SQL 造成的表或数据变化；修正后重新启动验证迁移与应用功能，保留记录。
7. JAR 回退不等于数据库回退。`host-deploy-test.sh` 的旧 JAR 回退机制不能撤销已经执行的迁移，发布前需确认前后版本的结构兼容性。

### 11.6 配置缺失边界

仓库只提供 dev/druid-dev，没有 application-live.yml。application.yml 注释提到生产须补 Flyway 配置，或关闭维持手工执行；**本毕业项目要求所有受控结构变更使用 Flyway，因此不采用关闭 Flyway、长期手改数据库的分支**。后续若设置自己的运行 profile，必须提供同样完整的迁移位置与独立连接信息。

**源码依据：** [S02]、[S09]、[S29]、[S35]；文件名核验直接使用 flyway-core 7.7.3，未连接数据库。

## 12. 参数校验、日志与数据安全规范

### 12.1 参数校验【沿用】

- 使用现有 Validation starter、`javax.validation.constraints` 与 Hibernate Validator 注解；按 SupperEntity 的 Add/Update 等分组处理同一对象在不同操作的必填项。
- 已有 `@Xss` 自定义约束、BeanValidators 工具与 XssFilter，复用机制，不新增一套校验引擎。
- 注解解决字段基本约束，Service 解决跨字段逻辑、状态转换、记录存在性和用户归属。
- 校验 message 有“字段名 → 业务码”的约定，新增字段同时检查 GlobalExceptionHandler 的映射。不要以为注解写一个字段名就能自动得到专属错误码。
- 当前请求体字段校验与 GET 参数绑定错误的业务 code 不同，按第 7 节测试真实响应。
- 分页大小、日期起止、枚举范围参照 SignalInfoChangeRecordController 的显式检查；空字符串、负数、越界和非法日期不能依赖数据库报错替代。

### 12.2 日志与审计【沿用】

- 使用 SLF4J/`@Slf4j` 与 `logback-spring.xml`；配置有控制台、滚动普通日志和错误日志，按日期/大小滚动，maxHistory=90。
- 日志模式包含线程与 MDC `TRACE_ID`；LogInterceptor 记录 URI、耗时和 HTTP 状态。它清理 startTime ThreadLocal，但当前未看到 afterCompletion 清除 MDC 的代码，不宣称已完整清理所有上下文。
- `@Log` 的 title/businessType 描述实际动作，LogAspect 记录操作用户 ID/用户名、URL、方法、状态、异常等，异步保存；请求和响应内容最大截取 2000 字符。
- 密码、token、TOTP 密钥、数据库密码、第三方凭证不打印、不返回普通业务 VO。`SysUser.password` 的只写序列化配置按原样保留。
- LogAspect 的 maskSensitive 只对顶层 JSONObject 中含 pwd/password/secret 的键打码；不递归覆盖嵌套对象、数组、所有 GET 参数或所有响应。涉及凭证的接口应利用已有 `@Log` 请求/响应保存开关控制记录，不能误认为全局自动脱敏已经覆盖所有数据。
- 日志文件目录改为毕业项目自己的目录。原 XML 的 log.path 是本地 property 固定值，而部署脚本又传 `-Dlog.path`；不要未经运行确认就认定系统参数一定覆盖该本地配置。

### 12.3 配置、文件与接口边界【业务替换】

- 参考 application-dev.yml、application-druid-dev.yml 含环境连接和敏感值。新工程保留键名/层级，所有值重新配置，文档不复制这些实际值。
- 保持现有 YAML/profile/启动参数组织方式；可使用 Spring 已有外部化配置能力传入毕业项目自己的值，不引入新的密钥管理平台作为毕业项目前置要求。
- JWT/TOTP 密钥、数据库/Redis 地址与密码、OSS 账户/桶、CRM/MT 端点、可信代理 IP、文件路径和应用名均为替换项。
- 当前 CORS 是 allowCredentials=true + allowedOriginPattern=`*`。实际毕业前端域名确定后，按同一个 ResourcesConfig 配置所需来源；不额外引入网关。
- XSS 过滤范围当前仅 `/system/*,/monitor/*,/tool/*`，排除 `/system/notice`，并不是所有 biz/client 接口都已过滤。涉及用户输入输出的功能需按原机制检查覆盖，不能把存在过滤类视为全覆盖证明。
- 上传大小当前单文件 15MB、总请求 20MB；OSS 图片/文件有扩展名配置。参考普通文件白名单较宽，包含可执行/压缩/HTML 类文件，毕业功能只开放其明确需要的类型，仍使用原上传实现。
- Druid 控制台当前关闭；Swagger 当前关闭；不为便利默认公开管理与诊断功能。
- IpUtils/TrustedProxyConfig 只对已信任直连代理处理转发 IP；不要把原服务器 IP 白名单原样复制到新工程。
- BlackIpForbiddenFilter 的实际黑名单检查已注释，目前主要是放行；不要将它计入已有效启用的黑名单能力。

**源码依据：** [S09]、[S12]、[S28]、[S32]、[S33]、[S34]、[S36]。

## 13. 必要的并发安全要求

### 13.1 已有工具及其能力边界【沿用】

| 方式 | 参考有效实例 | 使用边界 |
|---|---|---|
| 数据库行锁 + 事务 | 钱包/申请/信号 Mapper 的 `SELECT ... FOR UPDATE`，由事务 Service 调用 | 只用于确需互斥的记录；锁内检查状态并更新，不把查询锁放在无事务调用中 |
| 唯一键与重复处理 | 去重迁移、InvestorServiceImpl 的 DuplicateKeyException 处理 | 数据库约束是最终保障；先查后插本身不保证并发唯一 |
| Redis 原子操作 | TOTP 时间片 `setIfAbsent`，Redis 计数/Lua | 复用现有 Redis 客户端，不引入 Redisson/分布式锁框架 |
| 本进程互斥/防重入 | synchronized、ConcurrentHashMap、AtomicBoolean.compareAndSet | 只保护同 JVM，不声称具备多实例互斥能力 |
| `@RepeatSubmit` | SameUrlDataInterceptor 对 URL、token header、请求内容与时间进行比较 | GET 再 SET 不是原子互斥，不能作为资金/库存等一致性的唯一保证 |
| `@RateLimiter` | Lua 计数并设置过期 | 控制频率；超限异常构造需按第 7 节处理旧调用问题 |

`BaseController.checkAudittingById` 同样是 Redis 先读后写，虽然设置 3 分钟 TTL，但不是可靠的原子锁。毕业业务如果只是普通 CRUD，不为“预防未来并发”加锁；若确有重复提交/状态竞争，就选用参考项目已经存在的数据库约束、事务行锁或 Redis 原子操作。

### 13.2 异步与任务【沿用】

ThreadPoolConfig 已有 taskExecutor：核心 8、最大 16、队列 100、空闲 300 秒、CallerRunsPolicy；scheduledExecutorService 为 8 线程。需要异步时复用已有 Spring 线程池和 `@Async`/AsyncManager，不自行无界 new Thread，也不引入 MQ。

业务定时任务使用 `@Scheduled`，ScheduleTask 已启用调度。只迁移毕业业务确需的任务；不复制交易任务，不恢复已移除的通用 Quartz/代码生成模块。

涉及异步数据库写入、事务提交后动作和任务重入，参照原有代理调用及 SignalAsyncProcessor 的事务同步处理；根据实际需求验证失败传播，不把进程内任务描述成可靠消息投递系统。

### 13.3 并发验收的最小范围

对唯一新增、重复操作、审批状态转换等具体业务，至少验证两个同时请求不会重复成功或覆盖非法状态；事务失败不会留下半条业务；锁与 ThreadLocal 在失败路径释放。没有这些业务风险的普通只读接口不增加无关锁测试或基础设施。

**源码依据：** [S10]、[S26]、[S30]、[S31]。

## 14. 测试、构建与代码验收要求

### 14.1 参考项目已经具备什么

- admin 声明 `spring-boot-starter-test`，测试 scope；包含 JUnit Jupiter、Mockito、AssertJ 等现成能力。
- 在当前源码范围未发现 `src/test` 测试源文件；`target/test-classes` 目录的存在不能证明有有效测试，也不能把 `web/controller/tool/TestController.java` 视为自动化测试。
- Jenkinsfile/Jenkinsfile_dev 的打包命令是 `mvn clean package -DskipTests`。这能说明现有发布路径跳过测试，不能作为“已通过测试”的证据。
- 仓库没有显式固定 Surefire 版本或统一覆盖率门槛。不得捏造已有测试制度，也不引入新的测试平台。

### 14.2 毕业项目验收规则【在现有工具上执行】

1. 编译时使用确认的 JDK 8，检查 Maven/JDK 版本输出；源码必须能由根 reactor 构建。
2. 保留原打包路径与可执行 JAR 形式。测试通过以后再使用原 `-DskipTests` 发布命令，不用它代替验证。
3. 对新增业务的重要规则使用现有 JUnit/Mockito；接口可使用已有 Spring Test/MockMvc。无需为此新增 H2、Testcontainers、完整外部测试平台或复制原生产库。
4. 数据库集成验证在毕业项目自己的数据库中进行，MySQL 版本以确认后的参考环境版本为准；Redis 使用独立配置/命名空间，不连接原业务数据。
5. 检查 Maven 实际发现和执行了测试，查看测试数量与 Surefire 报告。若所确认 Maven 默认 Surefire 不支持所选 JUnit 引擎，先提供未执行/兼容错误，再确认最小插件配置；本次不擅自指定另一个新版本。
6. 验证标准功能链：合法/非法输入、未认证、无权限、菜单与接口一致、数据归属、分页 total、排序、重复写入、事务回滚、Flyway 从空库启动及升级。
7. 确认 Java 数据模型和实际 schema 一致，响应里无不应暴露的密码/密钥；日志的字段范围符合第 12 节。
8. 构建或迁移失败时先定位实际错误，不直接升级 Boot、Security、MP、Flyway 或替换组件。

### 14.3 后续工程中的命令形状

以下命令只在用户确认标准、建立独立毕业工程后运行，本次没有对参考项目执行这些会编译/启动的命令：

```text
mvn -version
mvn clean test
mvn clean package -DskipTests
java -jar <毕业入口模块>/target/<毕业入口模块>.jar --spring.profiles.active=dev,druid-dev
```

profile 先沿用现有组织；环境值在自己的配置中准备。不要求为了本任务新建多集群、多服务或新的发布平台。

### 14.4 本次已完成的实际验证

| 核验 | 结果 | 限制 |
|---|---|---|
| 参考目录、根/子 POM、配置、核心源码读取 | 完成 | 不等于验证全部线上功能 |
| POM 副本 `mvn -o validate`、有效模型提取 | 全部 7 个 reactor 项目成功（根 + 6 模块） | 不包含 Java 编译 |
| POM 副本全部模块 dependency:tree | 成功，完整入口树见附录 B | 不是线上 JAR 检查 |
| Flyway 7.7.3 配置与文件名解析 | 34 个 SQL，33 个有效名称，1 个无效名称 | 未执行任何 SQL |
| 参考文件前后 SHA-256 对比 | 819 个已盘点文件均未改变；Git 工作区仍无变更 | 不包含 .git/target 等元数据与构建目录 |
| 原项目编译、单元测试、启动、SQL 迁移 | 未执行 | 避免改参考产物或连接真实环境；不能报告通过 |

依赖树运行时 Maven 3.10.0 对 Druid 1.2.11 的上游 POM 报 `tools/jconsole systemPath` 模型警告，但最终树生成成功。该工具环境警告不等于业务应用编译失败，更不构成自动升级 Druid 的理由；完整构建时如复现，再按真实构建环境处理。

**源码依据：** [S01]–[S07]、[S35]，附录 B。

## 15. 新功能的标准开发流程

本节在标准确认后使用；当前不据此生成任何业务实现。

1. **确认需求与权限。** 明确功能输入/输出、用户角色、记录归属、状态转换、是否需要文件/任务/外部接口；不预设额外中间件。
2. **选取参考链路。** 普通列表优先 [S22]；用户角色管理参照 [S28]/[S23]；带事务的多表写入参照 [S31]。确认选定模式已经有实际调用。
3. **确定模块与路径。** Controller 放 admin，业务放 biz，通用系统能力用 system，公共无业务组件用 common/framework。沿用已有层次。
4. **设计最小数据库变更。** 只设计本功能需要的表/字段/约束，编写独立 Flyway 脚本；空库先有毕业项目自己的基础初始化版本。
5. **定义对象与校验。** 选择 BaseEntity/SupperEntity/普通 DTO/VO，明确类型、单位、范围、分组、错误码和禁止客户端修改的字段。
6. **实现 Mapper/XML。** namespace/id/@Param、结果映射、归属条件、索引与排序一致；检查复杂查询的 count 和 JOIN 粒度。
7. **实现 Service。** 校验记录归属和状态；必要时事务/行锁；继续传播真实错误；缓存只在确有需要时复用现有 Redis 工具。
8. **实现 Controller。** 权限注解、校验、分页路径、统一响应和审计日志齐全；同一查询只启用一种分页。
9. **同步菜单权限。** Flyway SQL 中添加对应菜单/按钮权限，前端标识与注解一致；使用现有角色授权逻辑，不硬编码原角色关系。
10. **验证。** 测试业务规则、权限失败、数据归属、分页总数、事务与迁移；必要并发场景使用现有工具验证。
11. **构建与复核版本。** 核对依赖树；不得出现未经确认的升级/新增依赖；检查 JAR 启动所需配置均为毕业项目独立值。
12. **提交并说明。** 同次交付代码、SQL、配置示例和验证结果；明确尚未验证的项目。未经确认不进入下一阶段的大范围业务生成。

**来源：** 本流程按前述已有模块、链路、迁移与构建方式串联，不新增架构层。对应依据 [S01]–[S36]。

## 16. 开发完成后的代码自检清单

### 16.1 架构与依赖

- [ ] 独立工程、独立数据库、独立凭证，没有引用参考工程的运行目录或真实数据。
- [ ] 保留同类 6 模块组织和已确认的模块依赖；未恢复 quartz/generator。
- [ ] 根 BOM、父子版本、Boot 插件、compiler 插件与确认清单一致。
- [ ] Lombok/HttpClient/Swagger 等模块间解析差异经过核对，没有凭单个 POM 误判全工程版本。
- [ ] 未新增未经业务证明的中间件、缓存、认证、迁移或数据访问框架。
- [ ] 包名改变已同步 Mapper 扫描、XML、配置和硬编码路径。

### 16.2 接口、异常、权限

- [ ] Controller、Service、Mapper 各层职责与参考模式一致。
- [ ] 普通返回为 AjaxResult；分页为 TableDataInfo，字段与原契约一致。
- [ ] ServiceException 的 String 参数按错误码使用；业务错误码有明确文案。
- [ ] 参数校验分组和错误映射已覆盖；没有把字段校验当作归属授权。
- [ ] 接口权限、菜单 perms、前端权限标识一致；拒绝路径实际测试。
- [ ] 未把后台 authority=admin 与超级管理员混同；角色变更能更新在线权限。
- [ ] 详情、更新、删除、导出校验当前用户权限与数据归属，没有依赖空切面。
- [ ] 已检查 HTTP status 和 JSON code 的实际差异。

### 16.3 分页、SQL、事务与并发

- [ ] 分页参数为 current/pageSize，默认值和上限明确；不混用两种分页拦截路径。
- [ ] 分页紧贴目标 SELECT，异常/提前返回清理分页状态。
- [ ] total 来自分页元数据，不是当前页 list.size；空页和汇总语义正确。
- [ ] SQL 参数绑定、动态条件、结果映射与 DTO/VO 一致，无请求输入直拼 SQL。
- [ ] 事务通过 Spring 代理生效，未吞掉应回滚的异常。
- [ ] noRollbackFor 仅用于确有业务依据的方法；本地事务不被误当作外部调用回滚保障。
- [ ] 有一致性要求的并发写入使用既有数据库约束/行锁/原子操作；未把 GET+SET 当作锁。

### 16.4 Flyway、配置、日志与验收

- [ ] 所有受版本管理的数据库变更都有独立 Flyway SQL，与代码同提交。
- [ ] V1 能初始化毕业项目空库；没有复制空占位后假定表已存在。
- [ ] 文件名为 V日期_时间__说明.sql，版本不重复；db/draft 中没有未准备执行的草稿。
- [ ] 已执行的脚本未修改/删除/重命名；迁移连接与业务 master schema 一致。
- [ ] 迁移失败时记录并处理实际结构/历史状态，未关闭校验、清空历史或绕过 Flyway。
- [ ] 未复制参考数据库/Redis/OSS/CRM/MT 地址、密码、密钥、代理名单及真实业务数据。
- [ ] 密码/密钥/token 无普通响应和日志泄露；已考虑现有脱敏实现的覆盖边界。
- [ ] 日志和上传路径属于毕业项目；Swagger/Druid/公开路由配置符合实际需求。
- [ ] 构建成功；测试确实执行且关键链路通过；未执行项已如实列明。
- [ ] 发布前考虑 JAR 与迁移兼容性，不将 JAR 回退等同于数据库回退。

## 17. 待核实事项与不能直接照搬的具体部分

这些条目不阻止交付本标准，但在相关实现/部署开始前必须补齐证据或确认最小适配。本次未修改原工程来“修复”这些内容。

| 编号 | 文件或信息位置 | 已知事实与缺口 | 对毕业项目的影响／处理 |
|---|---|---|---|
| U01 | 根 POM、Dockerfile、两个 Jenkinsfile、部署脚本 | Java 8 已确认；容器为 8u362；其他环境厂商/补丁及 Maven 发行版未固定 | 获取原构建环境 `mvn -version/java -version` 记录；不以本机版本替代 |
| U02 | application-druid-dev.yml / application-dev.yml | MySQL/Redis 服务端版本未给出 | 后续用经授权的版本信息确认；驱动版本保持已确认值 |
| U03 | 根 POM 的 jar-plugin 属性、各子 POM | jar 属性未引用；Surefire 等未锁定 | 构建前核实原 Maven 生命周期默认插件；不捏造统一版本 |
| U04 | application.yml；缺失 application-live.yml | 生产 profile 的 Flyway/运行配置不可从当前仓库确认 | 毕业项目只沿用可见配置组织，自行填独立值，不复制“未知生产配置” |
| U05 | db/draft/V1__baseline.sql | 空占位，没有完整空库建表基线 | 新工程编写自己的基础初始化迁移；是独立建库必要差异 |
| U06 | db/draft 中 34 个 SQL；真实 flyway_schema_history 不在文件中 | 1 个文件名无效，1 个命名与日期时间惯例不同；真实执行记录未知 | 不宣称全部已执行；新脚本遵守双下划线规则，旧库问题先查历史 |
| U07 | DataScopeAspect/DataPowerAspect/FieldAspect | 空实现或没有接入实际范围 | 采用已有效的 Service/SQL 归属校验；额外范围需求明确后才补相应最小实现 |
| U08 | ClientTokenService/ClientUserLoadService/CrmApiClient、MyBatisConfig | CRM 账户、跨库和 MT 业务耦合 | 保留 Security/JWT/Redis 机制，将账户来源接入毕业业务；不强制引入外部 CRM/交易系统 |
| U09 | ServiceException 与 RateLimiterAspect 等单 String 调用 | 字符串构造实际解析数字码，旧文本调用可能产生异常 | 移植时局部改为现有错误码构造，不引入新异常框架 |
| U10 | SysRoleMapper.xml 的 selectRolePermissionByUserId | SQL 使用 `ur.user_id`，共享 FROM 片段实际是 sys_user u，别名不匹配 | 该路径不能视为已验证；若毕业功能使用它，应依当前 role_id 关系作最小 SQL 修正并测试 |
| U11 | SysPermissionService.getRolePermission、UserDetailsServiceImpl、SysUser/SysRole | 管理员判断与角色集合装载存在不同旧路径 | 使用当前实际 hasPermi 链；需要 hasRole 时验证角色装载，不凭类名推断 |
| U12 | GlobalExceptionHandler、AuthenticationEntryPointImpl、ServletUtils | HTTP 状态和业务码不同，过滤器/MVC 拒绝出口不完全一致 | 保留契约并测试；若前端明确需调整，确认局部改动 |
| U13 | LogAspect、LogInterceptor、logback-spring.xml | 脱敏非递归；MDC 清理不完整；日志路径覆盖需要验证 | 使用现有开关保护敏感日志，按需求局部适配，不另建日志平台 |
| U14 | admin/pom.xml、源码目录、Jenkinsfile | 有测试依赖，但未发现测试源码；发布跳过测试 | 不能继承“全部已自动测试”的假设；使用已有测试栈补毕业业务的必要验证 |
| U15 | Druid 1.2.11 上游 POM、本次 Maven 3.10.0 输出 | 有 tools/jconsole systemPath 模型警告，依赖树仍成功 | 原环境完整构建兼容性待实际验证；不据此擅自换版本 |
| U16 | 全部 POM 与实际部署 JAR | 已核验源码解析结果，未获取线上 JAR/部署命令全量证据 | 如需声明与线上运行字节级一致，需核对实际 JAR 的 BOOT-INF/lib；当前不作此声明 |

业务角色名称、是否面向普通用户、是否注册、文件上传需求、后台 TOTP 是否按当前开启模式使用、业务表和页面范围，均待毕业需求明确后按本标准接入。当前标准默认保留参考后台认证强度，不预先关闭 TOTP。

**阶段交付约束：文档等待用户审阅确认；确认前不创建毕业项目业务工程、不生成完整业务表、不开始实现业务功能、不调整技术版本。**

<!-- GENERATED_AUDIT_APPENDIX -->
## 附录 A. 可定位的源码与配置依据
以下链接均指向只读分析的参考文件；文件内容可能包含敏感配置，请勿复制实际凭证到毕业项目。

### S01

根工程版本、BOM、聚合与插件

- [pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/pom.xml>)

### S02

入口模块依赖、Flyway 与打包插件

- [ac-fts-admin/pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/pom.xml>)

### S03

公共模块直接依赖

- [ac-fts-common/pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/pom.xml>)

### S04

框架模块依赖

- [ac-fts-framework/pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/pom.xml>)

### S05

系统模块依赖

- [ac-fts-system/pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-system/pom.xml>)

### S06

业务模块依赖

- [ac-fts-biz/pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/pom.xml>)

### S07

OSS 模块依赖

- [ac-fts-aliyun-oss/pom.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-aliyun-oss/pom.xml>)

### S08

应用启动与扫描

- [ac-fts-admin/src/main/java/com/saas/FtsAdminApplication.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/FtsAdminApplication.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/config/ApplicationConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/ApplicationConfig.java>)

### S09

运行配置（含敏感值，仅定位，不复制值）

- [ac-fts-admin/src/main/resources/application.yml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/application.yml>)
- [ac-fts-admin/src/main/resources/application-dev.yml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/application-dev.yml>)
- [ac-fts-admin/src/main/resources/application-druid-dev.yml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/application-druid-dev.yml>)

### S10

控制器、统一响应与实体基类

- [ac-fts-common/src/main/java/com/saas/common/core/controller/BaseController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/controller/BaseController.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/AjaxResult.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/AjaxResult.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/R.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/R.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/BaseEntity.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/BaseEntity.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/SupperEntity.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/SupperEntity.java>)

### S11

Mapper/Service 基类

- [ac-fts-common/src/main/java/com/saas/common/core/dao/BaseDao.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/dao/BaseDao.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/service/BaseService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/service/BaseService.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/service/BaseServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/service/BaseServiceImpl.java>)

### S12

全局异常与校验映射

- [ac-fts-framework/src/main/java/com/saas/framework/web/exception/GlobalExceptionHandler.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/exception/GlobalExceptionHandler.java>)

### S13

异常类型与构造签名

- [ac-fts-common/src/main/java/com/saas/common/exception/ServiceException.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/exception/ServiceException.java>)
- [ac-fts-common/src/main/java/com/saas/common/exception/GlobalException.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/exception/GlobalException.java>)
- [ac-fts-common/src/main/java/com/saas/common/exception/base/BaseException.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/exception/base/BaseException.java>)

### S14

业务错误码与响应码

- [ac-fts-common/src/main/java/com/saas/common/constant/ErrorCodeConstants.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/constant/ErrorCodeConstants.java>)
- [ac-fts-common/src/main/java/com/saas/common/constant/HttpStatus.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/constant/HttpStatus.java>)

### S15

认证异常出口与 HTTP 状态设置

- [ac-fts-framework/src/main/java/com/saas/framework/security/handle/AuthenticationEntryPointImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/security/handle/AuthenticationEntryPointImpl.java>)
- [ac-fts-common/src/main/java/com/saas/common/utils/ServletUtils.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/utils/ServletUtils.java>)

### S16

分页参数、工具、返回对象与排序检查

- [ac-fts-common/src/main/java/com/saas/common/core/page/TableSupport.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/page/TableSupport.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/page/PageDomain.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/page/PageDomain.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/page/TableDataInfo.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/page/TableDataInfo.java>)
- [ac-fts-common/src/main/java/com/saas/common/utils/PageUtils.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/utils/PageUtils.java>)
- [ac-fts-common/src/main/java/com/saas/common/utils/sql/SqlUtil.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/utils/sql/SqlUtil.java>)

### S17

MyBatis 工厂、MP 分页与业务拦截器

- [ac-fts-framework/src/main/java/com/saas/framework/config/MyBatisConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/MyBatisConfig.java>)

### S18

MyBatis 全局配置

- [ac-fts-admin/src/main/resources/mybatis/mybatis-config.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/mybatis/mybatis-config.xml>)

### S19

Security 链、JWT 过滤器和匿名路径

- [ac-fts-framework/src/main/java/com/saas/framework/config/SecurityConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/SecurityConfig.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/security/filter/AdminJwtAuthenticationTokenFilter.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/security/filter/AdminJwtAuthenticationTokenFilter.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/security/filter/ClientJwtAuthenticationTokenFilter.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/security/filter/ClientJwtAuthenticationTokenFilter.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/config/properties/PermitAllUrlProperties.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/properties/PermitAllUrlProperties.java>)

### S20

认证服务、token、TOTP 与身份装载

- [ac-fts-framework/src/main/java/com/saas/framework/web/service/TokenService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/TokenService.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/SysLoginService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/SysLoginService.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/TotpService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/TotpService.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/UserDetailsServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/UserDetailsServiceImpl.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/ClientTokenService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/ClientTokenService.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/ClientUserLoadService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/ClientUserLoadService.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/CrmApiClient.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/CrmApiClient.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/model/LoginUser.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/model/LoginUser.java>)
- [ac-fts-common/src/main/java/com/saas/common/constant/CacheConstants.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/constant/CacheConstants.java>)

### S21

权限表达式、角色与菜单关系

- [ac-fts-framework/src/main/java/com/saas/framework/web/service/PermissionService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/PermissionService.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/web/service/SysPermissionService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/web/service/SysPermissionService.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/entity/SysUser.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/entity/SysUser.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/domain/entity/SysRole.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/domain/entity/SysRole.java>)
- [ac-fts-system/src/main/resources/mapper/system/SysUserMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-system/src/main/resources/mapper/system/SysUserMapper.xml>)
- [ac-fts-system/src/main/resources/mapper/system/SysMenuMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-system/src/main/resources/mapper/system/SysMenuMapper.xml>)

### S22

代表性 DTO/VO 列表完整链路

- [ac-fts-admin/src/main/java/com/saas/web/controller/admin/signal/SignalInfoChangeRecordController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/controller/admin/signal/SignalInfoChangeRecordController.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/service/signal/ISignalInfoChangeRecordService.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/signal/ISignalInfoChangeRecordService.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/service/signal/impl/SignalInfoChangeRecordServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/signal/impl/SignalInfoChangeRecordServiceImpl.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/mapper/signal/SignalInfoChangeRecordMapper.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/mapper/signal/SignalInfoChangeRecordMapper.java>)
- [ac-fts-biz/src/main/resources/mapper/signal/SignalInfoChangeRecordMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/resources/mapper/signal/SignalInfoChangeRecordMapper.xml>)
- [ac-fts-biz/src/main/java/com/saas/biz/dto/SignalInfoChangeRecordDTO.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/dto/SignalInfoChangeRecordDTO.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/vo/signal/SignalInfoChangeRecordVO.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/vo/signal/SignalInfoChangeRecordVO.java>)

### S23

MP 分页及角色旧 SQL

- [ac-fts-admin/src/main/java/com/saas/web/controller/system/SysRoleController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/controller/system/SysRoleController.java>)
- [ac-fts-system/src/main/java/com/saas/system/service/impl/SysRoleServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-system/src/main/java/com/saas/system/service/impl/SysRoleServiceImpl.java>)
- [ac-fts-system/src/main/java/com/saas/system/mapper/SysRoleMapper.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-system/src/main/java/com/saas/system/mapper/SysRoleMapper.java>)
- [ac-fts-system/src/main/resources/mapper/system/SysRoleMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-system/src/main/resources/mapper/system/SysRoleMapper.xml>)

### S24

分页元数据和独立汇总

- [ac-fts-biz/src/main/java/com/saas/biz/service/impl/report/ISignalTradeDataReportServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/impl/report/ISignalTradeDataReportServiceImpl.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/service/user/impl/UserServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/user/impl/UserServiceImpl.java>)

### S25

数据源与动态库名

- [ac-fts-framework/src/main/java/com/saas/framework/config/DruidConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/DruidConfig.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/aspectj/DataSourceAspect.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/aspectj/DataSourceAspect.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/datasource/DynamicDataSource.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/datasource/DynamicDataSource.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/datasource/DynamicDataSourceContextHolder.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/datasource/DynamicDataSourceContextHolder.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/service/mt/MtServerDbResolver.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/mt/MtServerDbResolver.java>)

### S26

Redis 序列化、缓存与 TTL

- [ac-fts-framework/src/main/java/com/saas/framework/config/RedisConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/RedisConfig.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/config/FastJson2JsonRedisSerializer.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/FastJson2JsonRedisSerializer.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/redis/RedisCache.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/redis/RedisCache.java>)
- [ac-fts-common/src/main/java/com/saas/common/core/redis/GoRedisCache.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/core/redis/GoRedisCache.java>)
- [ac-fts-common/src/main/java/com/saas/common/constant/CacheConstants.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/constant/CacheConstants.java>)
- [ac-fts-common/src/main/java/com/saas/common/constant/CacheTimeConstants.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/constant/CacheTimeConstants.java>)

### S27

数据范围和字段处理的实际状态

- [ac-fts-framework/src/main/java/com/saas/framework/aspectj/DataScopeAspect.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/aspectj/DataScopeAspect.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/aspectj/DataPowerAspect.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/aspectj/DataPowerAspect.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/aspectj/FieldAspect.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/aspectj/FieldAspect.java>)

### S28

后台登录、用户与角色 Controller

- [ac-fts-admin/src/main/java/com/saas/web/controller/system/SysLoginController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/controller/system/SysLoginController.java>)
- [ac-fts-admin/src/main/java/com/saas/web/controller/system/SysUserController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/controller/system/SysUserController.java>)
- [ac-fts-admin/src/main/java/com/saas/web/controller/system/SysRoleController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/controller/system/SysRoleController.java>)

### S29

Flyway 基线、命名和增量脚本

- [ac-fts-admin/src/main/resources/db/draft/V1__baseline.sql](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/db/draft/V1__baseline.sql>)
- [ac-fts-admin/src/main/resources/db/draft/V20260715__1400__mail.sql](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/db/draft/V20260715__1400__mail.sql>)
- [ac-fts-admin/src/main/resources/db/draft/V20261016_170000_order_lifecycle_permissions.sql](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/db/draft/V20261016_170000_order_lifecycle_permissions.sql>)
- [ac-fts-admin/src/main/resources/db/draft/V20261014_100500__sys_user_add_totp_secret.sql](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/db/draft/V20261014_100500__sys_user_add_totp_secret.sql>)
- [ac-fts-admin/src/main/resources/db/draft/V20261006_100000__task_mt_group_symbol_dedupe_and_unique_key.sql](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/db/draft/V20261006_100000__task_mt_group_symbol_dedupe_and_unique_key.sql>)
- [ac-fts-admin/src/main/resources/db/draft/V20261021_120000__sys_menu_add_mail_send_log.sql](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/db/draft/V20261021_120000__sys_menu_add_mail_send_log.sql>)

### S30

防重复、限流和线程池

- [ac-fts-framework/src/main/java/com/saas/framework/interceptor/RepeatSubmitInterceptor.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/interceptor/RepeatSubmitInterceptor.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/interceptor/impl/SameUrlDataInterceptor.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/interceptor/impl/SameUrlDataInterceptor.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/aspectj/RateLimiterAspect.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/aspectj/RateLimiterAspect.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/config/ThreadPoolConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/ThreadPoolConfig.java>)
- [ac-fts-admin/src/main/java/com/saas/web/task/ScheduleTask.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/task/ScheduleTask.java>)

### S31

归属校验、事务、锁与异步实例

- [ac-fts-biz/src/main/java/com/saas/biz/service/wallet/impl/ApplyFtsMtServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/wallet/impl/ApplyFtsMtServiceImpl.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/service/wallet/impl/FtsWalletServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/wallet/impl/FtsWalletServiceImpl.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/service/investor/impl/InvestorServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/service/investor/impl/InvestorServiceImpl.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/processor/SignalAsyncProcessor.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/processor/SignalAsyncProcessor.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/processor/CommissionPayoutProcessor.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/processor/CommissionPayoutProcessor.java>)
- [ac-fts-biz/src/main/java/com/saas/biz/task/RefreshMTGroupAndSymbolsTask.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/java/com/saas/biz/task/RefreshMTGroupAndSymbolsTask.java>)
- [ac-fts-biz/src/main/resources/mapper/wallet/FtsWalletMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/resources/mapper/wallet/FtsWalletMapper.xml>)
- [ac-fts-biz/src/main/resources/mapper/wallet/ApplyFtsMtMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/resources/mapper/wallet/ApplyFtsMtMapper.xml>)
- [ac-fts-biz/src/main/resources/mapper/signal/SignalInfoMapper.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-biz/src/main/resources/mapper/signal/SignalInfoMapper.xml>)

### S32

操作审计与日志

- [ac-fts-common/src/main/java/com/saas/common/annotation/Log.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/annotation/Log.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/aspectj/LogAspect.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/aspectj/LogAspect.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/interceptor/impl/LogInterceptor.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/interceptor/impl/LogInterceptor.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/manager/AsyncManager.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/manager/AsyncManager.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/manager/factory/AsyncFactory.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/manager/factory/AsyncFactory.java>)
- [ac-fts-admin/src/main/resources/logback-spring.xml](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/resources/logback-spring.xml>)

### S33

过滤、校验、CORS 与代理

- [ac-fts-framework/src/main/java/com/saas/framework/config/FilterConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/FilterConfig.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/config/ResourcesConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/ResourcesConfig.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/config/TrustedProxyConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/config/TrustedProxyConfig.java>)
- [ac-fts-framework/src/main/java/com/saas/framework/security/filter/BlackIpForbiddenFilter.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-framework/src/main/java/com/saas/framework/security/filter/BlackIpForbiddenFilter.java>)
- [ac-fts-common/src/main/java/com/saas/common/utils/ip/IpUtils.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/utils/ip/IpUtils.java>)
- [ac-fts-common/src/main/java/com/saas/common/utils/bean/BeanValidators.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/utils/bean/BeanValidators.java>)
- [ac-fts-common/src/main/java/com/saas/common/xss/XssValidator.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/xss/XssValidator.java>)

### S34

API 文档

- [ac-fts-admin/src/main/java/com/saas/web/core/config/SwaggerConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-admin/src/main/java/com/saas/web/core/config/SwaggerConfig.java>)

### S35

部署入口和构建命令

- [Dockerfile](<C:/Users/eggma/Desktop/project/fts_admin_ac/Dockerfile>)
- [Jenkinsfile](<C:/Users/eggma/Desktop/project/fts_admin_ac/Jenkinsfile>)
- [Jenkinsfile_dev](<C:/Users/eggma/Desktop/project/fts_admin_ac/Jenkinsfile_dev>)
- [deploy/deploy.yml](<C:/Users/eggma/Desktop/project/fts_admin_ac/deploy/deploy.yml>)
- [deploy/host-deploy-test.sh](<C:/Users/eggma/Desktop/project/fts_admin_ac/deploy/host-deploy-test.sh>)

### S36

OSS 配置、上传和文件校验

- [ac-fts-aliyun-oss/src/main/java/com/saas/oss/config/OssConfig.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-aliyun-oss/src/main/java/com/saas/oss/config/OssConfig.java>)
- [ac-fts-aliyun-oss/src/main/java/com/saas/oss/controller/OssUploadController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-aliyun-oss/src/main/java/com/saas/oss/controller/OssUploadController.java>)
- [ac-fts-aliyun-oss/src/main/java/com/saas/oss/controller/LogOssUploadController.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-aliyun-oss/src/main/java/com/saas/oss/controller/LogOssUploadController.java>)
- [ac-fts-aliyun-oss/src/main/java/com/saas/oss/service/impl/AliyunOssServiceImpl.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-aliyun-oss/src/main/java/com/saas/oss/service/impl/AliyunOssServiceImpl.java>)
- [ac-fts-common/src/main/java/com/saas/common/utils/file/FileUploadUtils.java](<C:/Users/eggma/Desktop/project/fts_admin_ac/ac-fts-common/src/main/java/com/saas/common/utils/file/FileUploadUtils.java>)

### 本地版本证据

- [Spring Boot 2.5.14 BOM](<C:/Users/eggma/.m2/repository/org/springframework/boot/spring-boot-dependencies/2.5.14/spring-boot-dependencies-2.5.14.pom>)
- 本地 `org/mybatis/spring/boot/mybatis-spring-boot/2.2.2/` POM：MyBatis 与 MyBatis-Spring 上游版本。
- 本地 `com/github/pagehelper/pagehelper-spring-boot/1.4.1/` POM 与 autoconfigure JAR：PageHelper core 及插件注册。
- 本地 `org/flywaydb/flyway-core/7.7.3/` JAR：默认配置和迁移名称解析；Boot 2.5.14 的 FlywayProperties 同时核对。
- 上述后三项均相对于 `C:/Users/eggma/.m2/repository/`。本次解析使用其工作区副本，没有修改这些依赖版本。

## 附录 B. 机器提取的版本证据

### B.1 全部子模块直接依赖声明与有效版本
此表覆盖全部子 POM 的直接依赖，包含内部模块；不是要求每个业务都使用这些能力。有效版本是当前模块直接依赖的有效模型值，exclusions 包含父级管理继承项；下游仲裁差异见第 2.4 节。

| 模块 | Maven 坐标 | 原声明版本 | 本模块有效版本 | scope / optional | exclusions |
|---|---|---|---|---|---|
| ac-fts-admin | `org.springframework.boot:spring-boot-devtools` | `由父级/BOM管理` | `2.5.14` | compile / optional=true | — |
| ac-fts-admin | `io.springfox:springfox-boot-starter` | `由父级/BOM管理` | `3.0.0` | compile | io.swagger:swagger-models |
| ac-fts-admin | `io.swagger:swagger-models` | `1.6.2` | `1.6.2` | compile | — |
| ac-fts-admin | `mysql:mysql-connector-java` | `由父级/BOM管理` | `8.0.29` | compile | com.google.protobuf:protobuf-java |
| ac-fts-admin | `com.saas:ac-fts-framework` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-admin | `com.saas:ac-fts-aliyun-oss` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-admin | `com.saas:ac-fts-biz` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-admin | `org.flywaydb:flyway-core` | `由父级/BOM管理` | `7.7.3` | compile | — |
| ac-fts-admin | `org.springframework.boot:spring-boot-starter-test` | `由父级/BOM管理` | `2.5.14` | test | — |
| ac-fts-aliyun-oss | `com.aliyun.oss:aliyun-sdk-oss` | `由父级/BOM管理` | `3.15.1` | compile | — |
| ac-fts-aliyun-oss | `org.springframework.boot:spring-boot-configuration-processor` | `由父级/BOM管理` | `2.5.14` | compile / optional=true | — |
| ac-fts-aliyun-oss | `com.saas:ac-fts-common` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-biz | `com.saas:ac-fts-common` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-biz | `com.saas:ac-fts-system` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-common | `org.springframework:spring-context-support` | `由父级/BOM管理` | `5.3.20` | compile | — |
| ac-fts-common | `org.springframework:spring-web` | `由父级/BOM管理` | `5.3.20` | compile | — |
| ac-fts-common | `org.springframework.boot:spring-boot-starter-security` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-common | `com.github.pagehelper:pagehelper-spring-boot-starter` | `由父级/BOM管理` | `1.4.1` | compile | — |
| ac-fts-common | `org.springframework.boot:spring-boot-starter-validation` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-common | `org.apache.commons:commons-lang3` | `由父级/BOM管理` | `3.12.0` | compile | — |
| ac-fts-common | `com.fasterxml.jackson.core:jackson-databind` | `由父级/BOM管理` | `2.12.6.1` | compile | — |
| ac-fts-common | `com.alibaba.fastjson2:fastjson2` | `由父级/BOM管理` | `2.0.57` | compile | — |
| ac-fts-common | `javax.mail:javax.mail-api` | `1.6.2` | `1.6.2` | compile | — |
| ac-fts-common | `org.springframework.boot:spring-boot-starter-mail` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-common | `commons-io:commons-io` | `由父级/BOM管理` | `2.11.0` | compile | — |
| ac-fts-common | `commons-fileupload:commons-fileupload` | `由父级/BOM管理` | `1.4` | compile | — |
| ac-fts-common | `org.apache.poi:poi-ooxml` | `由父级/BOM管理` | `4.1.2` | compile | — |
| ac-fts-common | `org.yaml:snakeyaml` | `由父级/BOM管理` | `1.28` | compile | — |
| ac-fts-common | `io.jsonwebtoken:jjwt` | `由父级/BOM管理` | `0.9.1` | compile | — |
| ac-fts-common | `javax.xml.bind:jaxb-api` | `由父级/BOM管理` | `2.3.1` | compile | — |
| ac-fts-common | `org.springframework.boot:spring-boot-starter-data-redis` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-common | `org.apache.commons:commons-pool2` | `由父级/BOM管理` | `2.9.0` | compile | — |
| ac-fts-common | `eu.bitwalker:UserAgentUtils` | `由父级/BOM管理` | `1.21` | compile | — |
| ac-fts-common | `javax.servlet:javax.servlet-api` | `由父级/BOM管理` | `4.0.1` | compile | — |
| ac-fts-common | `com.baomidou:mybatis-plus` | `${mybatis-plus.version}` | `3.3.1` | compile | — |
| ac-fts-common | `com.baomidou:mybatisplus-spring-boot-starter` | `${mybatisplus-spring-boot-starter.version}` | `1.0.5` | compile | — |
| ac-fts-common | `cn.hutool:hutool-all` | `${hutool.version}` | `5.8.5` | compile | — |
| ac-fts-common | `org.projectlombok:lombok` | `1.18.30` | `1.18.30` | compile | — |
| ac-fts-common | `org.springframework.boot:spring-boot-starter-websocket` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-common | `org.java-websocket:Java-WebSocket` | `1.3.8` | `1.3.8` | compile | — |
| ac-fts-common | `org.apache.httpcomponents:httpclient` | `4.5.6` | `4.5.6` | compile | commons-logging:commons-logging |
| ac-fts-common | `com.alibaba:easyexcel` | `2.2.1` | `2.2.1` | compile | org.apache.poi:poi, org.apache.poi:poi-ooxml |
| ac-fts-common | `com.google.guava:guava` | `18.0` | `18.0` | compile | — |
| ac-fts-common | `io.swagger:swagger-annotations` | `1.5.21` | `1.5.21` | compile | — |
| ac-fts-framework | `org.springframework.boot:spring-boot-starter-web` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-framework | `org.springframework.boot:spring-boot-starter-aop` | `由父级/BOM管理` | `2.5.14` | compile | — |
| ac-fts-framework | `com.alibaba:druid-spring-boot-starter` | `由父级/BOM管理` | `1.2.11` | compile | — |
| ac-fts-framework | `com.github.penggle:kaptcha` | `由父级/BOM管理` | `2.3.2` | compile | javax.servlet:javax.servlet-api |
| ac-fts-framework | `com.github.oshi:oshi-core` | `由父级/BOM管理` | `6.1.6` | compile | — |
| ac-fts-framework | `com.saas:ac-fts-system` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-framework | `com.saas:ac-fts-biz` | `由父级/BOM管理` | `3.8.3` | compile | — |
| ac-fts-system | `com.saas:ac-fts-common` | `由父级/BOM管理` | `3.8.3` | compile | — |

### B.2 admin 入口模块完整解析依赖树

此树来自当前 POM 副本在本机已缓存依赖上的 Maven 解析。包含 compile/runtime/test 与 optional；它不是线上部署 JAR 的 BOOT-INF/lib 清单，不能据此说测试包一定被打入运行 JAR。

```text
com.saas:ac-fts-admin:jar:3.8.3
+- org.springframework.boot:spring-boot-devtools:jar:2.5.14:compile (optional) 
|  +- org.springframework.boot:spring-boot:jar:2.5.14:compile
|  |  \- org.springframework:spring-context:jar:5.3.20:compile
|  |     \- org.springframework:spring-expression:jar:5.3.20:compile
|  \- org.springframework.boot:spring-boot-autoconfigure:jar:2.5.14:compile
+- io.springfox:springfox-boot-starter:jar:3.0.0:compile
|  +- io.springfox:springfox-oas:jar:3.0.0:compile
|  |  +- io.swagger.core.v3:swagger-annotations:jar:2.1.2:compile
|  |  +- io.swagger.core.v3:swagger-models:jar:2.1.2:compile
|  |  +- io.springfox:springfox-spi:jar:3.0.0:compile
|  |  +- io.springfox:springfox-schema:jar:3.0.0:compile
|  |  +- io.springfox:springfox-core:jar:3.0.0:compile
|  |  +- io.springfox:springfox-spring-web:jar:3.0.0:compile
|  |  |  \- io.github.classgraph:classgraph:jar:4.8.83:compile
|  |  +- io.springfox:springfox-spring-webmvc:jar:3.0.0:compile
|  |  +- io.springfox:springfox-spring-webflux:jar:3.0.0:compile
|  |  +- io.springfox:springfox-swagger-common:jar:3.0.0:compile
|  |  \- org.mapstruct:mapstruct:jar:1.3.1.Final:runtime
|  +- io.springfox:springfox-data-rest:jar:3.0.0:compile
|  +- io.springfox:springfox-bean-validators:jar:3.0.0:compile
|  +- io.springfox:springfox-swagger2:jar:3.0.0:compile
|  +- io.springfox:springfox-swagger-ui:jar:3.0.0:compile
|  +- com.fasterxml:classmate:jar:1.5.1:compile
|  +- org.slf4j:slf4j-api:jar:1.7.36:compile
|  +- org.springframework.plugin:spring-plugin-core:jar:2.0.0.RELEASE:compile
|  |  +- org.springframework:spring-beans:jar:5.3.20:compile
|  |  \- org.springframework:spring-aop:jar:5.3.20:compile
|  \- org.springframework.plugin:spring-plugin-metadata:jar:2.0.0.RELEASE:compile
+- io.swagger:swagger-models:jar:1.6.2:compile
|  +- com.fasterxml.jackson.core:jackson-annotations:jar:2.12.6:compile
|  \- io.swagger:swagger-annotations:jar:1.6.2:compile
+- mysql:mysql-connector-java:jar:8.0.29:compile
+- com.saas:ac-fts-framework:jar:3.8.3:compile
|  +- org.springframework.boot:spring-boot-starter-web:jar:2.5.14:compile
|  |  +- org.springframework.boot:spring-boot-starter-json:jar:2.5.14:compile
|  |  |  +- com.fasterxml.jackson.datatype:jackson-datatype-jdk8:jar:2.12.6:compile
|  |  |  +- com.fasterxml.jackson.datatype:jackson-datatype-jsr310:jar:2.12.6:compile
|  |  |  \- com.fasterxml.jackson.module:jackson-module-parameter-names:jar:2.12.6:compile
|  |  +- org.springframework.boot:spring-boot-starter-tomcat:jar:2.5.14:compile
|  |  |  +- org.apache.tomcat.embed:tomcat-embed-core:jar:9.0.63:compile
|  |  |  +- org.apache.tomcat.embed:tomcat-embed-el:jar:9.0.63:compile
|  |  |  \- org.apache.tomcat.embed:tomcat-embed-websocket:jar:9.0.63:compile
|  |  +- org.springframework:spring-web:jar:5.3.20:compile
|  |  \- org.springframework:spring-webmvc:jar:5.3.20:compile
|  +- org.springframework.boot:spring-boot-starter-aop:jar:2.5.14:compile
|  |  \- org.aspectj:aspectjweaver:jar:1.9.7:compile
|  +- com.alibaba:druid-spring-boot-starter:jar:1.2.11:compile
|  |  \- com.alibaba:druid:jar:1.2.11:compile
|  +- com.github.penggle:kaptcha:jar:2.3.2:compile
|  |  \- com.jhlabs:filters:jar:2.0.235-1:compile
|  +- com.github.oshi:oshi-core:jar:6.1.6:compile
|  |  +- net.java.dev.jna:jna:jar:5.11.0:compile
|  |  \- net.java.dev.jna:jna-platform:jar:5.11.0:compile
|  \- com.saas:ac-fts-system:jar:3.8.3:compile
+- com.saas:ac-fts-aliyun-oss:jar:3.8.3:compile
|  +- com.aliyun.oss:aliyun-sdk-oss:jar:3.15.1:compile
|  |  +- org.apache.httpcomponents:httpclient:jar:4.5.13:compile
|  |  |  +- org.apache.httpcomponents:httpcore:jar:4.4.15:compile
|  |  |  \- commons-codec:commons-codec:jar:1.15:compile
|  |  +- org.jdom:jdom2:jar:2.0.6.1:compile
|  |  +- org.codehaus.jettison:jettison:jar:1.1:compile
|  |  |  \- stax:stax-api:jar:1.0.1:compile
|  |  +- com.aliyun:aliyun-java-sdk-core:jar:4.5.10:compile
|  |  |  +- com.google.code.gson:gson:jar:2.8.9:compile
|  |  |  +- commons-logging:commons-logging:jar:1.2:compile
|  |  |  +- org.jacoco:org.jacoco.agent:jar:runtime:0.8.5:compile
|  |  |  +- org.ini4j:ini4j:jar:0.5.4:compile
|  |  |  +- io.opentracing:opentracing-api:jar:0.33.0:compile
|  |  |  \- io.opentracing:opentracing-util:jar:0.33.0:compile
|  |  |     \- io.opentracing:opentracing-noop:jar:0.33.0:compile
|  |  +- com.aliyun:aliyun-java-sdk-ram:jar:3.1.0:compile
|  |  \- com.aliyun:aliyun-java-sdk-kms:jar:2.11.0:compile
|  \- com.saas:ac-fts-common:jar:3.8.3:compile
|     +- org.springframework:spring-context-support:jar:5.3.20:compile
|     +- org.springframework.boot:spring-boot-starter-security:jar:2.5.14:compile
|     |  +- org.springframework.security:spring-security-config:jar:5.5.8:compile
|     |  |  \- org.springframework.security:spring-security-core:jar:5.5.8:compile
|     |  |     \- org.springframework.security:spring-security-crypto:jar:5.5.8:compile
|     |  \- org.springframework.security:spring-security-web:jar:5.5.8:compile
|     +- com.github.pagehelper:pagehelper-spring-boot-starter:jar:1.4.1:compile
|     |  +- org.mybatis.spring.boot:mybatis-spring-boot-starter:jar:2.2.2:compile
|     |  |  +- org.mybatis.spring.boot:mybatis-spring-boot-autoconfigure:jar:2.2.2:compile
|     |  |  +- org.mybatis:mybatis:jar:3.5.9:compile
|     |  |  \- org.mybatis:mybatis-spring:jar:2.0.7:compile
|     |  +- com.github.pagehelper:pagehelper-spring-boot-autoconfigure:jar:1.4.1:compile
|     |  \- com.github.pagehelper:pagehelper:jar:5.3.0:compile
|     |     \- com.github.jsqlparser:jsqlparser:jar:4.2:compile
|     +- org.springframework.boot:spring-boot-starter-validation:jar:2.5.14:compile
|     |  \- org.hibernate.validator:hibernate-validator:jar:6.2.3.Final:compile
|     |     +- jakarta.validation:jakarta.validation-api:jar:2.0.2:compile
|     |     \- org.jboss.logging:jboss-logging:jar:3.4.3.Final:compile
|     +- org.apache.commons:commons-lang3:jar:3.12.0:compile
|     +- com.fasterxml.jackson.core:jackson-databind:jar:2.12.6.1:compile
|     |  \- com.fasterxml.jackson.core:jackson-core:jar:2.12.6:compile
|     +- com.alibaba.fastjson2:fastjson2:jar:2.0.57:compile
|     +- javax.mail:javax.mail-api:jar:1.6.2:compile
|     +- org.springframework.boot:spring-boot-starter-mail:jar:2.5.14:compile
|     |  \- com.sun.mail:jakarta.mail:jar:1.6.7:compile
|     |     \- com.sun.activation:jakarta.activation:jar:1.2.2:compile
|     +- commons-io:commons-io:jar:2.11.0:compile
|     +- commons-fileupload:commons-fileupload:jar:1.4:compile
|     +- org.apache.poi:poi-ooxml:jar:4.1.2:compile
|     |  +- org.apache.poi:poi:jar:4.1.2:compile
|     |  |  +- org.apache.commons:commons-collections4:jar:4.4:compile
|     |  |  +- org.apache.commons:commons-math3:jar:3.6.1:compile
|     |  |  \- com.zaxxer:SparseBitSet:jar:1.2:compile
|     |  +- org.apache.poi:poi-ooxml-schemas:jar:4.1.2:compile
|     |  |  \- org.apache.xmlbeans:xmlbeans:jar:3.1.0:compile
|     |  +- org.apache.commons:commons-compress:jar:1.19:compile
|     |  \- com.github.virtuald:curvesapi:jar:1.06:compile
|     +- org.yaml:snakeyaml:jar:1.28:compile
|     +- io.jsonwebtoken:jjwt:jar:0.9.1:compile
|     +- javax.xml.bind:jaxb-api:jar:2.3.1:compile
|     |  \- javax.activation:javax.activation-api:jar:1.2.0:compile
|     +- org.springframework.boot:spring-boot-starter-data-redis:jar:2.5.14:compile
|     |  +- org.springframework.data:spring-data-redis:jar:2.5.11:compile
|     |  |  +- org.springframework.data:spring-data-keyvalue:jar:2.5.11:compile
|     |  |  |  \- org.springframework.data:spring-data-commons:jar:2.5.11:compile
|     |  |  +- org.springframework:spring-tx:jar:5.3.20:compile
|     |  |  \- org.springframework:spring-oxm:jar:5.3.20:compile
|     |  \- io.lettuce:lettuce-core:jar:6.1.8.RELEASE:compile
|     |     +- io.netty:netty-common:jar:4.1.77.Final:compile
|     |     +- io.netty:netty-handler:jar:4.1.77.Final:compile
|     |     |  +- io.netty:netty-resolver:jar:4.1.77.Final:compile
|     |     |  +- io.netty:netty-buffer:jar:4.1.77.Final:compile
|     |     |  \- io.netty:netty-codec:jar:4.1.77.Final:compile
|     |     +- io.netty:netty-transport:jar:4.1.77.Final:compile
|     |     \- io.projectreactor:reactor-core:jar:3.4.18:compile
|     |        \- org.reactivestreams:reactive-streams:jar:1.0.3:compile
|     +- org.apache.commons:commons-pool2:jar:2.9.0:compile
|     +- eu.bitwalker:UserAgentUtils:jar:1.21:compile
|     +- javax.servlet:javax.servlet-api:jar:4.0.1:compile
|     +- com.baomidou:mybatis-plus:jar:3.3.1:compile
|     |  \- com.baomidou:mybatis-plus-extension:jar:3.3.1:compile
|     |     \- com.baomidou:mybatis-plus-core:jar:3.3.1:compile
|     |        \- com.baomidou:mybatis-plus-annotation:jar:3.3.1:compile
|     +- com.baomidou:mybatisplus-spring-boot-starter:jar:1.0.5:compile
|     |  +- org.springframework.boot:spring-boot-configuration-processor:jar:2.5.14:compile
|     |  \- org.springframework.boot:spring-boot-starter-jdbc:jar:2.5.14:compile
|     |     +- com.zaxxer:HikariCP:jar:4.0.3:compile
|     |     \- org.springframework:spring-jdbc:jar:5.3.20:compile
|     +- cn.hutool:hutool-all:jar:5.8.5:compile
|     +- org.projectlombok:lombok:jar:1.18.24:compile
|     +- org.springframework.boot:spring-boot-starter-websocket:jar:2.5.14:compile
|     |  +- org.springframework:spring-messaging:jar:5.3.20:compile
|     |  \- org.springframework:spring-websocket:jar:5.3.20:compile
|     +- org.java-websocket:Java-WebSocket:jar:1.3.8:compile
|     +- com.alibaba:easyexcel:jar:2.2.1:compile
|     |  +- cglib:cglib:jar:3.1:compile
|     |  |  \- org.ow2.asm:asm:jar:4.2:compile
|     |  \- org.ehcache:ehcache:jar:3.9.9:compile
|     \- com.google.guava:guava:jar:18.0:compile
+- com.saas:ac-fts-biz:jar:3.8.3:compile
+- org.flywaydb:flyway-core:jar:7.7.3:compile
\- org.springframework.boot:spring-boot-starter-test:jar:2.5.14:test
   +- org.springframework.boot:spring-boot-starter:jar:2.5.14:compile
   |  +- org.springframework.boot:spring-boot-starter-logging:jar:2.5.14:compile
   |  |  +- ch.qos.logback:logback-classic:jar:1.2.11:compile
   |  |  |  \- ch.qos.logback:logback-core:jar:1.2.11:compile
   |  |  +- org.apache.logging.log4j:log4j-to-slf4j:jar:2.17.2:compile
   |  |  |  \- org.apache.logging.log4j:log4j-api:jar:2.17.2:compile
   |  |  \- org.slf4j:jul-to-slf4j:jar:1.7.36:compile
   |  \- jakarta.annotation:jakarta.annotation-api:jar:1.3.5:compile
   +- org.springframework.boot:spring-boot-test:jar:2.5.14:test
   +- org.springframework.boot:spring-boot-test-autoconfigure:jar:2.5.14:test
   +- com.jayway.jsonpath:json-path:jar:2.5.0:test
   |  \- net.minidev:json-smart:jar:2.4.8:test
   |     \- net.minidev:accessors-smart:jar:2.4.8:test
   +- jakarta.xml.bind:jakarta.xml.bind-api:jar:2.3.3:test
   |  \- jakarta.activation:jakarta.activation-api:jar:1.2.2:test
   +- org.assertj:assertj-core:jar:3.19.0:test
   +- org.hamcrest:hamcrest:jar:2.2:test
   +- org.junit.jupiter:junit-jupiter:jar:5.7.2:test
   |  +- org.junit.jupiter:junit-jupiter-api:jar:5.7.2:test
   |  |  +- org.apiguardian:apiguardian-api:jar:1.1.0:test
   |  |  +- org.opentest4j:opentest4j:jar:1.2.0:test
   |  |  \- org.junit.platform:junit-platform-commons:jar:1.7.2:test
   |  +- org.junit.jupiter:junit-jupiter-params:jar:5.7.2:test
   |  \- org.junit.jupiter:junit-jupiter-engine:jar:5.7.2:test
   |     \- org.junit.platform:junit-platform-engine:jar:1.7.2:test
   +- org.mockito:mockito-core:jar:3.9.0:test
   |  +- net.bytebuddy:byte-buddy:jar:1.10.22:compile
   |  +- net.bytebuddy:byte-buddy-agent:jar:1.10.22:test
   |  \- org.objenesis:objenesis:jar:3.2:test
   +- org.mockito:mockito-junit-jupiter:jar:3.9.0:test
   +- org.skyscreamer:jsonassert:jar:1.5.0:test
   |  \- com.vaadin.external.google:android-json:jar:0.0.20131108.vaadin1:test
   +- org.springframework:spring-core:jar:5.3.20:compile
   |  \- org.springframework:spring-jcl:jar:5.3.20:compile
   +- org.springframework:spring-test:jar:5.3.20:test
   \- org.xmlunit:xmlunit-core:jar:2.8.4:test
```

### B.3 版本复核的可重复方式

本次在独立 POM 副本及缓存副本中运行 Maven 3.10.0，使用缓存已有的 dependency 插件 3.2.0 作分析工具；该插件没有加入参考项目或要求加入毕业项目 POM。核心命令形状：

```text
mvn -o -B -Daether.remoteRepositoryFilter.prefixes=false -Dmaven.repo.local=<缓存副本目录> org.apache.maven.plugins:maven-dependency-plugin:3.2.0:tree -DoutputFile=dependency-tree.txt
```
在 PowerShell 中含点号或路径的 -D 参数应整体加引号。分析命令没有执行 compile/test/package/start 或迁移，根及六个模块均返回成功；Druid 上游 POM 警告已列于第 14 节。

[S01]: #s01
[S02]: #s02
[S03]: #s03
[S04]: #s04
[S05]: #s05
[S06]: #s06
[S07]: #s07
[S08]: #s08
[S09]: #s09
[S10]: #s10
[S11]: #s11
[S12]: #s12
[S13]: #s13
[S14]: #s14
[S15]: #s15
[S16]: #s16
[S17]: #s17
[S18]: #s18
[S19]: #s19
[S20]: #s20
[S21]: #s21
[S22]: #s22
[S23]: #s23
[S24]: #s24
[S25]: #s25
[S26]: #s26
[S27]: #s27
[S28]: #s28
[S29]: #s29
[S30]: #s30
[S31]: #s31
[S32]: #s32
[S33]: #s33
[S34]: #s34
[S35]: #s35
[S36]: #s36
