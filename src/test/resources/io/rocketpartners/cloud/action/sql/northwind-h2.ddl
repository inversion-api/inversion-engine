/*---------------------------------------------------------------------- 
 Target DBMS:           H2			                                      
 Project name:          Northwind                                       
 Author:                Johannes Trame                                   
 Created on:            2014-12-27 09:00        
 
 from http://optique-project.eu/training-programme/module-ontop/
 http://www.optique-project.eu/optique-files/training-programme/Northwind.H2.sql
                         
 ---------------------------------------------------------------------- */
DROP SCHEMA IF EXISTS northwind;

CREATE SCHEMA IF NOT EXISTS northwind;

SET SCHEMA northwind;

CREATE TABLE `Categories` (
    `CategoryID` INTEGER NOT NULL AUTO_INCREMENT,
    `CategoryName` VARCHAR(15) NOT NULL,
    `Description` MEDIUMTEXT,
    `Picture` VARCHAR(255),
    CONSTRAINT `PK_Categories` PRIMARY KEY (`CategoryID`)
);

CREATE INDEX `CategoryName` ON `Categories` (`CategoryName`);


CREATE TABLE `CustomerCustomerDemo` (
    `CustomerID` VARCHAR(5) NOT NULL,
    `CustomerTypeID` VARCHAR(10) NOT NULL,
    CONSTRAINT `PK_CustomerCustomerDemo` PRIMARY KEY (`CustomerID`, `CustomerTypeID`)
);



CREATE TABLE `CustomerDemographics` (
    `CustomerTypeID` VARCHAR(10) NOT NULL,
    `CustomerDesc` MEDIUMTEXT,
    CONSTRAINT `PK_CustomerDemographics` PRIMARY KEY (`CustomerTypeID`)
);



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
CREATE INDEX `City` ON `Customers` (`City`);
CREATE INDEX `CompanyName` ON `Customers` (`CompanyName`);
CREATE INDEX `PostalCodeCustomers` ON `Customers` (`PostalCode`);
CREATE INDEX `Region` ON `Customers` (`Region`);



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
    `Notes` MEDIUMTEXT NOT NULL,
    `ReportsTo` INTEGER,
    `PhotoPath` VARCHAR(255),
     `Salary` FLOAT,
    CONSTRAINT `PK_Employees` PRIMARY KEY (`EmployeeID`)
);
CREATE INDEX `LastName` ON `Employees` (`LastName`);
CREATE INDEX `PostalCodeEmpolyees` ON `Employees` (`PostalCode`);



CREATE TABLE `EmployeeTerritories` (
    `EmployeeID` INTEGER NOT NULL,
    `TerritoryID` VARCHAR(20) NOT NULL,
    CONSTRAINT `PK_EmployeeTerritories` PRIMARY KEY (`EmployeeID`, `TerritoryID`)
);



CREATE TABLE `OrderDetails` (
    `OrderID` INTEGER NOT NULL,
    `ProductID` INTEGER NOT NULL,
    `UnitPrice` DECIMAL(10,4) NOT NULL DEFAULT 0,
    `Quantity` SMALLINT(2) NOT NULL DEFAULT 1,
    `Discount` DECIMAL(8,0) NOT NULL DEFAULT 0,
    CONSTRAINT `PK_OrderDetails` PRIMARY KEY (`OrderID`, `ProductID`)
);


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
CREATE INDEX `OrderDate` ON `Orders` (`OrderDate`);
CREATE INDEX `ShippedDate` ON `Orders` (`ShippedDate`);
CREATE INDEX `ShipPostalCode` ON `Orders` (`ShipPostalCode`);



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



CREATE TABLE `Region` (
    `RegionID` INTEGER NOT NULL,
    `RegionDescription` VARCHAR(52) NOT NULL,
    CONSTRAINT `PK_Region` PRIMARY KEY (`RegionID`)
);



CREATE TABLE `Shippers` (
    `ShipperID` INTEGER NOT NULL AUTO_INCREMENT,
    `CompanyName` VARCHAR(40) NOT NULL,
    `Phone` VARCHAR(24),
    CONSTRAINT `PK_Shippers` PRIMARY KEY (`ShipperID`)
);


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
CREATE INDEX `SuppliersCompanyName` ON `Suppliers` (`CompanyName`);
CREATE INDEX `SuppliersPostalCode` ON `Suppliers` (`PostalCode`);


CREATE TABLE `Territories` (
    `TerritoryID` VARCHAR(20) NOT NULL,
    `TerritoryDescription` VARCHAR(50) NOT NULL,
    `RegionID` INTEGER NOT NULL,
    CONSTRAINT `PK_Territories` PRIMARY KEY (`TerritoryID`)
);



TRUNCATE TABLE Categories;
INSERT INTO Categories VALUES(null,'Beverages','Soft drinks, coffees, teas, beers, and ales','/images/northwind-icons/category_beverages.png');
INSERT INTO Categories VALUES(null,'Condiments','Sweet and savory sauces, relishes, spreads, and seasonings','/images/northwind-icons/category_condiments.png');
INSERT INTO Categories VALUES(null,'Confections','Desserts, candies, and sweet breads','/images/northwind-icons/category_confections.png');
INSERT INTO Categories VALUES(null,'Dairy Products','Cheeses','/images/northwind-icons/category_dairy.png');
INSERT INTO Categories VALUES(null,'Grains/Cereals','Breads, crackers, pasta, and cereal','/images/northwind-icons/category_cereals.png');
INSERT INTO Categories VALUES(null,'Meat/Poultry','Prepared meats','/images/northwind-icons/category_meat.png');
INSERT INTO Categories VALUES(null,'Produce','Dried fruit and bean curd','/images/northwind-icons/category_produce.png');
INSERT INTO Categories VALUES(null,'Seafood','Seaweed and fish','/images/northwind-icons/category_seafood.png');


TRUNCATE TABLE Customers;
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ALFKI', 'Alfreds Futterkiste', 'Maria Anders', 'Sales Representative', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany', '030-0074321', '030-0076545');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ANATR', 'Ana Trujillo Emparedados y helados', 'Ana Trujillo', 'Owner', 'Avda. de la Constitucin 2222', 'Mxico D.F.', NULL, '05021', 'Mexico', '(5) 555-4729', '(5) 555-3745');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ANTON', 'Antonio Moreno Taquera', 'Antonio Moreno', 'Owner', 'Mataderos  2312', 'Mxico D.F.', NULL, '05023', 'Mexico', '(5) 555-3932', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('AROUT', 'Around the Horn', 'Thomas Hardy', 'Sales Representative', '120 Hanover Sq.', 'London', NULL, 'WA1 1DP', 'UK', '(171) 555-7788', '(171) 555-6750');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BERGS', 'Berglunds snabbkp', 'Christina Berglund', 'Order Administrator', 'Berguvsvgen  8', 'Lule', NULL, 'S-958 22', 'Sweden', '0921-12 34 65', '0921-12 34 67');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BLAUS', 'Blauer See Delikatessen', 'Hanna Moos', 'Sales Representative', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany', '0621-08460', '0621-08924');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BLONP', 'Blondesddsl pre et fils', 'Frdrique Citeaux', 'Marketing Manager', '24, place Klber', 'Strasbourg', NULL, '67000', 'France', '88.60.15.31', '88.60.15.32');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BOLID', 'Blido Comidas preparadas', 'Martn Sommer', 'Owner', 'C/ Araquil, 67', 'Madrid', NULL, '28023', 'Spain', '(91) 555 22 82', '(91) 555 91 99');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BONAP', 'Bon app''', 'Laurence Lebihan', 'Owner', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France', '91.24.45.40', '91.24.45.41');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BOTTM', 'Bottom-Dollar Markets', 'Elizabeth Lincoln', 'Accounting Manager', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada', '(604) 555-4729', '(604) 555-3745');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('BSBEV', 'B''s Beverages', 'Victoria Ashworth', 'Sales Representative', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK', '(171) 555-1212', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CACTU', 'Cactus Comidas para llevar', 'Patricio Simpson', 'Sales Agent', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina', '(1) 135-5555', '(1) 135-4892');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CENTC', 'Centro comercial Moctezuma', 'Francisco Chang', 'Marketing Manager', 'Sierras de Granada 9993', 'Mxico D.F.', NULL, '05022', 'Mexico', '(5) 555-3392', '(5) 555-7293');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CHOPS', 'Chop-suey Chinese', 'Yang Wang', 'Owner', 'Hauptstr. 29', 'Bern', NULL, '3012', 'Switzerland', '0452-076545', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('COMMI', 'Comrcio Mineiro', 'Pedro Afonso', 'Sales Associate', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil', '(11) 555-7647', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('CONSH', 'Consolidated Holdings', 'Elizabeth Brown', 'Sales Representative', 'Berkeley Gardens 12  Brewery', 'London', NULL, 'WX1 6LT', 'UK', '(171) 555-2282', '(171) 555-9199');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('DRACD', 'Drachenblut Delikatessen', 'Sven Ottlieb', 'Order Administrator', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany', '0241-039123', '0241-059428');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('DUMON', 'Du monde entier', 'Janine Labrune', 'Owner', '67, rue des Cinquante Otages', 'Nantes', NULL, '44000', 'France', '40.67.88.88', '40.67.89.89');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('EASTC', 'Eastern Connection', 'Ann Devon', 'Sales Agent', '35 King George', 'London', NULL, 'WX3 6FW', 'UK', '(171) 555-0297', '(171) 555-3373');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ERNSH', 'Ernst Handel', 'Roland Mendel', 'Sales Manager', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria', '7675-3425', '7675-3426');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FAMIA', 'Familia Arquibaldo', 'Aria Cruz', 'Marketing Assistant', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil', '(11) 555-9857', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FISSA', 'FISSA Fabrica Inter. Salchichas S.A.', 'Diego Roel', 'Accounting Manager', 'C/ Moralzarzal, 86', 'Madrid', NULL, '28034', 'Spain', '(91) 555 94 44', '(91) 555 55 93');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FOLIG', 'Folies gourmandes', 'Martine Ranc', 'Assistant Sales Agent', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France', '20.16.10.16', '20.16.10.17');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FOLKO', 'Folk och f HB', 'Maria Larsson', 'Owner', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden', '0695-34 67 21', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FRANK', 'Frankenversand', 'Peter Franken', 'Marketing Manager', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany', '089-0877310', '089-0877451');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FRANR', 'France restauration', 'Carine Schmitt', 'Marketing Manager', '54, rue Royale', 'Nantes', NULL, '44000', 'France', '40.32.21.21', '40.32.21.20');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FRANS', 'Franchi S.p.A.', 'Paolo Accorti', 'Sales Representative', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy', '011-4988260', '011-4988261');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('FURIB', 'Furia Bacalhau e Frutos do Mar', 'Lino Rodriguez', 'Sales Manager', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal', '(1) 354-2534', '(1) 354-2535');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GALED', 'Galera del gastrnomo', 'Eduardo Saavedra', 'Marketing Manager', 'Rambla de Catalua, 23', 'Barcelona', NULL, '08022', 'Spain', '(93) 203 4560', '(93) 203 4561');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GODOS', 'Godos Cocina Tpica', 'Jos Pedro Freyre', 'Sales Manager', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain', '(95) 555 82 82', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GOURL', 'Gourmet Lanchonetes', 'Andr Fonseca', 'Sales Associate', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil', '(11) 555-9482', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GREAL', 'Great Lakes Food Market', 'Howard Snyder', 'Marketing Manager', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA', '(503) 555-7555', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('GROSR', 'GROSELLA-Restaurante', 'Manuel Pereira', 'Owner', '5 Ave. Los Palos Grandes', 'Caracas', 'DF', '1081', 'Venezuela', '(2) 283-2951', '(2) 283-3397');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HANAR', 'Hanari Carnes', 'Mario Pontes', 'Accounting Manager', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil', '(21) 555-0091', '(21) 555-8765');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HILAA', 'HILARION-Abastos', 'Carlos Hernndez', 'Sales Representative', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela', '(5) 555-1340', '(5) 555-1948');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HUNGC', 'Hungry Coyote Import Store', 'Yoshi Latimer', 'Sales Representative', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA', '(503) 555-6874', '(503) 555-2376');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('HUNGO', 'Hungry Owl All-Night Grocers', 'Patricia McKenna', 'Sales Associate', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland', '2967 542', '2967 3333');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ISLAT', 'Island Trading', 'Helen Bennett', 'Marketing Manager', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK', '(198) 555-8888', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('KOENE', 'Kniglich Essen', 'Philip Cramer', 'Sales Associate', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany', '0555-09876', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LACOR', 'La corne d''abondance', 'Daniel Tonini', 'Sales Representative', '67, avenue de l''Europe', 'Versailles', NULL, '78000', 'France', '30.59.84.10', '30.59.85.11');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LAMAI', 'La maison d''Asie', 'Annette Roulet', 'Sales Manager', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France', '61.77.61.10', '61.77.61.11');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LAUGB', 'Laughing Bacchus Wine Cellars', 'Yoshi Tannamuri', 'Marketing Assistant', '1900 Oak St.', 'Vancouver', 'BC', 'V3F 2K1', 'Canada', '(604) 555-3392', '(604) 555-7293');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LAZYK', 'Lazy K Kountry Store', 'John Steel', 'Marketing Manager', '12 Orchestra Terrace', 'Walla Walla', 'WA', '99362', 'USA', '(509) 555-7969', '(509) 555-6221');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LEHMS', 'Lehmanns Marktstand', 'Renate Messner', 'Sales Representative', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany', '069-0245984', '069-0245874');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LETSS', 'Let''s Stop N Shop', 'Jaime Yorres', 'Owner', '87 Polk St. Suite 5', 'San Francisco', 'CA', '94117', 'USA', '(415) 555-5938', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LILAS', 'LILA-Supermercado', 'Carlos Gonzlez', 'Accounting Manager', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela', '(9) 331-6954', '(9) 331-7256');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LINOD', 'LINO-Delicateses', 'Felipe Izquierdo', 'Owner', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela', '(8) 34-56-12', '(8) 34-93-93');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('LONEP', 'Lonesome Pine Restaurant', 'Fran Wilson', 'Sales Manager', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA', '(503) 555-9573', '(503) 555-9646');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MAGAA', 'Magazzini Alimentari Riuniti', 'Giovanni Rovelli', 'Marketing Manager', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy', '035-640230', '035-640231');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MAISD', 'Maison Dewey', 'Catherine Dewey', 'Sales Agent', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium', '(02) 201 24 67', '(02) 201 24 68');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MEREP', 'Mre Paillarde', 'Jean Fresnire', 'Marketing Assistant', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada', '(514) 555-8054', '(514) 555-8055');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('MORGK', 'Morgenstern Gesundkost', 'Alexander Feuer', 'Marketing Assistant', 'Heerstr. 22', 'Leipzig', NULL, '04179', 'Germany', '0342-023176', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('NORTS', 'North/South', 'Simon Crowther', 'Sales Associate', 'South House 300 Queensbridge', 'London', NULL, 'SW7 1RZ', 'UK', '(171) 555-7733', '(171) 555-2530');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('OCEAN', 'Ocano Atlntico Ltda.', 'Yvonne Moncada', 'Sales Agent', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina', '(1) 135-5333', '(1) 135-5535');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('OLDWO', 'Old World Delicatessen', 'Rene Phillips', 'Sales Representative', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA', '(907) 555-7584', '(907) 555-2880');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('OTTIK', 'Ottilies Kseladen', 'Henriette Pfalzheim', 'Owner', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany', '0221-0644327', '0221-0765721');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PARIS', 'Paris spcialits', 'Marie Bertrand', 'Owner', '265, boulevard Charonne', 'Paris', NULL, '75012', 'France', '(1) 42.34.22.66', '(1) 42.34.22.77');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PERIC', 'Pericles Comidas clsicas', 'Guillermo Fernndez', 'Sales Representative', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '05033', 'Mexico', '(5) 552-3745', '(5) 545-3745');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PICCO', 'Piccolo und mehr', 'Georg Pipps', 'Sales Manager', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria', '6562-9722', '6562-9723');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('PRINI', 'Princesa Isabel Vinhos', 'Isabel de Castro', 'Sales Representative', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal', '(1) 356-5634', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('QUEDE', 'Que Delcia', 'Bernardo Batista', 'Accounting Manager', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil', '(21) 555-4252', '(21) 555-4545');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('QUEEN', 'Queen Cozinha', 'Lcia Carvalho', 'Marketing Assistant', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil', '(11) 555-1189', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('QUICK', 'QUICK-Stop', 'Horst Kloss', 'Accounting Manager', 'Taucherstrae 10', 'Cunewalde', NULL, '01307', 'Germany', '0372-035188', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RANCH', 'Rancho grande', 'Sergio Gutirrez', 'Sales Representative', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina', '(1) 123-5555', '(1) 123-5556');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RATTC', 'Rattlesnake Canyon Grocery', 'Paula Wilson', 'Assistant Sales Representative', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA', '(505) 555-5939', '(505) 555-3620');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('REGGC', 'Reggiani Caseifici', 'Maurizio Moroni', 'Sales Associate', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy', '0522-556721', '0522-556722');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RICAR', 'Ricardo Adocicados', 'Janete Limeira', 'Assistant Sales Agent', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil', '(21) 555-3412', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('RICSU', 'Richter Supermarkt', 'Michael Holz', 'Sales Manager', 'Grenzacherweg 237', 'Genve', NULL, '1203', 'Switzerland', '0897-034214', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('ROMEY', 'Romero y tomillo', 'Alejandra Camino', 'Accounting Manager', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain', '(91) 745 6200', '(91) 745 6210');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SANTG', 'Sant Gourmet', 'Jonas Bergulfsen', 'Owner', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway', '07-98 92 35', '07-98 92 47');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SAVEA', 'Save-a-lot Markets', 'Jose Pavarotti', 'Sales Representative', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA', '(208) 555-8097', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SEVES', 'Seven Seas Imports', 'Hari Kumar', 'Sales Manager', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK', '(171) 555-1717', '(171) 555-5646');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SIMOB', 'Simons bistro', 'Jytte Petersen', 'Owner', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark', '31 12 34 56', '31 13 35 57');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SPECD', 'Spcialits du monde', 'Dominique Perrier', 'Marketing Manager', '25, rue Lauriston', 'Paris', NULL, '75016', 'France', '(1) 47.55.60.10', '(1) 47.55.60.20');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SPLIR', 'Split Rail Beer & Ale', 'Art Braunschweiger', 'Sales Manager', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA', '(307) 555-4680', '(307) 555-6525');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('SUPRD', 'Suprmes dlices', 'Pascale Cartrain', 'Accounting Manager', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium', '(071) 23 67 22 20', '(071) 23 67 22 21');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('THEBI', 'The Big Cheese', 'Liz Nixon', 'Marketing Manager', '89 Jefferson Way Suite 2', 'Portland', 'OR', '97201', 'USA', '(503) 555-3612', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('THECR', 'The Cracker Box', 'Liu Wong', 'Marketing Assistant', '55 Grizzly Peak Rd.', 'Butte', 'MT', '59801', 'USA', '(406) 555-5834', '(406) 555-8083');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TOMSP', 'Toms Spezialitten', 'Karin Josephs', 'Marketing Manager', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany', '0251-031259', '0251-035695');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TORTU', 'Tortuga Restaurante', 'Miguel Angel Paolino', 'Owner', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '05033', 'Mexico', '(5) 555-2933', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TRADH', 'Tradio Hipermercados', 'Anabela Domingues', 'Sales Representative', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil', '(11) 555-2167', '(11) 555-2168');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('TRAIH', 'Trail''s Head Gourmet Provisioners', 'Helvetius Nagy', 'Sales Associate', '722 DaVinci Blvd.', 'Kirkland', 'WA', '98034', 'USA', '(206) 555-8257', '(206) 555-2174');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VAFFE', 'Vaffeljernet', 'Palle Ibsen', 'Sales Manager', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark', '86 21 32 43', '86 22 33 44');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('Val2 ', 'IT', 'Val2', 'IT', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VALON', 'IT', 'Valon Hoti', 'IT', NULL, NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VICTE', 'Victuailles en stock', 'Mary Saveley', 'Sales Agent', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France', '78.32.54.86', '78.32.54.87');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('VINET', 'Vins et alcools Chevalier', 'Paul Henriot', 'Accounting Manager', '59 rue de l''Abbaye', 'Reims', NULL, '51100', 'France', '26.47.15.10', '26.47.15.11');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WANDK', 'Die Wandernde Kuh', 'Rita Mller', 'Sales Representative', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany', '0711-020361', '0711-035428');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WARTH', 'Wartian Herkku', 'Pirkko Koskitalo', 'Accounting Manager', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland', '981-443655', '981-443655');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WELLI', 'Wellington Importadora', 'Paula Parente', 'Sales Manager', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil', '(14) 555-8122', NULL);
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WHITC', 'White Clover Markets', 'Karl Jablonski', 'Owner', '305 - 14th Ave. S. Suite 3B', 'Seattle', 'WA', '98128', 'USA', '(206) 555-4112', '(206) 555-4115');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WILMK', 'Wilman Kala', 'Matti Karttunen', 'Owner/Marketing Assistant', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland', '90-224 8858', '90-224 8858');
INSERT INTO Customers (CustomerID, CompanyName, ContactName, ContactTitle, Address, City, Region, PostalCode, Country, Phone, Fax)
VALUES('WOLZA', 'Wolski  Zajazd', 'Zbyszek Piestrzeniewicz', 'Owner', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland', '(26) 642-7012', '(26) 642-7012');



TRUNCATE TABLE Employees;
INSERT INTO Employees VALUES(null,'Davolio','Nancy','Sales Representative','Ms.','1948-12-08','1992-05-01','507 - 20th Ave. E.Apt. 2A','Seattle','WA','98122','USA','(206) 555-9857','5467','Education includes a BA in psychology from Colorado State University in 1970.  She also completed "The Art of the Cold Call."  Nancy is a member of Toastmasters International.',2
,'/images/northwind-icons/employee_woman_1.png','2954.55');
INSERT INTO Employees VALUES(null,'Fuller','Andrew','Vice President, Sales','Dr.','1952-02-19','1992-08-14','908 W. Capital Way','Tacoma','WA','98401','USA','(206) 555-9482','3457','Andrew received his BTS commercial in 1974 and a Ph.D. in international marketing from the University of Dallas in 1981.  He is fluent in French and Italian and reads German.  He joined the company as a sales representative, was promoted to sales manager in January 1992 and to vice president of sales in March 1993.  Andrew is a member of the Sales Management Roundtable, the Seattle Chamber of Commerce, and the Pacific Rim Importers Association.',NULL
,'/images/northwind-icons/employee_man_1.png','2254.49');
INSERT INTO Employees VALUES(null,'Leverling','Janet','Sales Representative','Ms.','1963-08-30','1992-04-01','722 Moss Bay Blvd.','Kirkland','WA','98033','USA','(206) 555-3412','3355','Janet has a BS degree in chemistry from Boston College (1984).  She has also completed a certificate program in food retailing management.  Janet was hired as a sales associate in 1991 and promoted to sales representative in February 1992.',2
,'/images/northwind-icons/employee_woman_2.png','3119.15');
INSERT INTO Employees VALUES(null,'Peacock','Margaret','Sales Representative','Mrs.','1937-09-19','1993-05-03','4110 Old Redmond Rd.','Redmond','WA','98052','USA','(206) 555-8122','5176','Margaret holds a BA in English literature from Concordia College (1958) and an MA from the American Institute of Culinary Arts (1966).  She was assigned to the London office temporarily from July through November 1992.',2
,'/images/northwind-icons/employee_woman_3.png','1861.08');
INSERT INTO Employees VALUES(null,'Buchanan','Steven','Sales Manager','Mr.','1955-03-04','1993-10-17','14 Garrett Hill','London',NULL,'SW1 8JR','UK','(71) 555-4848','3453','Steven Buchanan graduated from St. Andrews University, Scotland, with a BSC degree in 1976.  Upon joining the company as a sales representative in 1992, he spent 6 months in an orientation program at the Seattle office and then returned to his permanent post in London.  He was promoted to sales manager in March 1993.  Mr. Buchanan has completed the courses "Successful Telemarketing" and "International Sales Management."  He is fluent in French.',2
,'/images/northwind-icons/employee_man_2.png','1744.21');
INSERT INTO Employees VALUES(null,'Suyama','Michael','Sales Representative','Mr.','1963-07-02','1993-10-17','Coventry House
Miner Rd.','London',NULL,'EC2 7JR','UK','(71) 555-7773','428','Michael is a graduate of Sussex University (MA, economics, 1983) and the University of California at Los Angeles (MBA, marketing, 1986).  He has also taken the courses "Multi-Cultural Selling" and "Time Management for the Sales Professional."  He is fluent in Japanese and can read and write French, Portuguese, and Spanish.',5
,'/images/northwind-icons/employee_man_3.png','2004.07');
INSERT INTO Employees VALUES(null,'King','Robert','Sales Representative','Mr.','1960-05-29','1994-01-02','Edgeham Hollow
Winchester Way','London',NULL,'RG1 9SP','UK','(71) 555-5598','465','Robert King served in the Peace Corps and traveled extensively before completing his degree in English at the University of Michigan in 1992, the year he joined the company.  After completing a course entitled "Selling in Europe," he was transferred to the London office in March 1993.',5
,'/images/northwind-icons/employee_man_4.png','1991.55');
INSERT INTO Employees VALUES(null,'Callahan','Laura','Inside Sales Coordinator','Ms.','1958-01-09','1994-03-05','4726 - 11th Ave. N.E.','Seattle','WA','98105','USA','(206) 555-1189','2344','Laura received a BA in psychology from the University of Washington.  She has also completed a course in business French.  She reads and writes French.',2
,'/images/northwind-icons/employee_woman_4.png','2100.50');
INSERT INTO Employees VALUES(null,'Dodsworth','Anne','Sales Representative','Ms.','1966-01-27','1994-11-15','7 Houndstooth Rd.','London',NULL,'WG2 7LT','UK','(71) 555-4444','452','Anne has a BA degree in English from St. Lawrence College.  She is fluent in French and German.',5
,'/images/northwind-icons/employee_woman_5.png','2333.33');



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



INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10248, 'VINET', 5, TIMESTAMP '2013-01-04 00:00:00.0', TIMESTAMP '2013-02-01 00:00:00.0', TIMESTAMP '2013-01-16 00:00:00.0', 3, 32.3800, 'Vins et alcools Chevalier', '59 rue de l-Abbaye', 'Reims', NULL, '51100', 'France'),
(10249, 'TOMSP', 6, TIMESTAMP '2013-01-05 00:00:00.0', TIMESTAMP '2013-02-16 00:00:00.0', TIMESTAMP '2013-01-10 00:00:00.0', 1, 11.6100, 'Toms Spezialitten', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany'),
(10250, 'HANAR', 4, TIMESTAMP '2013-01-08 00:00:00.0', TIMESTAMP '2013-02-05 00:00:00.0', TIMESTAMP '2013-01-12 00:00:00.0', 2, 65.8300, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10251, 'VICTE', 3, TIMESTAMP '2013-01-08 00:00:00.0', TIMESTAMP '2013-02-05 00:00:00.0', TIMESTAMP '2013-01-15 00:00:00.0', 1, 41.3400, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10252, 'SUPRD', 4, TIMESTAMP '2013-01-09 00:00:00.0', TIMESTAMP '2013-02-06 00:00:00.0', TIMESTAMP '2013-01-11 00:00:00.0', 2, 51.3000, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10253, 'HANAR', 3, TIMESTAMP '2013-01-10 00:00:00.0', TIMESTAMP '2013-01-24 00:00:00.0', TIMESTAMP '2013-01-16 00:00:00.0', 2, 58.1700, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10254, 'CHOPS', 5, TIMESTAMP '2013-01-11 00:00:00.0', TIMESTAMP '2013-02-08 00:00:00.0', TIMESTAMP '2013-01-23 00:00:00.0', 2, 22.9800, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(10255, 'RICSU', 9, TIMESTAMP '2013-01-12 00:00:00.0', TIMESTAMP '2013-02-09 00:00:00.0', TIMESTAMP '2013-01-15 00:00:00.0', 3, 148.3300, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10256, 'WELLI', 3, TIMESTAMP '2013-01-15 00:00:00.0', TIMESTAMP '2013-02-12 00:00:00.0', TIMESTAMP '2013-01-17 00:00:00.0', 2, 13.9700, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10257, 'HILAA', 4, TIMESTAMP '2013-01-16 00:00:00.0', TIMESTAMP '2013-02-13 00:00:00.0', TIMESTAMP '2013-01-22 00:00:00.0', 3, 81.9100, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10258, 'ERNSH', 1, TIMESTAMP '2013-01-17 00:00:00.0', TIMESTAMP '2013-02-14 00:00:00.0', TIMESTAMP '2013-01-23 00:00:00.0', 1, 140.5100, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10259, 'CENTC', 4, TIMESTAMP '2013-01-18 00:00:00.0', TIMESTAMP '2013-02-15 00:00:00.0', TIMESTAMP '2013-01-25 00:00:00.0', 3, 3.2500, 'Centro comercial Moctezuma', 'Sierras de Granada 9993', 'Mxico D.F.', NULL, '5022', 'Mexico'),
(10260, 'OTTIK', 4, TIMESTAMP '2013-01-19 00:00:00.0', TIMESTAMP '2013-02-16 00:00:00.0', TIMESTAMP '2013-01-29 00:00:00.0', 1, 55.0900, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10261, 'QUEDE', 4, TIMESTAMP '2013-01-19 00:00:00.0', TIMESTAMP '2013-02-16 00:00:00.0', TIMESTAMP '2013-01-30 00:00:00.0', 2, 3.0500, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10262, 'RATTC', 8, TIMESTAMP '2013-01-22 00:00:00.0', TIMESTAMP '2013-02-19 00:00:00.0', TIMESTAMP '2013-01-25 00:00:00.0', 3, 48.2900, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10263, 'ERNSH', 9, TIMESTAMP '2013-01-23 00:00:00.0', TIMESTAMP '2013-02-20 00:00:00.0', TIMESTAMP '2013-01-31 00:00:00.0', 3, 146.0600, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10264, 'FOLKO', 6, TIMESTAMP '2013-01-24 00:00:00.0', TIMESTAMP '2013-02-21 00:00:00.0', TIMESTAMP '2013-02-23 00:00:00.0', 3, 3.6700, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10265, 'BLONP', 2, TIMESTAMP '2013-01-25 00:00:00.0', TIMESTAMP '2013-02-22 00:00:00.0', TIMESTAMP '2013-02-12 00:00:00.0', 1, 55.2800, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France');
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10266, 'WARTH', 3, TIMESTAMP '2013-01-26 00:00:00.0', TIMESTAMP '2013-03-06 00:00:00.0', TIMESTAMP '2013-01-31 00:00:00.0', 3, 25.7300, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10267, 'FRANK', 4, TIMESTAMP '2013-01-29 00:00:00.0', TIMESTAMP '2013-02-26 00:00:00.0', TIMESTAMP '2013-02-06 00:00:00.0', 1, 208.5800, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10268, 'GROSR', 8, TIMESTAMP '2013-01-30 00:00:00.0', TIMESTAMP '2013-02-27 00:00:00.0', TIMESTAMP '2013-02-02 00:00:00.0', 3, 66.2900, 'GROSELLA-Restaurante', '5 Ave. Los Palos Grandes', 'Caracas', 'DF', '1081', 'Venezuela'),
(10269, 'WHITC', 5, TIMESTAMP '2013-01-31 00:00:00.0', TIMESTAMP '2013-02-14 00:00:00.0', TIMESTAMP '2013-02-09 00:00:00.0', 1, 4.5600, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10270, 'WARTH', 1, TIMESTAMP '2013-02-01 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-02-02 00:00:00.0', 1, 136.5400, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10271, 'SPLIR', 6, TIMESTAMP '2013-02-01 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', 2, 4.5400, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10272, 'RATTC', 6, TIMESTAMP '2013-02-02 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-02-06 00:00:00.0', 2, 98.0300, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10273, 'QUICK', 3, TIMESTAMP '2013-02-05 00:00:00.0', TIMESTAMP '2013-03-02 00:00:00.0', TIMESTAMP '2013-02-12 00:00:00.0', 3, 76.0700, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10274, 'VINET', 6, TIMESTAMP '2013-02-06 00:00:00.0', TIMESTAMP '2013-03-03 00:00:00.0', TIMESTAMP '2013-02-16 00:00:00.0', 1, 6.0100, 'Vins et alcools Chevalier', '59 rue de l-Abbaye', 'Reims', NULL, '51100', 'France'),
(10275, 'MAGAA', 1, TIMESTAMP '2013-02-07 00:00:00.0', TIMESTAMP '2013-03-04 00:00:00.0', TIMESTAMP '2013-02-09 00:00:00.0', 1, 26.9300, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10276, 'TORTU', 8, TIMESTAMP '2013-02-08 00:00:00.0', TIMESTAMP '2013-02-22 00:00:00.0', TIMESTAMP '2013-02-14 00:00:00.0', 3, 13.8400, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10277, 'MORGK', 2, TIMESTAMP '2013-02-09 00:00:00.0', TIMESTAMP '2013-03-06 00:00:00.0', TIMESTAMP '2013-02-13 00:00:00.0', 3, 125.7700, 'Morgenstern Gesundkost', 'Heerstr. 22', 'Leipzig', NULL, '4179', 'Germany'),
(10278, 'BERGS', 8, TIMESTAMP '2013-02-12 00:00:00.0', TIMESTAMP '2013-03-09 00:00:00.0', TIMESTAMP '2013-02-16 00:00:00.0', 2, 92.6900, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10279, 'LEHMS', 8, TIMESTAMP '2013-02-13 00:00:00.0', TIMESTAMP '2013-03-10 00:00:00.0', TIMESTAMP '2013-02-16 00:00:00.0', 2, 25.8300, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10280, 'BERGS', 2, TIMESTAMP '2013-02-14 00:00:00.0', TIMESTAMP '2013-03-11 00:00:00.0', TIMESTAMP '2013-03-12 00:00:00.0', 1, 8.9800, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10281, 'ROMEY', 4, TIMESTAMP '2013-02-14 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-02-21 00:00:00.0', 1, 2.9400, 'Romero y tomillo', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain'),
(10282, 'ROMEY', 4, TIMESTAMP '2013-02-15 00:00:00.0', TIMESTAMP '2013-03-12 00:00:00.0', TIMESTAMP '2013-02-21 00:00:00.0', 1, 12.6900, 'Romero y tomillo', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain'),
(10283, 'LILAS', 3, TIMESTAMP '2013-02-16 00:00:00.0', TIMESTAMP '2013-03-13 00:00:00.0', TIMESTAMP '2013-02-23 00:00:00.0', 3, 84.8100, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10284, 'LEHMS', 4, TIMESTAMP '2013-02-19 00:00:00.0', TIMESTAMP '2013-03-16 00:00:00.0', TIMESTAMP '2013-02-27 00:00:00.0', 1, 76.5600, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10285, 'QUICK', 1, TIMESTAMP '2013-02-20 00:00:00.0', TIMESTAMP '2013-03-17 00:00:00.0', TIMESTAMP '2013-02-26 00:00:00.0', 2, 76.8300, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10286, 'QUICK', 8, TIMESTAMP '2013-02-21 00:00:00.0', TIMESTAMP '2013-03-18 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', 3, 229.2400, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10287, 'RICAR', 8, TIMESTAMP '2013-02-22 00:00:00.0', TIMESTAMP '2013-03-19 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', 3, 12.7600, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10288, 'REGGC', 4, TIMESTAMP '2013-02-23 00:00:00.0', TIMESTAMP '2013-03-20 00:00:00.0', TIMESTAMP '2013-03-03 00:00:00.0', 1, 7.4500, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10289, 'BSBEV', 7, TIMESTAMP '2013-02-26 00:00:00.0', TIMESTAMP '2013-03-23 00:00:00.0', TIMESTAMP '2013-02-28 00:00:00.0', 3, 22.7700, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10290, 'COMMI', 8, TIMESTAMP '2013-02-27 00:00:00.0', TIMESTAMP '2013-03-24 00:00:00.0', TIMESTAMP '2013-03-03 00:00:00.0', 1, 79.7000, 'Comrcio Mineiro', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil'),
(10291, 'QUEDE', 6, TIMESTAMP '2013-02-27 00:00:00.0', TIMESTAMP '2013-03-24 00:00:00.0', TIMESTAMP '2013-03-04 00:00:00.0', 2, 6.4000, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10292, 'TRADH', 1, TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-03-25 00:00:00.0', TIMESTAMP '2013-03-02 00:00:00.0', 2, 1.3500, 'Tradiao Hipermercados', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil'),
(10293, 'TORTU', 1, TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-03-26 00:00:00.0', TIMESTAMP '2013-03-11 00:00:00.0', 3, 21.1800, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10294, 'RATTC', 4, TIMESTAMP '2013-02-28 00:00:00.0', TIMESTAMP '2013-03-27 00:00:00.0', TIMESTAMP '2013-03-05 00:00:00.0', 2, 147.2600, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10295, 'VINET', 2, TIMESTAMP '2013-03-02 00:00:00.0', TIMESTAMP '2013-03-30 00:00:00.0', TIMESTAMP '2013-03-10 00:00:00.0', 2, 1.1500, 'Vins et alcools Chevalier', '59 rue de l-Abbaye', 'Reims', NULL, '51100', 'France'),
(10296, 'LILAS', 6, TIMESTAMP '2013-03-03 00:00:00.0', TIMESTAMP '2013-04-01 00:00:00.0', TIMESTAMP '2013-03-11 00:00:00.0', 1, 0.1200, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10297, 'BLONP', 5, TIMESTAMP '2013-03-04 00:00:00.0', TIMESTAMP '2013-04-16 00:00:00.0', TIMESTAMP '2013-03-10 00:00:00.0', 2, 5.7400, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10298, 'HUNGO', 6, TIMESTAMP '2013-03-05 00:00:00.0', TIMESTAMP '2013-04-03 00:00:00.0', TIMESTAMP '2013-03-11 00:00:00.0', 2, 168.2200, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10299, 'RICAR', 4, TIMESTAMP '2013-03-06 00:00:00.0', TIMESTAMP '2013-04-04 00:00:00.0', TIMESTAMP '2013-03-13 00:00:00.0', 2, 29.7600, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10300, 'MAGAA', 2, TIMESTAMP '2013-03-09 00:00:00.0', TIMESTAMP '2013-04-07 00:00:00.0', TIMESTAMP '2013-03-18 00:00:00.0', 2, 17.6800, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10301, 'WANDK', 8, TIMESTAMP '2013-03-09 00:00:00.0', TIMESTAMP '2013-04-07 00:00:00.0', TIMESTAMP '2013-03-17 00:00:00.0', 2, 45.0800, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany');            
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10302, 'SUPRD', 4, TIMESTAMP '2013-03-10 00:00:00.0', TIMESTAMP '2013-04-08 00:00:00.0', TIMESTAMP '2013-04-09 00:00:00.0', 2, 6.2700, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10303, 'GODOS', 7, TIMESTAMP '2013-03-11 00:00:00.0', TIMESTAMP '2013-04-09 00:00:00.0', TIMESTAMP '2013-03-18 00:00:00.0', 2, 107.8300, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10304, 'TORTU', 1, TIMESTAMP '2013-03-12 00:00:00.0', TIMESTAMP '2013-04-10 00:00:00.0', TIMESTAMP '2013-03-17 00:00:00.0', 2, 63.7900, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10305, 'OLDWO', 8, TIMESTAMP '2013-03-13 00:00:00.0', TIMESTAMP '2013-04-11 00:00:00.0', TIMESTAMP '2013-04-09 00:00:00.0', 3, 257.6200, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10306, 'ROMEY', 1, TIMESTAMP '2013-03-16 00:00:00.0', TIMESTAMP '2013-04-14 00:00:00.0', TIMESTAMP '2013-03-23 00:00:00.0', 3, 7.5600, 'Romero y tomillo', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain'),
(10307, 'LONEP', 2, TIMESTAMP '2013-03-17 00:00:00.0', TIMESTAMP '2013-04-15 00:00:00.0', TIMESTAMP '2013-03-25 00:00:00.0', 2, 0.5600, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(10308, 'ANATR', 7, TIMESTAMP '2013-03-18 00:00:00.0', TIMESTAMP '2013-04-16 00:00:00.0', TIMESTAMP '2013-03-24 00:00:00.0', 3, 1.6100, 'Ana Trujillo Emparedados y helados', 'Avda. de la Constitucin 2222', 'Mxico D.F.', NULL, '5021', 'Mexico'),
(10309, 'HUNGO', 3, TIMESTAMP '2013-03-19 00:00:00.0', TIMESTAMP '2013-04-17 00:00:00.0', TIMESTAMP '2013-04-23 00:00:00.0', 1, 47.3000, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10310, 'THEBI', 8, TIMESTAMP '2013-03-20 00:00:00.0', TIMESTAMP '2013-04-18 00:00:00.0', TIMESTAMP '2013-03-27 00:00:00.0', 2, 17.5200, 'The Big Cheese', '89 Jefferson Way Suite 2', 'Portland', 'OR', '97201', 'USA'),
(10311, 'DUMON', 1, TIMESTAMP '2013-03-20 00:00:00.0', TIMESTAMP '2013-04-04 00:00:00.0', TIMESTAMP '2013-03-26 00:00:00.0', 3, 24.6900, 'Du monde entier', '67, rue des Cinquante Otages', 'Nantes', NULL, '44000', 'France'),
(10312, 'WANDK', 2, TIMESTAMP '2013-03-23 00:00:00.0', TIMESTAMP '2013-04-21 00:00:00.0', TIMESTAMP '2013-04-03 00:00:00.0', 2, 40.2600, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10313, 'QUICK', 2, TIMESTAMP '2013-03-24 00:00:00.0', TIMESTAMP '2013-04-22 00:00:00.0', TIMESTAMP '2013-04-04 00:00:00.0', 2, 1.9600, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10314, 'RATTC', 1, TIMESTAMP '2013-03-25 00:00:00.0', TIMESTAMP '2013-04-23 00:00:00.0', TIMESTAMP '2013-04-04 00:00:00.0', 2, 74.1600, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10315, 'ISLAT', 4, TIMESTAMP '2013-03-26 00:00:00.0', TIMESTAMP '2013-04-24 00:00:00.0', TIMESTAMP '2013-04-03 00:00:00.0', 2, 41.7600, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10316, 'RATTC', 1, TIMESTAMP '2013-03-27 00:00:00.0', TIMESTAMP '2013-04-25 00:00:00.0', TIMESTAMP '2013-04-08 00:00:00.0', 3, 150.1500, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10317, 'LONEP', 6, TIMESTAMP '2013-03-30 00:00:00.0', TIMESTAMP '2013-04-28 00:00:00.0', TIMESTAMP '2013-04-10 00:00:00.0', 1, 12.6900, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(10318, 'ISLAT', 8, TIMESTAMP '2013-04-01 00:00:00.0', TIMESTAMP '2013-04-29 00:00:00.0', TIMESTAMP '2013-04-04 00:00:00.0', 2, 4.7300, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10319, 'TORTU', 7, TIMESTAMP '2013-04-02 00:00:00.0', TIMESTAMP '2013-04-30 00:00:00.0', TIMESTAMP '2013-04-11 00:00:00.0', 3, 64.5000, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico');      
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10320, 'WARTH', 5, TIMESTAMP '2013-04-03 00:00:00.0', TIMESTAMP '2013-04-17 00:00:00.0', TIMESTAMP '2013-04-18 00:00:00.0', 3, 34.5700, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10321, 'ISLAT', 3, TIMESTAMP '2013-04-03 00:00:00.0', TIMESTAMP '2013-04-30 00:00:00.0', TIMESTAMP '2013-04-11 00:00:00.0', 2, 3.4300, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10322, 'PERIC', 7, TIMESTAMP '2013-04-04 00:00:00.0', TIMESTAMP '2013-05-01 00:00:00.0', TIMESTAMP '2013-04-23 00:00:00.0', 3, 0.4000, 'Pericles Comidas clsicas', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10323, 'KOENE', 4, TIMESTAMP '2013-04-07 00:00:00.0', TIMESTAMP '2013-05-04 00:00:00.0', TIMESTAMP '2013-04-14 00:00:00.0', 1, 4.8800, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10324, 'SAVEA', 9, TIMESTAMP '2013-04-08 00:00:00.0', TIMESTAMP '2013-05-05 00:00:00.0', TIMESTAMP '2013-04-10 00:00:00.0', 1, 214.2700, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10325, 'KOENE', 1, TIMESTAMP '2013-04-09 00:00:00.0', TIMESTAMP '2013-04-23 00:00:00.0', TIMESTAMP '2013-04-14 00:00:00.0', 3, 64.8600, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10326, 'BOLID', 4, TIMESTAMP '2013-04-10 00:00:00.0', TIMESTAMP '2013-05-07 00:00:00.0', TIMESTAMP '2013-04-14 00:00:00.0', 2, 77.9200, 'Blido Comidas preparadas', 'C/ Araquil, 67', 'Madrid', NULL, '28023', 'Spain'),
(10327, 'FOLKO', 2, TIMESTAMP '2013-04-11 00:00:00.0', TIMESTAMP '2013-05-08 00:00:00.0', TIMESTAMP '2013-04-14 00:00:00.0', 1, 63.3600, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10328, 'FURIB', 4, TIMESTAMP '2013-04-14 00:00:00.0', TIMESTAMP '2013-05-11 00:00:00.0', TIMESTAMP '2013-04-17 00:00:00.0', 3, 87.0300, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10329, 'SPLIR', 4, TIMESTAMP '2013-04-15 00:00:00.0', TIMESTAMP '2013-05-26 00:00:00.0', TIMESTAMP '2013-04-23 00:00:00.0', 2, 191.6700, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10330, 'LILAS', 3, TIMESTAMP '2013-04-16 00:00:00.0', TIMESTAMP '2013-05-13 00:00:00.0', TIMESTAMP '2013-04-28 00:00:00.0', 1, 12.7500, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10331, 'BONAP', 9, TIMESTAMP '2013-04-16 00:00:00.0', TIMESTAMP '2013-05-27 00:00:00.0', TIMESTAMP '2013-04-21 00:00:00.0', 1, 10.1900, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10332, 'MEREP', 3, TIMESTAMP '2013-04-17 00:00:00.0', TIMESTAMP '2013-05-28 00:00:00.0', TIMESTAMP '2013-04-21 00:00:00.0', 2, 52.8400, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10333, 'WARTH', 5, TIMESTAMP '2013-04-18 00:00:00.0', TIMESTAMP '2013-05-15 00:00:00.0', TIMESTAMP '2013-04-25 00:00:00.0', 3, 0.5900, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10334, 'VICTE', 8, TIMESTAMP '2013-04-21 00:00:00.0', TIMESTAMP '2013-05-18 00:00:00.0', TIMESTAMP '2013-04-28 00:00:00.0', 2, 8.5600, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10335, 'HUNGO', 7, TIMESTAMP '2013-04-22 00:00:00.0', TIMESTAMP '2013-05-19 00:00:00.0', TIMESTAMP '2013-04-24 00:00:00.0', 2, 42.1100, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10336, 'PRINI', 7, TIMESTAMP '2013-04-23 00:00:00.0', TIMESTAMP '2013-05-20 00:00:00.0', TIMESTAMP '2013-04-25 00:00:00.0', 2, 15.5100, 'Princesa Isabel Vinhos', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal'),
(10337, 'FRANK', 4, TIMESTAMP '2013-04-24 00:00:00.0', TIMESTAMP '2013-05-21 00:00:00.0', TIMESTAMP '2013-04-29 00:00:00.0', 3, 108.2600, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10338, 'OLDWO', 4, TIMESTAMP '2013-04-25 00:00:00.0', TIMESTAMP '2013-05-22 00:00:00.0', TIMESTAMP '2013-04-29 00:00:00.0', 3, 84.2100, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10339, 'MEREP', 2, TIMESTAMP '2013-04-28 00:00:00.0', TIMESTAMP '2013-05-25 00:00:00.0', TIMESTAMP '2013-05-04 00:00:00.0', 2, 15.6600, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10340, 'BONAP', 1, TIMESTAMP '2013-04-29 00:00:00.0', TIMESTAMP '2013-05-26 00:00:00.0', TIMESTAMP '2013-05-08 00:00:00.0', 3, 166.3100, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10341, 'SIMOB', 7, TIMESTAMP '2013-04-29 00:00:00.0', TIMESTAMP '2013-05-26 00:00:00.0', TIMESTAMP '2013-05-05 00:00:00.0', 3, 26.7800, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(10342, 'FRANK', 4, TIMESTAMP '2013-04-30 00:00:00.0', TIMESTAMP '2013-05-13 00:00:00.0', TIMESTAMP '2013-05-04 00:00:00.0', 2, 54.8300, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10343, 'LEHMS', 4, TIMESTAMP '2013-04-30 00:00:00.0', TIMESTAMP '2013-05-28 00:00:00.0', TIMESTAMP '2013-05-06 00:00:00.0', 1, 110.3700, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10344, 'WHITC', 4, TIMESTAMP '2013-05-01 00:00:00.0', TIMESTAMP '2013-05-29 00:00:00.0', TIMESTAMP '2013-05-05 00:00:00.0', 2, 23.2900, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10345, 'QUICK', 2, TIMESTAMP '2013-05-04 00:00:00.0', TIMESTAMP '2013-06-02 00:00:00.0', TIMESTAMP '2013-05-11 00:00:00.0', 2, 249.0600, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10346, 'RATTC', 3, TIMESTAMP '2013-05-05 00:00:00.0', TIMESTAMP '2013-06-17 00:00:00.0', TIMESTAMP '2013-05-08 00:00:00.0', 3, 142.0800, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10347, 'FAMIA', 4, TIMESTAMP '2013-05-06 00:00:00.0', TIMESTAMP '2013-06-04 00:00:00.0', TIMESTAMP '2013-05-08 00:00:00.0', 3, 3.1000, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10348, 'WANDK', 4, TIMESTAMP '2013-05-07 00:00:00.0', TIMESTAMP '2013-06-05 00:00:00.0', TIMESTAMP '2013-05-15 00:00:00.0', 2, 0.7800, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10349, 'SPLIR', 7, TIMESTAMP '2013-05-08 00:00:00.0', TIMESTAMP '2013-06-06 00:00:00.0', TIMESTAMP '2013-05-15 00:00:00.0', 1, 8.6300, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10350, 'LAMAI', 6, TIMESTAMP '2013-05-11 00:00:00.0', TIMESTAMP '2013-06-09 00:00:00.0', TIMESTAMP '2013-06-03 00:00:00.0', 2, 64.1900, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10351, 'ERNSH', 1, TIMESTAMP '2013-05-11 00:00:00.0', TIMESTAMP '2013-06-09 00:00:00.0', TIMESTAMP '2013-05-20 00:00:00.0', 1, 162.3300, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10352, 'FURIB', 3, TIMESTAMP '2013-05-12 00:00:00.0', TIMESTAMP '2013-05-26 00:00:00.0', TIMESTAMP '2013-05-18 00:00:00.0', 3, 1.3000, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10353, 'PICCO', 7, TIMESTAMP '2013-05-13 00:00:00.0', TIMESTAMP '2013-06-11 00:00:00.0', TIMESTAMP '2013-05-25 00:00:00.0', 3, 360.6300, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10354, 'PERIC', 8, TIMESTAMP '2013-05-14 00:00:00.0', TIMESTAMP '2013-06-12 00:00:00.0', TIMESTAMP '2013-05-20 00:00:00.0', 3, 53.8000, 'Pericles Comidas clsicas', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10355, 'AROUT', 6, TIMESTAMP '2013-05-15 00:00:00.0', TIMESTAMP '2013-06-13 00:00:00.0', TIMESTAMP '2013-05-20 00:00:00.0', 1, 41.9500, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK');       
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10356, 'WANDK', 6, TIMESTAMP '2013-05-18 00:00:00.0', TIMESTAMP '2013-06-16 00:00:00.0', TIMESTAMP '2013-05-27 00:00:00.0', 2, 36.7100, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10357, 'LILAS', 1, TIMESTAMP '2013-05-19 00:00:00.0', TIMESTAMP '2013-06-17 00:00:00.0', TIMESTAMP '2013-06-02 00:00:00.0', 3, 34.8800, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10358, 'LAMAI', 5, TIMESTAMP '2013-05-20 00:00:00.0', TIMESTAMP '2013-06-18 00:00:00.0', TIMESTAMP '2013-05-27 00:00:00.0', 1, 19.6400, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10359, 'SEVES', 5, TIMESTAMP '2013-05-21 00:00:00.0', TIMESTAMP '2013-06-19 00:00:00.0', TIMESTAMP '2013-05-26 00:00:00.0', 3, 288.4300, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10360, 'BLONP', 4, TIMESTAMP '2013-05-22 00:00:00.0', TIMESTAMP '2013-06-20 00:00:00.0', TIMESTAMP '2013-06-02 00:00:00.0', 3, 131.7000, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10361, 'QUICK', 1, TIMESTAMP '2013-05-22 00:00:00.0', TIMESTAMP '2013-06-20 00:00:00.0', TIMESTAMP '2013-06-03 00:00:00.0', 2, 183.1700, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10362, 'BONAP', 3, TIMESTAMP '2013-05-25 00:00:00.0', TIMESTAMP '2013-06-23 00:00:00.0', TIMESTAMP '2013-05-28 00:00:00.0', 1, 96.0400, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10363, 'DRACD', 4, TIMESTAMP '2013-05-26 00:00:00.0', TIMESTAMP '2013-06-24 00:00:00.0', TIMESTAMP '2013-06-04 00:00:00.0', 3, 30.5400, 'Drachenblut Delikatessen', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany'),
(10364, 'EASTC', 1, TIMESTAMP '2013-05-26 00:00:00.0', TIMESTAMP '2013-07-07 00:00:00.0', TIMESTAMP '2013-06-04 00:00:00.0', 1, 71.9700, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(10365, 'ANTON', 3, TIMESTAMP '2013-05-27 00:00:00.0', TIMESTAMP '2013-06-25 00:00:00.0', TIMESTAMP '2013-06-02 00:00:00.0', 2, 22.0000, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico'),
(10366, 'GALED', 8, TIMESTAMP '2013-05-28 00:00:00.0', TIMESTAMP '2013-07-09 00:00:00.0', TIMESTAMP '2013-06-30 00:00:00.0', 2, 10.1400, 'Galera del gastronmo', 'Rambla de Catalua, 23', 'Barcelona', NULL, '8022', 'Spain'),
(10367, 'VAFFE', 7, TIMESTAMP '2013-05-28 00:00:00.0', TIMESTAMP '2013-06-26 00:00:00.0', TIMESTAMP '2013-06-02 00:00:00.0', 3, 13.5500, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10368, 'ERNSH', 2, TIMESTAMP '2013-05-29 00:00:00.0', TIMESTAMP '2013-06-27 00:00:00.0', TIMESTAMP '2013-06-02 00:00:00.0', 2, 101.9500, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10369, 'SPLIR', 8, TIMESTAMP '2013-06-02 00:00:00.0', TIMESTAMP '2013-06-30 00:00:00.0', TIMESTAMP '2013-06-09 00:00:00.0', 2, 195.6800, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10370, 'CHOPS', 6, TIMESTAMP '2013-06-03 00:00:00.0', TIMESTAMP '2013-06-30 00:00:00.0', TIMESTAMP '2013-06-27 00:00:00.0', 2, 1.1700, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(10371, 'LAMAI', 1, TIMESTAMP '2013-06-03 00:00:00.0', TIMESTAMP '2013-06-30 00:00:00.0', TIMESTAMP '2013-06-24 00:00:00.0', 1, 0.4500, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10372, 'QUEEN', 5, TIMESTAMP '2013-06-04 00:00:00.0', TIMESTAMP '2013-07-01 00:00:00.0', TIMESTAMP '2013-06-09 00:00:00.0', 2, 890.7800, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10373, 'HUNGO', 4, TIMESTAMP '2013-06-05 00:00:00.0', TIMESTAMP '2013-07-02 00:00:00.0', TIMESTAMP '2013-06-11 00:00:00.0', 3, 124.1200, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland');   
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10374, 'WOLZA', 1, TIMESTAMP '2013-06-05 00:00:00.0', TIMESTAMP '2013-07-02 00:00:00.0', TIMESTAMP '2013-06-09 00:00:00.0', 3, 3.9400, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland'),
(10375, 'HUNGC', 3, TIMESTAMP '2013-06-06 00:00:00.0', TIMESTAMP '2013-07-03 00:00:00.0', TIMESTAMP '2013-06-09 00:00:00.0', 2, 20.1200, 'Hungry Coyote Import Store', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA'),
(10376, 'MEREP', 1, TIMESTAMP '2013-06-09 00:00:00.0', TIMESTAMP '2013-07-06 00:00:00.0', TIMESTAMP '2013-06-13 00:00:00.0', 2, 20.3900, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10377, 'SEVES', 1, TIMESTAMP '2013-06-09 00:00:00.0', TIMESTAMP '2013-07-06 00:00:00.0', TIMESTAMP '2013-06-13 00:00:00.0', 3, 22.2100, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10378, 'FOLKO', 5, TIMESTAMP '2013-06-10 00:00:00.0', TIMESTAMP '2013-07-07 00:00:00.0', TIMESTAMP '2013-06-19 00:00:00.0', 3, 5.4400, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10379, 'QUEDE', 2, TIMESTAMP '2013-06-11 00:00:00.0', TIMESTAMP '2013-07-08 00:00:00.0', TIMESTAMP '2013-06-13 00:00:00.0', 1, 45.0300, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10380, 'HUNGO', 8, TIMESTAMP '2013-06-12 00:00:00.0', TIMESTAMP '2013-07-09 00:00:00.0', TIMESTAMP '2013-07-16 00:00:00.0', 3, 35.0300, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10381, 'LILAS', 3, TIMESTAMP '2013-06-12 00:00:00.0', TIMESTAMP '2013-07-09 00:00:00.0', TIMESTAMP '2013-06-13 00:00:00.0', 3, 7.9900, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10382, 'ERNSH', 4, TIMESTAMP '2013-06-13 00:00:00.0', TIMESTAMP '2013-07-10 00:00:00.0', TIMESTAMP '2013-06-16 00:00:00.0', 1, 94.7700, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10383, 'AROUT', 8, TIMESTAMP '2013-06-16 00:00:00.0', TIMESTAMP '2013-07-13 00:00:00.0', TIMESTAMP '2013-06-18 00:00:00.0', 3, 34.2400, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10384, 'BERGS', 3, TIMESTAMP '2013-06-16 00:00:00.0', TIMESTAMP '2013-07-13 00:00:00.0', TIMESTAMP '2013-06-20 00:00:00.0', 3, 168.6400, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10385, 'SPLIR', 1, TIMESTAMP '2013-06-17 00:00:00.0', TIMESTAMP '2013-07-14 00:00:00.0', TIMESTAMP '2013-06-23 00:00:00.0', 2, 30.9600, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10386, 'FAMIA', 9, TIMESTAMP '2013-06-18 00:00:00.0', TIMESTAMP '2013-07-01 00:00:00.0', TIMESTAMP '2013-06-25 00:00:00.0', 3, 13.9900, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10387, 'SANTG', 1, TIMESTAMP '2013-06-18 00:00:00.0', TIMESTAMP '2013-07-15 00:00:00.0', TIMESTAMP '2013-06-20 00:00:00.0', 2, 93.6300, 'Sant Gourmet', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway'),
(10388, 'SEVES', 2, TIMESTAMP '2013-06-19 00:00:00.0', TIMESTAMP '2013-07-16 00:00:00.0', TIMESTAMP '2013-06-20 00:00:00.0', 1, 34.8600, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10389, 'BOTTM', 4, TIMESTAMP '2013-06-20 00:00:00.0', TIMESTAMP '2013-07-17 00:00:00.0', TIMESTAMP '2013-06-24 00:00:00.0', 2, 47.4200, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10390, 'ERNSH', 6, TIMESTAMP '2013-06-23 00:00:00.0', TIMESTAMP '2013-07-20 00:00:00.0', TIMESTAMP '2013-06-26 00:00:00.0', 1, 126.3800, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10391, 'DRACD', 3, TIMESTAMP '2013-06-23 00:00:00.0', TIMESTAMP '2013-07-20 00:00:00.0', TIMESTAMP '2013-06-30 00:00:00.0', 3, 5.4500, 'Drachenblut Delikatessen', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany');         
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10392, 'PICCO', 2, TIMESTAMP '2013-06-24 00:00:00.0', TIMESTAMP '2013-07-21 00:00:00.0', TIMESTAMP '2013-07-01 00:00:00.0', 3, 122.4600, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10393, 'SAVEA', 1, TIMESTAMP '2013-06-25 00:00:00.0', TIMESTAMP '2013-07-22 00:00:00.0', TIMESTAMP '2013-07-03 00:00:00.0', 3, 126.5600, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10394, 'HUNGC', 1, TIMESTAMP '2013-06-25 00:00:00.0', TIMESTAMP '2013-07-22 00:00:00.0', TIMESTAMP '2013-07-03 00:00:00.0', 3, 30.3400, 'Hungry Coyote Import Store', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA'),
(10395, 'HILAA', 6, TIMESTAMP '2013-06-26 00:00:00.0', TIMESTAMP '2013-07-23 00:00:00.0', TIMESTAMP '2013-07-03 00:00:00.0', 1, 184.4100, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10396, 'FRANK', 1, TIMESTAMP '2013-06-27 00:00:00.0', TIMESTAMP '2013-07-10 00:00:00.0', TIMESTAMP '2013-07-06 00:00:00.0', 3, 135.3500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10397, 'PRINI', 5, TIMESTAMP '2013-06-27 00:00:00.0', TIMESTAMP '2013-07-24 00:00:00.0', TIMESTAMP '2013-07-02 00:00:00.0', 1, 60.2600, 'Princesa Isabel Vinhos', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal'),
(10398, 'SAVEA', 2, TIMESTAMP '2013-06-30 00:00:00.0', TIMESTAMP '2013-07-27 00:00:00.0', TIMESTAMP '2013-07-09 00:00:00.0', 3, 89.1600, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10399, 'VAFFE', 8, TIMESTAMP '2013-06-30 00:00:00.0', TIMESTAMP '2013-07-14 00:00:00.0', TIMESTAMP '2013-07-08 00:00:00.0', 3, 27.3600, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10400, 'EASTC', 1, TIMESTAMP '2013-07-01 00:00:00.0', TIMESTAMP '2013-07-29 00:00:00.0', TIMESTAMP '2013-07-16 00:00:00.0', 3, 83.9300, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(10401, 'RATTC', 1, TIMESTAMP '2013-07-01 00:00:00.0', TIMESTAMP '2013-07-29 00:00:00.0', TIMESTAMP '2013-07-10 00:00:00.0', 1, 12.5100, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10402, 'ERNSH', 8, TIMESTAMP '2013-07-02 00:00:00.0', TIMESTAMP '2013-08-13 00:00:00.0', TIMESTAMP '2013-07-10 00:00:00.0', 2, 67.8800, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10403, 'ERNSH', 4, TIMESTAMP '2013-07-03 00:00:00.0', TIMESTAMP '2013-07-31 00:00:00.0', TIMESTAMP '2013-07-09 00:00:00.0', 3, 73.7900, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10404, 'MAGAA', 2, TIMESTAMP '2013-07-03 00:00:00.0', TIMESTAMP '2013-07-31 00:00:00.0', TIMESTAMP '2013-07-08 00:00:00.0', 1, 155.9700, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10405, 'LINOD', 1, TIMESTAMP '2013-07-06 00:00:00.0', TIMESTAMP '2013-08-03 00:00:00.0', TIMESTAMP '2013-07-22 00:00:00.0', 1, 34.8200, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10406, 'QUEEN', 7, TIMESTAMP '2013-07-07 00:00:00.0', TIMESTAMP '2013-08-18 00:00:00.0', TIMESTAMP '2013-07-13 00:00:00.0', 1, 108.0400, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10407, 'OTTIK', 2, TIMESTAMP '2013-07-07 00:00:00.0', TIMESTAMP '2013-08-04 00:00:00.0', TIMESTAMP '2013-07-30 00:00:00.0', 2, 91.4800, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10408, 'FOLIG', 8, TIMESTAMP '2013-07-08 00:00:00.0', TIMESTAMP '2013-08-05 00:00:00.0', TIMESTAMP '2013-07-14 00:00:00.0', 1, 11.2600, 'Folies gourmandes', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France'),
(10409, 'OCEAN', 3, TIMESTAMP '2013-07-09 00:00:00.0', TIMESTAMP '2013-08-06 00:00:00.0', TIMESTAMP '2013-07-14 00:00:00.0', 1, 29.8300, 'Ocano Atlntico Ltda.', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina');           
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10410, 'BOTTM', 3, TIMESTAMP '2013-07-10 00:00:00.0', TIMESTAMP '2013-08-07 00:00:00.0', TIMESTAMP '2013-07-15 00:00:00.0', 3, 2.4000, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10411, 'BOTTM', 9, TIMESTAMP '2013-07-10 00:00:00.0', TIMESTAMP '2013-08-07 00:00:00.0', TIMESTAMP '2013-07-21 00:00:00.0', 3, 23.6500, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10412, 'WARTH', 8, TIMESTAMP '2013-07-13 00:00:00.0', TIMESTAMP '2013-08-10 00:00:00.0', TIMESTAMP '2013-07-15 00:00:00.0', 2, 3.7700, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10413, 'LAMAI', 3, TIMESTAMP '2013-07-14 00:00:00.0', TIMESTAMP '2013-08-11 00:00:00.0', TIMESTAMP '2013-07-16 00:00:00.0', 2, 95.6600, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10414, 'FAMIA', 2, TIMESTAMP '2013-07-14 00:00:00.0', TIMESTAMP '2013-08-11 00:00:00.0', TIMESTAMP '2013-07-17 00:00:00.0', 3, 21.4800, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10415, 'HUNGC', 3, TIMESTAMP '2013-07-15 00:00:00.0', TIMESTAMP '2013-08-12 00:00:00.0', TIMESTAMP '2013-07-24 00:00:00.0', 1, 0.2000, 'Hungry Coyote Import Store', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA'),
(10416, 'WARTH', 8, TIMESTAMP '2013-07-16 00:00:00.0', TIMESTAMP '2013-08-13 00:00:00.0', TIMESTAMP '2013-07-27 00:00:00.0', 3, 22.7200, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10417, 'SIMOB', 4, TIMESTAMP '2013-07-16 00:00:00.0', TIMESTAMP '2013-08-13 00:00:00.0', TIMESTAMP '2013-07-28 00:00:00.0', 3, 70.2900, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(10418, 'QUICK', 4, TIMESTAMP '2013-07-17 00:00:00.0', TIMESTAMP '2013-08-14 00:00:00.0', TIMESTAMP '2013-07-24 00:00:00.0', 1, 17.5500, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10419, 'RICSU', 4, TIMESTAMP '2013-07-20 00:00:00.0', TIMESTAMP '2013-08-17 00:00:00.0', TIMESTAMP '2013-07-30 00:00:00.0', 2, 137.3500, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10420, 'WELLI', 3, TIMESTAMP '2013-07-21 00:00:00.0', TIMESTAMP '2013-08-18 00:00:00.0', TIMESTAMP '2013-07-27 00:00:00.0', 1, 44.1200, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10421, 'QUEDE', 8, TIMESTAMP '2013-07-21 00:00:00.0', TIMESTAMP '2013-09-04 00:00:00.0', TIMESTAMP '2013-07-27 00:00:00.0', 1, 99.2300, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10422, 'FRANS', 2, TIMESTAMP '2013-07-22 00:00:00.0', TIMESTAMP '2013-08-19 00:00:00.0', TIMESTAMP '2013-07-31 00:00:00.0', 1, 3.0200, 'Franchi S.p.A.', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy'),
(10423, 'GOURL', 6, TIMESTAMP '2013-07-23 00:00:00.0', TIMESTAMP '2013-08-06 00:00:00.0', TIMESTAMP '2013-08-24 00:00:00.0', 3, 24.5000, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10424, 'MEREP', 7, TIMESTAMP '2013-07-23 00:00:00.0', TIMESTAMP '2013-08-20 00:00:00.0', TIMESTAMP '2013-07-27 00:00:00.0', 2, 370.6100, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10425, 'LAMAI', 6, TIMESTAMP '2013-07-24 00:00:00.0', TIMESTAMP '2013-08-21 00:00:00.0', TIMESTAMP '2013-08-14 00:00:00.0', 2, 7.9300, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10426, 'GALED', 4, TIMESTAMP '2013-07-27 00:00:00.0', TIMESTAMP '2013-08-24 00:00:00.0', TIMESTAMP '2013-08-06 00:00:00.0', 1, 18.6900, 'Galera del gastronmo', 'Rambla de Catalua, 23', 'Barcelona', NULL, '8022', 'Spain'),
(10427, 'PICCO', 4, TIMESTAMP '2013-07-27 00:00:00.0', TIMESTAMP '2013-08-24 00:00:00.0', TIMESTAMP '2013-09-03 00:00:00.0', 2, 31.2900, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria');            
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10428, 'REGGC', 7, TIMESTAMP '2013-07-28 00:00:00.0', TIMESTAMP '2013-08-25 00:00:00.0', TIMESTAMP '2013-08-04 00:00:00.0', 1, 11.0900, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10429, 'HUNGO', 3, TIMESTAMP '2013-07-29 00:00:00.0', TIMESTAMP '2013-09-12 00:00:00.0', TIMESTAMP '2013-08-07 00:00:00.0', 2, 56.6300, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10430, 'ERNSH', 4, TIMESTAMP '2013-07-30 00:00:00.0', TIMESTAMP '2013-08-13 00:00:00.0', TIMESTAMP '2013-08-03 00:00:00.0', 1, 458.7800, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10431, 'BOTTM', 4, TIMESTAMP '2013-07-30 00:00:00.0', TIMESTAMP '2013-08-13 00:00:00.0', TIMESTAMP '2013-08-07 00:00:00.0', 2, 44.1700, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10432, 'SPLIR', 3, TIMESTAMP '2013-07-31 00:00:00.0', TIMESTAMP '2013-08-14 00:00:00.0', TIMESTAMP '2013-08-07 00:00:00.0', 2, 4.3400, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10433, 'PRINI', 3, TIMESTAMP '2013-08-03 00:00:00.0', TIMESTAMP '2013-09-03 00:00:00.0', TIMESTAMP '2013-09-04 00:00:00.0', 3, 73.8300, 'Princesa Isabel Vinhos', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal'),
(10434, 'FOLKO', 3, TIMESTAMP '2013-08-03 00:00:00.0', TIMESTAMP '2013-09-03 00:00:00.0', TIMESTAMP '2013-08-13 00:00:00.0', 2, 17.9200, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10435, 'CONSH', 8, TIMESTAMP '2013-08-04 00:00:00.0', TIMESTAMP '2013-09-18 00:00:00.0', TIMESTAMP '2013-08-07 00:00:00.0', 2, 9.2100, 'Consolidated Holdings', 'Berkeley Gardens 12 Brewery', 'London', NULL, 'WX1 6LT', 'UK'),
(10436, 'BLONP', 3, TIMESTAMP '2013-08-05 00:00:00.0', TIMESTAMP '2013-09-05 00:00:00.0', TIMESTAMP '2013-08-11 00:00:00.0', 2, 156.6600, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10437, 'WARTH', 8, TIMESTAMP '2013-08-05 00:00:00.0', TIMESTAMP '2013-09-05 00:00:00.0', TIMESTAMP '2013-08-12 00:00:00.0', 1, 19.9700, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10438, 'TOMSP', 3, TIMESTAMP '2013-08-06 00:00:00.0', TIMESTAMP '2013-09-06 00:00:00.0', TIMESTAMP '2013-08-14 00:00:00.0', 2, 8.2400, 'Toms Spezialitten', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany'),
(10439, 'MEREP', 6, TIMESTAMP '2013-08-07 00:00:00.0', TIMESTAMP '2013-09-07 00:00:00.0', TIMESTAMP '2013-08-10 00:00:00.0', 3, 4.0700, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10440, 'SAVEA', 4, TIMESTAMP '2013-08-10 00:00:00.0', TIMESTAMP '2013-09-10 00:00:00.0', TIMESTAMP '2013-08-28 00:00:00.0', 2, 86.5300, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10441, 'OLDWO', 3, TIMESTAMP '2013-08-10 00:00:00.0', TIMESTAMP '2013-09-24 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', 2, 73.0200, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10442, 'ERNSH', 3, TIMESTAMP '2013-08-11 00:00:00.0', TIMESTAMP '2013-09-11 00:00:00.0', TIMESTAMP '2013-08-18 00:00:00.0', 2, 47.9400, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10443, 'REGGC', 8, TIMESTAMP '2013-08-12 00:00:00.0', TIMESTAMP '2013-09-12 00:00:00.0', TIMESTAMP '2013-08-14 00:00:00.0', 1, 13.9500, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10444, 'BERGS', 3, TIMESTAMP '2013-08-12 00:00:00.0', TIMESTAMP '2013-09-12 00:00:00.0', TIMESTAMP '2013-08-21 00:00:00.0', 3, 3.5000, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10445, 'BERGS', 3, TIMESTAMP '2013-08-13 00:00:00.0', TIMESTAMP '2013-09-13 00:00:00.0', TIMESTAMP '2013-08-20 00:00:00.0', 1, 9.3000, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10446, 'TOMSP', 6, TIMESTAMP '2013-08-14 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', TIMESTAMP '2013-08-19 00:00:00.0', 1, 14.6800, 'Toms Spezialitten', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany');     
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10447, 'RICAR', 4, TIMESTAMP '2013-08-14 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', TIMESTAMP '2013-09-07 00:00:00.0', 2, 68.6600, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10448, 'RANCH', 4, TIMESTAMP '2013-08-17 00:00:00.0', TIMESTAMP '2013-09-17 00:00:00.0', TIMESTAMP '2013-08-24 00:00:00.0', 2, 38.8200, 'Rancho grande', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10449, 'BLONP', 3, TIMESTAMP '2013-08-18 00:00:00.0', TIMESTAMP '2013-09-18 00:00:00.0', TIMESTAMP '2013-08-27 00:00:00.0', 2, 53.3000, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10450, 'VICTE', 8, TIMESTAMP '2013-08-19 00:00:00.0', TIMESTAMP '2013-09-19 00:00:00.0', TIMESTAMP '2013-09-11 00:00:00.0', 2, 7.2300, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10451, 'QUICK', 4, TIMESTAMP '2013-08-19 00:00:00.0', TIMESTAMP '2013-09-05 00:00:00.0', TIMESTAMP '2013-09-12 00:00:00.0', 3, 189.0900, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10452, 'SAVEA', 8, TIMESTAMP '2013-08-20 00:00:00.0', TIMESTAMP '2013-09-20 00:00:00.0', TIMESTAMP '2013-08-26 00:00:00.0', 1, 140.2600, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10453, 'AROUT', 1, TIMESTAMP '2013-08-21 00:00:00.0', TIMESTAMP '2013-09-21 00:00:00.0', TIMESTAMP '2013-08-26 00:00:00.0', 2, 25.3600, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10454, 'LAMAI', 4, TIMESTAMP '2013-08-21 00:00:00.0', TIMESTAMP '2013-09-21 00:00:00.0', TIMESTAMP '2013-08-25 00:00:00.0', 3, 2.7400, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10455, 'WARTH', 8, TIMESTAMP '2013-08-24 00:00:00.0', TIMESTAMP '2013-10-07 00:00:00.0', TIMESTAMP '2013-09-03 00:00:00.0', 2, 180.4500, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10456, 'KOENE', 8, TIMESTAMP '2013-08-25 00:00:00.0', TIMESTAMP '2013-10-08 00:00:00.0', TIMESTAMP '2013-08-28 00:00:00.0', 2, 8.1200, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10457, 'KOENE', 2, TIMESTAMP '2013-08-25 00:00:00.0', TIMESTAMP '2013-09-25 00:00:00.0', TIMESTAMP '2013-09-03 00:00:00.0', 1, 11.5700, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10458, 'SUPRD', 7, TIMESTAMP '2013-08-26 00:00:00.0', TIMESTAMP '2013-09-26 00:00:00.0', TIMESTAMP '2013-09-04 00:00:00.0', 3, 147.0600, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10459, 'VICTE', 4, TIMESTAMP '2013-08-27 00:00:00.0', TIMESTAMP '2013-09-27 00:00:00.0', TIMESTAMP '2013-08-28 00:00:00.0', 2, 25.0900, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10460, 'FOLKO', 8, TIMESTAMP '2013-08-28 00:00:00.0', TIMESTAMP '2013-09-28 00:00:00.0', TIMESTAMP '2013-09-03 00:00:00.0', 1, 16.2700, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10461, 'LILAS', 1, TIMESTAMP '2013-08-28 00:00:00.0', TIMESTAMP '2013-09-28 00:00:00.0', TIMESTAMP '2013-09-05 00:00:00.0', 3, 148.6100, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10462, 'CONSH', 2, TIMESTAMP '2013-09-03 00:00:00.0', TIMESTAMP '2013-09-30 00:00:00.0', TIMESTAMP '2013-09-18 00:00:00.0', 1, 6.1700, 'Consolidated Holdings', 'Berkeley Gardens 12 Brewery', 'London', NULL, 'WX1 6LT', 'UK'),
(10463, 'SUPRD', 5, TIMESTAMP '2013-09-04 00:00:00.0', TIMESTAMP '2013-10-01 00:00:00.0', TIMESTAMP '2013-09-06 00:00:00.0', 3, 14.7800, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10464, 'FURIB', 4, TIMESTAMP '2013-09-04 00:00:00.0', TIMESTAMP '2013-10-01 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', 2, 89.0000, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal');    
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10465, 'VAFFE', 1, TIMESTAMP '2013-09-05 00:00:00.0', TIMESTAMP '2013-10-02 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', 3, 145.0400, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10466, 'COMMI', 4, TIMESTAMP '2013-09-06 00:00:00.0', TIMESTAMP '2013-10-03 00:00:00.0', TIMESTAMP '2013-09-13 00:00:00.0', 1, 11.9300, 'Comrcio Mineiro', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil'),
(10467, 'MAGAA', 8, TIMESTAMP '2013-09-06 00:00:00.0', TIMESTAMP '2013-10-03 00:00:00.0', TIMESTAMP '2013-09-11 00:00:00.0', 2, 4.9300, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10468, 'KOENE', 3, TIMESTAMP '2013-09-07 00:00:00.0', TIMESTAMP '2013-10-04 00:00:00.0', TIMESTAMP '2013-09-12 00:00:00.0', 3, 44.1200, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10469, 'WHITC', 1, TIMESTAMP '2013-09-10 00:00:00.0', TIMESTAMP '2013-10-07 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', 1, 60.1800, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10470, 'BONAP', 4, TIMESTAMP '2013-09-11 00:00:00.0', TIMESTAMP '2013-10-08 00:00:00.0', TIMESTAMP '2013-09-14 00:00:00.0', 2, 64.5600, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10471, 'BSBEV', 2, TIMESTAMP '2013-09-11 00:00:00.0', TIMESTAMP '2013-10-08 00:00:00.0', TIMESTAMP '2013-09-18 00:00:00.0', 3, 45.5900, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10472, 'SEVES', 8, TIMESTAMP '2013-09-12 00:00:00.0', TIMESTAMP '2013-10-09 00:00:00.0', TIMESTAMP '2013-09-19 00:00:00.0', 1, 4.2000, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10473, 'ISLAT', 1, TIMESTAMP '2013-09-13 00:00:00.0', TIMESTAMP '2013-09-27 00:00:00.0', TIMESTAMP '2013-09-21 00:00:00.0', 3, 16.3700, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10474, 'PERIC', 5, TIMESTAMP '2013-09-13 00:00:00.0', TIMESTAMP '2013-10-10 00:00:00.0', TIMESTAMP '2013-09-21 00:00:00.0', 2, 83.4900, 'Pericles Comidas clsicas', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10475, 'SUPRD', 9, TIMESTAMP '2013-09-14 00:00:00.0', TIMESTAMP '2013-10-11 00:00:00.0', TIMESTAMP '2013-10-04 00:00:00.0', 1, 68.5200, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10476, 'HILAA', 8, TIMESTAMP '2013-09-17 00:00:00.0', TIMESTAMP '2013-10-14 00:00:00.0', TIMESTAMP '2013-09-24 00:00:00.0', 3, 4.4100, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10477, 'PRINI', 5, TIMESTAMP '2013-09-17 00:00:00.0', TIMESTAMP '2013-10-14 00:00:00.0', TIMESTAMP '2013-09-25 00:00:00.0', 2, 13.0200, 'Princesa Isabel Vinhos', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal'),
(10478, 'VICTE', 2, TIMESTAMP '2013-09-18 00:00:00.0', TIMESTAMP '2013-10-01 00:00:00.0', TIMESTAMP '2013-09-26 00:00:00.0', 3, 4.8100, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10479, 'RATTC', 3, TIMESTAMP '2013-09-19 00:00:00.0', TIMESTAMP '2013-10-16 00:00:00.0', TIMESTAMP '2013-09-21 00:00:00.0', 3, 708.9500, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10480, 'FOLIG', 6, TIMESTAMP '2013-09-20 00:00:00.0', TIMESTAMP '2013-10-17 00:00:00.0', TIMESTAMP '2013-09-24 00:00:00.0', 2, 1.3500, 'Folies gourmandes', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France'),
(10481, 'RICAR', 8, TIMESTAMP '2013-09-20 00:00:00.0', TIMESTAMP '2013-10-17 00:00:00.0', TIMESTAMP '2013-09-25 00:00:00.0', 2, 64.3300, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10482, 'LAZYK', 1, TIMESTAMP '2013-09-21 00:00:00.0', TIMESTAMP '2013-10-18 00:00:00.0', TIMESTAMP '2013-10-10 00:00:00.0', 3, 7.4800, 'Lazy K Kountry Store', '12 Orchestra Terrace', 'Walla Walla', 'WA', '99362', 'USA');
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10483, 'WHITC', 7, TIMESTAMP '2013-09-24 00:00:00.0', TIMESTAMP '2013-10-21 00:00:00.0', TIMESTAMP '2013-10-25 00:00:00.0', 2, 15.2800, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10484, 'BSBEV', 3, TIMESTAMP '2013-09-24 00:00:00.0', TIMESTAMP '2013-10-21 00:00:00.0', TIMESTAMP '2013-10-01 00:00:00.0', 3, 6.8800, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10485, 'LINOD', 4, TIMESTAMP '2013-09-25 00:00:00.0', TIMESTAMP '2013-10-08 00:00:00.0', TIMESTAMP '2013-09-30 00:00:00.0', 2, 64.4500, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10486, 'HILAA', 1, TIMESTAMP '2013-09-26 00:00:00.0', TIMESTAMP '2013-10-23 00:00:00.0', TIMESTAMP '2013-10-02 00:00:00.0', 2, 30.5300, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10487, 'QUEEN', 2, TIMESTAMP '2013-09-26 00:00:00.0', TIMESTAMP '2013-10-23 00:00:00.0', TIMESTAMP '2013-09-28 00:00:00.0', 2, 71.0700, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10488, 'FRANK', 8, TIMESTAMP '2013-09-27 00:00:00.0', TIMESTAMP '2013-10-24 00:00:00.0', TIMESTAMP '2013-10-02 00:00:00.0', 2, 4.9300, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10489, 'PICCO', 6, TIMESTAMP '2013-09-28 00:00:00.0', TIMESTAMP '2013-10-25 00:00:00.0', TIMESTAMP '2013-10-09 00:00:00.0', 2, 5.2900, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10490, 'HILAA', 7, TIMESTAMP '2013-09-30 00:00:00.0', TIMESTAMP '2013-10-28 00:00:00.0', TIMESTAMP '2013-10-03 00:00:00.0', 2, 210.1900, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10491, 'FURIB', 8, TIMESTAMP '2013-09-30 00:00:00.0', TIMESTAMP '2013-10-28 00:00:00.0', TIMESTAMP '2013-10-08 00:00:00.0', 3, 16.9600, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10492, 'BOTTM', 3, TIMESTAMP '2013-10-01 00:00:00.0', TIMESTAMP '2013-10-29 00:00:00.0', TIMESTAMP '2013-10-11 00:00:00.0', 1, 62.8900, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10493, 'LAMAI', 4, TIMESTAMP '2013-10-02 00:00:00.0', TIMESTAMP '2013-10-30 00:00:00.0', TIMESTAMP '2013-10-10 00:00:00.0', 3, 10.6400, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10494, 'COMMI', 4, TIMESTAMP '2013-10-02 00:00:00.0', TIMESTAMP '2013-10-30 00:00:00.0', TIMESTAMP '2013-10-09 00:00:00.0', 2, 65.9900, 'Comrcio Mineiro', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil'),
(10495, 'LAUGB', 3, TIMESTAMP '2013-10-03 00:00:00.0', TIMESTAMP '2013-11-01 00:00:00.0', TIMESTAMP '2013-10-11 00:00:00.0', 3, 4.6500, 'Laughing Bacchus Wine Cellars', '2319 Elm St.', 'Vancouver', 'BC', 'V3F 2K1', 'Canada'),
(10496, 'TRADH', 7, TIMESTAMP '2013-10-04 00:00:00.0', TIMESTAMP '2013-11-02 00:00:00.0', TIMESTAMP '2013-10-07 00:00:00.0', 2, 46.7700, 'Tradiao Hipermercados', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil'),
(10497, 'LEHMS', 7, TIMESTAMP '2013-10-04 00:00:00.0', TIMESTAMP '2013-11-02 00:00:00.0', TIMESTAMP '2013-10-07 00:00:00.0', 1, 36.2100, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10498, 'HILAA', 8, TIMESTAMP '2013-10-07 00:00:00.0', TIMESTAMP '2013-11-05 00:00:00.0', TIMESTAMP '2013-10-11 00:00:00.0', 2, 29.7500, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10499, 'LILAS', 4, TIMESTAMP '2013-10-08 00:00:00.0', TIMESTAMP '2013-11-06 00:00:00.0', TIMESTAMP '2013-10-16 00:00:00.0', 2, 102.0200, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10500, 'LAMAI', 6, TIMESTAMP '2013-10-09 00:00:00.0', TIMESTAMP '2013-11-07 00:00:00.0', TIMESTAMP '2013-10-17 00:00:00.0', 1, 42.6800, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10501, 'BLAUS', 9, TIMESTAMP '2013-10-09 00:00:00.0', TIMESTAMP '2013-11-07 00:00:00.0', TIMESTAMP '2013-10-16 00:00:00.0', 3, 8.8500, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(10502, 'PERIC', 2, TIMESTAMP '2013-10-10 00:00:00.0', TIMESTAMP '2013-11-08 00:00:00.0', TIMESTAMP '2013-10-29 00:00:00.0', 1, 69.3200, 'Pericles Comidas clsicas', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10503, 'HUNGO', 6, TIMESTAMP '2013-10-11 00:00:00.0', TIMESTAMP '2013-11-09 00:00:00.0', TIMESTAMP '2013-10-16 00:00:00.0', 2, 16.7400, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10504, 'WHITC', 4, TIMESTAMP '2013-10-11 00:00:00.0', TIMESTAMP '2013-11-09 00:00:00.0', TIMESTAMP '2013-10-18 00:00:00.0', 3, 59.1300, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10505, 'MEREP', 3, TIMESTAMP '2013-10-14 00:00:00.0', TIMESTAMP '2013-11-12 00:00:00.0', TIMESTAMP '2013-10-21 00:00:00.0', 3, 7.1300, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10506, 'KOENE', 9, TIMESTAMP '2013-10-15 00:00:00.0', TIMESTAMP '2013-11-13 00:00:00.0', TIMESTAMP '2013-11-02 00:00:00.0', 2, 21.1900, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10507, 'ANTON', 7, TIMESTAMP '2013-10-15 00:00:00.0', TIMESTAMP '2013-11-13 00:00:00.0', TIMESTAMP '2013-10-22 00:00:00.0', 1, 47.4500, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico'),
(10508, 'OTTIK', 1, TIMESTAMP '2013-10-16 00:00:00.0', TIMESTAMP '2013-11-14 00:00:00.0', TIMESTAMP '2013-11-13 00:00:00.0', 2, 4.9900, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10509, 'BLAUS', 4, TIMESTAMP '2013-10-17 00:00:00.0', TIMESTAMP '2013-11-15 00:00:00.0', TIMESTAMP '2013-10-29 00:00:00.0', 1, 0.1500, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(10510, 'SAVEA', 6, TIMESTAMP '2013-10-18 00:00:00.0', TIMESTAMP '2013-11-16 00:00:00.0', TIMESTAMP '2013-10-28 00:00:00.0', 3, 367.6300, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10511, 'BONAP', 4, TIMESTAMP '2013-10-18 00:00:00.0', TIMESTAMP '2013-11-16 00:00:00.0', TIMESTAMP '2013-10-21 00:00:00.0', 3, 350.6400, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10512, 'FAMIA', 7, TIMESTAMP '2013-10-21 00:00:00.0', TIMESTAMP '2013-11-19 00:00:00.0', TIMESTAMP '2013-10-24 00:00:00.0', 2, 3.5300, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10513, 'WANDK', 7, TIMESTAMP '2013-10-22 00:00:00.0', TIMESTAMP '2013-12-03 00:00:00.0', TIMESTAMP '2013-10-28 00:00:00.0', 1, 105.6500, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10514, 'ERNSH', 3, TIMESTAMP '2013-10-22 00:00:00.0', TIMESTAMP '2013-11-20 00:00:00.0', TIMESTAMP '2013-11-16 00:00:00.0', 2, 789.9500, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10515, 'QUICK', 2, TIMESTAMP '2013-10-23 00:00:00.0', TIMESTAMP '2013-11-07 00:00:00.0', TIMESTAMP '2013-11-23 00:00:00.0', 1, 204.4700, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10516, 'HUNGO', 2, TIMESTAMP '2013-10-24 00:00:00.0', TIMESTAMP '2013-11-22 00:00:00.0', TIMESTAMP '2013-11-01 00:00:00.0', 3, 62.7800, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10517, 'NORTS', 3, TIMESTAMP '2013-10-24 00:00:00.0', TIMESTAMP '2013-11-22 00:00:00.0', TIMESTAMP '2013-10-29 00:00:00.0', 3, 32.0700, 'North/South', 'South House 300 Queensbridge', 'London', NULL, 'SW7 1RZ', 'UK'),
(10518, 'TORTU', 4, TIMESTAMP '2013-10-25 00:00:00.0', TIMESTAMP '2013-11-09 00:00:00.0', TIMESTAMP '2013-11-05 00:00:00.0', 2, 218.1500, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico');      
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10519, 'CHOPS', 6, TIMESTAMP '2013-10-28 00:00:00.0', TIMESTAMP '2013-11-26 00:00:00.0', TIMESTAMP '2013-11-01 00:00:00.0', 3, 91.7600, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(10520, 'SANTG', 7, TIMESTAMP '2013-10-29 00:00:00.0', TIMESTAMP '2013-11-27 00:00:00.0', TIMESTAMP '2013-11-01 00:00:00.0', 1, 13.3700, 'Sant Gourmet', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway'),
(10521, 'CACTU', 8, TIMESTAMP '2013-10-29 00:00:00.0', TIMESTAMP '2013-11-27 00:00:00.0', TIMESTAMP '2013-11-02 00:00:00.0', 2, 17.2200, 'Cactus Comidas para llevar', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10522, 'LEHMS', 4, TIMESTAMP '2013-10-30 00:00:00.0', TIMESTAMP '2013-11-28 00:00:00.0', TIMESTAMP '2013-11-06 00:00:00.0', 1, 45.3300, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10523, 'SEVES', 7, TIMESTAMP '2013-11-01 00:00:00.0', TIMESTAMP '2013-11-29 00:00:00.0', TIMESTAMP '2013-11-30 00:00:00.0', 2, 77.6300, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10524, 'BERGS', 1, TIMESTAMP '2013-11-01 00:00:00.0', TIMESTAMP '2013-11-29 00:00:00.0', TIMESTAMP '2013-11-07 00:00:00.0', 2, 244.7900, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10525, 'BONAP', 1, TIMESTAMP '2013-11-02 00:00:00.0', TIMESTAMP '2013-11-30 00:00:00.0', TIMESTAMP '2013-11-23 00:00:00.0', 2, 11.0600, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10526, 'WARTH', 4, TIMESTAMP '2013-11-05 00:00:00.0', TIMESTAMP '2013-12-02 00:00:00.0', TIMESTAMP '2013-11-15 00:00:00.0', 2, 58.5900, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10527, 'QUICK', 7, TIMESTAMP '2013-11-05 00:00:00.0', TIMESTAMP '2013-12-02 00:00:00.0', TIMESTAMP '2013-11-07 00:00:00.0', 1, 41.9000, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10528, 'GREAL', 6, TIMESTAMP '2013-11-06 00:00:00.0', TIMESTAMP '2013-11-20 00:00:00.0', TIMESTAMP '2013-11-09 00:00:00.0', 2, 3.3500, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10529, 'MAISD', 5, TIMESTAMP '2013-11-07 00:00:00.0', TIMESTAMP '2013-12-04 00:00:00.0', TIMESTAMP '2013-11-09 00:00:00.0', 2, 66.6900, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(10530, 'PICCO', 3, TIMESTAMP '2013-11-08 00:00:00.0', TIMESTAMP '2013-12-05 00:00:00.0', TIMESTAMP '2013-11-12 00:00:00.0', 2, 339.2200, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10531, 'OCEAN', 7, TIMESTAMP '2013-11-08 00:00:00.0', TIMESTAMP '2013-12-05 00:00:00.0', TIMESTAMP '2013-11-19 00:00:00.0', 1, 8.1200, 'Ocano Atlntico Ltda.', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10532, 'EASTC', 7, TIMESTAMP '2013-11-09 00:00:00.0', TIMESTAMP '2013-12-06 00:00:00.0', TIMESTAMP '2013-11-12 00:00:00.0', 3, 74.4600, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(10533, 'FOLKO', 8, TIMESTAMP '2013-11-12 00:00:00.0', TIMESTAMP '2013-12-09 00:00:00.0', TIMESTAMP '2013-11-22 00:00:00.0', 1, 188.0400, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10534, 'LEHMS', 8, TIMESTAMP '2013-11-12 00:00:00.0', TIMESTAMP '2013-12-09 00:00:00.0', TIMESTAMP '2013-11-14 00:00:00.0', 2, 27.9400, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10535, 'ANTON', 4, TIMESTAMP '2013-11-13 00:00:00.0', TIMESTAMP '2013-12-10 00:00:00.0', TIMESTAMP '2013-11-21 00:00:00.0', 1, 15.6400, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico'),
(10536, 'LEHMS', 3, TIMESTAMP '2013-11-14 00:00:00.0', TIMESTAMP '2013-12-11 00:00:00.0', TIMESTAMP '2013-12-06 00:00:00.0', 2, 58.8800, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10537, 'RICSU', 1, TIMESTAMP '2013-11-14 00:00:00.0', TIMESTAMP '2013-11-28 00:00:00.0', TIMESTAMP '2013-11-19 00:00:00.0', 1, 78.8500, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland');          
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10538, 'BSBEV', 9, TIMESTAMP '2013-11-15 00:00:00.0', TIMESTAMP '2013-12-12 00:00:00.0', TIMESTAMP '2013-11-16 00:00:00.0', 3, 4.8700, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10539, 'BSBEV', 6, TIMESTAMP '2013-11-16 00:00:00.0', TIMESTAMP '2013-12-13 00:00:00.0', TIMESTAMP '2013-11-23 00:00:00.0', 3, 12.3600, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10540, 'QUICK', 3, TIMESTAMP '2013-11-19 00:00:00.0', TIMESTAMP '2013-12-16 00:00:00.0', TIMESTAMP '2013-12-13 00:00:00.0', 3, 1007.6400, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10541, 'HANAR', 2, TIMESTAMP '2013-11-19 00:00:00.0', TIMESTAMP '2013-12-16 00:00:00.0', TIMESTAMP '2013-11-29 00:00:00.0', 1, 68.6500, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10542, 'KOENE', 1, TIMESTAMP '2013-11-20 00:00:00.0', TIMESTAMP '2013-12-17 00:00:00.0', TIMESTAMP '2013-11-26 00:00:00.0', 3, 10.9500, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10543, 'LILAS', 8, TIMESTAMP '2013-11-21 00:00:00.0', TIMESTAMP '2013-12-18 00:00:00.0', TIMESTAMP '2013-11-23 00:00:00.0', 2, 48.1700, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10544, 'LONEP', 4, TIMESTAMP '2013-11-21 00:00:00.0', TIMESTAMP '2013-12-18 00:00:00.0', TIMESTAMP '2013-11-30 00:00:00.0', 1, 24.9100, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(10545, 'LAZYK', 8, TIMESTAMP '2013-11-22 00:00:00.0', TIMESTAMP '2013-12-19 00:00:00.0', TIMESTAMP '2013-12-26 00:00:00.0', 2, 11.9200, 'Lazy K Kountry Store', '12 Orchestra Terrace', 'Walla Walla', 'WA', '99362', 'USA'),
(10546, 'VICTE', 1, TIMESTAMP '2013-11-23 00:00:00.0', TIMESTAMP '2013-12-20 00:00:00.0', TIMESTAMP '2013-11-27 00:00:00.0', 3, 194.7200, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10547, 'SEVES', 3, TIMESTAMP '2013-11-23 00:00:00.0', TIMESTAMP '2013-12-20 00:00:00.0', TIMESTAMP '2013-12-02 00:00:00.0', 2, 178.4300, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10548, 'TOMSP', 3, TIMESTAMP '2013-11-26 00:00:00.0', TIMESTAMP '2013-12-23 00:00:00.0', TIMESTAMP '2013-12-02 00:00:00.0', 2, 1.4300, 'Toms Spezialitten', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany'),
(10549, 'QUICK', 5, TIMESTAMP '2013-11-27 00:00:00.0', TIMESTAMP '2013-12-10 00:00:00.0', TIMESTAMP '2013-11-30 00:00:00.0', 1, 171.2400, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10550, 'GODOS', 7, TIMESTAMP '2013-11-28 00:00:00.0', TIMESTAMP '2013-12-25 00:00:00.0', TIMESTAMP '2013-12-06 00:00:00.0', 3, 4.3200, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10551, 'FURIB', 4, TIMESTAMP '2013-11-28 00:00:00.0', TIMESTAMP '2014-01-09 00:00:00.0', TIMESTAMP '2013-12-06 00:00:00.0', 3, 72.9500, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10552, 'HILAA', 2, TIMESTAMP '2013-11-29 00:00:00.0', TIMESTAMP '2013-12-26 00:00:00.0', TIMESTAMP '2013-12-05 00:00:00.0', 1, 83.2200, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10553, 'WARTH', 2, TIMESTAMP '2013-11-30 00:00:00.0', TIMESTAMP '2013-12-27 00:00:00.0', TIMESTAMP '2013-12-03 00:00:00.0', 2, 149.4900, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10554, 'OTTIK', 4, TIMESTAMP '2013-11-30 00:00:00.0', TIMESTAMP '2013-12-27 00:00:00.0', TIMESTAMP '2013-12-05 00:00:00.0', 3, 120.9700, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10555, 'SAVEA', 6, TIMESTAMP '2013-12-02 00:00:00.0', TIMESTAMP '2013-12-30 00:00:00.0', TIMESTAMP '2013-12-04 00:00:00.0', 3, 252.4900, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA');   
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10556, 'SIMOB', 2, TIMESTAMP '2013-12-03 00:00:00.0', TIMESTAMP '2014-01-15 00:00:00.0', TIMESTAMP '2013-12-13 00:00:00.0', 1, 9.8000, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(10557, 'LEHMS', 9, TIMESTAMP '2013-12-03 00:00:00.0', TIMESTAMP '2013-12-17 00:00:00.0', TIMESTAMP '2013-12-06 00:00:00.0', 2, 96.7200, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10558, 'AROUT', 1, TIMESTAMP '2013-12-04 00:00:00.0', TIMESTAMP '2014-01-02 00:00:00.0', TIMESTAMP '2013-12-10 00:00:00.0', 2, 72.9700, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10559, 'BLONP', 6, TIMESTAMP '2013-12-05 00:00:00.0', TIMESTAMP '2014-01-03 00:00:00.0', TIMESTAMP '2013-12-13 00:00:00.0', 1, 8.0500, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10560, 'FRANK', 8, TIMESTAMP '2013-12-06 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', TIMESTAMP '2013-12-09 00:00:00.0', 1, 36.6500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10561, 'FOLKO', 2, TIMESTAMP '2013-12-06 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', TIMESTAMP '2013-12-09 00:00:00.0', 2, 242.2100, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10562, 'REGGC', 1, TIMESTAMP '2013-12-09 00:00:00.0', TIMESTAMP '2014-01-07 00:00:00.0', TIMESTAMP '2013-12-12 00:00:00.0', 1, 22.9500, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10563, 'RICAR', 2, TIMESTAMP '2013-12-10 00:00:00.0', TIMESTAMP '2014-01-22 00:00:00.0', TIMESTAMP '2013-12-24 00:00:00.0', 2, 60.4300, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10564, 'RATTC', 4, TIMESTAMP '2013-12-10 00:00:00.0', TIMESTAMP '2014-01-08 00:00:00.0', TIMESTAMP '2013-12-16 00:00:00.0', 3, 13.7500, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10565, 'MEREP', 8, TIMESTAMP '2013-12-11 00:00:00.0', TIMESTAMP '2014-01-09 00:00:00.0', TIMESTAMP '2013-12-18 00:00:00.0', 2, 7.1500, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10566, 'BLONP', 9, TIMESTAMP '2013-12-12 00:00:00.0', TIMESTAMP '2014-01-10 00:00:00.0', TIMESTAMP '2013-12-18 00:00:00.0', 1, 88.4000, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10567, 'HUNGO', 1, TIMESTAMP '2013-12-12 00:00:00.0', TIMESTAMP '2014-01-10 00:00:00.0', TIMESTAMP '2013-12-17 00:00:00.0', 1, 33.9700, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10568, 'GALED', 3, TIMESTAMP '2013-12-13 00:00:00.0', TIMESTAMP '2014-01-11 00:00:00.0', TIMESTAMP '2014-01-09 00:00:00.0', 3, 6.5400, 'Galera del gastronmo', 'Rambla de Catalua, 23', 'Barcelona', NULL, '8022', 'Spain'),
(10569, 'RATTC', 5, TIMESTAMP '2013-12-16 00:00:00.0', TIMESTAMP '2014-01-14 00:00:00.0', TIMESTAMP '2014-01-11 00:00:00.0', 1, 58.9800, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10570, 'MEREP', 3, TIMESTAMP '2013-12-17 00:00:00.0', TIMESTAMP '2014-01-15 00:00:00.0', TIMESTAMP '2013-12-19 00:00:00.0', 3, 188.9900, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10571, 'ERNSH', 8, TIMESTAMP '2013-12-17 00:00:00.0', TIMESTAMP '2014-01-29 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', 3, 26.0600, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10572, 'BERGS', 3, TIMESTAMP '2013-12-18 00:00:00.0', TIMESTAMP '2014-01-16 00:00:00.0', TIMESTAMP '2013-12-25 00:00:00.0', 2, 116.4300, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10573, 'ANTON', 7, TIMESTAMP '2013-12-19 00:00:00.0', TIMESTAMP '2014-01-17 00:00:00.0', TIMESTAMP '2013-12-20 00:00:00.0', 3, 84.8400, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico');
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10574, 'TRAIH', 4, TIMESTAMP '2013-12-19 00:00:00.0', TIMESTAMP '2014-01-17 00:00:00.0', TIMESTAMP '2013-12-30 00:00:00.0', 2, 37.6000, 'Trail-s Head Gourmet Provisioners', '722 DaVinci Blvd.', 'Kirkland', 'WA', '98034', 'USA'),
(10575, 'MORGK', 5, TIMESTAMP '2013-12-20 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', TIMESTAMP '2013-12-30 00:00:00.0', 1, 127.3400, 'Morgenstern Gesundkost', 'Heerstr. 22', 'Leipzig', NULL, '4179', 'Germany'),
(10576, 'TORTU', 3, TIMESTAMP '2013-12-23 00:00:00.0', TIMESTAMP '2014-01-07 00:00:00.0', TIMESTAMP '2013-12-30 00:00:00.0', 3, 18.5600, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10577, 'TRAIH', 9, TIMESTAMP '2013-12-23 00:00:00.0', TIMESTAMP '2014-02-04 00:00:00.0', TIMESTAMP '2013-12-30 00:00:00.0', 2, 25.4100, 'Trail-s Head Gourmet Provisioners', '722 DaVinci Blvd.', 'Kirkland', 'WA', '98034', 'USA'),
(10578, 'BSBEV', 4, TIMESTAMP '2013-12-24 00:00:00.0', TIMESTAMP '2014-01-22 00:00:00.0', TIMESTAMP '2014-01-25 00:00:00.0', 3, 29.6000, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10579, 'LETSS', 1, TIMESTAMP '2013-12-25 00:00:00.0', TIMESTAMP '2014-01-23 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', 2, 13.7300, 'Let-s Stop N Shop', '87 Polk St. Suite 5', 'San Francisco', 'CA', '94117', 'USA'),
(10580, 'OTTIK', 4, TIMESTAMP '2013-12-26 00:00:00.0', TIMESTAMP '2014-01-24 00:00:00.0', TIMESTAMP '2014-01-01 00:00:00.0', 3, 75.8900, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10581, 'FAMIA', 3, TIMESTAMP '2013-12-26 00:00:00.0', TIMESTAMP '2014-01-24 00:00:00.0', TIMESTAMP '2014-01-02 00:00:00.0', 1, 3.0100, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10582, 'BLAUS', 3, TIMESTAMP '2013-12-27 00:00:00.0', TIMESTAMP '2014-01-25 00:00:00.0', TIMESTAMP '2014-01-14 00:00:00.0', 2, 27.7100, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(10583, 'WARTH', 2, TIMESTAMP '2013-12-30 00:00:00.0', TIMESTAMP '2014-01-28 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', 2, 7.2800, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10584, 'BLONP', 4, TIMESTAMP '2013-12-30 00:00:00.0', TIMESTAMP '2014-01-28 00:00:00.0', TIMESTAMP '2014-01-04 00:00:00.0', 1, 59.1400, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10585, 'WELLI', 7, TIMESTAMP '2014-01-01 00:00:00.0', TIMESTAMP '2014-01-29 00:00:00.0', TIMESTAMP '2014-01-10 00:00:00.0', 1, 13.4100, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10586, 'REGGC', 9, TIMESTAMP '2014-01-02 00:00:00.0', TIMESTAMP '2014-01-30 00:00:00.0', TIMESTAMP '2014-01-09 00:00:00.0', 1, 0.4800, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10587, 'QUEDE', 1, TIMESTAMP '2014-01-02 00:00:00.0', TIMESTAMP '2014-01-30 00:00:00.0', TIMESTAMP '2014-01-09 00:00:00.0', 1, 62.5200, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10588, 'QUICK', 2, TIMESTAMP '2014-01-03 00:00:00.0', TIMESTAMP '2014-01-31 00:00:00.0', TIMESTAMP '2014-01-10 00:00:00.0', 3, 194.6700, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10589, 'GREAL', 8, TIMESTAMP '2014-01-04 00:00:00.0', TIMESTAMP '2014-02-01 00:00:00.0', TIMESTAMP '2014-01-14 00:00:00.0', 2, 4.4200, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10590, 'MEREP', 4, TIMESTAMP '2014-01-07 00:00:00.0', TIMESTAMP '2014-02-04 00:00:00.0', TIMESTAMP '2014-01-14 00:00:00.0', 3, 44.7700, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10591, 'VAFFE', 1, TIMESTAMP '2014-01-07 00:00:00.0', TIMESTAMP '2014-01-21 00:00:00.0', TIMESTAMP '2014-01-16 00:00:00.0', 1, 55.9200, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10592, 'LEHMS', 3, TIMESTAMP '2014-01-08 00:00:00.0', TIMESTAMP '2014-02-05 00:00:00.0', TIMESTAMP '2014-01-16 00:00:00.0', 1, 32.1000, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10593, 'LEHMS', 7, TIMESTAMP '2014-01-09 00:00:00.0', TIMESTAMP '2014-02-06 00:00:00.0', TIMESTAMP '2014-02-13 00:00:00.0', 2, 174.2000, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10594, 'OLDWO', 3, TIMESTAMP '2014-01-09 00:00:00.0', TIMESTAMP '2014-02-06 00:00:00.0', TIMESTAMP '2014-01-16 00:00:00.0', 2, 5.2400, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10595, 'ERNSH', 2, TIMESTAMP '2014-01-10 00:00:00.0', TIMESTAMP '2014-02-07 00:00:00.0', TIMESTAMP '2014-01-14 00:00:00.0', 1, 96.7800, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10596, 'WHITC', 8, TIMESTAMP '2014-01-11 00:00:00.0', TIMESTAMP '2014-02-08 00:00:00.0', TIMESTAMP '2014-02-12 00:00:00.0', 1, 16.3400, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10597, 'PICCO', 7, TIMESTAMP '2014-01-11 00:00:00.0', TIMESTAMP '2014-02-08 00:00:00.0', TIMESTAMP '2014-01-18 00:00:00.0', 3, 35.1200, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10598, 'RATTC', 1, TIMESTAMP '2014-01-14 00:00:00.0', TIMESTAMP '2014-02-11 00:00:00.0', TIMESTAMP '2014-01-18 00:00:00.0', 3, 44.4200, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10599, 'BSBEV', 6, TIMESTAMP '2014-01-15 00:00:00.0', TIMESTAMP '2014-02-26 00:00:00.0', TIMESTAMP '2014-01-21 00:00:00.0', 3, 29.9800, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10600, 'HUNGC', 4, TIMESTAMP '2014-01-16 00:00:00.0', TIMESTAMP '2014-02-13 00:00:00.0', TIMESTAMP '2014-01-21 00:00:00.0', 1, 45.1300, 'Hungry Coyote Import Store', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA'),
(10601, 'HILAA', 7, TIMESTAMP '2014-01-16 00:00:00.0', TIMESTAMP '2014-02-27 00:00:00.0', TIMESTAMP '2014-01-22 00:00:00.0', 1, 58.3000, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10602, 'VAFFE', 8, TIMESTAMP '2014-01-17 00:00:00.0', TIMESTAMP '2014-02-14 00:00:00.0', TIMESTAMP '2014-01-22 00:00:00.0', 2, 2.9200, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10603, 'SAVEA', 8, TIMESTAMP '2014-01-18 00:00:00.0', TIMESTAMP '2014-02-15 00:00:00.0', TIMESTAMP '2014-02-08 00:00:00.0', 2, 48.7700, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10604, 'FURIB', 1, TIMESTAMP '2014-01-18 00:00:00.0', TIMESTAMP '2014-02-15 00:00:00.0', TIMESTAMP '2014-01-29 00:00:00.0', 1, 7.4600, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10605, 'MEREP', 1, TIMESTAMP '2014-01-21 00:00:00.0', TIMESTAMP '2014-02-18 00:00:00.0', TIMESTAMP '2014-01-29 00:00:00.0', 2, 379.1300, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10606, 'TRADH', 4, TIMESTAMP '2014-01-22 00:00:00.0', TIMESTAMP '2014-02-19 00:00:00.0', TIMESTAMP '2014-01-31 00:00:00.0', 3, 79.4000, 'Tradiao Hipermercados', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil'),
(10607, 'SAVEA', 5, TIMESTAMP '2014-01-22 00:00:00.0', TIMESTAMP '2014-02-19 00:00:00.0', TIMESTAMP '2014-01-25 00:00:00.0', 1, 200.2400, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10608, 'TOMSP', 4, TIMESTAMP '2014-01-23 00:00:00.0', TIMESTAMP '2014-02-20 00:00:00.0', TIMESTAMP '2014-02-01 00:00:00.0', 2, 27.7900, 'Toms Spezialitten', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany'),
(10609, 'DUMON', 7, TIMESTAMP '2014-01-24 00:00:00.0', TIMESTAMP '2014-02-21 00:00:00.0', TIMESTAMP '2014-01-30 00:00:00.0', 2, 1.8500, 'Du monde entier', '67, rue des Cinquante Otages', 'Nantes', NULL, '44000', 'France');         
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10610, 'LAMAI', 8, TIMESTAMP '2014-01-25 00:00:00.0', TIMESTAMP '2014-02-22 00:00:00.0', TIMESTAMP '2014-02-06 00:00:00.0', 1, 26.7800, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10611, 'WOLZA', 6, TIMESTAMP '2014-01-25 00:00:00.0', TIMESTAMP '2014-02-22 00:00:00.0', TIMESTAMP '2014-02-01 00:00:00.0', 2, 80.6500, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland'),
(10612, 'SAVEA', 1, TIMESTAMP '2014-01-28 00:00:00.0', TIMESTAMP '2014-02-25 00:00:00.0', TIMESTAMP '2014-02-01 00:00:00.0', 2, 544.0800, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10613, 'HILAA', 4, TIMESTAMP '2014-01-29 00:00:00.0', TIMESTAMP '2014-02-26 00:00:00.0', TIMESTAMP '2014-02-01 00:00:00.0', 2, 8.1100, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10614, 'BLAUS', 8, TIMESTAMP '2014-01-29 00:00:00.0', TIMESTAMP '2014-02-26 00:00:00.0', TIMESTAMP '2014-02-01 00:00:00.0', 3, 1.9300, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(10615, 'WILMK', 2, TIMESTAMP '2014-01-30 00:00:00.0', TIMESTAMP '2014-02-27 00:00:00.0', TIMESTAMP '2014-02-06 00:00:00.0', 3, 0.7500, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(10616, 'GREAL', 1, TIMESTAMP '2014-01-31 00:00:00.0', TIMESTAMP '2014-02-28 00:00:00.0', TIMESTAMP '2014-02-05 00:00:00.0', 2, 116.5300, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10617, 'GREAL', 4, TIMESTAMP '2014-01-31 00:00:00.0', TIMESTAMP '2014-02-28 00:00:00.0', TIMESTAMP '2014-02-04 00:00:00.0', 2, 18.5300, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10618, 'MEREP', 1, TIMESTAMP '2014-02-01 00:00:00.0', TIMESTAMP '2014-03-12 00:00:00.0', TIMESTAMP '2014-02-08 00:00:00.0', 1, 154.6800, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10619, 'MEREP', 3, TIMESTAMP '2014-02-04 00:00:00.0', TIMESTAMP '2014-03-01 00:00:00.0', TIMESTAMP '2014-02-07 00:00:00.0', 3, 91.0500, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10620, 'LAUGB', 2, TIMESTAMP '2014-02-05 00:00:00.0', TIMESTAMP '2014-03-02 00:00:00.0', TIMESTAMP '2014-02-14 00:00:00.0', 3, 0.9400, 'Laughing Bacchus Wine Cellars', '2319 Elm St.', 'Vancouver', 'BC', 'V3F 2K1', 'Canada'),
(10621, 'ISLAT', 4, TIMESTAMP '2014-02-05 00:00:00.0', TIMESTAMP '2014-03-02 00:00:00.0', TIMESTAMP '2014-02-11 00:00:00.0', 2, 23.7300, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10622, 'RICAR', 4, TIMESTAMP '2014-02-06 00:00:00.0', TIMESTAMP '2014-03-03 00:00:00.0', TIMESTAMP '2014-02-11 00:00:00.0', 3, 50.9700, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10623, 'FRANK', 8, TIMESTAMP '2014-02-07 00:00:00.0', TIMESTAMP '2014-03-04 00:00:00.0', TIMESTAMP '2014-02-12 00:00:00.0', 2, 97.1800, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10624, 'THECR', 4, TIMESTAMP '2014-02-07 00:00:00.0', TIMESTAMP '2014-03-04 00:00:00.0', TIMESTAMP '2014-02-19 00:00:00.0', 2, 94.8000, 'The Cracker Box', '55 Grizzly Peak Rd.', 'Butte', 'MT', '59801', 'USA'),
(10625, 'ANATR', 3, TIMESTAMP '2014-02-08 00:00:00.0', TIMESTAMP '2014-03-05 00:00:00.0', TIMESTAMP '2014-02-14 00:00:00.0', 1, 43.9000, 'Ana Trujillo Emparedados y helados', 'Avda. de la Constitucin 2222', 'Mxico D.F.', NULL, '5021', 'Mexico'),
(10626, 'BERGS', 1, TIMESTAMP '2014-02-11 00:00:00.0', TIMESTAMP '2014-03-08 00:00:00.0', TIMESTAMP '2014-02-20 00:00:00.0', 2, 138.6900, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10627, 'SAVEA', 8, TIMESTAMP '2014-02-11 00:00:00.0', TIMESTAMP '2014-03-22 00:00:00.0', TIMESTAMP '2014-02-21 00:00:00.0', 3, 107.4600, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA');    
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10628, 'BLONP', 4, TIMESTAMP '2014-02-12 00:00:00.0', TIMESTAMP '2014-03-09 00:00:00.0', TIMESTAMP '2014-02-20 00:00:00.0', 3, 30.3600, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10629, 'GODOS', 4, TIMESTAMP '2014-02-12 00:00:00.0', TIMESTAMP '2014-03-09 00:00:00.0', TIMESTAMP '2014-02-20 00:00:00.0', 3, 85.4600, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10630, 'KOENE', 1, TIMESTAMP '2014-02-13 00:00:00.0', TIMESTAMP '2014-03-10 00:00:00.0', TIMESTAMP '2014-02-19 00:00:00.0', 2, 32.3500, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10631, 'LAMAI', 8, TIMESTAMP '2014-02-14 00:00:00.0', TIMESTAMP '2014-03-11 00:00:00.0', TIMESTAMP '2014-02-15 00:00:00.0', 1, 0.8700, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10632, 'WANDK', 8, TIMESTAMP '2014-02-14 00:00:00.0', TIMESTAMP '2014-03-11 00:00:00.0', TIMESTAMP '2014-02-19 00:00:00.0', 1, 41.3800, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10633, 'ERNSH', 7, TIMESTAMP '2014-02-15 00:00:00.0', TIMESTAMP '2014-03-12 00:00:00.0', TIMESTAMP '2014-02-18 00:00:00.0', 3, 477.9000, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10634, 'FOLIG', 4, TIMESTAMP '2014-02-15 00:00:00.0', TIMESTAMP '2014-03-12 00:00:00.0', TIMESTAMP '2014-02-21 00:00:00.0', 3, 487.3800, 'Folies gourmandes', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France'),
(10635, 'MAGAA', 8, TIMESTAMP '2014-02-18 00:00:00.0', TIMESTAMP '2014-03-15 00:00:00.0', TIMESTAMP '2014-02-21 00:00:00.0', 3, 47.4600, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10636, 'WARTH', 4, TIMESTAMP '2014-02-19 00:00:00.0', TIMESTAMP '2014-03-16 00:00:00.0', TIMESTAMP '2014-02-26 00:00:00.0', 1, 1.1500, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10637, 'QUEEN', 6, TIMESTAMP '2014-02-19 00:00:00.0', TIMESTAMP '2014-03-16 00:00:00.0', TIMESTAMP '2014-02-26 00:00:00.0', 1, 201.2900, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10638, 'LINOD', 3, TIMESTAMP '2014-02-20 00:00:00.0', TIMESTAMP '2014-03-17 00:00:00.0', TIMESTAMP '2014-03-01 00:00:00.0', 1, 158.4400, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10639, 'SANTG', 7, TIMESTAMP '2014-02-20 00:00:00.0', TIMESTAMP '2014-03-17 00:00:00.0', TIMESTAMP '2014-02-27 00:00:00.0', 3, 38.6400, 'Sant Gourmet', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway'),
(10640, 'WANDK', 4, TIMESTAMP '2014-02-21 00:00:00.0', TIMESTAMP '2014-03-18 00:00:00.0', TIMESTAMP '2014-02-28 00:00:00.0', 1, 23.5500, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10641, 'HILAA', 4, TIMESTAMP '2014-02-22 00:00:00.0', TIMESTAMP '2014-03-19 00:00:00.0', TIMESTAMP '2014-02-26 00:00:00.0', 2, 179.6100, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10642, 'SIMOB', 7, TIMESTAMP '2014-02-22 00:00:00.0', TIMESTAMP '2014-03-19 00:00:00.0', TIMESTAMP '2014-03-05 00:00:00.0', 3, 41.8900, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(10643, 'ALFKI', 6, TIMESTAMP '2014-02-25 00:00:00.0', TIMESTAMP '2014-03-22 00:00:00.0', TIMESTAMP '2014-03-02 00:00:00.0', 1, 29.4600, 'Alfreds Futterkiste', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany'),
(10644, 'WELLI', 3, TIMESTAMP '2014-02-25 00:00:00.0', TIMESTAMP '2014-03-22 00:00:00.0', TIMESTAMP '2014-03-01 00:00:00.0', 2, 0.1400, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10645, 'HANAR', 4, TIMESTAMP '2014-02-26 00:00:00.0', TIMESTAMP '2014-03-23 00:00:00.0', TIMESTAMP '2014-03-02 00:00:00.0', 1, 12.4100, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil');         
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10646, 'HUNGO', 9, TIMESTAMP '2014-02-27 00:00:00.0', TIMESTAMP '2014-04-08 00:00:00.0', TIMESTAMP '2014-03-03 00:00:00.0', 3, 142.3300, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10647, 'QUEDE', 4, TIMESTAMP '2014-02-27 00:00:00.0', TIMESTAMP '2014-03-10 00:00:00.0', TIMESTAMP '2014-03-03 00:00:00.0', 2, 45.5400, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10648, 'RICAR', 5, TIMESTAMP '2014-02-28 00:00:00.0', TIMESTAMP '2014-04-09 00:00:00.0', TIMESTAMP '2014-03-09 00:00:00.0', 2, 14.2500, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10649, 'MAISD', 5, TIMESTAMP '2014-02-28 00:00:00.0', TIMESTAMP '2014-03-25 00:00:00.0', TIMESTAMP '2014-02-28 00:00:00.0', 3, 6.2000, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(10650, 'FAMIA', 5, TIMESTAMP '2014-02-28 00:00:00.0', TIMESTAMP '2014-03-26 00:00:00.0', TIMESTAMP '2014-03-03 00:00:00.0', 3, 176.8100, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10651, 'WANDK', 8, TIMESTAMP '2014-03-01 00:00:00.0', TIMESTAMP '2014-03-29 00:00:00.0', TIMESTAMP '2014-03-11 00:00:00.0', 2, 20.6000, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10652, 'GOURL', 4, TIMESTAMP '2014-03-01 00:00:00.0', TIMESTAMP '2014-03-29 00:00:00.0', TIMESTAMP '2014-03-08 00:00:00.0', 2, 7.1400, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10653, 'FRANK', 1, TIMESTAMP '2014-03-02 00:00:00.0', TIMESTAMP '2014-03-30 00:00:00.0', TIMESTAMP '2014-03-19 00:00:00.0', 1, 93.2500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10654, 'BERGS', 5, TIMESTAMP '2014-03-02 00:00:00.0', TIMESTAMP '2014-03-30 00:00:00.0', TIMESTAMP '2014-03-11 00:00:00.0', 1, 55.2600, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10655, 'REGGC', 1, TIMESTAMP '2014-03-03 00:00:00.0', TIMESTAMP '2014-04-01 00:00:00.0', TIMESTAMP '2014-03-11 00:00:00.0', 2, 4.4100, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10656, 'GREAL', 6, TIMESTAMP '2014-03-04 00:00:00.0', TIMESTAMP '2014-04-02 00:00:00.0', TIMESTAMP '2014-03-10 00:00:00.0', 1, 57.1500, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10657, 'SAVEA', 2, TIMESTAMP '2014-03-04 00:00:00.0', TIMESTAMP '2014-04-02 00:00:00.0', TIMESTAMP '2014-03-15 00:00:00.0', 2, 352.6900, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10658, 'QUICK', 4, TIMESTAMP '2014-03-05 00:00:00.0', TIMESTAMP '2014-04-03 00:00:00.0', TIMESTAMP '2014-03-08 00:00:00.0', 1, 364.1500, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10659, 'QUEEN', 7, TIMESTAMP '2014-03-05 00:00:00.0', TIMESTAMP '2014-04-03 00:00:00.0', TIMESTAMP '2014-03-10 00:00:00.0', 2, 105.8100, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10660, 'HUNGC', 8, TIMESTAMP '2014-03-08 00:00:00.0', TIMESTAMP '2014-04-06 00:00:00.0', TIMESTAMP '2014-04-15 00:00:00.0', 1, 111.2900, 'Hungry Coyote Import Store', 'City Center Plaza 516 Main St.', 'Elgin', 'OR', '97827', 'USA'),
(10661, 'HUNGO', 7, TIMESTAMP '2014-03-09 00:00:00.0', TIMESTAMP '2014-04-07 00:00:00.0', TIMESTAMP '2014-03-15 00:00:00.0', 3, 17.5500, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10662, 'LONEP', 3, TIMESTAMP '2014-03-09 00:00:00.0', TIMESTAMP '2014-04-07 00:00:00.0', TIMESTAMP '2014-03-18 00:00:00.0', 2, 1.2800, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(10663, 'BONAP', 2, TIMESTAMP '2014-03-10 00:00:00.0', TIMESTAMP '2014-03-24 00:00:00.0', TIMESTAMP '2014-04-03 00:00:00.0', 2, 113.1500, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France');       
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10664, 'FURIB', 1, TIMESTAMP '2014-03-10 00:00:00.0', TIMESTAMP '2014-04-08 00:00:00.0', TIMESTAMP '2014-03-19 00:00:00.0', 3, 1.2700, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10665, 'LONEP', 1, TIMESTAMP '2014-03-11 00:00:00.0', TIMESTAMP '2014-04-09 00:00:00.0', TIMESTAMP '2014-03-17 00:00:00.0', 2, 26.3100, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(10666, 'RICSU', 7, TIMESTAMP '2014-03-12 00:00:00.0', TIMESTAMP '2014-04-10 00:00:00.0', TIMESTAMP '2014-03-22 00:00:00.0', 2, 232.4200, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10667, 'ERNSH', 7, TIMESTAMP '2014-03-12 00:00:00.0', TIMESTAMP '2014-04-10 00:00:00.0', TIMESTAMP '2014-03-19 00:00:00.0', 1, 78.0900, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10668, 'WANDK', 1, TIMESTAMP '2014-03-15 00:00:00.0', TIMESTAMP '2014-04-13 00:00:00.0', TIMESTAMP '2014-03-23 00:00:00.0', 2, 47.2200, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(10669, 'SIMOB', 2, TIMESTAMP '2014-03-15 00:00:00.0', TIMESTAMP '2014-04-13 00:00:00.0', TIMESTAMP '2014-03-22 00:00:00.0', 1, 24.3900, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(10670, 'FRANK', 4, TIMESTAMP '2014-03-16 00:00:00.0', TIMESTAMP '2014-04-14 00:00:00.0', TIMESTAMP '2014-03-18 00:00:00.0', 1, 203.4800, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10671, 'FRANR', 1, TIMESTAMP '2014-03-17 00:00:00.0', TIMESTAMP '2014-04-15 00:00:00.0', TIMESTAMP '2014-03-24 00:00:00.0', 1, 30.3400, 'France restauration', '54, rue Royale', 'Nantes', NULL, '44000', 'France'),
(10672, 'BERGS', 9, TIMESTAMP '2014-03-17 00:00:00.0', TIMESTAMP '2014-04-01 00:00:00.0', TIMESTAMP '2014-03-26 00:00:00.0', 2, 95.7500, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10673, 'WILMK', 2, TIMESTAMP '2014-03-18 00:00:00.0', TIMESTAMP '2014-04-16 00:00:00.0', TIMESTAMP '2014-03-19 00:00:00.0', 1, 22.7600, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(10674, 'ISLAT', 4, TIMESTAMP '2014-03-18 00:00:00.0', TIMESTAMP '2014-04-16 00:00:00.0', TIMESTAMP '2014-03-30 00:00:00.0', 2, 0.9000, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10675, 'FRANK', 5, TIMESTAMP '2014-03-19 00:00:00.0', TIMESTAMP '2014-04-17 00:00:00.0', TIMESTAMP '2014-03-23 00:00:00.0', 2, 31.8500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10676, 'TORTU', 2, TIMESTAMP '2014-03-22 00:00:00.0', TIMESTAMP '2014-04-20 00:00:00.0', TIMESTAMP '2014-03-29 00:00:00.0', 2, 2.0100, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10677, 'ANTON', 1, TIMESTAMP '2014-03-22 00:00:00.0', TIMESTAMP '2014-04-20 00:00:00.0', TIMESTAMP '2014-03-26 00:00:00.0', 3, 4.0300, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico'),
(10678, 'SAVEA', 7, TIMESTAMP '2014-03-23 00:00:00.0', TIMESTAMP '2014-04-21 00:00:00.0', TIMESTAMP '2014-04-16 00:00:00.0', 3, 388.9800, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10679, 'BLONP', 8, TIMESTAMP '2014-03-23 00:00:00.0', TIMESTAMP '2014-04-21 00:00:00.0', TIMESTAMP '2014-03-30 00:00:00.0', 3, 27.9400, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10680, 'OLDWO', 1, TIMESTAMP '2014-03-24 00:00:00.0', TIMESTAMP '2014-04-22 00:00:00.0', TIMESTAMP '2014-03-26 00:00:00.0', 1, 26.6100, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10681, 'GREAL', 3, TIMESTAMP '2014-03-25 00:00:00.0', TIMESTAMP '2014-04-23 00:00:00.0', TIMESTAMP '2014-03-30 00:00:00.0', 3, 76.1300, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10682, 'ANTON', 3, TIMESTAMP '2014-03-25 00:00:00.0', TIMESTAMP '2014-04-23 00:00:00.0', TIMESTAMP '2014-04-01 00:00:00.0', 2, 36.1300, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico');       
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10683, 'DUMON', 2, TIMESTAMP '2014-03-26 00:00:00.0', TIMESTAMP '2014-04-24 00:00:00.0', TIMESTAMP '2014-04-01 00:00:00.0', 1, 4.4000, 'Du monde entier', '67, rue des Cinquante Otages', 'Nantes', NULL, '44000', 'France'),
(10684, 'OTTIK', 3, TIMESTAMP '2014-03-26 00:00:00.0', TIMESTAMP '2014-04-24 00:00:00.0', TIMESTAMP '2014-03-30 00:00:00.0', 1, 145.6300, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10685, 'GOURL', 4, TIMESTAMP '2014-03-29 00:00:00.0', TIMESTAMP '2014-04-13 00:00:00.0', TIMESTAMP '2014-04-03 00:00:00.0', 2, 33.7500, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10686, 'PICCO', 2, TIMESTAMP '2014-03-30 00:00:00.0', TIMESTAMP '2014-04-28 00:00:00.0', TIMESTAMP '2014-04-08 00:00:00.0', 1, 96.5000, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10687, 'HUNGO', 9, TIMESTAMP '2014-03-30 00:00:00.0', TIMESTAMP '2014-04-28 00:00:00.0', TIMESTAMP '2014-04-30 00:00:00.0', 2, 296.4300, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10688, 'VAFFE', 4, TIMESTAMP '2014-04-01 00:00:00.0', TIMESTAMP '2014-04-15 00:00:00.0', TIMESTAMP '2014-04-07 00:00:00.0', 2, 299.0900, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10689, 'BERGS', 1, TIMESTAMP '2014-04-01 00:00:00.0', TIMESTAMP '2014-04-29 00:00:00.0', TIMESTAMP '2014-04-07 00:00:00.0', 2, 13.4200, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10690, 'HANAR', 1, TIMESTAMP '2014-04-02 00:00:00.0', TIMESTAMP '2014-04-30 00:00:00.0', TIMESTAMP '2014-04-03 00:00:00.0', 1, 15.8000, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10691, 'QUICK', 2, TIMESTAMP '2014-04-03 00:00:00.0', TIMESTAMP '2014-05-14 00:00:00.0', TIMESTAMP '2014-04-22 00:00:00.0', 2, 810.0500, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10692, 'ALFKI', 4, TIMESTAMP '2014-04-03 00:00:00.0', TIMESTAMP '2014-04-30 00:00:00.0', TIMESTAMP '2014-04-13 00:00:00.0', 2, 61.0200, 'Alfred-s Futterkiste', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany'),
(10693, 'WHITC', 3, TIMESTAMP '2014-04-06 00:00:00.0', TIMESTAMP '2014-04-20 00:00:00.0', TIMESTAMP '2014-04-10 00:00:00.0', 3, 139.3400, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10694, 'QUICK', 8, TIMESTAMP '2014-04-06 00:00:00.0', TIMESTAMP '2014-05-03 00:00:00.0', TIMESTAMP '2014-04-09 00:00:00.0', 3, 398.3600, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10695, 'WILMK', 7, TIMESTAMP '2014-04-07 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', TIMESTAMP '2014-04-14 00:00:00.0', 1, 16.7200, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(10696, 'WHITC', 8, TIMESTAMP '2014-04-08 00:00:00.0', TIMESTAMP '2014-05-19 00:00:00.0', TIMESTAMP '2014-04-14 00:00:00.0', 3, 102.5500, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10697, 'LINOD', 3, TIMESTAMP '2014-04-08 00:00:00.0', TIMESTAMP '2014-05-05 00:00:00.0', TIMESTAMP '2014-04-14 00:00:00.0', 1, 45.5200, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10698, 'ERNSH', 4, TIMESTAMP '2014-04-09 00:00:00.0', TIMESTAMP '2014-05-06 00:00:00.0', TIMESTAMP '2014-04-17 00:00:00.0', 1, 272.4700, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10699, 'MORGK', 3, TIMESTAMP '2014-04-09 00:00:00.0', TIMESTAMP '2014-05-06 00:00:00.0', TIMESTAMP '2014-04-13 00:00:00.0', 3, 0.5800, 'Morgenstern Gesundkost', 'Heerstr. 22', 'Leipzig', NULL, '4179', 'Germany'),
(10700, 'SAVEA', 3, TIMESTAMP '2014-04-10 00:00:00.0', TIMESTAMP '2014-05-07 00:00:00.0', TIMESTAMP '2014-04-16 00:00:00.0', 1, 65.1000, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10701, 'HUNGO', 6, TIMESTAMP '2014-04-13 00:00:00.0', TIMESTAMP '2014-04-27 00:00:00.0', TIMESTAMP '2014-04-15 00:00:00.0', 3, 220.3100, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland');    
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10702, 'ALFKI', 4, TIMESTAMP '2014-04-13 00:00:00.0', TIMESTAMP '2014-05-24 00:00:00.0', TIMESTAMP '2014-04-21 00:00:00.0', 1, 23.9400, 'Alfred-s Futterkiste', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany'),
(10703, 'FOLKO', 6, TIMESTAMP '2014-04-14 00:00:00.0', TIMESTAMP '2014-05-11 00:00:00.0', TIMESTAMP '2014-04-20 00:00:00.0', 2, 152.3000, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10704, 'QUEEN', 6, TIMESTAMP '2014-04-14 00:00:00.0', TIMESTAMP '2014-05-11 00:00:00.0', TIMESTAMP '2014-05-07 00:00:00.0', 1, 4.7800, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10705, 'HILAA', 9, TIMESTAMP '2014-04-15 00:00:00.0', TIMESTAMP '2014-05-12 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', 2, 3.5200, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10706, 'OLDWO', 8, TIMESTAMP '2014-04-16 00:00:00.0', TIMESTAMP '2014-05-13 00:00:00.0', TIMESTAMP '2014-04-21 00:00:00.0', 3, 135.6300, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10707, 'AROUT', 4, TIMESTAMP '2014-04-16 00:00:00.0', TIMESTAMP '2014-04-30 00:00:00.0', TIMESTAMP '2014-04-23 00:00:00.0', 3, 21.7400, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10708, 'THEBI', 6, TIMESTAMP '2014-04-17 00:00:00.0', TIMESTAMP '2014-05-28 00:00:00.0', TIMESTAMP '2014-05-05 00:00:00.0', 2, 2.9600, 'The Big Cheese', '89 Jefferson Way Suite 2', 'Portland', 'OR', '97201', 'USA'),
(10709, 'GOURL', 1, TIMESTAMP '2014-04-17 00:00:00.0', TIMESTAMP '2014-05-14 00:00:00.0', TIMESTAMP '2014-05-20 00:00:00.0', 3, 210.8000, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10710, 'FRANS', 1, TIMESTAMP '2014-04-20 00:00:00.0', TIMESTAMP '2014-05-17 00:00:00.0', TIMESTAMP '2014-04-23 00:00:00.0', 1, 4.9800, 'Franchi S.p.A.', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy'),
(10711, 'SAVEA', 5, TIMESTAMP '2014-04-21 00:00:00.0', TIMESTAMP '2014-06-02 00:00:00.0', TIMESTAMP '2014-04-29 00:00:00.0', 2, 52.4100, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10712, 'HUNGO', 3, TIMESTAMP '2014-04-21 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', TIMESTAMP '2014-04-30 00:00:00.0', 1, 89.9300, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10713, 'SAVEA', 1, TIMESTAMP '2014-04-22 00:00:00.0', TIMESTAMP '2014-05-19 00:00:00.0', TIMESTAMP '2014-04-24 00:00:00.0', 1, 167.0500, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10714, 'SAVEA', 5, TIMESTAMP '2014-04-22 00:00:00.0', TIMESTAMP '2014-05-19 00:00:00.0', TIMESTAMP '2014-04-27 00:00:00.0', 3, 24.4900, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10715, 'BONAP', 3, TIMESTAMP '2014-04-23 00:00:00.0', TIMESTAMP '2014-05-06 00:00:00.0', TIMESTAMP '2014-04-29 00:00:00.0', 1, 63.2000, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10716, 'RANCH', 4, TIMESTAMP '2014-04-24 00:00:00.0', TIMESTAMP '2014-05-21 00:00:00.0', TIMESTAMP '2014-04-27 00:00:00.0', 2, 22.5700, 'Rancho grande', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10717, 'FRANK', 1, TIMESTAMP '2014-04-24 00:00:00.0', TIMESTAMP '2014-05-21 00:00:00.0', TIMESTAMP '2014-04-29 00:00:00.0', 2, 59.2500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10718, 'KOENE', 1, TIMESTAMP '2014-04-27 00:00:00.0', TIMESTAMP '2014-05-24 00:00:00.0', TIMESTAMP '2014-04-29 00:00:00.0', 3, 170.8800, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10719, 'LETSS', 8, TIMESTAMP '2014-04-27 00:00:00.0', TIMESTAMP '2014-05-24 00:00:00.0', TIMESTAMP '2014-05-05 00:00:00.0', 2, 51.4400, 'Let-s Stop N Shop', '87 Polk St. Suite 5', 'San Francisco', 'CA', '94117', 'USA');   
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10720, 'QUEDE', 8, TIMESTAMP '2014-04-28 00:00:00.0', TIMESTAMP '2014-05-11 00:00:00.0', TIMESTAMP '2014-05-05 00:00:00.0', 2, 9.5300, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10721, 'QUICK', 5, TIMESTAMP '2014-04-29 00:00:00.0', TIMESTAMP '2014-05-26 00:00:00.0', TIMESTAMP '2014-04-30 00:00:00.0', 3, 48.9200, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10722, 'SAVEA', 8, TIMESTAMP '2014-04-29 00:00:00.0', TIMESTAMP '2014-06-10 00:00:00.0', TIMESTAMP '2014-05-04 00:00:00.0', 1, 74.5800, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10723, 'WHITC', 3, TIMESTAMP '2014-04-30 00:00:00.0', TIMESTAMP '2014-05-27 00:00:00.0', TIMESTAMP '2014-05-25 00:00:00.0', 1, 21.7200, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10724, 'MEREP', 8, TIMESTAMP '2014-04-30 00:00:00.0', TIMESTAMP '2014-06-11 00:00:00.0', TIMESTAMP '2014-05-05 00:00:00.0', 2, 57.7500, 'Mre Paillarde', '43 rue St. Laurent', 'Montral', 'Qubec', 'H1J 1C3', 'Canada'),
(10725, 'FAMIA', 4, TIMESTAMP '2014-04-30 00:00:00.0', TIMESTAMP '2014-05-28 00:00:00.0', TIMESTAMP '2014-05-05 00:00:00.0', 3, 10.8300, 'Familia Arquibaldo', 'Rua Ors, 92', 'Sao Paulo', 'SP', '05442-030', 'Brazil'),
(10726, 'EASTC', 4, TIMESTAMP '2014-05-03 00:00:00.0', TIMESTAMP '2014-05-17 00:00:00.0', TIMESTAMP '2014-06-05 00:00:00.0', 1, 16.5600, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(10727, 'REGGC', 2, TIMESTAMP '2014-05-03 00:00:00.0', TIMESTAMP '2014-06-01 00:00:00.0', TIMESTAMP '2014-06-05 00:00:00.0', 1, 89.9000, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10728, 'QUEEN', 4, TIMESTAMP '2014-05-04 00:00:00.0', TIMESTAMP '2014-06-02 00:00:00.0', TIMESTAMP '2014-05-11 00:00:00.0', 2, 58.3300, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10729, 'LINOD', 8, TIMESTAMP '2014-05-04 00:00:00.0', TIMESTAMP '2014-06-16 00:00:00.0', TIMESTAMP '2014-05-14 00:00:00.0', 3, 141.0600, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10730, 'BONAP', 5, TIMESTAMP '2014-05-05 00:00:00.0', TIMESTAMP '2014-06-03 00:00:00.0', TIMESTAMP '2014-05-14 00:00:00.0', 1, 20.1200, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10731, 'CHOPS', 7, TIMESTAMP '2014-05-06 00:00:00.0', TIMESTAMP '2014-06-04 00:00:00.0', TIMESTAMP '2014-05-14 00:00:00.0', 1, 96.6500, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(10732, 'BONAP', 3, TIMESTAMP '2014-05-06 00:00:00.0', TIMESTAMP '2014-06-04 00:00:00.0', TIMESTAMP '2014-05-07 00:00:00.0', 1, 16.9700, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10733, 'BERGS', 1, TIMESTAMP '2014-05-07 00:00:00.0', TIMESTAMP '2014-06-05 00:00:00.0', TIMESTAMP '2014-05-10 00:00:00.0', 3, 110.1100, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10734, 'GOURL', 2, TIMESTAMP '2014-05-07 00:00:00.0', TIMESTAMP '2014-06-05 00:00:00.0', TIMESTAMP '2014-05-12 00:00:00.0', 3, 1.6300, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10735, 'LETSS', 6, TIMESTAMP '2014-05-10 00:00:00.0', TIMESTAMP '2014-06-08 00:00:00.0', TIMESTAMP '2014-05-21 00:00:00.0', 2, 45.9700, 'Let-s Stop N Shop', '87 Polk St. Suite 5', 'San Francisco', 'CA', '94117', 'USA'),
(10736, 'HUNGO', 9, TIMESTAMP '2014-05-11 00:00:00.0', TIMESTAMP '2014-06-09 00:00:00.0', TIMESTAMP '2014-05-21 00:00:00.0', 2, 44.1000, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10737, 'VINET', 2, TIMESTAMP '2014-05-11 00:00:00.0', TIMESTAMP '2014-06-09 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', 2, 7.7900, 'Vins et alcools Chevalier', '59 rue de l-Abbaye', 'Reims', NULL, '51100', 'France');
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10738, 'SPECD', 2, TIMESTAMP '2014-05-12 00:00:00.0', TIMESTAMP '2014-06-10 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', 1, 2.9100, 'Spcialits du monde', '25, rue Lauriston', 'Paris', NULL, '75016', 'France'),
(10739, 'VINET', 3, TIMESTAMP '2014-05-12 00:00:00.0', TIMESTAMP '2014-06-10 00:00:00.0', TIMESTAMP '2014-05-17 00:00:00.0', 3, 11.0800, 'Vins et alcools Chevalier', '59 rue de l-Abbaye', 'Reims', NULL, '51100', 'France'),
(10740, 'WHITC', 4, TIMESTAMP '2014-05-13 00:00:00.0', TIMESTAMP '2014-06-11 00:00:00.0', TIMESTAMP '2014-05-25 00:00:00.0', 2, 81.8800, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10741, 'AROUT', 4, TIMESTAMP '2014-05-14 00:00:00.0', TIMESTAMP '2014-05-28 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', 3, 10.9600, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10742, 'BOTTM', 3, TIMESTAMP '2014-05-14 00:00:00.0', TIMESTAMP '2014-06-12 00:00:00.0', TIMESTAMP '2014-05-18 00:00:00.0', 3, 243.7300, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10743, 'AROUT', 1, TIMESTAMP '2014-05-17 00:00:00.0', TIMESTAMP '2014-06-15 00:00:00.0', TIMESTAMP '2014-05-21 00:00:00.0', 2, 23.7200, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10744, 'VAFFE', 6, TIMESTAMP '2014-05-17 00:00:00.0', TIMESTAMP '2014-06-15 00:00:00.0', TIMESTAMP '2014-05-24 00:00:00.0', 1, 69.1900, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10745, 'QUICK', 9, TIMESTAMP '2014-05-18 00:00:00.0', TIMESTAMP '2014-06-16 00:00:00.0', TIMESTAMP '2014-05-27 00:00:00.0', 1, 3.5200, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10746, 'CHOPS', 1, TIMESTAMP '2014-05-19 00:00:00.0', TIMESTAMP '2014-06-17 00:00:00.0', TIMESTAMP '2014-05-21 00:00:00.0', 3, 31.4300, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(10747, 'PICCO', 6, TIMESTAMP '2014-05-19 00:00:00.0', TIMESTAMP '2014-06-17 00:00:00.0', TIMESTAMP '2014-05-26 00:00:00.0', 1, 117.3300, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10748, 'SAVEA', 3, TIMESTAMP '2014-05-20 00:00:00.0', TIMESTAMP '2014-06-18 00:00:00.0', TIMESTAMP '2014-05-28 00:00:00.0', 1, 232.5500, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10749, 'ISLAT', 4, TIMESTAMP '2014-05-20 00:00:00.0', TIMESTAMP '2014-06-18 00:00:00.0', TIMESTAMP '2014-06-19 00:00:00.0', 2, 61.5300, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10750, 'WARTH', 9, TIMESTAMP '2014-05-21 00:00:00.0', TIMESTAMP '2014-06-19 00:00:00.0', TIMESTAMP '2014-05-24 00:00:00.0', 1, 79.3000, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10751, 'RICSU', 3, TIMESTAMP '2014-05-24 00:00:00.0', TIMESTAMP '2014-06-22 00:00:00.0', TIMESTAMP '2014-06-03 00:00:00.0', 3, 130.7900, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10752, 'NORTS', 2, TIMESTAMP '2014-05-24 00:00:00.0', TIMESTAMP '2014-06-22 00:00:00.0', TIMESTAMP '2014-05-28 00:00:00.0', 3, 1.3900, 'North/South', 'South House 300 Queensbridge', 'London', NULL, 'SW7 1RZ', 'UK'),
(10753, 'FRANS', 3, TIMESTAMP '2014-05-25 00:00:00.0', TIMESTAMP '2014-06-23 00:00:00.0', TIMESTAMP '2014-05-27 00:00:00.0', 1, 7.7000, 'Franchi S.p.A.', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy'),
(10754, 'MAGAA', 6, TIMESTAMP '2014-05-25 00:00:00.0', TIMESTAMP '2014-06-23 00:00:00.0', TIMESTAMP '2014-05-27 00:00:00.0', 3, 2.3800, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10755, 'BONAP', 4, TIMESTAMP '2014-05-26 00:00:00.0', TIMESTAMP '2014-06-24 00:00:00.0', TIMESTAMP '2014-05-28 00:00:00.0', 2, 16.7100, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France');   
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10756, 'SPLIR', 8, TIMESTAMP '2014-05-27 00:00:00.0', TIMESTAMP '2014-06-25 00:00:00.0', TIMESTAMP '2014-06-02 00:00:00.0', 2, 73.2100, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10757, 'SAVEA', 6, TIMESTAMP '2014-05-27 00:00:00.0', TIMESTAMP '2014-06-25 00:00:00.0', TIMESTAMP '2014-06-15 00:00:00.0', 1, 8.1900, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10758, 'RICSU', 3, TIMESTAMP '2014-05-28 00:00:00.0', TIMESTAMP '2014-06-26 00:00:00.0', TIMESTAMP '2014-06-04 00:00:00.0', 3, 138.1700, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10759, 'ANATR', 3, TIMESTAMP '2014-05-28 00:00:00.0', TIMESTAMP '2014-06-26 00:00:00.0', TIMESTAMP '2014-06-12 00:00:00.0', 3, 11.9900, 'Ana Trujillo Emparedados y helados', 'Avda. de la Constitucin 2222', 'Mxico D.F.', NULL, '5021', 'Mexico'),
(10760, 'MAISD', 4, TIMESTAMP '2014-06-01 00:00:00.0', TIMESTAMP '2014-06-29 00:00:00.0', TIMESTAMP '2014-06-10 00:00:00.0', 1, 155.6400, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(10761, 'RATTC', 5, TIMESTAMP '2014-06-02 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-06-08 00:00:00.0', 2, 18.6600, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10762, 'FOLKO', 3, TIMESTAMP '2014-06-02 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-06-09 00:00:00.0', 1, 328.7400, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10763, 'FOLIG', 3, TIMESTAMP '2014-06-03 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-06-08 00:00:00.0', 3, 37.3500, 'Folies gourmandes', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France'),
(10764, 'ERNSH', 6, TIMESTAMP '2014-06-03 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-06-08 00:00:00.0', 3, 145.4500, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10765, 'QUICK', 3, TIMESTAMP '2014-06-04 00:00:00.0', TIMESTAMP '2014-07-01 00:00:00.0', TIMESTAMP '2014-06-09 00:00:00.0', 3, 42.7400, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10766, 'OTTIK', 4, TIMESTAMP '2014-06-05 00:00:00.0', TIMESTAMP '2014-07-02 00:00:00.0', TIMESTAMP '2014-06-09 00:00:00.0', 1, 157.5500, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10767, 'SUPRD', 4, TIMESTAMP '2014-06-05 00:00:00.0', TIMESTAMP '2014-07-02 00:00:00.0', TIMESTAMP '2014-06-15 00:00:00.0', 3, 1.5900, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10768, 'AROUT', 3, TIMESTAMP '2014-06-08 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', TIMESTAMP '2014-06-15 00:00:00.0', 2, 146.3200, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10769, 'VAFFE', 3, TIMESTAMP '2014-06-08 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', TIMESTAMP '2014-06-12 00:00:00.0', 1, 65.0600, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10770, 'HANAR', 8, TIMESTAMP '2014-06-09 00:00:00.0', TIMESTAMP '2014-07-06 00:00:00.0', TIMESTAMP '2014-06-17 00:00:00.0', 3, 5.3200, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10771, 'ERNSH', 9, TIMESTAMP '2014-06-10 00:00:00.0', TIMESTAMP '2014-07-07 00:00:00.0', TIMESTAMP '2014-07-02 00:00:00.0', 2, 11.1900, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10772, 'LEHMS', 3, TIMESTAMP '2014-06-10 00:00:00.0', TIMESTAMP '2014-07-07 00:00:00.0', TIMESTAMP '2014-06-19 00:00:00.0', 2, 91.2800, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10773, 'ERNSH', 1, TIMESTAMP '2014-06-11 00:00:00.0', TIMESTAMP '2014-07-08 00:00:00.0', TIMESTAMP '2014-06-16 00:00:00.0', 3, 96.4300, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10774, 'FOLKO', 4, TIMESTAMP '2014-06-11 00:00:00.0', TIMESTAMP '2014-06-25 00:00:00.0', TIMESTAMP '2014-06-12 00:00:00.0', 1, 48.2000, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden');
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10775, 'THECR', 7, TIMESTAMP '2014-06-12 00:00:00.0', TIMESTAMP '2014-07-09 00:00:00.0', TIMESTAMP '2014-06-26 00:00:00.0', 1, 20.2500, 'The Cracker Box', '55 Grizzly Peak Rd.', 'Butte', 'MT', '59801', 'USA'),
(10776, 'ERNSH', 1, TIMESTAMP '2014-06-15 00:00:00.0', TIMESTAMP '2014-07-12 00:00:00.0', TIMESTAMP '2014-06-18 00:00:00.0', 3, 351.5300, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10777, 'GOURL', 7, TIMESTAMP '2014-06-15 00:00:00.0', TIMESTAMP '2014-06-29 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', 2, 3.0100, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10778, 'BERGS', 3, TIMESTAMP '2014-06-16 00:00:00.0', TIMESTAMP '2014-07-13 00:00:00.0', TIMESTAMP '2014-06-24 00:00:00.0', 1, 6.7900, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10779, 'MORGK', 3, TIMESTAMP '2014-06-16 00:00:00.0', TIMESTAMP '2014-07-13 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', 2, 58.1300, 'Morgenstern Gesundkost', 'Heerstr. 22', 'Leipzig', NULL, '4179', 'Germany'),
(10780, 'LILAS', 2, TIMESTAMP '2014-06-16 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-06-25 00:00:00.0', 1, 42.1300, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10781, 'WARTH', 2, TIMESTAMP '2014-06-17 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', TIMESTAMP '2014-06-19 00:00:00.0', 3, 73.1600, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(10782, 'CACTU', 9, TIMESTAMP '2014-06-17 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', TIMESTAMP '2014-06-22 00:00:00.0', 3, 1.1000, 'Cactus Comidas para llevar', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10783, 'HANAR', 4, TIMESTAMP '2014-06-18 00:00:00.0', TIMESTAMP '2014-07-15 00:00:00.0', TIMESTAMP '2014-06-19 00:00:00.0', 2, 124.9800, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10784, 'MAGAA', 4, TIMESTAMP '2014-06-18 00:00:00.0', TIMESTAMP '2014-07-15 00:00:00.0', TIMESTAMP '2014-06-22 00:00:00.0', 3, 70.0900, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10785, 'GROSR', 1, TIMESTAMP '2014-06-18 00:00:00.0', TIMESTAMP '2014-07-15 00:00:00.0', TIMESTAMP '2014-06-24 00:00:00.0', 3, 1.5100, 'GROSELLA-Restaurante', '5 Ave. Los Palos Grandes', 'Caracas', 'DF', '1081', 'Venezuela'),
(10786, 'QUEEN', 8, TIMESTAMP '2014-06-19 00:00:00.0', TIMESTAMP '2014-07-16 00:00:00.0', TIMESTAMP '2014-06-23 00:00:00.0', 1, 110.8700, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10787, 'LAMAI', 2, TIMESTAMP '2014-06-19 00:00:00.0', TIMESTAMP '2014-07-02 00:00:00.0', TIMESTAMP '2014-06-26 00:00:00.0', 1, 249.9300, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10788, 'QUICK', 1, TIMESTAMP '2014-06-22 00:00:00.0', TIMESTAMP '2014-07-19 00:00:00.0', TIMESTAMP '2014-07-19 00:00:00.0', 2, 42.7000, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10789, 'FOLIG', 1, TIMESTAMP '2014-06-22 00:00:00.0', TIMESTAMP '2014-07-19 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', 2, 100.6000, 'Folies gourmandes', '184, chausse de Tournai', 'Lille', NULL, '59000', 'France'),
(10790, 'GOURL', 6, TIMESTAMP '2014-06-22 00:00:00.0', TIMESTAMP '2014-07-19 00:00:00.0', TIMESTAMP '2014-06-26 00:00:00.0', 1, 28.2300, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10791, 'FRANK', 6, TIMESTAMP '2014-06-23 00:00:00.0', TIMESTAMP '2014-07-20 00:00:00.0', TIMESTAMP '2014-07-01 00:00:00.0', 2, 16.8500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10792, 'WOLZA', 1, TIMESTAMP '2014-06-23 00:00:00.0', TIMESTAMP '2014-07-20 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', 3, 23.7900, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland');            
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10793, 'AROUT', 3, TIMESTAMP '2014-06-24 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', TIMESTAMP '2014-07-08 00:00:00.0', 3, 4.5200, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10794, 'QUEDE', 6, TIMESTAMP '2014-06-24 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', TIMESTAMP '2014-07-02 00:00:00.0', 1, 21.4900, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10795, 'ERNSH', 8, TIMESTAMP '2014-06-24 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', TIMESTAMP '2014-07-20 00:00:00.0', 2, 126.6600, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10796, 'HILAA', 3, TIMESTAMP '2014-06-25 00:00:00.0', TIMESTAMP '2014-07-22 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', 1, 26.5200, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10797, 'DRACD', 7, TIMESTAMP '2014-06-25 00:00:00.0', TIMESTAMP '2014-07-22 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', 2, 33.3500, 'Drachenblut Delikatessen', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany'),
(10798, 'ISLAT', 2, TIMESTAMP '2014-06-26 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', 1, 2.3300, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10799, 'KOENE', 9, TIMESTAMP '2014-06-26 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', 3, 30.7600, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10800, 'SEVES', 1, TIMESTAMP '2014-06-26 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', 3, 137.4400, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10801, 'BOLID', 4, TIMESTAMP '2014-06-29 00:00:00.0', TIMESTAMP '2014-07-26 00:00:00.0', TIMESTAMP '2014-06-30 00:00:00.0', 2, 97.0900, 'Blido Comidas preparadas', 'C/ Araquil, 67', 'Madrid', NULL, '28023', 'Spain'),
(10802, 'SIMOB', 4, TIMESTAMP '2014-06-29 00:00:00.0', TIMESTAMP '2014-07-26 00:00:00.0', TIMESTAMP '2014-07-02 00:00:00.0', 2, 257.2600, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(10803, 'WELLI', 4, TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-07-06 00:00:00.0', 1, 55.2300, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10804, 'SEVES', 6, TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-07-07 00:00:00.0', 2, 27.3300, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10805, 'THEBI', 2, TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-07-09 00:00:00.0', 3, 237.3400, 'The Big Cheese', '89 Jefferson Way Suite 2', 'Portland', 'OR', '97201', 'USA'),
(10806, 'VICTE', 3, TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-07-28 00:00:00.0', TIMESTAMP '2014-07-05 00:00:00.0', 2, 22.1100, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10807, 'FRANS', 4, TIMESTAMP '2014-06-30 00:00:00.0', TIMESTAMP '2014-07-28 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', 1, 1.3600, 'Franchi S.p.A.', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy'),
(10808, 'OLDWO', 2, TIMESTAMP '2014-07-01 00:00:00.0', TIMESTAMP '2014-07-29 00:00:00.0', TIMESTAMP '2014-07-09 00:00:00.0', 3, 45.5300, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10809, 'WELLI', 7, TIMESTAMP '2014-07-01 00:00:00.0', TIMESTAMP '2014-07-29 00:00:00.0', TIMESTAMP '2014-07-07 00:00:00.0', 1, 4.8700, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10810, 'LAUGB', 2, TIMESTAMP '2014-07-01 00:00:00.0', TIMESTAMP '2014-07-29 00:00:00.0', TIMESTAMP '2014-07-07 00:00:00.0', 3, 4.3300, 'Laughing Bacchus Wine Cellars', '2319 Elm St.', 'Vancouver', 'BC', 'V3F 2K1', 'Canada');        
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10811, 'LINOD', 8, TIMESTAMP '2014-07-02 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', TIMESTAMP '2014-07-08 00:00:00.0', 1, 31.2200, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10812, 'REGGC', 5, TIMESTAMP '2014-07-02 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', TIMESTAMP '2014-07-12 00:00:00.0', 1, 59.7800, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10813, 'RICAR', 1, TIMESTAMP '2014-07-05 00:00:00.0', TIMESTAMP '2014-08-02 00:00:00.0', TIMESTAMP '2014-07-09 00:00:00.0', 1, 47.3800, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10814, 'VICTE', 3, TIMESTAMP '2014-07-05 00:00:00.0', TIMESTAMP '2014-08-02 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', 3, 130.9400, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10815, 'SAVEA', 2, TIMESTAMP '2014-07-05 00:00:00.0', TIMESTAMP '2014-08-02 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', 3, 14.6200, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10816, 'GREAL', 4, TIMESTAMP '2014-07-06 00:00:00.0', TIMESTAMP '2014-08-03 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', 2, 719.7800, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10817, 'KOENE', 3, TIMESTAMP '2014-07-06 00:00:00.0', TIMESTAMP '2014-07-20 00:00:00.0', TIMESTAMP '2014-07-13 00:00:00.0', 2, 306.0700, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10818, 'MAGAA', 7, TIMESTAMP '2014-07-07 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-07-12 00:00:00.0', 3, 65.4800, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10819, 'CACTU', 2, TIMESTAMP '2014-07-07 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-07-16 00:00:00.0', 3, 19.7600, 'Cactus Comidas para llevar', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10820, 'RATTC', 3, TIMESTAMP '2014-07-07 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-07-13 00:00:00.0', 2, 37.5200, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10821, 'SPLIR', 1, TIMESTAMP '2014-07-08 00:00:00.0', TIMESTAMP '2014-08-05 00:00:00.0', TIMESTAMP '2014-07-15 00:00:00.0', 1, 36.6800, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10822, 'TRAIH', 6, TIMESTAMP '2014-07-08 00:00:00.0', TIMESTAMP '2014-08-05 00:00:00.0', TIMESTAMP '2014-07-16 00:00:00.0', 3, 7.0000, 'Trail-s Head Gourmet Provisioners', '722 DaVinci Blvd.', 'Kirkland', 'WA', '98034', 'USA'),
(10823, 'LILAS', 5, TIMESTAMP '2014-07-09 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-07-13 00:00:00.0', 2, 163.9700, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10824, 'FOLKO', 8, TIMESTAMP '2014-07-09 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', 1, 1.2300, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10825, 'DRACD', 1, TIMESTAMP '2014-07-09 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-07-14 00:00:00.0', 1, 79.2500, 'Drachenblut Delikatessen', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany'),
(10826, 'BLONP', 6, TIMESTAMP '2014-07-12 00:00:00.0', TIMESTAMP '2014-08-09 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', 1, 7.0900, 'Blondel pre et fils', '24, place Klber', 'Strasbourg', NULL, '67000', 'France'),
(10827, 'BONAP', 1, TIMESTAMP '2014-07-12 00:00:00.0', TIMESTAMP '2014-07-26 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', 2, 63.5400, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10828, 'RANCH', 9, TIMESTAMP '2014-07-13 00:00:00.0', TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', 1, 90.8500, 'Rancho grande', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina');         
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10829, 'ISLAT', 9, TIMESTAMP '2014-07-13 00:00:00.0', TIMESTAMP '2014-08-10 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', 1, 154.7200, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10830, 'TRADH', 4, TIMESTAMP '2014-07-13 00:00:00.0', TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', 2, 81.8300, 'Tradiao Hipermercados', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil'),
(10831, 'SANTG', 3, TIMESTAMP '2014-07-14 00:00:00.0', TIMESTAMP '2014-08-11 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', 2, 72.1900, 'Sant Gourmet', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway'),
(10832, 'LAMAI', 2, TIMESTAMP '2014-07-14 00:00:00.0', TIMESTAMP '2014-08-11 00:00:00.0', TIMESTAMP '2014-07-19 00:00:00.0', 2, 43.2600, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10833, 'OTTIK', 6, TIMESTAMP '2014-07-15 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', 2, 71.4900, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(10834, 'TRADH', 1, TIMESTAMP '2014-07-15 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', TIMESTAMP '2014-07-19 00:00:00.0', 3, 29.7800, 'Tradiao Hipermercados', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil'),
(10835, 'ALFKI', 1, TIMESTAMP '2014-07-15 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', 3, 69.5300, 'Alfred-s Futterkiste', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany'),
(10836, 'ERNSH', 7, TIMESTAMP '2014-07-16 00:00:00.0', TIMESTAMP '2014-08-13 00:00:00.0', TIMESTAMP '2014-07-21 00:00:00.0', 1, 411.8800, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10837, 'BERGS', 9, TIMESTAMP '2014-07-16 00:00:00.0', TIMESTAMP '2014-08-13 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', 3, 13.3200, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10838, 'LINOD', 3, TIMESTAMP '2014-07-19 00:00:00.0', TIMESTAMP '2014-08-16 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', 3, 59.2800, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10839, 'TRADH', 3, TIMESTAMP '2014-07-19 00:00:00.0', TIMESTAMP '2014-08-16 00:00:00.0', TIMESTAMP '2014-07-22 00:00:00.0', 3, 35.4300, 'Tradiao Hipermercados', 'Av. Ins de Castro, 414', 'Sao Paulo', 'SP', '05634-030', 'Brazil'),
(10840, 'LINOD', 4, TIMESTAMP '2014-07-19 00:00:00.0', TIMESTAMP '2014-09-02 00:00:00.0', TIMESTAMP '2014-08-16 00:00:00.0', 2, 2.7100, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10841, 'SUPRD', 5, TIMESTAMP '2014-07-20 00:00:00.0', TIMESTAMP '2014-08-17 00:00:00.0', TIMESTAMP '2014-07-29 00:00:00.0', 2, 424.3000, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10842, 'TORTU', 1, TIMESTAMP '2014-07-20 00:00:00.0', TIMESTAMP '2014-08-17 00:00:00.0', TIMESTAMP '2014-07-29 00:00:00.0', 3, 54.4200, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10843, 'VICTE', 4, TIMESTAMP '2014-07-21 00:00:00.0', TIMESTAMP '2014-08-18 00:00:00.0', TIMESTAMP '2014-07-26 00:00:00.0', 2, 9.2600, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10844, 'PICCO', 8, TIMESTAMP '2014-07-21 00:00:00.0', TIMESTAMP '2014-08-18 00:00:00.0', TIMESTAMP '2014-07-26 00:00:00.0', 2, 25.2200, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(10845, 'QUICK', 8, TIMESTAMP '2014-07-21 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', 1, 212.9800, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10846, 'SUPRD', 2, TIMESTAMP '2014-07-22 00:00:00.0', TIMESTAMP '2014-09-05 00:00:00.0', TIMESTAMP '2014-07-23 00:00:00.0', 3, 56.4600, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium');        
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10847, 'SAVEA', 4, TIMESTAMP '2014-07-22 00:00:00.0', TIMESTAMP '2014-08-05 00:00:00.0', TIMESTAMP '2014-08-10 00:00:00.0', 3, 487.5700, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10848, 'CONSH', 7, TIMESTAMP '2014-07-23 00:00:00.0', TIMESTAMP '2014-08-20 00:00:00.0', TIMESTAMP '2014-07-29 00:00:00.0', 2, 38.2400, 'Consolidated Holdings', 'Berkeley Gardens 12 Brewery', 'London', NULL, 'WX1 6LT', 'UK'),
(10849, 'KOENE', 9, TIMESTAMP '2014-07-23 00:00:00.0', TIMESTAMP '2014-08-20 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', 2, 0.5600, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10850, 'VICTE', 1, TIMESTAMP '2014-07-23 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', 1, 49.1900, 'Victuailles en stock', '2, rue du Commerce', 'Lyon', NULL, '69004', 'France'),
(10851, 'RICAR', 5, TIMESTAMP '2014-07-26 00:00:00.0', TIMESTAMP '2014-08-23 00:00:00.0', TIMESTAMP '2014-08-02 00:00:00.0', 1, 160.5500, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10852, 'RATTC', 8, TIMESTAMP '2014-07-26 00:00:00.0', TIMESTAMP '2014-08-09 00:00:00.0', TIMESTAMP '2014-07-30 00:00:00.0', 1, 174.0500, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10853, 'BLAUS', 9, TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-08-03 00:00:00.0', 2, 53.8300, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(10854, 'ERNSH', 3, TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-08-05 00:00:00.0', 2, 100.2200, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10855, 'OLDWO', 3, TIMESTAMP '2014-07-27 00:00:00.0', TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', 1, 170.9700, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10856, 'ANTON', 3, TIMESTAMP '2014-07-28 00:00:00.0', TIMESTAMP '2014-08-25 00:00:00.0', TIMESTAMP '2014-08-10 00:00:00.0', 2, 58.4300, 'Antonio Moreno Taquera', 'Mataderos 2312', 'Mxico D.F.', NULL, '5023', 'Mexico'),
(10857, 'BERGS', 8, TIMESTAMP '2014-07-28 00:00:00.0', TIMESTAMP '2014-08-25 00:00:00.0', TIMESTAMP '2014-08-06 00:00:00.0', 2, 188.8500, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10858, 'LACOR', 2, TIMESTAMP '2014-07-29 00:00:00.0', TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-08-03 00:00:00.0', 1, 52.5100, 'La corne d-abondance', '67, avenue de l-Europe', 'Versailles', NULL, '78000', 'France'),
(10859, 'FRANK', 1, TIMESTAMP '2014-07-29 00:00:00.0', TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-08-02 00:00:00.0', 2, 76.1000, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10860, 'FRANR', 3, TIMESTAMP '2014-07-29 00:00:00.0', TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-08-04 00:00:00.0', 3, 19.2600, 'France restauration', '54, rue Royale', 'Nantes', NULL, '44000', 'France'),
(10861, 'WHITC', 4, TIMESTAMP '2014-07-30 00:00:00.0', TIMESTAMP '2014-08-27 00:00:00.0', TIMESTAMP '2014-08-17 00:00:00.0', 2, 14.9300, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10862, 'LEHMS', 8, TIMESTAMP '2014-07-30 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', TIMESTAMP '2014-08-02 00:00:00.0', 2, 53.2300, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10863, 'HILAA', 4, TIMESTAMP '2014-08-02 00:00:00.0', TIMESTAMP '2014-09-02 00:00:00.0', TIMESTAMP '2014-08-17 00:00:00.0', 2, 30.2600, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10864, 'AROUT', 4, TIMESTAMP '2014-08-02 00:00:00.0', TIMESTAMP '2014-09-02 00:00:00.0', TIMESTAMP '2014-08-09 00:00:00.0', 2, 3.0400, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10865, 'QUICK', 2, TIMESTAMP '2014-08-02 00:00:00.0', TIMESTAMP '2014-08-16 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', 1, 348.1400, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10866, 'BERGS', 5, TIMESTAMP '2014-08-03 00:00:00.0', TIMESTAMP '2014-09-03 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', 1, 109.1100, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10867, 'LONEP', 6, TIMESTAMP '2014-08-03 00:00:00.0', TIMESTAMP '2014-09-17 00:00:00.0', TIMESTAMP '2014-08-11 00:00:00.0', 1, 1.9300, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(10868, 'QUEEN', 7, TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', TIMESTAMP '2014-08-23 00:00:00.0', 2, 191.2700, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10869, 'SEVES', 5, TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', TIMESTAMP '2014-08-09 00:00:00.0', 1, 143.2800, 'Seven Seas Imports', '90 Wadhurst Rd.', 'London', NULL, 'OX15 4NB', 'UK'),
(10870, 'WOLZA', 5, TIMESTAMP '2014-08-04 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', TIMESTAMP '2014-08-13 00:00:00.0', 3, 12.0400, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland'),
(10871, 'BONAP', 9, TIMESTAMP '2014-08-05 00:00:00.0', TIMESTAMP '2014-09-05 00:00:00.0', TIMESTAMP '2014-08-10 00:00:00.0', 2, 112.2700, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10872, 'GODOS', 5, TIMESTAMP '2014-08-05 00:00:00.0', TIMESTAMP '2014-09-05 00:00:00.0', TIMESTAMP '2014-08-09 00:00:00.0', 2, 175.3200, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10873, 'WILMK', 4, TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-08-09 00:00:00.0', 1, 0.8200, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(10874, 'GODOS', 5, TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-08-11 00:00:00.0', 2, 19.5800, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10875, 'BERGS', 4, TIMESTAMP '2014-08-06 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-09-03 00:00:00.0', 2, 32.3700, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10876, 'BONAP', 7, TIMESTAMP '2014-08-09 00:00:00.0', TIMESTAMP '2014-09-09 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', 3, 60.4200, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10877, 'RICAR', 1, TIMESTAMP '2014-08-09 00:00:00.0', TIMESTAMP '2014-09-09 00:00:00.0', TIMESTAMP '2014-08-19 00:00:00.0', 1, 38.0600, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(10878, 'QUICK', 4, TIMESTAMP '2014-08-10 00:00:00.0', TIMESTAMP '2014-09-10 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', 1, 46.6900, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10879, 'WILMK', 3, TIMESTAMP '2014-08-10 00:00:00.0', TIMESTAMP '2014-09-10 00:00:00.0', TIMESTAMP '2014-08-12 00:00:00.0', 3, 8.5000, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(10880, 'FOLKO', 7, TIMESTAMP '2014-08-10 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-08-18 00:00:00.0', 1, 88.0100, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10881, 'CACTU', 4, TIMESTAMP '2014-08-11 00:00:00.0', TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-08-18 00:00:00.0', 1, 2.8400, 'Cactus Comidas para llevar', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10882, 'SAVEA', 4, TIMESTAMP '2014-08-11 00:00:00.0', TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-08-20 00:00:00.0', 3, 23.1000, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10883, 'LONEP', 8, TIMESTAMP '2014-08-12 00:00:00.0', TIMESTAMP '2014-09-12 00:00:00.0', TIMESTAMP '2014-08-20 00:00:00.0', 3, 0.5300, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA');           
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10884, 'LETSS', 4, TIMESTAMP '2014-08-12 00:00:00.0', TIMESTAMP '2014-09-12 00:00:00.0', TIMESTAMP '2014-08-13 00:00:00.0', 2, 90.9700, 'Let-s Stop N Shop', '87 Polk St. Suite 5', 'San Francisco', 'CA', '94117', 'USA'),
(10885, 'SUPRD', 6, TIMESTAMP '2014-08-12 00:00:00.0', TIMESTAMP '2014-09-12 00:00:00.0', TIMESTAMP '2014-08-18 00:00:00.0', 3, 5.6400, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10886, 'HANAR', 1, TIMESTAMP '2014-08-13 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', TIMESTAMP '2014-09-02 00:00:00.0', 1, 4.9900, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10887, 'GALED', 8, TIMESTAMP '2014-08-13 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', TIMESTAMP '2014-08-16 00:00:00.0', 3, 1.2500, 'Galera del gastronmo', 'Rambla de Catalua, 23', 'Barcelona', NULL, '8022', 'Spain'),
(10888, 'GODOS', 1, TIMESTAMP '2014-08-16 00:00:00.0', TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-08-23 00:00:00.0', 2, 51.8700, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10889, 'RATTC', 9, TIMESTAMP '2014-08-16 00:00:00.0', TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-08-23 00:00:00.0', 3, 280.6100, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10890, 'DUMON', 7, TIMESTAMP '2014-08-16 00:00:00.0', TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-08-18 00:00:00.0', 1, 32.7600, 'Du monde entier', '67, rue des Cinquante Otages', 'Nantes', NULL, '44000', 'France'),
(10891, 'LEHMS', 7, TIMESTAMP '2014-08-17 00:00:00.0', TIMESTAMP '2014-09-17 00:00:00.0', TIMESTAMP '2014-08-19 00:00:00.0', 2, 20.3700, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10892, 'MAISD', 4, TIMESTAMP '2014-08-17 00:00:00.0', TIMESTAMP '2014-09-17 00:00:00.0', TIMESTAMP '2014-08-19 00:00:00.0', 2, 120.2700, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(10893, 'KOENE', 9, TIMESTAMP '2014-08-18 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', TIMESTAMP '2014-08-20 00:00:00.0', 2, 77.7800, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(10894, 'SAVEA', 1, TIMESTAMP '2014-08-18 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', TIMESTAMP '2014-08-20 00:00:00.0', 1, 116.1300, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10895, 'ERNSH', 3, TIMESTAMP '2014-08-18 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', TIMESTAMP '2014-08-23 00:00:00.0', 1, 162.7500, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10896, 'MAISD', 7, TIMESTAMP '2014-08-19 00:00:00.0', TIMESTAMP '2014-09-19 00:00:00.0', TIMESTAMP '2014-08-27 00:00:00.0', 3, 32.4500, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(10897, 'HUNGO', 3, TIMESTAMP '2014-08-19 00:00:00.0', TIMESTAMP '2014-09-19 00:00:00.0', TIMESTAMP '2014-08-25 00:00:00.0', 2, 603.5400, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10898, 'OCEAN', 4, TIMESTAMP '2014-08-20 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', 2, 1.2700, 'Ocano Atlntico Ltda.', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10899, 'LILAS', 5, TIMESTAMP '2014-08-20 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-08-26 00:00:00.0', 3, 1.2100, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10900, 'WELLI', 1, TIMESTAMP '2014-08-20 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', 2, 1.6600, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10901, 'HILAA', 4, TIMESTAMP '2014-08-23 00:00:00.0', TIMESTAMP '2014-09-23 00:00:00.0', TIMESTAMP '2014-08-26 00:00:00.0', 1, 62.0900, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela');              
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10902, 'FOLKO', 1, TIMESTAMP '2014-08-23 00:00:00.0', TIMESTAMP '2014-09-23 00:00:00.0', TIMESTAMP '2014-09-03 00:00:00.0', 1, 44.1500, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10903, 'HANAR', 3, TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', 3, 36.7100, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10904, 'WHITC', 3, TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-08-27 00:00:00.0', 3, 162.9500, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(10905, 'WELLI', 9, TIMESTAMP '2014-08-24 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', 2, 13.7200, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10906, 'WOLZA', 4, TIMESTAMP '2014-08-25 00:00:00.0', TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-09-03 00:00:00.0', 3, 26.2900, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland'),
(10907, 'SPECD', 6, TIMESTAMP '2014-08-25 00:00:00.0', TIMESTAMP '2014-09-25 00:00:00.0', TIMESTAMP '2014-08-27 00:00:00.0', 3, 9.1900, 'Spcialits du monde', '25, rue Lauriston', 'Paris', NULL, '75016', 'France'),
(10908, 'REGGC', 4, TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-06 00:00:00.0', 2, 32.9600, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10909, 'SANTG', 1, TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-10 00:00:00.0', 2, 53.0500, 'Sant Gourmet', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway'),
(10910, 'WILMK', 1, TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', 3, 38.1100, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(10911, 'GODOS', 3, TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-05 00:00:00.0', 1, 38.1900, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10912, 'HUNGO', 2, TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 2, 580.9100, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10913, 'QUEEN', 4, TIMESTAMP '2014-08-26 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', 1, 33.0500, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10914, 'QUEEN', 6, TIMESTAMP '2014-08-27 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-09-02 00:00:00.0', 1, 21.1900, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10915, 'TORTU', 2, TIMESTAMP '2014-08-27 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-09-02 00:00:00.0', 2, 3.5100, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10916, 'RANCH', 1, TIMESTAMP '2014-08-27 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-09-09 00:00:00.0', 2, 63.7700, 'Rancho grande', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10917, 'ROMEY', 4, TIMESTAMP '2014-09-02 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-09-11 00:00:00.0', 2, 8.2900, 'Romero y tomillo', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain'),
(10918, 'BOTTM', 3, TIMESTAMP '2014-09-02 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-09-11 00:00:00.0', 3, 48.8300, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10919, 'LINOD', 2, TIMESTAMP '2014-09-02 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-09-04 00:00:00.0', 2, 19.8000, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela');      
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10920, 'AROUT', 4, TIMESTAMP '2014-09-03 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-09-09 00:00:00.0', 2, 29.6100, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10921, 'VAFFE', 1, TIMESTAMP '2014-09-03 00:00:00.0', TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-09-09 00:00:00.0', 1, 176.4800, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10922, 'HANAR', 5, TIMESTAMP '2014-09-03 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-09-05 00:00:00.0', 3, 62.7400, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10923, 'LAMAI', 7, TIMESTAMP '2014-09-03 00:00:00.0', TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', 3, 68.2600, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(10924, 'BERGS', 3, TIMESTAMP '2014-09-04 00:00:00.0', TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', 2, 151.5200, 'Berglunds snabbkp', 'Berguvsvgen 8', 'Lule', NULL, 'S-958 22', 'Sweden'),
(10925, 'HANAR', 3, TIMESTAMP '2014-09-04 00:00:00.0', TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', 1, 2.2700, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10926, 'ANATR', 4, TIMESTAMP '2014-09-04 00:00:00.0', TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-09-11 00:00:00.0', 3, 39.9200, 'Ana Trujillo Emparedados y helados', 'Avda. de la Constitucin 2222', 'Mxico D.F.', NULL, '5021', 'Mexico'),
(10927, 'LACOR', 4, TIMESTAMP '2014-09-05 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', 1, 19.7900, 'La corne d-abondance', '67, avenue de l-Europe', 'Versailles', NULL, '78000', 'France'),
(10928, 'GALED', 1, TIMESTAMP '2014-09-05 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 1, 1.3600, 'Galera del gastronmo', 'Rambla de Catalua, 23', 'Barcelona', NULL, '8022', 'Spain'),
(10929, 'FRANK', 6, TIMESTAMP '2014-09-05 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-09-12 00:00:00.0', 1, 33.9300, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(10930, 'SUPRD', 4, TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 3, 15.5500, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(10931, 'RICSU', 4, TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-09-19 00:00:00.0', 2, 13.6000, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10932, 'BONAP', 8, TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-10-03 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', 1, 134.6400, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10933, 'ISLAT', 6, TIMESTAMP '2014-09-06 00:00:00.0', TIMESTAMP '2014-10-03 00:00:00.0', TIMESTAMP '2014-09-16 00:00:00.0', 3, 54.1500, 'Island Trading', 'Garden House Crowther Way', 'Cowes', 'Isle of Wight', 'PO31 7PJ', 'UK'),
(10934, 'LEHMS', 3, TIMESTAMP '2014-09-09 00:00:00.0', TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-09-12 00:00:00.0', 3, 32.0100, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(10935, 'WELLI', 4, TIMESTAMP '2014-09-09 00:00:00.0', TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 3, 47.5900, 'Wellington Importadora', 'Rua do Mercado, 12', 'Resende', 'SP', '08737-363', 'Brazil'),
(10936, 'GREAL', 3, TIMESTAMP '2014-09-09 00:00:00.0', TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 2, 33.6800, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(10937, 'CACTU', 7, TIMESTAMP '2014-09-10 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', 3, 31.5100, 'Cactus Comidas para llevar', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina');          
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10938, 'QUICK', 3, TIMESTAMP '2014-09-10 00:00:00.0', TIMESTAMP '2014-10-07 00:00:00.0', TIMESTAMP '2014-09-16 00:00:00.0', 2, 31.8900, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10939, 'MAGAA', 2, TIMESTAMP '2014-09-10 00:00:00.0', TIMESTAMP '2014-10-07 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', 2, 76.3300, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10940, 'BONAP', 8, TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-09-23 00:00:00.0', 3, 19.7700, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(10941, 'SAVEA', 7, TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', 2, 400.8100, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10942, 'REGGC', 9, TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 3, 17.9500, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(10943, 'BSBEV', 4, TIMESTAMP '2014-09-11 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-09-19 00:00:00.0', 2, 2.1700, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10944, 'BOTTM', 6, TIMESTAMP '2014-09-12 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-09-13 00:00:00.0', 3, 52.9200, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10945, 'MORGK', 4, TIMESTAMP '2014-09-12 00:00:00.0', TIMESTAMP '2014-10-09 00:00:00.0', TIMESTAMP '2014-09-18 00:00:00.0', 1, 10.2200, 'Morgenstern Gesundkost', 'Heerstr. 22', 'Leipzig', NULL, '4179', 'Germany'),
(10946, 'VAFFE', 1, TIMESTAMP '2014-09-12 00:00:00.0', TIMESTAMP '2014-10-09 00:00:00.0', TIMESTAMP '2014-09-19 00:00:00.0', 2, 27.2000, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10947, 'BSBEV', 3, TIMESTAMP '2014-09-13 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', TIMESTAMP '2014-09-16 00:00:00.0', 2, 3.2600, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(10948, 'GODOS', 3, TIMESTAMP '2014-09-13 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', TIMESTAMP '2014-09-19 00:00:00.0', 3, 23.3900, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(10949, 'BOTTM', 2, TIMESTAMP '2014-09-13 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', TIMESTAMP '2014-09-17 00:00:00.0', 3, 74.4400, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10950, 'MAGAA', 1, TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-10-13 00:00:00.0', TIMESTAMP '2014-09-23 00:00:00.0', 2, 2.5000, 'Magazzini Alimentari Riuniti', 'Via Ludovico il Moro 22', 'Bergamo', NULL, '24100', 'Italy'),
(10951, 'RICSU', 9, TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-10-07 00:00:00.0', 2, 30.8500, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(10952, 'ALFKI', 1, TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', 1, 40.4200, 'Alfred-s Futterkiste', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany'),
(10953, 'AROUT', 9, TIMESTAMP '2014-09-16 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-09-25 00:00:00.0', 2, 23.7200, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(10954, 'LINOD', 5, TIMESTAMP '2014-09-17 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', 1, 27.9100, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(10955, 'FOLKO', 8, TIMESTAMP '2014-09-17 00:00:00.0', TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', 2, 3.2600, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10956, 'BLAUS', 6, TIMESTAMP '2014-09-17 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-09-20 00:00:00.0', 2, 44.6500, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(10957, 'HILAA', 8, TIMESTAMP '2014-09-18 00:00:00.0', TIMESTAMP '2014-10-15 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', 3, 105.3600, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10958, 'OCEAN', 7, TIMESTAMP '2014-09-18 00:00:00.0', TIMESTAMP '2014-10-15 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', 2, 49.5600, 'Ocano Atlntico Ltda.', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10959, 'GOURL', 6, TIMESTAMP '2014-09-18 00:00:00.0', TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-09-23 00:00:00.0', 2, 4.9800, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(10960, 'HILAA', 3, TIMESTAMP '2014-09-19 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', 1, 2.0800, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10961, 'QUEEN', 8, TIMESTAMP '2014-09-19 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', 1, 104.4700, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(10962, 'QUICK', 8, TIMESTAMP '2014-09-19 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-09-23 00:00:00.0', 2, 275.7900, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10963, 'FURIB', 9, TIMESTAMP '2014-09-19 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', 3, 2.7000, 'Furia Bacalhau e Frutos do Mar', 'Jardim das rosas n. 32', 'Lisboa', NULL, '1675', 'Portugal'),
(10964, 'SPECD', 3, TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-09-24 00:00:00.0', 2, 87.3800, 'Spcialits du monde', '25, rue Lauriston', 'Paris', NULL, '75016', 'France'),
(10965, 'OLDWO', 6, TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', 3, 144.3800, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(10966, 'CHOPS', 4, TIMESTAMP '2014-09-20 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', 1, 27.1900, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(10967, 'TOMSP', 2, TIMESTAMP '2014-09-23 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', 2, 62.2200, 'Toms Spezialitten', 'Luisenstr. 48', 'Mnster', NULL, '44087', 'Germany'),
(10968, 'ERNSH', 1, TIMESTAMP '2014-09-23 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', TIMESTAMP '2014-10-01 00:00:00.0', 3, 74.6000, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10969, 'COMMI', 1, TIMESTAMP '2014-09-23 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', 2, 0.2100, 'Comrcio Mineiro', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil'),
(10970, 'BOLID', 9, TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-10-07 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', 1, 16.1600, 'Blido Comidas preparadas', 'C/ Araquil, 67', 'Madrid', NULL, '28023', 'Spain'),
(10971, 'FRANR', 2, TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-10-21 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', 2, 121.8200, 'France restauration', '54, rue Royale', 'Nantes', NULL, '44000', 'France'),
(10972, 'LACOR', 4, TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-10-21 00:00:00.0', TIMESTAMP '2014-09-26 00:00:00.0', 2, 0.0200, 'La corne d-abondance', '67, avenue de l-Europe', 'Versailles', NULL, '78000', 'France'),
(10973, 'LACOR', 6, TIMESTAMP '2014-09-24 00:00:00.0', TIMESTAMP '2014-10-21 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', 2, 15.1700, 'La corne d-abondance', '67, avenue de l-Europe', 'Versailles', NULL, '78000', 'France');
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10974, 'SPLIR', 3, TIMESTAMP '2014-09-25 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-10-03 00:00:00.0', 3, 12.9600, 'Split Rail Beer & Ale', 'P.O. Box 555', 'Lander', 'WY', '82520', 'USA'),
(10975, 'BOTTM', 1, TIMESTAMP '2014-09-25 00:00:00.0', TIMESTAMP '2014-10-22 00:00:00.0', TIMESTAMP '2014-09-27 00:00:00.0', 3, 32.2700, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10976, 'HILAA', 1, TIMESTAMP '2014-09-25 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-10-03 00:00:00.0', 1, 37.9700, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(10977, 'FOLKO', 8, TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 3, 208.5000, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10978, 'MAISD', 9, TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', 2, 32.8200, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(10979, 'ERNSH', 8, TIMESTAMP '2014-09-26 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-09-30 00:00:00.0', 2, 353.0700, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10980, 'FOLKO', 4, TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-11-08 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', 1, 1.2600, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10981, 'HANAR', 1, TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', 2, 193.3700, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(10982, 'BOTTM', 2, TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', 1, 14.0100, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(10983, 'SAVEA', 2, TIMESTAMP '2014-09-27 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-10-06 00:00:00.0', 2, 657.5400, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10984, 'SAVEA', 1, TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-10-03 00:00:00.0', 3, 211.2200, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(10985, 'HUNGO', 2, TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', 1, 91.5100, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(10986, 'OCEAN', 8, TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-10-21 00:00:00.0', 2, 217.8600, 'Ocano Atlntico Ltda.', 'Ing. Gustavo Moncada 8585 Piso 20-A', 'Buenos Aires', NULL, '1010', 'Argentina'),
(10987, 'EASTC', 8, TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-10-06 00:00:00.0', 1, 185.4800, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(10988, 'RATTC', 3, TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 2, 61.1400, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(10989, 'QUEDE', 2, TIMESTAMP '2014-09-30 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-10-02 00:00:00.0', 1, 34.7600, 'Que Delcia', 'Rua da Panificadora, 12', 'Rio de Janeiro', 'RJ', '02389-673', 'Brazil'),
(10990, 'ERNSH', 2, TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-11-13 00:00:00.0', TIMESTAMP '2014-10-07 00:00:00.0', 3, 117.6100, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(10991, 'QUICK', 1, TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-10-07 00:00:00.0', 1, 38.5100, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany');      
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(10992, 'THEBI', 1, TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-10-03 00:00:00.0', 3, 4.2700, 'The Big Cheese', '89 Jefferson Way Suite 2', 'Portland', 'OR', '97201', 'USA'),
(10993, 'FOLKO', 7, TIMESTAMP '2014-10-01 00:00:00.0', TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 3, 8.8100, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(10994, 'VAFFE', 2, TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-10-09 00:00:00.0', 3, 65.5300, 'Vaffeljernet', 'Smagsloget 45', 'rhus', NULL, '8200', 'Denmark'),
(10995, 'PERIC', 1, TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-10-30 00:00:00.0', TIMESTAMP '2014-10-06 00:00:00.0', 3, 46.0000, 'Pericles Comidas clsicas', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(10996, 'QUICK', 4, TIMESTAMP '2014-10-02 00:00:00.0', TIMESTAMP '2014-10-30 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 2, 1.1200, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(10997, 'LILAS', 8, TIMESTAMP '2014-10-03 00:00:00.0', TIMESTAMP '2014-11-15 00:00:00.0', TIMESTAMP '2014-10-13 00:00:00.0', 2, 73.9100, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(10998, 'WOLZA', 8, TIMESTAMP '2014-10-03 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', 2, 20.3100, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland'),
(10999, 'OTTIK', 6, TIMESTAMP '2014-10-03 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 2, 96.3500, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(11000, 'RATTC', 2, TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-10-14 00:00:00.0', 3, 55.1200, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA'),
(11001, 'FOLKO', 2, TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-10-14 00:00:00.0', 2, 197.3000, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(11002, 'SAVEA', 4, TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', 1, 141.1600, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(11003, 'THECR', 3, TIMESTAMP '2014-10-06 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-10-08 00:00:00.0', 3, 14.9100, 'The Cracker Box', '55 Grizzly Peak Rd.', 'Butte', 'MT', '59801', 'USA'),
(11004, 'MAISD', 3, TIMESTAMP '2014-10-07 00:00:00.0', TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', 1, 44.8400, 'Maison Dewey', 'Rue Joseph-Bens 532', 'Bruxelles', NULL, 'B-1180', 'Belgium'),
(11005, 'WILMK', 2, TIMESTAMP '2014-10-07 00:00:00.0', TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 1, 0.7500, 'Wilman Kala', 'Keskuskatu 45', 'Helsinki', NULL, '21240', 'Finland'),
(11006, 'GREAL', 3, TIMESTAMP '2014-10-07 00:00:00.0', TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-10-15 00:00:00.0', 2, 25.1900, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(11007, 'PRINI', 8, TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-10-13 00:00:00.0', 2, 202.2400, 'Princesa Isabel Vinhos', 'Estrada da sade n. 58', 'Lisboa', NULL, '1756', 'Portugal'),
(11008, 'ERNSH', 7, TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', NULL, 3, 79.4600, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(11009, 'GODOS', 2, TIMESTAMP '2014-10-08 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 1, 59.1100, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(11010, 'REGGC', 2, TIMESTAMP '2014-10-09 00:00:00.0', TIMESTAMP '2014-11-07 00:00:00.0', TIMESTAMP '2014-10-21 00:00:00.0', 2, 28.7100, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy');      
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(11011, 'ALFKI', 3, TIMESTAMP '2014-10-09 00:00:00.0', TIMESTAMP '2014-11-07 00:00:00.0', TIMESTAMP '2014-10-13 00:00:00.0', 1, 1.2100, 'Alfred-s Futterkiste', 'Obere Str. 57', 'Berlin', NULL, '12209', 'Germany'),
(11012, 'FRANK', 1, TIMESTAMP '2014-10-09 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-10-17 00:00:00.0', 3, 242.9500, 'Frankenversand', 'Berliner Platz 43', 'Mnchen', NULL, '80805', 'Germany'),
(11013, 'ROMEY', 2, TIMESTAMP '2014-10-09 00:00:00.0', TIMESTAMP '2014-11-07 00:00:00.0', TIMESTAMP '2014-10-10 00:00:00.0', 1, 32.9900, 'Romero y tomillo', 'Gran Va, 1', 'Madrid', NULL, '28001', 'Spain'),
(11014, 'LINOD', 2, TIMESTAMP '2014-10-10 00:00:00.0', TIMESTAMP '2014-11-08 00:00:00.0', TIMESTAMP '2014-10-15 00:00:00.0', 3, 23.6000, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(11015, 'SANTG', 2, TIMESTAMP '2014-10-10 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', 2, 4.6200, 'Sant Gourmet', 'Erling Skakkes gate 78', 'Stavern', NULL, '4110', 'Norway'),
(11016, 'AROUT', 9, TIMESTAMP '2014-10-10 00:00:00.0', TIMESTAMP '2014-11-08 00:00:00.0', TIMESTAMP '2014-10-13 00:00:00.0', 2, 33.8000, 'Around the Horn', 'Brook Farm Stratford St. Mary', 'Colchester', 'Essex', 'CO7 6JX', 'UK'),
(11017, 'ERNSH', 9, TIMESTAMP '2014-10-13 00:00:00.0', TIMESTAMP '2014-11-11 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', 2, 754.2600, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(11018, 'LONEP', 4, TIMESTAMP '2014-10-13 00:00:00.0', TIMESTAMP '2014-11-11 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', 2, 11.6500, 'Lonesome Pine Restaurant', '89 Chiaroscuro Rd.', 'Portland', 'OR', '97219', 'USA'),
(11019, 'RANCH', 6, TIMESTAMP '2014-10-13 00:00:00.0', TIMESTAMP '2014-11-11 00:00:00.0', NULL, 3, 3.1700, 'Rancho grande', 'Av. del Libertador 900', 'Buenos Aires', NULL, '1010', 'Argentina'),
(11020, 'OTTIK', 2, TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-11-12 00:00:00.0', TIMESTAMP '2014-10-16 00:00:00.0', 2, 43.3000, 'Ottilies Kseladen', 'Mehrheimerstr. 369', 'Kln', NULL, '50739', 'Germany'),
(11021, 'QUICK', 3, TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-11-12 00:00:00.0', TIMESTAMP '2014-10-21 00:00:00.0', 1, 297.1800, 'QUICK-Stop', 'Taucherstrae 10', 'Cunewalde', NULL, '1307', 'Germany'),
(11022, 'HANAR', 9, TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-11-12 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', 2, 6.2700, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(11023, 'BSBEV', 1, TIMESTAMP '2014-10-14 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', 2, 123.8300, 'B-s Beverages', 'Fauntleroy Circus', 'London', NULL, 'EC2 5NT', 'UK'),
(11024, 'EASTC', 4, TIMESTAMP '2014-10-15 00:00:00.0', TIMESTAMP '2014-11-13 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', 1, 74.3600, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(11025, 'WARTH', 6, TIMESTAMP '2014-10-15 00:00:00.0', TIMESTAMP '2014-11-13 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', 3, 29.1700, 'Wartian Herkku', 'Torikatu 38', 'Oulu', NULL, '90110', 'Finland'),
(11026, 'FRANS', 4, TIMESTAMP '2014-10-15 00:00:00.0', TIMESTAMP '2014-11-13 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', 1, 47.0900, 'Franchi S.p.A.', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy'),
(11027, 'BOTTM', 1, TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-11-14 00:00:00.0', TIMESTAMP '2014-10-20 00:00:00.0', 1, 52.5200, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(11028, 'KOENE', 2, TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-11-14 00:00:00.0', TIMESTAMP '2014-10-22 00:00:00.0', 1, 29.5900, 'Kniglich Essen', 'Maubelstr. 90', 'Brandenburg', NULL, '14776', 'Germany'),
(11029, 'CHOPS', 4, TIMESTAMP '2014-10-16 00:00:00.0', TIMESTAMP '2014-11-14 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', 1, 47.8400, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland');            
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(11030, 'SAVEA', 7, TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-11-15 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', 2, 830.7500, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(11031, 'SAVEA', 6, TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-11-15 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', 2, 227.2200, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(11032, 'WHITC', 2, TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-11-15 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', 3, 606.1900, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(11033, 'RICSU', 7, TIMESTAMP '2014-10-17 00:00:00.0', TIMESTAMP '2014-11-15 00:00:00.0', TIMESTAMP '2014-10-23 00:00:00.0', 3, 84.7400, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(11034, 'OLDWO', 8, TIMESTAMP '2014-10-20 00:00:00.0', TIMESTAMP '2014-12-01 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', 1, 40.3200, 'Old World Delicatessen', '2743 Bering St.', 'Anchorage', 'AK', '99508', 'USA'),
(11035, 'SUPRD', 2, TIMESTAMP '2014-10-20 00:00:00.0', TIMESTAMP '2014-11-18 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', 2, 0.1700, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(11036, 'DRACD', 8, TIMESTAMP '2014-10-20 00:00:00.0', TIMESTAMP '2014-11-18 00:00:00.0', TIMESTAMP '2014-10-22 00:00:00.0', 3, 149.4700, 'Drachenblut Delikatessen', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany'),
(11037, 'GODOS', 7, TIMESTAMP '2014-10-21 00:00:00.0', TIMESTAMP '2014-11-19 00:00:00.0', TIMESTAMP '2014-10-27 00:00:00.0', 1, 3.2000, 'Godos Cocina Tpica', 'C/ Romero, 33', 'Sevilla', NULL, '41101', 'Spain'),
(11038, 'SUPRD', 1, TIMESTAMP '2014-10-21 00:00:00.0', TIMESTAMP '2014-11-19 00:00:00.0', TIMESTAMP '2014-10-30 00:00:00.0', 2, 29.5900, 'Suprmes dlices', 'Boulevard Tirou, 255', 'Charleroi', NULL, 'B-6000', 'Belgium'),
(11039, 'LINOD', 1, TIMESTAMP '2014-10-21 00:00:00.0', TIMESTAMP '2014-11-19 00:00:00.0', NULL, 2, 65.0000, 'LINO-Delicateses', 'Ave. 5 de Mayo Porlamar', 'I. de Margarita', 'Nueva Esparta', '4980', 'Venezuela'),
(11040, 'GREAL', 4, TIMESTAMP '2014-10-22 00:00:00.0', TIMESTAMP '2014-11-20 00:00:00.0', NULL, 3, 18.8400, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(11041, 'CHOPS', 3, TIMESTAMP '2014-10-22 00:00:00.0', TIMESTAMP '2014-11-20 00:00:00.0', TIMESTAMP '2014-10-28 00:00:00.0', 2, 48.2200, 'Chop-suey Chinese', 'Hauptstr. 31', 'Bern', NULL, '3012', 'Switzerland'),
(11042, 'COMMI', 2, TIMESTAMP '2014-10-22 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', 1, 29.9900, 'Comrcio Mineiro', 'Av. dos Lusadas, 23', 'Sao Paulo', 'SP', '05432-043', 'Brazil'),
(11043, 'SPECD', 5, TIMESTAMP '2014-10-22 00:00:00.0', TIMESTAMP '2014-11-20 00:00:00.0', TIMESTAMP '2014-10-29 00:00:00.0', 2, 8.8000, 'Spcialits du monde', '25, rue Lauriston', 'Paris', NULL, '75016', 'France'),
(11044, 'WOLZA', 4, TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-11-21 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', 1, 8.7200, 'Wolski Zajazd', 'ul. Filtrowa 68', 'Warszawa', NULL, '01-012', 'Poland'),
(11045, 'BOTTM', 6, TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-11-21 00:00:00.0', NULL, 2, 70.5800, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'),
(11046, 'WANDK', 8, TIMESTAMP '2014-10-23 00:00:00.0', TIMESTAMP '2014-11-21 00:00:00.0', TIMESTAMP '2014-10-24 00:00:00.0', 2, 71.6400, 'Die Wandernde Kuh', 'Adenauerallee 900', 'Stuttgart', NULL, '70563', 'Germany'),
(11047, 'EASTC', 7, TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-11-22 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', 3, 46.6200, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(11048, 'BOTTM', 7, TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-11-22 00:00:00.0', TIMESTAMP '2014-10-30 00:00:00.0', 3, 24.1200, 'Bottom-Dollar Markets', '23 Tsawassen Blvd.', 'Tsawassen', 'BC', 'T2F 8M4', 'Canada'); 
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(11049, 'GOURL', 3, TIMESTAMP '2014-10-24 00:00:00.0', TIMESTAMP '2014-11-22 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', 1, 8.3400, 'Gourmet Lanchonetes', 'Av. Brasil, 442', 'Campinas', 'SP', '04876-786', 'Brazil'),
(11050, 'FOLKO', 8, TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-11-25 00:00:00.0', TIMESTAMP '2014-11-05 00:00:00.0', 2, 59.4100, 'Folk och f HB', 'kergatan 24', 'Brcke', NULL, 'S-844 67', 'Sweden'),
(11051, 'LAMAI', 7, TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-11-25 00:00:00.0', NULL, 3, 2.7900, 'La maison d-Asie', '1 rue Alsace-Lorraine', 'Toulouse', NULL, '31000', 'France'),
(11052, 'HANAR', 3, TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-11-25 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', 1, 67.2600, 'Hanari Carnes', 'Rua do Pao, 67', 'Rio de Janeiro', 'RJ', '05454-876', 'Brazil'),
(11053, 'PICCO', 2, TIMESTAMP '2014-10-27 00:00:00.0', TIMESTAMP '2014-11-25 00:00:00.0', TIMESTAMP '2014-10-29 00:00:00.0', 2, 53.0500, 'Piccolo und mehr', 'Geislweg 14', 'Salzburg', NULL, '5020', 'Austria'),
(11054, 'CACTU', 8, TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-11-26 00:00:00.0', NULL, 1, 0.3300, 'Cactus Comidas para llevar', 'Cerrito 333', 'Buenos Aires', NULL, '1010', 'Argentina'),
(11055, 'HILAA', 7, TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-11-26 00:00:00.0', TIMESTAMP '2014-11-05 00:00:00.0', 2, 120.9200, 'HILARION-Abastos', 'Carrera 22 con Ave. Carlos Soublette 8-35', 'San Cristbal', 'Tchira', '5022', 'Venezuela'),
(11056, 'EASTC', 8, TIMESTAMP '2014-10-28 00:00:00.0', TIMESTAMP '2014-11-12 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', 2, 278.9600, 'Eastern Connection', '35 King George', 'London', NULL, 'WX3 6FW', 'UK'),
(11057, 'NORTS', 3, TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-11-27 00:00:00.0', TIMESTAMP '2014-11-01 00:00:00.0', 3, 4.1300, 'North/South', 'South House 300 Queensbridge', 'London', NULL, 'SW7 1RZ', 'UK'),
(11058, 'BLAUS', 9, TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-11-27 00:00:00.0', NULL, 3, 31.1400, 'Blauer See Delikatessen', 'Forsterstr. 57', 'Mannheim', NULL, '68306', 'Germany'),
(11059, 'RICAR', 2, TIMESTAMP '2014-10-29 00:00:00.0', TIMESTAMP '2014-12-10 00:00:00.0', NULL, 2, 85.8000, 'Ricardo Adocicados', 'Av. Copacabana, 267', 'Rio de Janeiro', 'RJ', '02389-890', 'Brazil'),
(11060, 'FRANS', 2, TIMESTAMP '2014-10-30 00:00:00.0', TIMESTAMP '2014-11-28 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', 2, 10.9800, 'Franchi S.p.A.', 'Via Monte Bianco 34', 'Torino', NULL, '10100', 'Italy'),
(11061, 'GREAL', 4, TIMESTAMP '2014-10-30 00:00:00.0', TIMESTAMP '2014-12-11 00:00:00.0', NULL, 3, 14.0100, 'Great Lakes Food Market', '2732 Baker Blvd.', 'Eugene', 'OR', '97403', 'USA'),
(11062, 'REGGC', 4, TIMESTAMP '2014-10-30 00:00:00.0', TIMESTAMP '2014-11-28 00:00:00.0', NULL, 2, 29.9300, 'Reggiani Caseifici', 'Strada Provinciale 124', 'Reggio Emilia', NULL, '42100', 'Italy'),
(11063, 'HUNGO', 3, TIMESTAMP '2014-10-30 00:00:00.0', TIMESTAMP '2014-11-28 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', 2, 81.7300, 'Hungry Owl All-Night Grocers', '8 Johnstown Road', 'Cork', 'Co. Cork', NULL, 'Ireland'),
(11064, 'SAVEA', 1, TIMESTAMP '2014-11-01 00:00:00.0', TIMESTAMP '2014-11-29 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', 1, 30.0900, 'Save-a-lot Markets', '187 Suffolk Ln.', 'Boise', 'ID', '83720', 'USA'),
(11065, 'LILAS', 8, TIMESTAMP '2014-11-01 00:00:00.0', TIMESTAMP '2014-11-29 00:00:00.0', NULL, 1, 12.9100, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(11066, 'WHITC', 7, TIMESTAMP '2014-11-01 00:00:00.0', TIMESTAMP '2014-11-29 00:00:00.0', TIMESTAMP '2014-11-04 00:00:00.0', 2, 44.7200, 'White Clover Markets', '1029 - 12th Ave. S.', 'Seattle', 'WA', '98124', 'USA'),
(11067, 'DRACD', 1, TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-11-18 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', 2, 7.9800, 'Drachenblut Delikatessen', 'Walserweg 21', 'Aachen', NULL, '52066', 'Germany');           
INSERT INTO ORDERS(ORDERID, CUSTOMERID, EMPLOYEEID, ORDERDATE, REQUIREDDATE, SHIPPEDDATE, SHIPVIA, FREIGHT, SHIPNAME, SHIPADDRESS, SHIPCITY, SHIPREGION, SHIPPOSTALCODE, SHIPCOUNTRY) VALUES
(11068, 'QUEEN', 8, TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-12-01 00:00:00.0', NULL, 2, 81.7500, 'Queen Cozinha', 'Alameda dos Canrios, 891', 'Sao Paulo', 'SP', '05487-020', 'Brazil'),
(11069, 'TORTU', 1, TIMESTAMP '2014-11-04 00:00:00.0', TIMESTAMP '2014-12-01 00:00:00.0', TIMESTAMP '2014-11-06 00:00:00.0', 2, 15.6700, 'Tortuga Restaurante', 'Avda. Azteca 123', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(11070, 'LEHMS', 2, TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-12-02 00:00:00.0', NULL, 1, 136.0000, 'Lehmanns Marktstand', 'Magazinweg 7', 'Frankfurt a.M.', NULL, '60528', 'Germany'),
(11071, 'LILAS', 1, TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-12-02 00:00:00.0', NULL, 1, 0.9300, 'LILA-Supermercado', 'Carrera 52 con Ave. Bolvar 65-98 Llano Largo', 'Barquisimeto', 'Lara', '3508', 'Venezuela'),
(11072, 'ERNSH', 4, TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-12-02 00:00:00.0', NULL, 2, 258.6400, 'Ernst Handel', 'Kirchgasse 6', 'Graz', NULL, '8010', 'Austria'),
(11073, 'PERIC', 2, TIMESTAMP '2014-11-05 00:00:00.0', TIMESTAMP '2014-12-02 00:00:00.0', NULL, 2, 24.9500, 'Pericles Comidas clsicas', 'Calle Dr. Jorge Cash 321', 'Mxico D.F.', NULL, '5033', 'Mexico'),
(11074, 'SIMOB', 7, TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-12-03 00:00:00.0', NULL, 2, 18.4400, 'Simons bistro', 'Vinbltet 34', 'Kobenhavn', NULL, '1734', 'Denmark'),
(11075, 'RICSU', 8, TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-12-03 00:00:00.0', NULL, 2, 6.1900, 'Richter Supermarkt', 'Starenweg 5', 'Genve', NULL, '1204', 'Switzerland'),
(11076, 'BONAP', 4, TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-12-03 00:00:00.0', NULL, 2, 38.2800, 'Bon app-', '12, rue des Bouchers', 'Marseille', NULL, '13008', 'France'),
(11077, 'RATTC', 1, TIMESTAMP '2014-11-06 00:00:00.0', TIMESTAMP '2014-12-03 00:00:00.0', NULL, 2, 8.5300, 'Rattlesnake Canyon Grocery', '2817 Milton Dr.', 'Albuquerque', 'NM', '87110', 'USA');   


TRUNCATE TABLE `OrderDetails`;
INSERT INTO `OrderDetails` Values (10248, 11, 14, 12, 0.0);
INSERT INTO `OrderDetails` Values (10248, 42, 9.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10248, 72, 34.8, 5, 0.0);
INSERT INTO `OrderDetails` Values (10249, 14, 18.6, 9, 0.0);
INSERT INTO `OrderDetails` Values (10249, 51, 42.4, 40, 0.0);
INSERT INTO `OrderDetails` Values (10250, 41, 7.7, 10, 0.0);
INSERT INTO `OrderDetails` Values (10250, 51, 42.4, 35, 0.15);
INSERT INTO `OrderDetails` Values (10250, 65, 16.8, 15, 0.15);
INSERT INTO `OrderDetails` Values (10251, 22, 16.8, 6, 0.05);
INSERT INTO `OrderDetails` Values (10251, 57, 15.6, 15, 0.05);
INSERT INTO `OrderDetails` Values (10251, 65, 16.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10252, 20, 64.8, 40, 0.05);
INSERT INTO `OrderDetails` Values (10252, 33, 2, 25, 0.05);
INSERT INTO `OrderDetails` Values (10252, 60, 27.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10253, 31, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10253, 39, 14.4, 42, 0.0);
INSERT INTO `OrderDetails` Values (10253, 49, 16, 40, 0.0);
INSERT INTO `OrderDetails` Values (10254, 24, 3.6, 15, 0.15);
INSERT INTO `OrderDetails` Values (10254, 55, 19.2, 21, 0.15);
INSERT INTO `OrderDetails` Values (10254, 74, 8, 21, 0.0);
INSERT INTO `OrderDetails` Values (10255, 2, 15.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10255, 16, 13.9, 35, 0.0);
INSERT INTO `OrderDetails` Values (10255, 36, 15.2, 25, 0.0);
INSERT INTO `OrderDetails` Values (10255, 59, 44, 30, 0.0);
INSERT INTO `OrderDetails` Values (10256, 53, 26.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10256, 77, 10.4, 12, 0.0);
INSERT INTO `OrderDetails` Values (10257, 27, 35.1, 25, 0.0);
INSERT INTO `OrderDetails` Values (10257, 39, 14.4, 6, 0.0);
INSERT INTO `OrderDetails` Values (10257, 77, 10.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10258, 2, 15.2, 50, 0.2);
INSERT INTO `OrderDetails` Values (10258, 5, 17, 65, 0.2);
INSERT INTO `OrderDetails` Values (10258, 32, 25.6, 6, 0.2);
INSERT INTO `OrderDetails` Values (10259, 21, 8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10259, 37, 20.8, 1, 0.0);
INSERT INTO `OrderDetails` Values (10260, 41, 7.7, 16, 0.25);
INSERT INTO `OrderDetails` Values (10260, 57, 15.6, 50, 0.0);
INSERT INTO `OrderDetails` Values (10260, 62, 39.4, 15, 0.25);
INSERT INTO `OrderDetails` Values (10260, 70, 12, 21, 0.25);
INSERT INTO `OrderDetails` Values (10261, 21, 8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10261, 35, 14.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10262, 5, 17, 12, 0.2);
INSERT INTO `OrderDetails` Values (10262, 7, 24, 15, 0.0);
INSERT INTO `OrderDetails` Values (10262, 56, 30.4, 2, 0.0);
INSERT INTO `OrderDetails` Values (10263, 16, 13.9, 60, 0.25);
INSERT INTO `OrderDetails` Values (10263, 24, 3.6, 28, 0.0);
INSERT INTO `OrderDetails` Values (10263, 30, 20.7, 60, 0.25);
INSERT INTO `OrderDetails` Values (10263, 74, 8, 36, 0.25);
INSERT INTO `OrderDetails` Values (10264, 2, 15.2, 35, 0.0);
INSERT INTO `OrderDetails` Values (10264, 41, 7.7, 25, 0.15);
INSERT INTO `OrderDetails` Values (10265, 17, 31.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10265, 70, 12, 20, 0.0);
INSERT INTO `OrderDetails` Values (10266, 12, 30.4, 12, 0.05);
INSERT INTO `OrderDetails` Values (10267, 40, 14.7, 50, 0.0);
INSERT INTO `OrderDetails` Values (10267, 59, 44, 70, 0.15);
INSERT INTO `OrderDetails` Values (10267, 76, 14.4, 15, 0.15);
INSERT INTO `OrderDetails` Values (10268, 29, 99, 10, 0.0);
INSERT INTO `OrderDetails` Values (10268, 72, 27.8, 4, 0.0);
INSERT INTO `OrderDetails` Values (10269, 33, 2, 60, 0.05);
INSERT INTO `OrderDetails` Values (10269, 72, 27.8, 20, 0.05);
INSERT INTO `OrderDetails` Values (10270, 36, 15.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10270, 43, 36.8, 25, 0.0);
INSERT INTO `OrderDetails` Values (10271, 33, 2, 24, 0.0);
INSERT INTO `OrderDetails` Values (10272, 20, 64.8, 6, 0.0);
INSERT INTO `OrderDetails` Values (10272, 31, 10, 40, 0.0);
INSERT INTO `OrderDetails` Values (10272, 72, 27.8, 24, 0.0);
INSERT INTO `OrderDetails` Values (10273, 10, 24.8, 24, 0.05);
INSERT INTO `OrderDetails` Values (10273, 31, 10, 15, 0.05);
INSERT INTO `OrderDetails` Values (10273, 33, 2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10273, 40, 14.7, 60, 0.05);
INSERT INTO `OrderDetails` Values (10273, 76, 14.4, 33, 0.05);
INSERT INTO `OrderDetails` Values (10274, 71, 17.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10274, 72, 27.8, 7, 0.0);
INSERT INTO `OrderDetails` Values (10275, 24, 3.6, 12, 0.05);
INSERT INTO `OrderDetails` Values (10275, 59, 44, 6, 0.05);
INSERT INTO `OrderDetails` Values (10276, 10, 24.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10276, 13, 4.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10277, 28, 36.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10277, 62, 39.4, 12, 0.0);
INSERT INTO `OrderDetails` Values (10278, 44, 15.5, 16, 0.0);
INSERT INTO `OrderDetails` Values (10278, 59, 44, 15, 0.0);
INSERT INTO `OrderDetails` Values (10278, 63, 35.1, 8, 0.0);
INSERT INTO `OrderDetails` Values (10278, 73, 12, 25, 0.0);
INSERT INTO `OrderDetails` Values (10279, 17, 31.2, 15, 0.25);
INSERT INTO `OrderDetails` Values (10280, 24, 3.6, 12, 0.0);
INSERT INTO `OrderDetails` Values (10280, 55, 19.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10280, 75, 6.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10281, 19, 7.3, 1, 0.0);
INSERT INTO `OrderDetails` Values (10281, 24, 3.6, 6, 0.0);
INSERT INTO `OrderDetails` Values (10281, 35, 14.4, 4, 0.0);
INSERT INTO `OrderDetails` Values (10282, 30, 20.7, 6, 0.0);
INSERT INTO `OrderDetails` Values (10282, 57, 15.6, 2, 0.0);
INSERT INTO `OrderDetails` Values (10283, 15, 12.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10283, 19, 7.3, 18, 0.0);
INSERT INTO `OrderDetails` Values (10283, 60, 27.2, 35, 0.0);
INSERT INTO `OrderDetails` Values (10283, 72, 27.8, 3, 0.0);
INSERT INTO `OrderDetails` Values (10284, 27, 35.1, 15, 0.25);
INSERT INTO `OrderDetails` Values (10284, 44, 15.5, 21, 0.0);
INSERT INTO `OrderDetails` Values (10284, 60, 27.2, 20, 0.25);
INSERT INTO `OrderDetails` Values (10284, 67, 11.2, 5, 0.25);
INSERT INTO `OrderDetails` Values (10285, 1, 14.4, 45, 0.2);
INSERT INTO `OrderDetails` Values (10285, 40, 14.7, 40, 0.2);
INSERT INTO `OrderDetails` Values (10285, 53, 26.2, 36, 0.2);
INSERT INTO `OrderDetails` Values (10286, 35, 14.4, 100, 0.0);
INSERT INTO `OrderDetails` Values (10286, 62, 39.4, 40, 0.0);
INSERT INTO `OrderDetails` Values (10287, 16, 13.9, 40, 0.15);
INSERT INTO `OrderDetails` Values (10287, 34, 11.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10287, 46, 9.6, 15, 0.15);
INSERT INTO `OrderDetails` Values (10288, 54, 5.9, 10, 0.1);
INSERT INTO `OrderDetails` Values (10288, 68, 10, 3, 0.1);
INSERT INTO `OrderDetails` Values (10289, 3, 8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10289, 64, 26.6, 9, 0.0);
INSERT INTO `OrderDetails` Values (10290, 5, 17, 20, 0.0);
INSERT INTO `OrderDetails` Values (10290, 29, 99, 15, 0.0);
INSERT INTO `OrderDetails` Values (10290, 49, 16, 15, 0.0);
INSERT INTO `OrderDetails` Values (10290, 77, 10.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (10291, 13, 4.8, 20, 0.1);
INSERT INTO `OrderDetails` Values (10291, 44, 15.5, 24, 0.1);
INSERT INTO `OrderDetails` Values (10291, 51, 42.4, 2, 0.1);
INSERT INTO `OrderDetails` Values (10292, 20, 64.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10293, 18, 50, 12, 0.0);
INSERT INTO `OrderDetails` Values (10293, 24, 3.6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10293, 63, 35.1, 5, 0.0);
INSERT INTO `OrderDetails` Values (10293, 75, 6.2, 6, 0.0);
INSERT INTO `OrderDetails` Values (10294, 1, 14.4, 18, 0.0);
INSERT INTO `OrderDetails` Values (10294, 17, 31.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10294, 43, 36.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10294, 60, 27.2, 21, 0.0);
INSERT INTO `OrderDetails` Values (10294, 75, 6.2, 6, 0.0);
INSERT INTO `OrderDetails` Values (10295, 56, 30.4, 4, 0.0);
INSERT INTO `OrderDetails` Values (10296, 11, 16.8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10296, 16, 13.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (10296, 69, 28.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10297, 39, 14.4, 60, 0.0);
INSERT INTO `OrderDetails` Values (10297, 72, 27.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10298, 2, 15.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10298, 36, 15.2, 40, 0.25);
INSERT INTO `OrderDetails` Values (10298, 59, 44, 30, 0.25);
INSERT INTO `OrderDetails` Values (10298, 62, 39.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10299, 19, 7.3, 15, 0.0);
INSERT INTO `OrderDetails` Values (10299, 70, 12, 20, 0.0);
INSERT INTO `OrderDetails` Values (10300, 66, 13.6, 30, 0.0);
INSERT INTO `OrderDetails` Values (10300, 68, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10301, 40, 14.7, 10, 0.0);
INSERT INTO `OrderDetails` Values (10301, 56, 30.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10302, 17, 31.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10302, 28, 36.4, 28, 0.0);
INSERT INTO `OrderDetails` Values (10302, 43, 36.8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10303, 40, 14.7, 40, 0.1);
INSERT INTO `OrderDetails` Values (10303, 65, 16.8, 30, 0.1);
INSERT INTO `OrderDetails` Values (10303, 68, 10, 15, 0.1);
INSERT INTO `OrderDetails` Values (10304, 49, 16, 30, 0.0);
INSERT INTO `OrderDetails` Values (10304, 59, 44, 10, 0.0);
INSERT INTO `OrderDetails` Values (10304, 71, 17.2, 2, 0.0);
INSERT INTO `OrderDetails` Values (10305, 18, 50, 25, 0.1);
INSERT INTO `OrderDetails` Values (10305, 29, 99, 25, 0.1);
INSERT INTO `OrderDetails` Values (10305, 39, 14.4, 30, 0.1);
INSERT INTO `OrderDetails` Values (10306, 30, 20.7, 10, 0.0);
INSERT INTO `OrderDetails` Values (10306, 53, 26.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10306, 54, 5.9, 5, 0.0);
INSERT INTO `OrderDetails` Values (10307, 62, 39.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (10307, 68, 10, 3, 0.0);
INSERT INTO `OrderDetails` Values (10308, 69, 28.8, 1, 0.0);
INSERT INTO `OrderDetails` Values (10308, 70, 12, 5, 0.0);
INSERT INTO `OrderDetails` Values (10309, 4, 17.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10309, 6, 20, 30, 0.0);
INSERT INTO `OrderDetails` Values (10309, 42, 11.2, 2, 0.0);
INSERT INTO `OrderDetails` Values (10309, 43, 36.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10309, 71, 17.2, 3, 0.0);
INSERT INTO `OrderDetails` Values (10310, 16, 13.9, 10, 0.0);
INSERT INTO `OrderDetails` Values (10310, 62, 39.4, 5, 0.0);
INSERT INTO `OrderDetails` Values (10311, 42, 11.2, 6, 0.0);
INSERT INTO `OrderDetails` Values (10311, 69, 28.8, 7, 0.0);
INSERT INTO `OrderDetails` Values (10312, 28, 36.4, 4, 0.0);
INSERT INTO `OrderDetails` Values (10312, 43, 36.8, 24, 0.0);
INSERT INTO `OrderDetails` Values (10312, 53, 26.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10312, 75, 6.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10313, 36, 15.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10314, 32, 25.6, 40, 0.1);
INSERT INTO `OrderDetails` Values (10314, 58, 10.6, 30, 0.1);
INSERT INTO `OrderDetails` Values (10314, 62, 39.4, 25, 0.1);
INSERT INTO `OrderDetails` Values (10315, 34, 11.2, 14, 0.0);
INSERT INTO `OrderDetails` Values (10315, 70, 12, 30, 0.0);
INSERT INTO `OrderDetails` Values (10316, 41, 7.7, 10, 0.0);
INSERT INTO `OrderDetails` Values (10316, 62, 39.4, 70, 0.0);
INSERT INTO `OrderDetails` Values (10317, 1, 14.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10318, 41, 7.7, 20, 0.0);
INSERT INTO `OrderDetails` Values (10318, 76, 14.4, 6, 0.0);
INSERT INTO `OrderDetails` Values (10319, 17, 31.2, 8, 0.0);
INSERT INTO `OrderDetails` Values (10319, 28, 36.4, 14, 0.0);
INSERT INTO `OrderDetails` Values (10319, 76, 14.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10320, 71, 17.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10321, 35, 14.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (10322, 52, 5.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10323, 15, 12.4, 5, 0.0);
INSERT INTO `OrderDetails` Values (10323, 25, 11.2, 4, 0.0);
INSERT INTO `OrderDetails` Values (10323, 39, 14.4, 4, 0.0);
INSERT INTO `OrderDetails` Values (10324, 16, 13.9, 21, 0.15);
INSERT INTO `OrderDetails` Values (10324, 35, 14.4, 70, 0.15);
INSERT INTO `OrderDetails` Values (10324, 46, 9.6, 30, 0.0);
INSERT INTO `OrderDetails` Values (10324, 59, 44, 40, 0.15);
INSERT INTO `OrderDetails` Values (10324, 63, 35.1, 80, 0.15);
INSERT INTO `OrderDetails` Values (10325, 6, 20, 6, 0.0);
INSERT INTO `OrderDetails` Values (10325, 13, 4.8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10325, 14, 18.6, 9, 0.0);
INSERT INTO `OrderDetails` Values (10325, 31, 10, 4, 0.0);
INSERT INTO `OrderDetails` Values (10325, 72, 27.8, 40, 0.0);
INSERT INTO `OrderDetails` Values (10326, 4, 17.6, 24, 0.0);
INSERT INTO `OrderDetails` Values (10326, 57, 15.6, 16, 0.0);
INSERT INTO `OrderDetails` Values (10326, 75, 6.2, 50, 0.0);
INSERT INTO `OrderDetails` Values (10327, 2, 15.2, 25, 0.2);
INSERT INTO `OrderDetails` Values (10327, 11, 16.8, 50, 0.2);
INSERT INTO `OrderDetails` Values (10327, 30, 20.7, 35, 0.2);
INSERT INTO `OrderDetails` Values (10327, 58, 10.6, 30, 0.2);
INSERT INTO `OrderDetails` Values (10328, 59, 44, 9, 0.0);
INSERT INTO `OrderDetails` Values (10328, 65, 16.8, 40, 0.0);
INSERT INTO `OrderDetails` Values (10328, 68, 10, 10, 0.0);
INSERT INTO `OrderDetails` Values (10329, 19, 7.3, 10, 0.05);
INSERT INTO `OrderDetails` Values (10329, 30, 20.7, 8, 0.05);
INSERT INTO `OrderDetails` Values (10329, 38, 210.8, 20, 0.05);
INSERT INTO `OrderDetails` Values (10329, 56, 30.4, 12, 0.05);
INSERT INTO `OrderDetails` Values (10330, 26, 24.9, 50, 0.15);
INSERT INTO `OrderDetails` Values (10330, 72, 27.8, 25, 0.15);
INSERT INTO `OrderDetails` Values (10331, 54, 5.9, 15, 0.0);
INSERT INTO `OrderDetails` Values (10332, 18, 50, 40, 0.2);
INSERT INTO `OrderDetails` Values (10332, 42, 11.2, 10, 0.2);
INSERT INTO `OrderDetails` Values (10332, 47, 7.6, 16, 0.2);
INSERT INTO `OrderDetails` Values (10333, 14, 18.6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10333, 21, 8, 10, 0.1);
INSERT INTO `OrderDetails` Values (10333, 71, 17.2, 40, 0.1);
INSERT INTO `OrderDetails` Values (10334, 52, 5.6, 8, 0.0);
INSERT INTO `OrderDetails` Values (10334, 68, 10, 10, 0.0);
INSERT INTO `OrderDetails` Values (10335, 2, 15.2, 7, 0.2);
INSERT INTO `OrderDetails` Values (10335, 31, 10, 25, 0.2);
INSERT INTO `OrderDetails` Values (10335, 32, 25.6, 6, 0.2);
INSERT INTO `OrderDetails` Values (10335, 51, 42.4, 48, 0.2);
INSERT INTO `OrderDetails` Values (10336, 4, 17.6, 18, 0.1);
INSERT INTO `OrderDetails` Values (10337, 23, 7.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10337, 26, 24.9, 24, 0.0);
INSERT INTO `OrderDetails` Values (10337, 36, 15.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10337, 37, 20.8, 28, 0.0);
INSERT INTO `OrderDetails` Values (10337, 72, 27.8, 25, 0.0);
INSERT INTO `OrderDetails` Values (10338, 17, 31.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10338, 30, 20.7, 15, 0.0);
INSERT INTO `OrderDetails` Values (10339, 4, 17.6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10339, 17, 31.2, 70, 0.05);
INSERT INTO `OrderDetails` Values (10339, 62, 39.4, 28, 0.0);
INSERT INTO `OrderDetails` Values (10340, 18, 50, 20, 0.05);
INSERT INTO `OrderDetails` Values (10340, 41, 7.7, 12, 0.05);
INSERT INTO `OrderDetails` Values (10340, 43, 36.8, 40, 0.05);
INSERT INTO `OrderDetails` Values (10341, 33, 2, 8, 0.0);
INSERT INTO `OrderDetails` Values (10341, 59, 44, 9, 0.15);
INSERT INTO `OrderDetails` Values (10342, 2, 15.2, 24, 0.2);
INSERT INTO `OrderDetails` Values (10342, 31, 10, 56, 0.2);
INSERT INTO `OrderDetails` Values (10342, 36, 15.2, 40, 0.2);
INSERT INTO `OrderDetails` Values (10342, 55, 19.2, 40, 0.2);
INSERT INTO `OrderDetails` Values (10343, 64, 26.6, 50, 0.0);
INSERT INTO `OrderDetails` Values (10343, 68, 10, 4, 0.05);
INSERT INTO `OrderDetails` Values (10343, 76, 14.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10344, 4, 17.6, 35, 0.0);
INSERT INTO `OrderDetails` Values (10344, 8, 32, 70, 0.25);
INSERT INTO `OrderDetails` Values (10345, 8, 32, 70, 0.0);
INSERT INTO `OrderDetails` Values (10345, 19, 7.3, 80, 0.0);
INSERT INTO `OrderDetails` Values (10345, 42, 11.2, 9, 0.0);
INSERT INTO `OrderDetails` Values (10346, 17, 31.2, 36, 0.1);
INSERT INTO `OrderDetails` Values (10346, 56, 30.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10347, 25, 11.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10347, 39, 14.4, 50, 0.15);
INSERT INTO `OrderDetails` Values (10347, 40, 14.7, 4, 0.0);
INSERT INTO `OrderDetails` Values (10347, 75, 6.2, 6, 0.15);
INSERT INTO `OrderDetails` Values (10348, 1, 14.4, 15, 0.15);
INSERT INTO `OrderDetails` Values (10348, 23, 7.2, 25, 0.0);
INSERT INTO `OrderDetails` Values (10349, 54, 5.9, 24, 0.0);
INSERT INTO `OrderDetails` Values (10350, 50, 13, 15, 0.1);
INSERT INTO `OrderDetails` Values (10350, 69, 28.8, 18, 0.1);
INSERT INTO `OrderDetails` Values (10351, 38, 210.8, 20, 0.05);
INSERT INTO `OrderDetails` Values (10351, 41, 7.7, 13, 0.0);
INSERT INTO `OrderDetails` Values (10351, 44, 15.5, 77, 0.05);
INSERT INTO `OrderDetails` Values (10351, 65, 16.8, 10, 0.05);
INSERT INTO `OrderDetails` Values (10352, 24, 3.6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10352, 54, 5.9, 20, 0.15);
INSERT INTO `OrderDetails` Values (10353, 11, 16.8, 12, 0.2);
INSERT INTO `OrderDetails` Values (10353, 38, 210.8, 50, 0.2);
INSERT INTO `OrderDetails` Values (10354, 1, 14.4, 12, 0.0);
INSERT INTO `OrderDetails` Values (10354, 29, 99, 4, 0.0);
INSERT INTO `OrderDetails` Values (10355, 24, 3.6, 25, 0.0);
INSERT INTO `OrderDetails` Values (10355, 57, 15.6, 25, 0.0);
INSERT INTO `OrderDetails` Values (10356, 31, 10, 30, 0.0);
INSERT INTO `OrderDetails` Values (10356, 55, 19.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10356, 69, 28.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10357, 10, 24.8, 30, 0.2);
INSERT INTO `OrderDetails` Values (10357, 26, 24.9, 16, 0.0);
INSERT INTO `OrderDetails` Values (10357, 60, 27.2, 8, 0.2);
INSERT INTO `OrderDetails` Values (10358, 24, 3.6, 10, 0.05);
INSERT INTO `OrderDetails` Values (10358, 34, 11.2, 10, 0.05);
INSERT INTO `OrderDetails` Values (10358, 36, 15.2, 20, 0.05);
INSERT INTO `OrderDetails` Values (10359, 16, 13.9, 56, 0.05);
INSERT INTO `OrderDetails` Values (10359, 31, 10, 70, 0.05);
INSERT INTO `OrderDetails` Values (10359, 60, 27.2, 80, 0.05);
INSERT INTO `OrderDetails` Values (10360, 28, 36.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10360, 29, 99, 35, 0.0);
INSERT INTO `OrderDetails` Values (10360, 38, 210.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10360, 49, 16, 35, 0.0);
INSERT INTO `OrderDetails` Values (10360, 54, 5.9, 28, 0.0);
INSERT INTO `OrderDetails` Values (10361, 39, 14.4, 54, 0.1);
INSERT INTO `OrderDetails` Values (10361, 60, 27.2, 55, 0.1);
INSERT INTO `OrderDetails` Values (10362, 25, 11.2, 50, 0.0);
INSERT INTO `OrderDetails` Values (10362, 51, 42.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10362, 54, 5.9, 24, 0.0);
INSERT INTO `OrderDetails` Values (10363, 31, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10363, 75, 6.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10363, 76, 14.4, 12, 0.0);
INSERT INTO `OrderDetails` Values (10364, 69, 28.8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10364, 71, 17.2, 5, 0.0);
INSERT INTO `OrderDetails` Values (10365, 11, 16.8, 24, 0.0);
INSERT INTO `OrderDetails` Values (10366, 65, 16.8, 5, 0.0);
INSERT INTO `OrderDetails` Values (10366, 77, 10.4, 5, 0.0);
INSERT INTO `OrderDetails` Values (10367, 34, 11.2, 36, 0.0);
INSERT INTO `OrderDetails` Values (10367, 54, 5.9, 18, 0.0);
INSERT INTO `OrderDetails` Values (10367, 65, 16.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10367, 77, 10.4, 7, 0.0);
INSERT INTO `OrderDetails` Values (10368, 21, 8, 5, 0.1);
INSERT INTO `OrderDetails` Values (10368, 28, 36.4, 13, 0.1);
INSERT INTO `OrderDetails` Values (10368, 57, 15.6, 25, 0.0);
INSERT INTO `OrderDetails` Values (10368, 64, 26.6, 35, 0.1);
INSERT INTO `OrderDetails` Values (10369, 29, 99, 20, 0.0);
INSERT INTO `OrderDetails` Values (10369, 56, 30.4, 18, 0.25);
INSERT INTO `OrderDetails` Values (10370, 1, 14.4, 15, 0.15);
INSERT INTO `OrderDetails` Values (10370, 64, 26.6, 30, 0.0);
INSERT INTO `OrderDetails` Values (10370, 74, 8, 20, 0.15);
INSERT INTO `OrderDetails` Values (10371, 36, 15.2, 6, 0.2);
INSERT INTO `OrderDetails` Values (10372, 20, 64.8, 12, 0.25);
INSERT INTO `OrderDetails` Values (10372, 38, 210.8, 40, 0.25);
INSERT INTO `OrderDetails` Values (10372, 60, 27.2, 70, 0.25);
INSERT INTO `OrderDetails` Values (10372, 72, 27.8, 42, 0.25);
INSERT INTO `OrderDetails` Values (10373, 58, 10.6, 80, 0.2);
INSERT INTO `OrderDetails` Values (10373, 71, 17.2, 50, 0.2);
INSERT INTO `OrderDetails` Values (10374, 31, 10, 30, 0.0);
INSERT INTO `OrderDetails` Values (10374, 58, 10.6, 15, 0.0);
INSERT INTO `OrderDetails` Values (10375, 14, 18.6, 15, 0.0);
INSERT INTO `OrderDetails` Values (10375, 54, 5.9, 10, 0.0);
INSERT INTO `OrderDetails` Values (10376, 31, 10, 42, 0.05);
INSERT INTO `OrderDetails` Values (10377, 28, 36.4, 20, 0.15);
INSERT INTO `OrderDetails` Values (10377, 39, 14.4, 20, 0.15);
INSERT INTO `OrderDetails` Values (10378, 71, 17.2, 6, 0.0);
INSERT INTO `OrderDetails` Values (10379, 41, 7.7, 8, 0.1);
INSERT INTO `OrderDetails` Values (10379, 63, 35.1, 16, 0.1);
INSERT INTO `OrderDetails` Values (10379, 65, 16.8, 20, 0.1);
INSERT INTO `OrderDetails` Values (10380, 30, 20.7, 18, 0.1);
INSERT INTO `OrderDetails` Values (10380, 53, 26.2, 20, 0.1);
INSERT INTO `OrderDetails` Values (10380, 60, 27.2, 6, 0.1);
INSERT INTO `OrderDetails` Values (10380, 70, 12, 30, 0.0);
INSERT INTO `OrderDetails` Values (10381, 74, 8, 14, 0.0);
INSERT INTO `OrderDetails` Values (10382, 5, 17, 32, 0.0);
INSERT INTO `OrderDetails` Values (10382, 18, 50, 9, 0.0);
INSERT INTO `OrderDetails` Values (10382, 29, 99, 14, 0.0);
INSERT INTO `OrderDetails` Values (10382, 33, 2, 60, 0.0);
INSERT INTO `OrderDetails` Values (10382, 74, 8, 50, 0.0);
INSERT INTO `OrderDetails` Values (10383, 13, 4.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10383, 50, 13, 15, 0.0);
INSERT INTO `OrderDetails` Values (10383, 56, 30.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10384, 20, 64.8, 28, 0.0);
INSERT INTO `OrderDetails` Values (10384, 60, 27.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10385, 7, 24, 10, 0.2);
INSERT INTO `OrderDetails` Values (10385, 60, 27.2, 20, 0.2);
INSERT INTO `OrderDetails` Values (10385, 68, 10, 8, 0.2);
INSERT INTO `OrderDetails` Values (10386, 24, 3.6, 15, 0.0);
INSERT INTO `OrderDetails` Values (10386, 34, 11.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10387, 24, 3.6, 15, 0.0);
INSERT INTO `OrderDetails` Values (10387, 28, 36.4, 6, 0.0);
INSERT INTO `OrderDetails` Values (10387, 59, 44, 12, 0.0);
INSERT INTO `OrderDetails` Values (10387, 71, 17.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10388, 45, 7.6, 15, 0.2);
INSERT INTO `OrderDetails` Values (10388, 52, 5.6, 20, 0.2);
INSERT INTO `OrderDetails` Values (10388, 53, 26.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10389, 10, 24.8, 16, 0.0);
INSERT INTO `OrderDetails` Values (10389, 55, 19.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10389, 62, 39.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10389, 70, 12, 30, 0.0);
INSERT INTO `OrderDetails` Values (10390, 31, 10, 60, 0.1);
INSERT INTO `OrderDetails` Values (10390, 35, 14.4, 40, 0.1);
INSERT INTO `OrderDetails` Values (10390, 46, 9.6, 45, 0.0);
INSERT INTO `OrderDetails` Values (10390, 72, 27.8, 24, 0.1);
INSERT INTO `OrderDetails` Values (10391, 13, 4.8, 18, 0.0);
INSERT INTO `OrderDetails` Values (10392, 69, 28.8, 50, 0.0);
INSERT INTO `OrderDetails` Values (10393, 2, 15.2, 25, 0.25);
INSERT INTO `OrderDetails` Values (10393, 14, 18.6, 42, 0.25);
INSERT INTO `OrderDetails` Values (10393, 25, 11.2, 7, 0.25);
INSERT INTO `OrderDetails` Values (10393, 26, 24.9, 70, 0.25);
INSERT INTO `OrderDetails` Values (10393, 31, 10, 32, 0.0);
INSERT INTO `OrderDetails` Values (10394, 13, 4.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10394, 62, 39.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (10395, 46, 9.6, 28, 0.1);
INSERT INTO `OrderDetails` Values (10395, 53, 26.2, 70, 0.1);
INSERT INTO `OrderDetails` Values (10395, 69, 28.8, 8, 0.0);
INSERT INTO `OrderDetails` Values (10396, 23, 7.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10396, 71, 17.2, 60, 0.0);
INSERT INTO `OrderDetails` Values (10396, 72, 27.8, 21, 0.0);
INSERT INTO `OrderDetails` Values (10397, 21, 8, 10, 0.15);
INSERT INTO `OrderDetails` Values (10397, 51, 42.4, 18, 0.15);
INSERT INTO `OrderDetails` Values (10398, 35, 14.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10398, 55, 19.2, 120, 0.1);
INSERT INTO `OrderDetails` Values (10399, 68, 10, 60, 0.0);
INSERT INTO `OrderDetails` Values (10399, 71, 17.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10399, 76, 14.4, 35, 0.0);
INSERT INTO `OrderDetails` Values (10399, 77, 10.4, 14, 0.0);
INSERT INTO `OrderDetails` Values (10400, 29, 99, 21, 0.0);
INSERT INTO `OrderDetails` Values (10400, 35, 14.4, 35, 0.0);
INSERT INTO `OrderDetails` Values (10400, 49, 16, 30, 0.0);
INSERT INTO `OrderDetails` Values (10401, 30, 20.7, 18, 0.0);
INSERT INTO `OrderDetails` Values (10401, 56, 30.4, 70, 0.0);
INSERT INTO `OrderDetails` Values (10401, 65, 16.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10401, 71, 17.2, 60, 0.0);
INSERT INTO `OrderDetails` Values (10402, 23, 7.2, 60, 0.0);
INSERT INTO `OrderDetails` Values (10402, 63, 35.1, 65, 0.0);
INSERT INTO `OrderDetails` Values (10403, 16, 13.9, 21, 0.15);
INSERT INTO `OrderDetails` Values (10403, 48, 10.2, 70, 0.15);
INSERT INTO `OrderDetails` Values (10404, 26, 24.9, 30, 0.05);
INSERT INTO `OrderDetails` Values (10404, 42, 11.2, 40, 0.05);
INSERT INTO `OrderDetails` Values (10404, 49, 16, 30, 0.05);
INSERT INTO `OrderDetails` Values (10405, 3, 8, 50, 0.0);
INSERT INTO `OrderDetails` Values (10406, 1, 14.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (10406, 21, 8, 30, 0.1);
INSERT INTO `OrderDetails` Values (10406, 28, 36.4, 42, 0.1);
INSERT INTO `OrderDetails` Values (10406, 36, 15.2, 5, 0.1);
INSERT INTO `OrderDetails` Values (10406, 40, 14.7, 2, 0.1);
INSERT INTO `OrderDetails` Values (10407, 11, 16.8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10407, 69, 28.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10407, 71, 17.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10408, 37, 20.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10408, 54, 5.9, 6, 0.0);
INSERT INTO `OrderDetails` Values (10408, 62, 39.4, 35, 0.0);
INSERT INTO `OrderDetails` Values (10409, 14, 18.6, 12, 0.0);
INSERT INTO `OrderDetails` Values (10409, 21, 8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10410, 33, 2, 49, 0.0);
INSERT INTO `OrderDetails` Values (10410, 59, 44, 16, 0.0);
INSERT INTO `OrderDetails` Values (10411, 41, 7.7, 25, 0.2);
INSERT INTO `OrderDetails` Values (10411, 44, 15.5, 40, 0.2);
INSERT INTO `OrderDetails` Values (10411, 59, 44, 9, 0.2);
INSERT INTO `OrderDetails` Values (10412, 14, 18.6, 20, 0.1);
INSERT INTO `OrderDetails` Values (10413, 1, 14.4, 24, 0.0);
INSERT INTO `OrderDetails` Values (10413, 62, 39.4, 40, 0.0);
INSERT INTO `OrderDetails` Values (10413, 76, 14.4, 14, 0.0);
INSERT INTO `OrderDetails` Values (10414, 19, 7.3, 18, 0.05);
INSERT INTO `OrderDetails` Values (10414, 33, 2, 50, 0.0);
INSERT INTO `OrderDetails` Values (10415, 17, 31.2, 2, 0.0);
INSERT INTO `OrderDetails` Values (10415, 33, 2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10416, 19, 7.3, 20, 0.0);
INSERT INTO `OrderDetails` Values (10416, 53, 26.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10416, 57, 15.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10417, 38, 210.8, 50, 0.0);
INSERT INTO `OrderDetails` Values (10417, 46, 9.6, 2, 0.25);
INSERT INTO `OrderDetails` Values (10417, 68, 10, 36, 0.25);
INSERT INTO `OrderDetails` Values (10417, 77, 10.4, 35, 0.0);
INSERT INTO `OrderDetails` Values (10418, 2, 15.2, 60, 0.0);
INSERT INTO `OrderDetails` Values (10418, 47, 7.6, 55, 0.0);
INSERT INTO `OrderDetails` Values (10418, 61, 22.8, 16, 0.0);
INSERT INTO `OrderDetails` Values (10418, 74, 8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10419, 60, 27.2, 60, 0.05);
INSERT INTO `OrderDetails` Values (10419, 69, 28.8, 20, 0.05);
INSERT INTO `OrderDetails` Values (10420, 9, 77.6, 20, 0.1);
INSERT INTO `OrderDetails` Values (10420, 13, 4.8, 2, 0.1);
INSERT INTO `OrderDetails` Values (10420, 70, 12, 8, 0.1);
INSERT INTO `OrderDetails` Values (10420, 73, 12, 20, 0.1);
INSERT INTO `OrderDetails` Values (10421, 19, 7.3, 4, 0.15);
INSERT INTO `OrderDetails` Values (10421, 26, 24.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (10421, 53, 26.2, 15, 0.15);
INSERT INTO `OrderDetails` Values (10421, 77, 10.4, 10, 0.15);
INSERT INTO `OrderDetails` Values (10422, 26, 24.9, 2, 0.0);
INSERT INTO `OrderDetails` Values (10423, 31, 10, 14, 0.0);
INSERT INTO `OrderDetails` Values (10423, 59, 44, 20, 0.0);
INSERT INTO `OrderDetails` Values (10424, 35, 14.4, 60, 0.2);
INSERT INTO `OrderDetails` Values (10424, 38, 210.8, 49, 0.2);
INSERT INTO `OrderDetails` Values (10424, 68, 10, 30, 0.2);
INSERT INTO `OrderDetails` Values (10425, 55, 19.2, 10, 0.25);
INSERT INTO `OrderDetails` Values (10425, 76, 14.4, 20, 0.25);
INSERT INTO `OrderDetails` Values (10426, 56, 30.4, 5, 0.0);
INSERT INTO `OrderDetails` Values (10426, 64, 26.6, 7, 0.0);
INSERT INTO `OrderDetails` Values (10427, 14, 18.6, 35, 0.0);
INSERT INTO `OrderDetails` Values (10428, 46, 9.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10429, 50, 13, 40, 0.0);
INSERT INTO `OrderDetails` Values (10429, 63, 35.1, 35, 0.25);
INSERT INTO `OrderDetails` Values (10430, 17, 31.2, 45, 0.2);
INSERT INTO `OrderDetails` Values (10430, 21, 8, 50, 0.0);
INSERT INTO `OrderDetails` Values (10430, 56, 30.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10430, 59, 44, 70, 0.2);
INSERT INTO `OrderDetails` Values (10431, 17, 31.2, 50, 0.25);
INSERT INTO `OrderDetails` Values (10431, 40, 14.7, 50, 0.25);
INSERT INTO `OrderDetails` Values (10431, 47, 7.6, 30, 0.25);
INSERT INTO `OrderDetails` Values (10432, 26, 24.9, 10, 0.0);
INSERT INTO `OrderDetails` Values (10432, 54, 5.9, 40, 0.0);
INSERT INTO `OrderDetails` Values (10433, 56, 30.4, 28, 0.0);
INSERT INTO `OrderDetails` Values (10434, 11, 16.8, 6, 0.0);
INSERT INTO `OrderDetails` Values (10434, 76, 14.4, 18, 0.15);
INSERT INTO `OrderDetails` Values (10435, 2, 15.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10435, 22, 16.8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10435, 72, 27.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10436, 46, 9.6, 5, 0.0);
INSERT INTO `OrderDetails` Values (10436, 56, 30.4, 40, 0.1);
INSERT INTO `OrderDetails` Values (10436, 64, 26.6, 30, 0.1);
INSERT INTO `OrderDetails` Values (10436, 75, 6.2, 24, 0.1);
INSERT INTO `OrderDetails` Values (10437, 53, 26.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10438, 19, 7.3, 15, 0.2);
INSERT INTO `OrderDetails` Values (10438, 34, 11.2, 20, 0.2);
INSERT INTO `OrderDetails` Values (10438, 57, 15.6, 15, 0.2);
INSERT INTO `OrderDetails` Values (10439, 12, 30.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10439, 16, 13.9, 16, 0.0);
INSERT INTO `OrderDetails` Values (10439, 64, 26.6, 6, 0.0);
INSERT INTO `OrderDetails` Values (10439, 74, 8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10440, 2, 15.2, 45, 0.15);
INSERT INTO `OrderDetails` Values (10440, 16, 13.9, 49, 0.15);
INSERT INTO `OrderDetails` Values (10440, 29, 99, 24, 0.15);
INSERT INTO `OrderDetails` Values (10440, 61, 22.8, 90, 0.15);
INSERT INTO `OrderDetails` Values (10441, 27, 35.1, 50, 0.0);
INSERT INTO `OrderDetails` Values (10442, 11, 16.8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10442, 54, 5.9, 80, 0.0);
INSERT INTO `OrderDetails` Values (10442, 66, 13.6, 60, 0.0);
INSERT INTO `OrderDetails` Values (10443, 11, 16.8, 6, 0.2);
INSERT INTO `OrderDetails` Values (10443, 28, 36.4, 12, 0.0);
INSERT INTO `OrderDetails` Values (10444, 17, 31.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10444, 26, 24.9, 15, 0.0);
INSERT INTO `OrderDetails` Values (10444, 35, 14.4, 8, 0.0);
INSERT INTO `OrderDetails` Values (10444, 41, 7.7, 30, 0.0);
INSERT INTO `OrderDetails` Values (10445, 39, 14.4, 6, 0.0);
INSERT INTO `OrderDetails` Values (10445, 54, 5.9, 15, 0.0);
INSERT INTO `OrderDetails` Values (10446, 19, 7.3, 12, 0.1);
INSERT INTO `OrderDetails` Values (10446, 24, 3.6, 20, 0.1);
INSERT INTO `OrderDetails` Values (10446, 31, 10, 3, 0.1);
INSERT INTO `OrderDetails` Values (10446, 52, 5.6, 15, 0.1);
INSERT INTO `OrderDetails` Values (10447, 19, 7.3, 40, 0.0);
INSERT INTO `OrderDetails` Values (10447, 65, 16.8, 35, 0.0);
INSERT INTO `OrderDetails` Values (10447, 71, 17.2, 2, 0.0);
INSERT INTO `OrderDetails` Values (10448, 26, 24.9, 6, 0.0);
INSERT INTO `OrderDetails` Values (10448, 40, 14.7, 20, 0.0);
INSERT INTO `OrderDetails` Values (10449, 10, 24.8, 14, 0.0);
INSERT INTO `OrderDetails` Values (10449, 52, 5.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10449, 62, 39.4, 35, 0.0);
INSERT INTO `OrderDetails` Values (10450, 10, 24.8, 20, 0.2);
INSERT INTO `OrderDetails` Values (10450, 54, 5.9, 6, 0.2);
INSERT INTO `OrderDetails` Values (10451, 55, 19.2, 120, 0.1);
INSERT INTO `OrderDetails` Values (10451, 64, 26.6, 35, 0.1);
INSERT INTO `OrderDetails` Values (10451, 65, 16.8, 28, 0.1);
INSERT INTO `OrderDetails` Values (10451, 77, 10.4, 55, 0.1);
INSERT INTO `OrderDetails` Values (10452, 28, 36.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10452, 44, 15.5, 100, 0.05);
INSERT INTO `OrderDetails` Values (10453, 48, 10.2, 15, 0.1);
INSERT INTO `OrderDetails` Values (10453, 70, 12, 25, 0.1);
INSERT INTO `OrderDetails` Values (10454, 16, 13.9, 20, 0.2);
INSERT INTO `OrderDetails` Values (10454, 33, 2, 20, 0.2);
INSERT INTO `OrderDetails` Values (10454, 46, 9.6, 10, 0.2);
INSERT INTO `OrderDetails` Values (10455, 39, 14.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10455, 53, 26.2, 50, 0.0);
INSERT INTO `OrderDetails` Values (10455, 61, 22.8, 25, 0.0);
INSERT INTO `OrderDetails` Values (10455, 71, 17.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10456, 21, 8, 40, 0.15);
INSERT INTO `OrderDetails` Values (10456, 49, 16, 21, 0.15);
INSERT INTO `OrderDetails` Values (10457, 59, 44, 36, 0.0);
INSERT INTO `OrderDetails` Values (10458, 26, 24.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (10458, 28, 36.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10458, 43, 36.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10458, 56, 30.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10458, 71, 17.2, 50, 0.0);
INSERT INTO `OrderDetails` Values (10459, 7, 24, 16, 0.05);
INSERT INTO `OrderDetails` Values (10459, 46, 9.6, 20, 0.05);
INSERT INTO `OrderDetails` Values (10459, 72, 27.8, 40, 0.0);
INSERT INTO `OrderDetails` Values (10460, 68, 10, 21, 0.25);
INSERT INTO `OrderDetails` Values (10460, 75, 6.2, 4, 0.25);
INSERT INTO `OrderDetails` Values (10461, 21, 8, 40, 0.25);
INSERT INTO `OrderDetails` Values (10461, 30, 20.7, 28, 0.25);
INSERT INTO `OrderDetails` Values (10461, 55, 19.2, 60, 0.25);
INSERT INTO `OrderDetails` Values (10462, 13, 4.8, 1, 0.0);
INSERT INTO `OrderDetails` Values (10462, 23, 7.2, 21, 0.0);
INSERT INTO `OrderDetails` Values (10463, 19, 7.3, 21, 0.0);
INSERT INTO `OrderDetails` Values (10463, 42, 11.2, 50, 0.0);
INSERT INTO `OrderDetails` Values (10464, 4, 17.6, 16, 0.2);
INSERT INTO `OrderDetails` Values (10464, 43, 36.8, 3, 0.0);
INSERT INTO `OrderDetails` Values (10464, 56, 30.4, 30, 0.2);
INSERT INTO `OrderDetails` Values (10464, 60, 27.2, 20, 0.0);
INSERT INTO `OrderDetails` Values (10465, 24, 3.6, 25, 0.0);
INSERT INTO `OrderDetails` Values (10465, 29, 99, 18, 0.1);
INSERT INTO `OrderDetails` Values (10465, 40, 14.7, 20, 0.0);
INSERT INTO `OrderDetails` Values (10465, 45, 7.6, 30, 0.1);
INSERT INTO `OrderDetails` Values (10465, 50, 13, 25, 0.0);
INSERT INTO `OrderDetails` Values (10466, 11, 16.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10466, 46, 9.6, 5, 0.0);
INSERT INTO `OrderDetails` Values (10467, 24, 3.6, 28, 0.0);
INSERT INTO `OrderDetails` Values (10467, 25, 11.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10468, 30, 20.7, 8, 0.0);
INSERT INTO `OrderDetails` Values (10468, 43, 36.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10469, 2, 15.2, 40, 0.15);
INSERT INTO `OrderDetails` Values (10469, 16, 13.9, 35, 0.15);
INSERT INTO `OrderDetails` Values (10469, 44, 15.5, 2, 0.15);
INSERT INTO `OrderDetails` Values (10470, 18, 50, 30, 0.0);
INSERT INTO `OrderDetails` Values (10470, 23, 7.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10470, 64, 26.6, 8, 0.0);
INSERT INTO `OrderDetails` Values (10471, 7, 24, 30, 0.0);
INSERT INTO `OrderDetails` Values (10471, 56, 30.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10472, 24, 3.6, 80, 0.05);
INSERT INTO `OrderDetails` Values (10472, 51, 42.4, 18, 0.0);
INSERT INTO `OrderDetails` Values (10473, 33, 2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10473, 71, 17.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10474, 14, 18.6, 12, 0.0);
INSERT INTO `OrderDetails` Values (10474, 28, 36.4, 18, 0.0);
INSERT INTO `OrderDetails` Values (10474, 40, 14.7, 21, 0.0);
INSERT INTO `OrderDetails` Values (10474, 75, 6.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10475, 31, 10, 35, 0.15);
INSERT INTO `OrderDetails` Values (10475, 66, 13.6, 60, 0.15);
INSERT INTO `OrderDetails` Values (10475, 76, 14.4, 42, 0.15);
INSERT INTO `OrderDetails` Values (10476, 55, 19.2, 2, 0.05);
INSERT INTO `OrderDetails` Values (10476, 70, 12, 12, 0.0);
INSERT INTO `OrderDetails` Values (10477, 1, 14.4, 15, 0.0);
INSERT INTO `OrderDetails` Values (10477, 21, 8, 21, 0.25);
INSERT INTO `OrderDetails` Values (10477, 39, 14.4, 20, 0.25);
INSERT INTO `OrderDetails` Values (10478, 10, 24.8, 20, 0.05);
INSERT INTO `OrderDetails` Values (10479, 38, 210.8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10479, 53, 26.2, 28, 0.0);
INSERT INTO `OrderDetails` Values (10479, 59, 44, 60, 0.0);
INSERT INTO `OrderDetails` Values (10479, 64, 26.6, 30, 0.0);
INSERT INTO `OrderDetails` Values (10480, 47, 7.6, 30, 0.0);
INSERT INTO `OrderDetails` Values (10480, 59, 44, 12, 0.0);
INSERT INTO `OrderDetails` Values (10481, 49, 16, 24, 0.0);
INSERT INTO `OrderDetails` Values (10481, 60, 27.2, 40, 0.0);
INSERT INTO `OrderDetails` Values (10482, 40, 14.7, 10, 0.0);
INSERT INTO `OrderDetails` Values (10483, 34, 11.2, 35, 0.05);
INSERT INTO `OrderDetails` Values (10483, 77, 10.4, 30, 0.05);
INSERT INTO `OrderDetails` Values (10484, 21, 8, 14, 0.0);
INSERT INTO `OrderDetails` Values (10484, 40, 14.7, 10, 0.0);
INSERT INTO `OrderDetails` Values (10484, 51, 42.4, 3, 0.0);
INSERT INTO `OrderDetails` Values (10485, 2, 15.2, 20, 0.1);
INSERT INTO `OrderDetails` Values (10485, 3, 8, 20, 0.1);
INSERT INTO `OrderDetails` Values (10485, 55, 19.2, 30, 0.1);
INSERT INTO `OrderDetails` Values (10485, 70, 12, 60, 0.1);
INSERT INTO `OrderDetails` Values (10486, 11, 16.8, 5, 0.0);
INSERT INTO `OrderDetails` Values (10486, 51, 42.4, 25, 0.0);
INSERT INTO `OrderDetails` Values (10486, 74, 8, 16, 0.0);
INSERT INTO `OrderDetails` Values (10487, 19, 7.3, 5, 0.0);
INSERT INTO `OrderDetails` Values (10487, 26, 24.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (10487, 54, 5.9, 24, 0.25);
INSERT INTO `OrderDetails` Values (10488, 59, 44, 30, 0.0);
INSERT INTO `OrderDetails` Values (10488, 73, 12, 20, 0.2);
INSERT INTO `OrderDetails` Values (10489, 11, 16.8, 15, 0.25);
INSERT INTO `OrderDetails` Values (10489, 16, 13.9, 18, 0.0);
INSERT INTO `OrderDetails` Values (10490, 59, 44, 60, 0.0);
INSERT INTO `OrderDetails` Values (10490, 68, 10, 30, 0.0);
INSERT INTO `OrderDetails` Values (10490, 75, 6.2, 36, 0.0);
INSERT INTO `OrderDetails` Values (10491, 44, 15.5, 15, 0.15);
INSERT INTO `OrderDetails` Values (10491, 77, 10.4, 7, 0.15);
INSERT INTO `OrderDetails` Values (10492, 25, 11.2, 60, 0.05);
INSERT INTO `OrderDetails` Values (10492, 42, 11.2, 20, 0.05);
INSERT INTO `OrderDetails` Values (10493, 65, 16.8, 15, 0.1);
INSERT INTO `OrderDetails` Values (10493, 66, 13.6, 10, 0.1);
INSERT INTO `OrderDetails` Values (10493, 69, 28.8, 10, 0.1);
INSERT INTO `OrderDetails` Values (10494, 56, 30.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10495, 23, 7.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10495, 41, 7.7, 20, 0.0);
INSERT INTO `OrderDetails` Values (10495, 77, 10.4, 5, 0.0);
INSERT INTO `OrderDetails` Values (10496, 31, 10, 20, 0.05);
INSERT INTO `OrderDetails` Values (10497, 56, 30.4, 14, 0.0);
INSERT INTO `OrderDetails` Values (10497, 72, 27.8, 25, 0.0);
INSERT INTO `OrderDetails` Values (10497, 77, 10.4, 25, 0.0);
INSERT INTO `OrderDetails` Values (10498, 24, 4.5, 14, 0.0);
INSERT INTO `OrderDetails` Values (10498, 40, 18.4, 5, 0.0);
INSERT INTO `OrderDetails` Values (10498, 42, 14, 30, 0.0);
INSERT INTO `OrderDetails` Values (10499, 28, 45.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10499, 49, 20, 25, 0.0);
INSERT INTO `OrderDetails` Values (10500, 15, 15.5, 12, 0.05);
INSERT INTO `OrderDetails` Values (10500, 28, 45.6, 8, 0.05);
INSERT INTO `OrderDetails` Values (10501, 54, 7.45, 20, 0.0);
INSERT INTO `OrderDetails` Values (10502, 45, 9.5, 21, 0.0);
INSERT INTO `OrderDetails` Values (10502, 53, 32.8, 6, 0.0);
INSERT INTO `OrderDetails` Values (10502, 67, 14, 30, 0.0);
INSERT INTO `OrderDetails` Values (10503, 14, 23.25, 70, 0.0);
INSERT INTO `OrderDetails` Values (10503, 65, 21.05, 20, 0.0);
INSERT INTO `OrderDetails` Values (10504, 2, 19, 12, 0.0);
INSERT INTO `OrderDetails` Values (10504, 21, 10, 12, 0.0);
INSERT INTO `OrderDetails` Values (10504, 53, 32.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10504, 61, 28.5, 25, 0.0);
INSERT INTO `OrderDetails` Values (10505, 62, 49.3, 3, 0.0);
INSERT INTO `OrderDetails` Values (10506, 25, 14, 18, 0.1);
INSERT INTO `OrderDetails` Values (10506, 70, 15, 14, 0.1);
INSERT INTO `OrderDetails` Values (10507, 43, 46, 15, 0.15);
INSERT INTO `OrderDetails` Values (10507, 48, 12.75, 15, 0.15);
INSERT INTO `OrderDetails` Values (10508, 13, 6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10508, 39, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10509, 28, 45.6, 3, 0.0);
INSERT INTO `OrderDetails` Values (10510, 29, 123.79, 36, 0.0);
INSERT INTO `OrderDetails` Values (10510, 75, 7.75, 36, 0.1);
INSERT INTO `OrderDetails` Values (10511, 4, 22, 50, 0.15);
INSERT INTO `OrderDetails` Values (10511, 7, 30, 50, 0.15);
INSERT INTO `OrderDetails` Values (10511, 8, 40, 10, 0.15);
INSERT INTO `OrderDetails` Values (10512, 24, 4.5, 10, 0.15);
INSERT INTO `OrderDetails` Values (10512, 46, 12, 9, 0.15);
INSERT INTO `OrderDetails` Values (10512, 47, 9.5, 6, 0.15);
INSERT INTO `OrderDetails` Values (10512, 60, 34, 12, 0.15);
INSERT INTO `OrderDetails` Values (10513, 21, 10, 40, 0.2);
INSERT INTO `OrderDetails` Values (10513, 32, 32, 50, 0.2);
INSERT INTO `OrderDetails` Values (10513, 61, 28.5, 15, 0.2);
INSERT INTO `OrderDetails` Values (10514, 20, 81, 39, 0.0);
INSERT INTO `OrderDetails` Values (10514, 28, 45.6, 35, 0.0);
INSERT INTO `OrderDetails` Values (10514, 56, 38, 70, 0.0);
INSERT INTO `OrderDetails` Values (10514, 65, 21.05, 39, 0.0);
INSERT INTO `OrderDetails` Values (10514, 75, 7.75, 50, 0.0);
INSERT INTO `OrderDetails` Values (10515, 9, 97, 16, 0.15);
INSERT INTO `OrderDetails` Values (10515, 16, 17.45, 50, 0.0);
INSERT INTO `OrderDetails` Values (10515, 27, 43.9, 120, 0.0);
INSERT INTO `OrderDetails` Values (10515, 33, 2.5, 16, 0.15);
INSERT INTO `OrderDetails` Values (10515, 60, 34, 84, 0.15);
INSERT INTO `OrderDetails` Values (10516, 18, 62.5, 25, 0.1);
INSERT INTO `OrderDetails` Values (10516, 41, 9.65, 80, 0.1);
INSERT INTO `OrderDetails` Values (10516, 42, 14, 20, 0.0);
INSERT INTO `OrderDetails` Values (10517, 52, 7, 6, 0.0);
INSERT INTO `OrderDetails` Values (10517, 59, 55, 4, 0.0);
INSERT INTO `OrderDetails` Values (10517, 70, 15, 6, 0.0);
INSERT INTO `OrderDetails` Values (10518, 24, 4.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10518, 38, 263.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10518, 44, 19.45, 9, 0.0);
INSERT INTO `OrderDetails` Values (10519, 10, 31, 16, 0.05);
INSERT INTO `OrderDetails` Values (10519, 56, 38, 40, 0.0);
INSERT INTO `OrderDetails` Values (10519, 60, 34, 10, 0.05);
INSERT INTO `OrderDetails` Values (10520, 24, 4.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (10520, 53, 32.8, 5, 0.0);
INSERT INTO `OrderDetails` Values (10521, 35, 18, 3, 0.0);
INSERT INTO `OrderDetails` Values (10521, 41, 9.65, 10, 0.0);
INSERT INTO `OrderDetails` Values (10521, 68, 12.5, 6, 0.0);
INSERT INTO `OrderDetails` Values (10522, 1, 18, 40, 0.2);
INSERT INTO `OrderDetails` Values (10522, 8, 40, 24, 0.0);
INSERT INTO `OrderDetails` Values (10522, 30, 25.89, 20, 0.2);
INSERT INTO `OrderDetails` Values (10522, 40, 18.4, 25, 0.2);
INSERT INTO `OrderDetails` Values (10523, 17, 39, 25, 0.1);
INSERT INTO `OrderDetails` Values (10523, 20, 81, 15, 0.1);
INSERT INTO `OrderDetails` Values (10523, 37, 26, 18, 0.1);
INSERT INTO `OrderDetails` Values (10523, 41, 9.65, 6, 0.1);
INSERT INTO `OrderDetails` Values (10524, 10, 31, 2, 0.0);
INSERT INTO `OrderDetails` Values (10524, 30, 25.89, 10, 0.0);
INSERT INTO `OrderDetails` Values (10524, 43, 46, 60, 0.0);
INSERT INTO `OrderDetails` Values (10524, 54, 7.45, 15, 0.0);
INSERT INTO `OrderDetails` Values (10525, 36, 19, 30, 0.0);
INSERT INTO `OrderDetails` Values (10525, 40, 18.4, 15, 0.1);
INSERT INTO `OrderDetails` Values (10526, 1, 18, 8, 0.15);
INSERT INTO `OrderDetails` Values (10526, 13, 6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10526, 56, 38, 30, 0.15);
INSERT INTO `OrderDetails` Values (10527, 4, 22, 50, 0.1);
INSERT INTO `OrderDetails` Values (10527, 36, 19, 30, 0.1);
INSERT INTO `OrderDetails` Values (10528, 11, 21, 3, 0.0);
INSERT INTO `OrderDetails` Values (10528, 33, 2.5, 8, 0.2);
INSERT INTO `OrderDetails` Values (10528, 72, 34.8, 9, 0.0);
INSERT INTO `OrderDetails` Values (10529, 55, 24, 14, 0.0);
INSERT INTO `OrderDetails` Values (10529, 68, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10529, 69, 36, 10, 0.0);
INSERT INTO `OrderDetails` Values (10530, 17, 39, 40, 0.0);
INSERT INTO `OrderDetails` Values (10530, 43, 46, 25, 0.0);
INSERT INTO `OrderDetails` Values (10530, 61, 28.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10530, 76, 18, 50, 0.0);
INSERT INTO `OrderDetails` Values (10531, 59, 55, 2, 0.0);
INSERT INTO `OrderDetails` Values (10532, 30, 25.89, 15, 0.0);
INSERT INTO `OrderDetails` Values (10532, 66, 17, 24, 0.0);
INSERT INTO `OrderDetails` Values (10533, 4, 22, 50, 0.05);
INSERT INTO `OrderDetails` Values (10533, 72, 34.8, 24, 0.0);
INSERT INTO `OrderDetails` Values (10533, 73, 15, 24, 0.05);
INSERT INTO `OrderDetails` Values (10534, 30, 25.89, 10, 0.0);
INSERT INTO `OrderDetails` Values (10534, 40, 18.4, 10, 0.2);
INSERT INTO `OrderDetails` Values (10534, 54, 7.45, 10, 0.2);
INSERT INTO `OrderDetails` Values (10535, 11, 21, 50, 0.1);
INSERT INTO `OrderDetails` Values (10535, 40, 18.4, 10, 0.1);
INSERT INTO `OrderDetails` Values (10535, 57, 19.5, 5, 0.1);
INSERT INTO `OrderDetails` Values (10535, 59, 55, 15, 0.1);
INSERT INTO `OrderDetails` Values (10536, 12, 38, 15, 0.25);
INSERT INTO `OrderDetails` Values (10536, 31, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10536, 33, 2.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10536, 60, 34, 35, 0.25);
INSERT INTO `OrderDetails` Values (10537, 31, 12.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10537, 51, 53, 6, 0.0);
INSERT INTO `OrderDetails` Values (10537, 58, 13.25, 20, 0.0);
INSERT INTO `OrderDetails` Values (10537, 72, 34.8, 21, 0.0);
INSERT INTO `OrderDetails` Values (10537, 73, 15, 9, 0.0);
INSERT INTO `OrderDetails` Values (10538, 70, 15, 7, 0.0);
INSERT INTO `OrderDetails` Values (10538, 72, 34.8, 1, 0.0);
INSERT INTO `OrderDetails` Values (10539, 13, 6, 8, 0.0);
INSERT INTO `OrderDetails` Values (10539, 21, 10, 15, 0.0);
INSERT INTO `OrderDetails` Values (10539, 33, 2.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10539, 49, 20, 6, 0.0);
INSERT INTO `OrderDetails` Values (10540, 3, 10, 60, 0.0);
INSERT INTO `OrderDetails` Values (10540, 26, 31.23, 40, 0.0);
INSERT INTO `OrderDetails` Values (10540, 38, 263.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10540, 68, 12.5, 35, 0.0);
INSERT INTO `OrderDetails` Values (10541, 24, 4.5, 35, 0.1);
INSERT INTO `OrderDetails` Values (10541, 38, 263.5, 4, 0.1);
INSERT INTO `OrderDetails` Values (10541, 65, 21.05, 36, 0.1);
INSERT INTO `OrderDetails` Values (10541, 71, 21.5, 9, 0.1);
INSERT INTO `OrderDetails` Values (10542, 11, 21, 15, 0.05);
INSERT INTO `OrderDetails` Values (10542, 54, 7.45, 24, 0.05);
INSERT INTO `OrderDetails` Values (10543, 12, 38, 30, 0.15);
INSERT INTO `OrderDetails` Values (10543, 23, 9, 70, 0.15);
INSERT INTO `OrderDetails` Values (10544, 28, 45.6, 7, 0.0);
INSERT INTO `OrderDetails` Values (10544, 67, 14, 7, 0.0);
INSERT INTO `OrderDetails` Values (10545, 11, 21, 10, 0.0);
INSERT INTO `OrderDetails` Values (10546, 7, 30, 10, 0.0);
INSERT INTO `OrderDetails` Values (10546, 35, 18, 30, 0.0);
INSERT INTO `OrderDetails` Values (10546, 62, 49.3, 40, 0.0);
INSERT INTO `OrderDetails` Values (10547, 32, 32, 24, 0.15);
INSERT INTO `OrderDetails` Values (10547, 36, 19, 60, 0.0);
INSERT INTO `OrderDetails` Values (10548, 34, 14, 10, 0.25);
INSERT INTO `OrderDetails` Values (10548, 41, 9.65, 14, 0.0);
INSERT INTO `OrderDetails` Values (10549, 31, 12.5, 55, 0.15);
INSERT INTO `OrderDetails` Values (10549, 45, 9.5, 100, 0.15);
INSERT INTO `OrderDetails` Values (10549, 51, 53, 48, 0.15);
INSERT INTO `OrderDetails` Values (10550, 17, 39, 8, 0.1);
INSERT INTO `OrderDetails` Values (10550, 19, 9.2, 10, 0.0);
INSERT INTO `OrderDetails` Values (10550, 21, 10, 6, 0.1);
INSERT INTO `OrderDetails` Values (10550, 61, 28.5, 10, 0.1);
INSERT INTO `OrderDetails` Values (10551, 16, 17.45, 40, 0.15);
INSERT INTO `OrderDetails` Values (10551, 35, 18, 20, 0.15);
INSERT INTO `OrderDetails` Values (10551, 44, 19.45, 40, 0.0);
INSERT INTO `OrderDetails` Values (10552, 69, 36, 18, 0.0);
INSERT INTO `OrderDetails` Values (10552, 75, 7.75, 30, 0.0);
INSERT INTO `OrderDetails` Values (10553, 11, 21, 15, 0.0);
INSERT INTO `OrderDetails` Values (10553, 16, 17.45, 14, 0.0);
INSERT INTO `OrderDetails` Values (10553, 22, 21, 24, 0.0);
INSERT INTO `OrderDetails` Values (10553, 31, 12.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10553, 35, 18, 6, 0.0);
INSERT INTO `OrderDetails` Values (10554, 16, 17.45, 30, 0.05);
INSERT INTO `OrderDetails` Values (10554, 23, 9, 20, 0.05);
INSERT INTO `OrderDetails` Values (10554, 62, 49.3, 20, 0.05);
INSERT INTO `OrderDetails` Values (10554, 77, 13, 10, 0.05);
INSERT INTO `OrderDetails` Values (10555, 14, 23.25, 30, 0.2);
INSERT INTO `OrderDetails` Values (10555, 19, 9.2, 35, 0.2);
INSERT INTO `OrderDetails` Values (10555, 24, 4.5, 18, 0.2);
INSERT INTO `OrderDetails` Values (10555, 51, 53, 20, 0.2);
INSERT INTO `OrderDetails` Values (10555, 56, 38, 40, 0.2);
INSERT INTO `OrderDetails` Values (10556, 72, 34.8, 24, 0.0);
INSERT INTO `OrderDetails` Values (10557, 64, 33.25, 30, 0.0);
INSERT INTO `OrderDetails` Values (10557, 75, 7.75, 20, 0.0);
INSERT INTO `OrderDetails` Values (10558, 47, 9.5, 25, 0.0);
INSERT INTO `OrderDetails` Values (10558, 51, 53, 20, 0.0);
INSERT INTO `OrderDetails` Values (10558, 52, 7, 30, 0.0);
INSERT INTO `OrderDetails` Values (10558, 53, 32.8, 18, 0.0);
INSERT INTO `OrderDetails` Values (10558, 73, 15, 3, 0.0);
INSERT INTO `OrderDetails` Values (10559, 41, 9.65, 12, 0.05);
INSERT INTO `OrderDetails` Values (10559, 55, 24, 18, 0.05);
INSERT INTO `OrderDetails` Values (10560, 30, 25.89, 20, 0.0);
INSERT INTO `OrderDetails` Values (10560, 62, 49.3, 15, 0.25);
INSERT INTO `OrderDetails` Values (10561, 44, 19.45, 10, 0.0);
INSERT INTO `OrderDetails` Values (10561, 51, 53, 50, 0.0);
INSERT INTO `OrderDetails` Values (10562, 33, 2.5, 20, 0.1);
INSERT INTO `OrderDetails` Values (10562, 62, 49.3, 10, 0.1);
INSERT INTO `OrderDetails` Values (10563, 36, 19, 25, 0.0);
INSERT INTO `OrderDetails` Values (10563, 52, 7, 70, 0.0);
INSERT INTO `OrderDetails` Values (10564, 17, 39, 16, 0.05);
INSERT INTO `OrderDetails` Values (10564, 31, 12.5, 6, 0.05);
INSERT INTO `OrderDetails` Values (10564, 55, 24, 25, 0.05);
INSERT INTO `OrderDetails` Values (10565, 24, 4.5, 25, 0.1);
INSERT INTO `OrderDetails` Values (10565, 64, 33.25, 18, 0.1);
INSERT INTO `OrderDetails` Values (10566, 11, 21, 35, 0.15);
INSERT INTO `OrderDetails` Values (10566, 18, 62.5, 18, 0.15);
INSERT INTO `OrderDetails` Values (10566, 76, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10567, 31, 12.5, 60, 0.2);
INSERT INTO `OrderDetails` Values (10567, 51, 53, 3, 0.0);
INSERT INTO `OrderDetails` Values (10567, 59, 55, 40, 0.2);
INSERT INTO `OrderDetails` Values (10568, 10, 31, 5, 0.0);
INSERT INTO `OrderDetails` Values (10569, 31, 12.5, 35, 0.2);
INSERT INTO `OrderDetails` Values (10569, 76, 18, 30, 0.0);
INSERT INTO `OrderDetails` Values (10570, 11, 21, 15, 0.05);
INSERT INTO `OrderDetails` Values (10570, 56, 38, 60, 0.05);
INSERT INTO `OrderDetails` Values (10571, 14, 23.25, 11, 0.15);
INSERT INTO `OrderDetails` Values (10571, 42, 14, 28, 0.15);
INSERT INTO `OrderDetails` Values (10572, 16, 17.45, 12, 0.1);
INSERT INTO `OrderDetails` Values (10572, 32, 32, 10, 0.1);
INSERT INTO `OrderDetails` Values (10572, 40, 18.4, 50, 0.0);
INSERT INTO `OrderDetails` Values (10572, 75, 7.75, 15, 0.1);
INSERT INTO `OrderDetails` Values (10573, 17, 39, 18, 0.0);
INSERT INTO `OrderDetails` Values (10573, 34, 14, 40, 0.0);
INSERT INTO `OrderDetails` Values (10573, 53, 32.8, 25, 0.0);
INSERT INTO `OrderDetails` Values (10574, 33, 2.5, 14, 0.0);
INSERT INTO `OrderDetails` Values (10574, 40, 18.4, 2, 0.0);
INSERT INTO `OrderDetails` Values (10574, 62, 49.3, 10, 0.0);
INSERT INTO `OrderDetails` Values (10574, 64, 33.25, 6, 0.0);
INSERT INTO `OrderDetails` Values (10575, 59, 55, 12, 0.0);
INSERT INTO `OrderDetails` Values (10575, 63, 43.9, 6, 0.0);
INSERT INTO `OrderDetails` Values (10575, 72, 34.8, 30, 0.0);
INSERT INTO `OrderDetails` Values (10575, 76, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10576, 1, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10576, 31, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10576, 44, 19.45, 21, 0.0);
INSERT INTO `OrderDetails` Values (10577, 39, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10577, 75, 7.75, 20, 0.0);
INSERT INTO `OrderDetails` Values (10577, 77, 13, 18, 0.0);
INSERT INTO `OrderDetails` Values (10578, 35, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10578, 57, 19.5, 6, 0.0);
INSERT INTO `OrderDetails` Values (10579, 15, 15.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10579, 75, 7.75, 21, 0.0);
INSERT INTO `OrderDetails` Values (10580, 14, 23.25, 15, 0.05);
INSERT INTO `OrderDetails` Values (10580, 41, 9.65, 9, 0.05);
INSERT INTO `OrderDetails` Values (10580, 65, 21.05, 30, 0.05);
INSERT INTO `OrderDetails` Values (10581, 75, 7.75, 50, 0.2);
INSERT INTO `OrderDetails` Values (10582, 57, 19.5, 4, 0.0);
INSERT INTO `OrderDetails` Values (10582, 76, 18, 14, 0.0);
INSERT INTO `OrderDetails` Values (10583, 29, 123.79, 10, 0.0);
INSERT INTO `OrderDetails` Values (10583, 60, 34, 24, 0.15);
INSERT INTO `OrderDetails` Values (10583, 69, 36, 10, 0.15);
INSERT INTO `OrderDetails` Values (10584, 31, 12.5, 50, 0.05);
INSERT INTO `OrderDetails` Values (10585, 47, 9.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10586, 52, 7, 4, 0.15);
INSERT INTO `OrderDetails` Values (10587, 26, 31.23, 6, 0.0);
INSERT INTO `OrderDetails` Values (10587, 35, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10587, 77, 13, 20, 0.0);
INSERT INTO `OrderDetails` Values (10588, 18, 62.5, 40, 0.2);
INSERT INTO `OrderDetails` Values (10588, 42, 14, 100, 0.2);
INSERT INTO `OrderDetails` Values (10589, 35, 18, 4, 0.0);
INSERT INTO `OrderDetails` Values (10590, 1, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10590, 77, 13, 60, 0.05);
INSERT INTO `OrderDetails` Values (10591, 3, 10, 14, 0.0);
INSERT INTO `OrderDetails` Values (10591, 7, 30, 10, 0.0);
INSERT INTO `OrderDetails` Values (10591, 54, 7.45, 50, 0.0);
INSERT INTO `OrderDetails` Values (10592, 15, 15.5, 25, 0.05);
INSERT INTO `OrderDetails` Values (10592, 26, 31.23, 5, 0.05);
INSERT INTO `OrderDetails` Values (10593, 20, 81, 21, 0.2);
INSERT INTO `OrderDetails` Values (10593, 69, 36, 20, 0.2);
INSERT INTO `OrderDetails` Values (10593, 76, 18, 4, 0.2);
INSERT INTO `OrderDetails` Values (10594, 52, 7, 24, 0.0);
INSERT INTO `OrderDetails` Values (10594, 58, 13.25, 30, 0.0);
INSERT INTO `OrderDetails` Values (10595, 35, 18, 30, 0.25);
INSERT INTO `OrderDetails` Values (10595, 61, 28.5, 120, 0.25);
INSERT INTO `OrderDetails` Values (10595, 69, 36, 65, 0.25);
INSERT INTO `OrderDetails` Values (10596, 56, 38, 5, 0.2);
INSERT INTO `OrderDetails` Values (10596, 63, 43.9, 24, 0.2);
INSERT INTO `OrderDetails` Values (10596, 75, 7.75, 30, 0.2);
INSERT INTO `OrderDetails` Values (10597, 24, 4.5, 35, 0.2);
INSERT INTO `OrderDetails` Values (10597, 57, 19.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10597, 65, 21.05, 12, 0.2);
INSERT INTO `OrderDetails` Values (10598, 27, 43.9, 50, 0.0);
INSERT INTO `OrderDetails` Values (10598, 71, 21.5, 9, 0.0);
INSERT INTO `OrderDetails` Values (10599, 62, 49.3, 10, 0.0);
INSERT INTO `OrderDetails` Values (10600, 54, 7.45, 4, 0.0);
INSERT INTO `OrderDetails` Values (10600, 73, 15, 30, 0.0);
INSERT INTO `OrderDetails` Values (10601, 13, 6, 60, 0.0);
INSERT INTO `OrderDetails` Values (10601, 59, 55, 35, 0.0);
INSERT INTO `OrderDetails` Values (10602, 77, 13, 5, 0.25);
INSERT INTO `OrderDetails` Values (10603, 22, 21, 48, 0.0);
INSERT INTO `OrderDetails` Values (10603, 49, 20, 25, 0.05);
INSERT INTO `OrderDetails` Values (10604, 48, 12.75, 6, 0.1);
INSERT INTO `OrderDetails` Values (10604, 76, 18, 10, 0.1);
INSERT INTO `OrderDetails` Values (10605, 16, 17.45, 30, 0.05);
INSERT INTO `OrderDetails` Values (10605, 59, 55, 20, 0.05);
INSERT INTO `OrderDetails` Values (10605, 60, 34, 70, 0.05);
INSERT INTO `OrderDetails` Values (10605, 71, 21.5, 15, 0.05);
INSERT INTO `OrderDetails` Values (10606, 4, 22, 20, 0.2);
INSERT INTO `OrderDetails` Values (10606, 55, 24, 20, 0.2);
INSERT INTO `OrderDetails` Values (10606, 62, 49.3, 10, 0.2);
INSERT INTO `OrderDetails` Values (10607, 7, 30, 45, 0.0);
INSERT INTO `OrderDetails` Values (10607, 17, 39, 100, 0.0);
INSERT INTO `OrderDetails` Values (10607, 33, 2.5, 14, 0.0);
INSERT INTO `OrderDetails` Values (10607, 40, 18.4, 42, 0.0);
INSERT INTO `OrderDetails` Values (10607, 72, 34.8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10608, 56, 38, 28, 0.0);
INSERT INTO `OrderDetails` Values (10609, 1, 18, 3, 0.0);
INSERT INTO `OrderDetails` Values (10609, 10, 31, 10, 0.0);
INSERT INTO `OrderDetails` Values (10609, 21, 10, 6, 0.0);
INSERT INTO `OrderDetails` Values (10610, 36, 19, 21, 0.25);
INSERT INTO `OrderDetails` Values (10611, 1, 18, 6, 0.0);
INSERT INTO `OrderDetails` Values (10611, 2, 19, 10, 0.0);
INSERT INTO `OrderDetails` Values (10611, 60, 34, 15, 0.0);
INSERT INTO `OrderDetails` Values (10612, 10, 31, 70, 0.0);
INSERT INTO `OrderDetails` Values (10612, 36, 19, 55, 0.0);
INSERT INTO `OrderDetails` Values (10612, 49, 20, 18, 0.0);
INSERT INTO `OrderDetails` Values (10612, 60, 34, 40, 0.0);
INSERT INTO `OrderDetails` Values (10612, 76, 18, 80, 0.0);
INSERT INTO `OrderDetails` Values (10613, 13, 6, 8, 0.1);
INSERT INTO `OrderDetails` Values (10613, 75, 7.75, 40, 0.0);
INSERT INTO `OrderDetails` Values (10614, 11, 21, 14, 0.0);
INSERT INTO `OrderDetails` Values (10614, 21, 10, 8, 0.0);
INSERT INTO `OrderDetails` Values (10614, 39, 18, 5, 0.0);
INSERT INTO `OrderDetails` Values (10615, 55, 24, 5, 0.0);
INSERT INTO `OrderDetails` Values (10616, 38, 263.5, 15, 0.05);
INSERT INTO `OrderDetails` Values (10616, 56, 38, 14, 0.0);
INSERT INTO `OrderDetails` Values (10616, 70, 15, 15, 0.05);
INSERT INTO `OrderDetails` Values (10616, 71, 21.5, 15, 0.05);
INSERT INTO `OrderDetails` Values (10617, 59, 55, 30, 0.15);
INSERT INTO `OrderDetails` Values (10618, 6, 25, 70, 0.0);
INSERT INTO `OrderDetails` Values (10618, 56, 38, 20, 0.0);
INSERT INTO `OrderDetails` Values (10618, 68, 12.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10619, 21, 10, 42, 0.0);
INSERT INTO `OrderDetails` Values (10619, 22, 21, 40, 0.0);
INSERT INTO `OrderDetails` Values (10620, 24, 4.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10620, 52, 7, 5, 0.0);
INSERT INTO `OrderDetails` Values (10621, 19, 9.2, 5, 0.0);
INSERT INTO `OrderDetails` Values (10621, 23, 9, 10, 0.0);
INSERT INTO `OrderDetails` Values (10621, 70, 15, 20, 0.0);
INSERT INTO `OrderDetails` Values (10621, 71, 21.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10622, 2, 19, 20, 0.0);
INSERT INTO `OrderDetails` Values (10622, 68, 12.5, 18, 0.2);
INSERT INTO `OrderDetails` Values (10623, 14, 23.25, 21, 0.0);
INSERT INTO `OrderDetails` Values (10623, 19, 9.2, 15, 0.1);
INSERT INTO `OrderDetails` Values (10623, 21, 10, 25, 0.1);
INSERT INTO `OrderDetails` Values (10623, 24, 4.5, 3, 0.0);
INSERT INTO `OrderDetails` Values (10623, 35, 18, 30, 0.1);
INSERT INTO `OrderDetails` Values (10624, 28, 45.6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10624, 29, 123.79, 6, 0.0);
INSERT INTO `OrderDetails` Values (10624, 44, 19.45, 10, 0.0);
INSERT INTO `OrderDetails` Values (10625, 14, 23.25, 3, 0.0);
INSERT INTO `OrderDetails` Values (10625, 42, 14, 5, 0.0);
INSERT INTO `OrderDetails` Values (10625, 60, 34, 10, 0.0);
INSERT INTO `OrderDetails` Values (10626, 53, 32.8, 12, 0.0);
INSERT INTO `OrderDetails` Values (10626, 60, 34, 20, 0.0);
INSERT INTO `OrderDetails` Values (10626, 71, 21.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10627, 62, 49.3, 15, 0.0);
INSERT INTO `OrderDetails` Values (10627, 73, 15, 35, 0.15);
INSERT INTO `OrderDetails` Values (10628, 1, 18, 25, 0.0);
INSERT INTO `OrderDetails` Values (10629, 29, 123.79, 20, 0.0);
INSERT INTO `OrderDetails` Values (10629, 64, 33.25, 9, 0.0);
INSERT INTO `OrderDetails` Values (10630, 55, 24, 12, 0.05);
INSERT INTO `OrderDetails` Values (10630, 76, 18, 35, 0.0);
INSERT INTO `OrderDetails` Values (10631, 75, 7.75, 8, 0.1);
INSERT INTO `OrderDetails` Values (10632, 2, 19, 30, 0.05);
INSERT INTO `OrderDetails` Values (10632, 33, 2.5, 20, 0.05);
INSERT INTO `OrderDetails` Values (10633, 12, 38, 36, 0.15);
INSERT INTO `OrderDetails` Values (10633, 13, 6, 13, 0.15);
INSERT INTO `OrderDetails` Values (10633, 26, 31.23, 35, 0.15);
INSERT INTO `OrderDetails` Values (10633, 62, 49.3, 80, 0.15);
INSERT INTO `OrderDetails` Values (10634, 7, 30, 35, 0.0);
INSERT INTO `OrderDetails` Values (10634, 18, 62.5, 50, 0.0);
INSERT INTO `OrderDetails` Values (10634, 51, 53, 15, 0.0);
INSERT INTO `OrderDetails` Values (10634, 75, 7.75, 2, 0.0);
INSERT INTO `OrderDetails` Values (10635, 4, 22, 10, 0.1);
INSERT INTO `OrderDetails` Values (10635, 5, 21.35, 15, 0.1);
INSERT INTO `OrderDetails` Values (10635, 22, 21, 40, 0.0);
INSERT INTO `OrderDetails` Values (10636, 4, 22, 25, 0.0);
INSERT INTO `OrderDetails` Values (10636, 58, 13.25, 6, 0.0);
INSERT INTO `OrderDetails` Values (10637, 11, 21, 10, 0.0);
INSERT INTO `OrderDetails` Values (10637, 50, 16.25, 25, 0.05);
INSERT INTO `OrderDetails` Values (10637, 56, 38, 60, 0.05);
INSERT INTO `OrderDetails` Values (10638, 45, 9.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10638, 65, 21.05, 21, 0.0);
INSERT INTO `OrderDetails` Values (10638, 72, 34.8, 60, 0.0);
INSERT INTO `OrderDetails` Values (10639, 18, 62.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (10640, 69, 36, 20, 0.25);
INSERT INTO `OrderDetails` Values (10640, 70, 15, 15, 0.25);
INSERT INTO `OrderDetails` Values (10641, 2, 19, 50, 0.0);
INSERT INTO `OrderDetails` Values (10641, 40, 18.4, 60, 0.0);
INSERT INTO `OrderDetails` Values (10642, 21, 10, 30, 0.2);
INSERT INTO `OrderDetails` Values (10642, 61, 28.5, 20, 0.2);
INSERT INTO `OrderDetails` Values (10643, 28, 45.6, 15, 0.25);
INSERT INTO `OrderDetails` Values (10643, 39, 18, 21, 0.25);
INSERT INTO `OrderDetails` Values (10643, 46, 12, 2, 0.25);
INSERT INTO `OrderDetails` Values (10644, 18, 62.5, 4, 0.1);
INSERT INTO `OrderDetails` Values (10644, 43, 46, 20, 0.0);
INSERT INTO `OrderDetails` Values (10644, 46, 12, 21, 0.1);
INSERT INTO `OrderDetails` Values (10645, 18, 62.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10645, 36, 19, 15, 0.0);
INSERT INTO `OrderDetails` Values (10646, 1, 18, 15, 0.25);
INSERT INTO `OrderDetails` Values (10646, 10, 31, 18, 0.25);
INSERT INTO `OrderDetails` Values (10646, 71, 21.5, 30, 0.25);
INSERT INTO `OrderDetails` Values (10646, 77, 13, 35, 0.25);
INSERT INTO `OrderDetails` Values (10647, 19, 9.2, 30, 0.0);
INSERT INTO `OrderDetails` Values (10647, 39, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10648, 22, 21, 15, 0.0);
INSERT INTO `OrderDetails` Values (10648, 24, 4.5, 15, 0.15);
INSERT INTO `OrderDetails` Values (10649, 28, 45.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10649, 72, 34.8, 15, 0.0);
INSERT INTO `OrderDetails` Values (10650, 30, 25.89, 30, 0.0);
INSERT INTO `OrderDetails` Values (10650, 53, 32.8, 25, 0.05);
INSERT INTO `OrderDetails` Values (10650, 54, 7.45, 30, 0.0);
INSERT INTO `OrderDetails` Values (10651, 19, 9.2, 12, 0.25);
INSERT INTO `OrderDetails` Values (10651, 22, 21, 20, 0.25);
INSERT INTO `OrderDetails` Values (10652, 30, 25.89, 2, 0.25);
INSERT INTO `OrderDetails` Values (10652, 42, 14, 20, 0.0);
INSERT INTO `OrderDetails` Values (10653, 16, 17.45, 30, 0.1);
INSERT INTO `OrderDetails` Values (10653, 60, 34, 20, 0.1);
INSERT INTO `OrderDetails` Values (10654, 4, 22, 12, 0.1);
INSERT INTO `OrderDetails` Values (10654, 39, 18, 20, 0.1);
INSERT INTO `OrderDetails` Values (10654, 54, 7.45, 6, 0.1);
INSERT INTO `OrderDetails` Values (10655, 41, 9.65, 20, 0.2);
INSERT INTO `OrderDetails` Values (10656, 14, 23.25, 3, 0.1);
INSERT INTO `OrderDetails` Values (10656, 44, 19.45, 28, 0.1);
INSERT INTO `OrderDetails` Values (10656, 47, 9.5, 6, 0.1);
INSERT INTO `OrderDetails` Values (10657, 15, 15.5, 50, 0.0);
INSERT INTO `OrderDetails` Values (10657, 41, 9.65, 24, 0.0);
INSERT INTO `OrderDetails` Values (10657, 46, 12, 45, 0.0);
INSERT INTO `OrderDetails` Values (10657, 47, 9.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10657, 56, 38, 45, 0.0);
INSERT INTO `OrderDetails` Values (10657, 60, 34, 30, 0.0);
INSERT INTO `OrderDetails` Values (10658, 21, 10, 60, 0.0);
INSERT INTO `OrderDetails` Values (10658, 40, 18.4, 70, 0.05);
INSERT INTO `OrderDetails` Values (10658, 60, 34, 55, 0.05);
INSERT INTO `OrderDetails` Values (10658, 77, 13, 70, 0.05);
INSERT INTO `OrderDetails` Values (10659, 31, 12.5, 20, 0.05);
INSERT INTO `OrderDetails` Values (10659, 40, 18.4, 24, 0.05);
INSERT INTO `OrderDetails` Values (10659, 70, 15, 40, 0.05);
INSERT INTO `OrderDetails` Values (10660, 20, 81, 21, 0.0);
INSERT INTO `OrderDetails` Values (10661, 39, 18, 3, 0.2);
INSERT INTO `OrderDetails` Values (10661, 58, 13.25, 49, 0.2);
INSERT INTO `OrderDetails` Values (10662, 68, 12.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10663, 40, 18.4, 30, 0.05);
INSERT INTO `OrderDetails` Values (10663, 42, 14, 30, 0.05);
INSERT INTO `OrderDetails` Values (10663, 51, 53, 20, 0.05);
INSERT INTO `OrderDetails` Values (10664, 10, 31, 24, 0.15);
INSERT INTO `OrderDetails` Values (10664, 56, 38, 12, 0.15);
INSERT INTO `OrderDetails` Values (10664, 65, 21.05, 15, 0.15);
INSERT INTO `OrderDetails` Values (10665, 51, 53, 20, 0.0);
INSERT INTO `OrderDetails` Values (10665, 59, 55, 1, 0.0);
INSERT INTO `OrderDetails` Values (10665, 76, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10666, 29, 123.79, 36, 0.0);
INSERT INTO `OrderDetails` Values (10666, 65, 21.05, 10, 0.0);
INSERT INTO `OrderDetails` Values (10667, 69, 36, 45, 0.2);
INSERT INTO `OrderDetails` Values (10667, 71, 21.5, 14, 0.2);
INSERT INTO `OrderDetails` Values (10668, 31, 12.5, 8, 0.1);
INSERT INTO `OrderDetails` Values (10668, 55, 24, 4, 0.1);
INSERT INTO `OrderDetails` Values (10668, 64, 33.25, 15, 0.1);
INSERT INTO `OrderDetails` Values (10669, 36, 19, 30, 0.0);
INSERT INTO `OrderDetails` Values (10670, 23, 9, 32, 0.0);
INSERT INTO `OrderDetails` Values (10670, 46, 12, 60, 0.0);
INSERT INTO `OrderDetails` Values (10670, 67, 14, 25, 0.0);
INSERT INTO `OrderDetails` Values (10670, 73, 15, 50, 0.0);
INSERT INTO `OrderDetails` Values (10670, 75, 7.75, 25, 0.0);
INSERT INTO `OrderDetails` Values (10671, 16, 17.45, 10, 0.0);
INSERT INTO `OrderDetails` Values (10671, 62, 49.3, 10, 0.0);
INSERT INTO `OrderDetails` Values (10671, 65, 21.05, 12, 0.0);
INSERT INTO `OrderDetails` Values (10672, 38, 263.5, 15, 0.1);
INSERT INTO `OrderDetails` Values (10672, 71, 21.5, 12, 0.0);
INSERT INTO `OrderDetails` Values (10673, 16, 17.45, 3, 0.0);
INSERT INTO `OrderDetails` Values (10673, 42, 14, 6, 0.0);
INSERT INTO `OrderDetails` Values (10673, 43, 46, 6, 0.0);
INSERT INTO `OrderDetails` Values (10674, 23, 9, 5, 0.0);
INSERT INTO `OrderDetails` Values (10675, 14, 23.25, 30, 0.0);
INSERT INTO `OrderDetails` Values (10675, 53, 32.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10675, 58, 13.25, 30, 0.0);
INSERT INTO `OrderDetails` Values (10676, 10, 31, 2, 0.0);
INSERT INTO `OrderDetails` Values (10676, 19, 9.2, 7, 0.0);
INSERT INTO `OrderDetails` Values (10676, 44, 19.45, 21, 0.0);
INSERT INTO `OrderDetails` Values (10677, 26, 31.23, 30, 0.15);
INSERT INTO `OrderDetails` Values (10677, 33, 2.5, 8, 0.15);
INSERT INTO `OrderDetails` Values (10678, 12, 38, 100, 0.0);
INSERT INTO `OrderDetails` Values (10678, 33, 2.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10678, 41, 9.65, 120, 0.0);
INSERT INTO `OrderDetails` Values (10678, 54, 7.45, 30, 0.0);
INSERT INTO `OrderDetails` Values (10679, 59, 55, 12, 0.0);
INSERT INTO `OrderDetails` Values (10680, 16, 17.45, 50, 0.25);
INSERT INTO `OrderDetails` Values (10680, 31, 12.5, 20, 0.25);
INSERT INTO `OrderDetails` Values (10680, 42, 14, 40, 0.25);
INSERT INTO `OrderDetails` Values (10681, 19, 9.2, 30, 0.1);
INSERT INTO `OrderDetails` Values (10681, 21, 10, 12, 0.1);
INSERT INTO `OrderDetails` Values (10681, 64, 33.25, 28, 0.0);
INSERT INTO `OrderDetails` Values (10682, 33, 2.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10682, 66, 17, 4, 0.0);
INSERT INTO `OrderDetails` Values (10682, 75, 7.75, 30, 0.0);
INSERT INTO `OrderDetails` Values (10683, 52, 7, 9, 0.0);
INSERT INTO `OrderDetails` Values (10684, 40, 18.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10684, 47, 9.5, 40, 0.0);
INSERT INTO `OrderDetails` Values (10684, 60, 34, 30, 0.0);
INSERT INTO `OrderDetails` Values (10685, 10, 31, 20, 0.0);
INSERT INTO `OrderDetails` Values (10685, 41, 9.65, 4, 0.0);
INSERT INTO `OrderDetails` Values (10685, 47, 9.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10686, 17, 39, 30, 0.2);
INSERT INTO `OrderDetails` Values (10686, 26, 31.23, 15, 0.0);
INSERT INTO `OrderDetails` Values (10687, 9, 97, 50, 0.25);
INSERT INTO `OrderDetails` Values (10687, 29, 123.79, 10, 0.0);
INSERT INTO `OrderDetails` Values (10687, 36, 19, 6, 0.25);
INSERT INTO `OrderDetails` Values (10688, 10, 31, 18, 0.1);
INSERT INTO `OrderDetails` Values (10688, 28, 45.6, 60, 0.1);
INSERT INTO `OrderDetails` Values (10688, 34, 14, 14, 0.0);
INSERT INTO `OrderDetails` Values (10689, 1, 18, 35, 0.25);
INSERT INTO `OrderDetails` Values (10690, 56, 38, 20, 0.25);
INSERT INTO `OrderDetails` Values (10690, 77, 13, 30, 0.25);
INSERT INTO `OrderDetails` Values (10691, 1, 18, 30, 0.0);
INSERT INTO `OrderDetails` Values (10691, 29, 123.79, 40, 0.0);
INSERT INTO `OrderDetails` Values (10691, 43, 46, 40, 0.0);
INSERT INTO `OrderDetails` Values (10691, 44, 19.45, 24, 0.0);
INSERT INTO `OrderDetails` Values (10691, 62, 49.3, 48, 0.0);
INSERT INTO `OrderDetails` Values (10692, 63, 43.9, 20, 0.0);
INSERT INTO `OrderDetails` Values (10693, 9, 97, 6, 0.0);
INSERT INTO `OrderDetails` Values (10693, 54, 7.45, 60, 0.15);
INSERT INTO `OrderDetails` Values (10693, 69, 36, 30, 0.15);
INSERT INTO `OrderDetails` Values (10693, 73, 15, 15, 0.15);
INSERT INTO `OrderDetails` Values (10694, 7, 30, 90, 0.0);
INSERT INTO `OrderDetails` Values (10694, 59, 55, 25, 0.0);
INSERT INTO `OrderDetails` Values (10694, 70, 15, 50, 0.0);
INSERT INTO `OrderDetails` Values (10695, 8, 40, 10, 0.0);
INSERT INTO `OrderDetails` Values (10695, 12, 38, 4, 0.0);
INSERT INTO `OrderDetails` Values (10695, 24, 4.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10696, 17, 39, 20, 0.0);
INSERT INTO `OrderDetails` Values (10696, 46, 12, 18, 0.0);
INSERT INTO `OrderDetails` Values (10697, 19, 9.2, 7, 0.25);
INSERT INTO `OrderDetails` Values (10697, 35, 18, 9, 0.25);
INSERT INTO `OrderDetails` Values (10697, 58, 13.25, 30, 0.25);
INSERT INTO `OrderDetails` Values (10697, 70, 15, 30, 0.25);
INSERT INTO `OrderDetails` Values (10698, 11, 21, 15, 0.0);
INSERT INTO `OrderDetails` Values (10698, 17, 39, 8, 0.05);
INSERT INTO `OrderDetails` Values (10698, 29, 123.79, 12, 0.05);
INSERT INTO `OrderDetails` Values (10698, 65, 21.05, 65, 0.05);
INSERT INTO `OrderDetails` Values (10698, 70, 15, 8, 0.05);
INSERT INTO `OrderDetails` Values (10699, 47, 9.5, 12, 0.0);
INSERT INTO `OrderDetails` Values (10700, 1, 18, 5, 0.2);
INSERT INTO `OrderDetails` Values (10700, 34, 14, 12, 0.2);
INSERT INTO `OrderDetails` Values (10700, 68, 12.5, 40, 0.2);
INSERT INTO `OrderDetails` Values (10700, 71, 21.5, 60, 0.2);
INSERT INTO `OrderDetails` Values (10701, 59, 55, 42, 0.15);
INSERT INTO `OrderDetails` Values (10701, 71, 21.5, 20, 0.15);
INSERT INTO `OrderDetails` Values (10701, 76, 18, 35, 0.15);
INSERT INTO `OrderDetails` Values (10702, 3, 10, 6, 0.0);
INSERT INTO `OrderDetails` Values (10702, 76, 18, 15, 0.0);
INSERT INTO `OrderDetails` Values (10703, 2, 19, 5, 0.0);
INSERT INTO `OrderDetails` Values (10703, 59, 55, 35, 0.0);
INSERT INTO `OrderDetails` Values (10703, 73, 15, 35, 0.0);
INSERT INTO `OrderDetails` Values (10704, 4, 22, 6, 0.0);
INSERT INTO `OrderDetails` Values (10704, 24, 4.5, 35, 0.0);
INSERT INTO `OrderDetails` Values (10704, 48, 12.75, 24, 0.0);
INSERT INTO `OrderDetails` Values (10705, 31, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10705, 32, 32, 4, 0.0);
INSERT INTO `OrderDetails` Values (10706, 16, 17.45, 20, 0.0);
INSERT INTO `OrderDetails` Values (10706, 43, 46, 24, 0.0);
INSERT INTO `OrderDetails` Values (10706, 59, 55, 8, 0.0);
INSERT INTO `OrderDetails` Values (10707, 55, 24, 21, 0.0);
INSERT INTO `OrderDetails` Values (10707, 57, 19.5, 40, 0.0);
INSERT INTO `OrderDetails` Values (10707, 70, 15, 28, 0.15);
INSERT INTO `OrderDetails` Values (10708, 5, 21.35, 4, 0.0);
INSERT INTO `OrderDetails` Values (10708, 36, 19, 5, 0.0);
INSERT INTO `OrderDetails` Values (10709, 8, 40, 40, 0.0);
INSERT INTO `OrderDetails` Values (10709, 51, 53, 28, 0.0);
INSERT INTO `OrderDetails` Values (10709, 60, 34, 10, 0.0);
INSERT INTO `OrderDetails` Values (10710, 19, 9.2, 5, 0.0);
INSERT INTO `OrderDetails` Values (10710, 47, 9.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10711, 19, 9.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10711, 41, 9.65, 42, 0.0);
INSERT INTO `OrderDetails` Values (10711, 53, 32.8, 120, 0.0);
INSERT INTO `OrderDetails` Values (10712, 53, 32.8, 3, 0.05);
INSERT INTO `OrderDetails` Values (10712, 56, 38, 30, 0.0);
INSERT INTO `OrderDetails` Values (10713, 10, 31, 18, 0.0);
INSERT INTO `OrderDetails` Values (10713, 26, 31.23, 30, 0.0);
INSERT INTO `OrderDetails` Values (10713, 45, 9.5, 110, 0.0);
INSERT INTO `OrderDetails` Values (10713, 46, 12, 24, 0.0);
INSERT INTO `OrderDetails` Values (10714, 2, 19, 30, 0.25);
INSERT INTO `OrderDetails` Values (10714, 17, 39, 27, 0.25);
INSERT INTO `OrderDetails` Values (10714, 47, 9.5, 50, 0.25);
INSERT INTO `OrderDetails` Values (10714, 56, 38, 18, 0.25);
INSERT INTO `OrderDetails` Values (10714, 58, 13.25, 12, 0.25);
INSERT INTO `OrderDetails` Values (10715, 10, 31, 21, 0.0);
INSERT INTO `OrderDetails` Values (10715, 71, 21.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10716, 21, 10, 5, 0.0);
INSERT INTO `OrderDetails` Values (10716, 51, 53, 7, 0.0);
INSERT INTO `OrderDetails` Values (10716, 61, 28.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10717, 21, 10, 32, 0.05);
INSERT INTO `OrderDetails` Values (10717, 54, 7.45, 15, 0.0);
INSERT INTO `OrderDetails` Values (10717, 69, 36, 25, 0.05);
INSERT INTO `OrderDetails` Values (10718, 12, 38, 36, 0.0);
INSERT INTO `OrderDetails` Values (10718, 16, 17.45, 20, 0.0);
INSERT INTO `OrderDetails` Values (10718, 36, 19, 40, 0.0);
INSERT INTO `OrderDetails` Values (10718, 62, 49.3, 20, 0.0);
INSERT INTO `OrderDetails` Values (10719, 18, 62.5, 12, 0.25);
INSERT INTO `OrderDetails` Values (10719, 30, 25.89, 3, 0.25);
INSERT INTO `OrderDetails` Values (10719, 54, 7.45, 40, 0.25);
INSERT INTO `OrderDetails` Values (10720, 35, 18, 21, 0.0);
INSERT INTO `OrderDetails` Values (10720, 71, 21.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (10721, 44, 19.45, 50, 0.05);
INSERT INTO `OrderDetails` Values (10722, 2, 19, 3, 0.0);
INSERT INTO `OrderDetails` Values (10722, 31, 12.5, 50, 0.0);
INSERT INTO `OrderDetails` Values (10722, 68, 12.5, 45, 0.0);
INSERT INTO `OrderDetails` Values (10722, 75, 7.75, 42, 0.0);
INSERT INTO `OrderDetails` Values (10723, 26, 31.23, 15, 0.0);
INSERT INTO `OrderDetails` Values (10724, 10, 31, 16, 0.0);
INSERT INTO `OrderDetails` Values (10724, 61, 28.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10725, 41, 9.65, 12, 0.0);
INSERT INTO `OrderDetails` Values (10725, 52, 7, 4, 0.0);
INSERT INTO `OrderDetails` Values (10725, 55, 24, 6, 0.0);
INSERT INTO `OrderDetails` Values (10726, 4, 22, 25, 0.0);
INSERT INTO `OrderDetails` Values (10726, 11, 21, 5, 0.0);
INSERT INTO `OrderDetails` Values (10727, 17, 39, 20, 0.05);
INSERT INTO `OrderDetails` Values (10727, 56, 38, 10, 0.05);
INSERT INTO `OrderDetails` Values (10727, 59, 55, 10, 0.05);
INSERT INTO `OrderDetails` Values (10728, 30, 25.89, 15, 0.0);
INSERT INTO `OrderDetails` Values (10728, 40, 18.4, 6, 0.0);
INSERT INTO `OrderDetails` Values (10728, 55, 24, 12, 0.0);
INSERT INTO `OrderDetails` Values (10728, 60, 34, 15, 0.0);
INSERT INTO `OrderDetails` Values (10729, 1, 18, 50, 0.0);
INSERT INTO `OrderDetails` Values (10729, 21, 10, 30, 0.0);
INSERT INTO `OrderDetails` Values (10729, 50, 16.25, 40, 0.0);
INSERT INTO `OrderDetails` Values (10730, 16, 17.45, 15, 0.05);
INSERT INTO `OrderDetails` Values (10730, 31, 12.5, 3, 0.05);
INSERT INTO `OrderDetails` Values (10730, 65, 21.05, 10, 0.05);
INSERT INTO `OrderDetails` Values (10731, 21, 10, 40, 0.05);
INSERT INTO `OrderDetails` Values (10731, 51, 53, 30, 0.05);
INSERT INTO `OrderDetails` Values (10732, 76, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10733, 14, 23.25, 16, 0.0);
INSERT INTO `OrderDetails` Values (10733, 28, 45.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10733, 52, 7, 25, 0.0);
INSERT INTO `OrderDetails` Values (10734, 6, 25, 30, 0.0);
INSERT INTO `OrderDetails` Values (10734, 30, 25.89, 15, 0.0);
INSERT INTO `OrderDetails` Values (10734, 76, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10735, 61, 28.5, 20, 0.1);
INSERT INTO `OrderDetails` Values (10735, 77, 13, 2, 0.1);
INSERT INTO `OrderDetails` Values (10736, 65, 21.05, 40, 0.0);
INSERT INTO `OrderDetails` Values (10736, 75, 7.75, 20, 0.0);
INSERT INTO `OrderDetails` Values (10737, 13, 6, 4, 0.0);
INSERT INTO `OrderDetails` Values (10737, 41, 9.65, 12, 0.0);
INSERT INTO `OrderDetails` Values (10738, 16, 17.45, 3, 0.0);
INSERT INTO `OrderDetails` Values (10739, 36, 19, 6, 0.0);
INSERT INTO `OrderDetails` Values (10739, 52, 7, 18, 0.0);
INSERT INTO `OrderDetails` Values (10740, 28, 45.6, 5, 0.2);
INSERT INTO `OrderDetails` Values (10740, 35, 18, 35, 0.2);
INSERT INTO `OrderDetails` Values (10740, 45, 9.5, 40, 0.2);
INSERT INTO `OrderDetails` Values (10740, 56, 38, 14, 0.2);
INSERT INTO `OrderDetails` Values (10741, 2, 19, 15, 0.2);
INSERT INTO `OrderDetails` Values (10742, 3, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10742, 60, 34, 50, 0.0);
INSERT INTO `OrderDetails` Values (10742, 72, 34.8, 35, 0.0);
INSERT INTO `OrderDetails` Values (10743, 46, 12, 28, 0.05);
INSERT INTO `OrderDetails` Values (10744, 40, 18.4, 50, 0.2);
INSERT INTO `OrderDetails` Values (10745, 18, 62.5, 24, 0.0);
INSERT INTO `OrderDetails` Values (10745, 44, 19.45, 16, 0.0);
INSERT INTO `OrderDetails` Values (10745, 59, 55, 45, 0.0);
INSERT INTO `OrderDetails` Values (10745, 72, 34.8, 7, 0.0);
INSERT INTO `OrderDetails` Values (10746, 13, 6, 6, 0.0);
INSERT INTO `OrderDetails` Values (10746, 42, 14, 28, 0.0);
INSERT INTO `OrderDetails` Values (10746, 62, 49.3, 9, 0.0);
INSERT INTO `OrderDetails` Values (10746, 69, 36, 40, 0.0);
INSERT INTO `OrderDetails` Values (10747, 31, 12.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (10747, 41, 9.65, 35, 0.0);
INSERT INTO `OrderDetails` Values (10747, 63, 43.9, 9, 0.0);
INSERT INTO `OrderDetails` Values (10747, 69, 36, 30, 0.0);
INSERT INTO `OrderDetails` Values (10748, 23, 9, 44, 0.0);
INSERT INTO `OrderDetails` Values (10748, 40, 18.4, 40, 0.0);
INSERT INTO `OrderDetails` Values (10748, 56, 38, 28, 0.0);
INSERT INTO `OrderDetails` Values (10749, 56, 38, 15, 0.0);
INSERT INTO `OrderDetails` Values (10749, 59, 55, 6, 0.0);
INSERT INTO `OrderDetails` Values (10749, 76, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10750, 14, 23.25, 5, 0.15);
INSERT INTO `OrderDetails` Values (10750, 45, 9.5, 40, 0.15);
INSERT INTO `OrderDetails` Values (10750, 59, 55, 25, 0.15);
INSERT INTO `OrderDetails` Values (10751, 26, 31.23, 12, 0.1);
INSERT INTO `OrderDetails` Values (10751, 30, 25.89, 30, 0.0);
INSERT INTO `OrderDetails` Values (10751, 50, 16.25, 20, 0.1);
INSERT INTO `OrderDetails` Values (10751, 73, 15, 15, 0.0);
INSERT INTO `OrderDetails` Values (10752, 1, 18, 8, 0.0);
INSERT INTO `OrderDetails` Values (10752, 69, 36, 3, 0.0);
INSERT INTO `OrderDetails` Values (10753, 45, 9.5, 4, 0.0);
INSERT INTO `OrderDetails` Values (10753, 74, 10, 5, 0.0);
INSERT INTO `OrderDetails` Values (10754, 40, 18.4, 3, 0.0);
INSERT INTO `OrderDetails` Values (10755, 47, 9.5, 30, 0.25);
INSERT INTO `OrderDetails` Values (10755, 56, 38, 30, 0.25);
INSERT INTO `OrderDetails` Values (10755, 57, 19.5, 14, 0.25);
INSERT INTO `OrderDetails` Values (10755, 69, 36, 25, 0.25);
INSERT INTO `OrderDetails` Values (10756, 18, 62.5, 21, 0.2);
INSERT INTO `OrderDetails` Values (10756, 36, 19, 20, 0.2);
INSERT INTO `OrderDetails` Values (10756, 68, 12.5, 6, 0.2);
INSERT INTO `OrderDetails` Values (10756, 69, 36, 20, 0.2);
INSERT INTO `OrderDetails` Values (10757, 34, 14, 30, 0.0);
INSERT INTO `OrderDetails` Values (10757, 59, 55, 7, 0.0);
INSERT INTO `OrderDetails` Values (10757, 62, 49.3, 30, 0.0);
INSERT INTO `OrderDetails` Values (10757, 64, 33.25, 24, 0.0);
INSERT INTO `OrderDetails` Values (10758, 26, 31.23, 20, 0.0);
INSERT INTO `OrderDetails` Values (10758, 52, 7, 60, 0.0);
INSERT INTO `OrderDetails` Values (10758, 70, 15, 40, 0.0);
INSERT INTO `OrderDetails` Values (10759, 32, 32, 10, 0.0);
INSERT INTO `OrderDetails` Values (10760, 25, 14, 12, 0.25);
INSERT INTO `OrderDetails` Values (10760, 27, 43.9, 40, 0.0);
INSERT INTO `OrderDetails` Values (10760, 43, 46, 30, 0.25);
INSERT INTO `OrderDetails` Values (10761, 25, 14, 35, 0.25);
INSERT INTO `OrderDetails` Values (10761, 75, 7.75, 18, 0.0);
INSERT INTO `OrderDetails` Values (10762, 39, 18, 16, 0.0);
INSERT INTO `OrderDetails` Values (10762, 47, 9.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10762, 51, 53, 28, 0.0);
INSERT INTO `OrderDetails` Values (10762, 56, 38, 60, 0.0);
INSERT INTO `OrderDetails` Values (10763, 21, 10, 40, 0.0);
INSERT INTO `OrderDetails` Values (10763, 22, 21, 6, 0.0);
INSERT INTO `OrderDetails` Values (10763, 24, 4.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10764, 3, 10, 20, 0.1);
INSERT INTO `OrderDetails` Values (10764, 39, 18, 130, 0.1);
INSERT INTO `OrderDetails` Values (10765, 65, 21.05, 80, 0.1);
INSERT INTO `OrderDetails` Values (10766, 2, 19, 40, 0.0);
INSERT INTO `OrderDetails` Values (10766, 7, 30, 35, 0.0);
INSERT INTO `OrderDetails` Values (10766, 68, 12.5, 40, 0.0);
INSERT INTO `OrderDetails` Values (10767, 42, 14, 2, 0.0);
INSERT INTO `OrderDetails` Values (10768, 22, 21, 4, 0.0);
INSERT INTO `OrderDetails` Values (10768, 31, 12.5, 50, 0.0);
INSERT INTO `OrderDetails` Values (10768, 60, 34, 15, 0.0);
INSERT INTO `OrderDetails` Values (10768, 71, 21.5, 12, 0.0);
INSERT INTO `OrderDetails` Values (10769, 41, 9.65, 30, 0.05);
INSERT INTO `OrderDetails` Values (10769, 52, 7, 15, 0.05);
INSERT INTO `OrderDetails` Values (10769, 61, 28.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10769, 62, 49.3, 15, 0.0);
INSERT INTO `OrderDetails` Values (10770, 11, 21, 15, 0.25);
INSERT INTO `OrderDetails` Values (10771, 71, 21.5, 16, 0.0);
INSERT INTO `OrderDetails` Values (10772, 29, 123.79, 18, 0.0);
INSERT INTO `OrderDetails` Values (10772, 59, 55, 25, 0.0);
INSERT INTO `OrderDetails` Values (10773, 17, 39, 33, 0.0);
INSERT INTO `OrderDetails` Values (10773, 31, 12.5, 70, 0.2);
INSERT INTO `OrderDetails` Values (10773, 75, 7.75, 7, 0.2);
INSERT INTO `OrderDetails` Values (10774, 31, 12.5, 2, 0.25);
INSERT INTO `OrderDetails` Values (10774, 66, 17, 50, 0.0);
INSERT INTO `OrderDetails` Values (10775, 10, 31, 6, 0.0);
INSERT INTO `OrderDetails` Values (10775, 67, 14, 3, 0.0);
INSERT INTO `OrderDetails` Values (10776, 31, 12.5, 16, 0.05);
INSERT INTO `OrderDetails` Values (10776, 42, 14, 12, 0.05);
INSERT INTO `OrderDetails` Values (10776, 45, 9.5, 27, 0.05);
INSERT INTO `OrderDetails` Values (10776, 51, 53, 120, 0.05);
INSERT INTO `OrderDetails` Values (10777, 42, 14, 20, 0.2);
INSERT INTO `OrderDetails` Values (10778, 41, 9.65, 10, 0.0);
INSERT INTO `OrderDetails` Values (10779, 16, 17.45, 20, 0.0);
INSERT INTO `OrderDetails` Values (10779, 62, 49.3, 20, 0.0);
INSERT INTO `OrderDetails` Values (10780, 70, 15, 35, 0.0);
INSERT INTO `OrderDetails` Values (10780, 77, 13, 15, 0.0);
INSERT INTO `OrderDetails` Values (10781, 54, 7.45, 3, 0.2);
INSERT INTO `OrderDetails` Values (10781, 56, 38, 20, 0.2);
INSERT INTO `OrderDetails` Values (10781, 74, 10, 35, 0.0);
INSERT INTO `OrderDetails` Values (10782, 31, 12.5, 1, 0.0);
INSERT INTO `OrderDetails` Values (10783, 31, 12.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10783, 38, 263.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10784, 36, 19, 30, 0.0);
INSERT INTO `OrderDetails` Values (10784, 39, 18, 2, 0.15);
INSERT INTO `OrderDetails` Values (10784, 72, 34.8, 30, 0.15);
INSERT INTO `OrderDetails` Values (10785, 10, 31, 10, 0.0);
INSERT INTO `OrderDetails` Values (10785, 75, 7.75, 10, 0.0);
INSERT INTO `OrderDetails` Values (10786, 8, 40, 30, 0.2);
INSERT INTO `OrderDetails` Values (10786, 30, 25.89, 15, 0.2);
INSERT INTO `OrderDetails` Values (10786, 75, 7.75, 42, 0.2);
INSERT INTO `OrderDetails` Values (10787, 2, 19, 15, 0.05);
INSERT INTO `OrderDetails` Values (10787, 29, 123.79, 20, 0.05);
INSERT INTO `OrderDetails` Values (10788, 19, 9.2, 50, 0.05);
INSERT INTO `OrderDetails` Values (10788, 75, 7.75, 40, 0.05);
INSERT INTO `OrderDetails` Values (10789, 18, 62.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10789, 35, 18, 15, 0.0);
INSERT INTO `OrderDetails` Values (10789, 63, 43.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (10789, 68, 12.5, 18, 0.0);
INSERT INTO `OrderDetails` Values (10790, 7, 30, 3, 0.15);
INSERT INTO `OrderDetails` Values (10790, 56, 38, 20, 0.15);
INSERT INTO `OrderDetails` Values (10791, 29, 123.79, 14, 0.05);
INSERT INTO `OrderDetails` Values (10791, 41, 9.65, 20, 0.05);
INSERT INTO `OrderDetails` Values (10792, 2, 19, 10, 0.0);
INSERT INTO `OrderDetails` Values (10792, 54, 7.45, 3, 0.0);
INSERT INTO `OrderDetails` Values (10792, 68, 12.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10793, 41, 9.65, 14, 0.0);
INSERT INTO `OrderDetails` Values (10793, 52, 7, 8, 0.0);
INSERT INTO `OrderDetails` Values (10794, 14, 23.25, 15, 0.2);
INSERT INTO `OrderDetails` Values (10794, 54, 7.45, 6, 0.2);
INSERT INTO `OrderDetails` Values (10795, 16, 17.45, 65, 0.0);
INSERT INTO `OrderDetails` Values (10795, 17, 39, 35, 0.25);
INSERT INTO `OrderDetails` Values (10796, 26, 31.23, 21, 0.2);
INSERT INTO `OrderDetails` Values (10796, 44, 19.45, 10, 0.0);
INSERT INTO `OrderDetails` Values (10796, 64, 33.25, 35, 0.2);
INSERT INTO `OrderDetails` Values (10796, 69, 36, 24, 0.2);
INSERT INTO `OrderDetails` Values (10797, 11, 21, 20, 0.0);
INSERT INTO `OrderDetails` Values (10798, 62, 49.3, 2, 0.0);
INSERT INTO `OrderDetails` Values (10798, 72, 34.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10799, 13, 6, 20, 0.15);
INSERT INTO `OrderDetails` Values (10799, 24, 4.5, 20, 0.15);
INSERT INTO `OrderDetails` Values (10799, 59, 55, 25, 0.0);
INSERT INTO `OrderDetails` Values (10800, 11, 21, 50, 0.1);
INSERT INTO `OrderDetails` Values (10800, 51, 53, 10, 0.1);
INSERT INTO `OrderDetails` Values (10800, 54, 7.45, 7, 0.1);
INSERT INTO `OrderDetails` Values (10801, 17, 39, 40, 0.25);
INSERT INTO `OrderDetails` Values (10801, 29, 123.79, 20, 0.25);
INSERT INTO `OrderDetails` Values (10802, 30, 25.89, 25, 0.25);
INSERT INTO `OrderDetails` Values (10802, 51, 53, 30, 0.25);
INSERT INTO `OrderDetails` Values (10802, 55, 24, 60, 0.25);
INSERT INTO `OrderDetails` Values (10802, 62, 49.3, 5, 0.25);
INSERT INTO `OrderDetails` Values (10803, 19, 9.2, 24, 0.05);
INSERT INTO `OrderDetails` Values (10803, 25, 14, 15, 0.05);
INSERT INTO `OrderDetails` Values (10803, 59, 55, 15, 0.05);
INSERT INTO `OrderDetails` Values (10804, 10, 31, 36, 0.0);
INSERT INTO `OrderDetails` Values (10804, 28, 45.6, 24, 0.0);
INSERT INTO `OrderDetails` Values (10804, 49, 20, 4, 0.15);
INSERT INTO `OrderDetails` Values (10805, 34, 14, 10, 0.0);
INSERT INTO `OrderDetails` Values (10805, 38, 263.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10806, 2, 19, 20, 0.25);
INSERT INTO `OrderDetails` Values (10806, 65, 21.05, 2, 0.0);
INSERT INTO `OrderDetails` Values (10806, 74, 10, 15, 0.25);
INSERT INTO `OrderDetails` Values (10807, 40, 18.4, 1, 0.0);
INSERT INTO `OrderDetails` Values (10808, 56, 38, 20, 0.15);
INSERT INTO `OrderDetails` Values (10808, 76, 18, 50, 0.15);
INSERT INTO `OrderDetails` Values (10809, 52, 7, 20, 0.0);
INSERT INTO `OrderDetails` Values (10810, 13, 6, 7, 0.0);
INSERT INTO `OrderDetails` Values (10810, 25, 14, 5, 0.0);
INSERT INTO `OrderDetails` Values (10810, 70, 15, 5, 0.0);
INSERT INTO `OrderDetails` Values (10811, 19, 9.2, 15, 0.0);
INSERT INTO `OrderDetails` Values (10811, 23, 9, 18, 0.0);
INSERT INTO `OrderDetails` Values (10811, 40, 18.4, 30, 0.0);
INSERT INTO `OrderDetails` Values (10812, 31, 12.5, 16, 0.1);
INSERT INTO `OrderDetails` Values (10812, 72, 34.8, 40, 0.1);
INSERT INTO `OrderDetails` Values (10812, 77, 13, 20, 0.0);
INSERT INTO `OrderDetails` Values (10813, 2, 19, 12, 0.2);
INSERT INTO `OrderDetails` Values (10813, 46, 12, 35, 0.0);
INSERT INTO `OrderDetails` Values (10814, 41, 9.65, 20, 0.0);
INSERT INTO `OrderDetails` Values (10814, 43, 46, 20, 0.15);
INSERT INTO `OrderDetails` Values (10814, 48, 12.75, 8, 0.15);
INSERT INTO `OrderDetails` Values (10814, 61, 28.5, 30, 0.15);
INSERT INTO `OrderDetails` Values (10815, 33, 2.5, 16, 0.0);
INSERT INTO `OrderDetails` Values (10816, 38, 263.5, 30, 0.05);
INSERT INTO `OrderDetails` Values (10816, 62, 49.3, 20, 0.05);
INSERT INTO `OrderDetails` Values (10817, 26, 31.23, 40, 0.15);
INSERT INTO `OrderDetails` Values (10817, 38, 263.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10817, 40, 18.4, 60, 0.15);
INSERT INTO `OrderDetails` Values (10817, 62, 49.3, 25, 0.15);
INSERT INTO `OrderDetails` Values (10818, 32, 32, 20, 0.0);
INSERT INTO `OrderDetails` Values (10818, 41, 9.65, 20, 0.0);
INSERT INTO `OrderDetails` Values (10819, 43, 46, 7, 0.0);
INSERT INTO `OrderDetails` Values (10819, 75, 7.75, 20, 0.0);
INSERT INTO `OrderDetails` Values (10820, 56, 38, 30, 0.0);
INSERT INTO `OrderDetails` Values (10821, 35, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10821, 51, 53, 6, 0.0);
INSERT INTO `OrderDetails` Values (10822, 62, 49.3, 3, 0.0);
INSERT INTO `OrderDetails` Values (10822, 70, 15, 6, 0.0);
INSERT INTO `OrderDetails` Values (10823, 11, 21, 20, 0.1);
INSERT INTO `OrderDetails` Values (10823, 57, 19.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10823, 59, 55, 40, 0.1);
INSERT INTO `OrderDetails` Values (10823, 77, 13, 15, 0.1);
INSERT INTO `OrderDetails` Values (10824, 41, 9.65, 12, 0.0);
INSERT INTO `OrderDetails` Values (10824, 70, 15, 9, 0.0);
INSERT INTO `OrderDetails` Values (10825, 26, 31.23, 12, 0.0);
INSERT INTO `OrderDetails` Values (10825, 53, 32.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10826, 31, 12.5, 35, 0.0);
INSERT INTO `OrderDetails` Values (10826, 57, 19.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10827, 10, 31, 15, 0.0);
INSERT INTO `OrderDetails` Values (10827, 39, 18, 21, 0.0);
INSERT INTO `OrderDetails` Values (10828, 20, 81, 5, 0.0);
INSERT INTO `OrderDetails` Values (10828, 38, 263.5, 2, 0.0);
INSERT INTO `OrderDetails` Values (10829, 2, 19, 10, 0.0);
INSERT INTO `OrderDetails` Values (10829, 8, 40, 20, 0.0);
INSERT INTO `OrderDetails` Values (10829, 13, 6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10829, 60, 34, 21, 0.0);
INSERT INTO `OrderDetails` Values (10830, 6, 25, 6, 0.0);
INSERT INTO `OrderDetails` Values (10830, 39, 18, 28, 0.0);
INSERT INTO `OrderDetails` Values (10830, 60, 34, 30, 0.0);
INSERT INTO `OrderDetails` Values (10830, 68, 12.5, 24, 0.0);
INSERT INTO `OrderDetails` Values (10831, 19, 9.2, 2, 0.0);
INSERT INTO `OrderDetails` Values (10831, 35, 18, 8, 0.0);
INSERT INTO `OrderDetails` Values (10831, 38, 263.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (10831, 43, 46, 9, 0.0);
INSERT INTO `OrderDetails` Values (10832, 13, 6, 3, 0.2);
INSERT INTO `OrderDetails` Values (10832, 25, 14, 10, 0.2);
INSERT INTO `OrderDetails` Values (10832, 44, 19.45, 16, 0.2);
INSERT INTO `OrderDetails` Values (10832, 64, 33.25, 3, 0.0);
INSERT INTO `OrderDetails` Values (10833, 7, 30, 20, 0.1);
INSERT INTO `OrderDetails` Values (10833, 31, 12.5, 9, 0.1);
INSERT INTO `OrderDetails` Values (10833, 53, 32.8, 9, 0.1);
INSERT INTO `OrderDetails` Values (10834, 29, 123.79, 8, 0.05);
INSERT INTO `OrderDetails` Values (10834, 30, 25.89, 20, 0.05);
INSERT INTO `OrderDetails` Values (10835, 59, 55, 15, 0.0);
INSERT INTO `OrderDetails` Values (10835, 77, 13, 2, 0.2);
INSERT INTO `OrderDetails` Values (10836, 22, 21, 52, 0.0);
INSERT INTO `OrderDetails` Values (10836, 35, 18, 6, 0.0);
INSERT INTO `OrderDetails` Values (10836, 57, 19.5, 24, 0.0);
INSERT INTO `OrderDetails` Values (10836, 60, 34, 60, 0.0);
INSERT INTO `OrderDetails` Values (10836, 64, 33.25, 30, 0.0);
INSERT INTO `OrderDetails` Values (10837, 13, 6, 6, 0.0);
INSERT INTO `OrderDetails` Values (10837, 40, 18.4, 25, 0.0);
INSERT INTO `OrderDetails` Values (10837, 47, 9.5, 40, 0.25);
INSERT INTO `OrderDetails` Values (10837, 76, 18, 21, 0.25);
INSERT INTO `OrderDetails` Values (10838, 1, 18, 4, 0.25);
INSERT INTO `OrderDetails` Values (10838, 18, 62.5, 25, 0.25);
INSERT INTO `OrderDetails` Values (10838, 36, 19, 50, 0.25);
INSERT INTO `OrderDetails` Values (10839, 58, 13.25, 30, 0.1);
INSERT INTO `OrderDetails` Values (10839, 72, 34.8, 15, 0.1);
INSERT INTO `OrderDetails` Values (10840, 25, 14, 6, 0.2);
INSERT INTO `OrderDetails` Values (10840, 39, 18, 10, 0.2);
INSERT INTO `OrderDetails` Values (10841, 10, 31, 16, 0.0);
INSERT INTO `OrderDetails` Values (10841, 56, 38, 30, 0.0);
INSERT INTO `OrderDetails` Values (10841, 59, 55, 50, 0.0);
INSERT INTO `OrderDetails` Values (10841, 77, 13, 15, 0.0);
INSERT INTO `OrderDetails` Values (10842, 11, 21, 15, 0.0);
INSERT INTO `OrderDetails` Values (10842, 43, 46, 5, 0.0);
INSERT INTO `OrderDetails` Values (10842, 68, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10842, 70, 15, 12, 0.0);
INSERT INTO `OrderDetails` Values (10843, 51, 53, 4, 0.25);
INSERT INTO `OrderDetails` Values (10844, 22, 21, 35, 0.0);
INSERT INTO `OrderDetails` Values (10845, 23, 9, 70, 0.1);
INSERT INTO `OrderDetails` Values (10845, 35, 18, 25, 0.1);
INSERT INTO `OrderDetails` Values (10845, 42, 14, 42, 0.1);
INSERT INTO `OrderDetails` Values (10845, 58, 13.25, 60, 0.1);
INSERT INTO `OrderDetails` Values (10845, 64, 33.25, 48, 0.0);
INSERT INTO `OrderDetails` Values (10846, 4, 22, 21, 0.0);
INSERT INTO `OrderDetails` Values (10846, 70, 15, 30, 0.0);
INSERT INTO `OrderDetails` Values (10846, 74, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10847, 1, 18, 80, 0.2);
INSERT INTO `OrderDetails` Values (10847, 19, 9.2, 12, 0.2);
INSERT INTO `OrderDetails` Values (10847, 37, 26, 60, 0.2);
INSERT INTO `OrderDetails` Values (10847, 45, 9.5, 36, 0.2);
INSERT INTO `OrderDetails` Values (10847, 60, 34, 45, 0.2);
INSERT INTO `OrderDetails` Values (10847, 71, 21.5, 55, 0.2);
INSERT INTO `OrderDetails` Values (10848, 5, 21.35, 30, 0.0);
INSERT INTO `OrderDetails` Values (10848, 9, 97, 3, 0.0);
INSERT INTO `OrderDetails` Values (10849, 3, 10, 49, 0.0);
INSERT INTO `OrderDetails` Values (10849, 26, 31.23, 18, 0.15);
INSERT INTO `OrderDetails` Values (10850, 25, 14, 20, 0.15);
INSERT INTO `OrderDetails` Values (10850, 33, 2.5, 4, 0.15);
INSERT INTO `OrderDetails` Values (10850, 70, 15, 30, 0.15);
INSERT INTO `OrderDetails` Values (10851, 2, 19, 5, 0.05);
INSERT INTO `OrderDetails` Values (10851, 25, 14, 10, 0.05);
INSERT INTO `OrderDetails` Values (10851, 57, 19.5, 10, 0.05);
INSERT INTO `OrderDetails` Values (10851, 59, 55, 42, 0.05);
INSERT INTO `OrderDetails` Values (10852, 2, 19, 15, 0.0);
INSERT INTO `OrderDetails` Values (10852, 17, 39, 6, 0.0);
INSERT INTO `OrderDetails` Values (10852, 62, 49.3, 50, 0.0);
INSERT INTO `OrderDetails` Values (10853, 18, 62.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10854, 10, 31, 100, 0.15);
INSERT INTO `OrderDetails` Values (10854, 13, 6, 65, 0.15);
INSERT INTO `OrderDetails` Values (10855, 16, 17.45, 50, 0.0);
INSERT INTO `OrderDetails` Values (10855, 31, 12.5, 14, 0.0);
INSERT INTO `OrderDetails` Values (10855, 56, 38, 24, 0.0);
INSERT INTO `OrderDetails` Values (10855, 65, 21.05, 15, 0.15);
INSERT INTO `OrderDetails` Values (10856, 2, 19, 20, 0.0);
INSERT INTO `OrderDetails` Values (10856, 42, 14, 20, 0.0);
INSERT INTO `OrderDetails` Values (10857, 3, 10, 30, 0.0);
INSERT INTO `OrderDetails` Values (10857, 26, 31.23, 35, 0.25);
INSERT INTO `OrderDetails` Values (10857, 29, 123.79, 10, 0.25);
INSERT INTO `OrderDetails` Values (10858, 7, 30, 5, 0.0);
INSERT INTO `OrderDetails` Values (10858, 27, 43.9, 10, 0.0);
INSERT INTO `OrderDetails` Values (10858, 70, 15, 4, 0.0);
INSERT INTO `OrderDetails` Values (10859, 24, 4.5, 40, 0.25);
INSERT INTO `OrderDetails` Values (10859, 54, 7.45, 35, 0.25);
INSERT INTO `OrderDetails` Values (10859, 64, 33.25, 30, 0.25);
INSERT INTO `OrderDetails` Values (10860, 51, 53, 3, 0.0);
INSERT INTO `OrderDetails` Values (10860, 76, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10861, 17, 39, 42, 0.0);
INSERT INTO `OrderDetails` Values (10861, 18, 62.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10861, 21, 10, 40, 0.0);
INSERT INTO `OrderDetails` Values (10861, 33, 2.5, 35, 0.0);
INSERT INTO `OrderDetails` Values (10861, 62, 49.3, 3, 0.0);
INSERT INTO `OrderDetails` Values (10862, 11, 21, 25, 0.0);
INSERT INTO `OrderDetails` Values (10862, 52, 7, 8, 0.0);
INSERT INTO `OrderDetails` Values (10863, 1, 18, 20, 0.15);
INSERT INTO `OrderDetails` Values (10863, 58, 13.25, 12, 0.15);
INSERT INTO `OrderDetails` Values (10864, 35, 18, 4, 0.0);
INSERT INTO `OrderDetails` Values (10864, 67, 14, 15, 0.0);
INSERT INTO `OrderDetails` Values (10865, 38, 263.5, 60, 0.05);
INSERT INTO `OrderDetails` Values (10865, 39, 18, 80, 0.05);
INSERT INTO `OrderDetails` Values (10866, 2, 19, 21, 0.25);
INSERT INTO `OrderDetails` Values (10866, 24, 4.5, 6, 0.25);
INSERT INTO `OrderDetails` Values (10866, 30, 25.89, 40, 0.25);
INSERT INTO `OrderDetails` Values (10867, 53, 32.8, 3, 0.0);
INSERT INTO `OrderDetails` Values (10868, 26, 31.23, 20, 0.0);
INSERT INTO `OrderDetails` Values (10868, 35, 18, 30, 0.0);
INSERT INTO `OrderDetails` Values (10868, 49, 20, 42, 0.1);
INSERT INTO `OrderDetails` Values (10869, 1, 18, 40, 0.0);
INSERT INTO `OrderDetails` Values (10869, 11, 21, 10, 0.0);
INSERT INTO `OrderDetails` Values (10869, 23, 9, 50, 0.0);
INSERT INTO `OrderDetails` Values (10869, 68, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10870, 35, 18, 3, 0.0);
INSERT INTO `OrderDetails` Values (10870, 51, 53, 2, 0.0);
INSERT INTO `OrderDetails` Values (10871, 6, 25, 50, 0.05);
INSERT INTO `OrderDetails` Values (10871, 16, 17.45, 12, 0.05);
INSERT INTO `OrderDetails` Values (10871, 17, 39, 16, 0.05);
INSERT INTO `OrderDetails` Values (10872, 55, 24, 10, 0.05);
INSERT INTO `OrderDetails` Values (10872, 62, 49.3, 20, 0.05);
INSERT INTO `OrderDetails` Values (10872, 64, 33.25, 15, 0.05);
INSERT INTO `OrderDetails` Values (10872, 65, 21.05, 21, 0.05);
INSERT INTO `OrderDetails` Values (10873, 21, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10873, 28, 45.6, 3, 0.0);
INSERT INTO `OrderDetails` Values (10874, 10, 31, 10, 0.0);
INSERT INTO `OrderDetails` Values (10875, 19, 9.2, 25, 0.0);
INSERT INTO `OrderDetails` Values (10875, 47, 9.5, 21, 0.1);
INSERT INTO `OrderDetails` Values (10875, 49, 20, 15, 0.0);
INSERT INTO `OrderDetails` Values (10876, 46, 12, 21, 0.0);
INSERT INTO `OrderDetails` Values (10876, 64, 33.25, 20, 0.0);
INSERT INTO `OrderDetails` Values (10877, 16, 17.45, 30, 0.25);
INSERT INTO `OrderDetails` Values (10877, 18, 62.5, 25, 0.0);
INSERT INTO `OrderDetails` Values (10878, 20, 81, 20, 0.05);
INSERT INTO `OrderDetails` Values (10879, 40, 18.4, 12, 0.0);
INSERT INTO `OrderDetails` Values (10879, 65, 21.05, 10, 0.0);
INSERT INTO `OrderDetails` Values (10879, 76, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10880, 23, 9, 30, 0.2);
INSERT INTO `OrderDetails` Values (10880, 61, 28.5, 30, 0.2);
INSERT INTO `OrderDetails` Values (10880, 70, 15, 50, 0.2);
INSERT INTO `OrderDetails` Values (10881, 73, 15, 10, 0.0);
INSERT INTO `OrderDetails` Values (10882, 42, 14, 25, 0.0);
INSERT INTO `OrderDetails` Values (10882, 49, 20, 20, 0.15);
INSERT INTO `OrderDetails` Values (10882, 54, 7.45, 32, 0.15);
INSERT INTO `OrderDetails` Values (10883, 24, 4.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (10884, 21, 10, 40, 0.05);
INSERT INTO `OrderDetails` Values (10884, 56, 38, 21, 0.05);
INSERT INTO `OrderDetails` Values (10884, 65, 21.05, 12, 0.05);
INSERT INTO `OrderDetails` Values (10885, 2, 19, 20, 0.0);
INSERT INTO `OrderDetails` Values (10885, 24, 4.5, 12, 0.0);
INSERT INTO `OrderDetails` Values (10885, 70, 15, 30, 0.0);
INSERT INTO `OrderDetails` Values (10885, 77, 13, 25, 0.0);
INSERT INTO `OrderDetails` Values (10886, 10, 31, 70, 0.0);
INSERT INTO `OrderDetails` Values (10886, 31, 12.5, 35, 0.0);
INSERT INTO `OrderDetails` Values (10886, 77, 13, 40, 0.0);
INSERT INTO `OrderDetails` Values (10887, 25, 14, 5, 0.0);
INSERT INTO `OrderDetails` Values (10888, 2, 19, 20, 0.0);
INSERT INTO `OrderDetails` Values (10888, 68, 12.5, 18, 0.0);
INSERT INTO `OrderDetails` Values (10889, 11, 21, 40, 0.0);
INSERT INTO `OrderDetails` Values (10889, 38, 263.5, 40, 0.0);
INSERT INTO `OrderDetails` Values (10890, 17, 39, 15, 0.0);
INSERT INTO `OrderDetails` Values (10890, 34, 14, 10, 0.0);
INSERT INTO `OrderDetails` Values (10890, 41, 9.65, 14, 0.0);
INSERT INTO `OrderDetails` Values (10891, 30, 25.89, 15, 0.05);
INSERT INTO `OrderDetails` Values (10892, 59, 55, 40, 0.05);
INSERT INTO `OrderDetails` Values (10893, 8, 40, 30, 0.0);
INSERT INTO `OrderDetails` Values (10893, 24, 4.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10893, 29, 123.79, 24, 0.0);
INSERT INTO `OrderDetails` Values (10893, 30, 25.89, 35, 0.0);
INSERT INTO `OrderDetails` Values (10893, 36, 19, 20, 0.0);
INSERT INTO `OrderDetails` Values (10894, 13, 6, 28, 0.05);
INSERT INTO `OrderDetails` Values (10894, 69, 36, 50, 0.05);
INSERT INTO `OrderDetails` Values (10894, 75, 7.75, 120, 0.05);
INSERT INTO `OrderDetails` Values (10895, 24, 4.5, 110, 0.0);
INSERT INTO `OrderDetails` Values (10895, 39, 18, 45, 0.0);
INSERT INTO `OrderDetails` Values (10895, 40, 18.4, 91, 0.0);
INSERT INTO `OrderDetails` Values (10895, 60, 34, 100, 0.0);
INSERT INTO `OrderDetails` Values (10896, 45, 9.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10896, 56, 38, 16, 0.0);
INSERT INTO `OrderDetails` Values (10897, 29, 123.79, 80, 0.0);
INSERT INTO `OrderDetails` Values (10897, 30, 25.89, 36, 0.0);
INSERT INTO `OrderDetails` Values (10898, 13, 6, 5, 0.0);
INSERT INTO `OrderDetails` Values (10899, 39, 18, 8, 0.15);
INSERT INTO `OrderDetails` Values (10900, 70, 15, 3, 0.25);
INSERT INTO `OrderDetails` Values (10901, 41, 9.65, 30, 0.0);
INSERT INTO `OrderDetails` Values (10901, 71, 21.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10902, 55, 24, 30, 0.15);
INSERT INTO `OrderDetails` Values (10902, 62, 49.3, 6, 0.15);
INSERT INTO `OrderDetails` Values (10903, 13, 6, 40, 0.0);
INSERT INTO `OrderDetails` Values (10903, 65, 21.05, 21, 0.0);
INSERT INTO `OrderDetails` Values (10903, 68, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10904, 58, 13.25, 15, 0.0);
INSERT INTO `OrderDetails` Values (10904, 62, 49.3, 35, 0.0);
INSERT INTO `OrderDetails` Values (10905, 1, 18, 20, 0.05);
INSERT INTO `OrderDetails` Values (10906, 61, 28.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10907, 75, 7.75, 14, 0.0);
INSERT INTO `OrderDetails` Values (10908, 7, 30, 20, 0.05);
INSERT INTO `OrderDetails` Values (10908, 52, 7, 14, 0.05);
INSERT INTO `OrderDetails` Values (10909, 7, 30, 12, 0.0);
INSERT INTO `OrderDetails` Values (10909, 16, 17.45, 15, 0.0);
INSERT INTO `OrderDetails` Values (10909, 41, 9.65, 5, 0.0);
INSERT INTO `OrderDetails` Values (10910, 19, 9.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10910, 49, 20, 10, 0.0);
INSERT INTO `OrderDetails` Values (10910, 61, 28.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10911, 1, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10911, 17, 39, 12, 0.0);
INSERT INTO `OrderDetails` Values (10911, 67, 14, 15, 0.0);
INSERT INTO `OrderDetails` Values (10912, 11, 21, 40, 0.25);
INSERT INTO `OrderDetails` Values (10912, 29, 123.79, 60, 0.25);
INSERT INTO `OrderDetails` Values (10913, 4, 22, 30, 0.25);
INSERT INTO `OrderDetails` Values (10913, 33, 2.5, 40, 0.25);
INSERT INTO `OrderDetails` Values (10913, 58, 13.25, 15, 0.0);
INSERT INTO `OrderDetails` Values (10914, 71, 21.5, 25, 0.0);
INSERT INTO `OrderDetails` Values (10915, 17, 39, 10, 0.0);
INSERT INTO `OrderDetails` Values (10915, 33, 2.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10915, 54, 7.45, 10, 0.0);
INSERT INTO `OrderDetails` Values (10916, 16, 17.45, 6, 0.0);
INSERT INTO `OrderDetails` Values (10916, 32, 32, 6, 0.0);
INSERT INTO `OrderDetails` Values (10916, 57, 19.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10917, 30, 25.89, 1, 0.0);
INSERT INTO `OrderDetails` Values (10917, 60, 34, 10, 0.0);
INSERT INTO `OrderDetails` Values (10918, 1, 18, 60, 0.25);
INSERT INTO `OrderDetails` Values (10918, 60, 34, 25, 0.25);
INSERT INTO `OrderDetails` Values (10919, 16, 17.45, 24, 0.0);
INSERT INTO `OrderDetails` Values (10919, 25, 14, 24, 0.0);
INSERT INTO `OrderDetails` Values (10919, 40, 18.4, 20, 0.0);
INSERT INTO `OrderDetails` Values (10920, 50, 16.25, 24, 0.0);
INSERT INTO `OrderDetails` Values (10921, 35, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10921, 63, 43.9, 40, 0.0);
INSERT INTO `OrderDetails` Values (10922, 17, 39, 15, 0.0);
INSERT INTO `OrderDetails` Values (10922, 24, 4.5, 35, 0.0);
INSERT INTO `OrderDetails` Values (10923, 42, 14, 10, 0.2);
INSERT INTO `OrderDetails` Values (10923, 43, 46, 10, 0.2);
INSERT INTO `OrderDetails` Values (10923, 67, 14, 24, 0.2);
INSERT INTO `OrderDetails` Values (10924, 10, 31, 20, 0.1);
INSERT INTO `OrderDetails` Values (10924, 28, 45.6, 30, 0.1);
INSERT INTO `OrderDetails` Values (10924, 75, 7.75, 6, 0.0);
INSERT INTO `OrderDetails` Values (10925, 36, 19, 25, 0.15);
INSERT INTO `OrderDetails` Values (10925, 52, 7, 12, 0.15);
INSERT INTO `OrderDetails` Values (10926, 11, 21, 2, 0.0);
INSERT INTO `OrderDetails` Values (10926, 13, 6, 10, 0.0);
INSERT INTO `OrderDetails` Values (10926, 19, 9.2, 7, 0.0);
INSERT INTO `OrderDetails` Values (10926, 72, 34.8, 10, 0.0);
INSERT INTO `OrderDetails` Values (10927, 20, 81, 5, 0.0);
INSERT INTO `OrderDetails` Values (10927, 52, 7, 5, 0.0);
INSERT INTO `OrderDetails` Values (10927, 76, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (10928, 47, 9.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10928, 76, 18, 5, 0.0);
INSERT INTO `OrderDetails` Values (10929, 21, 10, 60, 0.0);
INSERT INTO `OrderDetails` Values (10929, 75, 7.75, 49, 0.0);
INSERT INTO `OrderDetails` Values (10929, 77, 13, 15, 0.0);
INSERT INTO `OrderDetails` Values (10930, 21, 10, 36, 0.0);
INSERT INTO `OrderDetails` Values (10930, 27, 43.9, 25, 0.0);
INSERT INTO `OrderDetails` Values (10930, 55, 24, 25, 0.2);
INSERT INTO `OrderDetails` Values (10930, 58, 13.25, 30, 0.2);
INSERT INTO `OrderDetails` Values (10931, 13, 6, 42, 0.15);
INSERT INTO `OrderDetails` Values (10931, 57, 19.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10932, 16, 17.45, 30, 0.1);
INSERT INTO `OrderDetails` Values (10932, 62, 49.3, 14, 0.1);
INSERT INTO `OrderDetails` Values (10932, 72, 34.8, 16, 0.0);
INSERT INTO `OrderDetails` Values (10932, 75, 7.75, 20, 0.1);
INSERT INTO `OrderDetails` Values (10933, 53, 32.8, 2, 0.0);
INSERT INTO `OrderDetails` Values (10933, 61, 28.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10934, 6, 25, 20, 0.0);
INSERT INTO `OrderDetails` Values (10935, 1, 18, 21, 0.0);
INSERT INTO `OrderDetails` Values (10935, 18, 62.5, 4, 0.25);
INSERT INTO `OrderDetails` Values (10935, 23, 9, 8, 0.25);
INSERT INTO `OrderDetails` Values (10936, 36, 19, 30, 0.2);
INSERT INTO `OrderDetails` Values (10937, 28, 45.6, 8, 0.0);
INSERT INTO `OrderDetails` Values (10937, 34, 14, 20, 0.0);
INSERT INTO `OrderDetails` Values (10938, 13, 6, 20, 0.25);
INSERT INTO `OrderDetails` Values (10938, 43, 46, 24, 0.25);
INSERT INTO `OrderDetails` Values (10938, 60, 34, 49, 0.25);
INSERT INTO `OrderDetails` Values (10938, 71, 21.5, 35, 0.25);
INSERT INTO `OrderDetails` Values (10939, 2, 19, 10, 0.15);
INSERT INTO `OrderDetails` Values (10939, 67, 14, 40, 0.15);
INSERT INTO `OrderDetails` Values (10940, 7, 30, 8, 0.0);
INSERT INTO `OrderDetails` Values (10940, 13, 6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10941, 31, 12.5, 44, 0.25);
INSERT INTO `OrderDetails` Values (10941, 62, 49.3, 30, 0.25);
INSERT INTO `OrderDetails` Values (10941, 68, 12.5, 80, 0.25);
INSERT INTO `OrderDetails` Values (10941, 72, 34.8, 50, 0.0);
INSERT INTO `OrderDetails` Values (10942, 49, 20, 28, 0.0);
INSERT INTO `OrderDetails` Values (10943, 13, 6, 15, 0.0);
INSERT INTO `OrderDetails` Values (10943, 22, 21, 21, 0.0);
INSERT INTO `OrderDetails` Values (10943, 46, 12, 15, 0.0);
INSERT INTO `OrderDetails` Values (10944, 11, 21, 5, 0.25);
INSERT INTO `OrderDetails` Values (10944, 44, 19.45, 18, 0.25);
INSERT INTO `OrderDetails` Values (10944, 56, 38, 18, 0.0);
INSERT INTO `OrderDetails` Values (10945, 13, 6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10945, 31, 12.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (10946, 10, 31, 25, 0.0);
INSERT INTO `OrderDetails` Values (10946, 24, 4.5, 25, 0.0);
INSERT INTO `OrderDetails` Values (10946, 77, 13, 40, 0.0);
INSERT INTO `OrderDetails` Values (10947, 59, 55, 4, 0.0);
INSERT INTO `OrderDetails` Values (10948, 50, 16.25, 9, 0.0);
INSERT INTO `OrderDetails` Values (10948, 51, 53, 40, 0.0);
INSERT INTO `OrderDetails` Values (10948, 55, 24, 4, 0.0);
INSERT INTO `OrderDetails` Values (10949, 6, 25, 12, 0.0);
INSERT INTO `OrderDetails` Values (10949, 10, 31, 30, 0.0);
INSERT INTO `OrderDetails` Values (10949, 17, 39, 6, 0.0);
INSERT INTO `OrderDetails` Values (10949, 62, 49.3, 60, 0.0);
INSERT INTO `OrderDetails` Values (10950, 4, 22, 5, 0.0);
INSERT INTO `OrderDetails` Values (10951, 33, 2.5, 15, 0.05);
INSERT INTO `OrderDetails` Values (10951, 41, 9.65, 6, 0.05);
INSERT INTO `OrderDetails` Values (10951, 75, 7.75, 50, 0.05);
INSERT INTO `OrderDetails` Values (10952, 6, 25, 16, 0.05);
INSERT INTO `OrderDetails` Values (10952, 28, 45.6, 2, 0.0);
INSERT INTO `OrderDetails` Values (10953, 20, 81, 50, 0.05);
INSERT INTO `OrderDetails` Values (10953, 31, 12.5, 50, 0.05);
INSERT INTO `OrderDetails` Values (10954, 16, 17.45, 28, 0.15);
INSERT INTO `OrderDetails` Values (10954, 31, 12.5, 25, 0.15);
INSERT INTO `OrderDetails` Values (10954, 45, 9.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10954, 60, 34, 24, 0.15);
INSERT INTO `OrderDetails` Values (10955, 75, 7.75, 12, 0.2);
INSERT INTO `OrderDetails` Values (10956, 21, 10, 12, 0.0);
INSERT INTO `OrderDetails` Values (10956, 47, 9.5, 14, 0.0);
INSERT INTO `OrderDetails` Values (10956, 51, 53, 8, 0.0);
INSERT INTO `OrderDetails` Values (10957, 30, 25.89, 30, 0.0);
INSERT INTO `OrderDetails` Values (10957, 35, 18, 40, 0.0);
INSERT INTO `OrderDetails` Values (10957, 64, 33.25, 8, 0.0);
INSERT INTO `OrderDetails` Values (10958, 5, 21.35, 20, 0.0);
INSERT INTO `OrderDetails` Values (10958, 7, 30, 6, 0.0);
INSERT INTO `OrderDetails` Values (10958, 72, 34.8, 5, 0.0);
INSERT INTO `OrderDetails` Values (10959, 75, 7.75, 20, 0.15);
INSERT INTO `OrderDetails` Values (10960, 24, 4.5, 10, 0.25);
INSERT INTO `OrderDetails` Values (10960, 41, 9.65, 24, 0.0);
INSERT INTO `OrderDetails` Values (10961, 52, 7, 6, 0.05);
INSERT INTO `OrderDetails` Values (10961, 76, 18, 60, 0.0);
INSERT INTO `OrderDetails` Values (10962, 7, 30, 45, 0.0);
INSERT INTO `OrderDetails` Values (10962, 13, 6, 77, 0.0);
INSERT INTO `OrderDetails` Values (10962, 53, 32.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10962, 69, 36, 9, 0.0);
INSERT INTO `OrderDetails` Values (10962, 76, 18, 44, 0.0);
INSERT INTO `OrderDetails` Values (10963, 60, 34, 2, 0.15);
INSERT INTO `OrderDetails` Values (10964, 18, 62.5, 6, 0.0);
INSERT INTO `OrderDetails` Values (10964, 38, 263.5, 5, 0.0);
INSERT INTO `OrderDetails` Values (10964, 69, 36, 10, 0.0);
INSERT INTO `OrderDetails` Values (10965, 51, 53, 16, 0.0);
INSERT INTO `OrderDetails` Values (10966, 37, 26, 8, 0.0);
INSERT INTO `OrderDetails` Values (10966, 56, 38, 12, 0.15);
INSERT INTO `OrderDetails` Values (10966, 62, 49.3, 12, 0.15);
INSERT INTO `OrderDetails` Values (10967, 19, 9.2, 12, 0.0);
INSERT INTO `OrderDetails` Values (10967, 49, 20, 40, 0.0);
INSERT INTO `OrderDetails` Values (10968, 12, 38, 30, 0.0);
INSERT INTO `OrderDetails` Values (10968, 24, 4.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10968, 64, 33.25, 4, 0.0);
INSERT INTO `OrderDetails` Values (10969, 46, 12, 9, 0.0);
INSERT INTO `OrderDetails` Values (10970, 52, 7, 40, 0.2);
INSERT INTO `OrderDetails` Values (10971, 29, 123.79, 14, 0.0);
INSERT INTO `OrderDetails` Values (10972, 17, 39, 6, 0.0);
INSERT INTO `OrderDetails` Values (10972, 33, 2.5, 7, 0.0);
INSERT INTO `OrderDetails` Values (10973, 26, 31.23, 5, 0.0);
INSERT INTO `OrderDetails` Values (10973, 41, 9.65, 6, 0.0);
INSERT INTO `OrderDetails` Values (10973, 75, 7.75, 10, 0.0);
INSERT INTO `OrderDetails` Values (10974, 63, 43.9, 10, 0.0);
INSERT INTO `OrderDetails` Values (10975, 8, 40, 16, 0.0);
INSERT INTO `OrderDetails` Values (10975, 75, 7.75, 10, 0.0);
INSERT INTO `OrderDetails` Values (10976, 28, 45.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (10977, 39, 18, 30, 0.0);
INSERT INTO `OrderDetails` Values (10977, 47, 9.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (10977, 51, 53, 10, 0.0);
INSERT INTO `OrderDetails` Values (10977, 63, 43.9, 20, 0.0);
INSERT INTO `OrderDetails` Values (10978, 8, 40, 20, 0.15);
INSERT INTO `OrderDetails` Values (10978, 21, 10, 40, 0.15);
INSERT INTO `OrderDetails` Values (10978, 40, 18.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (10978, 44, 19.45, 6, 0.15);
INSERT INTO `OrderDetails` Values (10979, 7, 30, 18, 0.0);
INSERT INTO `OrderDetails` Values (10979, 12, 38, 20, 0.0);
INSERT INTO `OrderDetails` Values (10979, 24, 4.5, 80, 0.0);
INSERT INTO `OrderDetails` Values (10979, 27, 43.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (10979, 31, 12.5, 24, 0.0);
INSERT INTO `OrderDetails` Values (10979, 63, 43.9, 35, 0.0);
INSERT INTO `OrderDetails` Values (10980, 75, 7.75, 40, 0.2);
INSERT INTO `OrderDetails` Values (10981, 38, 263.5, 60, 0.0);
INSERT INTO `OrderDetails` Values (10982, 7, 30, 20, 0.0);
INSERT INTO `OrderDetails` Values (10982, 43, 46, 9, 0.0);
INSERT INTO `OrderDetails` Values (10983, 13, 6, 84, 0.15);
INSERT INTO `OrderDetails` Values (10983, 57, 19.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (10984, 16, 17.45, 55, 0.0);
INSERT INTO `OrderDetails` Values (10984, 24, 4.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (10984, 36, 19, 40, 0.0);
INSERT INTO `OrderDetails` Values (10985, 16, 17.45, 36, 0.1);
INSERT INTO `OrderDetails` Values (10985, 18, 62.5, 8, 0.1);
INSERT INTO `OrderDetails` Values (10985, 32, 32, 35, 0.1);
INSERT INTO `OrderDetails` Values (10986, 11, 21, 30, 0.0);
INSERT INTO `OrderDetails` Values (10986, 20, 81, 15, 0.0);
INSERT INTO `OrderDetails` Values (10986, 76, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (10986, 77, 13, 15, 0.0);
INSERT INTO `OrderDetails` Values (10987, 7, 30, 60, 0.0);
INSERT INTO `OrderDetails` Values (10987, 43, 46, 6, 0.0);
INSERT INTO `OrderDetails` Values (10987, 72, 34.8, 20, 0.0);
INSERT INTO `OrderDetails` Values (10988, 7, 30, 60, 0.0);
INSERT INTO `OrderDetails` Values (10988, 62, 49.3, 40, 0.1);
INSERT INTO `OrderDetails` Values (10989, 6, 25, 40, 0.0);
INSERT INTO `OrderDetails` Values (10989, 11, 21, 15, 0.0);
INSERT INTO `OrderDetails` Values (10989, 41, 9.65, 4, 0.0);
INSERT INTO `OrderDetails` Values (10990, 21, 10, 65, 0.0);
INSERT INTO `OrderDetails` Values (10990, 34, 14, 60, 0.15);
INSERT INTO `OrderDetails` Values (10990, 55, 24, 65, 0.15);
INSERT INTO `OrderDetails` Values (10990, 61, 28.5, 66, 0.15);
INSERT INTO `OrderDetails` Values (10991, 2, 19, 50, 0.2);
INSERT INTO `OrderDetails` Values (10991, 70, 15, 20, 0.2);
INSERT INTO `OrderDetails` Values (10991, 76, 18, 90, 0.2);
INSERT INTO `OrderDetails` Values (10992, 72, 34.8, 2, 0.0);
INSERT INTO `OrderDetails` Values (10993, 29, 123.79, 50, 0.25);
INSERT INTO `OrderDetails` Values (10993, 41, 9.65, 35, 0.25);
INSERT INTO `OrderDetails` Values (10994, 59, 55, 18, 0.05);
INSERT INTO `OrderDetails` Values (10995, 51, 53, 20, 0.0);
INSERT INTO `OrderDetails` Values (10995, 60, 34, 4, 0.0);
INSERT INTO `OrderDetails` Values (10996, 42, 14, 40, 0.0);
INSERT INTO `OrderDetails` Values (10997, 32, 32, 50, 0.0);
INSERT INTO `OrderDetails` Values (10997, 46, 12, 20, 0.25);
INSERT INTO `OrderDetails` Values (10997, 52, 7, 20, 0.25);
INSERT INTO `OrderDetails` Values (10998, 24, 4.5, 12, 0.0);
INSERT INTO `OrderDetails` Values (10998, 61, 28.5, 7, 0.0);
INSERT INTO `OrderDetails` Values (10998, 74, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (10998, 75, 7.75, 30, 0.0);
INSERT INTO `OrderDetails` Values (10999, 41, 9.65, 20, 0.05);
INSERT INTO `OrderDetails` Values (10999, 51, 53, 15, 0.05);
INSERT INTO `OrderDetails` Values (10999, 77, 13, 21, 0.05);
INSERT INTO `OrderDetails` Values (11000, 4, 22, 25, 0.25);
INSERT INTO `OrderDetails` Values (11000, 24, 4.5, 30, 0.25);
INSERT INTO `OrderDetails` Values (11000, 77, 13, 30, 0.0);
INSERT INTO `OrderDetails` Values (11001, 7, 30, 60, 0.0);
INSERT INTO `OrderDetails` Values (11001, 22, 21, 25, 0.0);
INSERT INTO `OrderDetails` Values (11001, 46, 12, 25, 0.0);
INSERT INTO `OrderDetails` Values (11001, 55, 24, 6, 0.0);
INSERT INTO `OrderDetails` Values (11002, 13, 6, 56, 0.0);
INSERT INTO `OrderDetails` Values (11002, 35, 18, 15, 0.15);
INSERT INTO `OrderDetails` Values (11002, 42, 14, 24, 0.15);
INSERT INTO `OrderDetails` Values (11002, 55, 24, 40, 0.0);
INSERT INTO `OrderDetails` Values (11003, 1, 18, 4, 0.0);
INSERT INTO `OrderDetails` Values (11003, 40, 18.4, 10, 0.0);
INSERT INTO `OrderDetails` Values (11003, 52, 7, 10, 0.0);
INSERT INTO `OrderDetails` Values (11004, 26, 31.23, 6, 0.0);
INSERT INTO `OrderDetails` Values (11004, 76, 18, 6, 0.0);
INSERT INTO `OrderDetails` Values (11005, 1, 18, 2, 0.0);
INSERT INTO `OrderDetails` Values (11005, 59, 55, 10, 0.0);
INSERT INTO `OrderDetails` Values (11006, 1, 18, 8, 0.0);
INSERT INTO `OrderDetails` Values (11006, 29, 123.79, 2, 0.25);
INSERT INTO `OrderDetails` Values (11007, 8, 40, 30, 0.0);
INSERT INTO `OrderDetails` Values (11007, 29, 123.79, 10, 0.0);
INSERT INTO `OrderDetails` Values (11007, 42, 14, 14, 0.0);
INSERT INTO `OrderDetails` Values (11008, 28, 45.6, 70, 0.05);
INSERT INTO `OrderDetails` Values (11008, 34, 14, 90, 0.05);
INSERT INTO `OrderDetails` Values (11008, 71, 21.5, 21, 0.0);
INSERT INTO `OrderDetails` Values (11009, 24, 4.5, 12, 0.0);
INSERT INTO `OrderDetails` Values (11009, 36, 19, 18, 0.25);
INSERT INTO `OrderDetails` Values (11009, 60, 34, 9, 0.0);
INSERT INTO `OrderDetails` Values (11010, 7, 30, 20, 0.0);
INSERT INTO `OrderDetails` Values (11010, 24, 4.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (11011, 58, 13.25, 40, 0.05);
INSERT INTO `OrderDetails` Values (11011, 71, 21.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (11012, 19, 9.2, 50, 0.05);
INSERT INTO `OrderDetails` Values (11012, 60, 34, 36, 0.05);
INSERT INTO `OrderDetails` Values (11012, 71, 21.5, 60, 0.05);
INSERT INTO `OrderDetails` Values (11013, 23, 9, 10, 0.0);
INSERT INTO `OrderDetails` Values (11013, 42, 14, 4, 0.0);
INSERT INTO `OrderDetails` Values (11013, 45, 9.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (11013, 68, 12.5, 2, 0.0);
INSERT INTO `OrderDetails` Values (11014, 41, 9.65, 28, 0.1);
INSERT INTO `OrderDetails` Values (11015, 30, 25.89, 15, 0.0);
INSERT INTO `OrderDetails` Values (11015, 77, 13, 18, 0.0);
INSERT INTO `OrderDetails` Values (11016, 31, 12.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (11016, 36, 19, 16, 0.0);
INSERT INTO `OrderDetails` Values (11017, 3, 10, 25, 0.0);
INSERT INTO `OrderDetails` Values (11017, 59, 55, 110, 0.0);
INSERT INTO `OrderDetails` Values (11017, 70, 15, 30, 0.0);
INSERT INTO `OrderDetails` Values (11018, 12, 38, 20, 0.0);
INSERT INTO `OrderDetails` Values (11018, 18, 62.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (11018, 56, 38, 5, 0.0);
INSERT INTO `OrderDetails` Values (11019, 46, 12, 3, 0.0);
INSERT INTO `OrderDetails` Values (11019, 49, 20, 2, 0.0);
INSERT INTO `OrderDetails` Values (11020, 10, 31, 24, 0.15);
INSERT INTO `OrderDetails` Values (11021, 2, 19, 11, 0.25);
INSERT INTO `OrderDetails` Values (11021, 20, 81, 15, 0.0);
INSERT INTO `OrderDetails` Values (11021, 26, 31.23, 63, 0.0);
INSERT INTO `OrderDetails` Values (11021, 51, 53, 44, 0.25);
INSERT INTO `OrderDetails` Values (11021, 72, 34.8, 35, 0.0);
INSERT INTO `OrderDetails` Values (11022, 19, 9.2, 35, 0.0);
INSERT INTO `OrderDetails` Values (11022, 69, 36, 30, 0.0);
INSERT INTO `OrderDetails` Values (11023, 7, 30, 4, 0.0);
INSERT INTO `OrderDetails` Values (11023, 43, 46, 30, 0.0);
INSERT INTO `OrderDetails` Values (11024, 26, 31.23, 12, 0.0);
INSERT INTO `OrderDetails` Values (11024, 33, 2.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (11024, 65, 21.05, 21, 0.0);
INSERT INTO `OrderDetails` Values (11024, 71, 21.5, 50, 0.0);
INSERT INTO `OrderDetails` Values (11025, 1, 18, 10, 0.1);
INSERT INTO `OrderDetails` Values (11025, 13, 6, 20, 0.1);
INSERT INTO `OrderDetails` Values (11026, 18, 62.5, 8, 0.0);
INSERT INTO `OrderDetails` Values (11026, 51, 53, 10, 0.0);
INSERT INTO `OrderDetails` Values (11027, 24, 4.5, 30, 0.25);
INSERT INTO `OrderDetails` Values (11027, 62, 49.3, 21, 0.25);
INSERT INTO `OrderDetails` Values (11028, 55, 24, 35, 0.0);
INSERT INTO `OrderDetails` Values (11028, 59, 55, 24, 0.0);
INSERT INTO `OrderDetails` Values (11029, 56, 38, 20, 0.0);
INSERT INTO `OrderDetails` Values (11029, 63, 43.9, 12, 0.0);
INSERT INTO `OrderDetails` Values (11030, 2, 19, 100, 0.25);
INSERT INTO `OrderDetails` Values (11030, 5, 21.35, 70, 0.0);
INSERT INTO `OrderDetails` Values (11030, 29, 123.79, 60, 0.25);
INSERT INTO `OrderDetails` Values (11030, 59, 55, 100, 0.25);
INSERT INTO `OrderDetails` Values (11031, 1, 18, 45, 0.0);
INSERT INTO `OrderDetails` Values (11031, 13, 6, 80, 0.0);
INSERT INTO `OrderDetails` Values (11031, 24, 4.5, 21, 0.0);
INSERT INTO `OrderDetails` Values (11031, 64, 33.25, 20, 0.0);
INSERT INTO `OrderDetails` Values (11031, 71, 21.5, 16, 0.0);
INSERT INTO `OrderDetails` Values (11032, 36, 19, 35, 0.0);
INSERT INTO `OrderDetails` Values (11032, 38, 263.5, 25, 0.0);
INSERT INTO `OrderDetails` Values (11032, 59, 55, 30, 0.0);
INSERT INTO `OrderDetails` Values (11033, 53, 32.8, 70, 0.1);
INSERT INTO `OrderDetails` Values (11033, 69, 36, 36, 0.1);
INSERT INTO `OrderDetails` Values (11034, 21, 10, 15, 0.1);
INSERT INTO `OrderDetails` Values (11034, 44, 19.45, 12, 0.0);
INSERT INTO `OrderDetails` Values (11034, 61, 28.5, 6, 0.0);
INSERT INTO `OrderDetails` Values (11035, 1, 18, 10, 0.0);
INSERT INTO `OrderDetails` Values (11035, 35, 18, 60, 0.0);
INSERT INTO `OrderDetails` Values (11035, 42, 14, 30, 0.0);
INSERT INTO `OrderDetails` Values (11035, 54, 7.45, 10, 0.0);
INSERT INTO `OrderDetails` Values (11036, 13, 6, 7, 0.0);
INSERT INTO `OrderDetails` Values (11036, 59, 55, 30, 0.0);
INSERT INTO `OrderDetails` Values (11037, 70, 15, 4, 0.0);
INSERT INTO `OrderDetails` Values (11038, 40, 18.4, 5, 0.2);
INSERT INTO `OrderDetails` Values (11038, 52, 7, 2, 0.0);
INSERT INTO `OrderDetails` Values (11038, 71, 21.5, 30, 0.0);
INSERT INTO `OrderDetails` Values (11039, 28, 45.6, 20, 0.0);
INSERT INTO `OrderDetails` Values (11039, 35, 18, 24, 0.0);
INSERT INTO `OrderDetails` Values (11039, 49, 20, 60, 0.0);
INSERT INTO `OrderDetails` Values (11039, 57, 19.5, 28, 0.0);
INSERT INTO `OrderDetails` Values (11040, 21, 10, 20, 0.0);
INSERT INTO `OrderDetails` Values (11041, 2, 19, 30, 0.2);
INSERT INTO `OrderDetails` Values (11041, 63, 43.9, 30, 0.0);
INSERT INTO `OrderDetails` Values (11042, 44, 19.45, 15, 0.0);
INSERT INTO `OrderDetails` Values (11042, 61, 28.5, 4, 0.0);
INSERT INTO `OrderDetails` Values (11043, 11, 21, 10, 0.0);
INSERT INTO `OrderDetails` Values (11044, 62, 49.3, 12, 0.0);
INSERT INTO `OrderDetails` Values (11045, 33, 2.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (11045, 51, 53, 24, 0.0);
INSERT INTO `OrderDetails` Values (11046, 12, 38, 20, 0.05);
INSERT INTO `OrderDetails` Values (11046, 32, 32, 15, 0.05);
INSERT INTO `OrderDetails` Values (11046, 35, 18, 18, 0.05);
INSERT INTO `OrderDetails` Values (11047, 1, 18, 25, 0.25);
INSERT INTO `OrderDetails` Values (11047, 5, 21.35, 30, 0.25);
INSERT INTO `OrderDetails` Values (11048, 68, 12.5, 42, 0.0);
INSERT INTO `OrderDetails` Values (11049, 2, 19, 10, 0.2);
INSERT INTO `OrderDetails` Values (11049, 12, 38, 4, 0.2);
INSERT INTO `OrderDetails` Values (11050, 76, 18, 50, 0.1);
INSERT INTO `OrderDetails` Values (11051, 24, 4.5, 10, 0.2);
INSERT INTO `OrderDetails` Values (11052, 43, 46, 30, 0.2);
INSERT INTO `OrderDetails` Values (11052, 61, 28.5, 10, 0.2);
INSERT INTO `OrderDetails` Values (11053, 18, 62.5, 35, 0.2);
INSERT INTO `OrderDetails` Values (11053, 32, 32, 20, 0.0);
INSERT INTO `OrderDetails` Values (11053, 64, 33.25, 25, 0.2);
INSERT INTO `OrderDetails` Values (11054, 33, 2.5, 10, 0.0);
INSERT INTO `OrderDetails` Values (11054, 67, 14, 20, 0.0);
INSERT INTO `OrderDetails` Values (11055, 24, 4.5, 15, 0.0);
INSERT INTO `OrderDetails` Values (11055, 25, 14, 15, 0.0);
INSERT INTO `OrderDetails` Values (11055, 51, 53, 20, 0.0);
INSERT INTO `OrderDetails` Values (11055, 57, 19.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (11056, 7, 30, 40, 0.0);
INSERT INTO `OrderDetails` Values (11056, 55, 24, 35, 0.0);
INSERT INTO `OrderDetails` Values (11056, 60, 34, 50, 0.0);
INSERT INTO `OrderDetails` Values (11057, 70, 15, 3, 0.0);
INSERT INTO `OrderDetails` Values (11058, 21, 10, 3, 0.0);
INSERT INTO `OrderDetails` Values (11058, 60, 34, 21, 0.0);
INSERT INTO `OrderDetails` Values (11058, 61, 28.5, 4, 0.0);
INSERT INTO `OrderDetails` Values (11059, 13, 6, 30, 0.0);
INSERT INTO `OrderDetails` Values (11059, 17, 39, 12, 0.0);
INSERT INTO `OrderDetails` Values (11059, 60, 34, 35, 0.0);
INSERT INTO `OrderDetails` Values (11060, 60, 34, 4, 0.0);
INSERT INTO `OrderDetails` Values (11060, 77, 13, 10, 0.0);
INSERT INTO `OrderDetails` Values (11061, 60, 34, 15, 0.0);
INSERT INTO `OrderDetails` Values (11062, 53, 32.8, 10, 0.2);
INSERT INTO `OrderDetails` Values (11062, 70, 15, 12, 0.2);
INSERT INTO `OrderDetails` Values (11063, 34, 14, 30, 0.0);
INSERT INTO `OrderDetails` Values (11063, 40, 18.4, 40, 0.1);
INSERT INTO `OrderDetails` Values (11063, 41, 9.65, 30, 0.1);
INSERT INTO `OrderDetails` Values (11064, 17, 39, 77, 0.1);
INSERT INTO `OrderDetails` Values (11064, 41, 9.65, 12, 0.0);
INSERT INTO `OrderDetails` Values (11064, 53, 32.8, 25, 0.1);
INSERT INTO `OrderDetails` Values (11064, 55, 24, 4, 0.1);
INSERT INTO `OrderDetails` Values (11064, 68, 12.5, 55, 0.0);
INSERT INTO `OrderDetails` Values (11065, 30, 25.89, 4, 0.25);
INSERT INTO `OrderDetails` Values (11065, 54, 7.45, 20, 0.25);
INSERT INTO `OrderDetails` Values (11066, 16, 17.45, 3, 0.0);
INSERT INTO `OrderDetails` Values (11066, 19, 9.2, 42, 0.0);
INSERT INTO `OrderDetails` Values (11066, 34, 14, 35, 0.0);
INSERT INTO `OrderDetails` Values (11067, 41, 9.65, 9, 0.0);
INSERT INTO `OrderDetails` Values (11068, 28, 45.6, 8, 0.15);
INSERT INTO `OrderDetails` Values (11068, 43, 46, 36, 0.15);
INSERT INTO `OrderDetails` Values (11068, 77, 13, 28, 0.15);
INSERT INTO `OrderDetails` Values (11069, 39, 18, 20, 0.0);
INSERT INTO `OrderDetails` Values (11070, 1, 18, 40, 0.15);
INSERT INTO `OrderDetails` Values (11070, 2, 19, 20, 0.15);
INSERT INTO `OrderDetails` Values (11070, 16, 17.45, 30, 0.15);
INSERT INTO `OrderDetails` Values (11070, 31, 12.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (11071, 7, 30, 15, 0.05);
INSERT INTO `OrderDetails` Values (11071, 13, 6, 10, 0.05);
INSERT INTO `OrderDetails` Values (11072, 2, 19, 8, 0.0);
INSERT INTO `OrderDetails` Values (11072, 41, 9.65, 40, 0.0);
INSERT INTO `OrderDetails` Values (11072, 50, 16.25, 22, 0.0);
INSERT INTO `OrderDetails` Values (11072, 64, 33.25, 130, 0.0);
INSERT INTO `OrderDetails` Values (11073, 11, 21, 10, 0.0);
INSERT INTO `OrderDetails` Values (11073, 24, 4.5, 20, 0.0);
INSERT INTO `OrderDetails` Values (11074, 16, 17.45, 14, 0.05);
INSERT INTO `OrderDetails` Values (11075, 2, 19, 10, 0.15);
INSERT INTO `OrderDetails` Values (11075, 46, 12, 30, 0.15);
INSERT INTO `OrderDetails` Values (11075, 76, 18, 2, 0.15);
INSERT INTO `OrderDetails` Values (11076, 6, 25, 20, 0.25);
INSERT INTO `OrderDetails` Values (11076, 14, 23.25, 20, 0.25);
INSERT INTO `OrderDetails` Values (11076, 19, 9.2, 10, 0.25);
INSERT INTO `OrderDetails` Values (11077, 2, 19, 24, 0.2);
INSERT INTO `OrderDetails` Values (11077, 3, 10, 4, 0.0);
INSERT INTO `OrderDetails` Values (11077, 4, 22, 1, 0.0);
INSERT INTO `OrderDetails` Values (11077, 6, 25, 1, 0.02);
INSERT INTO `OrderDetails` Values (11077, 7, 30, 1, 0.05);
INSERT INTO `OrderDetails` Values (11077, 8, 40, 2, 0.1);
INSERT INTO `OrderDetails` Values (11077, 10, 31, 1, 0.0);
INSERT INTO `OrderDetails` Values (11077, 12, 38, 2, 0.05);
INSERT INTO `OrderDetails` Values (11077, 13, 6, 4, 0.0);
INSERT INTO `OrderDetails` Values (11077, 14, 23.25, 1, 0.03);
INSERT INTO `OrderDetails` Values (11077, 16, 17.45, 2, 0.03);
INSERT INTO `OrderDetails` Values (11077, 20, 81, 1, 0.04);
INSERT INTO `OrderDetails` Values (11077, 23, 9, 2, 0.0);
INSERT INTO `OrderDetails` Values (11077, 32, 32, 1, 0.0);
INSERT INTO `OrderDetails` Values (11077, 39, 18, 2, 0.05);
INSERT INTO `OrderDetails` Values (11077, 41, 9.65, 3, 0.0);
INSERT INTO `OrderDetails` Values (11077, 46, 12, 3, 0.02);
INSERT INTO `OrderDetails` Values (11077, 52, 7, 2, 0.0);
INSERT INTO `OrderDetails` Values (11077, 55, 24, 2, 0.0);
INSERT INTO `OrderDetails` Values (11077, 60, 34, 2, 0.06);
INSERT INTO `OrderDetails` Values (11077, 64, 33.25, 2, 0.03);
INSERT INTO `OrderDetails` Values (11077, 66, 17, 1, 0.0);
INSERT INTO `OrderDetails` Values (11077, 73, 15, 2, 0.01);
INSERT INTO `OrderDetails` Values (11077, 75, 7.75, 4, 0.0);
INSERT INTO `OrderDetails` Values (11077, 77, 13, 2, 0.0);



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



TRUNCATE TABLE Region;
INSERT INTO Region (RegionID, RegionDescription)
VALUES(1, 'Eastern                                           ');
INSERT INTO Region (RegionID,  RegionDescription)
VALUES(2, 'Westerns                                           ');
INSERT INTO Region (RegionID, RegionDescription)
VALUES(3, 'Northern                                          ');
INSERT INTO Region (RegionID, RegionDescription)
VALUES(4, 'Southern                                          ');


TRUNCATE TABLE Shippers;
INSERT INTO Shippers (ShipperID, CompanyName, Phone)
VALUES(1, 'Speedy Express', '(503) 555-9831');
INSERT INTO Shippers (ShipperID, CompanyName, Phone)
VALUES(2, 'United Package', '(503) 555-3199');
INSERT INTO Shippers (ShipperID, CompanyName, Phone)
VALUES(3, 'Federal Shipping', '(503) 555-9931');


TRUNCATE TABLE Suppliers;
INSERT INTO Suppliers VALUES(1,'Exotic Liquids','Charlotte Cooper','Purchasing Manager','49 Gilbert St.','London',NULL,'EC1 4SD','UK','(171) 555-2222',NULL,NULL);
INSERT INTO Suppliers VALUES(2,'New Orleans Cajun Delights','Shelley Burke','Order Administrator','P.O. Box 78934','New Orleans','LA','70117','USA','(100) 555-4822',NULL,'CAJUN.HTM');
INSERT INTO Suppliers VALUES(3,'Grandma Kelly''s Homestead','Regina Murphy','Sales Representative','707 Oxford Rd.','Ann Arbor','MI','48104','USA','(313) 555-5735','(313) 555-3349',NULL);
INSERT INTO Suppliers VALUES(4,'Tokyo Traders','Yoshi Nagase','Marketing Manager','9-8 Sekimai
Musashino-shi','Tokyo',NULL,'100','Japan','(03) 3555-5011',NULL,NULL);
INSERT INTO Suppliers VALUES(5,'Cooperativa de Quesos ''Las Cabras''','Antonio del Valle Saavedra ','Export Administrator','Calle del Rosal 4','Oviedo','Asturias','33007','Spain','(98) 598 76 54',NULL,NULL);
INSERT INTO Suppliers VALUES(6,'Mayumi''s','Mayumi Ohno','Marketing Representative','92 Setsuko
Chuo-ku','Osaka',NULL,'545','Japan','(06) 431-7877',NULL,'Mayumi''s (on the World Wide Web)http://www.microsoft.com/accessdev/sampleapps/mayumi.htm');
INSERT INTO Suppliers VALUES(7,'Pavlova, Ltd.','Ian Devling','Marketing Manager','74 Rose St.
Moonie Ponds','Melbourne','Victoria','3058','Australia','(03) 444-2343','(03) 444-6588',NULL);
INSERT INTO Suppliers VALUES(8,'Specialty Biscuits, Ltd.','Peter Wilson','Sales Representative','29 King''s Way','Manchester',NULL,'M14 GSD','UK','(161) 555-4448',NULL,NULL);
INSERT INTO Suppliers VALUES(9,'PB Knckebrd AB','Lars Peterson','Sales Agent','Kaloadagatan 13','Gteborg',NULL,'S-345 67','Sweden ','031-987 65 43','031-987 65 91',NULL);
INSERT INTO Suppliers VALUES(10,'Refrescos Americanas LTDA','Carlos Diaz','Marketing Manager','Av. das Americanas 12.890','So Paulo',NULL,'5442','Brazil','(11) 555 4640',NULL,NULL);
INSERT INTO Suppliers VALUES(11,'Heli Swaren GmbH & Co. KG','Petra Winkler','Sales Manager','Tiergartenstrae 5','Berlin',NULL,'10785','Germany','(010) 9984510',NULL,NULL);
INSERT INTO Suppliers VALUES(12,'Plutzer Lebensmittelgromrkte AG','Martin Bein','International Marketing Mgr.','Bogenallee 51','Frankfurt',NULL,'60439','Germany','(069) 992755',NULL,'Plutzer (on the World Wide Web)http://www.microsoft.com/accessdev/sampleapps/plutzer.htm');
INSERT INTO Suppliers VALUES(13,'Nord-Ost-Fisch Handelsgesellschaft mbH','Sven Petersen','Coordinator Foreign Markets','Frahmredder 112a','Cuxhaven',NULL,'27478','Germany','(04721) 8713','(04721) 8714',NULL);
INSERT INTO Suppliers VALUES(14,'Formaggi Fortini s.r.l.','Elio Rossi','Sales Representative','Viale Dante, 75','Ravenna',NULL,'48100','Italy','(0544) 60323','(0544) 60603','FORMAGGI.HTM');
INSERT INTO Suppliers VALUES(15,'Norske Meierier','Beate Vileid','Marketing Manager','Hatlevegen 5','Sandvika',NULL,'1320','Norway','(0)2-953010',NULL,NULL);
INSERT INTO Suppliers VALUES(16,'Bigfoot Breweries','Cheryl Saylor','Regional Account Rep.','3400 - 8th Avenue
Suite 210','Bend','OR','97101','USA','(503) 555-9931',NULL,NULL);
INSERT INTO Suppliers VALUES(17,'Svensk Sjfda AB','Michael Bjrn','Sales Representative','Brovallavgen 231','Stockholm',NULL,'S-123 45','Sweden','08-123 45 67',NULL,NULL);
INSERT INTO Suppliers VALUES(18,'Aux joyeux ecclsiastiques','Guylne Nodier','Sales Manager','203, Rue des Francs-Bourgeois','Paris',NULL,'75004','France','(1) 03.83.00.68','(1) 03.83.00.62',NULL);
INSERT INTO Suppliers VALUES(19,'New England Seafood Cannery','Robb Merchant','Wholesale Account Agent','Order Processing Dept.
2100 Paul Revere Blvd.','Boston','MA','02134','USA','(617) 555-3267','(617) 555-3389',NULL);
INSERT INTO Suppliers VALUES(20,'Leka Trading','Chandra Leka','Owner','471 Serangoon Loop, Suite 402','Singapore',NULL,'0512','Singapore','555-8787',NULL,NULL);
INSERT INTO Suppliers VALUES(21,'Lyngbysild','Niels Petersen','Sales Manager','Lyngbysild
Fiskebakken 10','Lyngby',NULL,'2800','Denmark','43844108','43844115',NULL);
INSERT INTO Suppliers VALUES(22,'Zaanse Snoepfabriek','Dirk Luchte','Accounting Manager','Verkoop
Rijnweg 22','Zaandam',NULL,'9999 ZZ','Netherlands','(12345) 1212','(12345) 1210',NULL);
INSERT INTO Suppliers VALUES(23,'Karkki Oy','Anne Heikkonen','Product Manager','Valtakatu 12','Lappeenranta',NULL,'53120','Finland','(953) 10956',NULL,NULL);
INSERT INTO Suppliers VALUES(24,'G''day, Mate','Wendy Mackenzie','Sales Representative','170 Prince Edward Parade
Hunter''s Hill','Sydney','NSW','2042','Australia','(02) 555-5914','(02) 555-4873','G''day Mate (on the World Wide Web)http://www.microsoft.com/accessdev/sampleapps/gdaymate.htm');
INSERT INTO Suppliers VALUES(25,'Ma Maison','Jean-Guy Lauzon','Marketing Manager','2960 Rue St. Laurent','Montral','Qubec','H1J 1C3','Canada','(514) 555-9022',NULL,NULL);
INSERT INTO Suppliers VALUES(26,'Pasta Buttini s.r.l.','Giovanni Giudici','Order Administrator','Via dei Gelsomini, 153','Salerno',NULL,'84100','Italy','(089) 6547665','(089) 6547667',NULL);
INSERT INTO Suppliers VALUES(27,'Escargots Nouveaux','Marie Delamare','Sales Manager','22, rue H. Voiron','Montceau',NULL,'71300','France','85.57.00.07',NULL,NULL);
INSERT INTO Suppliers VALUES(28,'Gai pturage','Eliane Noz','Sales Representative','Bat. B
3, rue des Alpes','Annecy',NULL,'74000','France','38.76.98.06','38.76.98.58',NULL);
INSERT INTO Suppliers VALUES(29,'Forts d''rables','Chantal Goulet','Accounting Manager','148 rue Chasseur','Ste-Hyacinthe','Qubec','J2S 7S8','Canada','(514) 555-2955','(514) 555-2921',NULL);


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

ALTER TABLE `OrderDetails` ADD CONSTRAINT `FK_Order_Details_Orders` 
    FOREIGN KEY (`OrderID`) REFERENCES `Orders` (`OrderID`);

ALTER TABLE `OrderDetails` ADD CONSTRAINT `FK_Order_Details_Products` 
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
    
/* This table was added to support test cases with a multi part foreign key */

CREATE TABLE `EmployeeOrderDetails` (
	`EmployeeID` INTEGER NOT NULL,
	`OrderID` INTEGER NOT NULL,
	`ProductID` INTEGER NOT NULL,
	CONSTRAINT `PK_EmployeeOrderDetails` PRIMARY KEY ( `EmployeeID`, `OrderID`, `ProductID`)
);

ALTER TABLE `EmployeeOrderDetails` ADD CONSTRAINT `FK_EmpoyeeOrderDetails1`
	FOREIGN KEY (`EmployeeID`) REFERENCES `Employees` (`EmployeeID`) ON DELETE CASCADE;

ALTER TABLE `EmployeeOrderDetails` ADD CONSTRAINT `FK_EmpoyeeOrderDetails2`
	FOREIGN KEY (`OrderID`, `ProductID`) REFERENCES `OrderDetails` (`OrderID`, `ProductID`)  ON DELETE CASCADE;
    
INSERT INTO `EmployeeOrderDetails`  
SELECT o.EmployeeID, od.OrderId, od.ProductId FROM `Orders` o JOIN `OrderDetails` od ON o.OrderId = od.OrderId; 
    
