/*
** Copyright Microsoft, Inc. 1994 - 2000
** All Rights Reserved.
** Modified 2010 from Valon Hoti 
** For Most Used Tips 
*/

CREATE TABLE "Employees" (
	"EmployeeID" "int" IDENTITY (1, 1) NOT NULL ,
	"LastName" nvarchar (20) NOT NULL ,
	"FirstName" nvarchar (10) NOT NULL ,
	"Title" nvarchar (30) NULL ,
	"TitleOfCourtesy" nvarchar (25) NULL ,
	"BirthDate" "datetime" NULL ,
	"HireDate" "datetime" NULL ,
	"Address" nvarchar (60) NULL ,
	"City" nvarchar (15) NULL ,
	"Region" nvarchar (15) NULL ,
	"PostalCode" nvarchar (10) NULL ,
	"Country" nvarchar (15) NULL ,
	"HomePhone" nvarchar (24) NULL ,
	"Extension" nvarchar (4) NULL ,
	"Photo" nvarchar (30) NULL ,
	"Notes" varchar(max) NULL ,
	"ReportsTo" "int" NULL ,
	"PhotoPath" nvarchar (255) NULL ,
	"Salary" decimal(18,2) NULL,
	CONSTRAINT "PK_Employees" PRIMARY KEY  CLUSTERED 
	(
		"EmployeeID"
	),
	CONSTRAINT "FK_Employees_Employees" FOREIGN KEY 
	(
		"ReportsTo"
	) REFERENCES "dbo"."Employees" (
		"EmployeeID"
	),
	CONSTRAINT "CK_Birthdate" CHECK (BirthDate < getdate())
);

CREATE  INDEX "LastName" ON "dbo"."Employees"("LastName");
CREATE  INDEX "PostalCode" ON "dbo"."Employees"("PostalCode");


CREATE TABLE "Categories" (
	"CategoryID" "int" IDENTITY (1, 1) NOT NULL ,
	"CategoryName" nvarchar (15) NOT NULL ,
	"Description" nvarchar(max) NULL ,
	"Picture" nvarchar(30) NULL ,
	CONSTRAINT "PK_Categories" PRIMARY KEY  CLUSTERED 
	(
		"CategoryID"
	)
);
CREATE  INDEX "CategoryName" ON "dbo"."Categories"("CategoryName");


CREATE TABLE "Customers" (
	"CustomerID" nchar (5) NOT NULL ,
	"CompanyName" nvarchar (40) NOT NULL ,
	"ContactName" nvarchar (30) NULL ,
	"ContactTitle" nvarchar (30) NULL ,
	"Address" nvarchar (60) NULL ,
	"City" nvarchar (15) NULL ,
	"Region" nvarchar (15) NULL ,
	"PostalCode" nvarchar (10) NULL ,
	"Country" nvarchar (15) NULL ,
	"Phone" nvarchar (24) NULL ,
	"Fax" nvarchar (24) NULL ,
	CONSTRAINT "PK_Customers" PRIMARY KEY  CLUSTERED 
	(
		"CustomerID"
	)
);

CREATE  INDEX "City" ON "dbo"."Customers"("City");
CREATE  INDEX "CompanyName" ON "dbo"."Customers"("CompanyName");
CREATE  INDEX "PostalCode" ON "dbo"."Customers"("PostalCode");
CREATE  INDEX "Region" ON "dbo"."Customers"("Region");


CREATE TABLE "Shippers" (
	"ShipperID" "int" IDENTITY (1, 1) NOT NULL ,
	"CompanyName" nvarchar (40) NOT NULL ,
	"Phone" nvarchar (24) NULL ,
	CONSTRAINT "PK_Shippers" PRIMARY KEY  CLUSTERED 
	(
		"ShipperID"
	)
);

CREATE TABLE "Suppliers" (
	"SupplierID" "int" IDENTITY (1, 1) NOT NULL ,
	"CompanyName" nvarchar (40) NOT NULL ,
	"ContactName" nvarchar (30) NULL ,
	"ContactTitle" nvarchar (30) NULL ,
	"Address" nvarchar (60) NULL ,
	"City" nvarchar (15) NULL ,
	"Region" nvarchar (15) NULL ,
	"PostalCode" nvarchar (10) NULL ,
	"Country" nvarchar (15) NULL ,
	"Phone" nvarchar (24) NULL ,
	"Fax" nvarchar (24) NULL ,
	"HomePage" nvarchar(max) NULL ,
	CONSTRAINT "PK_Suppliers" PRIMARY KEY  CLUSTERED 
	(
		"SupplierID"
	)
);

CREATE  INDEX "CompanyName" ON "dbo"."Suppliers"("CompanyName");
CREATE  INDEX "PostalCode" ON "dbo"."Suppliers"("PostalCode");


CREATE TABLE "Orders" (
	"OrderID" "int" IDENTITY (1, 1) NOT NULL ,
	"CustomerID" nchar (5) NULL ,
	"EmployeeID" "int" NULL ,
	"OrderDate" "datetime" NULL ,
	"RequiredDate" "datetime" NULL ,
	"ShippedDate" "datetime" NULL ,
	"ShipVia" "int" NULL ,
	"Freight" "money" NULL CONSTRAINT "DF_Orders_Freight" DEFAULT (0),
	"ShipName" nvarchar (40) NULL ,
	"ShipAddress" nvarchar (60) NULL ,
	"ShipCity" nvarchar (15) NULL ,
	"ShipRegion" nvarchar (15) NULL ,
	"ShipPostalCode" nvarchar (10) NULL ,
	"ShipCountry" nvarchar (15) NULL ,
	CONSTRAINT "PK_Orders" PRIMARY KEY  CLUSTERED 
	(
		"OrderID"
	),
	CONSTRAINT "FK_Orders_Customers" FOREIGN KEY 
	(
		"CustomerID"
	) REFERENCES "dbo"."Customers" (
		"CustomerID"
	),
	CONSTRAINT "FK_Orders_Employees" FOREIGN KEY 
	(
		"EmployeeID"
	) REFERENCES "dbo"."Employees" (
		"EmployeeID"
	),
	CONSTRAINT "FK_Orders_Shippers" FOREIGN KEY 
	(
		"ShipVia"
	) REFERENCES "dbo"."Shippers" (
		"ShipperID"
	)
);

CREATE  INDEX "CustomerID" ON "dbo"."Orders"("CustomerID");
CREATE  INDEX "CustomersOrders" ON "dbo"."Orders"("CustomerID");
CREATE  INDEX "EmployeeID" ON "dbo"."Orders"("EmployeeID");
CREATE  INDEX "EmployeesOrders" ON "dbo"."Orders"("EmployeeID");
CREATE  INDEX "OrderDate" ON "dbo"."Orders"("OrderDate");
CREATE  INDEX "ShippedDate" ON "dbo"."Orders"("ShippedDate");
CREATE  INDEX "ShippersOrders" ON "dbo"."Orders"("ShipVia");
CREATE  INDEX "ShipPostalCode" ON "dbo"."Orders"("ShipPostalCode");


CREATE TABLE "Products" (
	"ProductID" "int" IDENTITY (1, 1) NOT NULL ,
	"ProductName" nvarchar (40) NOT NULL ,
	"SupplierID" "int" NULL ,
	"CategoryID" "int" NULL ,
	"QuantityPerUnit" nvarchar (20) NULL ,
	"UnitPrice" "money" NULL CONSTRAINT "DF_Products_UnitPrice" DEFAULT (0),
	"UnitsInStock" "smallint" NULL CONSTRAINT "DF_Products_UnitsInStock" DEFAULT (0),
	"UnitsOnOrder" "smallint" NULL CONSTRAINT "DF_Products_UnitsOnOrder" DEFAULT (0),
	"ReorderLevel" "smallint" NULL CONSTRAINT "DF_Products_ReorderLevel" DEFAULT (0),
	"Discontinued" "bit" NOT NULL CONSTRAINT "DF_Products_Discontinued" DEFAULT (0),
	CONSTRAINT "PK_Products" PRIMARY KEY  CLUSTERED 
	(
		"ProductID"
	),
	CONSTRAINT "FK_Products_Categories" FOREIGN KEY 
	(
		"CategoryID"
	) REFERENCES "dbo"."Categories" (
		"CategoryID"
	),
	CONSTRAINT "FK_Products_Suppliers" FOREIGN KEY 
	(
		"SupplierID"
	) REFERENCES "dbo"."Suppliers" (
		"SupplierID"
	),
	CONSTRAINT "CK_Products_UnitPrice" CHECK (UnitPrice >= 0),
	CONSTRAINT "CK_ReorderLevel" CHECK (ReorderLevel >= 0),
	CONSTRAINT "CK_UnitsInStock" CHECK (UnitsInStock >= 0),
	CONSTRAINT "CK_UnitsOnOrder" CHECK (UnitsOnOrder >= 0)
);

CREATE  INDEX "CategoriesProducts" ON "dbo"."Products"("CategoryID");
CREATE  INDEX "CategoryID" ON "dbo"."Products"("CategoryID");
CREATE  INDEX "ProductName" ON "dbo"."Products"("ProductName");
CREATE  INDEX "SupplierID" ON "dbo"."Products"("SupplierID");
CREATE  INDEX "SuppliersProducts" ON "dbo"."Products"("SupplierID");


