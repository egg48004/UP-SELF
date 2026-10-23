-- Existing courses receive explicit editable defaults; no historical learning is invented.
ALTER TABLE learning_course ADD difficulty VARCHAR(15) NOT NULL DEFAULT 'BEGINNER',
 ADD estimated_minutes INT NOT NULL DEFAULT 30;
CREATE TABLE learning_chapter_progress (
 student_id BIGINT NOT NULL,chapter_id BIGINT NOT NULL,
 last_visited_at DATETIME(6) NOT NULL,completed_at DATETIME(6) NULL,
 PRIMARY KEY(student_id,chapter_id),KEY idx_progress_visit(student_id,last_visited_at),
 FOREIGN KEY(student_id) REFERENCES sys_user(id),FOREIGN KEY(chapter_id) REFERENCES learning_chapter(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (600,300,'My learning progress','learning-history','C','learning:progress:own',5),
 (610,300,'Student overview','student-overview','C','learning:students:overview',5);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,600),(20,610);
