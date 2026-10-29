CREATE TABLE accessibility_defaults (
 id TINYINT NOT NULL PRIMARY KEY,
 text_size VARCHAR(16) NOT NULL,
 contrast VARCHAR(16) NOT NULL,
 reduce_motion TINYINT NOT NULL,
 version INT NOT NULL DEFAULT 0,
 updated_by BIGINT NULL,
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT INTO accessibility_defaults(id,text_size,contrast,reduce_motion) VALUES(1,'standard','standard',0);
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
 (1400,0,'Platform reading defaults','accessibility','F','settings:accessibility:edit',14),
 (1410,0,'My learning statistics','statistics','F','learning:statistics:own',15);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(30,1400),(10,1410);