CREATE TABLE "Order Details" (
	"OrderID" "int" NOT NULL ,
	"ProductID" "int" NOT NULL ,
	"UnitPrice" "money" NOT NULL CONSTRAINT "DF_Order_Details_UnitPrice" DEFAULT (0),
	"Quantity" "smallint" NOT NULL CONSTRAINT "DF_Order_Details_Quantity" DEFAULT (1),
	"Discount" "real" NOT NULL CONSTRAINT "DF_Order_Details_Discount" DEFAULT (0),
	CONSTRAINT "PK_Order_Details" PRIMARY KEY  CLUSTERED 
	(
		"OrderID",
		"ProductID"
	),
	CONSTRAINT "FK_Order_Details_Orders" FOREIGN KEY 
	(
		"OrderID"
	) REFERENCES "dbo"."Orders" (
		"OrderID"
	),
	CONSTRAINT "FK_Order_Details_Products" FOREIGN KEY 
	(
		"ProductID"
	) REFERENCES "dbo"."Products" (
		"ProductID"
	),
	CONSTRAINT "CK_Discount" CHECK (Discount >= 0 and (Discount <= 1)),
	CONSTRAINT "CK_Quantity" CHECK (Quantity > 0),
	CONSTRAINT "CK_UnitPrice" CHECK (UnitPrice >= 0)
);
CREATE  INDEX "OrderID" ON "dbo"."Order Details"("OrderID");
CREATE  INDEX "OrdersOrder_Details" ON "dbo"."Order Details"("OrderID");
CREATE  INDEX "ProductID" ON "dbo"."Order Details"("ProductID");
CREATE  INDEX "ProductsOrder_Details" ON "dbo"."Order Details"("ProductID");

