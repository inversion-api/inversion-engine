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

      suite.withResult("", "")//
           .withResult("", "")//
      ;
      suite.run();
   }
}
