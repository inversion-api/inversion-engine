## Pagination

| Parameter     | Description                                                                                  |
 | ------------ | -------------------------------------------------------------------------------------------- |
| page=N        | Translates into an offset clause using pagesize (or the default page size) as the multiplier. |
| pagenum=N     | An overloaded synonym for "page", the two are equivelant.                                    |
| pagesize=N    | The number of results to return.                                                              |
| offset=N      | Directly translates into a sql offset clause, overrides any page/pagenum params supplied.     |
| limit=N       | Directly translates into a SQL limit clause, overrides any pagesize params supplied.          |

## Sorting and Ordering

| Parameter                   | Description                                                                         |
 | -------------------------- | ----------------------------------------------------------------------------------- |
| sort=col1,+col2,-col3,colN  | Use of the + operator is the implied default.  Prefixing with "-" sorts descending. |
| order                       | An overloaded synonym for "sort", the two are equivalent.                           |



## Querying

This API supports Resource Query Language (RQL) which allows you to retrieve the specific data you are looking for. 

Endpoints that support the 'q' query parameter can be queried via RQL.  For example '/users?eq(firstName, John)' would
find all user resources with the first name 'John'.

Multiple query conditions can be passed as multiple 'q' parameters or as a comma separated list to a single 'q' parameter.
For example '/user?eq(firstName, John),gt(age,35)' would find all 'Johns' over 35 years old.



### General

* Many functions can be written in one of several equivelant forms:
* name=value - the traditional query string format
* function(column, value OR expression) - eq(col,value), lt(column,value), and(eq(col,value), lt(column,value)), in(col, val1, val2, val3, val4)
* name=eq=value, column=lt=value, col=in=val1,val2,val3
* Quotes & Escaping Quotes - ' or " can be used to quote values.  Use a \ to escape any inner occurrences of the outer quote.  If you quote with single quotes you don't have to escape inner double quotes and vice versa
* Wildcards - the '*' character is treated as the universal wildcard for all supported back ends.  
  For example, for SQL back ends '*' would be substituted with "%" and instead of using '=' operator the system would substitute 'LIKE'.  You use the normal '=' or 'eq' operator but the system uses LIKE and % under the covers.

  

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

