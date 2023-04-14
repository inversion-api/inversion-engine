Purpose of this Context and bean wiring

ENCODING:
1. inspect any beans that are passed in for encoding to determine the bean names that are eligible for properties to be set during decoding
2. FOR FULL CONFIG/HOSTED ONLY - allow an entire model to be serialized so the service can start up faster without having to introspect the db.

DECODING:
1. apply any configured properties to beans discovered during encoding
2. FOR FULL CONFIG/HOSTED ONLY - 
   1. Basic feature - load an entire model from manually constructed config files, still depending on db introspection
   2. Advanced feature - bypass introspection by loading a file that written out by the encoder after db introspection


bean.stringStringList = [asdf, asdf]


bean.stringStringList = bean_stringStringList_list
bean_stringStringList_list = [asdf, asdf]


