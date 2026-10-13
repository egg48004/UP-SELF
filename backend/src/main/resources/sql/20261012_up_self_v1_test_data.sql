-- ============================================================
-- UP-SELF 第一版测试数据
-- 适用数据库：MySQL 8.0
-- 前置条件：已执行 up_self_v1_init.sql
--
-- 说明：
-- 1. 使用 9001+ 的固定 ID，降低与普通自增数据冲突的概率。
-- 2. 脚本可重复执行；相同主键存在时更新测试数据。
-- 3. 所有数据均为虚构演示数据，不包含真实个人信息。
-- 4. password_hash 使用 TEST_ONLY 占位值，不能用于真实登录认证。
--    开发登录功能时，应通过 BCryptPasswordEncoder 生成密码哈希。
-- 5. 仅在本地开发数据库执行，不要用于生产环境。
-- ============================================================

USE `up_self`;

START TRANSACTION;

-- 1. 测试用户：一个学生、一个管理员（第一版由管理员兼任导师职责）
INSERT INTO `sys_user`
(`id`, `username`, `password_hash`, `display_name`, `email`, `role`, `status`, `deleted`)
VALUES
(9001, 'demo_student', 'TEST_ONLY_NOT_A_REAL_PASSWORD_HASH', 'Demo Student', 'student.demo@example.test', 'STUDENT', 'active', 0),
(9002, 'demo_admin',   'TEST_ONLY_NOT_A_REAL_PASSWORD_HASH', 'Demo Admin',   'admin.demo@example.test',   'ADMIN',   'active', 0)
ON DUPLICATE KEY UPDATE
`username` = VALUES(`username`),
`display_name` = VALUES(`display_name`),
`email` = VALUES(`email`),
`role` = VALUES(`role`),
`status` = VALUES(`status`),
`deleted` = VALUES(`deleted`);

-- 2. 课程
INSERT INTO `course`
(`id`, `title`, `description`, `difficulty`, `estimated_minutes`, `status`, `created_by`, `deleted`)
VALUES
(9001, '认识创业', '通过简单案例了解创业是什么，以及创业者如何解决生活中的问题。', 'beginner', 30, 'published', 9002, 0),
(9002, '我的第一个商业创意', '使用分步骤模板，把一个日常问题转化为初步商业创意。', 'beginner', 45, 'published', 9002, 0),
(9003, '基础成本与利润', '学习识别成本、收入与利润，并尝试进行简单估算。', 'intermediate', 40, 'draft', 9002, 0)
ON DUPLICATE KEY UPDATE
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`difficulty` = VALUES(`difficulty`),
`estimated_minutes` = VALUES(`estimated_minutes`),
`status` = VALUES(`status`),
`created_by` = VALUES(`created_by`),
`deleted` = VALUES(`deleted`);

-- 3. 章节
INSERT INTO `chapter`
(`id`, `course_id`, `title`, `description`, `sort_order`, `status`, `deleted`)
VALUES
(9001, 9001, '什么是创业？', '用生活中的例子认识创业。', 1, 'published', 0),
(9002, 9001, '发现身边的问题', '观察日常生活中的问题与需求。', 2, 'published', 0),
(9003, 9001, '认识创业者', '了解创业者如何尝试解决问题。', 3, 'published', 0),
(9004, 9002, '记录一个想法', '从自己感兴趣的事情开始。', 1, 'published', 0),
(9005, 9002, '谁会需要它？', '思考哪些人可能需要这个想法。', 2, 'draft', 0),
(9006, 9003, '认识成本和收入', '区分成本、收入和利润。', 1, 'published', 0)
ON DUPLICATE KEY UPDATE
`course_id` = VALUES(`course_id`),
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`sort_order` = VALUES(`sort_order`),
`status` = VALUES(`status`),
`deleted` = VALUES(`deleted`);

