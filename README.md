# rckt_snooze

Snooze is an "API as a Service" platform and the fastest way to deliver a REST API.

With Snooze, you can connect your web application front end directly to your backend data source without any server side programming required.

Snooze is not a code generator it is a runtime service that reflectively creates a secure best practice JSON REST API for CRUD operations against 
multiple back end data sources including Relational Database Systems (RDBMS) such as MySQL, and PostgreSQL, NoSQL systems including Elasticsearch and Amazon's DynamoDB.  


## Contents
1. [Features & Benefits](#features--benefits)
1. [Quick Start](#quickstart)
1. [Configuring Your Api](#configuring-your-api)
   * [Configuration Files Loading](#configuration-file-loading)
   * [URL Structure](#url-structure)
   * [Account](#account)
   * [Api](#api)
   * [Collections, Entities and Attributes](#collections-entities-and-attributes)
   * [Endpoints, Actions and Handlers](#endpoints-actions-and-handlers)
   * [AclRule](#aclrule)
   * [Permission](#permission)
   * [Change](#change)
   * [Db](#db)
   * [Tables, Columns and Relationships](#tables-columns-and-relationships)
   * [Tenant](#tenant)
1. [Resource Query Language (RQL)](#resource-query-language-rql)
   * [Reserved Query String Parameters](#reserved-query-string-parameters)
   * [Restricted & Required Parameters](#restricted--required-query-parameters)
   * [Sorting / Ordering](#sorting--ordering)
   * [Pagination / Offset & Limit](#pagination--offset--limit)
   * [Query Filters](#query-filters)
   * [Aggregations](#aggregations)
   * [Nested Document Expansion](#nested-document-expansion)
   * [Property Inclusion / Exclusion](#property-inclusion--exclusion)
   * [Miscellaneous](#miscellaneous)
1. [Security Model](#security-model)
   * [Account Roles](#account-roles)
   * [Api Permissions](#api-permissions)
   * [Authentication](#authentication)
   * [Authorization](#authorization)
   * [Multi-Tenant APIs](#multi-tenant-apis)
   * [Row Level Security](#row-level-security)   
1. [Elasticsearch Specifics](#elasticsearch-specifics)
1. [DynamoDB Specifics](#dynamodb-specifics)  
1. [Developer Notes](#developer-notes)
   * [Logging](#logging)
   * [Gradle, Maven, etc.](#gradle-maven-etc)    
1. [Changes](#changes)   
  
 
  
  
## Features & Benefits
 
 * Get a full featured secure REST JSON API for full CRUD operations against your backend data source without any coding.
 * Tables exposed as REST collections
 * GET requests to query tables/collections where complex sql WHERE and aggregate conditions as URL query paramters (Resource Query Language, RQL).  
 * RQL
 * Nested document expansion for foreign keys
 * Nested document put/post
 * Pagination / ordering
 * Batch/bulk put/post/delete
 * Declarative security
 * Sql Injection proof
 * Consistent response envelope
 * Always CORS cross site capable
 * Multiple RDBMS back ends in a single API
 * "Smart PUT/POST" - no id/href field no problem. Nested/mixed put/post 
 * "Explain" mode shows you the exact statements to be run
 * Mutli tenant design  
 * Elegant pluralization of db tables w/ intelligant redirect



  
## Quickstart

When deploying Snooze as a stand alone Java Servlet, all you need to do is supply a WEB-INF/soooze.properties configuration file.
Simply swap out the JDBC params in the example below and you will have a read only (GET only) API that exposes
all of the db tables to:  https://localhost/api/demo/helloworld/${YOUR_TABLE_NAME_HERE}

Try out some [RQL](#resource-query-language-rql) queries like:
* http&#58;//localhost/api/demo/helloworld/${YOUR_TABLE_NAME_HERE}?page=2&limit=10&sort=columnX,-columnY
* http&#58;//localhost/api/demo/helloworld/${YOUR_TABLE_NAME_HERE}?columnX=somevalue
* http&#58;//localhost/api/demo/helloworld/${YOUR_TABLE_NAME_HERE}?or(eq(columnX,value),eq(columnY,'another value'))
* http&#58;//localhost/api/demo/helloworld/${YOUR_TABLE_NAME_HERE}/${ROW_PK}
* http&#58;//localhost/api/demo/helloworld/${YOUR_TABLE_NAME_HERE}/${ROW_PK}/${FKCOL}

Swap the line 'restEp.methods=GET' with 'restEp.methods=GET,PUT,POST,DELETE' and restart and your API will be fully CRUD ready!
 

```properties
    
snooze.debug=true
snooze.servletMapping=api

########################################################################
## APIs 
########################################################################
api.class=io.rcktapp.api.Api
api.accountCode=demo
api.apiCode=helloworld
api.dbs=db
api.actions=restA
api.endpoints=restEp


########################################################################
## DATABASES 
########################################################################
db.class=io.rcktapp.api.Db
db.name=db
db.driver=com.mysql.jdbc.Driver
db.url=YOUR_JDBC_URL_HERE
db.user=YOUR_JDBC_USER_HERE
db.pass=YOUR_JDBC_PASSWORD_HERE
db.poolMin=3
db.poolMax=5


########################################################################
## HANDLERS 
########################################################################
restH.class=io.rcktapp.api.service.RestHandler


########################################################################
## ACTIONS 
########################################################################
restA.class=io.rcktapp.api.Action
restA.handler=restH
restA.includePaths=*
restA.methods=GET,PUT,POST,DELETE
restA.order=10


########################################################################
## ENDPOINTS 
########################################################################
restEp.class=io.rcktapp.api.Endpoint
restEp.includePaths=*
restEp.excludePaths=somethingExcluded*
restEp.methods=GET
#restEp.methods=GET,PUT,POST,DELETE
restEp.handler=restH


```


## Configuring Your API


### Configuration File Loading

Snooze looks for files named snooze[1-100][-${Snooze.profile}].properties in the WEB-INF folder.  Files without a profile are always loaded first in numerically assending 
order and then files with a profile matching ${Snooze.profile} (if there are any) are loaded in ascending order. All files are loaded into a shared map so "the last loaded 
key wins" in terms of overwriting settings.  This design is intended to make it easy to support multiple runtime configurations such as 'dev' or 'prod' with short files 
that do not have to duplicate config between them.
  
The config file itself is a glorified bean property map in form of bean.name=value. Any bean in scope can be used as a value on the right side of the assignment and '.'
notation to any level of nesting on the left hand side is valid.  You can assign multiple values to a list on the left hand side by setting a comma separated list ex: 
bean.aList=bean1,bean2,bean3.  Nearly any JavaBean property in the object model (see Java Docs) can be wired up through the config file.

Configuration and API bootstrapping takes place in the following stages:

1. Initial loading - this stage loads all of the user supplied values according to the above algorithm
1. Db reflection - the system looks for all Db config objects and inspects the table/column structure (this goes for Dynamo and ElasticSearch too) and generates
   bean.property=value markup for everything it finds.
1. Api creation - Column,Table,Entity,Collection configuration is created to match the underlying Dbs.
1. The user supplied config is merged down (overwriting any shared keys) into the generated config map including the Db and Api information.
1. JavaBeans are auto-wired together and all Api objects in the resulting output are then loading into Service.addApi() and are ready to run.

This process allows the user supplied configuration to be kept to a minimum while also allowing any reflectively generated configuration to be overridden.  Instead
of configing up the entire db-to-api mapping, all you have to supply are the changes you want to make to the generated defaults.  This reflective config generation
happens in memory at runtime NOT development time.  Snooze writes the merged output to the console so you can inspect any keys you might want to customize.




### URL Structure

Snooze is designed to host multiple APIs potentially owned by different organizations.  All functional URLs for an API are prefixed 
with an AccountCode and ApiCode path components.  The AccountCode uniquely identifies the organization that owns the API and is unique to the 
host server. The ApiCode uniquely identifies the Api within the namespace created by the AccountCode. 

Valid based URL formats are:
 * http(s)://host.com/[${servletPath}]/${accountCode}/${apiCode}/
 * http(s)://host.com/[${servletPath}]/${accountCode}/${apiCode}/
 * http(s)://${accountCode}.host.com/[${servletPath}]/${apiCode}/
 * http(s)://host.com/[${servletPath}]/${accountCode} ONLY when apiCode and accountCode are the same thing (used to make a prettier URL for a 'default' Api per Account)

A default configuration would then offer Endpoint URLs such as below where ${COLLECTION} is the pluralized version of your table names.  Non 
plural versions will be redirected to the plural url.
 * ${API_URL}/${COLLECTION}/
 * ${API_URL}/${COLLECTION}/${ENTITY_ID}
 * ${API_URL}/${COLLECTION}/${ENTITY_ID}/${RELATIONSHIP}

Examples example:  
 * 'http&#58;//localhost/johns_books/orders' would return a paginated listing of all orders from the api with an accountCode and apiCode of 'johns_books'
 * 'http&#58;//localhost/johns_books/orders/1234' would return the details of order 1234
 * 'http&#58;//localhost/johns_books/orders/1234/books' would return all of the books related to the order without returning order 1234 itself
 * 'http&#58;//localhost/johns_books/orders/1234?expands=books' would return the 1234 details document with the related array of books already expanded (see document expansion below) 


### Account

Snooze itself is multi-tenant, designed to host multiple APIs that may be owned by different organizations.  An Account represents an organization that
ownes an Api...which may be different from the organization that is running the Snooze instance hosting the Api.  


### Api

An Api exposes a set of Endpoints.  Generally, Snooze will auto configure Endpoints that map to Db backed Collections for CRUD operations.   
   

### Collections, Entities, and Attributes

Collections logically map to Db Tables.  An Entity logically represents a row in the Table.  An Attribute logically represents a Table Column.  Clients send GET/PUT/POST/DELETE requets
to Collections to perform CRUD operations on the underlying Db.  Collection and Attribute names can be mapped (or aliased) when the Table name or Column name would not work well
in a URL or as a JSON property name.


### Endpoints, Actions and Handlers

Endpoints, Actions, and Handlers are how you map requests to the work that actually gets done. Programmers familiar with AOP might be comfortable with a loose analogy of a an Endpoint 
acting as a Join point, an Action being a Pointcut, and a Handler being an Aspect.  There is nothing application specific about this pattern.  The magic of Snooze is in the implementation 
of various Handlers.

An Endpoint represents a specific combination or a URL path (that may contain a trailing wildcard *) and one or more HTTP methods that will be called by clients.  One or more Actions are selected
to run when an Endpoint is called by a client.  

Actions link Endpoints to Handlers.  Behind each Endpoint can be an orderd list of Actions. Actions can contain configuration used by Handlers.  One Handler instance may behave differently
based on the configuration information on the Action.  Actions are mapped to URL paths / http methods and as such may be selected to run as part of one or more Endpoints.     

Work is done in the Handlers.  If an application must have custom business logic or otherwise can't manage to achieve a desired result via configuration, 99% of the time, the answer
is a custom Handler. Handlers do not have to be singletons but the design pattern is that anything that would be "use case specific" should be abstracted into Action config which
ties things back to the url/http method being invoked.


Example Handlers:
 * io.rcktapp.api.handler.sql.GetHandler
 * io.rcktapp.api.handler.sql.PostHandler
 * io.rcktapp.api.handler.sql.DeleteHandler
 * io.rcktapp.api.handler.auth.AuthHandler
 * io.rcktapp.api.handler.auth.AclHandler
 * io.rcktapp.api.handler.util.LogHandler
 * io.rcktapp.api.handler.util.S3UploadHandler
 * io.rcktapp.api.handler.util.RateHandler




### AclRule

AclRules allow you to declare that a User must have specified Permissions to access resources at a given url path and http method.  Generally AuthHandler and AclHandler will 
be setup (in that order) to protect resources according to configed AclRules.
    
### Permissions 

The AuthHandler will set Permission objects on the per request User object that may be checked against AclRule declarations by the AclHandler.

### Change

Supplied default Handler implementations accumulate Changes that occur as part of any request.  LogHandler can be configured to save the list of Changes to a Db to implement 
effective per request user level change logging.  Custom Handler implementors should call Response.addChange if a call modifies persistent data.



### Db - change to data source to accomodate dynamo and elastic
### Table
### Column
### Relationship

### Tenant - relationship between app and multiple users of the app - goes with use of the api

    

## Resource Query Language (RQL)

RQL is the set of HTTP query string parameters that allows developers to "slice and dice" the data returned from the API to meet their specific needs.

you use to query a REST collection for entities that match specific criteria or otherise alter the operation to be performed and the response values. 


### General
 
* Many functions can be written in one of several equivelant forms:
 * name=value - the traditional query string format
 * function(column, value OR expression) - eq(col,value), lt(column,value), and(eq(col,value), lt(column,value)), in(col, val1, val2, val3, val4)
 * name=eq=value, column=lt=value, col=in=val1,val2,val3
* Quotes & Escaping Quotes - ' or " can be used to quote values.  Use a \ to escape any inner occurances of the outter quote.  If you quote with single quotes you don't have to escape inner double quotes and vice versa  
* Wildcards - the '*' character is treated as a univeral wildcard for all supported backents.  
  For example, for RDBMS '*' would be substituded with "%" and instead of using '=' operator the system would substitude 'LIKE'.  
  You use the normal '=' or 'eq' operator but the system uses LIKE and % under the covers.


[See io.rcktapp.api.service.TestRql for many examples of complex RQL queries](https://github.com/RocketPartners/rckt_snooze/blob/master/src/test/java/io/rcktapp/rql/RqlToSqlTest.java)

### Reserved Query String Parameters

 * q - 
 * explain - if you include an 'explain' param (any value other than 'explain=false' is exactly the same as not providing a value) the response will include additional
   debug information including the SQL run.  The response body will not be valid JSON.  For security reasons, Api.debug must be true or the request must be to "localhost" for this
   to work. 
 * includes - 
 * excludes - 
 * expands - 

### Restricted & Required Query Parameters

If a table has a column named "userId" or "accountId" these are special case known columns who's
values may not be supplied by an api user request.  The value of these fields always comes from
the User object which is configured during authentication (see above).  This is true for 
RQL query params as well as for JSON properties.


### Query Filters

 RQL Function                     | Database            | Elastic             | Dynamo             | Description  
 ---                              | :---:               | :---:               | :---:              | ---
 column=value                     | :heavy_check_mark:  | :grey_question:     | :grey_question:    | translates as expected into a sql column equality check "column = value" 
 column='singleTicks'             | :heavy_check_mark:  | :grey_question:     | :grey_question:    | 'values can have spaces with encapsulated in quotes'
 column="doubleQuotes"            | :heavy_check_mark:  | :grey_question:     | :grey_question:    | "double or single quotes work"
 column=" ' "                     | :heavy_check_mark:  | :grey_question:     | :grey_question:    | "the first quote type wins so a single quote like ' inside of double quotes is considered a literal"
 column=" \" "                    | :heavy_check_mark:  | :grey_question:     | :grey_question:    | "you can also \" escape quotes with a backslash"
 column=wild*card                 | :heavy_check_mark:  | :grey_question:     | :grey_question:    | something*blah - translates into "column LIKE 'something%blah'"
 eq(column,value)                 | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | alternate form of column=value
 gt(column,value)                 | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | greater than query filter eg: "column < value"
 ge(column,value)                 | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | greater than or equal to
 lt(column,value)                 | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | less than filter
 le(column,value)                 | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | less than or equal to
 ne(column,value)                 | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | not equal
 in(column,val1,[val2...valN])    | :heavy_check_mark:  | :heavy_check_mark:  |                    | translates into "where column in (val1,....valN)"
 out(column,val1,[val2...valN])   | :heavy_check_mark:  | :heavy_check_mark:  |                    | translates into "where column NOT in (val1,....valN)"
 and(clause1,clause2,[...clauseN) | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | ANDs multiple clauses.  Example: and(eq(city,Atlanta),gt(zipCode,30030))
 or(clause1,clause2,[...clauseN)  | :heavy_check_mark:  | :heavy_check_mark:  | :heavy_check_mark: | ORs multiple clauses.  Example: or(eq(city,Atlanta),gt(zipCode,30030))
 emp(column)                      | :grey_question:     | :heavy_check_mark:  |                    | retrieves empty rows for a column value. null or empty string values will be retrieved
 nemp(column)                     | :grey_question:     | :heavy_check_mark:  |                    | retrieves all rows that do not contain an empty string or null value for a specified column
 n(column)                        | :grey_question:     | :heavy_check_mark:  | :heavy_check_mark: | retrieves all rows that contain a null value for a specified column
 nn(column)                       | :grey_question:     | :heavy_check_mark:  | :heavy_check_mark: | retrieves all rows that do not contain a null value for a specified column
 w(column,[value])                |                     | :heavy_check_mark:  | :heavy_check_mark: | retrieves all rows 'with' that wildcarded value in the specified column
 ew(column,[value])               |                     | :heavy_check_mark:  |                    | retrieves all rows that 'end with' that wildcarded value in the specified column
 sw(column,[value])               |                     | :heavy_check_mark:  | :heavy_check_mark: | retrieves all rows that 'start with' that wildcarded value in the specified column

 
 ### Sorting / Ordering

 RQL Function                     | Database            | Elastic             | Dynamo             | Description  
 ---                              | :---:               | :---:               | :---:              | ---
 sort=col1,+col2,-col3,colN       | :heavy_check_mark:  |                     |                    | use of the + operator is the implied default.  Prefixing with "-" sorts descending.
 sort(col1,[...colN])             | :heavy_check_mark:  |                     |                    | same as sort= but with "function format"
 order	                          | :heavy_check_mark:  |                     |                    | an overloaded synonym for "sort", the two are equivelant.


### Pagination / Offset & Limit

 RQL Function                     | Database            | Elastic             | Dynamo             | Description  
 ---                              | :---:               | :---:               | :---:              | ---
 page=N                           | :heavy_check_mark:  |                     |                    | translates into an offset clause using pagesize (or the default page size) as the multiplier 
 pagenum=N                        | :heavy_check_mark:  |                     |                    | an overloaded synonym for "page", the two are equivelant.
 pagesize=N                       | :heavy_check_mark:  |                     |                    | the number of results to return
 offset=N                         | :heavy_check_mark:  |                     |                    | directly translates into a sql offset clause, overrides any page/pagenum params supplied
 limit=N                          | :heavy_check_mark:  |                     |                    | directly translates into a SQL limit clause, overrides any pagesize params supplied
  

### Property Inclusion / Exclusion

 RQL Function                     | Database            | Elastic             | Dynamo             | Description  
 ---                              | :---:               | :---:               | :---:              | ---
 includes=col1,col2,colN          | :heavy_check_mark:  |                     |                    | restricts the properties returned in the document to the ones specified.  All others will be excluded. 
 includes(col1...colN)            | :heavy_check_mark:  |                     |                    | same as above
 excludes=col1,col2,colN          | :heavy_check_mark:  |                     |                    | specifically excludes the supplied props.  All others will be included.   
 excludes(col1...colN)            | :heavy_check_mark:  |                     |                    | same as above




### Aggregations  

 RQL Function                       | Database            | Elastic             | Dynamo             | Description  
 ---                                | :---:               | :---:               | :---:              | ---
 group(col1, [...colN])             | :heavy_check_mark:  |                     |                    | adds cols to a GROUP BY clause
 sum(col, [renamedAs])              | :heavy_check_mark:  |                     |                    | sums the given column and optionally names the resulting JSON property
 count(col, [renamedAs])            | :heavy_check_mark:  |                     |                    | counts the given column and optionally names the resulting JSON property
 min(col, [renamedAs])              | :heavy_check_mark:  |                     |                    | sums the given column and optionally names the resulting JSON property
 max(col, [renamedAs])              | :heavy_check_mark:  |                     |                    | sums the given column and optionally names the resulting JSON property
 sum(col, [renamedAs])              | :heavy_check_mark:  |                     |                    | sums the given column and optionally names the resulting JSON property
countascol(col, value, [...valueN]) | :heavy_check_mark:  |                     |                    | Roughly translates to "select sum(if(eq(col, value), 1, 0)) as value
distinct                            | :heavy_check_mark:  |                     |                    | filters out duplicate rows
distinct(column)                    | :heavy_check_mark:  |                     |                    | filters out duplicates based on the given column
if(column OR expression, valwhentrue, valwhenfalse)| :heavy_check_mark:  |                     |                    |


  
* To Document
  * function(sqlfunction, col, value, [...valueN]) - tries to apply the requested aggregate function
  * rowcount



### Miscellaneous

* as(col, renamed) - you can rename a property in the returned JSON using the 'as' operator.  Works just like the SQL as operator.
 RQL Function                     | Database            | Elastic             | Dynamo             | Description  
 ---                              | :---:               | :---:               | :---:              | ---
 as(col, renamed)                 | :heavy_check_mark:  |                     |                    | change the name of the property in the return JSON, works just like SQL 'as' operator.


  
### Nested Document Expansion

 RQL Function                     | Database            | Elastic             | Dynamo             | Description  
 ---                              | :---:               | :---:               | :---:              | ---
 expands=collection.property[...property][,table2.property2...]         | :heavy_check_mark:  |                     |                    | if "property" is a foreign key, referenced entity will be included as a nested document in the returned JSON instead of an HREF reference value
  
    
  
  
## Elasticsearch Specifics
Currently, the following functions are available for use:

* `source=value1,value2,valueN` - MUST be a separate parameter. Limits returned data to only these property values.
* auto-suggest paths should be in the following format: `.../elastic/indexType/suggest?suggestField=value&type=prefix`
> a `type` auto-suggest parameter can be set to define the type of search.  By default, auto-suggest will do a 'prefix' search.  If no results are found, auto-suggest will then try a wildcard search and return those results.  If you want to limit auto-suggest to one type of search, set `type=prefix` or `type=wildcard`.  While both types are fast, prefix searches are the fastest (~2ms vs ~20ms) 
* nested searching is allowed simply by specifying the name of the nested field, such as: `player.location.city` would retrieve the nested `location.city` value from a player. 

The index/type will be automatically generated so that only one value needs to be sent.  
`/elastic/location/location?and(and(eq(locationCode,270*),eq(city,Chandler)),and(eq(address1,*McQueen*)))`

The index/type used above `.../elastic/location/location?and(...` is the same as `/elastic/location?and(...`.
In the second example, it is assumed that the index/type `location` are the same value.

#### Elasticsearch RQL Examples
Retrieve all location data for the locations in the city of Chandler
`http://localhost:8080/api/apiCode/elastic/location?eq(city,Chandler)`

Retrieve only the address properties for the locations in the city of Chandler
`http://localhost:8080/api/apiCode/elastic/location?eq(city,Chandler)?source=address1,address2,address3`

Retrieve the locations in the city of Chandler AND have a locationCode of 270*** **AND** have an address including *McQueen*
`http://localhost:8080/api/apiCode/elastic/location?and(and(eq(locationCode,270*),eq(city,Chandler)),and(eq(address1,*McQueen*)))`

Retrieve all locations with players.registerNum > 5 
`http://localhost:8080/api/apiCode/elastic/location?gt(players.registerNum,5)`


Retrieve the locations with an address1 that includes 'VALLEY' AND PHOENIX locations that have deleted players 
`http://localhost:8080/api/apiCode/elastic/location?and(and(eq(players.deleted,true),eq(city,PHOENIX)),and(eq(address1,*VALLEY*)))`

Retrieve auto-suggested cities that start with 'chan' 
`http://localhost:8080/api/apiCode/elastic/location/suggest?suggestCity=chan`

Retrieve locations with an empty state value
`http://localhost:8080/api/apiCode/elastic/location?emp(state)`





## DynamoDB Specifics

Configuration is done on the Endpoint's config property.

* tableMap
	* Maps a collection name to a dynamo table
	* FORMAT: collection name | dynamodb name  (comma separated)
	* EXAMPLE: promo|promo-dev
* conditionalWriteConf
	* Allows a conditional write expression to be configured for a dynamo table
	* FORMAT: collection name | withConditionExpression | payload fields  (comma separated)
	* EXAMPLE: promo|attribute_not_exists(primarykey) OR enddate <= :enddate|enddate
* blueprintRow
	* Config which row should be used for building the collection typeMap (otherwise first row of scan will be used) - *(Note: you will probably not need to use this unless new columns are introduced to a table in the future.)*
	* FORMAT: collection name | primaryKey | sortKey (optional)
	* EXAMPLE: loyalty-punchcard | 111 | abc
* appendTenantIdToPk
	* Enables appending the tenant id to the primary key. 
	* *(Note: must be multi-tenant api also for this to have any effect)*
	* FORMAT: collection name (comma separated)  
	* EXAMPLE: promo,loyalty-punchcard
	* On POST, this will append the tenant id to the primary key, for example, if the pk is a mobile number and was sent up as 4045551212 and the tenantId was 1, this record will be stored with a primary key of 1::4045551212.  On GET and DELETE, this will automatically append the tenantId prior to looking up the record and will strip it out of the results. So, this is completely invisible to the api user, they will only ever see the mobilenumber as 4045551212.  If you login to the AWS console and view the table, there you will see the actually mobilenumber as 1::4045551212.

Example Config
```
dynamoH.class=io.rcktapp.api.service.ext.DynamoDbHandler

dynamoEp.class=io.rcktapp.api.Endpoint
dynamoEp.includePaths=dynamo*
dynamoEp.methods=GET,POST,DELETE
dynamoEp.handler=dynamoH
dynamoEp.config=tableMap=promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev&conditionalWriteConf=promo|attribute_not_exists(primarykey) OR enddate <= :enddate|enddate&appendTenantIdToPk=loyalty-punchcard
```	

## REST to CRUD - GET,POST,PUT,DELETE       

QueryStrings params (and required status) can be used with POST/PUT/DELETE re not just GET method
requests.   




## Security Model

 
### Account Roles

An Api is associated with a single Account

Users have a system level Role relationship with one or more Accounts.  

Roles are not designed to have a functional relationship to an Api being served.  Genarally, Roles should not
be used by Api designers to provide application level entitlements, they are designed to provide Snooze system level entitlements.
 

Roles:
 * Owner - The person who can delete a Snooze account.  
 * Administrator - Someone who can configure an account including changing changing security and managing Users.
   (Owner and Administrator are not designed to be functionally useful for Api clients but there is nothing stopping you from
   requiring Owner or Administrator Roles to access various Api Endpoints, see below)
 * Member - Generally, someone who will be calling the Api.  An admin user of an end application, my only have the 
   Snooze Member role.
 * Guest - Represents an unauthenticated caller.

Roles are hierarchical by privilege.  Ex. having the Owner role gives you Administrator, Member and Guest authority.     

### Api Permissions

For each Api a Users can be assigned Api defined named Permissions.  Permissions are simple string tokens.  They do
not confer hierarchical privilege like roles.  An Api designer might define Permission string such as "ADMINISTRATOR"
or "SOMECOLLECTION_SOMEHTTPMETHOD".  This is a total designers choice free for all.  

Groups of Users can be created at the Account level and each Group can be given Roles and Permissions 
that the Users inherit.  In this way, you could create a functional "Admin" Group (different from Administrtor Role) 
and give that Group all of the Permissions desired and then assign Users to that Admin Group. 


### Authentication

As far as the framework is concerned, authentication involves populating a User object with their entitled
Roles and Permissions and placing the User object on the Request.  Session management, barer tokens etc. are left up 
to the handler implementation.  Authentication does not consider Rolls and Permissions it just validates the username/password. 

AuthHandler is the default authentication provider.  It currently supports HTTP basic auth along with, username/password 
query string params and session tokens.  Additionally if an application chooses to provide a JWT signed with a User's
secretKey, the roles and permissions defined in the JWT will become the roles and permissions assigned to the 
User for that request and any roles and permissions defined in the DB will not be used.  
 
If you want to secure your Api, you have to configure an instance of the AuthHandler (or create your own custom authentication handler)
and map it to the desired Endpoints through an Action. 
 
Failing Authentication should return a 401 Unauthorized HTTP response code. (There is a longstanding gripe with HTTP status
codes that 401 should be called something other than 'Unauthorized' because generally elswehere in software development
authorization (see below) is the process of determining if a users can access requrested resources, NOT validating a users credentials.)
 

### Authorization

Authorization is managed by the AclHandler.  If you want to use Role and Permission based authorization 
you must configure an instance of the AclHandler (or create your own implementation) and associate it to
the desired Endpoints through an Action.

The AclHandler matches configured AclRules objects against the Url path and HTTP method of the request.

AclHandler processes AclRules in sorted order and the first rule to allow access "wins".  If no rule allows access, 
then a 403 Forbidden HTTP status code is returned.



### Multi-Tenant APIs

Api's can be flagged as 'multiTenant'.  If so, the collection key in the url prefix must be immediately 
preceded by a tenantCode.

Ex: http://localhost/accountCode/apiCode/tenantCode/collectionKey/[entityKey] 

If the AuthHandler is being used, it will enforce that the Url tenantCode matches the logged in users
tenantCode (if there is a logged in user).

IMPORTANT: this Url match restriction alone will not prevent access to cross tenant data. To fully
restrict and require the tenantId and tenantCode query string parameters and JSON body properties
with the following configuration.

```properties

tenantAcl.class=io.rcktapp.api.AclRule
tenantAcl.info=true
tenantAcl.includePaths=*
#tenantAcl.excludePaths=
tenantAcl.restricts=*.tenantId,*.tenantCode
tenantAcl.requires=*.tenantId
tenantAcl.methods=GET,PUT,POST,DELETE

```

Including this configuration will ensure that tenantId is always considered in queries (if it exists
on the target Table) and can not be supplied by the caller, it will be pulled from the logged in 
user.
  


### Row Level Security 

The simplest way to restrict a users interaction with a row is to provide a "userId" column on the 
table in question.  Then use an AclRule to "require/restrict" userId.  This way a user can only 
read and write the rows they 'own'.  You could user a different combination of AclRules to achieve
permission based read and owner based write/delete.

```properties

userAcl.class=io.rcktapp.api.AclRule
userAcl.info=true
userAcl.includePaths=*
#userAcl.excludePaths=
userAcl.restricts=*.userId
userAcl.requires=*.userId
userAcl.methods=GET,PUT,POST,DELETE

```
 
If you need to implement a row level security feature that can not be mapped to a userId column, 
you can configure JOIN filters that will limit a users access to the desired rows.

TODO: add more specific doco here.
 



## Developer Notes

### Logging
 * Snooze uses logback, but it is not configured out of the box - the service implementing Snooze will be responsible for providing their own logback.xml config file!


### Gradle, Maven, etc.

If you want to extend Snooze as part of a custom application, you can use jitpack to pull your preferred branch directly from GitHub into your project.   

```gradle
repositories { 
   maven { url 'https://jitpack.io' }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}

dependencies {
    compile 'com.github.RocketPartners:rckt_snooze:release-0.2.x-SNAPSHOT'
} 
```   


  
## Changes

2018-05-09 --------------------------------

 * Changed the way that GetHandler expands documents so that netsted collection
   requrests are set back through the "front door" and will obey all endpoint
   security requirments for that subcollection

 * Added "explain" query string param for debugging sql queries

 * Added "required" and "restricted" action/endpoint configuration params that enable 
   api designers to enhance security


2018-05-04 --------------------------------

 * Add Reponse.changes to allow handlers to accumulate a list of changes
   that occure durring a request.  

 * Updated LogHandler to persist Response.changes values

2018-05-02 -------------------------------- 
 
 * Corrected rql handling off 'offset'

 * Corrected rql handling double quoted string params

 * Added test for double/single quotes

 * Rehabbed TestRql so that all tests pass

2018-04-24 --------------------------------
 
 * Fixed incorrectly named "href" fields for expanded relationships

2018-04-04 --------------------------------
 
 * Changed Endpoint.path to Endpoint.paths and Endpoint.method to Endpoint.methods.  A comma separated list
   from either the db configuration or props file configuration will correctly parse into a collection.
   This required a schema tweak and upgradingin to fortj-0.0.3.
   
 * Added the ability to set a Handler directly on an Endpoint for faster props file configuration.  Under 
   the covers in the Endpoint.setHandler method, an empty Action is created to hold the Handler.  
   
 * Added RestHandler as a shortcut that delegates to a stock Get/Post/Delete handler.  This made it much
   easier to minimize configuration now that a Endpoint can easily map to multiple methods.
   
2018-03-02 --------------------------------

 * Corrected Endpoint order sorting defect
 * Added support for comma separated list of methods for Endpoint matching

2018-02-23 --------------------------------

 * Enhanced pluralization redirects to cover 404 errors generated by handlers.
 * Prevented attempts at pluralization of things ending in 's'.  It was to error prone
 * Added 'order' column to Endpoint so that wildcard with conflicting paths can be prioritized
 * Added quoting for tables to prevent sql errors when tables have reserved names
  
2018-02-18 --------------------------------
 
 * Enabled schema refresh.  If api.debug=ture and api.refresh=true the schema will be refreshed
   from the DB within 60 seconds of the last API call.  This "developer mode" allows you to 
   change the target DB and almost immediately see the changes in the api.  This applies to changes
   in the target schema AND changes in the api configuration.  For example if you were to add a new
   end point mapping.

 * Added a 4th from of API to url mappin that ommits the 
   api.code when the api.code and account.code match.  This avoids host.com/myapi/myapai/collection
   duplication.
   
   http(s)://whateverhost/[${servletPrefix}/]${account.code}/${collection}

2017-11-16 --------------------------------

* Changed default behavior of the GetHandler so that all responses are wrapped as if they were
  paginated with a wrapper {meta:{}, data:[]}
  
* Enabled redirection to pluralized version of a collection if the singular form is used  

* Removed schema table URL and added schema columns Account.code and Api.code.  Now instead of 
  mapping an API to a specific URL the urls must be in one of three forms:
  
  1. http(s)://whateverhost/[${servletPrefix}/]${account.code}${api.code}/${collection}
  2. http(s)://${account.code}.domain.com/[${servletPrefix}/]${account.code}${api.code}/${collection}
  3. http(s)://${account.code}.domain.com/[${servletPrefix}/]${api.code}/${collection}
  
  
  
             
             
             


