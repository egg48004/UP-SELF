-- Earlier notes were promised to be private. Only new, acknowledged diaries are shared.
ALTER TABLE mood_entry ADD COLUMN note_shared TINYINT(1) NOT NULL DEFAULT 0;

CREATE TABLE knowledge_question (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 course_id BIGINT NOT NULL, question_text VARCHAR(500) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'DRAFT', version INT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_knowledge_course(course_id,status),
 FOREIGN KEY(course_id) REFERENCES learning_course(id),
 FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE knowledge_answer (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 question_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 choice VARCHAR(16) NOT NULL, question_version INT NOT NULL,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 UNIQUE KEY uk_knowledge_answer(question_id,student_id),
 KEY idx_knowledge_student(student_id),
 FOREIGN KEY(question_id) REFERENCES knowledge_question(id),
 FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1000,500,'Check knowledge','knowledge','C','knowledge:own:read',3),
 (1001,1000,'Save knowledge answer','','F','knowledge:own:record',1),
 (1010,510,'Knowledge questions','knowledge-questions','C','knowledge:questions:manage',3),
 (1011,1010,'Read knowledge answers','','F','knowledge:answers:read',1);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1000),(10,1001),(20,1010),(20,1011);