set identity_insert "Categories" on;
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(1,'Beverages','Soft drinks, coffees, teas, beers, and ales',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(2,'Condiments','Sweet and savory sauces, relishes, spreads, and seasonings',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(3,'Confections','Desserts, candies, and sweet breads',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(4,'Dairy Products','Cheeses',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(5,'Grains/Cereals','Breads, crackers, pasta, and cereal',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(6,'Meat/Poultry','Prepared meats',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(7,'Produce','Dried fruit and bean curd',NULL);
INSERT "Categories"("CategoryID","CategoryName","Description","Picture") VALUES(8,'Seafood','Seaweed and fish',NULL);
set identity_insert "Categories" off;

INSERT "Customers" VALUES('ALFKI','Alfreds Futterkiste','Maria Anders','Sales Representative','Obere Str. 57','Berlin',NULL,'12209','Germany','030-0074321','030-0076545');
INSERT "Customers" VALUES('ANATR','Ana Trujillo Emparedados y helados','Ana Trujillo','Owner','Avda. de la Constituci�n 2222','M�xico D.F.',NULL,'05021','Mexico','(5) 555-4729','(5) 555-3745');
INSERT "Customers" VALUES('ANTON','Antonio Moreno Taquer�a','Antonio Moreno','Owner','Mataderos  2312','M�xico D.F.',NULL,'05023','Mexico','(5) 555-3932',NULL);
INSERT "Customers" VALUES('AROUT','Around the Horn','Thomas Hardy','Sales Representative','120 Hanover Sq.','London',NULL,'WA1 1DP','UK','(171) 555-7788','(171) 555-6750');
INSERT "Customers" VALUES('BERGS','Berglunds snabbk�p','Christina Berglund','Order Administrator','Berguvsv�gen  8','Lule�',NULL,'S-958 22','Sweden','0921-12 34 65','0921-12 34 67');
INSERT "Customers" VALUES('BLAUS','Blauer See Delikatessen','Hanna Moos','Sales Representative','Forsterstr. 57','Mannheim',NULL,'68306','Germany','0621-08460','0621-08924');
INSERT "Customers" VALUES('BLONP','Blondesddsl p�re et fils','Fr�d�rique Citeaux','Marketing Manager','24, place Kl�ber','Strasbourg',NULL,'67000','France','88.60.15.31','88.60.15.32');
INSERT "Customers" VALUES('BOLID','B�lido Comidas preparadas','Mart�n Sommer','Owner','C/ Araquil, 67','Madrid',NULL,'28023','Spain','(91) 555 22 82','(91) 555 91 99');
INSERT "Customers" VALUES('BONAP','Bon app''','Laurence Lebihan','Owner','12, rue des Bouchers','Marseille',NULL,'13008','France','91.24.45.40','91.24.45.41');

INSERT "Customers" VALUES('BOTTM','Bottom-Dollar Markets','Elizabeth Lincoln','Accounting Manager','23 Tsawassen Blvd.','Tsawassen','BC','T2F 8M4','Canada','(604) 555-4729','(604) 555-3745');
INSERT "Customers" VALUES('BSBEV','B''s Beverages','Victoria Ashworth','Sales Representative','Fauntleroy Circus','London',NULL,'EC2 5NT','UK','(171) 555-1212',NULL);
INSERT "Customers" VALUES('CACTU','Cactus Comidas para llevar','Patricio Simpson','Sales Agent','Cerrito 333','Buenos Aires',NULL,'1010','Argentina','(1) 135-5555','(1) 135-4892');
INSERT "Customers" VALUES('CENTC','Centro comercial Moctezuma','Francisco Chang','Marketing Manager','Sierras de Granada 9993','M�xico D.F.',NULL,'05022','Mexico','(5) 555-3392','(5) 555-7293');
INSERT "Customers" VALUES('CHOPS','Chop-suey Chinese','Yang Wang','Owner','Hauptstr. 29','Bern',NULL,'3012','Switzerland','0452-076545',NULL);
INSERT "Customers" VALUES('COMMI','Com�rcio Mineiro','Pedro Afonso','Sales Associate','Av. dos Lus�adas, 23','Sao Paulo','SP','05432-043','Brazil','(11) 555-7647',NULL);
INSERT "Customers" VALUES('CONSH','Consolidated Holdings','Elizabeth Brown','Sales Representative','Berkeley Gardens 12  Brewery','London',NULL,'WX1 6LT','UK','(171) 555-2282','(171) 555-9199');
INSERT "Customers" VALUES('DRACD','Drachenblut Delikatessen','Sven Ottlieb','Order Administrator','Walserweg 21','Aachen',NULL,'52066','Germany','0241-039123','0241-059428');
INSERT "Customers" VALUES('DUMON','Du monde entier','Janine Labrune','Owner','67, rue des Cinquante Otages','Nantes',NULL,'44000','France','40.67.88.88','40.67.89.89');
INSERT "Customers" VALUES('EASTC','Eastern Connection','Ann Devon','Sales Agent','35 King George','London',NULL,'WX3 6FW','UK','(171) 555-0297','(171) 555-3373');
INSERT "Customers" VALUES('ERNSH','Ernst Handel','Roland Mendel','Sales Manager','Kirchgasse 6','Graz',NULL,'8010','Austria','7675-3425','7675-3426');

INSERT "Customers" VALUES('FAMIA','Familia Arquibaldo','Aria Cruz','Marketing Assistant','Rua Or�s, 92','Sao Paulo','SP','05442-030','Brazil','(11) 555-9857',NULL);
INSERT "Customers" VALUES('FISSA','FISSA Fabrica Inter. Salchichas S.A.','Diego Roel','Accounting Manager','C/ Moralzarzal, 86','Madrid',NULL,'28034','Spain','(91) 555 94 44','(91) 555 55 93');
INSERT "Customers" VALUES('FOLIG','Folies gourmandes','Martine Ranc�','Assistant Sales Agent','184, chauss�e de Tournai','Lille',NULL,'59000','France','20.16.10.16','20.16.10.17');
INSERT "Customers" VALUES('FOLKO','Folk och f� HB','Maria Larsson','Owner','�kergatan 24','Br�cke',NULL,'S-844 67','Sweden','0695-34 67 21',NULL);
INSERT "Customers" VALUES('FRANK','Frankenversand','Peter Franken','Marketing Manager','Berliner Platz 43','M�nchen',NULL,'80805','Germany','089-0877310','089-0877451');
INSERT "Customers" VALUES('FRANR','France restauration','Carine Schmitt','Marketing Manager','54, rue Royale','Nantes',NULL,'44000','France','40.32.21.21','40.32.21.20');
INSERT "Customers" VALUES('FRANS','Franchi S.p.A.','Paolo Accorti','Sales Representative','Via Monte Bianco 34','Torino',NULL,'10100','Italy','011-4988260','011-4988261');
INSERT "Customers" VALUES('FURIB','Furia Bacalhau e Frutos do Mar','Lino Rodriguez','Sales Manager','Jardim das rosas n. 32','Lisboa',NULL,'1675','Portugal','(1) 354-2534','(1) 354-2535');
INSERT "Customers" VALUES('GALED','Galer�a del gastr�nomo','Eduardo Saavedra','Marketing Manager','Rambla de Catalu�a, 23','Barcelona',NULL,'08022','Spain','(93) 203 4560','(93) 203 4561');
INSERT "Customers" VALUES('GODOS','Godos Cocina T�pica','Jos� Pedro Freyre','Sales Manager','C/ Romero, 33','Sevilla',NULL,'41101','Spain','(95) 555 82 82',NULL);

INSERT "Customers" VALUES('GOURL','Gourmet Lanchonetes','Andr� Fonseca','Sales Associate','Av. Brasil, 442','Campinas','SP','04876-786','Brazil','(11) 555-9482',NULL);
INSERT "Customers" VALUES('GREAL','Great Lakes Food Market','Howard Snyder','Marketing Manager','2732 Baker Blvd.','Eugene','OR','97403','USA','(503) 555-7555',NULL);
INSERT "Customers" VALUES('GROSR','GROSELLA-Restaurante','Manuel Pereira','Owner','5� Ave. Los Palos Grandes','Caracas','DF','1081','Venezuela','(2) 283-2951','(2) 283-3397');
INSERT "Customers" VALUES('HANAR','Hanari Carnes','Mario Pontes','Accounting Manager','Rua do Pa�o, 67','Rio de Janeiro','RJ','05454-876','Brazil','(21) 555-0091','(21) 555-8765');
INSERT "Customers" VALUES('HILAA','HILARION-Abastos','Carlos Hern�ndez','Sales Representative','Carrera 22 con Ave. Carlos Soublette #8-35','San Crist�bal','T�chira','5022','Venezuela','(5) 555-1340','(5) 555-1948');
INSERT "Customers" VALUES('HUNGC','Hungry Coyote Import Store','Yoshi Latimer','Sales Representative','City Center Plaza 516 Main St.','Elgin','OR','97827','USA','(503) 555-6874','(503) 555-2376');
INSERT "Customers" VALUES('HUNGO','Hungry Owl All-Night Grocers','Patricia McKenna','Sales Associate','8 Johnstown Road','Cork','Co. Cork',NULL,'Ireland','2967 542','2967 3333');
INSERT "Customers" VALUES('ISLAT','Island Trading','Helen Bennett','Marketing Manager','Garden House Crowther Way','Cowes','Isle of Wight','PO31 7PJ','UK','(198) 555-8888',NULL);
INSERT "Customers" VALUES('KOENE','K�niglich Essen','Philip Cramer','Sales Associate','Maubelstr. 90','Brandenburg',NULL,'14776','Germany','0555-09876',NULL);
INSERT "Customers" VALUES('LACOR','La corne d''abondance','Daniel Tonini','Sales Representative','67, avenue de l''Europe','Versailles',NULL,'78000','France','30.59.84.10','30.59.85.11');

INSERT "Customers" VALUES('LAMAI','La maison d''Asie','Annette Roulet','Sales Manager','1 rue Alsace-Lorraine','Toulouse',NULL,'31000','France','61.77.61.10','61.77.61.11');
INSERT "Customers" VALUES('LAUGB','Laughing Bacchus Wine Cellars','Yoshi Tannamuri','Marketing Assistant','1900 Oak St.','Vancouver','BC','V3F 2K1','Canada','(604) 555-3392','(604) 555-7293');
INSERT "Customers" VALUES('LAZYK','Lazy K Kountry Store','John Steel','Marketing Manager','12 Orchestra Terrace','Walla Walla','WA','99362','USA','(509) 555-7969','(509) 555-6221');
INSERT "Customers" VALUES('LEHMS','Lehmanns Marktstand','Renate Messner','Sales Representative','Magazinweg 7','Frankfurt a.M.',NULL,'60528','Germany','069-0245984','069-0245874');
INSERT "Customers" VALUES('LETSS','Let''s Stop N Shop','Jaime Yorres','Owner','87 Polk St. Suite 5','San Francisco','CA','94117','USA','(415) 555-5938',NULL);
INSERT "Customers" VALUES('LILAS','LILA-Supermercado','Carlos Gonz�lez','Accounting Manager','Carrera 52 con Ave. Bol�var #65-98 Llano Largo','Barquisimeto','Lara','3508','Venezuela','(9) 331-6954','(9) 331-7256');
INSERT "Customers" VALUES('LINOD','LINO-Delicateses','Felipe Izquierdo','Owner','Ave. 5 de Mayo Porlamar','I. de Margarita','Nueva Esparta','4980','Venezuela','(8) 34-56-12','(8) 34-93-93');
INSERT "Customers" VALUES('LONEP','Lonesome Pine Restaurant','Fran Wilson','Sales Manager','89 Chiaroscuro Rd.','Portland','OR','97219','USA','(503) 555-9573','(503) 555-9646');
INSERT "Customers" VALUES('MAGAA','Magazzini Alimentari Riuniti','Giovanni Rovelli','Marketing Manager','Via Ludovico il Moro 22','Bergamo',NULL,'24100','Italy','035-640230','035-640231');
INSERT "Customers" VALUES('MAISD','Maison Dewey','Catherine Dewey','Sales Agent','Rue Joseph-Bens 532','Bruxelles',NULL,'B-1180','Belgium','(02) 201 24 67','(02) 201 24 68');

INSERT "Customers" VALUES('MEREP','M�re Paillarde','Jean Fresni�re','Marketing Assistant','43 rue St. Laurent','Montr�al','Qu�bec','H1J 1C3','Canada','(514) 555-8054','(514) 555-8055');
INSERT "Customers" VALUES('MORGK','Morgenstern Gesundkost','Alexander Feuer','Marketing Assistant','Heerstr. 22','Leipzig',NULL,'04179','Germany','0342-023176',NULL);
INSERT "Customers" VALUES('NORTS','North/South','Simon Crowther','Sales Associate','South House 300 Queensbridge','London',NULL,'SW7 1RZ','UK','(171) 555-7733','(171) 555-2530');
INSERT "Customers" VALUES('OCEAN','Oc�ano Atl�ntico Ltda.','Yvonne Moncada','Sales Agent','Ing. Gustavo Moncada 8585 Piso 20-A','Buenos Aires',NULL,'1010','Argentina','(1) 135-5333','(1) 135-5535');
INSERT "Customers" VALUES('OLDWO','Old World Delicatessen','Rene Phillips','Sales Representative','2743 Bering St.','Anchorage','AK','99508','USA','(907) 555-7584','(907) 555-2880');
INSERT "Customers" VALUES('OTTIK','Ottilies K�seladen','Henriette Pfalzheim','Owner','Mehrheimerstr. 369','K�ln',NULL,'50739','Germany','0221-0644327','0221-0765721');
INSERT "Customers" VALUES('PARIS','Paris sp�cialit�s','Marie Bertrand','Owner','265, boulevard Charonne','Paris',NULL,'75012','France','(1) 42.34.22.66','(1) 42.34.22.77');
INSERT "Customers" VALUES('PERIC','Pericles Comidas cl�sicas','Guillermo Fern�ndez','Sales Representative','Calle Dr. Jorge Cash 321','M�xico D.F.',NULL,'05033','Mexico','(5) 552-3745','(5) 545-3745');
INSERT "Customers" VALUES('PICCO','Piccolo und mehr','Georg Pipps','Sales Manager','Geislweg 14','Salzburg',NULL,'5020','Austria','6562-9722','6562-9723');
INSERT "Customers" VALUES('PRINI','Princesa Isabel Vinhos','Isabel de Castro','Sales Representative','Estrada da sa�de n. 58','Lisboa',NULL,'1756','Portugal','(1) 356-5634',NULL);

INSERT "Customers" VALUES('QUEDE','Que Del�cia','Bernardo Batista','Accounting Manager','Rua da Panificadora, 12','Rio de Janeiro','RJ','02389-673','Brazil','(21) 555-4252','(21) 555-4545');
INSERT "Customers" VALUES('QUEEN','Queen Cozinha','L�cia Carvalho','Marketing Assistant','Alameda dos Can�rios, 891','Sao Paulo','SP','05487-020','Brazil','(11) 555-1189',NULL);
INSERT "Customers" VALUES('QUICK','QUICK-Stop','Horst Kloss','Accounting Manager','Taucherstra�e 10','Cunewalde',NULL,'01307','Germany','0372-035188',NULL);
INSERT "Customers" VALUES('RANCH','Rancho grande','Sergio Guti�rrez','Sales Representative','Av. del Libertador 900','Buenos Aires',NULL,'1010','Argentina','(1) 123-5555','(1) 123-5556');
INSERT "Customers" VALUES('RATTC','Rattlesnake Canyon Grocery','Paula Wilson','Assistant Sales Representative','2817 Milton Dr.','Albuquerque','NM','87110','USA','(505) 555-5939','(505) 555-3620');
INSERT "Customers" VALUES('REGGC','Reggiani Caseifici','Maurizio Moroni','Sales Associate','Strada Provinciale 124','Reggio Emilia',NULL,'42100','Italy','0522-556721','0522-556722');
INSERT "Customers" VALUES('RICAR','Ricardo Adocicados','Janete Limeira','Assistant Sales Agent','Av. Copacabana, 267','Rio de Janeiro','RJ','02389-890','Brazil','(21) 555-3412',NULL);
INSERT "Customers" VALUES('RICSU','Richter Supermarkt','Michael Holz','Sales Manager','Grenzacherweg 237','Gen�ve',NULL,'1203','Switzerland','0897-034214',NULL);
INSERT "Customers" VALUES('ROMEY','Romero y tomillo','Alejandra Camino','Accounting Manager','Gran V�a, 1','Madrid',NULL,'28001','Spain','(91) 745 6200','(91) 745 6210');
INSERT "Customers" VALUES('SANTG','Sant� Gourmet','Jonas Bergulfsen','Owner','Erling Skakkes gate 78','Stavern',NULL,'4110','Norway','07-98 92 35','07-98 92 47');

INSERT "Customers" VALUES('SAVEA','Save-a-lot Markets','Jose Pavarotti','Sales Representative','187 Suffolk Ln.','Boise','ID','83720','USA','(208) 555-8097',NULL);
INSERT "Customers" VALUES('SEVES','Seven Seas Imports','Hari Kumar','Sales Manager','90 Wadhurst Rd.','London',NULL,'OX15 4NB','UK','(171) 555-1717','(171) 555-5646');
INSERT "Customers" VALUES('SIMOB','Simons bistro','Jytte Petersen','Owner','Vinb�ltet 34','Kobenhavn',NULL,'1734','Denmark','31 12 34 56','31 13 35 57');
INSERT "Customers" VALUES('SPECD','Sp�cialit�s du monde','Dominique Perrier','Marketing Manager','25, rue Lauriston','Paris',NULL,'75016','France','(1) 47.55.60.10','(1) 47.55.60.20');
INSERT "Customers" VALUES('SPLIR','Split Rail Beer & Ale','Art Braunschweiger','Sales Manager','P.O. Box 555','Lander','WY','82520','USA','(307) 555-4680','(307) 555-6525');
INSERT "Customers" VALUES('SUPRD','Supr�mes d�lices','Pascale Cartrain','Accounting Manager','Boulevard Tirou, 255','Charleroi',NULL,'B-6000','Belgium','(071) 23 67 22 20','(071) 23 67 22 21');
INSERT "Customers" VALUES('THEBI','The Big Cheese','Liz Nixon','Marketing Manager','89 Jefferson Way Suite 2','Portland','OR','97201','USA','(503) 555-3612',NULL);
INSERT "Customers" VALUES('THECR','The Cracker Box','Liu Wong','Marketing Assistant','55 Grizzly Peak Rd.','Butte','MT','59801','USA','(406) 555-5834','(406) 555-8083');
INSERT "Customers" VALUES('TOMSP','Toms Spezialit�ten','Karin Josephs','Marketing Manager','Luisenstr. 48','M�nster',NULL,'44087','Germany','0251-031259','0251-035695');
INSERT "Customers" VALUES('TORTU','Tortuga Restaurante','Miguel Angel Paolino','Owner','Avda. Azteca 123','M�xico D.F.',NULL,'05033','Mexico','(5) 555-2933',NULL);

INSERT "Customers" VALUES('TRADH','Tradi��o Hipermercados','Anabela Domingues','Sales Representative','Av. In�s de Castro, 414','Sao Paulo','SP','05634-030','Brazil','(11) 555-2167','(11) 555-2168');
INSERT "Customers" VALUES('TRAIH','Trail''s Head Gourmet Provisioners','Helvetius Nagy','Sales Associate','722 DaVinci Blvd.','Kirkland','WA','98034','USA','(206) 555-8257','(206) 555-2174');
INSERT "Customers" VALUES('VAFFE','Vaffeljernet','Palle Ibsen','Sales Manager','Smagsloget 45','�rhus',NULL,'8200','Denmark','86 21 32 43','86 22 33 44');
INSERT "Customers" VALUES('VICTE','Victuailles en stock','Mary Saveley','Sales Agent','2, rue du Commerce','Lyon',NULL,'69004','France','78.32.54.86','78.32.54.87');
INSERT "Customers" VALUES('VINET','Vins et alcools Chevalier','Paul Henriot','Accounting Manager','59 rue de l''Abbaye','Reims',NULL,'51100','France','26.47.15.10','26.47.15.11');
INSERT "Customers" VALUES('WANDK','Die Wandernde Kuh','Rita M�ller','Sales Representative','Adenauerallee 900','Stuttgart',NULL,'70563','Germany','0711-020361','0711-035428');
INSERT "Customers" VALUES('WARTH','Wartian Herkku','Pirkko Koskitalo','Accounting Manager','Torikatu 38','Oulu',NULL,'90110','Finland','981-443655','981-443655');
INSERT "Customers" VALUES('WELLI','Wellington Importadora','Paula Parente','Sales Manager','Rua do Mercado, 12','Resende','SP','08737-363','Brazil','(14) 555-8122',NULL);
INSERT "Customers" VALUES('WHITC','White Clover Markets','Karl Jablonski','Owner','305 - 14th Ave. S. Suite 3B','Seattle','WA','98128','USA','(206) 555-4112','(206) 555-4115');
INSERT "Customers" VALUES('WILMK','Wilman Kala','Matti Karttunen','Owner/Marketing Assistant','Keskuskatu 45','Helsinki',NULL,'21240','Finland','90-224 8858','90-224 8858');

INSERT "Customers" VALUES('WOLZA','Wolski  Zajazd','Zbyszek Piestrzeniewicz','Owner','ul. Filtrowa 68','Warszawa',NULL,'01-012','Poland','(26) 642-7012','(26) 642-7012');


set identity_insert "Employees" on;
ALTER TABLE "Employees" NOCHECK CONSTRAINT ALL;

INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(1,'Davolio','Nancy','Sales Representative','Ms.','12/08/1948','05/01/1992','507 - 20th Ave. E.Apt. 2A','Seattle','WA','98122','USA','(206) 555-9857','5467',NULL,'Education includes a BA in psychology from Colorado State University in 1970.  She also completed "The Art of the Cold Call."  Nancy is a member of Toastmasters International.',2,'http://accweb/emmployees/davolio.bmp',2954.55);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(2,'Fuller','Andrew','Vice President, Sales','Dr.','02/19/1952','08/14/1992','908 W. Capital Way','Tacoma','WA','98401','USA','(206) 555-9482','3457',NULL,'Andrew received his BTS commercial in 1974 and a Ph.D. in international marketing from the University of Dallas in 1981.  He is fluent in French and Italian and reads German.  He joined the company as a sales representative, was promoted to sales manager in January 1992 and to vice president of sales in March 1993.  Andrew is a member of the Sales Management Roundtable, the Seattle Chamber of Commerce, and the Pacific Rim Importers Association.',NULL,'http://accweb/emmployees/fuller.bmp',2254.49);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(3,'Leverling','Janet','Sales Representative','Ms.','08/30/1963','04/01/1992','722 Moss Bay Blvd.','Kirkland','WA','98033','USA','(206) 555-3412','3355',NULL,'Janet has a BS degree in chemistry from Boston College (1984).  She has also completed a certificate program in food retailing management.  Janet was hired as a sales associate in 1991 and promoted to sales representative in February 1992.',2,'http://accweb/emmployees/leverling.bmp',3119.15);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(4,'Peacock','Margaret','Sales Representative','Mrs.','09/19/1937','05/03/1993','4110 Old Redmond Rd.','Redmond','WA','98052','USA','(206) 555-8122','5176',NULL,'Margaret holds a BA in English literature from Concordia College (1958) and an MA from the American Institute of Culinary Arts (1966).  She was assigned to the London office temporarily from July through November 1992.',2,'http://accweb/emmployees/peacock.bmp','1861.08');
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(5,'Buchanan','Steven','Sales Manager','Mr.','03/04/1955','10/17/1993','14 Garrett Hill','London',NULL,'SW1 8JR','UK','(71) 555-4848','3453',NULL,'Steven Buchanan graduated from St. Andrews University, Scotland, with a BSC degree in 1976.  Upon joining the company as a sales representative in 1992, he spent 6 months in an orientation program at the Seattle office and then returned to his permanent post in London.  He was promoted to sales manager in March 1993.  Mr. Buchanan has completed the courses "Successful Telemarketing" and "International Sales Management."  He is fluent in French.',2,'http://accweb/emmployees/buchanan.bmp',1744.21);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(6,'Suyama','Michael','Sales Representative','Mr.','07/02/1963','10/17/1993','Coventry House Miner Rd.','London',NULL,'EC2 7JR','UK','(71) 555-7773','428',NULL,'Michael is a graduate of Sussex University (MA, economics, 1983) and the University of California at Los Angeles (MBA, marketing, 1986).  He has also taken the courses "Multi-Cultural Selling" and "Time Management for the Sales Professional."  He is fluent in Japanese and can read and write French, Portuguese, and Spanish.',5,'http://accweb/emmployees/davolio.bmp',2004.07);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(7,'King','Robert','Sales Representative','Mr.','05/29/1960','01/02/1994','Edgeham Hollow Winchester Way','London',NULL,'RG1 9SP','UK','(71) 555-5598','465',NULL,'Robert King served in the Peace Corps and traveled extensively before completing his degree in English at the University of Michigan in 1992, the year he joined the company.  After completing a course entitled "Selling in Europe," he was transferred to the London office in March 1993.',5,'http://accweb/emmployees/davolio.bmp',1991.55);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(8,'Callahan','Laura','Inside Sales Coordinator','Ms.','01/09/1958','03/05/1994','4726 - 11th Ave. N.E.','Seattle','WA','98105','USA','(206) 555-1189','2344',NULL,'Laura received a BA in psychology from the University of Washington.  She has also completed a course in business French.  She reads and writes French.',2,'http://accweb/emmployees/davolio.bmp',2100.50);
INSERT "Employees"("EmployeeID","LastName","FirstName","Title","TitleOfCourtesy","BirthDate","HireDate","Address","City","Region","PostalCode","Country","HomePhone","Extension","Photo","Notes","ReportsTo","PhotoPath","Salary") VALUES(9,'Dodsworth','Anne','Sales Representative','Ms.','01/27/1966','11/15/1994','7 Houndstooth Rd.','London',NULL,'WG2 7LT','UK','(71) 555-4444','452',NULL,'Anne has a BA degree in English from St. Lawrence College.  She is fluent in French and German.',5,'http://accweb/emmployees/davolio.bmp',2333.33);
set identity_insert "Employees" off;
ALTER TABLE "Employees" CHECK CONSTRAINT ALL;




ALTER TABLE "Order Details" NOCHECK CONSTRAINT ALL;
INSERT "Order Details" VALUES(10248,11,14,12,0);
INSERT "Order Details" VALUES(10248,42,9.8,10,0);
INSERT "Order Details" VALUES(10248,72,34.8,5,0);
INSERT "Order Details" VALUES(10249,14,18.6,9,0);
INSERT "Order Details" VALUES(10249,51,42.4,40,0);
INSERT "Order Details" VALUES(10250,41,7.7,10,0);
INSERT "Order Details" VALUES(10250,51,42.4,35,0.15);
INSERT "Order Details" VALUES(10250,65,16.8,15,0.15);
INSERT "Order Details" VALUES(10251,22,16.8,6,0.05);
INSERT "Order Details" VALUES(10251,57,15.6,15,0.05);

INSERT "Order Details" VALUES(10251,65,16.8,20,0);
INSERT "Order Details" VALUES(10252,20,64.8,40,0.05);
INSERT "Order Details" VALUES(10252,33,2,25,0.05);
INSERT "Order Details" VALUES(10252,60,27.2,40,0);
INSERT "Order Details" VALUES(10253,31,10,20,0);
INSERT "Order Details" VALUES(10253,39,14.4,42,0);
INSERT "Order Details" VALUES(10253,49,16,40,0);
INSERT "Order Details" VALUES(10254,24,3.6,15,0.15);
INSERT "Order Details" VALUES(10254,55,19.2,21,0.15);
INSERT "Order Details" VALUES(10254,74,8,21,0);

INSERT "Order Details" VALUES(10255,2,15.2,20,0);
INSERT "Order Details" VALUES(10255,16,13.9,35,0);
INSERT "Order Details" VALUES(10255,36,15.2,25,0);
INSERT "Order Details" VALUES(10255,59,44,30,0);
INSERT "Order Details" VALUES(10256,53,26.2,15,0);
INSERT "Order Details" VALUES(10256,77,10.4,12,0);
INSERT "Order Details" VALUES(10257,27,35.1,25,0);
INSERT "Order Details" VALUES(10257,39,14.4,6,0);
INSERT "Order Details" VALUES(10257,77,10.4,15,0);
INSERT "Order Details" VALUES(10258,2,15.2,50,0.2);

INSERT "Order Details" VALUES(10258,5,17,65,0.2);
INSERT "Order Details" VALUES(10258,32,25.6,6,0.2);
INSERT "Order Details" VALUES(10259,21,8,10,0);
INSERT "Order Details" VALUES(10259,37,20.8,1,0);
INSERT "Order Details" VALUES(10260,41,7.7,16,0.25);
INSERT "Order Details" VALUES(10260,57,15.6,50,0);
INSERT "Order Details" VALUES(10260,62,39.4,15,0.25);
INSERT "Order Details" VALUES(10260,70,12,21,0.25);
INSERT "Order Details" VALUES(10261,21,8,20,0);
INSERT "Order Details" VALUES(10261,35,14.4,20,0);

INSERT "Order Details" VALUES(10262,5,17,12,0.2);
INSERT "Order Details" VALUES(10262,7,24,15,0);
INSERT "Order Details" VALUES(10262,56,30.4,2,0);
INSERT "Order Details" VALUES(10263,16,13.9,60,0.25);
INSERT "Order Details" VALUES(10263,24,3.6,28,0);
INSERT "Order Details" VALUES(10263,30,20.7,60,0.25);
INSERT "Order Details" VALUES(10263,74,8,36,0.25);
INSERT "Order Details" VALUES(10264,2,15.2,35,0);
INSERT "Order Details" VALUES(10264,41,7.7,25,0.15);
INSERT "Order Details" VALUES(10265,17,31.2,30,0);

INSERT "Order Details" VALUES(10265,70,12,20,0);
INSERT "Order Details" VALUES(10266,12,30.4,12,0.05);
INSERT "Order Details" VALUES(10267,40,14.7,50,0);
INSERT "Order Details" VALUES(10267,59,44,70,0.15);
INSERT "Order Details" VALUES(10267,76,14.4,15,0.15);
INSERT "Order Details" VALUES(10268,29,99,10,0);
INSERT "Order Details" VALUES(10268,72,27.8,4,0);
INSERT "Order Details" VALUES(10269,33,2,60,0.05);
INSERT "Order Details" VALUES(10269,72,27.8,20,0.05);
INSERT "Order Details" VALUES(10270,36,15.2,30,0);

INSERT "Order Details" VALUES(10270,43,36.8,25,0);
INSERT "Order Details" VALUES(10271,33,2,24,0);
INSERT "Order Details" VALUES(10272,20,64.8,6,0);
INSERT "Order Details" VALUES(10272,31,10,40,0);
INSERT "Order Details" VALUES(10272,72,27.8,24,0);

ALTER TABLE "Order Details" CHECK CONSTRAINT ALL;


set identity_insert "Orders" on;
ALTER TABLE "Orders" NOCHECK CONSTRAINT ALL;

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10248,N'VINET',5,'7/4/1996','8/1/1996','7/16/1996',3,32.38,
	N'Vins et alcools Chevalier',N'59 rue de l''Abbaye',N'Reims',
	NULL,N'51100',N'France');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10249,N'TOMSP',6,'7/5/1996','8/16/1996','7/10/1996',1,11.61,
	N'Toms Spezialit�ten',N'Luisenstr. 48',N'M�nster',
	NULL,N'44087',N'Germany');
	
INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10250,N'HANAR',4,'7/8/1996','8/5/1996','7/12/1996',2,65.83,
	N'Hanari Carnes',N'Rua do Pa�o, 67',N'Rio de Janeiro',
	N'RJ',N'05454-876',N'Brazil');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10251,N'VICTE',3,'7/8/1996','8/5/1996','7/15/1996',1,41.34,
	N'Victuailles en stock',N'2, rue du Commerce',N'Lyon',
	NULL,N'69004',N'France');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10252,N'SUPRD',4,'7/9/1996','8/6/1996','7/11/1996',2,51.30,
	N'Supr�mes d�lices',N'Boulevard Tirou, 255',N'Charleroi',
	NULL,N'B-6000',N'Belgium')

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10253,N'HANAR',3,'7/10/1996','7/24/1996','7/16/1996',2,58.17,
	N'Hanari Carnes',N'Rua do Pa�o, 67',N'Rio de Janeiro',
	N'RJ',N'05454-876',N'Brazil');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10254,N'CHOPS',5,'7/11/1996','8/8/1996','7/23/1996',2,22.98,
	N'Chop-suey Chinese',N'Hauptstr. 31',N'Bern',
	NULL,N'3012',N'Switzerland');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10255,N'RICSU',9,'7/12/1996','8/9/1996','7/15/1996',3,148.33,
	N'Richter Supermarkt',N'Starenweg 5',N'Gen�ve',
	NULL,N'1204',N'Switzerland');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10256,N'WELLI',3,'7/15/1996','8/12/1996','7/17/1996',2,13.97,
	N'Wellington Importadora',N'Rua do Mercado, 12',N'Resende',
	N'SP',N'08737-363',N'Brazil');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10257,N'HILAA',4,'7/16/1996','8/13/1996','7/22/1996',3,81.91,
	N'HILARION-Abastos',N'Carrera 22 con Ave. Carlos Soublette #8-35',N'San Crist�bal',
	N'T�chira',N'5022',N'Venezuela');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10258,N'ERNSH',1,'7/17/1996','8/14/1996','7/23/1996',1,140.51,
	N'Ernst Handel',N'Kirchgasse 6',N'Graz',
	NULL,N'8010',N'Austria');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10259,N'CENTC',4,'7/18/1996','8/15/1996','7/25/1996',3,3.25,
	N'Centro comercial Moctezuma',N'Sierras de Granada 9993',N'M�xico D.F.',
	NULL,N'05022',N'Mexico');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10260,N'OTTIK',4,'7/19/1996','8/16/1996','7/29/1996',1,55.09,
	N'Ottilies K�seladen',N'Mehrheimerstr. 369',N'K�ln',
	NULL,N'50739',N'Germany');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10261,N'QUEDE',4,'7/19/1996','8/16/1996','7/30/1996',2,3.05,
	N'Que Del�cia',N'Rua da Panificadora, 12',N'Rio de Janeiro',
	N'RJ',N'02389-673',N'Brazil');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10262,N'RATTC',8,'7/22/1996','8/19/1996','7/25/1996',3,48.29,
	N'Rattlesnake Canyon Grocery',N'2817 Milton Dr.',N'Albuquerque',
	N'NM',N'87110',N'USA');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10263,N'ERNSH',9,'7/23/1996','8/20/1996','7/31/1996',3,146.06,
	N'Ernst Handel',N'Kirchgasse 6',N'Graz',
	NULL,N'8010',N'Austria')

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10264,N'FOLKO',6,'7/24/1996','8/21/1996','8/23/1996',3,3.67,
	N'Folk och f� HB',N'�kergatan 24',N'Br�cke',
	NULL,N'S-844 67',N'Sweden');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10265,N'BLONP',2,'7/25/1996','8/22/1996','8/12/1996',1,55.28,
	N'Blondel p�re et fils',N'24, place Kl�ber',N'Strasbourg',
	NULL,N'67000',N'France');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10266,N'WARTH',3,'7/26/1996','9/6/1996','7/31/1996',3,25.73,
	N'Wartian Herkku',N'Torikatu 38',N'Oulu',
	NULL,N'90110',N'Finland');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10267,N'FRANK',4,'7/29/1996','8/26/1996','8/6/1996',1,208.58,
	N'Frankenversand',N'Berliner Platz 43',N'M�nchen',
	NULL,N'80805',N'Germany');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10268,N'GROSR',8,'7/30/1996','8/27/1996','8/2/1996',3,66.29,
	N'GROSELLA-Restaurante',N'5� Ave. Los Palos Grandes',N'Caracas',
	N'DF',N'1081',N'Venezuela');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10269,N'WHITC',5,'7/31/1996','8/14/1996','8/9/1996',1,4.56,
	N'White Clover Markets',N'1029 - 12th Ave. S.',N'Seattle',
	N'WA',N'98124',N'USA');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10270,N'WARTH',1,'8/1/1996','8/29/1996','8/2/1996',1,136.54,
	N'Wartian Herkku',N'Torikatu 38',N'Oulu',
	NULL,N'90110',N'Finland');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10271,N'SPLIR',6,'8/1/1996','8/29/1996','8/30/1996',2,4.54,
	N'Split Rail Beer & Ale',N'P.O. Box 555',N'Lander',
	N'WY',N'82520',N'USA');

