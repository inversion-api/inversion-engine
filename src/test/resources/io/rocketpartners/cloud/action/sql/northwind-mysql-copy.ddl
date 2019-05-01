
DROP DATABASE IF EXISTS northwindcopy;

CREATE DATABASE IF NOT EXISTS northwindcopy;

USE northwindcopy;


CREATE TABLE `Categories` (
    `CategoryID` INTEGER NOT NULL AUTO_INCREMENT,
    `CategoryName` VARCHAR(15) NOT NULL,
    `Description` MEDIUMTEXT,
    `Picture` LONGBLOB,
    CONSTRAINT `PK_Categories` PRIMARY KEY (`CategoryID`)
);
INSERT INTO `Categories` SELECT * FROM northwindsource.Categories ORDER BY `CategoryID`;


CREATE TABLE `CustomerCustomerDemo` (
    `CustomerID` VARCHAR(5) NOT NULL,
    `CustomerTypeID` VARCHAR(10) NOT NULL,
    CONSTRAINT `PK_CustomerCustomerDemo` PRIMARY KEY (`CustomerID`, `CustomerTypeID`)
);
INSERT INTO `CustomerCustomerDemo` SELECT * FROM northwindsource.CustomerCustomerDemo ORDER BY `CustomerID`, `CustomerTypeID`;


CREATE TABLE `CustomerDemographics` (
    `CustomerTypeID` VARCHAR(10) NOT NULL,
    `CustomerDesc` MEDIUMTEXT,
    CONSTRAINT `PK_CustomerDemographics` PRIMARY KEY (`CustomerTypeID`)
);
INSERT INTO `CustomerDemographics` SELECT * FROM northwindsource.CustomerDemographics ORDER BY `CustomerTypeID`;


CREATE TABLE `Customers` (
    `CustomerID` VARCHAR(5) NOT NULL,
    `CompanyName` VARCHAR(40) NOT NULL,
    `ContactName` VARCHAR(30),
    `ContactTitle` VARCHAR(30),
    `Address` VARCHAR(60),
    `City` VARCHAR(15),
    `Region` VARCHAR(15),
    `PostalCode` VARCHAR(10),
    `Country` VARCHAR(15),
    `Phone` VARCHAR(24),
    `Fax` VARCHAR(24),
    CONSTRAINT `PK_Customers` PRIMARY KEY (`CustomerID`)
);
INSERT INTO `Customers` SELECT * FROM northwindsource.Customers ORDER BY `CustomerID`;



CREATE TABLE `Employees` (
    `EmployeeID` INTEGER NOT NULL AUTO_INCREMENT,
    `LastName` VARCHAR(20) NOT NULL,
    `FirstName` VARCHAR(10) NOT NULL,
    `Title` VARCHAR(30),
    `TitleOfCourtesy` VARCHAR(25),
    `BirthDate` DATETIME,
    `HireDate` DATETIME,
    `Address` VARCHAR(60),
    `City` VARCHAR(15),
    `Region` VARCHAR(15),
    `PostalCode` VARCHAR(10),
    `Country` VARCHAR(15),
    `HomePhone` VARCHAR(24),
    `Extension` VARCHAR(4),
    `Photo` LONGBLOB,
    `Notes` MEDIUMTEXT NOT NULL,
    `ReportsTo` INTEGER,
    `PhotoPath` VARCHAR(255),
     `Salary` FLOAT,
    CONSTRAINT `PK_Employees` PRIMARY KEY (`EmployeeID`)
);
INSERT INTO `Employees` SELECT * FROM northwindsource.Employees ORDER BY `EmployeeID`;


CREATE TABLE `EmployeeTerritories` (
    `EmployeeID` INTEGER NOT NULL,
    `TerritoryID` VARCHAR(20) NOT NULL,
    CONSTRAINT `PK_EmployeeTerritories` PRIMARY KEY (`EmployeeID`, `TerritoryID`)
);
INSERT INTO `EmployeeTerritories` SELECT * FROM northwindsource.EmployeeTerritories ORDER BY `EmployeeID`, `TerritoryID`;



CREATE TABLE `Order Details` (
    `OrderID` INTEGER NOT NULL,
    `ProductID` INTEGER NOT NULL,
    `UnitPrice` DECIMAL(10,4) NOT NULL DEFAULT 0,
    `Quantity` SMALLINT(2) NOT NULL DEFAULT 1,
    `Discount` REAL(8,0) NOT NULL DEFAULT 0,
    CONSTRAINT `PK_Order Details` PRIMARY KEY (`OrderID`, `ProductID`)
);
INSERT INTO `Order Details` SELECT * FROM northwindsource.`Order Details` ORDER BY `OrderID`, `ProductID`;


