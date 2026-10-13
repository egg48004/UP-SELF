# 一、开发顺序

我建议按照以下顺序开发，而不是按照 FR 编号从 FR-01 一路实现到 FR-61。这样可以先打通数据库、课程展示和学习进度，再逐步实现其他模块，降低开发难度。

## 阶段 1：项目基础设施

**当前阶段**

先完成项目配置、数据库连接、Domain、Mapper 等基础工作。

- Spring Boot 启动与 MySQL 数据库连接
- MyBatis-Plus 配置
- 13 个 Domain 实体类
- Mapper 接口与基础数据库操作
- 统一返回结果与基础异常处理

**阶段目标：** 确保项目能够正常启动，并能够读写数据库，为后续业务功能开发做好准备。

## 阶段 2：学习模块（Learning Module）

**对应需求：FR-01～FR-10**

- 课程列表与课程搜索
- 课程详情展示，包括课程描述、难度等级和预计学习时间
- 章节与学习内容展示
- 继续学习功能
- 学习进度记录与更新
- 学习历史查询
- 课程收藏与取消收藏
- 无障碍设置
- 文本转语音（Text-to-Speech）

**阶段目标：** 优先打通“课程 → 章节 → 学习内容 → 学习进度”的完整流程，确保学生能够浏览课程、学习内容并记录学习进度。

## 阶段 3：作业模块（Assignment Module）

**对应需求：FR-11～FR-18**

- 查看作业与截止时间
- 提交作业
- 上传作业附件
- 截止日期前重新提交作业
- 查看评分与导师反馈
- 创建、编辑和删除作业
- 批改作业并提供反馈

**阶段目标：** 实现完整的作业生命周期，包括作业发布、学生提交、导师批改和反馈查看。

**注意事项：** 每次提交都应保留独立的提交记录，不能直接覆盖以前的提交，以便追踪学生的作业修改历史。

## 阶段 4：自我效能模块（Self-Efficacy Module）

**对应需求：FR-19～FR-27**

- 每日签到
- 连续签到记录与统计
- 每日心情记录
- 心情历史查询
- 创建、编辑和完成个人学习目标
- 信心评估
- 成就徽章发放与查看
- 学习数据统计与趋势分析

**阶段目标：** 先实现签到、心情记录和学习目标等基础功能，再逐步扩展信心评估、成就徽章和学习数据分析。

## 阶段 5：沟通模块（Communication Module）

**对应需求：FR-28～FR-35**

- 发送与接收私聊消息
- 聊天文件上传
- 创建群聊
- 管理群聊成员
- 屏蔽用户
- 举报不当行为
- 自动过滤不当内容

**阶段目标：** 先建立消息、群组和成员关系的基础结构，再实现用户屏蔽、举报与内容过滤等功能。

## 阶段 6：创业学习模块（Entrepreneurship Module）

**对应需求：FR-43～FR-48**

- 查看创业学习内容
- 使用商业创意模板
- 查看创业者案例
- 使用成本与利润计算器
- 参与市场模拟
- 接收同伴对商业创意的反馈

**阶段目标：** 优先实现创业内容展示、商业创意模板和成本计算器，再扩展市场模拟与同伴反馈功能。

## 阶段 7：管理与导师支持模块（Administration & Mentorship Support Modules）

**对应需求：FR-49～FR-61**

### 7.1 管理模块（Administration Module）

- 管理学生账号
- 管理导师账号
- 分配和管理用户角色
- 管理课程与学习资源
- 管理作业
- 导出系统数据与报表
- 配置默认无障碍设置

### 7.2 导师支持模块（Mentorship Support Module）

- 查看学生总览仪表板
- 监控学生学习进度
- 接收高风险学生提醒
- 向多名学生发送鼓励消息
- 添加私密导师备注
- 安排一对一导师辅导

**阶段目标：** 复用前面已经开发完成的课程、作业、签到、心情和学习进度数据，为管理员和导师提供统一的管理与支持功能。

## 二、总体开发原则

1. **先基础，后业务：** 优先完成项目配置、数据库结构和基础数据访问层，再开发具体业务功能。
2. **先核心，后扩展：** 优先实现课程学习、学习进度和作业提交等核心流程，再开发辅助功能。
3. **按业务依赖开发：** 先实现基础数据及其关联关系，再开发依赖这些数据的统计、管理和分析功能。
4. **逐步测试：** 每个阶段完成后，进行接口测试和必要的集成测试，确保已有功能正常运行后再进入下一阶段。
5. **避免重复开发：** 管理员和导师模块尽量复用已有的业务逻辑与数据查询接口。

通过以上顺序，可以逐步构建一个完整的 UP-SELF 平台，同时控制每个阶段的开发复杂度。

---

# 三、Functional Requirements

## 5.4.1.1 Learning Module

**Table 5.1 Learning Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-01 | View Course | Allows students to browse available learning courses. | Student |
| FR-02 | Search Course | Allows students to search for courses using keywords. | Student |
| FR-03 | View Course Details | Allows students to view course descriptions, difficulty levels, and estimated learning time. | Student |
| FR-04 | Continue Learning | Allows students to resume learning from the last accessed chapter. | Student |
| FR-05 | Track Learning Progress | Allows the system to automatically record and update learning progress. | Student |
| FR-06 | View Learning History | Allows students to review previously completed learning activities. | Student |
| FR-07 | Bookmark Course | Allows students to save courses for future reference. | Student |
| FR-08 | Manage Bookmark | Allows students to add or remove bookmarked courses. | Student |
| FR-09 | Text-to-Speech | Allows learning content to be read aloud. | Student |
| FR-10 | Configure Accessibility Settings | Allows students to customize font size, contrast, and reading preferences. | Student |

