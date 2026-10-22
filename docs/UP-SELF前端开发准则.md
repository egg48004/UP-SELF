> 用户已确认，2026-10-22开始实施P0及学习闭环。学生单独布局；管理三身份共享管理界面。原文“待确认”保留为历史版本，实际实施状态见《学习模块前后端交付与验收》。作业与心情仍在后续阶段。

> **本轮实施结果（2026-10-22）**：P0 与学习核心已完成。Node 24.22.1 / npm 11.19.1；Vue 3.5.44、Vite 7.3.7、TypeScript 5.9.3 等已锁定到 package-lock.json。BaseController/current 分页、角色候选接口和真实 Vue 页面均已接通；作业与心情未开发。实际版本、FR 状态、浏览器验收及限制以 [学习模块前后端交付与验收](学习模块前后端交付与验收.md) 为准。以下环境快照与缺口描述保留作实施前依据。

# UP-SELF 前端开发准则

版本：1.0，提交审阅稿。日期：2026-10-21。状态：**待确认；本轮不执行工程变更。**

与[UP-SELF前端开发计划.md](UP-SELF前端开发计划.md)配套。适用目标：一个面向马来西亚首批10–30名学生的独立学习网站，Vue 3前端与现有Java单体后端联通。规则中的“必须”表示计划确认后的代码验收要求，不表示目前已经实现。

## 1. 约束和来源优先级

1. 用户最新确认的需求优先；报告原文保留编号和差异记录；参考项目仅作为后端基准。
2. 独立后端已实现内容以实际源码为准，历史文档可能滞后。未实现的接口必须标为缺口，不在前端写假成功或虚构字段。
3. 后端架构、认证、依赖版本和数据库迁移方式不因前端开发改变；不修改参考项目，不更换分页/异常体系。
4. 前端是新增工程，没有“参考项目前端版本”。计划中的工具选型属于新建议，P0逐项核对并锁定版本，不能宣称从Java参考项目继承。
5. 只引入支撑已确定功能的能力。不创建微前端、monorepo工作区、多层repository/service、插件中心、后台页面生成器、通用低代码表单或第二状态管理库。

报告与会话差异、全部FR/NFR、阶段与API缺口见计划。准则不能代替FR验收：目录漂亮或测试数量多不代表用户流程已完成。

## 2. 开源项目与工业实践的取舍依据

截至2026-10-21，读取以下官方仓库/文档，**只借鉴可检查的模式，不把模板整体作为产品或复制其版本与认证协议**。

