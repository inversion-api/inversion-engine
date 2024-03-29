
# IMPORTANT
Restore 
- Order.getSorts()
- SqlQuery.getDefaultSorts()
- SqlQuery.printInitialSelect()
- SqlQuery.printTermsSelect()
- SqlQuery.printTableAlias()
- DbPostAction
  - swapRefsWithActualReferences
  - swapLogicalDuplicateReferences
  - collapse
- Update json codecPath to correct for querying arrays with text keys, algo working but not optimized for map vs list



* Set server variables on chain like endpoint and action
* Add support for Server (including adding all servers to a single api) in Wirer
* Add param support to Server & check on request matching
* Add server params to openapi
* Check that all servers have the same variables
* Add global route connection restriction and retry/backoff to RestClient
* Add default 404 handling to springboot
* Add url scrubbing to Engine




* Engine 
  * Operation Construction
    * Collection should not be a rule.
    * Filter out invalid ops...what does this mean at runtime
    * Test case for Operation naming
    * Add regexs back to Operation Parameters
    * Make link table collections mark an op as "private", same with "internal" endpoint...use same word
    * Add param matching back to Operation.matches()
    * Test case and better solution for unsupported ops see Engine.buildOperationName
  * Request to op matching
    * Double check engine,api,endpoint,action matching for the selected op
    * Data structure for faster op lookup on each request
    * Don't call internal/private ops if Chain depth is 1
    

* Rows / JSNode
  * Get rid of Rows, it is inefficient since everything is transformed to json

* Url
  * Reject special characters in codecPath

* Linker
  * Make linker support op param regexes
  * If op codecPath at an param index is not a variable, confirm its value matches before using...if no match value supplied, can use the linker but is not preferred to other matches
  * Make test cases with crazy 
  * Need to cache results for speed    


    
* Collection.encode all json names (in Db init)
* Make all objects reject a "name" field with special characters in it (ones that would be Collection.encodedStr)


* Path
 * Add constructor to take in and join multiple paths not just strings  
    

Wirer
 * Add support for enumerations
 * Add utility class/interface for encoding/decoding/bootstrapping different object types instead of hard coding in Wirer
 * DONE Replicate "reporting" name error
    * DONE Make deterministic
    * DONE Use collection.name not tablename for collections
    * DONE Don't ever set something that was not passed in via config
    * DONE Filter out config properties that don't have a "." in them      
  * DONE Refactor into "config" package and split out Encoder/Decoder/Namer
  * DONE Get rid of extra "Decoder.propKeys" map, try to use a single SortedTreeMap  
  * DONE Change Wirer to use "_" instead of "." when not intending to have implicit codecPath props set...or maybe use a prefix like '$' to indicate this was already hand wired not from config...or really just only do it for configged settings
  * DONE Error on conflicting name
  * DONE Make masked fields regexes like Db.illegals for sql injection



* New tests
  * Ambiguous paths
  * Non standard /relationship/resource/collection type of setup w/ Collections from diffrent DBs on different endpoints
  * Reencoding Collection.encodeStr should not alter string
    

* General
  * Try to isolate all Url encoding/decoding escaping to URL...the string resourceKey may need to become a list not a string (or proper ResourceKey object)
  * Return Collections.unmodifiableList/Map on all collection getters  
    
* Validations
  * Make validation in/matches/out etc "operators" and "filters" that can be configured.
    * Should this be expressed in RQL?  
  * Allow and/or/not on validations
  