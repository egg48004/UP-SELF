CREATE TABLE learning_bookmark (
 student_id BIGINT NOT NULL,
 course_id BIGINT NOT NULL,
 create_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(student_id,course_id),
 KEY idx_bookmark_saved(student_id,create_time,course_id),
 FOREIGN KEY(student_id) REFERENCES sys_user(id),
 FOREIGN KEY(course_id) REFERENCES learning_course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num)
 VALUES(800,300,'Saved courses','saved-courses','C','learning:bookmark:own',7);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,800);
