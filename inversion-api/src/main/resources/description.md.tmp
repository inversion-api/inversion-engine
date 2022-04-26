## Pagination


| Parameter     | Description                                                                                  
| ------------- | -------------------------------------------------------------------------------------------- 
| page=N        | Translates into an offset clause using the 'size' param (or 100 as the default) as the multiplier.
| size=N        | The number of results to return.                                                             

## Sorting

Results can be sorted by one or more params each in ascending or descending order.
If no 'sort' is included, results will be sorted by the underlying data source's primary index.

| Parameter                          | Description                                                                         
| ---------------------------------- | ----------------------------------------------------------------------------------- 
| sort=[-]property[,[-]property...]  | sort=city,state,-accountBalance 


## Querying

This API supports Resource Query Language (RQL) which allows you to retrieve the specific data you are looking for. 

Endpoints that support the 'q' query param can be queried via RQL.  For example **/users?q=eq(firstName, John)** would
find all user named name 'John'.

Multiple query conditions can be passed as multiple 'q' params or as a comma separated list to a single 'q' param.
For example **/user?q=eq(firstName, John),or(gt(age, 20), le(age,75))** would find all users named 'John' between the ages of 
21 and 75.

As seen above, instead of writing "traditional" query string params in "?param1=value1&param2=value2" format, all query params
are expressed in a "functional" notation simplified as **function(property OR value OR another function,...)**.

Here are some additional examples:
* q=eq(color, red)
* q=eq(make, 'Ford'),eq(model, 'F-150'),in(color, 'Red', 'White', 'Black')
* q=or(and(gt(miles, 100000), lt(price, 7500)),and(gt(miles, 50000), lt(price, 15000)))

### RQL BNF Grammar