CREATE TABLE `Orders` (
    `OrderID` INTEGER NOT NULL AUTO_INCREMENT,
    `CustomerID` VARCHAR(5),
    `EmployeeID` INTEGER,
    `OrderDate` DATETIME,
    `RequiredDate` DATETIME,
    `ShippedDate` DATETIME,
    `ShipVia` INTEGER,
    `Freight` DECIMAL(10,4) DEFAULT 0,
    `ShipName` VARCHAR(40),
    `ShipAddress` VARCHAR(60),
    `ShipCity` VARCHAR(15),
    `ShipRegion` VARCHAR(15),
    `ShipPostalCode` VARCHAR(10),
    `ShipCountry` VARCHAR(15),
    CONSTRAINT `PK_Orders` PRIMARY KEY (`OrderID`)
);
INSERT INTO `Orders` SELECT * FROM northwindsource.Orders ORDER BY `OrderID`;



CREATE TABLE `Products` (
    `ProductID` INTEGER NOT NULL AUTO_INCREMENT,
    `ProductName` VARCHAR(40) NOT NULL,
    `SupplierID` INTEGER,
    `CategoryID` INTEGER,
    `QuantityPerUnit` VARCHAR(20),
    `UnitPrice` DECIMAL(10,4) DEFAULT 0,
    `UnitsInStock` SMALLINT(2) DEFAULT 0,
    `UnitsOnOrder` SMALLINT(2) DEFAULT 0,
    `ReorderLevel` SMALLINT(2) DEFAULT 0,
    `Discontinued` BIT NOT NULL DEFAULT 0,
    CONSTRAINT `PK_Products` PRIMARY KEY (`ProductID`)
);
INSERT INTO `Products` SELECT * FROM northwindsource.Products ORDER BY `ProductID`;




CREATE TABLE `Region` (
    `RegionID` INTEGER NOT NULL,
    `RegionDescription` VARCHAR(50) NOT NULL,
    CONSTRAINT `PK_Region` PRIMARY KEY (`RegionID`)
);
INSERT INTO `Region` SELECT * FROM northwindsource.Region ORDER BY `RegionID`;

CREATE TABLE `Shippers` (
    `ShipperID` INTEGER NOT NULL AUTO_INCREMENT,
    `CompanyName` VARCHAR(40) NOT NULL,
    `Phone` VARCHAR(24),
    CONSTRAINT `PK_Shippers` PRIMARY KEY (`ShipperID`)
);
INSERT INTO `Shippers` SELECT * FROM northwindsource.Shippers ORDER BY `ShipperID`;

CREATE TABLE `Suppliers` (
    `SupplierID` INTEGER NOT NULL AUTO_INCREMENT,
    `CompanyName` VARCHAR(40) NOT NULL,
    `ContactName` VARCHAR(30),
    `ContactTitle` VARCHAR(30),
    `Address` VARCHAR(60),
    `City` VARCHAR(15),
    `Region` VARCHAR(15),
    `PostalCode` VARCHAR(10),
    `Country` VARCHAR(15),
    `Phone` VARCHAR(24),
    `Fax` VARCHAR(24),
    `HomePage` MEDIUMTEXT,
    CONSTRAINT `PK_Suppliers` PRIMARY KEY (`SupplierID`)
);
INSERT INTO `Suppliers` SELECT * FROM northwindsource.Suppliers ORDER BY `SupplierID`;

CREATE TABLE `Territories` (
    `TerritoryID` VARCHAR(20) NOT NULL,
    `TerritoryDescription` VARCHAR(50) NOT NULL,
    `RegionID` INTEGER NOT NULL,
    CONSTRAINT `PK_Territories` PRIMARY KEY (`TerritoryID`)
);
INSERT INTO `Territories` SELECT * FROM northwindsource.Territories ORDER BY `TerritoryID`;


