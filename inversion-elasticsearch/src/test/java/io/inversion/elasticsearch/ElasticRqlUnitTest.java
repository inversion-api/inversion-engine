package io.inversion.elasticsearch;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import io.inversion.Db;
import io.inversion.rql.AbstractRqlTest;

@TestInstance(Lifecycle.PER_CLASS)
public class ElasticRqlUnitTest extends AbstractRqlTest {

    public ElasticRqlUnitTest() {
        super("northwind/elasticsearch/", "elasticsearch");

        withExpectedResult("eq", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"filter\" : [ { \"term\" : { \"orderID\" : { \"value\" : \"10248\", \"boost\" : 1.0 } } }, { \"term\" : { \"shipCountry\" : { \"value\" : \"France\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("ne", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"term\" : { \"shipCountry\" : { \"value\" : \"France\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("n", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("nn", "index: orders, QueryBuilder={ \"query\" : { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("emp", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"should\" : [ { \"term\" : { \"shipRegion\" : { \"value\" : \"\", \"boost\" : 1.0 } } }, { \"bool\" : { \"must_not\" : [ { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("nemp",
                "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must\" : [ { \"bool\" : { \"must_not\" : [ { \"term\" : { \"shipRegion\" : { \"value\" : \"\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, { \"bool\" : { \"must\" : [ { \"exists\" : { \"field\" : \"shipRegion\", \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("sw", "index: orders, QueryBuilder={ \"query\" : { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"Franc*\", \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("ew", "index: orders, QueryBuilder={ \"query\" : { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"*nce\", \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("w", "index: orders, QueryBuilder={ \"query\" : { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"*ance*\", \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("wo", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"wildcard\" : { \"shipCountry\" : { \"wildcard\" : \"*ance*\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("lt", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : null, \"to\" : \"10\", \"include_lower\" : true, \"include_upper\" : false, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("le", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : null, \"to\" : \"10\", \"include_lower\" : true, \"include_upper\" : true, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("gt", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : \"3.67\", \"to\" : null, \"include_lower\" : false, \"include_upper\" : true, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("ge", "index: orders, QueryBuilder={ \"query\" : { \"range\" : { \"freight\" : { \"from\" : \"3.67\", \"to\" : null, \"include_lower\" : true, \"include_upper\" : true, \"boost\" : 1.0 } } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("in", "index: orders, QueryBuilder={ \"query\" : { \"terms\" : { \"shipCity\" : [ \"Reims\", \"Charleroi\" ], \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("out", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"must_not\" : [ { \"terms\" : { \"shipCity\" : [ \"Reims\", \"Charleroi\" ], \"boost\" : 1.0 } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("and", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"filter\" : [ { \"term\" : { \"shipCity\" : { \"value\" : \"Lyon\", \"boost\" : 1.0 } } }, { \"term\" : { \"shipCountry\" : { \"value\" : \"France\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("or", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"should\" : [ { \"term\" : { \"shipCity\" : { \"value\" : \"Reims\", \"boost\" : 1.0 } } }, { \"term\" : { \"shipCity\" : { \"value\" : \"Charleroi\", \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("not", "UNSUPPORTED");
        withExpectedResult("as", "UNSUPPORTED");
        withExpectedResult("includes", "UNSUPPORTED");
        withExpectedResult("distinct", "UNSUPPORTED");
        withExpectedResult("count1", "UNSUPPORTED");
        withExpectedResult("count2", "UNSUPPORTED");
        withExpectedResult("count3", "UNSUPPORTED");
        withExpectedResult("countAs", "UNSUPPORTED");
        withExpectedResult("sum", "UNSUPPORTED");
        withExpectedResult("sumAs", "UNSUPPORTED");
        withExpectedResult("sumIf", "UNSUPPORTED");
        withExpectedResult("min", "UNSUPPORTED");
        withExpectedResult("max", "UNSUPPORTED");
        withExpectedResult("groupCount", "UNSUPPORTED");
        withExpectedResult("offset", "UNSUPPORTED");
        withExpectedResult("limit", "UNSUPPORTED");
        withExpectedResult("page", "index: orders, QueryBuilder={ \"from\" : 3, \"size\" : 7, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("pageNum", "index: orders, QueryBuilder={ \"from\" : 3, \"size\" : 7, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("after", "UNSUPPORTED");
        withExpectedResult("sort", "index: orders, QueryBuilder={ \"sort\" : [ { \"shipCountry\" : { \"order\" : \"desc\" } }, { \"shipCity\" : { \"order\" : \"asc\" } }, { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("order", "index: orders, QueryBuilder={ \"sort\" : [ { \"shipCountry\" : { \"order\" : \"asc\" } }, { \"shipCity\" : { \"order\" : \"desc\" } }, { \"id\" : { \"order\" : \"asc\" } } ] }");
        withExpectedResult("likeMiddle", "UNSUPPORTED");
        withExpectedResult("likeStartsWith", "UNSUPPORTED");
        withExpectedResult("likeEndsWith", "UNSUPPORTED");
        withExpectedResult("onToManyExistsEq", "UNSUPPORTED");
        withExpectedResult("onToManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyToOneExistsEq", "UNSUPPORTED");
        withExpectedResult("manyToOneNotExistsNe", "UNSUPPORTED");
        withExpectedResult("manyTManyNotExistsNe", "UNSUPPORTED");
        withExpectedResult("eqNonexistantColumn", "index: orders, QueryBuilder={ \"query\" : { \"bool\" : { \"filter\" : [ { \"term\" : { \"nonexistantColumn\" : { \"value\" : \"12\", \"boost\" : 1.0 } } }, { \"range\" : { \"orderId\" : { \"from\" : \"1000\", \"to\" : null, \"include_lower\" : true, \"include_upper\" : true, \"boost\" : 1.0 } } } ], \"adjust_pure_negative\" : true, \"boost\" : 1.0 } }, \"sort\" : [ { \"id\" : { \"order\" : \"asc\" } } ] }");
    }

    @Override
    public Db buildDb() {
        return new ElasticsearchDb("elasticsearch");
    }

}
