# ---------------------------------------------------------------------- #
# Target DBMS:           MySQL 5                                         #
# Project name:          Northwind                                       #
# Author:                Valon Hoti                                      #
# Created on:            2010-07-07 20:00                                #
# ---------------------------------------------------------------------- #

# ---------------------------------------------------------------------- #
# Tables                                                                 #
# ---------------------------------------------------------------------- #
# ---------------------------------------------------------------------- #
# Add table "Categories"                                                 #
# ---------------------------------------------------------------------- #

-- SET @@GLOBAL.sql_mode= 'NO_ENGINE_SUBSTITUTION';
-- ONLY_FULL_GROUP_BY,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION';



CREATE TABLE `Categories` (
    `CategoryID` INTEGER NOT NULL AUTO_INCREMENT,
    `CategoryName` VARCHAR(15) NOT NULL,
    `Description` MEDIUMTEXT,
    `Picture` LONGBLOB,
    CONSTRAINT `PK_Categories` PRIMARY KEY (`CategoryID`)
);

CREATE INDEX `CategoryName` ON `Categories` (`CategoryName`);

# ---------------------------------------------------------------------- #
# Add table "CustomerCustomerDemo"                                       #
# ---------------------------------------------------------------------- #

CREATE TABLE `CustomerCustomerDemo` (
    `CustomerID` VARCHAR(5) NOT NULL,
    `CustomerTypeID` VARCHAR(10) NOT NULL,
    CONSTRAINT `PK_CustomerCustomerDemo` PRIMARY KEY (`CustomerID`, `CustomerTypeID`)
);

# ---------------------------------------------------------------------- #
# Add table "CustomerDemographics"                                       #
# ---------------------------------------------------------------------- #

CREATE TABLE `CustomerDemographics` (
    `CustomerTypeID` VARCHAR(10) NOT NULL,
    `CustomerDesc` MEDIUMTEXT,
    CONSTRAINT `PK_CustomerDemographics` PRIMARY KEY (`CustomerTypeID`)
);

# ---------------------------------------------------------------------- #
# Add table "Customers"                                                  #
# ---------------------------------------------------------------------- #

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
    CONSTRAINT `PK_customers` PRIMARY KEY (`CustomerID`)
);

CREATE INDEX `City` ON `Customers` (`City`);

CREATE INDEX `CompanyName` ON `Customers` (`CompanyName`);

CREATE INDEX `PostalCode` ON `Customers` (`PostalCode`);

CREATE INDEX `Region` ON `Customers` (`Region`);

# ---------------------------------------------------------------------- #
# Add table "Employees"                                                  #
# ---------------------------------------------------------------------- #

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

CREATE INDEX `LastName` ON `Employees` (`LastName`);

CREATE INDEX `PostalCode` ON `Employees` (`PostalCode`);

# ---------------------------------------------------------------------- #
# Add table "EmployeeTerritories"                                        #
# ---------------------------------------------------------------------- #

CREATE TABLE `EmployeeTerritories` (
    `EmployeeID` INTEGER NOT NULL,
    `TerritoryID` VARCHAR(20) NOT NULL,
    CONSTRAINT `PK_EmployeeTerritories` PRIMARY KEY (`EmployeeID`, `TerritoryID`)
);

# ---------------------------------------------------------------------- #
# Add table "Order Details"                                              #
# ---------------------------------------------------------------------- #

CREATE TABLE `Order Details` (
    `OrderID` INTEGER NOT NULL,
    `ProductID` INTEGER NOT NULL,
    `UnitPrice` DECIMAL(10,4) NOT NULL DEFAULT 0,
    `Quantity` SMALLINT(2) NOT NULL DEFAULT 1,
    `Discount` REAL(8,0) NOT NULL DEFAULT 0,
    CONSTRAINT `PK_Order Details` PRIMARY KEY (`OrderID`, `ProductID`)
);

# ---------------------------------------------------------------------- #
# Add table "orders"                                                     #
# ---------------------------------------------------------------------- #

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
    CONSTRAINT `PK_orders` PRIMARY KEY (`OrderID`)
);

CREATE INDEX `OrderDate` ON `Orders` (`OrderDate`);

CREATE INDEX `ShippedDate` ON `Orders` (`ShippedDate`);

CREATE INDEX `ShipPostalCode` ON `Orders` (`ShipPostalCode`);

# ---------------------------------------------------------------------- #
# Add table "Products"                                                   #
# ---------------------------------------------------------------------- #

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

CREATE INDEX `ProductName` ON `Products` (`ProductName`);

# ---------------------------------------------------------------------- #
# Add table "Region"                                                     #
# ---------------------------------------------------------------------- #

CREATE TABLE `Region` (
    `RegionID` INTEGER NOT NULL AUTO_INCREMENT,
    `RegionDescription` VARCHAR(50) NOT NULL,
    CONSTRAINT `PK_Region` PRIMARY KEY (`RegionID`)
);

# ---------------------------------------------------------------------- #
# Add table "Shippers"                                                   #
# ---------------------------------------------------------------------- #

CREATE TABLE `Shippers` (
    `ShipperID` INTEGER NOT NULL AUTO_INCREMENT,
    `CompanyName` VARCHAR(40) NOT NULL,
    `Phone` VARCHAR(24),
    CONSTRAINT `PK_Shippers` PRIMARY KEY (`ShipperID`)
);

# ---------------------------------------------------------------------- #
# Add table "Suppliers"                                                  #
# ---------------------------------------------------------------------- #

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

CREATE INDEX `CompanyName` ON `Suppliers` (`CompanyName`);

CREATE INDEX `PostalCode` ON `Suppliers` (`PostalCode`);

# ---------------------------------------------------------------------- #
# Add table "Territories"                                                #
# ---------------------------------------------------------------------- #

CREATE TABLE `Territories` (
    `TerritoryID` VARCHAR(20) NOT NULL,
    `TerritoryDescription` VARCHAR(50) NOT NULL,
    `RegionID` INTEGER NOT NULL,
    CONSTRAINT `PK_Territories` PRIMARY KEY (`TerritoryID`)
);

# ---------------------------------------------------------------------- #
# Add info into "Categories"                                             #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Categories;
INSERT INTO Categories VALUES(null,'Beverages','Soft drinks, coffees, teas, beers, and ales',null);
INSERT INTO Categories VALUES(null,'Condiments','Sweet and savory sauces, relishes, spreads, and seasonings',null);
INSERT INTO Categories VALUES(null,'Confections','Desserts, candies, and sweet breads',null);
INSERT INTO Categories VALUES(null,'Dairy Products','Cheeses',null);
INSERT INTO Categories VALUES(null,'Grains/Cereals','Breads, crackers, pasta, and cereal',null);
INSERT INTO Categories VALUES(null,'Meat/Poultry','Prepared meats',null);
INSERT INTO Categories VALUES(null,'Produce','Dried fruit and bean curd',null);
INSERT INTO Categories VALUES(null,'Seafood','Seaweed and fish',null);

