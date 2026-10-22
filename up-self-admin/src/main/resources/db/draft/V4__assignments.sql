CREATE TABLE assignment (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,course_id BIGINT NOT NULL,title VARCHAR(150) NOT NULL,instructions MEDIUMTEXT NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',due_at DATETIME NULL,max_score INT NOT NULL DEFAULT 100,version INT NOT NULL DEFAULT 0,language_code VARCHAR(35) NOT NULL DEFAULT 'en',created_by BIGINT NOT NULL,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_assignment_course(course_id,status),FOREIGN KEY(course_id) REFERENCES learning_course(id),FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE assignment_submission (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,assignment_id BIGINT NOT NULL,student_id BIGINT NOT NULL,answer_text MEDIUMTEXT NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'DRAFT',version INT NOT NULL DEFAULT 0,attempt_no INT NOT NULL DEFAULT 0,
 update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_submission_student(assignment_id,student_id),FOREIGN KEY(assignment_id) REFERENCES assignment(id),FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE assignment_attempt (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,submission_id BIGINT NOT NULL,attempt_no INT NOT NULL,request_key VARCHAR(36) NOT NULL,
 answer_text MEDIUMTEXT NOT NULL,status VARCHAR(12) NOT NULL DEFAULT 'SUBMITTED',late TINYINT NOT NULL,submitted_at DATETIME NOT NULL,
 max_score INT NOT NULL,score INT NULL,feedback TEXT NULL,reviewed_by BIGINT NULL,reviewed_at DATETIME NULL,
 UNIQUE KEY uk_attempt_number(submission_id,attempt_no),UNIQUE KEY uk_attempt_request(submission_id,request_key),FOREIGN KEY(submission_id) REFERENCES assignment_submission(id),FOREIGN KEY(reviewed_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE assignment_file (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,submission_id BIGINT NOT NULL,attempt_no INT NULL,storage_key VARCHAR(64) NOT NULL UNIQUE,
 original_name VARCHAR(150) NOT NULL,mime_type VARCHAR(100) NOT NULL,file_size BIGINT NOT NULL,deleted TINYINT NOT NULL DEFAULT 0,
 KEY idx_assignment_file(submission_id,attempt_no,deleted),FOREIGN KEY(submission_id) REFERENCES assignment_submission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (400,0,'Assignments','assignments','C','assignment:read',3),(401,400,'Maintain assignments','','F','assignment:edit',1),
 (402,400,'Submit own work','','F','assignment:submit',2),(403,400,'Review submissions','','F','assignment:review',3);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,400),(20,400),(20,401),(10,402),(20,403);