Hopefully the RQL examples above and extensive examples throughout this document are enough to get you comfortable 
writing power queries.  For those that would like a more detailed definition of the syntax, below is a simplified
BNF grammar for the construction of an RQL 'q' query param value.

   
    <term>         ::=  <token> | <function>(<list>)
    <list>         ::=  <term>  | <term>,<list>
    <token>        ::=  <property> | <value> | <singleTicked> | <doubleQuoted>
    <function>     ::=  eq | gt | lt ...etc.  See below for the full list.
    <property>     ::=  The name of a property defined on your collection.  
                        This value should not be quoted in single ticks or double quotes.   
    <value>        ::=  Any string without single ticks('), double quotes(") or commas(,). 
                        All backslashes(\) must be escaped by a backslash.
    <singleTicked> ::=  A single tick(') quoted string. 
                        Any singe ticks or backslashes(\) in the value are escaped by a backslash. 
    <doubleQuoted> ::=  A double quote(") quoted string.
                        Any double quotes(") or backslashes(\) in the value are escaped by a backslash.

### Single Ticks, Double Quotes, Commas, and Backslashes

RQL uses the backslash (\) character to escape other characters. If you pass a value to an RQL function and the value
contains backslashes, you need to escape the backslash by preceding it with another backslash.

Values passed to a function, such as 'F-150' above, may also be optionally quoted in either single ticks(') or double quotes(").
You only need to quote a value if it might itself contain single ticks, double quotes, or commas.  If you quote with
a single tick, you need to escape any single tick occurrences in the value with a backslash, but you do not have to escape any double quotes.
The same goes for double quoted strings, where you escape double quotes in the value, but you do not have to escape single quotes.  
Just as in an unquoted value, you must escape backslash characters in a quoted value. 

### Wildcards Matching

You can embed an asterisk (*) in a value to indicate a wildcard search when using the eq(), aka 'Equals', function.

Using an astrisk is equivalent to transparently using the LIKE operator, instead of an '=' based equality check, in an SQL database and
using percent (%) as the wildcard character. The percent character is a special character in query strings, so we use the asterisk instead.  

There are numerous examples of asterisk wildcard usage below in the 'Filter Functions' section below.


## Filter Functions

| Function           |  Name        | Examples                                                                                          
| ------------------ | ------------ | --------------------------------------------------------------------------------------------- 
| eq(term,term)      | Equals       | eq(property, value)<br />eq(property, value with spaces)<br />eq(property, 'single, t"ick\'s')<br />eq(property, "dou,bl'e qu\"otes")<br />eq(property, v*lue)<br />eq(property, \*alue)<br />eq(property, valu\*)<br />eq(property, "va, '\"l\*e")        
| gt(term,term)      | Greater Than | gt(price, 200)       
| lt(term,term)      | Less Than    | lt(price, 2.99)
| ge(term,term)      | Greater Than or Equal To | ge(age, 21)                                                                             
| le(term,term)      | Less Than or Equal To    | le(year, 2020)                                                                                
| ne(term,term)      | Not Equal To | ne(status, closed)                                                                                            
| w(term,term)       | With         | w(songTitle, love) - same as: eq(songTitle, \*love\*)                              
| sw(term,term)      | Starts With  | sw(songTitle, love) - same as: eq(songTitle, love\*)                   
| ew(term,term)      | Ends With    | ew(songTitle, love) - same as: eq(songTitle, \*love)
| in(term,term...)   | In a Set     | in(orderNumber, 1234, 5678, 9101112)                                                     
| out(term,term...)  | Out of a Set | out(color, brown, red, green)                                                 
| emp(term)          | Empty        | Checks that a value is either NULL or the empty string.  <br>emp(phoneNumber)               
| nemp(term)         | Not Empty    | nemp(closingDate)
| n(term)            | Is NULL      | Checks that a value is equal to NULL.<br>n(endDate)                                  
| nn(term)           | Not NULL     | Checks that a value is NOT equal to NULL.<br>nn(contractPrice)                           
| and(term,term...)  | And          | and(eq(city,Atlanta),eq(zipCode,30030))                      
| or(term,term...)   | Or           | or(eq(city,Atlanta),in(zipCode,30601,30603,30612))                              
| not(term)          | Not          | not(in(zipCode,30601,30603,30612))<br>not(contractClosed)<br>not(eq(color, red))




## Aggregation Functions

In addition to filtering functions, RQL includes a number of aggregation functions that are useful for summarizing
and reporting on data.  Aggregation functions that accept a 'term' argument can take a property name,
a value, or another aggregate or filter function. 

NOTE: Use of aggregate functions can change the 'shape' of the response JSON document beyond what is definable via
OpenApi 3.0 to the point where it may not be compatible with all client SDKs generated off of the OpenApi definition
of this API.  If you are using a simple HTTP client, such as in JavaScript in a web browser, curl, Postman etc. you will not have an issue.
You can also test response for all endpoints and param/function combinations in this console. 


| Function                              |  Name        | Examples
| ------------------------------------- | ------------ | --------------------------------------------------------------------------------------------- 
| group(property...)                    | Group        | Adds properties to a GROUP BY clause.<br /> group(firstName, zipCode)                                           
| count(term, [as(name)])               | Count        | Counts the given term and optionally names the resulting JSON property. <br />count(closingDate, 'Closed Contracts')
| sum(term, [as(name)])                 | Sum          | Sums the given term and optionally names the resulting JSON property as name. <br />sum(pay, totalPay)<br>sum(if(and(eq(type,'car'),eq(color, 'red'))), 1, 0), 'Number of Red Cars')   
| min(term, [as(name)])                 | Min          | Finds the minimum value of the property (per the grouping) and optionally names the resulting JSON property. <br />q=min(temperature, 'Coldest Day of 2020'),eq(year, 2020)    
| max(term, [as(name)])                 | Max          | Finds the maximum value of the property (per the grouping) and optionally names the resulting JSON property  <br />max(  
| countascol(property, value...)        | Count As Col | Roughly translates to "sum(if(eq(property, value), 1, 0)) as 'value'". <br />countascol(color, 'Red', 'Green', 'Blue')     
| distinct([property...])               | Distinct     | Filters out duplicates based on the given properties.<br />include=firstName,lastName&q=eq(state, 'CA'),distinct                         
| if(term, termWhenTrue, termWhenFalse) | If           | sum(if(lt(price,0),0,price)) 
| as([term, name]...)                   | As           | Changes the name of the term result in the return JSON, works just like SQL 'as' operator. |


## Property Inclusion / Exclusion

Often you may want to retrieve a subset of properties from a query.  As a made up example, when querying a 'user' collection you 
may only want to retrieve the user's userId, firstName, and lastName out of hundreds of user properties.

In this scenario, you can use the 'include' query param to list the specific properties that should be returned.
Conversely, you can use the 'exclude' param identify specific properties you don't want returned.
If a property is listed in both an 'include' and 'exclude' param, it will be excluded.

If you use the 'expand' param (see below) to 'pre fetch' related resource into a single request, you can use 
dot notation to specify nested JSON properties.'

You use the 'include' and 'exclude' param as a standard query param OR as an RQL function.


| Parameter                        | Description                                                                                  
| -------------------------------- | -------------------------------------------------------------------------------------------- 
| include=property[,property...]  | Limits the result to the properties listed.<br />include=userId,firstName,lastName,address.zipCode&expand=address 
| include(property[,property...]) | Same as above in RQL syntax to be included in the 'q' param.
| exclude=property[,property...]  | Specifically removes listed properties from the result.
| exclude(property[,property...]) | Same as above in RQL syntax to be included in the 'q' param.


## Including Related Resource

This spec defines relationships between the resource of different collections.  In a made up example, a 'book' resource
may be defined as having a relationship to an 'author' resource.  In addition to the OpenApi 'links' definitions, 
these relationships are also identified in the HAL format '_links' section of each response. To minimize the number of
requests round trips, you may want to use the 'expand' param to fetch a resource and one or more of its related
resources in the same request.  The resulting document will hold the JSON of the related resource in a property with the 
resource name.  The HAL '_links' to the related resource will still be included in the response.

In addition to minimizing GET requests, you may also modify child related resources, including adding new child resources,
and PUT the entire document back to the root documents enpdoint.  The child documents will be POSTed or PUT automatically
before the root document's PUT is processed.  This makes it MUCH easier to coordinate saving complex expanded documents.

You can use 'dot notation' to expand to any desired relationship depth. For example, in a factitious genealogy api, 
/people?expand=father.mother.brothers would return a result including with each user's father JSON nested under their 
'father' property, the father's mother nested under the father's 'mother' property, and the mother's brothers nested
under the mother's 'brothers' property. 

You use the 'expand' param as a standard query param OR as an RQL function.

NOTE: When using the 'expand' param for one-to-many or many-to-many relationships, where there could be more than
one result, the expanded result set is limited to the first 100 results sorted by the primary key.  If a relationship
could involve more than 100 child documents, it is best to fetch and paginate through that relationship directly
instead of using 'expand'.

NOTE: When using 'expand' it is possible that the same resource will be referenced multiple times in a single response.
For example, if you were querying a "books" endpoint with expand=author, if the same author wrote more than one book that 
author could logically appear more than once.  It is also possible to expand recursive relationships such as
/books?expand=author.books.  To minimize the payload size, if a resource is referenced more than once, all subsequent
occurrence of the resource will be sent as a JSON Pointer '$ref'.     

NOTE: Use of 'expand' can change the 'shape' of the response JSON document beyond what is definable via
OpenApi 3.0 to the point where it may not be compatible with all client SDKs generated off of the OpenApi definition
of this API.  If you are using a simple HTTP client, such as in JavaScript in a web browser, curl, Postman etc. you will not have an issue.
You can also test response for all endpoints and param/function combinations in this console. 

| Parameter                               | Description
| --------------------------------------- | ------------------------------------------------------------------ 
| expand=relationship[,relationship...]  | /books?expand=author,category,weekly.sales
| expand(relationship[,relationship...]) | Same as above in RQL syntax to be included in the 'q' param. 
