-- Personal learning milestones. Existing evidence is credited at migration time.
CREATE TABLE student_badge (
 student_id BIGINT NOT NULL,
 badge_code VARCHAR(32) NOT NULL,
 awarded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY(student_id,badge_code),
 FOREIGN KEY(student_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO student_badge(student_id,badge_code)
 SELECT DISTINCT p.student_id,'FIRST_STEP' FROM learning_chapter_progress p
 JOIN sys_user u ON u.id=p.student_id AND u.role_id=10 WHERE p.completed_at IS NOT NULL;
INSERT INTO student_badge(student_id,badge_code)
 SELECT DISTINCT s.student_id,'FIRST_SUBMISSION' FROM assignment_submission s
 JOIN assignment_attempt a ON a.submission_id=s.id
 JOIN sys_user u ON u.id=s.student_id AND u.role_id=10;
INSERT INTO student_badge(student_id,badge_code)
 SELECT DISTINCT g.student_id,'GOAL_REACHED' FROM personal_goal g
 JOIN sys_user u ON u.id=g.student_id AND u.role_id=10
 WHERE g.status='COMPLETED' AND g.completed_at IS NOT NULL;

INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num)
 VALUES(1600,0,'My badges','','F','learning:badges:own',1);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(10,1600);
