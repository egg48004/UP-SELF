CREATE TABLE personal_goal (
 id BIGINT NOT NULL AUTO_INCREMENT,
 student_id BIGINT NOT NULL,
 title VARCHAR(150) NOT NULL,
 details VARCHAR(2000) NOT NULL DEFAULT '',
 status VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
 completed_at DATETIME(6) NULL,
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 version INT NOT NULL DEFAULT 0,
 PRIMARY KEY(id), KEY idx_goal_student_status(student_id,status,id),
 FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num)
 VALUES(700,300,'My goals','goals','C','goals:own',6);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,700);