-- 4. 教学内容（文本、图片、音频示例；资源地址为演示值）
INSERT INTO `learning_content`
(`id`, `chapter_id`, `title`, `content_type`, `content_text`, `resource_url`, `sort_order`, `status`, `deleted`)
VALUES
(9001, 9001, '创业的简单定义', 'text', '创业可以从发现一个问题开始，再尝试提供有帮助的产品或服务。', NULL, 1, 'published', 0),
(9002, 9001, '生活问题示意图', 'image', '图片示例：展示一个生活问题及其可能的解决方式。', 'https://example.test/up-self/images/problem-solution.png', 2, 'published', 0),
(9003, 9001, '听一听创业例子', 'audio', '音频示例：用简短语言介绍一个创业案例。', 'https://example.test/up-self/audio/entrepreneur-example.mp3', 3, 'published', 0),
(9004, 9002, '观察练习说明', 'text', '想一想：你在学校、家里或社区遇到过什么不方便的事情？', NULL, 1, 'published', 0),
(9005, 9004, '商业创意记录提示', 'text', '先写下一个你想改善的小问题，不需要马上找到完美答案。', NULL, 1, 'published', 0),
(9006, 9006, '成本与收入示例', 'text', '如果制作一个物品花费 RM 5，卖出价格为 RM 8，暂不考虑其他费用时，差额为 RM 3。', NULL, 1, 'published', 0)
ON DUPLICATE KEY UPDATE
`chapter_id` = VALUES(`chapter_id`),
`title` = VALUES(`title`),
`content_type` = VALUES(`content_type`),
`content_text` = VALUES(`content_text`),
`resource_url` = VALUES(`resource_url`),
`sort_order` = VALUES(`sort_order`),
`status` = VALUES(`status`),
`deleted` = VALUES(`deleted`);

-- 5. 学生章节进度：第一章已完成，第二章访问过但未完成
INSERT INTO `chapter_progress`
(`id`, `student_id`, `chapter_id`, `is_completed`, `completed_at`, `last_accessed_at`)
VALUES
(9001, 9001, 9001, 1, '2026-10-10 10:00:00', '2026-10-10 10:00:00'),
(9002, 9001, 9002, 0, NULL, '2026-10-12 09:30:00')
ON DUPLICATE KEY UPDATE
`student_id` = VALUES(`student_id`),
`chapter_id` = VALUES(`chapter_id`),
`is_completed` = VALUES(`is_completed`),
`completed_at` = VALUES(`completed_at`),
`last_accessed_at` = VALUES(`last_accessed_at`);

-- 6. 课程收藏
INSERT INTO `course_bookmark` (`id`, `student_id`, `course_id`)
VALUES (9001, 9001, 9002)
ON DUPLICATE KEY UPDATE
`student_id` = VALUES(`student_id`),
`course_id` = VALUES(`course_id`);

-- 7. 无障碍偏好
INSERT INTO `user_preferences`
(`id`, `user_id`, `font_size`, `high_contrast`, `reading_preferences`)
VALUES
(9001, 9001, 'large', 1, JSON_OBJECT('preferredContentTypes', JSON_ARRAY('image', 'audio', 'text')))
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`font_size` = VALUES(`font_size`),
`high_contrast` = VALUES(`high_contrast`),
`reading_preferences` = VALUES(`reading_preferences`);

-- 8. 作业
INSERT INTO `assignment`
(`id`, `title`, `description`, `instructions`, `deadline`, `status`, `created_by`, `deleted`)
VALUES
(9001, '发现一个生活中的问题', '选择一个你想改善的日常问题。', '用几句话描述问题、可能受到影响的人，以及一个初步解决想法。', '2026-12-31 23:59:00', 'published', 9002, 0),
(9002, '我的商业创意草稿', '为你的创意整理一份简单草稿。', '说明你的创意、目标用户，以及它能带来的帮助。', '2027-01-31 23:59:00', 'draft', 9002, 0)
ON DUPLICATE KEY UPDATE
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`instructions` = VALUES(`instructions`),
`deadline` = VALUES(`deadline`),
`status` = VALUES(`status`),
`created_by` = VALUES(`created_by`),
`deleted` = VALUES(`deleted`);