## 5.4.1.2 Assignment Module

**Table 5.2 Assignment Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-11 | View Assignment | Allows students to view assignments and deadlines. | Student |
| FR-12 | Submit Assignment | Allows students to submit completed assignments. | Student |
| FR-13 | Upload Answer File | Allows students to upload assignment files. | Student |
| FR-14 | Resubmit Assignment | Allows students to resubmit assignments before the deadline. | Student |
| FR-15 | View Assignment Feedback | Allows students to view grades and mentor comments. | Student |
| FR-16 | Create Assignment | Allows mentors to create assignments. | Mentor |
| FR-17 | Manage Assignment | Allows mentors to edit or remove assignments. | Mentor |
| FR-18 | Grade Assignment | Allows mentors to evaluate submissions and provide feedback. | Mentor |

## 5.4.1.3 Self-Efficacy Module

**Table 5.3 Self-Efficacy Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-19 | Daily Check-In | Allows students to perform daily check-ins. | Student |
| FR-20 | View Check-In Streak | Allows students to view consecutive check-in records. | Student |
| FR-21 | Record Daily Mood | Allows students to record daily mood status. | Student |
| FR-22 | View Mood History | Allows students to review previous mood records. | Student |
| FR-23 | Create Goal | Allows students to create personal learning goals. | Student |
| FR-24 | Manage Goal | Allows students to update, edit, or complete goals. | Student |
| FR-25 | Confidence Assessment | Allows students to complete self-confidence assessments. | Student |
| FR-26 | Earn Badge | Allows the system to award achievement badges. | System |
| FR-27 | View Learning Analytics | Allows students to view learning statistics and trends. | Student |

## 5.4.1.4 Communication Module

**Table 5.4 Communication Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-28 | Send Message | Allows users to send messages. | Student, Mentor |
| FR-29 | Receive Message | Allows users to receive messages. | Student, Mentor |
| FR-30 | Upload File in Chat | Allows users to share files through chat. | Student, Mentor |
| FR-31 | Create Group Chat | Allows mentors to create discussion groups. | Mentor |
| FR-32 | Manage Group Members | Allows mentors to add or remove members. | Mentor |
| FR-33 | Block User | Allows students to block unwanted users. | Student |
| FR-34 | Report User | Allows users to report inappropriate behavior. | Student, Mentor |
| FR-35 | Filter Inappropriate Content | Allows the system to automatically filter harmful content. | System |

## 5.4.1.5 AI Assistance Module

**Table 5.5 AI Assistance Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-36 | Ask Learning Questions | Allows students to ask learning-related questions. | Student |
| FR-37 | Generate Learning Summary | Allows AI to generate content summaries. | Student |
| FR-38 | Explain Difficult Concepts | Allows AI to explain difficult concepts. | Student |
| FR-39 | Generate Practice Questions | Allows AI to generate practice questions. | Student |
| FR-40 | Provide Assignment Guidance | Allows AI to provide assignment assistance. | Student |
| FR-41 | Generate Encouragement Messages | Allows AI to generate motivational messages. | Student |
| FR-42 | Recommend Learning Goals | Allows AI to recommend personalized goals. | Student |

## 5.4.1.6 Entrepreneurship Module

**Table 5.6 Entrepreneurship Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-43 | View Entrepreneurship Content | Allows students to access entrepreneurship learning materials. | Student |
| FR-44 | Use Business Idea Template | Allows students to develop business ideas through guided templates. | Student |
| FR-45 | View Entrepreneur Stories | Allows students to learn from entrepreneur case studies. | Student |
| FR-46 | Use Cost Calculator | Allows students to estimate business costs and profits. | Student |
| FR-47 | Participate in Market Simulation | Allows students to practice entrepreneurship skills. | Student |
| FR-48 | Receive Peer Feedback | Allows students to receive peer feedback on business ideas. | Student |

## 5.4.1.7 Administration Module

**Table 5.7 Administration Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-49 | Manage Students | Allows administrators to manage student accounts. | Administrator |
| FR-50 | Manage Mentors | Allows administrators to manage mentor accounts. | Administrator |
| FR-51 | Manage User Roles | Allows administrators to assign user roles. | Administrator |
| FR-52 | Manage Courses | Allows administrators to manage courses and resources. | Administrator |
| FR-53 | Manage Assignments | Allows administrators to manage assignments. | Administrator |
| FR-54 | Export System Data | Allows administrators to export system reports. | Administrator |
| FR-55 | Configure Accessibility Settings | Allows administrators to configure default accessibility options. | Administrator |

## 5.4.1.8 Mentorship Support Module

**Table 5.8 Mentorship Support Module Functional Requirements Table**

| Feature ID | Feature | Description | Actor/Role |
|---|---|---|---|
| FR-56 | View Student Overview Dashboard | Allows mentors to view student progress, mood, and assignments. | Mentor |
| FR-57 | Monitor Student Progress | Allows mentors to monitor student learning activities. | Mentor |
| FR-58 | Receive At-Risk Student Alerts | Allows the system to notify mentors of at-risk students. | Mentor |
| FR-59 | Send Bulk Encouragement Messages | Allows mentors to send encouragement messages to multiple students. | Mentor |
| FR-60 | Add Mentor Notes | Allows mentors to record private observations about students. | Mentor |
| FR-61 | Schedule One-on-One Check-In | Allows mentors and students to schedule mentoring sessions. | Mentor |