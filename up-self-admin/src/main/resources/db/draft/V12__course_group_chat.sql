CREATE TABLE chat_group (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL, title VARCHAR(100) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', version INT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_chat_course(course_id,id),
 FOREIGN KEY(course_id) REFERENCES learning_course(id), FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE chat_member (
 group_id BIGINT NOT NULL, student_id BIGINT NOT NULL, active TINYINT(1) NOT NULL DEFAULT 1,
 joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(group_id,student_id), KEY idx_chat_student(student_id,active),
 FOREIGN KEY(group_id) REFERENCES chat_group(id), FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE chat_message (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, group_id BIGINT NOT NULL,
 sender_id BIGINT NOT NULL, body VARCHAR(2000) NOT NULL DEFAULT '',
 client_key VARCHAR(64) NOT NULL, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_chat_send(group_id,sender_id,client_key), KEY idx_chat_message(group_id,id),
 FOREIGN KEY(group_id) REFERENCES chat_group(id), FOREIGN KEY(sender_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE chat_file (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, group_id BIGINT NOT NULL, owner_id BIGINT NOT NULL,
 message_id BIGINT NULL, storage_key VARCHAR(50) NOT NULL, original_name VARCHAR(150) NOT NULL,
 mime_type VARCHAR(80) NOT NULL, size_bytes BIGINT NOT NULL, alt_text VARCHAR(200) NOT NULL DEFAULT '',
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_chat_file_message(message_id), KEY idx_chat_draft(group_id,owner_id,message_id),
 FOREIGN KEY(group_id) REFERENCES chat_group(id), FOREIGN KEY(owner_id) REFERENCES sys_user(id),
 FOREIGN KEY(message_id) REFERENCES chat_message(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1100,0,'Course discussions','discussions','C','chat:read',8),
 (1101,1100,'Send discussion messages','','F','chat:send',1),
 (1102,1100,'Manage discussion groups','','F','chat:manage',2);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1100),(10,1101),(20,1100),(20,1101),(20,1102);