# ---------------------------------------------------------------------- #
# Add info into "customers"                                              #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE `Customers`;
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ALFKI', 'Alfreds Futterkiste', 'Maria Anders', 'Sales Representative', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany', '030-0074321', '030-0076545');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ANATR', 'Ana Trujillo Emparedados y helados', 'Ana Trujillo', 'Owner', 'Avda. de la Constitucin 2222', 'Mxico D.F.', NULL, '05021', 'Mexico', '(5) 555-4729', '(5) 555-3745');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ANTON', 'Antonio Moreno Taquera', 'Antonio Moreno', 'Owner', 'Mataderos  2312', 'Mxico D.F.', NULL, '05023', 'Mexico', '(5) 555-3932', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('AROUT', 'Around the Horn', 'Thomas Hardy', 'Sales Representative', '120 Hanover Sq.', 'London', NULL, 'WA1 1DP', 'UK', '(171) 555-7788', '(171) 555-6750');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BERGS', 'Berglunds snabbkp', 'Christina Berglund', 'Order Administrator', 'Berguvsvgen  8', 'Lule', NULL, 'S-958 22', 'Sweden', '0921-12 34 65', '0921-12 34 67');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BLAUS', 'Blauer See Delikatessen', 'Hanna Moos', 'Sales Representative', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany', '0621-08460', '0621-08924');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BLONP', 'Blondesddsl pre et fils', 'Frdrique Citeaux', 'Marketing Manager', '24, place Klber', 'Strasbourg', NULL, '67000', 'France', '88.60.15.31', '88.60.15.32');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BOLID', 'Blido Comidas preparadas', 'Martn Sommer', 'Owner', 'C/ Araquil, 67', 'Madrid', NULL, '28023', 'Spain', '(91) 555 22 82', '(91) 555 91 99');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BONAP', 'Bon app''', 'Laurence Lebihan', 'Owner', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France', '91.24.45.40', '91.24.45.41');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BOTTM', 'Bottom-Dollar Markets', 'Elizabeth Lincoln', 'Accounting Manager', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada', '(604) 555-4729', '(604) 555-3745');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BSBEV', 'B''s Beverages', 'Victoria Ashworth', 'Sales Representative', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK', '(171) 555-1212', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CACTU', 'Cactus Comidas para llevar', 'Patricio Simpson', 'Sales Agent', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina', '(1) 135-5555', '(1) 135-4892');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CENTC', 'Centro comercial Moctezuma', 'Francisco Chang', 'Marketing Manager', 'Sierras de Granada 9993', 'Mxico D.F.', NULL, '05022', 'Mexico', '(5) 555-3392', '(5) 555-7293');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CHOPS', 'Chop-suey Chinese', 'Yang Wang', 'Owner', 'Hauptstr. 29', 'Bern', NULL, '3012', 'Switzerland', '0452-076545', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('COMMI', 'Comrcio Mineiro', 'Pedro Afonso', 'Sales Associate', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil', '(11) 555-7647', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CONSH', 'Consolidated Holdings', 'Elizabeth Brown', 'Sales Representative', 'Berkeley Gardens 12  Brewery', 'London', NULL, 'WX1 6LT', 'UK', '(171) 555-2282', '(171) 555-9199');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('DRACD', 'Drachenblut Delikatessen', 'Sven Ottlieb', 'Order Administrator', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany', '0241-039123', '0241-059428');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('DUMON', 'Du monde entier', 'Janine Labrune', 'Owner', '67, rue des Cinquante Otages', 'Nantes', NULL, '44000', 'France', '40.67.88.88', '40.67.89.89');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('EASTC', 'Eastern Connection', 'Ann Devon', 'Sales Agent', '35 King George', 'London', NULL, 'WX3 6FW', 'UK', '(171) 555-0297', '(171) 555-3373');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ERNSH', 'Ernst Handel', 'Roland Mendel', 'Sales Manager', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria', '7675-3425', '7675-3426');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FAMIA', 'Familia Arquibaldo', 'Aria Cruz', 'Marketing Assistant', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil', '(11) 555-9857', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FISSA', 'FISSA Fabrica Inter. Salchichas S.A.', 'Diego Roel', 'Accounting Manager', 'C/ Moralzarzal, 86', 'Madrid', NULL, '28034', 'Spain', '(91) 555 94 44', '(91) 555 55 93');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FOLIG', 'Folies gourmandes', 'Martine Ranc', 'Assistant Sales Agent', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France', '20.16.10.16', '20.16.10.17');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FOLKO', 'Folk och f HB', 'Maria Larsson', 'Owner', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden', '0695-34 67 21', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FRANK', 'Frankenversand', 'Peter Franken', 'Marketing Manager', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany', '089-0877310', '089-0877451');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FRANR', 'France restauration', 'Carine Schmitt', 'Marketing Manager', '54, rue Royale', 'Nantes', NULL, '44000', 'France', '40.32.21.21', '40.32.21.20');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FRANS', 'Franchi S.p.A.', 'Paolo Accorti', 'Sales Representative', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy', '011-4988260', '011-4988261');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FURIB', 'Furia Bacalhau e Frutos do Mar', 'Lino Rodriguez', 'Sales Manager', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal', '(1) 354-2534', '(1) 354-2535');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GALED', 'Galera del gastrnomo', 'Eduardo Saavedra', 'Marketing Manager', 'Rambla de Catalua, 23', 'Barcelona', NULL, '08022', 'Spain', '(93) 203 4560', '(93) 203 4561');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GODOS', 'Godos Cocina Tpica', 'Jos Pedro Freyre', 'Sales Manager', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain', '(95) 555 82 82', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GOURL', 'Gourmet Lanchonetes', 'Andr Fonseca', 'Sales Associate', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil', '(11) 555-9482', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GREAL', 'Great Lakes Food Market', 'Howard Snyder', 'Marketing Manager', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA', '(503) 555-7555', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GROSR', 'GROSELLA-Restaurante', 'Manuel Pereira', 'Owner', '5 Ave. Los Palos Grandes', 'Caracas', 'DF', '1081', 'Venezuela', '(2) 283-2951', '(2) 283-3397');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HANAR', 'Hanari Carnes', 'Mario Pontes', 'Accounting Manager', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil', '(21) 555-0091', '(21) 555-8765');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HILAA', 'HILARION-Abastos', 'Carlos Hernndez', 'Sales Representative', 'Carrera 22 con Ave. Carlos Soublette #8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela', '(5) 555-1340', '(5) 555-1948');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HUNGC', 'Hungry Coyote Import Store', 'Yoshi Latimer', 'Sales Representative', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA', '(503) 555-6874', '(503) 555-2376');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HUNGO', 'Hungry Owl All-Night Grocers', 'Patricia McKenna', 'Sales Associate', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland', '2967 542', '2967 3333');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ISLAT', 'Island Trading', 'Helen Bennett', 'Marketing Manager', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK', '(198) 555-8888', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('KOENE', 'Kniglich Essen', 'Philip Cramer', 'Sales Associate', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany', '0555-09876', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LACOR', 'La corne d''abondance', 'Daniel Tonini', 'Sales Representative', '67, avenue de l''Europe', 'Versailles', NULL, '78000', 'France', '30.59.84.10', '30.59.85.11');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LAMAI', 'La maison d''Asie', 'Annette Roulet', 'Sales Manager', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France', '61.77.61.10', '61.77.61.11');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LAUGB', 'Laughing Bacchus Wine Cellars', 'Yoshi Tannamuri', 'Marketing Assistant', '1900 Oak St.', 'Vancouver', 'BC', 'V3F 2K1', 'Canada', '(604) 555-3392', '(604) 555-7293');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LAZYK', 'Lazy K Kountry Store', 'John Steel', 'Marketing Manager', '12 Orchestra Terrace', 'Walla Walla', 'WA', '99362', 'USA', '(509) 555-7969', '(509) 555-6221');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LEHMS', 'Lehmanns Marktstand', 'Renate Messner', 'Sales Representative', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany', '069-0245984', '069-0245874');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LETSS', 'Let''s Stop N Shop', 'Jaime Yorres', 'Owner', '87 Polk St. Suite 5', 'San Francisco', 'CA', '94117', 'USA', '(415) 555-5938', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LILAS', 'LILA-Supermercado', 'Carlos Gonzlez', 'Accounting Manager', 'Carrera 52 con Ave. Bolvar #65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela', '(9) 331-6954', '(9) 331-7256');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LINOD', 'LINO-Delicateses', 'Felipe Izquierdo', 'Owner', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela', '(8) 34-56-12', '(8) 34-93-93');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LONEP', 'Lonesome Pine Restaurant', 'Fran Wilson', 'Sales Manager', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA', '(503) 555-9573', '(503) 555-9646');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MAGAA', 'Magazzini Alimentari Riuniti', 'Giovanni Rovelli', 'Marketing Manager', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy', '035-640230', '035-640231');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MAISD', 'Maison Dewey', 'Catherine Dewey', 'Sales Agent', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium', '(02) 201 24 67', '(02) 201 24 68');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MEREP', 'Mre Paillarde', 'Jean Fresnire', 'Marketing Assistant', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada', '(514) 555-8054', '(514) 555-8055');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MORGK', 'Morgenstern Gesundkost', 'Alexander Feuer', 'Marketing Assistant', 'Heerstr. 22', 'Leipzig', NULL, '04179', 'Germany', '0342-023176', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('NORTS', 'North/South', 'Simon Crowther', 'Sales Associate', 'South House 300 Queensbridge', 'London', NULL, 'SW7 1RZ', 'UK', '(171) 555-7733', '(171) 555-2530');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('OCEAN', 'Ocano Atlntico Ltda.', 'Yvonne Moncada', 'Sales Agent', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina', '(1) 135-5333', '(1) 135-5535');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('OLDWO', 'Old World Delicatessen', 'Rene Phillips', 'Sales Representative', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA', '(907) 555-7584', '(907) 555-2880');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('OTTIK', 'Ottilies Kseladen', 'Henriette Pfalzheim', 'Owner', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany', '0221-0644327', '0221-0765721');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PARIS', 'Paris spcialits', 'Marie Bertrand', 'Owner', '265, boulevard Charonne', 'Paris', NULL, '75012', 'France', '(1) 42.34.22.66', '(1) 42.34.22.77');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PERIC', 'Pericles Comidas clsicas', 'Guillermo Fernndez', 'Sales Representative', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '05033', 'Mexico', '(5) 552-3745', '(5) 545-3745');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PICCO', 'Piccolo und mehr', 'Georg Pipps', 'Sales Manager', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria', '6562-9722', '6562-9723');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PRINI', 'Princesa Isabel Vinhos', 'Isabel de Castro', 'Sales Representative', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal', '(1) 356-5634', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('QUEDE', 'Que Delcia', 'Bernardo Batista', 'Accounting Manager', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil', '(21) 555-4252', '(21) 555-4545');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('QUEEN', 'Queen Cozinha', 'Lcia Carvalho', 'Marketing Assistant', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil', '(11) 555-1189', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('QUICK', 'QUICK-Stop', 'Horst Kloss', 'Accounting Manager', 'Taucherstrae 10', 'Cunewalde', NULL, '01307', 'Germany', '0372-035188', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RANCH', 'Rancho grande', 'Sergio Gutirrez', 'Sales Representative', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina', '(1) 123-5555', '(1) 123-5556');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RATTC', 'Rattlesnake Canyon Grocery', 'Paula Wilson', 'Assistant Sales Representative', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA', '(505) 555-5939', '(505) 555-3620');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('REGGC', 'Reggiani Caseifici', 'Maurizio Moroni', 'Sales Associate', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy', '0522-556721', '0522-556722');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RICAR', 'Ricardo Adocicados', 'Janete Limeira', 'Assistant Sales Agent', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil', '(21) 555-3412', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RICSU', 'Richter Supermarkt', 'Michael Holz', 'Sales Manager', 'Grenzacherweg 237', 'Genve', NULL, '1203', 'Switzerland', '0897-034214', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ROMEY', 'Romero y tomillo', 'Alejandra Camino', 'Accounting Manager', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain', '(91) 745 6200', '(91) 745 6210');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SANTG', 'Sant Gourmet', 'Jonas Bergulfsen', 'Owner', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway', '07-98 92 35', '07-98 92 47');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SAVEA', 'Save-a-lot Markets', 'Jose Pavarotti', 'Sales Representative', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA', '(208) 555-8097', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SEVES', 'Seven Seas Imports', 'Hari Kumar', 'Sales Manager', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK', '(171) 555-1717', '(171) 555-5646');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SIMOB', 'Simons bistro', 'Jytte Petersen', 'Owner', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark', '31 12 34 56', '31 13 35 57');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SPECD', 'Spcialits du monde', 'Dominique Perrier', 'Marketing Manager', '25, rue Lauriston', 'Paris', NULL, '75016', 'France', '(1) 47.55.60.10', '(1) 47.55.60.20');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SPLIR', 'Split Rail Beer & Ale', 'Art Braunschweiger', 'Sales Manager', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA', '(307) 555-4680', '(307) 555-6525');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SUPRD', 'Suprmes dlices', 'Pascale Cartrain', 'Accounting Manager', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium', '(071) 23 67 22 20', '(071) 23 67 22 21');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('THEBI', 'The Big Cheese', 'Liz Nixon', 'Marketing Manager', '89 Jefferson Way Suite 2', 'Portland', 'OR', '97201', 'USA', '(503) 555-3612', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('THECR', 'The Cracker Box', 'Liu Wong', 'Marketing Assistant', '55 Grizzly Peak Rd.', 'Butte', 'MT', '59801', 'USA', '(406) 555-5834', '(406) 555-8083');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TOMSP', 'Toms Spezialitten', 'Karin Josephs', 'Marketing Manager', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany', '0251-031259', '0251-035695');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TORTU', 'Tortuga Restaurante', 'Miguel Angel Paolino', 'Owner', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '05033', 'Mexico', '(5) 555-2933', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TRADH', 'Tradio Hipermercados', 'Anabela Domingues', 'Sales Representative', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil', '(11) 555-2167', '(11) 555-2168');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TRAIH', 'Trail''s Head Gourmet Provisioners', 'Helvetius Nagy', 'Sales Associate', '722 DaVinci Blvd.', 'Kirkland', 'WA', '98034', 'USA', '(206) 555-8257', '(206) 555-2174');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VAFFE', 'Vaffeljernet', 'Palle Ibsen', 'Sales Manager', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark', '86 21 32 43', '86 22 33 44');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('Val2 ', 'IT', 'Val2', 'IT', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VALON', 'IT', 'Valon Hoti', 'IT', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VICTE', 'Victuailles en stock', 'Mary Saveley', 'Sales Agent', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France', '78.32.54.86', '78.32.54.87');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VINET', 'Vins et alcools Chevalier', 'Paul Henriot', 'Accounting Manager', '59 rue de l''Abbaye', 'Reims', NULL, '51100', 'France', '26.47.15.10', '26.47.15.11');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WANDK', 'Die Wandernde Kuh', 'Rita Mller', 'Sales Representative', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany', '0711-020361', '0711-035428');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WARTH', 'Wartian Herkku', 'Pirkko Koskitalo', 'Accounting Manager', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland', '981-443655', '981-443655');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WELLI', 'Wellington Importadora', 'Paula Parente', 'Sales Manager', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil', '(14) 555-8122', NULL);
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WHITC', 'White Clover Markets', 'Karl Jablonski', 'Owner', '305 - 14th Ave. S. Suite 3B', 'Seattle', 'WA', '98128', 'USA', '(206) 555-4112', '(206) 555-4115');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WILMK', 'Wilman Kala', 'Matti Karttunen', 'Owner/Marketing Assistant', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland', '90-224 8858', '90-224 8858');
INSERT INTO `Customers` (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WOLZA', 'Wolski  Zajazd', 'Zbyszek Piestrzeniewicz', 'Owner', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland', '(26) 642-7012', '(26) 642-7012');