-- 9. 作业提交：一条已提交记录、一条草稿记录
INSERT INTO `assignment_submission`
(`id`, `assignment_id`, `student_id`, `attempt_no`, `answer_text`, `status`, `submitted_at`, `grade`, `feedback`, `graded_by`, `graded_at`)
VALUES
(9001, 9001, 9001, 1, '学校午餐时间排队很长。可以尝试设计一个提前登记午餐的简单流程。', 'graded', '2026-10-11 14:00:00', 85.00, '想法描述清楚。下一步可以进一步说明谁会使用这个方案。', 9002, '2026-10-12 10:00:00'),
(9002, 9001, 9001, 2, '第二版：学生可以提前选择午餐，工作人员按登记数量准备。', 'submitted', '2026-10-12 11:00:00', NULL, NULL, NULL, NULL),
(9003, 9002, 9001, 1, '我想先记录一个帮助同学整理学习用品的想法。', 'draft', NULL, NULL, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
`assignment_id` = VALUES(`assignment_id`),
`student_id` = VALUES(`student_id`),
`attempt_no` = VALUES(`attempt_no`),
`answer_text` = VALUES(`answer_text`),
`status` = VALUES(`status`),
`submitted_at` = VALUES(`submitted_at`),
`grade` = VALUES(`grade`),
`feedback` = VALUES(`feedback`),
`graded_by` = VALUES(`graded_by`),
`graded_at` = VALUES(`graded_at`);

-- 10. 作业附件（仅元数据，URL 是演示值，不代表真实文件存在）
INSERT INTO `submission_file`
(`id`, `submission_id`, `file_name`, `file_url`, `file_size`, `mime_type`)
VALUES
(9001, 9001, 'business-idea.txt', 'https://example.test/up-self/uploads/business-idea.txt', 1024, 'text/plain')
ON DUPLICATE KEY UPDATE
`submission_id` = VALUES(`submission_id`),
`file_name` = VALUES(`file_name`),
`file_url` = VALUES(`file_url`),
`file_size` = VALUES(`file_size`),
`mime_type` = VALUES(`mime_type`);

-- 11. 签到：连续日期样例，供后续测试 streak 计算
INSERT INTO `daily_checkin` (`id`, `student_id`, `checkin_date`)
VALUES
(9001, 9001, '2026-10-10'),
(9002, 9001, '2026-10-11'),
(9003, 9001, '2026-10-12')
ON DUPLICATE KEY UPDATE
`student_id` = VALUES(`student_id`),
`checkin_date` = VALUES(`checkin_date`);

-- 12. 心情记录
INSERT INTO `mood_record` (`id`, `student_id`, `mood_value`, `note`, `recorded_at`)
VALUES
(9001, 9001, 'happy', '今天完成了一个学习章节。', '2026-10-10 18:00:00'),
(9002, 9001, 'calm', '今天按自己的节奏学习。', '2026-10-11 18:00:00'),
(9003, 9001, 'tired', '今天有点累，所以只完成了少量学习。', '2026-10-12 18:00:00')
ON DUPLICATE KEY UPDATE
`student_id` = VALUES(`student_id`),
`mood_value` = VALUES(`mood_value`),
`note` = VALUES(`note`),
`recorded_at` = VALUES(`recorded_at`);

-- 13. 学习目标
INSERT INTO `user_goal`
(`id`, `student_id`, `title`, `description`, `status`, `target_date`, `completed_at`)
VALUES
(9001, 9001, '每周完成两个学习章节', '按照自己的节奏完成课程内容。', 'active', '2026-10-31', NULL),
(9002, 9001, '完成第一个创业想法草稿', '记录问题、目标用户和初步解决办法。', 'completed', '2026-10-11', '2026-10-11 16:00:00')
ON DUPLICATE KEY UPDATE
`student_id` = VALUES(`student_id`),
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`status` = VALUES(`status`),
`target_date` = VALUES(`target_date`),
`completed_at` = VALUES(`completed_at`);

COMMIT;

-- 执行后检查
SELECT id, username, display_name, role FROM `sys_user` WHERE id IN (9001, 9002);
SELECT id, title, status FROM `course` WHERE id BETWEEN 9001 AND 9003 ORDER BY id;
SELECT id, course_id, title, sort_order, status FROM `chapter` WHERE id BETWEEN 9001 AND 9006 ORDER BY course_id, sort_order;
SELECT id, chapter_id, title, content_type FROM `learning_content` WHERE id BETWEEN 9001 AND 9006 ORDER BY chapter_id, sort_order;
SELECT * FROM `chapter_progress` WHERE student_id = 9001 ORDER BY last_accessed_at;
SELECT id, assignment_id, attempt_no, status, grade FROM `assignment_submission` WHERE student_id = 9001 ORDER BY assignment_id, attempt_no;
