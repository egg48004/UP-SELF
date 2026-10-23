CREATE TABLE self_efficacy_notice_ack (
 student_id BIGINT PRIMARY KEY,notice_version VARCHAR(30) NOT NULL,
 accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE self_efficacy_entry (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,student_id BIGINT NOT NULL,
 assessment_date DATE NOT NULL,instrument_version VARCHAR(32) NOT NULL,
 answer_one TINYINT NOT NULL,answer_two TINYINT NOT NULL,answer_three TINYINT NOT NULL,
 score DECIMAL(3,2) NOT NULL,create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_efficacy_student_date(student_id,assessment_date),
 FOREIGN KEY(student_id) REFERENCES self_efficacy_notice_ack(student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (900,500,'Self-belief','self-efficacy','C','efficacy:own:read',2),
 (901,900,'Record self-belief','','F','efficacy:own:record',1),
 (910,510,'Student self-belief','student-self-efficacy','C','efficacy:students:read',2);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,900),(10,901),(20,910);
UPDATE sys_menu SET menu_name='My check-in' WHERE menu_id=500;
UPDATE sys_menu SET menu_name='Student check-ins' WHERE menu_id=510;
