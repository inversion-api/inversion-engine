

-- -----------------------------------------------------
-- Table `mydb`.`Customer`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Customer` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `uuid` VARCHAR(36) NULL,
  `title` VARCHAR(255) NULL,
  `firstName` VARCHAR(255) NULL,
  `lastName` VARCHAR(255) NULL,
  `suffix` VARCHAR(255) NULL,
  `dateOfBirth` VARCHAR(255) NULL,
  CONSTRAINT `PK_Customer` PRIMARY KEY (`id`)
);


-- -----------------------------------------------------
-- Table `mydb`.`Identifier`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Identifier` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `customerId` BIGINT UNSIGNED  NULL,
  `providerCode` VARCHAR(255) NULL,
  `type` VARCHAR(255) NULL,
  `identifier` VARCHAR(255) NULL,
   CONSTRAINT `PK_Identifier` PRIMARY KEY (`id`)
);

ALTER TABLE `Identifier` ADD CONSTRAINT `fk_Identifier_Customer1` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`id`);


-- -----------------------------------------------------
-- Table `mydb`.`Properties`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `Properties` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `customerId` BIGINT UNSIGNED  NULL,
  `name` VARCHAR(255) NULL,
  `value` VARCHAR(1024) NULL,
  PRIMARY KEY (`id`),
   CONSTRAINT `PK_Properties` PRIMARY KEY (`id`)
  
);


ALTER TABLE `Properties` ADD CONSTRAINT `fk_Properties_Customer1` FOREIGN KEY (`customerId`) REFERENCES `Customer` (`id`);





INSERT INTO `Customer` (firstName, uuid) Values ('john', '23223b58-04a2-4c9d-b780-eabf356e650f');
INSERT INTO `Customer` (firstName, uuid) Values ('jane', '23223b58-04a2-4c9d-b780-eabf356e650f');


INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (1, 'loyalty_1', 'mobileNumber', '4048675309');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (1, 'loyalty_1', 'loyaltyCard', '8018782603900002665855');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (1, 'vendorA_1', 'uuid', '4fdac8c3-cfb1-42de-85c8-63d1a390d433');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (1, 'vendorB_1', 'uuid', '4ab53023-e525-4c35-a677-86305832b359');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (1, 'vendorC_1', 'shared', 'SHARED');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (1, 'vendorD_1', 'shared', 'SHARED');


INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (2, 'vendorA_1', 'uuid', '1d873ec9-d001-4958-886b-9196e566374d');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (2, 'vendorB_1', 'uuid', '4cff80be-a223-4cba-9fc6-11219576374a');
INSERT INTO `Identifier` (customerId, providerCode, type, identifier) Values (2, 'vendorC_1', 'shared', 'SHARED');



