CREATE TABLE `Person` (
    
    `Type` VARCHAR(20),
    `Identifier` INTEGER,
    `LastName` VARCHAR(20),
    `FirstName` VARCHAR(10),
    `Title` VARCHAR(30),
    `TitleOfCourtesy` VARCHAR(25),
    `BirthDate` DATETIME,
    `Address` VARCHAR(60),
    `City` VARCHAR(15),
    `Region` VARCHAR(15),
    `PostalCode` VARCHAR(10),
    `Country` VARCHAR(15),
    `HomePhone` VARCHAR(24),
    `Extension` VARCHAR(4),
    `Notes` MEDIUMTEXT,
    `ReportsTo` VARCHAR(500),
    `PhotoPath` VARCHAR(255),
    `Salary` FLOAT,
    CONSTRAINT `PK_Person` PRIMARY KEY (`Type`, `Identifier`)
);


CREATE TABLE `Props` (
	`id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`personEntityKey` VARCHAR(500),
	`name` VARCHAR(500),
	`value` VARCHAR(500),
	CONSTRAINT `PK_Customer` PRIMARY KEY (`id`)
);


INSERT INTO `Person` (Type, Identifier, LastName) Values ('employee', '12345', 'boss1');
INSERT INTO `Person` (Type, Identifier, LastName, ReportsTo) Values ('employee', '67890', 'employee1', 'employee~boss1');
INSERT INTO `Props` (personEntityKey, name, value) Values ('employee~12345', 'prop1', 'val1');
