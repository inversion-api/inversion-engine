package io.rocketpartners.cloud.action.dynamo;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({ //

      TestDynamoDbGetActions.class, //
      TestDynamoDbPostActions.class, //
      TestDynamoDbDeleteActions.class //

})

public class TestSuiteDynamoDb
{
}