/* This table was added to support test cases with a multi part foreign key */
CREATE TABLE `EmployeeOrderDetails` (
	`EmployeeID` INTEGER NOT NULL,
	`OrderID` INTEGER NOT NULL,
	`ProductID` INTEGER NOT NULL,
	CONSTRAINT `PK_EmployeeOrderDetails` PRIMARY KEY ( `EmployeeID`, `OrderID`, `ProductID`)
);

    
INSERT INTO `EmployeeOrderDetails`  
SELECT o.EmployeeID, od.OrderId, od.ProductId 
FROM `Orders` o JOIN `Order Details` od ON o.OrderId = od.OrderId; 
    


CREATE INDEX `CategoryName` ON `Categories` (`CategoryName`);
CREATE INDEX `City` ON `Customers` (`City`);
CREATE INDEX `CompanyName` ON `Customers` (`CompanyName`);
CREATE INDEX `PostalCode` ON `Customers` (`PostalCode`);
CREATE INDEX `Region` ON `Customers` (`Region`);
CREATE INDEX `LastName` ON `Employees` (`LastName`);
CREATE INDEX `PostalCode` ON `Employees` (`PostalCode`);
CREATE INDEX `OrderDate` ON `Orders` (`OrderDate`);
CREATE INDEX `ShippedDate` ON `Orders` (`ShippedDate`);
CREATE INDEX `ShipPostalCode` ON `Orders` (`ShipPostalCode`);
CREATE INDEX `ProductName` ON `Products` (`ProductName`);
CREATE INDEX `CompanyName` ON `Suppliers` (`CompanyName`);
CREATE INDEX `PostalCode` ON `Suppliers` (`PostalCode`);


ALTER TABLE `CustomerCustomerDemo` ADD CONSTRAINT `FK_CustomerCustomerDemo` 
    FOREIGN KEY (`CustomerTypeID`) REFERENCES `CustomerDemographics` (`CustomerTypeID`);

ALTER TABLE `CustomerCustomerDemo` ADD CONSTRAINT `FK_CustomerCustomerDemo_Customers` 
    FOREIGN KEY (`CustomerID`) REFERENCES `Customers` (`CustomerID`);

ALTER TABLE `Employees` ADD CONSTRAINT `FK_Employees_Employees` 
    FOREIGN KEY (`ReportsTo`) REFERENCES `Employees` (`EmployeeID`);

ALTER TABLE `EmployeeTerritories` ADD CONSTRAINT `FK_EmployeeTerritories_Employees` 
    FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`);

ALTER TABLE `EmployeeTerritories` ADD CONSTRAINT `FK_EmployeeTerritories_Territories` 
    FOREIGN KEY (`TerritoryID`) REFERENCES `Territories` (`TerritoryID`);

ALTER TABLE `Order Details` ADD CONSTRAINT `FK_Order_Details_Orders` 
    FOREIGN KEY (`OrderID`) REFERENCES `Orders` (`OrderID`);

ALTER TABLE `Order Details` ADD CONSTRAINT `FK_Order_Details_Products` 
    FOREIGN KEY (`ProductID`) REFERENCES `Products` (`ProductID`);

ALTER TABLE `Orders` ADD CONSTRAINT `FK_Orders_Customers` 
    FOREIGN KEY (`CustomerID`) REFERENCES `Customers` (`CustomerID`);

ALTER TABLE `Orders` ADD CONSTRAINT `FK_Orders_Employees` 
    FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`);

ALTER TABLE `Orders` ADD CONSTRAINT `FK_Orders_Shippers` 
    FOREIGN KEY (`ShipVia`) REFERENCES `Shippers` (`ShipperID`);

ALTER TABLE `Products` ADD CONSTRAINT `FK_Products_Categories` 
    FOREIGN KEY (`CategoryID`) REFERENCES `Categories` (`CategoryID`);

ALTER TABLE `Products` ADD CONSTRAINT `FK_Products_Suppliers` 
    FOREIGN KEY (`SupplierID`) REFERENCES `Suppliers` (`SupplierID`);

ALTER TABLE `Territories` ADD CONSTRAINT `FK_Territories_Region` 
    FOREIGN KEY (`RegionID`) REFERENCES `Region` (`RegionID`);
    
ALTER TABLE `EmployeeOrderDetails` ADD CONSTRAINT `FK_EmpoyeeOrderDetails1`
	FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`) ON DELETE CASCADE;

ALTER TABLE `EmployeeOrderDetails` ADD CONSTRAINT `FK_EmpoyeeOrderDetails2`
	FOREIGN KEY (`OrderID`, `ProductID`) REFERENCES `Order Details` (`OrderID`, `ProductID`)  ON DELETE CASCADE;
    
    
    
