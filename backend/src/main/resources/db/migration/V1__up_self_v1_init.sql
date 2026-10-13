-- ============================================================
-- UP-SELF 第一版核心数据库初始化脚本
-- 数据库：up_self
-- 技术栈：MySQL 8.0 + Spring Boot + MyBatis-Plus
--
-- 目标：
--   1) 建立第一版核心用户、学习、作业、自我效能数据结构
--   2) 保留章节级学习状态与作业每次提交的历史
--   3) 采用最小可用结构，不提前建立聊天、AI 对话、课程版本等扩展表
--
-- 执行前检查：
--   - 本脚本用于新建开发数据库，不是现有生产库的升级脚本。
--   - CREATE TABLE IF NOT EXISTS 只避免重复创建同名表，不会自动修复已有表结构。
--   - 请先确认当前连接的 MySQL 实例和账号权限，再执行。
--   - 外键约束用于保护关联数据；如需删除业务数据，应先明确关联记录的处理规则。
--
-- 重要业务约定（第一版设计建议，非全部来自需求文档的强制规定）：
--   - 用户角色暂定 STUDENT / ADMIN；导师职责由 ADMIN 兼任。
--   - 课程层级：course -> chapter -> learning_content。
--   - 学生每个章节仅保留一条当前进度记录；完成后保留 completed_at。
--   - 课程进度由当前有效章节和章节完成状态计算，不冗余保存百分比。
--   - 已发布章节优先下架，不直接物理删除；下架章节不计入当前课程进度。
--   - assignment_submission 每一行代表一次提交/草稿；重复提交不覆盖旧记录。
--   - 本脚本不建立课程版本、聊天、AI 对话、徽章、预约等扩展表。
--
-- 需求依据：
--   - FR-01 至 FR-10：课程浏览、详情、继续学习、学习进度、学习历史、
--     收藏、TTS 与无障碍偏好。
--   - FR-11 至 FR-18：作业查看、提交、上传、重新提交、评分和反馈。
--   - FR-19 至 FR-27：签到、心情、目标及相关记录。
--   - NFR-05 / NFR-06：登录认证与角色权限（由应用层实现）。
--   - NFR-07：作业草稿自动保存（由应用层定时保存实现）。
--   - NFR-08：学习进度持久化。
--
-- 幂等说明：
--   - 数据库使用 CREATE DATABASE IF NOT EXISTS。
--   - 表使用 CREATE TABLE IF NOT EXISTS。
--   - 若同名表已经存在，MySQL 不会比较或补齐其中的字段、索引和约束。
--   - 若后续要修改已存在表，应另建版本化迁移脚本，不要依赖本初始化脚本升级。
--
-- 回滚提示：
--   - 本脚本不包含 DROP DATABASE / DROP TABLE，避免误删已有数据。
--   - 如需清理本地测试库，请确认数据库名称和数据后，再手动制定清理脚本。
-- ============================================================

CREATE DATABASE IF NOT EXISTS `up_self`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `up_self`;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` VARCHAR(50) NOT NULL COMMENT '登录用户名',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希，不存明文密码',
  `display_name` VARCHAR(100) NOT NULL COMMENT '展示名称',
  `email` VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
  `role` VARCHAR(20) NOT NULL COMMENT '角色：STUDENT/ADMIN',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '账号状态：active/disabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  UNIQUE KEY `uk_sys_user_email` (`email`),
  KEY `idx_sys_user_role_status` (`role`, `status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='系统用户：学生与管理员';

-- ============================================================
-- 2. 课程表
-- ============================================================
CREATE TABLE IF NOT EXISTS `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '课程主键',
  `title` VARCHAR(200) NOT NULL COMMENT '课程标题',
  `description` TEXT DEFAULT NULL COMMENT '课程描述',
  `difficulty` VARCHAR(30) DEFAULT NULL COMMENT '难度等级',
  `estimated_minutes` INT DEFAULT NULL COMMENT '预计学习时长（分钟）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/unavailable',
  `created_by` BIGINT NOT NULL COMMENT '创建管理员用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_course_status_deleted` (`status`, `deleted`),
  KEY `idx_course_created_by` (`created_by`),
  CONSTRAINT `fk_course_creator`
    FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程';

