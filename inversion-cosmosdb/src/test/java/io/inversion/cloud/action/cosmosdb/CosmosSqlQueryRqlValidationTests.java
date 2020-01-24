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

      suite.withResult("eq_queryForEquals", "CosmosDb 'SELECT * FROM orders WHERE orders[\"OrderID\"] = @OrderID1 AND orders[\"ShipCountry\"] = @ShipCountry2 ORDER BY orders[\"OrderID\"] ASC' args={@OrderID1=\"1234\", @ShipCountry2=\"France\"}")//
           .withResult("ne_queryForNotEquals", "CosmosDb 'SELECT * FROM orders WHERE (NOT (ordeasdfasdfasrs[\"ShipCountry\"] = @ShipCountry1)) ORDER BY orders[\"OrderID\"] ASC' args={@ShipCountry1=\"France\"}") 
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
