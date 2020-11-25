![Inversion Logo Title](docs/images/logoheader.png)


Inversion is the fastest way to deliver full featured and secure REST APIs.

With Inversion, you can create an API that connects your app front end directly to your back end data source without any server side programming required.

Inversion is not a code generator it is a runtime service that reflectively creates secure best practice JSON REST APIs for CRUD operations against
multiple back end data sources including Relational Database Systems (RDBMS) such as MySQL, and PostgreSQL, NoSQL systems including Elasticsearch and Amazon's DynamoDB, Azure CosmosDb and many more.

#### Build Status

| Project                                                                                                                    | Latest Build                                                                                                                                                                              | Test Status                                                                                                                                                         |
| -------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [Inversion Quickstart Spring Boot](https://github.com/inversion-api/inversion-quickstart-spring-boot)                      |                                                                                                                                                                                           | [![](https://travis-ci.org/inversion-api/inversion-quickstart-spring-boot.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-quickstart-spring-boot) |
| [Inversion Quickstart Lambda](https://github.com/inversion-api/inversion-quickstart-lambda)                                |                                                                                                                                                                                           | [![](https://travis-ci.org/inversion-api/inversion-quickstart-lambda.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-quickstart-lambda)           |
| [Inversion Demos](https://github.com/inversion-api/inversion-demos)                                                        | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-demos/images/download.svg) ](https://bintray.com/inversion/repo/inversion-demos/_latestVersion)                   | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Api Core Libs](https://github.com/inversion-api/inversion-api)                                                  | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-api/images/download.svg) ](https://bintray.com/inversion/repo/inversion-api/_latestVersion)                       | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion CosmosDB Connector](https://github.com/inversion-api/inversion-engine/tree/master/inversion-cosmosdb)           | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-cosmosdb/images/download.svg) ](https://bintray.com/inversion/repo/inversion-cosmosdb/_latestVersion)             | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion DynamoDB Connector](https://github.com/inversion-api/inversion-engine/tree/master/inversion-dynamodb)           | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-dynamodb/images/download.svg) ](https://bintray.com/inversion/repo/inversion-dynamodb/_latestVersion)             | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Elasticsearch Connector](https://github.com/inversion-api/inversion-engine/tree/master/inversion-elasticsearch) | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-elasticsearch/images/download.svg) ](https://bintray.com/inversion/repo/inversion-elasticsearch/_latestVersion)   | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Kinesis Connector](https://github.com/inversion-api/inversion-engine/tree/master/inversion-kinesis)             | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-kinesis/images/download.svg) ](https://bintray.com/inversion/repo/inversion-kinesis/_latestVersion)               | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Lambda Support](https://github.com/inversion-api/inversion-engine/tree/master/inversion-lambda)                 | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-lambda/images/download.svg) ](https://bintray.com/inversion/repo/inversion-lambda/_latestVersion)                 | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Redis Connector](https://github.com/inversion-api/inversion-engine/tree/master/inversion-redis)                 | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-redis/images/download.svg) ](https://bintray.com/inversion/repo/inversion-redis/_latestVersion)                   | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion S3 Connector](https://github.com/inversion-api/inversion-engine/tree/master/inversion-s3)                       | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-s3/images/download.svg) ](https://bintray.com/inversion/repo/inversion-s3/_latestVersion)                         | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Scripting Support](https://github.com/inversion-api/inversion-engine/tree/master/inversion-script)              | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-script/images/download.svg) ](https://bintray.com/inversion/repo/inversion-script/_latestVersion)                 | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |
| [Inversion Spring Boot Support](https://github.com/inversion-api/inversion-engine/tree/master/inversion-spring-boot)       | [ ![Download](https://api.bintray.com/packages/inversion/repo/inversion-spring-boot/images/download.svg) ](https://bintray.com/inversion/repo/inversion-spring-boot/_latestVersion)       | [![](https://travis-ci.org/inversion-api/inversion-engine.svg?branch=master)](https://travis-ci.org/inversion-api/inversion-engine)                                 |

## Contents
- [Inversion Cloud API Engine](#inversion-cloud-api-engine)
  - [Build Status](#build-status)
  - [Contents](#contents)
  - [Features and Benefits](#features-and-benefits)
  - [Quick Start](#quick-start)
    - [Maven Repos](#maven-repos)
    - [Coding Your Own API](#coding-your-own-api)
    - [Configuration Instead of Code](#configuration-instead-of-code)
  - [URL Structure](#url-structure)
  - [Configuring Your API](#configuring-your-api)
    - [Configuration File Loading](#configuration-file-loading)
  - [Keeping Passwords out of Config Files](#keeping-passwords-out-of-config-files)
  - [Resource Query Language (RQL)](#resource-query-language-rql)
    - [General](#general)
    - [Query Functions](#query-functions)
    - [Sorting and Ordering](#sorting-and-ordering)
    - [Pagination, Offset and Limit](#pagination-offset-and-limit)
    - [Property Inclusion / Exclusion](#property-inclusion--exclusion)
    - [Aggregations](#aggregations)
    - [Nested Document Expansion](#nested-document-expansion)
    - [Reserved Query String Parameters](#reserved-query-string-parameters)
    - [Restricted and Required Query Parameters](#restricted-and-required-query-parameters)
    - [Miscellaneous](#miscellaneous)
  - [Core Object Model Concepts](#core-object-model-concepts)
    - [Apis](#apis)
    - [Dbs, Tables, Columns and Indexes](#dbs-tables-columns-and-indexes)
    - [Collections, Entities, Attributes and Relationships](#collections-entities-attributes-and-relationships)
    - [Endpoints and Actions](#endpoints-and-actions)
    - [AclAction and AclRules](#aclaction-and-aclrules)
    - [Path Matching](#path-matching)
  - [Security Model](#security-model)
    - [Account Roles](#account-roles)
    - [Api Permissions](#api-permissions)
    - [Authentication](#authentication)
    - [Authorization](#authorization)
    - [Multi-Tenant APIs](#multi-tenant-apis)
    - [Row Level Security](#row-level-security)
  - [Elasticsearch Specifics](#elasticsearch-specifics)
      - [Elasticsearch RQL Examples](#elasticsearch-rql-examples)
  - [DynamoDB Specifics](#dynamodb-specifics)
  - [Developer Notes](#developer-notes)
    - [Javadocs](#javadocs)
    - [Logging](#logging)
    - [Gradle, Maven, etc.](#gradle-maven-etc)
  - [REST API Design Resources](#rest-api-design-resources)
    - [HTTP Status Codes](#http-status-codes)
    - [Standards-ish](#standards-ish)
    - [Web Query Languages](#web-query-languages)
    - [Best Practices and Design Resources](#best-practices-and-design-resources)
    - [REST APIs in the Wild](#rest-apis-in-the-wild)
 
   
## Features and Benefits
 * Deploy a secure JSON REST API against your back end database in five minutes without any coding.
 * Support for MySql, PostgreSQL, Microsoft SqlServer, AWS DynamoDb, ElasticSearch, Azure CommosDb, S3, Redis and many more.
 * Makes structured relational database backed APIs as frictionless as working with an unstructured document store dbs.
 * Database tables are automatically exposed as REST collections endpoints.
 * Database foreign key relationships result in nested documents that can be retrieved in a single request to eliminate over fetching.
 * Elegant "beautification" and pluralization of "ugly" table and column names.  All collection and attribute names are converted to JSON friendly camelCase.  "MY_COLUMN_NAME" gets converted to "myColumnName".
 * Smart pagination and ordering with a consistent document envelope.
 * Powerful Resource Query Language (RQL) lets you select the exact resources you are looking for.
 * For complex nested document PUT/POSTS, all resources are "upserted" with new or updated foreign key dependencies updated first and relationships resolved automatically.  You can compose a rich client side JSON model mixing existing and new resources and PUT/POST the entire object in one request.
 * SQL Injection proof through the use of prepared statements instead of dynamic sql.
 * Permission and role based declarative security model.
 * Always CORS cross site enabled.
 * Expose multiple back ends databases in a single API.
 * Designed to support single and multi-tenant API usage patterns.
 * "Explain" mode shows you exactly what the back end is doing including all SQL statements run.
 * Configured via drop dead simple properties files or directly in Java code.



## Quick Start

With just a few lines of code, [Demo001SqlDbNorthwind.java](https://github.com/inversion-api/inversion-demos/blob/master/src/main/java/io/rocketpartners/demo/demo001/Demo001SqlDbNorthwind.java)
launches a full featured demo API that exposes SQL database tables as REST collection endpoints.  The demo 
supports full GET,PUT,POST,DELETE operations with an extensive Resource Query Language (RQL) for GET requests.
 
The demo connects to an in memory H2 SQL database that gets initialized from
scratch each time the demo is run.  That means you can fully explore
modifying operations (POST,PUT,PATCH,DELETE) and 'break' whatever you want. When
you restart that data will be back in its original demo state.

To run the demo simply clone the GitHub repo, build it with Gradle, then launch the demo app via Gradle.

```
git clone https://github.com/inversion-api/inversion-engine.git
./gradlew build
./gradlew demo1
```

The demo API is now running at 'http://localhost:8080/northwind with REST collection endpoints for each DB resource.

You can get started by exploring some of these urls:
 - GET http://localhost:8080/northwind/products
 - GET http://localhost:8080/northwind/orders?expands=orderDetails&page=2
 - GET http://localhost:8080/northwind/customers?in(country,France,Spain)&sort=-customerid&pageSize=10
 - GET http://localhost:8080/northwind/customers?orders.shipCity=Mannheim
      
Append '&explain=true' to any query string to see an explanation of what is happening under the covers
 - GET http://localhost:8080/northwind/employees?title='Sales%20Representative'&sort=employeeid&pageSize=2&page=2&explain=true


### Maven Repos

This project uses Travis CI to publish master branch commits as snapshot builds to 
the [oss.jfrog.org (aka OJO) maven repository](https://oss.jfrog.org/artifactory/webapp/#/artifacts/browse/tree/General/oss-snapshot-local/io/inversion/) and 
release branch commits to to the [Inversion bintray maven repository](https://bintray.com/inversion/repo) 
which syncs to jcenter. 

If you are a Java developer comfortable with maven, you can include the following repos 
in your project pull release or snapshot builds.  Here is a gradle config example.

```
   repositories {
 		jcenter()
 		maven { url 'https://oss.jfrog.org/artifactory/oss-snapshot-local' }
    }
    
    dependencies {

		//change to pull the version you want 
		compile "io.inversion:inversion-api:0.6+"
		
		//you can pin to a specific release build like ths
		//these come from jcenter / bintray
		//compile "io.inversion:inversion-api:0.6.18"
		
		//any version that includes "-SNAPSHOT" will pull
		//from the oss.jfrog.org repo, not jcenter
		//compile "io.inversion:inversion-api:0.6.18-SNAPSHOT"
		
		//if you are including multiple inversion libs, it is a good 
		//idea to set a variable such as "inversionVersion=0.6.18" in your 
		//gradle.properties file and use that to import your dependencies 
		//compile "io.inversion:inversion-api:${inversionVersion}"
		//compile "io.inversion:inversion-dynamodb:${inversionVersion}"
		//compile "io.inversion:inversion-s3:${inversionVersion}"
		//compile "io.inversion:inversion-elasticsearch:${inversionVersion}"

	}
    
```

If OJO snapshots are not cutting edge enough for you, you can also pull snapshots directly from GitHub 
via [Jitpack](https://jitpack.io/#inversion-api/inversion-engine).  While you can pull tagged releases 
from Jitpack, it is not recommended.  Please consider only use Jitpack for pulling snapshots of branches.

```
	repositories {
		jcenter()
		maven { url 'https://jitpack.io' }
		}
		
	dependencies {
		
		compile 'compile 'com.github.inversion-api.inversion-engine:inversion-api:master-SNAPSHOT'
		compile 'compile 'com.github.inversion-api.inversion-engine:inversion-dynamodb:master-SNAPSHOT'
		
	}
```



### Coding Your Own API

You can build an API that connects to your own DB back end with just a few lines of Java code.

```java
InversionMain.run(new Api()
            .withName("demo")
            .withDb(new JdbcDb("dbNickname", 
                              "${YOUR_JDBC_DRIVER}", 
                              "${YOUR_JDBC_URL}", 
                              "${YOUR_JDBC_USERNAME}", 
                              "${YOUR_JDBC_PASSWORD"))
            .withEndpoint("GET,PUT,POST,DELETE", "/*", new DbAction()));
```

### Configuration Instead of Code

Or, if you prefer, you can wire up an API via configuration files, instead of through code. 
The properties file below will create an identical API to the Java coded example above

Place the example below into a file "./inversion.properties". 

```properties
demo.class=io.inversion.Api

db.class=io.inversion.jdbc.JdbcDb
db.driver=${YOUR_JDBC_DRIVER}
db.url=${YOUR_JDBC_URL}
db.user=${YOUR_JDBC_USERNAME}
db.pass=${YOUR_JDBC_PASSWORD}

ep.class=io.inversion.Endpoint
ep.methods=GET,PUT,POST,DELETE
ep.path=/*
ep.actions=rest

rest.class=io.inversion.action.db.DbAction
```

Then launch Inversion and it will wire up your API from the configuration.  If 
Inversion has already been built, you skip the 'gradle build' in the example below.

```
gradle build
java -jar build/libs/rocket-inversion-master.jar
```

## URL Structure

Inversion is designed to host multiple APIs potentially owned by different tenants.  All URLs are prefixed with an accountCode and apiCode path components.  The accountCode uniquely identifies the organization that owns the API and is unique to the host server. The apiCode uniquely identifies the Api within the namespace created by the accountCode. 

Valid based URL formats are:
 * http(s)://host.com/[${servletPath}]/${accountCode}/${apiCode}/
 * http(s)://host.com/[${servletPath}]/${accountCode}/${apiCode}/
 * http(s)://${accountCode}.host.com/[${servletPath}]/${apiCode}/
 * http(s)://host.com/[${servletPath}]/${accountCode} ONLY when apiCode and accountCode are the same thing (used to make a prettier URL for a 'default' Api per Account)

A default configuration would then offer Endpoint URLs such as below where ${COLLECTION} is the pluralized version of your table names.  Non 
plural versions will be redirected to the plural url.
 * ${API_URL}/[${OPTIONAL_ENDPOINT_PATH}]/${COLLECTION}/
 * ${API_URL}/[${OPTIONAL_ENDPOINT_PATH}]/${COLLECTION}/${RESOURCE}
 * ${API_URL}/[${OPTIONAL_ENDPOINT_PATH}]/${COLLECTION}/${RESOURCE}/${RELATIONSHIP}

Examples example:  
 * 'http&#58;//localhost/johns_books/orders' would return a paginated listing of all orders from the api with an accountCode and apiCode of 'johns_books'
 * 'http&#58;//localhost/johns_books/orders/1234' would return the details of order 1234
 * 'http&#58;//localhost/johns_books/orders/1234/books' would return all of the books related to the order without returning order 1234 itself
 * 'http&#58;//localhost/johns_books/orders/1234?expands=books' would return the 1234 details document with the related array of books already expanded (see document expansion below) 

## Configuring Your API


### Configuration File Loading

An Inversion API can be wired up natively in Java code or via configuration files.

When started, Inversion attempts to locate files with the following naming conventions in the classpath (including the working directory).
* inversion[-][1-100].properties
* inversion[-][1-100][-${inversion.profile}].properties
* inversion[-][-${inversion.profile}][-][1-100].properties

${inversion.profile} is an optional environment variable or Java system param that allows you to load different configrations at runtime based on container attributes.  Files are loaded sequentially and files with a matching ${inversion.profile} in their hame are loaded AFTER files without a profile.  

All files are loaded into a shared map so "the last loaded 
key wins" in terms of overwriting settings.  This design is intended to make it easy to support multiple runtime configurations such as 'dev' or 'prod' with files that do not have to duplicate config between them.

A typical configuration, for example, may load:
* inversion1.properties, inversion2.properties,inversion-dev-1.properties, if ${inversion.profile} has been set to 'dev'
* OR, inversion.properties, inversion-prod.properties,inversion-prod-1.properties if ${inversion.profile} has been set to 'prod'

A helpful development trick here is to launch Inversion with a JVM parameter '-Dinversion.profile=dev' add something like "inversion99-dev.properties" 
to your .gitignore and keep any local developement only settings in that file.  That way you won't commit settings you don't want to share 
and you custom settings will load last trumping any other keys shared with other files. 

Each config file itself is a glorified bean property map in form of bean.name=value. Any bean in scope can be used as a value on the right side of the assignment and '.'
notation to any level of nesting on the left hand side is valid.  You can assign multiple values to a list on the left hand side by setting a comma separated list ex: 
bean.aList=bean1,bean2,bean3.  Nearly any JavaBean property in the object model (see Java Docs) can be wired up through the config file.  


Configuration and API bootstrapping takes place in the following stages:

1. Inversion service wiring - All 'inversion.*' bean properties are set on the core Inversion service instance.  This is useful for things like setting 'inversion.debug' or changing 'inversion.profile'.  
1. Initial loading - This stage loads/merges all of the user supplied config files according to the above algorithm
1. Api and Db instantiation - All Api and Db instances from the user supplied config are instantiated and wired up.  This is a minimum initial wiring. 
1. Db reflection - 'db.bootstrapApi(api)' is called for each Db configed by the user.  The Db instance reflectively inspects its data source and creates a Table,Column,Index
model to match the datasource and then adds Collection,Resource,Attribute,Relationship objects to the Api that map back to the Tables,Columns and Indexes. 
1. Serialization - The dynamically configured Api model is then serialized back out to name=value property format as if it were user supplied config.    
1. The user supplied configs from step 1 are then merged down on onto the system generated config.  This means that you can overwrite any dynamically configured key/value pair.  
1. JavaBeans are auto-wired together and all Api objects in the resulting output are then loading into Inversion.addApi() and are ready to run.

This process allows the user supplied configuration to be kept to a minimum while also allowing any reflectively generated configuration to be overridden.  Instead
of configing up the entire db-to-api mapping, all you have to supply are the changes you want to make to the generated defaults.  This reflective config generation
happens in memory at runtime NOT development time.  

If you set the config property "inversion.configOut=some/file/path/merged.properties" Inversion will output the final merged properites file so you can inpspect any keys to find 
any that you may want to customize. 

The wiring parser is made to be as forgiving as possible and knows about the expected relationships between different object types.  For instance if you do NOT
supply a key such as "api.dbs=db1,db2,db3" the system will go ahead and assign all of the configed Dbs to the Api.  If there is more than one Api defined or if
the dbs have been set via config already, this auto wiring step will not happen.  Same thing goes for Endpoints being automatically added to an Api.


## Keeping Passwords out of Config Files

If you want to keep your database passwords (or any other sensative info) out of your inversion.properties config files, you can simply set an environment variable OR
JVM system property using the relevant key.  For example you could add '-Ddb.pass=MY_PASSWORD' to your JVM launch configuration OR something like 'EXPORT db.pass=MY_PASSWORD' to the top of the batch file you use to launch our app or application server.  



## Resource Query Language (RQL)

RQL is the set of HTTP query string parameters that allows developers to "slice and dice" the data returned from the API to meet their specific needs.

### General
 
* Many functions can be written in one of several equivelant forms:
 * name=value - the traditional query string format
 * function(column, value OR expression) - eq(col,value), lt(column,value), and(eq(col,value), lt(column,value)), in(col, val1, val2, val3, val4)
 * name=eq=value, column=lt=value, col=in=val1,val2,val3
* Quotes & Escaping Quotes - ' or " can be used to quote values.  Use a \ to escape any inner occurances of the outter quote.  If you quote with single quotes you don't have to escape inner double quotes and vice versa  
* Wildcards - the '*' character is treated as a univeral wildcard for all supported back ends.  
  For example, for SQL back ends '*' would be substituded with "%" and instead of using '=' operator the system would substitude 'LIKE'.  You use the normal '=' or 'eq' operator but the system uses LIKE and % under the covers.


[See TestSqlQuery.java for many examples of complex RQL queries and how they translate into SQL](https://github.com/RocketPartners/rocket-inversion/blob/master/src/test/java/io/rocketpartners/action/sql/TestSqlQuery.java)


### Query Functions

 | RQL Function                     |      Database      |      Elastic       |       Dynamo       | Description                                                                                          |
 | -------------------------------- | :----------------: | :----------------: | :----------------: | ---------------------------------------------------------------------------------------------------- |
 | column=value                     | :heavy_check_mark: |  :grey_question:   |  :grey_question:   | translates as expected into a sql column equality check "column = value"                             |
 | column='singleTicks'             | :heavy_check_mark: |  :grey_question:   |  :grey_question:   | 'values can have spaces with encapsulated in quotes'                                                 |
 | column="doubleQuotes"            | :heavy_check_mark: |  :grey_question:   |  :grey_question:   | "double or single quotes work"                                                                       |
 | column=" ' "                     | :heavy_check_mark: |  :grey_question:   |  :grey_question:   | "the first quote type wins so a single quote like ' inside of double quotes is considered a literal" |
 | column=" \" "                    | :heavy_check_mark: |  :grey_question:   |  :grey_question:   | "you can also \" escape quotes with a backslash"                                                     |
 | column=wild*card                 | :heavy_check_mark: |  :grey_question:   |  :grey_question:   | something*blah - translates into "column LIKE 'something%blah'"                                      |
 | eq(column,value)                 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | alternate form of column=value                                                                       |
 | gt(column,value)                 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | greater than query filter eg: "column < value"                                                       |
 | ge(column,value)                 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | greater than or equal to                                                                             |
 | lt(column,value)                 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | less than filter                                                                                     |
 | le(column,value)                 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | less than or equal to                                                                                |
 | ne(column,value)                 | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | not equal                                                                                            |
 | in(column,val1,[val2...valN])    | :heavy_check_mark: | :heavy_check_mark: |                    | translates into "where column in (val1,....valN)"                                                    |
 | out(column,val1,[val2...valN])   | :heavy_check_mark: | :heavy_check_mark: |                    | translates into "where column NOT in (val1,....valN)"                                                |
 | and(clause1,clause2,[...clauseN) | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | ANDs multiple clauses.  Example: and(eq(city,Atlanta),gt(zipCode,30030))                             |
 | or(clause1,clause2,[...clauseN)  | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark: | ORs multiple clauses.  Example: or(eq(city,Atlanta),gt(zipCode,30030))                               |
 | emp(column)                      |  :grey_question:   | :heavy_check_mark: |                    | retrieves empty rows for a column value. null or empty string values will be retrieved               |
 | nemp(column)                     |  :grey_question:   | :heavy_check_mark: |                    | retrieves all rows that do not contain an empty string or null value for a specified column          |
 | n(column)                        |  :grey_question:   | :heavy_check_mark: | :heavy_check_mark: | retrieves all rows that contain a null value for a specified column                                  |
 | nn(column)                       |  :grey_question:   | :heavy_check_mark: | :heavy_check_mark: | retrieves all rows that do not contain a null value for a specified column                           |
 | w(column,[value])                |                    | :heavy_check_mark: | :heavy_check_mark: | retrieves all rows 'with' that wildcarded value in the specified column                              |
 | ew(column,[value])               |                    | :heavy_check_mark: |                    | retrieves all rows that 'end with' that wildcarded value in the specified column                     |
 | sw(column,[value])               |                    | :heavy_check_mark: | :heavy_check_mark: | retrieves all rows that 'start with' that wildcarded value in the specified column                   |

 
 ### Sorting and Ordering

 | RQL Function               |      Database      | Elastic |       Dynamo       | Description                                                                         |
 | -------------------------- | :----------------: | :-----: | :----------------: | ----------------------------------------------------------------------------------- |
 | sort=col1,+col2,-col3,colN | :heavy_check_mark: |         | :grey_exclamation: | use of the + operator is the implied default.  Prefixing with "-" sorts descending. |
 | sort(col1,[...colN])       | :heavy_check_mark: |         |                    | same as sort= but with "function format"                                            |
 | order                      | :heavy_check_mark: |         |                    | an overloaded synonym for "sort", the two are equivelant.                           |


### Pagination, Offset and Limit

 | RQL Function |      Database      | Elastic | Dynamo | Description                                                                                  |
 | ------------ | :----------------: | :-----: | :----: | -------------------------------------------------------------------------------------------- |
 | page=N       | :heavy_check_mark: |         |        | translates into an offset clause using pagesize (or the default page size) as the multiplier |
 | pagenum=N    | :heavy_check_mark: |         |        | an overloaded synonym for "page", the two are equivelant.                                    |
 | pagesize=N   | :heavy_check_mark: |         |        | the number of results to return                                                              |
 | offset=N     | :heavy_check_mark: |         |        | directly translates into a sql offset clause, overrides any page/pagenum params supplied     |
 | limit=N      | :heavy_check_mark: |         |        | directly translates into a SQL limit clause, overrides any pagesize params supplied          |
  

### Property Inclusion / Exclusion

 | RQL Function            |      Database      | Elastic | Dynamo | Description                                                                                            |
 | ----------------------- | :----------------: | :-----: | :----: | ------------------------------------------------------------------------------------------------------ |
 | includes=col1,col2,colN | :heavy_check_mark: |         |        | restricts the properties returned in the document to the ones specified.  All others will be excluded. |
 | includes(col1...colN)   | :heavy_check_mark: |         |        | same as above                                                                                          |
 | excludes=col1,col2,colN | :heavy_check_mark: |         |        | specifically excludes the supplied props.  All others will be included.                                |
 | excludes(col1...colN)   | :heavy_check_mark: |         |        | same as above                                                                                          |




### Aggregations  

 | RQL Function                                        |      Database      | Elastic | Dynamo | Description                                                              |
 | --------------------------------------------------- | :----------------: | :-----: | :----: | ------------------------------------------------------------------------ |
 | group(col1, [...colN])                              | :heavy_check_mark: |         |        | adds cols to a GROUP BY clause                                           |
 | sum(col, [renamedAs])                               | :heavy_check_mark: |         |        | sums the given column and optionally names the resulting JSON property   |
 | count(col, [renamedAs])                             | :heavy_check_mark: |         |        | counts the given column and optionally names the resulting JSON property |
 | min(col, [renamedAs])                               | :heavy_check_mark: |         |        | sums the given column and optionally names the resulting JSON property   |
 | max(col, [renamedAs])                               | :heavy_check_mark: |         |        | sums the given column and optionally names the resulting JSON property   |
 | sum(col, [renamedAs])                               | :heavy_check_mark: |         |        | sums the given column and optionally names the resulting JSON property   |
 | countascol(col, value, [...valueN])                 | :heavy_check_mark: |         |        | Roughly translates to "select sum(if(eq(col, value), 1, 0)) as value     |
 | distinct                                            | :heavy_check_mark: |         |        | filters out duplicate rows                                               |
 | distinct(column)                                    | :heavy_check_mark: |         |        | filters out duplicates based on the given column                         |
 | if(column OR expression, valwhentrue, valwhenfalse) | :heavy_check_mark: |         |        |


  
* To Document
  * function(sqlfunction, col, value, [...valueN]) - tries to apply the requested aggregate function
  * rowcount

  
### Nested Document Expansion

 | RQL Function                                                   |      Database      | Elastic | Dynamo | Description                                                                                                                                     |
 | -------------------------------------------------------------- | :----------------: | :-----: | :----: | ----------------------------------------------------------------------------------------------------------------------------------------------- |
 | expands=collection.property[...property][,table2.property2...] | :heavy_check_mark: |         |        | if "property" is a foreign key, referenced resource will be included as a nested document in the returned JSON instead of an HREF reference value |
  

### Reserved Query String Parameters

 * **explain** - if you include an 'explain' param (any value other than 'explain=false' is exactly the same as not providing a value) the response will include additional debug information including the SQL run.  The response body will NOT be valid JSON.  For security reasons, Api.debug must be true or the request must be to "localhost" for this to work. 
 * **expands** - A comma separated list of relationships that should be expanded into nested documents instead of referenced by URL in the response body.  For example, if a db 'Order' table has a foreign key to the 'Customer' table, you could query "/orders?expands=customer" or "/customers?expands=orders" to pre expand the relationship and avoid haveing to execute multiple requrests.
 * **includes** - A comma separted list of collection attributes (including dotted.path.references for nested document attributes )that should be included in the response.  All attributes are included if this param is empty...unless they are excluded as below.
 * **excludes** - A comma separated list of collection attributes to exclude.
 

### Restricted and Required Query Parameters

If a table has a column named "userId" or "accountId" these are special case known columns who's
values may not be supplied by an api user request.  The value of these fields always comes from
the User object which is configured during authentication (see above).  This is true for 
RQL query params as well as for JSON properties.

### Miscellaneous

* as(col, renamed) - you can rename a property in the returned JSON using the 'as' operator.  Works just like the SQL as operator.
 
 | RQL Function     |      Database      | Elastic | Dynamo | Description                                                                            |
 | ---------------- | :----------------: | :-----: | :----: | -------------------------------------------------------------------------------------- |
 | as(col, renamed) | :heavy_check_mark: |         |        | change the name of the property in the return JSON, works just like SQL 'as' operator. |


## Core Object Model Concepts

### Apis

An Api exposes a set of Endpoints offering access to Collections backed by Db Tables.  

### Dbs, Tables, Columns and Indexes

The Db, Table, Column and Index objects are modeled off of RDBMS systems but are not specifically bound to RDBMS or SQL based systems.  For example, there are DynamoDb and ElasticsearchDB subclasses of Db.  As would be expected, Tables are composed of Columns and a Db can have one or more Tables. Tables are optimized, related to each other, and constrained through Indexes. 

Implementing support for a new back end datasource is simply a matter of implementing a new Db subclass.  The data source specific Db/Table/Column/Index implementation should abstract all back end details away so that datasource agnostic Actions can work transparently with Collections that may be backed by completely different back end types.

### Collections, Entities, Attributes and Relationships

Collections logically map to Db Tables.  An Resource logically represents a row in the Table.  An Attribute logically represents a Table Column.  Clients send GET/PUT/POST/DELETE requests to Collections to perform CRUD operations on the underlying Db Tables.  Collection and Attribute names can be mapped (or aliased) when the Table name or Column name would not work well in a URL or as a JSON property name.

Example of aliased collection: ``api.collections.db_users.alias=profile`` Notice that the name of the database should be included with the name of the collection in order to set the alias property.


### Endpoints and Actions

An Endpoint maps one or more HTTP methods and URL pattern to an ordered list of one or more Actions via [path matching](#path-matching).  Actions are where the work actually gets done.  If an API needs custom business logic that can not be achieved by configuring an existing Action, a custom Action subclass is the answer.

An Action can be set directly on an Endpoint to be 'private' to that endpoint, or an Action can be added directly to the Api and be eligible to be run across multiple Endpoints.  An example of an Action that would normaly be configured to run across multiple Endpoints would be something like the security AclAction or LogAction.



Example [Handlers](https://rocketpartners.github.io/rckt_inversion/0.3.x/javadoc/io/rcktapp/api/Handler.html):
 * RestGetAction - Returns a Collection listing matching RQL criteria or can return a single requested Resource from a Db Table.
 * RestPostAction - Intelligently handles both PUT (update) and POST (insert) operations including complex nested documents  
 * RestDeleteDelete - Deletes Collection Entities from the underlying Db Table.
 * AuthAction - Logs a user in and puts a User object in the Request
 * AclAction - Processes AclRules to secure your Endpoints
 * LogAction - Logs requests and the JSON payloads
 * S3UploadAction - Allows you to do a HTTP multi-part post upload to an S3 bucket
 * RateLimitAction - Protecs Endpoints from accidental or intentional DoS type traffic


### AclAction and AclRules

AclRules allow you to declare that a User must have specified roles and permissions to access resources at a given url path and http method.  

Generally AuthHandler and AclHandler will 
be setup (in that order) to protect resources according to configed AclRules.  AclRules are matched to urls via [path matching](#path-matching) just like Endpoints and Actions.
    
    
### Path Matching

Endpoints, Actions and AclRules are selected when they can be matched to a request url path.  Each one of these objects contains an "includesPaths" and "excludesPaths" configuration property that takes a comma separated list of paths definitions.  The wildcard character "*" can be used to match any arbitrary path ending.  Regular expressions can be used to match specific path requirements.  If a path is both included and excluded, the exclusion will "win" and the path will not be considered a match. 
Leading and trailing '/' characters are not considered when path matching.

Regular expression matches are modeled off of [angular-ui](https://github.com/angular-ui/ui-router/wiki/URL-Routing#url-parameters) regex path matching.  A regex-based match component follows the pattern "{optionalParamName:regex}".  If you surround any path part with [] it makes that part and all subsequent
path parts optional.  Regular expression matches can not match across a '/'.

Here are some examples: 

 * endpoint1.includesPaths=dir1/dir2/*
 * endpoint1.excludePaths=dir1/dir2/hidden/*
 * endpoint2.includePath=something/{collection:[a-z]}/*
 
One easy way to restrict the underlying tables exposed by an Api is to use regex path matching on your Endopoint.
 
 * endpoint3.includesPaths={collection:customers|books|albums}/[{resource:[0-9]}]/{relationship:[a-z]}

If path params are given names {like_this:regex} then the path value will be added as a name/value pair to the Request params overriding any matching key that may 
have been supplied by the query string.

The names "component", "resource", and "relationship" are special.  If you configure a path match to them, Inversion will use those values when configuring the Request object.
If you don't supply them the parser will assume the pattern .../[endpoint.path]/[collection]/[resource]/[relationship].  


## Security Model
 
### Account Roles

An Api is associated with a single Account

Users have a system level Role relationship with one or more Accounts.  

Roles are not designed to have a functional relationship to an Api being served.  Generally, Roles should not
be used by Api designers to provide application level entitlements.  Roles are designed to provide Inversion system level entitlements.
 

Roles:
 * Owner - The person who can delete a Inversion account.  
 * Administrator - Someone who can configure an account including changing security and managing Users.
   (Owner and Administrator are not designed to be functionally useful for Api clients.  However, there is nothing stopping you from
   requiring Owner or Administrator Roles to access various Api Endpoints, see below)
 * Member - Generally, someone who will be calling the Api.  An admin user of an end application may only have the 
   Inversion Member role.
 * Guest - Represents an unauthenticated caller.

Roles are hierarchical by privilege.  Ex. having the Owner role gives you Administrator, Member and Guest authority.     

### Api Permissions

For each Api, a User can be assigned Api-defined named Permissions.  Permissions are simple string tokens.  They do
not confer hierarchical privilege like roles.  An Api designer might define a Permission string such as "ADMINISTRATOR"
or "SOMECOLLECTION_SOMEHTTPMETHOD".  This is a total designers choice free for all.  

Groups of Users can be created at the Account level and each Group can be given Roles and Permissions 
that the Users inherit.  In this way, you could create a functional "Admin" Group (different from Administrator Role) 
and give that Group all of the Permissions desired.  Users assigned to this "Admin" group would then inherit all those assigned Roles and Permissions. 


### Authentication

As far as the framework is concerned, authentication involves populating a User object with their entitled
Roles and Permissions and placing the User object on the Request.  Session management, barer tokens, etc. are left up 
to the handler implementation.  Authentication does not consider Roles and Permissions: it only validates the username/password. 

AuthHandler is the default authentication provider.  It currently supports HTTP basic auth along with username/password 
query string params and session tokens.  Additionally, if an application chooses to provide a JWT signed with a User's
secretKey, the roles and permissions defined in the JWT will become the roles and permissions assigned to the 
User for that request.  Any roles and permissions defined in the DB will not be used.  
 
If you want to secure your Api, you have to configure an instance of the AuthHandler (or create your own custom authentication handler)
and map it to the desired Endpoints through an Action. 
 
Failing Authentication should return a 401 Unauthorized HTTP response code. (There is a longstanding gripe with HTTP status
codes that 401 should be called something other than 'Unauthorized' because generally elsewhere in software development
authorization (see below) is the process of determining if a users can access requested resources, NOT validating a users credentials.)
 

### Authorization

Authorization is managed by the AclHandler. If you want to use Role and Permission based authorization,
you must configure an instance of the AclHandler (or create your own implementation) and associate it to
the desired Endpoints through an Action.

The AclHandler matches configured AclRules objects against the Url path and HTTP method of the request.

AclHandler processes AclRules in sorted order and the first rule to allow access "wins".  If no rule allows access,
then a 403 Forbidden HTTP status code is returned.



### Multi-Tenant APIs

Apis can be flagged as 'multiTenant'.  If so, a tenantCode must immediately follow the apiCode in the URL.

Ex: ```http://localhost/accountCode/apiCode/tenantCode/[endpoint.path]/collectionKey/[resourceKey]```

If the AuthAction is being used, it will enforce that the Url tenantCode matches the logged in users tenantCode (if there is a logged in user).

IMPORTANT: this Url match restriction alone will not prevent access to cross tenant data. To fully
restrict and require the tenantId and tenantCode query string parameters and JSON body properties
with the following configuration:

```properties

tenantAcl.class=io.rcktapp.api.AclRule
tenantAcl.info=true
tenantAcl.includePaths=*
#tenantAcl.excludePaths=
tenantAcl.restricts=*.tenantId,*.tenantCode
tenantAcl.requires=*.tenantId
tenantAcl.methods=GET,PUT,POST,DELETE

```

Including this configuration will ensure that tenantId is always considered in queries (if it exists on the target Table) and can not be supplied by the caller, it will be pulled from the logged in user.


### Row Level Security

The simplest way to restrict a user's interaction with a row is to provide a "userId" column on the
table in question.  Then use an AclRule to "require/restrict" userId.  This way a user can only
read and write the rows they 'own'.  You could utilize a different combination of AclRules to achieve 
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

Elastic pagination: paging beyond the 10,000th item is significantly slower than paging within the range of 0-10,000. Once the 10k index has been reached, elastic must manually cycle through the data in order to obtain the desired page.  10,000 is the default max_result value within ElasticSearch.


#### Elasticsearch RQL Examples
Retrieve all location data for the locations in the city of Chandler
`http://localhost:8080/apiCode/elastic/location?eq(city,Chandler)`

Retrieve only the address properties for the locations in the city of Chandler
`http://localhost:8080/apiCode/elastic/location?eq(city,Chandler)?source=address1,address2,address3`

Retrieve the locations in the city of Chandler AND have a locationCode of 270*** **AND** have an address including *McQueen*
`http://localhost:8080/apiCode/elastic/location?and(and(eq(locationCode,270*),eq(city,Chandler)),and(eq(address1,*McQueen*)))`

Retrieve all locations with players.registerNum > 5 
`http://localhost:8080/apiCode/elastic/location?gt(players.registerNum,5)`


Retrieve the locations with an address1 that includes 'VALLEY' AND PHOENIX locations that have deleted players 
`http://localhost:8080/apiCode/elastic/location?and(and(eq(players.deleted,true),eq(city,PHOENIX)),and(eq(address1,*VALLEY*)))`

Retrieve auto-suggested cities that start with 'chan' 
`http://localhost:8080/apiCode/elastic/location/suggest?suggestCity=chan`

Retrieve locations with an empty state value
`http://localhost:8080/apiCode/elastic/location?emp(state)`

Example Config
```
elasticdb.class=io.rcktapp.api.handler.elastic.ElasticDb
elasticdb.url=https://yourElasticSearchDB.amazonaws.com

elasticH.class=io.rcktapp.api.handler.elastic.ElasticDbRestHandler
elasticEp.class=io.rcktapp.api.Endpoint
elasticEp.path=desiredPath
elasticEp.methods=GET
elasticEp.handler=elasticH
```

[See io.rcktapp.rql.RqlToElasticSearchTest for several examples of RQL to Elastic queries](https://github.com/RocketPartners/rckt_inversion/blob/wb/readme_updates/src/test/java/io/rcktapp/rql/RqlToElasticSearchTest.java)


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
* sorting queries
	* Scans cannot be sorted, only queries can.
	* When sorting a query, the field that is sorted MUST match the sort key of the query index

Example Config
```
dynamoH.class=io.rcktapp.api.service.ext.DynamoDbHandler

dynamoEp.class=io.rcktapp.api.Endpoint
dynamoEp.includePaths=dynamo*
dynamoEp.methods=GET,POST,DELETE
dynamoEp.handler=dynamoH
dynamoEp.config=tableMap=promo|promo-dev,loyalty-punchcard|loyalty-punchcard-dev&conditionalWriteConf=promo|attribute_not_exists(primarykey) OR enddate <= :enddate|enddate&appendTenantIdToPk=loyalty-punchcard
```


## Developer Notes

### Javadocs

For all Handler configuration options or to understand how to use Inversion as a framework for a custom application
check out the Javadocs.

 * 0.3.x - https://rocketpartners.github.io/rckt_inversion/0.3.x/javadoc/


### Logging

 * Inversion uses logback, but it is not configured out of the box - the service implementing Inversion will be responsible for providing their own logback.xml config file!
```
dependencies {
    ...
    compile 'net.rakugakibox.spring.boot:logback-access-spring-boot-starter:2.6.0'
}
```


### Gradle, Maven, etc.

#### Publishing Releases
1. Checkout the master branch
1. Update the version property in gradle.properties
1. Commit your changes
1. Tag the repo:
  * git tag -a 1.4 -m "version 1.4"
  * git push --tags
1. Merge your code into the current release branch to have Travis CI publish the built assets to Bintray


#### Maven Repo for Releases

```gradle
repositories {
	maven { url 'https://dl.bintray.com/inversion/repo' }
}

dependencies {
    compile "io.inversion:inversion-spring-boot:${inversionVersion}"
}
```


#### Maven Repo for SNAPSHOTS

If you want to extend Inversion as part of a custom application, you can use jitpack to pull your preferred branch directly from GitHub into your project.   

```gradle
repositories { 
   maven { url 'https://jitpack.io' }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor 0, 'seconds'
}

dependencies {
    compile 'com.github.inversion-api.inversion-engine:inversion-spring-boot:master-SNAPSHOT'
} 
```   


## REST API Design Resources

### HTTP Status Codes
 * http://msdn.microsoft.com/en-us/library/azure/dd179357.aspx
 * http://www.restapitutorial.com/httpstatuscodes.html
 * http://www.restapitutorial.com/lessons/httpmethods.html 
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections


### Standards-ish

 * http://stackoverflow.com/questions/12806386/standard-json-api-response-format
 * https://jsonapi.org/
 * https://labs.omniti.com/labs/jsend
 * http://stateless.co/hal_specification.html
 * http://www.odata.org/
 * https://en.wikipedia.org/wiki/HATEOAS
 * https://github.com/swagger-api/swagger-core/wiki
 * http://json-schema.org/
 * https://google.github.io/styleguide/jsoncstyleguide.xml
 * https://developers.facebook.com/docs/graph-api/
 
 
### Web Query Languages
 
 * http://dundalek.com/rql/
 * https://doc.apsstandard.org/2.1/spec/rql/
 * https://www.sitepen.com/blog/2010/11/02/resource-query-language-a-query-language-for-the-web-nosql/
 * http://msdn.microsoft.com/en-us/library/gg309461.aspx - odata endpoint filters
 * http://tools.ietf.org/html/draft-nottingham-atompub-fiql-00
 * http://stackoverflow.com/questions/16371610/rest-rql-java-implementation
 * https://graphql.org/
  
 
### Best Practices and Design Resources

 * https://stormpath.com/blog/linking-and-resource-expansion-rest-api-tips/
 * http://docs.stormpath.com/rest/product-guide/#link-expansion
 * http://www.vinaysahni.com/best-practices-for-a-pragmatic-restful-api
 * http://blog.mwaysolutions.com/2014/06/05/10-best-practices-for-better-restful-api/
 * http://samplacette.com/five-json-rest-api-link-formats-compared/
 * https://www.mnot.net/blog/2011/11/25/linking_in_json
 * https://blog.safaribooksonline.com/2013/05/23/instrumenting-apis-with-links-in-rest/
 * http://stackoverflow.com/questions/297005/what-is-the-gold-standard-for-website-apis-twitter-flickr-facebook-etc
 * https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/#
 * https://github.com/NationalBankBelgium/REST-API-Design-Guide/wiki
 * https://cloud.google.com/bigquery/docs/reference/legacy-sql
 * https://github.com/Azure/azure-rest-api-specs
 
### REST APIs in the Wild

* Stripe - https://stripe.com/docs/api
* Wordpress - https://developer.wordpress.org/rest-api/
* Google - https://developers.google.com/
* Facebook - https://developers.facebook.com/docs
* LinkedIn - https://developer.linkedin.com/docs
* Amazon S3 - https://docs.aws.amazon.com/AmazonS3/latest/API/Welcome.html
* Twitter - https://developer.twitter.com/
* Spotify - https://developer.spotify.com/documentation/web-api/

  

             
             
             


