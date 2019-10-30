insert into Role (id, name) values (1, 'Guest');
insert into Role (id, name) values (100, 'Member');
insert into Role (id, name) values (1000, 'Administrator');
insert into Role (id, name) values (10000, 'Owner');


insert into User (id, username, password) values (10, 'ct_admin','5F4DCC3B5AA765D61D8327DEB882CF99');  -- password is "password"
insert into ApiKey (id, userId, accessKey, secretKey) values (10, 10, '94kfk39eodk','5F4DCC3B5AA765D61D8327DEB882CF99');  -- secretKey is "password"

insert into UserRole (id, accountCode, apiCode, userId, roleId) values (10, 'tests', 'v1', 10, 1000);
insert into UserRole (id, accountCode, apiCode, userId, roleId) values (20, 'tests', 'v1', 10, 100);

insert into Permission (id, accountCode, apiCode, name) values (10, 'tests', 'v1', 'permission1');
insert into Permission (id, accountCode, apiCode, name) values (20, 'tests', 'v1', 'permission2');
insert into Permission (id, accountCode, apiCode, name) values (30, 'tests', 'v1', 'permission3');
insert into Permission (id, accountCode, apiCode, name) values (40, 'tests', 'v1', 'permission4');
insert into Permission (id, accountCode, apiCode, name) values (50, 'tests', 'v1', 'permission5');

insert into `Group` (id, accountCode, apiCode, name) values (10, 'tests', 'v1', 'admin_users');
insert into `Group` (id, accountCode, apiCode, name) values (20, 'tests', 'v1', 'contributors');
insert into `Group` (id, accountCode, apiCode, name) values (30, 'tests', 'v1', 'read_only');

insert into GroupRole (id, accountCode, apiCode, groupId, roleId) values (10, 'tests', 'v1', 30, 1);
insert into RolePermission (id, accountCode, apiCode, roleId, permissionId) values (10, 'tests', 'v1', 1, 40);
insert into RolePermission (id, accountCode, apiCode, roleId, permissionId) values (20, 'tests', 'v1', 100, 50);

insert into UserPermission(id, accountCode, apiCode, userId, permissionId) values (10, 'tests', 'v1', 10, 10);
insert into UserPermission(id, accountCode, apiCode, userId, permissionId) values (20, 'tests', 'v1', 10, 20);

insert into GroupPermission(id, accountCode, apiCode, groupId, permissionId) values (10, 'tests', 'v1', 10, 20);
insert into GroupPermission(id, accountCode, apiCode, groupId, permissionId) values (20, 'tests', 'v1', 10, 30);
 
insert into UserGroup(id, accountCode, apiCode, userId, groupId) values (10, 'tests', 'v1', 10, 10);
insert into UserGroup(id, accountCode, apiCode, userId, groupId) values (20, 'tests', 'v1', 10, 30); 
 

