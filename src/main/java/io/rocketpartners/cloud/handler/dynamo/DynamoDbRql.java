/**
 * 
 */
package io.rocketpartners.cloud.handler.dynamo;

import io.rocketpartners.cloud.model.Table;
import io.rocketpartners.cloud.rql.Rql;

/**
 * @author tc-rocket
 *
 */
public class DynamoDbRql extends Rql<Table, DynamoDbQuery>
{

   static
   {
      Rql.addRql(new DynamoDbRql());
   }

   private DynamoDbRql()
   {
      super("dynamo");
      //setDoQuote(false);
   }

   public DynamoDbQuery buildQuery(Table table, Object requestParams)
   {
      DynamoDbQuery dynamoQuery = new DynamoDbQuery(table);
      dynamoQuery.withTerms(requestParams);
      return dynamoQuery;
   }

}
