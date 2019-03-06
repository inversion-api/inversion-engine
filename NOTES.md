
# TODO

* TODO: fix elastic search
* TODO: fix s3
* DONE: fix firehose
* DONE: fix redis

* TODO: link to another table with a compound key ...orderdetails was it?
* TODO: add back support for non collection selects
* TODO: make iterator or lambda stream to loop over all results including pagination
* DONE: put excludes and expands into restgetaction
* TODO: add put handler collapse support back
* TODO: make all "with" methods reuse existing items with the name of supplied thing see table.withColumn(name, type)
* TODO: create cross collection foreign key test case
* TODO: Look into cloudwatch support
* TODO: RestGetAction url term replacement needs to be case insensative
* DONE: Add Utils.findSysEnvProp to Sql/S3
* TODO: Consider changing Term.token to an Object so that the actions can do the casting before passing to the DB and the Db/Query don't have to cast
* TODO: fixup service.forward

test comman separtaed entitykeys and comman separated compound entitykeys

## Dynamo
 * TODO: subcollection linking
 * TODO: add dynamo 'includes' projections
 * TODO: parameter to support strongly consistent reads
 * TODO: add back conditional write support
 * TODO: need to test against a table that does not have a primary sort key
 * TODO: add support for compounding keys automatically w/ configuration
   * TODO: add support for automatically appending tenantcode to every primary/gsi hask key
   * TODO: auto append table space name to different columns to create table namespace???? or make it column have that value alone
 * TODO: key values in a global secondary do not need to be unique. create a test case for that
   * TODO: https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/GSI.html
 * DONE: can a gsi share an attribute with a lsi? if so...need to test that  
 * DONE continuation token
 * DONE implement name map to avoid expression keyword conflicts.
 * DONE primary key / href linking
 


DONE - ChainLocal.makeLink does not know how to work with endpointPath/collectionPath
DONE Implement db.startup()/shutdown();
DONE get rid of request.params in favor of url.params
DONE Put meta & data objects into response as first class citizens
DONE Make superclass "beautify" method for collection names and attribute names



# Misc Notes

SQL functions supported by big query
https://cloud.google.com/bigquery/docs/reference/legacy-sql

GitHub design system: https://styleguide.github.com/primer/utilities/layout/
morning start design system???



https://github.com/angular-ui/ui-router/wiki/URL-Routing
https://swagger.io/docs/specification/describing-parameters/



Here are a couple articles advocating using GitHub for API docs:
https://swagger.io/blog/api-development/generator-openapi-repo/
https://apievangelist.com/2017/05/25/every-api-should-begin-with-a-github-repository/
Microsoft's usage of GitHub for Azure docs is great:
https://github.com/Azure/azure-rest-api-specs

Using GitHub gives your project some visibility and, if you want it, gives users the ability to interact with you/the api by filing issue reports etc.  


Here are numerous examples of great API docs from "big boys" that are awesome...but major custom efforts:
https://developer.twitter.com/en/docs.html
https://developer.github.com/v3/
https://api.github.com/users/octocat/orgs
https://api.twitter.com/1.1/tweets/
https://developers.facebook.com/
https://developers.facebook.com/v3.0/me?fields=id,name
https://developer.wordpress.org/rest-api/reference/
https://developers.google.com/apis-explorer/#p/
https://www.googleapis.com/drive/v3
https://stripe.com/docs/api
https://api.stripe.com/v1/charges
https://www.census.gov/data/developers/data-sets/acs-1year.html
https://api.census.gov/data/2017/acs/acs1?get=NAME,group(B01001)&for=us:1
https://developer.spotify.com/documentation/web-api/
https://api.spotify.com/v1/search