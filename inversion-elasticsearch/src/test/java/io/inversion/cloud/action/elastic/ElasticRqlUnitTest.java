package io.inversion.cloud.action.elastic;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.cloud.model.Db;
import io.inversion.cloud.rql.AbstractRqlTest;
import io.inversion.cloud.rql.RqlValidationSuite;

@TestInstance(Lifecycle.PER_CLASS)
public class ElasticRqlUnitTest extends AbstractRqlTest
{

   public ElasticRqlUnitTest()
   {
      super(ElasticsearchQuery.class.getName(), "elasticsearch");
   }

   @Override
   public void initializeDb()
   {
      Db db = getDb();
      if (db == null)
      {
         db = new ElasticsearchDb("elasticsearch");
         setDb(db);
      }
   }

   @Override
   protected void customizeUnitTestSuite(RqlValidationSuite suite)
   {

      suite//
           .withResult("eq", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"filter\" : [ { \"term\" : { \"orderID\" : { \"value\" : \"10248\", \"boost\" : 1.0 } } }, { \"term\" : { \"shipCountry\" : { \"value\" : \"France\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("ne", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"term\" : { \"shipCountry\" : { \"value\" : \"France\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("n", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("nn", "index: orders, QueryBuilder={ \"query\" : { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("emp", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"should\" : [ { \"term\" : { \"shipRegion\" : { \"value\" : \"\", \"boost\" : 1.0 } } }, { \"bool\" : { \"must_not\" : [ { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("nemp",
                 "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must\" : [ { \"bool\" : { \"must_not\" : [ { \"term\" : { \"shipRegion\" : { \"value\" : \"\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, { \"bool\" : { \"must\" : [ { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("sw", "index: orders, QueryBuilder={ \"query\" : { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"Franc*\", \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("ew", "index: orders, QueryBuilder={ \"query\" : { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"*nce\", \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("w", "index: orders, QueryBuilder={ \"query\" : { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"*ance*\", \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("wo", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"*ance*\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("lt", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : null, \"to\" : \"10\", \"include_lower\" : true, \"include_upper\" : false, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("le", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : null, \"to\" : \"10\", \"include_lower\" : true, \"include_upper\" : true, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("gt", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : \"3.67\", \"to\" : null, \"include_lower\" : false, \"include_upper\" : true, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("ge", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : \"3.67\", \"to\" : null, \"include_lower\" : true, \"include_upper\" : true, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("in", "index: orders, QueryBuilder={ \"query\" : { \"terms\" : { \"shipCity\" : [ \"Reims\", \"Charleroi\" ], \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("out", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"terms\" : { \"shipCity\" : [ \"Reims\", \"Charleroi\" ], \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("and", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"filter\" : [ { \"term\" : { \"shipCity\" : { \"value\" : \"Lyon\", \"boost\" : 1.0 } } }, { \"term\" : { \"shipCountry\" : { \"value\" : \"France\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("or", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"should\" : [ { \"term\" : { \"shipCity\" : { \"value\" : \"Reims\", \"boost\" : 1.0 } } }, { \"term\" : { \"shipCity\" : { \"value\" : \"Charleroi\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("not", "UNSUPPORTED")//
           .withResult("as", "UNSUPPORTED")//
           .withResult("includes", "UNSUPPORTED")//
           .withResult("distinct", "UNSUPPORTED")//
           .withResult("count1", "UNSUPPORTED")//
           .withResult("count2", "UNSUPPORTED")//
           .withResult("count3", "UNSUPPORTED")//
           .withResult("countAs", "UNSUPPORTED")//
           .withResult("sum", "UNSUPPORTED")//
           .withResult("sumAs", "UNSUPPORTED")//
           .withResult("sumIf", "UNSUPPORTED")//
           .withResult("min", "UNSUPPORTED")//
           .withResult("max", "UNSUPPORTED")//
           .withResult("groupCount", "UNSUPPORTED")//
           .withResult("offset", "UNSUPPORTED")//
           .withResult("limit", "UNSUPPORTED")//
           .withResult("page", "index: orders, QueryBuilder={ \"from\" : 3, \"size\" : 7, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("pageNum", "index: orders, QueryBuilder={ \"from\" : 3, \"size\" : 7, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("after", "UNSUPPORTED")//
           .withResult("sort", "index: orders, QueryBuilder={ \"sort\" : [ { \"shipCountry\" : { \"order\" : \"desc\" } }, { \"shipCity\" : { \"order\" : \"asc\" } }, { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("order", "index: orders, QueryBuilder={ \"sort\" : [ { \"shipCountry\" : { \"order\" : \"asc\" } }, { \"shipCity\" : { \"order\" : \"desc\" } }, { \"id\" : { \"order\" : \"asc\" } } ] }")//
           .withResult("likeMiddle", "UNSUPPORTED")//
           .withResult("likeStartsWith", "UNSUPPORTED")//
           .withResult("likeEndsWith", "UNSUPPORTED")//
           .withResult("onToManyExistsEq", "UNSUPPORTED")//
           .withResult("onToManyNotExistsNe", "UNSUPPORTED")//
           .withResult("manyToOneExistsEq", "UNSUPPORTED")//
           .withResult("manyToOneNotExistsNe", "UNSUPPORTED")//
           .withResult("manyTManyNotExistsNe", "UNSUPPORTED")//

      ;
   }

}