| 一手来源 | 本次实际读取 | 借鉴 | 明确不照搬 |
|---|---|---|---|
| [v3-admin-vite](https://github.com/un-pany/v3-admin-vite) | [package.json](https://raw.githubusercontent.com/un-pany/v3-admin-vite/main/package.json)、[router/index.ts](https://raw.githubusercontent.com/un-pany/v3-admin-vite/main/src/router/index.ts)、[router/guard.ts](https://raw.githubusercontent.com/un-pany/v3-admin-vite/main/src/router/guard.ts) | 路由与守卫分文件、按页面组织、入口加载身份、显式依赖 | 动态多级路由展平、标签页、通用仪表盘、全部依赖；守卫中遇所有错误就注销的行为不适合网络波动 |
| [pure-admin-thin](https://github.com/pure-admin/pure-admin-thin) | [utils/http/index.ts](https://raw.githubusercontent.com/pure-admin/pure-admin-thin/main/src/utils/http/index.ts)、[api/user.ts](https://raw.githubusercontent.com/pure-admin/pure-admin-thin/main/src/api/user.ts)、[router/index.ts](https://raw.githubusercontent.com/pure-admin/pure-admin-thin/main/src/router/index.ts) | 请求入口统一、业务API与HTTP实现分离、明确响应类型 | 模板refreshToken、重放队列、Cookie/多标签状态、直接依赖用户store的HTTP类；本后端协议不匹配 |
| [Vue composables](https://vuejs.org/guide/reusability/composables)和[状态管理指南](https://vuejs.org/guide/scaling-up/state-management.html) | 官方指南 | 将有状态复用逻辑封装，副作用有清理，区分本地状态与共享状态 | 不把每个函数都变composable或全局store |
| [Vue Router meta](https://router.vuejs.org/guide/advanced/meta.html)、[Pinia store](https://pinia.vuejs.org/core-concepts/) | 官方接口职责 | 显式路由meta、少量身份和偏好store | 不建立可执行后端菜单脚本或任意import路径 |
| [Moodle课程主页](https://docs.moodle.org/en/Course_homepage) | 章节索引、内容分区、前后导航的产品流程 | 结构明确、当前位置可见、学生与编辑操作分离 | 完整LMS、插件商店、复杂区块、全部功能菜单 |

这些是规模适合或可裁剪的参考，未把它们声明为经审计的无障碍方案。GitHub main会变化；若后续复制具体代码，必须记录当时commit、路径和许可证，并保留必要声明。当前只读取上述文件，未做全仓库代码审计；取舍是本项目的设计判断。

## 3. 工程目录与依赖方向

建议单一 `up-self/frontend`，以下是**未来目录设计**，当前不创建。只为首版功能建立文件；后续模块在真正开发时添加。

```text
frontend/
  package.json / package-lock.json
  vite.config.ts / tsconfig.json
  index.html
  public/                         # 无敏感静态素材；不是课程私有文件
  src/
    main.ts / App.vue
    app/
      router.ts / guards.ts       # 组合各模块路由与身份校验
      setup.ts                    # 连接http回调、router、store、i18n
      layouts/
        StudentLayout.vue
        ManagementLayout.vue
    shared/
      http/
        client.ts                 # Axios实例、超时、取消、解析
        contracts.ts              # Ajax/Table/登录特殊响应类型
        errors.ts                 # ApiError分类，无UI弹窗
      auth/
        session.ts                # token/channel读写与清理，不依赖Pinia
        permissions.ts            # 纯函数，明确any/all，不读router
      ui/                         # 少量跨模块可访问组件
        AsyncState.vue
        ConfirmDialog.vue
        PaginationBar.vue
        FormField.vue             # 只有实际统一label/error需要才建立
      composables/                # 真实跨模块的状态逻辑，如usePagination
      i18n/
        index.ts
        en/common.ts
        en/auth.ts
        en/learning.ts
        en/accounts.ts
      styles/
        tokens.css / base.css
      utils/                      # 纯日期/URL等工具，不放业务万能函数
    features/
      auth/
        api.ts / types.ts / store.ts / routes.ts
        pages/LoginPage.vue / SecurityPage.vue
      preferences/
        store.ts / routes.ts
        pages/PreferencesPage.vue
        components/ReadingControls.vue
        composables/useSpeech.ts
      accounts/
        api.ts / types.ts / routes.ts
        pages/AccountListPage.vue
        components/AccountForm.vue / AccountPicker.vue
      learning/
        api.ts / types.ts / routes.ts
        pages/
          StudentCoursesPage.vue / StudentCoursePage.vue / ChapterPage.vue
          AdminCoursesPage.vue / AdminCoursePage.vue
          MentorCoursesPage.vue / MentorCoursePage.vue / ChapterEditPage.vue
          CoursePostsPage.vue
        components/
          CourseCard.vue / CourseForm.vue / ChapterNavigation.vue
          CourseReader.vue / MediaViewer.vue / ResourceUpload.vue
          ChapterEditor.vue / PostEditor.vue / CourseMemberPicker.vue
        composables/
          useCourseList.ts         # 确实有多个页面共用筛选/分页时提取
          useMediaAccess.ts
      # assignments、mood等只在P2/P3开始时建立，届时结构同样简单
    pages/ForbiddenPage.vue / NotFoundPage.vue
  tests/
    unit/ / components/ / e2e/
```

`api.ts`与`types.ts`就近存放，模块增长后再按course/resource/post拆文件，不一开始建多层文件夹。英文语言文件按功能拆在统一i18n目录，避免为了找一条文案翻遍组件。路由写在模块，app统一注册，组件不自行注册全局路由。

### 3.1 必须保持的依赖边界

| 层 | 可以依赖 | 不允许依赖 |
|---|---|---|
| shared纯工具/类型/HTTP/基础UI | 标准库、选定基础依赖、同层无环模块 | features、router实例、业务store、特定角色页面 |
| feature API | shared/http、该模块types、纯参数映射 | Vue组件、UI弹窗、router、Pinia业务store |
| feature composable/store | 本模块API、纯工具、少量明确共享契约 | 页面组件；其他模块内部文件 |
| feature页面 | 本模块组件/API/composable、shared、正式公开的跨模块接口 | 另一页面组件的内部状态、另一模块私有实现 |
| app组合与布局 | 路由、功能公开入口、身份/偏好store、shared | 数据库、秘密配置、业务SQL |

HTTP不能import auth store，auth store又import API再import HTTP形成环。解决方式：HTTP读取`shared/auth/session.ts`的令牌，返回标准错误；app/setup注册会话失效通知并连接store/router。回调只一种清楚的连接方式，不加事件总线。响应解析和错误类型不依赖界面。

跨模块示例：课程分配页面可通过accounts模块公开的AccountPicker或候选API使用账号信息，不直接访问AccountListPage的ref。AccountPicker只返回选中ID和可读名称，不自动给人授权。学习页面不能import未来mood模块去拉所有心情；导师概览需要新授权聚合接口时先定后端契约。

不要循环导出大量`index.ts`。只有模块需要被别的模块使用时建立很小的公开出口；其余使用明确文件路径。不要把业务组件搬进shared来“解决”循环依赖。

## 4. 封装准则与复杂度限制

| 对象 | 应封装的稳定能力 | 留在页面或功能模块的内容 |
|---|---|---|
| HTTP | token、业务响应识别、错误分类、超时/取消 | 课程API路径、表单、成功跳转、业务权限 |
| 分页 | current/pageSize/total、切页/换筛选重置规则 | 列表SQL条件、列定义、课程卡片样式 |
| 表单 | 一致label/error关联和提交中交互 | CourseForm/PostEditor各自字段、规则及保存流程；不建JSON schema生成器 |
| 上传 | 文件选择、进度、取消、失败反馈 | 教学资源允许类型/字幕关系放learning；作业附件规则放assignments |
| 媒体 | 同一学习模块内ticket加载/有限重试/释放 | 课程身份判断和文案留上层；不引入通用媒体插件系统 |
| 权限 | 纯hasAnyPermission/hasAllPermissions | 哪个动作需要哪些权限及归属，由路由/功能组件声明，后端最终裁决 |
| 样式 | 色彩、间距、字号、焦点、断点变量 | 各页面布局；不另造完整设计系统组件库 |

提取的理由是“真实重复、需要独立测试、或存在明确生命周期”，而不是代码超过某个行数。第一次出现的纯业务表单可以就地实现；第二个相同需求出现再提取共享。认证、无障碍反馈等明确公共职责可一开始提取。避免一个万能BasePage/BaseForm/BaseTable承担全部业务。

组件通过props输入、emits输出，不直接修改父props，不把整个store传给子组件。媒体组件接收资源元数据/加载能力，触发error/position事件；是否发布、谁能上传不写进播放器。CourseReader复用只读内容，管理员/导师编辑按钮在外围页面，不能让播放器知道四种角色。

优先组合，拒绝页面继承与全局mixin。小型一次性计算使用普通函数，不为它引入useX。composable清理监听器、AbortController、计时器、object URL及speech队列；参考[Vue composables生命周期说明](https://vuejs.org/guide/reusability/composables)。

### 4.1 状态放置

- 页面ref/reactive：当前表单、选中章节、弹窗、单页上传队列、列表结果、当前播放位置。
- 路由query：可分享的搜索词和页码；不得含token、私有文本或密码。pageSize有限可写入query。
- Pinia auth：已验证的user、roles、permissions、channel和加载状态；不复制所有课程结果进去。
- Pinia preferences：字号、对比、减少动态、界面locale；非敏感偏好可带schemaVersion存本机，支持恢复默认。
- 服务端：账号角色、课程关系、进度、收藏、正式作业/草稿、评分、心情及分享关系；前端状态不是这些数据的权威来源。
- 不引入查询缓存库；先页面按需请求和成功后回读。确实出现重复请求和失效管理困难，再拿具体问题评估，不能先搭双重缓存。

### 4.2 命名与类型

组件PascalCase且名称说明职责，如CourseForm；composable为useX；API函数为listCourses/createCourse/assignCourseMentor；DTO、枚举和返回类型用明确业务名称。不要使用data1、handleAll或几千行utils.ts。

TypeScript strict；不通过any掩盖响应差异。外部JSON在请求边界做必要的字段检查，异常值不直接当已验证类型。DOM event、可空值和失败态有明确处理；不再额外引入复杂schema库，除非实际契约规模证明有需要。

API DTO沿后端真实字段，表单模型只有格式/可空/显示转换确有差异时才单独定义，不为每个实体机械建立DTO/VO/Entity三套前端类型。当前long ID为小整数；若后端以后返回超出JavaScript安全整数范围的ID，应约定字符串，不能无声精度截断。

## 5. HTTP、响应和权限协议

### 5.1 请求入口必须单一

所有业务JSON通过shared/http；组件不直接调用Axios或fetch。API函数提供有名称的操作，返回明确类型，既不弹toast也不自行导航。调用方处理成功反馈；HTTP仅处理通用错误和会话失效通知，避免同一失败弹两次。

请求客户端统一API前缀、Bearer头、超时、取消，上传与JSON走同一认证入口。上传FormData不强行设application/json或手工边界；网络请求失败不清空表单。GET列表可取消旧请求/标记请求序号，防止较慢旧搜索覆盖较新结果。POST/PUT不自动重放；超时可能已保存，应回读查询或由幂等接口确认。

### 5.2 保留真实响应形状

| 形状 | 成功示例字段（非真实业务数据） | 处理 |
|---|---|---|
| 普通Ajax | code=200、msg、data | 显式requestData用于详情和创建ID；无数据成功不强求data非空 |
| 表格分页 | code=200、msg、rows、total | requestTable返回rows/total，不误读data.rows |
| 登录 | code=200、msg、token | 专门login调用返回token，不套普通data解包 |
| getInfo | code=200、user、roles、permissions | 专门身份响应类型，不能把菜单标识当页面按钮 |
| 媒体 | HTTP200/206 + Content-Type + 字节流 | 不通过JSON解码器，不依赖AjaxResult |

不能一套拦截器总是`return response.data.data`。JSON解析前判断响应类型；非JSON失败转标准网络/服务器错误，日志不输出token、密码、心情或媒体ticket。

业务code与HTTP分开判断：200成功；401会话无效（当前可能HTTP500）；403无权（当前可能HTTP200）；400参数；404目标；1010登录失败/锁定；429限流；其他500给英文通用错误。HTTP非2xx若JSON存在仍读取业务code，不能因Axios先抛错而跳过。只有认证明确失败才注销，超时/断网不注销账号。

### 5.3 分页按参考项目对齐

参考源码：`ac-fts-common/src/main/java/com/saas/common/core/controller/BaseController.java`的`startPage/getDataTable`，以及`core/page/TableSupport.java`的current/pageSize、`TableDataInfo.java`的顶层rows/total。P0迁移到独立common后，Vue只发current/pageSize，搜索归1，默认10、上限100。不得在前端再次切割已分页结果。无分页的小集合明确使用数组，不伪造total。

排序只给允许字段，不能把任意用户输入变成SQL列；当前接口未提供排序参数时不显示假排序箭头。章节sortOrder已有，可用数字或Move up/Move down；不只支持拖拽。P0列表契约变更须同步既有脚本与旧页，不改已执行迁移。

### 5.4 登录与权限

- 学生POST `/client/login`；导师/管理员POST `/empLogin`。密码按原文经HTTPS提交，前端不MD5、不伪造TOTP、不设置后台角色ID作为登录依据。
- 当前TOTP关闭，不能显示绑定/动态码必填界面；后端若以后启用，应作为独立确认变更。
- token/channel保持现有sessionStorage方案；Pinia加载/getInfo后才决定角色页；不将后端签名密钥或密码写进前端环境变量。
- 守卫先恢复身份，再检查route meta中明确role/channel/permissions。未知路由404，已登录无权403，未登录转login；returnTo只接受本站允许的相对路由，避免任意外链跳转。
- 操作权限和业务归属不同。`system:user`更新要求edit和changeStatus，必须all；只查其中一个会错误显示按钮。`*:*:*`只按现有维护身份识别，不给平台管理员伪造。
- 后端`getRouters`返回扁平菜单，不是可直接渲染的Vue路由。首版采用静态模块路由 + meta权限 + 已知菜单映射，不通过后端字符串执行动态import，不构建复杂动态菜单系统。
- 退出优先请求服务器logout，清理token与身份/课程/媒体状态。服务器不可达时仍本地退出，并说明服务器注销未确认；不声称远端token一定已撤销。
- 401并发只处理一次，避免多个弹窗和导航循环；原页面编辑内容只保留必要内存，不把密码或心情自动存localStorage。网络恢复后重试需用户明确，不能无条件重复提交。

源码依据：独立 `TokenService.java`、`SysUserController.java`、`LearningController.java`、`LearningServiceImpl.java`和V3权限脚本。sessionStorage也可被同源脚本访问，不能称为XSS安全保险；应配合禁止不可信HTML、依赖审查及部署CSP，不能直接改认证体系来逃避实现。

## 6. 表单、上传和内容安全

表单验证与后端DTO一致：课程/章节/资源/帖子标题最多150；描述/正文上限20000；图片altText最多500；languageCode格式按现有DTO，默认en；会议HTTPS链接最多2000，时间采用含时区offset的ISO8601。具体范围最终以源码为准，调整时两端一起改。不用前端输入规则替代服务端验证。

正文当前是plain text，Vue文本插值渲染，不使用v-html。不为了富文本编辑器一次增加HTML清洗、复杂编辑器与数据库结构；确需富文本另立需求。显示用户名字/文件名/错误消息也不当HTML执行。

上传教学资源：图片PNG/JPEG/WebP≤10MB、视频MP4≤100MB、字幕VTT≤1MB；立即说明允许类型/大小和所需替代说明。浏览器先校验用于反馈，服务器签名及权限检查最终裁决。上传取消不等于服务器文件一定被删；成功后用真实资源ID回读。替换二进制文件当前无独立PUT能力，界面采用新上传/明确替换流程，不能静默覆盖引用关系。

提交按钮在请求中禁用，但后端仍需并发/幂等校验。分页筛选可在URL保存，密码、作业正文和心情不放URL。会议链接解析并限制https，不允许javascript/data URL或嵌入凭证；新窗口注明“opens in a new tab”且rel=noopener noreferrer，不把会议URL当嵌入脚本。

资源删除/下架/归档有明确不同文案。归档课程现有后端拒绝内容修改，不在前端绕过。导师自己的帖子才可编辑，新的负责导师不继承原作者身份。确认框指出资源名称和影响，拒绝“Are you sure?”无上下文提示；取消后焦点回原按钮。

## 7. 媒体访问、生命周期与进度

### 7.1 媒体要真实可播放

用原生video controls，preload=metadata，不autoplay，不自动跳下一视频，不自动重放；允许用户主动选择播放速度。img使用altText；captionResourceId对应同章节VTT，独立取得ticket作为track source；transcript提供可展开文字。优先保留浏览器控制，不自绘复杂播放器。

元数据/内容接口见计划L4/L4a。ticket只绑定特定资源/会话/channel且有效1800秒，**不是永久URL**。统一resolveApiUrl处理`/api`代理前缀；生产同源仍应一致处理。课程文件不放public目录，也不暴露.local/uploads。

封装useMediaAccess只负责状态和ticket：idle/loading/ready/error，取消旧加载，最多一次刷新尝试；卸载时释放Blob URL、事件及计时器。不得让播放器直接访问账号store、课程分配逻辑或数据库。

403可能是ticket过期，也可能是资源/课程/登录权限撤销：最多重新鉴权/取ticket一次，若失败停止并解释，不能定时无限续链掩盖失权。暂停超过30分钟恢复、字幕加载、视频拖动Range206与不合法Range416都进入测试。

当前媒体GET不刷新Redis登录TTL。长时间只看视频可能会话失效；不偷偷后台保活，应显示登录恢复入口。前端内存可保存当前播放秒数用于本页恢复，但不能当作FR-04/05/NFR-08的服务端进度。已发出的长响应和已缓冲内容不能被撤销“收回”，验收检查后续请求确实被拒绝。

### 7.2 内容无障碍不是字段检查

字幕必须与视频真实内容一致，文字稿不能只有标题；对关键信息仅在画面中的视频，需导师提供描述性文字/解说，不能用一份对白字幕宣称满足全部媒体无障碍要求。测试使用含对白、重要画面说明的真实短视频，而不是仅验证MP4文件头。[WAI媒体指导](https://www.w3.org/WAI/media/av/)

### 7.3 P1b进度接口才负责学习记录

先定义“访问/进行中/完成”的业务规则。播放结束不证明理解；后台不应因用户快进就自动授予成就。保存需节流、幂等和资源身份校验，断网提示未同步，页面离开不只靠unload异步请求。此段是后续设计约束，不能据此宣称已有实现。

## 8. 包容性与可访问验收准则

目标：结合报告FR-09/10及NFR-01～03，按实际页面检查相关WCAG 2.2 A/AA要求，并采用认知可用性补充指导。仅做设计或自动扫描不能宣称完整符合WCAG。[WCAG 2.2正式文本](https://www.w3.org/TR/WCAG22/)、[W3C认知与学习可用性说明](https://www.w3.org/TR/coga-usable/)

以下具体数值或交互选择中，“项目规则”是本项目的实施决定；不把它们全部说成WCAG强制条文。

| 检查项 | 项目实现规则 | 手动验收方法 |
|---|---|---|
| 页面结构 | 一个明确主标题，main/nav/header语义，Skip to content，章节层级连续 | 键盘跳过重复导航，屏幕阅读器可辨区域和标题 |
| 稳定导航 | 三角色同类导航位置固定，面包屑和当前章节可见；学生首屏只保留当前任务 | 完成一次学习流程不依赖记住隐藏按钮 |
| 任务说明 | 一步一个明确目标；按钮使用Save draft/Publish course等具体动词；说明放输入前 | 新用户可说出下一步，不依靠权限字符串理解页面 |
| 键盘与焦点 | 全部操作可Tab/Enter/Space；弹窗进入/返回焦点正确；无陷阱，焦点不被固定栏遮挡 | 不使用鼠标完成登录、进入章节、上传选择、保存、关闭确认框 |
| 对比 | 普通文字目标≥4.5:1，大字≥3:1；关键控件/状态边界≥3:1；不能只靠颜色 | 对标准/高对比主题都检查文字和控件，包括错误与disabled说明 |
| 字号与重排 | 项目建议正文16/18/20px等效rem三档；支持200%缩放，320CSS像素重排；表格合理局部滚动 | 三档全部测试，正文/按钮不截断，不锁浏览器缩放 |
| 点击目标 | 项目优先44px高度；WCAG2.5.8的24CSS像素或间距例外另行按标准检查 | 小图标也有名称及足够点击区域，不把44px说成全部AA统一要求 |
| 非文本提示 | 状态图标附文字；教学图片必须有意义alt；装饰图alt为空 | 关闭图片或阅读器下仍能理解关键内容 |
| 低干扰 | 不闪烁、无轮播自动切换、无自动播放；默认少动效并尊重prefers-reduced-motion | 开启系统减少动态后，无位移/缩放等非必要效果 |
| 字段与错误 | label显式关联，required说明，aria-describedby/invalid；字段错误+必要错误摘要 | 提交空表单后焦点能到首个错误；原输入不丢；成功/失败状态可被读出 |
| 消息与加载 | 通用进度提示，错误持续可读并能重试；不只2秒toast或spinner | 网络失败能找到Retry；无不断朗读的高频进度通知 |
| 认证便利 | 允许密码管理器、粘贴、自动填充；不强迫记忆谜题；有清晰入口选择 | 用粘贴和自动填充登录；不重复实现已关闭的TOTP |
| 排序操作 | 章节移动提供按钮/数字，拖拽只能作为附加 | 纯键盘可调整顺序并确认保存 |
| 媒体支持 | 字幕/文字稿/替代说明明确入口；暂停/音量/速度可控；缺失时诚实提示 | 真正打开CC，看内容同步，读文字稿；不能只有“Supports captions”说明 |
| TTS | Read aloud一键启动当前正文，提供Pause/Resume/Stop；换页或播放视频时停止朗读 | 三浏览器声音列表检查、开始/停止验证；不支持时明确提示，不静默失败 |
| 个人偏好 | Standard/Large/Extra large与Standard/High contrast选择；Reset preferences；不预设单一神经类型主题 | 刷新后保持非敏感本机设置，切账号不泄漏个人学习数据 |
| 时间与压力 | 不强制学习倒计时，不惩罚断签，不推送“落后他人”；作业截止用清楚当地时间 | 无人身评价、红色失败排名或连续提醒催促 |
| 心情与趋势 | 离散记录+可读表格；没有数据的周保留缺口；不用颜色独占含义、不自动医学诊断 | 表格与图一致，无记录不当0分/正常；明确谁能看、何时撤销 |

结构和表单的具体做法另参[WAI页面结构](https://www.w3.org/WAI/tutorials/page-structure/)与[WAI表单通知](https://www.w3.org/WAI/tutorials/forms/notifications/)。文字对比、重排、键盘、字幕及认证便利的最终合规判断按标准逐项检查，不以使用Element Plus自动保证。

TTS优先浏览器SpeechSynthesis，需异步等待可用语音、基于内容languageCode选择并给用户控制；不同浏览器/系统的voice和是否依赖网络可能不同，不能承诺离线或绝不外传。上线前记录所选声音与数据处理行为，不把作业/心情自动送入朗读或云服务；本期不新增云TTS费用。[MDN SpeechSynthesis](https://developer.mozilla.org/en-US/docs/Web/API/SpeechSynthesis)

可用性检查邀请少量自愿目标用户（建议3–5人）观察找课程、播放字幕、保存表单等任务，允许休息/退出，不要求提交诊断信息。样本反馈用于迭代，不能宣称代表所有神经多样性学生。

## 9. 多语言、时间与样式

### 9.1 英文优先但可扩展

使用Vue I18n管理全局locale，当前en及fallback en。键按功能，如learning.course.publish、learning.resource.uploadFailed；使用参数插值，不用拼接英文碎片。按钮、aria-label、表单校验、空态、上传和确认提示、页面标题及UI库默认文案都英语化。[Vue I18n作用域与locale](https://vue-i18n.intlify.dev/guide/essentials/scope)

组件不得散落用户可见字符串；枚举映射到语言键，不直接把PUBLISHED、roleKey或权限字符串作为用户文案。后端msg目前是英文，保留可读业务反馈；401/403等稳定code映射前端文案，未知消息作为安全纯文本兜底，不按英文句子内容反向匹配业务逻辑。后续细粒度本地化若需稳定错误标识，单独与后端约定，不擅改现有响应。

界面locale、课程languageCode、字幕语言是三个不同概念。语言字段不自动翻译内容，也不意味着一个课程已经有多语言版本。首版不显示不能工作的语言切换菜单；留好资源和入口后，新增语言需同步键完整性及布局测试。

### 9.2 日期、时区与CSS

界面日期使用Intl格式化；英语月份/日期格式明确。会议时间从datetime-local读取后必须附选定时区offset，不能直接追加Z。默认展示建议Asia/Kuala_Lumpur，并在会议/作业截止旁标明；服务器现有Date字段格式需在P0确认，不能猜测无offset字符串时区。周心情按服务端周边界生成weekId，不能只靠浏览器时区决定是否能提交。

tokens.css集中正文、背景、边界、错误、焦点、间距、行高、阅读宽度。组件样式scoped，必要全局样式只写基础层；避免深度选择器修改另一业务模块。Element Plus只按需引入并统一主题变量，不为每个按钮再包BaseButton；只在统一无障碍行为有价值时封装。学生内容区阅读宽度建议约60–75字符、行高约1.5–1.7，作为项目初始参数交由用户测试调整，不作为所有学生的唯一偏好。

## 10. 后续作业与心情的专项规则

### 10.1 作业阶段P2

草稿保存与正式提交必须两种明确操作。30秒仅在内容改变时保存，记录最新已确认的服务端版本/时间；请求中继续修改要排队最新版本或使用版本校验，不能旧响应覆盖新草稿。离网显示Unsaved changes；不能自动把附件/正文永久保存在共享浏览器里。

正式提交前显示作业名称、附件和截止时间；禁重复按钮与后端幂等共同保护。重交显示版本；导师批改草稿与发布反馈分开；学生只见本人已发布反馈。管理员管理作业权限按确认后的G5契约，不复用课程管理员权限读取全部提交内容。界面临时预览不能绕过私有附件下载权限。

### 10.2 心情阶段P3

每周记录频率来自用户后续要求，不能同时在界面写Daily mood。周边界和修改窗口先确认，服务端唯一约束及鉴权负责一致性。记录“每周离散变化”，不画成连续实时检测；不插值缺失周，不用streak羞辱学生。默认本人可见，分享选择具体导师、明确解释范围，并能撤销；界面与后端都不能因为导师有授课关系就默认读取。

心情正文不进入日志、埋点、URL、默认导出或全局store。图表同时有日期/心情文字表格；首版可用简单SVG折线加表格，不先引入大型图表框架；等级是自我报告的类别，不能暗示临床分值或自动风险诊断。FR-58提醒需独立规则、误判处理和权限讨论后再做。

## 11. 测试、启动与提交标准

### 11.1 版本与构建

只有一个npm锁文件；P0记录实际精确版本和Node/npm来源。开发脚本拟定dev/build/typecheck/lint/test/test:e2e，实施时与实际package.json一致，不把计划命令写成已经可执行。VS Code使用项目本地工具；前端与Java分别启动，避免F5误指向空页面。Java8与Node运行时独立，不能把Java语言服务器的Java21当后端运行版本。

当前普通shell仅有Codex捆绑Node24.19.0而npm不可解析；先检查用户可复现环境，再配置所需开发工具。本轮未安装。依赖变更必须经过安装/类型/构建和核心用例验证，不能只把package.json中的latest换个数字。

### 11.2 测试必须验证行为

按[Vue测试指南](https://vuejs.org/guide/scaling-up/testing.html)组织关键纯逻辑和组件测试。重点HTTP500/code401、all权限、表单错误保留、上传失败、列表竞态和媒体有限重试，不测试每个按钮的实现细节或以大量快照替代行为。

使用[Playwright浏览器项目](https://playwright.dev/docs/test-projects)安排真实管理员/导师/学生流程。页面定位优先role/name/label，避免脆弱nth-child。测试token或账号由本地配置/受控测试环境提供，不提交密码。截图脱敏，日志不记录凭证；模拟接口仅用于组件边界测试，完整验收必须连接真实后端与数据库。

Chrome、Edge、Firefox实测分别记录版本和结果。只跑Chromium不宣称完成全部浏览器；系统TTS、实际MP4编码、WebVTT和键盘需真实手动检查。自动无障碍扫描可补充，但不能替代字幕内容、认知负担和操作可理解性检查。

### 11.3 每个功能的完成定义

- [ ] 对应FR或用户需求编号明确，页面/API/权限/数据范围有映射。
- [ ] 控件有真实行为，无console未处理异常或失败请求被忽略。
- [ ] 创建/编辑后真实保存，刷新、重登可回读；失败不显示成功。
- [ ] 加载、空、错误、无权限、会话过期、重复提交状态完整。
- [ ] 无散落用户可见中文/硬编码角色标识，语言键可追踪。
- [ ] 无token/密码/心情泄漏到URL、日志、前端环境变量或截图。
- [ ] 键盘、焦点、标签、对比、字号/重排和相关媒体支持已手测。
- [ ] 权限除了隐藏按钮，还测试地址直达、改ID、旧token/媒体ticket及撤销后访问。
- [ ] 类型/构建/lint和必要测试通过，未测限制明确记录。
- [ ] 无跨模块私有import、HTTP/store循环依赖、重复请求封装、万能页面组件。
- [ ] README启动方式和接口文档同步，演示数据标注，临时垃圾清理。

提交说明写“问题、行为变化、验证、限制”，不只写“优化代码”。完成P1只能标P1范围通过，不能把报告61FR勾成全部完成。

## 12. 维护和审查示例

| 变更 | 应只影响的范围 | 如果出现以下现象需重新检查耦合 |
|---|---|---|
| 新增一种课程发布提示 | learning语言资源与对应页面/类型（后端必要契约另算） | 需要改登录store、全局表格和三个无关模块 |
| 后端分页从pageNum对齐current | shared契约、API参数、分页composable、相关旧调用和测试 | 每个页面都有自己的字符串拼接和业务code判断 |
| 新增第二语言 | i18n资源、locale配置和测试 | 修改几十个业务组件中的散落英文 |
| 更换会议链接显示样式 | PostEditor/帖子展示及局部样式 | 播放器、账号模块、全局router都要改 |
| 新增作业附件上传 | assignment API及规则，复用基础选择/进度能力 | 为复用教学上传而授予学生learning:resource:upload权限 |
| 修改心情分享权限 | mood契约、对应界面和服务端范围校验 | 修改课程成员列表就自动向全部导师公开心情 |

新增功能按同样顺序：定位需求 → 查实际接口 → 定页面状态与权限 → 页面内实现最小闭环 → 出现稳定复用再提取 → 真实浏览器验收 → 更新计划与文档。重构必须有实际可维护性收益，不能为了目录对称把简单功能拆成十层。

## 附录. 当前源码证据位置

独立根目录 `C:\Users\eggma\Desktop\codex\up-self`：

| 路径 | 本准则据此确认 |
|---|---|
| `up-self-admin/src/main/resources/static/index.html`、`app.js`、`style.css` | 当前只是账号页；尚无Vue学习交互；现有sessionStorage方案 |
| `up-self-admin/src/main/java/com/upself/web/controller/system/SysUserController.java`、`up-self-admin/src/main/java/com/upself/web/model/AuthBodies.java` | 单角色账号操作、all权限、密码字段约束 |
| `up-self-admin/src/main/java/com/upself/web/controller/learning/LearningController.java`、`LearningMediaController.java` | pageNum差异、学习接口、multipart上传、媒体字节响应 |
| `up-self-biz/src/main/java/com/upself/biz/learning/dto/LearningBodies.java`及`domain/Course.java` | 字段/状态/长度、课程难度和预计时长尚缺 |
| `up-self-biz/src/main/java/com/upself/biz/learning/service/impl/LearningServiceImpl.java`、`service/LearningFileStorage.java` | 课程归属、资源发布、作者限制、文件校验与私有存储 |
| `up-self-biz/src/main/resources/mapper/learning/LearningMapper.xml` | 角色/加入过滤与数据查询 |
| `up-self-framework/src/main/java/com/upself/framework/web/service/TokenService.java`、`LearningMediaService.java` | Redis会话、channel、媒体ticket范围及不续期 |
| `up-self-common/src/main/java/com/upself/common/core/domain/AjaxResult.java` | code/msg/data，特殊响应仍在顶层 |
| `up-self-admin/src/main/resources/db/draft/V3__learning_and_english_permissions.sql` | 实际learning:post:*等权限，不沿用历史announcement:*草案名称 |

参考根目录 `C:\Users\eggma\Desktop\project\fts_admin_ac`：

- `ac-fts-common/src/main/java/com/saas/common/core/controller/BaseController.java`
- `ac-fts-common/src/main/java/com/saas/common/core/page/TableSupport.java`
- `ac-fts-common/src/main/java/com/saas/common/core/page/TableDataInfo.java`

参考项目没有前端，准则中的Vue工程是明确的新建议。以上源码核查为只读，所有实施项待计划确认后执行。
