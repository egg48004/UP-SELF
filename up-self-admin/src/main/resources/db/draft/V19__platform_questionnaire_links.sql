-- Platform-wide external questionnaires. No course, enrollment or answer table.
CREATE TABLE questionnaire_link (
 id BIGINT NOT NULL AUTO_INCREMENT,
 created_by BIGINT NOT NULL,
 title VARCHAR(150) NOT NULL,
 url VARCHAR(2000) NOT NULL,
 visible TINYINT(1) NOT NULL DEFAULT 0,
 version INT NOT NULL DEFAULT 0,
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 PRIMARY KEY(id), KEY idx_questionnaire_visible(visible,id), KEY idx_questionnaire_author(created_by,id),
 FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1700,0,'Questionnaires','','F','questionnaires:read',1),
 (1701,0,'Manage questionnaires','','F','questionnaires:manage',1);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1700),(20,1701);