-- ============================================================
-- 3. 章节表
-- ============================================================
CREATE TABLE IF NOT EXISTS `chapter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节主键',
  `course_id` BIGINT NOT NULL COMMENT '所属课程ID',
  `title` VARCHAR(200) NOT NULL COMMENT '章节标题',
  `description` TEXT DEFAULT NULL COMMENT '章节描述',
  `sort_order` INT NOT NULL DEFAULT 1 COMMENT '章节在课程中的排序值',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/unavailable',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_chapter_course_order` (`course_id`, `sort_order`),
  KEY `idx_chapter_course_status` (`course_id`, `status`, `deleted`),
  CONSTRAINT `fk_chapter_course`
    FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='课程章节';

-- ============================================================
-- 4. 教学内容表
-- 一条记录表示章节中的一个内容单元；图片/音频等文件先存资源地址。
-- ============================================================
CREATE TABLE IF NOT EXISTS `learning_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '教学内容主键',
  `chapter_id` BIGINT NOT NULL COMMENT '所属章节ID',
  `title` VARCHAR(200) NOT NULL COMMENT '内容标题',
  `content_type` VARCHAR(30) NOT NULL COMMENT '内容类型，如 text/image/audio/video',
  `content_text` TEXT DEFAULT NULL COMMENT '文本内容或说明',
  `resource_url` VARCHAR(1000) DEFAULT NULL COMMENT '媒体资源地址',
  `sort_order` INT NOT NULL DEFAULT 1 COMMENT '章节内排序值',
  `status` VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '内容状态',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_content_chapter_order` (`chapter_id`, `sort_order`),
  KEY `idx_content_chapter_status` (`chapter_id`, `status`, `deleted`),
  CONSTRAINT `fk_content_chapter`
    FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='章节教学内容单元';

-- ============================================================
-- 5. 学生章节进度表
-- 唯一约束确保同一学生对同一章节只有一条当前状态。
-- last_accessed_at 可用于恢复上次访问的章节。
-- ============================================================
CREATE TABLE IF NOT EXISTS `chapter_progress` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '章节进度主键',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `chapter_id` BIGINT NOT NULL COMMENT '章节ID',
  `is_completed` TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：0=否，1=是',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `last_accessed_at` DATETIME DEFAULT NULL COMMENT '最近访问时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_chapter` (`student_id`, `chapter_id`),
  KEY `idx_progress_student_last_access` (`student_id`, `last_accessed_at`),
  KEY `idx_progress_chapter` (`chapter_id`),
  CONSTRAINT `fk_progress_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_progress_chapter`
    FOREIGN KEY (`chapter_id`) REFERENCES `chapter` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生章节当前学习状态';

-- ============================================================
-- 6. 课程收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS `course_bookmark` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏主键',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course_bookmark` (`student_id`, `course_id`),
  KEY `idx_bookmark_course` (`course_id`),
  CONSTRAINT `fk_bookmark_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_bookmark_course`
    FOREIGN KEY (`course_id`) REFERENCES `course` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生课程收藏';

-- ============================================================
-- 7. 用户无障碍偏好表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_preferences` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '偏好主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `font_size` VARCHAR(20) NOT NULL DEFAULT 'medium' COMMENT '字体大小：small/medium/large',
  `high_contrast` TINYINT NOT NULL DEFAULT 0 COMMENT '高对比度：0=关闭，1=开启',
  `reading_preferences` JSON DEFAULT NULL COMMENT '其他阅读偏好；仅保存已确认需要的设置',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_preferences_user` (`user_id`),
  CONSTRAINT `fk_preferences_user`
    FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户无障碍与阅读偏好';

