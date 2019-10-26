

SET FOREIGN_KEY_CHECKS=0;
drop table if exists `Account`;

drop table if exists `Acl`;
drop table if exists `AclPermission`;
drop table if exists `AclRole`;

drop table if exists `Role`;
drop table if exists `User`;
drop table if exists `UserRole`;
drop table if exists `Api`;
drop table if exists `Tenant`;
drop table if exists `Group`;
drop table if exists `UserGroup`;
drop table if exists `Permission`;
drop table if exists `UserPermission`;
drop table if exists `GroupPermission`;

drop table if exists `Action`;
drop table if exists `ApiDbs`;
drop table if exists `Attribute`;
drop table if exists `Db`;
drop table if exists `Endpoint`;
drop table if exists `Entity`;
drop table if exists `Error`;
drop table if exists `Handler`;
drop table if exists `Method`;
drop table if exists `OAuthConfig`;
drop table if exists `OAuthUser`;

SET FOREIGN_KEY_CHECKS=1;



CREATE TABLE `Account` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(1024) NOT NULL,
  `accountCode` varchar(128) NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


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
  PRIMARY KEY (`id`),
  KEY `fk_Api_Account1_idx` (`accountId`),
  CONSTRAINT `fk_Api_Account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Role` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `level` int NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `User` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `accessKey` varchar(255) DEFAULT NULL,
  `secretKey` varchar(255) DEFAULT NULL,  
  `requestAt` bigint(20) DEFAULT NULL,
  `failedNum` smallint(2) NOT NULL,
  `remoteAddr` varchar(255) DEFAULT NULL,  
  `revoked` tinyint(1) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `k_account_username_unique` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `UserRole` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED DEFAULT NULL,
  `userId` int(11) UNSIGNED NOT NULL,
  `roleId` int(11) UNSIGNED NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_userrole_account_idx` (`accountId`),
  KEY `fk_userrole_api_idx` (`apiId`),
  KEY `fk_userrole_user_idx` (`userId`),
  KEY `fk_userrole_role_idx` (`roleId`),
  CONSTRAINT `fk_userrole_account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_userrole_api1` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_userrole_user1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_userrole_role1` FOREIGN KEY (`roleId`) REFERENCES `Role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;







CREATE TABLE `Tenant` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,  
  `apiId` int(11) UNSIGNED NOT NULL,
  `name` varchar(512) NOT NULL,
  `tenantCode` varchar(128) NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `k_tenant_api_code` (`apiId`,`tenantCode`),
  KEY `fk_Tenant_accountId_idx` (`accountId`),
  KEY `fk_Tenant_apiId_idx` (`apiId`),
  CONSTRAINT `fk_Tenant_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Tenant_api` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `Group` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,
  `tenantId` int(11) UNSIGNED NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_group_tenant_idx` (`tenantId`),
  UNIQUE KEY `fk_group_UNIQUE` (`tenantId`,`name`),
  KEY `fk_Group_accountId_idx` (`accountId`),
  KEY `fk_Group_apiId_idx` (`apiId`),
  CONSTRAINT `fk_Group_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Group_apiId` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Group_tenant` FOREIGN KEY (`tenantId`) REFERENCES `Tenant` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `UserGroup` (
  `userId` int(11) UNSIGNED NOT NULL,
  `groupId` int(11) UNSIGNED NOT NULL,
  PRIMARY KEY (`userId`,`groupId`),
  KEY `fk_User_has_Group_Group1_idx` (`groupId`),
  KEY `fk_User_has_Group_User_idx` (`userId`),
  CONSTRAINT `fk_User_has_Group_Group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_User_has_Group_User` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Permission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `un_Permission_apiId_perm_idx` (`apiId`,`name`),
  KEY `fk_Permission_accountId_idx` (`accountId`),
  KEY `fk_Permission_apiId_idx` (`apiId`),
  CONSTRAINT `fk_Permission_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Permission_apiId` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `UserPermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,
  `tenantId` int(11) UNSIGNED NOT NULL,
  `userId` int(11) UNSIGNED  NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_UserPermission_accountId_idx` (`accountId`),
  KEY `fk_UserPermission_apiId_idx` (`apiId`),
  KEY `fk_UserPermission_tenantId_idx` (`tenantId`),
  KEY `fk_UserPermission_userId_idx` (`userId`),
  KEY `fk_UserPermission_Permission1_idx` (`permissionId`),
  CONSTRAINT `fk_UserPermission_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_apiId` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_Tenant1` FOREIGN KEY (`tenantId`) REFERENCES `Tenant` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_Permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_UserPermission_User1` FOREIGN KEY (`userId`) REFERENCES `User` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `GroupPermission` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,
  `tenantId` int(11) UNSIGNED NOT NULL,
  `groupId` int(11) UNSIGNED NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_GroupPermission_accountId_idx` (`accountId`),
  KEY `fk_GroupPermission_apiId_idx` (`apiId`),
  KEY `fk_GroupPermission_tenantId_idx` (`tenantId`),
  KEY `fk_GroupPermission_groupId_idx` (`groupId`),
  KEY `fk_GroupPermission_Permission1_idx` (`permissionId`),
  CONSTRAINT `fk_GroupPermission_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_apiId` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_Tenant1` FOREIGN KEY (`tenantId`) REFERENCES `Tenant` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_Group1` FOREIGN KEY (`groupId`) REFERENCES `Group` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_GroupPermission_Permission1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Acl` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,    
  `order` int(11) NOT NULL,
  `methods` varchar(128) DEFAULT NULL,
  `includePaths` varchar(1024) DEFAULT NULL,
  `excludePaths` varchar(1024) DEFAULT NULL,
  `allow` tinyint(1) DEFAULT 1,
  `comment` text,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_Acl_Account1_idx` (`accountId`),
  KEY `fk_Acl_Api1_idx` (`apiId`),
  CONSTRAINT `fk_Acl_Account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Acl_Api1` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8;






