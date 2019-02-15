package io.rocketpartners.cloud;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import io.rocketpartners.cloud.action.sql.TestSqlGetAction;
import io.rocketpartners.cloud.action.sql.TestSqlQuery;
import io.rocketpartners.cloud.model.TestRule;
import io.rocketpartners.cloud.rql.TestParser;
import io.rocketpartners.cloud.rql.TestQuery;
import io.rocketpartners.cloud.rql.TestTokenizer;

@RunWith(Suite.class)

@Suite.SuiteClasses({ //
   
//      TestRule.class, //
//   
//      TestTokenizer.class, //
//      TestParser.class, //
//      TestQuery.class,//
      
      TestSqlQuery.class,
      TestSqlGetAction.class //
})

public class TestSuite
{
}

