package io.rocketpartners.cloud;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import io.rocketpartners.cloud.action.dynamo.TestDynamoDbDeleteActions;
import io.rocketpartners.cloud.action.dynamo.TestDynamoDbGetActions;
import io.rocketpartners.cloud.action.dynamo.TestDynamoDbPostActions;
import io.rocketpartners.cloud.action.rest.TestCollapse;
import io.rocketpartners.cloud.action.sql.TestSqlDeleteAction;
import io.rocketpartners.cloud.action.sql.TestSqlGetAction;
import io.rocketpartners.cloud.action.sql.TestSqlPostAction;
import io.rocketpartners.cloud.action.sql.TestSqlQuery;
import io.rocketpartners.cloud.action.sql.TestSqlTokenizer;
import io.rocketpartners.cloud.model.TestDb;
import io.rocketpartners.cloud.model.TestRule;
import io.rocketpartners.cloud.model.TestUrl;
import io.rocketpartners.cloud.rql.TestParser;
import io.rocketpartners.cloud.rql.TestQuery;
import io.rocketpartners.cloud.rql.TestTokenizer;

@RunWith(Suite.class)

@Suite.SuiteClasses({ //

      TestRule.class, //
      TestUrl.class, //
      TestCollapse.class, //

      TestTokenizer.class, //
      TestParser.class, //
      TestQuery.class, //

      TestDb.class, //

      TestSqlTokenizer.class, //
      TestSqlQuery.class, //
      TestSqlGetAction.class, //
      TestSqlPostAction.class, //
      TestSqlDeleteAction.class, //

      TestDynamoDbGetActions.class, //
      TestDynamoDbDeleteActions.class, //
      TestDynamoDbPostActions.class //

})

public class TestSuite
{
}
