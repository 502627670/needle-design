
insert into t_group(id, name, parent_id, path) values(1, '系统管理组', null, "1-");

insert into t_user(id,username,password,sort_order,user_status,group_id) 
values(1, 'administrator', '$2a$10$K6O53bmvbE/zHQ6ZW7kX4u/cEdgWFhd7WrQbmKztb5bZqJN5.Aq/e', 0, 'ENABLE',1);






insert into t_user(id,username,password) values(1, 'admin', '123');
insert into t_user(id,username,password) values(2, 'operator', '123');
insert into t_user(id,username,password) values(3, 'auditor', '123');

insert into t_role(id,name,description) values(1, '管理员', '管理员');
insert into t_role(id,name,description) values(2, '操作员', '操作员');
insert into t_role(id,name,description) values(3, '审核员', '审核员');

insert into t_user_roles(id, user_id, role_id) values(1, 1, 1);
insert into t_user_roles(id, user_id, role_id) values(2, 1, 3);
insert into t_user_roles(id, user_id, role_id) values(3, 2, 2);
insert into t_user_roles(id, user_id, role_id) values(4, 3, 3);
insert into t_user_roles(id, user_id, role_id) values(5, 2, 3);

insert into t_permission(id, role_id, name, resource, resource_type) values
(1, 1, '新建用户', '/user/create', 'action'),
(2, 1, '修改用户', '/user/update', 'action'),
(3, 1, '删除用户', '/user/remove', 'action'),
(4, 1, '定制操作', '/user/action', 'action');