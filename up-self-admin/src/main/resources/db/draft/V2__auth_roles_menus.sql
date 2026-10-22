INSERT INTO sys_role VALUES(1,'维护超级管理员','admin','0'),(10,'学生','student','0'),(20,'导师','mentor','0'),(30,'平台管理员','platform_admin','0');
INSERT INTO sys_menu(menu_id,parent_id,menu_name,path,menu_type,perms,order_num) VALUES
(100,0,'个人中心','profile','C','',1),
(200,0,'账号管理','users','C','system:user:list',2),
(201,200,'新增账号','','F','system:user:add',1),
(202,200,'编辑及角色分配','','F','system:user:edit',2),
(203,200,'重置密码','','F','system:user:resetPwd',3),
(204,200,'停用或启用','','F','system:user:changeStatus',4),
(205,200,'重置验证器','','F','system:user:resetTotp',5);
INSERT INTO sys_role_menu VALUES(10,100),(20,100),(30,100),(30,200),(30,201),(30,202),(30,203),(30,204);
