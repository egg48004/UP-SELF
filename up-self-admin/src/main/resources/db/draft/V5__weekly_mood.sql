-- New independent feature; never edit an already executed migration.
CREATE TABLE mood_notice_ack (
 student_id BIGINT PRIMARY KEY,notice_version VARCHAR(30) NOT NULL,
 accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE mood_entry (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,week_start DATE NOT NULL,
 mood_level TINYINT NOT NULL,private_note TEXT NOT NULL,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_mood_student_week(student_id,week_start),
 FOREIGN KEY(student_id) REFERENCES mood_notice_ack(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (500,0,'My mood','mood','C','mood:own:read',4),
 (501,500,'Record weekly mood','','F','mood:own:record',1),
 (510,0,'Student mood','student-mood','C','mood:students:read',4);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,500),(10,501),(20,510);
