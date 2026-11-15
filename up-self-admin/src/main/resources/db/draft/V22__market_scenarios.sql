-- Teacher-authored decisions. No random demand, prices or grades.
CREATE TABLE market_task (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, course_id BIGINT NOT NULL,
 title VARCHAR(150) NOT NULL, objective VARCHAR(300) NOT NULL,
 situation VARCHAR(2000) NOT NULL, question VARCHAR(300) NOT NULL,
 reflection_prompt VARCHAR(300) NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT', version INT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_market_course(course_id,status,id),
 FOREIGN KEY(course_id) REFERENCES learning_course(id), FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE market_option (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT NOT NULL,
 position INT NOT NULL, label VARCHAR(300) NOT NULL,
 outcome VARCHAR(1000) NOT NULL, explanation VARCHAR(1000) NOT NULL,
 UNIQUE KEY uk_market_position(task_id,position),
 FOREIGN KEY(task_id) REFERENCES market_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE market_attempt (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL, option_id BIGINT NOT NULL, request_key VARCHAR(36) NOT NULL,
 reflection VARCHAR(1000) NOT NULL DEFAULT '', version INT NOT NULL DEFAULT 0,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_market_request(task_id,student_id,request_key),
 KEY idx_market_student(task_id,student_id,id),
 FOREIGN KEY(task_id) REFERENCES market_task(id), FOREIGN KEY(student_id) REFERENCES sys_user(id),
 FOREIGN KEY(option_id) REFERENCES market_option(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1900,0,'Market practice','','F','market:own',1),
 (1901,0,'Manage market practice','','F','market:manage',1);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1900),(20,1901);
