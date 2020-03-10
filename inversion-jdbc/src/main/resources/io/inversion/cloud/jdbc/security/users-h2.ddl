

drop table if exists `GroupPermission`;
drop table if exists `UserPermission`;
drop table if exists `Permission`;
drop table if exists `UserGroup`;
drop table if exists `Group`;
drop table if exists `UserRole`;
drop table if exists `User`;
drop table if exists `Role`;



CREATE TABLE `User` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `tenant` varchar(255) default null,
  `requestAt` bigint(20) DEFAULT 0,
  `failedNum` smallint(2) DEFAULT 0,
  `remoteAddr` varchar(255) DEFAULT NULL,
  `revoked` tinyint(1) DEFAULT 0,
  CONSTRAINT `PK_User` PRIMARY KEY (`id`)
);
-- CREATE UNIQUE INDEX `User_username` ON `User` (`username`);



CREATE TABLE `ApiKey` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) UNSIGNED NOT NULL,
  `accessKey` varchar(255) DEFAULT NULL,
  `secretKey` varchar(255) DEFAULT NULL,
  `revoked` tinyint(1) DEFAULT NULL,
  CONSTRAINT `PK_ApiKey` PRIMARY KEY (`id`),
  CONSTRAINT `fk_apikey_userid` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `ApiKey_accessKey` ON `ApiKey` (`accessKey`);


CREATE TABLE `Group` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  CONSTRAINT `PK_Group` PRIMARY KEY (`id`)
);


CREATE TABLE `Role` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  CONSTRAINT `PK_Role` PRIMARY KEY (`id`)
);

CREATE TABLE `Permission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  CONSTRAINT `PK_Permission` PRIMARY KEY (`id`)
);



CREATE TABLE `UserRole` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) UNSIGNED NOT NULL,
  `roleId` int(11) UNSIGNED NOT NULL,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  CONSTRAINT `PK_UserRole` PRIMARY KEY (`id`),
  CONSTRAINT `fk_userrole_user1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_userrole_role1` FOREIGN KEY (`roleId`) REFERENCES `Role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `UserRole_uratc` ON `UserRole` (`userId`,`roleId`,`api`,`tenant`);

CREATE TABLE `UserGroup` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) UNSIGNED NOT NULL,
  `groupId` int(11) UNSIGNED NOT NULL,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  CONSTRAINT `PK_UserGroup` PRIMARY KEY (`id`),
  CONSTRAINT `fk_UserGroup_user1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserGroup_group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `UserGroup_ugatc` ON `UserGroup` (`userId`,`groupId`,`api`,`tenant`);

CREATE TABLE `GroupRole` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `groupId` int(11) UNSIGNED NOT NULL,
  `roleId` int(11) UNSIGNED NOT NULL,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  CONSTRAINT `PK_GroupRole` PRIMARY KEY (`id`),
  CONSTRAINT `fk_GroupRole_group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupRole_role1` FOREIGN KEY (`roleId`) REFERENCES `Role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `GroupRole_ugatc` ON `GroupRole` (`groupId`,`roleId`,`api`,`tenant`);



CREATE TABLE `UserPermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `userId` int(11) UNSIGNED NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  CONSTRAINT `PK_UserPermission` PRIMARY KEY (`id`),
  CONSTRAINT `fk_UserPermission_user1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `UserPermission_upatc` ON `UserPermission` (`userId`,`permissionId`,`api`,`tenant`);



CREATE TABLE `GroupPermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `groupId` int(11) UNSIGNED NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  CONSTRAINT `PK_GroupPermission` PRIMARY KEY (`id`),
  CONSTRAINT `fk_GroupPermission_group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `GroupPermission_upatc` ON `GroupPermission` (`groupId`,`permissionId`,`api`,`tenant`);


CREATE TABLE `RolePermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `roleId` int(11) UNSIGNED NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `api` varchar(128) DEFAULT NULL,
  `tenant` varchar(128) DEFAULT NULL,
  CONSTRAINT `PK_RolePermission` PRIMARY KEY (`id`),
  CONSTRAINT `fk_RolePermission_role1` FOREIGN KEY (`roleId`) REFERENCES `Role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_RolePermission_permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
);
CREATE UNIQUE INDEX `RolePermission_upatc` ON `RolePermission` (`roleId`,`permissionId`,`api`,`tenant`);
