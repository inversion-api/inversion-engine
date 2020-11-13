
CREATE TABLE IF NOT EXISTS `Customer` (
  `id` VARCHAR(36) NOT NULL,
   CONSTRAINT `PK_Customer` PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `Account` (
  `id` VARCHAR(255) NOT NULL,
   CONSTRAINT `PK_Account` PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `Balance` (
  `customerId` VARCHAR(36) NOT NULL,
  `accountId`  VARCHAR(255) NOT NULL,
  `balance` FLOAT NOT NULL,
  `version` INT NOT NULL DEFAULT 0,
   CONSTRAINT `PK_Balance` PRIMARY KEY (`customerId`,`accountId`)
);
ALTER TABLE `Account` ADD CONSTRAINT `FK_Account_customerId` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`id`);
ALTER TABLE `Account` ADD CONSTRAINT `FK_Account_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`);


CREATE TABLE IF NOT EXISTS `Journal` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `customerId` VARCHAR(36) NOT NULL,
  `accountId` VARCHAR(255) NOT NULL,
  `nonce` VARCHAR(36) NULL,
  `amount` FLOAT NOT NULL,
  `balance` FLOAT NOT NULL,
  `version` INT NOT NULL,
  CONSTRAINT `PK_Journal` PRIMARY KEY (`id`)
);
CREATE UNIQUE INDEX IDX_Journal_nonce ON Journal(nonce);
ALTER TABLE `Journal` ADD CONSTRAINT `FK_Journal_customerId` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`id`);
ALTER TABLE `Journal` ADD CONSTRAINT `FK_Journal_accountId` FOREIGN KEY (`accountId`) REFERENCES `Account` (`id`);
ALTER TABLE `Journal` ADD CONSTRAINT `FK_Journal_customerId_accountId` FOREIGN KEY (`customerId`,`accountId`) REFERENCES `Balance` (`customerId`,`accountId`);