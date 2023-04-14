# DynamodDb Test Cases #

### Docker Local Integration Testing
docker run --name dynamodb -p 8000:8000 amazon/dynamodb-local


## Schema Design


Dynamo Table - 'test-northwind'

| Col   | Type   | Indexes | Aliases
|-------|--------|-------------|--
| hk 	  | N	     | P:HK, GS3:HK	  | ORDER:orderid
| sk 	  | S	     | P:SK, GS3:SK	  | ORDER:type
| gs1hk	| N	     | GS1:HK	        | ORDER:employeeId
| gs1sk	| S	     | GS1:SK	        | ORDER:orderDate
| gs2hk	| S	     | GS2:HK	        | 
| gs2sk	| S	     | GS2:SK	        | 
| ls1	  | S	     | LS1:SK	        | ORDER:shipCity
| ls2	  | S	     | LS2:SK       	| ORDER:shipName
| ls3	  | S	     | LS3:SK	        | ORDER:requireDate
|	|	|	|
|	|	|	|
|	|	|	|


### Northwind SQL Tables Mapped To Dynamo Table

#### ORDER Table

| Prop            | Dynamo Col | Dynamo Idx 
|-----------------|------------|------------|
| orderid         | hk         | p:hk, gs1:
| type*           | sk
| customerid      | gs2hk
| employeeid      | gs1hk 
| orderdate       | gs1sk
| requireddate    | ls3        | ls3,gs2:sk
| shippeddate 
| shipvia 
| freight 
| shipsame        | ls2
| shipaddress 
| shipcity        | ls1
| shipregion
| shippostalcode
| shipcountry

\* Property 'type' is not part of the sql table.  It exists only to enable overloading different types into this single
Dynamo table

#### Use Cases

| Use Case 					                     | Table        |  Index      |  HashKey        |   SortKey  |  Ex
|----------------------------------------|--------------|-------------|-----------------|------------|----------|
Get an order by id                       | ORDER        |  Primary    | hk/orderid      | sk/type    | 12345,ORDER
Get orders sorted by orderid             | ORDER        |    GS3      | sk/type         | pk/orderid | ORDER,1234
Get a customer's orders by required date | ORDER        |    GS2      | gs2hk/customerid| ls3/requiredate

TBD: Find all orders for a given customer| Order  |    GS1      |  CustomerID |        OrderDate     | 99999, 2013-01-08

TBD: List orders by date -                HK: type              SK: OrderDate  ----  'ORDER' | '2013-01-08'

TBD: List orders by employee              HK: employeeId        SK: 




### Patterns we want to test
* system should select the right primary/global/local index based on supplied params
* have primary and global secondary that share a hash key with different sort keys
* have primary and global secondary with different hash keys and same sort key
* primary hash + local secondary sort vs global with same hash and different sort
* primary hash + local secondary sort vs global with same hash and sort (might be a table design mistake, but what should the system do?)
* can't GetItem on a GSI - https://stackoverflow.com/questions/43732835/getitem-from-secondary-index-with-dynamodb

## Filter Patterns That Needs testing
EQ | NE | LE | LT | GE | GT | NOT_NULL | NULL | CONTAINS | NOT_CONTAINS | BEGINS_WITH | IN | BETWEEN

X = ANY condition




| Case | P:HK   | P:SK   | GS1:PK | GS1:HK | GS2:HK | GS2:SK | GS3:PK | GS3:SK | LS1    | LS2    | LS3    | FIELD-N  | COOSE             | Notes         
|------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|--------|----------|-------------------|------------------------------|
  A    |        |        |        |        |        |        |        |        |        |        |        |    X     | Scan              | eq(ShipPostalCode, 30305)
  B    |        |        |        |        |        |        |        |        |  X     |        |        |          | Scan              |
  C    |  =     |        |        |        |        |        |        |        |        |        |        |          | Query - PRIMARY   | eq(OrderId, 12345)
  D    |  =     |  =     |        |        |        |        |        |        |        |        |        |          | GetItem - PRIMARY | eq(OrderId, 12345)&eq(type, 'ORDER')
  E    |  =     |  >     |        |        |        |        |        |        |        |        |        |          | Query - PRIMARY   | eq(OrderId, 12345)&gt(type, 'AAAAA')
  F    |  =     |  >     |        |        |        |        |        |        |  >     |        |        |          | Query - PRIMARY   | eq(OrderId, 12345)&gt(type, 'AAAAA')&eq(ShipCity,Atlanta)
  G    |  =     |  sw    |        |        |        |        |        |        |        |        |        |          | Query - PRIMARY   | eq(OrderId, 12345)&sw(type, 'ORD')
  H    |  =     |  sw    |        |        |        |        |        |        | =      |        |        |          | Query - LS1       | eq(OrderId, 12345)&sw(type, 'ORD')&eq(ShipCity,Atlanta)
  I    |  =     |  sw    | =      | =      |        |        |        |        |        |        |        |          | Query - GS1       | eq(OrderId, 12345)&sw(type, 'ORD')&eq(CustomerId,9999)&eq(OrderDate,'2013-01-08')
  J    |  =     |  sw    | =      | sw     |  =     |  =     |        |        |        |        |        |          | Query - GS2       |
  K    |  gt    |  =     |        |        |        |        |        |        |        |        |        |          | Scan - Primary    | gt(OrderId, 12345)&eq(type, 'ORDER")
  L    |  gt    |  sw    | =      |        |        |        |        |        |        |        |        |          | ????              |                               
  M    |        |        | =      |        |  =     |        |        |        |        |        |        |          | Query - GS2       | eq(customerid,val)                              

FIX: TEST K CHANGED TO USE GS3


