# inversion-demos

## Running a simple JDBC backed API

```
gradle JdbcApiMain -Ddb.driver=com.mysql.cj.jdbc.Driver -Ddb.url=jdbc:mysql://localhost:3306/inventory?serverTimezone=UTC -Ddb.user=root -Ddb.pass=password
```

## Pulling Configuration from an Azure KeyVault

Documentation and examples for pulling Inversion configuration from an Azure KeyVault

NOTE: This project is designed to run against a local project copy of inversion.  Make sure you checkout the inversion-engine project to the same parent directory as this project before building.

An Inversion Engine is reflectively configured by name value pairs that are found in properties files, environment variables and system properties.  

Developers can customize the configuration param lookup routine to add additional data sources such as pulling name value pairs from an Azure KeyVault to enable runtime configuration of secure secrets.

The Inversion Config object is a static convenience wrapper around a singleton Commons Configuration CompositeConfiguration object.  

To augment the config lookup, all you have to do is add your own Configuration to CompositeConfiguration prior to calling Engine.startup().


  