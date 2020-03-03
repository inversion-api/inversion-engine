insert into Role (id, name) values (1, 'Guest');
insert into Role (id, name) values (100, 'Member');
insert into Role (id, name) values (1000, 'Administrator');
insert into Role (id, name) values (10000, 'Owner');


insert into User (id, username, password) values (10, 'api_admin','5F4DCC3B5AA765D61D8327DEB882CF99');  -- password is "password"
insert into ApiKey (id, userId, accessKey, secretKey) values (10, 10, '94kfk39eodk','5F4DCC3B5AA765D61D8327DEB882CF99');  -- secretKey is "password"

insert into UserRole (id, apiCode, userId, roleId) values (10, 'someApi', 10, 1000);
insert into UserRole (id, apiCode, userId, roleId) values (20, 'someApi', 10, 100);

insert into Permission (id, apiCode, name) values (10, 'someApi', 'permission1');
insert into Permission (id, apiCode, name) values (20, 'someApi', 'permission2');
insert into Permission (id, apiCode, name) values (30, 'someApi', 'permission3');
insert into Permission (id, apiCode, name) values (40, 'someApi', 'permission4');
insert into Permission (id, apiCode, name) values (50, 'someApi', 'permission5');

insert into `Group` (id, apiCode, name) values (10, 'someApi', 'admin_users');
insert into `Group` (id, apiCode, name) values (20, 'someApi', 'contributors');
insert into `Group` (id, apiCode, name) values (30, 'someApi', 'read_only');

insert into GroupRole (id, apiCode, groupId, roleId) values (10, 'someApi', 30, 1);
insert into RolePermission (id, apiCode, roleId, permissionId) values (10, 'someApi', 1, 40);
insert into RolePermission (id, apiCode, roleId, permissionId) values (20, 'someApi', 100, 50);

insert into UserPermission(id, apiCode, userId, permissionId) values (10, 'someApi', 10, 10);
insert into UserPermission(id, apiCode, userId, permissionId) values (20, 'someApi', 10, 20);

insert into GroupPermission(id, apiCode, groupId, permissionId) values (10, 'someApi', 10, 20);
insert into GroupPermission(id, apiCode, groupId, permissionId) values (20, 'someApi', 10, 30);
 
insert into UserGroup(id, apiCode, userId, groupId) values (10, 'someApi', 10, 10);
insert into UserGroup(id, apiCode, userId, groupId) values (20, 'someApi', 10, 30); 
 