-- ============================================================
-- 8. 作业表
-- ============================================================
CREATE TABLE IF NOT EXISTS `assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '作业主键',
  `title` VARCHAR(200) NOT NULL COMMENT '作业标题',
  `description` TEXT DEFAULT NULL COMMENT '作业描述',
  `instructions` TEXT DEFAULT NULL COMMENT '作业要求',
  `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/published/unavailable',
  `created_by` BIGINT NOT NULL COMMENT '创建管理员用户ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删除，1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_assignment_status_deadline` (`status`, `deadline`),
  KEY `idx_assignment_creator` (`created_by`),
  CONSTRAINT `fk_assignment_creator`
    FOREIGN KEY (`created_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='作业';

-- ============================================================
-- 9. 作业提交表
-- 每次提交/草稿独立保存；评分与反馈对应到具体提交。
-- attempt_no 由后端在事务中分配，不应直接信任客户端传值。
-- ============================================================
CREATE TABLE IF NOT EXISTS `assignment_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交记录主键',
  `assignment_id` BIGINT NOT NULL COMMENT '作业ID',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `attempt_no` INT NOT NULL COMMENT '该学生该作业的提交轮次',
  `answer_text` TEXT DEFAULT NULL COMMENT '文本答案',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态：draft/submitted/graded',
  `submitted_at` DATETIME DEFAULT NULL COMMENT '正式提交时间',
  `grade` DECIMAL(6,2) DEFAULT NULL COMMENT '该次提交的得分',
  `feedback` TEXT DEFAULT NULL COMMENT '该次提交的评分反馈',
  `graded_by` BIGINT DEFAULT NULL COMMENT '评分管理员用户ID',
  `graded_at` DATETIME DEFAULT NULL COMMENT '评分时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_student_attempt` (`assignment_id`, `student_id`, `attempt_no`),
  KEY `idx_submission_student_assignment` (`student_id`, `assignment_id`),
  KEY `idx_submission_assignment_status` (`assignment_id`, `status`),
  CONSTRAINT `fk_submission_assignment`
    FOREIGN KEY (`assignment_id`) REFERENCES `assignment` (`id`),
  CONSTRAINT `fk_submission_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`),
  CONSTRAINT `fk_submission_grader`
    FOREIGN KEY (`graded_by`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='作业提交历史与对应评分反馈';

-- ============================================================
-- 10. 作业提交文件表
-- 文件本体存储在文件存储服务/磁盘中，数据库保存路径与元数据。
-- ============================================================
CREATE TABLE IF NOT EXISTS `submission_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交文件主键',
  `submission_id` BIGINT NOT NULL COMMENT '所属提交记录ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_url` VARCHAR(1000) NOT NULL COMMENT '文件存储地址',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT '媒体类型',
  `uploaded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_submission_file_submission` (`submission_id`),
  CONSTRAINT `fk_submission_file_submission`
    FOREIGN KEY (`submission_id`) REFERENCES `assignment_submission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='作业提交附件';

-- ============================================================
-- 11. 每日签到表
-- ============================================================
CREATE TABLE IF NOT EXISTS `daily_checkin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到主键',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `checkin_date` DATE NOT NULL COMMENT '签到日期（按应用约定的时区计算）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_checkin_date` (`student_id`, `checkin_date`),
  CONSTRAINT `fk_checkin_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生每日签到';

-- ============================================================
-- 12. 心情记录表
-- 心情属于个人记录，应限制学生只能访问自己的记录。
-- ============================================================
CREATE TABLE IF NOT EXISTS `mood_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '心情记录主键',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `mood_value` VARCHAR(30) NOT NULL COMMENT '心情选项编码',
  `note` TEXT DEFAULT NULL COMMENT '学生自愿填写的备注',
  `recorded_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_mood_student_time` (`student_id`, `recorded_at`),
  CONSTRAINT `fk_mood_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生心情历史记录';

-- ============================================================
-- 13. 个人目标表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_goal` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '目标主键',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `title` VARCHAR(200) NOT NULL COMMENT '目标标题',
  `description` TEXT DEFAULT NULL COMMENT '目标描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/completed/cancelled',
  `target_date` DATE DEFAULT NULL COMMENT '目标日期',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_goal_student_status` (`student_id`, `status`),
  CONSTRAINT `fk_goal_student`
    FOREIGN KEY (`student_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='学生个人目标';

-- ============================================================
-- 初始化完成
--
-- 建议的执行后检查：
--   SHOW TABLES;
--   SHOW CREATE TABLE `chapter_progress`;
--   SHOW CREATE TABLE `assignment_submission`;
--
-- 应用层仍需实现：
--   1) 用户角色授权与学生数据隔离；
--   2) 章节访问时更新 last_accessed_at；
--   3) 点击完成时更新 is_completed / completed_at；
--   4) 课程进度只统计当前有效章节；
--   5) 下架章节不通过正常课程入口访问；
--   6) 作业草稿自动保存与提交轮次的事务安全分配；
--   7) 图片、音频及作业文件的上传、访问权限与存储策略。
-- ============================================================