# ---------------------------------------------------------------------- #
# Add info into "Employees"                                              #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Employees;
INSERT INTO Employees VALUES(null,'Davolio','Nancy','Sales Representative','Ms.','1948-12-08','1992-05-01','507 - 20th Ave. E.Apt. 2A','Seattle','WA','98122','USA','(206) 555-9857','5467',X'','Education includes a BA in psychology from Colorado State University in 1970.  She also completed "The Art of the Cold Call."  Nancy is a member of Toastmasters International.',2
,'http://accweb/emmployees/davolio.bmp','2954.55');
INSERT INTO Employees VALUES(null,'Fuller','Andrew','Vice President, Sales','Dr.','1952-02-19','1992-08-14','908 W. Capital Way','Tacoma','WA','98401','USA','(206) 555-9482','3457',X'','Andrew received his BTS commercial in 1974 and a Ph.D. in international marketing from the University of Dallas in 1981.  He is fluent in French and Italian and reads German.  He joined the company as a sales representative, was promoted to sales manager in January 1992 and to vice president of sales in March 1993.  Andrew is a member of the Sales Management Roundtable, the Seattle Chamber of Commerce, and the Pacific Rim Importers Association.',NULL
,'http://accweb/emmployees/fuller.bmp','2254.49');
INSERT INTO Employees VALUES(null,'Leverling','Janet','Sales Representative','Ms.','1963-08-30','1992-04-01','722 Moss Bay Blvd.','Kirkland','WA','98033','USA','(206) 555-3412','3355',X'','Janet has a BS degree in chemistry from Boston College (1984).  She has also completed a certificate program in food retailing management.  Janet was hired as a sales associate in 1991 and promoted to sales representative in February 1992.',2
,'http://accweb/emmployees/leverling.bmp','3119.15');
INSERT INTO Employees VALUES(null,'Peacock','Margaret','Sales Representative','Mrs.','1937-09-19','1993-05-03','4110 Old Redmond Rd.','Redmond','WA','98052','USA','(206) 555-8122','5176',X'','Margaret holds a BA in English literature from Concordia College (1958) and an MA from the American Institute of Culinary Arts (1966).  She was assigned to the London office temporarily from July through November 1992.',2
,'http://accweb/emmployees/peacock.bmp','1861.08');
INSERT INTO Employees VALUES(null,'Buchanan','Steven','Sales Manager','Mr.','1955-03-04','1993-10-17','14 Garrett Hill','London',NULL,'SW1 8JR','UK','(71) 555-4848','3453',X'','Steven Buchanan graduated from St. Andrews University, Scotland, with a BSC degree in 1976.  Upon joining the company as a sales representative in 1992, he spent 6 months in an orientation program at the Seattle office and then returned to his permanent post in London.  He was promoted to sales manager in March 1993.  Mr. Buchanan has completed the courses "Successful Telemarketing" and "International Sales Management."  He is fluent in French.',2
,'http://accweb/emmployees/buchanan.bmp','1744.21');
INSERT INTO Employees VALUES(null,'Suyama','Michael','Sales Representative','Mr.','1963-07-02','1993-10-17','Coventry House
Miner Rd.','London',NULL,'EC2 7JR','UK','(71) 555-7773','428',X'','Michael is a graduate of Sussex University (MA, economics, 1983) and the University of California at Los Angeles (MBA, marketing, 1986).  He has also taken the courses "Multi-Cultural Selling" and "Time Management for the Sales Professional."  He is fluent in Japanese and can read and write French, Portuguese, and Spanish.',5
,'http://accweb/emmployees/davolio.bmp','2004.07');
INSERT INTO Employees VALUES(null,'King','Robert','Sales Representative','Mr.','1960-05-29','1994-01-02','Edgeham Hollow
Winchester Way','London',NULL,'RG1 9SP','UK','(71) 555-5598','465',X'','Robert King served in the Peace Corps and traveled extensively before completing his degree in English at the University of Michigan in 1992, the year he joined the company.  After completing a course entitled "Selling in Europe," he was transferred to the London office in March 1993.',5
,'http://accweb/emmployees/davolio.bmp','1991.55');
INSERT INTO Employees VALUES(null,'Callahan','Laura','Inside Sales Coordinator','Ms.','1958-01-09','1994-03-05','4726 - 11th Ave. N.E.','Seattle','WA','98105','USA','(206) 555-1189','2344',X'','Laura received a BA in psychology from the University of Washington.  She has also completed a course in business French.  She reads and writes French.',2
,'http://accweb/emmployees/davolio.bmp','2100.50');
INSERT INTO Employees VALUES(null,'Dodsworth','Anne','Sales Representative','Ms.','1966-01-27','1994-11-15','7 Houndstooth Rd.','London',NULL,'WG2 7LT','UK','(71) 555-4444','452',X'','Anne has a BA degree in English from St. Lawrence College.  She is fluent in French and German.',5
,'http://accweb/emmployees/davolio.bmp','2333.33');

