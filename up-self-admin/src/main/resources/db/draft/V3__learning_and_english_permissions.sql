CREATE TABLE learning_course (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(150) NOT NULL,description TEXT NOT NULL,language_code VARCHAR(35) NOT NULL DEFAULT 'en',mentor_id BIGINT NULL,status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',created_by BIGINT NOT NULL,deleted TINYINT NOT NULL DEFAULT 0,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 KEY idx_course_mentor(mentor_id,status),FOREIGN KEY(mentor_id) REFERENCES sys_user(id),FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE learning_enrollment (
 course_id BIGINT NOT NULL,student_id BIGINT NOT NULL,active TINYINT NOT NULL DEFAULT 1,create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(course_id,student_id),KEY idx_enrollment_student(student_id,active),FOREIGN KEY(course_id) REFERENCES learning_course(id),FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE learning_chapter (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,course_id BIGINT NOT NULL,title VARCHAR(150) NOT NULL,body TEXT NOT NULL,sort_order INT NOT NULL DEFAULT 0,status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',language_code VARCHAR(35) NOT NULL DEFAULT 'en',deleted TINYINT NOT NULL DEFAULT 0,
 update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,KEY idx_chapter_course(course_id,deleted,status,sort_order),FOREIGN KEY(course_id) REFERENCES learning_course(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE learning_resource (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,chapter_id BIGINT NOT NULL,kind VARCHAR(10) NOT NULL,title VARCHAR(150) NOT NULL,storage_key VARCHAR(64) NOT NULL UNIQUE,original_name VARCHAR(150) NOT NULL,mime_type VARCHAR(80) NOT NULL,file_size BIGINT NOT NULL,alt_text VARCHAR(500) NULL,caption_resource_id BIGINT NULL,transcript TEXT NULL,language_code VARCHAR(35) NOT NULL DEFAULT 'en',status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',created_by BIGINT NOT NULL,deleted TINYINT NOT NULL DEFAULT 0,
 update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,KEY idx_resource_chapter(chapter_id,deleted,status),KEY idx_caption_reference(caption_resource_id),FOREIGN KEY(chapter_id) REFERENCES learning_chapter(id),FOREIGN KEY(caption_resource_id) REFERENCES learning_resource(id),FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE TABLE learning_post (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,course_id BIGINT NOT NULL,title VARCHAR(150) NOT NULL,body TEXT NOT NULL,type VARCHAR(15) NOT NULL DEFAULT 'ANNOUNCEMENT',meeting_url VARCHAR(2000) NULL,meeting_starts_at VARCHAR(40) NULL,language_code VARCHAR(35) NOT NULL DEFAULT 'en',status VARCHAR(10) NOT NULL DEFAULT 'DRAFT',created_by BIGINT NOT NULL,deleted TINYINT NOT NULL DEFAULT 0,
 create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,KEY idx_post_course(course_id,deleted,status),FOREIGN KEY(course_id) REFERENCES learning_course(id),FOREIGN KEY(created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
UPDATE sys_role SET role_name=CASE role_key WHEN 'admin' THEN 'Maintenance administrator' WHEN 'student' THEN 'Student' WHEN 'mentor' THEN 'Mentor' WHEN 'platform_admin' THEN 'Platform administrator' ELSE role_name END;
UPDATE sys_menu SET menu_name=CASE menu_id WHEN 100 THEN 'My account' WHEN 200 THEN 'Account management' WHEN 201 THEN 'Create account' WHEN 202 THEN 'Edit account and role' WHEN 203 THEN 'Reset password' WHEN 204 THEN 'Enable or disable account' WHEN 205 THEN 'Reset authenticator' ELSE menu_name END WHERE menu_id BETWEEN 100 AND 205;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(300,0,'Courses','courses','C','learning:course:query',2);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,300);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,300);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,300);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(301,300,'Create course','','F','learning:course:add',3);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,301);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(302,300,'Edit course','','F','learning:course:edit',4);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,302);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(303,300,'Assign course members','','F','learning:course:assign',5);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,303);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(304,300,'Publish course','','F','learning:course:publish',6);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,304);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(305,300,'Course members','','F','learning:course:members',7);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,305);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,305);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(306,300,'View chapters','','F','learning:chapter:query',8);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,306);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,306);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,306);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(307,300,'Maintain chapters','','F','learning:chapter:edit',9);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,307);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,307);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(308,300,'View resources','','F','learning:resource:query',10);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,308);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,308);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,308);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(309,300,'Maintain resources','','F','learning:resource:upload',11);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,309);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,309);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(310,300,'Course posts','','F','learning:post:query',12);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,310);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,310);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,310);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(311,300,'Create post','','F','learning:post:add',13);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,311);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(312,300,'Edit own post','','F','learning:post:edit',14);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,312);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES(313,300,'Unpublish post','','F','learning:post:offline',15);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(20,313);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,313);
