package io.inversion.cloud.action.cosmosdb;

import org.junit.Test;

import io.inversion.cloud.rql.RqlValidationSuite;
import junit.framework.TestCase;

public class CosmosSqlQueryRqlValidationTests extends TestCase
{
   @Test
   public void test_doSelect_validateRql() throws Exception
   {
      RqlValidationSuite suite = new RqlValidationSuite(CosmosSqlQuery.class.getName(), new CosmosDocumentDb());

      suite.withResult("eq_queryForEquals", "CosmosDb: SqlQuerySpec={\"query\":\"SELECT * FROM Orders WHERE Orders[\\\"ShipCountry\\\"] = @ShipCountry1 AND Orders[\\\"type\\\"] = @type2 AND Orders[\\\"OrderID\\\"] = @OrderID3 ORDER BY Orders[\\\"id\\\"] ASC\",\"parameters\":[{\"name\":\"@ShipCountry1\",\"value\":\"France\"},{\"name\":\"@type2\",\"value\":\"orders\"},{\"name\":\"@OrderID3\",\"value\":1234}]} FeedOptions={enableCrossPartitionQuery=true}")//
//           .withResult("ne_queryForNotEquals", "CosmosDb 'SELECT * FROM orders WHERE (NOT (ordeasdfasdfasrs[\"ShipCountry\"] = @ShipCountry1)) ORDER BY orders[\"OrderID\"] ASC' args={@ShipCountry1=\"France\"}") 
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
           .withResult("", "")
      ;
      suite.run();
   }
}
