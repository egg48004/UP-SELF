CREATE TABLE encouragement_message (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 mentor_id BIGINT NOT NULL, body VARCHAR(2000) NOT NULL,
 client_key VARCHAR(64) NOT NULL, recipient_count INT NOT NULL,
 sent_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_encouragement_send(mentor_id,client_key),
 FOREIGN KEY(mentor_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE encouragement_recipient (
 message_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 PRIMARY KEY(message_id,student_id), KEY idx_encouragement_inbox(student_id,message_id),
 FOREIGN KEY(message_id) REFERENCES encouragement_message(id),
 FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Function permissions only: these do not add main navigation entries.
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1500,0,'Send encouragement','','F','mentorship:encouragement:send',11),
 (1501,0,'Read own encouragement','','F','mentorship:encouragement:read',12);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,1500),(10,1501);
