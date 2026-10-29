CREATE TABLE mentor_note (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, mentor_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 content VARCHAR(2000) NOT NULL, version INT NOT NULL DEFAULT 0,
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_note_owner(mentor_id,student_id,id),
 FOREIGN KEY(mentor_id) REFERENCES sys_user(id), FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE business_idea (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, student_id BIGINT NOT NULL,
 title VARCHAR(100) NOT NULL, offer VARCHAR(500) NOT NULL DEFAULT '', audience VARCHAR(500) NOT NULL DEFAULT '',
 resources VARCHAR(500) NOT NULL DEFAULT '', next_step VARCHAR(500) NOT NULL DEFAULT '', version INT NOT NULL DEFAULT 0,
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 KEY idx_idea_owner(student_id,id), FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE student_cost_plan (
 student_id BIGINT NOT NULL PRIMARY KEY, materials_cost DECIMAL(12,2) NOT NULL,
 other_costs DECIMAL(12,2) NOT NULL, quantity INT NOT NULL, price_per_item DECIMAL(12,2) NOT NULL,
 version INT NOT NULL DEFAULT 1, update_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
 FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1200,0,'Mentor notes','mentor-notes','C','mentorship:notes:read',9),
 (1201,1200,'Write mentor notes','','F','mentorship:notes:write',1),
 (1210,0,'Business practice','business','C','business:own',10);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,1200),(20,1201),(10,1210);
