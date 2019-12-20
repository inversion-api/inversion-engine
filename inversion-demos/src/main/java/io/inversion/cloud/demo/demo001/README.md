
##README - Demo001SqlDbNorthwind.java

This demo launches an API that exposes SQL database tables as REST collection endpoints.  
The demo supports full GET,PUT,POST,DELETE operations with an extensive Resource Query Language
(RQL) for GET requests

After running Demo001SqlDemoNorthwind.java, your API will be running at 
'http://localhost:8080/northwind' with REST collection endpoints for db entity

You can get started by exploring some of these urls:
  - GET http://localhost:8080/northwind/products
  - GET http://localhost:8080/northwind/orders?expands=orderDetails&page=2
  - GET http://localhost:8080/northwind/customers?in(country,France,Spain)&sort=-customerid&pageSize=10
  - GET http://localhost:8080/northwind/customers?orders.shipCity=Mannheim

Append '&explain=true' to any query string to see an explanation of what is happening under the covers
  - GET http://localhost:8080/northwind/employees?title='Sales Representative'&sort=employeeid&pageSize=2&page=2&explain=true


The demo connects to an in memory H2 sql db that gets initialized from scratch each time this 
demo is run.  That means you can fully explore modifying operations (PUT,POST,DELETE) and 'break'
whatever you want then restart and have a clean demo app again

If you want to explore your own JDBC DB, you can swap the "withDb()" line with the commented
out one and fill in your connection info

Northwind is a demo db that has shipped with various Microsoft products for years. Some of 
its table designs seem strange or antiquated  compared to modern conventions but it makes a great 
demo and test specifically because it shows how Inversion can accommodate a broad range of 
database design patterns.  

@see Demo1SqlDbNorthwind.ddl for more details on the db
@see https://github.com/inversion-api/inversion-engine for more information on building awesome APIs with Inversion

