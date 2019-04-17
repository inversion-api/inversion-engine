package io.rocketpartners.cloud;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import io.rocketpartners.cloud.action.dynamo.TestDynamoDb;
import io.rocketpartners.cloud.action.dynamo.TestDynamoDbDeleteActions;
import io.rocketpartners.cloud.action.dynamo.TestDynamoDbGetActions;
import io.rocketpartners.cloud.action.rest.TestCollapse;
import io.rocketpartners.cloud.action.sql.TestSqlGetAction;
import io.rocketpartners.cloud.action.sql.TestSqlPostAction;
import io.rocketpartners.cloud.action.sql.TestSqlQuery;
import io.rocketpartners.cloud.model.TestRule;
import io.rocketpartners.cloud.rql.TestParser;
import io.rocketpartners.cloud.rql.TestQuery;
import io.rocketpartners.cloud.rql.TestTokenizer;

@RunWith(Suite.class)

@Suite.SuiteClasses({ //

      TestRule.class, //
      TestCollapse.class, //

      TestTokenizer.class, //
      TestParser.class, //
      TestQuery.class, //

      TestSqlQuery.class, //
      TestSqlGetAction.class, //
      TestSqlPostAction.class,

      TestDynamoDb.class, //
      TestDynamoDbGetActions.class, //
      TestDynamoDbDeleteActions.class,

})

public class TestSuite
{
}
