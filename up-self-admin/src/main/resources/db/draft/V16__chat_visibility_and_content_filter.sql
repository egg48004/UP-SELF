-- Existing messages have no reliable role-at-send evidence. Keep them visible.
ALTER TABLE chat_message ADD COLUMN sender_role VARCHAR(16) NOT NULL DEFAULT 'LEGACY' AFTER sender_id;
CREATE TABLE chat_hidden_peer (
 group_id BIGINT NOT NULL, viewer_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(group_id,viewer_id,student_id),
 FOREIGN KEY(group_id) REFERENCES chat_group(id) ON DELETE CASCADE,
 FOREIGN KEY(viewer_id) REFERENCES sys_user(id) ON DELETE CASCADE,
 FOREIGN KEY(student_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num)
 VALUES(1103,1100,'Hide peer messages','','F','chat:hide',3);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1103);
