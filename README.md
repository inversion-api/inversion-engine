# rckt_snooze

API as a service platform
The fastest way to deliver a REST API.

"Snooze" is a REST API automation platform.  Snooze is not a code generator it is a runtime service that exposes a 
reflectively created secure best practice JSON REST API for CRUD operations against Relational Database Systems (RDBMS) 
such as MySQL, ElasticSearch, and Amazon's Dynamo DB.  

With Snooze, you can connect your web application front end directly to your data source without any server side programming required.  

So why is it called Snooze, you might ask?  Well we are a bunch of code nerds not marketers.  This is a REST automation framework so, calling it "Snooze" seemed nerd punny to us.


## Contents
1. [Features & Benefits](#features--benefits)
1. [Quick Start](#quickstart)
1. [Config File](#config-file)
1. [Resource Query Language (RQL)](#resource-query-language-rql)
   * [Reserved Query String Parameters](#reserved-query-string-parameters)
   * [Restricted & Required Parameters](#restricted--required-query-parameters)
   * [Sorting / Ordering](#sorting--ordering)
   * [Pagination / Offset & Limit](#pagination--offset--limit)
   * [Query Filters](#query-filters)
   * [Aggregations](#aggregations)
   * [Nested Document Expansion](#nested-document-expansion)
   * [Property Inclusion / Exclusion](#propert-inclusion--exclusion)
   * [Miscellaneous](#miscellaneous)
1. [Security Model](#security-model)
   * [Access / Login / Sessions](#access--login--sessions)
   * [Roll & Permission Based Authorization](#roll--permission-based--authorization)
   * [Multi-Tenant APIs](#multi-tenant-apis)
   * [Row Level Security](#row-level-security)
1. [Object Model](#object-model)
   * Api
   * Endpoitns
   * Actions
   * Handler
   * Database
1. [Handlers](#handlers)
   * SQL CentricRest   
     * Get
     * Put/Post
     * Post
     * Delete
    
   
1. [Developer Notes](#developer-notes)
   * [Logging](#logging)
   * Gradle, Maven, etc.    
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
 * Always COORS cross site capable
 * Multiple RDBMS back ends in a single API
 * "Smart PUT/POST" - no id/href field no problem. Nested/mixed put/post 
 * "Explain" mode shows you the exact statements to be run
 * Mutli tenant design  
 * Elegant pluralization of db tables w/ intelligant redirect
 * 


  
## Quickstart





## Configuring your API




### URL Structure

Snooze is designed to host multiple APIs potentially owned by controlled by different organizations.  All functional URLs for an API are prefixed 
with an Account Code and API Code path components.  The Account Code uniquely identifies the organization that owns the API and is unique to the 
platform. The ApiCode uniquely identifies the Api within the namespace created by the AccountCode. 

Valid URL formats are:

 * https://host.com/[${servletPath}]/${accountCode}/${apiCode}/
 * https://host.com/[${servletPath}]/${accountCode}/${apiCode}/
 * https://${accountCode}.host.com/[${servletPath}]/${apiCode}/
 * https://host.com/[${servletPath}]/${accountCode} ONLY when apiCode and accountCode are the same thing (used to make a prettier URL for a 'default' Api per Account)


### Account

Snooze itself is multi-tenant, designed to host multiple APIs that may be owned by different organizations.  An Account represents an organization that
ownes an Api...which may be different from the organization that is running the Snooze instance hosting the Api.  


### Api

An API exposes a set of Endpoints.  Generally, Snooze will auto configure Endpoints that map to Db backed Collections for CRUD operations.   
   

### Collections, Entities, and Attributes

Collections logically map to Db Tables.  An Entity logically represents a row in the Table.  An Attribute logically represents a Table Column.  Clients send GET/PUT/POST/DELETE requets
to Collections to perform CRUD operations on the underlying Db.  Collection and Attribute names can be mapped (or aliased) when the Table name or Column name would not work well
in a URL or as a JSON property name.


### Endpoints, Actions and Handlers

Endpoints, Actions, and Handlers are how you map requests to the work that actually gets done. Programmers familiar with AOP might be comfortable with a loose metaphore of a an Endpoint 
being a Join point, an Action being a Pointcut, and a Handler being an Aspect.  There is nothing application specific about this pattern.  The magic of Snooze is in the implementation 
of various Handlers.

An Endpoint represents a specific combination or a URL path (that may contain a trailing wildcard *) and one or more HTTP methods that will be called by clients.  One or more Actions are selected
to run when an Endpoint is called by a client.  

Actions link Endpoints to Handlers.  Behind each Endpoint can be an orderd list of Actions. Actions can contain configuration used by Handlers.  One Handler instance may behave differently
based on the configuration information on the Action.  Actions are mapped to URL paths / http methods and as such may be selected to run as part of one or more Endpoints.     

Work is done in the Handlers.  If an application must have custom business logic or otherwise can't managed to achieve a desired result via configuration, 99% of the time, the answer
is a custom Handler. Handlers do not have to be singletons but the design pattern is that anything that would be "use case specific" should be abstracted into Action config which
ties things back to the url/http method being invoked.


Example Handlers:
 * SqlGetHandler
 * S3UploadHandler
 * AuthHandler
 * AclHandler
 * LogHandler




### AclRule
### Permissions
### Change
### Db - change to data source to accomodate dynamo and elastic
### Table
### Column
### Relationship

### Tenant - relationship between app and multiple users of the app - goes with use of the api





## Resource Query Language (RQL)

RQL is the set of query string parameters that you use to query a REST collection for entities that match specific criteria or otherise alter 
the operation to be performe and the response values. 


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
  
    
  
  
# RQL Query String Params to Elastic
Currently, the following functions are available for use:

* `source=value1,value2,valueN` - MUST be a separate parameter. Limits returned data to only these property values.
* auto-suggest paths should be in the following format: `.../elastic/indexType/suggest?suggestField=value&type=prefix`
> a `type` auto-suggest parameter can be set to define the type of search.  By default, auto-suggest will do a 'prefix' search.  If no results are found, auto-suggest will then try a wildcard search and return those results.  If you want to limit auto-suggest to one type of search, set `type=prefix` or `type=wildcard`.  While both types are fast, prefix searches are the fastest (~2ms vs ~20ms) 
* nested searching is allowed simply by specifying the name of the nested field, such as: `player.location.city` would retrieve the nested `location.city` value from a player. 

The index/type will be automatically generated so that only one value needs to be sent.  
`/elastic/location/location?and(and(eq(locationCode,270*),eq(city,Chandler)),and(eq(address1,*McQueen*)))`

The index/type used above `.../elastic/location/location?and(...` is the same as `/elastic/location?and(...`.
In the second example, it is assumed that the index/type `location` are the same value.

#### RQL Examples
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





## Dynamo Specifics

 ### Dynamo Specific Configuration

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
requests.  The values will be appended to the 





## Security Model

An Api is associated with a single Account

Users have system level Role relationship with one or more Accounts.  

Roles really are not supposed to have a functional relationship to Api/application being served.  Roles do not provide
application level entitlements, they provide Snooze level entitlements.

Roles:
 * Owner - The person who can delete a Snooze account.  
 * Administrator - Someone who can configure an account including changing changing security and managing Users.
   (Owner and Administrator are not designed to be functionally useful for Api clients but there is nothing stopping you from
   requiring Owner or Administrator Roles to access various Api Endpoints, see below)
 * Member - Generally, someone who will be calling the Api.  An admin user of an end application, my only have the 
   Snooze Member role.
 * Guest - Represents an unauthenticated caller.

Roles are hierarchical by privilege.  Ex. having the Owner role gives you Member and Guest authority.     

For each Api a Users can be assigned Api defined named Permissions.  Permissions are simple string tokens.  They do
not confer hierarchical privilege like roles.  An Api designer might define Permission string such as "ADMINISTRATOR"
or "SOMECOLLECTION_SOMEHTTPMETHOD".  This is a total designers choice free for all.  

Groups of Users can be created at the Account level and each Group can be given Roles and Permissions 
that the Users inherit.  In this way, you could create a functional "Admin" Group (different from Administrtor Role) 
and give that Group all of the Permissions required and then assign Users to that Admin Group. 


### Access / Login / Sessions

Each Api defines one or more Endpoints.  If authentication is going to be used, at least one of the Endpoints
must be configured with an Action that handles authentication. See AuthHandler.

As far as the framework is concerned, authentication involves populating a User object with their entitled
Roles and Permissions placing the User object on the Request.  Session management, barer tokens etc. are left up 
to the handler implementation.  

AuthHandler is the default authentication provider.  It currently supports HTTP basic auth along with, username/password 
query string params and session tokens.  Additionally if an application chooses to provide a JWT signed with a User's
secretKey, the roles and permissions defined in the JWT will become the roles and permissions assigned to the 
User for that request and any roles and permissions defined in the DB will not be used.  
 
Failing Authentication should return a 501 Unauthorized HTTP response code.


### Roll & Permission Based Authorization 

After a users has been successfully logged in, the Request User object will be populated with the Role and 
Permission obejects assigned to the user.

Acl objects can be set on the Api to declare different combinations of roles and privildges that Users must
have to access different method/path combinations.

The AclHandler (or a developer provided customization/substitute) can be configured on an Action perform
the actual work of comparing the current request to the users roles and permissions.

Acl objects can be ordered through their "order" field.  They are processed by AclHandler in sort order. 
The first rule to "allow" access wins.  If no rule allows access, then a 403 is thrown.



### Multi-Tenant APIs

Api's can be flagged as 'multiTennant'.  If so, the collection key in the url prefix must be immediately 
preceded by a tenantCode.

Ex: http://localhost/accountCode/apiCode/tenantCode/collectionKey/[entityKey] 




The first line of defense 

Many Apis will expose multi-tenant databases and authorization to access and Endpoint should not necessarily 
entitle a User to all the data found at that Endpoint.




### Row Level Security 


SUSPECT NEED TO UPDATE

The simplest way to restrict a users interaction with a row is to provide a "userId" column on the 
table in question.  The param "userId" is a known special case 

If row level security is required, a filter SQL statements can be provided as a JSON Action params.
 

An Action can be configured with "required" params which means that the api caller must supply
these as query string name=value pairs.  

```javascript
{
	required: ['userId', 'col1', 'col2']
}
```








## Developer Notes

## Logging
 * Snooze uses logback, but it is not configured out of the box - the service implementing Snooze will be responsible for providing their own logback.xml config file!


### Gradle, Maven, etc.

If you want to extend Snooze as part of a custom application, you can use jitpack to pull your preferred branch dirctly from GitHub into your project.   

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
  
  
  
             
             
             


