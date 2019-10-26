DROP SCHEMA IF EXISTS users;
CREATE SCHEMA IF NOT EXISTS users;
SET SCHEMA users;

-- SET FOREIGN_KEY_CHECKS=0;
drop table if exists `GroupPermission`;
drop table if exists `UserPermission`;
drop table if exists `Permission`;
drop table if exists `UserGroup`;
drop table if exists `Group`;
drop table if exists `UserRole`;
drop table if exists `User`;
drop table if exists `Role`;
drop table if exists `Account`;
-- SET FOREIGN_KEY_CHECKS=1;


CREATE TABLE `Account` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(1024) NOT NULL,
  `accountCode` varchar(128) NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_Account` PRIMARY KEY (`id`)
);


CREATE TABLE `Api` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `apiCode` varchar(128) NOT NULL,
  `grants` varchar(255) DEFAULT NULL,
  `reloadable` tinyint(1) DEFAULT 1,
  `multiTenant` tinyint(1) DEFAULT 0,  
  `debug` tinyint(1) DEFAULT 1,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_Api` PRIMARY KEY (`id`),
  CONSTRAINT `fk_Api_Account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX `Api_apiCode` ON `Api` (`apiCode`);
-- 

CREATE TABLE `Role` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `level` int NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_Role` PRIMARY KEY (`id`)
);



CREATE TABLE `User` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `requestAt` bigint(20) DEFAULT NULL,
  `failedNum` smallint(2) NOT NULL,
  `remoteAddr` varchar(255) DEFAULT NULL,  
  `revoked` tinyint(1) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_User` PRIMARY KEY (`id`)
 
);
CREATE UNIQUE INDEX `User_username` ON `User` (`username`);




CREATE TABLE `ApiKey` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) UNSIGNED NOT NULL,
  `accessKey` varchar(255) DEFAULT NULL,
  `secretKey` varchar(255) DEFAULT NULL,  
  `revoked` tinyint(1) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_ApiKey` PRIMARY KEY (`id`),
  CONSTRAINT `fk_apikey_userid` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX `ApiKey_userId` ON `ApiKey` (`userId`);
CREATE UNIQUE INDEX `ApiKey_accessKey` ON `ApiKey` (`accessKey`);



CREATE TABLE `UserRole` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiCode` varchar(128) NOT NULL,
  `userId` int(11) UNSIGNED NOT NULL,
  `roleId` int(11) UNSIGNED NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_UserRole` PRIMARY KEY (`id`),
  CONSTRAINT `fk_userrole_account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_userrole_user1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_userrole_role1` FOREIGN KEY (`roleId`) REFERENCES `Role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
  
);
CREATE INDEX `UserRole_accountId` ON `UserRole` (`accountId`);
CREATE INDEX `UserRole_apiCode` ON `UserRole` (`apiCode`);
CREATE INDEX `UserRole_userId` ON `UserRole` (`userId`);
CREATE INDEX `UserRole_roleId` ON `UserRole` (`roleId`);


CREATE TABLE `Group` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiCode` varchar(128) NOT NULL,
  `tenantCode` varchar(128) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_Group` PRIMARY KEY (`id`),
  CONSTRAINT `fk_Group_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `Group_tenantCode_name` ON `Group` (`tenantCode`,`name`);
CREATE INDEX `Group_accountId` ON `Group` (`accountId`);
CREATE INDEX `Group_apiCode` ON `Group` (`apiCode`);



CREATE TABLE `UserGroup` (
  `userId` int(11) UNSIGNED NOT NULL,
  `groupId` int(11) UNSIGNED NOT NULL,
  CONSTRAINT `PK_UserGroup` PRIMARY KEY (`userId`, `groupId`),
  CONSTRAINT `fk_User_has_Group_Group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_User_has_Group_User` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX `UserGroup_groupId` ON `UserGroup` (`groupId`);
CREATE INDEX `UserGroup_userId` ON `UserGroup` (`userId`);


CREATE TABLE `Permission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiCode` varchar(128) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_Permission` PRIMARY KEY (`id`),
  CONSTRAINT `fk_Permission_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `Permission_apiCode_name` ON `Permission` (`apiCode`,`name`);
CREATE INDEX `Permission_accountId` ON `Permission` (`accountId`);
CREATE INDEX `Permission_apiCode` ON `Permission` (`apiCode`);



CREATE TABLE `UserPermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiCode` varchar(128) NOT NULL,
  `tenantCode` varchar(128) NOT NULL,
  `userId` int(11) UNSIGNED  NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_UserPermission` PRIMARY KEY (`id`),
  CONSTRAINT `fk_UserPermission_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_Permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_User1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
  
);
CREATE INDEX `UserPermission_accountId` ON `UserPermission` (`accountId`);
CREATE INDEX `UserPermission_apiCode` ON `UserPermission` (`apiCode`);
CREATE INDEX `UserPermission_tenantCode` ON `UserPermission` (`tenantCode`);
CREATE INDEX `UserPermission_userId` ON `UserPermission` (`userId`);
CREATE INDEX `UserPermission_permissionId` ON `UserPermission` (`permissionId`);


CREATE TABLE `GroupPermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiCode` varchar(128) NOT NULL,
  `tenantCode` varchar(128) NOT NULL,
  `groupId` int(11) UNSIGNED NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `PK_GroupPermission` PRIMARY KEY (`id`),
  CONSTRAINT `fk_GroupPermission_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_Group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_Permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE INDEX `GroupPermission_accountId` ON `GroupPermission` (`accountId`);
CREATE INDEX `GroupPermission_apiCode` ON `GroupPermission` (`apiCode`);
CREATE INDEX `GroupPermission_tenantCode` ON `GroupPermission` (`tenantCode`);
CREATE INDEX `GroupPermission_groupId` ON `GroupPermission` (`groupId`);
CREATE INDEX `GroupPermission_permissionId` ON `GroupPermission` (`permissionId`);



insert into Role (id, name, level) values (1, 'Guest', 1);
insert into Role (id, name, level) values (100, 'Member', 100);
insert into Role (id, name, level) values (1000, 'Administrator', 1000);
insert into Role (id, name, level) values (10000, 'Owner', 10000);

insert into Account (id, name, accountCode) values (20, 'Circle K Stores Inc.', 'lift');
insert into User (id, username, password) values (10, 'admin_us','5f4dcc3b5aa765d61d8327deb882cf99');  -- password is "password"