CREATE TABLE `AclPermission` (
  `aclId` int(11) UNSIGNED NOT NULL,
  `permissionId` int(11) UNSIGNED NOT NULL,
  KEY `fk_AclPermissionaclId1_idx` (`aclId`),
  KEY `fk_AclPermission_permissionId1_idx` (`permissionId`),
  CONSTRAINT `fkAclPermission_aclId1` FOREIGN KEY (`aclId`) REFERENCES `Acl` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_AclPermission_permissionId1` FOREIGN KEY (`permissionId`) REFERENCES `Permission` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `AclRole` (
  `aclId` int(11) UNSIGNED NOT NULL,
  `roleId` int(11) UNSIGNED NOT NULL,
  KEY `fk_AclRole_aclId1_idx` (`aclId`),
  KEY `fk_AclRole_roleId1_idx` (`roleId`),
  CONSTRAINT `fk_AclRole_aclId1` FOREIGN KEY (`aclId`) REFERENCES `Acl` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_AclRole_roleId1` FOREIGN KEY (`roleId`) REFERENCES `Role` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Db` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED DEFAULT NULL,
  `name` varchar(1024) DEFAULT NULL,
  `driver` varchar(1024) DEFAULT NULL,
  `url` varchar(2048) DEFAULT NULL,
  `user` varchar(512) DEFAULT NULL,
  `pass` varchar(512) DEFAULT NULL,
  `poolMin` int(11) DEFAULT NULL,
  `poolMax` int(11) DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_Db_Account1_idx` (`accountId`),
  CONSTRAINT `fk_Db_Account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `ApiDbs` (
  `apiId` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `dbId` int(11) UNSIGNED NOT NULL,
  PRIMARY KEY (`apiId`,`dbId`),
  KEY `fk_Api_has_Db_Db1_idx` (`dbId`),
  KEY `fk_Api_has_Db_Api1_idx` (`apiId`),
  CONSTRAINT `fk_Api_has_Db_Api1` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Api_has_Db_Db1` FOREIGN KEY (`dbId`) REFERENCES `Db` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Method` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,  
  `method` varchar(1024) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_Method_accountId_idx` (`accountId`),
  CONSTRAINT `fk_Method_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Handler` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,  
  `name` varchar(1024) DEFAULT NULL,
  `description` text DEFAULT NULL,
  `className` varchar(1024) DEFAULT NULL,
  `params` text DEFAULT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_Handler_accountId_idx` (`accountId`),
  CONSTRAINT `fk_Handler_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `Endpoint` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,
  `methods` varchar(25) DEFAULT NULL,
  `includePaths` varchar(1024) DEFAULT NULL,
  `excludePaths` varchar(1024) DEFAULT NULL,
  `order` int(11) NOT NULL,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_Endpoint_Account1_idx` (`accountId`),
  KEY `fk_Endpoint_Api1_idx` (`apiId`),
  CONSTRAINT `fk_Endpoint_Account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Endpoint_Api1` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)ENGINE=InnoDB DEFAULT CHARSET=utf8;





CREATE TABLE `Action` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `accountId` int(11) UNSIGNED NOT NULL,
  `apiId` int(11) UNSIGNED NOT NULL,    
  `endpointId` int(11) UNSIGNED NULL,
  `order` int(11) NOT NULL,
  `handlerId` int(11) UNSIGNED DEFAULT NULL,
  `methods` varchar(128) DEFAULT NULL,
  `includePaths` varchar(1024) DEFAULT NULL,
  `excludePaths` varchar(1024) DEFAULT NULL,
  `comment` text,
  `params` text,
  `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_Action_Account1_idx` (`accountId`),
  KEY `fk_Action_Api1_idx` (`apiId`),
  KEY `fk_Action_Endpoint1_idx` (`apiId`),
  KEY `fk_Action_Handler1_idx` (`apiId`),
  CONSTRAINT `fk_Action_Account1` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Action_Api1` FOREIGN KEY (`apiId`) REFERENCES `Api` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Action_Endpoint1` FOREIGN KEY (`endpointId`) REFERENCES `Endpoint` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_Action_1` FOREIGN KEY (`handlerId`) REFERENCES `Handler` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
)
ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `OAuthConfig` (
  `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT,
  `apiId` int(11) UNSIGNED NOT NULL,
  `name` varchar(128) NOT NULL,
  `clientId` varchar(256) NOT NULL,
  `secretKey` varchar(256) NOT NULL,
  `scope` varchar(512) DEFAULT NULL,
  `accessTokenUrl` varchar(128) NOT NULL,
  `userAuthUrl` varchar(128) NOT NULL,
  `callbackUrl` varchar(128) NOT NULL,
  `createdAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_OAuthConfig_Api_idx` (`apiId`)
) ENGINE=InnoDB AUTO_INCREMENT=32 DEFAULT CHARSET=utf8;


CREATE TABLE `OAuthUser` (
  `id` int(11) UNSIGNED  NOT NULL AUTO_INCREMENT,
  `apiId` int(11) UNSIGNED NOT NULL,
  `oAuthConfigId` int(11) UNSIGNED NOT NULL,
  `userId` int(11) UNSIGNED NOT NULL,
  `userServiceLoginName` varchar(256) NOT NULL DEFAULT '',
  `accessToken` varchar(256) DEFAULT NULL,
  `refreshToken` varchar(256) DEFAULT NULL,
  `expirationTime` timestamp NULL DEFAULT NULL,
  `createdAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modifiedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_OAuthUser_Api_idx` (`apiId`),
  KEY `fk_OAuthUser_OAuthConfig_idx` (`oAuthConfigId`),
  KEY `fk_OAuthUser_UserId_idx` (`userId`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8;

