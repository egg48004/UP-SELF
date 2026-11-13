CREATE TABLE student_daily_check_in (
 student_id BIGINT NOT NULL, check_date DATE NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(student_id,check_date), FOREIGN KEY(student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num)
 VALUES(1800,0,'Daily check-in','','F','learning:checkin:own',1);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1800);