INSERT INTO "Orders"
("OrderID","CustomerID","EmployeeID","OrderDate","RequiredDate",
	"ShippedDate","ShipVia","Freight","ShipName","ShipAddress",
	"ShipCity","ShipRegion","ShipPostalCode","ShipCountry")
VALUES (10272,N'RATTC',6,'8/2/1996','8/30/1996','8/6/1996',2,98.03,
	N'Rattlesnake Canyon Grocery',N'2817 Milton Dr.',N'Albuquerque',
	N'NM',N'87110',N'USA');
	
set identity_insert "Orders" off;
ALTER TABLE "Orders" CHECK CONSTRAINT ALL;	
	

set identity_insert "Products" on;
ALTER TABLE "Products" NOCHECK CONSTRAINT ALL;

INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(1,'Chai',1,1,'10 boxes x 20 bags',18,39,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(2,'Chang',1,1,'24 - 12 oz bottles',19,17,40,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(3,'Aniseed Syrup',1,2,'12 - 550 ml bottles',10,13,70,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(4,'Chef Anton''s Cajun Seasoning',2,2,'48 - 6 oz jars',22,53,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(5,'Chef Anton''s Gumbo Mix',2,2,'36 boxes',21.35,0,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(6,'Grandma''s Boysenberry Spread',3,2,'12 - 8 oz jars',25,120,0,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(7,'Uncle Bob''s Organic Dried Pears',3,7,'12 - 1 lb pkgs.',30,15,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(8,'Northwoods Cranberry Sauce',3,2,'12 - 12 oz jars',40,6,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(9,'Mishi Kobe Niku',4,6,'18 - 500 g pkgs.',97,29,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(10,'Ikura',4,8,'12 - 200 ml jars',31,31,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(11,'Queso Cabrales',5,4,'1 kg pkg.',21,22,30,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(12,'Queso Manchego La Pastora',5,4,'10 - 500 g pkgs.',38,86,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(13,'Konbu',6,8,'2 kg box',6,24,0,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(14,'Tofu',6,7,'40 - 100 g pkgs.',23.25,35,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(15,'Genen Shouyu',6,2,'24 - 250 ml bottles',15.5,39,0,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(16,'Pavlova',7,3,'32 - 500 g boxes',17.45,29,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(17,'Alice Mutton',7,6,'20 - 1 kg tins',39,0,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(18,'Carnarvon Tigers',7,8,'16 kg pkg.',62.5,42,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(19,'Teatime Chocolate Biscuits',8,3,'10 boxes x 12 pieces',9.2,25,0,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(20,'Sir Rodney''s Marmalade',8,3,'30 gift boxes',81,40,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(21,'Sir Rodney''s Scones',8,3,'24 pkgs. x 4 pieces',10,3,40,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(22,'Gustaf''s Kn�ckebr�d',9,5,'24 - 500 g pkgs.',21,104,0,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(23,'Tunnbr�d',9,5,'12 - 250 g pkgs.',9,61,0,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(24,'Guaran� Fant�stica',10,1,'12 - 355 ml cans',4.5,20,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(25,'NuNuCa Nu�-Nougat-Creme',11,3,'20 - 450 g glasses',14,76,0,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(26,'Gumb�r Gummib�rchen',11,3,'100 - 250 g bags',31.23,15,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(27,'Schoggi Schokolade',11,3,'100 - 100 g pieces',43.9,49,0,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(28,'R�ssle Sauerkraut',12,7,'25 - 825 g cans',45.6,26,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(29,'Th�ringer Rostbratwurst',12,6,'50 bags x 30 sausgs.',123.79,0,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(30,'Nord-Ost Matjeshering',13,8,'10 - 200 g glasses',25.89,10,0,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(31,'Gorgonzola Telino',14,4,'12 - 100 g pkgs',12.5,0,70,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(32,'Mascarpone Fabioli',14,4,'24 - 200 g pkgs.',32,9,40,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(33,'Geitost',15,4,'500 g',2.5,112,0,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(34,'Sasquatch Ale',16,1,'24 - 12 oz bottles',14,111,0,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(35,'Steeleye Stout',16,1,'24 - 12 oz bottles',18,20,0,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(36,'Inlagd Sill',17,8,'24 - 250 g  jars',19,112,0,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(37,'Gravad lax',17,8,'12 - 500 g pkgs.',26,11,50,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(38,'C�te de Blaye',18,1,'12 - 75 cl bottles',263.5,17,0,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(39,'Chartreuse verte',18,1,'750 cc per bottle',18,69,0,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(40,'Boston Crab Meat',19,8,'24 - 4 oz tins',18.4,123,0,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(41,'Jack''s New England Clam Chowder',19,8,'12 - 12 oz cans',9.65,85,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(42,'Singaporean Hokkien Fried Mee',20,5,'32 - 1 kg pkgs.',14,26,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(43,'Ipoh Coffee',20,1,'16 - 500 g tins',46,17,10,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(44,'Gula Malacca',20,2,'20 - 2 kg bags',19.45,27,0,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(45,'Rogede sild',21,8,'1k pkg.',9.5,5,70,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(46,'Spegesild',21,8,'4 - 450 g glasses',12,95,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(47,'Zaanse koeken',22,3,'10 - 4 oz boxes',9.5,36,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(48,'Chocolade',22,3,'10 pkgs.',12.75,15,70,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(49,'Maxilaku',23,3,'24 - 50 g pkgs.',20,10,60,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(50,'Valkoinen suklaa',23,3,'12 - 100 g bars',16.25,65,0,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(51,'Manjimup Dried Apples',24,7,'50 - 300 g pkgs.',53,20,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(52,'Filo Mix',24,5,'16 - 2 kg boxes',7,38,0,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(53,'Perth Pasties',24,6,'48 pieces',32.8,0,0,0,1);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(54,'Tourti�re',25,6,'16 pies',7.45,21,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(55,'P�t� chinois',25,6,'24 boxes x 2 pies',24,115,0,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(56,'Gnocchi di nonna Alice',26,5,'24 - 250 g pkgs.',38,21,10,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(57,'Ravioli Angelo',26,5,'24 - 250 g pkgs.',19.5,36,0,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(58,'Escargots de Bourgogne',27,8,'24 pieces',13.25,62,0,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(59,'Raclette Courdavault',28,4,'5 kg pkg.',55,79,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(60,'Camembert Pierrot',28,4,'15 - 300 g rounds',34,19,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(61,'Sirop d''�rable',29,2,'24 - 500 ml bottles',28.5,113,0,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(62,'Tarte au sucre',29,3,'48 pies',49.3,17,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(63,'Vegie-spread',7,2,'15 - 625 g jars',43.9,24,0,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(64,'Wimmers gute Semmelkn�del',12,5,'20 bags x 4 pieces',33.25,22,80,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(65,'Louisiana Fiery Hot Pepper Sauce',2,2,'32 - 8 oz bottles',21.05,76,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(66,'Louisiana Hot Spiced Okra',2,2,'24 - 8 oz jars',17,4,100,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(67,'Laughing Lumberjack Lager',16,1,'24 - 12 oz bottles',14,52,0,10,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(68,'Scottish Longbreads',8,3,'10 boxes x 8 pieces',12.5,6,10,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(69,'Gudbrandsdalsost',15,4,'10 kg pkg.',36,26,0,15,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(70,'Outback Lager',7,1,'24 - 355 ml bottles',15,15,10,30,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(71,'Flotemysost',15,4,'10 - 500 g pkgs.',21.5,26,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(72,'Mozzarella di Giovanni',14,4,'24 - 200 g pkgs.',34.8,14,0,0,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(73,'R�d Kaviar',17,8,'24 - 150 g jars',15,101,0,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(74,'Longlife Tofu',4,7,'5 kg pkg.',10,4,20,5,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(75,'Rh�nbr�u Klosterbier',12,1,'24 - 0.5 l bottles',7.75,125,0,25,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(76,'Lakkalik��ri',23,1,'500 ml',18,57,0,20,0);
INSERT "Products"("ProductID","ProductName","SupplierID","CategoryID","QuantityPerUnit","UnitPrice","UnitsInStock","UnitsOnOrder","ReorderLevel","Discontinued") VALUES(77,'Original Frankfurter gr�ne So�e',12,2,'12 boxes',13,32,0,15,0);
set identity_insert "Products" off;
ALTER TABLE "Products" CHECK CONSTRAINT ALL;	


set identity_insert "Shippers" on;
INSERT "Shippers"("ShipperID","CompanyName","Phone") VALUES(1,'Speedy Express','(503) 555-9831');
INSERT "Shippers"("ShipperID","CompanyName","Phone") VALUES(2,'United Package','(503) 555-3199');
INSERT "Shippers"("ShipperID","CompanyName","Phone") VALUES(3,'Federal Shipping','(503) 555-9931');
set identity_insert "Shippers" off;


set identity_insert "Suppliers" on;
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(1,'Exotic Liquids','Charlotte Cooper','Purchasing Manager','49 Gilbert St.','London',NULL,'EC1 4SD','UK','(171) 555-2222',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(2,'New Orleans Cajun Delights','Shelley Burke','Order Administrator','P.O. Box 78934','New Orleans','LA','70117','USA','(100) 555-4822',NULL,'#CAJUN.HTM#');
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(3,'Grandma Kelly''s Homestead','Regina Murphy','Sales Representative','707 Oxford Rd.','Ann Arbor','MI','48104','USA','(313) 555-5735','(313) 555-3349',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(4,'Tokyo Traders','Yoshi Nagase','Marketing Manager','9-8 Sekimai Musashino-shi','Tokyo',NULL,'100','Japan','(03) 3555-5011',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(5,'Cooperativa de Quesos ''Las Cabras''','Antonio del Valle Saavedra','Export Administrator','Calle del Rosal 4','Oviedo','Asturias','33007','Spain','(98) 598 76 54',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(6,'Mayumi''s','Mayumi Ohno','Marketing Representative','92 Setsuko Chuo-ku','Osaka',NULL,'545','Japan','(06) 431-7877',NULL,'Mayumi''s (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/mayumi.htm#');
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(7,'Pavlova, Ltd.','Ian Devling','Marketing Manager','74 Rose St. Moonie Ponds','Melbourne','Victoria','3058','Australia','(03) 444-2343','(03) 444-6588',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(8,'Specialty Biscuits, Ltd.','Peter Wilson','Sales Representative','29 King''s Way','Manchester',NULL,'M14 GSD','UK','(161) 555-4448',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(9,'PB Kn�ckebr�d AB','Lars Peterson','Sales Agent','Kaloadagatan 13','G�teborg',NULL,'S-345 67','Sweden','031-987 65 43','031-987 65 91',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(10,'Refrescos Americanas LTDA','Carlos Diaz','Marketing Manager','Av. das Americanas 12.890','Sao Paulo',NULL,'5442','Brazil','(11) 555 4640',NULL,NULL);

INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(11,'Heli S��waren GmbH & Co. KG','Petra Winkler','Sales Manager','Tiergartenstra�e 5','Berlin',NULL,'10785','Germany','(010) 9984510',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(12,'Plutzer Lebensmittelgro�m�rkte AG','Martin Bein','International Marketing Mgr.','Bogenallee 51','Frankfurt',NULL,'60439','Germany','(069) 992755',NULL,'Plutzer (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/plutzer.htm#');
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(13,'Nord-Ost-Fisch Handelsgesellschaft mbH','Sven Petersen','Coordinator Foreign Markets','Frahmredder 112a','Cuxhaven',NULL,'27478','Germany','(04721) 8713','(04721) 8714',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(14,'Formaggi Fortini s.r.l.','Elio Rossi','Sales Representative','Viale Dante, 75','Ravenna',NULL,'48100','Italy','(0544) 60323','(0544) 60603','#FORMAGGI.HTM#');
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(15,'Norske Meierier','Beate Vileid','Marketing Manager','Hatlevegen 5','Sandvika',NULL,'1320','Norway','(0)2-953010',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(16,'Bigfoot Breweries','Cheryl Saylor','Regional Account Rep.','3400 - 8th Avenue Suite 210','Bend','OR','97101','USA','(503) 555-9931',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(17,'Svensk Sj�f�da AB','Michael Bj�rn','Sales Representative','Brovallav�gen 231','Stockholm',NULL,'S-123 45','Sweden','08-123 45 67',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(18,'Aux joyeux eccl�siastiques','Guyl�ne Nodier','Sales Manager','203, Rue des Francs-Bourgeois','Paris',NULL,'75004','France','(1) 03.83.00.68','(1) 03.83.00.62',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(19,'New England Seafood Cannery','Robb Merchant','Wholesale Account Agent','Order Processing Dept. 2100 Paul Revere Blvd.','Boston','MA','02134','USA','(617) 555-3267','(617) 555-3389',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(20,'Leka Trading','Chandra Leka','Owner','471 Serangoon Loop, Suite #402','Singapore',NULL,'0512','Singapore','555-8787',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(21,'Lyngbysild','Niels Petersen','Sales Manager','Lyngbysild Fiskebakken 10','Lyngby',NULL,'2800','Denmark','43844108','43844115',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(22,'Zaanse Snoepfabriek','Dirk Luchte','Accounting Manager','Verkoop Rijnweg 22','Zaandam',NULL,'9999 ZZ','Netherlands','(12345) 1212','(12345) 1210',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(23,'Karkki Oy','Anne Heikkonen','Product Manager','Valtakatu 12','Lappeenranta',NULL,'53120','Finland','(953) 10956',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(24,'G''day, Mate','Wendy Mackenzie','Sales Representative','170 Prince Edward Parade Hunter''s Hill','Sydney','NSW','2042','Australia','(02) 555-5914','(02) 555-4873','G''day Mate (on the World Wide Web)#http://www.microsoft.com/accessdev/sampleapps/gdaymate.htm#');
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(25,'Ma Maison','Jean-Guy Lauzon','Marketing Manager','2960 Rue St. Laurent','Montr�al','Qu�bec','H1J 1C3','Canada','(514) 555-9022',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(26,'Pasta Buttini s.r.l.','Giovanni Giudici','Order Administrator','Via dei Gelsomini, 153','Salerno',NULL,'84100','Italy','(089) 6547665','(089) 6547667',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(27,'Escargots Nouveaux','Marie Delamare','Sales Manager','22, rue H. Voiron','Montceau',NULL,'71300','France','85.57.00.07',NULL,NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(28,'Gai p�turage','Eliane Noz','Sales Representative','Bat. B 3, rue des Alpes','Annecy',NULL,'74000','France','38.76.98.06','38.76.98.58',NULL);
INSERT "Suppliers"("SupplierID","CompanyName","ContactName","ContactTitle","Address","City","Region","PostalCode","Country","Phone","Fax","HomePage") VALUES(29,'For�ts d''�rables','Chantal Goulet','Accounting Manager','148 rue Chasseur','Ste-Hyacinthe','Qu�bec','J2S 7S8','Canada','(514) 555-2955','(514) 555-2921',NULL);
set identity_insert "Suppliers" off;

CREATE TABLE CustomerCustomerDemo 
(
	CustomerID nchar (5) NOT NULL,
	CustomerTypeID nchar (10) NOT NULL
);

CREATE TABLE CustomerDemographics 
(
	CustomerTypeID nchar (10) NOT NULL ,
	CustomerDesc nvarchar(max) NULL 
);
	
CREATE TABLE "Region" 
(
	RegionID "int" IDENTITY (1, 1) NOT NULL ,
	RegionDescription nchar (50) NOT NULL 
);

CREATE TABLE "Territories" 
(
	TerritoryID nvarchar (20) NOT NULL ,
	TerritoryDescription nchar (50) NOT NULL ,
    RegionID int NOT NULL
);

CREATE TABLE "EmployeeTerritories" 
(
	EmployeeID int NOT NULL,
	TerritoryID nvarchar (20) NOT NULL 
);

-- The following adds data to the tables just created.

-- set identity_insert "Region" on;
Insert Into "Region" ("RegionDescription") Values ('Eastern');
Insert Into "Region" ("RegionDescription") Values ('Western');
Insert Into "Region" ("RegionDescription") Values ('Northern');
Insert Into "Region" ("RegionDescription") Values ('Southern');
-- set identity_insert "Region" off;

Insert Into Territories Values ('01581','Westboro',1);
Insert Into Territories Values ('01730','Bedford',1);
Insert Into Territories Values ('01833','Georgetow',1);
Insert Into Territories Values ('02116','Boston',1);
Insert Into Territories Values ('02139','Cambridge',1);
Insert Into Territories Values ('02184','Braintree',1);
Insert Into Territories Values ('02903','Providence',1);
Insert Into Territories Values ('03049','Hollis',3);
Insert Into Territories Values ('03801','Portsmouth',3);
Insert Into Territories Values ('06897','Wilton',1);
Insert Into Territories Values ('07960','Morristown',1);
Insert Into Territories Values ('08837','Edison',1);
Insert Into Territories Values ('10019','New York',1);
Insert Into Territories Values ('10038','New York',1);
Insert Into Territories Values ('11747','Mellvile',1);
Insert Into Territories Values ('14450','Fairport',1);
Insert Into Territories Values ('19428','Philadelphia',3);
Insert Into Territories Values ('19713','Neward',1);
Insert Into Territories Values ('20852','Rockville',1);
Insert Into Territories Values ('27403','Greensboro',1);
Insert Into Territories Values ('27511','Cary',1);
Insert Into Territories Values ('29202','Columbia',4);
Insert Into Territories Values ('30346','Atlanta',4);
Insert Into Territories Values ('31406','Savannah',4);
Insert Into Territories Values ('32859','Orlando',4);
Insert Into Territories Values ('33607','Tampa',4);
Insert Into Territories Values ('40222','Louisville',1);
Insert Into Territories Values ('44122','Beachwood',3);
Insert Into Territories Values ('45839','Findlay',3);
Insert Into Territories Values ('48075','Southfield',3);
Insert Into Territories Values ('48084','Troy',3);
Insert Into Territories Values ('48304','Bloomfield Hills',3);
Insert Into Territories Values ('53404','Racine',3);
Insert Into Territories Values ('55113','Roseville',3);
Insert Into Territories Values ('55439','Minneapolis',3);
Insert Into Territories Values ('60179','Hoffman Estates',2);
Insert Into Territories Values ('60601','Chicago',2);
Insert Into Territories Values ('72716','Bentonville',4);
Insert Into Territories Values ('75234','Dallas',4);
Insert Into Territories Values ('78759','Austin',4);
Insert Into Territories Values ('80202','Denver',2);
Insert Into Territories Values ('80909','Colorado Springs',2);
Insert Into Territories Values ('85014','Phoenix',2);
Insert Into Territories Values ('85251','Scottsdale',2);
Insert Into Territories Values ('90405','Santa Monica',2);
Insert Into Territories Values ('94025','Menlo Park',2);
Insert Into Territories Values ('94105','San Francisco',2);
Insert Into Territories Values ('95008','Campbell',2);
Insert Into Territories Values ('95054','Santa Clara',2);
Insert Into Territories Values ('95060','Santa Cruz',2);
Insert Into Territories Values ('98004','Bellevue',2);
Insert Into Territories Values ('98052','Redmond',2);
Insert Into Territories Values ('98104','Seattle',2);


Insert Into EmployeeTerritories Values (1,'06897');
Insert Into EmployeeTerritories Values (1,'19713');
Insert Into EmployeeTerritories Values (2,'01581');
Insert Into EmployeeTerritories Values (2,'01730');
Insert Into EmployeeTerritories Values (2,'01833');
Insert Into EmployeeTerritories Values (2,'02116');
Insert Into EmployeeTerritories Values (2,'02139');
Insert Into EmployeeTerritories Values (2,'02184');
Insert Into EmployeeTerritories Values (2,'40222');
Insert Into EmployeeTerritories Values (3,'30346');
Insert Into EmployeeTerritories Values (3,'31406');
Insert Into EmployeeTerritories Values (3,'32859');
Insert Into EmployeeTerritories Values (3,'33607');
Insert Into EmployeeTerritories Values (4,'20852');
Insert Into EmployeeTerritories Values (4,'27403');
Insert Into EmployeeTerritories Values (4,'27511');
Insert Into EmployeeTerritories Values (5,'02903');
Insert Into EmployeeTerritories Values (5,'07960');
Insert Into EmployeeTerritories Values (5,'08837');
Insert Into EmployeeTerritories Values (5,'10019');
Insert Into EmployeeTerritories Values (5,'10038');
Insert Into EmployeeTerritories Values (5,'11747');
Insert Into EmployeeTerritories Values (5,'14450');
Insert Into EmployeeTerritories Values (6,'85014');
Insert Into EmployeeTerritories Values (6,'85251');
Insert Into EmployeeTerritories Values (6,'98004');
Insert Into EmployeeTerritories Values (6,'98052');
Insert Into EmployeeTerritories Values (6,'98104');
Insert Into EmployeeTerritories Values (7,'60179');
Insert Into EmployeeTerritories Values (7,'60601');
Insert Into EmployeeTerritories Values (7,'80202');
Insert Into EmployeeTerritories Values (7,'80909');
Insert Into EmployeeTerritories Values (7,'90405');
Insert Into EmployeeTerritories Values (7,'94025');
Insert Into EmployeeTerritories Values (7,'94105');
Insert Into EmployeeTerritories Values (7,'95008');
Insert Into EmployeeTerritories Values (7,'95054');
Insert Into EmployeeTerritories Values (7,'95060');
Insert Into EmployeeTerritories Values (8,'19428');
Insert Into EmployeeTerritories Values (8,'44122');
Insert Into EmployeeTerritories Values (8,'45839');
Insert Into EmployeeTerritories Values (8,'53404');
Insert Into EmployeeTerritories Values (9,'03049');
Insert Into EmployeeTerritories Values (9,'03801');
Insert Into EmployeeTerritories Values (9,'48075');
Insert Into EmployeeTerritories Values (9,'48084');
Insert Into EmployeeTerritories Values (9,'48304');
Insert Into EmployeeTerritories Values (9,'55113');
Insert Into EmployeeTerritories Values (9,'55439');




--  The following adds constraints to the Northwind database

ALTER TABLE CustomerCustomerDemo
	ADD CONSTRAINT [PK_CustomerCustomerDemo] PRIMARY KEY  NONCLUSTERED 
	(
		[CustomerID],
		[CustomerTypeID]
	) ON [PRIMARY]
;

ALTER TABLE CustomerDemographics
	ADD CONSTRAINT [PK_CustomerDemographics] PRIMARY KEY  NONCLUSTERED 
	(
		[CustomerTypeID]
	) ON [PRIMARY]
;

ALTER TABLE CustomerCustomerDemo
	ADD CONSTRAINT [FK_CustomerCustomerDemo] FOREIGN KEY 
	(
		[CustomerTypeID]
	) REFERENCES [dbo].[CustomerDemographics] (
		[CustomerTypeID]
	)
;

ALTER TABLE CustomerCustomerDemo
	ADD CONSTRAINT [FK_CustomerCustomerDemo_Customers] FOREIGN KEY
	(
		[CustomerID]
	) REFERENCES [dbo].[Customers] (
		[CustomerID]
	)
;

ALTER TABLE Region
	ADD CONSTRAINT [PK_Region] PRIMARY KEY  NONCLUSTERED 
	(
		[RegionID]
	)  ON [PRIMARY] 
;

ALTER TABLE Territories
	ADD CONSTRAINT [PK_Territories] PRIMARY KEY  NONCLUSTERED 
	(
		[TerritoryID]
	)  ON [PRIMARY] 
;

ALTER TABLE Territories
	ADD CONSTRAINT [FK_Territories_Region] FOREIGN KEY 
	(
		[RegionID]
	) REFERENCES [dbo].[Region] (
		[RegionID]
	)
;

ALTER TABLE EmployeeTerritories
	ADD CONSTRAINT [PK_EmployeeTerritories] PRIMARY KEY  NONCLUSTERED 
	(
		[EmployeeID],
		[TerritoryID]
	) ON [PRIMARY]
;

ALTER TABLE EmployeeTerritories
	ADD CONSTRAINT [FK_EmployeeTerritories_Employees] FOREIGN KEY 
	(
		[EmployeeID]
	) REFERENCES [dbo].[Employees] (
		[EmployeeID]
	)
;


ALTER TABLE EmployeeTerritories	
	ADD CONSTRAINT [FK_EmployeeTerritories_Territories] FOREIGN KEY 
	(
		[TerritoryID]
	) REFERENCES [dbo].[Territories] (
		[TerritoryID]
	)
;


CREATE TABLE employeeorderdetails (
	"EmployeeID" INTEGER NOT NULL,
	"OrderID" INTEGER NOT NULL,
	"ProductID" INTEGER NOT NULL,
	CONSTRAINT "PK_EmployeeOrderDetails" PRIMARY KEY ( "EmployeeID", "OrderID", "ProductID")
);

ALTER TABLE employeeorderdetails ADD CONSTRAINT "FK_EmpoyeeOrderDetails1"
	FOREIGN KEY ("EmployeeID") REFERENCES employees ("EmployeeID") ON DELETE CASCADE;

ALTER TABLE employeeorderdetails ADD CONSTRAINT "FK_EmpoyeeOrderDetails2"
	FOREIGN KEY ("OrderID", "ProductID") REFERENCES "Order Details" ("OrderID", "ProductID")  ON DELETE CASCADE;
    
INSERT INTO employeeorderdetails 
SELECT o."EmployeeID", od."OrderID", od."ProductID" 
FROM orders o JOIN "Order Details" od ON o."OrderID" = od."OrderID"; 
    


--- This table was added to support test cases for standalone table with 1 pk and no FK constaints */
CREATE TABLE "indexlog" (
    "id" "int" IDENTITY (1, 1) NOT NULL ,
    "tenantCode" VARCHAR(100) NOT NULL,
    "entityId" INTEGER,
    "entityType" VARCHAR(100),
    "error" VARCHAR(1024),
    "noIndex" BIT DEFAULT 0,
    "modifiedAt" datetime,
    CONSTRAINT "PK_indexlog" PRIMARY KEY ( "id")
);

set identity_insert "indexlog" on;
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (1, 'us', 100, 'locations', NULL, 0, '2019-02-12 15:44:18');
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (2, 'us', 200, 'locations', NULL, 1, '2019-04-02 15:51:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (3, 'us', 300, 'locations', 'error', 1, '2019-05-02 15:33:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (4, 'us', 567, 'ads', NULL, 0, '2019-01-22 15:51:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (5, 'ca', 837, 'ads', NULL, 0, '2019-05-01 12:03:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (6, 'us', 23, 'ads', NULL, 0, '2019-04-05 11:51:17'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (7, 'us', 24, 'ads', NULL, 0, '2019-04-22 15:05:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (8, 'us', 65, 'ads', NULL, 0, '2019-04-12 13:21:44'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (9, 'us', 765, 'ads', NULL, 0, '2019-04-08 03:00:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (10, 'ca', 239, 'ads', NULL, 0, '2019-04-09 23:32:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (11, 'ca', 8263, 'ads', NULL, 0, '2019-04-16 15:51:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (12, 'us', 103, 'items', NULL, 0, '2019-04-02 15:09:00'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (13, 'us', 105, 'items', NULL, 0, '2019-05-02 21:34:15'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (14, 'us', 23, 'ads', NULL, 0, '2019-05-02 15:51:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (15, 'ca', 8272, 'items', NULL, 0, '2019-05-02 15:51:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (16, 'us', 430, 'ads', 'error',1, '2019-03-01 12:21:22'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (17, 'us', 6252, 'ads', NULL, 0, '2019-05-02 03:51:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (18, 'us', 21, 'ads', NULL, 0, '2019-03-02 13:21:44'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (19, 'us', 23, 'ads', 'ERROR_MSG foo', 0, '2019-05-02 05:05:18'); 
INSERT INTO "indexlog" ("id", "tenantCode", "entityId", "entityType", "error", "noIndex", "modifiedAt") VALUES (20, 'us', 567, 'ads', 'some ERROR MSG', 0, '2019-03-02 12:21:22'); 
set identity_insert "indexlog" off;


--- This table was added to support test cases for keys that have non url save characters in them */
CREATE TABLE "urls" (
    "url" VARCHAR(512) NOT NULL,
    "short" VARCHAR(100) NOT NULL,
    "text" VARCHAR(500) NOT NULL,
     CONSTRAINT "PK_Urls" PRIMARY KEY ("url", "short")
);

INSERT INTO "urls" ("url", "short", "text") VALUES ('http://www.rocketpartners.io/inversion', '74593jd1', 'some description');



-- set identity_insert "Orders" on;

