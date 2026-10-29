CREATE TABLE user_report (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, reporter_id BIGINT NOT NULL, accused_id BIGINT NOT NULL,
 message_id BIGINT NOT NULL, reporter_name VARCHAR(200) NOT NULL, accused_name VARCHAR(200) NOT NULL,
 course_title VARCHAR(150) NOT NULL, group_title VARCHAR(100) NOT NULL, message_body VARCHAR(2000) NOT NULL,
 attachment_summary TEXT NOT NULL, reason VARCHAR(24) NOT NULL, details VARCHAR(500) NOT NULL DEFAULT '',
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING', pending_key TINYINT NULL DEFAULT 1,
 resolution VARCHAR(1000) NOT NULL DEFAULT '', reviewed_by BIGINT NULL, reviewed_at DATETIME(6) NULL,
 version INT NOT NULL DEFAULT 0, create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_pending_report(reporter_id,message_id,pending_key), KEY idx_reports(status,id),
 FOREIGN KEY(reporter_id) REFERENCES sys_user(id), FOREIGN KEY(accused_id) REFERENCES sys_user(id), FOREIGN KEY(reviewed_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE follow_up_reminder (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, mentor_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 kind VARCHAR(20) NOT NULL, scope_key BIGINT NOT NULL, course_id BIGINT NULL, assignment_id BIGINT NULL,
 reason VARCHAR(300) NOT NULL, event_date VARCHAR(30) NOT NULL,
 active TINYINT NOT NULL DEFAULT 1, reviewed TINYINT NOT NULL DEFAULT 0, version INT NOT NULL DEFAULT 0,
 detected_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), reviewed_at DATETIME(6) NULL,
 UNIQUE KEY uk_follow_up(mentor_id,student_id,kind,scope_key), KEY idx_follow_up(mentor_id,active,reviewed),
 FOREIGN KEY(mentor_id) REFERENCES sys_user(id), FOREIGN KEY(student_id) REFERENCES sys_user(id),
 FOREIGN KEY(course_id) REFERENCES learning_course(id), FOREIGN KEY(assignment_id) REFERENCES assignment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1300,0,'My reports','my-reports','C','reports:own',11),
 (1301,0,'Reports','reports','C','reports:review',12),
 (1310,0,'Follow-up reminders','follow-ups','C','followups:read',13),
 (1320,0,'Export data','exports','C','exports:read',14);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1300),(20,1300),(30,1301),(20,1310),(30,1320);