# ---------------------------------------------------------------------- #
# Add info into "EmployeeTerritories"                                    #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE EmployeeTerritories;
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(1, '06897');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(1, '19713');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '01581');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '01730');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '01833');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '02116');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '02139');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '02184');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(2, '40222');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(3, '30346');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(3, '31406');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(3, '32859');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(3, '33607');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(4, '20852');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(4, '27403');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(4, '27511');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '02903');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '07960');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '08837');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '10019');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '10038');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '11747');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(5, '14450');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(6, '85014');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(6, '85251');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(6, '98004');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(6, '98052');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(6, '98104');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '60179');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '60601');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '80202');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '80909');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '90405');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '94025');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '94105');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '95008');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '95054');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(7, '95060');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(8, '19428');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(8, '44122');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(8, '45839');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(8, '53404');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '03049');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '03801');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '48075');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '48084');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '48304');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '55113');
INSERT INTO EmployeeTerritories (EmployeeID, TerritoryID)
VALUES(9, '55439');

# ---------------------------------------------------------------------- #
# Add info into "orders"                                                 #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Orders;
INSERT INTO Orders  Values ('10248','VINET',5,'1996-07-04 00:00:00.000','1996-08-01 00:00:00.000','1996-07-16 00:00:00.000',3,32.38,'Vins et alcools Chevalier','59 rue de l-Abbaye','Reims',NULL,'51100','France');
INSERT INTO Orders  Values ('10249','TOMSP',6,'1996-07-05 00:00:00.000','1996-08-16 00:00:00.000','1996-07-10 00:00:00.000',1,11.61,'Toms Spezialitten','Luisenstr. 48','Mnster',NULL,'44087','Germany');
INSERT INTO Orders  Values ('10250','HANAR',4,'1996-07-08 00:00:00.000','1996-08-05 00:00:00.000','1996-07-12 00:00:00.000',2,65.83,'Hanari Carnes','Rua do Pao, 67','Rio de Janeiro','RJ','05454-876','Brazil');
INSERT INTO Orders  Values ('10251','VICTE',3,'1996-07-08 00:00:00.000','1996-08-05 00:00:00.000','1996-07-15 00:00:00.000',1,41.34,'Victuailles en stock','2, rue du Commerce','Lyon',NULL,'69004','France');
INSERT INTO Orders  Values ('10252','SUPRD',4,'1996-07-09 00:00:00.000','1996-08-06 00:00:00.000','1996-07-11 00:00:00.000',2,51.3,'Suprmes dlices','Boulevard Tirou, 255','Charleroi',NULL,'B-6000','Belgium');
INSERT INTO Orders  Values ('10253','HANAR',3,'1996-07-10 00:00:00.000','1996-07-24 00:00:00.000','1996-07-16 00:00:00.000',2,58.17,'Hanari Carnes','Rua do Pao, 67','Rio de Janeiro','RJ','05454-876','Brazil');
INSERT INTO Orders  Values ('10254','CHOPS',5,'1996-07-11 00:00:00.000','1996-08-08 00:00:00.000','1996-07-23 00:00:00.000',2,22.98,'Chop-suey Chinese','Hauptstr. 31','Bern',NULL,'3012','Switzerland');
INSERT INTO Orders  Values ('10255','RICSU',9,'1996-07-12 00:00:00.000','1996-08-09 00:00:00.000','1996-07-15 00:00:00.000',3,148.33,'Richter Supermarkt','Starenweg 5','Genve',NULL,'1204','Switzerland');
INSERT INTO Orders  Values ('10256','WELLI',3,'1996-07-15 00:00:00.000','1996-08-12 00:00:00.000','1996-07-17 00:00:00.000',2,13.97,'Wellington Importadora','Rua do Mercado, 12','Resende','SP','08737-363','Brazil');
INSERT INTO Orders  Values ('10257','HILAA',4,'1996-07-16 00:00:00.000','1996-08-13 00:00:00.000','1996-07-22 00:00:00.000',3,81.91,'HILARION-Abastos','Carrera 22 con Ave. Carlos Soublette #8-35','San Cristbal','Tchira','5022','Venezuela');
INSERT INTO Orders  Values ('10258','ERNSH',1,'1996-07-17 00:00:00.000','1996-08-14 00:00:00.000','1996-07-23 00:00:00.000',1,140.51,'Ernst Handel','Kirchgasse 6','Graz',NULL,'8010','Austria');
INSERT INTO Orders  Values ('10259','CENTC',4,'1996-07-18 00:00:00.000','1996-08-15 00:00:00.000','1996-07-25 00:00:00.000',3,3.25,'Centro comercial Moctezuma','Sierras de Granada 9993','Mxico D.F.',NULL,'5022','Mexico');
INSERT INTO Orders  Values ('10260','OTTIK',4,'1996-07-19 00:00:00.000','1996-08-16 00:00:00.000','1996-07-29 00:00:00.000',1,55.09,'Ottilies Kseladen','Mehrheimerstr. 369','Kln',NULL,'50739','Germany');
INSERT INTO Orders  Values ('10261','QUEDE',4,'1996-07-19 00:00:00.000','1996-08-16 00:00:00.000','1996-07-30 00:00:00.000',2,3.05,'Que Delcia','Rua da Panificadora, 12','Rio de Janeiro','RJ','02389-673','Brazil');
INSERT INTO Orders  Values ('10262','RATTC',8,'1996-07-22 00:00:00.000','1996-08-19 00:00:00.000','1996-07-25 00:00:00.000',3,48.29,'Rattlesnake Canyon Grocery','2817 Milton Dr.','Albuquerque','NM','87110','USA');
INSERT INTO Orders  Values ('10263','ERNSH',9,'1996-07-23 00:00:00.000','1996-08-20 00:00:00.000','1996-07-31 00:00:00.000',3,146.06,'Ernst Handel','Kirchgasse 6','Graz',NULL,'8010','Austria');
INSERT INTO Orders  Values ('10264','FOLKO',6,'1996-07-24 00:00:00.000','1996-08-21 00:00:00.000','1996-08-23 00:00:00.000',3,3.67,'Folk och f HB','kergatan 24','Brcke',NULL,'S-844 67','Sweden');
INSERT INTO Orders  Values ('10265','BLONP',2,'1996-07-25 00:00:00.000','1996-08-22 00:00:00.000','1996-08-12 00:00:00.000',1,55.28,'Blondel pre et fils','24, place Klber','Strasbourg',NULL,'67000','France');
INSERT INTO Orders  Values ('10266','WARTH',3,'1996-07-26 00:00:00.000','1996-09-06 00:00:00.000','1996-07-31 00:00:00.000',3,25.73,'Wartian Herkku','Torikatu 38','Oulu',NULL,'90110','Finland');
INSERT INTO Orders  Values ('10267','FRANK',4,'1996-07-29 00:00:00.000','1996-08-26 00:00:00.000','1996-08-06 00:00:00.000',1,208.58,'Frankenversand','Berliner Platz 43','Mnchen',NULL,'80805','Germany');
INSERT INTO Orders  Values ('10268','GROSR',8,'1996-07-30 00:00:00.000','1996-08-27 00:00:00.000','1996-08-02 00:00:00.000',3,66.29,'GROSELLA-Restaurante','5 Ave. Los Palos Grandes','Caracas','DF','1081','Venezuela');
INSERT INTO Orders  Values ('10269','WHITC',5,'1996-07-31 00:00:00.000','1996-08-14 00:00:00.000','1996-08-09 00:00:00.000',1,4.56,'White Clover Markets','1029 - 12th Ave. S.','Seattle','WA','98124','USA');
INSERT INTO Orders  Values ('10270','WARTH',1,'1996-08-01 00:00:00.000','1996-08-29 00:00:00.000','1996-08-02 00:00:00.000',1,136.54,'Wartian Herkku','Torikatu 38','Oulu',NULL,'90110','Finland');
INSERT INTO Orders  Values ('10271','SPLIR',6,'1996-08-01 00:00:00.000','1996-08-29 00:00:00.000','1996-08-30 00:00:00.000',2,4.54,'Split Rail Beer & Ale','P.O. Box 555','Lander','WY','82520','USA');
INSERT INTO Orders  Values ('10272','RATTC',6,'1996-08-02 00:00:00.000','1996-08-30 00:00:00.000','1996-08-06 00:00:00.000',2,98.03,'Rattlesnake Canyon Grocery','2817 Milton Dr.','Albuquerque','NM','87110','USA');


