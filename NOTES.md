

ChainLocal.makeLink does not know how to work with endpointPath/collectionPath

dynamo
 - add dynamo 'includes' projections
 - primary key / href linking
 - continuation token
 - subcollection linking 
 - put excludes and expands into get handler superclass
 - implement name map to avoid expression keyword conflicts.

make all "with" methods reuse existing items with the name of supplied thing see table.withColumn(name, type)


DONE Implement db.startup()/shutdown();
DONE get rid of request.params in favor of url.params

Put meta & data objects into response as first class citizens

Make superclass "beautify" method for collection names and attribute names

Pull "expands" out of SqlGetHandler and put it into a superclass that can work with Dynamo/Elastic etc.


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