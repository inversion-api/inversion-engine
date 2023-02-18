

CREATE TABLE "Product"
(
    "productId"   VARCHAR(50),
    "upc"         VARCHAR(50),
    "modifier"    VARCHAR(50),
    "description" VARCHAR(255),
    CONSTRAINT "PK_Product" PRIMARY KEY ("productId")
);

CREATE TABLE "Channel"
(
    "channelId" INTEGER NOT NULL AUTO_INCREMENT,
    "name"      VARCHAR(50),
    CONSTRAINT "PK_Channel" PRIMARY KEY ("channelId")
);

CREATE TABLE "Store"
(
    "storeId" INTEGER NOT NULL AUTO_INCREMENT,
    "number" INTEGER,
    "address" VARCHAR(255),
    "pricebook" INTEGER,
    CONSTRAINT "PK_Store" PRIMARY KEY ("storeId")
);

CREATE TABLE "StoreProducts_1234_20210301" (
    "productId" VARCHAR(50),
    "price" DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT "PK_Items_1234_20210301" PRIMARY KEY ("productId")
);
CREATE TABLE "StoreProductChannels_1234_20210301" (
    "productId" VARCHAR(50),
    "channelId" INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT "Channel_1234" PRIMARY KEY ("productId", "channelId")
);


insert into "Product" values('111111111111-1', '1', 'Soda Pop 1', 'A refreshing drink.');
insert into "Product" values('222222222222-1', '1', 'Soda Pop 2', 'A different refreshing drink.');

insert into "Channel" ("name") values ('In-Store');
insert into "Store" ("number", "address", "pricebook") values (1234, '1234 Main Street', 20210301);
insert into "StoreProducts_1234_20210301" values ('111111111111-1', 0.99);
insert into "StoreProductChannels_1234_20210301" values ('111111111111-1', 1);