# ---------------------------------------------------------------------- #
# Add info into "Order Details"                                          #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE `Order Details`;
INSERT INTO `Order Details` Values (10248, 11, 14, 12, 0.0);
INSERT INTO `Order Details` Values (10248, 42, 9.8, 10, 0.0);
INSERT INTO `Order Details` Values (10248, 72, 34.8, 5, 0.0);
INSERT INTO `Order Details` Values (10249, 14, 18.6, 9, 0.0);
INSERT INTO `Order Details` Values (10249, 51, 42.4, 40, 0.0);
INSERT INTO `Order Details` Values (10250, 41, 7.7, 10, 0.0);
INSERT INTO `Order Details` Values (10250, 51, 42.4, 35, 0.15);
INSERT INTO `Order Details` Values (10250, 65, 16.8, 15, 0.15);
INSERT INTO `Order Details` Values (10251, 22, 16.8, 6, 0.05);
INSERT INTO `Order Details` Values (10251, 57, 15.6, 15, 0.05);
INSERT INTO `Order Details` Values (10251, 65, 16.8, 20, 0.0);
INSERT INTO `Order Details` Values (10252, 20, 64.8, 40, 0.05);
INSERT INTO `Order Details` Values (10252, 33, 2, 25, 0.05);
INSERT INTO `Order Details` Values (10252, 60, 27.2, 40, 0.0);
INSERT INTO `Order Details` Values (10253, 31, 10, 20, 0.0);
INSERT INTO `Order Details` Values (10253, 39, 14.4, 42, 0.0);
INSERT INTO `Order Details` Values (10253, 49, 16, 40, 0.0);
INSERT INTO `Order Details` Values (10254, 24, 3.6, 15, 0.15);
INSERT INTO `Order Details` Values (10254, 55, 19.2, 21, 0.15);
INSERT INTO `Order Details` Values (10254, 74, 8, 21, 0.0);
INSERT INTO `Order Details` Values (10255, 2, 15.2, 20, 0.0);
INSERT INTO `Order Details` Values (10255, 16, 13.9, 35, 0.0);
INSERT INTO `Order Details` Values (10255, 36, 15.2, 25, 0.0);
INSERT INTO `Order Details` Values (10255, 59, 44, 30, 0.0);
INSERT INTO `Order Details` Values (10256, 53, 26.2, 15, 0.0);
INSERT INTO `Order Details` Values (10256, 77, 10.4, 12, 0.0);
INSERT INTO `Order Details` Values (10257, 27, 35.1, 25, 0.0);
INSERT INTO `Order Details` Values (10257, 39, 14.4, 6, 0.0);
INSERT INTO `Order Details` Values (10257, 77, 10.4, 15, 0.0);
INSERT INTO `Order Details` Values (10258, 2, 15.2, 50, 0.2);
INSERT INTO `Order Details` Values (10258, 5, 17, 65, 0.2);
INSERT INTO `Order Details` Values (10258, 32, 25.6, 6, 0.2);
INSERT INTO `Order Details` Values (10259, 21, 8, 10, 0.0);
INSERT INTO `Order Details` Values (10259, 37, 20.8, 1, 0.0);
INSERT INTO `Order Details` Values (10260, 41, 7.7, 16, 0.25);
INSERT INTO `Order Details` Values (10260, 57, 15.6, 50, 0.0);
INSERT INTO `Order Details` Values (10260, 62, 39.4, 15, 0.25);
INSERT INTO `Order Details` Values (10260, 70, 12, 21, 0.25);
INSERT INTO `Order Details` Values (10261, 21, 8, 20, 0.0);
INSERT INTO `Order Details` Values (10261, 35, 14.4, 20, 0.0);
INSERT INTO `Order Details` Values (10262, 5, 17, 12, 0.2);
INSERT INTO `Order Details` Values (10262, 7, 24, 15, 0.0);
INSERT INTO `Order Details` Values (10262, 56, 30.4, 2, 0.0);
INSERT INTO `Order Details` Values (10263, 16, 13.9, 60, 0.25);
INSERT INTO `Order Details` Values (10263, 24, 3.6, 28, 0.0);
INSERT INTO `Order Details` Values (10263, 30, 20.7, 60, 0.25);
INSERT INTO `Order Details` Values (10263, 74, 8, 36, 0.25);
INSERT INTO `Order Details` Values (10264, 2, 15.2, 35, 0.0);
INSERT INTO `Order Details` Values (10264, 41, 7.7, 25, 0.15);
INSERT INTO `Order Details` Values (10265, 17, 31.2, 30, 0.0);
INSERT INTO `Order Details` Values (10265, 70, 12, 20, 0.0);
INSERT INTO `Order Details` Values (10266, 12, 30.4, 12, 0.05);
INSERT INTO `Order Details` Values (10267, 40, 14.7, 50, 0.0);
INSERT INTO `Order Details` Values (10267, 59, 44, 70, 0.15);
INSERT INTO `Order Details` Values (10267, 76, 14.4, 15, 0.15);
INSERT INTO `Order Details` Values (10268, 29, 99, 10, 0.0);
INSERT INTO `Order Details` Values (10268, 72, 27.8, 4, 0.0);
INSERT INTO `Order Details` Values (10269, 33, 2, 60, 0.05);
INSERT INTO `Order Details` Values (10269, 72, 27.8, 20, 0.05);
INSERT INTO `Order Details` Values (10270, 36, 15.2, 30, 0.0);
INSERT INTO `Order Details` Values (10270, 43, 36.8, 25, 0.0);
INSERT INTO `Order Details` Values (10271, 33, 2, 24, 0.0);
INSERT INTO `Order Details` Values (10272, 20, 64.8, 6, 0.0);
INSERT INTO `Order Details` Values (10272, 31, 10, 40, 0.0);
INSERT INTO `Order Details` Values (10272, 72, 27.8, 24, 0.0);

# ---------------------------------------------------------------------- #
# Add info into "Products"                                               #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Products;
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(1, 'Chai', 1, 1, '10 boxes x 20 bags', 18, 39, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(2, 'Chang', 1, 1, '24 - 12 oz bottles', 19, 17, 40, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(3, 'Aniseed Syrup', 1, 2, '12 - 550 ml bottles', 10, 13, 70, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(4, 'Chef Anton''s Cajun Seasoning', 2, 2, '48 - 6 oz jars', 22, 53, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(5, 'Chef Anton''s Gumbo Mix', 2, 2, '36 boxes', 21.35, 0, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(6, 'Grandma''s Boysenberry Spread', 3, 2, '12 - 8 oz jars', 25, 120, 0, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(7, 'Uncle Bob''s Organic Dried Pears', 3, 7, '12 - 1 lb pkgs.', 30, 15, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(8, 'Northwoods Cranberry Sauce', 3, 2, '12 - 12 oz jars', 40, 6, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(9, 'Mishi Kobe Niku', 4, 6, '18 - 500 g pkgs.', 97, 29, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(10, 'Ikura', 4, 8, '12 - 200 ml jars', 31, 31, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(11, 'Queso Cabrales', 5, 4, '1 kg pkg.', 21, 22, 30, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(12, 'Queso Manchego La Pastora', 5, 4, '10 - 500 g pkgs.', 38, 86, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(13, 'Konbu', 6, 8, '2 kg box', 6, 24, 0, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(14, 'Tofu', 6, 7, '40 - 100 g pkgs.', 23.25, 35, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(15, 'Genen Shouyu', 6, 2, '24 - 250 ml bottles', 15.5, 39, 0, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(16, 'Pavlova', 7, 3, '32 - 500 g boxes', 17.45, 29, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(17, 'Alice Mutton', 7, 6, '20 - 1 kg tins', 39, 0, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(18, 'Carnarvon Tigers', 7, 8, '16 kg pkg.', 62.5, 42, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(19, 'Teatime Chocolate Biscuits', 8, 3, '10 boxes x 12 pieces', 9.2, 25, 0, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(20, 'Sir Rodney''s Marmalade', 8, 3, '30 gift boxes', 81, 40, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(21, 'Sir Rodney''s Scones', 8, 3, '24 pkgs. x 4 pieces', 10, 3, 40, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(22, 'Gustaf''s Knckebrd', 9, 5, '24 - 500 g pkgs.', 21, 104, 0, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(23, 'Tunnbrd', 9, 5, '12 - 250 g pkgs.', 9, 61, 0, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(24, 'Guaran Fantstica', 10, 1, '12 - 355 ml cans', 4.5, 20, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(25, 'NuNuCa Nu-Nougat-Creme', 11, 3, '20 - 450 g glasses', 14, 76, 0, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(26, 'Gumbr Gummibrchen', 11, 3, '100 - 250 g bags', 31.23, 15, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(27, 'Schoggi Schokolade', 11, 3, '100 - 100 g pieces', 43.9, 49, 0, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(28, 'Rssle Sauerkraut', 12, 7, '25 - 825 g cans', 45.6, 26, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(29, 'Thringer Rostbratwurst', 12, 6, '50 bags x 30 sausgs.', 123.79, 0, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(30, 'Nord-Ost Matjeshering', 13, 8, '10 - 200 g glasses', 25.89, 10, 0, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(31, 'Gorgonzola Telino', 14, 4, '12 - 100 g pkgs', 12.5, 0, 70, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(32, 'Mascarpone Fabioli', 14, 4, '24 - 200 g pkgs.', 32, 9, 40, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(33, 'Geitost', 15, 4, '500 g', 2.5, 112, 0, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(34, 'Sasquatch Ale', 16, 1, '24 - 12 oz bottles', 14, 111, 0, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(35, 'Steeleye Stout', 16, 1, '24 - 12 oz bottles', 18, 20, 0, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(36, 'Inlagd Sill', 17, 8, '24 - 250 g  jars', 19, 112, 0, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(37, 'Gravad lax', 17, 8, '12 - 500 g pkgs.', 26, 11, 50, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(38, 'Cte de Blaye', 18, 1, '12 - 75 cl bottles', 263.5, 17, 0, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(39, 'Chartreuse verte', 18, 1, '750 cc per bottle', 18, 69, 0, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(40, 'Boston Crab Meat', 19, 8, '24 - 4 oz tins', 18.4, 123, 0, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(41, 'Jack''s New England Clam Chowder', 19, 8, '12 - 12 oz cans', 9.65, 85, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(42, 'Singaporean Hokkien Fried Mee', 20, 5, '32 - 1 kg pkgs.', 14, 26, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(43, 'Ipoh Coffee', 20, 1, '16 - 500 g tins', 46, 17, 10, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(44, 'Gula Malacca', 20, 2, '20 - 2 kg bags', 19.45, 27, 0, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(45, 'Rogede sild', 21, 8, '1k pkg.', 9.5, 5, 70, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(46, 'Spegesild', 21, 8, '4 - 450 g glasses', 12, 95, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(47, 'Zaanse koeken', 22, 3, '10 - 4 oz boxes', 9.5, 36, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(48, 'Chocolade', 22, 3, '10 pkgs.', 12.75, 15, 70, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(49, 'Maxilaku', 23, 3, '24 - 50 g pkgs.', 20, 10, 60, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(50, 'Valkoinen suklaa', 23, 3, '12 - 100 g bars', 16.25, 65, 0, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(51, 'Manjimup Dried Apples', 24, 7, '50 - 300 g pkgs.', 53, 20, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(52, 'Filo Mix', 24, 5, '16 - 2 kg boxes', 7, 38, 0, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(53, 'Perth Pasties', 24, 6, '48 pieces', 32.8, 0, 0, 0, 1);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(54, 'Tourtire', 25, 6, '16 pies', 7.45, 21, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(55, 'Pt chinois', 25, 6, '24 boxes x 2 pies', 24, 115, 0, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(56, 'Gnocchi di nonna Alice', 26, 5, '24 - 250 g pkgs.', 38, 21, 10, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(57, 'Ravioli Angelo', 26, 5, '24 - 250 g pkgs.', 19.5, 36, 0, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(58, 'Escargots de Bourgogne', 27, 8, '24 pieces', 13.25, 62, 0, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(59, 'Raclette Courdavault', 28, 4, '5 kg pkg.', 55, 79, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(60, 'Camembert Pierrot', 28, 4, '15 - 300 g rounds', 34, 19, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(61, 'Sirop d''rable', 29, 2, '24 - 500 ml bottles', 28.5, 113, 0, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(62, 'Tarte au sucre', 29, 3, '48 pies', 49.3, 17, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(63, 'Vegie-spread', 7, 2, '15 - 625 g jars', 43.9, 24, 0, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(64, 'Wimmers gute Semmelkndel', 12, 5, '20 bags x 4 pieces', 33.25, 22, 80, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(65, 'Louisiana Fiery Hot Pepper Sauce', 2, 2, '32 - 8 oz bottles', 21.05, 76, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(66, 'Louisiana Hot Spiced Okra', 2, 2, '24 - 8 oz jars', 17, 4, 100, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(67, 'Laughing Lumberjack Lager', 16, 1, '24 - 12 oz bottles', 14, 52, 0, 10, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(68, 'Scottish Longbreads', 8, 3, '10 boxes x 8 pieces', 12.5, 6, 10, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(69, 'Gudbrandsdalsost', 15, 4, '10 kg pkg.', 36, 26, 0, 15, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(70, 'Outback Lager', 7, 1, '24 - 355 ml bottles', 15, 15, 10, 30, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(71, 'Flotemysost', 15, 4, '10 - 500 g pkgs.', 21.5, 26, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(72, 'Mozzarella di Giovanni', 14, 4, '24 - 200 g pkgs.', 34.8, 14, 0, 0, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(73, 'Rd Kaviar', 17, 8, '24 - 150 g jars', 15, 101, 0, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(74, 'Longlife Tofu', 4, 7, '5 kg pkg.', 10, 4, 20, 5, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(75, 'Rhnbru Klosterbier', 12, 1, '24 - 0.5 l bottles', 7.75, 125, 0, 25, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(76, 'Lakkalikri', 23, 1, '500 ml', 18, 57, 0, 20, 0);
INSERT INTO Products (ProductID, ProductName, SupplierID, CategoryID, QuantityPerUnit, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued)
VALUES(77, 'Original Frankfurter grne Soe', 12, 2, '12 boxes', 13, 32, 0, 15, 0);

# ---------------------------------------------------------------------- #
# Add info into "Region"                                                 #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Region;
INSERT INTO Region (RegionID, RegionDescription)
VALUES(1, 'Eastern                                           ');
INSERT INTO Region (RegionID, RegionDescription)
VALUES(2, 'Westerns                                           ');
INSERT INTO Region (RegionID, RegionDescription)
VALUES(3, 'Northern                                          ');
INSERT INTO Region (RegionID, RegionDescription)
VALUES(4, 'Southern                                          ');

# ---------------------------------------------------------------------- #
# Add info into "Shippers"                                               #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Shippers;
INSERT INTO Shippers (ShipperID, CompanyName, Phone)
VALUES(1, 'Speedy Express', '(503) 555-9831');
INSERT INTO Shippers (ShipperID, CompanyName, Phone)
VALUES(2, 'United Package', '(503) 555-3199');
INSERT INTO Shippers (ShipperID, CompanyName, Phone)
VALUES(3, 'Federal Shipping', '(503) 555-9931');

# ---------------------------------------------------------------------- #
# Add info into "Suppliers"                                              #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Suppliers;
INSERT INTO Suppliers VALUES(1,'Exotic Liquids','Charlotte Cooper','Purchasing Manager','49 Gilbert St.','London',NULL,'EC1 4SD','UK','(171) 555-2222',NULL,NULL);
INSERT INTO Suppliers VALUES(2,'New Orleans Cajun Delights','Shelley Burke','Order Administrator','P.O. Box 78934','New Orleans','LA','70117','USA','(100) 555-4822',NULL,'#CAJUN.HTM#');
INSERT INTO Suppliers VALUES(3,'Grandma Kelly''s Homestead','Regina Murphy','Sales Representative','707 Oxford Rd.','Ann Arbor','MI','48104','USA','(313) 555-5735','(313) 555-3349',NULL);
INSERT INTO Suppliers VALUES(4,'Tokyo Traders','Yoshi Nagase','Marketing Manager','9-8 Sekimai
Musashino-shi','Tokyo',NULL,'100','Japan','(03) 3555-5011',NULL,NULL);
INSERT INTO Suppliers VALUES(5,'Cooperativa de Quesos ''Las Cabras''','Antonio del Valle Saavedra ','Export Administrator','Calle del Rosal 4','Oviedo','Asturias','33007','Spain','(98) 598 76 54',NULL,NULL);
INSERT INTO Suppliers VALUES(6,'Mayumi''s','Mayumi Ohno','Marketing Representative','92 Setsuko
Chuo-ku','Osaka',NULL,'545','Japan','(06) 431-7877',NULL,'Mayumi''s (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/mayumi.htm#');
INSERT INTO Suppliers VALUES(7,'Pavlova, Ltd.','Ian Devling','Marketing Manager','74 Rose St.
Moonie Ponds','Melbourne','Victoria','3058','Australia','(03) 444-2343','(03) 444-6588',NULL);
INSERT INTO Suppliers VALUES(8,'Specialty Biscuits, Ltd.','Peter Wilson','Sales Representative','29 King''s Way','Manchester',NULL,'M14 GSD','UK','(161) 555-4448',NULL,NULL);
INSERT INTO Suppliers VALUES(9,'PB Knckebrd AB','Lars Peterson','Sales Agent','Kaloadagatan 13','Gteborg',NULL,'S-345 67','Sweden ','031-987 65 43','031-987 65 91',NULL);
INSERT INTO Suppliers VALUES(10,'Refrescos Americanas LTDA','Carlos Diaz','Marketing Manager','Av. das Americanas 12.890','So Paulo',NULL,'5442','Brazil','(11) 555 4640',NULL,NULL);
INSERT INTO Suppliers VALUES(11,'Heli Swaren GmbH & Co. KG','Petra Winkler','Sales Manager','Tiergartenstrae 5','Berlin',NULL,'10785','Germany','(010) 9984510',NULL,NULL);
INSERT INTO Suppliers VALUES(12,'Plutzer Lebensmittelgromrkte AG','Martin Bein','International Marketing Mgr.','Bogenallee 51','Frankfurt',NULL,'60439','Germany','(069) 992755',NULL,'Plutzer (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/plutzer.htm#');
INSERT INTO Suppliers VALUES(13,'Nord-Ost-Fisch Handelsgesellschaft mbH','Sven Petersen','Coordinator Foreign Markets','Frahmredder 112a','Cuxhaven',NULL,'27478','Germany','(04721) 8713','(04721) 8714',NULL);
INSERT INTO Suppliers VALUES(14,'Formaggi Fortini s.r.l.','Elio Rossi','Sales Representative','Viale Dante, 75','Ravenna',NULL,'48100','Italy','(0544) 60323','(0544) 60603','#FORMAGGI.HTM#');
INSERT INTO Suppliers VALUES(15,'Norske Meierier','Beate Vileid','Marketing Manager','Hatlevegen 5','Sandvika',NULL,'1320','Norway','(0)2-953010',NULL,NULL);
INSERT INTO Suppliers VALUES(16,'Bigfoot Breweries','Cheryl Saylor','Regional Account Rep.','3400 - 8th Avenue
Suite 210','Bend','OR','97101','USA','(503) 555-9931',NULL,NULL);
INSERT INTO Suppliers VALUES(17,'Svensk Sjfda AB','Michael Bjrn','Sales Representative','Brovallavgen 231','Stockholm',NULL,'S-123 45','Sweden','08-123 45 67',NULL,NULL);
INSERT INTO Suppliers VALUES(18,'Aux joyeux ecclsiastiques','Guylne Nodier','Sales Manager','203, Rue des Francs-Bourgeois','Paris',NULL,'75004','France','(1) 03.83.00.68','(1) 03.83.00.62',NULL);
INSERT INTO Suppliers VALUES(19,'New England Seafood Cannery','Robb Merchant','Wholesale Account Agent','Order Processing Dept.
2100 Paul Revere Blvd.','Boston','MA','02134','USA','(617) 555-3267','(617) 555-3389',NULL);
INSERT INTO Suppliers VALUES(20,'Leka Trading','Chandra Leka','Owner','471 Serangoon Loop, Suite #402','Singapore',NULL,'0512','Singapore','555-8787',NULL,NULL);
INSERT INTO Suppliers VALUES(21,'Lyngbysild','Niels Petersen','Sales Manager','Lyngbysild
Fiskebakken 10','Lyngby',NULL,'2800','Denmark','43844108','43844115',NULL);
INSERT INTO Suppliers VALUES(22,'Zaanse Snoepfabriek','Dirk Luchte','Accounting Manager','Verkoop
Rijnweg 22','Zaandam',NULL,'9999 ZZ','Netherlands','(12345) 1212','(12345) 1210',NULL);
INSERT INTO Suppliers VALUES(23,'Karkki Oy','Anne Heikkonen','Product Manager','Valtakatu 12','Lappeenranta',NULL,'53120','Finland','(953) 10956',NULL,NULL);
INSERT INTO Suppliers VALUES(24,'G''day, Mate','Wendy Mackenzie','Sales Representative','170 Prince Edward Parade
Hunter''s Hill','Sydney','NSW','2042','Australia','(02) 555-5914','(02) 555-4873','G''day Mate (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/gdaymate.htm#');
INSERT INTO Suppliers VALUES(25,'Ma Maison','Jean-Guy Lauzon','Marketing Manager','2960 Rue St. Laurent','Montral','Qubec','H1J 1C3','Canada','(514) 555-9022',NULL,NULL);
INSERT INTO Suppliers VALUES(26,'Pasta Buttini s.r.l.','Giovanni Giudici','Order Administrator','Via dei Gelsomini, 153','Salerno',NULL,'84100','Italy','(089) 6547665','(089) 6547667',NULL);
INSERT INTO Suppliers VALUES(27,'Escargots Nouveaux','Marie Delamare','Sales Manager','22, rue H. Voiron','Montceau',NULL,'71300','France','85.57.00.07',NULL,NULL);
INSERT INTO Suppliers VALUES(28,'Gai pturage','Eliane Noz','Sales Representative','Bat. B
3, rue des Alpes','Annecy',NULL,'74000','France','38.76.98.06','38.76.98.58',NULL);
INSERT INTO Suppliers VALUES(29,'Forts d''rables','Chantal Goulet','Accounting Manager','148 rue Chasseur','Ste-Hyacinthe','Qubec','J2S 7S8','Canada','(514) 555-2955','(514) 555-2921',NULL);

# ---------------------------------------------------------------------- #
# Add info into "Territories"                                            #
# ---------------------------------------------------------------------- #

TRUNCATE TABLE Territories;
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('01581', 'Westboro                                          ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('01730', 'Bedford                                           ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('01833', 'Georgetow                                         ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('02116', 'Boston                                            ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('02139', 'Cambridge                                         ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('02184', 'Braintree                                         ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('02903', 'Providence                                        ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('03049', 'Hollis                                            ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('03801', 'Portsmouth                                        ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('06897', 'Wilton                                            ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('07960', 'Morristown                                        ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('08837', 'Edison                                            ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('10019', 'New York                                          ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('10038', 'New York                                          ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('11747', 'Mellvile                                          ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('14450', 'Fairport                                          ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('19428', 'Philadelphia                                      ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('19713', 'Neward                                            ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('20852', 'Rockville                                         ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('27403', 'Greensboro                                        ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('27511', 'Cary                                              ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('29202', 'Columbia                                          ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('30346', 'Atlanta                                           ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('31406', 'Savannah                                          ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('32859', 'Orlando                                           ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('33607', 'Tampa                                             ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('40222', 'Louisville                                        ', 1);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('44122', 'Beachwood                                         ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('45839', 'Findlay                                           ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('48075', 'Southfield                                        ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('48084', 'Troy                                              ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('48304', 'Bloomfield Hills                                  ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('53404', 'Racine                                            ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('55113', 'Roseville                                         ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('55439', 'Minneapolis                                       ', 3);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('60179', 'Hoffman Estates                                   ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('60601', 'Chicago                                           ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('72716', 'Bentonville                                       ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('75234', 'Dallas                                            ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('78759', 'Austin                                            ', 4);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('80202', 'Denver                                            ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('80909', 'Colorado Springs                                  ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('85014', 'Phoenix                                           ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('85251', 'Scottsdale                                        ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('90405', 'Santa Monica                                      ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('94025', 'Menlo Park                                        ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('94105', 'San Francisco                                     ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('95008', 'Campbell                                          ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('95054', 'Santa Clara                                       ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('95060', 'Santa Cruz                                        ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('98004', 'Bellevue                                          ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('98052', 'Redmond                                           ', 2);
INSERT INTO Territories (TerritoryID, TerritoryDescription, RegionID)
VALUES('98104', 'Seattle                                           ', 2);


# ---------------------------------------------------------------------- #
# Foreign key constraints                                                #
# ---------------------------------------------------------------------- #

ALTER TABLE `CustomerCustomerDemo` ADD CONSTRAINT `FK_CustomerCustomerDemo` 
    FOREIGN KEY (`CustomerTypeID`) REFERENCES `CustomerDemographics` (`CustomerTypeID`);

ALTER TABLE `CustomerCustomerDemo` ADD CONSTRAINT `FK_CustomerCustomerDemo_customers` 
    FOREIGN KEY (`CustomerID`) REFERENCES `Customers` (`CustomerID`);

ALTER TABLE `Employees` ADD CONSTRAINT `FK_Employees_Employees` 
    FOREIGN KEY (`ReportsTo`) REFERENCES `Employees` (`EmployeeID`);

ALTER TABLE `EmployeeTerritories` ADD CONSTRAINT `FK_EmployeeTerritories_Employees` 
    FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`);

ALTER TABLE `EmployeeTerritories` ADD CONSTRAINT `FK_EmployeeTerritories_Territories` 
    FOREIGN KEY (`TerritoryID`) REFERENCES `Territories` (`TerritoryID`);

ALTER TABLE `Order Details` ADD CONSTRAINT `FK_Order_Details_orders` 
    FOREIGN KEY (`OrderID`) REFERENCES `Orders` (`OrderID`);

ALTER TABLE `Order Details` ADD CONSTRAINT `FK_Order_Details_Products` 
    FOREIGN KEY (`ProductID`) REFERENCES `Products` (`ProductID`);

ALTER TABLE `Orders` ADD CONSTRAINT `FK_orders_customers` 
    FOREIGN KEY (`CustomerID`) REFERENCES `Customers` (`CustomerID`);

ALTER TABLE `Orders` ADD CONSTRAINT `FK_orders_Employees` 
    FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`);

ALTER TABLE `Orders` ADD CONSTRAINT `FK_orders_Shippers` 
    FOREIGN KEY (`ShipVia`) REFERENCES `Shippers` (`ShipperID`);

ALTER TABLE `Products` ADD CONSTRAINT `FK_Products_Categories` 
    FOREIGN KEY (`CategoryID`) REFERENCES `Categories` (`CategoryID`);

ALTER TABLE `Products` ADD CONSTRAINT `FK_Products_Suppliers` 
    FOREIGN KEY (`SupplierID`) REFERENCES `Suppliers` (`SupplierID`);

ALTER TABLE `Territories` ADD CONSTRAINT `FK_Territories_Region` 
    FOREIGN KEY (`RegionID`) REFERENCES `Region` (`RegionID`);
    

--- This table was added to support test cases with a multi part foreign key */
CREATE TABLE `EmployeeOrderDetails` (
	`EmployeeID` INTEGER NOT NULL,
	`OrderID` INTEGER NOT NULL,
	`ProductID` INTEGER NOT NULL,
	CONSTRAINT `PK_EmployeeOrderDetails` PRIMARY KEY ( `EmployeeID`, `OrderID`, `ProductID`) 
);

INSERT INTO `EmployeeOrderDetails`  
SELECT o.EmployeeID, od.OrderId, od.ProductId FROM `Orders` o JOIN `Order Details` od ON o.OrderId = od.OrderId; 
    
ALTER TABLE `EmployeeOrderDetails` ADD CONSTRAINT `FK_EmpoyeeOrderDetails1`
	FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`) ON DELETE CASCADE ;

ALTER TABLE `EmployeeOrderDetails` ADD CONSTRAINT `FK_EmpoyeeOrderDetails2`
	FOREIGN KEY (`OrderID`, `ProductID`) REFERENCES `Order Details` (`OrderID`, `ProductID`) ON DELETE CASCADE ;
    



    
--- This table was added to support test cases for standalone table with 1 pk and no FK constaints */
CREATE TABLE `IndexLog` (
    `id` INTEGER NOT NULL AUTO_INCREMENT,
    `tenantCode` VARCHAR(100) NOT NULL,
    `entityId` INTEGER,
    `entityType` VARCHAR(100),
    `error` VARCHAR(1024),
    `noIndex` tinyint(1) DEFAULT '0',
    `modifiedAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (1, 'us', 100, 'locations', NULL, 0, '2019-02-12 15:44:18');
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (2, 'us', 200, 'locations', NULL, 1, '2019-04-02 15:51:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (3, 'us', 300, 'locations', 'error', 1, '2019-05-02 15:33:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (4, 'us', 567, 'ads', NULL, 0, '2019-01-22 15:51:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (5, 'ca', 837, 'ads', NULL, 0, '2019-05-01 12:03:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (6, 'us', 23, 'ads', NULL, 0, '2019-04-05 11:51:17'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (7, 'us', 24, 'ads', NULL, 0, '2019-04-22 15:05:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (8, 'us', 65, 'ads', NULL, 0, '2019-04-12 13:21:44'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (9, 'us', 765, 'ads', NULL, 0, '2019-04-08 03:00:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (10, 'ca', 239, 'ads', NULL, 0, '2019-04-09 23:32:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (11, 'ca', 8263, 'ads', NULL, 0, '2019-04-16 15:51:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (12, 'us', 103, 'items', NULL, 0, '2019-04-02 15:09:00'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (13, 'us', 105, 'items', NULL, 0, '2019-05-02 21:34:15'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (14, 'us', 23, 'ads', NULL, 0, '2019-05-02 15:51:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (15, 'ca', 8272, 'items', NULL, 0, '2019-05-02 15:51:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (16, 'us', 430, 'ads', 'error',1, '2019-03-01 12:21:22'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (17, 'us', 6252, 'ads', NULL, 0, '2019-05-02 03:51:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (18, 'us', 21, 'ads', NULL, 0, '2019-03-02 13:21:44'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (19, 'us', 23, 'ads', 'ERROR_MSG foo', 0, '2019-05-02 05:05:18'); 
INSERT INTO `IndexLog` (`id`, `tenantCode`, `entityId`, `entityType`, `error`, `noIndex`, `modifiedAt`) VALUES (20, 'us', 567, 'ads', 'some ERROR MSG', 0, '2019-03-02 12:21:22'); 


--- This table was added to support test cases for keys that have non url save characters in them */
CREATE TABLE `Urls` (
    `url` VARCHAR(512) NOT NULL,
    `short` VARCHAR(100) NOT NULL,
    `text` VARCHAR(512) NOT NULL,
     CONSTRAINT `PK_Urls` PRIMARY KEY (`url`, `short`)
);


INSERT INTO `Urls` (`url`, `short`, `text`) VALUES ('http://www.rocketpartners.io/inversion', '74593jd1', 'some description');


